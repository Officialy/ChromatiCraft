package reika.chromaticraft.registry;

import java.util.function.Supplier;

import net.minecraft.world.item.Item;

import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import reika.chromaticraft.ChromatiCraft;

/**
 * ChromatiCraft item registry. Port-in-progress rewrite of the 1.7.10 {@code ChromaItems} enum into
 * 26.2 {@link DeferredRegister} form (mirrors ReactorItems); grows as items are ported. Block items
 * are registered by {@link ChromaBlocks}.
 */
public final class ChromaItems {

	public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ChromatiCraft.MODID);

	public static Item.Properties itemProperties() {
		return new Item.Properties();
	}

	static DeferredItem<Item> reg(String name, Supplier<Item> factory) {
		return ITEMS.register(name, factory);
	}

	private ChromaItems() {}
}
