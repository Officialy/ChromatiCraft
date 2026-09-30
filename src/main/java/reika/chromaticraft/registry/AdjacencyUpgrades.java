/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.registry;

import java.util.Locale;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import reika.chromaticraft.base.tileentity.TileEntityAdjacencyUpgrade;
import reika.chromaticraft.tileentity.aoe.effect.TileEntityAccelerator;
import reika.chromaticraft.tileentity.aoe.effect.TileEntityEfficiencyUpgrade;
import reika.chromaticraft.tileentity.aoe.effect.TileEntityRangeBoost;
import reika.dragonapi.libraries.java.ReikaStringParser;

/**
 * V33a {@code AdjacencyUpgrades}: the sixteen Adjacency Upgrade colours, their two render colours and their tile. A
 * colour with no tile is not implemented: it has no block, item, recipe or creative entry. In V33a only BLUE was
 * unimplemented; here the other colours join it until each one's effect tile is ported, when its constructor gains the
 * tile factory and {@link #getFactor}/{@link #getDesc} their case.
 */
public enum AdjacencyUpgrades {

	BLACK(0x000000, 0x505050, TileEntityEfficiencyUpgrade::new),
	RED(0xff0000, 0x800000),
	GREEN(0x00C90E, 0x008206),
	BROWN(0xA06139, 0x56341F),
	BLUE(0x0000FF, 0x0065FF),
	PURPLE(0xBE00EA, 0x7400EA),
	CYAN(0x009F8C, 0x009FF0),
	LIGHTGRAY(0x808080, 0xC0C0C0),
	GRAY(0x404040, 0x808080),
	PINK(0xff4040, 0xffcfcf),
	LIME(0x00ff00, 0x00ffff, TileEntityRangeBoost::new),
	YELLOW(0xffff00, 0xffffff),
	LIGHTBLUE(0x0000ff, 0x00ffff, TileEntityAccelerator::new),
	MAGENTA(0xFF61DC, 0xD900DC),
	ORANGE(0xffcf00, 0xff4000),
	WHITE(0xffffff, 0xffffff);

	public final int color1;
	public final int color2;

	private final @Nullable TileFactory tileFactory;

	public static final AdjacencyUpgrades[] upgrades = values();

	private AdjacencyUpgrades(int c1, int c2) {
		this(c1, c2, null);
	}

	private AdjacencyUpgrades(int c1, int c2, @Nullable TileFactory c) {
		tileFactory = c;
		color1 = c1;
		color2 = c2;
	}

	public boolean isImplemented() {
		return tileFactory != null;
	}

	public @Nullable TileEntityAdjacencyUpgrade createTileEntity(BlockPos pos, BlockState state) {
		if (!this.isImplemented())
			return null;
		return tileFactory.create(pos, state);
	}

	public Component getName() {
		return Component.translatable("adjacency."+this.name().toLowerCase(Locale.ROOT));
	}

	public String getDesc(int tier) {
		switch(this) {
			case BLACK:
				return String.format("Reduces lumen consumption by %sx", ReikaStringParser.getAutoDecimal(1-this.getFactor(tier)));
			case BLUE:
				break;
			case LIME:
				return String.format("Increases construct range %sx", ReikaStringParser.getAutoDecimal(this.getFactor(tier)));
			case LIGHTBLUE:
				return String.format("Accelerates time by %dx for TileEntities adjacent to it.", (int)this.getFactor(tier));
			default:
				break;
		}
		return "Does nothing...yet";
	}

	public double getFactor(int tier) {
		switch(this) {
			case BLACK:
				return TileEntityEfficiencyUpgrade.getCostFactor(tier);
			case BLUE:
				break;
			case LIME:
				return TileEntityRangeBoost.getFactor(tier);
			case LIGHTBLUE:
				return 1+TileEntityAccelerator.getAccelFromTier(tier);
			default:
				break;
		}
		return 0;
	}

	/** V33a {@code getStackOfTier}: one of this colour's item, with its tier set. */
	public ItemStack getStackOfTier(int tier) {
		ItemStack is = new ItemStack(ChromaBlocks.adjacencyUpgradeItem(this).get());
		is.set(ChromaDataComponents.ADJACENCY_TIER.get(), tier);
		return is;
	}

	public CrystalElement getColor() {
		return CrystalElement.elements[this.ordinal()];
	}

	@FunctionalInterface
	public interface TileFactory {
		TileEntityAdjacencyUpgrade create(BlockPos pos, BlockState state);
	}

}
