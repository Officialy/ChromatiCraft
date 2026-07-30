package reika.chromaticraft.block.worldgen26;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.render.particle.ChromaParticle;

/** V33a Glow Daisy: a dim-cave floor plant that slowly spreads without forming dense carpets. */
public final class BlockGlowDaisy extends VegetationBlock {
    public static final BooleanProperty CROP_BOOSTED = BooleanProperty.create("crop_boosted");
    private static final VoxelShape SHAPE = BlockGlowDaisy.box(0, 0, 0, 16, 10, 16);
    private final MapCodec<BlockGlowDaisy> codec = MapCodec.unit(this);

    public BlockGlowDaisy(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(CROP_BOOSTED, false));
    }

    @Override
    public MapCodec<? extends BlockGlowDaisy> codec() {
        return codec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CROP_BOOSTED);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks,
            BlockPos pos, Direction direction, BlockPos neighbourPos, BlockState neighbourState,
            RandomSource random) {
        BlockState updated = super.updateShape(state, level, ticks, pos, direction, neighbourPos,
                neighbourState, random);
        return updated.is(this) ? updated.setValue(CROP_BOOSTED, hasAdjacentCrop(level, pos)) : updated;
    }
    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(BlockTags.DIRT) || state.is(ChromaBlocks.CLIFF_DIRT.get())
                || state.is(ChromaBlocks.CLIFF_GRASS.get());
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(CROP_BOOSTED,
                hasAdjacentCrop(context.getLevel(), context.getClickedPos()));
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int neighbours = 0;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (level.getBlockState(pos.relative(direction)).is(this))
                neighbours++;
        }
        if (neighbours >= 2 || neighbours == 1 && random.nextBoolean() || random.nextInt(240) != 0)
            return;
        Direction direction = Direction.Plane.HORIZONTAL.getRandomDirection(random);
        BlockPos target = pos.relative(direction);
        if (level.getBlockState(target).isAir() && level.getBrightness(LightLayer.SKY, target) <= 8
                && defaultBlockState().canSurvive(level, target)) {
            BlockState grown = defaultBlockState().setValue(CROP_BOOSTED, hasAdjacentCrop(level, target));
            level.setBlockAndUpdate(target, grown);
            playGrowthEffects(level, pos, target, state, grown);
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (level instanceof net.minecraft.client.multiplayer.ClientLevel client)
            ChromaParticle.spawnGlowDaisy(client, pos, random);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
            CollisionContext context) {
        return SHAPE;
    }

    private static void playGrowthEffects(ServerLevel level, BlockPos source, BlockPos target,
            BlockState sourceState, BlockState targetState) {
        level.playSound(null, source, SoundEvents.GRASS_BREAK, SoundSource.BLOCKS, 1, 1);
        level.playSound(null, target, SoundEvents.GRASS_BREAK, SoundSource.BLOCKS, 1, 1);
        level.levelEvent(2001, source, Block.getId(sourceState));
        level.levelEvent(2001, target, Block.getId(targetState));
    }

    private static boolean hasAdjacentCrop(LevelReader level, BlockPos pos) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (level.getBlockState(pos.relative(direction)).is(BlockTags.CROPS))
                return true;
        }
        return false;
    }
}
