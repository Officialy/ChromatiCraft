package reika.chromaticraft.tileentity.dimension;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import reika.chromaticraft.base.tileentity.TileEntityChromaticBase;
import reika.chromaticraft.block.dimension.BlockVoidRift;
import reika.chromaticraft.registry.ChromaBlockEntities;
import reika.chromaticraft.registry.ChromaTiles;
import reika.chromaticraft.registry.CrystalElement;

/**
 * V33a {@code BlockVoidRift.TileEntityVoidRift}: what the rift's aura renderer reads.
 *
 * <p>It does nothing on its own — upstream's {@code canUpdate()} returns false, so the particle burst
 * its {@code updateEntity} would spawn is dead code in V33a and is not reproduced here. What the tile is
 * actually for is {@link #getAt}: the renderer asks each of the four horizontal neighbours whether it is
 * another rift and of which colour, so a run of rifts blends its aura along the seam instead of showing
 * a hard edge at every block. That lookup is cached, because it is asked every frame and the answer
 * cannot change without the block changing.
 *
 * <p>The renderer that consumes it is not ported: its texture is fetched at runtime from Reika's server
 * rather than shipped. See {@link BlockVoidRift}.
 */
public class TileEntityVoidRift extends TileEntityChromaticBase {

	/** V33a HEIGHT: how far above the rift the aura reaches, and so how far its render box extends. */
	public static final int HEIGHT = 16;

	/** The 3x3 of neighbours the aura pass reads, indexed by offset+1. Null means "not asked yet". */
	private final BlockState[][] blockCache = new BlockState[3][3];

	public TileEntityVoidRift(BlockPos pos, BlockState state) {
		super(ChromaBlockEntities.VOID_RIFT.get(), pos, state);
	}

	@Override
	public ChromaTiles getTile() {
		return ChromaTiles.VOIDRIFT;
	}

	/** V33a getColor: the colour is the block's own identity in this port, not its metadata. */
	public CrystalElement getColor() {
		return this.getBlockState().getBlock() instanceof BlockVoidRift rift
				? rift.getElement() : CrystalElement.WHITE;
	}

	/** V33a getAt: the cached neighbour state one block out in the given horizontal offset. */
	public BlockState getAt(int dx, int dz) {
		BlockState cached = blockCache[dx + 1][dz + 1];
		if (cached == null && this.getLevel() != null) {
			cached = this.getLevel().getBlockState(worldPosition.offset(dx, 0, dz));
			blockCache[dx + 1][dz + 1] = cached;
		}
		return cached;
	}

	/** V33a hasAt: whether that neighbour is another rift, of any colour. */
	public boolean hasAt(int dx, int dz) {
		BlockState at = this.getAt(dx, dz);
		return at != null && at.getBlock() instanceof BlockVoidRift;
	}

	/** The colour of the neighbouring rift on that side, or null if there is not one. */
	public CrystalElement colorAt(Direction dir) {
		BlockState at = this.getAt(dir.getStepX(), dir.getStepZ());
		return at != null && at.getBlock() instanceof BlockVoidRift rift ? rift.getElement() : null;
	}

	/**
	 * V33a getRenderBoundingBox: the aura reaches {@value #HEIGHT} blocks above the rift, so the box has
	 * to as well or the whole thing vanishes the moment the rift's own block leaves the frustum.
	 */
	@Override
	public net.minecraft.world.phys.AABB getRenderBoundingBox() {
		return new net.minecraft.world.phys.AABB(worldPosition.getX(), worldPosition.getY(),
				worldPosition.getZ(), worldPosition.getX() + 1, worldPosition.getY() + 1 + HEIGHT,
				worldPosition.getZ() + 1);
	}

	@Override
	public void updateEntity(Level world, BlockPos pos) {}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {}
}
