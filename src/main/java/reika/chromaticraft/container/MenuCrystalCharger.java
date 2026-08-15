package reika.chromaticraft.container;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import reika.chromaticraft.registry.ChromaItems;
import reika.chromaticraft.registry.ChromaMenus;
import reika.chromaticraft.registry.CrystalElement;
import reika.chromaticraft.tileentity.auxiliary.TileEntityCrystalCharger;
import reika.dragonapi.base.CoreContainer;

/** V33a Charger layout: one visible crystal slot, sixteen energy bars, and toggle state. */
public final class MenuCrystalCharger extends CoreContainer<TileEntityCrystalCharger> {

	private final int[] clientEnergy = new int[CrystalElement.elements.length];
	private int clientToggles;

	public MenuCrystalCharger(int id, Inventory inventory, FriendlyByteBuf data) {
		this(id, inventory, (TileEntityCrystalCharger)inventory.player.level()
				.getBlockEntity(data.readBlockPos()));
	}

	public MenuCrystalCharger(int id, Inventory inventory, TileEntityCrystalCharger charger) {
		super(ChromaMenus.CRYSTAL_CHARGER.get(), id, inventory, requireCharger(charger));
		this.addSlot(new Slot(charger, 0, 80, 47) {
			@Override public boolean mayPlace(ItemStack stack) {
				return ChromaItems.isStorageCrystal(stack);
			}
		});
		this.addPlayerInventoryWithOffset(inventory, 0, 25);
		for (CrystalElement element : CrystalElement.elements) {
			int index = element.ordinal();
			this.addDataSlot(new DataSlot() {
				@Override public int get() {
					return charger.getLevel() != null && !charger.getLevel().isClientSide()
							? charger.getEnergy(element) : clientEnergy[index];
				}
				@Override public void set(int value) { clientEnergy[index] = value; }
			});
		}
		this.addDataSlot(new DataSlot() {
			@Override public int get() {
				return charger.getLevel() != null && !charger.getLevel().isClientSide()
						? charger.toggleFlags() : clientToggles;
			}
			@Override public void set(int value) { clientToggles = value; }
		});
	}

	private static TileEntityCrystalCharger requireCharger(TileEntityCrystalCharger charger) {
		if (charger == null) throw new IllegalStateException("Crystal Charger menu opened without its block entity");
		return charger;
	}

	public TileEntityCrystalCharger charger() { return tile; }
	public int energy(CrystalElement element) { return clientEnergy[element.ordinal()]; }
	public boolean isToggled(CrystalElement element) {
		return (clientToggles & 1 << element.ordinal()) != 0;
	}

	@Override
	public ItemStack quickMoveStack(Player player, int slotIndex) {
		if (slotIndex < 0 || slotIndex >= slots.size()) return ItemStack.EMPTY;
		Slot slot = slots.get(slotIndex);
		if (!slot.hasItem()) return ItemStack.EMPTY;
		ItemStack source = slot.getItem();
		ItemStack result = source.copy();
		if (slotIndex == 0) {
			if (!moveItemStackTo(source, 1, slots.size(), true)) return ItemStack.EMPTY;
		}
		else {
			if (!ChromaItems.isStorageCrystal(source) || !moveItemStackTo(source, 0, 1, false))
				return ItemStack.EMPTY;
		}
		if (source.isEmpty()) slot.set(ItemStack.EMPTY);
		else slot.setChanged();
		return result;
	}
}
