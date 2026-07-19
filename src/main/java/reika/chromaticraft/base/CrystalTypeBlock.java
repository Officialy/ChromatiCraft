/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.base;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

import reika.chromaticraft.registry.CrystalElement;

/**
 * Base for the 16-colour crystal blocks. The 1.7.10 int metadata (0-15 = {@link CrystalElement}
 * ordinal) becomes the {@link #COLOR} blockstate property — this is the metadata→blockstate template
 * for the mod. Port deferrals (cosmetic / framework not yet ported): the "ding" note on
 * add/break/walk/click routed through {@code ChromaSounds}+{@code CrystalMusicManager} (sound
 * framework unported), and the per-position / COLORLIGHT glow (26.2 light is per-blockstate — the
 * concrete block sets {@code Properties.lightLevel}). The colour is set by worldgen / the concrete
 * block, not carried on a placement item, so the legacy {@code damageDropped} is not needed.
 */
public abstract class CrystalTypeBlock extends Block {

	public static final IntegerProperty COLOR = IntegerProperty.create("color", 0, CrystalElement.elements.length - 1);

	protected CrystalTypeBlock(BlockBehaviour.Properties props) {
		super(props);
		this.registerDefaultState(this.stateDefinition.any().setValue(COLOR, 0));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(COLOR);
	}

	public final CrystalElement getCrystalElement(BlockState state) {
		return CrystalElement.elements[state.getValue(COLOR)];
	}
}
