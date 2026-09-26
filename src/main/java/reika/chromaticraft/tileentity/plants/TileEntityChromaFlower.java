/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.tileentity.plants;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import reika.chromaticraft.base.tileentity.TileEntityChromaticBase;
import reika.chromaticraft.registry.ChromaBlockEntities;
import reika.chromaticraft.registry.ChromaTiles;

/**
 * V33a {@code TileEntityChromaFlower}: a purely decorative flower whose three petal layers cycle through the hues, each
 * layer a little ahead of or behind the first. The hues are per-flower random, as V33a's were (unsaved, so a reload
 * reshuffles them).
 *
 * <p>V33a also advanced the base hue from {@code animateWithTick} for the inventory render tile (not in the world);
 * the item form is now drawn by {@code ChromaFlowerItemRenderer}, which takes its hue from the clock instead.
 */
public class TileEntityChromaFlower extends TileEntityChromaticBase {

	private int hueBaseOffset = rand.nextInt(360);
	private int hueOffset = 10+rand.nextInt(21);

	public TileEntityChromaFlower(BlockPos pos, BlockState state) {
		super(ChromaBlockEntities.CHROMA_FLOWER.get(), pos, state);
	}

	@Override
	public ChromaTiles getTile() {
		return ChromaTiles.CHROMAFLOWER;
	}

	@Override
	public void updateEntity(Level world, BlockPos pos) {

	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {

	}

	public int getHue1() {
		return (hueBaseOffset+this.getTicksExisted())%360;
	}

	public int getHue2() {
		return (hueBaseOffset+this.getTicksExisted()+hueOffset)%360;
	}

	public int getHue3() {
		return Math.floorMod(hueBaseOffset+this.getTicksExisted()-hueOffset, 360);
	}

}
