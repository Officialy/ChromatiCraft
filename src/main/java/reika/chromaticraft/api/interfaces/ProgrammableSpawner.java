/*******************************************************************************
 * @author Reika Kalseki
 * 
 * Copyright 2017
 * 
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.api.interfaces;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BaseSpawner;

/** Implement this if you want the spawner controller to be able to reprogram your spawner item. */
public interface ProgrammableSpawner {

	/** The entity class that the spawner corresponds to. Args: Spawner itemstack */
	public Class<? extends Mob> getSpawnerEntity(ItemStack is);

	/** Actually sets the spawner's entity type. Args: Spawner itemstack, entity class */
	public void setSpawnerType(ItemStack is, Class<? extends Mob> cl);

	/** Actually sets the spawner's data. See {@link BaseSpawner}. */
	public void setSpawnerData(ItemStack is, int minDelay, int maxDelay, int maxNear, int spawnCount, int spawnRange, int activeRange);

}
