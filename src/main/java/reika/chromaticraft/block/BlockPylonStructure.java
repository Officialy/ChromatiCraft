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

/**
 * Crystalline stone — the decorative structural block that makes up ChromatiCraft's pylons and
 * multiblocks. The 1.7.10 original was a single {@code Block} carrying 16 metadata variants
 * ({@link StoneTypes}); here that becomes one block with the {@link #TYPE} {@link IntegerProperty}
 * (0..15) and 16 distinct {@link net.minecraft.world.item.BlockItem}s (one per variant, registered in
 * {@link ChromaBlocks}) — the one-block-many-items convention GeoStrata's rock/lava blocks use, which
 * keeps the same-block-different-value neighbour checks the deferred render/structure code relies on.
 *
 * <p><b>Deferred</b> (re-add when the crystal-network tiles + DragonAPI structure search port — see
 * {@code chromaticraft-port.md}): the {@code BlockProtectedByStructure} base (makes the block
 * unbreakable while part of a live multiblock — every tile it scans for, {@code MultiBlockChromaTile}
 * /{@code CrystalSource}/{@code OwnedTile}, is unported), the {@code onBlockAdded}/{@code breakBlock}
 * structure (re)validation hooks (they call {@code TileEntityCrystalPylon}/{@code CrystalRepeater}/
 * {@code PowerTree}), the explosion-driven pylon invalidation, the connected-texture / directional /
 * bright-pass render (the {@code glows()} variants render fullbright but emit no world light — the
 * original has no {@code setLightValue}, so no {@code lightLevel} here), and the Christmas particles.
 */
public class BlockPylonStructure extends Block {

	public static final IntegerProperty TYPE = IntegerProperty.create("type", 0, StoneTypes.list.length - 1);

	public enum StoneTypes {
		SMOOTH(),
		BEAM(),
		COLUMN(),
		GLOWCOL(),
		GLOWBEAM(),
		FOCUS(),
		CORNER(),
		ENGRAVED(),
		EMBOSSED(),
		FOCUSFRAME(),
		GROOVE1(),
		GROOVE2(),
		BRICKS(),
		MULTICHROMIC(),
		STABILIZER(),
		RESORING();

		public static final StoneTypes[] list = values();

		public boolean needsSilkTouch() {
			return this == GLOWCOL || this == GLOWBEAM || this == FOCUS;
		}

		public boolean isBeam() {
			return this == BEAM || this == GLOWBEAM;
		}

		public boolean isColumn() {
			return this == COLUMN || this == GLOWCOL;
		}

		public boolean glows() {
			return this == GLOWCOL || this == GLOWBEAM || this == FOCUS || this == RESORING || this == STABILIZER || this == MULTICHROMIC;
		}

		/** The variant dropped when mined without silk touch (glow variants degrade to their base form). */
		public StoneTypes getDropVariant() {
			switch (this) {
				case GLOWCOL:
					return COLUMN;
				case GLOWBEAM:
					return BEAM;
				case FOCUS:
					return FOCUSFRAME;
				default:
					return this;
			}
		}

		public StoneTypes getGlowingVariant() {
			switch (this) {
				case BEAM:
					return GLOWBEAM;
				case COLUMN:
					return GLOWCOL;
				case FOCUSFRAME:
					return FOCUS;
				default:
					return null;
			}
		}
	}

	public BlockPylonStructure(BlockBehaviour.Properties props) {
		super(props);
		this.registerDefaultState(this.stateDefinition.any().setValue(TYPE, StoneTypes.SMOOTH.ordinal()));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(TYPE);
	}

	public static StoneTypes getStoneType(BlockState state) {
		return StoneTypes.list[state.getValue(TYPE)];
	}

	@Override
	protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
		return new ItemStack(ChromaBlocks.PYLONSTRUCT_ITEMS.get(state.getValue(TYPE)).get());
	}
}
