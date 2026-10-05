package reika.chromaticraft.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.registry.ChromaOptions;
import reika.dragonapi.libraries.level.ReikaWorldHelper;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * V33a {@code EnderPoolGenerator} and {@code DecoratorEnderForest}: the Liquid Ender pools of the Ender
 * Forest.
 *
 * <p>A pool is a shallow stepped bowl sunk into the ground: three layers of Liquid Ender whose radii
 * fall by one and then two ({@code d, d-1, d-3} for {@code d} of 2-4, so the smallest pools are only two
 * layers deep), and above it three layers of air a block and a half wider than the liquid, which is what
 * leaves the open, slightly sunken hollow around the surface. Half the pools are round; the other half
 * are squashed along one axis by a factor of 0.5-1.
 *
 * <p>Upstream is careful about where one may go, and all of that is here. It probes two to six blocks
 * below the surface for dirt, grass, gravel, stone or sand, and tries a pool at each such depth in turn.
 * Every liquid cell must be a solid block whose sides and floor are all solid, and whose top is neither
 * vanilla water nor lava nor Liquid Ender; no cell of the air above may be vanilla water or lava. Any
 * failure abandons that pool before a single block is written. Once placed, dirt left exposed under the
 * opened hollow is turned to grass.
 *
 * <p>The density is a runtime option in V33a, not worldgen data: {@code ENDERPOOLS} of 1, 2 or 3 is a
 * one-in-12, 6 or 3 chance per chunk. It is therefore rolled here from the live option, not baked into
 * the placed feature, so changing the option changes new chunks without regenerating the datapack.
 */
public final class EnderPoolFeature implements Feature {

    public static final com.mojang.serialization.MapCodec<EnderPoolFeature> CODEC =
            com.mojang.serialization.MapCodec.unit(EnderPoolFeature::new);

    @Override
    public com.mojang.serialization.MapCodec<EnderPoolFeature> codec() {
        return CODEC;
    }

	/** V33a {@code placeables}: exact identities, which is how upstream compared them. */
	private static final Set<Block> PLACEABLES = Set.of(
			Blocks.DIRT, Blocks.GRASS_BLOCK, Blocks.GRAVEL, Blocks.STONE, Blocks.SAND);

	public EnderPoolFeature() {

	}

	/** V33a DecoratorEnderForest's option switch, with its own default for an out-of-range value. */
	static int chunkChance(int option) {
		return switch (option) {
			case 1 -> 12;
			case 2 -> 6;
			case 3 -> 3;
			default -> 6;
		};
	}

	@Override
	public boolean place(net.minecraft.world.level.WorldGenLevel world, net.minecraft.world.level.chunk.ChunkGenerator generator, net.minecraft.util.RandomSource random, net.minecraft.core.BlockPos origin) {
		if (random.nextInt(chunkChance(ChromaOptions.ENDERPOOLS.getValue())) != 0)
			return false;
		for (int i = 2; i < 7; i++) {
			BlockPos at = origin.below(i);
			if (PLACEABLES.contains(world.getBlockState(at).getBlock()) && placeShape(world, random, at))
				return true;
		}
		return false;
	}

	/** V33a generateRandomShapeAt. */
	private static boolean placeShape(WorldGenLevel world, RandomSource random, BlockPos center) {
		BlockState ender = enderBlock();
		Set<BlockPos> blocks = new LinkedHashSet<>();
		boolean ok = random.nextDouble() < 0.5
				? circularPool(world, center, random, blocks)
				: ellipticalPool(world, center, random, blocks);
		if (!ok)
			return false;
		for (BlockPos c : blocks) {
			if (c.getY() <= center.getY()) {
				world.setBlock(c, ender, 2);
			}
			else {
				world.setBlock(c, Blocks.AIR.defaultBlockState(), 2);
				BlockPos below = c.below();
				if (world.getBlockState(below).is(Blocks.DIRT))
					world.setBlock(below, Blocks.GRASS_BLOCK.defaultBlockState(), 2);
			}
		}
		return true;
	}

