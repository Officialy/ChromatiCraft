package reika.chromaticraft.registry;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;

import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.block.BlockCastingItemStand;
import reika.chromaticraft.block.BlockCastingTable;
import reika.chromaticraft.block.BlockCrystalCharger;
import reika.chromaticraft.block.BlockItemAuraInfuser;
import reika.chromaticraft.block.BlockChromaFluid;
import reika.chromaticraft.block.BlockChromaMud;
import reika.chromaticraft.block.BlockChromaPortal;
import reika.chromaticraft.block.BlockChromaDoor;
import reika.chromaticraft.block.BlockHeatLamp;
import reika.chromaticraft.block.BlockTrapFloor;
import reika.chromaticraft.block.BlockHoverBlock;
import reika.chromaticraft.block.dimension.structure.lightpanel.BlockLightPanel;
import reika.chromaticraft.block.dimension.structure.lightpanel.BlockLightSwitch;
import reika.chromaticraft.block.dimension.structure.locks.BlockColoredLock;
import reika.chromaticraft.block.dimension.structure.locks.BlockLockKey;
import reika.chromaticraft.block.decoration.BlockMusicTrigger;
import reika.chromaticraft.block.dimension.structure.shiftmaze.BlockShiftLock;
import reika.chromaticraft.block.dimension.structure.BlockBiomeReplay;
import reika.chromaticraft.block.BlockChromaticTile;
import reika.chromaticraft.block.BlockCrystallineStone;
import reika.chromaticraft.block.BlockCrystallineStoneBeam;
import reika.chromaticraft.block.BlockCrystalRune;
import reika.chromaticraft.block.BlockEncrustedCrystal;
import reika.chromaticraft.block.BlockMultiStorage;
import reika.chromaticraft.block.BlockCrystallineStone;
import reika.chromaticraft.block.BlockCrystallineStone.StoneTypes;
import reika.chromaticraft.block.crystal.BlockCaveCrystal;
import reika.chromaticraft.block.crystal.BlockCrystalLamp;
import reika.chromaticraft.block.crystal.BlockSuperCrystal;
import reika.chromaticraft.block.dye26.BlockDyeLeaf;
import reika.chromaticraft.block.dye26.BlockDyeSapling;
import reika.chromaticraft.block.dye26.BlockRainbowLeaf;
import reika.chromaticraft.block.dye26.BlockRainbowSapling;
import reika.chromaticraft.block.decoration.BlockMetaAlloyLamp;
import reika.chromaticraft.block.worldgen26.BlockCliffStone;
import reika.chromaticraft.block.worldgen26.BlockEnderFluid;
import reika.chromaticraft.block.worldgen26.BlockGlowingLeaf;
import reika.chromaticraft.block.worldgen26.BlockGlowDaisy;
import reika.chromaticraft.block.worldgen26.BlockGlowRoot;
import reika.chromaticraft.block.worldgen26.BlockLumaFluid;
import reika.chromaticraft.block.worldgen26.BlockTieredOre;
import reika.chromaticraft.block.worldgen26.BlockTieredPlant;
import reika.chromaticraft.block.worldgen26.BlockCaveIndicator;
import reika.chromaticraft.block.worldgen26.BlockUnknownArtefact;
import reika.chromaticraft.block.worldgen26.BlockStructureShield;
import reika.chromaticraft.block.BlockDummyAux;
import reika.chromaticraft.block.worldgen26.BlockLootChest;
import reika.chromaticraft.block.worldgen26.BlockStructureController;
import reika.chromaticraft.block.worldgen26.BlockWarpNode;
import reika.chromaticraft.block.worldgen26.BlockDecoFlower;
import reika.chromaticraft.magic.progression.ProgressStage;

/**
 * ChromatiCraft block registry. Port-in-progress rewrite of the 1.7.10 {@code ChromaBlocks} enum
 * into 26.2 {@link DeferredRegister} form (mirrors ReactorBlocks): each block is a
 * {@link DeferredBlock} field that registers its {@link net.minecraft.world.item.BlockItem} too, and
 * grows as blocks are ported. Consumers migrate from {@code ChromaBlocks.X.getBlockInstance()} to
 * {@code ChromaBlocks.X.get()} when they port.
 */
public final class ChromaBlocks {

