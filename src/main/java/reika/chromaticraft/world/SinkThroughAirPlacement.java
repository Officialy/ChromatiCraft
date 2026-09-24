package reika.chromaticraft.world;

import java.util.stream.Stream;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;

import reika.chromaticraft.registry.ChromaPlacementModifiers;

/**
 * Minecraft 1.7.10 {@code WorldGenTallGrass}'s first step: from the starting height, move down while the
 * block there is air or leaves, stopping on the first block that is neither (or at the world floor).
 *
 * <p>Vanilla's environment scan does the same walk but is capped at 32 steps, and 1.7.10's grass started
 * anywhere below y 128, which is often much further above the ground than that.
 */
public final class SinkThroughAirPlacement extends PlacementModifier {

	public static final SinkThroughAirPlacement INSTANCE = new SinkThroughAirPlacement();
	public static final MapCodec<SinkThroughAirPlacement> CODEC = MapCodec.unit(INSTANCE);

	private SinkThroughAirPlacement() {}

	@Override
	public Stream<BlockPos> getPositions(PlacementContext context, RandomSource random, BlockPos origin) {
		WorldGenLevel level = context.getLevel();
		BlockPos.MutableBlockPos pos = origin.mutable();
		while (pos.getY() > level.getMinY()) {
			BlockState state = level.getBlockState(pos);
			if (!state.isAir() && !state.is(BlockTags.LEAVES))
				break;
			pos.move(0, -1, 0);
		}
		return Stream.of(pos.immutable());
	}

	@Override
	public PlacementModifierType<?> type() {
		return ChromaPlacementModifiers.SINK_THROUGH_AIR.get();
	}
}
