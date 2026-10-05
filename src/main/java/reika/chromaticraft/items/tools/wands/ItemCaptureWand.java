/*******************************************************************************
 * @author Reika Kalseki
 * Copyright 2017. All rights reserved.
 * Distribution requires explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.items.tools.wands;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.neoforged.neoforge.common.Tags;
import reika.chromaticraft.base.ItemWandBase;
import reika.chromaticraft.magic.ElementTagCompound;
import reika.chromaticraft.magic.PlayerElementBuffer;
import reika.chromaticraft.registry.ChromaSounds;
import reika.chromaticraft.registry.CrystalElement;
import reika.dragonapi.interfaces.item.EntityCapturingItem;
import reika.dragonapi.libraries.registry.ReikaItemHelper;

import java.util.function.Consumer;

/** V33a CAPTURE: stores a non-boss mob, paying a tenth of the capture cost each held tick. */
public class ItemCaptureWand extends ItemWandBase implements EntityCapturingItem {
    private static final String MOB = "mob";
    private final ElementTagCompound perTick;

    public ItemCaptureWand(Properties properties) {
        super(properties);
        addEnergyCost(CrystalElement.PINK, 10);
        addEnergyCost(CrystalElement.LIME, 20);
        addEnergyCost(CrystalElement.LIGHTGRAY, 50);
        perTick = getEnergy(0.1F);
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity owner, EquipmentSlot slot) {
        if (slot == EquipmentSlot.MAINHAND && owner instanceof Player player && hasEntity(stack) && !player.isCreative()) {
            if (PlayerElementBuffer.instance.playerHas(player, perTick))
                PlayerElementBuffer.instance.removeFromPlayer(player, perTick);
            else {
                releaseMob(stack, player, level, player.blockPosition());
                ChromaSounds.ERROR.playSound(player);
            }
        }
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (hasEntity(player.getItemInHand(hand))) {
            var look = player.getLookAngle();
            BlockPos pos = BlockPos.containing(player.getX()+look.x*2, player.getY()+look.y*5, player.getZ()+look.z*2);
            if (level instanceof ServerLevel server)
                releaseMob(player.getItemInHand(hand), player, server, pos);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!hasEntity(context.getItemInHand()) || context.getPlayer() == null)
            return InteractionResult.PASS;
        if (context.getLevel() instanceof ServerLevel level)
            return releaseMob(context.getItemInHand(), context.getPlayer(), level,
                    context.getClickedPos().relative(context.getClickedFace())) ? InteractionResult.SUCCESS : InteractionResult.FAIL;
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (hasEntity(stack) || !(target instanceof Mob mob) || target.getType().builtInRegistryHolder().is(Tags.EntityTypes.BOSSES)
                || !sufficientEnergy(player))
            return InteractionResult.PASS;
        if (target.level().isClientSide()) return InteractionResult.SUCCESS;
        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, target.registryAccess());
        if (!mob.saveAsPassenger(output)) return InteractionResult.FAIL;
        CompoundTag data = output.buildResult();
        // Upstream saved the captured entity, not its passengers. They remain real entities in the world.
        data.remove("Passengers");
        ReikaItemHelper.updateStackTag(stack, tag -> tag.put(MOB, data));
        drainPlayer(player);
        mob.discard();
        return InteractionResult.SUCCESS;
    }

    private boolean releaseMob(ItemStack stack, Player player, ServerLevel level, BlockPos pos) {
        Mob mob = getMob(stack, level);
        if (mob == null) return false;
        mob.setPos(pos.getX()+0.5, pos.getY(), pos.getZ()+0.5);
        if (!level.addFreshEntity(mob)) return false;
        drainPlayer(player);
        ReikaItemHelper.updateStackTag(stack, tag -> tag.remove(MOB));
        return true;
    }

    public static Mob getMob(ItemStack stack, Level level) {
        CompoundTag tag = ReikaItemHelper.getStackTag(stack);
        if (tag == null || !tag.contains(MOB)) return null;
        var input = TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), tag.getCompoundOrEmpty(MOB));
        Entity entity = EntityType.create(input, level, new net.minecraft.world.entity.EntitySpawnRequest(
                EntitySpawnReason.LOAD, true)).orElse(null);
        return entity instanceof Mob mob ? mob : null;
    }

    @Override public boolean hasEntity(ItemStack stack) {
        CompoundTag tag = ReikaItemHelper.getStackTag(stack);
        return tag != null && tag.contains(MOB);
    }

    @Override public String currentEntityName(ItemStack stack) {
        CompoundTag tag = ReikaItemHelper.getStackTag(stack);
        return tag != null ? tag.getCompoundOrEmpty(MOB).getStringOr("id", "") : "";
    }

    @Override public boolean isFoil(ItemStack stack) { return hasEntity(stack); }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
            Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        if (!hasEntity(stack)) {
            tooltip.accept(Component.translatable("item.chromaticraft.capture_star.empty"));
        } else {
            tooltip.accept(Component.translatable("item.chromaticraft.capture_star.mob", currentEntityName(stack)));
            CompoundTag mob = ReikaItemHelper.getStackTag(stack).getCompoundOrEmpty(MOB);
            tooltip.accept(Component.translatable("item.chromaticraft.capture_star.health",
                    Math.round(mob.getFloatOr("Health", 0)/2)));
        }
    }
}
