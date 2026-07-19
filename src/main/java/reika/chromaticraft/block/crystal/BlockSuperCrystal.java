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
 * Super crystal (large-range, high-level effect crystal). Ported core. Deferred (unported content):
 * the ChromaOptions NOISE toggle (config unported), the base-block (obsidian) ISBRH render hook, and
 * the drops (loot dropSelf). Light 15 + the SUPER 1.5x enchant boost are set at registration / in
 * CrystalBlock.getEnchantPowerBonus.
 */
public class BlockSuperCrystal extends CrystalBlock {

	public BlockSuperCrystal(BlockBehaviour.Properties props) {
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
		return true;
	}

	@Override
	public int getRange() {
		return 12;
	}

	@Override
	public int getDuration(CrystalElement e) {
		return 6000;
	}

	@Override
	public int getPotionLevel(CrystalElement e) {
		return 2;
	}

	@Override
	public boolean renderBase() {
		return true;
	}
}
