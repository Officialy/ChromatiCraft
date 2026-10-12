package reika.chromaticraft.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import reika.chromaticraft.auxiliary.structure.NBTStructureLoader;
import reika.chromaticraft.block.BlockChromaDoor;
import reika.chromaticraft.block.BlockHeatLamp;
import reika.chromaticraft.item.ItemDoorKey;
import reika.chromaticraft.magic.progression.ProgressStage;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.registry.ChromaItems;
import reika.chromaticraft.registry.CrystalElement;
import reika.chromaticraft.tileentity.TileEntityChromaDoor;
import reika.chromaticraft.tileentity.TileEntityHeatLamp;
import reika.chromaticraft.tileentity.TileEntityLootChest;
import reika.chromaticraft.tileentity.TileEntityStructureController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** NBT-backed runtime feature family for V33a's six natural overworld structures. */
public final class OverworldStructureFeature implements Feature {
	public static final com.mojang.serialization.MapCodec<OverworldStructureFeature> CODEC =
			com.mojang.serialization.codecs.RecordCodecBuilder.mapCodec(instance -> instance.group(
					com.mojang.serialization.Codec.STRING.fieldOf("structure")
							.forGetter(feature -> feature.type.name().toLowerCase(java.util.Locale.ROOT)),
					com.mojang.serialization.Codec.BOOL.fieldOf("natural")
							.forGetter(feature -> feature.natural)
			).apply(instance, (name, natural) -> new OverworldStructureFeature(
					Type.valueOf(name.toUpperCase(java.util.Locale.ROOT)), natural)));

	@Override
	public com.mojang.serialization.MapCodec<OverworldStructureFeature> codec() {
		return CODEC;
	}
	private static final Identifier BURROW_FURNACE = NBTStructureLoader.chromaTemplate(
			"worldgen/overworld/burrow_furnace");
	private static final Identifier BURROW_LOOT = NBTStructureLoader.chromaTemplate(
			"worldgen/overworld/burrow_loot");
	private static final Identifier OCEAN_PIT_SLICE = NBTStructureLoader.chromaTemplate(
			"worldgen/overworld/ocean_pit_slice");
	private static final BlockPos BURROW_ANNEX_ANCHOR = new BlockPos(0, 3, 4);
	public static final ResourceKey<net.minecraft.world.level.storage.loot.LootTable> BURROW_CACHE_LOOT =
			ResourceKey.create(Registries.LOOT_TABLE,
					Identifier.fromNamespaceAndPath("chromaticraft", "chests/burrow_cache"));
	private record FurnaceOre(TagKey<Item> tag, int weight, int maximum) { }
	private static final List<FurnaceOre> FURNACE_ORES = List.of(
			ore("iron", 40, 64), ore("gold", 15, 24), ore("copper", 50, 40),
			ore("tin", 50, 40), ore("lead", 10, 24), ore("nickel", 10, 24),
			ore("silver", 15, 24), ore("aluminum", 20, 40), ore("platinum", 2, 8));

	public enum Type {
		CAVERN("cavern", new BlockPos(7, 2, 5), TileEntityStructureController.StructureType.CAVERN),
		BURROW("burrow", new BlockPos(3, 3, 3), TileEntityStructureController.StructureType.BURROW),
		OCEAN("ocean", new BlockPos(3, 5, 3), TileEntityStructureController.StructureType.OCEAN),
		DESERT("desert", new BlockPos(7, 3, 7), TileEntityStructureController.StructureType.DESERT),
		SNOW("snow", new BlockPos(8, 3, 6), TileEntityStructureController.StructureType.SNOW),
		BIOME_FRAGMENT("biome_fragment", new BlockPos(7, 2, 7),
				TileEntityStructureController.StructureType.BIOME_FRAGMENT);

		private final Identifier template;
		private final BlockPos templateAnchor;
		private final TileEntityStructureController.StructureType controllerType;

		Type(String path, BlockPos templateAnchor,
				TileEntityStructureController.StructureType controllerType) {
			template = NBTStructureLoader.chromaTemplate("worldgen/overworld/" + path);
			this.templateAnchor = templateAnchor;
			this.controllerType = controllerType;
		}
	}

	private final Type type;
	private final boolean natural;

	public OverworldStructureFeature(Type type, boolean natural) {

		this.type = type;
		this.natural = natural;
	}

	@Override
	public boolean place(net.minecraft.world.level.WorldGenLevel world, net.minecraft.world.level.chunk.ChunkGenerator generator, net.minecraft.util.RandomSource random, net.minecraft.core.BlockPos origin) {
		if (!natural)
			return this.placeAt(world, random, origin) != null;
		return this.placeNatural(world, random, origin);
	}

	/**
	 * V33a {@code DungeonGenerator.checkChunk}: only a chunk the zone planner marked PLANNED generates.
	 * It makes {@code getStructureTriesPerChunk} attempts at random columns of its own chunk
	 * ({@code tryGenerateInChunk}), then the same in each surrounding chunk ({@code tryGenerateInChunksAround}),
	 * and records SUCCESS on the chunk that took it or FAILURE on its own.
	 *
	 * <p>The saved post-population queue runs the complete 48-96 block search on the server, after
	 * asynchronously loading its footprint. The feature never escapes its WorldGenRegion.
	 */
	private boolean placeNatural(WorldGenLevel world, RandomSource random, BlockPos origin) {
		net.minecraft.server.level.ServerLevel level = world.getLevel();
		if (!DungeonZones.canGenerateIn(level))
			return false;
		int cx = origin.getX() >> 4;
		int cz = origin.getZ() >> 4;
		if (DungeonZones.isGennableChunk(level, cx << 4, cz << 4, type) != DungeonZones.Status.PLANNED)
			return false;
		PostPopulationWorldgen.get(level).enqueueDungeon(type,
				new net.minecraft.world.level.ChunkPos(cx, cz), random.nextLong());
		return true;
	}

