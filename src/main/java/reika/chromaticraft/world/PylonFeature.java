package reika.chromaticraft.world;

import java.util.BitSet;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import net.neoforged.neoforge.common.NeoForge;

import reika.chromaticraft.api.event.PylonGenerationEvent;
import reika.chromaticraft.auxiliary.structure.PylonStructure;
import reika.chromaticraft.block.BlockCrystallineStone;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.registry.ChromaOptions;
import reika.chromaticraft.registry.CrystalElement;
import reika.chromaticraft.tileentity.networking.TileEntityCrystalPylon;
import reika.chromaticraft.tileentity.auxiliary.TileEntityChromaCrystal;
import reika.chromaticraft.block.BlockCrystallineStone.StoneTypes;

/**
 * Natural pylon generation, expressed as a modern feature while preserving V33a's shuffled
 * 256-chunk grid, 24 surface probes, footprint rules, optional damage, and generation event.
 * Geometry is always placed from the canonical {@code multiblock/pylon.nbt} template.
 */
public final class PylonFeature extends Feature<NoneFeatureConfiguration> {

    public enum Variant {
        NORMAL,
        TURBOCHARGED,
        POWER_CRYSTAL_BOOSTED
    }

    private static final int GRID_SIZE = 256;
    private static final int GRID_DEVIATION = 4;
    private static final int GRID_SEPARATION = 10;
    private static final int ATTEMPTS = 24;
    private static final ConcurrentHashMap<Long, BitSet> GRIDS = new ConcurrentHashMap<>();
    private final Variant variant;
    private final CrystalElement fixedColor;

    public PylonFeature() {
        this(Variant.NORMAL, null);
    }

    public PylonFeature(Variant variant) {
        this(variant, null);
    }

    /** Command/debug feature whose registry identity guarantees its pylon colour. */
    public PylonFeature(CrystalElement color) {
        this(Variant.NORMAL, color);
    }

    private PylonFeature(Variant variant, CrystalElement fixedColor) {
        super(NoneFeatureConfiguration.CODEC);
        this.variant = variant;
        this.fixedColor = fixedColor;
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel world = context.level();

        BlockPos origin = context.origin();

        RandomSource random = context.random();
        for (int i = 0; i < ATTEMPTS; i++) {
            int x = origin.getX() + random.nextInt(16);
            int z = origin.getZ() + random.nextInt(16);
            if (tryPlaceAt(world, new BlockPos(x, groundLevel(world, x, z), z), random, variant, fixedColor))
                return true;
        }
        return false;
    }

    /**
     * The topmost non-foliage solid block, which is what V33a's site picker gave it.
     *
     * <p>1.7.10's {@code getTopSolidOrLiquidBlock} skipped {@code Material.leaves} and anything
     * {@code isFoliage}, so in a forest it returned the ground under the canopy. The nearest modern
     * heightmap, {@code WORLD_SURFACE_WG}, is simply "not air" and returns the top of the tree -- so
     * every attempt that landed in a forest hit leaves and was rejected outright by the log/leaf test
     * in {@link #canGenerateAt}, and forests are a large share of the overworld. That is a big part of
     * why pylons became rare. Descending past the canopy restores the original behaviour.
     */
    private static int groundLevel(WorldGenLevel world, int x, int z) {
        int y = world.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
        int floor = world.getMinY();
        while (y > floor) {
            BlockState state = world.getBlockState(new BlockPos(x, y, z));
            if (!state.is(BlockTags.LEAVES) && !state.is(BlockTags.LOGS) && !state.isAir())
                break;
            y--;
        }
        return y;
    }

    public static boolean isSelectedChunk(long worldSeed, int chunkX, int chunkZ) {
        BitSet grid = GRIDS.computeIfAbsent(worldSeed, PylonFeature::createGrid);
        int x = Math.floorMod(chunkX, GRID_SIZE);
        int z = Math.floorMod(chunkZ, GRID_SIZE);
        return grid.get(x * GRID_SIZE + z);
    }

    private static BitSet createGrid(long seed) {
        Random random = new Random(seed);
        random.nextBoolean();
        random.nextBoolean();
        BitSet grid = new BitSet(GRID_SIZE * GRID_SIZE);
        for (int x = GRID_DEVIATION; x < GRID_SIZE - GRID_DEVIATION; x += GRID_SEPARATION) {
            for (int z = GRID_DEVIATION; z < GRID_SIZE - GRID_DEVIATION; z += GRID_SEPARATION) {
                int px = x - GRID_DEVIATION + random.nextInt(GRID_DEVIATION * 2 + 1);
                int pz = z - GRID_DEVIATION + random.nextInt(GRID_DEVIATION * 2 + 1);
                grid.set(px * GRID_SIZE + pz);
            }
        }
        return grid;
    }

    /** Deterministic placement seam for worldgen and focused GameTests. */
    public static boolean tryPlaceAt(WorldGenLevel world, BlockPos base, RandomSource random) {
        return tryPlaceAt(world, base, random, Variant.NORMAL);
    }

    public static boolean tryPlaceAt(WorldGenLevel world, BlockPos base, RandomSource random, Variant variant) {
        return tryPlaceAt(world, base, random, variant, null);
    }

