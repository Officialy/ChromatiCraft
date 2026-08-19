package reika.chromaticraft.world.dimension;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.registry.ChromaShieldTypes;

/**
 * V33a {@code WorldGenFireJet}: the vents at the bottom of Proxima's pools.
 *
 * <p>Small and particular. It finds the topmost solid block in the column and places nothing at all
 * unless that block is water with at least two blocks of water above it — so a jet only ever appears
 * under a real pool, never in a puddle or on a shore. The jet then gets a ring of four Cloak Shielding
 * blocks around it, which is what stops a player mining out its surroundings and draining the pool.
 *
 * <p>Upstream searches down from y 255 for the first non-air block. The modern heightmap does the same
 * job, and {@code MOTION_BLOCKING} is the one that counts water as blocking — {@code WORLD_SURFACE}
 * would land on the surface of the pool rather than in it, and {@code OCEAN_FLOOR} would skip past the
 * water entirely to the bed beneath.
 */
public final class FireJetFeature extends Feature<NoneFeatureConfiguration> {

	/** V33a: at least this much water above the jet. */
	private static final int REQUIRED_DEPTH = 2;

	public FireJetFeature() {
		super(NoneFeatureConfiguration.CODEC);
	}

	@Override
	public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
		WorldGenLevel world = context.level();
		BlockPos origin = context.origin();
		// MOTION_BLOCKING counts water, so this lands on the pool's own surface column.
		int surface = world.getHeight(Heightmap.Types.MOTION_BLOCKING, origin.getX(), origin.getZ());
		BlockPos top = new BlockPos(origin.getX(), surface - 1, origin.getZ());
		if (!world.getBlockState(top).is(Blocks.WATER))
			return false;

		// Walk down to the bed: the jet sits at the bottom of the column, not at its surface.
		BlockPos at = top;
		int depth = 0;
		while (world.getBlockState(at).is(Blocks.WATER) && at.getY() > world.getMinY()) {
			depth++;
			at = at.below();
		}
		if (depth < REQUIRED_DEPTH)
			return false;
		// `at` is now the bed; the jet replaces the lowest water cell above it.
		BlockPos jet = at.above();

		world.setBlock(jet, ChromaBlocks.FIRE_JET.get().defaultBlockState(), 3);
		BlockState cloak = ChromaBlocks.shielding(ChromaShieldTypes.CLOAK).get().defaultBlockState();
		for (Direction dir : Direction.Plane.HORIZONTAL)
			world.setBlock(jet.relative(dir), cloak, 3);
		return true;
	}
}
