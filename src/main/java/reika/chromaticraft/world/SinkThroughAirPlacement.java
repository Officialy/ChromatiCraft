package reika.chromaticraft.world;

import java.util.stream.Stream;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;

import reika.chromaticraft.registry.ChromaPlacementModifiers;

/**
 * The two ways Minecraft 1.7.10 decorators dropped a starting height to the ground.
 *
 * <ul>
 * <li>{@link Mode#ONTO_GROUND}, {@code WorldGenTallGrass}: move down while the block there is air or
 * leaves, stopping on the first block that is neither.</li>
 * <li>{@link Mode#ABOVE_GROUND}, {@code BiomeDecorator}'s lily-pad loop: move down while the block
 * below is air, stopping on top of whatever is there (for a lily pad, the water).</li>
 * </ul>
 *
 * <p>Vanilla's environment scan does the same walks but is capped at 32 steps, and 1.7.10's starting
 * heights were often much further above the ground than that. Both stop at the world floor.
 */
public final class SinkThroughAirPlacement extends PlacementModifier {

	public enum Mode implements StringRepresentable {
		ONTO_GROUND,
		ABOVE_GROUND;

		public static final Codec<Mode> CODEC = StringRepresentable.fromEnum(Mode::values);

		@Override
		public String getSerializedName() {
			return name().toLowerCase(java.util.Locale.ROOT);
		}
	}

	public static final SinkThroughAirPlacement INSTANCE = new SinkThroughAirPlacement(Mode.ONTO_GROUND);
	public static final SinkThroughAirPlacement ABOVE_GROUND = new SinkThroughAirPlacement(Mode.ABOVE_GROUND);
	public static final MapCodec<SinkThroughAirPlacement> CODEC = Mode.CODEC
			.optionalFieldOf("mode", Mode.ONTO_GROUND)
			.xmap(mode -> mode == Mode.ONTO_GROUND ? INSTANCE : ABOVE_GROUND, p -> p.mode);

	private final Mode mode;

	private SinkThroughAirPlacement(Mode mode) {
		this.mode = mode;
	}

	@Override
	public Stream<BlockPos> getPositions(PlacementContext context, RandomSource random, BlockPos origin) {
		WorldGenLevel level = context.getLevel();
		BlockPos.MutableBlockPos pos = origin.mutable();
		while (pos.getY() > level.getMinY()) {
			if (mode == Mode.ONTO_GROUND) {
				BlockState state = level.getBlockState(pos);
				if (!state.isAir() && !state.is(BlockTags.LEAVES))
					break;
			}
			else if (!level.getBlockState(pos.below()).isAir()) {
				break;
			}
			pos.move(0, -1, 0);
		}
		return Stream.of(pos.immutable());
	}

	@Override
	public PlacementModifierType<?> type() {
		return ChromaPlacementModifiers.SINK_THROUGH_AIR.get();
	}
}
