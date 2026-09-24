package reika.chromaticraft.registry;

import com.mojang.serialization.MapCodec;

import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.world.biome.RainbowForestSpawnDensity;

/** ChromatiCraft's own biome-modifier types, for biome values V33a read from live config. */
public final class ChromaBiomeModifiers {

	public static final DeferredRegister<MapCodec<? extends BiomeModifier>> SERIALIZERS =
			DeferredRegister.create(NeoForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS, ChromatiCraft.MODID);

	public static final DeferredHolder<MapCodec<? extends BiomeModifier>, MapCodec<RainbowForestSpawnDensity>>
			RAINBOW_FOREST_SPAWN_DENSITY = SERIALIZERS.register("rainbow_forest_spawn_density",
					() -> RainbowForestSpawnDensity.CODEC);

	private ChromaBiomeModifiers() {}
}
