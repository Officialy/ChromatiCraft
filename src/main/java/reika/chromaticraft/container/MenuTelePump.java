package reika.chromaticraft.container;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import reika.chromaticraft.registry.ChromaMenus;
import reika.chromaticraft.tileentity.acquisition.TileEntityTeleportationPump;
import reika.dragonapi.base.CoreContainer;

/**
 * V33a {@code ContainerTelePump}: the storage crystal (slot 0) and bucket (slot 1) over the player inventory, which is
 * shifted 9 right and 2 down. V33a synced the tank by packet every tick; the tank's level and fluid are data slots.
 */
public final class MenuTelePump extends CoreContainer<TileEntityTeleportationPump> {

	private int clientLevel;
	private int clientFluid = -1;

	public MenuTelePump(int id, Inventory inventory, FriendlyByteBuf data) {
		this(id, inventory, (TileEntityTeleportationPump)inventory.player.level().getBlockEntity(data.readBlockPos()));
	}

	public MenuTelePump(int id, Inventory inventory, TileEntityTeleportationPump te) {
		super(ChromaMenus.TELEPUMP.get(), id, inventory, te);

		this.addSlot(0, 152, 35);
		this.addSlot(1, 152, 54);

		//this.addSlot(2, 170, 54);
		//this.addSlot(3, 170, 35);

		this.addPlayerInventoryWithOffset(inventory, 9, 2);

		this.addDataSlot(new DataSlot() {
			@Override public int get() { return serverSide() ? te.getTankLevel() : clientLevel; }
			@Override public void set(int value) { clientLevel = value; }
		});
		this.addDataSlot(new DataSlot() {
			@Override public int get() {
				Fluid f = te.getTankFluid();
				return serverSide() ? (f != null ? BuiltInRegistries.FLUID.getId(f) : -1) : clientFluid;
			}
			@Override public void set(int value) { clientFluid = value; }
		});
	}

	private boolean serverSide() {
		return tile.getLevel() != null && !tile.getLevel().isClientSide();
	}

	public TileEntityTeleportationPump pump() {
		return tile;
	}

	/** V33a {@code getLiquidScaled} against the synced tank level. */
	public int getLiquidScaled(int a) {
		return a*clientLevel/4000;
	}

	public Fluid tankFluid() {
		return clientFluid >= 0 ? BuiltInRegistries.FLUID.byId(clientFluid) : Fluids.EMPTY;
	}

}
