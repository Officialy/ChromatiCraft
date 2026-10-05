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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import reika.chromaticraft.api.interfaces.CustomRangeUpgrade;
import reika.chromaticraft.api.interfaces.CustomRangeUpgrade.RangeUpgradeable;
import reika.chromaticraft.base.tileentity.TileEntityAdjacencyUpgrade;
import reika.chromaticraft.registry.ChromaBlockEntities;
import reika.chromaticraft.registry.CrystalElement;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * V33a {@code TileEntityRangeBoost}, the lime Range Expander. Machines with a range read its tier through their own
 * adjacency lookup and multiply their range by {@link #getFactor}.
 * <p>
 * Its own per-tick push is dormant, exactly as in V33a: {@code specialInteractions} is keyed by tile class but
 * {@link #tickDirection} looks it up with the tile instance, so the lookup never matches and the push never runs.
 * Keying the lookup by class would call {@code upgradeRange} every tick on machines that already apply the upgrade
 * themselves, applying it twice.
 */
public class TileEntityRangeBoost extends TileEntityAdjacencyUpgrade {

	private static final double[] factors = {
			1.03125,
			1.0625,
			1.125,
			1.25,
			1.5,
			2,
			3,
			4
	};

	private static final Map<Object, RangeEffect> specialInteractions = new ConcurrentHashMap<>();

	public static final RangeEffect basicRangeUpgradeable = new RangeEffect() {

		@Override
		public String getDescription() {
			return "Expands range";
		}

		@Override
		protected void upgradeRange(BlockEntity te, double r) {
			((RangeUpgradeable)te).upgradeRange(r);
		}

	};

	public TileEntityRangeBoost(BlockPos pos, BlockState state) {
		super(ChromaBlockEntities.ADJACENCY.get(), pos, state);
	}

	public static double getFactor(int tier) {
		return factors[tier];
	}

	public static void customizeTile(Class<?> c, CustomRangeUpgrade h) {
		specialInteractions.put(c, new CustomRangeEffect(h));
	}

	public static void addBasicHandling(Class<? extends RangeUpgradeable> c, List<Supplier<ItemStack>> items) {
		specialInteractions.put(c, basicRangeUpgradeable);
		for (Supplier<ItemStack> is : items) {
			AdjacencyEffectDescription adj = TileEntityAdjacencyUpgrade.registerEffectDescription(CrystalElement.LIME, basicRangeUpgradeable.getDescription());
			adj.addItems(items);
		}
	}

	public static void addBasicHandling(Class<? extends RangeUpgradeable> c, ItemStack... items) {
		List<Supplier<ItemStack>> li = new ArrayList<>();
		for (ItemStack is : items)
			li.add(() -> is);
		addBasicHandling(c, li);
	}

	@Override
	protected EffectResult tickDirection(Level world, BlockPos pos, Direction dir, long startTime) {
		BlockEntity te = this.getEffectiveTileOnSide(dir);
		RangeEffect c = te != null ? specialInteractions.get(te) : null;
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
	protected void animateWithTick(Level world, BlockPos pos) {

	}

	@Override
	public int getRedstoneOverride() {
		return 0;
	}

	private static class CustomRangeEffect extends RangeEffect {

		private final CustomRangeUpgrade effect;

		protected CustomRangeEffect(CustomRangeUpgrade e) {
			super();
			effect = e;

			AdjacencyEffectDescription adj = TileEntityAdjacencyUpgrade.registerEffectDescription(CrystalElement.LIME, this.getDescription());
			adj.addItems(effect.getItems().toArray(new ItemStack[0]));
		}

		@Override
		public String getDescription() {
			return effect.getDescription();
		}

		@Override
		protected void upgradeRange(BlockEntity te, double r) {
			effect.upgradeRange(te, r);
		}

	}

	public static abstract class RangeEffect {

		protected RangeEffect() {

		}

		public abstract String getDescription();

		protected abstract void upgradeRange(BlockEntity te, double r);

	}

}
