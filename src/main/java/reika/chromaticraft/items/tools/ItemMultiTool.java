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
import net.minecraft.world.level.block.state.BlockState;
import reika.chromaticraft.api.interfaces.EnchantableItem;
import reika.chromaticraft.base.ItemChromaTool;

import java.util.Locale;

/** V33a's constant-speed Multitool: diamond harvesting, 900 uses, gold enchantability. */
public final class ItemMultiTool extends ItemChromaTool implements EnchantableItem {
    public ItemMultiTool(Properties properties) {
        super(properties.durability(900).enchantable(22));
    }

    @Override public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
        return Items.DIAMOND_PICKAXE.isCorrectToolForDrops(stack, state);
    }
    @Override public float getDestroySpeed(ItemStack stack, BlockState state) { return 4; }

    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity owner) {
        if (!level.isClientSide() && state.getDestroySpeed(level,pos)>0)
            stack.hurtAndBreak(1,owner,EquipmentSlot.MAINHAND);
        return true;
    }

    static boolean soulbound(Holder<Enchantment> enchantment) {
        return enchantment.unwrapKey().map(key -> key.identifier().getPath().toLowerCase(Locale.ROOT).contains("soulbound"))
                .orElse(false) || enchantment.value().description().getString().toLowerCase(Locale.ROOT).contains("soulbound");
    }

    @Override
    public TriState getEnchantValidity(Holder<Enchantment> enchantment, ItemStack stack) {
        return enchantment.is(Enchantments.UNBREAKING) || enchantment.is(Enchantments.EFFICIENCY)
                || enchantment.is(Enchantments.SILK_TOUCH) || enchantment.is(Enchantments.FORTUNE) || soulbound(enchantment)
                ? TriState.TRUE : TriState.FALSE;
    }
    @Override public TagKey<Item> getEnchantingCategory() { return ItemTags.MINING_ENCHANTABLE; }
    @Override public boolean supportsEnchantment(ItemStack stack, Holder<Enchantment> enchantment) {
        return getEnchantValidity(enchantment,stack)==TriState.TRUE;
    }
    @Override public boolean isPrimaryItemFor(ItemStack stack, Holder<Enchantment> enchantment) {
        return supportsEnchantment(stack,enchantment);
    }
}
