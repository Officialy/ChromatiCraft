package reika.chromaticraft.api.interfaces;

import net.minecraft.world.item.ItemStack;

public interface CrystalFurnaceMultiplier {

	public int getMultiplyRateAsInput(ItemStack is, ItemStack to);
	public int getMultiplyRateAsOutput(ItemStack is, ItemStack from);

}
