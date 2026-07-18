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

import reika.chromaticraft.registry.CrystalElement;
import reika.dragonapi.instantiable.data.immutable.DecimalPosition;

public interface EnergyBeamReceiver {

	public DecimalPosition getTargetRenderOffset(CrystalElement e);

	public double getIncomingBeamRadius();

}
