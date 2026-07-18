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

import java.util.ArrayList;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.client.MinecraftForgeClient;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.base.tileentity.TileEntityChromaticBase;
import reika.dragonapi.base.DragonAPIMod;
import reika.dragonapi.base.TileEntityBase;
import reika.dragonapi.base.TileEntityRenderBase;
import reika.dragonapi.instantiable.rendering.StructureRenderer;
import reika.dragonapi.interfaces.TextureFetcher;

public abstract class ChromaRenderBase extends TileEntityRenderBase implements TextureFetcher {

	@Override
	public final String getTextureFolder() {
		return "/Reika/ChromatiCraft/Textures/TileEntity/";
	}

	@Override
	protected Class getModClass() {
		return ChromatiCraft.class;
	}

	protected final void renderModel(TileEntity tile, ChromaModelBase model, Object... args) {
		this.preRenderModel();
		if (args.length > 0) {
			ArrayList li = new ArrayList();
			for (int i = 0; i < args.length; i++)
				li.add(args[i]);
			model.renderAll(tile, li);
		}
		else {
			model.renderAll(tile, null);
		}
		this.postRenderModel(tile);
	}

	protected void postRenderModel(TileEntity tile) {
		if (tile.hasWorldObj())
			GL11.glDisable(GL12.GL_RESCALE_NORMAL);
		GL11.glPopMatrix();
	}

	protected void preRenderModel() {
		GL11.glPushMatrix();
		GL11.glScalef(1.0F, -1.0F, -1.0F);
		GL11.glTranslatef(0.5F, -1.5F, -0.5F);
		GL11.glEnable(GL12.GL_RESCALE_NORMAL);
	}

	protected final void renderModel(TileEntityChromaticBase tile, ChromaModelBase model, Object... args) {
		this.renderModel(tile, model, this.getTextureFolder()+this.getImageFileName(tile), args);
	}

	protected final void renderModel(TileEntityChromaticBase tile, ChromaModelBase model, String tex, Object... args) {
		if (!tile.renderModelsInPass1() && (MinecraftForgeClient.getRenderPass() != 0 && !StructureRenderer.isRenderingTiles()) && tile.isInWorld())
			return;
		this.bindTextureByName(tex);
		GL11.glPushMatrix();
		GL11.glScalef(1.0F, -1.0F, -1.0F);
		GL11.glTranslatef(0.5F, -1.5F, -0.5F);
		GL11.glEnable(GL12.GL_RESCALE_NORMAL);
		if (args.length > 0) {
			ArrayList li = new ArrayList();
			for (int i = 0; i < args.length; i++)
				li.add(args[i]);
			model.renderAll(tile, li);
		}
		else {
			model.renderAll(tile, null);
		}
		if (tile.isInWorld())
			GL11.glDisable(GL12.GL_RESCALE_NORMAL);
		GL11.glPopMatrix();
	}

	@Override
	protected final DragonAPIMod getOwnerMod() {
		return ChromatiCraft.instance;
	}

	@Override
	protected final boolean doRenderModel(TileEntityBase te) {
		return this.isValidMachineRenderPass(te);
	}

}
