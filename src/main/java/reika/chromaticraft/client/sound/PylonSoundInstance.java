package reika.chromaticraft.client.sound;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;

import reika.chromaticraft.registry.ChromaSounds;
import reika.chromaticraft.tileentity.networking.TileEntityCrystalPylon;

/** Continuous client-side pylon ambience which begins silently and attenuates while approaching. */
public final class PylonSoundInstance extends AbstractTickableSoundInstance {

    private final TileEntityCrystalPylon pylon;

    PylonSoundInstance(TileEntityCrystalPylon pylon) {
        super(ChromaSounds.POWER.getSoundEvent(), ChromaSounds.POWER.getCategory(),
                SoundInstance.createUnseededRandom());
        this.pylon = pylon;
        BlockPos pos = pylon.getBlockPos();
        x = pos.getX() + 0.5;
        y = pos.getY() + 0.5;
        z = pos.getZ() + 0.5;
        looping = true;
        delay = 0;
        this.updateSound();
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }

    @Override
    public void tick() {
        if (pylon.isRemoved() || !pylon.hasLevel() || !pylon.hasStructure()) {
            this.stop();
            return;
        }
        this.updateSound();
    }

    private void updateSound() {
        volume = ChromaSounds.POWER.getModulatedVolume();
        pitch = pylon.isEnhanced() ? 1.125F : 1F;
    }
}