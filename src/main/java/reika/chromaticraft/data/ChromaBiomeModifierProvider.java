package reika.chromaticraft.data;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonObject;

import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.levelgen.GenerationStep;

import reika.chromaticraft.ChromatiCraft;

/** Adds cave crystals and NBT-backed natural pylons to their V33a-compatible biome sets. */
public final class ChromaBiomeModifierProvider implements DataProvider {

    private static final String CAVE_CRYSTAL = id("cave_crystal").toString();
    private static final String PYLON = id("pylon").toString();
    private final PackOutput.PathProvider pathProvider;

    public ChromaBiomeModifierProvider(PackOutput output) {
        pathProvider = output.createPathProvider(PackOutput.Target.DATA_PACK, "neoforge/biome_modifier");
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        ImmutableList.Builder<CompletableFuture<?>> futures = ImmutableList.builder();
        futures.add(save(cache, "cave_crystal_overworld", "#minecraft:is_overworld",
                CAVE_CRYSTAL, GenerationStep.Decoration.UNDERGROUND_DECORATION));
        futures.add(save(cache, "cave_crystal_nether", "#minecraft:is_nether",
                CAVE_CRYSTAL, GenerationStep.Decoration.UNDERGROUND_DECORATION));
        futures.add(save(cache, "pylon_overworld", "#minecraft:is_overworld",
                PYLON, GenerationStep.Decoration.SURFACE_STRUCTURES));
        // V33a TieredWorldGenerator runs in every ordinary dimension; each ore's own host block and
        // y band are what confine it, so the overworld pair goes everywhere overworld and the
        // netherrack-hosted one everywhere nether.
        futures.add(saveMany(cache, "tiered_ore_overworld", "#minecraft:is_overworld",
                List.of(id("energized_rock").toString(), id("elemental_stones").toString()),
                GenerationStep.Decoration.UNDERGROUND_ORES));
        futures.add(save(cache, "tiered_ore_nether", "#minecraft:is_nether",
                id("firestone").toString(), GenerationStep.Decoration.UNDERGROUND_ORES));
        return CompletableFuture.allOf(futures.build().toArray(CompletableFuture[]::new));
    }

    private CompletableFuture<?> saveMany(CachedOutput cache, String name, String biomes,
            List<String> features, GenerationStep.Decoration step) {
        Path path = pathProvider.json(id(name));
        JsonObject json = new JsonObject();
        json.addProperty("type", "neoforge:add_features");
        json.addProperty("biomes", biomes);
        com.google.gson.JsonArray array = new com.google.gson.JsonArray();
        features.forEach(array::add);
        json.add("features", array);
        json.addProperty("step", step.getName());
        return DataProvider.saveStable(cache, json, path);
    }

    private CompletableFuture<?> save(CachedOutput cache, String name, String biomes,
            String feature, GenerationStep.Decoration step) {
        Path path = pathProvider.json(id(name));
        JsonObject json = new JsonObject();
        json.addProperty("type", "neoforge:add_features");
        json.addProperty("biomes", biomes);
        json.addProperty("features", feature);
        json.addProperty("step", step.getName());
        return DataProvider.saveStable(cache, json, path);
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, path);
    }

    @Override
    public String getName() {
        return "ChromatiCraft Biome Modifiers";
    }
}
