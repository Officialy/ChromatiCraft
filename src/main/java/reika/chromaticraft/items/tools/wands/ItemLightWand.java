/*******************************************************************************
 * @author Reika Kalseki
 * 
 * Copyright 2017
 * 
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.items.tools.wands;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import reika.chromaticraft.base.ItemWandBase;
import reika.chromaticraft.entity.EntityFlyingLight;
import reika.chromaticraft.registry.CrystalElement;
import reika.dragonapi.libraries.io.ReikaSoundHelper;

public class ItemLightWand extends ItemWandBase {

	public ItemLightWand(int index) {
		super(index);
		this.addEnergyCost(CrystalElement.BLUE, 1);
	}

	@Override
	public ItemStack onItemRightClick(ItemStack is, World world, EntityPlayer ep) {

		if (!world.isRemote) {
			EntityFlyingLight e = new EntityFlyingLight(world, ep);
			Vec3 vec = ep.getLookVec();
			e.setLocationAndAngles(ep.posX+vec.xCoord, ep.posY+vec.yCoord+1.5, ep.posZ+vec.zCoord, 0, 0);
			world.spawnEntityInWorld(e);

			ReikaSoundHelper.playSoundAtEntity(world, e, "random.fizz", 2, 0.7F);
		}

		return is;
	}

}
