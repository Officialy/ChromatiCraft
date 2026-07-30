package reika.chromaticraft.block.dye26;

import com.mojang.serialization.MapCodec;
import java.awt.Color;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.ParticleUtils;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** Rainbow-tree leaves retain the original position-varying hue instead of biome foliage tint. */
public final class BlockRainbowLeaf extends LeavesBlock {
    private final MapCodec<BlockRainbowLeaf> codec = MapCodec.unit(this);

    public BlockRainbowLeaf(BlockBehaviour.Properties properties) {
        super(0.02F, properties);
    }

    public static int getTintColor(BlockPos pos) {
        int scale = 32;
        double distance = Math.sqrt(pos.getX() * (double)pos.getX() + pos.getY() * 3D * pos.getY() * 3D
                + (pos.getZ() + pos.getX()) * (double)(pos.getZ() + pos.getX()));
        float hue = (float)(distance % scale) / scale;
        return 0xFF000000 | (Color.HSBtoRGB(hue, 0.7F, 1F) & 0xFFFFFF);
    }

    @Override
    public MapCodec<? extends BlockRainbowLeaf> codec() {
        return codec;
    }
    @Override
    protected void spawnFallingLeavesParticle(Level level, BlockPos pos, RandomSource random) {
        ParticleUtils.spawnParticleBelow(level, pos, random,
                ColorParticleOption.create(ParticleTypes.TINTED_LEAVES, getTintColor(pos)));
    }
}