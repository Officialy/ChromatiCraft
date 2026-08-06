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
		return level.getBlockEntity(pos) instanceof TileEntityDummyAux dummy
				&& dummy.getFlag(Flags.HITBOX) ? Shapes.block() : Shapes.empty();
	}

	/** V33a: the block is only mouse-targetable when it asked to be. */
	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
			CollisionContext context) {
		return level.getBlockEntity(pos) instanceof TileEntityDummyAux dummy
				&& (dummy.getFlag(Flags.HITBOX) || dummy.getFlag(Flags.MOUSEOVER))
				? Shapes.block() : Shapes.empty();
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
