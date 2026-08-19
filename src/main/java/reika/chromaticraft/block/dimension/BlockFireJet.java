package reika.chromaticraft.block.dimension;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

import reika.chromaticraft.magic.progression.ProgressStage;
import reika.chromaticraft.tileentity.dimension.TileEntityFireJet;

/**
 * V33a's Fire Jet, one of the two types of {@code BlockDimensionDecoTile}.
 *
 * <p>It sits at the bottom of Proxima's pools — the generator only places it under two blocks of water
 * or more — and every four hundred ticks or so it lights, throwing a column of coloured flame up through
 * the water for a minute or two before going quiet again. The countdown lives on the block entity so it
 * survives a save.
 *
 * <p>Two rules of upstream's are easy to lose and both are here. It is <b>mineable only by a player who
 * has reached {@code ProgressStage.CTM}</b> — {@code getPlayerRelativeBlockHardness} returns -1
 * otherwise, which 26.2 expresses as a destroy progress of zero — and it is <b>solid</b>, unlike the
 * other deco tile, so a player can stand on one in the middle of a pool.
 *
 * <h2>The other type is not here</h2>
 *
 * <p>{@code GLOWCRACKS} shares this block upstream and is deliberately absent: its art is a
 * 1024x1024 sheet ({@code Textures/glowcracks.png}) drawn by a custom renderer across a nine by nine
 * area, not a block icon, and no block icon for it exists in V33a's repository. Registering it with a
 * stand-in would be inventing art. Its generator waits with it.
 */
public class BlockFireJet extends BaseEntityBlock {

	public static final MapCodec<BlockFireJet> CODEC = simpleCodec(BlockFireJet::new);

	/** V33a tickRate: four hundred ticks between attempts, plus up to that again at random. */
	public static final int TICK_RATE = 400;

	public BlockFireJet(BlockBehaviour.Properties properties) {
		super(properties);
	}

	@Override
	protected MapCodec<? extends BaseEntityBlock> codec() {
		return CODEC;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new TileEntityFireJet(pos, state);
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
			BlockEntityType<T> type) {
		return (world, pos, blockState, be) -> {
			if (be instanceof TileEntityFireJet jet) {
				jet.updateEntity();
				jet.updateEntity(world, pos);
			}
		};
	}

	/** V33a onBlockAdded/onNeighborBlockChange: the jet re-arms whenever its surroundings change. */
	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState,
			boolean movedByPiston) {
		level.scheduleTick(pos, this, 1);
	}

	@Override
	protected void neighborChanged(BlockState state, Level level, BlockPos pos,
			net.minecraft.world.level.block.Block block, @javax.annotation.Nullable
			net.minecraft.world.level.redstone.Orientation orientation, boolean movedByPiston) {
		level.scheduleTick(pos, this, 1);
	}

	/** V33a updateTick: light, then schedule the next attempt somewhere in the next two tick rates. */
	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (level.getBlockEntity(pos) instanceof TileEntityFireJet jet)
			jet.activate();
		level.scheduleTick(pos, this, TICK_RATE + random.nextInt(1 + TICK_RATE));
	}

	/**
	 * V33a getPlayerRelativeBlockHardness: -1 for anyone who has not reached CTM, which is 26.2's
	 * destroy progress of zero. Expressed here rather than on a break event so it also covers
	 * explosions and other mods' miners.
	 */
	@Override
	protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
		return ProgressStage.CTM.isPlayerAtStage(player)
				? super.getDestroyProgress(state, player, level, pos) : 0;
	}
}
