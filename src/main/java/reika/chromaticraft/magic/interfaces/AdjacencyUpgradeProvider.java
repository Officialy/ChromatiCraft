package reika.chromaticraft.magic.interfaces;

import reika.chromaticraft.registry.CrystalElement;

public interface AdjacencyUpgradeProvider {

	CrystalElement getAdjacencyColor();

	int getAdjacencyTier();

	boolean isAdjacencyUpgradeActive();
}
