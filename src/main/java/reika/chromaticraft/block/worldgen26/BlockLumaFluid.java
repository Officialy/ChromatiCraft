package reika.chromaticraft.block.worldgen26;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import reika.chromaticraft.magic.progression.ProgressStage;

/** V33a ethereal Luma: luminous, breathable fluid found throughout Luminous Cliffs. */
public final class BlockLumaFluid extends LiquidBlock {
    public BlockLumaFluid(FlowingFluid fluid, Properties properties) {
        super(fluid, properties);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity,
            InsideBlockEffectApplier effects, boolean precise) {
        if (entity instanceof LivingEntity living && living.getAirSupply() < living.getMaxAirSupply())
            living.setAirSupply(living.getAirSupply() + 1);
        if (!level.isClientSide() && entity instanceof Player player && state.getValue(LEVEL) == 0)
            ProgressStage.LUMA.stepPlayerTo(player);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextBoolean()) {
            int color = random.nextBoolean() ? 0xFFC89CF4 : 0xFFF29BF2;
            level.addParticle(new DustParticleOptions(color, 0.8F), pos.getX()+random.nextDouble(),
                    pos.getY()+0.65+random.nextDouble()*0.35, pos.getZ()+random.nextDouble(), 0, 0.015, 0);
        }
    }
}