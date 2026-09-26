/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.base;

import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;

import reika.chromaticraft.registry.ChromaItemTags;

/**
 * V33a {@code ChromaticEnchantment}: the behaviour side of a ChromatiCraft enchantment. In 26.2 the enchantment
 * itself is a data-driven registry entry ({@code ChromaEnchants.bootstrap}); this class keeps what V33a's
 * {@code Enchantment} subclass knew -- its maximum level, what it may go on, the formulas its effects use, and
 * which levels a player has learnt of.
 *
 * <p>V33a's {@code canApplyAtEnchantingTable} was always false: none of these are in the enchanting-table,
 * random-loot or trade tags. V33a gave them weight 0, so an anvil charged nothing per level for them.
 */
public abstract class ChromaticEnchantment {

	/** V33a {@code EnumEnchantmentType}, plus the weapon-or-bow union five enchantments' {@code canApply} took. */
	public enum Target {
		/** {@code weapon}: {@code ItemSword} only. */
		WEAPON(ItemTags.SWORDS, EquipmentSlotGroup.MAINHAND),
		/** {@code bow}. */
		BOW(ItemTags.BOW_ENCHANTABLE, EquipmentSlotGroup.MAINHAND),
		/** {@code weapon.canEnchantItem || bow.canEnchantItem}. */
		WEAPON_OR_BOW(ChromaItemTags.ENCHANTABLE_WEAPON_OR_BOW, EquipmentSlotGroup.MAINHAND),
		/** {@code digger}: {@code ItemTool} -- pickaxes, axes and shovels, not hoes or shears. */
		DIGGER(ChromaItemTags.ENCHANTABLE_DIGGER, EquipmentSlotGroup.MAINHAND),
		/** {@code armor_feet}. */
		ARMOR_FEET(ItemTags.FOOT_ARMOR_ENCHANTABLE, EquipmentSlotGroup.FEET);

		public final TagKey<Item> items;
		public final EquipmentSlotGroup slots;

		Target(TagKey<Item> items, EquipmentSlotGroup slots) {
			this.items = items;
			this.slots = slots;
		}
	}

	private final Target target;

	protected ChromaticEnchantment(Target target) {
		this.target = target;
	}

	public final Target getTarget() {
		return target;
	}

	public abstract int getMaxLevel();

	/** Whether this player has learnt of the enchantment at this level (the Auto Enchanter only offers those). */
	public abstract boolean isVisibleToPlayer(Player ep, int level);

}
