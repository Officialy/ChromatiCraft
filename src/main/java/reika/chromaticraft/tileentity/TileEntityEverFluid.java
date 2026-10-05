package reika.chromaticraft.tileentity;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import reika.chromaticraft.registry.ChromaBlockEntities;

/**
 * V33a {@code BlockEverFluid.TileEntityEverFluid}: which source a cell of Channeled Flow fluid belongs
 * to, and which floor it is on. A cell that loses its connection to its source drains away.
 */
public final class TileEntityEverFluid extends BlockEntity {

	private @Nullable BlockPos sourceLocation;
	private int level;

	public TileEntityEverFluid(BlockPos pos, BlockState state) {
		super(ChromaBlockEntities.EVER_FLUID.get(), pos, state);
	}

	public @Nullable BlockPos sourceLocation() { return sourceLocation; }
	public int floor() { return level; }

	public void setSource(@Nullable BlockPos source) {
		sourceLocation = source == null ? null : source.immutable();
		setChanged();
	}

	public void setFloor(int floor) {
		level = floor;
		setChanged();
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		if (sourceLocation != null)
			output.store("loc", BlockPos.CODEC, sourceLocation);
		output.putInt("lvl", level);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		sourceLocation = input.read("loc", BlockPos.CODEC).orElse(null);
		level = input.getIntOr("lvl", 0);
	}
}
