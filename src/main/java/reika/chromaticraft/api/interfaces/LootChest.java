package reika.chromaticraft.api.interfaces;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.WorldlyContainer;

public interface LootChest extends WorldlyContainer {


	public boolean isOwnedBy(Player ep);

	public boolean isAccessibleBy(Player ep);

	public boolean isUntouchedWorldgen();

}
