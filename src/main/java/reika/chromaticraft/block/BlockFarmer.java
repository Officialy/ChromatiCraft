package reika.chromaticraft.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import reika.chromaticraft.registry.ChromaTiles;

public final class BlockFarmer extends BlockChromaticTile {
    public BlockFarmer(Properties properties) {
        super(properties, ChromaTiles.FARMER);
        registerDefaultState(stateDefinition.any().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH));
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BlockStateProperties.HORIZONTAL_FACING);
    }

    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,
                context.getHorizontalDirection().getOpposite());
    }

    @Override protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(BlockStateProperties.HORIZONTAL_FACING,
                rotation.rotate(state.getValue(BlockStateProperties.HORIZONTAL_FACING)));
    }

    @Override protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(BlockStateProperties.HORIZONTAL_FACING)));
    }

    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.INVISIBLE; }

    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }
}
