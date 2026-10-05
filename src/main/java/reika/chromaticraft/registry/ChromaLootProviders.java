package reika.chromaticraft.registry;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProvider;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.auxiliary.loot.ChromaBerryCount;
import reika.chromaticraft.auxiliary.loot.CrystalShardCount;
import reika.chromaticraft.auxiliary.loot.FortuneScaledChance;
import reika.chromaticraft.auxiliary.loot.TuningScaledCount;

/** Custom loot number providers for V33a drop formulas vanilla cannot express. */
public final class ChromaLootProviders {

	public static final DeferredRegister<MapCodec<? extends ContextIntProvider>> NUMBER_PROVIDERS =
			DeferredRegister.create(BuiltInRegistries.CONTEXT_INT_PROVIDER_TYPE, ChromatiCraft.MODID);

	public static final DeferredHolder<MapCodec<? extends ContextIntProvider>, MapCodec<CrystalShardCount>>
			CRYSTAL_SHARD_COUNT = NUMBER_PROVIDERS.register("crystal_shard_count",
					() -> CrystalShardCount.MAP_CODEC);

	public static final DeferredHolder<MapCodec<? extends ContextIntProvider>, MapCodec<ChromaBerryCount>>
			CHROMA_BERRY_COUNT = NUMBER_PROVIDERS.register("chroma_berry_count",
					() -> ChromaBerryCount.MAP_CODEC);

	public static final DeferredHolder<MapCodec<? extends ContextIntProvider>, MapCodec<TuningScaledCount>>
			TUNING_SCALED_COUNT = NUMBER_PROVIDERS.register("tuning_scaled_count",
					() -> TuningScaledCount.MAP_CODEC);

	public static final DeferredHolder<MapCodec<? extends ContextIntProvider>, MapCodec<reika.chromaticraft.auxiliary.loot.FortuneBonusCount>>
			FORTUNE_BONUS_COUNT = NUMBER_PROVIDERS.register("fortune_bonus_count",
					() -> reika.chromaticraft.auxiliary.loot.FortuneBonusCount.MAP_CODEC);

	public static final DeferredHolder<MapCodec<? extends ContextIntProvider>, MapCodec<reika.chromaticraft.auxiliary.loot.RainbowLeafDyeCount>>
			RAINBOW_LEAF_DYE_COUNT = NUMBER_PROVIDERS.register("rainbow_leaf_dye_count",
					() -> reika.chromaticraft.auxiliary.loot.RainbowLeafDyeCount.MAP_CODEC);

	public static final DeferredRegister<MapCodec<? extends LootItemCondition>> CONDITIONS =
			DeferredRegister.create(BuiltInRegistries.LOOT_CONDITION_TYPE, ChromatiCraft.MODID);

	public static final DeferredHolder<MapCodec<? extends LootItemCondition>, MapCodec<FortuneScaledChance>>
			FORTUNE_SCALED_CHANCE = CONDITIONS.register("fortune_scaled_chance",
					() -> FortuneScaledChance.MAP_CODEC);

	public static final DeferredHolder<MapCodec<? extends LootItemCondition>, MapCodec<reika.chromaticraft.auxiliary.loot.RainbowGoldAppleChance>>
			RAINBOW_GOLD_APPLE_CHANCE = CONDITIONS.register("rainbow_gold_apple_chance",
					() -> reika.chromaticraft.auxiliary.loot.RainbowGoldAppleChance.MAP_CODEC);

	private ChromaLootProviders() {}
}
