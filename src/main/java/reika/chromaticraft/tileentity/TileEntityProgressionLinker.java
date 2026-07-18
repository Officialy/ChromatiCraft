/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2018
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.tileentity;

import java.util.List;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.auxiliary.interfaces.MultiBlockChromaTile;
import reika.chromaticraft.auxiliary.interfaces.OwnedTile;
import reika.chromaticraft.base.tileentity.TileEntityChromaticBase;
import reika.chromaticraft.magic.progression.ProgressionLinking;
import reika.chromaticraft.magic.progression.progressionlinking.LinkFailure;
import reika.chromaticraft.registry.ChromaPackets;
import reika.chromaticraft.registry.ChromaSounds;
import reika.chromaticraft.registry.ChromaStructures;
import reika.chromaticraft.registry.ChromaTiles;
import reika.dragonapi.DragonAPICore;
import reika.dragonapi.auxiliary.trackers.TickScheduler;
import reika.dragonapi.instantiable.data.immutable.Coordinate;
import reika.dragonapi.instantiable.event.ScheduledTickEvent;
import reika.dragonapi.instantiable.event.scheduledtickevent.ScheduledSoundEvent;
import reika.dragonapi.instantiable.io.PacketTarget;
import reika.dragonapi.libraries.ReikaAABBHelper;
import reika.dragonapi.libraries.io.ReikaPacketHelper;
import reika.dragonapi.libraries.java.ReikaObfuscationHelper;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;


public class TileEntityProgressionLinker extends TileEntityChromaticBase implements OwnedTile, MultiBlockChromaTile {

	private boolean hasStructure;

	private int linkProgress;
	private UUID targetPlayer;

	private int failTicks = 0;
	public LinkFailure failure;

	public static final int DURATION = 600;
	private static final int FAIL_FADE = DURATION/2;

	@Override
	protected void onFirstTick(World world, int x, int y, int z) {
		super.onFirstTick(world, x, y, z);

		this.validateStructure();
	}

	public void validateStructure() {
		ChromaStructures.PROGRESSLINK.getStructure().resetToDefaults();
		hasStructure = ChromaStructures.PROGRESSLINK.getArray(worldObj, xCoord, yCoord, zCoord).matchInWorld();
	}

	@Override
	public ChromaTiles getTile() {
		return ChromaTiles.PROGRESSLINK;
	}

	@Override
	public void updateEntity(World world, int x, int y, int z, int meta) {
		if (failTicks > 0)
			failTicks--;
		if (failTicks <= 0)
			failure = null;
		if (failTicks > 0)
			return;
		if (this.hasStructure() && this.hasPlayer()) {
			EntityPlayer ep = world.func_152378_a(targetPlayer);
			if (ep == null || !ep.boundingBox.intersectsWith(ReikaAABBHelper.getBlockAABB(this).addCoord(0, 4, 0))) {
				linkProgress = 0;
				targetPlayer = null;
			}
			else {
				if (linkProgress%20 == 0)
					ChromaSounds.LOREHEX.playSoundAtBlock(this, 1, 0.5F);
				linkProgress--;
				//ReikaJavaLibrary.pConsole(linkProgress, Side.SERVER);
				if (linkProgress == 0 && !world.isRemote) {
					this.linkPlayers(this.getPlacer(), ep);
					targetPlayer = null;
				}
				else {
					double dy = 0.1875*Math.sin(System.currentTimeMillis()/400D);
					ep.posX = x+0.5;
					ep.posZ = z+0.5;
					ep.posY = y+0.625+1.62+dy;
					ep.onGround = false;
					ep.motionX = ep.motionY = ep.motionZ = 0;
					ep.fallDistance = 0;
					ep.isAirBorne = true;
					ep.limbSwing = 0;
					ep.limbSwingAmount = 0;
				}
			}
		}
	}

	private void linkPlayers(EntityPlayer ep, EntityPlayer target) {
		LinkFailure lf = ProgressionLinking.instance.linkProgression(ep, target);
		if (lf == null) {
			for (int i = 0; i <= 100; i += 5) {
				//ChromaSounds.LOREHEX.playSoundAtBlock(this, 1, 0.5F);
				ScheduledSoundEvent evt = new ScheduledSoundEvent(ChromaSounds.LOREHEX, ep, 1, 2F);
				TickScheduler.instance.scheduleEvent(new ScheduledTickEvent(evt), 1+i);
			}
		}
		else {
			ChromaSounds.ERROR.playSoundAtBlock(this);
			failTicks = FAIL_FADE;
			NBTTagCompound tag = lf.writeToNBT();
			new Coordinate(this).writeToNBT("loc", tag);
			ReikaPacketHelper.sendNBTPacket(ChromatiCraft.packetChannel, ChromaPackets.LINKFAIL.ordinal(), tag, new PacketTarget.RadiusTarget(this, 64));
		}
	}

	public boolean trigger(EntityPlayer ep) {
		if (failTicks > 0)
			return false;
		if (this.hasStructure() && this.isOwnedByPlayer(ep) && !this.hasPlayer()) {
			targetPlayer = this.findPlayer();
			if (targetPlayer != null) {
				linkProgress = DURATION;
				ChromaSounds.USE.playSoundAtBlock(this);
				return true;
			}
		}
		ChromaSounds.ERROR.playSoundAtBlock(this);
		return false;
	}

	private UUID findPlayer() {
		AxisAlignedBB box = ReikaAABBHelper.getBlockAABB(this).offset(0, 0.5, 0);
		List<EntityPlayer> li = worldObj.getEntitiesWithinAABB(EntityPlayer.class, box);
		for (EntityPlayer ep : li) {
			if (!this.isOwnedByPlayer(ep) || (DragonAPICore.isReikasComputer() && ReikaObfuscationHelper.isDeObfEnvironment())) {
				return ep.getUniqueID();
			}
		}
		return null;
	}

	@Override
	protected void animateWithTick(World world, int x, int y, int z) {

	}

	public boolean hasPlayer() {
		return linkProgress > 0 && targetPlayer != null;
	}

	@Override
	public void readSyncTag(NBTTagCompound NBT) {
		super.readSyncTag(NBT);

		hasStructure = NBT.getBoolean("struct");
		failTicks = NBT.getInteger("fail");
	}

	@Override
	public void writeSyncTag(NBTTagCompound NBT) {
		super.writeSyncTag(NBT);

		NBT.setBoolean("struct", hasStructure);
		NBT.setInteger("fail", failTicks);
	}

	public boolean hasStructure() {
		return hasStructure;
	}

	public float getFailFade() {
		return failTicks/(float)FAIL_FADE;
	}

	@SideOnly(Side.CLIENT)
	public void showFailure(LinkFailure f) {
		failTicks = FAIL_FADE;
		failure = f;
	}

	@Override
	public ChromaStructures getPrimaryStructure() {
		return ChromaStructures.PROGRESSLINK;
	}

	@Override
	public Coordinate getStructureOffset() {
		return null;
	}

	public boolean canStructureBeInspected() {
		return true;
	}

	@Override
	public void getTagsToWriteToStack(NBTTagCompound NBT) {
		this.writeOwnerData(NBT);
	}

	@Override
	public void setDataFromItemStackTag(ItemStack is) {
		this.readOwnerData(is);
	}

	@Override
	public void addTooltipInfo(List li, boolean shift) {

	}

}
