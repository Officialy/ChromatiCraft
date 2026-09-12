package reika.chromaticraft.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import reika.chromaticraft.registry.ChromaTiles;
import reika.chromaticraft.tileentity.auxiliary.TileEntityFunctionRelay;

public final class BlockFunctionRelay extends BlockChromaticTile {
    public BlockFunctionRelay(Properties properties) {
        super(properties, ChromaTiles.FUNCTIONRELAY);
    }

    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.INVISIBLE; }

    @Override protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

    @Override public float getEnchantPowerBonus(BlockState state, BlockGetter world, BlockPos pos) {
        if (world instanceof net.minecraft.world.level.Level level
                && world.getBlockEntity(pos) instanceof TileEntityFunctionRelay relay)
            return relay.getEnchantPowerInRange(level, pos);
        return super.getEnchantPowerBonus(state, world, pos);
    }
}
