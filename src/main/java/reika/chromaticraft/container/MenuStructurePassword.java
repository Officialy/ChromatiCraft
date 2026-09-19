package reika.chromaticraft.container;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import reika.chromaticraft.registry.ChromaMenus;
import reika.chromaticraft.tileentity.TileEntityStructurePassword;

/** V33a structure-password layout: eight single-crystal slots and the player inventory. */
public final class MenuStructurePassword extends AbstractContainerMenu {

	private final TileEntityStructurePassword password;
	private final Player player;

	public MenuStructurePassword(int id, Inventory inventory, FriendlyByteBuf data) {
		this(id, inventory, requirePassword(inventory, data));
	}

	public MenuStructurePassword(int id, Inventory inventory, TileEntityStructurePassword password) {
		super(ChromaMenus.STRUCTURE_PASSWORD.get(), id);
		this.password = password;
		player = inventory.player;
		for (int slot = 0; slot < 8; slot++) this.addSlot(new Slot(password, slot, 17 + slot * 18, 25) {
			@Override public boolean mayPlace(ItemStack stack) {
				return TileEntityStructurePassword.elementOf(stack) != null;
			}
			@Override public int getMaxStackSize() { return 1; }
		});
		for (int row = 0; row < 3; row++) for (int column = 0; column < 9; column++)
			this.addSlot(new Slot(inventory, column + row * 9 + 9,
					8 + column * 18, 55 + row * 18));
		for (int column = 0; column < 9; column++)
			this.addSlot(new Slot(inventory, column, 8 + column * 18, 113));
	}

	private static TileEntityStructurePassword requirePassword(Inventory inventory, FriendlyByteBuf data) {
		if (inventory.player.level().getBlockEntity(data.readBlockPos())
				instanceof TileEntityStructurePassword password) return password;
		throw new IllegalStateException("Structure Password menu opened without its block entity");
	}

	public TileEntityStructurePassword password() { return password; }

	@Override public boolean stillValid(Player player) { return password.stillValid(player); }

	@Override
	public void slotsChanged(net.minecraft.world.Container container) {
		super.slotsChanged(container);
		if (container == password) password.checkPassword(player);
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
		Slot slot = slots.get(index);
		if (!slot.hasItem()) return ItemStack.EMPTY;
		ItemStack source = slot.getItem();
		ItemStack result = source.copy();
		if (index < 8) {
			if (!moveItemStackTo(source, 8, slots.size(), true)) return ItemStack.EMPTY;
		}
		else if (TileEntityStructurePassword.elementOf(source) == null
				|| !moveItemStackTo(source, 0, 8, false)) return ItemStack.EMPTY;
		if (source.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
		password.checkPassword(player);
		return result;
	}
}
