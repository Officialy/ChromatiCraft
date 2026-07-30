package reika.chromaticraft.block.worldgen26;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import reika.chromaticraft.render.particle.ChromaParticle;
import reika.chromaticraft.registry.ChromaBlocks;

/** V33a Glow Root: a self-limiting luminous chain suspended from natural cave ceilings. */
public final class BlockGlowRoot extends Block {
    private static final VoxelShape SHAPE = BlockGlowRoot.box(4, 4, 4, 12, 12, 12);
    private final MapCodec<BlockGlowRoot> codec = MapCodec.unit(this);

    public BlockGlowRoot(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<? extends BlockGlowRoot> codec() {
        return codec;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockState above = level.getBlockState(pos.above());
        boolean ceiling = above.is(this) || above.is(BlockTags.BASE_STONE_OVERWORLD)
                || above.is(BlockTags.DIRT) || above.is(ChromaBlocks.CLIFF_STONE.get())
                || above.is(ChromaBlocks.CLIFF_DIRT.get()) || above.is(ChromaBlocks.CLIFF_GRASS.get());
        return ceiling && !isRootTooLong(level, pos);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks,
            BlockPos pos, Direction direction, BlockPos neighbourPos, BlockState neighbourState,
            RandomSource random) {
        return direction == Direction.UP && !canSurvive(state, level, pos)
                ? Blocks.AIR.defaultBlockState()
                : super.updateShape(state, level, ticks, pos, direction, neighbourPos, neighbourState, random);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        BlockPos target = pos.below();
        if (random.nextInt(36) == 0 && level.getBlockState(target).isAir()
                && level.getBlockState(target.below()).isAir()
                && defaultBlockState().canSurvive(level, target)) {
            BlockState grown = defaultBlockState();
            level.setBlockAndUpdate(target, grown);
            level.playSound(null, pos, SoundEvents.GRASS_BREAK, SoundSource.BLOCKS, 1, 1);
            level.playSound(null, target, SoundEvents.GRASS_BREAK, SoundSource.BLOCKS, 1, 1);
            level.levelEvent(2001, pos, Block.getId(state));
            level.levelEvent(2001, target, Block.getId(grown));
        }
        // CHROMA-PORT: V33a also emits a weighted Fertility Seed on 1/4 non-growth ticks.
        // Restore that call when the seven concrete (former metadata) Fertility Seed items land.
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(3) == 0 && (!level.getBlockState(pos.below()).is(this)
                || Math.floorMod(pos.getY(), 8) == 0)
                && level instanceof net.minecraft.client.multiplayer.ClientLevel client)
            ChromaParticle.spawnGlowRoot(client, pos, random);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
            CollisionContext context) {
        return SHAPE;
    }

    private boolean isRootTooLong(LevelReader level, BlockPos pos) {
        BlockPos bottom = pos;
        while (level.getBlockState(bottom.below()).is(this))
            bottom = bottom.below();
        int length = 1;
        BlockPos cursor = bottom;
        while (level.getBlockState(cursor.above()).is(this)) {
            cursor = cursor.above();
            length++;
        }
        if (length < 4)
            return false;
        int span = length;
        cursor = bottom;
        while (cursor.getY() > level.getMinY() && level.getBlockState(cursor.below()).isAir()) {
            cursor = cursor.below();
            span++;
        }
        return length > span * 0.33;
    }
}
