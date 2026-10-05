/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.magic.interfaces;

import reika.chromaticraft.magic.CrystalTarget;
import reika.chromaticraft.registry.CrystalElement;
import reika.dragonapi.instantiable.data.immutable.WorldLocation;

import java.util.Collection;

public interface EnergyBeamRenderer {

	Collection<CrystalTarget> getTargets();

	void addTarget(WorldLocation loc, CrystalElement e, double dx, double dy, double dz, double w, double maxW);

	void addSelfTickingTarget(WorldLocation loc, CrystalElement e, double dx, double dy, double dz, double w, double maxW, int ticks);

	void removeTarget(WorldLocation loc, CrystalElement e);

	void clearTargets(boolean unload);

	double getOutgoingBeamRadius();

}
