package reika.chromaticraft.registry;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import reika.chromaticraft.ChromatiCraft;

/** The six V33a ChromatiCraft creative inventories, populated from modern registry identities. */
@EventBusSubscriber(modid = ChromatiCraft.MODID)
public final class ChromaTabs {

	public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
			DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ChromatiCraft.MODID);

	public static final DeferredHolder<CreativeModeTab, CreativeModeTab> CHROMATICRAFT = tab(
			"chromaticraft", "tab.chromaticraft", () -> new ItemStack(ChromaBlocks.PORTAL.get()));
	public static final DeferredHolder<CreativeModeTab, CreativeModeTab> DECORATION = tab(
			"decoration", "tab.chromaticraft.decoration",
			() -> new ItemStack(ChromaBlocks.CRYSTAL_LAMPS.get(CrystalElement.BLUE.ordinal()).get()));
	public static final DeferredHolder<CreativeModeTab, CreativeModeTab> WORLDGEN = tab(
			"worldgen", "tab.chromaticraft.worldgen", () -> new ItemStack(ChromaBlocks.RAINBOW_SAPLING.get()));
	public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TOOLS = tab(
			"tools", "tab.chromaticraft.tools", () -> new ItemStack(ChromaItems.MANIPULATOR.get()));
	public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ITEMS = tab(
			"items", "tab.chromaticraft.items", () -> ChromaItems.shardStack(CrystalElement.RED));
	public static final DeferredHolder<CreativeModeTab, CreativeModeTab> FRAGMENTS = tab(
			"fragments", "tab.chromaticraft.fragments", () -> new ItemStack(ChromaItems.INFO_FRAGMENT.get()));

	private static DeferredHolder<CreativeModeTab, CreativeModeTab> tab(String id, String title,
			java.util.function.Supplier<ItemStack> icon) {
		return CREATIVE_MODE_TABS.register(id, () -> CreativeModeTab.builder()
				.title(Component.translatable(title)).icon(icon).build());
	}

	@SubscribeEvent
	public static void onBuildContents(BuildCreativeModeTabContentsEvent event) {
		CreativeModeTab selected = event.getTab();
		if (selected == FRAGMENTS.get()) {
			event.accept(ChromaItems.INFO_FRAGMENT.get());
			for (var page : reika.chromaticraft.magic.progression.LexiconCatalog.obtainablePages())
				event.accept(reika.chromaticraft.item.ItemInfoFragment.forPage(page));
			return;
		}

		if (selected == CHROMATICRAFT.get() || selected == DECORATION.get() || selected == WORLDGEN.get()) {
			Set<Item> seen = new HashSet<>();
			for (var holder : ChromaBlocks.ITEMS.getEntries()) {
				Item item = holder.get();
				if (!(item instanceof BlockItem blockItem) || item == Items.AIR || !seen.add(item))
					continue;
				if (selected == tabFor(category(blockItem.getBlock())).get())
					event.accept(item);
			}
			return;
		}

		if (selected != TOOLS.get() && selected != ITEMS.get())
			return;
		Set<Item> seen = new HashSet<>();
		for (var holder : ChromaItems.ITEMS.getEntries()) {
			Item item = holder.get();
			if (item == ChromaItems.INFO_FRAGMENT.get() || !seen.add(item))
				continue;
			if ((selected == TOOLS.get()) == isTool(item))
				event.accept(item);
		}
		if (selected == TOOLS.get()) {
			event.accept(reika.chromaticraft.item.ItemChromaBook.creativeStack());
			event.accept(reika.chromaticraft.items.tools.ItemEnderCrystal.filledCreativeStack(
					ChromaItems.ENDER_CRYSTAL_MOVER.get()));
		}
		else {
			for (StorageCrystalTier tier : StorageCrystalTier.list)
				event.accept(reika.chromaticraft.items.ItemStorageCrystal.fullStack(tier));
		}
	}

	private static boolean isTool(Item item) {
		String type = item.getClass().getName();
		return type.contains(".items.tools.") || type.endsWith(".ItemManipulator")
				|| type.endsWith(".ItemChromaBook") || type.endsWith(".ItemDoorKey")
				|| item instanceof BucketItem;
	}

	private static BlockCategory category(Block block) {
		String type = block.getClass().getName();
		String id = BuiltInRegistries.BLOCK.getKey(block).getPath();
		if (type.contains(".block.worldgen26.") || type.contains(".block.dimension.")
				|| type.contains(".block.dye26.") || type.endsWith(".BlockCaveCrystal")
				|| id.contains("ore") || id.contains("artefact") || id.contains("warp_node"))
			return BlockCategory.WORLDGEN;
		if (type.contains(".block.decoration.") || type.endsWith(".BlockCrystalLamp")
				|| type.endsWith(".BlockSuperCrystal") || id.contains("decor")
				|| id.contains("altar") || id.contains("glass"))
			return BlockCategory.DECORATION;
		return BlockCategory.MAIN;
	}

	private static DeferredHolder<CreativeModeTab, CreativeModeTab> tabFor(BlockCategory category) {
		return switch (category) {
			case MAIN -> CHROMATICRAFT;
			case DECORATION -> DECORATION;
			case WORLDGEN -> WORLDGEN;
		};
	}

	private enum BlockCategory { MAIN, DECORATION, WORLDGEN }

	private ChromaTabs() {}
}
