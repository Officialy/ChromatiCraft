package reika.chromaticraft.tileentity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import reika.chromaticraft.base.tileentity.TileEntityChromaticBase;
import reika.chromaticraft.registry.ChromaBlockEntities;
import reika.chromaticraft.registry.ChromaTiles;

public class TileEntityDisplayPoint extends TileEntityChromaticBase {

	public TileEntityDisplayPoint(BlockPos pos, BlockState state) {
		super(ChromaBlockEntities.DISPLAY.get(), pos, state);
	}

	@Override
	public ChromaTiles getTile() {
		return ChromaTiles.DISPLAY;
	}

	@Override
	public void updateEntity(Level world, BlockPos pos) {

	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {

	}
}
