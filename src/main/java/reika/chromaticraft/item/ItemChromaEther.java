package reika.chromaticraft.item;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import reika.chromaticraft.block.BlockChromaFluid.TileEntityChroma;
import reika.chromaticraft.magic.progression.ProgressStage;

/** Dropped Ether Berries saturate a Liquid Chroma source for the original shard speed multiplier. */
public final class ItemChromaEther extends Item {
	public ItemChromaEther(Properties properties) { super(properties); }

	@Override
	public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
		if (entity.level().isClientSide() || !(entity.getOwner() instanceof Player player)) return false;
		if (!ProgressStage.SHARDCHARGE.playerHasPrerequisites(player)
				&& !ProgressStage.ALLOY.playerHasPrerequisites(player)) return false;
		if (entity.level().getBlockEntity(entity.blockPosition()) instanceof TileEntityChroma pool) {
			int accepted = pool.etherize(stack.getCount());
			if (accepted > 0) {
				stack.shrink(accepted);
				if (stack.isEmpty()) entity.discard();
				else entity.setItem(stack);
			}
		}
		return false;
	}
}
