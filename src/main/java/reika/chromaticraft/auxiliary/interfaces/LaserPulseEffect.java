package reika.chromaticraft.auxiliary.interfaces;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import reika.chromaticraft.entity.EntityLaserPulse;

/** Blocks outside the Chromatic Beams effector family that consume or redirect a laser pulse. */
public interface LaserPulseEffect {
	boolean onImpact(Level world, BlockPos pos, EntityLaserPulse pulse);
}