	/** V33a {@code tryGenerateInChunk}: up to {@code tries} random columns, each checked for biome first. */
	net.minecraft.world.level.ChunkPos tryGenerateInChunk(WorldGenLevel world,
			net.minecraft.server.level.ServerLevel level, int cx, int cz, RandomSource random, int tries) {
		DungeonZones.Status previous = DungeonZones.Data.get(level).get(type, cx, cz);
		if (previous != DungeonZones.Status.SUCCESS)
			DungeonZones.markChunkStatus(level, cx, cz, type, DungeonZones.Status.GENERATING);
		for (int n = 0; n < tries; n++) {
			int x = (cx << 4) + random.nextInt(16);
			int z = (cz << 4) + random.nextInt(16);
			int y = world.getHeight(surfaceHeightmap(world), x, z);
			if (!DungeonZones.isValidBiome(type, biomeAt(world, new BlockPos(x, y, z))))
				continue;
			BlockPos controller = this.placeAt(world, random, new BlockPos(x, world.getMinY(), z));
			if (controller != null) {
				if (previous != DungeonZones.Status.SUCCESS)
					DungeonZones.markChunkStatus(level, cx, cz, type, DungeonZones.Status.INERT_GEN);
				return net.minecraft.world.level.ChunkPos.containing(controller);
			}
		}
		// V33a finalizes every searched chunk, not just the zone's flagged origin. Leaving
		// failed retries GENERATING makes the distribution map advertise nonexistent temples.
		if (previous != DungeonZones.Status.SUCCESS)
			DungeonZones.markChunkStatus(level, cx, cz, type, DungeonZones.Status.INERT_GEN);
		return null;
	}

	/**
	 * Keep vanilla's fiddled block-biome selection without asking a bounded WorldGenRegion for
	 * a neighbouring chunk. Its biome manager can sample a quart just across the edge of the
	 * feature step's available chunks, even when the position itself is inside the write window.
	 */
	private static Holder<Biome> biomeAt(WorldGenLevel world, BlockPos pos) {
		if (world instanceof net.minecraft.server.level.WorldGenRegion)
			return world.getBiomeManager().withDifferentSource(world::getUncachedNoiseBiome).getBiome(pos);
		return world.getBiome(pos);
	}

	/** Full chunks update live heightmaps; generation-only maps may be absent or stale after decoration. */
    private static Heightmap.Types surfaceHeightmap(WorldGenLevel world) {
        return world instanceof net.minecraft.server.level.WorldGenRegion
                ? Heightmap.Types.WORLD_SURFACE_WG : Heightmap.Types.WORLD_SURFACE;
    }

	/** One placement attempt with {@code origin}'s column; returns the controller position, or null. */
	private BlockPos placeAt(WorldGenLevel world, RandomSource random, BlockPos originIn) {
		// A structure or its construction checks can reach past the chunks this step may access.
		// What slides is the origin, before anything is derived from it:
		// naturalControllerPosition reads the heightmap at the origin's own column and validNaturalSite
		// judges the site it returns, so moving the structure afterwards would place it at a depth
		// taken from a different column, on ground that was never approved. Sliding first also keeps
		// the random draw for the Cavern's depth to a single call.
		net.minecraft.core.Vec3i controllerOffset = natural ? naturalControllerOffset(type)
				: net.minecraft.core.Vec3i.ZERO;
		BlockPos fitAnchor = NBTStructureLoader.fitToWriteWindow(world, originIn.offset(controllerOffset),
				this.writeFootprint(world));
		BlockPos origin = fitAnchor.subtract(controllerOffset);
		BlockPos controllerPos = natural ? naturalControllerPosition(world, origin, random) : origin;
		if (natural && !validNaturalSite(world, controllerPos))
			return null;

		List<BlockPos> placed = NBTStructureLoader.place(world, type.template, controllerPos,
				type.templateAnchor, state -> state, 2);
		CrystalElement color = CrystalElement.WHITE;
		boolean furnaceRoom = false;
		boolean lootRoom = false;
		if (type == Type.CAVERN)
			carveCavernTunnel(world, controllerPos);
		else if (type == Type.BURROW) {
			color = CrystalElement.elements[random.nextInt(CrystalElement.elements.length)];
			world.setBlock(controllerPos.offset(0, -2, 0),
					ChromaBlocks.crystalLamp(color)
							.get().defaultBlockState(), 2);
			// Natural Burrows retain V33a's one-half furnace roll followed by its one-half cache roll.
			// The command feature is the inspection form: it deliberately includes both optional rooms
			// and does not require the surrounding host terrain, so `/place feature chromaticraft:burrow`
			// exposes the Heat Lamps, furnaces, hidden key and UUID-locked cache in one placement.
			if (!natural || random.nextInt(2) == 0) {
				furnaceRoom = placeBurrowFurnaceRoom(world, controllerPos, random, natural);
				if (furnaceRoom && (!natural || random.nextInt(2) == 0))
					lootRoom = placeBurrowLootRoom(world, controllerPos, random, natural);
			}
		}
		else if (type == Type.OCEAN)
			placeOceanPit(world, controllerPos);

		world.setBlock(controllerPos, ChromaBlocks.STRUCTURE_CONTROLLER.get().defaultBlockState(), 3);
		if (!(world.getBlockEntity(controllerPos) instanceof TileEntityStructureController controller))
			return null;
		controller.initialize(type.controllerType, color, type == Type.DESERT ? 1 : 0, furnaceRoom, lootRoom);
		if (type == Type.BIOME_FRAGMENT)
			finishBiomeFragment(world, controllerPos, controller, random);
		if (type == Type.BIOME_FRAGMENT && natural)
			weatherNaturalBiomeFragment(world, controllerPos, placed, random);
		if (natural && type == Type.DESERT) finishNaturalDesert(world, placed, random);
		if (natural && (type == Type.OCEAN || type == Type.SNOW || type == Type.BURROW)) {
			weatherNaturalBlocks(world, placed, random, type == Type.BURROW);
			if (type == Type.BURROW) {
				if (furnaceRoom) weatherNaturalBlocks(world,
						NBTStructureLoader.templatePositions(world.getLevel(), BURROW_FURNACE, controllerPos, BURROW_ANNEX_ANCHOR), random, true);
				if (lootRoom) weatherNaturalBlocks(world,
						NBTStructureLoader.templatePositions(world.getLevel(), BURROW_LOOT, controllerPos, BURROW_ANNEX_ANCHOR), random, true);
			}
		}
		if (natural && type == Type.SNOW) finishNaturalSnow(world, controllerPos, placed);
		controller.setLootTable(type == Type.SNOW ? BuiltInLootTables.STRONGHOLD_CORRIDOR
				: BuiltInLootTables.STRONGHOLD_LIBRARY, random.nextLong());
		int fragments = type == Type.CAVERN ? 1 + random.nextInt(4) * (1 + random.nextInt(2))
				: type == Type.BURROW ? 1 + random.nextInt(3) : 1 + random.nextInt(2);
		controller.addReward(new ItemStack(ChromaItems.INFO_FRAGMENT.get(), fragments));

		for (BlockPos pos : placed) {
			if (world.getBlockEntity(pos) instanceof TileEntityLootChest chest) {
				chest.setLootTable(type == Type.OCEAN ? BuiltInLootTables.JUNGLE_TEMPLE
						: type == Type.DESERT ? BuiltInLootTables.DESERT_PYRAMID
						: type == Type.SNOW ? BuiltInLootTables.STRONGHOLD_CORRIDOR
						: type == Type.BIOME_FRAGMENT ? BuiltInLootTables.VILLAGE_WEAPONSMITH
						: BuiltInLootTables.SIMPLE_DUNGEON, random.nextLong());
				chest.addProgress(type == Type.BURROW ? ProgressStage.BURROW
						: type == Type.OCEAN ? ProgressStage.OCEAN
						: type == Type.DESERT ? ProgressStage.DESERTSTRUCT
						: type == Type.SNOW ? ProgressStage.SNOWSTRUCT
						: type == Type.BIOME_FRAGMENT ? ProgressStage.BIOMESTRUCT : ProgressStage.CAVERN);
				if (type == Type.BIOME_FRAGMENT)
					addBiomeFragmentToChest(chest, controllerPos, pos, random);
			}
		}
		if (type == Type.OCEAN) {
			// V33a widens only the two entrance chests after regeneration.
			for (BlockPos relative : List.of(new BlockPos(-2, -1, 0), new BlockPos(0, -1, -1)))
				if (world.getBlockEntity(controllerPos.offset(relative)) instanceof TileEntityLootChest chest)
					chest.setMaxReach(2.5);
		}
		return controllerPos;
	}

