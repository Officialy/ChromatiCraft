package reika.chromaticraft.auxiliary.structure;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.dragonapi.instantiable.data.blockstruct.FilledBlockArray;
import reika.dragonapi.instantiable.data.immutable.BlockKey;

/**
 * Loads a canonical Minecraft structure NBT and exposes it through DragonAPI's multiblock matcher.
 * The NBT remains the source of geometry; {@link FilledBlockArray} is only the compatibility view
 * used by existing machines, handbook displays, and focused tests while those consumers port.
 */
public final class NBTStructureLoader {

    private NBTStructureLoader() {
    }

    public static FilledBlockArray load(Level world, Identifier templateId, BlockPos worldAnchor,
            BlockPos templateAnchor, UnaryOperator<BlockState> stateTransform) {
        return load(world, templateId, worldAnchor, templateAnchor, stateTransform, true);
    }

    /**
     * @param exactRuneColour V33a wrote most rune cells as {@code setBlock(RUNE, colour.ordinal())},
     *                        so the colour is part of the contract — the pylon's power-crystal
     *                        sockets read theirs from the ring. The casting temple instead used the
     *                        bare {@code RUNE} block instance, which accepted any metadata; pass
     *                        false there so all sixteen registry identities match.
     */
    public static FilledBlockArray load(Level world, Identifier templateId, BlockPos worldAnchor,
            BlockPos templateAnchor, UnaryOperator<BlockState> stateTransform, boolean exactRuneColour) {
        if (!(world instanceof ServerLevel server))
            throw new IllegalStateException("Structure template " + templateId + " requires a server level");

        StructureTemplate template = server.getStructureManager().get(templateId).orElseThrow(() ->
                new IllegalStateException("Missing ChromatiCraft structure template data/" + templateId.getNamespace()
                        + "/structure/" + templateId.getPath() + ".nbt"));
        CompoundTag serialized = template.save(new CompoundTag());
        ListTag paletteTag = serialized.getListOrEmpty("palette");
        List<BlockState> palette = new ArrayList<>(paletteTag.size());
        var blockLookup = server.registryAccess().lookupOrThrow(Registries.BLOCK);
        for (int i = 0; i < paletteTag.size(); i++)
            palette.add(NbtUtils.readBlockState(blockLookup, paletteTag.getCompoundOrEmpty(i)));

        FilledBlockArray result = new FilledBlockArray(world);
        ListTag blocks = serialized.getListOrEmpty("blocks");
        for (int i = 0; i < blocks.size(); i++) {
            CompoundTag entry = blocks.getCompoundOrEmpty(i);
            ListTag coordinates = entry.getListOrEmpty("pos");
            if (coordinates.size() != 3)
                throw new IllegalStateException("Malformed block position in structure " + templateId + ": " + entry);
            BlockPos relative = new BlockPos(
                    coordinates.getIntOr(0, 0), coordinates.getIntOr(1, 0), coordinates.getIntOr(2, 0));
            int paletteIndex = entry.getIntOr("state", -1);
            if (paletteIndex < 0 || paletteIndex >= palette.size())
                throw new IllegalStateException("Invalid palette index " + paletteIndex + " in structure " + templateId);
            BlockState state = stateTransform.apply(palette.get(paletteIndex));
            BlockPos target = worldAnchor.offset(relative.subtract(templateAnchor));
            // Empty cells are part of V33a's multiblock contract, not just a placement instruction:
            // PylonStructure requires its whole 3-wide cross clearance empty, and CastingL1Structure
            // requires the shell interior and the cells around the table. The palette encodes the
            // three distinct meanings the original setEmpty/absent distinction had:
            //   structure_void -> not part of the array at all (deferred or optional cells)
            //   air            -> setEmpty(false, false), the strict V33a check
            //   cave_air       -> setEmpty(true, true), V33a's soft/non-solid-tolerant check
            if (state.is(Blocks.STRUCTURE_VOID))
                continue;
            if (state.is(Blocks.CAVE_AIR)) {
                result.setEmpty(target.getX(), target.getY(), target.getZ(), true, true);
                continue;
            }
            if (state.isAir()) {
                result.setEmpty(target.getX(), target.getY(), target.getZ(), false, false);
                continue;
            }
            // Colour-agnostic runes still place the template's own rune, so the placed structure is
            // a legal member of the set it matches.
            if (!exactRuneColour && ChromaBlocks.isRune(state)) {
                FilledBlockArray.MultiKey colors = new FilledBlockArray.MultiKey();
                ChromaBlocks.RUNES.forEach(rune -> colors.add(new BlockKey(rune.get().defaultBlockState())));
                result.setBlock(target.getX(), target.getY(), target.getZ(), colors);
                result.setPlacementOverride(target.getX(), target.getY(), target.getZ(), state);
            }
            else {
                result.setBlock(target.getX(), target.getY(), target.getZ(), state);
            }
        }
        return result;
    }
    /**
     * Places a canonical template through the bounded world-generation view. This must be used by
     * features instead of creating a {@link FilledBlockArray}: the latter reads existing states
     * through its {@link Level}, and using the backing {@link ServerLevel} during decoration can
     * synchronously request a neighbouring chunk and deadlock the worldgen worker.
     *
     * @return every non-void template position, including explicit air cells
     */
    public static List<BlockPos> place(WorldGenLevel world, Identifier templateId, BlockPos worldAnchor,
            BlockPos templateAnchor, UnaryOperator<BlockState> stateTransform, int flags) {
        ServerLevel server = world.getLevel();
        StructureTemplate template = server.getStructureManager().get(templateId).orElseThrow(() ->
                new IllegalStateException("Missing ChromatiCraft structure template data/" + templateId.getNamespace()
                        + "/structure/" + templateId.getPath() + ".nbt"));
        CompoundTag serialized = template.save(new CompoundTag());
        ListTag paletteTag = serialized.getListOrEmpty("palette");
        List<BlockState> palette = new ArrayList<>(paletteTag.size());
        var blockLookup = server.registryAccess().lookupOrThrow(Registries.BLOCK);
        for (int i = 0; i < paletteTag.size(); i++)
            palette.add(NbtUtils.readBlockState(blockLookup, paletteTag.getCompoundOrEmpty(i)));

        List<BlockPos> placed = new ArrayList<>();
        ListTag blocks = serialized.getListOrEmpty("blocks");
        for (int i = 0; i < blocks.size(); i++) {
            CompoundTag entry = blocks.getCompoundOrEmpty(i);
            ListTag coordinates = entry.getListOrEmpty("pos");
            if (coordinates.size() != 3)
                throw new IllegalStateException("Malformed block position in structure " + templateId + ": " + entry);
            BlockPos relative = new BlockPos(
                    coordinates.getIntOr(0, 0), coordinates.getIntOr(1, 0), coordinates.getIntOr(2, 0));
            int paletteIndex = entry.getIntOr("state", -1);
            if (paletteIndex < 0 || paletteIndex >= palette.size())
                throw new IllegalStateException("Invalid palette index " + paletteIndex + " in structure " + templateId);
            BlockState state = stateTransform.apply(palette.get(paletteIndex));
            if (state.is(Blocks.STRUCTURE_VOID))
                continue;
            // The soft-empty marker only carries a matching rule; it clears to ordinary air.
            if (state.is(Blocks.CAVE_AIR))
                state = Blocks.AIR.defaultBlockState();
            BlockPos target = worldAnchor.offset(relative.subtract(templateAnchor));
            world.setBlock(target, state, flags);
            placed.add(target.immutable());
        }
        return placed;
    }
    public static Identifier chromaTemplate(String path) {
        return Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, path);
    }
}
