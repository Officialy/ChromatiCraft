/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.api;

import java.util.Locale;

import reika.chromaticraft.registry.CrystalElement;

public class CrystalElementAccessor {

	/** An interface for the internal CrystalElement enum, to which (or just {@link Enum}) you can cast it; consult that enum for thematic meanings. */
	public static interface CrystalElementProxy {

		public String displayName();
		public int getColor();
		public int ordinal();

	}

	public static CrystalElementProxy getByEnum(String name) {
		return CrystalElement.valueOf(name.toUpperCase(Locale.ROOT));
	}

	public static CrystalElementProxy getByIndex(int idx) {
		return CrystalElement.elements[idx];
	}

}
