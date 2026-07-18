/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.tileentity;

import java.util.ArrayList;
import java.util.Collection;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.auxiliary.interfaces.ComplexAOE;
import reika.chromaticraft.base.tileentity.TileEntityRelayPowered;
import reika.chromaticraft.magic.ElementTagCompound;
import reika.chromaticraft.registry.ChromaPackets;
import reika.chromaticraft.registry.ChromaTiles;
import reika.chromaticraft.registry.CrystalElement;
import reika.chromaticraft.render.particle.EntityCCBlurFX;
import reika.chromaticraft.tileentity.auxiliary.TileEntityFunctionRelay;
import reika.dragonapi.instantiable.data.WeightedRandom;
import reika.dragonapi.instantiable.data.immutable.Coordinate;
import reika.dragonapi.instantiable.effects.EntityBlurFX;
import reika.dragonapi.interfaces.registry.CropType;
import reika.dragonapi.interfaces.registry.croptype.CropMethods;
import reika.dragonapi.libraries.ReikaDirectionHelper;
import reika.dragonapi.libraries.io.ReikaPacketHelper;
import reika.dragonapi.libraries.io.ReikaSoundHelper;
import reika.dragonapi.libraries.registry.ReikaCropHelper;
import reika.dragonapi.libraries.registry.ReikaItemHelper;
import reika.dragonapi.libraries.world.ReikaWorldHelper;
import reika.dragonapi.modregistry.ModCropList;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class TileEntityFarmer extends TileEntityRelayPowered implements ComplexAOE {

	private static final WeightedRandom<Coordinate>[] coordinateRand = new WeightedRandom[4];

	static {
		for (int i = 0; i < 4; i++) {
			WeightedRandom<Coordinate> wr = new WeightedRandom();
			ForgeDirection dir = ForgeDirection.VALID_DIRECTIONS[i+2];
			ForgeDirection left = ReikaDirectionHelper.getLeftBy90(dir);
			for (int r = 0; r < 16; r++) {
				for (int a = -r; a <= r; a++) {
					int dx = r*dir.offsetX+a*left.offsetX;
					int dz = r*dir.offsetZ+a*left.offsetZ;
					double wt = 100;
					if (Math.abs(a) > 2 && Math.abs(a) >= r/2)
						wt -= (Math.abs(a)/3D)*10;
					if (r > 8)
						wt *= 1-((r-8)/8D);
					if (wt > 0)
						wr.addEntry(new Coordinate(dx, 0, dz), wt);
				}
			}
			coordinateRand[i] = wr;
		}
	}

	@Override
	public void updateEntity(World world, int x, int y, int z, int meta) {
		super.updateEntity(world, x, y, z, meta);

		if (!world.isRemote && !this.hasRedstoneSignal() && this.getEnergy(CrystalElement.GREEN) >= 200) {
			int n = this.getNumberAttempts();
			for (int i = 0; i < n; i++) {
				Coordinate c = this.getRandomPosition(world, x, y, z);
				if (c != null && this.operateAt(world, c, true)) {
					this.sendParticles(c);
					break;
				}
			}
		}
	}

	private boolean operateAt(World world, Coordinate c, boolean allowRelays) {
		if (c == null)
			return false;
		Object o = this.getCropOrRelayAt(world, c);
		if (allowRelays && o instanceof TileEntityFunctionRelay) {
			return this.operateAt(world, ((TileEntityFunctionRelay)o).getRandomCoordinate(), false);
		}
		else if (o instanceof CropType) {
			CropType crop = (CropType)o;
			if (crop.isRipe(world, c.xCoord, c.yCoord, c.zCoord)) {
				int fortune = this.getFortune();
				ArrayList<ItemStack> li = crop.getDrops(world, c.xCoord, c.yCoord, c.zCoord, fortune);
				if (fortune < 3) {
					CropMethods.removeOneSeed(crop, li);
				}
				ReikaItemHelper.dropItems(world, c.xCoord+0.5, c.yCoord+0.5, c.zCoord+0.5, li);
				crop.setHarvested(world, c.xCoord, c.yCoord, c.zCoord);
				ReikaSoundHelper.playBreakSound(world, c.xCoord, c.yCoord, c.zCoord, c.getBlock(world));
				this.drainEnergy(CrystalElement.GREEN, 200);
				this.drainEnergy(CrystalElement.PURPLE, 50);
				return true;
			}
		}
		else if (o instanceof Block) {
			Block b = (Block)o;
			int meta = c.getBlockMetadata(world);
			int fortune = this.getFortune();
			ArrayList<ItemStack> li = b.getDrops(world, c.xCoord, c.yCoord, c.zCoord, fortune, meta);
			ReikaItemHelper.dropItems(world, c.xCoord+0.5, c.yCoord+0.5, c.zCoord+0.5, li);
			c.setBlock(world, Blocks.air);
			ReikaSoundHelper.playBreakSound(world, c.xCoord, c.yCoord, c.zCoord, b);
			this.drainEnergy(CrystalElement.GREEN, 200);
			this.drainEnergy(CrystalElement.PURPLE, 50);
			return true;
		}
		return false;
	}

	private int getNumberAttempts() {
		return Math.max(1, this.getEnergy(CrystalElement.GREEN)/2500);
	}

	private void sendParticles(Coordinate c) {
		ReikaPacketHelper.sendDataPacketWithRadius(ChromatiCraft.packetChannel, ChromaPackets.FARMERHARVEST.ordinal(), this, 48, c.xCoord, c.yCoord, c.zCoord);
	}

	@SideOnly(Side.CLIENT)
	public void doParticles(int tx, int ty, int tz) {
		double v = 0.15;
		double vx = v*(tx-xCoord);
		double vy = v*(ty-yCoord);
		double vz = v*(tz-zCoord);
		EntityBlurFX fx = new EntityCCBlurFX(worldObj, xCoord+0.5, yCoord+0.5, zCoord+0.5, vx, vy, vz).setColor(0, 192, 0);
		fx.setScale(4).setLife(10).forceIgnoreLimits();
		Minecraft.getMinecraft().effectRenderer.addEffect(fx);
	}

	private int getFortune() {
		return this.getEnergy(CrystalElement.PURPLE)/1000;
	}

	private Object getCropOrRelayAt(World world, Coordinate c) {
		Block b = c.getBlock(world);
		if ((b == Blocks.cactus || b == Blocks.reeds) && c.offset(0, -1, 0).getBlock(world) == b)
			return b;
		int meta = c.getBlockMetadata(world);
		if (ChromaTiles.getTileFromIDandMetadata(b, meta) == ChromaTiles.FUNCTIONRELAY) {
			return c.getTileEntity(world);
		}
		CropType type = ReikaCropHelper.getCrop(b);
		if (type == null)
			type = ModCropList.getModCrop(b, meta);
		return type;
	}

	private Coordinate getRandomPosition(World world, int x, int y, int z) {/*
		ForgeDirection dir = this.getFacing();
		ForgeDirection left = ReikaDirectionHelper.getLeftBy90(dir);
		int r = rand.nextInt(16);
		int sp = ReikaRandomHelper.getRandomPlusMinus(0, r);//r/2
		int dx = x+r*dir.offsetX+sp*left.offsetX;//ReikaRandomHelper.getRandomPlusMinus(x, r);
		int dz = z+r*dir.offsetZ+sp*left.offsetZ;//ReikaRandomHelper.getRandomPlusMinus(z, r);
		int dy = ReikaWorldHelper.findTopBlockBelowY(world, dx, y, dz);//Math.min(y, world.getTopSolidOrLiquidBlock(x, z));
		return new Coordinate(dx, dy, dz);*/
		Coordinate pos = coordinateRand[this.getFacing().ordinal()-2].getRandomEntry().offset(x, y, z);
		int dy = ReikaWorldHelper.findTopBlockBelowY(world, pos.xCoord, y, pos.zCoord);//Math.min(y, world.getTopSolidOrLiquidBlock(x, z));
		Coordinate ret = pos.setY(dy);
		if (ReikaWorldHelper.isSubmerged(world, x, y, z) && ret.getTaxicabDistanceTo(x, y, z) >= 5)
			ret = null;
		return ret;
	}
	/*
	@Override
	public void onPathBroken(CrystalElement e) {

	}

	@Override
	public int getReceiveRange() {
		return 24;
	}

	@Override
	public boolean isConductingElement(CrystalElement e) {
		return e == CrystalElement.GREEN || e == CrystalElement.PURPLE;
	}

	@Override
	public int maxThroughput() {
		return 500;
	}

	@Override
	public boolean canConduct() {
		return true;
	}
	 */
	@Override
	public int getMaxStorage(CrystalElement e) {
		switch(e) {
			case GREEN:
				return 10000;
			case PURPLE:
				return 5000;
			default:
				return 0;
		}
	}

	@Override
	public ChromaTiles getTile() {
		return ChromaTiles.FARMER;
	}

	@Override
	protected void animateWithTick(World world, int x, int y, int z) {

	}
	/*
	@Override
	public boolean canExtractItem(int slot, ItemStack is, int side) {
		return true;
	}

	@Override
	public int getSizeInventory() {
		return 2;
	}

	@Override
	public int getInventoryStackLimit() {
		return 1;
	}

	@Override
	public boolean isItemValidForSlot(int slot, ItemStack is) {
		return true;
	}*/

	@Override
	protected boolean canReceiveFrom(CrystalElement e, ForgeDirection dir) {
		return this.isAcceptingColor(e);
	}

	@Override
	public ElementTagCompound getRequiredEnergy() {
		ElementTagCompound tag = new ElementTagCompound();
		tag.addTag(CrystalElement.GREEN, this.getMaxStorage(CrystalElement.GREEN)-energy.getValue(CrystalElement.GREEN));
		tag.addTag(CrystalElement.PURPLE, this.getMaxStorage(CrystalElement.PURPLE)-energy.getValue(CrystalElement.PURPLE));
		return tag;
	}

	@Override
	public boolean isAcceptingColor(CrystalElement e) {
		return e == CrystalElement.GREEN || e == CrystalElement.PURPLE;
	}

	public ForgeDirection getFacing() {
		switch(this.getBlockMetadata()) {
			case 0:
				return ForgeDirection.WEST;
			case 1:
				return ForgeDirection.EAST;
			case 2:
				return ForgeDirection.NORTH;
			case 3:
				return ForgeDirection.SOUTH;
			default:
				return ForgeDirection.UNKNOWN;
		}
	}

	@Override
	public Collection<Coordinate> getPossibleRelativePositions() {
		return coordinateRand[3].getValues();
	}

	@Override
	public double getNormalizedWeight(Coordinate c) {
		return coordinateRand[3].getNormalizedWeight(c);
	}

}
