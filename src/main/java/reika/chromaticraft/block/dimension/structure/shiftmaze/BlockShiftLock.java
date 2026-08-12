package reika.chromaticraft.block.dimension.structure.shiftmaze;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.registry.ChromaSounds;

/** Directionally passable, optionally shield-disguised lock used by the Snow and Shift Maze puzzles. */
public final class BlockShiftLock extends Block {

	public enum Passability implements StringRepresentable {
		CLOSED, OPEN,
		EAST_CLOSED, EAST_OPEN, WEST_CLOSED, WEST_OPEN,
		SOUTH_CLOSED, SOUTH_OPEN, NORTH_CLOSED, NORTH_OPEN,
		CLOSED_HIDDEN, EAST_HIDDEN, WEST_HIDDEN, SOUTH_HIDDEN, NORTH_HIDDEN,
		BREAKABLE;

		@Override public String getSerializedName() { return name().toLowerCase(java.util.Locale.ROOT); }

		public boolean useOpenTexture() {
			return this != CLOSED && this != EAST_CLOSED && this != WEST_CLOSED
					&& this != SOUTH_CLOSED && this != NORTH_CLOSED && this != CLOSED_HIDDEN;
		}

		public boolean isPassable(Direction side) {
			return switch (this) {
				case OPEN -> true;
				case EAST_HIDDEN, WEST_HIDDEN -> side.getAxis() == Direction.Axis.X;
				case NORTH_HIDDEN, SOUTH_HIDDEN -> side.getAxis() == Direction.Axis.Z;
				case EAST_OPEN -> side == Direction.EAST;
				case WEST_OPEN -> side == Direction.WEST;
				case NORTH_OPEN -> side == Direction.NORTH;
				case SOUTH_OPEN -> side == Direction.SOUTH;
				default -> false;
			};
		}

		public boolean isDisguised(Direction side) {
			return switch (this) {
				case CLOSED_HIDDEN -> true;
				case EAST_HIDDEN -> side != Direction.WEST;
				case WEST_HIDDEN -> side != Direction.EAST;
				case SOUTH_HIDDEN -> side != Direction.NORTH;
				case NORTH_HIDDEN -> side != Direction.SOUTH;
				case EAST_OPEN, EAST_CLOSED -> side != Direction.EAST;
				case WEST_OPEN, WEST_CLOSED -> side != Direction.WEST;
				case SOUTH_OPEN, SOUTH_CLOSED -> side != Direction.SOUTH;
				case NORTH_OPEN, NORTH_CLOSED -> side != Direction.NORTH;
				default -> false;
			};
		}

		public static Passability hidden(Direction direction) {
			return switch (direction) {
				case EAST -> EAST_HIDDEN;
				case WEST -> WEST_HIDDEN;
				case SOUTH -> SOUTH_HIDDEN;
				case NORTH -> NORTH_HIDDEN;
				default -> throw new IllegalArgumentException("Shift locks require a horizontal direction");
			};
		}
	}

	public static final EnumProperty<Passability> PASSABILITY = EnumProperty.create("passability", Passability.class);
	private static final double INSET = 2;
	private final MapCodec<BlockShiftLock> codec = MapCodec.unit(this);

	public BlockShiftLock(BlockBehaviour.Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(PASSABILITY, Passability.CLOSED));
	}

	@Override public MapCodec<? extends BlockShiftLock> codec() { return codec; }

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(PASSABILITY);
	}

	@Override
	protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
		return state.getValue(PASSABILITY) == Passability.BREAKABLE
				? super.getDestroyProgress(state, player, level, pos) : 0;
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
			CollisionContext context) {
		Passability passability = state.getValue(PASSABILITY);
		if (passability == Passability.OPEN) return Shapes.empty();
		if (context instanceof EntityCollisionContext entities) {
			Entity entity = entities.getEntity();
			if (entity != null && entity.getBoundingBox().intersects(new AABB(pos)))
				return Shapes.empty();
		}
		double minX = 0, minY = 0, minZ = 0, maxX = 16, maxY = 16, maxZ = 16;
		if (passability.isPassable(Direction.DOWN)) minY += INSET;
		if (passability.isPassable(Direction.UP)) maxY -= INSET;
		if (passability.isPassable(Direction.EAST)) maxX -= INSET;
		if (passability.isPassable(Direction.WEST)) minX += INSET;
		if (passability.isPassable(Direction.NORTH)) minZ += INSET;
		if (passability.isPassable(Direction.SOUTH)) maxZ -= INSET;
		return box(minX, minY, minZ, maxX, maxY, maxZ);
	}

	public static void setOpen(Level level, BlockPos pos, boolean open) {
		BlockState state = level.getBlockState(pos);
		if (!state.is(ChromaBlocks.SHIFT_LOCK.get())) return;
		Passability old = state.getValue(PASSABILITY);
		Passability next = switch (old) {
			case CLOSED, OPEN -> open ? Passability.OPEN : Passability.CLOSED;
			case EAST_CLOSED, EAST_OPEN -> open ? Passability.EAST_OPEN : Passability.EAST_CLOSED;
			case WEST_CLOSED, WEST_OPEN -> open ? Passability.WEST_OPEN : Passability.WEST_CLOSED;
			case SOUTH_CLOSED, SOUTH_OPEN -> open ? Passability.SOUTH_OPEN : Passability.SOUTH_CLOSED;
			case NORTH_CLOSED, NORTH_OPEN -> open ? Passability.NORTH_OPEN : Passability.NORTH_CLOSED;
			default -> old;
		};
		if (old != next) {
			level.setBlock(pos, state.setValue(PASSABILITY, next), 3);
			ChromaSounds.CAST.playSoundAtBlock(level, pos, 0.5F, 0.75F);
		}
	}
}
