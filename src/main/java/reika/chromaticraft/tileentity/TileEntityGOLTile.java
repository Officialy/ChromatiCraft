package reika.chromaticraft.tileentity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import org.jspecify.annotations.Nullable;

import reika.chromaticraft.registry.ChromaBlockEntities;

/** Persistent controller binding for one interactive Cellular-Automata floor cell. */
public final class TileEntityGOLTile extends BlockEntity {

	private @Nullable BlockPos controller;

	public TileEntityGOLTile(BlockPos pos, BlockState state) {
		super(ChromaBlockEntities.GOL_TILE.get(), pos, state);
	}

	public void bind(BlockPos controllerPos) {
		controller = controllerPos.immutable();
		setChanged();
	}

	public @Nullable BlockPos controller() { return controller; }

	public void requestToggle() {
		if (level != null && controller != null
				&& level.getBlockEntity(controller) instanceof TileEntityGOLController owner)
			owner.toggleCell(worldPosition);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		if (controller != null) output.putLong("controller", controller.asLong());
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		controller = input.getLong("controller").map(BlockPos::of).orElse(null);
	}
}
