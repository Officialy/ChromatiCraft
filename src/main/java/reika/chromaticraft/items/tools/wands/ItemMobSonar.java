/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.items.tools.wands;

import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;
import reika.chromaticraft.auxiliary.MobSonar.EntitySonarType;
import reika.chromaticraft.base.ItemWandBase;
import reika.chromaticraft.modinterface.SatisforestrySonar;
import reika.chromaticraft.network.ChromaNetwork;
import reika.chromaticraft.registry.CrystalElement;

/** V33a Creature Sounding Star, with its original component-backed category flags. */
public final class ItemMobSonar extends ItemWandBase {
    public ItemMobSonar(Properties properties) {
        super(properties);
        addEnergyCost(CrystalElement.PINK, 4);
        addEnergyCost(CrystalElement.BLUE, 4);
    }

    public static int getTypeFlags(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag()
                .getIntOr("typeFlags", EntitySonarType.getAllFlags());
    }

    public static void prepareData(ItemStack stack) {
        var tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!tag.contains("typeFlags")) {
            tag.putInt("typeFlags", EntitySonarType.getAllFlags());
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        prepareData(player.getItemInHand(hand));
        // Source does not gate the ping on sufficientEnergy; it drains the buffer on the server.
        if (level.isClientSide()) {
            reika.chromaticraft.client.ClientPayloadHandlers.mobSonarPing(player);
        } else {
            drainPlayer(player);
            if (player instanceof ServerPlayer server) SatisforestrySonar.nearestSpawn(server).ifPresent(point ->
                    PacketDistributor.sendToPlayer(server, new ChromaNetwork.DoggoSonar(point.pos(), point.entityType())));
        }
        return InteractionResult.SUCCESS;
    }
}
