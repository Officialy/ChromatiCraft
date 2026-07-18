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

import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import reika.chromaticraft.base.tileentity.TileEntityAdjacencyUpgrade;
import reika.chromaticraft.magic.interfaces.LumenConsumer;
import reika.chromaticraft.registry.AdjacencyUpgrades;
import reika.chromaticraft.registry.ChromaOptions;
import reika.chromaticraft.registry.ChromaTiles;
import reika.chromaticraft.registry.CrystalElement;


public class TileEntityEfficiencyUpgrade extends TileEntityAdjacencyUpgrade {

	private static double[] factor = {
			0.9375,
			0.875,
			0.75,
			0.625,
			0.5,
			0.25,
			0.125,
			0.0625,
	};
	;

	private static void initHandlers() {
		String desc = "Improves lumen efficiency usage";
		AdjacencyEffectDescription adj = TileEntityAdjacencyUpgrade.registerEffectDescription(CrystalElement.BLACK, desc);
		for (int i = 0; i < ChromaTiles.TEList.length; i++) {
			ChromaTiles r = ChromaTiles.TEList[i];
			if (r == ChromaTiles.ADJACENCY || r.isDummiedOut() || r.isIncomplete())
				continue;
			if (LumenConsumer.class.isAssignableFrom(r.getTEClass()) && !r.isRepeater()) {
				LumenConsumer lc = (LumenConsumer)r.createTEInstanceForRender(0);
				if (lc.allowsEfficiencyBoost()) {
					adj.addItems(r.getCraftedProduct());
				}
			}
		}
		if (ChromaOptions.POWEREDACCEL.getState()) {
			for (int i = 0; i < 16; i++) {
				if (AdjacencyUpgrades.upgrades[i].isImplemented())
					adj.addItems(AdjacencyUpgrades.upgrades[i].getStackOfTier(2));
			}
		}
	}

	@Override
	protected EffectResult tickDirection(World world, int x, int y, int z, ForgeDirection dir, long startTime) {
		return EffectResult.STOP;
	}

	@Override
	public CrystalElement getColor() {
		return CrystalElement.BLACK;
	}

	@Override
	protected void animateWithTick(World world, int x, int y, int z) {

	}

	public static double getCostFactor(int tier) {
		return factor[tier];
	}

}
