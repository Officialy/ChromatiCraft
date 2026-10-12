package reika.chromaticraft.world.biome;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;
import reika.chromaticraft.ChromatiCraft;
import terrablender.api.Region;
import terrablender.api.RegionType;

import java.util.function.Consumer;

/** Ender Forest's climate placement and advertised Rainbow family; LargeBiomePlacement owns Rainbow regions. */
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
            // Retain these entries in TerraBlender's possibleBiomes list. The resolver replaces
            // their climate fragments with the planned, minimum-sized Rainbow regions.
            builder.replaceBiome(Biomes.FOREST, ChromaBiomes.RAINBOW_FOREST);
            builder.replaceBiome(Biomes.RIVER, ChromaBiomes.RAINBOW_STREAM);
            // V33a's Ender Forest replaced ordinary forest; dark forest is the closest slot that
            // does not collide with the Rainbow Forest already taking FOREST.
            builder.replaceBiome(Biomes.DARK_FOREST, ChromaBiomes.ENDER_FOREST);
        });
    }
}
