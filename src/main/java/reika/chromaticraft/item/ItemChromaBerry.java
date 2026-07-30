package reika.chromaticraft.item;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import reika.chromaticraft.block.BlockChromaFluid.TileEntityChroma;
import reika.chromaticraft.registry.CrystalElement;

/** Metadata-free V33a Chroma Berry identity and dropped-pool activation behavior. */
public final class ItemChromaBerry extends Item {
	private final CrystalElement element;

	public ItemChromaBerry(CrystalElement element, Properties properties) {
		super(properties);
		this.element = element;
	}

	public CrystalElement element() { return element; }

	@Override
	public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
		if (entity.level().isClientSide()) return false;
		if (entity.level().getBlockEntity(entity.blockPosition()) instanceof TileEntityChroma pool) {
			int accepted = pool.activate(element, stack.getCount());
			if (accepted > 0) {
				stack.shrink(accepted);
				if (stack.isEmpty()) entity.discard();
				else entity.setItem(stack);
			}
		}
		return false;
	}
}
