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

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import reika.chromaticraft.magic.ElementTagCompound;
import reika.chromaticraft.magic.interfaces.CrystalReceiver;
import reika.chromaticraft.magic.interfaces.CrystalSource;
import reika.chromaticraft.magic.interfaces.CrystalTransmitter;
import reika.chromaticraft.magic.interfaces.LumenConsumer;
import reika.chromaticraft.magic.interfaces.LumenRequestingTile;
import reika.chromaticraft.magic.network.CrystalNetworker;
import reika.chromaticraft.magic.progression.ProgressStage;
import reika.chromaticraft.registry.CrystalElement;
import reika.dragonapi.instantiable.StepTimer;
import reika.dragonapi.instantiable.data.immutable.DecimalPosition;

/**
 * Base for network tiles that receive + store crystal energy (per-colour, capped by
 * {@link #getMaxStorage}). Handles the receive-cooldown, energy request/drain, and sync.
 *
 * <p>Deferred (re-add with their subsystems): the adjacency efficiency-upgrade boost
 * ({@code TileEntityAdjacencyUpgrade}/{@code TileEntityEfficiencyUpgrade} — efficiencyBoost stays 0),
 * the owner-data + item-stack energy transfer ({@code setDataFromItemStackTag} reads via the old
 * {@code stackTagCompound}; 26.2 needs a DataComponent rework), and the debug energy-fill block.
 */
public abstract class CrystalReceiverBase extends TileEntityCrystalBase implements CrystalReceiver, LumenConsumer, LumenRequestingTile {

	protected final ElementTagCompound energy = new ElementTagCompound();
	private int receiveCooldown = this.getCooldownLength();
	protected StepTimer checkTimer = new StepTimer(this.getCooldownLength());

	private long lastRequestDecrTime = -1;

	private int efficiencyBoost;

