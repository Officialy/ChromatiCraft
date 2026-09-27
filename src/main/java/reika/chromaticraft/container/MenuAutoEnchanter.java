package reika.chromaticraft.container;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import reika.chromaticraft.registry.ChromaMenus;
import reika.chromaticraft.tileentity.processing.TileEntityAutoEnchanter;
import reika.dragonapi.base.CoreContainer;

/**
 * V33a {@code ContainerAutoEnchanter}: the item to enchant at (80, 35), the donor item at (14, 51), the player's
 * inventory 15 lower than usual; progress and the chroma level are synchronised (V33a sent a progress bar and a tank
 * packet).
 */
public final class MenuAutoEnchanter extends CoreContainer<TileEntityAutoEnchanter> {

	private final ContainerData data;

	public MenuAutoEnchanter(int id, Inventory inventory, FriendlyByteBuf buf) {
		this(id, inventory, (TileEntityAutoEnchanter)inventory.player.level().getBlockEntity(buf.readBlockPos()), new SimpleContainerData(2));
	}

	public MenuAutoEnchanter(int id, Inventory inventory, TileEntityAutoEnchanter te, ContainerData data) {
		super(ChromaMenus.AUTO_ENCHANTER.get(), id, inventory, require(te));
		this.data = data;
		this.addSlot(new EnchanterSlot(te, 0, 80, 35));
		this.addSlot(new EnchanterSlot(te, 1, 14, 51));
		this.addPlayerInventoryWithOffset(inventory, 0, 15);
		this.addDataSlots(data);
	}

	private static TileEntityAutoEnchanter require(TileEntityAutoEnchanter te) {
		if (te == null) throw new IllegalStateException("Chromatic Enchanter menu opened without its block entity");
		return te;
	}

	/** Mirrored into the client tile, which the screen reads, as V33a's progress bar and tank packet were. */
	@Override
	public void setData(int index, int value) {
		super.setData(index, value);
		tile.data.set(index, value);
	}

	@Override
	public ItemStack quickMoveStack(Player player, int slotIndex) {
		if (slotIndex < 0 || slotIndex >= slots.size()) return ItemStack.EMPTY;
		Slot slot = slots.get(slotIndex);
		if (!slot.hasItem()) return ItemStack.EMPTY;
		ItemStack source = slot.getItem();
		ItemStack result = source.copy();
		if (slotIndex < 2) {
			if (!moveItemStackTo(source, 2, slots.size(), true)) return ItemStack.EMPTY;
		}
		else if (!moveItemStackTo(source, 0, 1, false)) {
			return ItemStack.EMPTY;
		}
		if (source.isEmpty()) slot.set(ItemStack.EMPTY);
		else slot.setChanged();
		return result;
	}

	/** A one-item slot; the main slot takes only what the enchanter could enchant with its current selection. */
	private static final class EnchanterSlot extends Slot {

		private final TileEntityAutoEnchanter te;

		private EnchanterSlot(TileEntityAutoEnchanter te, int slot, int x, int y) {
			super(te, slot, x, y);
			this.te = te;
		}

		@Override
		public boolean mayPlace(ItemStack stack) {
			return te.canPlaceItem(this.getContainerSlot(), stack);
		}

		@Override
		public int getMaxStackSize() {
			return 1;
		}
	}
}
