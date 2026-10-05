package reika.chromaticraft.world.dimension.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;

import reika.chromaticraft.block.BlockChromaDoor;
import reika.chromaticraft.block.BlockHoverBlock;
import reika.chromaticraft.block.dimension.structure.water.BlockEverFluid26;
import reika.chromaticraft.block.dimension.structure.water.BlockRotatingLock26;
import reika.chromaticraft.block.worldgen26.BlockLootChest;
import reika.chromaticraft.block.worldgen26.BlockStructureShield;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.registry.ChromaShieldTypes;
import reika.chromaticraft.registry.CrystalElement;
import reika.chromaticraft.tileentity.TileEntityEverFluid;
import reika.chromaticraft.tileentity.TileEntityLootChest;
import reika.chromaticraft.tileentity.TileEntityRotatingLock;
import reika.chromaticraft.tileentity.TileEntityStructurePassword;
import reika.chromaticraft.tileentity.technical.TileEntityDimensionCore;
import reika.chromaticraft.world.dimension.DimensionStructureType;
import reika.chromaticraft.world.dimension.ProximaGenerators;
import reika.chromaticraft.world.dimension.StructureCalculator;
import reika.chromaticraft.world.dimension.structure.StructureGeneratorBase.StructurePair;
import reika.chromaticraft.world.dimension.structure.WaterPuzzleLayout.Floor;
import reika.chromaticraft.world.dimension.structure.WaterPuzzleLayout.Lock;

/**
 * Chunk-clipped port of V33a's Channeled Flow writes: {@code WaterPuzzleGenerator.calculate},
 * {@code WaterFloor.generate}, {@code WaterLoot} and the dynamic {@code WaterStructureEntrance}.
 *
 * <p>V33a wrote the structure as one ordered sequence in which later writes overwrite earlier ones (a lower
 * floor's stairwell cuts into the floor above it, each lock clears its own channel arms, and so on). Every
 * chunk replays that same sequence from the stored seed and keeps only the writes inside its own box, which
 * gives exactly the global result. The only randomness after layout is the channel moss and the entrance
 * pool, both of which V33a drew from unseeded randoms; here they come from a per-position hash of the
 * seed, so the chunks agree with one another.
 */
public final class WaterStructurePiece extends StructurePiece {

	private final BlockPos center;
	private final int surfaceY;
	private final int difficulty;
	private final long puzzleSeed;
	private final CrystalElement color;
	private final int generationIndex;

	public WaterStructurePiece(StructureCalculator.StructurePlacement placement, WaterStructureGenerator plan,
			int surfaceY) {
		this(placement.placement(), plan, surfaceY, placement.color, placement.generationIndex);
	}

	/** Also used by the in-world chunk-placement test, with the same data as natural placement. */
	public WaterStructurePiece(BlockPos placement, WaterStructureGenerator plan, int surfaceY,
			CrystalElement element, int index) {
		super(ProximaStructurePieces.WATER.get(), 0, boundsFor(placement, plan.layout(), surfaceY));
		center = new BlockPos(placement.getX(), 0, placement.getZ());
		this.surfaceY = surfaceY;
		difficulty = plan.difficulty();
		puzzleSeed = plan.puzzleSeed();
		color = element;
		generationIndex = index;
	}

	public WaterStructurePiece(CompoundTag tag) {
		super(ProximaStructurePieces.WATER.get(), tag);
		center = new BlockPos(tag.getIntOr("X", 0), 0, tag.getIntOr("Z", 0));
		surfaceY = tag.getIntOr("SurfaceY", 100);
		difficulty = Math.clamp(tag.getIntOr("Difficulty", 3), 1, 3);
		puzzleSeed = tag.getLongOr("PuzzleSeed", 0);
		color = CrystalElement.elements[Math.floorMod(tag.getIntOr("Color", 0), CrystalElement.elements.length)];
		generationIndex = tag.getIntOr("GenerationIndex", 0);
	}

	private static BoundingBox boundsFor(BlockPos center, WaterPuzzleLayout layout, int surfaceY) {
		int r = Math.max(layout.bottom().width(), 10);
		int minY = layout.floorY(layout.floors().size() - 1) - 5;
		int maxY = Math.max(layout.topY() + WaterPuzzleLayout.FLOOR_HEIGHT + 5, surfaceY);
		return new BoundingBox(center.getX() - r, minY, center.getZ() - r, center.getX() + r, maxY, center.getZ() + r);
	}

