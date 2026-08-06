package reika.chromaticraft.block.worldgen26;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import reika.chromaticraft.tileentity.TileEntityLootChest;

/**
 * V33a {@code BlockLootChest}: the chest ChromatiCraft's structures hide their rewards in.
 *
 * <p>Its defining rule is that a covered chest is a locked chest. If a solid block sits directly on
 * top, only the chest's owner may open it — which is how structures make a reward conditional on
 * taking the structure apart correctly rather than tunnelling to the box. An unowned, uncovered chest
 * behaves like any other. A chest that cannot be opened also cannot be broken: V33a returns -1
 * relative hardness in that case.
 */
public class BlockLootChest extends Block implements EntityBlock {

	public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

	/** V33a setBlockBoundsBasedOnState: the usual inset chest body. */
	private static final VoxelShape SHAPE = box(1, 0, 1, 15, 14, 15);

	private final MapCodec<BlockLootChest> codec = MapCodec.unit(this);

	public BlockLootChest(BlockBehaviour.Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	@Override
	public MapCodec<? extends BlockLootChest> codec() {
		return codec;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new TileEntityLootChest(pos, state);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	/** V33a onBlockPlacedBy: whoever places it owns it. */
	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer,
			ItemStack stack) {
		super.setPlacedBy(level, pos, state, placer, stack);
		if (placer instanceof Player player && level.getBlockEntity(pos) instanceof TileEntityLootChest chest)
			chest.setPlacer(player.getUUID());
	}

	/**
	 * V33a canOpen. A covered chest is owner-only; an uncovered one only needs to be reachable and
	 * unclaimed. Fake players are refused outright so automation cannot strip a structure.
	 */
	public static boolean canOpen(Level level, BlockPos pos, Player player) {
		if (!(level.getBlockEntity(pos) instanceof TileEntityLootChest chest))
			return false;
		if (player != null && player instanceof net.neoforged.neoforge.common.util.FakePlayer)
			return false;
		// CHROMA-PORT: V33a additionally gates on DimensionTuningManager.TuningThresholds.CHESTS
		// inside the ChromatiCraft pocket dimension, which is not ported.
		if (level.getBlockState(pos.above()).isFaceSturdy(level, pos.above(), Direction.DOWN))
			return player == null || chest.isOwnedBy(player);
		return player == null || chest.isAccessibleBy(player);
	}

	/** V33a getPlayerRelativeBlockHardness: a chest you cannot open is a chest you cannot break. */
	@Override
	protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
		if (level instanceof Level real && !canOpen(real, pos, player))
			return 0;
		return super.getDestroyProgress(state, player, level, pos);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
			Player player, BlockHitResult hit) {
		if (level.isClientSide())
			return InteractionResult.SUCCESS;
		if (!(level.getBlockEntity(pos) instanceof TileEntityLootChest chest))
			return InteractionResult.PASS;
		if (!canOpen(level, pos, player)) {
			level.playSound(null, pos, net.minecraft.sounds.SoundEvents.STONE_PLACE,
					net.minecraft.sounds.SoundSource.BLOCKS, 1, 1);
			chest.markOpened();
			return InteractionResult.SUCCESS;
		}
		// CHROMA-PORT: V33a grants the chest's ProgressElement triggers here, which the structures
		// attach when they generate; that set lands with the structure/progression wiring.
		chest.markOpened();
		player.openMenu(chest);
		return InteractionResult.SUCCESS;
	}

	// V33a breakBlock drops the inventory; the generated loot table handles that on the modern side.

	@Override
	protected boolean hasAnalogOutputSignal(BlockState state) {
		return true;
	}

	@Override
	protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
		return level.getBlockEntity(pos) instanceof TileEntityLootChest chest
				? net.minecraft.world.inventory.AbstractContainerMenu.getRedstoneSignalFromContainer(chest)
				: 0;
	}
}
