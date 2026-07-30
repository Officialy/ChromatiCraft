package reika.chromaticraft.world.biome;

import java.util.function.Consumer;

import com.mojang.datafixers.util.Pair;

import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;

import terrablender.api.Region;
import terrablender.api.RegionType;

import reika.chromaticraft.ChromatiCraft;

/** Low-weight overworld climate region that restores natural Rainbow Forest generation. */
public final class ChromaRegion extends Region {

    public static final int WEIGHT = 2;

    public ChromaRegion() {
        super(Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "rainbow_forest"),
                RegionType.OVERWORLD, WEIGHT);
    }

    @Override
    public void addBiomes(Registry<Biome> registry,
            Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> mapper) {
        this.addModifiedVanillaOverworldBiomes(mapper, builder -> {
            builder.replaceBiome(Biomes.FOREST, ChromaBiomes.RAINBOW_FOREST);
            builder.replaceBiome(Biomes.RIVER, ChromaBiomes.RAINBOW_STREAM);
            builder.replaceBiome(Biomes.WINDSWEPT_HILLS, ChromaBiomes.LUMINOUS_CLIFFS);
            builder.replaceBiome(Biomes.WINDSWEPT_FOREST, ChromaBiomes.LUMINOUS_CLIFFS);
            builder.replaceBiome(Biomes.STONY_SHORE, ChromaBiomes.LUMINOUS_CLIFFS_SHORES);
        });
    }
}
