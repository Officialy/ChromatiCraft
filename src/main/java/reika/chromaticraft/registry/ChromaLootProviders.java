package reika.chromaticraft.registry;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;

import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import reika.chromaticraft.ChromatiCraft;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

import reika.chromaticraft.auxiliary.loot.ChromaBerryCount;
import reika.chromaticraft.auxiliary.loot.CrystalShardCount;
import reika.chromaticraft.auxiliary.loot.FortuneScaledChance;

/** Custom loot number providers for V33a drop formulas vanilla cannot express. */
public final class ChromaLootProviders {

	public static final DeferredRegister<MapCodec<? extends NumberProvider>> NUMBER_PROVIDERS =
			DeferredRegister.create(BuiltInRegistries.LOOT_NUMBER_PROVIDER_TYPE, ChromatiCraft.MODID);

	public static final DeferredHolder<MapCodec<? extends NumberProvider>, MapCodec<CrystalShardCount>>
			CRYSTAL_SHARD_COUNT = NUMBER_PROVIDERS.register("crystal_shard_count",
					() -> CrystalShardCount.MAP_CODEC);

	public static final DeferredHolder<MapCodec<? extends NumberProvider>, MapCodec<ChromaBerryCount>>
			CHROMA_BERRY_COUNT = NUMBER_PROVIDERS.register("chroma_berry_count",
					() -> ChromaBerryCount.MAP_CODEC);

	public static final DeferredRegister<MapCodec<? extends LootItemCondition>> CONDITIONS =
			DeferredRegister.create(BuiltInRegistries.LOOT_CONDITION_TYPE, ChromatiCraft.MODID);

	public static final DeferredHolder<MapCodec<? extends LootItemCondition>, MapCodec<FortuneScaledChance>>
			FORTUNE_SCALED_CHANCE = CONDITIONS.register("fortune_scaled_chance",
					() -> FortuneScaledChance.MAP_CODEC);

	private ChromaLootProviders() {}
}
