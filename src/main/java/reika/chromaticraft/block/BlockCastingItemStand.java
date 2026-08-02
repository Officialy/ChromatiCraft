package reika.chromaticraft.block;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import reika.chromaticraft.registry.ChromaTiles;
import reika.chromaticraft.tileentity.recipe.TileEntityItemStand;

/** Player-facing V33a casting stand interaction and dynamic inventory drops. */
public final class BlockCastingItemStand extends BlockChromaticTile {
	public BlockCastingItemStand(Properties properties) { super(properties, ChromaTiles.STAND); }
	@Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(level, pos, state, placer, stack);
		if (placer instanceof Player player && level.getBlockEntity(pos) instanceof TileEntityItemStand stand) stand.setPlacer(player);
	}
	@Override protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) { return interact(level, pos, player, hand); }
	@Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) { return interact(level, pos, player, InteractionHand.MAIN_HAND); }
	private InteractionResult interact(Level level, BlockPos pos, Player player, InteractionHand hand) {
		BlockEntity blockEntity = level.getBlockEntity(pos);
		if (!(blockEntity instanceof TileEntityItemStand stand)) return InteractionResult.PASS;
		if (level.isClientSide()) return InteractionResult.SUCCESS;
		// V33a: sneak-right-clicking an empty stand with an empty hand adds it to the
		// player's spread set. The next stand click with an item distributes that stack
		// evenly over every selected stand.
		if (player.isShiftKeyDown() && player.getItemInHand(hand).isEmpty()) {
			stand.queueSpread(player);
			return InteractionResult.SUCCESS;
		}
		return stand.interact(player, hand) ? InteractionResult.SUCCESS : InteractionResult.FAIL;
	}
	@Override public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player, ItemStack toolStack, boolean willHarvest, FluidState fluid) {
		if (level.getBlockEntity(pos) instanceof TileEntityItemStand stand && stand.isLocked()) return false;
		return super.onDestroyedByPlayer(state, level, pos, player, toolStack, willHarvest, fluid);
	}
	@Override public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
		List<ItemStack> drops = new ArrayList<>(super.getDrops(state, params));
		BlockEntity blockEntity = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
		if (blockEntity instanceof TileEntityItemStand stand && !stand.getItem(0).isEmpty()) drops.add(stand.getItem(0).copy());
		return drops;
	}
}
