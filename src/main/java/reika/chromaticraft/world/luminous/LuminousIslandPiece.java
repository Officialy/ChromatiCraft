package reika.chromaticraft.world.luminous;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.features.TreeFeatures;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.material.Fluids;
import reika.chromaticraft.block.worldgen26.BlockCliffStone;
import reika.chromaticraft.data.ChromaWorldGenProvider;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.world.PostPopulationWorldgen;
import reika.chromaticraft.world.dimension.structure.ProximaStructurePieces;
import reika.chromaticraft.world.luminous.LuminousIsland.Kind;
import reika.chromaticraft.world.luminous.LuminousIsland.Shape;

import java.util.*;

/**
 * One Luminous Cliffs floating island, painted after its complete footprint is populated.
 *
 * <p>The server checks every authored cell for air before painting the complete body (V33a
 * {@code placeBlocks}), decoration (V33a {@code genIslandDecorations}) and ore veins. New candidates
 * consume ore draws from the shared generation stream before their parent continues calculating;
 * previously saved pieces retain their serialized veins.
 *
 * <p>Decoration is upstream's, with two things to know. Upstream seeded it with the world seed for every
 * island, so all islands draw the same decoration sequence; the anchors here are drawn from that stream in
 * upstream's order, and each attempt's own rolls from a seed derived from it, so that every chunk agrees
 * on where each attempt is. And the tree, flower, grass, reed and spring loops pass the loop counter
 * where the x coordinate belongs when they ask for a height — {@code is.getTopY(world, j, k)} — which
 * reads some far-off column and, in practice, yields the island's base height plus one. That value is
 * used directly: the trees, flowers and cane grow only where the island's top sits at its base height,
 * its flat low ground and lake shores, as they did upstream.
 *
 * <p>The ore veins are {@code WorldGenMinable} into vanilla stone, as upstream wrote them. The island is
 * cliff stone, so they almost never find anything to replace.
 */
public final class LuminousIslandPiece extends StructurePiece {

	private record Ore(int type, int x, int y, int z, int size, long seed) {}

	/** V33a {@code oreRand}: diamond 20, emerald 10, redstone 40, gold 30. */
	private static final int[] ORE_WEIGHTS = {20, 10, 40, 30};

	private static final Map<Long, Shape> SHAPES = new LinkedHashMap<>(16, 0.75F, true) {
		@Override
		protected boolean removeEldestEntry(Map.Entry<Long, Shape> eldest) {
			return this.size() > 16;
		}
	};

	private final LuminousIsland island;
	private final List<Ore> ores;

	LuminousIslandPiece(LuminousIsland island, List<Ore> ores) {
		super(ProximaStructurePieces.LUMINOUS_ISLAND.get(), 0, boundsFor(island));
		this.island = island;
		this.ores = ores;
	}

	LuminousIslandPiece(LuminousIsland island) {
		this(island, List.of());
	}

	public LuminousIslandPiece(CompoundTag tag) {
		super(ProximaStructurePieces.LUMINOUS_ISLAND.get(), tag);
		this.island = LuminousIsland.load(tag.getCompoundOrEmpty("Island"));
		this.ores = new ArrayList<>();
		for (Tag t : tag.getListOrEmpty("Ores")) {
			if (t instanceof CompoundTag o)
				ores.add(new Ore(o.getIntOr("T", 0), o.getIntOr("X", 0), o.getIntOr("Y", 0), o.getIntOr("Z", 0),
						o.getIntOr("S", 12), o.getLongOr("Seed", 0)));
		}
	}

	private static BoundingBox boundsFor(LuminousIsland is) {
		return new BoundingBox(is.minX(), is.minY(), is.minZ(), is.maxX(), is.maxY() + 1, is.maxZ());
	}

	/** V33a {@code generateIsland}'s ore loop, drawn at start time from the island's random. */
	static List<Ore> rollOres(LuminousIsland is, Random rand) {
		List<Ore> list = new ArrayList<>();
		int n = 2 + rand.nextInt(7);
		for (int i = 0; i < n; i++) {
			int roll = rand.nextInt(100);
			int type = 0;
			while (roll >= ORE_WEIGHTS[type]) {
				roll -= ORE_WEIGHTS[type];
				type++;
			}
			int dx = is.getRandomX(rand);
			int dy = is.getRandomY(rand);
			int dz = is.getRandomZ(rand);
			int size = 12 + rand.nextInt(20);
			list.add(new Ore(type, dx, dy, dz, size, rand.nextLong()));
		}
		return list;
	}

