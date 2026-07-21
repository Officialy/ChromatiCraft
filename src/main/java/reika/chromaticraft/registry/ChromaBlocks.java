package reika.chromaticraft.registry;

import java.util.List;
import java.util.function.Supplier;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.block.BlockChromaticTile;
import reika.chromaticraft.block.BlockCrystalRune;
import reika.chromaticraft.block.BlockMultiStorage;
import reika.chromaticraft.block.BlockPylonStructure;
import reika.chromaticraft.block.BlockPylonStructure.StoneTypes;
import reika.chromaticraft.block.crystal.BlockCaveCrystal;
import reika.chromaticraft.block.crystal.BlockCrystalLamp;
import reika.chromaticraft.block.crystal.BlockSuperCrystal;
import reika.chromaticraft.item.BlockItemCrystalRune;
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

	private static DeferredBlock<Block> register(String name, Supplier<Block> factory) {
		DeferredBlock<Block> block = registerBlockOnly(name, factory);
		ITEMS.registerSimpleBlockItem(block);
		return block;
	}

	/** Registers a block with NO auto BlockItem (variant blocks register their own items). */
	private static DeferredBlock<Block> registerBlockOnly(String name, Supplier<Block> factory) {
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

	public static final DeferredBlock<Block> STORAGE =
			register("storage", () -> new BlockMultiStorage(blockProperties().strength(2F, 8F)));

	public static final DeferredBlock<Block> CAVE_CRYSTAL =
			register("cave_crystal", () -> new BlockCaveCrystal(
					blockProperties().strength(1F, 2F).lightLevel(s -> 10).noOcclusion().sound(SoundType.GLASS)));

	public static final DeferredBlock<Block> LAMP =
			register("crystal_lamp", () -> new BlockCrystalLamp(
					blockProperties().strength(1F, 2F).lightLevel(s -> 15).noOcclusion().sound(SoundType.GLASS)));

	public static final DeferredBlock<Block> SUPER =
			register("super_crystal", () -> new BlockSuperCrystal(
					blockProperties().strength(1F, 2F).lightLevel(s -> 15).noOcclusion().sound(SoundType.GLASS)));

	// First TileEntity block (the TE-infrastructure proving vertical). References ChromaTiles.DISPLAY,
	// which back-references this DeferredBlock — both are lazy holders, so no init-order cycle.
	public static final DeferredBlock<Block> DISPLAY_POINT =
			register("display_point", () -> new BlockChromaticTile(
					blockProperties().strength(2F).noOcclusion(), reika.chromaticraft.registry.ChromaTiles.DISPLAY));

	// Crystalline stone: one block + the TYPE property + 16 distinct BlockItems (one per StoneTypes
	// variant). hardness 3 / resistance 12 as in the 1.7.10 original.
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

	// Crystal rune: one block + the COLOR property + 16 distinct BlockItems (one per CrystalElement).
	public static final DeferredBlock<Block> RUNE =
			registerBlockOnly("crystal_rune", () -> new BlockCrystalRune(blockProperties().strength(3F, 12F)));

	/** One BlockItem per {@link reika.chromaticraft.registry.CrystalElement} colour, indexed by ordinal. */
	public static final List<DeferredItem<Item>> RUNE_ITEMS = registerRuneItems();

	private static List<DeferredItem<Item>> registerRuneItems() {
		DeferredItem<Item>[] items = new DeferredItem[reika.chromaticraft.registry.CrystalElement.elements.length];
		for (reika.chromaticraft.registry.CrystalElement e : reika.chromaticraft.registry.CrystalElement.elements) {
			final int ord = e.ordinal();
			items[ord] = registerItemOnly("rune_" + e.name().toLowerCase(java.util.Locale.ENGLISH),
					() -> new BlockItemCrystalRune(RUNE.get(), ord, itemProperties()));
		}
		return List.of(items);
	}

	private ChromaBlocks() {}
}
