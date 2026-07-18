package reika.chromaticraft.base;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import reika.chromaticraft.registry.CrystalElement;
import reika.dragonapi.auxiliary.progressiverecursivebreaker.BreakerCallback;
import reika.dragonapi.instantiable.data.blockstruct.BlockArray;

public abstract class ItemBlockChangingWand extends ItemWandBase implements BreakerCallback {

	public ItemBlockChangingWand(int index) {
		super(index);
		this.addEnergyCost(CrystalElement.BROWN, 1);
	}

	public abstract int getDepth(EntityPlayer ep, World world, int x, int y, int z);

	public abstract void getSpreadBlocks(World world, int x, int y, int z, BlockArray arr, EntityPlayer ep, ItemStack is);

	public abstract boolean canSpreadOn(World world, int x, int y, int z, Block b, int meta);

}
