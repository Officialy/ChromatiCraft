package reika.chromaticraft.block.dimension.structure.pistontape;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import reika.chromaticraft.auxiliary.interfaces.LaserPulseEffect;
import reika.chromaticraft.entity.EntityLaserPulse;
import reika.chromaticraft.world.dimension.structure.laser.LaserPulseLogic.BeamColor;
import reika.dragonapi.instantiable.RGBColorData;

/**
 * V33a {@code BlockPistonTapeBit}: one cell of Filter Cycle's tape. Its metadata is kept as {@link #BIT}: an even
 * value is a clear (white) cell, an odd value a filter whose colour is {@code meta/2} (0 yellow, 1 cyan, 2
 * magenta); 8 is the error colour. A plain click toggles the filter on and off, a sneaking click steps its
 * colour. It carries no tile, so pistons move it and its colour together, and only players collide with it.
 * A Chromatic Beams pulse that crosses it is filtered to the intersection and continues.
 */
public final class BlockPistonTapeBit26 extends Block implements LaserPulseEffect {

	public static final IntegerProperty BIT = IntegerProperty.create("bit", 0, 8);

	public BlockPistonTapeBit26(BlockBehaviour.Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(BIT, 0));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(BIT);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
			BlockHitResult hit) {
		int meta = state.getValue(BIT);
		if (player.isShiftKeyDown()) {
			meta = (meta + 2) | 1;
			if (meta >= 7)
				meta = 1;
		}
		else {
			meta ^= 1;
		}
		if (!level.isClientSide()) {
			level.setBlock(pos, state.setValue(BIT, Math.min(meta, 8)), Block.UPDATE_ALL);
			level.playSound(null, pos, SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS, 1,
					meta % 2 == 1 ? 0.875F : 0.75F);
		}
		return InteractionResult.SUCCESS;
	}

	/**
	 * V33a hardness 0 with {@code SemiUnbreakable.isUnbreakable} true: pistons may move it (vanilla refuses blocks of
	 * destroy speed -1), but a player can never mine it.
	 */
	@Override
	protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
		return 0;
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return context instanceof EntityCollisionContext entityContext && !(entityContext.getEntity() instanceof Player)
				? Shapes.empty() : Shapes.block();
	}

	/** V33a {@code onImpact}: the pulse keeps going, filtered to this cell's colour. */
	@Override
	public boolean onImpact(Level level, BlockPos pos, EntityLaserPulse pulse) {
		RGBColorData c = getColor(level.getBlockState(pos).getValue(BIT));
		pulse.setColor(pulse.color().intersect(new BeamColor(c.red, c.green, c.blue)));
		return false;
	}

	/** V33a {@code getColor(meta)}. */
	public static RGBColorData getColor(int meta) {
		if (meta < 6 && meta % 2 == 0)
			return new RGBColorData(true, true, true);
		boolean r = true, g = true, b = true;
		switch (meta / 2) {
			case 0 -> b = false;
			case 1 -> r = false;
			case 2 -> g = false;
			default -> {
				return RGBColorData.black();
			}
		}
		return new RGBColorData(r, g, b);
	}

	/** V33a {@code getMetaFor}. */
	public static int getMetaFor(RGBColorData c, boolean active) {
		if (c == null || c.isWhite())
			return 0;
		if (c.isPrimary() || c.isBlack())
			return 8;
		int base = 0;
		if (c.red && c.green)
			base = 0;
		else if (c.green && c.blue)
			base = 2;
		else if (c.red && c.blue)
			base = 4;
		return active ? base + 1 : base;
	}

	/** V33a {@code colorMultiplier}. */
	public static int tint(BlockState state) {
		return 0xFF000000 | getColor(state.getValue(BIT)).getRenderColor();
	}
}
