package reika.chromaticraft.block.dimension.structure.pistontape;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

import reika.chromaticraft.auxiliary.interfaces.LaserPulseEffect;
import reika.chromaticraft.entity.EntityLaserPulse;
import reika.chromaticraft.entity.EntityPistonSpline;
import reika.chromaticraft.registry.ChromaBlockEntities;
import reika.chromaticraft.registry.ChromaSounds;
import reika.chromaticraft.tileentity.TileEntityPistonTarget;
import reika.dragonapi.instantiable.RGBColorData;

/**
 * V33a {@code BlockPistonTarget}. Metadata 0 is a read head, which turns a filtered Chromatic Beams pulse that
 * arrives along its facing into a spline sent to the current door; metadata 1 is a door's colour receiver.
 * Both are drawn as the {@code piston_target} cube with a full-bright overlay on the two faces of their axis,
 * white for a read head and the expected colour for a receiver ({@link #COLOR}).
 */
public final class BlockPistonTarget26 extends Block implements EntityBlock, LaserPulseEffect {

	public static final IntegerProperty TYPE = IntegerProperty.create("type", 0, 1);
	public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;
	/** The overlay tint: RGB bits 4/2/1, white (7) for a read head. */
	public static final IntegerProperty COLOR = IntegerProperty.create("color", 0, 7);

	public BlockPistonTarget26(BlockBehaviour.Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(TYPE, 0).setValue(FACING, Direction.NORTH).setValue(COLOR, 7));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(TYPE, FACING, COLOR);
	}

	public static int colorBits(RGBColorData c) {
		return (c.red ? 4 : 0) | (c.green ? 2 : 0) | (c.blue ? 1 : 0);
	}

	public static int tint(BlockState state) {
		int c = state.getValue(COLOR);
		return 0xFF000000 | ((c & 4) != 0 ? 0xFF0000 : 0) | ((c & 2) != 0 ? 0x00FF00 : 0) | ((c & 1) != 0 ? 0x0000FF : 0);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new TileEntityPistonTarget(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state,
			BlockEntityType<T> type) {
		if (level.isClientSide() || state.getValue(TYPE) != 1 || type != ChromaBlockEntities.PISTON_TARGET.get())
			return null;
		return (world, pos, current, entity) -> TileEntityPistonTarget.serverTick(world, pos, current,
				(TileEntityPistonTarget)entity);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
			BlockHitResult hit) {
		return InteractionResult.SUCCESS;
	}

	/** V33a {@code onImpact}: a read head fires when the beam arrives along its facing; every pulse stops here. */
	@Override
	public boolean onImpact(Level level, BlockPos pos, EntityLaserPulse pulse) {
		BlockState state = level.getBlockState(pos);
		if (state.getValue(TYPE) == 0 && level.getBlockEntity(pos) instanceof TileEntityPistonTarget te
				&& pulse.direction().isCardinal() && te.facing() == pulse.direction().getCardinal()) {
			var c = pulse.color();
			te.fire(new RGBColorData(c.red(), c.green(), c.blue()));
		}
		return true;
	}

	/** V33a {@code receiveSplineParticle}: only a receiver of the same colour accepts it. */
	public static void receiveSplineParticle(Level level, BlockPos pos, EntityPistonSpline e) {
		if (level.isClientSide() || level.getBlockState(pos).getValue(TYPE) != 1
				|| !(level.getBlockEntity(pos) instanceof TileEntityPistonTarget te))
			return;
		if (te.color().matchColor(e.color()))
			te.receive();
		else
			ChromaSounds.ERROR.playSoundAtBlock(te);
	}

	@Override
	protected boolean triggerEvent(BlockState state, Level level, BlockPos pos, int id, int param) {
		super.triggerEvent(state, level, pos, id, param);
		BlockEntity tile = level.getBlockEntity(pos);
		return tile != null && tile.triggerEvent(id, param);
	}
}
