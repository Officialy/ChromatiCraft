package reika.chromaticraft.world.dimension;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.registry.ChromaShieldTypes;
import reika.chromaticraft.registry.CrystalElement;
import reika.dragonapi.libraries.mathsci.ReikaMathLibrary;

/**
 * V33a {@code WorldGenCrystalPit}: the crystal geodes hollowed out of Proxima's plains.
 *
 * <p>An ellipsoid seventeen blocks across and nine tall is carved out and lined with Cloak Shielding,
 * and a third of the lining cells below the halfway point sprout a cave crystal. The colours are not
 * arbitrary: upstream groups the sixteen elements into four palettes and a geode draws its crystals
 * from one of them, so a pit reads as blues and reds, or greens and yellows, rather than a jumble.
 *
 * <p>Nothing is written until the whole ellipsoid has been checked, and the check is the interesting
 * half. A geode refuses to form if any cell of it is liquid — it would flood — and every lining cell
 * must have solid rock, grass, ground or metal beneath it, so a pit cannot hang half out of a cliff
 * face or over a cave. Upstream tests all of that before touching a block.
 */
public final class CrystalPitFeature extends Feature<NoneFeatureConfiguration> {

	/** V33a's horizontal and vertical radii. */
	private static final int RADIUS = 8;
	private static final int HEIGHT = 4;

	/**
	 * V33a's four crystal palettes. A geode picks one and draws every crystal from it, which is what
	 * gives each pit a colour scheme instead of sixteen unrelated crystals.
	 */
	private static final CrystalElement[][] PALETTES = {
			{CrystalElement.BLUE, CrystalElement.RED, CrystalElement.PURPLE, CrystalElement.MAGENTA},
			{CrystalElement.GREEN, CrystalElement.YELLOW, CrystalElement.CYAN, CrystalElement.LIME},
			{CrystalElement.BROWN, CrystalElement.PINK, CrystalElement.ORANGE, CrystalElement.LIGHTBLUE},
			{CrystalElement.BLACK, CrystalElement.GRAY, CrystalElement.LIGHTGRAY, CrystalElement.WHITE},
	};

	public CrystalPitFeature() {
		super(NoneFeatureConfiguration.CODEC);
	}

	@Override
	public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
		WorldGenLevel world = context.level();
		RandomSource random = context.random();
		BlockPos origin = context.origin();
		if (!canFormAt(world, origin))
			return false;

		CrystalElement[] palette = PALETTES[random.nextInt(PALETTES.length)];
		BlockState cloak = ChromaBlocks.shielding(ChromaShieldTypes.CLOAK).get().defaultBlockState();
		for (int i = -RADIUS; i <= RADIUS; i++)
			for (int k = -RADIUS; k <= RADIUS; k++)
				for (int j = HEIGHT; j >= -HEIGHT; j--) {
					if (!inShell(i, j, k))
						continue;
					BlockPos pos = origin.offset(i, j, k);
					if (isHollow(i, j, k)) {
						world.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
						continue;
					}
					world.setBlock(pos, cloak, 3);
					// V33a seeds crystals only on the floor of the pit, a third of the time.
					if (j <= -2 && random.nextInt(3) == 0)
						world.setBlock(pos.above(), ChromaBlocks.caveCrystal(
								palette[random.nextInt(palette.length)]).get().defaultBlockState(), 3);
				}
		return true;
	}

	/** V33a {@code canGenAt}: no liquid anywhere, and solid ground under every lining cell. */
	private static boolean canFormAt(WorldGenLevel world, BlockPos origin) {
		for (int i = -RADIUS; i <= RADIUS; i++)
			for (int k = -RADIUS; k <= RADIUS; k++)
				for (int j = HEIGHT; j >= -HEIGHT; j--) {
					if (!inShell(i, j, k))
						continue;
					BlockPos pos = origin.offset(i, j, k);
					if (world.isOutsideBuildHeight(pos.getY()))
						return false;
					// A geode carved into water or lava would simply flood.
					if (!world.getFluidState(pos).isEmpty())
						return false;
					if (isHollow(i, j, k))
						continue;
					BlockState below = world.getBlockState(pos.below());
					if (below.isAir())
						return false;
					// V33a accepts rock, grass, ground and metal beneath the lining; anything else means
					// the pit is hanging out of a cliff or over a cave.
					if (!below.is(BlockTags.DIRT) && !below.is(BlockTags.BASE_STONE_OVERWORLD)
							&& !below.is(BlockTags.BASE_STONE_NETHER) && !below.is(BlockTags.SAND)
							&& !below.isSolidRender())
						return false;
				}
		return true;
	}

	/** Inside the outer ellipsoid at all. */
	private static boolean inShell(int i, int j, int k) {
		return ReikaMathLibrary.isPointInsideEllipse(i, j, k, RADIUS, HEIGHT, RADIUS);
	}

	/**
	 * V33a's hollow test: everything at or above the equator, and anything inside the ellipsoid one
	 * smaller below it — which is what leaves a lining only under the floor and around the lower walls.
	 */
	private static boolean isHollow(int i, int j, int k) {
		return j >= 0 || ReikaMathLibrary.isPointInsideEllipse(i, j, k, RADIUS - 1, HEIGHT - 1, RADIUS - 1);
	}
}
