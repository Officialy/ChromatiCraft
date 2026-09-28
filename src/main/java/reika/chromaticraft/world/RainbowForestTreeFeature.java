package reika.chromaticraft.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;

import reika.chromaticraft.block.dye26.BlockDyeFlower;
import reika.chromaticraft.block.dye26.BlockDyeSapling;
import reika.chromaticraft.block.dye26.BlockDyeVine;
import reika.chromaticraft.block.worldgen26.BlockTieredPlant;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.registry.CrystalElement;
import reika.dragonapi.instantiable.math.noise.NoiseGeneratorBase;
import reika.dragonapi.instantiable.math.noise.Simplex3DGenerator;
import reika.dragonapi.instantiable.math.noise.VoronoiNoiseGenerator;
import reika.dragonapi.libraries.level.ReikaWorldHelper;
import reika.dragonapi.libraries.mathsci.ReikaMathLibrary;

/**
 * V33a {@code RainbowForestGenerator}: one tree attempt of the Rainbow Forest, nine per chunk.
 *
 * <p>The tree's colour is not random. It comes from a 3D Voronoi field of cells about twenty blocks
 * across, their seed points jittered 45% and the whole field warped by simplex noise, and each cell
 * hashes to one of the sixteen dyes. So the forest is a patchwork of single-colour groves with ragged
 * borders — which is the look of the biome.
 *
 * <p>One attempt in ten tries a rainbow tree instead: the large authored tree if it fits, followed by
 * ten patches of mud scattered within six blocks (each with a 2/3 chance of spreading to each side);
 * otherwise, one time in five, the small rainbow tree. If neither is placed, or on the other nine
 * attempts in ten, it is a dye tree (see {@link TreeShaper}), two times in five with vines.
 *
 * <p>CHROMA-PORT: with Thaumcraft present and {@code ETHEREAL} on, the eight extra patches after the
 * mud were Ethereal Blooms. Thaumcraft-only behaviour stays disabled, so those eight attempts are not
 * made.
 */
public final class RainbowForestTreeFeature implements Feature {

    public static final com.mojang.serialization.MapCodec<RainbowForestTreeFeature> CODEC =
            com.mojang.serialization.MapCodec.unit(RainbowForestTreeFeature::new);

    @Override
    public com.mojang.serialization.MapCodec<RainbowForestTreeFeature> codec() {
        return CODEC;
    }

	private static final float FERTILE_VINE_CHANCE_FACTOR = 0.12F;

	private static final long ROOT = 34897534781342377L;
	private static final NoiseGeneratorBase EXTRA_X = new Simplex3DGenerator(4587 ^ ROOT).setFrequency(1 / 8D);
	private static final NoiseGeneratorBase EXTRA_Y = new Simplex3DGenerator(-3735 ^ ROOT).setFrequency(1 / 8D);
	private static final NoiseGeneratorBase EXTRA_Z = new Simplex3DGenerator(823741 ^ ROOT).setFrequency(1 / 8D);
	private static final VoronoiNoiseGenerator COLOR_NOISE = createColorNoise();

	/** V33a {@code woodGlowNoise}, which picks the Rainbow Forest log-glow overlay. */
	private static final NoiseGeneratorBase WOOD_GLOW_NOISE = new Simplex3DGenerator(191327 ^ ROOT).setFrequency(0.8);

	public RainbowForestTreeFeature() {

	}

	private static VoronoiNoiseGenerator createColorNoise() {
		VoronoiNoiseGenerator noise = (VoronoiNoiseGenerator)new VoronoiNoiseGenerator(83745 ^ ROOT)
				.setFrequency(1 / 20D).setDisplacement(EXTRA_X, EXTRA_Y, EXTRA_Z, 6);
		// V33a set this on every getColor call; it never changes, so it is set once.
		noise.randomFactor = 0.45;
		return noise;
	}

	/** V33a {@code getColor}: the dye of the Voronoi cell containing the position. */
	public static CrystalElement getColor(int x, int y, int z) {
		return CrystalElement.elements[(COLOR_NOISE.getClosestRoot(x, y, z).hashCode() % 16 + 16) % 16];
	}

