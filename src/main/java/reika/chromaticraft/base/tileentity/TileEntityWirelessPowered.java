/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.base.tileentity;

import java.util.Collection;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import reika.chromaticraft.auxiliary.interfaces.AdjacencyUpgrade;
import reika.chromaticraft.magic.ElementTagCompound;
import reika.chromaticraft.magic.interfaces.LumenConsumer;
import reika.chromaticraft.magic.interfaces.WirelessSource;
import reika.chromaticraft.magic.network.CrystalNetworker;
import reika.chromaticraft.registry.CrystalElement;
import reika.chromaticraft.tileentity.aoe.TileEntityAuraPoint;
import reika.chromaticraft.tileentity.aoe.effect.TileEntityEfficiencyUpgrade;
import reika.dragonapi.DragonAPI;
import reika.dragonapi.libraries.registry.ReikaItemHelper;

/**
 * V33a {@code TileEntityWirelessPowered}: a lumen consumer fed without a network path, by requesting from any
 * {@link WirelessSource} (the Ambient Lumen Distributor, Lumen Leaves) within its receive range, half as far again for
 * a player who owns an Aura Point. A black Efficiency Augment on any face scales its costs.
 */
public abstract class TileEntityWirelessPowered extends TileEntityChromaticBase implements LumenConsumer {

	/** V33a: registers the black Efficiency Augment's check handler (its lexicon entry). */
	public static void loadAdjacencyHandler() {
		reika.chromaticraft.base.tileentity.TileEntityAdjacencyUpgrade.getOrCreateAdjacencyCheckHandler(CrystalElement.BLACK, null,
				java.util.List.of());
	}

	protected final ElementTagCompound energy = new ElementTagCompound();

	private int efficiencyBoost;

	protected TileEntityWirelessPowered(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Override
	public final void onAdjacentUpdate(Level world, BlockPos pos, Block b) {
		this.calcEfficiency();
		this.syncAllData(false);
	}

	@Override
	public final int getEfficiencyBoost() {
		return efficiencyBoost;
	}

	protected final float getEnergyCostScale() {
		float f = 1;
		int e = this.getEfficiencyBoost();
		if (e > 0)
			f *= TileEntityEfficiencyUpgrade.getCostFactor(e-1);
		return f;
	}

	private void calcEfficiency() {
		efficiencyBoost = AdjacencyUpgrade.getAdjacentTier(level, worldPosition, CrystalElement.BLACK);
	}

	protected final boolean requestEnergy(CrystalElement e, int amt) {
		if (DragonAPI.debugtest && !level.isClientSide()) {
			energy.addValueToColor(e, amt);
		}
		int r = this.getReceiveRange(e);
		if (TileEntityAuraPoint.hasAuraPoints(placerUUID))
			r *= 1.5;
		Collection<WirelessSource> c = CrystalNetworker.instance.getNearTilesOfType(level, worldPosition.getX(),
				worldPosition.getY(), worldPosition.getZ(), WirelessSource.class, r);
		for (WirelessSource s : c) {
			if (s.canConduct() && s.canTransmitTo(this)) {
				int ret = s.request(e, amt, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ());
				if (ret > 0) {
					energy.addValueToColor(e, ret);
					// V33a also sent ChromaPackets.WIRELESS here, whose client handler (doEnergyRequestClient, a
					// "particle bezier" TODO) was empty; the packet had no effect, so it is not sent.
					this.setChanged();
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public final int getEnergy(CrystalElement e) {
		return energy.getValue(e);
	}

	protected abstract int getReceiveRange(CrystalElement e);

	@Override
	protected void readSyncTag(CompoundTag NBT) {
		super.readSyncTag(NBT);

		energy.readFromNBT("energy", NBT);

		efficiencyBoost = NBT.getIntOr("eff", 0);
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT) {
		super.writeSyncTag(NBT);

		energy.writeToNBT("energy", NBT);

		NBT.putInt("eff", efficiencyBoost);
	}

	protected final void drainEnergy(CrystalElement e, int amt) {
		if (this.allowsEfficiencyBoost())
			amt = (int)Math.max(1, amt*this.getEnergyCostScale());
		energy.subtract(e, amt);
	}

	protected final void drainEnergy(ElementTagCompound tag) {
		if (this.allowsEfficiencyBoost()) {
			tag = tag.copy();
			tag.scale(this.getEnergyCostScale());
		}
		energy.subtract(tag);
	}

	@Override
	public boolean allowsEfficiencyBoost() {
		return true;
	}

	public final void setEnergyClient(CrystalElement e, int lvl) {
		energy.setTag(e, lvl);
	}

	@Override
	public final ElementTagCompound getEnergy() {
		return energy.copy();
	}

	@Override
	public void getTagsToWriteToStack(CompoundTag NBT) {
		this.writeOwnerData(NBT);
		energy.writeToNBT("energy", NBT);
	}

	@Override
	public void setDataFromItemStackTag(ItemStack is) {
		this.readOwnerData(is);
		CompoundTag tag = is != null ? ReikaItemHelper.getStackTag(is) : null;
		if (tag == null)
			return;
		energy.readFromNBT("energy", tag);
	}

	@Override
	public void addTooltipInfo(List li, boolean shift) {

	}

}
