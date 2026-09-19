package reika.chromaticraft.block.worldgen26;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

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
 * <p>{@code velocityToAddToEntity} multiplied the flow vector by {@code quantaPerBlock*4}, making
 * this current thirty-two times the ordinary normalized-fluid push. In 26.2 that multiplier lives
 * on {@code FluidType.motionScale}; {@code ChromaFluids.ENDER_TYPE} carries the same 32x scale.
 */
public final class BlockEnderFluid extends LiquidBlock {

    public BlockEnderFluid(FlowingFluid fluid, Properties properties) {
        super(fluid, properties);
    }

    /**
     * 26.2 only invokes {@link #entityInside} for the shape returned here. Liquid blocks normally
     * return an empty shape because ordinary fluid interaction is handled by fluid pushing, which
     * meant the V33a toss callback was never reached and entities simply bogged down in the pool.
     * This shape is an interaction volume only; the block remains non-colliding.
     */
    @Override
    protected VoxelShape getEntityInsideCollisionShape(BlockState state, BlockGetter level,
            BlockPos pos, Entity entity) {
        return Shapes.block();
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
        entity.hurtMarked = true;
        if (entity instanceof LivingEntity)
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                    SoundEvents.ENDERMAN_TELEPORT, SoundSource.BLOCKS, 0.5F, 1.0F);
    }
}
