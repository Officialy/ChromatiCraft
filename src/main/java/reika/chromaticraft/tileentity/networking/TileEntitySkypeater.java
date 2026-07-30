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

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

import reika.chromaticraft.base.tileentity.CrystalTransmitterBase;
import reika.chromaticraft.magic.interfaces.CrystalReceiver;
import reika.chromaticraft.magic.interfaces.CrystalRepeater;
import reika.chromaticraft.magic.interfaces.CrystalSource;
import reika.chromaticraft.magic.interfaces.CrystalTransmitter;
import reika.chromaticraft.magic.interfaces.NaturalNetworkTile;
import reika.chromaticraft.registry.ChromaBlockEntities;
import reika.chromaticraft.registry.ChromaTiles;
import reika.chromaticraft.registry.CrystalElement;
import reika.dragonapi.instantiable.data.immutable.DecimalPosition;

/** The skypeater — a natural long-range network relay (spawned in luminous cliffs / near water). */
public class TileEntitySkypeater extends CrystalTransmitterBase implements CrystalRepeater, NaturalNetworkTile {

	private NodeClass type;

	public TileEntitySkypeater(BlockPos pos, BlockState state) {
		super(ChromaBlockEntities.SKYPEATER.get(), pos, state);
	}

	@Override
	public int receiveElement(CrystalSource src, CrystalElement e, int amt) {
		return 1;
	}

	@Override
	public int getReceiveRange() {
		return 64;
	}

	@Override
	public boolean canReceiveFrom(CrystalTransmitter r) {
		return r instanceof CrystalRepeater;
	}

	@Override
	public boolean needsLineOfSightFromTransmitter(CrystalTransmitter r) {
		return true;//!(r instanceof TileEntityAirRepeater);
	}

	@Override
	public boolean isConductingElement(CrystalElement e) {
		return true;
	}

	@Override
	public int maxThroughput() {
		return Integer.MAX_VALUE;
	}

	@Override
	public boolean canConduct() {
		return true;
	}

	@Override
	public DecimalPosition getTargetRenderOffset(CrystalElement e) {
		return null;
	}

	@Override
	public int getSendRange() {
		return 64;
	}

	@Override
	public boolean needsLineOfSightToReceiver(CrystalReceiver r) {
		return true;//!(r instanceof TileEntityAirRepeater);
	}

	@Override
	public boolean canTransmitTo(CrystalReceiver r) {
		return r instanceof CrystalRepeater;
	}

	@Override
	public int getPathPriority() {
		return -800000;
	}

	@Override
	public int getSignalDegradation(boolean point) {
		return 0;
	}

	@Override
	public int getThoughputBonus(boolean point) {
		return point ? 100 : 0;
	}

	@Override
	public int getThoughputInsurance() {
		return 0;
	}

	@Override
	public int getSignalDepth(CrystalElement e) {
		return 0;
	}

	@Override
	public void setSignalDepth(CrystalElement e, int d) {

	}

	@Override
	public boolean checkConnectivity() {
		return false;
	}

	public boolean canBeSuppliedBy(CrystalSource te, CrystalElement e) {
		return true;
	}

	@Override
	public ChromaTiles getTile() {
		return ChromaTiles.SKYPEATER;
	}

	public void setNodeType(NodeClass c) {
		type = c;
		this.syncAllData(false);
	}

	public NodeClass getNodeType() {
		return type != null ? type : NodeClass.WATER;
	}

	@Override
	protected void saveAdditional(CompoundTag NBT) {
		super.saveAdditional(NBT);

		NBT.putInt("type", this.getNodeType().ordinal());
	}

	@Override
	public void load(CompoundTag NBT) {
		super.load(NBT);

		type = NodeClass.list[NBT.getIntOr("type", 0)];
	}

	public static enum NodeClass {
		WATER(0x22aaff),
		SHORE(0xff00ff),
		//PLATEAU(0xff0000),
		;

		public final int color;

		private static final NodeClass[] list = values();

		private NodeClass(int c) {
			color = c;
		}

		public boolean isAbove(NodeClass c) {
			return c.ordinal() > this.ordinal();
		}
	}

}
