/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.tileentity.aoe.defence;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.auxiliary.interfaces.OwnedTile;
import reika.chromaticraft.base.tileentity.TileEntityAdjacencyUpgrade;
import reika.chromaticraft.base.tileentity.tileentityadjacencyupgrade.AdjacencyCheckHandlerImpl;
import reika.chromaticraft.base.tileentity.TileEntityRelayPowered;
import reika.chromaticraft.block.blockcrystalfence.CrystalFenceAuxTile;
import reika.chromaticraft.magic.ElementTagCompound;
import reika.chromaticraft.registry.AdjacencyUpgrades;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.registry.ChromaOptions;
import reika.chromaticraft.registry.ChromaPackets;
import reika.chromaticraft.registry.ChromaSounds;
import reika.chromaticraft.registry.ChromaTiles;
import reika.chromaticraft.registry.CrystalElement;
import reika.dragonapi.instantiable.StepTimer;
import reika.dragonapi.instantiable.data.Perimeter;
import reika.dragonapi.instantiable.data.immutable.Coordinate;
import reika.dragonapi.interfaces.tileentity.BreakAction;
import reika.dragonapi.libraries.ReikaPlayerAPI;
import reika.dragonapi.libraries.io.ReikaPacketHelper;
import reika.dragonapi.libraries.mathsci.ReikaMathLibrary;
import reika.dragonapi.libraries.mathsci.reikamusichelper.KeySignature;
import reika.dragonapi.libraries.mathsci.reikamusichelper.MusicKey;
import reika.dragonapi.libraries.mathsci.reikamusichelper.Note;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

//make very expensive, but aux cheaper
public class TileEntityCrystalFence extends TileEntityRelayPowered implements OwnedTile, BreakAction {

	private final Perimeter fence = new Perimeter().disallowVertical();

	private ForgeDirection outputFace = ForgeDirection.DOWN;

	private HashMap<Integer, Integer> active = new HashMap();

	private boolean controller = true;
	private int mainCount = 1;
	private int damageAmount;

	public static final int RANGE = 16;
	public static final int MAX_STEPS = ChromaOptions.getMaxFenceSections();

	private final StepTimer calcTimer = new StepTimer(100);

	private static final int FADE_STEP = 32;
	private static final int FADE_LENGTH = FADE_STEP*4;
	private static final int FADE_START = 512;

	private int chargingTick;

	private boolean[] colorFade;

	private static final ElementTagCompound required = new ElementTagCompound();

	private static final AdjacencyCheckHandlerImpl adjacency = TileEntityAdjacencyUpgrade.getOrCreateAdjacencyCheckHandler(CrystalElement.PINK, "Increase damage", ChromaTiles.FENCE);

	static {
		required.addTag(CrystalElement.RED, 100);
	}

	@Override
	public ChromaTiles getTile() {
		return ChromaTiles.FENCE;
	}

	@Override
	public void updateEntity(World world, int x, int y, int z, int meta) {
		super.updateEntity(world, x, y, z, meta);

		//calcTimer.update();
		//if (calcTimer.checkCap()) {
		//	this.calcFence();
		//}

		if (!world.isRemote && !fence.isEmpty()) {
			if (chargingTick > 0) {
				chargingTick--;
				int t = 5;
				if (this.getTicksExisted()%t == 0) {
					int i = (this.getTicksExisted()/t)%fence.segmentCount();
					this.triggerSegment(i, false);
					Note n = KeySignature.C.getScale().get(i%7);
					int offset = (i/7)*12;
					MusicKey key = MusicKey.getByIndex(MusicKey.C5.ordinal()+n.ordinal()+offset%12);
					if (key == null)
						key = MusicKey.A1;
					ChromaSounds.DING.playSoundAtBlock(this, 1, (float)key.getRatio(MusicKey.C5));
				}
			}
			else {
				if (controller && this.getTicksExisted()%10 == 0)
					this.validateFence(world, x, y, z);
				if (controller && fence.isClosed() && energy.containsAtLeast(required)) {
					this.affectEntities();
				}
			}
		}

		for (Integer key : active.keySet()) {
			if (key != null) {
				int val = active.get(key);
				if (val > 0)
					val -= FADE_STEP;
				active.put(key, val);
			}
		}
	}

