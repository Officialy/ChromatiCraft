/*******************************************************************************
 * @author Reika Kalseki
 * Copyright 2017. All rights reserved.
 * Distribution requires explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.items.tools;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import reika.chromaticraft.base.ItemChromaTool;

/** V33a EFFICIENCY: reduces powered-tool drain and ability costs while carried. */
public class ItemEfficiencyCrystal extends ItemChromaTool {
    private static final String TAG = "last_efficiency";

    public ItemEfficiencyCrystal(Properties properties) { super(properties); }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity owner, EquipmentSlot slot) {
        if (owner instanceof Player player)
            player.getPersistentData().putLong(TAG, level.getGameTime());
    }

    public static boolean isActive(Player player) {
        if (player.level().getGameTime()-player.getPersistentData().getLongOr(TAG, Long.MIN_VALUE/2) < 20)
            return true;
        // Server inventory ticks do not sync entity persistent data to the client.
        if (player.level().isClientSide()) {
            for (ItemStack stack : player.getInventory())
                if (stack.getItem() instanceof ItemEfficiencyCrystal)
                    return true;
            return ItemAuraPouch.containsActive(player, stack -> stack.getItem() instanceof ItemEfficiencyCrystal);
        }
        return false;
    }
}
