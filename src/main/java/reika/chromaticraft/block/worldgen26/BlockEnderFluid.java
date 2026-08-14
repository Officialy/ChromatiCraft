package reika.chromaticraft.block.worldgen26;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;

import reika.chromaticraft.registry.ChromaOptions;
import reika.dragonapi.libraries.java.ReikaRandomHelper;

/**
 * V33a {@code BlockLiquidEnder}: the Liquid Ender pool fluid used by the Ender Forest and by the
 * portal's four corner basins.
 *
 * <p>Its one gameplay behavior is the "ender effect" toss: any entity inside a cell is thrown by a
 * random horizontal impulse of up to one block per tick, given a small upward kick, has its fall
 * distance cleared, and — for living entities — plays the enderman teleport cue. The whole effect is
 * behind {@link ChromaOptions#ENDEREFFECT} exactly as upstream, so a server can disable it.
 *
 * <p>Not ported: {@code velocityToAddToEntity} multiplied the flow vector by {@code quantaPerBlock*4}
 * to make the current violently fast. 26.2 applies flow through {@link FlowingFluid#getFlow} inside
 * {@code Entity.updateFluidHeightAndDoFluidPushing}, whose multiplier comes from
 * {@code FluidType.motionScale}; the scaled push is expressed there instead (see
 * {@code ChromaFluids.ENDER_TYPE}) rather than by overriding a method that no longer exists.
 */
public final class BlockEnderFluid extends LiquidBlock {

    public BlockEnderFluid(FlowingFluid fluid, Properties properties) {
        super(fluid, properties);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity,
            InsideBlockEffectApplier effects, boolean precise) {
        if (!ChromaOptions.ENDEREFFECT.getState())
            return;
        double v = 1;
        entity.setDeltaMovement(
                ReikaRandomHelper.getRandomPlusMinus(0, v),
                entity.getDeltaMovement().y + 0.2,
                ReikaRandomHelper.getRandomPlusMinus(0, v));
        entity.fallDistance = 0;
        if (entity instanceof LivingEntity)
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                    SoundEvents.ENDERMAN_TELEPORT, SoundSource.BLOCKS, 0.5F, 1.0F);
    }
}
