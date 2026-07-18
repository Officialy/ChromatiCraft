/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.render.isbrh;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.world.IBlockAccess;

import reika.chromaticraft.block.worldgen.BlockDecoFlower;
import reika.chromaticraft.block.worldgen.blockdecoflower.Flowers;
import reika.dragonapi.base.ISBRH;


public class DecoFlowerRenderer extends ISBRH {

	public DecoFlowerRenderer(int id) {
		super(id);
	}

	@Override
	public boolean renderWorldBlock(IBlockAccess world, int x, int y, int z, Block block, int modelId, RenderBlocks renderer) {
		BlockDecoFlower b = (BlockDecoFlower)block;
		Flowers f = Flowers.list[world.getBlockMetadata(x, y, z)];
		Tessellator.instance.setColorOpaque_I(0xffffff);
		f.render(world, x, y, z, block, renderer, Tessellator.instance);
		return true;
	}

	@Override
	public boolean shouldRender3DInInventory(int modelId) {
		return false;
	}

}
