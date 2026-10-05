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

import appeng.api.config.FuzzyMode;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEKeyType;
import appeng.api.storage.cells.IBasicCellItem;
import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.UpgradeInventories;
import appeng.items.contents.CellConfig;
import appeng.util.ConfigInventory;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import reika.chromaticraft.base.ItemChromaTool;

import java.util.Set;
import java.util.function.Consumer;

/**
 * V33a {@code ItemShieldedCell}, the Corrosion Resistant Cell: a small (1024-byte, two-type) ME storage cell that an
 * Unknown Artefact cannot corrupt - the artefact's ME power drain ({@link UAMESystemEffect}) ignores it. Two upgrade
 * slots and a 63-slot partition, like the Void Cell.
 */
public class ItemShieldedCell extends ItemChromaTool implements IBasicCellItem {

	public ItemShieldedCell(Properties properties) {
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
	public FuzzyMode getFuzzyMode(ItemStack is) {
		return FuzzyMode.IGNORE_ALL;
	}

	@Override
	public void setFuzzyMode(ItemStack is, FuzzyMode fzMode) {

	}

	@Override
	public IUpgradeInventory getUpgrades(ItemStack is) {
		return UpgradeInventories.forItem(is, 2);
	}

	@Override
	public ConfigInventory getConfigInventory(ItemStack is) {
		return CellConfig.create(Set.of(AEKeyType.items()), is, 63);
	}

	@Override
	public int getBytes(ItemStack cellItem) { //Total storage
		return 1024;
	}

	@Override
	public int getBytesPerType(ItemStack cellItem) {
		return 1;
	}

	@Override
	public int getTotalTypes(ItemStack cellItem) {
		return 2;
	}

	@Override
	public boolean isBlackListed(ItemStack cellItem, AEKey requestedAddition) {
		return false;
	}

	@Override
	public boolean storableInStorageCell() {
		return false;
	}

	@Override
	public double getIdleDrain() {
		return 4;
	}

	@Override
	public void appendHoverText(ItemStack is, TooltipContext ctx, TooltipDisplay display, Consumer<Component> li, TooltipFlag flag) {
		this.addCellInformationToTooltip(is, li);
	}

}
