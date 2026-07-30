package reika.chromaticraft.block.worldgen26;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/** One concrete block identity for each V33a cliff-stone material proxy. */
public final class BlockCliffStone extends Block {
    public static final BooleanProperty TRANSPARENT = BooleanProperty.create("transparent");

    public enum Type { STONE, DIRT, GRASS, FARMLAND }
    private final Type type;

    public BlockCliffStone(Properties properties, Type type) {
        super(properties);
        this.type = type;
        registerDefaultState(stateDefinition.any().setValue(TRANSPARENT, false));
    }

    public Type getCliffType() { return type; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TRANSPARENT);
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state) {
        return state.getValue(TRANSPARENT);
    }
}