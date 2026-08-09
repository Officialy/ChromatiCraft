package reika.chromaticraft.world;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.auxiliary.structure.NBTStructureLoader;
import reika.chromaticraft.block.worldgen26.BlockStructureShield;
import reika.chromaticraft.magic.lore.Towers;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.registry.ChromaShieldTypes;
import reika.chromaticraft.tileentity.TileEntityDataNode;
import reika.chromaticraft.tileentity.TileEntityDummyAux;
import reika.chromaticraft.tileentity.TileEntityDummyAux.Flags;
import reika.chromaticraft.tileentity.TileEntityLootChest;

/** V33a's thirteen fixed lore towers, placed from the canonical DATANODE NBT template. */
public final class DataTowerFeature extends Feature<NoneFeatureConfiguration> {

	private static final Identifier TEMPLATE =
			Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "worldgen/data_node");
	private static final BlockPos ANCHOR = new BlockPos(1, 0, 1);

	public DataTowerFeature() {
		super(NoneFeatureConfiguration.CODEC);
	}

	@Override
	public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
		WorldGenLevel world = context.level();
		BlockPos origin = context.origin();
		Towers tower = Towers.getTower(world.getLevel(), origin.getX(), origin.getZ());
		if (tower == null)
			return false;
		return placeAt(world, origin.offset(8, 0, 8), tower, context.random());
	}

	/** Deterministic seam used by the feature command and focused GameTests. */
	public static boolean placeAt(WorldGenLevel world, BlockPos column, Towers tower, RandomSource random) {
		int y = footprintGroundLevel(world, column);
		if (y <= world.getMinY())
			return false;
		BlockPos floor = new BlockPos(column.getX(), y + 1, column.getZ());
		clearTreeAndVegetation(world, floor);
		BlockState stone = ChromaBlocks.shielding(ChromaShieldTypes.STONE).get().defaultBlockState()
				.setValue(BlockStructureShield.REINFORCED, true);
		BlockState moss = ChromaBlocks.shielding(ChromaShieldTypes.MOSS).get().defaultBlockState()
				.setValue(BlockStructureShield.REINFORCED, true);
		List<BlockPos> placed = NBTStructureLoader.place(world, TEMPLATE, floor, ANCHOR,
				state -> state.is(stone.getBlock()) ? (random.nextInt(3) == 0 ? moss : stone) : state, 3);

		BlockPos nodePos = floor.above();
		if (!(world.getBlockEntity(nodePos) instanceof TileEntityDataNode node))
			return false;
		node.setTower(tower);
		tower.generatedAt(nodePos.getX(), nodePos.getY(), nodePos.getZ());
		for (int i = 1; i <= 4; i++) {
			if (world.getBlockEntity(nodePos.above(i)) instanceof TileEntityDummyAux dummy) {
				dummy.link(nodePos);
				dummy.setFlag(Flags.HITBOX, true);
				dummy.setFlag(Flags.RENDER, false);
				dummy.setFlag(Flags.MOUSEOVER, false);
			}
		}

		BlockPos chestPos = floor.offset(random.nextInt(3) - 1, -1, random.nextInt(3) - 1);
		world.setBlock(chestPos, ChromaBlocks.LOOT_CHEST.get().defaultBlockState(), 3);
		if (world.getBlockEntity(chestPos) instanceof TileEntityLootChest chest) {
			chest.setLootTable(net.minecraft.world.level.storage.loot.BuiltInLootTables.STRONGHOLD_LIBRARY,
					random.nextLong());
		}
		return !placed.isEmpty();
	}

	private static int groundLevel(WorldGenLevel world, int x, int z) {
		int y = world.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
		while (y > world.getMinY()) {
			BlockState state = world.getBlockState(new BlockPos(x, y, z));
			if (!state.isAir() && !state.is(BlockTags.LEAVES) && !state.is(BlockTags.LOGS))
				return y;
			y--;
		}
		return y;
	}

	/** V33a lowers the shared tower floor until all nine foundation columns are below soft/tree blocks. */
	private static int footprintGroundLevel(WorldGenLevel world, BlockPos column) {
		int y = world.getMaxY();
		for (int dz = -1; dz <= 1; dz++)
			for (int dx = -1; dx <= 1; dx++)
				y = Math.min(y, groundLevel(world, column.getX() + dx, column.getZ() + dz));
		return y;
	}

	/**
	 * Data towers run after vegetation so the NBT monument wins deterministically. Clear only tree
	 * and replaceable vegetation in a compact radius; terrain and player/structure materials are not
	 * flattened. The fixed center is eight blocks inside its chunk, so this entire read/write volume
	 * stays inside the feature's writable chunk and cannot trigger cross-chunk worldgen reads.
	 */
	private static void clearTreeAndVegetation(WorldGenLevel world, BlockPos floor) {
		int top = floor.getY();
		for (int dz = -4; dz <= 4; dz++)
			for (int dx = -4; dx <= 4; dx++)
				top = Math.max(top, world.getHeight(Heightmap.Types.WORLD_SURFACE_WG,
						floor.getX() + dx, floor.getZ() + dz));
		top = Math.min(world.getMaxY(), top + 12);
		BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
		for (int dz = -4; dz <= 4; dz++) {
			for (int dx = -4; dx <= 4; dx++) {
				for (int y = floor.getY(); y <= top; y++) {
					cursor.set(floor.getX() + dx, y, floor.getZ() + dz);
					BlockState state = world.getBlockState(cursor);
					if (state.is(BlockTags.LOGS) || state.is(BlockTags.LEAVES)
							|| (!state.isAir() && state.getFluidState().isEmpty() && state.canBeReplaced()))
						world.removeBlock(cursor, false);
				}
			}
		}
	}
}
