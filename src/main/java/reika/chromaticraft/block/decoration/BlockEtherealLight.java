package reika.chromaticraft.block.decoration;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

import reika.chromaticraft.render.particle.ChromaParticle;

/**
 * V33a's Ethereal Light: light with nothing there.
 *
 * <p>The block is air in every way that matters — no collision, no model, replaceable, dropping
 * nothing, and reporting itself as air to anything that asks — while emitting full light. It is what
 * lights a glowing cave from the inside without a lamp being visible anywhere in it.
 *
 * <p>Its colour is a function of height alone: pure red hue-rotated by {@code 360 * y / seaLevel}, so a
 * cave lit by these shifts through the spectrum as it descends and every light at one depth agrees.
 * Nothing else in the mod picks its colour that way, and it is what makes the caves read as layered.
 *
 * <h2>The flags</h2>
 *
 * <p>Upstream packs four independent flags into the metadata, and they are independent — a light can
 * decay slowly <em>and</em> throw particles — so they are four boolean properties rather than variants.
 * {@code SLOWDECAY} re-schedules on every neighbour change and then dies one time in five;
 * {@code FASTDECAY} dies at the first neighbour change and is scheduled the moment it is placed.
 * {@code MINEABLE} is upstream's own dead letter: every branch that would have dropped an item for it is
 * commented out in V33a, so it is carried for the generator's sake and grants nothing.
 */
public class BlockEtherealLight extends Block {

	public static final MapCodec<BlockEtherealLight> CODEC = simpleCodec(BlockEtherealLight::new);

	/** V33a Flags.MINEABLE — recorded, and deliberately without effect; see the class note. */
	public static final BooleanProperty MINEABLE = BooleanProperty.create("mineable");
	/** V33a Flags.PARTICLES. */
	public static final BooleanProperty PARTICLES = BooleanProperty.create("particles");
	/** V33a Flags.SLOWDECAY. */
	public static final BooleanProperty SLOW_DECAY = BooleanProperty.create("slow_decay");
	/** V33a Flags.FASTDECAY. */
	public static final BooleanProperty FAST_DECAY = BooleanProperty.create("fast_decay");

	public BlockEtherealLight(BlockBehaviour.Properties properties) {
		super(properties);
		this.registerDefaultState(this.stateDefinition.any().setValue(MINEABLE, false)
				.setValue(PARTICLES, false).setValue(SLOW_DECAY, false).setValue(FAST_DECAY, false));
	}

	@Override
	protected MapCodec<? extends Block> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(MINEABLE, PARTICLES, SLOW_DECAY, FAST_DECAY);
	}

	/** V33a isAir: it reports itself as air, which is what lets anything build straight through it. */
	@Override
	protected boolean isRandomlyTicking(BlockState state) {
		return state.getValue(SLOW_DECAY) || state.getValue(FAST_DECAY);
	}

	/** V33a updateTick: a fast light always dies, a slow one dies one time in five. */
	@Override
	protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (state.getValue(FAST_DECAY) || (state.getValue(SLOW_DECAY) && random.nextInt(5) == 0))
			level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
	}

	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (state.getValue(FAST_DECAY) || (state.getValue(SLOW_DECAY) && random.nextInt(5) == 0))
			level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
	}

	/** V33a onBlockAdded: a fast-decay light is already dying when it is placed. */
	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState,
			boolean movedByPiston) {
		if (state.getValue(FAST_DECAY))
			level.scheduleTick(pos, this, 15 + level.getRandom().nextInt(16));
	}

	/**
	 * V33a onNeighborBlockChange: a slow light re-arms its timer whenever anything near it changes; a
	 * fast one simply goes out.
	 */
	@Override
	protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block,
			@javax.annotation.Nullable net.minecraft.world.level.redstone.Orientation orientation,
			boolean movedByPiston) {
		if (state.getValue(SLOW_DECAY))
			level.scheduleTick(pos, this, 40 + level.getRandom().nextInt(161));
		else if (state.getValue(FAST_DECAY))
			level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
	}

	/** V33a randomDisplayTick: one drifting mote every other tick, in the light's own colour. */
	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (state.getValue(PARTICLES) && random.nextInt(2) == 0)
			ChromaParticle.spawnEtherealLight(level, pos);
	}

	/**
	 * V33a getParticleColor: red, hue-rotated by the fraction of sea level this sits at. Upstream reads
	 * {@code provider.getAverageGroundLevel()}, which 26.2 spells as the level's sea level.
	 */
	public static int particleColor(Level level, int y) {
		int seaLevel = Math.max(1, level.getSeaLevel());
		float[] hsb = java.awt.Color.RGBtoHSB(255, 0, 0, null);
		return java.awt.Color.HSBtoRGB((float)(hsb[0] + y / (double)seaLevel), hsb[1], hsb[2]) & 0xFFFFFF;
	}
}
