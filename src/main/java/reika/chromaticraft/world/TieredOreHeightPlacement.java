package reika.chromaticraft.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;

import java.util.function.Consumer;

/**
 * Preserves the original 50/50 deep/shallow bias over the expanded Overworld underground.
 * The low branches cover floor+4..-1 and floor+4..63; wide Overworld deposits use the full
 * build height. Nether and End retain the original absolute 0..127 band.
 */
public record TieredOreHeightPlacement(boolean deepBand) implements PlacementModifier {


    public static final MapCodec<TieredOreHeightPlacement> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.BOOL.fieldOf("deep_band").forGetter(TieredOreHeightPlacement::deepBand)
    ).apply(i, TieredOreHeightPlacement::new));

    @Override
    public void modify(PlacementContext context, RandomSource random, BlockPos origin, Consumer<BlockPos> output) {
        int y;
        if (context.getLevel().getLevel().dimension() == net.minecraft.world.level.Level.OVERWORLD) {
            int ceiling = deepBand ? context.getLevel().getMaxY() : random.nextBoolean()
                    ? reika.dragonapi.libraries.level.WorldgenHeight.undergroundY(context.getLevel(), 32) - 1 : 63;
            y = reika.dragonapi.libraries.level.WorldgenHeight.randomBetween(context.getLevel(), random,
                    reika.dragonapi.libraries.level.WorldgenHeight.caveFloor(context.getLevel()), ceiling);
        } else {
            y = deepBand ? random.nextInt(128) : random.nextBoolean() ? random.nextInt(32) : random.nextInt(64);
        }
        output.accept(new BlockPos(origin.getX(), y, origin.getZ()));
    }

    @Override
    public MapCodec<TieredOreHeightPlacement> codec() {
        return CODEC;
    }
}
