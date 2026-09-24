package reika.chromaticraft.world.luminous;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import reika.chromaticraft.world.GlowingCliffsColumnShaper;
import reika.chromaticraft.world.GlowingCliffsColumnShaper.GlowCliffRegion;
import reika.chromaticraft.world.dimension.structure.ProximaStructures;
import reika.dragonapi.instantiable.math.noise.NoiseGeneratorBase;
import reika.dragonapi.instantiable.math.noise.SimplexNoiseGenerator;
import reika.dragonapi.libraries.java.ReikaRandomHelper;
import reika.dragonapi.libraries.mathsci.ReikaMathLibrary;

/**
 * V33a {@code GlowingCliffsAuxGenerator.generateIslands}: where the Luminous Cliffs' floating islands
 * are.
 *
 * <p>Every chunk rolls once. A slow noise sets a size scale of 0.5-2, and the chunk gets a candidate with
 * chance {@code 33/scale} percent — so small islands are the common ones. The candidate must stand in the
 * cliffs; over the sea it is kept one time in three, over land one in six. It floats at least ten blocks
 * above the ground plus its own thickness (and never below 90), plus up to 80 more, capped at 250.
 *
 * <p>Upstream built islands after the chunks around them were populated and refused any island that
 * would touch a non-air block. An island is far too large for a feature, so this is a structure: its
 * start is decided here, before the terrain it floats over has been generated, and the ground is
 * predicted instead — the cliffs from {@link GlowingCliffsColumnShaper} (the same function the terrain
 * pass runs), anything else from the chunk generator's own height, blended as the terrain pass blends
 * it. What cannot be predicted is recorded, not guessed: trees, and islands started from other chunks.
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
		Random random = new Random(context.random().nextLong());

		NoiseGeneratorBase frequency = FREQUENCY.computeIfAbsent(seed,
				s -> new SimplexNoiseGenerator(s << 1).setFrequency(1 / 32D));
		double s = ReikaMathLibrary.normalizeToBounds(frequency.getValue(chunkX, chunkZ), 1D / SCALE_RANGE, SCALE_RANGE);
		if (!ReikaRandomHelper.doWithChance(33 / s, random))
			return Optional.empty();
		int x = chunkX + random.nextInt(16) + 8;
		int z = chunkZ + random.nextInt(16) + 8;

		Prediction terrain = new Prediction(context);
		if (!terrain.sampler.biome(x, z).isGlowingCliffs())
			return Optional.empty();
		boolean water = terrain.column(x, z).region() == GlowCliffRegion.WATER;
		int c = water ? 3 : 6;
		if (random.nextInt(c) != 0)
			return Optional.empty();

		LuminousIsland island = LuminousIsland.initialize(terrain, x, z, random, s, water);
		List<LuminousIsland> accepted = new ArrayList<>();
		LuminousIsland.generate(terrain, random, seed, island, accepted);
		if (accepted.isEmpty())
			return Optional.empty();

		// V33a draws each accepted island's ore veins from the same random after generating it.
		List<LuminousIslandPiece> pieces = new ArrayList<>();
		for (LuminousIsland is : accepted)
			pieces.add(new LuminousIslandPiece(is, LuminousIslandPiece.rollOres(is, random)));
		BlockPos anchor = new BlockPos(x, context.chunkGenerator().getSeaLevel(), z);
		return Optional.of(new Structure.GenerationStub(anchor, builder -> pieces.forEach(builder::addPiece)));
	}

	@Override
	public StructureType<?> type() {
		return ProximaStructures.LUMINOUS_ISLAND_TYPE.get();
	}

	/** The ground an island must clear, predicted from the shaper and the chunk generator. */
	private static final class Prediction implements LuminousIsland.Terrain {

		private final Structure.GenerationContext context;
		final GlowingCliffsColumnShaper.Sampler sampler;
		private final Map<Long, GlowingCliffsColumnShaper.Column> columns = new java.util.HashMap<>();
		private final Map<Long, Integer> otherTops = new java.util.HashMap<>();

		Prediction(Structure.GenerationContext context) {
			this.context = context;
			var source = context.biomeSource();
			var climate = context.randomState().sampler();
			this.sampler = GlowingCliffsColumnShaper.forSeed(context.seed()).new Sampler(new LuminousCliffsBiomeMask(
					(qx, qy, qz) -> source.getNoiseBiome(qx, qy, qz, climate), context.chunkGenerator().getSeaLevel()));
		}

		private GlowingCliffsColumnShaper.Column column(int x, int z) {
			return columns.computeIfAbsent(BlockPos.asLong(x, 0, z), k -> sampler.column(x, z));
		}

		/** The top solid block of a column outside the cliffs, after the terrain pass's blend. */
		private int otherTop(int x, int z) {
			return otherTops.computeIfAbsent(BlockPos.asLong(x, 0, z), k -> this.computeOtherTop(x, z));
		}

		private int computeOtherTop(int x, int z) {
			int base = context.chunkGenerator().getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG,
					context.heightAccessor(), context.randomState()) - 1;
			int blended = sampler.blendedTop(x, z, base);
			return blended < 0 ? base : blended;
		}

		/** Minecraft 1.7.10 {@code getTopSolidOrLiquidBlock}: one above the top solid block. */
		@Override
		public int topSolidOrLiquid(int x, int z) {
			if (!sampler.biome(x, z).isGlowingCliffs())
				return this.otherTop(x, z) + 1;
			GlowingCliffsColumnShaper.Column col = this.column(x, z);
			return col.region() == GlowCliffRegion.WATER ? col.oceanFloor() : col.top() + 1;
		}

		@Override
		public boolean isSolid(int x, int y, int z) {
			if (!sampler.biome(x, z).isGlowingCliffs())
				return y <= this.otherTop(x, z);
			return this.column(x, z).isSolid(y);
		}
	}
}
