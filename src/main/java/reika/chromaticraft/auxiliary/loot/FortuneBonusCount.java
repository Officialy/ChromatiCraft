package reika.chromaticraft.auxiliary.loot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;

/**
 * V33a's {@code base + rand.nextInt(1 + fortune/divisor)} stack size, with Java's integer division.
 *
 * <p>Vanilla's uniform bonus count adds {@code nextInt(round(level*multiplier) + 1)}, which rounds where
 * upstream truncates, so a divisor of two already disagrees at Fortune I. {@code BlockDyeVine.getDrops}
 * uses this shape twice: {@code 1 + rand(1 + fortune/2)} shards from a fertile vine and
 * {@code 1 + rand(1 + fortune)} berries.
 */
public record FortuneBonusCount(int base, int divisor) implements NumberProvider {

	public static final MapCodec<FortuneBonusCount> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			Codec.INT.fieldOf("base").forGetter(FortuneBonusCount::base),
			Codec.INT.optionalFieldOf("divisor", 1).forGetter(FortuneBonusCount::divisor)
	).apply(instance, FortuneBonusCount::new));

	@Override
	public MapCodec<FortuneBonusCount> codec() {
		return MAP_CODEC;
	}

	@Override
	public int getInt(LootContext context) {
		int fortune = CrystalShardCount.fortuneLevel(context);
		return base + context.getRandom().nextInt(1 + fortune / Math.max(1, divisor));
	}

	@Override
	public float getFloat(LootContext context) {
		return this.getInt(context);
	}
}
