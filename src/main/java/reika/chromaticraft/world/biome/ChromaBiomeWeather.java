package reika.chromaticraft.world.biome;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import reika.chromaticraft.registry.ChromaWeatherAttributes;

public final class ChromaBiomeWeather {
    private ChromaBiomeWeather() {}

    public static float rainbowTemperature(float base, int y) {
        if (y > 127) base -= (y-127)/127F*base;
        if (y > 191) base = .15F+.025F*(y-255)/64F;
        if (y >= 255) base = .1F;
        return Math.max(base,.125F);
    }

    public static boolean hasRainbowCooling(Biome biome) {
        return ChromaWeatherAttributes.RAINBOW_COOLING.isBound()
                && biome.getAttributes().applyModifier(ChromaWeatherAttributes.RAINBOW_COOLING.get(),false);
    }

    public static boolean canSpawnLightning(ServerLevel level, BlockPos pos) {
        if (level.isRainingAt(pos)) return true;
        return ChromaWeatherAttributes.RAINLESS_THUNDER.isBound()
                && level.getBiome(pos).value().getAttributes().applyModifier(ChromaWeatherAttributes.RAINLESS_THUNDER.get(),false)
                && level.isRaining() && level.canSeeSky(pos)
                && level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING,pos).getY() <= pos.getY();
    }
}
