package reika.chromaticraft.world.luminous;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import reika.chromaticraft.block.worldgen26.BlockCliffStone;

/**
 * V33a {@code GlowingCliffsDecorator.NoFloatingGravelVein}: the Luminous Cliffs' gravel.
 *
 * <p>Upstream swapped the decorator's gravel generator for a DragonAPI {@code ControllableOreVein} of 32
 * blocks that refuses two kinds of cell: cliff stone, and any cell without a solid block under it. That
 * last rule is the point — ordinary gravel veins in a landscape of overhangs and floating shelves would
 * leave gravel hanging in cave roofs and under the upper plateau, ready to fall. The vein itself is
 * Minecraft 1.7.10's {@code WorldGenMinable} blob, centred on the placement position (the source's
 * eight-block shift only pushed veins across the 1.7.10 chunk seam), and like it only replaces stone.
 *
 * <p>The same generator served both of upstream's uses: ten veins a chunk anywhere from y 0 to 255 (in
 * place of the ordinary gravel ore), and the two "sand" patches a chunk laid on the top solid block,
 * because the decorator's second sand loop used the gravel generator.
 */
public final class NoFloatingGravelFeature extends Feature<NoneFeatureConfiguration> {

	/** V33a {@code new NoFloatingGravelVein(32)}. */
	private static final int VEIN_SIZE = 32;

	public NoFloatingGravelFeature() {
		super(NoneFeatureConfiguration.CODEC);
	}

	/** DragonAPI {@code ControllableOreVein.generate}. */
	@Override
	public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
		WorldGenLevel world = context.level();
		RandomSource rand = context.random();
		int x = context.origin().getX() - 8;
		int y = context.origin().getY();
		int z = context.origin().getZ() - 8;
		BlockState gravel = Blocks.GRAVEL.defaultBlockState();

		float f = rand.nextFloat() * (float)Math.PI;
		double xPos = x + 8 + Mth.sin(f) * VEIN_SIZE / 8F;
		double xNeg = x + 8 - Mth.sin(f) * VEIN_SIZE / 8F;
		double zPos = z + 8 + Mth.cos(f) * VEIN_SIZE / 8F;
		double zNeg = z + 8 - Mth.cos(f) * VEIN_SIZE / 8F;
		double y1 = y + rand.nextInt(3) - 2;
		double y2 = y + rand.nextInt(3) - 2;
		boolean placed = false;

		for (int l = 0; l <= VEIN_SIZE; l++) {
			double px = xPos + (xNeg - xPos) * l / VEIN_SIZE;
			double py = y1 + (y2 - y1) * l / VEIN_SIZE;
			double pz = zPos + (zNeg - zPos) * l / VEIN_SIZE;
			double range = rand.nextDouble() * VEIN_SIZE / 16D;
			double rh = (Mth.sin(l * (float)Math.PI / VEIN_SIZE) + 1F) * range + 1D;
			double rv = (Mth.sin(l * (float)Math.PI / VEIN_SIZE) + 1F) * range + 1D;
			int minX = Mth.floor(px - rh / 2D);
			int minY = Mth.floor(py - rv / 2D);
			int minZ = Mth.floor(pz - rh / 2D);
			int maxX = Mth.floor(px + rh / 2D);
			int maxY = Mth.floor(py + rv / 2D);
			int maxZ = Mth.floor(pz + rh / 2D);

			for (int dx = minX; dx <= maxX; dx++) {
				double rx = (dx + 0.5D - px) / (rh / 2D);
				if (rx * rx >= 1D)
					continue;
				for (int dy = minY; dy <= maxY; dy++) {
					double ry = (dy + 0.5D - py) / (rv / 2D);
					if (rx * rx + ry * ry >= 1D)
						continue;
					for (int dz = minZ; dz <= maxZ; dz++) {
						double rz = (dz + 0.5D - pz) / (rh / 2D);
						if (rx * rx + ry * ry + rz * rz >= 1D)
							continue;
						BlockPos pos = new BlockPos(dx, dy, dz);
						if (world.isOutsideBuildHeight(dy) || !world.getBlockState(pos).is(Blocks.STONE))
							continue;
						if (this.canPlaceBlockHere(world, pos)) {
							world.setBlock(pos, gravel, 2);
							placed = true;
						}
					}
				}
			}
		}
		return placed;
	}

	/** V33a {@code NoFloatingGravelVein.canPlaceBlockHere}. */
	private boolean canPlaceBlockHere(WorldGenLevel world, BlockPos pos) {
		if (world.getBlockState(pos).getBlock() instanceof BlockCliffStone)
			return false;
		return world.getBlockState(pos.below()).isSolid();
	}
}
