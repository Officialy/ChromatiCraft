package reika.chromaticraft.tileentity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;

import reika.chromaticraft.registry.ChromaBlockEntities;

/**
 * V33a {@code BlockDummyAux.TileEntityDummyAux}: a stand-in block that forwards to a real tile
 * somewhere else.
 *
 * <p>Large ChromatiCraft structures need a block to occupy space above their controller — for a
 * hitbox you can click, or a body you can see — without that block being a second machine. This is
 * that block. It holds a link to the tile it stands for and three independent flags, so the same
 * block can be a solid clickable body, an invisible collision box, or a purely visual shell
 * depending on what the structure wanted.
 */
public class TileEntityDummyAux extends BlockEntity {

	/** V33a Flags, stored as a bitfield in one int exactly as upstream does. */
	public enum Flags {
		HITBOX,
		MOUSEOVER,
		RENDER;

		private final int bit = 1 << this.ordinal();
	}

	private int flags;
	private BlockPos relay;

	public TileEntityDummyAux(BlockPos pos, BlockState state) {
		super(ChromaBlockEntities.DUMMY_AUX.get(), pos, state);
	}

	/** V33a link: the tile this block stands in for. */
	public void link(BlockPos pos) {
		relay = pos;
		this.setChanged();
	}

	public BlockPos getLink() {
		return relay;
	}

	public void setFlag(Flags f, boolean set) {
		if (this.getFlag(f) != set)
			flags ^= f.bit;
		this.setChanged();
	}

	public boolean getFlag(Flags f) {
		return (f.bit & flags) != 0;
	}

	public BlockEntity getLinkedTile() {
		return relay != null && level != null ? level.getBlockEntity(relay) : null;
	}

	/** V33a relayClick: a click here is a click on the linked block, at the same face and offsets. */
	public InteractionResult relayClick(Player player, BlockHitResult hit) {
		if (relay == null || level == null)
			return InteractionResult.PASS;
		BlockState target = level.getBlockState(relay);
		return target.useWithoutItem(level, player,
				hit.withPosition(relay).withDirection(hit.getDirection()));
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putInt("flags", flags);
		if (relay != null) {
			output.putInt("locx", relay.getX());
			output.putInt("locy", relay.getY());
			output.putInt("locz", relay.getZ());
		}
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		flags = input.getIntOr("flags", 0);
		relay = input.getInt("locx").isPresent()
				? new BlockPos(input.getIntOr("locx", 0), input.getIntOr("locy", 0),
						input.getIntOr("locz", 0))
				: null;
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}
}