	/**
	 * Every column this placement reads or writes, relative to the controller, fitted as one before any
	 * terrain is probed. Cells within {@link NBTStructureLoader#NEIGHBOUR_MARGIN} of it (neighbour-shape
	 * resolution, the Burrow's six-neighbour host test, the Ocean's flooded-end probe at +28) are covered
	 * by the margin the fit already keeps.
	 */
	private NBTStructureLoader.HorizontalFootprint writeFootprint(WorldGenLevel world) {
		var footprint = type == Type.BURROW
				// Both optional rooms are built from the same controller. Their template footprints extend
				// beyond the core, including the UUID-door cells in the loot room. Reserve their entire
				// union before site checks, regardless of which room rolls later succeed.
				? NBTStructureLoader.horizontalFootprint(world.getLevel(),
						new NBTStructureLoader.TemplateFootprint(type.template, type.templateAnchor),
						new NBTStructureLoader.TemplateFootprint(BURROW_FURNACE, BURROW_ANNEX_ANCHOR),
						new NBTStructureLoader.TemplateFootprint(BURROW_LOOT, BURROW_ANNEX_ANCHOR))
				: NBTStructureLoader.horizontalFootprint(world.getLevel(),
						new NBTStructureLoader.TemplateFootprint(type.template, type.templateAnchor));
		if (!natural)
			return footprint;
		return switch (type) {
			// Cavern site validation and the exit tunnel both read through controller + 17 X.
			case CAVERN -> footprint.union(new NBTStructureLoader.HorizontalFootprint(0, 17, 0, 0));
			// weatherNaturalBiomeFragment clears logs and regrades the rim out to nine blocks each way.
			case BIOME_FRAGMENT -> footprint.union(new NBTStructureLoader.HorizontalFootprint(-9, 9, -9, 9));
			default -> footprint;
		};
	}

	private static void placeOceanPit(WorldGenLevel world, BlockPos controllerPos) {
		BlockPos anchor = new BlockPos(3, 0, 3);
		int exit = oceanCaveDepth(world, controllerPos);
		// Command placement retains a short inspection shaft if there is no validated cave.
		int limit = exit >= 0 ? exit : Math.min(32, controllerPos.getY() - world.getMinY());
		for (int depth = 3; depth < limit; depth++) {
			BlockPos sliceCenter = controllerPos.below(depth);
			NBTStructureLoader.place(world, OCEAN_PIT_SLICE, sliceCenter, anchor, state -> state, 2);
		}
		BlockState cloak = ChromaBlocks.shielding(reika.chromaticraft.registry.ChromaShieldTypes.CLOAK)
				.get().defaultBlockState().setValue(
						reika.chromaticraft.block.worldgen26.BlockStructureShield.REINFORCED, true);
		for (int x = -1; x <= 1; x++)
			for (int z = -1; z <= 1; z++)
				world.setBlock(controllerPos.offset(x, -3, z), cloak, 2);
	}

	private static FurnaceOre ore(String name, int weight, int maximum) {
		return new FurnaceOre(TagKey.create(Registries.ITEM,
				Identifier.fromNamespaceAndPath("c", "ores/" + name)), weight, maximum);
	}

	/** V33a's first optional Burrow roll and its two furnace callbacks. */
	private static boolean placeBurrowFurnaceRoom(WorldGenLevel world, BlockPos controllerPos,
			RandomSource random, boolean validateHost) {
		boolean valid = !validateHost || NBTStructureLoader.canPlace(world, BURROW_FURNACE, controllerPos,
				BURROW_ANNEX_ANCHOR, state -> state,
				(pos, authored) -> authored.is(Blocks.STRUCTURE_VOID) || !world.getBlockState(pos).isAir());
		if (!valid) return false;
		NBTStructureLoader.place(world, BURROW_FURNACE, controllerPos, BURROW_ANNEX_ANCHOR,
				state -> state, 2);
		for (BlockPos relative : List.of(new BlockPos(4, 0, 2), new BlockPos(5, 0, 2))) {
			BlockPos furnacePos = controllerPos.offset(relative);
			BlockPos lampPos = furnacePos.above();
			world.setBlock(lampPos, ChromaBlocks.HEAT_LAMP.get().defaultBlockState()
					.setValue(BlockHeatLamp.FACING, net.minecraft.core.Direction.UP), 2);
			if (world.getBlockEntity(lampPos) instanceof TileEntityHeatLamp lamp)
				lamp.setTemperature(50 + random.nextInt(111));
			if (world.getBlockEntity(furnacePos) instanceof AbstractFurnaceBlockEntity furnace)
				furnace.setItem(0, randomFurnaceOre(world, random));
		}
		return true;
	}

	private static ItemStack randomFurnaceOre(WorldGenLevel world, RandomSource random) {
		var registry = world.registryAccess().lookupOrThrow(Registries.ITEM);
		java.util.ArrayList<FurnaceOre> available = new java.util.ArrayList<>();
		int totalWeight = 0;
		for (FurnaceOre ore : FURNACE_ORES) {
			if (registry.get(ore.tag()).filter(set -> set.size() > 0).isPresent()) {
				available.add(ore);
				totalWeight += ore.weight();
			}
		}
		if (available.isEmpty()) return new ItemStack(net.minecraft.world.item.Items.IRON_ORE, 4);
		int roll = random.nextInt(totalWeight);
		FurnaceOre selected = available.getFirst();
		for (FurnaceOre ore : available) {
			roll -= ore.weight();
			if (roll < 0) { selected = ore; break; }
		}
		var entries = registry.get(selected.tag()).orElseThrow().stream().toList();
		Item item = entries.get(random.nextInt(entries.size())).value();
		int maximum = Math.min(item.getDefaultMaxStackSize(), selected.maximum());
		return new ItemStack(item, 4 + random.nextInt(Math.max(1, maximum - 3)));
	}

