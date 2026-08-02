package reika.chromaticraft.world;

import java.util.stream.Stream;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;

import reika.chromaticraft.registry.ChromaPlacementModifiers;

/**
 * V33a {@code TieredOres.generate} picks its own y, and not from a uniform band:
 *
 * <pre>y = ordinal >= FIRESTONE ? rand(128) : (rand.nextBoolean() ? rand(32) : rand(64))</pre>
 *
 * The overworld roll is a 50/50 mix of {@code uniform[0,32)} and {@code uniform[0,64)}, which is
 * markedly denser toward bedrock than either band alone; the nether/end ores use a single flat
 * {@code uniform[0,128)}. Neither is expressible with vanilla's height providers, so the roll is
 * reproduced exactly here.
 *
 * <p>These are absolute y values in the source, where the world floor was 0. The modifier therefore
 * offsets by the level's minimum build height so the same band sits at the bottom of a modern world.
 */
public final class TieredOreHeightPlacement extends PlacementModifier {

	private final boolean deepBand;

	public TieredOreHeightPlacement(boolean deepBand) {
		this.deepBand = deepBand;
	}

	public boolean deepBand() {
		return deepBand;
	}

	public static final MapCodec<TieredOreHeightPlacement> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			com.mojang.serialization.Codec.BOOL.fieldOf("deep_band").forGetter(TieredOreHeightPlacement::deepBand)
	).apply(i, TieredOreHeightPlacement::new));

	@Override
	public Stream<BlockPos> getPositions(PlacementContext context, RandomSource random, BlockPos origin) {
		int y = deepBand ? random.nextInt(128)
				: random.nextBoolean() ? random.nextInt(32) : random.nextInt(64);
		return Stream.of(new BlockPos(origin.getX(), context.getMinY() + y, origin.getZ()));
	}

	@Override
	public PlacementModifierType<?> type() {
		return ChromaPlacementModifiers.TIERED_ORE_HEIGHT.get();
	}
}
