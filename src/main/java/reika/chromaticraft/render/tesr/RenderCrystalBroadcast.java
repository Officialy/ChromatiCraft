/*******************************************************************************
 * @author Reika Kalseki
 * 
 * Copyright 2017
 * 
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.render.tesr;

import net.minecraft.tileentity.TileEntity;

import reika.chromaticraft.tileentity.networking.TileEntityCrystalRepeater;

public class RenderCrystalBroadcast extends RenderCrystalRepeater {

	@Override
	public void renderTileEntityAt(TileEntity tile, double par2, double par4, double par6, float par8) {
		super.renderTileEntityAt(tile, par2, par4, par6, par8);
	}

	@Override
	protected int getHaloRenderColor(TileEntityCrystalRepeater te) {
		return super.getHaloRenderColor(te);
	}

}
