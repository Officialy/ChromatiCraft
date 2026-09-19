package reika.chromaticraft.block.relay;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import org.jspecify.annotations.Nullable;

import reika.chromaticraft.registry.ChromaItems;
import reika.chromaticraft.registry.CrystalElement;
import reika.dragonapi.libraries.io.ReikaSoundHelper;

/**
 * Common V33a contract for the non-colliding relay conduits. The item identity owns the element;
 * only the player-selected incoming direction is dynamic and therefore belongs in block-entity NBT.
 */
public abstract class BlockRelayBase extends BaseEntityBlock {

	protected BlockRelayBase(BlockBehaviour.Properties properties) {
		super(properties);
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
			BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		if (!stack.is(ChromaItems.MANIPULATOR.get())) return InteractionResult.PASS;
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof TileRelayBase relay)
			relay.setInput(hit.getDirection());
		return InteractionResult.SUCCESS;
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
			CollisionContext context) {
		return Shapes.empty();
	}

	@Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.INVISIBLE; }

	public abstract static class TileRelayBase extends BlockEntity {

		private Direction input;

		protected TileRelayBase(BlockEntityType<?> type, BlockPos pos, BlockState state) {
			super(type, pos, state);
			input = state.hasProperty(BlockLumenRelay.FACING)
					? state.getValue(BlockLumenRelay.FACING).getOpposite() : Direction.DOWN;
		}

		public abstract boolean canTransmit(CrystalElement element);

		/** Reserved for the source's old light-value branch; V33a relays never set it true. */
		public final boolean isTransmitting() { return false; }
		public final Direction getInput() { return input; }

		public final void setInput(Direction direction) {
			if (direction == null || direction == input) return;
			input = direction;
			setChanged();
			if (level != null) {
				ReikaSoundHelper.playBreakSound(level, worldPosition, getBlockState().getBlock());
				level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
			}
		}

		@Override
		protected void saveAdditional(ValueOutput output) {
			super.saveAdditional(output);
			output.putInt("dir", input.ordinal());
		}

		@Override
		protected void loadAdditional(ValueInput valueInput) {
			super.loadAdditional(valueInput);
			int ordinal = valueInput.getIntOr("dir", Direction.DOWN.ordinal());
			input = Direction.from3DDataValue(Math.floorMod(ordinal, Direction.values().length));
		}

		@Override public CompoundTag getUpdateTag(HolderLookup.Provider provider) {
			return saveCustomOnly(provider);
		}
		@Override public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
			return ClientboundBlockEntityDataPacket.create(this);
		}
	}
}
