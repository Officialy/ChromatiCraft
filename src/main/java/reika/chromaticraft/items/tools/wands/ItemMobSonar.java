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
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.auxiliary.render.MobSonarRenderer;
import reika.chromaticraft.base.ItemWandBase;
import reika.chromaticraft.registry.ChromaPackets;
import reika.chromaticraft.registry.ChromaSounds;
import reika.chromaticraft.registry.CrystalElement;
import reika.dragonapi.ModList;
import reika.dragonapi.asm.dependentmethodstripper.ModDependent;
import reika.dragonapi.libraries.io.ReikaPacketHelper;
import reika.dragonapi.libraries.io.ReikaSoundHelper;
import reika.satisforestry.api.PointSpawnLocation;
import reika.satisforestry.api.SFAPI;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class ItemMobSonar extends ItemWandBase {

	private static final int RANGE = 64;

	public ItemMobSonar(int index) {
		super(index);

		this.addEnergyCost(CrystalElement.PINK, 4);
		this.addEnergyCost(CrystalElement.BLUE, 4);
	}

	@Override
	public ItemStack onItemRightClick(ItemStack is, World world, EntityPlayer ep) {
		this.prepareNBT(is);
		if (world.isRemote) {
			this.doPing(world, ep);
		}
		else {
			this.drainPlayer(ep);
			if (ModList.SATISFORESTRY.isLoaded()) {
				this.findLizardDoggo(world, ep);
			}
		}
		return is;
	}

	private void prepareNBT(ItemStack is) {
		if (is.stackTagCompound == null) {
			is.stackTagCompound = new NBTTagCompound();
		}
		if (!is.stackTagCompound.hasKey("typeFlags")) {
			is.stackTagCompound.setInteger("typeFlags", MobSonarRenderer.EntitySonarType.getAllFlags());
		}
	}

	@ModDependent(ModList.SATISFORESTRY)
	private void findLizardDoggo(World world, EntityPlayer ep) {
		PointSpawnLocation s = SFAPI.spawningHandler.getNearestSpawnPointOfType(ep, 512, SFAPI.genericLookups.getDoggoClass());
		if (s != null) {
			ReikaPacketHelper.sendDataPacket(ChromatiCraft.packetChannel, ChromaPackets.DOGGOSONAR.ordinal(), (EntityPlayerMP)ep, s.getX(), s.getY(), s.getZ());
		}
	}

	@SideOnly(Side.CLIENT)
	private void doPing(World world, EntityPlayer ep) {
		MobSonarRenderer.instance.addPing(ep, RANGE);
		ReikaSoundHelper.playClientSound(ChromaSounds.ORB, ep, 1, 0.5F);
	}

}
