package reika.chromaticraft.container;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;

import reika.chromaticraft.registry.ChromaMenus;
import reika.chromaticraft.tileentity.TileEntityHeatLamp;

/** Slotless V33a Heat Lamp menu; its one synchronized value is the configured temperature. */
public final class MenuHeatLamp extends AbstractContainerMenu {

	private final TileEntityHeatLamp lamp;
	private int clientTemperature;

	public MenuHeatLamp(int id, Inventory inventory, FriendlyByteBuf data) {
		this(id, inventory, (TileEntityHeatLamp)inventory.player.level().getBlockEntity(data.readBlockPos()));
	}

	public MenuHeatLamp(int id, Inventory inventory, TileEntityHeatLamp lamp) {
		super(ChromaMenus.HEAT_LAMP.get(), id);
		if (lamp == null) throw new IllegalStateException("Heat Lamp menu opened without its block entity");
		this.lamp = lamp;
		clientTemperature = lamp.getTemperature();
		addDataSlot(new DataSlot() {
			@Override public int get() {
				return lamp.getLevel() != null && !lamp.getLevel().isClientSide()
						? lamp.getTemperature() : clientTemperature;
			}
			@Override public void set(int value) { clientTemperature = value; }
		});
	}

	public TileEntityHeatLamp lamp() { return lamp; }
	public int temperature() { return clientTemperature; }

	@Override
	public boolean stillValid(Player player) {
		return lamp.getLevel() != null && lamp.getLevel().getBlockEntity(lamp.getBlockPos()) == lamp
				&& player.distanceToSqr(lamp.getBlockPos().getX() + 0.5,
				lamp.getBlockPos().getY() + 0.5, lamp.getBlockPos().getZ() + 0.5) <= 64;
	}

	@Override
	public net.minecraft.world.item.ItemStack quickMoveStack(Player player, int slot) {
		return net.minecraft.world.item.ItemStack.EMPTY;
	}
}
