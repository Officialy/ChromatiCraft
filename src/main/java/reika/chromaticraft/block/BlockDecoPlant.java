/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.registry.ChromaTiles;

/**
 * Modern distinct-block form of V33a {@code DECOPLANT} metadata. The authored backing/overlay
 * layers are data-generated; {@link #CROP_FORM} preserves the encased-vine render recursion.
 */
public final class BlockDecoPlant extends BlockChromaticTile {

	public static final BooleanProperty CROP_FORM = BooleanProperty.create("crop_form");
	private static final VoxelShape FULL_SHAPE = Shapes.block();
	private static final VoxelShape NO_COLLISION = Shapes.empty();

	private final Kind kind;

	public BlockDecoPlant(Properties properties, Kind kind) {
		super(properties, kind.tile());
		this.kind = kind;
		this.registerDefaultState(stateDefinition.any().setValue(CROP_FORM, false));
	}

	public Kind kind() { return kind; }

	@Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(CROP_FORM);
	}

	@Override public BlockState getStateForPlacement(BlockPlaceContext context) {
		return this.defaultBlockState().setValue(CROP_FORM,
				this.shouldUseCropForm(context.getLevel(), context.getClickedPos()));
	}

	@Override protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos,
			CollisionContext context) {
		return FULL_SHAPE;
	}

	@Override protected VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos,
			CollisionContext context) {
		return kind == Kind.ACCELERATOR && state.getValue(CROP_FORM) ? FULL_SHAPE : NO_COLLISION;
	}

	@Override
	protected boolean canSurvive(BlockState state, LevelReader world, BlockPos pos) {
		return switch (kind) {
			case ACCELERATOR -> canAccelerationPlantSurvive(world, pos);
			case HARVEST -> {
				BlockState below = world.getBlockState(pos.below());
				yield below.is(BlockTags.SUPPORTS_VEGETATION) || below.is(BlockTags.LEAVES)
						|| below.is(ChromaBlocks.PLANT_ACCELERATOR.get());
			}
		};
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader world, ScheduledTickAccess ticks,
			BlockPos pos, Direction directionToNeighbour, BlockPos neighbourPos, BlockState neighbourState,
			RandomSource random) {
		if (!this.canSurvive(state, world, pos)) return Blocks.AIR.defaultBlockState();
		return state.setValue(CROP_FORM, this.shouldUseCropForm(world, pos));
	}

	private boolean shouldUseCropForm(BlockGetter world, BlockPos pos) {
		if (kind == Kind.ACCELERATOR) return isEncased(world, pos);
		BlockState support = world.getBlockState(pos.below());
		return support.getBlock() instanceof BlockDecoPlant && support.getValue(CROP_FORM);
	}

	public static boolean isEncased(BlockGetter world, BlockPos pos) {
		for (Direction direction : Direction.Plane.HORIZONTAL) {
			BlockPos adjacent = pos.relative(direction);
			BlockState state = world.getBlockState(adjacent);
			if (!state.is(ChromaBlocks.PLANT_ACCELERATOR.get())
					&& state.getCollisionShape(world, adjacent).isEmpty()) return false;
		}
		return true;
	}

	public static boolean canAccelerationPlantSurvive(LevelReader world, BlockPos pos) {
		if (isViableAnchor(world, pos.above()) || isViableAnchor(world, pos.below())) return true;
		BlockPos top = pos;
		while (world.getBlockState(top.above()).is(ChromaBlocks.PLANT_ACCELERATOR.get())) top = top.above();
		BlockPos bottom = pos;
		while (world.getBlockState(bottom.below()).is(ChromaBlocks.PLANT_ACCELERATOR.get())) bottom = bottom.below();
		return (top.getY() != pos.getY() || bottom.getY() != pos.getY())
				&& (isViableAnchor(world, top.above()) || isViableAnchor(world, bottom.below()));
	}

	private static boolean isViableAnchor(BlockGetter world, BlockPos pos) {
		BlockState state = world.getBlockState(pos);
		return state.is(BlockTags.LEAVES) || state.isSolidRender()
				|| state.isCollisionShapeFullBlock(world, pos);
	}

	@Override
	public void animateTick(BlockState state, Level world, BlockPos pos, RandomSource random) {
		if (kind != Kind.HARVEST) return;
		world.addParticle(new DustParticleOptions(0xFFFF00, 1F),
				pos.getX() + random.nextDouble(), pos.getY() + random.nextDouble(),
				pos.getZ() + random.nextDouble(), 0, 0, 0);
	}

	@Override
	protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity,
			InsideBlockEffectApplier effects, boolean precise) {
		if (kind == Kind.HARVEST && entity instanceof LivingEntity living) {
			entity.hurt(entity.damageSources().cactus(), 1);
			living.addEffect(new MobEffectInstance(MobEffects.HUNGER, 20, 1));
		}
	}

	public enum Kind {
		ACCELERATOR(ChromaTiles.PLANTACCEL, 3),
		HARVEST(ChromaTiles.HARVESTPLANT, 5);

		private final ChromaTiles tile;
		private final int textureIndex;

		Kind(ChromaTiles tile, int textureIndex) {
			this.tile = tile;
			this.textureIndex = textureIndex;
		}

		public ChromaTiles tile() { return tile; }
		public int textureIndex() { return textureIndex; }
	}
}
