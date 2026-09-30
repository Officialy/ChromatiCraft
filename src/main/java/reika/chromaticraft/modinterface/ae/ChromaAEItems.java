package reika.chromaticraft.modinterface.ae;

import net.minecraft.world.item.Item;

import reika.chromaticraft.registry.ChromaItems;

/**
 * Constructors for ChromatiCraft's Applied Energistics 2 items, whose classes implement AE interfaces. Kept out of
 * {@link ChromaItems} so that class never has to load them; only called when AE2 is loaded (V33a registered these
 * items only with {@code ModList.APPENG}).
 */
public final class ChromaAEItems {

	private ChromaAEItems() {

	}

	public static Item voidCell() {
		return new ItemVoidStorage(ChromaItems.itemProperties());
	}

	public static Item crystalCell() {
		return new ItemCrystalCell(ChromaItems.itemProperties());
	}

	public static Item shieldedCell() {
		return new ItemShieldedCell(ChromaItems.itemProperties());
	}

}
