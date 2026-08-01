package reika.chromaticraft.block;

import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;

import reika.chromaticraft.block.BlockCrystallineStone.StoneTypes;

/**
 * A crystalline-stone beam, which runs along X or Z and never vertically.
 *
 * <p>Uses vanilla's {@link BlockStateProperties#HORIZONTAL_AXIS}, whose value set is exactly
 * {@code X} and {@code Z} — a beam has no upright form, so offering a Y value would be a state the
 * block can never legitimately hold. Placement takes the axis from the face that was clicked,
 * falling back to the placer's facing when that face is the top or bottom.
 */
public class BlockCrystallineStoneBeam extends BlockCrystallineStone {

	public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;

	public BlockCrystallineStoneBeam(BlockBehaviour.Properties props, StoneTypes type) {
		super(props, type);
		this.registerDefaultState(this.stateDefinition.any().setValue(AXIS, Direction.Axis.X));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(AXIS);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		Direction face = context.getClickedFace();
		Direction.Axis axis = face.getAxis() == Direction.Axis.Y
				? context.getHorizontalDirection().getAxis()
				: face.getAxis();
		return this.defaultBlockState().setValue(AXIS, axis);
	}
}
