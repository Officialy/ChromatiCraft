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
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import reika.chromaticraft.magic.ElementTagCompound;
import reika.chromaticraft.magic.interfaces.AdjacencyUpgradeProvider;
import reika.chromaticraft.magic.interfaces.LumenConsumer;
import reika.chromaticraft.magic.network.RelayNetworker;
import reika.chromaticraft.magic.progression.ProgressStage;
import reika.chromaticraft.registry.CrystalElement;
import reika.chromaticraft.tileentity.networking.TileEntityRelaySource;
import reika.dragonapi.DragonAPI;
import reika.dragonapi.libraries.registry.ReikaItemHelper;

public abstract class TileEntityRelayPowered extends TileEntityChromaticBase implements LumenConsumer {

    private static final float[] EFFICIENCY_FACTORS = {
            0.9375F, 0.875F, 0.75F, 0.625F, 0.5F, 0.25F, 0.125F, 0.0625F
    };

    protected final ElementTagCompound energy = new ElementTagCompound();
    private int requestTimer = rand.nextInt(200);
    private long lastRequestDecrTime = -1;
    private int efficiencyBoost;

    protected TileEntityRelayPowered(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public final void onAdjacentUpdate(Level world, BlockPos pos, Block block) {
        if (!world.isClientSide()) {
            this.calcEfficiency();
            this.syncAllData(false);
        }
    }

    @Override
    protected void onFirstTick(Level world, BlockPos pos) {
        super.onFirstTick(world, pos);
        if (!world.isClientSide()) this.calcEfficiency();
    }

    @Override public final int getEfficiencyBoost() { return efficiencyBoost; }

    protected final float getEnergyCostScale() {
        return efficiencyBoost > 0 ? EFFICIENCY_FACTORS[Math.min(efficiencyBoost - 1,
                EFFICIENCY_FACTORS.length - 1)] : 1;
    }

    private void calcEfficiency() {
        efficiencyBoost = 0;
        if (level == null) return;
        for (Direction direction : Direction.values()) {
            BlockEntity adjacent = level.getBlockEntity(worldPosition.relative(direction));
            if (adjacent instanceof AdjacencyUpgradeProvider upgrade
                    && upgrade.getAdjacencyColor() == CrystalElement.BLACK
                    && upgrade.isAdjacencyUpgradeActive())
                efficiencyBoost = Math.max(efficiencyBoost, 1 + upgrade.getAdjacencyTier());
        }
    }

    @Override
    public void updateEntity(Level world, BlockPos pos) {
        if (world.isClientSide()) return;
        if (DragonAPI.debugtest) {
            CrystalElement element = CrystalElement.randomElement();
            if (this.isAcceptingColor(element)) energy.addValueToColor(element, 500);
        }
        if (!this.makeRequests()) return;
        if (requestTimer == 0) {
            for (CrystalElement element : this.getRequiredEnergy().elementSet()) {
                for (Direction direction : Direction.values()) {
                    if (this.canReceiveFrom(element, direction)
                            && this.requestEnergy(element, this.getRemainingSpace(element), direction))
                        break;
                }
            }
            requestTimer = 200;
            this.setChanged();
        }
        else {
            long time = world.getGameTime();
            if (lastRequestDecrTime != time) {
                requestTimer--;
                this.setChanged();
            }
            lastRequestDecrTime = time;
        }
    }

    protected boolean makeRequests() { return true; }
    protected abstract boolean canReceiveFrom(CrystalElement element, Direction direction);
    public abstract ElementTagCompound getRequiredEnergy();

    private boolean requestEnergy(CrystalElement element, int amount, Direction direction) {
        if (amount <= 0) return true;
        TileEntityRelaySource source = RelayNetworker.instance.findRelaySource(
                level, worldPosition, direction, element, amount, 128);
        if (source == null) return false;
        int available = source.getEnergy(element);
        int transfer = Math.max(0, Math.min(Math.min(amount, available), this.getRemainingSpace(element)));
        if (transfer > 0) {
            source.drainEnergy(element, transfer);
            source.onDrain(element, transfer);
            source.setChanged();
            energy.addValueToColor(element, transfer);
            this.setChanged();
            var owner = this.getPlacer();
            if (owner != null) ProgressStage.RELAYS.stepPlayerTo(owner);
        }
        return available >= amount;
    }

    public abstract boolean isAcceptingColor(CrystalElement element);
    @Override public abstract int getMaxStorage(CrystalElement element);
    @Override public final int getEnergy(CrystalElement element) { return energy.getValue(element); }
    @Override public final ElementTagCompound getEnergy() { return energy.copy(); }

    public final int getEnergyScaled(CrystalElement element, int scale) {
        int capacity = this.getMaxStorage(element);
        return capacity > 0 ? (int)((long)scale * this.getEnergy(element) / capacity) : 0;
    }

    public final int getRemainingSpace(CrystalElement element) {
        return Math.max(0, this.getMaxStorage(element) - this.getEnergy(element));
    }

    @Override
    protected void readSyncTag(CompoundTag tag) {
        super.readSyncTag(tag);
        energy.readFromNBT("energy", tag);
        efficiencyBoost = tag.getIntOr("eff", 0);
        requestTimer = Math.clamp(tag.getIntOr("relayRequestTimer", requestTimer), 0, 200);
    }

    @Override
    protected void writeSyncTag(CompoundTag tag) {
        super.writeSyncTag(tag);
        energy.writeToNBT("energy", tag);
        tag.putInt("eff", efficiencyBoost);
        tag.putInt("relayRequestTimer", requestTimer);
    }

    protected final void drainEnergy(CrystalElement element, int amount) {
        if (this.allowsEfficiencyBoost())
            amount = (int)Math.max(1, amount * this.getEnergyCostScale());
        energy.subtract(element, amount);
        this.setChanged();
    }

    protected final void drainEnergy(ElementTagCompound cost) {
        if (this.allowsEfficiencyBoost()) {
            cost = cost.copy();
            cost.scale(this.getEnergyCostScale());
        }
        energy.subtract(cost);
        this.setChanged();
    }

    @Override public boolean allowsEfficiencyBoost() { return true; }

    public final void setEnergyClient(CrystalElement element, int amount) {
        energy.setTag(element, amount);
    }

    @Override
    public void getTagsToWriteToStack(CompoundTag tag) {
        energy.writeToNBT("energy", tag);
        if (placer != null && !placer.isEmpty()) tag.putString("place", placer);
        if (placerUUID != null) tag.putString("placeUUID", placerUUID.toString());
    }

    @Override
    public void setDataFromItemStackTag(ItemStack stack) {
        CompoundTag tag = ReikaItemHelper.getStackTag(stack);
        if (tag == null) {
            energy.clear();
            return;
        }
        energy.readFromNBT("energy", tag);
        placer = tag.getStringOr("place", placer != null ? placer : "");
        String owner = tag.getStringOr("placeUUID", "");
        if (!owner.isEmpty()) {
            try { placerUUID = UUID.fromString(owner); }
            catch (IllegalArgumentException ignored) { placerUUID = this.getPlacerID(); }
        }
        this.setChanged();
    }

    @Override
    public void addTooltipInfo(List lines, boolean shift) {
        if (shift) {
            for (CrystalElement element : energy.elementSet())
                lines.add(net.minecraft.network.chat.Component.literal(
                        element.displayName() + ": " + energy.getValue(element)));
        }
    }
}
