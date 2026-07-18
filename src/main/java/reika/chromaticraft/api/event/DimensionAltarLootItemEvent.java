package reika.chromaticraft.api.event;

import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;

import reika.dragonapi.instantiable.event.TileEntityEvent;

import cpw.mods.fml.common.eventhandler.Cancelable;

@Cancelable
public class DimensionAltarLootItemEvent extends TileEntityEvent {

	private final ItemStack item;

	public DimensionAltarLootItemEvent(TileEntity te, ItemStack is) {
		super(te);
		item = is;
	}

	public ItemStack getItem() {
		return item.copy();
	}

}
