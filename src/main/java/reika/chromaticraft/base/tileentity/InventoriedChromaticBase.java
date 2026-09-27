/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.base.tileentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import reika.dragonapi.interfaces.blockentity.ConditionBreakDropsInventory;
import reika.dragonapi.interfaces.blockentity.InertIInv;

/**
 * V33a {@code InventoriedChromaticBase}: a ChromatiCraft tile with an inventory, every slot open from every side unless
 * the tile is an {@link InertIInv}, insertion gated by {@link #canPlaceItem}. On 26.2's container and persistence APIs,
 * as {@link InventoriedCrystalReceiver} does for the networked tiles; V33a's {@code onPreSlotSet}/{@code onSlotSet}
 * hooks are kept.
 */
public abstract class InventoriedChromaticBase extends TileEntityChromaticBase implements WorldlyContainer, ConditionBreakDropsInventory {

	protected final NonNullList<ItemStack> inv;

	protected InventoriedChromaticBase(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
		inv = NonNullList.withSize(this.getSizeInventory(), ItemStack.EMPTY);
	}

	public abstract int getSizeInventory();

	@Override
	public final int getContainerSize() {
		return this.getSizeInventory();
	}

	@Override
	public final int[] getSlotsForFace(Direction side) {
		if (this instanceof InertIInv)
			return new int[0];
		int[] slots = new int[this.getContainerSize()];
		for (int i = 0; i < slots.length; i++)
			slots[i] = i;
		return slots;
	}

	@Override
	public final boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
		if (this instanceof InertIInv)
			return false;
		return this.canPlaceItem(slot, stack);
	}

	@Override
	public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
		return !(this instanceof InertIInv);
	}

	@Override
	public final boolean isEmpty() {
		return inv.stream().allMatch(ItemStack::isEmpty);
	}

	@Override
	public final ItemStack getItem(int slot) {
		return inv.get(slot);
	}

	@Override
	public final void setItem(int slot, ItemStack is) {
		this.onPreSlotSet(slot, is);
		inv.set(slot, is);
		is.limitSize(this.getMaxStackSize());
		this.onSlotSet(slot, is);
		this.setChanged();
	}

	protected void onPreSlotSet(int slot, ItemStack is) {

	}

	protected void onSlotSet(int slot, ItemStack is) {

	}

	@Override
	public final ItemStack removeItem(int slot, int amount) {
		ItemStack removed = ContainerHelper.removeItem(inv, slot, amount);
		if (!removed.isEmpty()) {
			this.onSlotSet(slot, inv.get(slot));
			this.setChanged();
		}
		return removed;
	}

	@Override
	public final ItemStack removeItemNoUpdate(int slot) {
		ItemStack removed = ContainerHelper.takeItem(inv, slot);
		if (!removed.isEmpty())
			this.onSlotSet(slot, ItemStack.EMPTY);
		return removed;
	}

	@Override
	public boolean stillValid(Player player) {
		return this.isPlayerAccessible(player);
	}

	@Override
	public void clearContent() {
		inv.clear();
		this.setChanged();
	}

	/** V33a {@code markDirty}: also tells comparators and neighbours the contents changed. */
	@Override
	public void setChanged() {
		super.setChanged();
		if (this.getLevel() != null)
			this.getLevel().updateNeighbourForOutputSignal(this.getBlockPos(), this.getBlockState().getBlock());
	}

	/**
	 * The contents drop from 26.2's {@code preRemoveSideEffects} (the stacks themselves, every component kept) however
	 * the block goes. DragonAPI's player-break inventory drop is skipped: it rebuilt the stacks without their
	 * components, and a block adding its contents to its loot as well dropped them twice when exploded.
	 */
	@Override
	public boolean dropsInventoryOnBroken() {
		return false;
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		inv.clear();
		ContainerHelper.loadAllItems(input, inv);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, inv);
	}

}
