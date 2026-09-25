/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.block.crystal;

import java.util.Locale;
import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.base.CrystalTypeBlock;
import reika.chromaticraft.block.BlockCrystallineStone.StoneTypes;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.registry.CrystalElement;
import reika.chromaticraft.render.particle.ChromaParticle;

/**
 * V33a {@code BlockCrystalGlow}: a small glowing crystal bar (light 15) mounted on a plate of some base material,
 * stuck to the face of a block. It has no collision, falls off (dropping itself) if its support goes, and the
 * Manipulator turns it a quarter turn or, sneaking, cycles its crystal between the element's own colour,
 * iridescent and rainbow.
 *
 * <p>V33a kept the element in metadata and the base, facing, long axis and crystal mode in a tile entity. None of
 * that changes once placed except by the Manipulator, and all of it is only drawn, so: one block per element and
 * base (so drops, pick-block and names come for free), and the rest in the blockstate. The long axis only ever
 * mattered as parallel or perpendicular to the renderer's reference direction, which is {@link #SWAPPED}; a fresh
 * placement is swapped, because V33a started {@code longAxis} on that reference direction.
 */
public class BlockCrystalGlow extends CrystalTypeBlock {

	public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;
	public static final BooleanProperty SWAPPED = BooleanProperty.create("swapped");
	public static final EnumProperty<Mode> MODE = EnumProperty.create("mode", Mode.class);

	private static final VoxelShape[] SHAPES = new VoxelShape[6];

	static {
		double h = 2;
		double w = 4;
		SHAPES[Direction.UP.ordinal()] = Block.box(8 - w, 0, 8 - w, 8 + w, h, 8 + w);
		SHAPES[Direction.DOWN.ordinal()] = Block.box(8 - w, 16 - h, 8 - w, 8 + w, 16, 8 + w);
		SHAPES[Direction.EAST.ordinal()] = Block.box(0, 8 - w, 8 - w, h, 8 + w, 8 + w);
		SHAPES[Direction.WEST.ordinal()] = Block.box(16 - h, 8 - w, 8 - w, 16, 8 + w, 8 + w);
		SHAPES[Direction.SOUTH.ordinal()] = Block.box(8 - w, 8 - w, 0, 8 + w, 8 + w, h);
		SHAPES[Direction.NORTH.ordinal()] = Block.box(8 - w, 8 - w, 16 - h, 8 + w, 8 + w, 16);
	}

	private final Bases base;

	public BlockCrystalGlow(BlockBehaviour.Properties props, CrystalElement element, Bases base) {
		super(props, element);
		this.base = base;
		this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.UP).setValue(SWAPPED, true)
				.setValue(MODE, Mode.NORMAL));
	}

	public Bases getBase() {
		return base;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, SWAPPED, MODE);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockState state = this.defaultBlockState().setValue(FACING, context.getClickedFace());
		return this.canSurvive(state, context.getLevel(), context.getClickedPos()) ? state : null;
	}

	/** V33a {@code canPlaceOn}: the face it sits on must be solid (its slab special case is part of this test). */
	@Override
	protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		Direction facing = state.getValue(FACING);
		BlockPos support = pos.relative(facing.getOpposite());
		return level.getBlockState(support).isFaceSturdy(level, support, facing);
	}

	/** V33a {@code onNeighborBlockChange}: it falls off, dropping itself, when its support goes. */
	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
			Direction direction, BlockPos neighborPos, BlockState neighbor, RandomSource random) {
		if (direction == state.getValue(FACING).getOpposite() && !this.canSurvive(state, level, pos))
			return Blocks.AIR.defaultBlockState();
		return super.updateShape(state, level, ticks, pos, direction, neighborPos, neighbor, random);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPES[state.getValue(FACING).ordinal()];
	}

	/**
	 * V33a {@code randomDisplayTick}: a mote drifting in or out across the block, rising unless the glow hangs
	 * from a ceiling.
	 */
	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		ChromaParticle.spawnCrystalGlow(level, pos, this.getCrystalElement(), state.getValue(FACING) == Direction.DOWN,
				random);
	}

	/** V33a {@code TileEntityCrystalGlow.toggle} (sneaking Manipulator): own colour, then iridescent, then rainbow. */
	public static void toggle(Level level, BlockPos pos, BlockState state) {
		if (!(state.getBlock() instanceof BlockCrystalGlow glow))
			return;
		ding(level, pos, glow.getCrystalElement());
		level.setBlock(pos, state.setValue(MODE, state.getValue(MODE).next()), 3);
		playManipulatorSound(level, pos);
	}

	/** V33a {@code TileEntityCrystalGlow.rotate} (Manipulator): a quarter turn about the facing. */
	public static void rotate(Level level, BlockPos pos, BlockState state) {
		if (!(state.getBlock() instanceof BlockCrystalGlow glow))
			return;
		ding(level, pos, glow.getCrystalElement());
		level.setBlock(pos, state.setValue(SWAPPED, !state.getValue(SWAPPED)), 3);
		playManipulatorSound(level, pos);
	}

	/** V33a {@code ReikaSoundHelper.playBreakSound(world, x, y, z, Blocks.stone, 0.35F, 0.05F)}. */
	private static void playManipulatorSound(Level level, BlockPos pos) {
		level.playSound(null, pos, SoundEvents.STONE_BREAK, SoundSource.BLOCKS, 0.35F, 0.05F);
	}

	public static enum Mode implements StringRepresentable {
		NORMAL,
		IRIDESCENT,
		RAINBOW;

		public Mode next() {
			return switch (this) {
				case NORMAL -> IRIDESCENT;
				case IRIDESCENT -> RAINBOW;
				case RAINBOW -> NORMAL;
			};
		}

		@Override
		public String getSerializedName() {
			return this.name().toLowerCase(Locale.ROOT);
		}
	}

	/** V33a {@code BlockCrystalGlow.Bases}: the plate material, its recipe ingredient and its texture (side 0). */
	public static enum Bases {
		STONE("Stone", () -> Items.STONE, Identifier.withDefaultNamespace("block/stone")),
		CRYSTALSTONE("Crystal Stone", () -> ChromaBlocks.crystallineStone(StoneTypes.SMOOTH).get(),
				Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "block/pylon/block_0")),
		IRON("Iron", () -> Items.IRON_INGOT, Identifier.withDefaultNamespace("block/iron_block")),
		GOLD("Gold", () -> Items.GOLD_INGOT, Identifier.withDefaultNamespace("block/gold_block")),
		OBSIDIAN("Obsidian", () -> Items.OBSIDIAN, Identifier.withDefaultNamespace("block/obsidian")),
		QUARTZ("Quartz", () -> Items.QUARTZ_BLOCK, Identifier.withDefaultNamespace("block/quartz_block_bottom"));

		public static final Bases[] baseList = values();

		public final String displayName;
		public final Supplier<ItemLike> ingredient;
		public final Identifier texture;

		private Bases(String s, Supplier<ItemLike> ingredient, Identifier texture) {
			displayName = s;
			this.ingredient = ingredient;
			this.texture = texture;
		}

		public String id() {
			return this.name().toLowerCase(Locale.ROOT);
		}
	}

}