	/** V33a's second optional Burrow roll: UUID doors, hidden key chest, and paired cache chests. */
	private static boolean placeBurrowLootRoom(WorldGenLevel world, BlockPos controllerPos,
			RandomSource random, boolean validateHost) {
		boolean valid = !validateHost || NBTStructureLoader.canPlace(world, BURROW_LOOT, controllerPos,
				BURROW_ANNEX_ANCHOR, state -> state,
				(pos, authored) -> authored.is(Blocks.STRUCTURE_VOID) || !world.getBlockState(pos).isAir());
		if (!valid) return false;
		NBTStructureLoader.place(world, BURROW_LOOT, controllerPos, BURROW_ANNEX_ANCHOR,
				state -> state, 2);

		UUID doorId = UUID.randomUUID();
		List<BlockPos> doorOffsets = List.of(new BlockPos(5, -2, -2), new BlockPos(5, -1, -2),
				new BlockPos(6, -2, -2), new BlockPos(6, -1, -2));
		for (BlockPos relative : doorOffsets) {
			BlockPos doorPos = controllerPos.offset(relative);
			world.setBlock(doorPos, BlockChromaDoor.state(false, false, true, true), 2);
		}
		for (BlockPos relative : doorOffsets) {
			BlockPos doorPos = controllerPos.offset(relative);
			world.setBlock(doorPos, BlockChromaDoor.withConnections(
					world.getBlockState(doorPos), world, doorPos), 2);
		}
		BlockPos firstDoor = controllerPos.offset(5, -2, -2);
		if (world.getBlockEntity(firstDoor) instanceof TileEntityChromaDoor door)
			door.bindUUID(null, doorId, false);

		BlockPos keyChestPos = controllerPos.offset(3, 1, 1);
		if (world.getBlockEntity(keyChestPos) instanceof ChestBlockEntity keyChest) {
			ItemStack key = new ItemStack(ChromaItems.DOOR_KEY.get());
			((ItemDoorKey)key.getItem()).setID(key, doorId);
			keyChest.setItem(random.nextInt(keyChest.getContainerSize()), key);
		}
		for (BlockPos relative : List.of(new BlockPos(6, -2, 0), new BlockPos(5, -2, 0))) {
			if (world.getBlockEntity(controllerPos.offset(relative)) instanceof TileEntityLootChest chest) {
				chest.setLootTable(BURROW_CACHE_LOOT, random.nextLong());
				chest.addProgress(ProgressStage.BURROW);
			}
		}
		return true;
	}

	/**
	 * The horizontal shift {@link #naturalControllerPosition} applies for each type, which is fixed
	 * per type and carries no randomness. It is needed on its own so the write-window fit can be
	 * applied to the origin while still accounting for where the template will actually land.
	 */
	private static net.minecraft.core.Vec3i naturalControllerOffset(Type type) {
		return switch (type) {
			case DESERT -> net.minecraft.core.Vec3i.ZERO;
			case SNOW -> new net.minecraft.core.Vec3i(8, 0, 6);
			// V33a calls BurrowStructure with the grass surface coordinate; its controller is (-5,-8,-2).
			case BURROW -> new net.minecraft.core.Vec3i(-5, 0, -2);
			case CAVERN, OCEAN, BIOME_FRAGMENT -> net.minecraft.core.Vec3i.ZERO;
		};
	}

