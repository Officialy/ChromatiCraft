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
            // Structure-template air is a placement instruction, not part of V33a's multiblock
            // contract. The original FilledBlockArray structures listed required solids only; making
            // every palette air cell a setEmpty check prevents legal adjacent automation (including
            // the Casting Table's own six-direction output pass) and invalidates structures when the
            // surrounding volume contains harmless blocks.
            if (state.is(Blocks.STRUCTURE_VOID) || state.isAir())
                continue;
            if (ChromaBlocks.isRune(state)) {
                FilledBlockArray.MultiKey colors = new FilledBlockArray.MultiKey();
                ChromaBlocks.RUNES.forEach(rune -> colors.add(new BlockKey(rune.get().defaultBlockState())));
                result.setBlock(target.getX(), target.getY(), target.getZ(), colors);
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
