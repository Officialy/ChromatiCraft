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
        // V33a BlockLightedLeaf: shouldRandomTick() and decays() both return false. These never rot.
        // The default state has to say so as well as isRandomlyTicking below, and for two reasons.
        // Persistent is what stops this leaf decaying; a distance of one is what stops it poisoning its
        // neighbours -- LeavesBlock.getDistanceAt reads a leaf's DISTANCE straight out of its state, so
        // a glow leaf left at the default 7 tells every ordinary leaf touching it that it is seven from
        // the nearest log, and those then rot. That is why the decay was worst exactly where glowing
        // leaves met normal ones.
        this.registerDefaultState(this.defaultBlockState()
                .setValue(DISTANCE, 1).setValue(PERSISTENT, true));
    }

    /** V33a shouldRandomTick(): false. Belt and braces with the persistent default state above. */
    @Override
    protected boolean isRandomlyTicking(net.minecraft.world.level.block.state.BlockState state) {
        return false;
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
