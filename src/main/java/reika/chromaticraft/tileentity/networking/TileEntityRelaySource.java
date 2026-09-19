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

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import reika.chromaticraft.auxiliary.interfaces.MultiBlockChromaTile;
import reika.chromaticraft.base.tileentity.InventoriedCrystalReceiver;
import reika.chromaticraft.items.ItemStorageCrystal;
import reika.chromaticraft.magic.interfaces.WeakRepeaterSafeReceiver;
import reika.chromaticraft.magic.progression.LexiconCatalog;
import reika.chromaticraft.magic.progression.PlayerResearch;
import reika.chromaticraft.magic.progression.ProgressStage;
import reika.chromaticraft.registry.ChromaBlockEntities;
import reika.chromaticraft.registry.ChromaItems;
import reika.chromaticraft.registry.ChromaStructures;
import reika.chromaticraft.registry.ChromaTiles;
import reika.chromaticraft.registry.CrystalElement;
import reika.chromaticraft.render.particle.ChromaParticle;
import reika.dragonapi.instantiable.data.immutable.Coordinate;
import reika.dragonapi.interfaces.blockentity.InertIInv;

/**
 * V33a's all-colour Lumen Relay source. A charged Storage Crystal can be inserted directly into
 * the machine and is unloaded into sixteen independently capped network buffers. The researched
 * 5x5 focus below it raises capacity, range and throughput and halves the adaptive request delay.
 */
