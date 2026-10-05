package reika.chromaticraft.world;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementFilter;
import reika.chromaticraft.registry.ChromaOptions;

/**
 * Applies V33a's seeded shuffled pylon grid to natural placed-feature generation. Keeping this
 * policy outside {@link PylonFeature} lets the configured feature remain directly testable with
 * {@code /place feature chromaticraft:pylon}.
 */
public final class PylonGridPlacement implements PlacementFilter {

    public static final PylonGridPlacement INSTANCE = new PylonGridPlacement();
    public static final MapCodec<PylonGridPlacement> CODEC = MapCodec.unit(() -> INSTANCE);

    private PylonGridPlacement() {}

    @Override
    public boolean shouldPlace(PlacementContext context, RandomSource random, BlockPos origin) {
        if (context.getLevel().getLevel().dimension() != Level.OVERWORLD
                || context.generator() instanceof FlatLevelSource && !ChromaOptions.FLATGEN.getState())
            return false;
        int chunkX = Math.floorDiv(origin.getX(), 16);
        int chunkZ = Math.floorDiv(origin.getZ(), 16);
        return PylonFeature.isSelectedChunk(context.getLevel().getSeed(), chunkX, chunkZ);
    }

    @Override
    public MapCodec<PylonGridPlacement> codec() {
        return CODEC;
    }
}