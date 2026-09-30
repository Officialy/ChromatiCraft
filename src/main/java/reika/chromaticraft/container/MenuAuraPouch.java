package reika.chromaticraft.container;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.items.tools.ItemAuraPouch;
import reika.chromaticraft.registry.ChromaMenus;

/**
 * V33a {@code ContainerAuraPouch}: the held pouch's 27 slots over the player inventory. Every change is written
 * straight back into the pouch in hand, and again on close. Shift-clicking moves nothing. Right-clicking a pouch slot
 * toggles it active instead of picking up, as V33a did for any click with button 1. A pouch in the hotbar can be
 * neither clicked nor swapped in with its number key (V33a's slot check and its HotbarKeyEvent cancel), so the open
 * pouch cannot be moved into itself. V33a also refused clicks on a pouch slot whose index held a pouch in the player's
 * inventory; that check is kept as written.
 */
public final class MenuAuraPouch extends AbstractContainerMenu {

	private final Player player;
	private final SimpleContainer inventory = new SimpleContainer(ItemAuraPouch.SIZE) {
		@Override
		public void setChanged() {
			super.setChanged();
			MenuAuraPouch.this.slotsChanged(this);
		}
	};
	private boolean loading = true;

	public MenuAuraPouch(int id, Inventory inv, FriendlyByteBuf ignored) {
		this(id, inv);
	}

	public MenuAuraPouch(int id, Inventory inv) {
		super(ChromaMenus.AURA_POUCH.get(), id);
		player = inv.player;

		int w = 9;
		int h = ItemAuraPouch.SIZE/w;
		for (int i = 0; i < h; ++i)
			for (int k = 0; k < w; ++k)
				this.addSlot(new Slot(inventory, k+i*w, 8+k*18, 17+i*18));
		for (int i = 0; i < 3; ++i)
			for (int k = 0; k < 9; ++k)
				this.addSlot(new Slot(inv, k+i*9+9, 8+k*18, 84+i*18));
		for (int i = 0; i < 9; ++i)
			this.addSlot(new Slot(inv, i, 8+i*18, 142));

		ItemStack tool = player.getMainHandItem();
		if (!(tool.getItem() instanceof ItemAuraPouch iil)) {
			ChromatiCraft.LOGGER.error("Opened an Aura Pouch GUI without holding an aura pouch!?");
			loading = false;
			return;
		}
		ItemStack[] li = iil.getInventory(tool);
		for (int i = 0; i < li.length; i++)
			inventory.setItem(i, li[i]);
		loading = false;
	}

	/** V33a {@code onCraftMatrixChanged}: the pouch in hand takes the slots' contents. */
	@Override
	public void slotsChanged(Container ii) {
		super.slotsChanged(ii);
		if (!loading)
			this.writeBack();
	}

	private void writeBack() {
		ItemStack is = player.getMainHandItem();
		if (is.getItem() instanceof ItemAuraPouch iil) {
			ItemStack[] items = new ItemStack[ItemAuraPouch.SIZE];
			for (int i = 0; i < items.length; i++)
				items[i] = inventory.getItem(i).copy();
			iil.setItems(is, items);
		}
	}

	public int getSize() {
		return inventory.getContainerSize();
	}

	@Override
	public boolean stillValid(Player ep) {
		return ep.getMainHandItem().getItem() instanceof ItemAuraPouch;
	}

	@Override
	public ItemStack quickMoveStack(Player ep, int slot) {
		return ItemStack.EMPTY;
	}

	@Override
	public void clicked(int slot, int button, ContainerInput input, Player ep) {
		// V33a HotbarKeyEvent: no number-key swap of a pouch while the pouch GUI is open.
		if (input == ContainerInput.SWAP && button >= 0 && button < 9
				&& ep.getInventory().getItem(button).getItem() instanceof ItemAuraPouch)
			return;
		boolean inGUI = slot < ItemAuraPouch.SIZE && slot >= 0;
		if (inGUI) {
			if (ep.getInventory().getItem(slot).getItem() instanceof ItemAuraPouch)
				return;
			if (button == 1) {
				ItemStack is = player.getMainHandItem();
				if (is.getItem() instanceof ItemAuraPouch iil) {
					boolean[] act = iil.getActiveSlots(is);
					iil.setSlotActive(is, slot, !act[slot]);
				}
				return;
			}
			super.clicked(slot, button, input, ep);
			return;
		}
		else if (slot >= ItemAuraPouch.SIZE+27) {
			ItemStack in = ep.getInventory().getItem(slot-ItemAuraPouch.SIZE-27);
			if (in.getItem() instanceof ItemAuraPouch)
				return;
		}
		super.clicked(slot, button, input, ep);
	}

	@Override
	public void removed(Player ep) {
		super.removed(ep);
		ItemStack is = ep.getMainHandItem();
		if (!(is.getItem() instanceof ItemAuraPouch)) {
			ChromatiCraft.LOGGER.error("Tried to access aura pouch inventory without a pouch!");
			return;
		}
		this.writeBack();
	}

}
