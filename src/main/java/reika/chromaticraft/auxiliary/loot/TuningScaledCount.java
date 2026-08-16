package reika.chromaticraft.auxiliary.loot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.level.storage.loot.providers.number.NumberProviders;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

import reika.chromaticraft.world.dimension.DimensionTuningManager;

/**
 * V33a {@code DimDecoTypes.addDrops}: how much of Proxima's decoration a player gets is decided by how
 * tuned they are to the dimension.
 *
 * <pre>
 * int n = 1;                                          // Glow Cave: getRandomBetween(1, 6)
 * if (ep != null)
 *     n = DimensionTuningManager.instance.getTunedDropCount(ep, n, 1, this.getMaxDrops());
 * </pre>
 *
 * <p>This cannot be a plain count in the table, because the multiplier depends on who is breaking the
 * block and the ceiling depends on which variant it is. It wraps the base roll rather than replacing
 * it, so the generated JSON still shows what the variant drops before tuning — Glow Cave's base really
 * is one to six — and the scaling is layered on top exactly as upstream layers it.
 *
 * <p>{@code getTunedDropCount} short-circuits outside Proxima and returns the base untouched, which is
 * upstream's dimension-id test: decoration broken back home drops what it says it drops. A drop with
 * no player at all — an explosion, a machine — likewise takes the base, matching V33a's
 * {@code ep != null} guard.
 *
 * @param base    the unscaled roll, which is the variant's own drop count
 * @param maximum the variant's {@code getMaxDrops} ceiling
 */
public record TuningScaledCount(NumberProvider base, int maximum) implements NumberProvider {

	public static final MapCodec<TuningScaledCount> MAP_CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance.group(
					NumberProviders.CODEC.fieldOf("base").forGetter(TuningScaledCount::base),
					Codec.INT.fieldOf("maximum").forGetter(TuningScaledCount::maximum))
					.apply(instance, TuningScaledCount::new));

	/** V33a's ordinary case: a single item before tuning. */
	public static TuningScaledCount single(int maximum) {
		return new TuningScaledCount(ConstantValue.exactly(1), maximum);
	}

	/** V33a's Glow Cave case: {@code getRandomBetween(1, 6)} before tuning. */
	public static TuningScaledCount between(int minimum, int maximum, int ceiling) {
		return new TuningScaledCount(UniformGenerator.between(minimum, maximum), ceiling);
	}

	@Override
	public MapCodec<TuningScaledCount> codec() {
		return MAP_CODEC;
	}

	@Override
	public int getInt(LootContext context) {
		int rolled = base.getInt(context);
		// Block loot carries the breaking entity as THIS_ENTITY; upstream reads the same thing from
		// its harvesters thread-local.
		if (!(context.getOptionalParameter(LootContextParams.THIS_ENTITY) instanceof Player player))
			return rolled;
		return DimensionTuningManager.instance.getTunedDropCount(player, rolled, 1, maximum);
	}

	@Override
	public float getFloat(LootContext context) {
		return this.getInt(context);
	}
}
