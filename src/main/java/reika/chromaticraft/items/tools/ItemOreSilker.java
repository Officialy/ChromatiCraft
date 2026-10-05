/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.items.tools;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.TriState;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedStoneOreBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import reika.chromaticraft.api.interfaces.EnchantableItem;
import reika.chromaticraft.base.ItemChromaTool;

/** V33a Ore Harvester: automatic Silk Touch only on silk-harvestable ores and glowstone. */
public final class ItemOreSilker extends ItemChromaTool implements EnchantableItem {
    public ItemOreSilker(Properties properties) { super(properties.durability(180)); }

    public static void register() { NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, ItemOreSilker::harvest); }

    public static void harvest(BreakBlockEvent event) {
        var player = event.getPlayer();
        ItemStack stack = player.getMainHandItem();
        if (event.isCanceled() || !(stack.getItem() instanceof ItemOreSilker) || player.getAbilities().instabuild
                || !(player.level() instanceof ServerLevel level)) return;
        BlockState state = event.getState();
        if (!state.is(Blocks.GLOWSTONE) && !state.is(Tags.Blocks.ORES) && !(state.getBlock() instanceof RedStoneOreBlock)
                && !reika.dragonapi.modregistry.ModOreList.isModOre(state.getBlock())) return;
        ItemStack lootTool = stack.copy();
        lootTool.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH),1);
        BlockPos pos = event.getPos();
        var drops = Block.getDrops(state,level,pos,level.getBlockEntity(pos),player,lootTool);
        // Modern loot tables express canSilkHarvest: if Silk Touch does not yield the block, let
        // normal harvesting proceed. Never turn a non-silkable ore into a fabricated block item.
        if (drops.size()!=1 || !drops.getFirst().is(state.getBlock().asItem())) return;
        if (!level.removeBlock(pos,false)) return;
        event.setCanceled(true);
        event.setNotifyClient(true);
        level.levelEvent(2001,pos,Block.getId(state));
        Block.popResource(level,pos,drops.getFirst());
        stack.hurtAndBreak(1,player,EquipmentSlot.MAINHAND);
    }

    @Override public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
        return Items.DIAMOND_PICKAXE.isCorrectToolForDrops(stack,state);
    }
    @Override public float getDestroySpeed(ItemStack stack, BlockState state) {
        return state.is(BlockTags.MINEABLE_WITH_PICKAXE) || state.getBlock() instanceof RedStoneOreBlock ? 4 : 1;
    }
    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity owner) {
        if (!level.isClientSide() && state.getDestroySpeed(level,pos)>0)
            stack.hurtAndBreak(1,owner,EquipmentSlot.MAINHAND);
        return true;
    }
    @Override public TriState getEnchantValidity(Holder<Enchantment> enchantment, ItemStack stack) {
        return enchantment.is(Enchantments.UNBREAKING) || enchantment.is(Enchantments.EFFICIENCY)
                || ItemMultiTool.soulbound(enchantment) ? TriState.TRUE : TriState.FALSE;
    }
    @Override public TagKey<Item> getEnchantingCategory() { return ItemTags.MINING_ENCHANTABLE; }
    @Override public boolean supportsEnchantment(ItemStack stack, Holder<Enchantment> enchantment) {
        return getEnchantValidity(enchantment,stack)==TriState.TRUE;
    }
    @Override public boolean isPrimaryItemFor(ItemStack stack, Holder<Enchantment> enchantment) {
        return supportsEnchantment(stack,enchantment);
    }
}