public final class TileEntityRelaySource extends InventoriedCrystalReceiver
		implements InertIInv, MultiBlockChromaTile, WeakRepeaterSafeReceiver {

	public static final int CAPACITY = 720_000;
	public static final int ENHANCED_CAPACITY = 3_600_000;
	private static final int MIN_COOLDOWN = 100;
	private static final int MAX_COOLDOWN = 200;

	private int cooldown = MAX_COOLDOWN;
	private final int[] drainValue = new int[CrystalElement.elements.length];
	private int visualCrystalCapacity;
	private final int[] visualCrystalEnergy = new int[CrystalElement.elements.length];
	private boolean enhanced;
	private boolean hasEnhancedStructure;

	public TileEntityRelaySource(BlockPos pos, BlockState state) {
		super(ChromaBlockEntities.RELAY_SOURCE.get(), pos, state);
	}

	@Override protected int getCooldownLength() { return cooldown; }
	@Override public boolean allowsEfficiencyBoost() { return false; }

	@Override
	public void updateEntity(Level world, BlockPos pos) {
		super.updateEntity(world, pos);
		if (!world.isClientSide()) {
			if (checkTimer.checkCap()) this.checkAndRequest();
			this.updateAdaptiveCooldown();
			this.transferFromCrystal();
			if (this.getTicksExisted() % 20 == 0) this.validateStructure();
		}
		else if (enhanced) {
			ChromaParticle.spawnRelaySource(world, pos, this.getTicksExisted(), this.rand);
		}
	}

	@Override
	protected void onFirstTick(Level world, BlockPos pos) {
		super.onFirstTick(world, pos);
		if (!world.isClientSide()) this.validateStructure();
	}

	private void updateAdaptiveCooldown() {
		int averageDrain = 0;
		for (int i = 0; i < drainValue.length; i++) {
			drainValue[i] = (int)(drainValue[i] * 0.95F);
			averageDrain += drainValue[i];
		}
		averageDrain /= drainValue.length;
		if (cooldown > MIN_COOLDOWN && averageDrain >= CAPACITY / cooldown)
			cooldown--;
		else if (cooldown < MAX_COOLDOWN)
			cooldown++;
		checkTimer.setCap(Math.max(1, enhanced ? cooldown / 2 : cooldown));
	}

	private void transferFromCrystal() {
		ItemStack crystal = inv.getFirst();
		if (!ItemStorageCrystal.isStorageCrystal(crystal)) return;
		boolean changed = false;
		for (CrystalElement element : ItemStorageCrystal.getStoredTags(crystal).elementSet()) {
			int amount = ItemStorageCrystal.getStoredEnergy(crystal, element);
			int add = Math.min(amount,
					Math.min(this.getRemainingSpace(element), this.maxThroughput() * 4));
			if (add <= 0) continue;
			ItemStorageCrystal.removeEnergy(crystal, element, add);
			energy.addValueToColor(element, add);
			changed = true;
		}
		if (changed) {
			this.setChanged();
			this.syncAllData(false);
		}
	}

	private void checkAndRequest() {
		float threshold = enhanced ? 0.8F : 0.5F;
		for (CrystalElement element : CrystalElement.elements) {
			if (this.getFillFraction(element) < threshold)
				this.requestEnergy(element, this.getRemainingSpace(element));
		}
	}

	@Override
	public void validateStructure() {
		Level world = this.getLevel();
		if (world == null || world.isClientSide()) return;
		boolean structure = ChromaStructures.RELAY.getArray(world, this.getX(), this.getY(), this.getZ())
				.matchInWorld();
		Player owner = this.getPlacer();
		var page = LexiconCatalog.byId("RELAYSTRUCT");
		boolean active = structure && owner != null && page != null
				&& PlayerResearch.hasFragment(owner, page);
		if (structure != hasEnhancedStructure || active != enhanced) {
			hasEnhancedStructure = structure;
			enhanced = active;
			for (CrystalElement element : CrystalElement.elements) this.clamp(element);
			checkTimer.setCap(Math.max(1, enhanced ? cooldown / 2 : cooldown));
			this.setChanged();
			this.syncAllData(true);
		}
	}

	/** Records actual relay-network draw so V33a's request cadence can adapt to sustained demand. */
	public void onDrain(CrystalElement element, int amount) {
		if (element == null || amount <= 0) return;
		drainValue[element.ordinal()] += amount;
		if (this.getLevel() instanceof ServerLevel server) {
			for (Player player : server.players()) {
				if (player.distanceToSqr(Vec3.atCenterOf(this.getBlockPos())) <= 64)
					ProgressStage.RELAYS.stepPlayerTo(player);
			}
		}
	}

	@Override public int getReceiveRange() { return enhanced ? 48 : 32; }
	@Override public boolean isConductingElement(CrystalElement element) { return element != null; }
	@Override public int maxThroughput() { return enhanced ? 30_000 : 6_000; }
	@Override public boolean canConduct() { return true; }
	@Override public int getMaxStorage(CrystalElement element) {
		return enhanced ? ENHANCED_CAPACITY : CAPACITY;
	}
	@Override public ChromaTiles getTile() { return ChromaTiles.RELAYSOURCE; }
	@Override protected void animateWithTick(Level world, BlockPos pos) {}

	@Override public int getSizeInventory() { return 1; }
	@Override public int getMaxStackSize() { return 1; }
	@Override public boolean canPlaceItem(int slot, ItemStack stack) {
		return slot == 0 && ChromaItems.isStorageCrystal(stack)
				&& ItemStorageCrystal.getTotalEnergy(stack) > 0;
	}
	@Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
		return side == Direction.DOWN;
	}

	public boolean isEnhanced() { return enhanced; }
	public boolean hasEnhancedStructure() { return hasEnhancedStructure; }
	public int requestCooldown() { return cooldown; }
	public int getRenderedCrystalCapacity() {
		return this.getLevel() != null && this.getLevel().isClientSide()
				? visualCrystalCapacity : ItemStorageCrystal.getCapacity(inv.getFirst());
	}
	public int getRenderedCrystalEnergy(CrystalElement element) {
		return this.getLevel() != null && this.getLevel().isClientSide()
				? visualCrystalEnergy[element.ordinal()]
				: ItemStorageCrystal.getStoredEnergy(inv.getFirst(), element);
	}
	public void noteDrainForTest(CrystalElement element, int amount) { this.onDrain(element, amount); }
	public void runAdaptiveCycleForTest() { this.updateAdaptiveCooldown(); }
	public void runTransferCycleForTest() { this.transferFromCrystal(); }

	@Override public ChromaStructures getPrimaryStructure() { return ChromaStructures.RELAY; }
	@Override public Coordinate getStructureOffset() { return null; }
	@Override public boolean canStructureBeInspected() { return true; }
	@Override public boolean hasStructure() { return hasEnhancedStructure; }

	@Override
	protected void readSyncTag(CompoundTag tag) {
		super.readSyncTag(tag);
		enhanced = tag.getBooleanOr("enhance", false);
		hasEnhancedStructure = tag.getBooleanOr("enstruct", false);
		cooldown = Math.clamp(tag.getIntOr("relayCooldown", MAX_COOLDOWN),
				MIN_COOLDOWN, MAX_COOLDOWN);
		for (int i = 0; i < drainValue.length; i++)
			drainValue[i] = Math.max(0, tag.getIntOr("relayDrain" + i, 0));
		visualCrystalCapacity = Math.max(0, tag.getIntOr("relayCrystalCapacity", 0));
		for (int i = 0; i < visualCrystalEnergy.length; i++)
			visualCrystalEnergy[i] = Math.max(0, tag.getIntOr("relayCrystal" + i, 0));
	}

	@Override
	protected void writeSyncTag(CompoundTag tag) {
		super.writeSyncTag(tag);
		tag.putBoolean("enhance", enhanced);
		tag.putBoolean("enstruct", hasEnhancedStructure);
		tag.putInt("relayCooldown", cooldown);
		for (int i = 0; i < drainValue.length; i++) tag.putInt("relayDrain" + i, drainValue[i]);
		ItemStack crystal = inv.getFirst();
		tag.putInt("relayCrystalCapacity", ItemStorageCrystal.getCapacity(crystal));
		for (CrystalElement element : CrystalElement.elements)
			tag.putInt("relayCrystal" + element.ordinal(),
					ItemStorageCrystal.getStoredEnergy(crystal, element));
	}

	@Override public void addTooltipInfo(List list, boolean shift) {}
}
