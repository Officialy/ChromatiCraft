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
import reika.chromaticraft.registry.ChromaTieredPlants;

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
        // V33a's PylonGenerator was a RetroactiveGenerator, so it ran after the chunk was fully
        // populated -- which is why its site test is built around trees (log/leaf replaceability,
        // getTreeDodgeAttempt, sinking the array through wood and leaves). SURFACE_STRUCTURES is
        // step 4 and VEGETAL_DECORATION is step 9, so running there meant no tree had been placed
        // yet: all of that logic was dead during worldgen, and trees then generated on top of the
        // site the feature had just verified as clear. TOP_LAYER_MODIFICATION is the last step, so
        // it is the faithful analogue of generating post-population.
        // V33a Flowers.canGenerateIn, one modifier per flower's biome set. Snow for Luma Lotus,
        // jungle for Ether Berries, swamp for Void Reeds, and hills/mountains for Aura Ivy.
        futures.add(saveMany(cache, "luma_lotus_snowy", "#c:is_snowy",
                List.of(id("luma_lotus").toString()), GenerationStep.Decoration.VEGETAL_DECORATION));
        futures.add(saveMany(cache, "sano_bloom_jungle", "#minecraft:is_jungle",
                List.of(id("sano_bloom").toString()), GenerationStep.Decoration.VEGETAL_DECORATION));
        futures.add(saveMany(cache, "void_reeds_swamp", "#c:is_swamp",
                List.of(id("void_reeds").toString()), GenerationStep.Decoration.VEGETAL_DECORATION));
        futures.add(saveManyBiomes(cache, "ender_forest_flowers",
                List.of(id("ender_forest").toString()),
                List.of(id("enderflower").toString(), id("resonant_clover").toString()),
                GenerationStep.Decoration.VEGETAL_DECORATION));
        futures.add(saveMany(cache, "aura_ivy_hills", "#minecraft:is_mountain",
                List.of(id("aura_ivy").toString()), GenerationStep.Decoration.VEGETAL_DECORATION));
        // V33a CaveIndicatorGenerator is gated on BiomeGlowingCliffs.isGlowingCliffs, so this goes
        // to the two Luminous Cliffs biomes only rather than to an overworld tag.
        futures.add(saveManyBiomes(cache, "cave_indicator_cliffs",
                List.of(id("luminous_cliffs").toString(), id("luminous_cliffs_shores").toString()),
                List.of(id("cave_indicator").toString()),
                GenerationStep.Decoration.UNDERGROUND_DECORATION));
        futures.add(save(cache, "pylon_overworld", "#minecraft:is_overworld",
                PYLON, GenerationStep.Decoration.TOP_LAYER_MODIFICATION));
        // V33a TieredWorldGenerator was a RetroactiveGenerator, so plants were sited against a fully
        // populated chunk: Element Bulbs looks for leaves, which only exist once trees have run.
        // VEGETAL_DECORATION is where the trees themselves are, and ordering within a step is not
        // guaranteed, so a plant placed there can miss the leaves entirely and can be overwritten by
        // a tree afterwards -- measured as Element Bulbs in 1 chunk of 1,764. TOP_LAYER_MODIFICATION
        // is the last step and is the same choice the pylon feature makes, for the same reason.
        futures.add(saveMany(cache, "tiered_plant_overworld", "#minecraft:is_overworld",
                java.util.Arrays.stream(ChromaTieredPlants.list)
                        .map(p -> id(p.registryName()).toString()).toList(),
                GenerationStep.Decoration.TOP_LAYER_MODIFICATION));
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

    /** Same as {@link #saveMany} but for an explicit biome list rather than a tag. */
    private CompletableFuture<?> saveManyBiomes(CachedOutput cache, String name, List<String> biomes,
            List<String> features, GenerationStep.Decoration step) {
        Path path = pathProvider.json(id(name));
        JsonObject json = new JsonObject();
        json.addProperty("type", "neoforge:add_features");
        com.google.gson.JsonArray biomeArray = new com.google.gson.JsonArray();
        biomes.forEach(biomeArray::add);
        json.add("biomes", biomeArray);
        com.google.gson.JsonArray array = new com.google.gson.JsonArray();
        features.forEach(array::add);
        json.add("features", array);
        json.addProperty("step", step.getName());
        return DataProvider.saveStable(cache, json, path);
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