	public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ChromatiCraft.MODID);
	public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ChromatiCraft.MODID);

	private static final ThreadLocal<ResourceKey<Block>> CURRENT_BLOCK_KEY = new ThreadLocal<>();
	private static final ThreadLocal<ResourceKey<Item>> CURRENT_ITEM_KEY = new ThreadLocal<>();

	/** 26.2 requires Block.Properties to carry its registry id before the constructor runs. */
	public static BlockBehaviour.Properties blockProperties() {
		BlockBehaviour.Properties p = BlockBehaviour.Properties.of();
		ResourceKey<Block> k = CURRENT_BLOCK_KEY.get();
		if (k != null) p.setId(k);
		return p;
	}

	/** 26.2 requires Item.Properties to carry its registry id before the constructor runs. */
	public static Item.Properties itemProperties() {
		Item.Properties p = new Item.Properties();
		ResourceKey<Item> k = CURRENT_ITEM_KEY.get();
		if (k != null) p.setId(k);
		return p;
	}

	private static <B extends Block> DeferredBlock<B> register(String name, Supplier<B> factory) {
		DeferredBlock<B> block = registerBlockOnly(name, factory);
		ITEMS.registerSimpleBlockItem(block);
		return block;
	}

	/** Registers a block with NO auto BlockItem (variant blocks register their own items). */
	private static <B extends Block> DeferredBlock<B> registerBlockOnly(String name, Supplier<B> factory) {
		return BLOCKS.register(name, rl -> {
			CURRENT_BLOCK_KEY.set(ResourceKey.create(Registries.BLOCK, rl));
			try {
				return factory.get();
			} finally {
				CURRENT_BLOCK_KEY.remove();
			}
		});
	}

	private static DeferredItem<Item> registerItemOnly(String name, Supplier<Item> factory) {
		return ITEMS.register(name, rl -> {
			CURRENT_ITEM_KEY.set(ResourceKey.create(Registries.ITEM, rl));
			try {
				return factory.get();
			} finally {
				CURRENT_ITEM_KEY.remove();
			}
		});
	}

	/** Dye trees were metadata families in V33a; 26.2 gives every colour its own leaf and sapling id. */
	public static final List<DeferredBlock<BlockDyeLeaf>> DYE_LEAVES = registerDyeLeaves();
	public static final List<DeferredBlock<BlockDyeSapling>> DYE_SAPLINGS = registerDyeSaplings();
	public static final DeferredBlock<BlockRainbowLeaf> RAINBOW_LEAVES = register("rainbow_leaves",
			() -> new BlockRainbowLeaf(blockProperties().strength(0.2F).randomTicks().noOcclusion()
					.sound(SoundType.GRASS).ignitedByLava()));
	public static final DeferredBlock<BlockRainbowSapling> RAINBOW_SAPLING = register("rainbow_sapling",
			() -> new BlockRainbowSapling(blockProperties().noCollision().randomTicks().instabreak().sound(SoundType.GRASS)));
	public static final DeferredBlock<BlockMetaAlloyLamp> META_ALLOY_LAMP = register("meta_alloy_lamp",
			() -> new BlockMetaAlloyLamp(blockProperties().strength(0.25F).randomTicks().noOcclusion()
					.sound(SoundType.GRASS).lightLevel(state -> state.getValue(BlockMetaAlloyLamp.POD) ? 15 : 0)
					.pushReaction(PushReaction.DESTROY)));

	private static List<DeferredBlock<BlockDyeLeaf>> registerDyeLeaves() {
		DeferredBlock<BlockDyeLeaf>[] blocks = new DeferredBlock[CrystalElement.elements.length];
		for (CrystalElement element : CrystalElement.elements) {
			blocks[element.ordinal()] = register(coloredName("dye_leaves", element),
					() -> new BlockDyeLeaf(blockProperties().strength(0.2F).randomTicks().noOcclusion()
							.sound(SoundType.GRASS).ignitedByLava(), element));
		}
		return List.of(blocks);
	}

	private static List<DeferredBlock<BlockDyeSapling>> registerDyeSaplings() {
		DeferredBlock<BlockDyeSapling>[] blocks = new DeferredBlock[CrystalElement.elements.length];
		for (CrystalElement element : CrystalElement.elements) {
			blocks[element.ordinal()] = register(coloredName("dye_sapling", element),
					() -> new BlockDyeSapling(blockProperties().noCollision().randomTicks().instabreak()
							.sound(SoundType.GRASS), element));
		}
		return List.of(blocks);
	}

	public static DeferredBlock<BlockDyeLeaf> dyeLeaves(CrystalElement element) {
		return DYE_LEAVES.get(element.ordinal());
	}

	public static DeferredBlock<BlockDyeSapling> dyeSapling(CrystalElement element) {
		return DYE_SAPLINGS.get(element.ordinal());
	}
	public static final DeferredBlock<BlockGlowDaisy> GLOW_DAISY = register("glow_daisy",
            () -> new BlockGlowDaisy(blockProperties().noCollision().instabreak().randomTicks()
                    .sound(SoundType.GRASS).lightLevel(state -> state.getValue(BlockGlowDaisy.CROP_BOOSTED) ? 12 : 10)));
    public static final DeferredBlock<BlockGlowRoot> GLOW_ROOT = register("glow_root",
            () -> new BlockGlowRoot(blockProperties().noCollision().instabreak().randomTicks()
                    .sound(SoundType.GRASS).lightLevel(state -> 6)));
    public static final DeferredBlock<BlockGlowingLeaf> GLOWING_LEAVES = register("glowing_leaves",
            () -> new BlockGlowingLeaf(blockProperties().strength(0.2F).randomTicks().noOcclusion()
                    .sound(SoundType.GRASS).ignitedByLava().lightLevel(state -> 12)));
    public static final DeferredBlock<BlockCliffStone> CLIFF_STONE = register("cliff_stone",
			() -> new BlockCliffStone(blockProperties().mapColor(MapColor.STONE).strength(1.5F, 6F), BlockCliffStone.Type.STONE));
	public static final DeferredBlock<BlockCliffStone> CLIFF_DIRT = register("cliff_dirt",
			() -> new BlockCliffStone(blockProperties().mapColor(MapColor.DIRT).strength(0.5F), BlockCliffStone.Type.DIRT));
	public static final DeferredBlock<BlockCliffStone> CLIFF_GRASS = register("cliff_grass_block",
			() -> new BlockCliffStone(blockProperties().mapColor(MapColor.GRASS).strength(0.6F).randomTicks(), BlockCliffStone.Type.GRASS));
	public static final DeferredBlock<BlockCliffStone> CLIFF_FARMLAND = register("cliff_farmland",
			() -> new BlockCliffStone(blockProperties().mapColor(MapColor.DIRT).strength(0.6F).randomTicks().lightLevel(state -> 6), BlockCliffStone.Type.FARMLAND));
	/**
	 * V33a tiered ores: disguised as their host stone until the miner reaches the stage. Only the
	 * ores whose drop item is registered and which V33a renders as an ordinary overlay ore are here.
	 * The six geode-rendered ones (BINDING, FOCAL, TELEPORT, FIRAXITE, THERMITE, SPACERIFT) need the
	 * source's custom geode mesh, and the rest (WATERY, LUMA, ECHO, THERMITE, RESO, RAINBOW, AVOLITE)
	 * drop tiered resources that are not registered yet; none are stubbed in with stand-in visuals.
	 */
	public static final DeferredBlock<BlockTieredOre> ENERGIZED_ROCK = register("energized_rock",
			() -> new BlockTieredOre(oreProperties(), ProgressStage.CRYSTALS, Blocks.STONE,
					(into, fortune, random, miner) -> {
						// V33a INFUSED: min(16, 1 + rand(5)*(1+rand(1+fortune))) chromic dust.
						int n = Math.min(16, 1 + random.nextInt(5) * (1 + random.nextInt(1 + fortune)));
						for (int i = 0; i < n; i++)
							into.add(new ItemStack(ChromaItems.TIERED.get(ChromaTieredItems.CHROMA_DUST).get()));
					}));
	public static final DeferredBlock<BlockTieredOre> ELEMENTAL_STONES = register("elemental_stones",
			() -> new BlockTieredOre(oreProperties(), ProgressStage.RUNEUSE, Blocks.STONE,
					BlockTieredOre.ELEMENTAL_STONE_DROPS));
	public static final DeferredBlock<BlockTieredOre> FIRESTONE = register("firestone",
			() -> new BlockTieredOre(oreProperties(), ProgressStage.LINK, Blocks.NETHERRACK,
					(into, fortune, random, miner) -> {
						// V33a FIRESTONE: 1 + rand(6)*(1+fortune/2) fire essence.
						int n = 1 + random.nextInt(6) * (1 + fortune / 2);
						for (int i = 0; i < n; i++)
							into.add(new ItemStack(ChromaItems.TIERED.get(ChromaTieredItems.FIRE_ESSENCE).get()));
					}));

	/**
	 * V33a tiered plants, one registered identity each; see {@link ChromaTieredPlants}. Vibrant Pod
	 * and Glowing Roots are absent because their drops have no registered identity yet.
	 *
	 * <p>V33a sets hardness 0, resistance 2, the grass step sound, and a constant light of 4.
	 */
	public static final Map<ChromaTieredPlants, DeferredBlock<BlockTieredPlant>> TIERED_PLANTS =
			registerTieredPlants();

	private static Map<ChromaTieredPlants, DeferredBlock<BlockTieredPlant>> registerTieredPlants() {
		EnumMap<ChromaTieredPlants, DeferredBlock<BlockTieredPlant>> map =
				new EnumMap<>(ChromaTieredPlants.class);
		for (ChromaTieredPlants plant : ChromaTieredPlants.list)
			map.put(plant, register(plant.registryName(),
					() -> new BlockTieredPlant(blockProperties().noCollision().instabreak()
							.explosionResistance(2).sound(SoundType.GRASS).lightLevel(state -> 4)
							.noOcclusion().pushReaction(PushReaction.DESTROY), plant)));
		return map;
	}

	public static DeferredBlock<BlockTieredPlant> tieredPlant(ChromaTieredPlants plant) {
		return TIERED_PLANTS.get(plant);
	}

	/** V33a deco flowers, one registered identity each; see {@link ChromaDecoFlowers}. */
	public static final Map<ChromaDecoFlowers, DeferredBlock<BlockDecoFlower>> DECO_FLOWERS =
			registerDecoFlowers();

	private static Map<ChromaDecoFlowers, DeferredBlock<BlockDecoFlower>> registerDecoFlowers() {
		EnumMap<ChromaDecoFlowers, DeferredBlock<BlockDecoFlower>> map =
				new EnumMap<>(ChromaDecoFlowers.class);
		for (ChromaDecoFlowers flower : ChromaDecoFlowers.list)
			map.put(flower, register(flower.registryName(),
					() -> new BlockDecoFlower(blockProperties().noCollision().instabreak()
							.sound(SoundType.GRASS).noOcclusion()
							.pushReaction(PushReaction.DESTROY), flower)));
		return map;
	}

	public static DeferredBlock<BlockDecoFlower> decoFlower(ChromaDecoFlowers flower) {
		return DECO_FLOWERS.get(flower);
	}

	/** V33a Warp Node: unbreakable, blast resistance 6000000, and invisible without its renderer. */
	public static final DeferredBlock<BlockWarpNode> WARP_NODE = registerBlockOnly("warp_node",
			() -> new BlockWarpNode(blockProperties().mapColor(MapColor.NONE)
					.strength(-1F, 6000000F).noOcclusion().noCollision()));

	/** V33a Loot Chest: hardness 6, resistance 60. */
	public static final DeferredBlock<BlockLootChest> LOOT_CHEST = register("loot_chest",
			() -> new BlockLootChest(blockProperties().mapColor(MapColor.WOOD)
					.strength(6F, 60F).noOcclusion()));

	/** Natural fragment-structure root; never exposed as an inventory block. */
	// Hardness 1.5 rather than 6: this is the root you break to claim a fragment structure's reward,
	// so a nine-second bare-handed dig was tedious for no design reason. The blast resistance stays at
	// 6000 -- it still cannot be opened with explosives, only found and mined.
	public static final DeferredBlock<BlockStructureController> STRUCTURE_CONTROLLER =
			registerBlockOnly("structure_controller", () -> new BlockStructureController(
					blockProperties().mapColor(MapColor.NONE).strength(1.5F, 6000F).noOcclusion()));

	/**
	 * V33a's Dimension Core: the prize at the end of a puzzle structure, and the sixteen a player plants
	 * around the monument. Unbreakable by blast, like the controller, and sealed by its own tile until
	 * its structure is solved.
	 */
	public static final EnumMap<CrystalElement,
			DeferredBlock<reika.chromaticraft.block.dimension.BlockDimensionCore>> DIMENSION_CORES =
			registerDimensionCores();

	private static EnumMap<CrystalElement,
			DeferredBlock<reika.chromaticraft.block.dimension.BlockDimensionCore>> registerDimensionCores() {
		EnumMap<CrystalElement, DeferredBlock<reika.chromaticraft.block.dimension.BlockDimensionCore>> map =
				new EnumMap<>(CrystalElement.class);
		for (CrystalElement element : CrystalElement.elements)
			map.put(element, register(coloredName("dimension_core", element),
					() -> new reika.chromaticraft.block.dimension.BlockDimensionCore(
							blockProperties().mapColor(MapColor.COLOR_PURPLE).strength(1.5F, 6000F)
									.lightLevel(state -> 11).noOcclusion(), element)));
		return map;
	}

	public static DeferredBlock<reika.chromaticraft.block.dimension.BlockDimensionCore> dimensionCore(
			CrystalElement element) {
		return DIMENSION_CORES.get(element);
	}

	/** The same identity where a plain {@code DeferredBlock<Block>} is what the caller holds. */
	@SuppressWarnings("unchecked")
	public static DeferredBlock<Block> dimensionCoreBlock(CrystalElement element) {
		return (DeferredBlock<Block>)(DeferredBlock<?>)DIMENSION_CORES.get(element);
	}

	/**
	 * V33a's Void Rift, one identity per element. Blast resistance 900,000 and a hardness of 10 --
	 * upstream's {@code setBlockUnbreakable()} is commented out, so the block is nominally breakable and
	 * the two progression gates on it are what actually stop you.
	 */
	public static final EnumMap<CrystalElement,
			DeferredBlock<reika.chromaticraft.block.dimension.BlockVoidRift>> VOID_RIFTS =
			registerVoidRifts();

	private static EnumMap<CrystalElement,
			DeferredBlock<reika.chromaticraft.block.dimension.BlockVoidRift>> registerVoidRifts() {
		EnumMap<CrystalElement, DeferredBlock<reika.chromaticraft.block.dimension.BlockVoidRift>> map =
				new EnumMap<>(CrystalElement.class);
		for (CrystalElement element : CrystalElement.elements)
			map.put(element, register(coloredName("void_rift", element),
					() -> new reika.chromaticraft.block.dimension.BlockVoidRift(
							blockProperties().mapColor(MapColor.COLOR_BLACK).strength(10F, 900000F),
							element)));
		return map;
	}

	public static DeferredBlock<reika.chromaticraft.block.dimension.BlockVoidRift> voidRift(
			CrystalElement element) {
		return VOID_RIFTS.get(element);
	}

	/** The same identity where a plain {@code DeferredBlock<Block>} is what the caller holds. */
	@SuppressWarnings("unchecked")
	public static DeferredBlock<Block> voidRiftBlock(CrystalElement element) {
		return (DeferredBlock<Block>)(DeferredBlock<?>)VOID_RIFTS.get(element);
	}

	/**
	 * V33a's Glowing Cracks. A sliver of a block: everything you see is its renderer, painting Reika's
	 * own 1024x1024 sheet across the nine-by-nine of ground around it.
	 */
	public static final DeferredBlock<Block> GLOWING_CRACKS =
			register("glowing_cracks",
					() -> (Block)new reika.chromaticraft.block.dimension.BlockGlowingCracks(
							blockProperties().mapColor(MapColor.COLOR_MAGENTA).strength(1F, 6000F)
									.noOcclusion().lightLevel(state -> 7)));

	/** V33a's Void Cave: the unbreakable lip a glowing cave's floor falls away at. */
	public static final DeferredBlock<reika.chromaticraft.block.dimension.BlockVoidCave> VOID_CAVE =
			register("void_cave", () -> new reika.chromaticraft.block.dimension.BlockVoidCave(
					blockProperties().mapColor(MapColor.COLOR_BLACK).strength(-1F, 3600000F)
							.noLootTable()));

	/** V33a's cracked bedrock: nine depths of it, and the only source of Proximal Essence. */
	public static final DeferredBlock<reika.chromaticraft.block.dimension.BlockBedrockCrack> BEDROCK_CRACK =
			register("bedrock_crack",
					() -> new reika.chromaticraft.block.dimension.BlockBedrockCrack(
							blockProperties().mapColor(MapColor.COLOR_BLACK).strength(12F, 3600000F)));

	/**
	 * V33a's Ethereal Light: light with nothing there. Air in every way that matters, and full bright.
	 */
	public static final DeferredBlock<reika.chromaticraft.block.decoration.BlockEtherealLight> ETHEREAL_LIGHT =
			register("ethereal_light",
					() -> new reika.chromaticraft.block.decoration.BlockEtherealLight(
							blockProperties().mapColor(MapColor.NONE).strength(0F, 3600000F)
									.noCollision().noOcclusion().replaceable().noLootTable()
									.lightLevel(state -> 15).randomTicks()
									.pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY)));

	/** V33a's Aura Point: the monument's completed form, and a standing area effect thereafter. */
	public static final DeferredBlock<Block> AURA_POINT =
			register("aura_point", () -> (Block)new reika.chromaticraft.block.dimension.BlockAuraPoint(
					blockProperties().mapColor(MapColor.COLOR_LIGHT_BLUE).strength(-1F, 6000000F)
							.lightLevel(state -> 15).noOcclusion()));

	/**
	 * V33a's Fire Jet, a {@code BlockDimensionDecoTile} type. Solid, unlike the other deco tiles, so a
	 * player can stand on one; hardness 10 and resistance 50000, and mineable only past CTM.
	 */
	public static final DeferredBlock<Block> FIRE_JET = register("fire_jet",
			() -> (Block)new reika.chromaticraft.block.dimension.BlockFireJet(
					blockProperties().mapColor(MapColor.FIRE).strength(10F, 50000F)
							.lightLevel(state -> 10)));

	/** V33a connected UUID door; explicit properties replace its packed metadata flags. */
	public static final DeferredBlock<BlockChromaDoor> CHROMA_DOOR = register("chroma_door",
			() -> new BlockChromaDoor(blockProperties().mapColor(MapColor.COLOR_PURPLE)
					.strength(-1F, 600000F).lightLevel(state -> 12).noOcclusion()));

	/** V33a Snow/Shift-Maze puzzle blocks; metadata modes are modern blockstate properties. */
	public static final DeferredBlock<BlockTrapFloor> TRAP_FLOOR = register("trap_floor",
			() -> new BlockTrapFloor(blockProperties().mapColor(MapColor.STONE).strength(1F, 0F)
					.sound(SoundType.STONE).noOcclusion()));
	public static final DeferredBlock<BlockShiftLock> SHIFT_LOCK = register("shift_lock",
			() -> new BlockShiftLock(blockProperties().mapColor(MapColor.STONE).strength(1F, 600000F)
					.sound(SoundType.STONE).noOcclusion()));
	public static final DeferredBlock<BlockHoverBlock> HOVER = register("hover",
			() -> new BlockHoverBlock(blockProperties().mapColor(MapColor.NONE).strength(-1F, 600000F)
					.noCollision().noOcclusion().lightLevel(state -> 6)));
	public static final DeferredBlock<BlockLightPanel> LIGHT_PANEL = register("light_panel",
			() -> new BlockLightPanel(blockProperties().mapColor(MapColor.STONE).strength(-1F, 600000F)
					.sound(SoundType.STONE).lightLevel(state -> state.getValue(BlockLightPanel.ACTIVE) ? 15 : 0)));
	public static final DeferredBlock<BlockLightSwitch> PANEL_SWITCH = register("panel_switch",
			() -> new BlockLightSwitch(blockProperties().mapColor(MapColor.STONE).strength(-1F, 600000F)
					.sound(SoundType.STONE)));
	public static final DeferredBlock<BlockColoredLock> COLOR_LOCK = register("color_lock",
			() -> new BlockColoredLock(blockProperties().mapColor(MapColor.STONE).strength(-1F, 600000F)
					.sound(SoundType.STONE).noOcclusion()));
	public static final DeferredBlock<BlockLockKey> LOCK_KEY = register("lock_key",
			() -> new BlockLockKey(blockProperties().mapColor(MapColor.NONE).strength(0.15F)
					.sound(SoundType.GLASS).lightLevel(state -> 15).noOcclusion()));
	public static final DeferredBlock<BlockMusicTrigger> MUSIC_TRIGGER = register("music_trigger",
			() -> new BlockMusicTrigger(blockProperties().mapColor(MapColor.STONE).strength(6F, 60000F)
					.sound(SoundType.STONE)));
	/** V33a DIMDATA callback cell used by the natural Biome Fragment's melody replay pedestal. */
	public static final DeferredBlock<BlockBiomeReplay> BIOME_REPLAY = registerBlockOnly("biome_replay",
			() -> new BlockBiomeReplay(blockProperties().mapColor(MapColor.STONE)
					.strength(-1F, 600000F).lightLevel(state -> 8)));

	/** V33a metadata 0 and 8 are content identities in 26.2, not a reconstructed variant value. */
	public static final DeferredBlock<BlockHeatLamp> HEAT_LAMP = register("heat_lamp",
			() -> new BlockHeatLamp(blockProperties().mapColor(MapColor.COLOR_ORANGE)
					.instabreak().lightLevel(state -> 7).noOcclusion().noCollision(), false));
	public static final DeferredBlock<BlockHeatLamp> COLD_LAMP = register("cold_lamp",
			() -> new BlockHeatLamp(blockProperties().mapColor(MapColor.COLOR_LIGHT_BLUE)
					.instabreak().lightLevel(state -> 7).noOcclusion().noCollision(), true));

	/** V33a Dummy Aux: a structure's stand-in block; unbreakable, resistance 60000. */
	public static final DeferredBlock<BlockDummyAux> DUMMY_AUX = registerBlockOnly("dummy_aux",
			() -> new BlockDummyAux(blockProperties().mapColor(MapColor.STONE)
					.strength(-1F, 60000F).noOcclusion()));

	/** V33a structure shielding, one registered identity per material. */
	public static final Map<ChromaShieldTypes, DeferredBlock<BlockStructureShield>> SHIELDING =
			registerShielding();

	private static Map<ChromaShieldTypes, DeferredBlock<BlockStructureShield>> registerShielding() {
		EnumMap<ChromaShieldTypes, DeferredBlock<BlockStructureShield>> map =
				new EnumMap<>(ChromaShieldTypes.class);
		for (ChromaShieldTypes type : ChromaShieldTypes.list)
			map.put(type, register(type.registryName(), () -> {
				// V33a hardness 2, resistance 6000; Light emits 15 and the transparent materials do
				// not occlude.
				BlockBehaviour.Properties props = blockProperties().mapColor(MapColor.STONE)
						.strength(2F, 6000F).requiresCorrectToolForDrops()
						.lightLevel(state -> type.lightValue());
				if (type.isTransparent())
					props = props.noOcclusion();
				return new BlockStructureShield(props, type);
			}));
		return map;
	}

	public static DeferredBlock<BlockStructureShield> shielding(ChromaShieldTypes type) {
		return SHIELDING.get(type);
	}

	/**
	 * V33a Proxima decoration, one registered identity per material. Two of upstream's ten are absent
	 * because their textures do not exist in V33a either; see {@link ProximaDecoTypes}.
	 */
	public static final Map<ProximaDecoTypes, DeferredBlock<reika.chromaticraft.block.dimension.BlockDimensionDeco>>
			DIMENSION_DECO = registerDimensionDeco();

	private static Map<ProximaDecoTypes, DeferredBlock<reika.chromaticraft.block.dimension.BlockDimensionDeco>>
			registerDimensionDeco() {
		EnumMap<ProximaDecoTypes, DeferredBlock<reika.chromaticraft.block.dimension.BlockDimensionDeco>> map =
				new EnumMap<>(ProximaDecoTypes.class);
		for (ProximaDecoTypes type : ProximaDecoTypes.list)
			map.put(type, register(type.registryName(), () -> {
				// V33a hardness 0.75, resistance 5. isOpaqueCube and renderAsNormalBlock are both
				// false upstream, which is noOcclusion here; the walk-through variants additionally
				// have no collision, which lives on the block class.
				BlockBehaviour.Properties props = blockProperties().mapColor(MapColor.STONE)
						.strength(0.75F, 5F).noOcclusion()
						.lightLevel(state -> type.lightValue());
				if (type.requiresPickaxe())
					props = props.requiresCorrectToolForDrops();
				return new reika.chromaticraft.block.dimension.BlockDimensionDeco(props, type);
			}));
		return map;
	}

	public static DeferredBlock<reika.chromaticraft.block.dimension.BlockDimensionDeco> deco(
			ProximaDecoTypes type) {
		return DIMENSION_DECO.get(type);
	}

	// V33a's GLOWLEAF is already registered above as GLOWING_LEAVES: BiomeGlowingCliffs and the
	// Proxima glow trees place the same block, so there is one identity, not two.

	/** V33a Glowing Log: an ordinary log whose glow is an overlay, not emitted light. */
	public static final DeferredBlock<reika.chromaticraft.block.dimension.BlockLightedLog> GLOW_LOG =
			register("glow_log", () -> new reika.chromaticraft.block.dimension.BlockLightedLog(
					blockProperties().mapColor(MapColor.WOOD).strength(2F).sound(SoundType.WOOD)));

	/** V33a Glowing Sapling: light 9, three less than the canopy it grows into. */
	public static final DeferredBlock<reika.chromaticraft.block.dimension.BlockLightedSapling> GLOW_SAPLING =
			register("glow_sapling", () -> new reika.chromaticraft.block.dimension.BlockLightedSapling(
					blockProperties().mapColor(MapColor.PLANT).noCollision().instabreak()
							.sound(SoundType.GRASS).lightLevel(state -> 9)));

	/** V33a Unknown Artefact: hardness 12, and resistance 300000 so it cannot be blasted out. */
	public static final DeferredBlock<BlockUnknownArtefact> UNKNOWN_ARTEFACT = register("unknown_artefact",
			() -> new BlockUnknownArtefact(blockProperties().mapColor(MapColor.STONE)
					.strength(12F, 300000F).requiresCorrectToolForDrops().noOcclusion()));

	/**
	 * V33a Piezo Crystal: stone's hardness, a third of its resistance, and light 10 while active.
	 */
	public static final DeferredBlock<BlockCaveIndicator> CAVE_INDICATOR = register("cave_indicator",
			() -> new BlockCaveIndicator(blockProperties().mapColor(MapColor.STONE)
					.strength(1.5F, 10F / 3F).requiresCorrectToolForDrops()
					.lightLevel(state -> state.getValue(BlockCaveIndicator.ACTIVE) ? 10 : 0)));

	/** V33a BlockTieredOre: hardness 4, resistance 5. */
	private static BlockBehaviour.Properties oreProperties() {
		return blockProperties().mapColor(MapColor.STONE).strength(4, 5).requiresCorrectToolForDrops();
	}

	public static final DeferredBlock<BlockLumaFluid> LUMA = registerBlockOnly("luma",
			() -> new BlockLumaFluid(ChromaFluids.LUMA.get(), blockProperties().mapColor(MapColor.COLOR_PURPLE)
					.strength(100F, 500F).lightLevel(state -> 15).noCollision().replaceable().liquid()));
	public static final DeferredBlock<Block> STORAGE =
			register("storage", () -> new BlockMultiStorage(blockProperties().strength(2F, 8F)));


	public static final DeferredBlock<BlockChromaMud> MUD =
			register("chroma_mud", () -> new BlockChromaMud(
					blockProperties().strength(0.6F).sound(SoundType.MUD).friction(0.6F).noOcclusion()));

	public static final DeferredBlock<BlockChromaFluid> CHROMA =
			registerBlockOnly("liquid_chroma", () -> new BlockChromaFluid(ChromaFluids.CHROMA.get(),
					blockProperties().strength(100F, 500F).lightLevel(state -> 15).noCollision().replaceable().liquid()));
	/**
	 * V33a Portal Rift. {@code setBlockUnbreakable()} plus {@code setResistance(50000)}, no collision
	 * and no model — the whole block is its renderer and its entity trigger. Only the Elemental
	 * Manipulator removes it, which is why it also drops nothing on its own.
	 */
	public static final DeferredBlock<BlockChromaPortal> PORTAL = register("portal_rift",
			() -> new BlockChromaPortal(portalProperties(), false));
	/**
	 * V33a Portal Rift metadata 15: destination is the Overworld and it is complete without any
	 * structure. Upstream never places it — it exists only through commands/creative — and this port
	 * does not invent an acquisition path for it, but the behavior is preserved as its own identity
	 * rather than being lost with the metadata.
	 */
	public static final DeferredBlock<BlockChromaPortal> RETURN_PORTAL = register("return_portal_rift",
			() -> new BlockChromaPortal(portalProperties(), true));

	private static BlockBehaviour.Properties portalProperties() {
		return blockProperties().mapColor(MapColor.COLOR_PURPLE).strength(-1F, 50000F)
				.lightLevel(state -> 15).noOcclusion().noCollision().noLootTable();
	}

	/** V33a BlockLiquidEnder: hardness 100, resistance 500, light opacity 0, luminosity 4. */
	public static final DeferredBlock<BlockEnderFluid> ENDER =
			registerBlockOnly("liquid_ender", () -> new BlockEnderFluid(ChromaFluids.ENDER.get(),
					blockProperties().mapColor(MapColor.COLOR_BLACK).strength(100F, 500F)
							.lightLevel(state -> 4).noCollision().replaceable().liquid()));
	/** One block and BlockItem registry identity per former cave-crystal metadata colour. */
	public static final List<DeferredBlock<BlockCaveCrystal>> CAVE_CRYSTALS = registerCaveCrystals();

	private static List<DeferredBlock<BlockCaveCrystal>> registerCaveCrystals() {
		DeferredBlock<BlockCaveCrystal>[] blocks = new DeferredBlock[CrystalElement.elements.length];
		for (CrystalElement element : CrystalElement.elements) {
			blocks[element.ordinal()] = register(coloredName("cave_crystal", element),
					() -> new BlockCaveCrystal(blockProperties().strength(1F, 2F).lightLevel(s -> 10)
							.noOcclusion().sound(SoundType.GLASS), element));
		}
		return List.of(blocks);
	}

	public static DeferredBlock<BlockCaveCrystal> caveCrystal(CrystalElement element) {
		return CAVE_CRYSTALS.get(element.ordinal());
	}

	public static boolean isCaveCrystal(BlockState state) {
		return state.getBlock() instanceof BlockCaveCrystal;
	}

	public static final List<DeferredBlock<BlockCrystalLamp>> CRYSTAL_LAMPS = registerCrystalLamps();
	public static final List<DeferredBlock<BlockSuperCrystal>> SUPER_CRYSTALS = registerSuperCrystals();

	private static List<DeferredBlock<BlockCrystalLamp>> registerCrystalLamps() {
		DeferredBlock<BlockCrystalLamp>[] blocks = new DeferredBlock[CrystalElement.elements.length];
		for (CrystalElement element : CrystalElement.elements) {
			blocks[element.ordinal()] = register(coloredName("crystal_lamp", element),
					() -> new BlockCrystalLamp(blockProperties().strength(1F, 2F).lightLevel(s -> 15)
							.noOcclusion().sound(SoundType.GLASS), element));
		}
		return List.of(blocks);
	}

	private static List<DeferredBlock<BlockSuperCrystal>> registerSuperCrystals() {
		DeferredBlock<BlockSuperCrystal>[] blocks = new DeferredBlock[CrystalElement.elements.length];
		for (CrystalElement element : CrystalElement.elements) {
			blocks[element.ordinal()] = register(coloredName("super_crystal", element),
					() -> new BlockSuperCrystal(blockProperties().strength(1F, 2F).lightLevel(s -> 15)
							.noOcclusion().sound(SoundType.GLASS), element));
		}
		return List.of(blocks);
	}

	public static DeferredBlock<BlockCrystalLamp> crystalLamp(CrystalElement element) {
		return CRYSTAL_LAMPS.get(element.ordinal());
	}

	public static DeferredBlock<BlockSuperCrystal> superCrystal(CrystalElement element) {
		return SUPER_CRYSTALS.get(element.ordinal());
	}

	// First TileEntity block (the TE-infrastructure proving vertical). References ChromaTiles.DISPLAY,
	// which back-references this DeferredBlock — both are lazy holders, so no init-order cycle.
	public static final DeferredBlock<Block> DISPLAY_POINT =
			register("display_point", () -> new BlockChromaticTile(
					blockProperties().strength(2F).noOcclusion(), reika.chromaticraft.registry.ChromaTiles.DISPLAY));

	// Crystal pylon (network energy source). The block hosts the server-authoritative TE; its
	// multiblock is canonical structure NBT and its block/item definitions are datagen-owned.
	public static final DeferredBlock<Block> PYLON =
			register("pylon", () -> new reika.chromaticraft.block.BlockCrystalPylonTile(
					blockProperties().strength(5F, 30F).noOcclusion().lightLevel(s -> 12),
					reika.chromaticraft.registry.ChromaTiles.PYLON));

	public static final DeferredBlock<Block> REPEATER =
			register("crystal_repeater", () -> new BlockChromaticTile(
					blockProperties().strength(3F, 12F).noOcclusion(), reika.chromaticraft.registry.ChromaTiles.REPEATER));

	public static final DeferredBlock<Block> SKYPEATER =
			register("skypeater", () -> new BlockChromaticTile(
					blockProperties().strength(3F, 12F).noOcclusion(), reika.chromaticraft.registry.ChromaTiles.SKYPEATER));

	public static final DeferredBlock<Block> CREATIVEPYLON =
			register("creative_pylon", () -> new BlockChromaticTile(
					blockProperties().strength(5F, 30F).noOcclusion().lightLevel(s -> 15), reika.chromaticraft.registry.ChromaTiles.CREATIVEPYLON));

	public static final DeferredBlock<Block> COMPOUND =
			register("compound_repeater", () -> new BlockChromaticTile(
					blockProperties().strength(3F, 12F).noOcclusion(), reika.chromaticraft.registry.ChromaTiles.COMPOUND));
	public static final DeferredBlock<Block> ITEM_STAND =
			register("casting_item_stand", () -> new BlockCastingItemStand(
					blockProperties().strength(2F, 8F).noOcclusion()));
	public static final DeferredBlock<Block> CASTING_TABLE =
			register("casting_table", () -> new BlockCastingTable(blockProperties().strength(4F, 16F).noOcclusion()));
	public static final DeferredBlock<Block> CRYSTAL_CHARGER =
			register("crystal_charger", () -> new BlockCrystalCharger(
					blockProperties().strength(3F, 12F).noOcclusion()));
	public static final DeferredBlock<Block> ITEM_INFUSER =
			register("item_aura_infuser", () -> new BlockItemAuraInfuser(
					blockProperties().strength(3F, 12F).noOcclusion().lightLevel(state -> 8)));
	public static final DeferredBlock<Block> PLAYER_INFUSER =
			register("player_aura_infuser", () -> new BlockItemAuraInfuser(
					blockProperties().strength(3F, 12F).noOcclusion().lightLevel(state -> 8),
					reika.chromaticraft.registry.ChromaTiles.PLAYERINFUSER));
	public static final DeferredBlock<Block> DATA_NODE =
			register("data_node", () -> new BlockChromaticTile(
					blockProperties().strength(-1F, 3600000F).noOcclusion().lightLevel(s -> 12),
					reika.chromaticraft.registry.ChromaTiles.DATANODE));

	public static final DeferredBlock<Block> FOCUS_CRYSTAL =
			register("focus_crystal", () -> new BlockChromaticTile(
					blockProperties().strength(2F, 8F).noOcclusion().lightLevel(s -> 8),
					reika.chromaticraft.registry.ChromaTiles.FOCUSCRYSTAL));

	public static final DeferredBlock<Block> PYLON_LINK =
			register("pylon_link", () -> new BlockChromaticTile(
					blockProperties().strength(3F, 12F).noOcclusion(), reika.chromaticraft.registry.ChromaTiles.PYLONLINK));


	// Crystalline stone: one block + the TYPE property + 16 distinct BlockItems (one per StoneTypes
	// variant). hardness 3 / resistance 12 as in the 1.7.10 original.
	public static final DeferredBlock<Block> POWER_CRYSTAL =
			register("power_crystal", () -> new BlockChromaticTile(
					blockProperties().strength(3F, 12F).noOcclusion().lightLevel(s -> 10),
					reika.chromaticraft.registry.ChromaTiles.CRYSTAL));

	/** One real block and BlockItem registry identity per encrusted-crystal colour. */
	public static final List<DeferredBlock<BlockEncrustedCrystal>> ENCRUSTED_CRYSTALS = registerEncrustedCrystals();

	private static List<DeferredBlock<BlockEncrustedCrystal>> registerEncrustedCrystals() {
		DeferredBlock<BlockEncrustedCrystal>[] blocks = new DeferredBlock[CrystalElement.elements.length];
		for (CrystalElement element : CrystalElement.elements) {
			blocks[element.ordinal()] = register(coloredName("encrusted_crystal", element),
					() -> new BlockEncrustedCrystal(blockProperties().strength(0.6F, 5F).noOcclusion()
							.noCollision().randomTicks(), element));
		}
		return List.of(blocks);
	}

	public static DeferredBlock<BlockEncrustedCrystal> encrustedCrystal(CrystalElement element) {
		return ENCRUSTED_CRYSTALS.get(element.ordinal());
	}

	public static boolean isEncrustedCrystal(BlockState state) {
		return state.getBlock() instanceof BlockEncrustedCrystal;
	}
	/**
	 * The sixteen crystalline-stone variants, each its own block and BlockItem.
	 *
	 * <p>V33a packed these into one block's metadata; that is exactly the legacy-metadata shape the
	 * project rule forbids re-creating, and it also meant every one of them reported as
	 * {@code chromaticraft:pylon_structure} when looked at. Registry names come from the enum so they
	 * stay stable against display-name edits; the display names themselves come from V33a's
	 * {@code chromablock.pylon.N} lang keys.
	 */
	private static final String[] CRYSTALLINE_STONE_NAMES = {
		"crystalline_stone", "crystalline_stone_beam", "crystalline_stone_column",
		"crystalline_energy_stabilizer", "energized_crystalline_stone_beam", "crystal_pylon_focus",
		"crystalline_stone_corner", "engraved_crystalline_stone", "embossed_crystalline_stone",
		"crystal_pylon_focus_frame", "crystalline_stone_groove_1", "crystalline_stone_groove_2",
		"crystalline_stone_bricks", "multichromic_rune", "aura_stabilizer", "resonance_ring",
	};

	public static final List<DeferredBlock<BlockCrystallineStone>> CRYSTALLINE_STONE = registerCrystallineStone();


	@SuppressWarnings("unchecked")
	private static List<DeferredBlock<BlockCrystallineStone>> registerCrystallineStone() {
		DeferredBlock<BlockCrystallineStone>[] blocks = new DeferredBlock[StoneTypes.list.length];
		for (StoneTypes t : StoneTypes.list) {
			// blockProperties() reads the CURRENT_BLOCK_KEY thread-local that registerBlockOnly sets,
			// so it has to be called inside the supplier -- hoisting it leaves the block id unset.
			blocks[t.ordinal()] = register(CRYSTALLINE_STONE_NAMES[t.ordinal()],
					t.isBeam()
							? () -> new BlockCrystallineStoneBeam(blockProperties().strength(3F, 12F), t)
							: () -> new BlockCrystallineStone(blockProperties().strength(3F, 12F), t));
		}
		return List.of(blocks);
	}

	public static String crystallineStoneName(StoneTypes type) {
		return CRYSTALLINE_STONE_NAMES[type.ordinal()];
	}

	public static DeferredBlock<BlockCrystallineStone> crystallineStone(StoneTypes type) {
		return CRYSTALLINE_STONE.get(type.ordinal());
	}

	/** One real block and BlockItem registry identity per crystal-rune colour. */
	public static final List<DeferredBlock<BlockCrystalRune>> RUNES = registerRunes();

	private static List<DeferredBlock<BlockCrystalRune>> registerRunes() {
		DeferredBlock<BlockCrystalRune>[] blocks = new DeferredBlock[CrystalElement.elements.length];
		for (CrystalElement element : CrystalElement.elements) {
			blocks[element.ordinal()] = register(coloredName("crystal_rune", element),
					() -> new BlockCrystalRune(blockProperties().strength(3F, 12F), element));
		}
		return List.of(blocks);
	}

	public static DeferredBlock<BlockCrystalRune> rune(CrystalElement element) {
		return RUNES.get(element.ordinal());
	}

	public static boolean isRune(BlockState state) {
		return state.getBlock() instanceof BlockCrystalRune;
	}
	/**
	 * Colour-suffixed registry id. Deliberately diverges from V33a's enum constant spelling
	 * ({@code LIGHTGRAY}/{@code LIGHTBLUE}) to match vanilla {@link net.minecraft.world.item.DyeColor}
	 * id convention ({@code light_gray}/{@code light_blue}); {@link CrystalElement#getEnglishName()}
	 * already returns the underscored vanilla dye name for every element.
	 */
	private static String coloredName(String base, CrystalElement element) {
		return base + '_' + element.getEnglishName();
	}

	private ChromaBlocks() {}
}