	/** V33a {@code getWoodGlowIndex}. */
	public static int getWoodGlowIndex(int x, int y, int z, int icons) {
		return Mth.clamp((int)Math.round(ReikaMathLibrary.normalizeToBounds(
				WOOD_GLOW_NOISE.getValue(x, y, z), -0.6, icons - 0.4)), 0, icons - 1);
	}

	@Override
	public boolean place(net.minecraft.world.level.WorldGenLevel world, net.minecraft.world.level.chunk.ChunkGenerator generator, net.minecraft.util.RandomSource random, net.minecraft.core.BlockPos origin) {
		int x = origin.getX();
		int z = origin.getZ();
		BlockPos pos = new BlockPos(x, ReikaWorldHelper.getTopSolidOrLiquidBlock(world, x, z), z);
		if (!TreeShaper.canGenerateTree(world, x, z) || !BlockDyeSapling.canGrowAt(world, pos, true))
			return false;
		CrystalElement color = getColor(pos.getX(), pos.getY(), pos.getZ());
		if (random.nextInt(10) == 0) {
			boolean generated = false;
			BlockState log = RainbowTreeFeature.chooseLog(random);
			if (RainbowTreeFeature.placeLarge(world, pos, log)) {
				generated = true;
				for (int i = 0; i < 10; i++) {
					int dx = x - 6 + random.nextInt(13);
					int dz = z - 6 + random.nextInt(13);
					int dy = ReikaWorldHelper.getTopSolidOrLiquidBlock(world, dx, dz);
					tryGenerateMud(world, dx, dy, dz);
					for (Direction dir : Direction.Plane.HORIZONTAL) {
						if (random.nextFloat() < 0.67F)
							tryGenerateMud(world, dx + dir.getStepX(), dy, dz + dir.getStepZ());
					}
				}
			}
			else if (random.nextInt(5) == 0) {
				generated = RainbowTreeFeature.placeSmall(world, pos, random, log);
			}
			if (!generated)
				TreeShaper.generateRandomWeightedTree(world, pos, random, color, false, getVineChance(random), FERTILE_VINE_CHANCE_FACTOR);
		}
		else {
			TreeShaper.generateRandomWeightedTree(world, pos, random, color, false, getVineChance(random), FERTILE_VINE_CHANCE_FACTOR);
		}
		return true;
	}

	/**
	 * V33a {@code tryGenerateMud}: sink through vines, tiered plants, air and leaves to the ground; on
	 * grass whose four sides are all solid, with a dye flower or something soft on top, clear the top
	 * and turn the grass to mud.
	 */
	private static boolean tryGenerateMud(WorldGenLevel world, int x, int y, int z) {
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, y, z);
		BlockState below = world.getBlockState(pos.below());
		while (y > world.getMinY() && sinksThrough(below)) {
			y--;
			pos.setY(y);
			below = world.getBlockState(pos.below());
		}
		if (!below.is(Blocks.GRASS_BLOCK))
			return false;
		BlockPos ground = pos.below();
		for (Direction dir : Direction.Plane.HORIZONTAL)
			if (ReikaWorldHelper.softBlocks(world, ground.relative(dir)))
				return false;
		if (!(world.getBlockState(pos).getBlock() instanceof BlockDyeFlower) && !ReikaWorldHelper.softBlocks(world, pos))
			return false;
		world.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
		world.setBlock(ground, ChromaBlocks.MUD.get().defaultBlockState(), 2);
		return true;
	}

	private static boolean sinksThrough(BlockState state) {
		return state.getBlock() instanceof BlockDyeVine || state.getBlock() instanceof BlockTieredPlant
				|| state.isAir() || state.is(BlockTags.LEAVES);
	}

	/** V33a {@code getVineChance}: two times in five, 0.6-1.0; otherwise no vines. */
	private static float getVineChance(RandomSource rand) {
		return rand.nextInt(5) <= 1 ? 0.6F + rand.nextFloat() * 0.4F : 0;
	}
}
