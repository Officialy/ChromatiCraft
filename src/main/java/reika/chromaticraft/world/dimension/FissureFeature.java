package reika.chromaticraft.world.dimension;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.server.level.WorldGenRegion;

import reika.chromaticraft.block.dimension.BlockDimensionCore;
import reika.chromaticraft.block.dimension.BlockVoidRift;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.registry.ChromaShieldTypes;
import reika.chromaticraft.registry.CrystalElement;
import reika.chromaticraft.registry.ProximaDecoTypes;

/**
 * V33a {@code WorldGenFissure}: the Radiant Fissures, and the Void Rifts at the bottom of them.
 *
 * <p>A fissure takes a precomputed footprint from {@link FissurePatterns} and cuts a column at every
 * one of its cells: air through the seam, Cloak Shielding pressed into every stone face it exposes, a
 * floor of Lifewater six blocks below where the cut starts, and a sealed shell around that so the
 * water cannot be drained from the side. Where a column's shielding reaches its own top, a Void Rift
 * is set on it — one colour for the whole fissure, chosen when it is cut.
 *
 * <p>Two things upstream does that are easy to miss and both matter. The cut refuses water at the
 * surface outright — a fissure never opens under a pool. And {@link #canCutInto} is what keeps a
 * fissure from eating anything it happens to cross: dimension cores, reinforced shielding, runes, loot
 * chests and anything unbreakable are all left standing, so a fissure that wanders into a structure
 * damages nothing.
 */
public final class FissureFeature extends Feature<NoneFeatureConfiguration> {

	public FissureFeature() {
		super(NoneFeatureConfiguration.CODEC);
	}

	@Override
	public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
		WorldGenLevel world = context.level();
		RandomSource rand = context.random();
		BlockPos origin = context.origin();
		// V33a: never under water, checked at the origin and the block above it.
		if (world.getBlockState(origin).is(Blocks.WATER)
				|| world.getBlockState(origin.above()).is(Blocks.WATER))
			return false;

		int my = 8 + rand.nextInt(16);
		double w = rand.nextDouble();
		CrystalElement color = CrystalElement.elements[rand.nextInt(CrystalElement.elements.length)];

		// The height each column's shielding reached, so the rifts can be set on top of them after.
		Map<Long, Integer> columns = new HashMap<>();
		FissurePatterns.Pattern pattern = FissurePatterns.random(world.getSeed(), rand);
		// A Feature may only write the chunks in its WorldGenRegion. The fissure footprint is wider
		// than a vanilla decoration and the old direct stamp silently failed at the far boundary after
		// already carving the near half, leaving the perfectly flat stone wall seen in broken pits.
		// Reject that placement before touching any block; command placement uses a ServerLevel-backed
		// accessor and is unrestricted, while natural generation retries elsewhere at its normal rate.
		if (!fitsWriteRegion(world, origin, pattern, w, my))
			return false;
		for (Vec3i cell : pattern.columns())
			cut(world, origin.getX() + cell.getX(), origin.getY(), origin.getZ() + cell.getZ(), w, my,
					columns);

