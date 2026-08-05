package reika.chromaticraft.block.worldgen26;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * V33a {@code BlockCaveIndicator}, the Piezo Crystal: a stone-looking block that lights up and
 * emits redstone for a while when something steps on it.
 *
 * <p>V33a stores that on/off in block metadata, but this is the case the concrete-identity rule
 * explicitly exempts — it is one object with genuine runtime state, not a variant family — so it
 * becomes an ordinary boolean blockstate rather than two registered blocks.
 *
 * <p>Behaviour retained from source: light 10 while active and 0 otherwise, weak redstone 15 while
 * active, reactivation refreshing the timer instead of stacking a second one, deactivation after
 * 100-300 ticks, and dropping what the stone it replaced would drop rather than itself.
 */
public class BlockCaveIndicator extends Block {

	public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

	private final MapCodec<BlockCaveIndicator> codec = MapCodec.unit(this);

	public BlockCaveIndicator(BlockBehaviour.Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(ACTIVE, false));
	}

	@Override
	public MapCodec<? extends BlockCaveIndicator> codec() {
		return codec;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(ACTIVE);
	}

	/** V33a onEntityWalking. */
	@Override
	public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
		this.trigger(level, pos, state);
	}

	/** V33a onFallenUpon. */
	@Override
	public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, double fallDistance) {
		super.fallOn(level, state, pos, entity, fallDistance);
		this.trigger(level, pos, state);
	}

	/**
	 * V33a trigger: play the crystal's note, light up if it was dark, and (re)schedule the shutdown.
	 * An already-active crystal cancels its pending tick first, so walking over it repeatedly keeps
	 * it lit rather than letting an old tick switch it off early.
	 */
	private void trigger(Level level, BlockPos pos, BlockState state) {
		if (!(level instanceof ServerLevel server))
			return;
		// CHROMA-PORT: V33a plays ChromaSounds.DING upshifted to a random degree of the C major
		// scale, via MusicKey/KeySignature. ChromaSounds is registered but that DragonAPI music
		// interval helper is not ported yet, so the pitched variant is deliberately not faked here.
		if (!state.getValue(ACTIVE))
			server.setBlock(pos, state.setValue(ACTIVE, true), 3);
		server.scheduleTick(pos, this, 100 + server.getRandom().nextInt(200));
	}

	/** V33a updateTick: the scheduled shutdown. */
	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (state.getValue(ACTIVE))
			level.setBlock(pos, state.setValue(ACTIVE, false), 3);
	}

	@Override
	protected boolean isSignalSource(BlockState state) {
		return true;
	}

	/** V33a isProvidingWeakPower: full signal while lit. */
	@Override
	protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
		return state.getValue(ACTIVE) ? 15 : 0;
	}
}
