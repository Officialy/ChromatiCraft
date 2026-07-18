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

import reika.chromaticraft.magic.ElementTagCompound;

@Deprecated //?
public interface AuraSource {

	public ElementTagCompound getAuras();

	public double getDistancePower();

	public double getCoefficient();

}