	/** V33a generateCircularPool. */
	private static boolean circularPool(WorldGenLevel world, BlockPos c, RandomSource random, Set<BlockPos> blocks) {
		int d = 2 + random.nextInt(3);
		int[] r = {d, d - 1, d - 3};
		for (int i = 0; i < 3; i++)
			for (int j = -r[i]; j <= r[i]; j++)
				for (int k = -r[i]; k <= r[i]; k++)
					if (Math.sqrt(j * j + k * k) <= r[i] + 0.5) {
						BlockPos at = c.offset(j, -i, k);
						if (!isValidBlock(world, at))
							return false;
						blocks.add(at);
					}
		for (int i = 0; i < 3; i++)
			for (int j = -r[i] - 1; j <= r[i] + 1; j++)
				for (int k = -r[i] - 1; k <= r[i] + 1; k++)
					if (Math.sqrt(j * j + k * k) <= r[i] + 1.5) {
						BlockPos at = c.offset(j, i + 1, k);
						if (isVanillaLiquid(world.getBlockState(at)))
							return false;
						blocks.add(at);
					}
		return true;
	}

	/**
	 * V33a generateEllipticalPool. The squash applies only to the Z extent, and upstream compares the
	 * loop bound as a double ({@code k <= r*sc}) against a floored start, which is reproduced exactly.
	 */
	private static boolean ellipticalPool(WorldGenLevel world, BlockPos c, RandomSource random, Set<BlockPos> blocks) {
		int d = 2 + random.nextInt(3);
		int[] r = {d, d - 1, d - 3};
		double sc = 0.5 + random.nextDouble() * 0.5;
		for (int i = 0; i < 3; i++)
			for (int j = -r[i]; j <= r[i]; j++)
				for (int k = (int)Math.floor(-r[i] * sc); k <= r[i] * sc; k++)
					if (Math.sqrt(j * j + k * k) <= r[i] + 0.5) {
						BlockPos at = c.offset(j, -i, k);
						if (!isValidBlock(world, at))
							return false;
						blocks.add(at);
					}
		for (int i = 0; i < 3; i++)
			for (int j = -r[i] - 1; j <= r[i] + 1; j++)
				for (int k = (int)Math.floor(-r[i] * sc - 1); k <= r[i] * sc + 1; k++)
					if (Math.sqrt(j * j + k * k) <= r[i] + 1.5) {
						BlockPos at = c.offset(j, i + 1, k);
						if (isVanillaLiquid(world.getBlockState(at)))
							return false;
						blocks.add(at);
					}
		return true;
	}

	/**
	 * V33a isValidBlock. The cell itself must not be soft; above it must be neither vanilla liquid nor
	 * Liquid Ender; every other neighbour must not be soft. Upstream's top-face test is
	 * {@code bid != air && bid instanceof BlockLiquid || bid == id}, which by operator precedence is the
	 * two clauses above joined by or.
	 */
	private static boolean isValidBlock(WorldGenLevel world, BlockPos pos) {
		if (ReikaWorldHelper.softBlocks(world, pos))
			return false;
		Block ender = enderBlock().getBlock();
		for (Direction dir : Direction.values()) {
			BlockPos side = pos.relative(dir);
			if (dir == Direction.UP) {
				BlockState above = world.getBlockState(side);
				if (isVanillaLiquid(above) || above.is(ender))
					return false;
			}
			else if (ReikaWorldHelper.softBlocks(world, side)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * V33a tests {@code instanceof BlockLiquid}, which in 1.7.10 is vanilla water and lava only — Forge
	 * fluid blocks, Liquid Ender among them, were a different class. Every 26.2 fluid block is a
	 * {@code LiquidBlock}, so the two vanilla fluids are named instead to keep the source's meaning.
	 */
	private static boolean isVanillaLiquid(BlockState state) {
		return state.is(Blocks.WATER) || state.is(Blocks.LAVA);
	}

	/**
	 * V33a {@code ChromatiCraft.getEnderBlockToGenerate}: Thermal Foundation's Resonant Ender when that
	 * mod was installed, otherwise ChromatiCraft's own Liquid Ender.
	 *
	 * <p>CHROMA-PORT: Thermal Foundation has no 26.2 release this port links against, so the Thermal
	 * branch is recorded rather than reproduced and the pools are always ChromatiCraft's own fluid.
	 */
	private static BlockState enderBlock() {
		return ChromaBlocks.ENDER.get().defaultBlockState();
	}
}
