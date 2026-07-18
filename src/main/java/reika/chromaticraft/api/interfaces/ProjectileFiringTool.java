/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2018
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.api.interfaces;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Implement this on tools that fire projectiles, to allow CC systems to fire them */
public interface ProjectileFiringTool {

	public void fire(ItemStack is, Level world, Player ep, boolean randomVec);

	/** How many ticks per shot */
	public int getAutofireRate();

}
