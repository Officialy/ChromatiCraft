/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.registry.CrystalElement;

/**
 * Crystal rune — the coloured runes placed in ChromatiCraft's casting-table and network multiblocks.
 * The 1.7.10 original was a 16-metadata {@code BlockDyeTypes} keyed by {@link CrystalElement}; here that
 * becomes one block with the {@link #COLOR} property (0..15) and 16 distinct BlockItems (the
 * one-block-many-items pattern crystalline stone / {@code BlockPylonStructure} uses). hardness 3 /
 * resistance 12 as in the original.
 *
 * <p><b>Deferred</b> (re-add with the crystal-network + casting-table + structure-protection systems —
 * all reference unported content): the {@code SemiUnbreakable}/{@code getDestroyProgress} structure
 * protection (via {@code MultiBlockChromaTile}), the {@code onBlockPlacedBy} owner assignment
 * ({@code OwnedTile}), the {@code breakBlock} structure invalidation, the {@code CrystalSource}/network
 * behaviour, the casting-table right-click interaction ({@code TileEntityCastingTable}), and the rune
 * particle FX ({@code EntityRuneFX}). The block is a complete, structure-matchable coloured block now;
 * its active behaviour lights up as those systems port.
 */
public class BlockCrystalRune extends Block {

	public static final IntegerProperty COLOR = IntegerProperty.create("color", 0, CrystalElement.elements.length - 1);

	public BlockCrystalRune(BlockBehaviour.Properties props) {
		super(props);
		this.registerDefaultState(this.stateDefinition.any().setValue(COLOR, CrystalElement.WHITE.ordinal()));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(COLOR);
	}

	public static CrystalElement getColor(BlockState state) {
		return CrystalElement.elements[state.getValue(COLOR)];
	}

	@Override
	protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
		return new ItemStack(ChromaBlocks.RUNE_ITEMS.get(state.getValue(COLOR)).get());
	}
}
