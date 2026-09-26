package reika.chromaticraft.container;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import reika.chromaticraft.registry.ChromaMenus;
import reika.chromaticraft.tileentity.recipe.TileEntityCrystalBrewer;
import reika.dragonapi.base.CoreContainer;

/**
 * V33a {@code ContainerCrystalBrewer}: three bottle slots under the shard slot, and the brew time sent to the client.
 * The slots sit where 26.2's brewing stand art has them (the bottles 5px lower than 1.7.10's).
 */
public final class MenuCrystalBrewer extends CoreContainer<TileEntityCrystalBrewer> {

	private int clientBrewTime = TileEntityCrystalBrewer.BREW_TIME;

	public MenuCrystalBrewer(int id, Inventory inventory, FriendlyByteBuf data) {
		this(id, inventory, (TileEntityCrystalBrewer)inventory.player.level().getBlockEntity(data.readBlockPos()));
	}

	public MenuCrystalBrewer(int id, Inventory inventory, TileEntityCrystalBrewer te) {
		super(ChromaMenus.CRYSTAL_BREWER.get(), id, inventory, requireBrewer(te));
		this.addSlot(new BrewerSlot(te, 1, 56, 51));
		this.addSlot(new BrewerSlot(te, 2, 79, 58));
		this.addSlot(new BrewerSlot(te, 3, 102, 51));
		this.addSlot(new BrewerSlot(te, 0, 79, 17));
		this.addPlayerInventory(inventory);
		this.addDataSlot(new DataSlot() {
			@Override public int get() {
				return te.getLevel() != null && !te.getLevel().isClientSide() ? te.getBrewTime() : clientBrewTime;
			}
			@Override public void set(int value) { clientBrewTime = value; }
		});
	}

	private static TileEntityCrystalBrewer requireBrewer(TileEntityCrystalBrewer te) {
		if (te == null) throw new IllegalStateException("Crystal Brewery menu opened without its block entity");
		return te;
	}

	public int getBrewTime() {
		return clientBrewTime;
	}

	@Override
	public ItemStack quickMoveStack(Player player, int slotIndex) {
		if (slotIndex < 0 || slotIndex >= slots.size()) return ItemStack.EMPTY;
		Slot slot = slots.get(slotIndex);
		if (!slot.hasItem()) return ItemStack.EMPTY;
		ItemStack source = slot.getItem();
		ItemStack result = source.copy();
		if (slotIndex < 4) {
			if (!moveItemStackTo(source, 4, slots.size(), true)) return ItemStack.EMPTY;
		}
		else if (!moveItemStackTo(source, 3, 4, false) && !moveItemStackTo(source, 0, 3, false)) {
			return ItemStack.EMPTY;
		}
		if (source.isEmpty()) slot.set(ItemStack.EMPTY);
		else slot.setChanged();
		return result;
	}

	/** A one-item slot that takes what the brewer accepts there. */
	private static final class BrewerSlot extends Slot {

		private final TileEntityCrystalBrewer te;

		private BrewerSlot(TileEntityCrystalBrewer te, int slot, int x, int y) {
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
