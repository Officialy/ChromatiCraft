/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.tileentity.networking;

import java.util.EnumMap;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import reika.chromaticraft.block.BlockPylonStructure;
import reika.chromaticraft.block.BlockPylonStructure.StoneTypes;
import reika.chromaticraft.magic.interfaces.ConnectivityAction;
import reika.chromaticraft.magic.interfaces.CrystalReceiver;
import reika.chromaticraft.magic.interfaces.CrystalTransmitter;
import reika.chromaticraft.magic.network.CrystalPath;
import reika.chromaticraft.registry.ChromaBlockEntities;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.registry.ChromaStructures;
import reika.chromaticraft.registry.ChromaTiles;
import reika.chromaticraft.registry.CrystalElement;

/**
 * Compound repeater — a colour-cycling repeater (conducts all colours in turn) built on a taller
 * crystalline-stone column. Extends {@link TileEntityCrystalRepeater}.
 *
 * <p>Deferred: the client colour-cycle particle FX.
 */
public class TileEntityCompoundRepeater extends TileEntityCrystalRepeater implements ConnectivityAction {

	private final EnumMap<CrystalElement, Integer> depth = new EnumMap<>(CrystalElement.class);

	private boolean connectedToPylon = false;

	public TileEntityCompoundRepeater(BlockPos pos, BlockState state) {
		super(ChromaBlockEntities.COMPOUND.get(), pos, state);
	}

	@Override
	public void updateEntity(Level world, BlockPos pos) {
		super.updateEntity(world, pos);
		//Deferred: client colour-cycle particles.
	}

	public CrystalElement getRenderColorWithOffset(int i) {
		return CrystalElement.elements[((this.getColorCycleTick() + i) / 32) % 16];
	}

	public int getColorCycleTick() {
		BlockPos p = this.getBlockPos();
		return (int)((this.getLevel().getGameTime() + p.getX() / 8D % 16 + p.getZ() / 8D % 16) % 512D);
	}

	@Override
	public boolean isConductingElement(CrystalElement e) {
		return e != null && this.canConduct();
	}

	@Override
	public int maxThroughput() {
		return this.isTurbocharged() ? (this.isEnhancedStructure() ? 20000 : 12000) : 1000;
	}

	@Override
	public int getSignalDegradation(boolean point) {
		return this.isTurbocharged() ? (this.isEnhancedStructure() ? (point ? 0 : 10) : 20) : 100;
	}

	@Override
	protected boolean checkForStructure() {
		Direction f = this.getFacing();
		for (int i = 1; i <= 5; i++) {
			BlockState s = this.stateAt(f.getStepX() * i, f.getStepY() * i, f.getStepZ() * i);
			if (s.getBlock() != ChromaBlocks.PYLONSTRUCT.get())
				return false;
			int m2 = i == 3 ? 13 : (i == 1 || i == 5 ? 12 : this.getColumnBeam());
			int m2b = m2 == this.getColumnBeam() && this.isTurbocharged() ? StoneTypes.list[m2].getGlowingVariant().ordinal() : m2;
			int type = s.getValue(BlockPylonStructure.TYPE);
			if (type != m2 && type != m2b)
				return false;
		}
		return true;
	}

	private int getColumnBeam() {
		return this.getFacing().getStepY() == 0 ? 1 : 2;
	}

	@Override
	protected boolean checkEnhancedStructure() {
		Direction f = this.getFacing();
		for (int i = 2; i <= 4; i += 2) {
			BlockState s = this.stateAt(f.getStepX() * i, f.getStepY() * i, f.getStepZ() * i);
			if (s.getBlock() != ChromaBlocks.PYLONSTRUCT.get()
					|| s.getValue(BlockPylonStructure.TYPE) != StoneTypes.list[this.getColumnBeam()].getGlowingVariant().ordinal())
				return false;
		}
		return true;
	}

	@Override
	public ChromaStructures getPrimaryStructure() {
		return ChromaStructures.COMPOUND;
	}

	@Override
	public ChromaTiles getTile() {
		return ChromaTiles.COMPOUND;
	}

	@Override
	public CrystalElement getActiveColor() {
		return CrystalElement.elements[(this.getRenderColorWithOffset(7).ordinal() + 2) % 16];
	}

	@Override
	public void setSignalDepth(CrystalElement e, int d) {
		depth.put(e, d);
	}

	@Override
	public int getSignalDepth(CrystalElement e) {
		Integer d = depth.get(e);
		return d != null ? d.intValue() : -1;
	}

	@Override
	protected void readSyncTag(CompoundTag NBT) {
		super.readSyncTag(NBT);
		for (CrystalElement e : CrystalElement.elements) {
			String s = "depth_" + e.ordinal();
			if (NBT.contains(s))
				depth.put(e, NBT.getIntOr(s, 0));
		}
		connectedToPylon = NBT.getBooleanOr("pylon", false);
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT) {
		super.writeSyncTag(NBT);
		for (CrystalElement e : depth.keySet())
			NBT.putInt("depth_" + e.ordinal(), depth.get(e));
		NBT.putBoolean("pylon", connectedToPylon);
	}

	@Override
	public float getFailureWeight(CrystalElement e) {
		return 1.25F;
	}

	@Override
	public int getPathPriority() {
		return -10;
	}

	@Override
	public void notifySendingTo(CrystalPath p, CrystalReceiver r) {

	}

	@Override
	public void notifyReceivingFrom(CrystalPath p, CrystalTransmitter t) {
		if (t instanceof TileEntityCrystalPylon) {
			p.addBaseAttenuation(1000);
			connectedToPylon = true;
		}
		else {
			connectedToPylon = false;
		}
	}

	public boolean connectedToPylon() {
		return connectedToPylon;
	}
}
