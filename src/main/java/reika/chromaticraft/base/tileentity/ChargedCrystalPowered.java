package reika.chromaticraft.base.tileentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import reika.chromaticraft.auxiliary.interfaces.AdjacencyUpgrade;
import reika.chromaticraft.items.ItemStorageCrystal;
import reika.chromaticraft.magic.ElementTagCompound;
import reika.chromaticraft.magic.interfaces.LumenConsumer;
import reika.chromaticraft.registry.CrystalElement;

/** V33a's charged-storage-crystal machine base, with the original black efficiency upgrade scale. */
public abstract class ChargedCrystalPowered extends InventoriedChromaticBase implements LumenConsumer {
	private int efficiencyBoost;

	protected ChargedCrystalPowered(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Override
	protected void onFirstTick(Level world, BlockPos pos) {
		super.onFirstTick(world, pos);
		this.calcEfficiency();
	}

	@Override
	public void onAdjacentUpdate(Level world, BlockPos pos, Block changed) {
		this.calcEfficiency();
		this.syncAllData(false);
	}

	private void calcEfficiency() {
		if (level == null)
			return;
		efficiencyBoost = AdjacencyUpgrade.getAdjacentTier(level, worldPosition, CrystalElement.BLACK);
	}

	@Override
	protected void readSyncTag(CompoundTag tag) {
		super.readSyncTag(tag);
		efficiencyBoost = tag.getIntOr("eff", 0);
	}

	@Override
	protected void writeSyncTag(CompoundTag tag) {
		super.writeSyncTag(tag);
		tag.putInt("eff", efficiencyBoost);
	}

	@Override
	public final int getEfficiencyBoost() {
		return efficiencyBoost;
	}

	@Override
	public boolean allowsEfficiencyBoost() {
		return true;
	}

	protected final float getEnergyCostScale() {
		float factor = this.getCostModifier();
		if (efficiencyBoost > 0)
			factor *= reika.chromaticraft.tileentity.aoe.effect.TileEntityEfficiencyUpgrade.getCostFactor(Math.min(8, efficiencyBoost) - 1);
		return factor;
	}

	public abstract float getCostModifier();
	public abstract boolean usesColor(CrystalElement element);
	public abstract ElementTagCompound getRequiredEnergy();
	protected abstract boolean canExtractOtherItem(int slot, ItemStack stack, Direction side);
	protected abstract boolean isItemValidForOtherSlot(int slot, ItemStack stack);

	@Override
	public final int getEnergy(CrystalElement element) {
		return element != null ? ItemStorageCrystal.getStoredEnergy(inv.get(0), element) : 0;
	}

	protected final int getStoredEnergy(boolean onlyUsedColors) {
		ElementTagCompound stored = ItemStorageCrystal.getStoredTags(inv.get(0));
		if (onlyUsedColors) {
			for (CrystalElement element : CrystalElement.elements) {
				if (!this.usesColor(element))
					stored.removeTag(element);
			}
		}
		return stored.getTotalEnergy();
	}

	protected final void useEnergy(CrystalElement element, int amount) {
		if (this.allowsEfficiencyBoost())
			amount = (int)Math.max(1, amount * this.getEnergyCostScale());
		ItemStorageCrystal.removeEnergy(inv.get(0), element, amount);
		this.setChanged();
	}

	protected final void useEnergy(ElementTagCompound tag) {
		for (CrystalElement element : tag.elementSet())
			this.useEnergy(element, tag.getValue(element));
	}

	@Override
	public final int getMaxStorage(CrystalElement element) {
		return ItemStorageCrystal.getCapacity(inv.get(0));
	}

	protected final boolean hasEnergy(ElementTagCompound tag) {
		for (CrystalElement element : tag.elementSet()) {
			if (this.getEnergy(element) < tag.getValue(element))
				return false;
		}
		return true;
	}

	@Override
	public final ElementTagCompound getEnergy() {
		ElementTagCompound tag = new ElementTagCompound();
		for (CrystalElement element : CrystalElement.elements) {
			if (this.usesColor(element))
				tag.setTag(element, this.getEnergy(element));
		}
		return tag;
	}

	@Override
	public final boolean canPlaceItem(int slot, ItemStack stack) {
		return slot == 0 ? ItemStorageCrystal.isStorageCrystal(stack)
				: this.isItemValidForOtherSlot(slot, stack);
	}

	@Override
	public final boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
		return slot == 0 ? this.getStoredEnergy(true) == 0 : this.canExtractOtherItem(slot, stack, side);
	}
}