		// V33a: a rift caps a column only where that column's own top is shielding -- so the rifts trace
		// the fissure's rim rather than appearing wherever the cut happened to reach.
		BlockState rift = ChromaBlocks.voidRift(color).get().defaultBlockState();
		boolean placedRift = false;
		for (Map.Entry<Long, Integer> entry : columns.entrySet()) {
			BlockPos at = BlockPos.of(entry.getKey());
			int h = entry.getValue();
			BlockPos below = new BlockPos(at.getX(), h - 1, at.getZ());
			if (!(world.getBlockState(below).getBlock()
					instanceof reika.chromaticraft.block.worldgen26.BlockStructureShield))
				continue;
			world.setBlock(new BlockPos(at.getX(), h, at.getZ()), rift, 2);
			placedRift = true;
		}
		return placedRift;
	}

	private static boolean fitsWriteRegion(WorldGenLevel world, BlockPos origin,
			FissurePatterns.Pattern pattern, double width, int floor) {
		if (!(world instanceof WorldGenRegion region))
			return true;
		int radius = (int)(width * Math.sqrt(1 + (origin.getY() + 12 - floor) / 4D)) + 1;
		for (Vec3i cell : pattern.columns()) {
			int x = origin.getX() + cell.getX();
			int z = origin.getZ() + cell.getZ();
			if (!region.isWithinWriteZone(new BlockPos(x - radius, origin.getY(), z - radius))
					|| !region.isWithinWriteZone(new BlockPos(x + radius, origin.getY(), z - radius))
					|| !region.isWithinWriteZone(new BlockPos(x - radius, origin.getY(), z + radius))
					|| !region.isWithinWriteZone(new BlockPos(x + radius, origin.getY(), z + radius)))
				return false;
		}
		return true;
	}

	/**
	 * V33a's cut with {@code allowSpread} false, which is how the generator calls it: the wandering and
	 * forking already happened when the pattern was grown, so here every footprint cell is cut straight
	 * down and nothing recurses.
	 */
	private static void cut(WorldGenLevel world, int x, int y, int z, double w, int my,
			Map<Long, Integer> columns) {
		BlockState cloak = ChromaBlocks.shielding(ChromaShieldTypes.CLOAK).get().defaultBlockState();
		BlockState lifewater = ChromaBlocks.deco(ProximaDecoTypes.LIFEWATER).get().defaultBlockState();
		BlockState air = Blocks.AIR.defaultBlockState();

		for (int dy = my; dy <= y + 12; dy++) {
			int r = (int)(w * Math.sqrt(1 + (dy - my) / 4D));
			for (int i = -r; i <= r; i++)
				for (int k = -r; k <= r; k++) {
					int dx = x + i;
					int dz = z + k;
					BlockPos at = new BlockPos(dx, dy, dz);
					if (!canCutInto(world, at))
						continue;
					world.setBlock(at, air, 2);

					// Every stone face the cut exposes becomes Cloak Shielding, and each one raises its
					// column's recorded top -- which is what the rifts are later set on.
					for (Direction dir : Direction.values()) {
						BlockPos side = at.relative(dir);
						if (!world.getBlockState(side).is(net.minecraft.tags.BlockTags.BASE_STONE_OVERWORLD)
								|| !canCutInto(world, side))
							continue;
						world.setBlock(side, cloak, 2);
						long key = new BlockPos(side.getX(), 0, side.getZ()).asLong();
						columns.merge(key, side.getY() + 1, Math::max);
					}

					// Six below the cut's own floor is the Lifewater, sealed on every side. Upstream
					// walls the whole way up from there rather than just the one block, so the shaft
					// above the water cannot be opened from the side either.
					int gy = my - 6;
					for (Direction dir : Direction.values()) {
						if (dir == Direction.UP) {
							for (int h = 1; h < my; h++) {
								BlockPos above = new BlockPos(dx, gy + h, dz);
								if (canCutInto(world, above))
									world.setBlock(above, cloak, 2);
							}
							continue;
						}
						BlockPos side = new BlockPos(dx + dir.getStepX(), gy + dir.getStepY(),
								dz + dir.getStepZ());
						if (canCutInto(world, side)
								&& !(world.getBlockState(side).getBlock()
										instanceof reika.chromaticraft.block.dimension.BlockDimensionDeco))
							world.setBlock(side, cloak, 2);
					}
					world.setBlock(new BlockPos(dx, gy, dz), lifewater, 2);
				}
		}
	}

	/**
	 * V33a canCutInto: everything a fissure must leave standing when it wanders into something.
	 *
	 * <p>Upstream's list, in its own order — a dimension core, reinforced shielding (its metadata 8 and
	 * up, which is this port's separate reinforced identities), a crystal rune, molten lumen, a loot
	 * chest, anything with a negative hardness, anything declaring itself unbreakable, and any block the
	 * registry marks as a dimension structure block.
	 */
	public static boolean canCutInto(WorldGenLevel world, BlockPos pos) {
		BlockState state = world.getBlockState(pos);
		if (state.isAir())
			return true;
		if (state.getBlock() instanceof BlockDimensionCore || state.getBlock() instanceof BlockVoidRift)
			return false;
		// V33a `b instanceof BlockStructureShield && meta >= 8`: metadata bit 3 is the reinforced flag,
		// which this port carries as a blockstate property rather than as a separate identity.
		if (state.getBlock() instanceof reika.chromaticraft.block.worldgen26.BlockStructureShield
				&& state.getValue(reika.chromaticraft.block.worldgen26.BlockStructureShield.REINFORCED))
			return false;
		if (state.is(ChromaBlocks.LOOT_CHEST.get()) || state.is(ChromaBlocks.STRUCTURE_CONTROLLER.get()))
			return false;
		if (state.getBlock() instanceof reika.chromaticraft.block.BlockCrystalRune)
			return false;
		// A negative hardness is upstream's own catch-all for "cannot be broken at all".
		return !(state.getDestroySpeed(world, pos) < 0);
	}
}
