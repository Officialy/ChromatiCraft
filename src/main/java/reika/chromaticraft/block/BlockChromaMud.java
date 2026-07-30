package reika.chromaticraft.block;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import net.minecraft.world.level.redstone.Orientation;
import reika.chromaticraft.magic.progression.ProgressStage;

/** V33a chroma mud: shallow, fall-safe, slowing ground created underneath Liquid Chroma. */
public final class BlockChromaMud extends Block {
	private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 15, 16);

	public BlockChromaMud(Properties properties) {
		super(properties);
	}

	@Override
	protected VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level,
			BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, net.minecraft.world.level.BlockGetter level,
			BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, double fallDistance) {
		entity.resetFallDistance();
	}

	@Override
	protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity,
			InsideBlockEffectApplier effectApplier, boolean isPrecise) {
		Vec3 movement = entity.getDeltaMovement();
		entity.setDeltaMovement(movement.x*0.67D, movement.y, movement.z*0.67D);
		if (entity instanceof Player player && !level.isClientSide())
			ProgressStage.MUDHINT.stepPlayerTo(player);
	}

	@Override
	protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor,
			@Nullable Orientation orientation, boolean movedByPiston) {
		if (!level.isClientSide() && level.getBlockState(pos.above()).blocksMotion())
			level.setBlockAndUpdate(pos, Blocks.DIRT.defaultBlockState());
	}

	@Override
	protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
		return new ItemStack(Blocks.DIRT);
	}
}
