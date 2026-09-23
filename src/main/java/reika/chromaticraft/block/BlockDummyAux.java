package reika.chromaticraft.block;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import reika.chromaticraft.tileentity.TileEntityDummyAux;
import reika.chromaticraft.tileentity.TileEntityDummyAux.Flags;

/**
 * V33a {@code BlockDummyAux}: the block a structure puts where it needs presence but not a machine.
 *
 * <p>Everything it does is decided by the flags on its {@link TileEntityDummyAux}. With
 * {@link Flags#HITBOX} it collides; with {@link Flags#RENDER} it draws as structure stone and is
 * otherwise invisible; clicking it is forwarded to whatever tile it was linked to. Unbreakable with
 * resistance 60000, because it is part of a structure and removing it would leave a hole in one.
 */
public class BlockDummyAux extends Block implements EntityBlock {

	private final MapCodec<BlockDummyAux> codec = MapCodec.unit(this);

	public BlockDummyAux(BlockBehaviour.Properties properties) {
		super(properties);
	}

	@Override
	public MapCodec<? extends BlockDummyAux> codec() {
		return codec;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new TileEntityDummyAux(pos, state);
	}

	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
			BlockEntityType<T> type) {
		return null; // V33a canUpdate() returns false.
	}

	/** V33a getCollisionBoundingBoxFromPool: solid only when the HITBOX flag is set. */
	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
			CollisionContext context) {
		if (!(level.getBlockEntity(pos) instanceof TileEntityDummyAux dummy)
				|| !dummy.getFlag(Flags.HITBOX)) return Shapes.empty();
		return linkedShape(dummy, pos);
	}

	/** V33a: the block is only mouse-targetable when it asked to be. */
	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
			CollisionContext context) {
		if (!(level.getBlockEntity(pos) instanceof TileEntityDummyAux dummy)
				|| !(dummy.getFlag(Flags.HITBOX) || dummy.getFlag(Flags.MOUSEOVER)))
			return Shapes.empty();
		return linkedShape(dummy, pos);
	}

	private static VoxelShape linkedShape(TileEntityDummyAux dummy, BlockPos pos) {
		if (!(dummy.getLinkedTile() instanceof reika.chromaticraft.tileentity.TileEntityDataNode tower))
			return Shapes.block();
		int offset = pos.getY() - tower.getBlockPos().getY();
		double lowerStart = tower.getExtension1();
		double upperStart = 1 + tower.getExtension1() + tower.getExtension2();
		return Shapes.or(
				towerSection(offset, lowerStart, lowerStart + 2.5, 4 / 16D),
				towerSection(offset, upperStart, upperStart + 1.5, 5 / 16D));
	}

	private static VoxelShape towerSection(int offset, double bottom, double top, double inset) {
		double localBottom = Math.max(0, bottom - offset);
		double localTop = Math.min(1, top - offset);
		return localTop > localBottom
				? Shapes.box(inset, localBottom, inset, 1 - inset, localTop, 1 - inset)
				: Shapes.empty();
	}

	/** V33a onBlockActivated: forward the click to the linked tile. */
	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
			Player player, BlockHitResult hit) {
		if (level.getBlockEntity(pos) instanceof TileEntityDummyAux dummy)
			return dummy.relayClick(player, hit);
		return InteractionResult.PASS;
	}
}
