package reika.chromaticraft.client.sound;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

import reika.chromaticraft.registry.ChromaSounds;
import reika.chromaticraft.tileentity.TileEntityCrystalPortal;

/** Seamless client loop for the formed Portal Rift's V33a ambient sound. */
public final class PortalSoundInstance extends AbstractTickableSoundInstance {
	private static final float AUDIBLE_RANGE = 24F;

    private final TileEntityCrystalPortal portal;

    PortalSoundInstance(TileEntityCrystalPortal portal) {
        super(ChromaSounds.PORTAL.getSoundEvent(), ChromaSounds.PORTAL.getCategory(),
                SoundInstance.createUnseededRandom());
        this.portal = portal;
        BlockPos pos = portal.getBlockPos();
        x = pos.getX() + 0.5;
        y = pos.getY() + 0.5;
        z = pos.getZ() + 0.5;
        looping = true;
        delay = 0;
		// Vanilla linear attenuation decides volume only when the sound begins, which is why arriving
		// before the next one-shot was silent and crossing its radius could jump to full volume.
		attenuation = SoundInstance.Attenuation.NONE;
		volume = 0;
        pitch = 1F;
    }

	@Override
	public boolean canStartSilent() {
		return true;
	}

    @Override
    public void tick() {
		if (portal.isRemoved() || !portal.hasLevel() || !portal.isPadCentre() || !portal.isComplete()) {
            this.stop();
			return;
		}
		var player = Minecraft.getInstance().player;
		if (player == null) {
			volume = 0;
			return;
		}
		double distance = player.position().distanceTo(new net.minecraft.world.phys.Vec3(x, y, z));
		float proximity = Math.max(0F, 1F - (float)(distance / AUDIBLE_RANGE));
		// Smoothstep avoids an audible slope discontinuity at the edge while retaining the exact
		// zero-at-range and full-at-source endpoints.
		proximity = proximity * proximity * (3F - 2F * proximity);
		volume = ChromaSounds.PORTAL.getModulatedVolume() * proximity;
    }
}
