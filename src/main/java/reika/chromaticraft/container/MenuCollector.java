package reika.chromaticraft.container;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import reika.chromaticraft.registry.ChromaMenus;
import reika.chromaticraft.tileentity.acquisition.TileEntityCollector;
import reika.dragonapi.base.CoreContainer;

/** Exact V33a two-slot Collector layout plus both tank levels. */
public final class MenuCollector extends CoreContainer<TileEntityCollector> {
	private int clientInput;
	private int clientOutput;
	private int clientInputFluid = -1;

	public MenuCollector(int id, Inventory inventory, FriendlyByteBuf data) {
		this(id, inventory, (TileEntityCollector)inventory.player.level().getBlockEntity(data.readBlockPos()));
	}
	public MenuCollector(int id, Inventory inventory, TileEntityCollector collector) {
		super(ChromaMenus.COLLECTOR.get(), id, inventory, requireCollector(collector));
		this.addSlot(new Slot(collector, 0, 17, 16) {
			@Override public boolean mayPlace(ItemStack stack) { return stack.is(Items.EXPERIENCE_BOTTLE); }
		});
		this.addSlot(new Slot(collector, 1, 17, 54) {
			@Override public boolean mayPlace(ItemStack stack) { return false; }
		});
		this.addPlayerInventory(inventory);
		this.addDataSlot(new DataSlot() {
			@Override public int get() { return serverSide() ? collector.getInputLevel() : clientInput; }
			@Override public void set(int value) { clientInput = value; }
		});
		this.addDataSlot(new DataSlot() {
			@Override public int get() { return serverSide() ? collector.getOutputLevel() : clientOutput; }
			@Override public void set(int value) { clientOutput = value; }
		});
		this.addDataSlot(new DataSlot() {
			@Override public int get() {
				return serverSide() && !collector.getInputFluid().isEmpty()
						? BuiltInRegistries.FLUID.getId(collector.getInputFluid().getFluid()) : clientInputFluid;
			}
			@Override public void set(int value) { clientInputFluid = value; }
		});
	}
	private boolean serverSide() { return tile.getLevel() != null && !tile.getLevel().isClientSide(); }
	private static TileEntityCollector requireCollector(TileEntityCollector collector) {
		if (collector == null) throw new IllegalStateException("Collector menu opened without its block entity");
		return collector;
	}
	public int inputLevel() { return clientInput; }
	public int outputLevel() { return clientOutput; }
	public Fluid inputFluid() {
		return clientInputFluid >= 0 ? BuiltInRegistries.FLUID.byId(clientInputFluid) : Fluids.EMPTY;
	}

	@Override public ItemStack quickMoveStack(Player player, int slotIndex) {
		if (slotIndex < 0 || slotIndex >= slots.size()) return ItemStack.EMPTY;
		Slot slot = slots.get(slotIndex);
		if (!slot.hasItem()) return ItemStack.EMPTY;
		ItemStack source = slot.getItem();
		ItemStack result = source.copy();
		if (slotIndex < 2) {
			if (!moveItemStackTo(source, 2, slots.size(), true)) return ItemStack.EMPTY;
		}
		else if (!source.is(Items.EXPERIENCE_BOTTLE) || !moveItemStackTo(source, 0, 1, false)) return ItemStack.EMPTY;
		if (source.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
		return result;
	}
}
