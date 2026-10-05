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
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;

import reika.chromaticraft.block.BlockChromaDoor;
import reika.chromaticraft.block.BlockEncrustedCrystal;
import reika.chromaticraft.block.BlockHoverBlock;
import reika.chromaticraft.block.dimension.structure.laser.BlockLaserEffector26;
import reika.chromaticraft.block.dimension.structure.pistontape.BlockPistonController26;
import reika.chromaticraft.block.dimension.structure.pistontape.BlockPistonTapeBit26;
import reika.chromaticraft.block.dimension.structure.pistontape.BlockPistonTarget26;
import reika.chromaticraft.block.dimension.structure.shiftmaze.BlockShiftLock;
import reika.chromaticraft.block.worldgen26.BlockLootChest;
import reika.chromaticraft.block.worldgen26.BlockStructureShield;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.registry.ChromaShieldTypes;
import reika.chromaticraft.registry.CrystalElement;
import reika.chromaticraft.tileentity.TileEntityLaserEffector;
import reika.chromaticraft.tileentity.TileEntityLootChest;
import reika.chromaticraft.tileentity.TileEntityPistonController;
import reika.chromaticraft.tileentity.TileEntityPistonTarget;
import reika.chromaticraft.tileentity.TileEntityStructurePassword;
import reika.chromaticraft.tileentity.technical.TileEntityDimensionCore;
import reika.chromaticraft.world.dimension.DimensionStructureType;
import reika.chromaticraft.world.dimension.ProximaGenerators;
import reika.chromaticraft.world.dimension.StructureCalculator;
import reika.chromaticraft.world.dimension.structure.LaserPuzzleLayout.EffectType;
import reika.chromaticraft.world.dimension.structure.StructureGeneratorBase.StructurePair;
import reika.dragonapi.instantiable.RGBColorData;
import reika.dragonapi.libraries.ReikaDirectionHelper.CubeDirections;

/**
 * Chunk-clipped composition of V33a's Filter Cycle: every chunk replays {@link PistonTapePlan#run} from the stored
 * seed and keeps the writes inside its box, which reproduces V33a's ordered overwrite sequence exactly.
 */
public final class PistonTapeStructurePiece extends StructurePiece {

	private final int originX;
	private final int originZ;
	private final int surfaceY;
	private final int difficulty;
	private final long puzzleSeed;
	private final CrystalElement color;
	private final int generationIndex;

	public PistonTapeStructurePiece(StructureCalculator.StructurePlacement placement, PistonTapeStructureGenerator plan,
			int surfaceY) {
		this(plan, surfaceY, placement.color, placement.generationIndex);
	}

	/** Also used by the in-world placement tests, with the same data as natural placement. */
	public PistonTapeStructurePiece(PistonTapeStructureGenerator plan, int surfaceY, CrystalElement element, int index) {
		super(ProximaStructurePieces.PISTONTAPE.get(), 0, boundsFor(plan.plan(), surfaceY));
		originX = plan.plan().originX();
		originZ = plan.plan().originZ();
		this.surfaceY = surfaceY;
		difficulty = plan.difficulty();
		puzzleSeed = plan.puzzleSeed();
		color = element;
		generationIndex = index;
	}

	public PistonTapeStructurePiece(CompoundTag tag) {
		super(ProximaStructurePieces.PISTONTAPE.get(), tag);
		originX = tag.getIntOr("X", 0);
		originZ = tag.getIntOr("Z", 0);
		surfaceY = tag.getIntOr("SurfaceY", 100);
		difficulty = Math.clamp(tag.getIntOr("Difficulty", 3), 1, 3);
		puzzleSeed = tag.getLongOr("PuzzleSeed", 0);
		color = CrystalElement.elements[Math.floorMod(tag.getIntOr("Color", 0), CrystalElement.elements.length)];
		generationIndex = tag.getIntOr("GenerationIndex", 0);
	}

	private static BoundingBox boundsFor(PistonTapePlan plan, int surfaceY) {
		int[] b = plan.bounds();
		BlockPos s = plan.entranceStart();
		return new BoundingBox(Math.min(b[0], s.getX() - 6), b[1], Math.min(b[2], s.getZ() - 6),
				Math.max(b[3], s.getX() + 6), Math.max(b[4], surfaceY), Math.max(b[5], s.getZ() + 6));
	}