	@Override
	protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
		CompoundTag is = new CompoundTag();
		island.save(is);
		tag.put("Island", is);
		ListTag list = new ListTag();
		for (Ore ore : ores) {
			CompoundTag o = new CompoundTag();
			o.putInt("T", ore.type());
			o.putInt("X", ore.x());
			o.putInt("Y", ore.y());
			o.putInt("Z", ore.z());
			o.putInt("S", ore.size());
			o.putLong("Seed", ore.seed());
			list.add(o);
		}
		tag.put("Ores", list);
	}

	private Shape shape(long worldSeed) {
		long key = island.key() ^ worldSeed;
		synchronized (SHAPES) {
			Shape shape = SHAPES.get(key);
			if (shape != null)
				return shape;
		}
		Shape shape = island.shape(worldSeed);
		synchronized (SHAPES) {
			SHAPES.put(key, shape);
		}
		return shape;
	}

	@Override
	public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator,
			RandomSource unusedRandom, BoundingBox chunkBB, ChunkPos chunkPos, BlockPos referencePos) {
		// Enqueue once from the island's centre. All parts are placed together after vegetation,
		// so a collision in a distant chunk rejects the whole island before any cells are written.
		ChunkPos origin = new ChunkPos(island.originX >> 4, island.originZ >> 4);
		if (!chunkPos.equals(origin)) return;
		int reach = Math.max(Math.max(island.originX - boundingBox.minX(), boundingBox.maxX() - island.originX),
				Math.max(island.originZ - boundingBox.minZ(), boundingBox.maxZ() - island.originZ));
		PostPopulationWorldgen.get(level.getLevel()).enqueueIsland(origin, island.key(),
				(reach + 15) / 16 + 2, this.createTag(StructurePieceSerializationContext.fromLevel(level.getLevel())));
	}

	/** Atomic V33a air check against populated terrain, trees, fluids and previously placed islands. */
	public boolean placeAfterPopulation(net.minecraft.server.level.ServerLevel level) {
		if (!this.placeBodyAndDecoration(level)) return false;
		for (Ore ore : ores)
			this.generateOre(level, null, ore, new Random(ore.seed()));
		return true;
	}

	/** New candidates use the shared source stream through every vein before the parent continues. */
	boolean placeAfterPopulation(net.minecraft.server.level.ServerLevel level, Random random) {
		if (!this.placeBodyAndDecoration(level)) return false;
		int count = 2 + random.nextInt(7);
		for (int i = 0; i < count; i++) {
			int roll = random.nextInt(100), type = 0;
			while (roll >= ORE_WEIGHTS[type]) {
				roll -= ORE_WEIGHTS[type];
				type++;
			}
			Ore ore = new Ore(type, island.getRandomX(random), island.getRandomY(random), island.getRandomZ(random),
					12 + random.nextInt(20), 0);
			this.generateOre(level, null, ore, random);
		}
		return true;
	}

	private boolean placeBodyAndDecoration(net.minecraft.server.level.ServerLevel level) {
		Shape shape = this.shape(level.getSeed());
		if (!shape.canGenerate(new LuminousIsland.Terrain() {
			public int topSolidOrLiquid(int x, int z) {
				return level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.OCEAN_FLOOR, x, z);
			}
			public boolean isSolid(int x, int y, int z) {
				return !level.getBlockState(new BlockPos(x, y, z)).isAir();
			}
		})) return false;
		BoundingBox whole = this.getBoundingBox();
		this.placeBlocks(level, whole, shape);
		this.decorate(level, level.getChunkSource().getGenerator(), whole, shape);
		return true;
	}

	private BlockState stateFor(Kind kind) {
		return switch (kind) {
			case STONE -> ChromaBlocks.CLIFF_STONE.get().defaultBlockState().setValue(BlockCliffStone.TRANSPARENT, true);
			case DIRT -> Blocks.DIRT.defaultBlockState();
			case GRASS -> Blocks.GRASS_BLOCK.defaultBlockState();
			case LIQUID -> island.luma ? ChromaBlocks.LUMA.get().defaultBlockState() : Blocks.WATER.defaultBlockState();
			case NONE -> Blocks.AIR.defaultBlockState();
		};
	}

	/**
	 * V33a {@code placeBlocks}. Upstream placed flowing water and poked it with a block update so the
	 * rivers pour off the rims; here the fluid is given its tick.
	 */
	private void placeBlocks(WorldGenLevel level, BoundingBox box, Shape shape) {
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (Map.Entry<Long, LuminousIsland.Column> e : shape.columns().entrySet()) {
			int x = BlockPos.getX(e.getKey());
			int z = BlockPos.getZ(e.getKey());
			if (x < box.minX() || x > box.maxX() || z < box.minZ() || z > box.maxZ())
				continue;
			LuminousIsland.Column col = e.getValue();
			for (int i = 0; i < col.height(); i++) {
				int y = col.minY() + i;
				if (col.get(y) == Kind.NONE || y < box.minY() || y > box.maxY())
					continue;
				Kind kind = shape.resolve(x, y, z, !island.luma);
				pos.set(x, y, z);
				level.setBlock(pos, this.stateFor(kind), 2);
				if (kind == Kind.LIQUID) {
					var fluid = island.luma ? ChromaBlocks.LUMA.get().defaultBlockState().getFluidState().getType() : Fluids.WATER;
					level.scheduleTick(pos.immutable(), fluid, fluid.getTickDelay(level));
				}
			}
		}
	}

	private enum Deco { SAND_DISK, CLAY_DISK, GRAVEL_DISK, TREE, FLOWER, GRASS, REED, SPRING_WATER, SPRING_LAVA }

	private record Attempt(Deco deco, int x, int y, int z, long seed) {}

	/** V33a {@code genIslandDecorations}, anchors in upstream's order from its world-seeded random. */
	private List<Attempt> attempts(long worldSeed, Shape shape) {
		Random ir = new Random(worldSeed);
		List<Attempt> list = new ArrayList<>();
		int base = island.originY + 1;
		for (int i = 0; i < 6; i++)
			this.add(list, Deco.SAND_DISK, ir, shape, false, base);
		this.add(list, Deco.CLAY_DISK, ir, shape, false, base);
		for (int i = 0; i < 2; i++)
			this.add(list, Deco.GRAVEL_DISK, ir, shape, false, base);
		int trees = 4;
		if (ir.nextInt(10) == 0)
			trees++;
		for (int i = 0; i < trees; i++)
			this.add(list, Deco.TREE, ir, shape, true, base);
		for (int i = 0; i < 8; i++)
			this.add(list, Deco.FLOWER, ir, shape, true, base);
		for (int i = 0; i < 3; i++)
			this.add(list, Deco.GRASS, ir, shape, true, base);
		for (int i = 0; i < 10 + 10; i++)
			this.add(list, Deco.REED, ir, shape, true, base);
		for (int a = 0; a < 2; a++) {
			for (int i = 0; i < 50; i++)
				this.add(list, Deco.SPRING_WATER, ir, shape, true, base);
			for (int i = 0; i < 20; i++)
				this.add(list, Deco.SPRING_LAVA, ir, shape, true, base);
		}
		return list;
	}

	private void add(List<Attempt> list, Deco deco, Random ir, Shape shape, boolean baseHeight, int base) {
		int x = island.getRandomX(ir);
		int z = island.getRandomZ(ir);
		int y = baseHeight ? base : this.getTopY(shape, x, z);
		list.add(new Attempt(deco, x, y, z, ir.nextLong() ^ list.size()));
	}

	/**
	 * V33a {@code Island.getTopY}: one above the higher of the island's base height and the column's top
	 * solid block. Upstream read that block from the world; the island's own cells answer it here, and a
	 * column the island does not cover answers the base height.
	 */
	private int getTopY(Shape shape, int x, int z) {
		LuminousIsland.Column col = shape.columns().get(BlockPos.asLong(x, 0, z));
		int top = Integer.MIN_VALUE;
		if (col != null) {
			for (int i = col.height() - 1; i >= 0; i--) {
				int y = col.minY() + i;
				Kind kind = col.get(y) == Kind.NONE ? Kind.NONE : shape.resolve(x, y, z, !island.luma);
				if (kind != Kind.NONE && kind != Kind.LIQUID) {
					top = y + 1;
					break;
				}
			}
		}
		return 1 + Math.max(island.originY, top);
	}

	private void decorate(WorldGenLevel level, ChunkGenerator generator, BoundingBox box, Shape shape) {
		for (Attempt a : this.attempts(level.getSeed(), shape)) {
			if (a.x() < box.minX() || a.x() > box.maxX() || a.z() < box.minZ() || a.z() > box.maxZ())
				continue;
			Random rand = new Random(a.seed());
			BlockPos pos = new BlockPos(a.x(), a.y(), a.z());
			switch (a.deco()) {
				case SAND_DISK -> this.disk(level, rand, pos, 7, 2, Blocks.SAND.defaultBlockState(), false);
				case CLAY_DISK -> this.disk(level, rand, pos, 4, 1, Blocks.CLAY.defaultBlockState(), true);
				case GRAVEL_DISK -> this.disk(level, rand, pos, 6, 2, Blocks.GRAVEL.defaultBlockState(), false);
				case TREE -> this.tree(level, generator, rand, pos);
				case FLOWER -> this.flowers(level, rand, pos);
				case GRASS -> this.grass(level, rand, pos);
				case REED -> this.reeds(level, rand, pos);
				case SPRING_WATER -> this.spring(level, pos, Blocks.WATER.defaultBlockState(), Fluids.WATER);
				case SPRING_LAVA -> this.spring(level, pos, Blocks.LAVA.defaultBlockState(), Fluids.LAVA);
			}
		}
	}

	/**
	 * Minecraft 1.7.10 {@code WorldGenSand} ({@code clay} false) and {@code WorldGenClay} (true): only
	 * under water, a disk of radius 2 to {@code radius-1} replacing dirt or grass (dirt or clay for clay)
	 * within {@code depth} blocks up and down.
	 */
	private void disk(WorldGenLevel level, Random rand, BlockPos pos, int radius, int depth, BlockState block, boolean clay) {
		if (!level.getFluidState(pos).is(Fluids.WATER))
			return;
		int r = rand.nextInt(radius - 2) + 2;
		for (int i = pos.getX() - r; i <= pos.getX() + r; i++) {
			for (int k = pos.getZ() - r; k <= pos.getZ() + r; k++) {
				int dx = i - pos.getX();
				int dz = k - pos.getZ();
				if (dx * dx + dz * dz > r * r)
					continue;
				for (int j = pos.getY() - depth; j <= pos.getY() + depth; j++) {
					BlockPos at = new BlockPos(i, j, k);
					BlockState b = level.getBlockState(at);
					if (b.is(Blocks.DIRT) || (clay ? b.is(Blocks.CLAY) : b.is(Blocks.GRASS_BLOCK)))
						level.setBlock(at, block, 2);
				}
			}
		}
	}

	/** V33a {@code BiomeGlowingCliffs.func_150567_a}: a big glowing tree 1/20, a big oak 1/10, an oak. */
	private void tree(WorldGenLevel level, ChunkGenerator generator, Random rand, BlockPos pos) {
		ResourceKey<Feature> key = rand.nextInt(20) == 0 ? ChromaWorldGenProvider.GLOWING_TREE
				: rand.nextInt(10) == 0 ? TreeFeatures.FANCY_OAK : TreeFeatures.OAK;
		Feature tree = level.registryAccess().lookupOrThrow(Registries.FEATURE).getValue(key);
		if (tree != null)
			tree.place(level, generator, RandomSource.create(rand.nextLong()), pos);
	}

	/** Minecraft 1.7.10 {@code WorldGenFlowers} with {@code func_150572_a}'s dandelion-or-poppy. */
	private void flowers(WorldGenLevel level, Random rand, BlockPos pos) {
		BlockState flower = rand.nextInt(3) > 0 ? Blocks.DANDELION.defaultBlockState() : Blocks.POPPY.defaultBlockState();
		for (int l = 0; l < 64; l++) {
			BlockPos at = pos.offset(rand.nextInt(8) - rand.nextInt(8), rand.nextInt(4) - rand.nextInt(4), rand.nextInt(8) - rand.nextInt(8));
			if (level.getBlockState(at).isAir() && flower.canSurvive(level, at))
				level.setBlock(at, flower, 2);
		}
	}

	/** Minecraft 1.7.10 {@code WorldGenTallGrass}: sink through air and leaves, then 128 tries. */
	private void grass(WorldGenLevel level, Random rand, BlockPos pos) {
		BlockPos.MutableBlockPos at = pos.mutable();
		while (at.getY() > level.getMinY()) {
			BlockState b = level.getBlockState(at);
			if (!b.isAir() && !b.is(BlockTags.LEAVES))
				break;
			at.move(Direction.DOWN);
		}
		BlockState grass = Blocks.SHORT_GRASS.defaultBlockState();
		for (int l = 0; l < 128; l++) {
			BlockPos p = at.offset(rand.nextInt(8) - rand.nextInt(8), rand.nextInt(4) - rand.nextInt(4), rand.nextInt(8) - rand.nextInt(8));
			if (level.getBlockState(p).isAir() && grass.canSurvive(level, p))
				level.setBlock(p, grass, 2);
		}
	}

	/** Minecraft 1.7.10 {@code WorldGenReed}. */
	private void reeds(WorldGenLevel level, Random rand, BlockPos pos) {
		BlockState cane = Blocks.SUGAR_CANE.defaultBlockState();
		for (int l = 0; l < 20; l++) {
			BlockPos at = pos.offset(rand.nextInt(4) - rand.nextInt(4), 0, rand.nextInt(4) - rand.nextInt(4));
			if (!level.getBlockState(at).isAir())
				continue;
			BlockPos below = at.below();
			boolean water = false;
			for (Direction dir : Direction.Plane.HORIZONTAL)
				water |= level.getFluidState(below.relative(dir)).is(Fluids.WATER);
			if (!water)
				continue;
			int h = 2 + rand.nextInt(rand.nextInt(3) + 1);
			for (int i = 0; i < h; i++) {
				BlockPos p = at.above(i);
				if (cane.canSurvive(level, p))
					level.setBlock(p, cane, 2);
			}
		}
	}

	/**
	 * Minecraft 1.7.10 {@code WorldGenLiquids}: vanilla stone above and below, the spot air or stone, and
	 * exactly three stone sides and one open. The island is cliff stone, so this almost never fires.
	 */
	private void spring(WorldGenLevel level, BlockPos pos, BlockState liquid, net.minecraft.world.level.material.Fluid fluid) {
		if (!level.getBlockState(pos.above()).is(Blocks.STONE) || !level.getBlockState(pos.below()).is(Blocks.STONE))
			return;
		BlockState at = level.getBlockState(pos);
		if (!at.isAir() && !at.is(Blocks.STONE))
			return;
		int stone = 0;
		int air = 0;
		for (Direction dir : Direction.Plane.HORIZONTAL) {
			BlockState b = level.getBlockState(pos.relative(dir));
			if (b.is(Blocks.STONE))
				stone++;
			else if (b.isAir())
				air++;
		}
		if (stone == 3 && air == 1) {
			level.setBlock(pos, liquid, 2);
			level.scheduleTick(pos, fluid, fluid.getTickDelay(level));
		}
	}

	/** Minecraft 1.7.10 {@code WorldGenMinable} into vanilla stone, clipped to this chunk. */
	private void generateOre(WorldGenLevel level, BoundingBox box, Ore ore, Random rand) {
		BlockState block = switch (ore.type()) {
			case 0 -> Blocks.DIAMOND_ORE.defaultBlockState();
			case 1 -> Blocks.EMERALD_ORE.defaultBlockState();
			case 2 -> Blocks.REDSTONE_ORE.defaultBlockState();
			default -> Blocks.GOLD_ORE.defaultBlockState();
		};
		int size = ore.size();
		float f = rand.nextFloat() * (float)Math.PI;
		double x1 = ore.x() + 8 + Mth.sin(f) * size / 8F;
		double x2 = ore.x() + 8 - Mth.sin(f) * size / 8F;
		double z1 = ore.z() + 8 + Mth.cos(f) * size / 8F;
		double z2 = ore.z() + 8 - Mth.cos(f) * size / 8F;
		double y1 = ore.y() + rand.nextInt(3) - 2;
		double y2 = ore.y() + rand.nextInt(3) - 2;
		for (int l = 0; l <= size; l++) {
			double px = x1 + (x2 - x1) * l / size;
			double py = y1 + (y2 - y1) * l / size;
			double pz = z1 + (z2 - z1) * l / size;
			double range = rand.nextDouble() * size / 16D;
			double rh = (Mth.sin(l * (float)Math.PI / size) + 1F) * range + 1D;
			double rv = (Mth.sin(l * (float)Math.PI / size) + 1F) * range + 1D;
			for (int dx = Mth.floor(px - rh / 2D); dx <= Mth.floor(px + rh / 2D); dx++) {
				double rx = (dx + 0.5D - px) / (rh / 2D);
				if (rx * rx >= 1D)
					continue;
				for (int dy = Mth.floor(py - rv / 2D); dy <= Mth.floor(py + rv / 2D); dy++) {
					double ry = (dy + 0.5D - py) / (rv / 2D);
					if (rx * rx + ry * ry >= 1D)
						continue;
					for (int dz = Mth.floor(pz - rh / 2D); dz <= Mth.floor(pz + rh / 2D); dz++) {
						double rz = (dz + 0.5D - pz) / (rh / 2D);
						if (rx * rx + ry * ry + rz * rz >= 1D)
							continue;
						BlockPos p = new BlockPos(dx, dy, dz);
						if ((box == null || box.isInside(p)) && level.getBlockState(p).is(Blocks.STONE))
							level.setBlock(p, block, 2);
					}
				}
			}
		}
	}
}
