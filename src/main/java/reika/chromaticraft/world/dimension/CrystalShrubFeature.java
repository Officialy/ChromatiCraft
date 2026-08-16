package reika.chromaticraft.world.dimension;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.registry.ChromaShieldTypes;
import reika.chromaticraft.registry.ProximaDecoTypes;

/**
 * V33a {@code WorldGenCrystalShrub}: the crystal bushes of Proxima's Crystal Forest.
 *
 * <p>Upstream rolls a size before doing anything: one in forty is the large form, otherwise one in
 * fifteen is the small form, and otherwise nothing grows at all — so most attempts produce nothing and
 * the forest stays sparse. Both forms are built from Crystal Leaves, and only the large one has a
 * trunk, a single Stone Shielding block at its base ({@code WorldGenCrystalTree.CRYSTAL_TRUNK}).
 *
 * <p>The two shapes are upstream's exactly. The large form is a five-by-five ring two blocks tall —
 * the {@code Math.abs(i) < 2 || Math.abs(k) < 2} test keeps its corners open — capped by three
 * diamond layers of radii 2, 2 and 1. The small form is those same three diamond layers starting at
 * ground level, which is why a small shrub reads as the crown of a large one with its body missing.
 *
 * <p>Every cell is placed only into air and the whole shape is five blocks across, so it cannot reach
 * outside the chunks this generation step may write to.
 */
public final class CrystalShrubFeature extends Feature<NoneFeatureConfiguration> {

	/** V33a's three crown layers: diamonds of radius 2, 2 then 1, tested as {@code |i|+|k| <= r+1}. */
	private static final int[] CROWN_RADII = {2, 2, 1};

	public CrystalShrubFeature() {
		super(NoneFeatureConfiguration.CODEC);
	}

	@Override
	public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
		WorldGenLevel world = context.level();
		RandomSource random = context.random();
		BlockPos origin = context.origin();
		// V33a requires grass directly beneath; a shrub does not grow on stone or sand.
		if (!world.getBlockState(origin.below()).is(Blocks.GRASS_BLOCK))
			return false;

		int size = 0;
		if (random.nextInt(40) == 0)
			size = 2;
		else if (random.nextInt(15) == 0)
			size = 1;
		if (size == 0)
			return false;

		BlockState leaves = ChromaBlocks.deco(ProximaDecoTypes.CRYSTALLEAF).get().defaultBlockState();
		if (size == 2) {
			// The body: a hollow-cornered five-by-five, two blocks tall.
			for (int y = 0; y < 2; y++)
				for (int i = -2; i <= 2; i++)
					for (int k = -2; k <= 2; k++)
						if (Math.abs(i) < 2 || Math.abs(k) < 2)
							placeLeaf(world, origin.offset(i, y, k), leaves);
			crown(world, origin.above(2), leaves);
			// V33a WorldGenCrystalTree.CRYSTAL_TRUNK: Stone Shielding, placed over the leaves.
			world.setBlock(origin, ChromaBlocks.shielding(ChromaShieldTypes.STONE).get()
					.defaultBlockState(), 3);
		}
		else {
			crown(world, origin, leaves);
		}
		return true;
	}

	/** The three diamond layers both forms share. */
	private static void crown(WorldGenLevel world, BlockPos base, BlockState leaves) {
		for (int y = 0; y < CROWN_RADII.length; y++)
			for (int i = -2; i <= 2; i++)
				for (int k = -2; k <= 2; k++)
					if (Math.abs(i) + Math.abs(k) <= CROWN_RADII[y] + 1)
						placeLeaf(world, base.offset(i, y, k), leaves);
	}

	/** V33a only ever writes into air, so a shrub never eats the terrain it grows out of. */
	private static void placeLeaf(WorldGenLevel world, BlockPos pos, BlockState leaves) {
		if (world.getBlockState(pos).isAir())
			world.setBlock(pos, leaves, 3);
	}
}
