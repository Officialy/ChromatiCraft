package reika.chromaticraft.auxiliary.structure;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;
import java.util.function.BiPredicate;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.util.ProblemReporter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.storage.TagValueInput;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.dragonapi.instantiable.data.blockstruct.FilledBlockArray;
import reika.dragonapi.instantiable.data.immutable.BlockKey;
import reika.dragonapi.interfaces.BlockCheck;

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
        return load(world, templateId, worldAnchor, templateAnchor, stateTransform, exactRuneColour, null);
    }

    /**
     * A per-position override of the default exact-state comparison. V33a's {@code FilledBlockArray}
     * accepted three different cell contracts for the same block — an exact {@code (block, metadata)}
     * pair, a metadata wildcard, and {@code FluidCheck}'s source/non-source distinction — and some
     * structures (the portal's Luma fountain) use two of them on the same block at different
     * positions. A structure that needs those semantics supplies them here so they stay attached to
     * the template cell instead of being flattened into a literal state match.
     */
    @FunctionalInterface
    public interface CellRule {
        /**
         * @param relative the position within the template
         * @param state    the state the template holds there
         * @return the check this cell requires, or {@code null} to use the default handling
         */
        BlockCheck check(BlockPos relative, BlockState state);
    }

    public static FilledBlockArray load(Level world, Identifier templateId, BlockPos worldAnchor,
            BlockPos templateAnchor, UnaryOperator<BlockState> stateTransform, boolean exactRuneColour,
            CellRule cellRule) {
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
            if (cellRule != null) {
                BlockCheck override = cellRule.check(relative, state);
                if (override != null) {
                    result.setBlock(target.getX(), target.getY(), target.getZ(), override);
                    result.setPlacementOverride(target.getX(), target.getY(), target.getZ(), state);
                    continue;
                }
            }
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
        try (ProblemReporter.ScopedCollector reporter = new ProblemReporter.ScopedCollector(
                org.slf4j.LoggerFactory.getLogger(NBTStructureLoader.class))) {
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
            CompoundTag blockEntityData = entry.getCompound("nbt").orElse(null);
            // Match StructureTemplate.placeInWorld: force any old block entity out before creating
            // and hydrating the new one. Without this, spawner timings, loot markers, controller
            // state, and every future NBT-backed structure callback silently disappear.
            if (blockEntityData != null)
                world.setBlock(target, Blocks.BARRIER.defaultBlockState(), 820);
            world.setBlock(target, state, flags);
            if (blockEntityData != null) {
                BlockEntity blockEntity = world.getBlockEntity(target);
                if (blockEntity != null) {
                    blockEntity.loadWithComponents(TagValueInput.create(
                            reporter.forChild(blockEntity.problemPath()), world.registryAccess(), blockEntityData));
                    blockEntity.setChanged();
                }
            }
            placed.add(target.immutable());
        }
        }
        resolveConnectedShapes(world, placed, flags);
        return placed;
    }

    /**
     * Second pass, matching {@code StructureTemplate.placeInWorld}: every placed cell re-resolves its
     * state against its neighbours, then fires a block update.
     *
     * <p>Without this, anything whose appearance depends on its neighbours keeps whatever state the
     * template stored: fences, walls, panes, iron bars, stairs and vines all come out unconnected,
     * because a template necessarily serialises them in their default shape.
     *
     * <p>Redstone is the case worth spelling out, because it was measured while chasing a Nether Temple
     * fault. {@code onPlace} runs regardless of the update flags but only recomputes <em>power</em>; the
     * connection shape comes from {@code updateShape}, which is exactly what this pass drives. That
     * still leaves a trap upstream of here: {@code RedStoneWireBlock.getConnectionState} short-circuits
     * on a wire that is already a dot, so a template storing "none" on all four sides stays a dot
     * through every update. The templates therefore author wires as crosses, and this pass resolves
     * them down to whatever their neighbours actually justify.
     */
    private static void resolveConnectedShapes(WorldGenLevel world, List<BlockPos> placed, int flags) {
        for (BlockPos pos : placed) {
            BlockState current = world.getBlockState(pos);
            BlockState resolved = Block.updateFromNeighbourShapes(current, world, pos);
            if (current != resolved)
                world.setBlock(pos, resolved,
                        flags & ~Block.UPDATE_NEIGHBORS | Block.UPDATE_KNOWN_SHAPE);
            world.updateNeighborsAt(pos, resolved.getBlock());
        }
    }

    /**
     * Tests only the cells actually present in a template against the bounded worldgen view. This
     * is deliberately separate from {@link #load}: features must never read through the backing
     * ServerLevel while neighbouring decoration chunks may still be locked by other workers.
     */
    public static boolean canPlace(WorldGenLevel world, Identifier templateId, BlockPos worldAnchor,
            BlockPos templateAnchor, UnaryOperator<BlockState> stateTransform,
            BiPredicate<BlockPos, BlockState> cellPredicate) {
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
            BlockPos target = worldAnchor.offset(relative.subtract(templateAnchor));
            if (!cellPredicate.test(target, stateTransform.apply(palette.get(paletteIndex))))
                return false;
        }
        return true;
    }
    public static Identifier chromaTemplate(String path) {
        return Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, path);
    }
}
