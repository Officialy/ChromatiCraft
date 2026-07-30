/*******************************************************************************
 * @author Reika Kalseki
 * 
 * Copyright 2017
 * 
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.auxiliary.interfaces;

import net.minecraft.world.entity.player.Player;

public interface SneakPop {

	public void drop();

	public boolean canDrop(Player ep);
	
	public boolean allowMining(Player ep);

}
