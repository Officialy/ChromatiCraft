package reika.chromaticraft.world;

import java.util.function.Consumer;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;

import reika.chromaticraft.registry.ChromaPlacementModifiers;

/**
 * Minecraft 1.7.10's decorator heights: a random y below the column's height-map value, scaled and
 * offset — {@code nextInt(getHeightValue(x, z) * multiplier + add)}, where a bound of 1 or less gives 0.
 * Flowers used {@code height + 32}; grass, lily pads, mushrooms and reeds {@code height * 2}. An attempt
 * only succeeds where that lands near something it can grow on, which is why 1.7.10 decorators made
 * so many.
 *
 * <p>1.7.10's height map is the first light-blocking block from the top, which counts leaves and water;
 * {@code MOTION_BLOCKING} is the 26.2 height map that does the same.
 */
public final class LegacyHeightmapScaledPlacement implements PlacementModifier {

	public static final MapCodec<LegacyHeightmapScaledPlacement> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.INT.fieldOf("multiplier").forGetter(LegacyHeightmapScaledPlacement::multiplier),
			Codec.INT.fieldOf("add").forGetter(LegacyHeightmapScaledPlacement::add)
	).apply(i, LegacyHeightmapScaledPlacement::new));

	private final int multiplier;
	private final int add;

	public LegacyHeightmapScaledPlacement(int multiplier, int add) {
		this.multiplier = multiplier;
		this.add = add;
	}

	public int multiplier() {
		return multiplier;
	}

	public int add() {
		return add;
	}

	@Override
	public void modify(PlacementContext context, RandomSource random, BlockPos origin, Consumer<BlockPos> output) {
		int height = context.getHeight(Heightmap.Types.MOTION_BLOCKING, origin.getX(), origin.getZ());
		int bound = height * multiplier + add;
		int y = bound <= 1 ? 0 : random.nextInt(bound);
		output.accept(new BlockPos(origin.getX(), y, origin.getZ()));
	}

	@Override
	public MapCodec<LegacyHeightmapScaledPlacement> codec() {
		return CODEC;
	}
}
