package reika.chromaticraft.base;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import reika.chromaticraft.registry.CrystalElement;
import reika.dragonapi.auxiliary.ProgressiveRecursiveBreaker.BreakerCallback;
import reika.dragonapi.instantiable.data.blockstruct.BlockArray;

public abstract class ItemBlockChangingWand extends ItemWandBase implements BreakerCallback {

	public ItemBlockChangingWand(Properties properties) {
		super(properties);
		this.addEnergyCost(CrystalElement.BROWN, 1);
	}

    public abstract int getDepth(Player player, Level level, BlockPos pos);

	public abstract void getSpreadBlocks(Level level, BlockPos pos, BlockArray blocks, Player player, ItemStack stack);

	public abstract boolean canSpreadOn(Level level, BlockPos pos, BlockState state);

}
