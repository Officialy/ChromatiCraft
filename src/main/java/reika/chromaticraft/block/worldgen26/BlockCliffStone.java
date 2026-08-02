package reika.chromaticraft.block.worldgen26;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

import reika.dragonapi.auxiliary.ProgressiveRecursiveBreaker;
import reika.dragonapi.auxiliary.ProgressiveRecursiveBreaker.BreakerCallback;
import reika.dragonapi.auxiliary.ProgressiveRecursiveBreaker.ProgressiveBreaker;
import reika.dragonapi.libraries.io.ReikaSoundHelper;

/** One concrete block identity for each V33a cliff-stone material proxy. */
public final class BlockCliffStone extends Block {
    public static final BooleanProperty TRANSPARENT = BooleanProperty.create("transparent");

    public enum Type { STONE, DIRT, GRASS, FARMLAND }
    private final Type type;

    private static final BreakerCallback TRANSPARIFIER = new BreakerCallback() {
        @Override
        public boolean canBreak(ProgressiveBreaker breaker, Level level, BlockPos pos, Block block) {
            BlockState state = level.getBlockState(pos);
            return block instanceof BlockCliffStone cliff && cliff.type == Type.STONE
                    && !state.getValue(TRANSPARENT);
        }

        @Override
        public void onPreBreak(ProgressiveBreaker breaker, Level level, BlockPos pos, Block block) {
        }

        @Override
        public void onPostBreak(ProgressiveBreaker breaker, Level level, BlockPos pos, Block block) {
            level.setBlock(pos, block.defaultBlockState().setValue(TRANSPARENT, true), 2);
            for (int dy = -1; dy <= 4; dy++)
                level.getLightEngine().checkBlock(pos.above(dy));
            ReikaSoundHelper.playPlaceSound(level, pos.getX(), pos.getY(), pos.getZ(), block);
        }

        @Override
        public void onFinish(ProgressiveBreaker breaker) {
        }
    };

    public BlockCliffStone(Properties properties, Type type) {
        super(properties);
        this.type = type;
        registerDefaultState(stateDefinition.any().setValue(TRANSPARENT, false));
    }

    public Type getCliffType() { return type; }

    /** V33a Manipulator action: progressively reveal the connected natural cliff-stone mass. */
    public static boolean transparify(Level level, BlockPos pos, Player player) {
        if (level.isClientSide())
            return false;
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof BlockCliffStone cliff) || cliff.type != Type.STONE
                || state.getValue(TRANSPARENT))
            return false;
        ProgressiveBreaker breaker = ProgressiveRecursiveBreaker.instance.addBlockPosWithReturn(level, pos, 30);
        breaker.call = TRANSPARIFIER;
        breaker.drops = false;
        breaker.extraSpread = true;
        breaker.player = player;
        breaker.causeUpdates = false;
        return true;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TRANSPARENT);
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state) {
        return state.getValue(TRANSPARENT);
    }
}