package reika.chromaticraft.block.dimension;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

import reika.chromaticraft.render.particle.ChromaParticle;

/**
 * V33a's Void Cave: the bedrock lip a glowing cave's floor falls away at, and the light that pours
 * over it.
 *
 * <p>Unbreakable and blast-proof — this is the floor of the world in a cave that has none, and there is
 * nothing under it to fall into. The generator sets one wherever a bedrock wall at y 2 has open air
 * beside it and below that, so a void cave block only ever appears at an actual edge.
 *
 * <p>Its four horizontal flags are upstream's metadata bitfield, one bit per direction, saying which
 * sides the drop is on. Each set side pours a slow fall of blue-white light outward and downward — the
 * cave's one moving thing. As a bitfield it is genuinely blockstate data rather than a variant: the same
 * block wearing a different connectivity, exactly as a fence does.
 */
public class BlockVoidCave extends Block {

	public static final MapCodec<BlockVoidCave> CODEC = simpleCodec(BlockVoidCave::new);

	public static final BooleanProperty NORTH = BooleanProperty.create("north");
	public static final BooleanProperty SOUTH = BooleanProperty.create("south");
	public static final BooleanProperty WEST = BooleanProperty.create("west");
	public static final BooleanProperty EAST = BooleanProperty.create("east");

	public BlockVoidCave(BlockBehaviour.Properties properties) {
		super(properties);
		this.registerDefaultState(this.stateDefinition.any().setValue(NORTH, false)
				.setValue(SOUTH, false).setValue(WEST, false).setValue(EAST, false));
	}

	@Override
	protected MapCodec<? extends Block> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(NORTH, SOUTH, WEST, EAST);
	}

	/** The property for one horizontal side, matching V33a's bit {@code 1 << (side-2)}. */
	public static BooleanProperty property(Direction dir) {
		return switch (dir) {
			case NORTH -> NORTH;
			case SOUTH -> SOUTH;
			case WEST -> WEST;
			case EAST -> EAST;
			default -> throw new IllegalArgumentException("Void cave edges are horizontal: " + dir);
		};
	}

	/** Whether this state has any edge at all; a void cave with none emits nothing. */
	public static boolean hasAnyEdge(BlockState state) {
		for (Direction dir : Direction.Plane.HORIZONTAL)
			if (state.getValue(property(dir)))
				return true;
		return false;
	}

	/**
	 * V33a randomDisplayTick: sixteen particles per set side per tick, drifting outward and falling.
	 * The colour is a white-to-blue mix, hue-shifted up to thirty degrees either way and dimmed by up to
	 * half, so the fall reads as light rather than as a texture.
	 */
	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		for (Direction dir : Direction.Plane.HORIZONTAL)
			if (state.getValue(property(dir)))
				ChromaParticle.spawnVoidCaveFall(level, pos, dir);
	}
}
