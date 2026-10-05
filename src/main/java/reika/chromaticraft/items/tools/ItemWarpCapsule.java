/*******************************************************************************
 * @author Reika Kalseki
 * Copyright 2017. All rights reserved.
 * Distribution requires explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.items.tools;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import reika.chromaticraft.base.ItemChromaTool;
import reika.chromaticraft.registry.ChromaSounds;
import reika.dragonapi.libraries.mathsci.ReikaPhysicsHelper;
import reika.dragonapi.libraries.registry.ReikaItemHelper;

import java.util.Locale;
import java.util.function.Consumer;

/** V33a WARPCAPSULE: bind once, then consume to recall within 2000 blocks under open sky. */
public class ItemWarpCapsule extends ItemChromaTool {
    public static final int MAXRANGE = 2000;

    public ItemWarpCapsule(Properties properties) { super(properties); }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!isBound(stack)) {
            if (!level.isClientSide())
                ReikaItemHelper.updateStackTag(stack, tag -> {
                    CompoundTag pos = new CompoundTag();
                    pos.putDouble("x", player.getX());
                    pos.putDouble("y", player.getY());
                    pos.putDouble("z", player.getZ());
                    tag.put("pos", pos);
                    tag.putString("dim", level.dimension().identifier().toString());
                });
        } else if (player instanceof ServerPlayer serverPlayer)
            teleport(serverPlayer, stack, hand);
        return InteractionResult.SUCCESS;
    }

    public static boolean isBound(ItemStack stack) {
        CompoundTag tag = ReikaItemHelper.getStackTag(stack);
        return tag != null && tag.getCompound("pos").isPresent();
    }

    public static Vec3 position(ItemStack stack) {
        CompoundTag pos = ReikaItemHelper.getStackTag(stack).getCompoundOrEmpty("pos");
        return new Vec3(pos.getDoubleOr("x", 0), pos.getDoubleOr("y", 0), pos.getDoubleOr("z", 0));
    }

    private static void teleport(ServerPlayer player, ItemStack stack, InteractionHand hand) {
        Vec3 pos = position(stack);
        var level = player.level();
        String dimension = ReikaItemHelper.getStackTag(stack).getStringOr("dim", "");
        if (!dimension.equals(level.dimension().identifier().toString())
                || !level.canSeeSky(player.blockPosition()) || player.position().distanceTo(pos) > MAXRANGE)
            return;
        var random = player.getRandom();
        double x = 0, y = pos.y, z = 0;
        AABB shape = player.getBoundingBox().deflate(0.25);
        // V33a's two rejection samples, bounded to avoid hanging when a saved target becomes solid.
        boolean found = false;
        for (int attempt = 0; attempt < 4096; attempt++) {
            x = pos.x+(random.nextDouble()*2-1)*12;
            z = pos.z+(random.nextDouble()*2-1)*12;
            if (level.noCollision(player, shape.move(x-player.getX(), y-player.getY(), z-player.getZ()))) {
                found = true;
                break;
            }
        }
        if (!found) return;
        found = false;
        for (int attempt = 0; attempt < 4096; attempt++) {
            y = pos.y+1+random.nextDouble()*8;
            if (level.noCollision(player, shape.move(x-player.getX(), y-player.getY(), z-player.getZ()))) {
                found = true;
                break;
            }
        }
        if (!found) return;
        player.teleportTo(x, y, z);
        double[] velocity = ReikaPhysicsHelper.polarToCartesian(1+random.nextDouble()*2,
                random.nextDouble()*360, random.nextDouble()*360);
        player.setDeltaMovement(new Vec3(velocity[0], velocity[1], velocity[2]));
        player.syncVelocity = true;
        player.fallDistance = random.nextFloat()*5+(float)Math.max(-velocity[1]*5, 0);
        // Upstream consumed the held tool even in creative.
        player.setItemInHand(hand, ItemStack.EMPTY);
        for (int i = 0; i < 2; i++)
            level.playSound(null, x, y, z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 2, 1);
        ChromaSounds.RIFT.playSound(player, 1, 0.5F);
        ChromaSounds.RIFT.playSound(player, 1, 0.75F);
        player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 150, 5));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
            Consumer<Component> tooltip, TooltipFlag flag) {
        if (isBound(stack)) {
            Vec3 pos = position(stack);
            tooltip.accept(Component.translatable("item.chromaticraft.recall_tablet.bound",
                    String.format(Locale.ROOT, "(%.1f, %.1f, %.1f)", pos.x, pos.y, pos.z),
                    ReikaItemHelper.getStackTag(stack).getStringOr("dim", "")));
        }
    }
}
