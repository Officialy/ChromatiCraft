/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.block.crystal;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

import reika.chromaticraft.base.CrystalTypeBlock;
import reika.chromaticraft.registry.CrystalElement;

/**
 * V33a {@code BlockCrystalGlass}: glowing (light 12) glass in an element's colour, with a bright white overlay
 * over every face. The block model draws both, as V33a's {@code CrystalGlassRenderer} did: the glass tinted
 * with the element colour on the translucent pass, and the overlay untinted at full brightness just outside
 * each face.
 *
 * <p>V33a's glass was one block for all sixteen colours, and a face was hidden against any other crystal
 * glass, whatever its colour; so it is here.
 */
public class BlockCrystalGlass extends CrystalTypeBlock {

	public BlockCrystalGlass(BlockBehaviour.Properties props, CrystalElement element) {
		super(props, element);
	}

	@Override
	protected boolean skipRendering(BlockState state, BlockState neighbor, Direction direction) {
		return neighbor.getBlock() instanceof BlockCrystalGlass || super.skipRendering(state, neighbor, direction);
	}

}
