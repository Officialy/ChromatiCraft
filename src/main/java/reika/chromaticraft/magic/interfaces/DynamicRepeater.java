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


public interface DynamicRepeater extends CrystalRepeater {

	public int getModifiedThoughput(int basethru, CrystalSource src, CrystalReceiver r);

}
