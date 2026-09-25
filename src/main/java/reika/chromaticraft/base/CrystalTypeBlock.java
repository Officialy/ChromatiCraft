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

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

import reika.chromaticraft.auxiliary.CrystalMusicManager;
import reika.chromaticraft.registry.ChromaSounds;
import reika.chromaticraft.registry.CrystalElement;
import reika.dragonapi.libraries.java.ReikaRandomHelper;

/**
 * V33a {@code CrystalTypeBlock}: the base of the sixteen-colour crystal building blocks (Crystal Glass, Glow,
 * the coloured altars). V33a kept the colour in metadata; each colour is its own block here, carrying its
 * {@link CrystalElement}.
 *
 * <p>The block chimes -- a note from its element's chord -- when placed, broken, walked on or struck. V33a's
 * step sound is stone at zero volume, so walking makes only that chime ({@link #SILENT_STONE}). V33a's
 * per-position light ({@code getBrightness}) is a blockstate light level in 26.2, set on the properties.
 */
public abstract class CrystalTypeBlock extends Block {

	/** V33a {@code new SoundType("stone", 0, 0)}. */
	public static final SoundType SILENT_STONE = new SoundType(0F, 0F, SoundEvents.STONE_BREAK, SoundEvents.STONE_STEP,
			SoundEvents.STONE_PLACE, SoundEvents.STONE_HIT, SoundEvents.STONE_FALL);

	private final CrystalElement element;

	protected CrystalTypeBlock(BlockBehaviour.Properties props, CrystalElement element) {
		super(props);
		this.element = element;
	}

	public final CrystalElement getCrystalElement() {
		return element;
	}

	public final CrystalElement getCrystalElement(BlockState state) {
		return element;
	}

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
		super.onPlace(state, level, pos, oldState, movedByPiston);
		if (!oldState.is(this))
			ding(level, pos, element);
	}

	@Override
	protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
		super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
		ding(level, pos, element);
		this.onBroken(level, pos);
	}

	protected void onBroken(ServerLevel level, BlockPos pos) {

	}

	@Override
	public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
		super.stepOn(level, pos, state, entity);
		ding(level, pos, element);
	}

	@Override
	protected void attack(BlockState state, Level level, BlockPos pos, Player player) {
		super.attack(state, level, pos, player);
		ding(level, pos, element);
	}

	/** The chime, from the server so every nearby player hears it once. */
	public static void ding(Level level, BlockPos pos, CrystalElement e) {
		ding(level, pos, e, getRandomPitch(e));
	}

	public static void ding(Level level, BlockPos pos, CrystalElement e, float pitch) {
		if (!level.isClientSide())
			ChromaSounds.DING.playSoundAtBlock(level, pos, (float)ReikaRandomHelper.getRandomPlusMinus(1, 0.2), pitch);
	}

	/** Generates a major or minor chord. */
	private static float getRandomPitch(CrystalElement e) {
		return CrystalMusicManager.instance.getRandomScaledDing(e);
	}

}
