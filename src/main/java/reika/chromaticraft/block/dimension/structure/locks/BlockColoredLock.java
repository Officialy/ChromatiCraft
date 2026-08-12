package reika.chromaticraft.block.dimension.structure.locks;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import reika.chromaticraft.registry.ChromaItems;
import reika.chromaticraft.registry.ChromaBlockEntities;
import reika.chromaticraft.registry.CrystalElement;
import reika.chromaticraft.tileentity.TileEntityColorLock;

/** V33a colored lock: solid while any required color remains closed, intangible when solved. */
public final class BlockColoredLock extends Block implements EntityBlock {

	public static final BooleanProperty OPEN = BooleanProperty.create("open");
	public static final BooleanProperty GATE = BooleanProperty.create("gate");
	private final MapCodec<BlockColoredLock> codec = MapCodec.unit(this);

	public BlockColoredLock(BlockBehaviour.Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(OPEN, false).setValue(GATE, false));
	}

	@Override public MapCodec<? extends BlockColoredLock> codec() { return codec; }

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(OPEN, GATE);
	}

	@Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new TileEntityColorLock(pos, state);
	}

	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
			BlockEntityType<T> type) {
		if (level.isClientSide() || type != ChromaBlockEntities.COLOR_LOCK.get()) return null;
		return (tickLevel, pos, tickState, entity) ->
				TileEntityColorLock.serverTick(tickLevel, pos, tickState, (TileEntityColorLock)entity);
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
			CollisionContext context) {
		return state.getValue(OPEN) ? Shapes.empty() : Shapes.block();
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
			Player player, InteractionHand hand, BlockHitResult hit) {
		if (!player.isCreative() || !(level.getBlockEntity(pos) instanceof TileEntityColorLock lock))
			return InteractionResult.PASS;
		for (CrystalElement element : CrystalElement.elements) {
			if (stack.is(ChromaItems.SHARDS.get(element).get())) {
				if (!level.isClientSide()) lock.addColor(element);
				return InteractionResult.SUCCESS;
			}
		}
		if (stack.is(Blocks.OBSIDIAN.asItem())) {
			if (!level.isClientSide()) level.setBlock(pos, state.setValue(GATE, true), 3);
			return InteractionResult.SUCCESS;
		}
		return InteractionResult.PASS;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
			Player player, BlockHitResult hit) {
		if (player.isCreative() && player.isShiftKeyDown()
				&& level.getBlockEntity(pos) instanceof TileEntityColorLock lock) {
			if (!level.isClientSide()) lock.clearColors();
			return InteractionResult.SUCCESS;
		}
		return InteractionResult.PASS;
	}

	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (!state.getValue(OPEN) || state.getValue(GATE)
				|| !(level.getBlockEntity(pos) instanceof TileEntityColorLock lock))
			return;
		CrystalElement element = lock.randomColor(random);
		if (element != null) {
			int color = element.getColor();
			level.addParticle(new DustParticleOptions(color, 1F), pos.getX() + random.nextDouble(),
					pos.getY() + random.nextDouble(), pos.getZ() + random.nextDouble(), 0, 0, 0);
		}
	}
}
