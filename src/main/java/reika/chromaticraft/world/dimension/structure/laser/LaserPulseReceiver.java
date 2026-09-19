package reika.chromaticraft.world.dimension.structure.laser;

import reika.chromaticraft.entity.EntityLaserPulse;

/** Runtime block-entity contract used by each of the twelve named laser effectors. */
public interface LaserPulseReceiver {
	/** @return true when the incoming entity is absorbed at this receiver. */
	boolean receiveLaserPulse(EntityLaserPulse pulse);
}
