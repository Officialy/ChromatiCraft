package reika.chromaticraft.block.worldgen26;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.ParticleUtils;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** The animated, light-emitting leaf pockets embedded in V33a Luminous Cliffs trees. */
public final class BlockGlowingLeaf extends LeavesBlock {
    private final MapCodec<BlockGlowingLeaf> codec = MapCodec.unit(this);

    public BlockGlowingLeaf(BlockBehaviour.Properties properties) {
        super(0.01F, properties);
    }

    @Override
    public MapCodec<? extends BlockGlowingLeaf> codec() {
        return codec;
    }

    @Override
    protected void spawnFallingLeavesParticle(Level level, BlockPos pos, RandomSource random) {
        ParticleUtils.spawnParticleBelow(level, pos, random,
                ColorParticleOption.create(ParticleTypes.TINTED_LEAVES, 0xffb9ffff));
    }
}
