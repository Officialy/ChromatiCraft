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

import java.util.Collection;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import reika.chromaticraft.base.tileentity.TileEntityPylonEnhancer;
import reika.chromaticraft.block.BlockCrystalRune;
import reika.chromaticraft.magic.network.CrystalNetworker;
import reika.chromaticraft.network.ChromaNetwork;
import reika.chromaticraft.registry.ChromaBlockEntities;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.registry.ChromaSounds;
import reika.chromaticraft.registry.ChromaTiles;
import reika.chromaticraft.registry.CrystalElement;
import reika.chromaticraft.tileentity.networking.TileEntityCrystalPylon;
import reika.dragonapi.instantiable.data.immutable.Coordinate;

/** One of the eight owner-bound power crystals surrounding a pylon. */
public class TileEntityChromaCrystal extends TileEntityPylonEnhancer {

    // Retained for V33a save compatibility; the abandoned RotaryCraft mechanical input never
    // affected the crystal's behavior, but old worlds may still contain these values.
    private int omega;
    private int torque;
    private long power;

    private Coordinate pylonLocation;

    public TileEntityChromaCrystal(BlockPos pos, BlockState state) {
        super(ChromaBlockEntities.POWER_CRYSTAL.get(), pos, state);
    }

    @Override
    public ChromaTiles getTile() {
        return ChromaTiles.CRYSTAL;
    }

    public boolean isConnected() {
        return this.getPylon() != null;
    }

    public TileEntityCrystalPylon getPylon() {
        if (pylonLocation == null || this.getLevel() == null)
            return null;
        BlockEntity tile = pylonLocation.getBlockEntity(this.getLevel());
        return tile instanceof TileEntityCrystalPylon pylon ? pylon : null;
    }

    @Override
    public void updateEntity(Level world, BlockPos pos) {
        if (this.getTicksExisted() < 5 && !world.isClientSide())
            this.refreshConnection();
    }

    @Override
    protected void onFirstTick(Level world, BlockPos pos) {
        super.onFirstTick(world, pos);
        if (!world.isClientSide())
            this.refreshConnection();
    }

    /** Re-evaluates the rune colour and the eight legal socket positions. */
    public TileEntityCrystalPylon refreshConnection() {
        TileEntityCrystalPylon old = this.getPylon();
        TileEntityCrystalPylon found = this.findPylon();
        pylonLocation = found != null ? new Coordinate(found) : null;
        if (found != old) {
            this.setChanged();
            this.syncAllData(true);
            this.triggerBlockUpdate();
        }
        return found;
    }

    private TileEntityCrystalPylon findPylon() {
        Level world = this.getLevel();
        BlockPos pos = this.getBlockPos();
        if (world == null || !ChromaBlocks.isRune(world.getBlockState(pos.below())))
            return null;
        CrystalElement color = BlockCrystalRune.getColor(world.getBlockState(pos.below()));
        Collection<TileEntityCrystalPylon> pylons = CrystalNetworker.instance.getNearbyPylons(
                world, pos.getX(), pos.getY(), pos.getZ(), color, 8, false);
        for (TileEntityCrystalPylon pylon : pylons) {
            if (pylon != null && pylon.isValidPowerCrystal(this))
                return pylon;
        }
        return null;
    }

    @Override
    protected void animateWithTick(Level world, BlockPos pos) {
        if (pylonLocation != null && rand.nextInt(3) == 0) {
            double x = pos.getX() + rand.nextDouble();
            double y = pos.getY() + rand.nextDouble();
            double z = pos.getZ() + rand.nextDouble();
            world.addParticle(ParticleTypes.END_ROD, x, y, z, 0, 0.015, 0);
        }
    }

    public void destroy() {
        Level world = this.getLevel();
        if (world == null)
            return;
        ChromaSounds.POWERDOWN.playSoundAtBlock(this, 2, 1);
        if (world instanceof net.minecraft.server.level.ServerLevel server) {
            ChromaNetwork.sendPowerCrystalDestroy(server, this.getBlockPos());
            world.levelEvent(2001, this.getBlockPos(), net.minecraft.world.level.block.Block.getId(this.getBlockState()));
        }
        this.delete();
    }

    public static void doDestroyParticles(Level world, int x, int y, int z) {
        RandomSource random = RandomSource.create();
        for (int i = 0; i < 40; i++) {
            double px = x + random.nextDouble();
            double py = y + random.nextDouble();
            double pz = z + random.nextDouble();
            double vx = (random.nextDouble() - 0.5) * 0.25;
            double vy = 0.0625 + random.nextDouble() * 0.125;
            double vz = (random.nextDouble() - 0.5) * 0.25;
            world.addParticle(i < 24 ? ParticleTypes.END_ROD : ParticleTypes.ENCHANT, px, py, pz, vx, vy, vz);
        }
    }

    @Override
    protected void writeSyncTag(CompoundTag tag) {
        super.writeSyncTag(tag);
        if (pylonLocation != null)
            pylonLocation.writeToNBT("pylon", tag);
        tag.putInt("omega", omega);
        tag.putInt("torque", torque);
        tag.putLong("power", power);
    }

    @Override
    protected void readSyncTag(CompoundTag tag) {
        super.readSyncTag(tag);
        pylonLocation = tag.contains("pylon") ? Coordinate.readFromNBT("pylon", tag) : null;
        omega = tag.getIntOr("omega", 0);
        torque = tag.getIntOr("torque", 0);
        power = tag.getLongOr("power", 0);
    }

    @Override
    public void breakBlock() {
        TileEntityCrystalPylon pylon = this.getPylon();
        if (pylon != null)
            pylon.onPowerCrystalBreak(this);
    }
}
