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
 * Crystal lamp (decorative full-bright crystal, no effects). Ported core. Deferred (unported content):
 * the structure-shield unbreakable/base logic (MUSICTRIGGER/STRUCTSHIELD blocks), the ChromaOptions
 * NOISE toggle (config unported — noise always on for now), and the drops (loot dropSelf).
 */
public class BlockCrystalLamp extends CrystalBlock {

	public BlockCrystalLamp(BlockBehaviour.Properties props) {
		super(props);
	}

	@Override
	public boolean shouldMakeNoise() {
		return true;
	}

	@Override
	public boolean shouldGiveEffects(CrystalElement e) {
		return false;
	}

	@Override
	public boolean performEffect(CrystalElement e) {
		return false;
	}

	@Override
	public int getRange() {
		return 3;
	}

	@Override
	public int getDuration(CrystalElement e) {
		return 200;
	}

	@Override
	public int getPotionLevel(CrystalElement e) {
		return 0;
	}

	@Override
	public boolean renderBase() {
		return true;
	}
}