    private static boolean tryPlaceAt(WorldGenLevel world, BlockPos base, RandomSource random,
            Variant variant, CrystalElement fixedColor) {
        if (!canGenerateAt(world, base))
            return false;

        CrystalElement color = fixedColor != null ? fixedColor
                : CrystalElement.elements[random.nextInt(CrystalElement.elements.length)];
        BlockPos pylonPos = base.above(9);
        List<BlockPos> templateBlocks = PylonStructure.placeForWorldgen(world, pylonPos, color, 3,
                variant == Variant.TURBOCHARGED);
        boolean broken = variant == Variant.NORMAL && ChromaOptions.BROKENPYLON.getState()
                && random.nextInt(2) == 0;
        if (broken)
            breakPylon(world, templateBlocks, random);
        placeFoundation(world, base);

        world.setBlock(pylonPos, ChromaBlocks.PYLON.get().defaultBlockState(), 3);
        if (!(world.getBlockEntity(pylonPos) instanceof TileEntityCrystalPylon pylon))
            return false;
        pylon.initializeGenerated(color, !broken);
        if (variant == Variant.TURBOCHARGED)
            pylon.enhance();
        else if (variant == Variant.POWER_CRYSTAL_BOOSTED)
            placePowerCrystals(world, pylon, pylonPos);
        NeoForge.EVENT_BUS.post(new PylonGenerationEvent(world, pylonPos, random, broken, color));
        return true;
    }

    private static void placePowerCrystals(WorldGenLevel world, TileEntityCrystalPylon pylon,
            BlockPos pylonPos) {
        long value = pylonPos.asLong();
        java.util.UUID sharedOwner = new java.util.UUID(value, ~value);
        for (BlockPos offset : TileEntityCrystalPylon.getPowerCrystalLocations()) {
            BlockPos crystalPos = pylonPos.offset(offset);
            world.setBlock(crystalPos, ChromaBlocks.POWER_CRYSTAL.get().defaultBlockState(), 3);
            if (world.getBlockEntity(crystalPos) instanceof TileEntityChromaCrystal crystal)
                crystal.initializeGeneratedBoost(pylonPos, sharedOwner);
        }
    }
    public static boolean canGenerateAt(WorldGenLevel world, BlockPos base) {
        if (base.getY() <= world.getMinY() || base.getY() + 9 >= world.getMaxY())
            return false;
        if (world.getBlockState(new BlockPos(base.getX(), world.getMinY(), base.getZ())).isAir())
            return false;
        BlockState center = world.getBlockState(base);
        if (center.is(BlockTags.LOGS) || center.is(BlockTags.LEAVES))
            return false;

        for (int y = 0; y <= 9; y++) {
            for (int direction = 0; direction < 4; direction++) {
                int stepX = direction == 0 ? 1 : direction == 1 ? -1 : 0;
                int stepZ = direction == 2 ? 1 : direction == 3 ? -1 : 0;
                for (int distance = 0; distance <= 3; distance++) {
                    for (int lateral = -1; lateral <= 1; lateral++) {
                        int dx = stepX * distance + stepZ * lateral;
                        int dz = stepZ * distance + stepX * lateral;
                        BlockState state = world.getBlockState(base.offset(dx, y, dz));
                        if (!state.getFluidState().isEmpty())
                            return false;
                        if (y == 0 ? !isFloorReplaceable(state) : !isAirReplaceable(state))
                            return false;
                    }
                }
            }
        }
        return true;
    }

    private static boolean isFloorReplaceable(BlockState state) {
        return state.canBeReplaced() || state.is(BlockTags.BASE_STONE_OVERWORLD)
                || state.is(BlockTags.DIRT) || state.is(BlockTags.SAND)
                || state.is(Blocks.GRAVEL) || state.is(Blocks.TERRACOTTA)
                || state.is(BlockTags.LOGS) || state.is(BlockTags.LEAVES);
    }

    private static boolean isAirReplaceable(BlockState state) {
        return state.isAir() || state.canBeReplaced() || state.is(BlockTags.DIRT)
                || state.is(Blocks.GRAVEL) || state.is(BlockTags.LOGS) || state.is(BlockTags.LEAVES);
    }

    private static void placeFoundation(WorldGenLevel world, BlockPos base) {
        BlockState stone = ChromaBlocks.crystallineStone(StoneTypes.SMOOTH).get().defaultBlockState();
        for (int y = -4; y < 0; y++) {
            for (int direction = 0; direction < 4; direction++) {
                int stepX = direction == 0 ? 1 : direction == 1 ? -1 : 0;
                int stepZ = direction == 2 ? 1 : direction == 3 ? -1 : 0;
                for (int distance = 0; distance <= 3; distance++) {
                    for (int lateral = -1; lateral <= 1; lateral++) {
                        int dx = stepX * distance + stepZ * lateral;
                        int dz = stepZ * distance + stepX * lateral;
                        BlockPos pos = base.offset(dx, y, dz);
                        BlockState state = world.getBlockState(pos);
                        if (state.isAir() || state.canBeReplaced())
                            world.setBlock(pos, stone, 3);
                    }
                }
            }
        }
    }
    private static void breakPylon(WorldGenLevel world, List<BlockPos> templateBlocks, RandomSource random) {
        List<BlockPos> eligible = templateBlocks.stream().filter(pos -> {
            BlockState state = world.getBlockState(pos);
            if (!BlockCrystallineStone.isCrystallineStone(state.getBlock()))
                return false;
            StoneTypes type = ((BlockCrystallineStone)state.getBlock()).getStoneType();
            return type == StoneTypes.SMOOTH || type == StoneTypes.BEAM || type == StoneTypes.COLUMN
                    || type == StoneTypes.ENGRAVED || type == StoneTypes.EMBOSSED;
        }).collect(java.util.stream.Collectors.toCollection(java.util.ArrayList::new));
        int count = 3 + random.nextInt(4);
        for (int removed = 0; removed < count && !eligible.isEmpty(); removed++) {
            BlockPos pos = eligible.remove(random.nextInt(eligible.size()));
            world.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        }
    }
}

