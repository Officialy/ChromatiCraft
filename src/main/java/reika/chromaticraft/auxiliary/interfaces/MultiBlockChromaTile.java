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

import reika.chromaticraft.registry.ChromaStructures;
import reika.dragonapi.instantiable.data.immutable.Coordinate;

public interface MultiBlockChromaTile {

	public void validateStructure();

	public ChromaStructures getPrimaryStructure();

	public Coordinate getStructureOffset();

	public boolean canStructureBeInspected();

	public boolean hasStructure();

}