	private void validateFence(World world, int x, int y, int z) {
		for (Coordinate c : fence.getPoints()) {
			if (c.equals(x, y, z))
				continue;
			Block b = c.getBlock(world);
			int meta = c.getBlockMetadata(world);
			if (b == this.getTile().getBlock() && meta == this.getTile().getBlockMetadata())
				continue;
			if (b == ChromaBlocks.FENCE.getBlockInstance())
				continue;
			this.onFenceBreak(world, x, y, z, true);
			return;
		}
	}

	public void onFenceBreak(World world, int x, int y, int z) {
		this.onFenceBreak(world, x, y, z, false);
	}

	private void onFenceBreak(World world, int x, int y, int z, boolean notify) {
		if (!world.isRemote && notify) {
			ChromaSounds.POWERDOWN.playSoundAtBlock(this);
			for (Coordinate c : fence.getPoints()) {
				if (c.equals(x, y, z))
					continue;
				TileEntity te = c.getTileEntity(world);
				if (te instanceof TileEntityCrystalFence) {
					((TileEntityCrystalFence)te).onFenceBreak(world, x, y, z, false);
				}
			}
			ReikaPacketHelper.sendDataPacketWithRadius(ChromatiCraft.packetChannel, ChromaPackets.FENCEBREAK.ordinal(), world, x, y, z, 128);
		}
		fence.clear();
	}

	@Override
	protected void onFirstTick(World world, int x, int y, int z) {
		this.calcFence(false);
	}

	public void calcFence(boolean renderOnly) {
		fence.clear();
		fence.addPoint(xCoord, yCoord, zCoord);
		this.findFrom(xCoord, zCoord, outputFace, 0);
		int adj = adjacency.getAdjacentUpgrade(this);
		double f = adj == 0 ? 1 : AdjacencyUpgrades.PINK.getFactor(adj-1);
		damageAmount *= f;
		if (!renderOnly) {
			colorFade = new boolean[fence.segmentCount()];
			chargingTick = 600;
		}
	}

	public Perimeter getFence() {
		return fence;
	}

	public boolean isValid() {
		return controller && fence.segmentCount() >= 1 && fence.isClosed();
	}

	private void findFrom(int x, int z, ForgeDirection dir, int step) {
		damageAmount = 2;
		mainCount = 0;
		for (int i = 1; i < RANGE; i++) {
			int dx = x+dir.offsetX*i;
			int dz = z+dir.offsetZ*i;
			TileEntity te = worldObj.getTileEntity(dx, yCoord, dz);
			if (this.isValid()) {
				return;
			}
			else if (step > MAX_STEPS) {
				fence.clear();
				return;
			}
			else if (te == this) {
				fence.addPoint(dx, yCoord, dz);
				controller = true;
				return;
			}
			else if (te instanceof TileEntityCrystalFence) {
				TileEntityCrystalFence fen = (TileEntityCrystalFence)te;
				fen.controller = false;
				mainCount++;
				damageAmount += 2;
				fence.addPoint(dx, yCoord, dz);
				this.findFrom(dx, dz, fen.outputFace, step+1);
				return;
			}
			else if (te instanceof CrystalFenceAuxTile) {
				CrystalFenceAuxTile fen = (CrystalFenceAuxTile)te;
				if (fen.getInput() == dir.getOpposite()) {
					fence.addPoint(dx, yCoord, dz);
					this.findFrom(dx, dz, fen.getOutput(), step+1);
				}
				else {
					fence.clear();
				}
				return;
			}
		}
	}

