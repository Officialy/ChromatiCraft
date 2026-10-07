package reika.chromaticraft.block.dimension.structure.rayblend;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;

import reika.chromaticraft.base.CrystalBlock;
import reika.chromaticraft.base.CrystalTypeBlock;
import reika.chromaticraft.registry.ChromaDimensions;
import reika.chromaticraft.registry.ChromaSounds;
import reika.chromaticraft.registry.CrystalElement;
import reika.chromaticraft.tileentity.TileEntityRayblendFloor;
import reika.chromaticraft.world.dimension.DimensionTuningManager;

/**
 * V33a {@code BlockRayblendFloor} (RAYBLEND): Crystal Interference's floor tile, one per puzzle cell, under the
 * crystal layer. Unbreakable (V33a {@code BlockDimensionStructureTile}), drawn in the translucent pass.
 *
 * <p>The cell's static data lives in the state so the chunk mesh can draw it ({@code RayblendFloorModel}):
 * {@link #REGION} is its subgrid (whose borders the edge glints outline), {@link #BLOCKED} marks a cell no colour
 * fits (V33a's X), and {@link #CAGE} is its mix cage colour plus one, or 0 (V33a {@code colorMultiplier} and rune).
 */
public final class BlockRayblendFloor26 extends Block implements EntityBlock {

	public static final IntegerProperty REGION = IntegerProperty.create("region", 0, 15);
	public static final BooleanProperty BLOCKED = BooleanProperty.create("blocked");
	public static final IntegerProperty CAGE = IntegerProperty.create("cage", 0, 16);

	public BlockRayblendFloor26(BlockBehaviour.Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(REGION, 0).setValue(BLOCKED, false).setValue(CAGE, 0));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(REGION, BLOCKED, CAGE);
	}

	public static BlockState state(BlockState base, int region, boolean blocked, @Nullable CrystalElement cage) {
		return base.setValue(REGION, region & 15).setValue(BLOCKED, blocked)
				.setValue(CAGE, cage == null ? 0 : cage.ordinal() + 1);
	}

	public static @Nullable CrystalElement cage(BlockState state) {
		int c = state.getValue(CAGE);
		return c == 0 ? null : CrystalElement.elements[c - 1];
	}

	/** V33a {@code b instanceof CrystalTypeBlock}: in 26.3 the cave/lamp crystals and the other crystal blocks split. */
	public static @Nullable CrystalElement crystalOf(BlockState state) {
		Block b = state.getBlock();
		if (b instanceof CrystalBlock crystal)
			return crystal.getCrystalElement(state);
		if (b instanceof CrystalTypeBlock crystal)
			return crystal.getCrystalElement(state);
		return null;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new TileEntityRayblendFloor(pos, state);
	}

	/** V33a {@code shouldSideBeRendered}: a face hides only against another floor tile. */
	@Override
	protected boolean skipRendering(BlockState state, BlockState neighbour, Direction side) {
		return neighbour.is(this) || super.skipRendering(state, neighbour, side);
	}

	/** V33a {@code onNeighborBlockChange}: the crystal above came or went. */
	@Override
	protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block,
			@Nullable Orientation orientation, boolean movedByPiston) {
		super.neighborChanged(state, level, pos, block, orientation, movedByPiston);
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof TileEntityRayblendFloor te)
			te.blockUpdate();
	}

	/**
	 * V33a {@code onRightClicked} behind {@code BlockDimensionStructureTile}'s tuning gate: holding a crystal, the
	 * click is refused with an error when the cell's region, row or column already holds that colour, and otherwise
	 * lets the crystal be placed; anything else is swallowed.
	 */
	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
			InteractionHand hand, BlockHitResult hit) {
		if (!tunedFor(level, player)) {
			if (!level.isClientSide())
				ChromaSounds.ERROR.playSoundAtBlock(level, pos);
			return InteractionResult.SUCCESS;
		}
		CrystalElement e = stack.getItem() instanceof BlockItem item
				? crystalOf(item.getBlock().defaultBlockState()) : null;
		if (e == null)
			return InteractionResult.SUCCESS;
		if (level.getBlockEntity(pos) instanceof TileEntityRayblendFloor te && !te.allowsCrystal(e)) {
			if (!level.isClientSide())
				ChromaSounds.ERROR.playSoundAtBlock(level, pos);
			return InteractionResult.SUCCESS;
		}
		return InteractionResult.PASS;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
			BlockHitResult hit) {
		if (!tunedFor(level, player) && !level.isClientSide())
			ChromaSounds.ERROR.playSoundAtBlock(level, pos);
		return InteractionResult.SUCCESS;
	}

	/** V33a: outside Proxima, or for a player tuned to its structures. */
	private static boolean tunedFor(Level level, Player player) {
		return level.dimension() != ChromaDimensions.PROXIMA
				|| DimensionTuningManager.TuningThresholds.STRUCTURES.isSufficientlyTuned(player);
	}

	@Override
	protected boolean triggerEvent(BlockState state, Level level, BlockPos pos, int id, int param) {
		super.triggerEvent(state, level, pos, id, param);
		BlockEntity tile = level.getBlockEntity(pos);
		return tile != null && tile.triggerEvent(id, param);
	}
}
