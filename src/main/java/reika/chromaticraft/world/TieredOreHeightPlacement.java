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
 * <p>These are absolute y values, and they stay absolute. The earlier port offset them by the level's
 * minimum build height, reasoning that the band should sit the same distance above bedrock as it did
 * when the world floor was 0. That put the whole overworld roll in y [-64, 0) — which is deepslate,
 * while the ore features target {@code minecraft:stone} — so no overworld tiered ore could ever
 * place. Absolute y is also the faithful reading everywhere else: the Nether and End floors are still
 * 0, so only the overworld floor ever moved, and V33a's own {@code rand(128)} nether band already
 * means what it says.
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
		return Stream.of(new BlockPos(origin.getX(), y, origin.getZ()));
	}

	@Override
	public PlacementModifierType<?> type() {
		return ChromaPlacementModifiers.TIERED_ORE_HEIGHT.get();
	}
}