	private void affectEntities() {
		ArrayList<AxisAlignedBB> li = fence.getAABBs();
		for (int i = 0; i < li.size(); i++) {
			AxisAlignedBB aabb = li.get(i);
			aabb.minY = yCoord-this.getFenceDepth();
			aabb.maxY = yCoord+1+this.getFenceHeight();
			List<EntityLivingBase> ents = worldObj.getEntitiesWithinAABB(EntityLivingBase.class, aabb);
			for (EntityLivingBase e : ents) {
				boolean att = true;
				if (e instanceof EntityPlayer) {
					EntityPlayer ep = (EntityPlayer)e;
					if (ep == this.getPlacer())
						att = false;
					else if (ReikaPlayerAPI.isReika(ep))
						att = false;
				}
				if (att) {
					e.attackEntityFrom(DamageSource.cactus, damageAmount);
					//e.knockBack(null, 0, 0, 0);
					double dx = e.posX-xCoord-0.5;
					double dz = e.posZ-zCoord-0.5;
					double dd = ReikaMathLibrary.py3d(dx, 0, dz);
					e.motionX = dx/dd;
					e.motionY = 0.5;
					e.motionZ = dz/dd;
					e.velocityChanged = true;
					ChromaSounds.DISCHARGE.playSound(e, 1, 1.4F);

					this.triggerSegment(i, true);

					this.drainEnergy(required);
				}
			}
		}
	}

	public void triggerSegment(int i, boolean fadeColor) {
		if (worldObj.isRemote) {
			active.put(i, FADE_START+FADE_LENGTH);
			if (colorFade.length > i)
				colorFade[i] = fadeColor;
		}
		else {
			ReikaPacketHelper.sendDataPacketWithRadius(ChromatiCraft.packetChannel, ChromaPackets.FENCETRIGGER.ordinal(), this, 64, i, fadeColor ? 1 : 0);
		}
	}

	public int getSegmentAlpha(int i) {
		Integer get = active.get(i);
		if (get == null)
			return 0;
		if (get.intValue() > FADE_START) {
			return FADE_START-((get.intValue()-FADE_START)*(FADE_START/FADE_LENGTH));
		}
		else {
			return get.intValue();
		}
	}

	public boolean colorFade(int i) {
		return false;
	}

	public int getFenceHeight() {
		return 2;
	}

	public int getFenceDepth() {
		return 0;
	}

	@Override
	protected void animateWithTick(World world, int x, int y, int z) {

	}

	@Override
	protected void writeSyncTag(NBTTagCompound NBT)
	{
		super.writeSyncTag(NBT);

		NBT.setBoolean("enable", controller);
		NBT.setInteger("main", mainCount);
		NBT.setInteger("dmg", damageAmount);

		NBT.setInteger("face", outputFace.ordinal());

		NBT.setInteger("charge", chargingTick);
	}

	@Override
	protected void readSyncTag(NBTTagCompound NBT)
	{
		super.readSyncTag(NBT);

		controller = NBT.getBoolean("enable");
		mainCount = NBT.getInteger("main");
		damageAmount = NBT.getInteger("dmg");

		outputFace = dirs[NBT.getInteger("face")];

		chargingTick = NBT.getInteger("charge");
	}

	@Override
	public boolean isAcceptingColor(CrystalElement e) {
		return controller && required.contains(e);
	}

	@Override
	public int getMaxStorage(CrystalElement e) {
		return 20000;
	}

	@Override
	public ElementTagCompound getRequiredEnergy() {
		return required.copy();
	}

	@Override
	protected boolean canReceiveFrom(CrystalElement e, ForgeDirection dir) {
		return true;
	}

	public void setFacing(ForgeDirection dir) {
		outputFace = dir;
		this.syncAllData(false);
	}

	@Override
	public AxisAlignedBB getRenderBoundingBox() {
		return INFINITE_EXTENT_AABB;
	}

	@Override
	public boolean renderModelsInPass1() {
		return true;
	}

	@Override
	public void breakBlock() {
		this.onFenceBreak(worldObj, xCoord, yCoord, zCoord);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public double getMaxRenderDistanceSquared() {
		return super.getMaxRenderDistanceSquared()*16;
	}

}
