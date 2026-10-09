package reika.chromaticraft.world.dimension.structure;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;

import reika.chromaticraft.block.BlockChromaDoor;
import reika.chromaticraft.block.dimension.structure.rayblend.BlockRayblendFloor26;
import reika.chromaticraft.block.worldgen26.BlockLootChest;
import reika.chromaticraft.block.worldgen26.BlockStructureShield;
import reika.chromaticraft.data.ChromaStructureTemplateProvider;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.registry.ChromaClusterItems;
import reika.chromaticraft.registry.ChromaCraftingItems;
import reika.chromaticraft.registry.ChromaItems;
import reika.chromaticraft.registry.ChromaShieldTypes;
import reika.chromaticraft.registry.ChromaTieredItems;
import reika.chromaticraft.registry.CrystalElement;
import reika.chromaticraft.registry.ProximaDecoTypes;
import reika.chromaticraft.tileentity.TileEntityLootChest;
import reika.chromaticraft.tileentity.TileEntityRayblendFloor;
import reika.chromaticraft.tileentity.TileEntityStructurePassword;
import reika.chromaticraft.tileentity.technical.TileEntityDimensionCore;
import reika.chromaticraft.world.dimension.DimensionStructureType;
import reika.chromaticraft.world.dimension.ProximaGenerators;
import reika.chromaticraft.world.dimension.StructureCalculator;
import reika.chromaticraft.world.dimension.structure.StructureGeneratorBase.StructurePair;

/**
 * Chunk-clipped composition of V33a's Crystal Interference, in its write order: the rooms, corridor and password
 * alcove ({@link RayBlendLayout#run}), the reward room (the datagen-imported {@code RayBlendLoot} template, then its
 * chests and core), then the entrance ({@link RayBlendEntrancePlan}). Every chunk replays the plan from the stored
 * seed and keeps the writes inside its box. The entrance's crystal deposits depend on terrain the chunk cannot see,
 * so they were planned when the structure started and travel with the piece.
 */
public final class RayBlendStructurePiece extends StructurePiece {

	private final int originX;
	private final int originZ;
	private final int surfaceY;
	private final int layoutVersion;
	private final int difficulty;
	private final long puzzleSeed;
	private final CrystalElement color;
	private final int generationIndex;
	private final List<RayBlendEntrancePlan.Deposit> deposits;

	public RayBlendStructurePiece(StructureCalculator.StructurePlacement placement, RayBlendStructureGenerator plan,
			int surfaceY, List<RayBlendEntrancePlan.Deposit> deposits) {
		this(plan, surfaceY, placement.color, placement.generationIndex, deposits);
	}

	/** Also used by the in-world tests, with the same data as natural placement. */
	public RayBlendStructurePiece(RayBlendStructureGenerator plan, int surfaceY, CrystalElement element, int index,
			List<RayBlendEntrancePlan.Deposit> deposits) {
		super(ProximaStructurePieces.RAYBLEND.get(), 0, boundsFor(plan.layout(), surfaceY, deposits));
		originX = plan.layout().originX();
		originZ = plan.layout().originZ();
		this.surfaceY = surfaceY;
		layoutVersion = plan.layout().version();
		difficulty = plan.difficulty();
		puzzleSeed = plan.puzzleSeed();
		color = element;
		generationIndex = index;
		this.deposits = List.copyOf(deposits);
	}

	public RayBlendStructurePiece(CompoundTag tag) {
		super(ProximaStructurePieces.RAYBLEND.get(), tag);
		originX = tag.getIntOr("X", 0);
		originZ = tag.getIntOr("Z", 0);
		surfaceY = tag.getIntOr("SurfaceY", 100);
		layoutVersion = tag.getIntOr("LayoutVersion", RayBlendLayout.LEGACY_VERSION);
		difficulty = Math.clamp(tag.getIntOr("Difficulty", 3), 1, 3);
		puzzleSeed = tag.getLongOr("PuzzleSeed", 0);
		color = CrystalElement.elements[Math.floorMod(tag.getIntOr("Color", 0), CrystalElement.elements.length)];
		generationIndex = tag.getIntOr("GenerationIndex", 0);
		List<RayBlendEntrancePlan.Deposit> list = new ArrayList<>();
		for (Tag t : tag.getListOrEmpty("Deposits")) {
			if (!(t instanceof CompoundTag d))
				continue;
			List<BlockPos> crystals = new ArrayList<>();
			for (long l : d.getLongArray("Crystals").orElse(new long[0]))
				crystals.add(BlockPos.of(l));
			byte[] colors = d.getByteArray("Colors").orElse(new byte[crystals.size()]);
			if (colors.length != crystals.size())
				colors = java.util.Arrays.copyOf(colors, crystals.size());
			list.add(new RayBlendEntrancePlan.Deposit(BlockPos.of(d.getLongOr("Origin", 0)), List.copyOf(crystals), colors));
		}
		deposits = List.copyOf(list);
	}

