/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.world.dimension.structure.altar;


import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import reika.chromaticraft.base.DynamicStructurePiece;
import reika.chromaticraft.world.dimension.structure.AltarGenerator;

public class SurfaceAccess extends DynamicStructurePiece<AltarGenerator> {

	public final ForgeDirection direction;

	public SurfaceAccess(AltarGenerator g, ForgeDirection dir) {
		super(g);
		direction = dir;
	}

	@Override
	public void generate(World world, int x, int z) {

	}

}
