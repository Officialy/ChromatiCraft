package reika.chromaticraft.block.dimension;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.FluidState;

import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.registry.CrystalElement;
import reika.chromaticraft.tileentity.technical.TileEntityDimensionCore;

/**
 * V33a's Dimension Core block, registered once per crystal element.
 *
 * <p>Upstream has one block whose sixteen colours live on its tile entity. Here each colour is its own
 * registry identity, which is the port's rule for every one of V33a's colour-carrying blocks and what
 * makes the sixteen distinguishable as items at all — a core is a thing you place from an inventory,
 * so a colour held only on the tile would leave sixteen identical stacks and one nameless block.
 *
 * <p>Everything else that makes a core a core — which structure it belongs to, who placed it, whether
 * it is sealed or primed — still lives on the block entity, as upstream has it.
 *
 * <p>{@link #getDestroyProgress} is the seal. A core inside an unsolved puzzle structure returns zero,
 * which is how vanilla expresses "this cannot be mined at all" without a special case at every break
 * site; the same answer covers explosions and other mods' miners, which a break event alone would not.
 */
public class BlockDimensionCore extends BaseEntityBlock {

	public static final MapCodec<BlockDimensionCore> CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance.group(
					propertiesCodec(),
					CrystalElement.CODEC.fieldOf("element").forGetter(BlockDimensionCore::getElement)
			).apply(instance, BlockDimensionCore::new));

	private final CrystalElement element;

	public BlockDimensionCore(BlockBehaviour.Properties properties, CrystalElement element) {
		super(properties);
		this.element = element;
	}

	/** The colour this registry identity is. The block entity reads its colour from here. */
	public CrystalElement getElement() {
		return element;
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
	 * Upstream draws a core entirely through {@code RenderDimensionCore}; the block model is therefore
	 * intentionally invisible and exists only to supply a particle sprite.
	 */
	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return RenderShape.INVISIBLE;
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
		// The colour is the block's identity now, so only the placer has to be taken from the placement.
		// doChecks refuses a ring whose cores do not all record the same one.
		if (placer instanceof Player player)
			core.setPlacer(player);
		// V33a ItemChromaPlacer primed every Dimension Core immediately after placing it. The
		// modern BlockItem path comes through setPlacedBy instead, so this is the equivalent hook:
		// each newly inserted core joins the monument's staged note sequence, and removing/ritual
		// shutdown still calls prime(false) through the controller lifecycle.
		core.prime(true);
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
		return new net.minecraft.world.item.ItemStack(ChromaBlocks.dimensionCore(element).get());
	}

	@Override
	protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
		if (level.getBlockEntity(pos) instanceof TileEntityDimensionCore core && !core.isBreakable(player))
			return 0;
		return super.getDestroyProgress(state, player, level, pos);
	}

	@Override
	public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player,
			ItemStack toolStack, boolean willHarvest, FluidState fluid) {
		if (level.getBlockEntity(pos) instanceof TileEntityDimensionCore core
				&& !core.breakByPlayer(player))
			return false;
		return super.onDestroyedByPlayer(state, level, pos, player, toolStack, willHarvest, fluid);
	}
}
