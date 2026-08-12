package reika.chromaticraft.block;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/** V33a's intangible vertical-motion field, used as the Biome Fragment's entrance lift. */
public final class BlockHoverBlock extends Block {

	public enum HoverType implements StringRepresentable {
		STATIONARY("stationary", "Brake", 0xffff00, 0),
		DAMPER("damper", "Damper", 0xff00ff, -0.25),
		ELEVATE("elevate", "Elevator", 0x2288ff, 0.125),
		FAST_ELEVATE("fast_elevate", "Rapid Elevator", 0x00ff00, 0.75);

		private final String name;
		public final String description;
		public final int renderColor;
		public final double velocityFactor;
		HoverType(String name, String description, int color, double velocity) {
			this.name = name; this.description = description; renderColor = color; velocityFactor = velocity;
		}
		@Override public String getSerializedName() { return name; }
		public boolean movesUpwards() { return velocityFactor > 0; }
	}

	public enum Decay implements StringRepresentable {
		PERMANENT("permanent"), ARMED("armed"), DECAYING("decaying");
		private final String name;
		Decay(String name) { this.name = name; }
		@Override public String getSerializedName() { return name; }
	}

	public static final EnumProperty<HoverType> TYPE = EnumProperty.create("type", HoverType.class);
	public static final EnumProperty<Decay> DECAY = EnumProperty.create("decay", Decay.class);
	private final MapCodec<BlockHoverBlock> codec = MapCodec.unit(this);

	public BlockHoverBlock(BlockBehaviour.Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(TYPE, HoverType.STATIONARY)
				.setValue(DECAY, Decay.PERMANENT));
	}

	@Override public MapCodec<? extends BlockHoverBlock> codec() { return codec; }

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(TYPE, DECAY);
	}

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moved) {
		if (!oldState.is(this) && !level.isClientSide()) level.scheduleTick(pos, this, 80);
	}

	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		switch (state.getValue(DECAY)) {
			case PERMANENT -> { }
			case ARMED -> {
				level.setBlock(pos, state.setValue(DECAY, Decay.DECAYING), 3);
				level.scheduleTick(pos, this, 20 + random.nextInt(60));
			}
			case DECAYING -> level.removeBlock(pos, false);
		}
	}

	@Override
	protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity,
			InsideBlockEffectApplier effects, boolean precise) {
		if (!(entity instanceof Player player) || player.isShiftKeyDown()) return;
		double target = state.getValue(TYPE).velocityFactor;
		double delta = entity.getDeltaMovement().y - target - 0.3;
		if (delta < 0) {
			double newY = delta > 0.0625 ? target : entity.getDeltaMovement().y - 0.25 * delta;
			entity.setDeltaMovement(entity.getDeltaMovement().x, newY, entity.getDeltaMovement().z);
			entity.resetFallDistance();
		}
	}

	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (!level.getBlockState(pos.below()).is(this) || random.nextInt(6) == 0) {
			spawnPortal(level, pos, random);
			spawnPortal(level, pos.below(), random);
		}
	}

	private static void spawnPortal(Level level, BlockPos pos, RandomSource random) {
		level.addParticle(ParticleTypes.PORTAL, pos.getX() + random.nextDouble(),
				pos.getY() + random.nextDouble(), pos.getZ() + random.nextDouble(),
				random.nextGaussian() * 0.05, random.nextDouble() * 0.08, random.nextGaussian() * 0.05);
	}

	public static int tint(BlockState state) {
		int color = state.getValue(TYPE).renderColor;
		if (state.getValue(DECAY) == Decay.DECAYING) {
			int r = color >> 16 & 255, g = color >> 8 & 255, b = color & 255;
			int gray = (r + g + b) / 3;
			r = (3 * r + gray) / 4; g = (3 * g + gray) / 4; b = (3 * b + gray) / 4;
			color = r << 16 | g << 8 | b;
		}
		return 0xff000000 | color;
	}
}
