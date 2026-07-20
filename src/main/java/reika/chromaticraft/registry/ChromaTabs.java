package reika.chromaticraft.registry;

import java.util.HashSet;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import reika.chromaticraft.ChromatiCraft;

/**
 * ChromatiCraft creative tab(s). Port-in-progress: the 1.7.10 original had five metadata-tab objects
 * (tabChroma/Deco/Gen/Tools/Items) + a fragments tab; this starts with the single main tab (mirrors
 * ReactorTabs) and auto-collects every registered block/item, expanding into the multi-tab layout as
 * content ports.
 */
@EventBusSubscriber(modid = ChromatiCraft.MODID)
public final class ChromaTabs {

	public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
			DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ChromatiCraft.MODID);

	public static final DeferredHolder<CreativeModeTab, CreativeModeTab> CHROMATICRAFT =
			CREATIVE_MODE_TABS.register("chromaticraft", () -> CreativeModeTab.builder()
					.title(Component.translatable("tab.chromaticraft"))
					.icon(() -> new ItemStack(ChromaBlocks.STORAGE.get()))
					.build());

	@SubscribeEvent
	public static void onBuildContents(BuildCreativeModeTabContentsEvent event) {
		if (event.getTab() != CHROMATICRAFT.get())
			return;
		HashSet<Item> seen = new HashSet<>();
		// Iterate the BlockItem registry (not block.asItem()) so multi-variant blocks like crystalline
		// stone contribute all 16 of their BlockItems, not just the variant-0 item.
		for (var holder : ChromaBlocks.ITEMS.getEntries()) {
			Item item = holder.get();
			if (item != Items.AIR && seen.add(item))
				event.accept(item);
		}
		for (var holder : ChromaItems.ITEMS.getEntries()) {
			Item item = holder.get();
			if (seen.add(item))
				event.accept(item);
		}
	}

	private ChromaTabs() {}
}
