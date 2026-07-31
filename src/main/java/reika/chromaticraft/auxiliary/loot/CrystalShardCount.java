package reika.chromaticraft.auxiliary.loot;

import com.mojang.serialization.MapCodec;

import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;

/**
 * V33a {@code BlockCaveCrystal.getNumberDrops(Random, int fortune)}:
 *
 * <pre>1 + rand.nextInt(6+fortune) + (1+fortune)*rand.nextInt(3) + rand.nextInt(1+fortune)</pre>
 *
 * This is deliberately not expressible as a composition of vanilla number providers — it is three
 * independent rolls whose ranges each scale differently with Fortune (documented averages in the
 * source: 4.5 at Fortune 0 rising to ~44 at Fortune 20). Approximating it with a uniform range would
 * change the mod's entire early-game shard economy, so the formula is registered as its own provider
 * and the loot table stays data-driven.
 */
public record CrystalShardCount() implements NumberProvider {

	public static final CrystalShardCount INSTANCE = new CrystalShardCount();
	public static final MapCodec<CrystalShardCount> MAP_CODEC = MapCodec.unit(INSTANCE);

	@Override
	public MapCodec<CrystalShardCount> codec() {
		return MAP_CODEC;
	}

	@Override
	public int getInt(LootContext context) {
		RandomSource random = context.getRandom();
		int fortune = fortuneLevel(context);
		return 1 + random.nextInt(6 + fortune)
				+ (1 + fortune) * random.nextInt(3)
				+ random.nextInt(1 + fortune);
	}

	@Override
	public float getFloat(LootContext context) {
		return this.getInt(context);
	}

	/** Shared with the dye-leaf drop providers. */
	static int fortuneLevel(LootContext context) {
		ItemInstance tool = context.getOptionalParameter(LootContextParams.TOOL);
		if (tool == null || tool.count() <= 0)
			return 0;
		return EnchantmentHelper.getItemEnchantmentLevel(
				context.getLevel().registryAccess()
						.lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
						.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.FORTUNE),
				tool);
	}
}
