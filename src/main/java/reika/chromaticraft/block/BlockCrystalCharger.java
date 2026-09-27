package reika.chromaticraft.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import reika.chromaticraft.registry.ChromaTiles;
import reika.chromaticraft.tileentity.auxiliary.TileEntityCrystalCharger;

/** Player and exact V33a selection-box behavior for the Storage Crystal Charger; its inventory drops as a container. */
public final class BlockCrystalCharger extends BlockChromaticTile {

	private static final VoxelShape SHAPE = box(2, 0, 2, 14, 16, 14);

	public BlockCrystalCharger(Properties properties) {
		super(properties, ChromaTiles.CHARGER);
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(level, pos, state, placer, stack);
		if (placer instanceof Player player
				&& level.getBlockEntity(pos) instanceof TileEntityCrystalCharger charger)
			charger.setPlacer(player);
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
			Player player, InteractionHand hand, BlockHitResult hit) {
		return this.open(level, pos, player);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
			Player player, BlockHitResult hit) {
		return this.open(level, pos, player);
	}

	private InteractionResult open(Level level, BlockPos pos, Player player) {
		if (!(level.getBlockEntity(pos) instanceof TileEntityCrystalCharger charger))
			return InteractionResult.PASS;
		if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer)
			serverPlayer.openMenu(charger, pos);
		return InteractionResult.SUCCESS;
	}
}
