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
import reika.dragonapi.interfaces.blockentity.InertIInv;

/** V33a inventory receiver semantics on the managed 26.2 container and persistence APIs. */
public abstract class InventoriedCrystalReceiver extends CrystalReceiverBase implements WorldlyContainer {
	protected final NonNullList<ItemStack> inv;
	protected InventoriedCrystalReceiver(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
		inv = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
	}
	public abstract int getSizeInventory();
	@Override public final int getContainerSize() { return this.getSizeInventory(); }
	@Override public final int[] getSlotsForFace(Direction side) {
		if (this instanceof InertIInv) return new int[0];
		int[] slots = new int[this.getContainerSize()];
		for (int i = 0; i < slots.length; i++) slots[i] = i;
		return slots;
	}
	@Override public final boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
		return !(this instanceof InertIInv) && this.canPlaceItem(slot, stack);
	}
	@Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
		return !(this instanceof InertIInv);
	}
	@Override public final boolean isEmpty() { return inv.stream().allMatch(ItemStack::isEmpty); }
	@Override public final ItemStack getItem(int slot) { return inv.get(slot); }
	@Override public final void setItem(int slot, ItemStack stack) {
		inv.set(slot, stack);
		stack.limitSize(this.getMaxStackSize());
		this.onInventorySlotChanged(slot);
		this.setChanged();
	}
	protected void onInventorySlotChanged(int slot) {}
	@Override public final ItemStack removeItem(int slot, int amount) {
		ItemStack removed = ContainerHelper.removeItem(inv, slot, amount);
		if (!removed.isEmpty()) { this.onInventorySlotChanged(slot); this.setChanged(); }
		return removed;
	}
	@Override public final ItemStack removeItemNoUpdate(int slot) {
		ItemStack removed = ContainerHelper.takeItem(inv, slot);
		if (!removed.isEmpty()) this.onInventorySlotChanged(slot);
		return removed;
	}
	@Override public boolean stillValid(Player player) { return this.isPlayerAccessible(player); }
	@Override public void clearContent() { inv.clear(); this.setChanged(); }
	@Override public void setChanged() {
		super.setChanged();
		if (this.getLevel() != null)
			this.getLevel().updateNeighbourForOutputSignal(this.getBlockPos(), this.getBlockState().getBlock());
	}
	@Override protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		inv.clear();
		ContainerHelper.loadAllItems(input, inv);
	}
	@Override protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, inv);
	}
}
