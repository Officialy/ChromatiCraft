package reika.chromaticraft.auxiliary.loot;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

import reika.chromaticraft.registry.ChromaOptions;

/**
 * V33a {@code BlockRainbowLeaf.getDrops}' enchanted golden apple roll:
 * {@code ChromaOptions.getRainbowLeafGoldAppleDropChance() * (1+fortune*3)}. The base is a config option, so it is
 * read when the leaf breaks rather than baked into the loot table.
 */
public record RainbowGoldAppleChance() implements LootItemCondition {

	public static final RainbowGoldAppleChance INSTANCE = new RainbowGoldAppleChance();
	public static final MapCodec<RainbowGoldAppleChance> MAP_CODEC = MapCodec.unit(INSTANCE);

	@Override
	public boolean test(LootContext context) {
		double chance = ChromaOptions.getRainbowLeafGoldAppleDropChance() * (1 + CrystalShardCount.fortuneLevel(context) * 3);
		return context.getRandom().nextDouble() < chance;
	}

	@Override
	public MapCodec<? extends LootItemCondition> codec() {
		return MAP_CODEC;
	}
}
