/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.magic.artefact;

import net.minecraft.world.Container;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;

/** V33a {@code ExplodeEffect}: a strength-3, fireless explosion that also removes the container. */
public class ExplodeEffect extends UABombingEffect.BlockEffect {

	@Override
	public void trigger(Container inv, BlockEntity te) {
		var pos = te.getBlockPos();
		te.getLevel().explode(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 3, false, Level.ExplosionInteraction.BLOCK);
		te.getLevel().setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
	}

}
