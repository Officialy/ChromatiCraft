/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.auxiliary.interfaces;

import java.util.Collection;

import reika.dragonapi.instantiable.data.immutable.Coordinate;

public interface FocusAcceleratable {

	public float getAccelerationFactor();

	public float getMaximumAcceleratability();

	public float getProgressToNextStep();

	public void recountFocusCrystals();

	public Collection<Coordinate> getRelativeFocusCrystalLocations();

}
