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

import java.util.HashMap;

import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import reika.chromaticraft.api.interfaces.CustomRangeUpgrade;
import reika.chromaticraft.api.interfaces.customrangeupgrade.RangeUpgradeable;
import reika.chromaticraft.base.tileentity.TileEntityAdjacencyUpgrade;
import reika.chromaticraft.registry.CrystalElement;


public class TileEntityRangeBoost extends TileEntityAdjacencyUpgrade {

	private static double[] factors = {
			1.03125,
			1.0625,
			1.125,
			1.25,
			1.5,
			2,
			3,
			4
	};

	private static final HashMap<Class, RangeEffect> specialInteractions = new HashMap();

	public static final RangeEffect basicRangeUpgradeable = new RangeEffect() {

		@Override
		public String getDescription() {
			return "Expands range";
		}

		@Override
		protected void upgradeRange(TileEntity te, double r) {
			((RangeUpgradeable)te).upgradeRange(r);
		}

	};

	public static double getFactor(int tier) {
		return factors[tier];
	}

	public static void customizeTile(Class c, CustomRangeUpgrade h) {
		specialInteractions.put(c, new CustomRangeEffect(h));
	}

	public static void addBasicHandling(Class<? extends RangeUpgradeable> c, ItemStack... items) {
		specialInteractions.put(c, basicRangeUpgradeable);
		for (ItemStack is : items) {
			AdjacencyEffectDescription adj = TileEntityAdjacencyUpgrade.registerEffectDescription(CrystalElement.LIME, basicRangeUpgradeable.getDescription());
			adj.addItems(items);
		}
	}

	@Override
	protected EffectResult tickDirection(World world, int x, int y, int z, ForgeDirection dir, long startTime) {
		TileEntity te = this.getEffectiveTileOnSide(dir);
		RangeEffect c = specialInteractions.get(te);
		if (c != null) {
			c.upgradeRange(te, this.getRangeFactor());
			return EffectResult.ACTION;
		}
		return EffectResult.CONTINUE;
	}

	public double getRangeFactor() {
		return factors[this.getTier()];
	}

	@Override
	public CrystalElement getColor() {
		return CrystalElement.LIME;
	}

	@Override
	protected void animateWithTick(World world, int x, int y, int z) {

	}

	private static class CustomRangeEffect extends RangeEffect {

		private CustomRangeUpgrade effect;

		protected CustomRangeEffect(CustomRangeUpgrade e) {
			super();
			effect = e;

			AdjacencyEffectDescription adj = TileEntityAdjacencyUpgrade.registerEffectDescription(CrystalElement.LIME, this.getDescription());
			adj.addItems(effect.getItems());
		}

		@Override
		public String getDescription() {
			return effect.getDescription();
		}

		@Override
		protected void upgradeRange(TileEntity te, double r) {
			effect.upgradeRange(te, r);
		}

	}

	public static abstract class RangeEffect {

		protected RangeEffect() {

		}

		public abstract String getDescription();

		protected abstract void upgradeRange(TileEntity te, double r);

	}

}
