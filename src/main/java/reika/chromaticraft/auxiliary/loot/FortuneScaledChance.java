package reika.chromaticraft.auxiliary.loot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

/**
 * V33a's Fortune-scaled drop chances, which scale multiplicatively rather than through a
 * per-level lookup table like vanilla's leaves do. From {@code BlockDyeLeaf.getDrops}:
 *
 * <pre>
 * saplingChance *= (1+fortune);
 * appleChance   *= (1+fortune*5);
 * dyeChance     *= (1+fortune);
 * rainbowChance *= (1+fortune)*(1+fortune);
 * berryChance    = 0.1F*intpow2(2, fortune);
 * </pre>
 *
 * The three shapes are covered by {@link Mode}. Vanilla has no number provider or condition that
 * composes into any of them, and approximating them with a fixed table would change the drop rates
 * at every Fortune level.
 */
public record FortuneScaledChance(double base, double perLevel, Mode mode) implements LootItemCondition {

	public enum Mode {
		/** {@code base * (1 + fortune*perLevel)} */
		LINEAR,
		/** {@code base * (1 + fortune)^2} */
		QUADRATIC,
		/** {@code base * 2^fortune} */
		EXPONENTIAL,
		/**
		 * {@code 1 / max(1, base - fortune*perLevel)} — a one-in-N roll whose N shrinks with Fortune,
		 * which is how V33a writes {@code rand.nextInt(Math.max(1, 50-fortune*5)) == 0}. The clamp is
		 * upstream's and matters: without it a high enough Fortune would invert the odds.
		 */
		RECIPROCAL,
		/**
		 * {@code 1 - 1/(base + floor((1+fortune)*perLevel))} — the odds that a one-in-N roll comes up
		 * anything but zero, which is V33a's {@code rand.nextInt(1+(1+fortune)/2) > 0}. At Fortune 0
		 * that is a certainty of failure, and upstream relies on it: an unenchanted axe gets no
		 * glowstone from a Glowing Leaf at all.
		 */
		RECIPROCAL_COMPLEMENT;

		public static final Codec<Mode> CODEC = Codec.STRING.xmap(
				name -> valueOf(name.toUpperCase(java.util.Locale.ROOT)),
				m -> m.name().toLowerCase(java.util.Locale.ROOT));
	}

	public static final MapCodec<FortuneScaledChance> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			Codec.DOUBLE.fieldOf("base").forGetter(FortuneScaledChance::base),
			Codec.DOUBLE.optionalFieldOf("per_level", 1D).forGetter(FortuneScaledChance::perLevel),
			Mode.CODEC.optionalFieldOf("mode", Mode.LINEAR).forGetter(FortuneScaledChance::mode)
	).apply(instance, FortuneScaledChance::new));

	public static FortuneScaledChance linear(double base, double perLevel) {
		return new FortuneScaledChance(base, perLevel, Mode.LINEAR);
	}

	public static FortuneScaledChance quadratic(double base) {
		return new FortuneScaledChance(base, 1, Mode.QUADRATIC);
	}

	public static FortuneScaledChance exponential(double base) {
		return new FortuneScaledChance(base, 1, Mode.EXPONENTIAL);
	}

	/** V33a's {@code rand.nextInt(Math.max(1, base - fortune*perLevel)) == 0}. */
	public static FortuneScaledChance reciprocal(double base, double perLevel) {
		return new FortuneScaledChance(base, perLevel, Mode.RECIPROCAL);
	}

	/** V33a's {@code rand.nextInt(base + (1+fortune)*perLevel) > 0}. */
	public static FortuneScaledChance reciprocalComplement(double base, double perLevel) {
		return new FortuneScaledChance(base, perLevel, Mode.RECIPROCAL_COMPLEMENT);
	}

	/** The V33a chance for a given Fortune level, shared with {@link ChromaBerryCount}. */
	public double chanceAt(int fortune) {
		return switch (mode) {
			case LINEAR -> base * (1 + fortune * perLevel);
			case QUADRATIC -> base * (1 + fortune) * (1 + fortune);
			case EXPONENTIAL -> base * Math.pow(2, fortune);
			case RECIPROCAL -> 1 / Math.max(1, base - fortune * perLevel);
			case RECIPROCAL_COMPLEMENT -> 1 - 1 / (base + Math.floor((1 + fortune) * perLevel));
		};
	}

	@Override
	public boolean test(LootContext context) {
		return context.getRandom().nextDouble() < this.chanceAt(CrystalShardCount.fortuneLevel(context));
	}

	@Override
	public MapCodec<? extends LootItemCondition> codec() {
		return MAP_CODEC;
	}
}