	/** V33a getTop: ground/seabed with logs and leaves ignored after population. */
	static int terrainTop(WorldGenLevel world, int x, int z) {
		int y = world.getHeight(world instanceof net.minecraft.server.level.WorldGenRegion
				? Heightmap.Types.OCEAN_FLOOR_WG : Heightmap.Types.OCEAN_FLOOR, x, z) - 1;
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, y, z);
		while (y >= world.getMinY()) {
			BlockState state = world.getBlockState(pos.setY(y));
			if (!state.isAir() && !state.is(BlockTags.LOGS) && !state.is(BlockTags.LEAVES)) break;
			y--;
		}
		return y + 1;
	}

	private BlockPos naturalControllerPosition(WorldGenLevel world, BlockPos origin, RandomSource random) {
		if (type == Type.CAVERN)
            return new BlockPos(origin.getX(), reika.dragonapi.libraries.level.WorldgenHeight.randomBetween(world, random,
                    reika.dragonapi.libraries.level.WorldgenHeight.undergroundY(world, 10),
                    reika.dragonapi.libraries.level.WorldgenHeight.undergroundY(world, 49)), origin.getZ());
		if (type == Type.OCEAN) {
			// V33a's getTopSolidOrLiquidBlock ignored water; use the seabed rather than the sea surface.
			int top = terrainTop(world, origin.getX(), origin.getZ());
			return new BlockPos(origin.getX(), top - 3, origin.getZ());
		}
		if (type == Type.DESERT) {
			int top = terrainTop(world, origin.getX(), origin.getZ());
			// V33a subtracts (7,3,7) for the template, then restores it for the controller.
			return new BlockPos(origin.getX(), top - 8, origin.getZ());
		}
		if (type == Type.SNOW) {
			int top = terrainTop(world, origin.getX(), origin.getZ());
			return new BlockPos(origin.getX() + 8, top - 4, origin.getZ() + 6);
		}
		if (type == Type.BIOME_FRAGMENT) {
			int top = terrainTop(world, origin.getX(), origin.getZ());
			return sinkBiomeFragment(world, new BlockPos(origin.getX(), top - 5, origin.getZ()));
		}
		// V33a calls BurrowStructure with the grass surface coordinate; its controller is (-5,-8,-2).
		int surfaceY = terrainTop(world, origin.getX(), origin.getZ()) - 1;
		return new BlockPos(origin.getX() - 5, surfaceY - 8, origin.getZ() - 2);
	}

	/** V33a {@code FilledBlockArray.sink}: descend while every authored cell has a soft host below. */
	private BlockPos sinkBiomeFragment(WorldGenLevel world, BlockPos initial) {
		BlockPos anchor = initial;
		while (anchor.getY() - 3 > world.getMinY()) {
			BlockPos candidate = anchor;
			boolean canSink = NBTStructureLoader.canPlace(world, type.template, candidate,
					type.templateAnchor, state -> state, (pos, authored) -> authored.is(Blocks.STRUCTURE_VOID)
						|| biomeSinkHost(world.getBlockState(pos.below())));
			if (!canSink) break;
			anchor = anchor.below();
		}
		return anchor;
	}

	private static boolean biomeSinkHost(BlockState state) {
		return state.isAir() || state.canBeReplaced() || state.is(BlockTags.LOGS)
				|| state.is(BlockTags.LEAVES) || !state.getFluidState().isEmpty()
				|| state.is(Blocks.PUMPKIN) || state.is(Blocks.CARVED_PUMPKIN)
				|| state.is(Blocks.JACK_O_LANTERN) || state.is(Blocks.MELON);
	}

	/** V33a MOSSIFY + ADJTREES2 + CLEANENTRANCE post-placement passes. */
	private static void weatherNaturalBiomeFragment(WorldGenLevel world, BlockPos root,
			List<BlockPos> placed, RandomSource random) {
		BlockState stone = ChromaBlocks.shielding(reika.chromaticraft.registry.ChromaShieldTypes.STONE)
				.get().defaultBlockState();
		BlockState moss = ChromaBlocks.shielding(reika.chromaticraft.registry.ChromaShieldTypes.MOSS)
				.get().defaultBlockState();
		for (BlockPos pos : placed) {
			BlockState current = world.getBlockState(pos);
			if (current.is(stone.getBlock())) {
				int dy = pos.getY() - (root.getY() - 2);
				if (random.nextInt(Math.max(1, dy * 2 - 2)) == 0)
					world.setBlock(pos, moss.setValue(
							reika.chromaticraft.block.worldgen26.BlockStructureShield.REINFORCED,
							current.getValue(reika.chromaticraft.block.worldgen26.BlockStructureShield.REINFORCED)), 2);
			}
		}

		int minY = root.getY() - 2;
		int maxY = root.getY() + 11;
		for (int x = root.getX() - 8; x <= root.getX() + 8; x++)
			for (int z = root.getZ() - 8; z <= root.getZ() + 8; z++) {
				boolean withinWalls = Math.abs(x - root.getX()) <= 7 && Math.abs(z - root.getZ()) <= 7;
				for (int y = minY; y <= maxY + 2; y++) {
					BlockPos pos = new BlockPos(x, y, z);
					BlockState state = world.getBlockState(pos);
					if (state.is(BlockTags.LOGS) || withinWalls && state.is(BlockTags.LEAVES))
						world.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
				}
			}

		for (int x = root.getX() - 9; x <= root.getX() + 9; x++)
			for (int z = root.getZ() - 9; z <= root.getZ() + 9; z++) {
				boolean outside = Math.abs(x - root.getX()) > 7 || Math.abs(z - root.getZ()) > 7;
				if (!outside) continue;
				boolean nearWall = Math.abs(x - root.getX()) <= 8 && Math.abs(z - root.getZ()) <= 8;
				BlockState top = world.getBlockState(new BlockPos(x, maxY + 1, z));
				BlockState roof = world.getBlockState(new BlockPos(x, maxY, z));
				if (top.is(BlockTags.LOGS) || top.is(BlockTags.LEAVES)
						|| roof.is(BlockTags.LOGS) || roof.is(BlockTags.LEAVES)) continue;
				int floor = maxY - (nearWall ? 5 : 3);
				for (int y = maxY; y >= floor; y--) {
					BlockPos pos = new BlockPos(x, y, z);
					BlockState state = world.getBlockState(pos);
					if (!(state.canBeReplaced() || state.is(Blocks.DIRT)
							|| state.is(Blocks.GRASS_BLOCK))) break;
					world.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
					BlockPos below = pos.below();
					if (!world.getBlockState(below).canBeReplaced())
						world.setBlock(below, Blocks.GRASS_BLOCK.defaultBlockState(), 2);
				}
			}
	}

	private boolean validNaturalSite(WorldGenLevel world, BlockPos controllerPos) {
		if (type == Type.BIOME_FRAGMENT)
			return validNaturalBiomeFragmentSite(world, controllerPos);
		if (type == Type.OCEAN)
			return validNaturalOceanSite(world, controllerPos);
		if (type == Type.DESERT)
			return validNaturalDesertSite(world, controllerPos);
		if (type == Type.SNOW)
			return validNaturalSnowSite(world, controllerPos);
		if (type == Type.BURROW)
			return validNaturalBurrowSite(world, controllerPos);
		boolean tunnelExit = false;
		for (int x = 6; x < 18; x++) {
			BlockPos upper = controllerPos.offset(x, 0, 0);
			if (world.getBlockState(upper).isAir() && world.getBlockState(upper.below()).isAir()) {
				tunnelExit = true;
				break;
			}
		}
		if (!tunnelExit)
			return false;
		return NBTStructureLoader.canPlace(world, type.template, controllerPos, type.templateAnchor,
				state -> state, (pos, authored) -> authored.is(Blocks.STRUCTURE_VOID)
						|| isSolidCaveHost(world, pos) && isSolidCaveHost(world, pos.above()));
	}

	private boolean validNaturalBiomeFragmentSite(WorldGenLevel world, BlockPos controllerPos) {
		var biome = biomeAt(world, controllerPos);
		if (biome.is(reika.chromaticraft.world.biome.ChromaBiomes.RAINBOW_STREAM)
				|| !(biome.is(reika.chromaticraft.world.biome.ChromaBiomes.RAINBOW_FOREST)
				|| biome.is(reika.chromaticraft.world.biome.ChromaBiomes.ENDER_FOREST)
				|| biome.is(reika.chromaticraft.world.biome.ChromaBiomes.LUMINOUS_CLIFFS)
				|| biome.is(reika.chromaticraft.world.biome.ChromaBiomes.LUMINOUS_CLIFFS_SHORES)))
			return false;
		if ((biome.is(reika.chromaticraft.world.biome.ChromaBiomes.LUMINOUS_CLIFFS)
				|| biome.is(reika.chromaticraft.world.biome.ChromaBiomes.LUMINOUS_CLIFFS_SHORES))
				&& controllerPos.getY() > GlowingCliffsColumnShaper.MAX_MIDDLE_TOP_Y + 2)
			return false;
		if (!levelGround(world, List.of(controllerPos.offset(-7, 0, -7), controllerPos.offset(7, 0, -7),
				controllerPos.offset(-7, 0, 7), controllerPos.offset(7, 0, 7)), 4)) return false;
		return NBTStructureLoader.canPlace(world, type.template, controllerPos, type.templateAnchor,
				state -> state, (pos, authored) -> authored.is(Blocks.STRUCTURE_VOID)
						|| !(world.getBlockState(pos).getBlock()
								instanceof reika.chromaticraft.block.worldgen26.BlockStructureShield));
	}

	private static void finishBiomeFragment(WorldGenLevel world, BlockPos root,
			TileEntityStructureController controller, RandomSource random) {
		int[][] runes = {{5,5,6},{6,5,5},{-5,5,6},{-6,5,5},
				{5,5,-6},{6,5,-5},{-5,5,-6},{-6,5,-5}};
		for (int i = 0; i < runes.length; i++) {
			int[] at = runes[i];
			world.setBlock(root.offset(at[0], at[1], at[2]),
					ChromaBlocks.rune(controller.getBiomeRuneColor(i)).get().defaultBlockState(), 2);
		}
		int[][] crystals = {{-4,3,1},{-5,3,1},{-1,3,-4},{-1,3,-5},
				{4,3,-1},{5,3,-1},{1,3,4},{1,3,5}};
		for (int i = 0; i < crystals.length; i++) {
			int[] at = crystals[i];
			world.setBlock(root.offset(at[0], at[1], at[2]),
					ChromaBlocks.crystalLamp(controller.getBiomeCrystalColor(i)).get().defaultBlockState(), 2);
		}
		BlockState liquid = biomeFragmentLiquid(world, root);
		for (int x = -1; x <= 1; x++)
			for (int z = -1; z <= 1; z++) world.setBlock(root.offset(x, -1, z), liquid, 2);
		for (int[] direction : new int[][] {{0,-1,-1,0},{0,1,1,0},{-1,0,0,1},{1,0,0,-1}})
			for (int a = 3; a <= 5; a++)
				for (int b = 4; b <= 5; b++) {
					if (b == 5 && a == 4) continue;
					world.setBlock(root.offset(direction[0] * b + direction[2] * a, 1,
							direction[1] * b + direction[3] * a), liquid, 2);
				}
		for (BlockPos relative : List.of(new BlockPos(-4, 2, -5), new BlockPos(5, 2, -4),
				new BlockPos(4, 2, 5), new BlockPos(-5, 2, 4)))
			if (world.getBlockEntity(root.offset(relative)) instanceof TileEntityLootChest chest)
				chest.setStructureLocked(true);
		controller.bindBiomePuzzleBlocks();
	}

	private static BlockState biomeFragmentLiquid(WorldGenLevel world, BlockPos root) {
		var biome = biomeAt(world, root);
		if (biome.is(reika.chromaticraft.world.biome.ChromaBiomes.RAINBOW_FOREST))
			return ChromaBlocks.CHROMA.get().defaultBlockState();
		if (biome.is(reika.chromaticraft.world.biome.ChromaBiomes.LUMINOUS_CLIFFS)
				|| biome.is(reika.chromaticraft.world.biome.ChromaBiomes.LUMINOUS_CLIFFS_SHORES))
			return ChromaBlocks.LUMA.get().defaultBlockState();
		if (biome.is(reika.chromaticraft.world.biome.ChromaBiomes.ENDER_FOREST)) {
			TagKey<net.minecraft.world.level.material.Fluid> ender = TagKey.create(Registries.FLUID,
					Identifier.fromNamespaceAndPath("c", "ender"));
			var fluids = world.registryAccess().lookupOrThrow(Registries.FLUID).get(ender);
			if (fluids.isPresent() && fluids.get().size() > 0)
				return fluids.get().stream().findFirst().orElseThrow().value()
						.defaultFluidState().createLegacyBlock();
		}
		return Blocks.LAVA.defaultBlockState();
	}

	private static void addBiomeFragmentToChest(TileEntityLootChest chest, BlockPos root,
			BlockPos chestPos, RandomSource random) {
		boolean lower = chestPos.getY() - root.getY() <= 4;
		int fragments;
		if (lower) {
			int roll = random.nextInt(12);
			fragments = roll == 11 ? 4 : roll > 7 ? 3 : roll > 2 ? 2 : 1;
		}
		else fragments = random.nextInt(3) > 0 ? 2 : 1;
		chest.unpackLootTable(null);
		ItemStack stack = new ItemStack(ChromaItems.INFO_FRAGMENT.get(), fragments);
		for (int slot = 0; slot < chest.getContainerSize(); slot++)
			if (chest.getItem(slot).isEmpty()) {
				chest.setItem(slot, stack);
				break;
			}
	}

	private boolean validNaturalSnowSite(WorldGenLevel world, BlockPos controllerPos) {
		BlockPos sourceOrigin = controllerPos.offset(-8, 3, -6);
		List<BlockPos> corners = List.of(sourceOrigin, sourceOrigin.offset(16, 0, 0),
				sourceOrigin.offset(0, 0, 16), sourceOrigin.offset(16, 0, 16));
		if (!levelGround(world, corners, 5)) return false;
		return NBTStructureLoader.canPlace(world, type.template, controllerPos, type.templateAnchor,
				state -> state, (pos, authored) -> authored.is(Blocks.STRUCTURE_VOID)
						|| !(world.getBlockState(pos).getBlock()
								instanceof reika.chromaticraft.block.worldgen26.BlockStructureShield));
	}

	private static boolean levelGround(WorldGenLevel world, List<BlockPos> corners, int supportDepth) {
		int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
		var biome = biomeAt(world, corners.getFirst());
		for (BlockPos corner : corners) {
			int height = terrainTop(world, corner.getX(), corner.getZ());
			min = Math.min(min, height); max = Math.max(max, height);
			if (!biomeAt(world, corner).equals(biome)) return false;
			for (int depth = 1; depth <= supportDepth; depth++) {
				BlockPos support = new BlockPos(corner.getX(), height - depth, corner.getZ());
				if (world.getBlockState(support).isAir() || !world.getFluidState(support).isEmpty()) return false;
			}
		}
		return max - min <= 2;
	}

	private static void finishNaturalSnow(WorldGenLevel world, BlockPos controllerPos, List<BlockPos> cells) {
		// V33a ADJTREES removes a connected log/leaf group touching an authored cell, bounded
		// to twelve blocks from the touching log so the entrance cannot be buried by a tree.
		for (BlockPos cell : cells) for (net.minecraft.core.Direction dir : net.minecraft.core.Direction.values()) {
			BlockPos start = cell.relative(dir);
			if (!world.getBlockState(start).is(BlockTags.LOGS)) continue;
			java.util.ArrayDeque<BlockPos> pending = new java.util.ArrayDeque<>();
			java.util.HashSet<BlockPos> visited = new java.util.HashSet<>();
			pending.add(start);
			while (!pending.isEmpty()) {
				BlockPos at = pending.removeFirst();
				if (!visited.add(at) || Math.abs(at.getX() - start.getX()) > 12
						|| Math.abs(at.getY() - start.getY()) > 12 || Math.abs(at.getZ() - start.getZ()) > 12) continue;
				BlockState state = world.getBlockState(at);
				if (!state.is(BlockTags.LOGS) && !state.is(BlockTags.LEAVES)) continue;
				world.setBlock(at, Blocks.AIR.defaultBlockState(), 2);
				for (net.minecraft.core.Direction side : net.minecraft.core.Direction.values()) pending.add(at.relative(side));
			}
		}
		var moss = ChromaBlocks.shielding(reika.chromaticraft.registry.ChromaShieldTypes.MOSS).get();
		for (BlockPos cell : cells) {
			if (!world.getBlockState(cell).is(moss) || world.getBlockState(cell.above()).is(moss)) continue;
			for (int rise = 1; rise < 5; rise++) for (int dx = -rise; dx <= rise; dx++) for (int dz = -rise; dz <= rise; dz++) {
				BlockPos at = cell.offset(dx, rise, dz);
				BlockState state = world.getBlockState(at);
				if (state.is(Blocks.GRASS_BLOCK) || state.is(BlockTags.DIRT) || state.is(Blocks.STONE)
						|| state.is(Blocks.SNOW) || state.canBeReplaced() && state.getFluidState().isEmpty())
					world.setBlock(at, Blocks.AIR.defaultBlockState(), 2);
			}
		}
		BlockPos min = controllerPos.offset(-8, -3, -6);
		// V33a addMissingSupport starts below the lowest authored cell in each column.
		// Supporting every shield block also treats room ceilings as foundations and
		// fills the authored interior air with stone.
		Map<BlockPos, Integer> bottoms = new HashMap<>();
		for (BlockPos cell : cells)
			bottoms.merge(new BlockPos(cell.getX(), 0, cell.getZ()), cell.getY(), Math::min);
		for (Map.Entry<BlockPos, Integer> column : bottoms.entrySet())
			for (int depth = 1; depth <= 5; depth++) {
				BlockPos below = new BlockPos(column.getKey().getX(), column.getValue() - 1 - depth,
						column.getKey().getZ());
				if (reika.dragonapi.libraries.level.ReikaWorldHelper.softBlocks(world, below))
					world.setBlock(below, Blocks.STONE.defaultBlockState(), 2);
			}
		for (int x = -4; x <= 20; x++) for (int z = -4; z <= 20; z++) {
			BlockPos top = new BlockPos(min.getX() + x, terrainTop(world, min.getX() + x, min.getZ() + z), min.getZ() + z);
			if (world.getBlockState(top).isAir() && Blocks.SNOW.defaultBlockState().canSurvive(world, top))
				world.setBlock(top, Blocks.SNOW.defaultBlockState(), 2);
		}
	}

	private boolean validNaturalDesertSite(WorldGenLevel world, BlockPos controllerPos) {
		BlockPos sourceOrigin = controllerPos.offset(-7, -3, -7);
		int surface = terrainTop(world, controllerPos.getX(), controllerPos.getZ()) - 1;
		if (!world.getBlockState(new BlockPos(controllerPos.getX(), surface, controllerPos.getZ())).is(Blocks.SAND))
			return false;
		TagKey<net.minecraft.world.level.biome.Biome> sandy = TagKey.create(Registries.BIOME,
				Identifier.fromNamespaceAndPath("c", "is_sandy"));
		for (BlockPos corner : List.of(sourceOrigin, sourceOrigin.offset(14, 0, 0),
				sourceOrigin.offset(0, 0, 15), sourceOrigin.offset(14, 0, 15))) {
			Holder<Biome> biome = biomeAt(world, corner);
			if (!biome.is(sandy) || biome.is(BiomeTags.IS_BADLANDS))
				return false;
			if (world.getBlockState(corner.above(3)).isAir()) return false;
		}
		return NBTStructureLoader.canPlace(world, type.template, controllerPos, type.templateAnchor,
				state -> state, (pos, authored) -> authored.is(Blocks.STRUCTURE_VOID)
						|| !(world.getBlockState(pos).getBlock()
								instanceof reika.chromaticraft.block.worldgen26.BlockStructureShield));
	}

	private boolean validNaturalOceanSite(WorldGenLevel world, BlockPos controllerPos) {
		if (controllerPos.getY() - 5 < world.getMinY()) return false;
		if (!world.getFluidState(controllerPos.above(3)).is(net.minecraft.tags.FluidTags.WATER)
				|| !world.getFluidState(controllerPos.above(8)).is(net.minecraft.tags.FluidTags.WATER))
			return false;
		// V33a requires both authored bounds to remain ocean and at least one flooded end cap.
		for (BlockPos corner : List.of(controllerPos.offset(-3, 0, -3), controllerPos.offset(27, 0, -3),
				controllerPos.offset(-3, 0, 27), controllerPos.offset(27, 0, 27)))
			if (!biomeAt(world, corner).is(BiomeTags.IS_OCEAN))
				return false;
		// The source's water probes sit five blocks above the actual template mouths. Keep
		// those probes, but also require a usable mouth so a seabed ridge cannot seal both exits.
		boolean openX = floodedOceanEnd(world, controllerPos.offset(28, 8, 0), false)
				&& floodedOceanEnd(world, controllerPos.offset(28, 3, 0), false);
		boolean openZ = floodedOceanEnd(world, controllerPos.offset(0, 8, 28), true)
				&& floodedOceanEnd(world, controllerPos.offset(0, 3, 28), true);
		if (!openX && !openZ)
			return false;
		if (!NBTStructureLoader.canPlace(world, type.template, controllerPos, type.templateAnchor,
				state -> state, (pos, authored) -> authored.is(Blocks.STRUCTURE_VOID)
						|| (!(world.getBlockState(pos).getBlock()
								instanceof reika.chromaticraft.block.worldgen26.BlockStructureShield)
							&& (pos.getY() < 55 || !world.getBlockState(pos).isAir())
							&& biomeAt(world, pos).is(BiomeTags.IS_OCEAN))))
			return false;
		// The original rejects the site unless the five-wide shaft reaches three consecutive cave-air
		// slices below it. This prevents an Ocean Temple from producing a blind bedrock chute.
		return oceanCaveDepth(world, controllerPos) >= 0;
	}

	private static int oceanCaveDepth(WorldGenLevel world, BlockPos controllerPos) {
		int consecutive = 0;
		for (int depth = 3; depth < controllerPos.getY() - world.getMinY(); depth++) {
			boolean allAir = true;
			BlockPos center = controllerPos.below(depth);
			for (int x = -2; x <= 2 && allAir; x++)
				for (int z = -2; z <= 2; z++)
					if (!world.getBlockState(center.offset(x, 0, z)).isAir()) {
						allAir = false;
						break;
					}
			if (allAir && depth > 6) {
				if (++consecutive >= 3) return depth - 2;
			}
			else consecutive = 0;
		}
		return -1;
	}

	private static boolean floodedOceanEnd(WorldGenLevel world, BlockPos center, boolean alongX) {
		for (int y = 0; y < 3; y++)
			for (int side = -1; side <= 1; side++)
				if (!world.getFluidState(center.offset(alongX ? side : 0, y, alongX ? 0 : side))
						.is(net.minecraft.tags.FluidTags.WATER))
					return false;
		return true;
	}

	private boolean validNaturalBurrowSite(WorldGenLevel world, BlockPos controllerPos) {
		BlockPos surface = controllerPos.offset(5, 8, 2);
		if (!world.getBlockState(surface).is(Blocks.GRASS_BLOCK))
			return false;
		// V33a surface-visibility column: eight high by five wide, rejecting trees and overhangs.
		for (int y = 1; y <= 8; y++)
			for (int z = -3; z <= 1; z++) {
				BlockPos pos = surface.offset(0, y, z);
				if (!world.getBlockState(pos).canBeReplaced() && !world.getBlockState(pos).isAir())
					return false;
			}
		// The three-cube lake test from DungeonGenerator.isValidBurrowLocation.
		for (BlockPos pos : BlockPos.betweenClosed(surface.offset(-1, -1, -1), surface.offset(1, 1, 1)))
			if (!world.getFluidState(pos).isEmpty())
				return false;
		return NBTStructureLoader.canPlace(world, type.template, controllerPos, type.templateAnchor,
				state -> state, (pos, authored) -> {
					if (world.getBlockState(pos).is(ChromaBlocks.CLIFF_STONE.get())) return false;
					if (authored.is(Blocks.STRUCTURE_VOID) || authored.isAir())
						return true;
					if (authored.is(Blocks.STONE)
							|| authored.getBlock() instanceof reika.chromaticraft.block.worldgen26.BlockStructureShield) {
						if (world.getBlockState(pos).isAir() || !world.getFluidState(pos).isEmpty())
							return false;
						for (net.minecraft.core.Direction direction : net.minecraft.core.Direction.values())
							if (world.getBlockState(pos.relative(direction)).isAir())
								return false;
					}
					return terrainTop(world, pos.getX(), pos.getZ())
							>= surface.getY() - 2;
				});
	}

	/** V33a MOSSIFY and GRASSDIRT, restricted to the authored cells. */
	private static void weatherNaturalBlocks(WorldGenLevel world, List<BlockPos> cells, RandomSource random, boolean burrow) {
		int minY = cells.stream().mapToInt(BlockPos::getY).min().orElse(world.getMinY());
		BlockState moss = ChromaBlocks.shielding(reika.chromaticraft.registry.ChromaShieldTypes.MOSS).get().defaultBlockState();
		var stone = ChromaBlocks.shielding(reika.chromaticraft.registry.ChromaShieldTypes.STONE).get();
		for (BlockPos pos : cells) {
			BlockState state = world.getBlockState(pos);
			if (state.is(stone)) {
				int count = Math.max(1, (pos.getY() - minY) * (burrow ? 1 : 2) - 2 + (burrow ? 5 : 0));
				if (random.nextInt(count) == 0) world.setBlock(pos, moss.setValue(
						reika.chromaticraft.block.worldgen26.BlockStructureShield.REINFORCED,
						state.getValue(reika.chromaticraft.block.worldgen26.BlockStructureShield.REINFORCED)), 2);
			} else if (state.is(Blocks.DIRT) && world.getMaxLocalRawBrightness(pos.above()) > 8)
				world.setBlock(pos, Blocks.GRASS_BLOCK.defaultBlockState(), 2);
		}
	}

	/** V33a's exposed sand erosion and sparse cactus pass over the Sandy Burrow. */
	private static void finishNaturalDesert(WorldGenLevel world, List<BlockPos> cells, RandomSource random) {
		int maxY = cells.stream().mapToInt(BlockPos::getY).max().orElse(world.getMinY());
		int minX = cells.stream().mapToInt(BlockPos::getX).min().orElseThrow();
		int maxX = cells.stream().mapToInt(BlockPos::getX).max().orElseThrow();
		int minZ = cells.stream().mapToInt(BlockPos::getZ).min().orElseThrow();
		int maxZ = cells.stream().mapToInt(BlockPos::getZ).max().orElseThrow();
		int midX = (minX + maxX) / 2, midZ = (minZ + maxZ) / 2;
		for (BlockPos pos : cells) {
			double chance = 20 + 30D * (Math.abs(pos.getX() - midX) + Math.abs(pos.getZ() - midZ)) / 7D;
			if (pos.getY() > maxY - 2 && world.getBlockState(pos.above()).isAir()
					&& world.getBlockState(pos.below()).is(Blocks.SAND) && random.nextDouble() * 100 < chance)
				world.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
		}
		for (int x = minX; x <= maxX; x++) for (int z = minZ; z <= maxZ; z++) {
			BlockPos top = new BlockPos(x, terrainTop(world, x, z), z);
			if (!world.getBlockState(top).isAir() || !world.getBlockState(top.below()).is(Blocks.SAND)
					|| random.nextDouble() >= 0.02) continue;
			boolean clear = true;
			for (net.minecraft.core.Direction dir : net.minecraft.core.Direction.Plane.HORIZONTAL)
				clear &= world.getBlockState(top.relative(dir)).isAir();
			if (!clear) continue;
			world.setBlock(top, Blocks.CACTUS.defaultBlockState(), 2);
			if (random.nextDouble() < 0.4) {
				world.setBlock(top.above(), Blocks.CACTUS.defaultBlockState(), 2);
				if (random.nextDouble() < 0.4) world.setBlock(top.above(2), Blocks.CACTUS.defaultBlockState(), 2);
			}
		}
	}

	private static boolean isSolidCaveHost(WorldGenLevel world, BlockPos pos) {
		return !world.getBlockState(pos).isAir() && world.getFluidState(pos).isEmpty();
	}

	private static void carveCavernTunnel(WorldGenLevel world, BlockPos controllerPos) {
		for (int x = 7; x < 18; x++) {
			BlockPos upper = controllerPos.offset(x, 0, 0);
			BlockPos lower = upper.below();
			if (world.getBlockState(upper).isAir() && world.getBlockState(lower).isAir())
				break;
			world.setBlock(upper, Blocks.AIR.defaultBlockState(), 2);
			world.setBlock(lower, Blocks.AIR.defaultBlockState(), 2);
		}
	}
}
