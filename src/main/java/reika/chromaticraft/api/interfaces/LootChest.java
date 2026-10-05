package reika.chromaticraft.api.interfaces;

import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;

public interface LootChest extends WorldlyContainer {


	boolean isOwnedBy(Player ep);

	boolean isAccessibleBy(Player ep);

	boolean isUntouchedWorldgen();

}
