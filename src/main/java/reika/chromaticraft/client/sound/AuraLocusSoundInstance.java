package reika.chromaticraft.client.sound;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import reika.chromaticraft.registry.ChromaDimensions;
import reika.chromaticraft.registry.ChromaSounds;
import reika.chromaticraft.tileentity.aoe.TileEntityAuraPoint;

/** Immediately responsive, distance-attenuated form of V33a's repeating Aura Locus ambience. */
public final class AuraLocusSoundInstance extends AbstractTickableSoundInstance {

	/** V33a emitted at volume two; vanilla's variable-range rule makes that audible for 32 blocks. */
	private static final float AUDIBLE_RANGE = 32F;
	private final TileEntityAuraPoint locus;

	AuraLocusSoundInstance(TileEntityAuraPoint locus) {
		super(ChromaSounds.AURALOCUS.getSoundEvent(), ChromaSounds.AURALOCUS.getCategory(),
				SoundInstance.createUnseededRandom());
		this.locus = locus;
		BlockPos pos = locus.getBlockPos();
		x = pos.getX() + 0.5;
		y = pos.getY() + 0.5;
		z = pos.getZ() + 0.5;
		looping = true;
		delay = 0;
		attenuation = SoundInstance.Attenuation.NONE;
		volume = 0;
		this.updateSound();
	}

	@Override
	public boolean canStartSilent() {
		return true;
	}

	@Override
	public void tick() {
		if (locus.isRemoved() || !locus.hasLevel()) {
			this.stop();
			return;
		}
		this.updateSound();
	}

	private void updateSound() {
		var player = Minecraft.getInstance().player;
		if (player == null) {
			volume = 0;
			return;
		}
		double distance = player.distanceToSqr(Vec3.atCenterOf(locus.getBlockPos()));
		float proximity = Math.max(0F, 1F - (float)(Math.sqrt(distance) / AUDIBLE_RANGE));
		// V33a's source volume and Proxima pitch are preserved; only its sporadic one-shot launch is
		// replaced by a live loop so moving into or out of range is reflected on the next sound tick.
		volume = 2F * proximity;
		pitch = locus.getLevel().dimension() == ChromaDimensions.PROXIMA ? 2F : 1F;
	}
}
