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

import reika.chromaticraft.magic.network.CrystalLink;

public interface LinkWatchingRepeater extends CrystalRepeater {

	public void onLinkRecalculated(CrystalLink l);

}
