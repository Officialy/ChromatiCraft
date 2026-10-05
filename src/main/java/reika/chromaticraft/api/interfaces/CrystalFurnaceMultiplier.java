package reika.chromaticraft.api.interfaces;

import net.minecraft.world.item.ItemStack;

public interface CrystalFurnaceMultiplier {

	int getMultiplyRateAsInput(ItemStack is, ItemStack to);
	int getMultiplyRateAsOutput(ItemStack is, ItemStack from);

}
