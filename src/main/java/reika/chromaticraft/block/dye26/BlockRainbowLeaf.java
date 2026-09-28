package reika.chromaticraft.block.dye26;

import java.awt.Color;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.ParticleUtils;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import reika.chromaticraft.entity.EntityBallLightning;
import reika.chromaticraft.registry.ChromaOptions;
import reika.chromaticraft.registry.CrystalElement;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** Rainbow-tree leaves retain the original position-varying hue instead of biome foliage tint. */
public final class BlockRainbowLeaf extends LeavesBlock {


    public BlockRainbowLeaf(BlockBehaviour.Properties properties) {
        super(net.minecraft.world.level.block.sounds.AmbientLeavesBlockSoundPlayer.noAmbientSound(), properties);
    }

    public static int getTintColor(BlockPos pos) {
        int scale = 32;
        double distance = Math.sqrt(pos.getX() * (double)pos.getX() + pos.getY() * 3D * pos.getY() * 3D
                + (pos.getZ() + pos.getX()) * (double)(pos.getZ() + pos.getX()));
        float hue = (float)(distance % scale) / scale;
        return 0xFF000000 | (Color.HSBtoRGB(hue, 0.7F, 1F) & 0xFFFFFF);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        super.animateTick(state, level, pos, random);
        if (random.nextFloat() < 0.02F)
            ParticleUtils.spawnParticleBelow(level, pos, random,
                    ColorParticleOption.create(ParticleTypes.TINTED_LEAVES, getTintColor(pos)));
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos,
            boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        if (level.getRandom().nextInt(10) == 0 && ChromaOptions.BALLLIGHTNING.getState()) {
            CrystalElement color = CrystalElement.elements[level.getRandom().nextInt(16)];
            level.addFreshEntity(new EntityBallLightning(level, color,
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5).setNoDrops());
        }
    }
}
