package reika.chromaticraft.world;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.registry.ChromaDecoFlowers;

/**
 * V33a {@code DecoFlowerGenerator}, one feature per flower.
 *
 * <p>Upstream makes a burst rather than a scatter: once the one-in-N chunk roll passes, it wants
 * {@code n} placements, where n is usually 1 but one time in five is {@code 1 + rand(4) + rand(6)},
 * and it keeps trying positions until it has them or has burned 40 attempts. That shape is kept
 * here, because expressing it as a vanilla count modifier would lose the occasional dense patch.
 * The chunk roll itself is the placed feature's {@code rarity_filter}.
 *
 * <p>Void Reeds and Aura Ivy additionally extend along their column once sited — reeds upward in a
 * run of up to 4, ivy downward in a run of up to 12.
 */
public final class DecoFlowerFeature extends Feature<NoneFeatureConfiguration> {

	private static final int MAX_TRIES = 40;

	private final ChromaDecoFlowers flower;

	public DecoFlowerFeature(ChromaDecoFlowers flower) {
		super(NoneFeatureConfiguration.CODEC);
		this.flower = flower;
	}

	@Override
	public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
		WorldGenLevel world = context.level();
		RandomSource random = context.random();
		BlockPos origin = context.origin();
		BlockState state = ChromaBlocks.decoFlower(flower).get().defaultBlockState();
		int wanted = random.nextInt(5) == 0 ? 1 + random.nextInt(4) + random.nextInt(6) : 1;
		int done = 0;
		for (int tries = 0; done < wanted && tries < MAX_TRIES; tries++) {
			// V33a insets Aura Ivy one block from the chunk edge specifically to stop chunk spilling,
			// since it then extends sideways-supported down a column.
			int inset = flower.siting() == ChromaDecoFlowers.Siting.IVY ? 1 : 0;
			int span = 16 - inset * 2;
			int x = origin.getX() + inset + random.nextInt(span);
			int z = origin.getZ() + inset + random.nextInt(span);
			BlockPos pos = this.findSite(world, x, z, random, state);
			if (pos == null)
				continue;
			world.setBlock(pos, state, 2);
			this.extendRun(world, pos, random, state);
			done++;
		}
		return done > 0;
	}

	/**
	 * V33a's column walk: start within 25 blocks of the surface, settle, and take the first spot that
	 * will hold this flower.
	 *
	 * <p>Aura Ivy is deliberately not the same walk. V33a skips the "drop out of the air" step for it,
	 * then climbs to the *top* of whatever wall it found and requires air beneath, because ivy hangs
	 * off a face and grows downward. Running it through the ordinary path settled it in open air above
	 * the ground, where its "solid block horizontally adjacent" test cannot pass, and it generated in
	 * zero chunks.
	 */
	private BlockPos findSite(WorldGenLevel world, int x, int z, RandomSource random, BlockState state) {
		int surface = world.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
		boolean ivy = flower.siting() == ChromaDecoFlowers.Siting.IVY;
		// V33a: getRandomPlusMinus(getTopSolidOrLiquidBlock, 25).
		int y = surface + random.nextInt(51) - 25;
		y = Math.min(Math.max(y, world.getMinY() + 1), world.getMaxY() - 1);
		if (!ivy) {
			while (y > world.getMinY() + 1 && world.getBlockState(new BlockPos(x, y - 1, z)).isAir())
				y--;
			if (y <= world.getMinY() + 1)
				return null;
		}
		while (y < world.getMaxY() - 1 && !world.getBlockState(new BlockPos(x, y, z)).isAir())
			y++;
		BlockPos pos = new BlockPos(x, y, z);
		if (!world.getBlockState(pos).isAir() || !state.canSurvive(world, pos))
			return null;
		if (ivy) {
			// Climb the face to its top, then insist on open air below so it hangs rather than sits.
			while (pos.getY() < world.getMaxY() - 1
					&& world.getBlockState(pos.above()).isAir()
					&& state.canSurvive(world, pos.above()))
				pos = pos.above();
			if (!world.getBlockState(pos.below()).isAir())
				return null;
		}
		return pos;
	}

	/** V33a's post-placement runs: reeds stack upward, ivy hangs downward. */
	private void extendRun(WorldGenLevel world, BlockPos start, RandomSource random, BlockState state) {
		int run = flower.runLength();
		if (run <= 1)
			return;
		boolean upward = flower.siting() == ChromaDecoFlowers.Siting.REED;
		int extra = random.nextInt(run);
		for (int i = 1; i < extra; i++) {
			BlockPos pos = upward ? start.above(i) : start.below(i);
			if (!world.getBlockState(pos).isAir() || !state.canSurvive(world, pos))
				return;
			world.setBlock(pos, state, 2);
		}
	}
}
