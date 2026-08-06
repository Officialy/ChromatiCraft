package reika.chromaticraft.world;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.material.Fluids;

import reika.chromaticraft.data.ChromaBiomeTagProvider;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.tileentity.networking.TileEntitySkypeater;
import reika.chromaticraft.tileentity.networking.TileEntitySkypeater.NodeClass;
import reika.chromaticraft.world.luminous.LuminousCliffsTerrainFeature;
import reika.chromaticraft.world.luminous.LuminousCliffsTerrainFeature.GlowCliffRegion;

/**
 * V33a {@code SkypeaterGenerator}: Lumen Nodes hanging in the air over the Luminous Cliffs' water.
 *
 * <p>Placement is a shuffled sweep of sixteen fixed offsets within the chunk — a 4x4 lattice at
 * {@code (i*4+2, k*4+2)} — tried in random order until one succeeds, so a chunk yields at most one
 * node but samples its whole area rather than a single random column. Which node type appears depends
 * on the cliff region: over open water it is a {@link NodeClass#WATER} node sitting 16 blocks below
 * the middle shelf top, over the shores a {@link NodeClass#SHORE} node only 4 below.
 *
 * <p>A node needs open sky, air at its own position, and water directly above the terrain beneath it;
 * and V33a refuses to place one within 32 blocks of another, which is what stops them clustering.
 */
public final class SkypeaterFeature extends Feature<NoneFeatureConfiguration> {

	private static final int SEPARATION = 32;

	private final List<int[]> attemptOffsets = new ArrayList<>();

	public SkypeaterFeature() {
		super(NoneFeatureConfiguration.CODEC);
		for (int i = 0; i < 4; i++)
			for (int k = 0; k < 4; k++)
				attemptOffsets.add(new int[] {i * 4 + 2, k * 4 + 2});
	}

	@Override
	public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
		WorldGenLevel world = context.level();
		RandomSource random = context.random();
		BlockPos origin = context.origin();
		List<int[]> attempts = new ArrayList<>(attemptOffsets);
		while (!attempts.isEmpty()) {
			int[] offset = attempts.remove(random.nextInt(attempts.size()));
			int x = origin.getX() + offset[0];
			int z = origin.getZ() + offset[1];
			BlockPos column = new BlockPos(x, world.getSeaLevel(), z);
			if (!world.getBiome(column).is(ChromaBiomeTagProvider.LUMINOUS_CLIFFS))
				continue;
			GlowCliffRegion region = LuminousCliffsTerrainFeature.getRegion(world, x, z);
			int middleTop = LuminousCliffsTerrainFeature.getMiddleTop(world, x, z);
			NodeClass type;
			int y;
			if (region == GlowCliffRegion.WATER) {
				y = middleTop - 16 + random.nextInt(8);
				type = NodeClass.WATER;
			}
			else if (region == GlowCliffRegion.SHORES) {
				y = middleTop - 4 + random.nextInt(8);
				type = NodeClass.SHORE;
			}
			else {
				continue;
			}
			if (this.generateAt(world, new BlockPos(x, y, z), type))
				return true;
		}
		return false;
	}

	private boolean generateAt(WorldGenLevel world, BlockPos pos, NodeClass type) {
		if (!world.getBlockState(pos).isAir() || !world.canSeeSky(pos))
			return false;
		// V33a: there must be water directly above the terrain in this column.
		int surface = world.getHeight(Heightmap.Types.WORLD_SURFACE_WG, pos.getX(), pos.getZ());
		if (!world.getFluidState(new BlockPos(pos.getX(), surface, pos.getZ())).is(Fluids.WATER)
				&& !world.getFluidState(new BlockPos(pos.getX(), surface - 1, pos.getZ())).is(Fluids.WATER))
			return false;
		if (this.hasNodeNearby(world, pos))
			return false;
		world.setBlock(pos, ChromaBlocks.SKYPEATER.get().defaultBlockState(), 3);
		if (world.getBlockEntity(pos) instanceof TileEntitySkypeater node) {
			node.setNodeType(type);
			// CHROMA-PORT: V33a also calls CrystalNetworker.addTile here. The tile caches itself into
			// the network on its first server tick, so the eager registration is redundant during
			// worldgen and would need a live ServerLevel the feature does not have.
			return true;
		}
		return false;
	}

	/**
	 * V33a asks CrystalNetworker for the nearest Skypeater within 32 blocks. During worldgen the
	 * network is not populated for chunks that have not ticked, so this scans the world directly —
	 * the same question, asked of the only source that can answer it at this point.
	 */
	private boolean hasNodeNearby(WorldGenLevel world, BlockPos pos) {
		BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
		for (int dx = -SEPARATION; dx <= SEPARATION; dx += 4) {
			for (int dz = -SEPARATION; dz <= SEPARATION; dz += 4) {
				for (int dy = -SEPARATION; dy <= SEPARATION; dy += 4) {
					cursor.set(pos.getX() + dx, pos.getY() + dy, pos.getZ() + dz);
					if (world.getBlockState(cursor).is(ChromaBlocks.SKYPEATER.get()))
						return true;
				}
			}
		}
		return false;
	}
}
