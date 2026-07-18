/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.tileentity.auxiliary;

import java.util.Collection;

import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.base.tileentity.TileEntityPylonEnhancer;
import reika.chromaticraft.magic.network.CrystalNetworker;
import reika.chromaticraft.magic.progression.ProgressStage;
import reika.chromaticraft.magic.progression.ProgressionCatchupHandling;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.registry.ChromaIcons;
import reika.chromaticraft.registry.ChromaPackets;
import reika.chromaticraft.registry.ChromaSounds;
import reika.chromaticraft.registry.ChromaTiles;
import reika.chromaticraft.registry.CrystalElement;
import reika.chromaticraft.render.particle.EntityCCFloatingSeedsFX;
import reika.chromaticraft.render.particle.EntityChromaFluidFX;
import reika.chromaticraft.tileentity.networking.TileEntityCrystalPylon;
import reika.chromaticraft.world.iwg.PylonGenerator;
import reika.dragonapi.instantiable.data.immutable.Coordinate;
import reika.dragonapi.instantiable.effects.EntityFloatingSeedsFX;
import reika.dragonapi.libraries.io.ReikaPacketHelper;
import reika.dragonapi.libraries.java.ReikaRandomHelper;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class TileEntityChromaCrystal extends TileEntityPylonEnhancer {

	private int omega;
	private int torque;
	private long power;

	private Coordinate pylonLocation;

	@Override
	public ChromaTiles getTile() {
		return ChromaTiles.CRYSTAL;
	}

	public boolean isConnected() {
		return pylonLocation != null;
	}

	@Override
	public void updateEntity(World world, int x, int y, int z, int meta) {
		if (world.isRemote && pylonLocation != null)
			ProgressionCatchupHandling.instance.attemptSync(this, 9, ProgressStage.POWERCRYSTAL);

		if (this.getTicksExisted() < 5)
			this.update();
	}

	@Override
	protected void onFirstTick(World world, int x, int y, int z) {
		if (!world.isRemote)
			pylonLocation = this.findPylonLocation(world, x, y, z);
		this.update();
	}

	private void update() {
		this.syncAllData(true);
		this.triggerBlockUpdate();
	}

	private Coordinate findPylonLocation(World world, int x, int y, int z) {
		if (world.getBlock(x, y-1, z) != ChromaBlocks.RUNE.getBlockInstance())
			return null;
		CrystalElement e = CrystalElement.elements[world.getBlockMetadata(x, y-1, z)];
		Collection<TileEntityCrystalPylon> c = CrystalNetworker.instance.getNearbyPylons(world, x, y, z, e, 8, false);
		for (TileEntityCrystalPylon te : c) {
			if (te.isValidPowerCrystal(this)) {
				PylonGenerator.instance.cachePylon(te);
				return new Coordinate(te);
			}
		}
		return null;
	}

	@Override
	protected void animateWithTick(World world, int x, int y, int z) {

	}

	public void destroy() {
		ChromaSounds.POWERDOWN.playSoundAtBlock(this, 2, 1);
		ReikaPacketHelper.sendDataPacketWithRadius(ChromatiCraft.packetChannel, ChromaPackets.POWERCRYSDESTROY.ordinal(), this, 32);
		this.delete();
	}

	@SideOnly(Side.CLIENT)
	public static void doDestroyParticles(World world, int x, int y, int z) {
		for (int i = 0; i < 24; i++) {
			double vx = ReikaRandomHelper.getRandomPlusMinus(0, 0.125);
			double vz = ReikaRandomHelper.getRandomPlusMinus(0, 0.125);
			double vy = ReikaRandomHelper.getRandomPlusMinus(0.125, 0.0625);
			double px = x+rand.nextDouble();
			double py = y+rand.nextDouble();
			double pz = z+rand.nextDouble();
			EntityFX fx = new EntityChromaFluidFX(world, px, py, pz, vx, vy, vz).setLife(80).setGravity(0.25F).setScale(1+rand.nextFloat());
			fx.noClip = false;
			Minecraft.getMinecraft().effectRenderer.addEffect(fx);
		}
		for (int i = 0; i < 16; i++) {
			double px = x+rand.nextDouble();
			double py = y+rand.nextDouble();
			double pz = z+rand.nextDouble();
			EntityFloatingSeedsFX fx = (EntityFloatingSeedsFX)new EntityCCFloatingSeedsFX(world, px, py, pz, 0, 90, ChromaIcons.CHROMA).setLife(80);
			fx.particleVelocity = 0.125;
			fx.freedom *= 2;
			Minecraft.getMinecraft().effectRenderer.addEffect(fx);
		}
	}

	@Override
	public void writeToNBT(NBTTagCompound NBT) {
		super.writeToNBT(NBT);

		if (pylonLocation != null)
			pylonLocation.writeToNBT("pylon", NBT);
	}

	@Override
	public void readFromNBT(NBTTagCompound NBT) {
		super.readFromNBT(NBT);

		if (NBT.hasKey("pylon"))
			pylonLocation = Coordinate.readFromNBT("pylon", NBT);
	}

	@Override
	public void breakBlock() {
		if (pylonLocation != null) {
			TileEntityCrystalPylon te = (TileEntityCrystalPylon)pylonLocation.getTileEntity(worldObj);
			te.onPowerCrystalBreak(this);
		}
	}
}
