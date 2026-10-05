package reika.chromaticraft.items;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** V33a {@code OwnableItem}: an item bound to one player, which item collectors must respect. */
public interface OwnableItem {

	boolean isUsableBy(ItemStack is, Player ep);

	boolean isCollectableBy(ItemEntity ei, Player ep);

}
