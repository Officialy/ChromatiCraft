/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.api.interfaces;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;

/** Implement this to make the Mineral Extractor care about and mine your block. */
public interface MinerBlock {

	/** Whether or not to target the block. Args: Block metadata */
    boolean isMineable(int meta);

	boolean allowSilkTouch(int meta);

	/** What items the block will provide when harvested. Normally returns getDrops(world, x, y, z, meta, fortune). */
    ArrayList<ItemStack> getHarvestItems(Level world, int x, int y, int z, int meta, int fortune);

	/** For when the mineral extractor is tuned to only mine specific categories. */
    MineralCategory getCategory();

	/** What block is placed down when your block is mined. */
    Block getReplacedBlock(Level world, int x, int y, int z);

	enum MineralCategory {
		UBIQUITOUS_ORE(),
		COMMON_ORE(),
		UNCOMMON_ORE(),
		RARE_ORE(),
		MISC_UNDERGROUND(),
		MISC_UNDERGROUND_VALUABLE()
    }

}
