package reika.chromaticraft.tileentity;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import reika.chromaticraft.registry.ChromaBlockEntities;

/** Persistent routing data for a panel switch, replacing V33a's generator UUID lookup. */
public final class TileEntityLightSwitch extends BlockEntity {

	public interface Handler {
		void onLightSwitch(BlockPos switchPos, int level, int channel, boolean up, @Nullable Player player);
	}

	private int panelLevel;
	private int channel;
	private @Nullable BlockPos delegate;

	public TileEntityLightSwitch(BlockPos pos, BlockState state) {
		super(ChromaBlockEntities.LIGHT_SWITCH.get(), pos, state);
	}

	public void setData(int level, int channel) {
		this.panelLevel = level;
		this.channel = channel;
		setChanged();
	}

	public void setDelegate(BlockPos pos) {
		delegate = pos.immutable();
		setChanged();
	}

	public void sendState(@Nullable Player player, boolean up) {
		if (level == null || delegate == null)
			return;
		if (level.getBlockEntity(delegate) instanceof Handler handler)
			handler.onLightSwitch(worldPosition, panelLevel, channel, up, player);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putInt("panelLevel", panelLevel);
		output.putInt("channel", channel);
		if (delegate != null)
			output.putLong("delegate", delegate.asLong());
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		panelLevel = input.getIntOr("panelLevel", 0);
		channel = input.getIntOr("channel", 0);
		delegate = input.getLong("delegate").map(BlockPos::of).orElse(null);
	}
}
