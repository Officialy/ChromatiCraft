package reika.chromaticraft.world;

import java.util.List;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;

import reika.chromaticraft.auxiliary.structure.NBTStructureLoader;
import reika.chromaticraft.magic.progression.ProgressStage;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.registry.ChromaItems;
import reika.chromaticraft.registry.CrystalElement;
import reika.chromaticraft.tileentity.TileEntityLootChest;
import reika.chromaticraft.tileentity.TileEntityStructureController;
import reika.chromaticraft.tileentity.TileEntityHeatLamp;
import reika.chromaticraft.block.BlockHeatLamp;
import reika.chromaticraft.block.BlockChromaDoor;
import reika.chromaticraft.item.ItemDoorKey;
import reika.chromaticraft.tileentity.TileEntityChromaDoor;

/** NBT-backed runtime feature family for V33a's six natural overworld structures. */
public final class OverworldStructureFeature extends Feature<NoneFeatureConfiguration> {
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
		super(NoneFeatureConfiguration.CODEC);
		this.type = type;
		this.natural = natural;
	}

	@Override
	public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
		WorldGenLevel world = context.level();
		RandomSource random = context.random();
		// The Ocean structure is 31 blocks across and can reach past the chunks this step may write to,
		// which leaves it half built. What slides is the origin, before anything is derived from it:
		// naturalControllerPosition reads the heightmap at the origin's own column and validNaturalSite
		// judges the site it returns, so moving the structure afterwards would place it at a depth
		// taken from a different column, on ground that was never approved. Sliding first also keeps
		// the random draw for the Cavern's depth to a single call.
		net.minecraft.core.Vec3i controllerOffset = natural ? naturalControllerOffset(type)
				: net.minecraft.core.Vec3i.ZERO;
		BlockPos origin = NBTStructureLoader.fitToWriteWindow(world, type.template,
				context.origin().offset(controllerOffset), type.templateAnchor).subtract(controllerOffset);
		BlockPos controllerPos = natural ? naturalControllerPosition(world, origin, random) : origin;
		if (natural && !validNaturalSite(world, controllerPos))
			return false;

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
			if (natural && random.nextInt(2) == 0) {
				furnaceRoom = placeBurrowFurnaceRoom(world, controllerPos, random);
				if (furnaceRoom && random.nextInt(2) == 0)
					lootRoom = placeBurrowLootRoom(world, controllerPos, random);
			}
		}
		else if (type == Type.OCEAN)
			placeOceanPit(world, controllerPos);
		else if (type == Type.SNOW && natural)
			finishNaturalSnow(world, controllerPos);

		world.setBlock(controllerPos, ChromaBlocks.STRUCTURE_CONTROLLER.get().defaultBlockState(), 3);
		if (!(world.getBlockEntity(controllerPos) instanceof TileEntityStructureController controller))
			return false;
		controller.initialize(type.controllerType, color, type == Type.DESERT ? 1 : 0, furnaceRoom, lootRoom);
		if (type == Type.BIOME_FRAGMENT)
			finishBiomeFragment(world, controllerPos, controller, random);
		if (type == Type.BIOME_FRAGMENT && natural)
			weatherNaturalBiomeFragment(world, controllerPos, placed, random);
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
				chest.addProgress(type == Type.OCEAN ? ProgressStage.OCEAN
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
		return true;
	}

	private static void placeOceanPit(WorldGenLevel world, BlockPos controllerPos) {
		BlockPos anchor = new BlockPos(3, 0, 3);
		int consecutiveAir = 0;
		for (int depth = 3; depth < Math.min(32, controllerPos.getY()); depth++) {
			BlockPos sliceCenter = controllerPos.below(depth);
			boolean allAir = true;
			for (int x = -2; x <= 2 && allAir; x++)
				for (int z = -2; z <= 2; z++)
					if (!world.getBlockState(sliceCenter.offset(x, 0, z)).isAir()) {
						allAir = false;
						break;
					}
			if (allAir && depth > 6) {
				if (++consecutiveAir >= 1)
					break;
			}
			else consecutiveAir = 0;
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
			RandomSource random) {
		boolean valid = NBTStructureLoader.canPlace(world, BURROW_FURNACE, controllerPos,
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
			RandomSource random) {
		boolean valid = NBTStructureLoader.canPlace(world, BURROW_LOOT, controllerPos,
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
				chest.addProgress(ProgressStage.CAVERN);
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
			case DESERT -> new net.minecraft.core.Vec3i(7, 0, 7);
			case SNOW -> new net.minecraft.core.Vec3i(8, 0, 6);
			// V33a calls BurrowStructure with the grass surface coordinate; its controller is (-5,-8,-2).
			case BURROW -> new net.minecraft.core.Vec3i(-5, 0, -2);
			case CAVERN, OCEAN, BIOME_FRAGMENT -> net.minecraft.core.Vec3i.ZERO;
		};
	}

	private BlockPos naturalControllerPosition(WorldGenLevel world, BlockPos origin, RandomSource random) {
		if (type == Type.CAVERN)
			return new BlockPos(origin.getX(), 10 + random.nextInt(40), origin.getZ());
		if (type == Type.OCEAN) {
			int top = world.getHeight(Heightmap.Types.WORLD_SURFACE_WG, origin.getX(), origin.getZ());
			return new BlockPos(origin.getX(), top - 3, origin.getZ());
		}
		if (type == Type.DESERT) {
			int top = world.getHeight(Heightmap.Types.WORLD_SURFACE_WG, origin.getX(), origin.getZ());
			return new BlockPos(origin.getX() + 7, top - 5, origin.getZ() + 7);
		}
		if (type == Type.SNOW) {
			int top = world.getHeight(Heightmap.Types.WORLD_SURFACE_WG, origin.getX(), origin.getZ());
			return new BlockPos(origin.getX() + 8, top - 4, origin.getZ() + 6);
		}
		if (type == Type.BIOME_FRAGMENT) {
			int top = world.getHeight(Heightmap.Types.WORLD_SURFACE_WG, origin.getX(), origin.getZ());
			return sinkBiomeFragment(world, new BlockPos(origin.getX(), top - 5, origin.getZ()));
		}
		// V33a calls BurrowStructure with the grass surface coordinate; its controller is (-5,-8,-2).
		int surfaceY = world.getHeight(Heightmap.Types.WORLD_SURFACE_WG, origin.getX(), origin.getZ()) - 1;
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
		var biome = world.getBiome(controllerPos);
		if (biome.is(reika.chromaticraft.world.biome.ChromaBiomes.RAINBOW_STREAM)
				|| !(biome.is(reika.chromaticraft.world.biome.ChromaBiomes.RAINBOW_FOREST)
				|| biome.is(reika.chromaticraft.world.biome.ChromaBiomes.ENDER_FOREST)
				|| biome.is(reika.chromaticraft.world.biome.ChromaBiomes.LUMINOUS_CLIFFS)
				|| biome.is(reika.chromaticraft.world.biome.ChromaBiomes.LUMINOUS_CLIFFS_SHORES)))
			return false;
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
		var biome = world.getBiome(root);
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
		int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
		var biome = world.getBiome(corners.getFirst());
		for (BlockPos corner : corners) {
			int height = world.getHeight(Heightmap.Types.WORLD_SURFACE_WG, corner.getX(), corner.getZ());
			min = Math.min(min, height); max = Math.max(max, height);
			if (!world.getBiome(corner).equals(biome)) return false;
			for (int depth = 1; depth <= 5; depth++) {
				BlockPos support = new BlockPos(corner.getX(), height - depth, corner.getZ());
				if (world.getBlockState(support).isAir() || !world.getFluidState(support).isEmpty()) return false;
			}
		}
		if (max - min > 2) return false;
		return NBTStructureLoader.canPlace(world, type.template, controllerPos, type.templateAnchor,
				state -> state, (pos, authored) -> authored.is(Blocks.STRUCTURE_VOID)
						|| !(world.getBlockState(pos).getBlock()
								instanceof reika.chromaticraft.block.worldgen26.BlockStructureShield));
	}

	private static void finishNaturalSnow(WorldGenLevel world, BlockPos controllerPos) {
		BlockPos min = controllerPos.offset(-8, -3, -6);
		for (int x = 0; x < 17; x++)
			for (int z = 0; z < 17; z++) {
				for (int y = 0; y < 15; y++) {
					BlockPos at = min.offset(x, y, z);
					if (!(world.getBlockState(at).getBlock()
							instanceof reika.chromaticraft.block.worldgen26.BlockStructureShield)) continue;
					BlockPos below = at.below();
					for (int depth = 0; depth < 5 && world.getBlockState(below).isAir(); depth++, below = below.below())
						world.setBlock(below, Blocks.STONE.defaultBlockState(), 2);
				}
				BlockPos top = new BlockPos(min.getX() + x,
						world.getHeight(Heightmap.Types.MOTION_BLOCKING, min.getX() + x, min.getZ() + z),
						min.getZ() + z);
				if (world.getBlockState(top).isAir() && world.getBlockState(top.below()).isCollisionShapeFullBlock(world, top.below()))
					world.setBlock(top, Blocks.SNOW.defaultBlockState(), 2);
			}
	}

	private boolean validNaturalDesertSite(WorldGenLevel world, BlockPos controllerPos) {
		BlockPos sourceOrigin = controllerPos.offset(-7, -3, -7);
		int surface = world.getHeight(Heightmap.Types.WORLD_SURFACE_WG,
				sourceOrigin.getX(), sourceOrigin.getZ()) - 1;
		if (!world.getBlockState(new BlockPos(sourceOrigin.getX(), surface, sourceOrigin.getZ())).is(Blocks.SAND))
			return false;
		TagKey<net.minecraft.world.level.biome.Biome> sandy = TagKey.create(Registries.BIOME,
				Identifier.fromNamespaceAndPath("c", "is_sandy"));
		for (BlockPos corner : List.of(sourceOrigin, sourceOrigin.offset(14, 0, 0),
				sourceOrigin.offset(0, 0, 15), sourceOrigin.offset(14, 0, 15)))
			if (!world.getBiome(corner).is(sandy) || world.getBiome(corner).is(BiomeTags.IS_BADLANDS))
				return false;
		return NBTStructureLoader.canPlace(world, type.template, controllerPos, type.templateAnchor,
				state -> state, (pos, authored) -> authored.is(Blocks.STRUCTURE_VOID)
						|| !(world.getBlockState(pos).getBlock()
								instanceof reika.chromaticraft.block.worldgen26.BlockStructureShield));
	}

	private boolean validNaturalOceanSite(WorldGenLevel world, BlockPos controllerPos) {
		if (!world.getFluidState(controllerPos.above(3)).is(net.minecraft.tags.FluidTags.WATER)
				|| !world.getFluidState(controllerPos.above(8)).is(net.minecraft.tags.FluidTags.WATER))
			return false;
		// V33a requires both authored bounds to remain ocean and at least one flooded end cap.
		for (BlockPos corner : List.of(controllerPos.offset(-3, 0, -3), controllerPos.offset(27, 0, -3),
				controllerPos.offset(-3, 0, 27), controllerPos.offset(27, 0, 27)))
			if (!world.getBiome(corner).is(BiomeTags.IS_OCEAN))
				return false;
		boolean openX = floodedOceanEnd(world, controllerPos.offset(28, 3, 0));
		boolean openZ = floodedOceanEnd(world, controllerPos.offset(0, 3, 28));
		if (!openX && !openZ)
			return false;
		if (!NBTStructureLoader.canPlace(world, type.template, controllerPos, type.templateAnchor,
				state -> state, (pos, authored) -> authored.is(Blocks.STRUCTURE_VOID)
						|| !(world.getBlockState(pos).getBlock()
								instanceof reika.chromaticraft.block.worldgen26.BlockStructureShield)))
			return false;
		// The original rejects the site unless the five-wide shaft reaches three consecutive cave-air
		// slices below it. This prevents an Ocean Temple from producing a blind bedrock chute.
		int consecutive = 0;
		for (int depth = 3; depth < controllerPos.getY(); depth++) {
			boolean allAir = true;
			BlockPos center = controllerPos.below(depth);
			for (int x = -2; x <= 2 && allAir; x++)
				for (int z = -2; z <= 2; z++)
					if (!world.getBlockState(center.offset(x, 0, z)).isAir()) {
						allAir = false;
						break;
					}
			if (allAir && depth > 6) {
				if (++consecutive >= 3) return true;
			}
			else consecutive = 0;
		}
		return false;
	}

	private static boolean floodedOceanEnd(WorldGenLevel world, BlockPos center) {
		for (int y = 0; y < 3; y++)
			for (int side = -1; side <= 1; side++)
				if (!world.getFluidState(center.offset(0, y, side)).is(net.minecraft.tags.FluidTags.WATER))
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
					return world.getHeight(Heightmap.Types.WORLD_SURFACE_WG, pos.getX(), pos.getZ()) - 1
							>= surface.getY() - 2;
				});
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
