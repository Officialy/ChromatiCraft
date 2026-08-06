package reika.chromaticraft.tileentity.aoe;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import reika.chromaticraft.magic.WarpNetwork;
import reika.chromaticraft.registry.ChromaBlockEntities;
import reika.dragonapi.instantiable.data.immutable.WorldLocation;

/**
 * V33a's {@code BlockWarpNode.TileEntityWarpNode}, lifted out of the block into its own file.
 *
 * <p>It holds one flag. A node exists in the world from generation, but it is inert until a player
 * opens it with the Manipulator — only then is it registered in {@link WarpNetwork} and able to
 * receive or send a jump. The flag is synchronized because the renderer draws an open node very
 * differently from a closed one, and it is visible from a long way off.
 */
public class TileEntityWarpNode extends BlockEntity {

	private boolean isOpen;

	public TileEntityWarpNode(BlockPos pos, BlockState state) {
		super(ChromaBlockEntities.WARP_NODE.get(), pos, state);
	}

	public void open() {
		isOpen = true;
		if (level != null && !level.isClientSide())
			WarpNetwork.instance.addLocation(new WorldLocation(level, worldPosition));
		this.setChanged();
		if (level != null)
			level.sendBlockUpdated(worldPosition, this.getBlockState(), this.getBlockState(), 3);
	}

	public boolean isOpen() {
		return isOpen;
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putBoolean("open", isOpen);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		isOpen = input.getBooleanOr("open", false);
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	/**
	 * V33a getRenderBoundingBox: the block's own box expanded by 0.6, with a 512-block render
	 * distance, because the node's effect is drawn well outside its own cube. 26.x removed the
	 * per-BlockEntity hook, so the renderer supplies this when the warp-node BER lands.
	 */
	public AABB getWarpRenderBounds() {
		return new AABB(worldPosition).inflate(0.6);
	}
}
