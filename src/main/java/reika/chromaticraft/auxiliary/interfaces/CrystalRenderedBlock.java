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

import net.minecraft.world.IBlockAccess;
import net.minecraftforge.common.util.ForgeDirection;

import reika.dragonapi.instantiable.data.immutable.BlockKey;

public interface CrystalRenderedBlock {

	public boolean renderBase();

	public BlockKey getBaseBlock(IBlockAccess iba, int x, int y, int z, ForgeDirection up);

	public boolean renderAllArms();

	public int getTintColor(int meta);

}
