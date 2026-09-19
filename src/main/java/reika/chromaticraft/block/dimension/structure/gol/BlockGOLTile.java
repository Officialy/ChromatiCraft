package reika.chromaticraft.block.dimension.structure.gol;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

import org.jspecify.annotations.Nullable;

import reika.chromaticraft.tileentity.TileEntityGOLTile;

/**
 * V33a Cellular-Automata floor and its ceiling memory. The old 0/1/2/3 metadata values described
 * mutable state, not content identities, so two explicit boolean properties are the faithful 26.2
 * representation. Only floor cells own block entities; memory cells are passive history pixels.
 */
public final class BlockGOLTile extends Block implements EntityBlock {

    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
    public static final BooleanProperty MEMORY = BooleanProperty.create("memory");
    private final MapCodec<BlockGOLTile> codec = MapCodec.unit(this);

    public BlockGOLTile(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(ACTIVE, false).setValue(MEMORY, false));
    }

    private static void toggle(Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof TileEntityGOLTile tile) tile.requestToggle();
    }

    @Override
    public MapCodec<? extends BlockGOLTile> codec() {
        return codec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ACTIVE, MEMORY);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(MEMORY) ? null : new TileEntityGOLTile(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (state.getValue(MEMORY)) return InteractionResult.PASS;
        if (!level.isClientSide()) toggle(level, pos);
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void attack(BlockState state, Level level, BlockPos pos, Player player) {
        if (!state.getValue(MEMORY) && !level.isClientSide()) toggle(level, pos);
        super.attack(state, level, pos, player);
    }

    @Override
    public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, double fallDistance) {
        super.fallOn(level, state, pos, entity, fallDistance);
        if (fallDistance > 1 && entity instanceof Player && !state.getValue(MEMORY) && !level.isClientSide())
            toggle(level, pos);
    }
}
