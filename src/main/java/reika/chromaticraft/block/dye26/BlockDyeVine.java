package reika.chromaticraft.block.dye26;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import reika.chromaticraft.registry.CrystalElement;
import reika.dragonapi.libraries.registry.ReikaDyeHelper;
import reika.dragonapi.libraries.rendering.ReikaColorAPI;

/**
 * V33a {@code BlockDyeVine}: the hanging strands under Rainbow Forest dye trees, one registered block per
 * former metadata colour, in an ordinary and a fertile kind.
 *
 * <p>A vine hangs only from a dye leaf or another dye vine of its own colour directly above it, which is
 * upstream's {@code canBlockStay}; losing that support drops the vine, and the vine below it then loses
 * its own, so a cut strand falls apart from the cut down. Upstream extended {@code BlockTallGrass}, so a
 * vine is replaceable and has no collision.
 *
 * <p>Fertile vines are the bottom tips of some strands. They carry an extra full-bright overlay in their
 * crystal colour and are the only ones that drop crystal shards. Drops are in the loot tables; the
 * geometry is {@code DyeVineModel}.
 */
public final class BlockDyeVine extends VegetationBlock {

	/** V33a {@code setBlockBoundsBasedOnState}: 0.1-0.9 across, full height. */
	private static final VoxelShape SHAPE = Block.box(1.6, 0, 1.6, 14.4, 16, 14.4);

	/** V33a mixes the leaf colour toward this grey. */
	public static final int SHADE = 0x404040;

	private final CrystalElement element;
	private final boolean fertile;
	private final MapCodec<BlockDyeVine> codec = MapCodec.unit(this);

	public BlockDyeVine(BlockBehaviour.Properties properties, CrystalElement element, boolean fertile) {
		super(properties);
		this.element = element;
		this.fertile = fertile;
	}

	public CrystalElement getElement() {
		return element;
	}

	public boolean isFertile() {
		return fertile;
	}

	@Override
	protected MapCodec<? extends BlockDyeVine> codec() {
		return codec;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	/** V33a {@code canBlockStay}: a dye leaf or dye vine of the same colour directly above. */
	@Override
	protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		return canHangBelow(level.getBlockState(pos.above()), element);
	}

	public static boolean canHangBelow(BlockState above, CrystalElement element) {
		if (above.getBlock() instanceof BlockDyeLeaf leaf)
			return leaf.getElement() == element;
		if (above.getBlock() instanceof BlockDyeVine vine)
			return vine.element == element;
		return false;
	}

	/**
	 * V33a {@code colorMultiplier}: the dye leaf's colour mixed 70% toward it with the grey. The world
	 * renderer replaces the 0.7 with a per-position 0.675-0.875, which is {@code DyeVineModel}'s job.
	 */
	public int getTintColor() {
		return 0xFF000000 | ReikaColorAPI.mixColors(leafColor(element), SHADE, 0.7F);
	}

	/** V33a {@code BlockDyeLeaf.getColor(meta, false)}. */
	public static int leafColor(CrystalElement element) {
		return ReikaDyeHelper.dyes[element.ordinal()].getJavaColor().brighter().getRGB() & 0xFFFFFF;
	}
}
