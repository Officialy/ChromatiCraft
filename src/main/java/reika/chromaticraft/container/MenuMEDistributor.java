/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.container;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import reika.chromaticraft.modinterface.ae.TileEntityMEDistributor;
import reika.chromaticraft.registry.ChromaMenus;
import reika.dragonapi.base.CoreContainer;

/**
 * V33a {@code ContainerMEDistributor}: ten pairs of ghost slots - "query" on the left, "transfer" on the right of
 * each column - over the player inventory. Clicking a pair slot with an item sets that mapping to one of it (an empty
 * hand clears it); nothing is taken from the player. The slots are drawn by the screen from the tile's mappings.
 */
public final class MenuMEDistributor extends CoreContainer<TileEntityMEDistributor> {

	private final TileEntityMEDistributor tile;

	public MenuMEDistributor(int id, Inventory inventory, FriendlyByteBuf data) {
		this(id, inventory, (TileEntityMEDistributor)inventory.player.level().getBlockEntity(data.readBlockPos()));
	}

	public MenuMEDistributor(int id, Inventory inventory, TileEntityMEDistributor te) {
		super(ChromaMenus.ME_DISTRIBUTOR.get(), id, inventory, require(te));
		tile = te;
		SimpleContainer ghosts = new SimpleContainer(TileEntityMEDistributor.NSLOTS * 2);
		int w = 67;
		int w2 = 115;
		for (int i = 0; i < TileEntityMEDistributor.NSLOTS; i++) {
			int dx = 8;
			if (i >= 5)
				dx += w2;
			int dy = 7 + (i % 5) * 20 + 9;
			this.addSlot(ghost(ghosts, i, dx + w, dy));
			this.addSlot(ghost(ghosts, i + TileEntityMEDistributor.NSLOTS, dx, dy));
		}
		this.addPlayerInventoryWithOffset(inventory, 19, 49);
	}

	private static Slot ghost(SimpleContainer c, int idx, int x, int y) {
		return new Slot(c, idx, x, y) {
			@Override public boolean mayPlace(ItemStack stack) { return false; }
			@Override public boolean mayPickup(Player player) { return false; }
		};
	}

	private static TileEntityMEDistributor require(TileEntityMEDistributor te) {
		if (te == null) throw new IllegalStateException("ME Item Distributor menu opened without its block entity");
		return te;
	}

	public TileEntityMEDistributor tile() {
		return tile;
	}

	@Override
	public void clicked(int slot, int mouse, ContainerInput action, Player ep) {
		if (slot >= 0 && slot < TileEntityMEDistributor.NSLOTS * 2) {
			if (mouse == 0 && !ep.level().isClientSide()) {
				int idx = this.getSlot(slot).getContainerSlot();
				tile.setMapping(idx, this.getCarried().isEmpty() ? ItemStack.EMPTY : this.getCarried().copyWithCount(1));
			}
			return;
		}
		super.clicked(slot, mouse, action, ep);
	}

	@Override
	public ItemStack quickMoveStack(Player player, int slot) {
		return ItemStack.EMPTY;
	}

}
