package reika.chromaticraft.block.dye26;

import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.ParticleUtils;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import reika.chromaticraft.magic.progression.ProgressStage;
import reika.chromaticraft.registry.CrystalElement;

/** A concrete registry identity for one legacy dye-leaf metadata colour. */
public final class BlockDyeLeaf extends LeavesBlock {
    private final CrystalElement element;


    public BlockDyeLeaf(BlockBehaviour.Properties properties, CrystalElement element) {
        super(net.minecraft.world.level.block.sounds.AmbientLeavesBlockSoundPlayer.noAmbientSound(), properties);
        this.element = element;
    }

    public CrystalElement getElement() {
        return element;
    }

    /**
     * V33a {@code BlockDyeLeaf.getColor(meta, false)}: the vanilla dye colour brightened. Upstream used
     * the dye palette ({@code ReikaDyeHelper}), not the crystal palette; the two differ for most colours
     * (red is 0x993333 as a dye, 0xFF0000 as a crystal).
     */
    public int getTintColor() {
        return 0xFF000000 | reika.dragonapi.libraries.registry.ReikaDyeHelper.dyes[element.ordinal()]
                .getJavaColor().brighter().getRGB();
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        super.animateTick(state, level, pos, random);
        if (random.nextFloat() < 0.01F)
            ParticleUtils.spawnParticleBelow(level, pos, random,
                    ColorParticleOption.create(ParticleTypes.TINTED_LEAVES, getTintColor()));
    }

    /**
     * V33a {@code BlockDyeLeaf.removedByPlayer}: harvesting dye leaves is the only grant site for
     * {@link ProgressStage#DYETREE}. 26.2 renamed the hook to {@code playerWillDestroy}, which fires
     * before the block is removed; V33a's {@code willHarvest} guard is the equivalent of not
     * granting in creative, where the block is destroyed without being harvested.
     */
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && !player.getAbilities().instabuild)
            ProgressStage.DYETREE.stepPlayerTo(player);
        return super.playerWillDestroy(level, pos, state, player);
    }
}
