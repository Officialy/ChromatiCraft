package reika.chromaticraft.world.luminous;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import reika.chromaticraft.world.dimension.structure.ProximaStructures;
import reika.dragonapi.instantiable.math.noise.NoiseGeneratorBase;
import reika.dragonapi.instantiable.math.noise.SimplexNoiseGenerator;
import reika.dragonapi.libraries.java.ReikaRandomHelper;
import reika.dragonapi.libraries.mathsci.ReikaMathLibrary;

import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * V33a {@code GlowingCliffsAuxGenerator.generateIslands}: where the Luminous Cliffs' floating islands
 * are.
 *
 * <p>Every chunk rolls once. A slow noise sets a size scale of 0.5-2, and the chunk gets a candidate with
 * chance {@code 33/scale} percent — so small islands are the common ones. The candidate must stand in the
 * cliffs; over the sea it is kept one time in three, over land one in six. It floats at least ten blocks
 * above the ground plus its own thickness, with the sea-relative altitude spread stretched for 26.3.
 * The island origin stops at 294 so its six-block contour reaches at most Y300.
 *
 * <p>Upstream built islands after the chunks around them were populated and refused any island that
 * would touch a non-air block. An island is far too large for a feature, so this is a structure: its
 * start saves a candidate and its random seed. The post-population queue completes its terrain and
 * vegetation footprint before doing the original air checks. Children are painted as they are
 * accepted, so their siblings, parent and neighbouring starts see their blocks and decoration.
 */
public final class LuminousIslandStructure extends Structure {

	public static final MapCodec<LuminousIslandStructure> CODEC = simpleCodec(LuminousIslandStructure::new);

	private static final int SCALE_RANGE = 2;
	private static final Map<Long, NoiseGeneratorBase> FREQUENCY = new ConcurrentHashMap<>();

	public LuminousIslandStructure(Structure.StructureSettings settings) {
		super(settings);
	}

	@Override
	public Optional<Structure.GenerationStub> findGenerationPoint(Structure.GenerationContext context) {
		long seed = context.seed();
		int chunkX = context.chunkPos().getMinBlockX();
		int chunkZ = context.chunkPos().getMinBlockZ();
		long candidateSeed = context.random().nextLong();
		Random random = new Random(candidateSeed);

		NoiseGeneratorBase frequency = FREQUENCY.computeIfAbsent(seed,
				s -> new SimplexNoiseGenerator(s << 1).setFrequency(1 / 32D));
		double s = ReikaMathLibrary.normalizeToBounds(frequency.getValue(chunkX, chunkZ), 1D / SCALE_RANGE, SCALE_RANGE);
		if (!ReikaRandomHelper.doWithChance(33 / s, random))
			return Optional.empty();
		int x = chunkX + random.nextInt(16) + 8;
		int z = chunkZ + random.nextInt(16) + 8;

		var resolver = context.biomeSource().createUncachedResolver(context.randomState());
        var mask = new LuminousCliffsBiomeMask(resolver::getNoiseBiome, context.chunkGenerator().getSeaLevel());
        if (!mask.at(x, z).isGlowingCliffs()) return Optional.empty();
        var candidate = new LuminousIslandCandidatePiece(x, z, s, candidateSeed,
                context.heightAccessor().getMinY(), context.heightAccessor().getMaxY() - 1);
        return Optional.of(new Structure.GenerationStub(new BlockPos(x, context.chunkGenerator().getSeaLevel(), z),
                builder -> builder.addPiece(candidate)));
    }

	@Override
	public StructureType<?> type() {
		return ProximaStructures.LUMINOUS_ISLAND_TYPE.get();
	}

    /** V33a generation against completed terrain; children are committed before the parent continues. */
    public static void placeAfterPopulation(net.minecraft.server.level.ServerLevel level,
            net.minecraft.nbt.CompoundTag candidate) {
        int x = candidate.getIntOr("X", 0), z = candidate.getIntOr("Z", 0);
        double scale = candidate.getDoubleOr("Scale", 1);
        Random random = new Random(candidate.getLongOr("Seed", 0));
        // Replay the selection draws, then continue the very same stream through children and ores.
        ReikaRandomHelper.doWithChance(33 / scale, random);
        random.nextInt(16); random.nextInt(16);
        LuminousIsland.Terrain terrain = new LuminousIsland.Terrain() {
            public int topSolidOrLiquid(int bx, int bz) {
                int y = level.getHeight(Heightmap.Types.OCEAN_FLOOR, bx, bz) - 1;
                BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(bx, y, bz);
                while (y >= level.getMinY()) {
                    var state = level.getBlockState(pos.setY(y));
                    if (!state.isAir() && !state.is(net.minecraft.tags.BlockTags.LOGS)
                            && !state.is(net.minecraft.tags.BlockTags.LEAVES)) break;
                    y--;
                }
                return y + 1;
            }
            public boolean isSolid(int bx, int by, int bz) {
                return !level.getBlockState(new BlockPos(bx, by, bz)).isAir();
            }
        };
        boolean water = level.getFluidState(new BlockPos(x, terrain.topSolidOrLiquid(x, z) + 1, z))
                .is(net.minecraft.tags.FluidTags.WATER);
        if (random.nextInt(water ? 3 : 6) != 0) return;
        LuminousIsland island = LuminousIsland.initialize(terrain, x, z, random, scale, water);
        LuminousIsland.generate(terrain, random, level.getSeed(), island, accepted -> {
            // This callback paints immediately: subsequent children and parents see its full body,
            // its trees and its fluids. It also draws ores at the original point in the random stream.
            new LuminousIslandPiece(accepted).placeAfterPopulation(level, random);
        });
    }
}
