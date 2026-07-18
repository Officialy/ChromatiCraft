package reika.chromaticraft.auxiliary.interfaces;

import net.minecraft.world.World;

import reika.chromaticraft.entity.EntityLaserPulse;

public interface LaserPulseEffect {

	public boolean onImpact(World world, int x, int y, int z, EntityLaserPulse e);

}