	@Override
	protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
		tag.putInt("X", center.getX());
		tag.putInt("Z", center.getZ());
		tag.putInt("SurfaceY", surfaceY);
		tag.putInt("Difficulty", difficulty);
		tag.putLong("PuzzleSeed", puzzleSeed);
		tag.putInt("Color", color.ordinal());
		tag.putInt("GenerationIndex", generationIndex);
	}

	@Override
	public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator,
			RandomSource random, BoundingBox chunkBox, ChunkPos chunkPos, BlockPos referencePos) {
		WaterPuzzleLayout layout = WaterPuzzleLayout.create(difficulty, puzzleSeed);
		Writer w = new Writer(level, chunkBox);
		int x = center.getX();
		int z = center.getZ();
		int n = layout.floors().size();
		for (int i = 0; i < n; i++)
			generateFloor(w, layout, layout.floors().get(i), x, layout.floorY(i), z);

		int bottomY = layout.floorY(n - 1);
		for (int i = 0; i < 4; i++) {
			int[] c = layout.lootCorner(i);
			generateLoot(w, x + c[0], bottomY, z + c[1], i == layout.coreCorner());
		}

		int topY = layout.topY();
		Floor first = layout.floors().getFirst();
		int r = first.width();
		for (int i = -r; i <= r; i++) {
			for (int k = -r; k <= r; k++) {
				for (int h = 1; h <= 5; h++) {
					int dy = topY + WaterPuzzleLayout.FLOOR_HEIGHT + h;
					if (Math.abs(i) == r || Math.abs(k) == r || h == 5)
						w.set(x + i, dy, z + k, shield((Math.abs(i) + Math.abs(k)) % 6 == 0
								? ChromaShieldTypes.LIGHT : ChromaShieldTypes.STONE));
					else
						w.set(x + i, dy, z + k, Blocks.AIR.defaultBlockState());
				}
			}
		}
		WaterPuzzleLayout.Cell start = first.path().start();
		int dx = x + start.x() * WaterPuzzleLayout.LOCK_SPACING;
		int dz = z + start.z() * WaterPuzzleLayout.LOCK_SPACING;
		for (int i = 0; i <= 4; i++) {
			int y = topY + WaterPuzzleLayout.FLOOR_HEIGHT + i;
			w.set(dx, y, dz, Blocks.AIR.defaultBlockState());
			if (i > 0) {
				BlockState glass = shield(ChromaShieldTypes.GLASS);
				w.set(dx + 1, y, dz, glass);
				w.set(dx - 1, y, dz, glass);
				w.set(dx, y, dz + 1, glass);
				w.set(dx, y, dz - 1, glass);
			}
		}
		BlockPos source = new BlockPos(dx, topY + WaterPuzzleLayout.FLOOR_HEIGHT + 4, dz);
		if (chunkBox.isInside(source)) {
			BlockEverFluid26.placeSource(level, source);
			if (level.getBlockEntity(source) instanceof TileEntityEverFluid te)
				te.setFloor(0);
		}
		BlockPos password = new BlockPos(x, topY + WaterPuzzleLayout.FLOOR_HEIGHT, z);
		if (chunkBox.isInside(password)) {
			level.setBlock(password, ChromaBlocks.STRUCTURE_PASSWORD.get().defaultBlockState(), 2);
			if (level.getBlockEntity(password) instanceof TileEntityStructurePassword pw)
				pw.setStructure(color, DimensionStructureType.WATER, generationIndex);
		}

		generateEntrance(w, x, topY + WaterPuzzleLayout.FLOOR_HEIGHT + 5, z);
		resolveDoors(level, chunkBox, layout, x, z);
	}

	// --------------------------------------------------------------------------------- WaterFloor

	private void generateFloor(Writer w, WaterPuzzleLayout layout, Floor f, int x, int y, int z) {
		int level = f.index();
		boolean bottom = level == layout.floors().size() - 1;
		int r0 = f.radius();
		int r = f.width();
		int H = WaterPuzzleLayout.FLOOR_HEIGHT;
		int spacing = WaterPuzzleLayout.LOCK_SPACING;
		int iMin = Math.max(-r, w.box.minX() - x), iMax = Math.min(r, w.box.maxX() - x);
		int kMin = Math.max(-r, w.box.minZ() - z), kMax = Math.min(r, w.box.maxZ() - z);
		for (int i = iMin; i <= iMax; i++) {
			for (int k = kMin; k <= kMax; k++) {
				int ai = Math.abs(i), ak = Math.abs(k);
				for (int h = -3; h <= H; h++) {
					if ((ai >= r - 4 || ak >= r - 4) && ak < r && ai < r && (h < H || (ai > r - 4 && ak > r - 4))) {
						if (ai == r - 4 || ak == r - 4) {
							w.set(x + i, y + h, z + k, h <= 2 ? shield(ChromaShieldTypes.COBBLE) : Blocks.AIR.defaultBlockState());
						}
						else if (h == 2) {
							if (ai > r - 4 && ak > r - 4)
								w.set(x + i, y + h, z + k, ChromaBlocks.CHROMA_DOOR.get().defaultBlockState());
							else
								w.set(x + i, y + h, z + k, shield(bottom ? ChromaShieldTypes.STONE : ChromaShieldTypes.GLASS));
						}
						else {
							w.set(x + i, y + h, z + k, Blocks.AIR.defaultBlockState());
						}
					}
					else if ((h >= 0 || ai < r - 4 || ak < r - 4) && (ai == r || ak == r || (h == 0 && bottom) || h == H)) {
						w.set(x + i, y + h, z + k, shield((ai + ak) % 6 == 0 ? ChromaShieldTypes.LIGHT : ChromaShieldTypes.STONE));
					}
					else if (h == 1) {
						w.set(x + i, y + h, z + k, shield(ChromaShieldTypes.CLOAK));
					}
					else if (h == 2) {
						if (ai % spacing == WaterPuzzleLayout.LOCK_SIZE + 1 || ak % spacing == WaterPuzzleLayout.LOCK_SIZE + 1)
							w.set(x + i, y + h, z + k, shield(moss(x + i, y + h, z + k)
									? ChromaShieldTypes.MOSS : ChromaShieldTypes.STONE));
						else
							w.set(x + i, y + h, z + k, pad());
					}
					else if (h >= 0 || (ai < r - 4 && ak < r - 4)) {
						w.set(x + i, y + h, z + k, h > 0 ? Blocks.AIR.defaultBlockState() : shield(ChromaShieldTypes.COBBLE));
					}
				}
			}
		}

		if (level > 0) {
			int r2 = layout.floors().get(level - 1).width();
			for (int h = H + 1; h <= H + 4; h++) {
				for (int i = r2 - 3; i <= r; i++) {
					for (int k = r2 - 3; k <= r; k++) {
						w.set(x + i, y + h, z + k, Blocks.AIR.defaultBlockState());
						w.set(x - i, y + h, z + k, Blocks.AIR.defaultBlockState());
						w.set(x + i, y + h, z - k, Blocks.AIR.defaultBlockState());
						w.set(x - i, y + h, z - k, Blocks.AIR.defaultBlockState());
					}
				}
				for (int i = -2; i <= 2; i++) {
					for (int k = -2; k <= 2; k++) {
						BlockState s = Math.abs(i) == 2 || Math.abs(k) == 2 || h == H + 4
								? shield(ChromaShieldTypes.STONE) : Blocks.AIR.defaultBlockState();
						w.set(x - r + 2 + i, y + h, z - r + 2 + k, s);
						w.set(x - r + 2 + i, y + h, z + r - 2 + k, s);
						w.set(x + r - 2 + i, y + h, z - r + 2 + k, s);
						w.set(x + r - 2 + i, y + h, z + r - 2 + k, s);
					}
				}
				BlockState stone = shield(ChromaShieldTypes.STONE);
				for (int i = r2 - 4; i <= r; i++) {
					for (int k = r2 - 4; k <= r; k++) {
						if (Math.abs(i) > r2 || Math.abs(k) > r2) {
							w.set(x + i, y + H + 4, z + k, stone);
							w.set(x - i, y + H + 4, z + k, stone);
							w.set(x + i, y + H + 4, z - k, stone);
							w.set(x - i, y + H + 4, z - k, stone);
						}
					}
				}
				for (int i = r2; i <= r; i++) {
					w.set(x + i, y + h, z + r2 - 4, stone);
					w.set(x - i, y + h, z + r2 - 4, stone);
					w.set(x + i, y + h, z - r2 + 4, stone);
					w.set(x - i, y + h, z - r2 + 4, stone);
					w.set(x + r2 - 4, y + h, z + i, stone);
					w.set(x + r2 - 4, y + h, z - i, stone);
					w.set(x - r2 + 4, y + h, z + i, stone);
					w.set(x - r2 + 4, y + h, z - i, stone);
				}
				for (int i = r2 - 4; i <= r; i++) {
					w.set(x + i, y + h, z + r, stone);
					w.set(x + i, y + h, z - r, stone);
					w.set(x - i, y + h, z + r, stone);
					w.set(x - i, y + h, z - r, stone);
					w.set(x + r, y + h, z + i, stone);
					w.set(x - r, y + h, z + i, stone);
					w.set(x + r, y + h, z - i, stone);
					w.set(x - r, y + h, z - i, stone);
				}
				w.set(x + r2, y + h, z + r2, stone);
				w.set(x - r2, y + h, z + r2, stone);
				w.set(x + r2, y + h, z - r2, stone);
				w.set(x - r2, y + h, z - r2, stone);
				w.set(x + r2 + 1, y + h, z + r2 + 1, stone);
				w.set(x - r2 - 1, y + h, z + r2 + 1, stone);
				w.set(x + r2 + 1, y + h, z - r2 - 1, stone);
				w.set(x - r2 - 1, y + h, z - r2 - 1, stone);
			}
			for (int h = H + 1; h < H + 4; h++) {
				for (int i = 1; i <= 3; i++) {
					w.set(x + r - i, y + h, z + r - 4, Blocks.AIR.defaultBlockState());
					w.set(x - r + i, y + h, z + r - 4, Blocks.AIR.defaultBlockState());
					w.set(x + r - i, y + h, z - r + 4, Blocks.AIR.defaultBlockState());
					w.set(x - r + i, y + h, z - r + 4, Blocks.AIR.defaultBlockState());
					w.set(x - r + 4, y + h, z + r - i, Blocks.AIR.defaultBlockState());
					w.set(x - r + 4, y + h, z - r + i, Blocks.AIR.defaultBlockState());
					w.set(x + r - 4, y + h, z + r - i, Blocks.AIR.defaultBlockState());
					w.set(x + r - 4, y + h, z - r + i, Blocks.AIR.defaultBlockState());
				}
			}
			int cy = y + H + 1;
			chest(w, x + r2 + 1, cy, z + r2, Direction.EAST);
			chest(w, x + r2 + 2, cy, z + r2 + 1, Direction.NORTH);
			chest(w, x + r2, cy, z + r2 + 1, Direction.SOUTH);
			chest(w, x + r2 + 1, cy, z + r2 + 2, Direction.WEST);
			chest(w, x - r2 - 1, cy, z + r2, Direction.WEST);
			chest(w, x - r2 - 2, cy, z + r2 + 1, Direction.NORTH);
			chest(w, x - r2, cy, z + r2 + 1, Direction.SOUTH);
			chest(w, x - r2 - 1, cy, z + r2 + 2, Direction.EAST);
			chest(w, x + r2 + 1, cy, z - r2, Direction.EAST);
			chest(w, x + r2 + 2, cy, z - r2 - 1, Direction.SOUTH);
			chest(w, x + r2, cy, z - r2 - 1, Direction.NORTH);
			chest(w, x + r2 + 1, cy, z - r2 - 2, Direction.WEST);
			chest(w, x - r2 - 1, cy, z - r2, Direction.WEST);
			chest(w, x - r2 - 2, cy, z - r2 - 1, Direction.SOUTH);
			chest(w, x - r2, cy, z - r2 - 1, Direction.NORTH);
			chest(w, x - r2 - 1, cy, z - r2 - 2, Direction.EAST);
		}

		BlockPos floorOrigin = new BlockPos(x, y, z);
		for (int i = -r0; i <= r0; i++) {
			for (int k = -r0; k <= r0; k++) {
				Lock lock = f.lock(i, k);
				BlockPos at = TileEntityRotatingLock.lockPosition(floorOrigin, i, k);
				if (w.box.isInside(at)) {
					w.level.setBlock(at, ChromaBlocks.ROTATING_LOCK.get().defaultBlockState(), 2);
					if (w.level.getBlockEntity(at) instanceof TileEntityRotatingLock tile)
						tile.configure(lock, level, f.checkpoints().contains(lock.location()),
								f.path().end().equals(lock.location()), floorOrigin, r, r0);
				}
				for (Direction dir : Direction.Plane.HORIZONTAL) {
					for (int d = 0; d <= WaterPuzzleLayout.LOCK_SIZE; d++)
						if (lock.isOpen(dir))
							w.set(at.getX() + d * dir.getStepX(), y + 2, at.getZ() + d * dir.getStepZ(),
									Blocks.AIR.defaultBlockState());
					int gap = WaterPuzzleLayout.LOCK_SIZE + 1;
					w.set(at.getX() + gap * dir.getStepX(), y + 2, at.getZ() + gap * dir.getStepZ(),
							Blocks.AIR.defaultBlockState());
				}
			}
		}

		int lx = x + f.path().start().x() * spacing;
		int lz = z + f.path().start().z() * spacing;
		for (int i = 0; i <= 4; i++)
			w.set(lx, y + H + i, lz, Blocks.AIR.defaultBlockState());
	}

	private void chest(Writer w, int x, int y, int z, Direction facing) {
		BlockPos at = new BlockPos(x, y, z);
		if (!w.box.isInside(at))
			return;
		w.level.setBlock(at, ChromaBlocks.LOOT_CHEST.get().defaultBlockState().setValue(BlockLootChest.FACING, facing), 2);
		if (w.level.getBlockEntity(at) instanceof TileEntityLootChest chest) {
			chest.setLootTable(BuiltInLootTables.SIMPLE_DUNGEON, puzzleSeed ^ at.asLong());
			chest.unpackLootTable(null);
		}
	}

	// ----------------------------------------------------------------------------------- WaterLoot

	private void generateLoot(Writer w, int x, int y, int z, boolean core) {
		for (int h = 1; h <= 5; h++) {
			for (int i = -2; i <= 2; i++) {
				for (int k = -2; k <= 2; k++) {
					if (Math.abs(i) == 2 || Math.abs(k) == 2 || h >= 2)
						w.set(x + i, y - h, z + k, breakableShield(ChromaShieldTypes.STONE));
					else
						w.set(x + i, y - h, z + k, Blocks.AIR.defaultBlockState());
				}
			}
		}
		BlockPos at = new BlockPos(x, y, z);
		if (core) {
			if (w.box.isInside(at)) {
				w.level.setBlock(at, ChromaBlocks.dimensionCore(color).get().defaultBlockState(), 2);
				if (w.level.getBlockEntity(at) instanceof TileEntityDimensionCore tile) {
					StructureGeneratorBase live = findLiveGenerator();
					if (live != null)
						tile.setStructure(new StructurePair(live, color));
				}
			}
		}
		else {
			w.set(x, y - 1, z, Blocks.SMOOTH_STONE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.DOUBLE));
			w.set(x, y, z, Blocks.CAKE.defaultBlockState());
		}
	}

	// ---------------------------------------------------------------------- WaterStructureEntrance

	private void generateEntrance(Writer w, int x, int y, int z) {
		int top = surfaceY;
		int r = 4;
		for (int i = -r; i <= r; i++) {
			for (int k = -r; k <= r; k++) {
				int d = Math.abs(i) + Math.abs(k);
				if (d > 5)
					continue;
				for (int dy = y; dy <= top; dy++) {
					if (Math.abs(i) == r || Math.abs(k) == r || d == 5)
						w.set(x + i, dy, z + k, shield(ChromaShieldTypes.STONE));
					else if (dy == y)
						w.set(x + i, dy, z + k, ChromaBlocks.HOVER.get().defaultBlockState()
								.setValue(BlockHoverBlock.TYPE, BlockHoverBlock.HoverType.DAMPER)
								.setValue(BlockHoverBlock.DECAY, BlockHoverBlock.Decay.PERMANENT));
					else
						w.set(x + i, dy, z + k, Blocks.AIR.defaultBlockState());
				}
			}
		}
		r += 6;
		for (int i = -r; i <= r; i++) {
			for (int k = -r; k <= r; k++) {
				int d = Math.abs(i) + Math.abs(k);
				if ((d > 5 || Math.abs(i) > 4 || Math.abs(k) > 4) && d <= 11) {
					RandomSource rand = RandomSource.create(puzzleSeed ^ BlockPos.asLong(x + i, top, z + k));
					if (d < 11 && rand.nextInt(3) == 0) {
						w.set(x + i, top - 1, z + k, Blocks.WATER.defaultBlockState());
						w.set(x + i, top - 2, z + k, shield(rand.nextInt(4) == 0 ? ChromaShieldTypes.LIGHT
								: rand.nextInt(3) == 0 ? ChromaShieldTypes.STONE : ChromaShieldTypes.MOSS));
					}
					else {
						w.set(x + i, top - 1, z + k, shield(rand.nextInt(3) == 0 ? ChromaShieldTypes.STONE : ChromaShieldTypes.MOSS));
						w.set(x + i, top - 2, z + k, shield(rand.nextInt(3) == 0 ? ChromaShieldTypes.STONE : ChromaShieldTypes.MOSS));
					}
				}
			}
		}
	}

	// ------------------------------------------------------------------------------------- helpers

	/** Joins each floor's corner trapdoor panes once every pane in this chunk exists. */
	private static void resolveDoors(WorldGenLevel level, BoundingBox box, WaterPuzzleLayout layout, int x, int z) {
		for (Floor f : layout.floors()) {
			int r = f.width();
			int y = layout.floorY(f.index()) + 2;
			for (int sx = -1; sx <= 1; sx += 2)
				for (int sz = -1; sz <= 1; sz += 2)
					for (int i = r - 3; i <= r - 1; i++)
						for (int k = r - 3; k <= r - 1; k++) {
							BlockPos at = new BlockPos(x + sx * i, y, z + sz * k);
							if (box.isInside(at) && level.getBlockState(at).is(ChromaBlocks.CHROMA_DOOR.get()))
								level.setBlock(at, BlockChromaDoor.withConnections(level.getBlockState(at), level, at), 2);
						}
		}
	}

	/** V33a {@code ReikaRandomHelper.doWithChance(40)} for the channel walls, from the seed. */
	private boolean moss(int x, int y, int z) {
		return RandomSource.create(puzzleSeed ^ BlockPos.asLong(x, y, z) ^ 0x5DEECE66DL).nextInt(100) < 40;
	}

	private static BlockState pad() {
		return ChromaBlocks.ROTATING_LOCK.get().defaultBlockState().setValue(BlockRotatingLock26.PAD, true);
	}

	private static BlockState shield(ChromaShieldTypes type) {
		return ChromaBlocks.shielding(type).get().defaultBlockState().setValue(BlockStructureShield.REINFORCED, true);
	}

	/** V33a {@code addBreakable}: the loot pits' walls may be mined. */
	private static BlockState breakableShield(ChromaShieldTypes type) {
		return ChromaBlocks.shielding(type).get().defaultBlockState().setValue(BlockStructureShield.REINFORCED, false);
	}

	private StructureGeneratorBase findLiveGenerator() {
		ProximaGenerators.Layout layout = ProximaGenerators.getLayout();
		if (layout == null)
			return null;
		for (StructureCalculator.StructurePlacement placement : layout.structures().getPlacements())
			if (placement.color == color && placement.type == DimensionStructureType.WATER
					&& placement.generationIndex == generationIndex
					&& placement.getGenerator() instanceof StructureGeneratorBase base)
				return base;
		return null;
	}

	/** One ordered write, kept only when it falls inside this chunk's box. */
	private static final class Writer {
		final WorldGenLevel level;
		final BoundingBox box;
		private final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

		Writer(WorldGenLevel level, BoundingBox box) {
			this.level = level;
			this.box = box;
		}

		void set(int x, int y, int z, BlockState state) {
			cursor.set(x, y, z);
			if (box.isInside(cursor))
				level.setBlock(cursor, state, 2);
		}
	}
}