	@Override
	protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
		tag.putInt("X", originX);
		tag.putInt("Z", originZ);
		tag.putInt("SurfaceY", surfaceY);
		tag.putInt("Difficulty", difficulty);
		tag.putLong("PuzzleSeed", puzzleSeed);
		tag.putInt("Color", color.ordinal());
		tag.putInt("GenerationIndex", generationIndex);
	}

	@Override
	public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator,
			RandomSource random, BoundingBox chunkBox, ChunkPos chunkPos, BlockPos referencePos) {
		PistonTapePlan plan = TileEntityPistonController.plan(difficulty, puzzleSeed, originX, originZ);
		Writer w = new Writer(level, chunkBox, plan);
		plan.run(w, surfaceY);
		for (PistonTapePlan.Stage stage : plan.stages())
			for (PistonTapePlan.DoorKey door : stage.doors())
				for (BlockPos panel : door.panels())
					if (chunkBox.isInside(panel) && level.getBlockState(panel).is(ChromaBlocks.CHROMA_DOOR.get()))
						level.setBlock(panel, BlockChromaDoor.withConnections(level.getBlockState(panel), level, panel), 2);
	}

	private StructureGeneratorBase findLiveGenerator() {
		ProximaGenerators.Layout layout = ProximaGenerators.getLayout();
		if (layout == null)
			return null;
		for (StructureCalculator.StructurePlacement placement : layout.structures().getPlacements())
			if (placement.color == color && placement.type == DimensionStructureType.PISTONTAPE
					&& placement.generationIndex == generationIndex
					&& placement.getGenerator() instanceof StructureGeneratorBase base)
				return base;
		return null;
	}

	private final class Writer implements PistonTapePlan.Sink {
		private final WorldGenLevel level;
		private final BoundingBox box;
		private final PistonTapePlan plan;
		private final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

		Writer(WorldGenLevel level, BoundingBox box, PistonTapePlan plan) {
			this.level = level;
			this.box = box;
			this.plan = plan;
		}

		private boolean inside(int x, int y, int z) {
			cursor.set(x, y, z);
			return box.isInside(cursor);
		}

		private void set(int x, int y, int z, BlockState state) {
			if (inside(x, y, z))
				level.setBlock(cursor, state, 2);
		}

		@Override
		public void block(int x, int y, int z, PistonTapePlan.Kind kind) {
			set(x, y, z, switch (kind) {
				case AIR -> Blocks.AIR.defaultBlockState();
				case STONE_BRICK_SLAB -> Blocks.STONE_BRICK_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM);
				case STONE_BRICKS -> Blocks.STONE_BRICKS.defaultBlockState();
			});
		}

		@Override
		public void shield(int x, int y, int z, ChromaShieldTypes type) {
			set(x, y, z, ChromaBlocks.shielding(type).get().defaultBlockState()
					.setValue(BlockStructureShield.REINFORCED, !plan.isBreakable(x, y, z)));
		}

		@Override
		public void specialShield(int x, int y, int z, ChromaShieldTypes type) {
			set(x, y, z, ChromaBlocks.specialShielding(type).get().defaultBlockState()
					.setValue(BlockStructureShield.REINFORCED, !plan.isBreakable(x, y, z)));
		}

		@Override
		public void piston(int x, int y, int z, Direction push) {
			set(x, y, z, Blocks.PISTON.defaultBlockState().setValue(PistonBaseBlock.FACING, push));
		}

		@Override
		public void bit(int x, int y, int z, int meta) {
			set(x, y, z, ChromaBlocks.PISTON_TAPE_BIT.get().defaultBlockState().setValue(BlockPistonTapeBit26.BIT, meta));
		}

		/** V33a {@code PistonEmitterCallback}: a full-block, silent, white emitter, 1.25x speed, facing the slice. */
		@Override
		public void emitter(int x, int y, int z, Direction facing) {
			if (!inside(x, y, z))
				return;
			CubeDirections dir = CubeDirections.getFromVectors(facing.getStepX(), facing.getStepZ());
			LaserPuzzleLayout.Effector effector = new LaserPuzzleLayout.Effector(BlockPos.ZERO, EffectType.EMITTER, dir,
					true, true, true, false, true, 0, 2, 0, true, true, 1.25);
			level.setBlock(cursor, ChromaBlocks.LASER_EFFECTOR.get().defaultBlockState()
					.setValue(BlockLaserEffector26.TYPE, EffectType.EMITTER.ordinal())
					.setValue(BlockLaserEffector26.DIRECTION, dir.ordinal())
					.setValue(BlockLaserEffector26.FULL_BLOCK, true)
					.setValue(BlockLaserEffector26.COLOR, 7)
					.setValue(BlockLaserEffector26.ROTATEABLE, false), 2);
			if (level.getBlockEntity(cursor) instanceof TileEntityLaserEffector te)
				te.configure(effector, null);
		}

		@Override
		public void readHead(int x, int y, int z, Direction facing, int stage, int bus, int busWidth) {
			if (!inside(x, y, z))
				return;
			level.setBlock(cursor, ChromaBlocks.PISTON_TARGET.get().defaultBlockState()
					.setValue(BlockPistonTarget26.TYPE, 0).setValue(BlockPistonTarget26.FACING, facing)
					.setValue(BlockPistonTarget26.COLOR, 7), 2);
			if (level.getBlockEntity(cursor) instanceof TileEntityPistonTarget te)
				te.configure(facing, stage, -1, bus, busWidth);
		}

		@Override
		public void doorTarget(int x, int y, int z, Direction facing, int stage, int door, int bus, int busWidth,
				RGBColorData clr, BlockPos doorPanel) {
			if (!inside(x, y, z))
				return;
			level.setBlock(cursor, ChromaBlocks.PISTON_TARGET.get().defaultBlockState()
					.setValue(BlockPistonTarget26.TYPE, 1).setValue(BlockPistonTarget26.FACING, facing)
					.setValue(BlockPistonTarget26.COLOR, BlockPistonTarget26.colorBits(clr)), 2);
			if (level.getBlockEntity(cursor) instanceof TileEntityPistonTarget te) {
				te.configure(facing, stage, door, bus, busWidth);
				te.configureDoor(clr, doorPanel);
			}
		}

		@Override
		public void controller(int x, int y, int z, int type, int stage, Direction facing) {
			if (!inside(x, y, z))
				return;
			level.setBlock(cursor, ChromaBlocks.PISTON_CONTROLLER.get().defaultBlockState()
					.setValue(BlockPistonController26.TYPE, type).setValue(BlockPistonController26.FACING, facing), 2);
			if (level.getBlockEntity(cursor) instanceof TileEntityPistonController te)
				te.configure(stage, difficulty, puzzleSeed, originX, originZ);
		}

		@Override
		public void password(int x, int y, int z) {
			if (!inside(x, y, z))
				return;
			level.setBlock(cursor, ChromaBlocks.STRUCTURE_PASSWORD.get().defaultBlockState(), 2);
			if (level.getBlockEntity(cursor) instanceof TileEntityStructurePassword pw)
				pw.setStructure(color, DimensionStructureType.PISTONTAPE, generationIndex);
		}

		@Override
		public void doorPanel(int x, int y, int z) {
			set(x, y, z, ChromaBlocks.CHROMA_DOOR.get().defaultBlockState());
		}

		@Override
		public void shiftLock(int x, int y, int z, BlockShiftLock.Passability passability) {
			set(x, y, z, ChromaBlocks.SHIFT_LOCK.get().defaultBlockState().setValue(BlockShiftLock.PASSABILITY, passability));
		}

		@Override
		public void lootChest(int x, int y, int z, Direction facing) {
			if (!inside(x, y, z))
				return;
			level.setBlock(cursor, ChromaBlocks.LOOT_CHEST.get().defaultBlockState().setValue(BlockLootChest.FACING, facing), 2);
			if (level.getBlockEntity(cursor) instanceof TileEntityLootChest chest) {
				chest.setLootTable(BuiltInLootTables.ABANDONED_MINESHAFT, puzzleSeed ^ cursor.asLong());
				chest.unpackLootTable(null);
			}
		}

		/** V33a {@code EncrustedCrystalCallback}: 3-9 growth on every side that can hold it. */
		@Override
		public void encrusted(int x, int y, int z) {
			if (!inside(x, y, z))
				return;
			BlockPos at = cursor.immutable();
			level.setBlock(at, ChromaBlocks.encrustedCrystal(color).get().defaultBlockState(), 2);
			if (level.getBlockEntity(at) instanceof BlockEncrustedCrystal.TileCrystalEncrusted te) {
				java.util.Random rand = new java.util.Random(puzzleSeed ^ at.asLong());
				for (Direction dir : Direction.values())
					if (BlockEncrustedCrystal.CrystalGrowth.canExist(level, at, dir))
						te.addGrowth(dir, 3 + rand.nextInt(7));
				level.scheduleTick(at, te.getBlockState().getBlock(), 100);
			}
		}

		@Override
		public void core(int x, int y, int z) {
			if (!inside(x, y, z))
				return;
			level.setBlock(cursor, ChromaBlocks.dimensionCore(color).get().defaultBlockState(), 2);
			if (level.getBlockEntity(cursor) instanceof TileEntityDimensionCore tile) {
				StructureGeneratorBase live = findLiveGenerator();
				if (live != null)
					tile.setStructure(new StructurePair(live, color));
			}
		}

		@Override
		public void hover(int x, int y, int z) {
			set(x, y, z, ChromaBlocks.HOVER.get().defaultBlockState()
					.setValue(BlockHoverBlock.TYPE, BlockHoverBlock.HoverType.DAMPER)
					.setValue(BlockHoverBlock.DECAY, BlockHoverBlock.Decay.PERMANENT));
		}
	}
}
