/*******************************************************************************
 * @author Reika Kalseki
 * Copyright 2017
 * All rights reserved.
 * Distribution requires the owner's explicit prior permission.
 ******************************************************************************/
package reika.chromaticraft.items.tools.wands;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.event.EventHooks;
import reika.chromaticraft.base.ItemWandBase;
import reika.chromaticraft.block.BlockHoverBlock;
import reika.chromaticraft.block.BlockHoverBlock.HoverType;
import reika.chromaticraft.network.ChromaNetwork;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.registry.ChromaSounds;
import reika.chromaticraft.registry.CrystalElement;
import reika.dragonapi.libraries.ReikaPlayerAPI;
import reika.dragonapi.libraries.registry.ReikaItemHelper;

/** V33a Hover Star: eight temporary motion-field blocks, with the original four selectable modes. */
public final class ItemFlightWand extends ItemWandBase {
    public ItemFlightWand(Properties properties) {
        super(properties);
        addEnergyCost(CrystalElement.BLACK, 5);
        addEnergyCost(CrystalElement.LIME, 20);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!sufficientEnergy(player) || !trigger(level, player.getItemInHand(hand), player,
                ReikaPlayerAPI.getLookedAtBlock(player, 8, true))) {
            if (player instanceof ServerPlayer server)
                PacketDistributor.sendToPlayer(server, new ChromaNetwork.OpenHoverWand(hand == InteractionHand.OFF_HAND));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getPlayer() == null) return InteractionResult.PASS;
        if (context.getLevel().isClientSide()) return InteractionResult.SUCCESS;
        return sufficientEnergy(context.getPlayer()) && trigger(context.getLevel(), context.getItemInHand(),
                context.getPlayer(), new BlockHitResult(context.getClickLocation(), context.getClickedFace(),
                        context.getClickedPos(), false)) ? InteractionResult.SUCCESS : InteractionResult.PASS;
    }

    private boolean trigger(Level level, ItemStack stack, Player player, BlockHitResult hit) {
        if (hit == null || hit.getType() != HitResult.Type.BLOCK || !level.getBlockState(hit.getBlockPos().above()).isAir())
            return false;
        HoverType mode = getMode(stack);
        BlockPos base = hit.getBlockPos();
        boolean placed = false;
        for (int i = 1; i <= 8; i++) {
            BlockPos pos = base.above(i);
            if (level.isOutsideBuildHeight(pos) || !level.getBlockState(pos).isAir()) continue;
            if (!player.mayUseItemAt(pos, hit.getDirection(), stack)) continue;
            BlockSnapshot snapshot = BlockSnapshot.create(level.dimension(), level, pos);
            if (level.setBlock(pos, ChromaBlocks.HOVER.get().defaultBlockState().setValue(BlockHoverBlock.TYPE, mode)
                    .setValue(BlockHoverBlock.DECAY, BlockHoverBlock.Decay.ARMED), 3)) {
                if (EventHooks.onBlockPlace(player, snapshot, hit.getDirection())) {
                    snapshot.restore();
                    continue;
                }
                // V33a checks the base cost once, then drains for every placed field; the buffer exempts creative users.
                drainPlayer(player, costFactor(mode));
                placed = true;
            }
        }
        if (placed) {
            ChromaSounds.CAST.playSound(player);
            if (level instanceof ServerLevel server) for (int i = 0; i <= 8; i++) {
                double d = i / 8D;
                server.sendParticles(ParticleTypes.ENCHANT, player.getX()+d*(base.getX()-player.getX()),
                        player.getY()+d*(base.getY()-player.getY()), player.getZ()+d*(base.getZ()-player.getZ()),
                        0, (player.getRandom().nextDouble()*2-1)*0.03125,
                        (player.getRandom().nextDouble()*2-1)*0.03125, (player.getRandom().nextDouble()*2-1)*0.03125, 1);
            }
        }
        return placed;
    }

    public static HoverType getMode(ItemStack stack) {
        var data = ReikaItemHelper.getStackTag(stack);
        int index = data == null ? HoverType.ELEVATE.ordinal() : data.getIntOr("mode", HoverType.ELEVATE.ordinal());
        return index >= 0 && index < HoverType.values().length ? HoverType.values()[index] : HoverType.ELEVATE;
    }

    public static void setMode(ItemStack stack, HoverType mode) {
        ReikaItemHelper.updateStackTag(stack, data -> data.putInt("mode", mode.ordinal()));
    }

    public static float costFactor(HoverType mode) {
        return switch (mode) { case ELEVATE -> 1.5F; case FAST_ELEVATE -> 2F; default -> 1F; };
    }
}

