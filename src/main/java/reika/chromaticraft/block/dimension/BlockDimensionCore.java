package reika.chromaticraft.block.dimension;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.RenderShape;

import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.registry.CrystalElement;
import reika.chromaticraft.tileentity.technical.TileEntityDimensionCore;

/**
 * V33a's Dimension Core block.
 *
 * <p>The block itself is deliberately thin: everything that makes a core a core — its colour, which
 * structure it belongs to, whether it is sealed — lives on the block entity, because upstream's single
 * block carried all sixteen colours through its tile rather than through metadata.
 *
 * <p>{@link #getDestroyProgress} is the seal. A core inside an unsolved puzzle structure returns zero,
 * which is how vanilla expresses "this cannot be mined at all" without a special case at every break
 * site; the same answer covers explosions and other mods' miners, which a break event alone would not.
 */
public class BlockDimensionCore extends BaseEntityBlock {

	public static final MapCodec<BlockDimensionCore> CODEC = simpleCodec(BlockDimensionCore::new);

	public BlockDimensionCore(BlockBehaviour.Properties properties) {
		super(properties);
	}

	@Override
	protected MapCodec<? extends BaseEntityBlock> codec() {
		return CODEC;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new TileEntityDimensionCore(pos, state);
	}

	/**
	 * Upstream draws a core entirely through {@code RenderDimensionCore}, which is glow-knot geometry
	 * and the DIMCORE shader with no texture at all — {@code getImageFileName} returns null. Neither
	 * that renderer nor DragonAPI's {@code GlowKnot} is ported, and returning INVISIBLE without one
	 * makes the block impossible to see, which makes the sixteen-core ring impossible to build.
	 *
	 * <p>So it draws as a cube of Reika's own {@code roundflare} sprite, tinted with the core's colour
	 * by {@code ChromaBlockColors}. That is a stand-in for the renderer and is marked as one; it invents
	 * no art, and it makes each core readable at a glance, which is the whole point of a coloured ring.
	 */
	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

	/**
	 * V33a's core carries its colour in the item's stack tag and its owner from whoever placed it. Both
	 * have to survive the placement or the ritual refuses: {@code doChecks} reads the colour to match it
	 * against the ring position, and the placer to insist every core came from one person.
	 */
	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state,
			net.minecraft.world.entity.LivingEntity placer, net.minecraft.world.item.ItemStack stack) {
		super.setPlacedBy(level, pos, state, placer, stack);
		if (!(level.getBlockEntity(pos) instanceof TileEntityDimensionCore core))
			return;
		core.setDataFromItemStackTag(stack);
		if (placer instanceof Player player)
			core.setPlacer(player);
	}

	/**
	 * Pick-block returns the core's own colour rather than a blank one, so a ring can be built by
	 * copying a placed core instead of hunting the right entry out of sixteen in the creative menu.
	 */
	@Override
	public net.minecraft.world.item.ItemStack getCloneItemStack(
			net.minecraft.world.level.LevelReader level, BlockPos pos, BlockState state,
			boolean includeData, Player player) {
		return level.getBlockEntity(pos) instanceof TileEntityDimensionCore core
				? of(core.getColor())
				: super.getCloneItemStack(level, pos, state, includeData, player);
	}

	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
			BlockEntityType<T> type) {
		// Both halves are required: updateEntity() runs the base lifecycle -- first tick, sync,
		// callbacks -- and updateEntity(level, pos) is this tile's own body.
		return (world, pos, blockState, be) -> {
			if (be instanceof TileEntityDimensionCore core) {
				core.updateEntity();
				core.updateEntity(world, pos);
			}
		};
	}

	/**
	 * A core of one colour, as the creative menu and the ritual both want it. V33a keeps the colour in
	 * the stack's tag rather than in sixteen registered items, so this is one item wearing a colour —
	 * and the display name carries that colour, because sixteen identically-named cores would be
	 * unplaceable in a ring that cares which is which.
	 */
	public static net.minecraft.world.item.ItemStack of(CrystalElement element) {
		net.minecraft.world.item.ItemStack stack =
				new net.minecraft.world.item.ItemStack(ChromaBlocks.DIMENSION_CORE.get());
		reika.dragonapi.libraries.registry.ReikaItemHelper.updateStackTag(stack,
				tag -> tag.putInt("color", element.ordinal()));
		stack.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,
				net.minecraft.network.chat.Component.translatable("block.chromaticraft.dimension_core")
						.append(" (" + element.displayName() + ")"));
		return stack;
	}

	@Override
	protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
		if (level.getBlockEntity(pos) instanceof TileEntityDimensionCore core && !core.isBreakable(player))
			return 0;
		return super.getDestroyProgress(state, player, level, pos);
	}
}