	/** The plan's own bounds, plus the entrance column's reach and every deposit cell. */
	static BoundingBox boundsFor(RayBlendLayout layout, int surfaceY, List<RayBlendEntrancePlan.Deposit> deposits) {
		int[] b = layout.bounds();
		BlockPos c = RayBlendEntrancePlan.center(layout);
		int r = RayBlendEntrancePlan.REACH;
		int minY = Math.min(b[1], layout.posY() + 2), maxY = Math.max(b[4], surfaceY + 5);
		for (RayBlendEntrancePlan.Deposit d : deposits) {
			for (BlockPos p : d.crystals()) {
				minY = Math.min(minY, p.getY() - 1);
				maxY = Math.max(maxY, p.getY() + 4);
			}
		}
		return new BoundingBox(Math.min(b[0], c.getX() - r), minY, Math.min(b[2], c.getZ() - r),
				Math.max(b[3], c.getX() + r), maxY, Math.max(b[5], c.getZ() + r));
	}

	@Override
	protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
		tag.putInt("X", originX);
		tag.putInt("Z", originZ);
		tag.putInt("SurfaceY", surfaceY);
		tag.putInt("LayoutVersion", layoutVersion);
		tag.putInt("Difficulty", difficulty);
		tag.putLong("PuzzleSeed", puzzleSeed);
		tag.putInt("Color", color.ordinal());
		tag.putInt("GenerationIndex", generationIndex);
		ListTag list = new ListTag();
		for (RayBlendEntrancePlan.Deposit d : deposits) {
			CompoundTag dt = new CompoundTag();
			dt.putLong("Origin", d.origin().asLong());
			long[] crystals = new long[d.crystals().size()];
			for (int i = 0; i < crystals.length; i++)
				crystals[i] = d.crystals().get(i).asLong();
			dt.putLongArray("Crystals", crystals);
			dt.putByteArray("Colors", d.colors());
			list.add(dt);
		}
		tag.put("Deposits", list);
	}

	@Override
	public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator,
			RandomSource random, BoundingBox chunkBox, ChunkPos chunkPos, BlockPos referencePos) {
		RayBlendLayout layout = RayBlendLayout.cached(layoutVersion, difficulty, puzzleSeed, originX, originZ);
		Writer w = new Writer(level, chunkBox);
		layout.run(w);

		BlockPos loot = layout.lootOrigin();
		placeTemplate(level, chunkBox, ChromaStructureTemplateProvider.PROXIMA_RAYBLEND_LOOT, loot);
		List<BlockPos> chests = layout.rewardChests();
		rewardChest(level, chunkBox, chests.get(0), Direction.SOUTH);
		rewardChest(level, chunkBox, chests.get(1), Direction.NORTH);
		BlockPos core = layout.core();
		if (chunkBox.isInside(core)) {
			level.setBlock(core, ChromaBlocks.dimensionCore(color).get().defaultBlockState(), 2);
			if (level.getBlockEntity(core) instanceof TileEntityDimensionCore tile) {
				StructureGeneratorBase live = findLiveGenerator();
				if (live != null)
					tile.setStructure(new StructurePair(live, color));
			}
		}

		RayBlendEntrancePlan.run(w, layout, surfaceY, deposits);

		for (RayBlendLayout.Room room : layout.rooms())
			for (BlockPos panel : room.doors())
				if (chunkBox.isInside(panel) && level.getBlockState(panel).is(ChromaBlocks.CHROMA_DOOR.get()))
					level.setBlock(panel, BlockChromaDoor.withConnections(level.getBlockState(panel), level, panel), 2);
	}

	private void placeTemplate(WorldGenLevel level, BoundingBox chunkBox, Identifier id, BlockPos anchor) {
		var template = level.getLevel().getStructureTemplateManager().get(id)
				.orElseThrow(() -> new IllegalStateException("Missing Crystal Interference template " + id));
		var size = template.getSize();
		if (anchor.getX() > chunkBox.maxX() || anchor.getX() + size.getX() - 1 < chunkBox.minX()
				|| anchor.getZ() > chunkBox.maxZ() || anchor.getZ() + size.getZ() - 1 < chunkBox.minZ())
			return;
		template.placeInWorld(level, anchor, anchor, new StructurePlaceSettings().setRotation(Rotation.NONE)
				.setIgnoreEntities(true).setBoundingBox(chunkBox), RandomSource.create(anchor.asLong() ^ puzzleSeed), 2);
	}

	/**
	 * V33a {@code generateLootChest(.., STRONGHOLD_CROSSING, 0, lumaDust 50, multiShard 90, lumenGem 50,
	 * glowChunk 10)}: the loot table, then each extra that rolls its percentage dropped into a random slot.
	 */
	private void rewardChest(WorldGenLevel level, BoundingBox box, BlockPos at, Direction facing) {
		if (!box.isInside(at))
			return;
		level.setBlock(at, ChromaBlocks.LOOT_CHEST.get().defaultBlockState().setValue(BlockLootChest.FACING, facing), 2);
		if (!(level.getBlockEntity(at) instanceof TileEntityLootChest chest))
			return;
		chest.setLootTable(BuiltInLootTables.STRONGHOLD_CROSSING, puzzleSeed ^ at.asLong());
		chest.unpackLootTable(null);
		Random rand = new Random(puzzleSeed ^ at.asLong() ^ 0x52617942L);
		Object[][] extras = {
				{new ItemStack(ChromaItems.TIERED.get(ChromaTieredItems.LUMA_DUST).get()), 50},
				{ChromaItems.clusterStack(ChromaClusterItems.MULTI_CRYSTAL), 90},
				{new ItemStack(ChromaItems.TIERED.get(ChromaTieredItems.LUMEN_GEM).get()), 50},
				{new ItemStack(ChromaItems.CRAFTING.get(ChromaCraftingItems.GLOW_CHUNK).get()), 10}};
		for (Object[] extra : extras)
			if (rand.nextDouble() < (Integer)extra[1] / 100D)
				chest.setItem(rand.nextInt(chest.getContainerSize()), ((ItemStack)extra[0]).copy());
	}

	private StructureGeneratorBase findLiveGenerator() {
		ProximaGenerators.Layout layout = ProximaGenerators.getLayout();
		if (layout == null)
			return null;
		for (StructureCalculator.StructurePlacement placement : layout.structures().getPlacements())
			if (placement.color == color && placement.type == DimensionStructureType.RAYBLEND
					&& placement.generationIndex == generationIndex
					&& placement.getGenerator() instanceof StructureGeneratorBase base)
				return base;
		return null;
	}

	private final class Writer implements RayBlendLayout.Sink {
		private final WorldGenLevel level;
		private final BoundingBox box;
		private final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

		Writer(WorldGenLevel level, BoundingBox box) {
			this.level = level;
			this.box = box;
		}

		private boolean inside(int x, int y, int z) {
			cursor.set(x, y, z);
			return box.isInside(cursor);
		}

		private void set(int x, int y, int z, BlockState state) {
			if (inside(x, y, z))
				level.setBlock(cursor, state, 2);
		}

		@Override public void air(int x, int y, int z) { set(x, y, z, Blocks.AIR.defaultBlockState()); }

		@Override
		public void shield(int x, int y, int z, ChromaShieldTypes type) {
			set(x, y, z, ChromaBlocks.shielding(type).get().defaultBlockState().setValue(BlockStructureShield.REINFORCED, true));
		}

		@Override
		public void specialShield(int x, int y, int z, ChromaShieldTypes type) {
			set(x, y, z, ChromaBlocks.specialShielding(type).get().defaultBlockState()
					.setValue(BlockStructureShield.REINFORCED, true));
		}

		@Override
		public void lifewater(int x, int y, int z) {
			set(x, y, z, ChromaBlocks.deco(ProximaDecoTypes.LIFEWATER).get().defaultBlockState());
		}

		@Override
		public void floor(int x, int y, int z, RayBlendLayout.Room room, int cellX, int cellZ) {
			if (!inside(x, y, z))
				return;
			int c = room.cell(cellX, cellZ);
			level.setBlock(cursor, BlockRayblendFloor26.state(ChromaBlocks.RAYBLEND_FLOOR.get().defaultBlockState(),
					room.region()[c], room.blocked()[c], room.cage()[c]), 2);
			if (level.getBlockEntity(cursor) instanceof TileEntityRayblendFloor te)
				te.configure(layoutVersion, difficulty, puzzleSeed, originX, originZ, room.index(), cellX, cellZ);
		}

		@Override
		public void crystal(int x, int y, int z, CrystalElement e) {
			set(x, y, z, ChromaBlocks.caveCrystal(e).get().defaultBlockState());
		}

		@Override public void brick(int x, int y, int z) { set(x, y, z, Blocks.BRICKS.defaultBlockState()); }

		@Override public void door(int x, int y, int z) { set(x, y, z, ChromaBlocks.CHROMA_DOOR.get().defaultBlockState()); }

		@Override
		public void password(int x, int y, int z) {
			if (!inside(x, y, z))
				return;
			level.setBlock(cursor, ChromaBlocks.STRUCTURE_PASSWORD.get().defaultBlockState(), 2);
			if (level.getBlockEntity(cursor) instanceof TileEntityStructurePassword pw)
				pw.setStructure(color, DimensionStructureType.RAYBLEND, generationIndex);
		}

		/** V33a {@code Cell.generateLootChest}: {@code populateChest(STRONGHOLD_CROSSING, null, 0, ..)}. */
		@Override
		public void lootChest(int x, int y, int z, Direction facing) {
			if (!inside(x, y, z))
				return;
			level.setBlock(cursor, ChromaBlocks.LOOT_CHEST.get().defaultBlockState().setValue(BlockLootChest.FACING, facing), 2);
			if (level.getBlockEntity(cursor) instanceof TileEntityLootChest chest) {
				chest.setLootTable(BuiltInLootTables.STRONGHOLD_CROSSING, puzzleSeed ^ cursor.asLong());
				chest.unpackLootTable(null);
			}
		}
	}
}
