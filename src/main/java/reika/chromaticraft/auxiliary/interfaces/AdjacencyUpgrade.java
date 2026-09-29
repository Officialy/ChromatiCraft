package reika.chromaticraft.auxiliary.interfaces;

import reika.chromaticraft.registry.CrystalElement;

/** The active tier and colour supplied by an operating Adjacency Upgrade. */
public interface AdjacencyUpgrade {
	CrystalElement getColor();
	int getTier();
	boolean isOperating();
}
