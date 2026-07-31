package reika.chromaticraft.registry;

import java.util.List;
import java.util.function.Supplier;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;

import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.block.BlockCastingItemStand;
import reika.chromaticraft.block.BlockCastingTable;
import reika.chromaticraft.block.BlockChromaFluid;
import reika.chromaticraft.block.BlockChromaMud;
import reika.chromaticraft.block.BlockChromaticTile;
import reika.chromaticraft.block.BlockCrystalRune;
import reika.chromaticraft.block.BlockEncrustedCrystal;
import reika.chromaticraft.block.BlockMultiStorage;
import reika.chromaticraft.block.BlockPylonStructure;
import reika.chromaticraft.block.BlockPylonStructure.StoneTypes;
import reika.chromaticraft.block.crystal.BlockCaveCrystal;
import reika.chromaticraft.block.crystal.BlockCrystalLamp;
import reika.chromaticraft.block.crystal.BlockSuperCrystal;
import reika.chromaticraft.block.dye26.BlockDyeLeaf;
import reika.chromaticraft.block.dye26.BlockDyeSapling;
import reika.chromaticraft.block.dye26.BlockRainbowLeaf;
import reika.chromaticraft.block.dye26.BlockRainbowSapling;
import reika.chromaticraft.block.worldgen26.BlockCliffStone;
import reika.chromaticraft.block.worldgen26.BlockGlowingLeaf;
import reika.chromaticraft.block.worldgen26.BlockGlowDaisy;
import reika.chromaticraft.block.worldgen26.BlockGlowRoot;
import reika.chromaticraft.block.worldgen26.BlockLumaFluid;
import reika.chromaticraft.item.BlockItemPylonStructure;

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
	public static final DeferredBlock<Block> PYLONSTRUCT =
			registerBlockOnly("pylon_structure", () -> new BlockPylonStructure(blockProperties().strength(3F, 12F)));

	/** One BlockItem per {@link StoneTypes} variant, indexed by ordinal. */
	public static final List<DeferredItem<Item>> PYLONSTRUCT_ITEMS = registerPylonItems();

	private static List<DeferredItem<Item>> registerPylonItems() {
		DeferredItem<Item>[] items = new DeferredItem[StoneTypes.list.length];
		for (StoneTypes t : StoneTypes.list) {
			final int ord = t.ordinal();
			items[ord] = registerItemOnly("pylon_structure_" + t.name().toLowerCase(java.util.Locale.ENGLISH),
					() -> new BlockItemPylonStructure(PYLONSTRUCT.get(), ord, itemProperties()));
		}
		return List.of(items);
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
