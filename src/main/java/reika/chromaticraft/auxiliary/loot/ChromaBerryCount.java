package reika.chromaticraft.auxiliary.loot;

import com.mojang.serialization.MapCodec;

import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;

/**
 * V33a chroma-berry stack size from {@code BlockDyeLeaf.getDrops}:
 *
 * <pre>
 * float berryChance = 0.1F*intpow2(2, fortune);
 * int berryNum = 1;
 * while (berryChance &gt; 1) {
 *     berryChance = Math.max(1, berryChance-0.5F);
 *     berryNum++;
 * }
 * ... li.add(BERRY.getCraftedMetadataProduct(berryNum+(int)berryChance, meta));
 * </pre>
 *
 * The chance and the count are the same running variable: once Fortune pushes the chance past 1 the
 * overflow is repeatedly traded for extra berries. {@link FortuneScaledChance#exponential} tests the
 * same curve for whether the drop happens at all.
 */
public record ChromaBerryCount() implements NumberProvider {

	public static final ChromaBerryCount INSTANCE = new ChromaBerryCount();
	public static final MapCodec<ChromaBerryCount> MAP_CODEC = MapCodec.unit(INSTANCE);

	/** V33a's base berry chance before the overflow loop. */
	public static final double BASE_CHANCE = 0.1;

	@Override
	public MapCodec<ChromaBerryCount> codec() {
		return MAP_CODEC;
	}

	@Override
	public int getInt(LootContext context) {
		double chance = BASE_CHANCE * Math.pow(2, CrystalShardCount.fortuneLevel(context));
		int count = 1;
		while (chance > 1) {
			chance = Math.max(1, chance - 0.5);
			count++;
		}
		return count + (int)chance;
	}

	@Override
	public float getFloat(LootContext context) {
		return this.getInt(context);
	}
}
