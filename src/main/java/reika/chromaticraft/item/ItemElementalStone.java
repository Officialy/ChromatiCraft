package reika.chromaticraft.item;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import reika.chromaticraft.block.BlockChromaFluid.TileEntityChroma;
import reika.chromaticraft.magic.progression.ProgressStage;
import reika.chromaticraft.registry.CrystalElement;

/** Metadata-free V33a Elemental Stone and its one-per-pool matching-color deposit behavior. */
public final class ItemElementalStone extends Item {
	private final CrystalElement element;

	public ItemElementalStone(CrystalElement element, Properties properties) {
		super(properties);
		this.element = element;
	}

	public CrystalElement element() { return element; }

	@Override
	public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
		if (entity.level().isClientSide() || !(entity.getOwner() instanceof Player player)) return false;
		if (!ProgressStage.SHARDCHARGE.playerHasPrerequisites(player)) return false;
		if (entity.level().getBlockEntity(entity.blockPosition()) instanceof TileEntityChroma pool
				&& pool.addElementalStone(element)) {
			stack.shrink(1);
			if (stack.isEmpty()) entity.discard();
			else entity.setItem(stack);
		}
		return false;
	}
}
