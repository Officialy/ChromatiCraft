package reika.chromaticraft.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import net.neoforged.neoforge.transfer.fluid.FluidUtil;

import reika.chromaticraft.registry.ChromaTiles;
import reika.chromaticraft.tileentity.recipe.TileEntityAuraInfuser;

/** Interaction/collision block for V33a's modelled Item Aura Infuser pedestal. */
public final class BlockItemAuraInfuser extends BlockChromaticTile {

	private static final VoxelShape SHAPE = box(0, 0, 0, 16, 8, 16);

	public BlockItemAuraInfuser(Properties properties) {
		this(properties, ChromaTiles.INFUSER);
	}

	public BlockItemAuraInfuser(Properties properties, ChromaTiles tile) {
		super(properties, tile);
	}

	@Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}
	@Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
			CollisionContext context) { return SHAPE; }

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(level, pos, state, placer, stack);
		if (placer instanceof Player player && level.getBlockEntity(pos) instanceof TileEntityAuraInfuser infuser)
			infuser.setPlacer(player);
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
			Player player, InteractionHand hand, BlockHitResult hit) {
		if (!(level.getBlockEntity(pos) instanceof TileEntityAuraInfuser infuser)) return InteractionResult.PASS;
		if (FluidUtil.interactWithFluidHandler(player, hand, pos, infuser.fluidHandler(), null))
			return InteractionResult.SUCCESS;
		if (!level.isClientSide()) player.setItemInHand(hand, infuser.interact(stack, player));
		return InteractionResult.SUCCESS;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
			Player player, BlockHitResult hit) {
		if (!(level.getBlockEntity(pos) instanceof TileEntityAuraInfuser infuser)) return InteractionResult.PASS;
		if (!level.isClientSide()) infuser.interact(ItemStack.EMPTY, player);
		return InteractionResult.SUCCESS;
	}

	@Override
	protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity,
			InsideBlockEffectApplier effects, boolean precise) {
		if (entity instanceof ItemEntity item && level.getBlockEntity(pos) instanceof TileEntityAuraInfuser infuser)
			infuser.onItemCollision(item);
	}
}
