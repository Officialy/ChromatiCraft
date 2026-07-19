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

import net.minecraft.world.level.block.state.BlockBehaviour;

import reika.chromaticraft.base.CrystalBlock;
import reika.chromaticraft.registry.CrystalElement;

/**
 * The worldgen cave crystal. Ported core: the per-element effect behaviour (gives potion effects,
 * 16 colours via the COLOR property) + light. Deferred (reference unported content — re-add as they
 * port): the SHARD/crystal-powder drops + DimensionTuningManager tuning (26.2 loot-table territory),
 * the ProgressionTrigger (ProgressStage.CRYSTALS) and MinerBlock (MineralCategory) integrations, and
 * the RUNE/STRUCTSHIELD "dark structure" light dimming. Loot is a drops-self placeholder until the
 * SHARD item ports.
 */
public class BlockCaveCrystal extends CrystalBlock {

	public BlockCaveCrystal(BlockBehaviour.Properties props) {
		super(props);
	}

	@Override
	public boolean shouldMakeNoise() {
		return true;
	}

	@Override
	public boolean shouldGiveEffects(CrystalElement e) {
		return true;
	}

	@Override
	public boolean performEffect(CrystalElement e) {
		return e == CrystalElement.BROWN || e == CrystalElement.BLUE ? rand.nextInt(4) == 0 : true;
	}

	@Override
	public int getRange() {
		return 4;
	}

	@Override
	public int getDuration(CrystalElement e) {
		return e == CrystalElement.BROWN ? 5 : 200;
	}

	@Override
	public int getPotionLevel(CrystalElement e) {
		return 0;
	}

	@Override
	public boolean renderBase() {
		return false;
	}
}
