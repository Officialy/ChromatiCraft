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

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import reika.chromaticraft.magic.progression.ProgressStage;

/**
 * A block that grants progression simply by being looked at, scanned each player tick by
 * {@link reika.chromaticraft.auxiliary.ExplorationMonitor}. This is how the mod's opening step is
 * reached: looking at a cave crystal is what unlocks {@link ProgressStage#CRYSTALS}.
 */
public interface ProgressionTrigger {

	//public boolean canTrigger(Player ep, Level world, BlockPos pos);

	public ProgressStage[] getTriggers(Player ep, Level world, BlockPos pos);

}
