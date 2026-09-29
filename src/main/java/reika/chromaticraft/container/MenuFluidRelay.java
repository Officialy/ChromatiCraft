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
import net.minecraft.world.level.material.Fluid;

import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import org.jspecify.annotations.Nullable;

import reika.chromaticraft.network.ChromaNetwork;
import reika.chromaticraft.registry.ChromaMenus;
import reika.chromaticraft.tileentity.transport.TileEntityFluidRelay;
import reika.dragonapi.base.CoreContainer;
import reika.dragonapi.libraries.ReikaFluidHelper;

/**
 * V33a {@code ContainerFluidRelay}: seven filter slots over the player inventory. Left-clicking a slot sets it to the
 * fluid in the held container item (or clears it with an empty hand); right-clicking selects the slot for the GUI's
 * letter search, whose pick is sent back with {@link #setFluid}. V33a's ghost slots had no inventory behind them;
 * 26.x slots need one, so they sit on an inert container nothing can be put into or taken from.
 */
public final class MenuFluidRelay extends CoreContainer<TileEntityFluidRelay> {

	public static final int FILTER_SLOTS = 7;

	private final TileEntityFluidRelay relay;
	private int manualSelectionSlot = -1;

	public MenuFluidRelay(int id, Inventory inventory, FriendlyByteBuf data) {
		this(id, inventory, (TileEntityFluidRelay)inventory.player.level().getBlockEntity(data.readBlockPos()));
	}

	public MenuFluidRelay(int id, Inventory inventory, TileEntityFluidRelay te) {
		super(ChromaMenus.FLUID_RELAY.get(), id, inventory, requireRelay(te));
		relay = te;
		SimpleContainer ghosts = new SimpleContainer(FILTER_SLOTS);
		for (int i = 0; i < FILTER_SLOTS; i++) {
			int x = 14+22*i;
			int y = 17;
			this.addSlot(new Slot(ghosts, i, x, y) {
				@Override public boolean mayPlace(ItemStack stack) { return false; }
				@Override public boolean mayPickup(Player player) { return false; }
			});
		}
		this.addPlayerInventoryWithOffset(inventory, 0, -16);
	}

	private static TileEntityFluidRelay requireRelay(TileEntityFluidRelay te) {
		if (te == null) throw new IllegalStateException("Fluid Link Anchor menu opened without its block entity");
		return te;
	}

	public TileEntityFluidRelay relay() {
		return relay;
	}

	public int getManualSelectSlot() {
		return manualSelectionSlot;
	}

	/** V33a {@code setFluid}: from the client, sent to the server as RELAYFLUIDKEY for the selected slot. */
	public void setFluid(@Nullable Fluid f) {
		if (relay.getLevel() != null && relay.getLevel().isClientSide())
			ClientPacketDistributor.sendToServer(new ChromaNetwork.FluidRelayControl(relay.getBlockPos(),
					ChromaNetwork.FluidRelayControl.SET_FLUID, manualSelectionSlot, fluidId(f)));
		else
			relay.setFluid(manualSelectionSlot, f);
	}

	public static int fluidId(@Nullable Fluid f) {
		return f == null ? -1 : net.minecraft.core.registries.BuiltInRegistries.FLUID.getId(f);
	}

	@Override
	public void clicked(int slot, int mouse, ContainerInput action, Player ep) {
		manualSelectionSlot = -1;
		if (slot >= 0 && slot < FILTER_SLOTS) {
			ItemStack held = this.getCarried();
			if (mouse == 0) {
				var fs = held.isEmpty() ? null : ReikaFluidHelper.getFluidForItem(held);
				if (!ep.level().isClientSide())
					relay.setFluid(slot, fs != null && !fs.isEmpty() ? fs.getFluid() : null);
			}
			else if (mouse == 1) {
				manualSelectionSlot = slot;
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
