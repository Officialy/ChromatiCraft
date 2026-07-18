package reika.chromaticraft.api.interfaces;

import net.minecraft.world.entity.player.Player;

/** Implement this on TileEntities that can react to having the manipulator used on them. */
public interface ManipulatorInteraction {

	/** Return true to "eat" the right click */
	public boolean onManipulatorInteract(Player ep, int side);

}
