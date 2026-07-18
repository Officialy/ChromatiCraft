/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.items.tools;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import reika.chromaticraft.base.itemprojectilefiringtool.ProgressGatedProjectileFiringTool;
import reika.chromaticraft.entity.EntityVacuum;
import reika.dragonapi.libraries.registry.ReikaItemHelper;

public class ItemVacuumGun extends ProgressGatedProjectileFiringTool {

	public ItemVacuumGun(int index) {
		super(index, UseResult.PUNISHSEVERE);
	}

	@Override
	protected void harmDisallowedPlayer(EntityPlayer ep, boolean severe) {
		super.harmDisallowedPlayer(ep, severe);

		if (severe) {
			ReikaItemHelper.dropInventory(ep);
		}
	}

	@Override
	protected Entity createProjectile(ItemStack is, World world, EntityPlayer ep, boolean randomVec) {
		return new EntityVacuum(world, ep, randomVec);
	}

	@Override
	public int getAutofireRate() {
		return 50;
	}

}
