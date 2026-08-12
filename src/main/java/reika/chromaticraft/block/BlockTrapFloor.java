package reika.chromaticraft.block;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import reika.chromaticraft.block.worldgen26.BlockStructureShield;
import reika.chromaticraft.item.ItemManipulator;
import reika.chromaticraft.registry.ChromaSounds;

/** V33a's disguised 7/8-height floor, with the block below owning its hazards. */
public final class BlockTrapFloor extends Block {

	public enum Disguise implements StringRepresentable {
		SELF("self"), STONE_BRICKS("stone_bricks"), OAK_PLANKS("oak_planks"), STRUCTURE_STONE("structure_stone");

		private final String name;
		Disguise(String name) { this.name = name; }
		@Override public String getSerializedName() { return name; }
	}

	public static final EnumProperty<Disguise> DISGUISE = EnumProperty.create("disguise", Disguise.class);
	private static final VoxelShape FLOOR = box(0, 0, 0, 16, 14, 16);
	private final MapCodec<BlockTrapFloor> codec = MapCodec.unit(this);

	public BlockTrapFloor(BlockBehaviour.Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(DISGUISE, Disguise.SELF));
	}

	@Override public MapCodec<? extends BlockTrapFloor> codec() { return codec; }

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(DISGUISE);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return FLOOR;
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return FLOOR;
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
			Player player, InteractionHand hand, BlockHitResult hit) {
		if (!(stack.getItem() instanceof ItemManipulator))
			return InteractionResult.PASS;
		if (!level.isClientSide()) {
			Disguise[] values = Disguise.values();
			level.setBlock(pos, state.setValue(DISGUISE,
					values[(state.getValue(DISGUISE).ordinal() + 1) % values.length]), 3);
			ChromaSounds.CAST.playSoundAtBlock(level, pos, 0.5F, 0.75F);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
		for (int depth = 1; depth <= 4; depth++) {
			BlockState below = level.getBlockState(pos.below(depth));
			if (below.getBlock() instanceof BlockStructureShield shield)
				return shield.isUnbreakable(below) ? 0 : super.getDestroyProgress(state, player, level, pos);
		}
		return super.getDestroyProgress(state, player, level, pos);
	}

	@Override
	protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity,
			InsideBlockEffectApplier effects, boolean precise) {
		BlockState below = level.getBlockState(pos.below());
		if (below.is(Blocks.LAVA)) entity.lavaIgnite();
		else if (!below.getFluidState().isEmpty()) entity.clearFire();
		below.entityInside(level, pos.below(), entity, effects, precise);
	}

	@Override
	public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
		level.getBlockState(pos.below()).getBlock().stepOn(level, pos.below(),
				level.getBlockState(pos.below()), entity);
	}
}
