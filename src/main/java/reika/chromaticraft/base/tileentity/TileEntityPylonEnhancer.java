package reika.chromaticraft.base.tileentity;

import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import reika.chromaticraft.auxiliary.interfaces.OwnedTile;
import reika.dragonapi.interfaces.tileentity.BreakAction;

public abstract class TileEntityPylonEnhancer extends TileEntityChromaticBase implements OwnedTile, BreakAction {


	@Override
	public void getTagsToWriteToStack(NBTTagCompound NBT) {
		this.writeOwnerData(NBT);
	}

	@Override
	public void setDataFromItemStackTag(ItemStack is) {
		this.readOwnerData(is);
	}

	@Override
	public void addTooltipInfo(List li, boolean shift) {

	}

}
