package reika.chromaticraft.block.dimension;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import reika.chromaticraft.magic.progression.ProgressStage;
import reika.chromaticraft.tileentity.dimension.TileEntityGlowingCracks;

/**
 * V33a's Glowing Cracks: the seam of light in the ground that a Radiant Fissures biome is named for.
 *
 * <p>The block itself is almost nothing — {@code BlockBounds.block().cut(UP, 0.999)}, a sliver a
 * thousandth of a block thick — because none of what you see is the block. The whole appearance is
 * {@link reika.chromaticraft.render.tesr.dimension.RenderGlowingCracks} painting a nine-by-nine sheet
 * of {@code glowcracks.png} across the ground around it, hue-cycling on its own clock.
 *
 * <p>That is also why it takes a block entity for what is otherwise a decoration: a block entity is
 * what gets a renderer, and the renderer is the block.
 *
 * <p>It shares {@code BlockDimensionDecoTile} with the Fire Jet upstream and inherits its mining gate —
 * only a player past {@code ProgressStage.CTM} can break one, expressed as destroy progress so
 * explosions and other mods' miners are covered as well.
 */
public class BlockGlowingCracks extends BaseEntityBlock {

	public static final MapCodec<BlockGlowingCracks> CODEC = simpleCodec(BlockGlowingCracks::new);

	/** V33a {@code BlockBounds.block().cut(ForgeDirection.UP, 0.999)}. */
	private static final VoxelShape SHAPE = Shapes.box(0, 0, 0, 1, 0.001, 1);

	public BlockGlowingCracks(BlockBehaviour.Properties properties) {
		super(properties);
	}

	@Override
	protected MapCodec<? extends BaseEntityBlock> codec() {
		return CODEC;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new TileEntityGlowingCracks(pos, state);
	}

	/**
	 * Nothing is drawn from the model. The renderer is the whole of it, and a cube here would sit as a
	 * visible slab under the light.
	 */
	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return RenderShape.INVISIBLE;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
			CollisionContext context) {
		return SHAPE;
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
			CollisionContext context) {
		return Shapes.empty();
	}

	/** V33a getPlayerRelativeBlockHardness: nothing before CTM. */
	@Override
	protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
		return ProgressStage.CTM.isPlayerAtStage(player)
				? super.getDestroyProgress(state, player, level, pos) : 0;
	}
}
