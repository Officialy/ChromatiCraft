/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.tileentity.aoe.effect;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import reika.chromaticraft.base.tileentity.TileEntityAdjacencyUpgrade;
import reika.chromaticraft.magic.interfaces.LumenConsumer;
import reika.chromaticraft.registry.AdjacencyUpgrades;
import reika.chromaticraft.registry.ChromaBlockEntities;
import reika.chromaticraft.registry.ChromaOptions;
import reika.chromaticraft.registry.ChromaTiles;
import reika.chromaticraft.registry.CrystalElement;

/**
 * V33a {@code TileEntityEfficiencyUpgrade}, the black Efficiency Augment. It does nothing itself; lumen consumers on
 * any of its faces read its tier and scale their costs by {@link #getCostFactor}.
 */
public class TileEntityEfficiencyUpgrade extends TileEntityAdjacencyUpgrade {

	private static final double[] factor = {
			0.9375,
			0.875,
			0.75,
			0.625,
			0.5,
			0.25,
			0.125,
			0.0625,
	};

	public TileEntityEfficiencyUpgrade(BlockPos pos, BlockState state) {
		super(ChromaBlockEntities.ADJACENCY.get(), pos, state);
	}

	/**
	 * V33a {@code initHandlers}: the lexicon lists every non-repeater lumen consumer that accepts the boost, and with
	 * powered upgrades on, every implemented upgrade too (they drain lumens).
	 */
	public static void initHandlers() {
		String desc = "Improves lumen efficiency usage";
		AdjacencyEffectDescription adj = TileEntityAdjacencyUpgrade.registerEffectDescription(CrystalElement.BLACK, desc);
		for (int i = 0; i < ChromaTiles.TEList.length; i++) {
			ChromaTiles r = ChromaTiles.TEList[i];
			if (r == ChromaTiles.ADJACENCY)
				continue;
			if (LumenConsumer.class.isAssignableFrom(r.getTEClass()) && !r.isRepeater()) {
				BlockEntity lc = r.createBlockEntity(BlockPos.ZERO, r.getBlockState());
				if (((LumenConsumer)lc).allowsEfficiencyBoost()) {
					adj.addItems(java.util.List.of(r::getCraftedProduct));
				}
			}
		}
		if (ChromaOptions.POWEREDACCEL.getState()) {
			for (int i = 0; i < 16; i++) {
				AdjacencyUpgrades a = AdjacencyUpgrades.upgrades[i];
				if (a.isImplemented())
					adj.addItems(java.util.List.of(() -> a.getStackOfTier(2)));
			}
		}
	}

	@Override
	protected EffectResult tickDirection(Level world, BlockPos pos, Direction dir, long startTime) {
		return EffectResult.STOP;
	}

	@Override
	public CrystalElement getColor() {
		return CrystalElement.BLACK;
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {

	}

	@Override
	public int getRedstoneOverride() {
		return 0;
	}

	public static double getCostFactor(int tier) {
		return factor[tier];
	}

}
