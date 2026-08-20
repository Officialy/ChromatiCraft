package reika.chromaticraft.tileentity.dimension;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import reika.chromaticraft.base.tileentity.TileEntityChromaticBase;
import reika.chromaticraft.registry.ChromaBlockEntities;
import reika.chromaticraft.registry.ChromaTiles;

/**
 * V33a's {@code TileGlowingCracks}: a block entity that exists only so the cracks have a renderer.
 *
 * <p>It holds no state and does nothing on either side — upstream's {@code GLOWCRACKS} case is an empty
 * {@code break} in every branch of {@code DimensionDecoTile}. What it does carry is a render bounding
 * box wide enough for the sheet the renderer paints: nine blocks across, centred on this one, so the
 * cracks do not blink out the moment their own block leaves the frustum.
 */
public class TileEntityGlowingCracks extends TileEntityChromaticBase {

	/** V33a's renderer draws from {@code -r} to {@code r+1} with {@code r == 4}. */
	public static final int RADIUS = 4;

	public TileEntityGlowingCracks(BlockPos pos, BlockState state) {
		super(ChromaBlockEntities.GLOWING_CRACKS.get(), pos, state);
	}

	@Override
	public ChromaTiles getTile() {
		return ChromaTiles.GLOWCRACKS;
	}

	/**
	 * Deliberately far wider than the sheet itself.
	 *
	 * <p>A box drawn to the exact extent of the geometry is the wrong box: the renderer is culled as
	 * soon as that box leaves the frustum, so a player standing on the cracks and looking along the
	 * ground loses them entirely — the anchor block is behind the camera while most of the sheet is not.
	 * Widening it costs nothing (the box is only a visibility test) and is what keeps the effect from
	 * blinking out at exactly the angles it is most visible from.
	 */
	@Override
	public net.minecraft.world.phys.AABB getRenderBoundingBox() {
		final int r = RADIUS * 4;
		return new net.minecraft.world.phys.AABB(worldPosition.getX() - r, worldPosition.getY() - 4,
				worldPosition.getZ() - r, worldPosition.getX() + r + 1,
				worldPosition.getY() + 8, worldPosition.getZ() + r + 1);
	}

	@Override
	public void updateEntity(Level world, BlockPos pos) {}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {}
}
