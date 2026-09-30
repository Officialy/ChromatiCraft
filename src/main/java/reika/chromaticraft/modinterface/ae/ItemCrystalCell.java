/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.modinterface.ae;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.ItemLike;

import appeng.api.config.FuzzyMode;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEKeyType;
import appeng.api.storage.StorageCells;
import appeng.api.storage.cells.IBasicCellItem;
import appeng.api.storage.cells.StorageCell;
import appeng.util.ConfigInventory;
import reika.chromaticraft.base.ItemChromaTool;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.registry.ChromaItems;
import reika.chromaticraft.registry.CrystalElement;

/**
 * V33a {@code ItemCrystalCell}: a 1,048,576-byte, 16-type ME storage cell that only takes one "crystal type" - the
 * sixteen colours of one crystal item or block (shards, berries, seeds, stones, pendants, cave crystals, lamps,
 * runes, dye blocks, plants...) or vanilla dye - so all sixteen fit in one cell.
 *
 * <p>V33a told the colours of one type apart by metadata ({@code damage/16} was the type). The port split every
 * colour into its own item, each colour family being one {@code CrystalElement}-keyed map in {@link ChromaItems}
 * or {@link ChromaBlocks}, so the family is looked up from those maps. Dimension cores, as in V33a, only stack with
 * the exact same core. V33a's crystal cell partition inventory was never saved, so it has none here.
 *
 * <p>CHROMA-PORT: V33a also drew the stored type, cycling through its colours, over the cell's icon while Shift was
 * held ({@code SpriteRenderCallback}); that needs a custom item model.
 */
public class ItemCrystalCell extends ItemChromaTool implements IBasicCellItem {

	/** V33a colour families that were not CrystalTypesProxy / ItemCrystalBasic / crystal or dye ItemBlocks. */
	private static final Set<String> NOT_CRYSTAL_TYPES = Set.of("CRYSTAL_POTIONS", "SPLASH_CRYSTAL_POTIONS", "TINTED_LENSES", "LUMEN_RELAYS");
	private static final String DYES = "DYE";
	private static final String EXACT = "DIMENSION_CORES";

	private static Map<Item, String> families;

	public ItemCrystalCell(Properties properties) {
		super(properties);
	}

	@Override
	public AEKeyType getKeyType() {
		return AEKeyType.items();
	}

	@Override
	public boolean isEditable(ItemStack is) {
		return true;
	}

	@Override
	public ConfigInventory getConfigInventory(ItemStack is) {
		return ConfigInventory.emptyTypes();
	}

	@Override
	public FuzzyMode getFuzzyMode(ItemStack is) {
		return FuzzyMode.IGNORE_ALL;
	}

	@Override
	public void setFuzzyMode(ItemStack is, FuzzyMode fzMode) {

	}

	@Override
	public int getBytes(ItemStack cellItem) { //Total storage
		return 262144 * 4; //increased 4x
	}

	@Override
	public int getBytesPerType(ItemStack cellItem) {
		return 1;
	}

	@Override
	public int getTotalTypes(ItemStack cellItem) {
		return 16;
	}

	@Override
	public boolean isBlackListed(ItemStack cellItem, AEKey requestedAddition) {
		return !(requestedAddition instanceof AEItemKey key) || !isItemValid(cellItem, key.toStack());
	}

	@Override
	public boolean storableInStorageCell() {
		return false;
	}

	@Override
	public double getIdleDrain() {
		return 2;
	}

	@Override
	public void appendHoverText(ItemStack is, TooltipContext ctx, TooltipDisplay display, Consumer<Component> li, TooltipFlag flag) {
		this.addCellInformationToTooltip(is, li);
		ItemStack in = getStoredItemType(is);
		if (!in.isEmpty()) {
			String family = getFamily(in.getItem());
			li.accept(DYES.equals(family) ? Component.literal("Stores Dye.") : Component.literal("Stores ").append(in.getHoverName()).append("."));
		}
	}

	/** An item of the type the cell holds, or EMPTY if it is empty. */
	public static ItemStack getStoredItemType(ItemStack is) {
		StorageCell cellInv = StorageCells.getCellInventory(is, null);
		if (cellInv == null)
			return ItemStack.EMPTY;
		AEKey key = cellInv.getAvailableStacks().getFirstKey();
		return key instanceof AEItemKey iae ? iae.toStack() : ItemStack.EMPTY;
	}

	public static boolean isItemValid(ItemStack cell, ItemStack is) {
		String fam = getFamily(is.getItem());
		if (fam == null)
			return false;
		ItemStack in = getStoredItemType(cell);
		if (in.isEmpty())
			return true;
		if (EXACT.equals(fam))
			return ItemStack.isSameItemSameComponents(is, in);
		return fam.equals(getFamily(in.getItem())); //V33a: same item, same damage/16 (plain and boosted shards are separate families here too)
	}

	public static boolean isTypeStorable(ItemStack is) {
		return getFamily(is.getItem()) != null;
	}

	@Nullable
	private static String getFamily(Item item) {
		if (families == null)
			families = buildFamilies();
		return families.get(item);
	}

	private static Map<Item, String> buildFamilies() {
		Map<Item, String> map = new HashMap<>();
		scan(ChromaItems.class, map);
		scan(ChromaBlocks.class, map);
		for (Item item : net.minecraft.core.registries.BuiltInRegistries.ITEM) {
			if (item instanceof DyeItem)
				map.put(item, DYES);
		}
		return map;
	}

	/** Every static CrystalElement-keyed map of registry holders is one colour family. */
	private static void scan(Class<?> registry, Map<Item, String> map) {
		for (Field f : registry.getDeclaredFields()) {
			if (!Modifier.isStatic(f.getModifiers()) || !Map.class.isAssignableFrom(f.getType()) || NOT_CRYSTAL_TYPES.contains(f.getName()))
				continue;
			try {
				f.setAccessible(true);
				Map<?, ?> m = (Map<?, ?>)f.get(null);
				if (m == null || m.isEmpty() || !(m.keySet().iterator().next() instanceof CrystalElement))
					continue;
				for (Object v : m.values()) {
					Object o = v instanceof Supplier<?> s ? s.get() : v;
					if (o instanceof ItemLike il && il.asItem() != null && il.asItem() != net.minecraft.world.item.Items.AIR)
						map.put(il.asItem(), f.getName());
				}
			}
			catch (ReflectiveOperationException | RuntimeException e) {
				e.printStackTrace();
			}
		}
	}

}
