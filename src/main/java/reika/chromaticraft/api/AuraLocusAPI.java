package reika.chromaticraft.api;

import java.util.Collection;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

import reika.dragonapi.instantiable.data.immutable.Coordinate;

public interface AuraLocusAPI {

	public Collection<Coordinate> getAuraPoints(EntityPlayer ep);

	public boolean isPointWithin(World world, int x, int y, int z, int r);

}
