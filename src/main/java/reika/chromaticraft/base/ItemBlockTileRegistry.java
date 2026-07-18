package reika.chromaticraft.base;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;

import reika.chromaticraft.items.itemblock.ItemBlockMultiType;
import reika.chromaticraft.registry.ChromaTiles;


public class ItemBlockTileRegistry extends ItemBlockMultiType {

	public ItemBlockTileRegistry(Block b) {
		super(b);
	}

	@Override
	public final String getItemStackDisplayName(ItemStack is) {
		ChromaTiles c = ChromaTiles.getTileByCraftedItem(is);
		if (c == null)
			c = ChromaTiles.getTileFromIDandMetadata(field_150939_a, is.getItemDamage());
		return c != null ? c.getName() : super.getItemStackDisplayName(is);
	}

}
