package reika.chromaticraft.world;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.material.Fluids;

import reika.chromaticraft.magic.lore.Towers;
import reika.chromaticraft.registry.ChromaBlocks;

/**
 * V33a {@code UnknownArtefactGenerator}: artefacts buried in a ring around each lore tower.
 *
 * <p>They are not scattered. Every tower projects an annulus between 384 and 512 blocks, and the
 * generator walks that annulus at 2-degree steps and 8-block radial steps, collecting the chunks it
 * crosses; only those chunks can ever hold an artefact, and even then only one attempt in forty
 * succeeds. The effect is a broken ring of relics at a consistent distance from each tower, which is
 * the game's way of telling you something is nearby before you can see it.
 *
 * <p>The chunk set depends only on the tower layout, so it is cached per world seed and rebuilt when
 * that changes — as upstream does.
 */
public final class UnknownArtefactFeature extends Feature<NoneFeatureConfiguration> {

	private static final int INNER_RADIUS = 384;
	private static final int OUTER_RADIUS = 512;

	private final Set<ChunkPos> artefactChunks = new HashSet<>();
	private long cachedSeed;
	private boolean cached;

	public UnknownArtefactFeature() {
		super(NoneFeatureConfiguration.CODEC);
	}

	@Override
	public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
		WorldGenLevel world = context.level();
		RandomSource random = context.random();
		BlockPos origin = context.origin();
		if (!(world.getLevel() instanceof ServerLevel server))
			return false;
		this.calculate(server);
		// V33a keys the set on the chunk's block-aligned corner, not its chunk index.
		if (!artefactChunks.contains(new ChunkPos(origin.getX() & ~15, origin.getZ() & ~15)))
			return false;
		if (random.nextInt(40) > 0)
			return false;

		int x = origin.getX() + random.nextInt(16);
		int z = origin.getZ() + random.nextInt(16);
		int y = world.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
		BlockPos surface = new BlockPos(x, y, z);
		if (!canGenerateArtefactAt(world, surface))
			return false;
		// V33a places the artefact one below the surface block, so it sits half-buried.
		world.setBlock(surface.below(), ChromaBlocks.UNKNOWN_ARTEFACT.get().defaultBlockState(), 2);
		return true;
	}

	private void calculate(ServerLevel world) {
		if (cached && cachedSeed == world.getSeed())
			return;
		if (!Towers.initialized(world))
			Towers.loadPositions(world, 64 * 16 * 2);
		artefactChunks.clear();
		for (Towers tower : Towers.towerList) {
			ChunkPos root = tower.getRootPosition();
			if (root == null)
				continue;
			// getRootPosition is in block coords despite the type; V33a offsets by 8 to the centre.
			int cx = root.x() + 8;
			int cz = root.z() + 8;
			for (double a = 0; a < 360; a += 2) {
				double cos = Math.cos(Math.toRadians(a));
				double sin = Math.sin(Math.toRadians(a));
				for (int r = INNER_RADIUS; r <= OUTER_RADIUS; r += 8) {
					int dx = Mth.floor(cx + r * cos);
					int dz = Mth.floor(cz + r * sin);
					artefactChunks.add(new ChunkPos((dx >> 4) << 4, (dz >> 4) << 4));
				}
			}
		}
		cachedSeed = world.getSeed();
		cached = true;
	}

	/**
	 * V33a canGenerateArtefactAt, where the artefact itself ends up at {@code pos.below()}: the
	 * surface must be real terrain, what is above it must be passable, what is below must be filler,
	 * and nothing within three blocks in any direction but up may be air. That last rule is what keeps
	 * artefacts out of cave roofs and cliff faces.
	 */
	public static boolean canGenerateArtefactAt(WorldGenLevel world, BlockPos pos) {
		BlockState surface = world.getBlockState(pos);
		if (!surface.is(Blocks.GRASS_BLOCK) && !surface.is(Blocks.DIRT) && !surface.is(Blocks.SAND)
				&& !surface.is(Blocks.STONE) && !surface.is(Blocks.GRAVEL) && !surface.is(Blocks.SNOW_BLOCK))
			return false;
		if (!isValidSurfaceBlock(world, pos.above()))
			return false;
		BlockState below = world.getBlockState(pos.below());
		if (!below.is(Blocks.DIRT) && !below.is(Blocks.SAND) && !below.is(Blocks.STONE)
				&& !below.is(Blocks.GRAVEL))
			return false;
		for (Direction dir : Direction.values()) {
			if (dir == Direction.UP)
				continue;
			for (int d = 1; d <= 3; d++)
				if (world.getBlockState(pos.relative(dir, d)).isAir())
					return false;
		}
		return true;
	}

	/** V33a isValidSurfaceBlock: air, snow, a flower, or anything else soft may cover it. */
	private static boolean isValidSurfaceBlock(WorldGenLevel world, BlockPos pos) {
		BlockState state = world.getBlockState(pos);
		if (state.isAir() || state.is(Blocks.SNOW))
			return true;
		if (state.canBeReplaced() && state.getFluidState().isEmpty())
			return true;
		// V33a additionally allows ocean water directly above; the artefact is then a seabed relic.
		return state.getFluidState().isSourceOfType(Fluids.WATER);
	}
}