	protected CrystalReceiverBase(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Override
	public final void onAdjacentUpdate(Level world, BlockPos pos, net.minecraft.world.level.block.Block b) {
		//Deferred: adjacency efficiency recalculation.
		this.syncAllData(false);
	}

	@Override
	public final int getEfficiencyBoost() {
		return efficiencyBoost;
	}

	protected final float getEnergyCostScale() {
		return 1; //efficiency-upgrade cost scaling deferred (efficiencyBoost is always 0)
	}

	@Override
	public void updateEntity(Level world, BlockPos pos) {
		super.updateEntity(world, pos);
		long time = world.getGameTime();
		boolean flag = lastRequestDecrTime != time;
		if (flag)
			checkTimer.update();

		if (receiveCooldown > 0) {
			if (flag)
				receiveCooldown--;
		}
		lastRequestDecrTime = time;
	}

	protected int getCooldownLength() {
		return 40;
	}

	@Override
	protected void onFirstTick(Level world, BlockPos pos) {
		super.onFirstTick(world, pos);
		checkTimer.setTick(rand.nextInt(1 + checkTimer.getCap()));
	}

	protected final int getCooldown() {
		return receiveCooldown;
	}

	public abstract int getMaxStorage(CrystalElement e);

	public final int getEnergyScaled(CrystalElement e, int a) {
		return a * this.getEnergy(e) / this.getMaxStorage(e);
	}

	protected final boolean requestEnergy(CrystalElement e, int amount) {
		int amt = Math.min(amount, this.getRemainingSpace(e));
		boolean flag = false;
		if (amt > 0)
			flag = CrystalNetworker.instance.makeRequest(this, e, amt, this.getReceiveRange());
		if (flag) {
			Player ep = this.getPlacer();
			if (ep != null)
				ProgressStage.USEENERGY.stepPlayerTo(ep);
		}
		return flag;
	}

	protected final boolean requestEnergy(ElementTagCompound tag) {
		return this.requestEnergy(tag, false);
	}

	protected final boolean requestEnergy(ElementTagCompound tag, boolean requireAll) {
		boolean flag = true;
		boolean requested = false;
		if (requireAll) {
			for (CrystalElement e : tag.elementSet()) {
				if (!CrystalNetworker.instance.checkConnectivity(e, this))
					return false;
			}
		}
		for (CrystalElement e : tag.elementSet()) {
			int amount = Math.min(tag.getValue(e), this.getRemainingSpace(e));
			if (amount > 0) {
				requested = true;
				flag &= this.requestEnergy(e, amount);
			}
		}
		return requested && flag;
	}

	protected final boolean requestEnergyDifference(ElementTagCompound tag) {
		return this.requestEnergyDifference(tag, false);
	}

	protected final boolean requestEnergyDifference(ElementTagCompound tag, boolean requireAll) {
		ElementTagCompound needed = tag.copy();
		needed.subtract(energy);
		return this.requestEnergy(needed, requireAll);
	}

	public final int getRemainingSpace(CrystalElement e) {
		return this.getMaxStorage(e) - this.getEnergy(e);
	}

	public final float getFillFraction(CrystalElement e) {
		return (float)energy.getValue(e) / this.getMaxStorage(e);
	}

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

	@Override
	public final int receiveElement(CrystalSource src, CrystalElement e, int amt) {
		int add = Math.max(0, Math.min(amt, this.getRemainingSpace(e)));
		if (add <= 0)
			return 0;
		energy.addValueToColor(e, add);
		receiveCooldown = this.getCooldownLength();
		this.onReceiveEnergy(e, add);
		return add;
	}

	protected void onReceiveEnergy(CrystalElement e, int amt) {

	}

	public final int getEnergy(CrystalElement e) {
		return energy.getValue(e);
	}

	protected final void drainEnergy(CrystalElement e, int amt) {
		if (this.allowsEfficiencyBoost())
			amt = (int)Math.max(1, amt * this.getEnergyCostScale());
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

	protected final void clamp(CrystalElement e) {
		int max = this.getMaxStorage(e);
		if (this.getEnergy(e) > max)
			energy.put(e, max);
	}

	public final void setEnergy(CrystalElement e, int lvl) {
		energy.put(e, lvl);
	}

	@Override
	public DecimalPosition getTargetRenderOffset(CrystalElement e) {
		return null;
	}

	public final ElementTagCompound getEnergy() {
		return energy.copy();
	}

	public ElementTagCompound getRequestedTotal() {
		return this.getCapacity();
	}

	protected final ElementTagCompound getCapacity() {
		ElementTagCompound tag = new ElementTagCompound();
		for (int i = 0; i < CrystalElement.elements.length; i++) {
			CrystalElement e = CrystalElement.elements[i];
			if (this.isConductingElement(e))
				tag.put(e, this.getMaxStorage(e));
		}
		return tag;
	}

	protected final ElementTagCompound getDifference() {
		ElementTagCompound tag = new ElementTagCompound();
		for (int i = 0; i < CrystalElement.elements.length; i++) {
			CrystalElement e = CrystalElement.elements[i];
			tag.put(e, this.getMaxStorage(e) - this.getEnergy(e));
		}
		return tag;
	}

	@Override
	public void getTagsToWriteToStack(CompoundTag NBT) {
		energy.writeToNBT("energy", NBT);
		//Deferred: owner data (writeOwnerData) — owner system not yet ported.
	}

	@Override
	public void setDataFromItemStackTag(ItemStack is) {
		//Deferred: item-stack energy/owner transfer — 26.2 item NBT is DataComponent-based
		//(the 1.7.10 is.stackTagCompound is gone). Re-add with the item-component rework.
	}

	@Override
	public boolean canReceiveFrom(CrystalTransmitter r) {
		return true;
	}

	@Override
	public boolean needsLineOfSightFromTransmitter(CrystalTransmitter r) {
		return true;
	}

	@Override
	public boolean canBeSuppliedBy(CrystalSource te, CrystalElement e) {
		return true;
	}

	@Override
	public void addTooltipInfo(List li, boolean shift) {

	}

}
