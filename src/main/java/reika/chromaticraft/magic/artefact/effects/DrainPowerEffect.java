/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.magic.artefact.effects;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import reika.chromaticraft.magic.artefact.UABombingEffect;
import reika.dragonapi.libraries.java.ReikaRandomHelper;

/**
 * V33a {@code DrainPowerEffect}: six random blocks within 8 (4 vertically) have up to five million energy
 * drained. V33a drained CoFH RF storages; 26.2's shared energy capability is their successor.
 */
public class DrainPowerEffect extends UABombingEffect.BlockEffect {

	private static final int LOCATIONS = 6;
	private static final int RANGE = 8;
	private static final int RANGE_Y = 4;

	@Override
	public void trigger(Container inv, BlockEntity te) {
		BlockPos c = te.getBlockPos();
		for (int i = 0; i < LOCATIONS; i++) {
			BlockPos pos = new BlockPos(ReikaRandomHelper.getRandomPlusMinus(c.getX(), RANGE),
					ReikaRandomHelper.getRandomPlusMinus(c.getY(), RANGE_Y), ReikaRandomHelper.getRandomPlusMinus(c.getZ(), RANGE));
			var handler = te.getLevel().getCapability(Capabilities.Energy.BLOCK, pos, null);
			if (handler != null) {
				try (Transaction tx = Transaction.openRoot()) {
					handler.extract(5000000, tx);
					tx.commit();
				}
			}
		}
	}

}
