package reika.chromaticraft.auxiliary.interfaces;

import java.util.Collection;

import reika.dragonapi.instantiable.data.immutable.Coordinate;

public interface ComplexAOE {

	public Collection<Coordinate> getPossibleRelativePositions();
	public double getNormalizedWeight(Coordinate c);

}
