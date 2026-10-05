package reika.chromaticraft.auxiliary.interfaces;

import reika.dragonapi.instantiable.data.immutable.Coordinate;

import java.util.Collection;

public interface ComplexAOE {

	Collection<Coordinate> getPossibleRelativePositions();
	double getNormalizedWeight(Coordinate c);

}
