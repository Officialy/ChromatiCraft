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

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import reika.chromaticraft.base.tileentity.InventoriedCrystalReceiver;
import reika.chromaticraft.container.MenuCrystalCharger;
import reika.chromaticraft.items.ItemStorageCrystal;
import reika.chromaticraft.magic.network.CrystalNetworker;
import reika.chromaticraft.magic.progression.ProgressStage;
import reika.chromaticraft.registry.ChromaBlockEntities;
import reika.chromaticraft.registry.ChromaItems;
import reika.chromaticraft.registry.ChromaTiles;
import reika.chromaticraft.registry.CrystalElement;

/** Complete V33a two-slot network charger for the component-backed Storage Crystal family. */
public final class TileEntityCrystalCharger extends InventoriedCrystalReceiver implements MenuProvider {

	public static final int CAPACITY = 120000;
	private static final int TOGGLE_MASK = (1 << CrystalElement.elements.length) - 1;

	private float angle;
	private int toggles = TOGGLE_MASK;

	public TileEntityCrystalCharger(BlockPos pos, BlockState state) {
		super(ChromaBlockEntities.CRYSTAL_CHARGER.get(), pos, state);
	}

	@Override
	public ChromaTiles getTile() {
		return ChromaTiles.CHARGER;
	}

	@Override
	public void updateEntity(Level world, BlockPos pos) {
		super.updateEntity(world, pos);
		if (world.isClientSide()) return;
		if (this.getCooldown() == 0 && checkTimer.checkCap()) this.checkAndRequest();
		this.transferToCrystal();
	}

	private void transferToCrystal() {
		if (!this.hasItem()) return;
		ItemStack crystal = inv.get(0);
		for (CrystalElement element : CrystalElement.elements) {
			if (!this.isToggled(element)) continue;
			int stored = this.getEnergy(element);
			int put = Math.min(this.getMaxTransfer(element),
					Math.min(stored, ItemStorageCrystal.getSpace(element, crystal)));
			if (put <= 0) continue;
			ItemStorageCrystal.addEnergy(crystal, element, put);
			this.drainEnergy(element, put);
			Player placer = this.getPlacer();
			if (placer != null) ProgressStage.CHARGECRYSTAL.stepPlayerTo(placer);
			this.setChanged();
		}
	}

	private int getMaxTransfer(CrystalElement element) {
		int maximum = 10 + (int)Math.sqrt(this.getEnergy(element));
		return this.hasSpeedUpgrade() ? 8 * maximum : maximum;
	}

	public boolean hasSpeedUpgrade() {
		return inv.get(1).is(ChromaItems.SPEED_UPGRADE.get());
	}

	private void checkAndRequest() {
		for (CrystalElement element : CrystalElement.elements) {
			if (!this.isToggled(element)) continue;
			int capacity = this.getMaxStorage(element);
			int stored = this.getEnergy(element);
			if (this.hasItem()) {
				capacity += ItemStorageCrystal.getCapacity(inv.get(0));
				stored += ItemStorageCrystal.getStoredEnergy(inv.get(0), element);
			}
			int space = capacity - stored;
			if (space > 0) this.requestEnergy(element, space);
		}
	}

	@Override
	protected void onInventorySlotChanged(int slot) {
		if (this.getLevel() != null && !this.getLevel().isClientSide())
			CrystalNetworker.instance.breakPaths(this);
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {
		if (world == null) {
			angle = 0;
			return;
		}
		int total = energy.getTotalEnergy();
		if (this.hasItem()) total += ItemStorageCrystal.getTotalEnergy(inv.get(0));
		if (total > 0) angle = (float)((angle + Math.log(total) / Math.log(2)) % 180D);
	}

	public float getAngle(float partialTick) {
		return angle;
	}

	@Override public int getReceiveRange() { return 20; }
	@Override public boolean isConductingElement(CrystalElement element) { return element != null; }
	@Override public int maxThroughput() { return 4000; }
	@Override public boolean canConduct() { return true; }
	@Override public int getMaxStorage(CrystalElement element) { return CAPACITY; }

	@Override
	public int getSizeInventory() {
		return 2;
	}

	@Override
	public int getMaxStackSize() {
		return 1;
	}

	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		return switch (slot) {
			case 0 -> ChromaItems.isStorageCrystal(stack);
			case 1 -> stack.is(ChromaItems.SPEED_UPGRADE.get());
			default -> false;
		};
	}

	@Override
	public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
		return slot == 0 && ChromaItems.isStorageCrystal(stack) && ItemStorageCrystal.isFull(stack);
	}

	public boolean hasItem() {
		return ChromaItems.isStorageCrystal(inv.get(0));
	}

	public boolean isToggled(CrystalElement element) {
		return (toggles & 1 << element.ordinal()) != 0;
	}

	public int toggleFlags() {
		return toggles;
	}

	public void toggle(CrystalElement element) {
		toggles ^= 1 << element.ordinal();
		this.setChanged();
		this.syncAllData(false);
	}

	@Override
	protected void readSyncTag(CompoundTag tag) {
		super.readSyncTag(tag);
		toggles = tag.contains("toggle") ? tag.getIntOr("toggle", TOGGLE_MASK) & TOGGLE_MASK : TOGGLE_MASK;
	}

	@Override
	protected void writeSyncTag(CompoundTag tag) {
		super.writeSyncTag(tag);
		tag.putInt("toggle", toggles);
	}

	@Override
	public Component getDisplayName() {
		return Component.translatable("block.chromaticraft.crystal_charger");
	}

	@Override
	public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
		return new MenuCrystalCharger(id, inventory, this);
	}

	/** Focused test seam: production transfer remains the exact per-tick implementation above. */
	public void runTransferCycleForTest() {
		this.transferToCrystal();
	}

	/** Focused persistence/menu test seam without exposing the mutable backing array. */
	public void setToggleFlagsForTest(int flags) {
		toggles = flags & TOGGLE_MASK;
	}
}
