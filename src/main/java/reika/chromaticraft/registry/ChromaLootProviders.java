package reika.chromaticraft.registry;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;

import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.auxiliary.loot.CrystalShardCount;

/** Custom loot number providers for V33a drop formulas vanilla cannot express. */
public final class ChromaLootProviders {

	public static final DeferredRegister<MapCodec<? extends NumberProvider>> NUMBER_PROVIDERS =
			DeferredRegister.create(BuiltInRegistries.LOOT_NUMBER_PROVIDER_TYPE, ChromatiCraft.MODID);

	public static final DeferredHolder<MapCodec<? extends NumberProvider>, MapCodec<CrystalShardCount>>
			CRYSTAL_SHARD_COUNT = NUMBER_PROVIDERS.register("crystal_shard_count",
					() -> CrystalShardCount.MAP_CODEC);

	private ChromaLootProviders() {}
}
