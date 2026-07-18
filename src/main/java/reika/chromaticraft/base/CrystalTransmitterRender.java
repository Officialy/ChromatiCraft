/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.base;

import org.lwjgl.opengl.GL11;

import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.client.MinecraftForgeClient;

import reika.chromaticraft.auxiliary.ChromaFX;
import reika.chromaticraft.base.tileentity.CrystalTransmitterBase;
import reika.dragonapi.instantiable.data.immutable.WorldLocation;
import reika.dragonapi.interfaces.tileentity.RenderFetcher;

public abstract class CrystalTransmitterRender extends ChromaRenderBase {

	@Override
	public String getImageFileName(RenderFetcher te) {
		return "";
	}

	@Override
	public void renderTileEntityAt(TileEntity tile, double par2, double par4, double par6, float par8) {
		if (tile.hasWorldObj() && MinecraftForgeClient.getRenderPass() == 0) {
			CrystalTransmitterBase te = (CrystalTransmitterBase)tile;
			GL11.glPushMatrix();
			GL11.glTranslated(par2, par4, par6);
			ChromaFX.drawEnergyTransferBeams(new WorldLocation(te), te.getOutgoingBeamRadius(), te.getTargets());
			GL11.glPopMatrix();
		}
	}

}
