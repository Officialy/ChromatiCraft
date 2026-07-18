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

import reika.chromaticraft.registry.CrystalElement;

public interface ElementEffector {

	public Collection<CrystalElement> getCurrentElements();

	public int getElement(CrystalElement e);

}
