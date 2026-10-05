package reika.chromaticraft.world;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
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

import java.util.List;

/** V33a's thirteen fixed lore towers, placed from the canonical DATANODE NBT template. */
public final class DataTowerFeature implements Feature {
    public static final net.minecraft.resources.ResourceKey<net.minecraft.world.level.storage.loot.LootTable> LOOT_TABLE =
            net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE,
                    Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "chests/data_tower"));

    public static final com.mojang.serialization.MapCodec<DataTowerFeature> CODEC =
            com.mojang.serialization.MapCodec.unit(DataTowerFeature::new);

    @Override
    public com.mojang.serialization.MapCodec<DataTowerFeature> codec() {
        return CODEC;
    }

	private static final Identifier TEMPLATE =
			Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "worldgen/data_node");
	private static final BlockPos ANCHOR = new BlockPos(1, 0, 1);

	public DataTowerFeature() {

	}

	@Override
	public boolean place(net.minecraft.world.level.WorldGenLevel world, net.minecraft.world.level.chunk.ChunkGenerator generator, net.minecraft.util.RandomSource random, net.minecraft.core.BlockPos origin) {
		if (!UnknownArtefactFeature.canGenerateIn(world.getLevel())
				|| world.getBlockState(new BlockPos(origin.getX(), world.getMinY(), origin.getZ())).isAir())
			return false;
		Towers tower = Towers.getTower(world.getLevel(), origin.getX(), origin.getZ());
		if (tower == null)
			return false;
		if (world instanceof net.minecraft.server.level.WorldGenRegion) {
			PostPopulationWorldgen.get(world.getLevel()).enqueueTower(net.minecraft.world.level.ChunkPos.containing(origin), tower, random.nextLong());
			return true;
		}
		return placeAt(world, origin.offset(8, 0, 8), tower, random);
	}

	/** Deterministic seam used by the feature command and focused GameTests. */
	public static boolean placeAt(WorldGenLevel world, BlockPos column, Towers tower, RandomSource random) {
		prepareTerrain(world, column);
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
			chest.setLootTable(LOOT_TABLE,
					random.nextLong());
		}
		return !placed.isEmpty();
	}

	private static int groundLevel(WorldGenLevel world, int x, int z) {
		int y = OverworldStructureFeature.terrainTop(world, x, z) - 1;
		while (y > world.getMinY()) {
			BlockState state = world.getBlockState(new BlockPos(x, y, z));
			if (!state.isAir() && !state.is(BlockTags.LEAVES) && !state.is(BlockTags.LOGS))
				return y;
			y--;
		}
		return y;
	}

	/** Original prepareIsland/prepareCrater, after all neighbouring terrain has populated. */
	private static void prepareTerrain(WorldGenLevel world, BlockPos column) {
		int x = column.getX(), z = column.getZ(), ground = groundLevel(world, x, z);
		if (!world.getFluidState(new BlockPos(x, ground + 1, z)).isEmpty()) {
			// V33a targets y=64 explicitly, including custom/superflat generators.
			int h = 64 - ground;
			for (int dx = -18; dx <= 18; dx++) for (int dz = -18; dz <= 18; dz++) {
				double distance = Math.hypot(dx, dz);
				if (distance > 18) continue;
				double f = distance / 18D;
				int rise = (int)Math.ceil(h * (f < 1D / 6 ? 1 : Math.pow(1 - (f - 1D / 6) / (5D / 6), 2)));
				if (rise == h) rise--;
				int localTop = groundLevel(world, x + dx, z + dz);
				// 26.3's biome surfaces live in noise surface rules, rather than Biome.topBlock/
				// fillerBlock. Retain the already generated surface and filler at each column.
				BlockState top = world.getBlockState(new BlockPos(x + dx, localTop, z + dz));
				BlockState filler = world.getBlockState(new BlockPos(x + dx, localTop - 1, z + dz));
				if (top.isAir() || !top.getFluidState().isEmpty()) top = net.minecraft.world.level.block.Blocks.GRASS_BLOCK.defaultBlockState();
				if (filler.isAir() || !filler.getFluidState().isEmpty()) filler = net.minecraft.world.level.block.Blocks.DIRT.defaultBlockState();
				for (int j = Math.min(0, localTop + 1 - ground); j <= rise; j++) {
					int y = ground + j;
					BlockState state = j == rise && y >= 62 ? top
							: rise - j <= 3 ? filler : net.minecraft.world.level.block.Blocks.STONE.defaultBlockState();
					world.setBlock(new BlockPos(x + dx, y, z + dz), state, 2);
				}
			}
		} else if (ground > world.getMaxY() - 9) {
			int safeTop = world.getMaxY() - 9, depth = ground - safeTop;
			for (int dx = -12; dx <= 12; dx++) for (int dz = -12; dz <= 12; dz++) {
				double distance = Math.hypot(dx, dz);
				if (distance > 12) continue;
				int from = distance <= 4 ? safeTop + 1 : ground - (int)Math.ceil(Math.max(0, 1 - distance / 12) * depth);
				for (int y = from; y < world.getMaxY(); y++)
					world.setBlock(new BlockPos(x + dx, y, z + dz), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 2);
			}
		}
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
	 * flattened by this clearance. Natural placement runs on the server once the island/crater and
	 * monument's complete neighbouring footprint is FULL.
	 */
	private static void clearTreeAndVegetation(WorldGenLevel world, BlockPos floor) {
		int top = floor.getY();
		for (int dz = -4; dz <= 4; dz++)
			for (int dx = -4; dx <= 4; dx++)
				top = Math.max(top, world.getHeight(world instanceof net.minecraft.server.level.WorldGenRegion
						? Heightmap.Types.WORLD_SURFACE_WG : Heightmap.Types.WORLD_SURFACE,
						floor.getX() + dx, floor.getZ() + dz));
		top = Math.min(world.getMaxY() - 1, top + 12);
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
