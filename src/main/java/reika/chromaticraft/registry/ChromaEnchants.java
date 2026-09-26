/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.registry;

import java.util.Locale;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Util;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.core.component.DataComponents;

import it.unimi.dsi.fastutil.objects.Object2IntMap;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.base.ChromaticEnchantment;
import reika.chromaticraft.magic.enchantment.EnchantmentAggroMask;
import reika.chromaticraft.magic.enchantment.EnchantmentAirMiner;
import reika.chromaticraft.magic.enchantment.EnchantmentAutoCollect;
import reika.chromaticraft.magic.enchantment.EnchantmentBetterEfficiency;
import reika.chromaticraft.magic.enchantment.EnchantmentBossKill;
import reika.chromaticraft.magic.enchantment.EnchantmentDataKeeper;
import reika.chromaticraft.magic.enchantment.EnchantmentEnderLock;
import reika.chromaticraft.magic.enchantment.EnchantmentFastSinking;
import reika.chromaticraft.magic.enchantment.EnchantmentHarvestLevel;
import reika.chromaticraft.magic.enchantment.EnchantmentPhasingSequence;
import reika.chromaticraft.magic.enchantment.EnchantmentRareLoot;
import reika.chromaticraft.magic.enchantment.EnchantmentUseRepair;
import reika.chromaticraft.magic.enchantment.EnchantmentWeaponAOE;

/**
 * V33a {@code ChromaEnchants}: the thirteen ChromatiCraft enchantments. Each is a data-driven
 * {@code chromaticraft:<id>} enchantment ({@link #bootstrap}) whose effects are code, in {@code ChromaEnchantEvents},
 * reading the level through {@link #getLevel}; the behaviour object ({@link #getEnchantment}) holds V33a's formulas.
 *
 * <p>V33a's {@code getLevel} capped an over-enchanted tool (one carrying a level above the maximum, as from an
 * anvil or command) at the maximum and flagged it with a {@code cc_overenchant} NBT tag, which the client drew
 * particles for. The cap stays; the flag is now computed from the enchantments themselves
 * ({@link #isOverenchanted}) rather than written onto the stack every time a level is read.
 */
public enum ChromaEnchants {

	WEAPONAOE("weapon_aoe", new EnchantmentWeaponAOE()),
	AGGROMASK("aggro_mask", new EnchantmentAggroMask()),
	ENDERLOCK("ender_lock", new EnchantmentEnderLock()),
	USEREPAIR("use_repair", new EnchantmentUseRepair()),
	RARELOOT("rare_loot", new EnchantmentRareLoot()),
	FASTSINK("fast_sink", new EnchantmentFastSinking()),
	HARVESTLEVEL("harvest_level", new EnchantmentHarvestLevel()),
	AIRMINER("air_miner", new EnchantmentAirMiner()),
	PHASING("phasing", new EnchantmentPhasingSequence()),
	BOSSKILL("boss_kill", new EnchantmentBossKill()),
	AUTOCOLLECT("auto_collect", new EnchantmentAutoCollect()),
	DATAKEEP("data_keep", new EnchantmentDataKeeper()),
	MINETIME("mine_time", new EnchantmentBetterEfficiency());

	private final ResourceKey<Enchantment> key;
	private final ChromaticEnchantment enchantment;

	public static final ChromaEnchants[] enchantmentList = values();

	private ChromaEnchants(String id, ChromaticEnchantment e) {
		key = ResourceKey.create(Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, id));
		enchantment = e;
	}

	public ResourceKey<Enchantment> key() {
		return key;
	}

	/** The behaviour side: maximum level, target items, formulas and per-level visibility. */
	public ChromaticEnchantment getEnchantment() {
		return enchantment;
	}

	/** The V33a lexicon ({@code enchants.xml}) node, V33a's {@code chroma.<name>} key. */
	public String getLexiconKey() {
		return this.name().toLowerCase(Locale.ENGLISH);
	}

	public String getUnlocalizedName() {
		return Util.makeDescriptionId("enchantment", key.identifier());
	}

	public Component getBasicName() {
		return Component.translatable(this.getUnlocalizedName());
	}

	public Holder<Enchantment> getHolder(HolderLookup.Provider registries) {
		return registries.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
	}

	/** The level on the stack as written, uncapped. No registry is needed: the stack's holders carry their keys. */
	public int getRawLevel(ItemStack tool) {
		if (tool == null || tool.isEmpty())
			return 0;
		ItemEnchantments enchants = tool.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
		for (Object2IntMap.Entry<Holder<Enchantment>> e : enchants.entrySet()) {
			if (e.getKey().is(key))
				return e.getIntValue();
		}
		return 0;
	}

	/** The effective level: V33a caps an over-enchanted tool at the maximum. */
	public int getLevel(ItemStack tool) {
		return Math.min(this.getRawLevel(tool), enchantment.getMaxLevel());
	}

	public boolean isOverenchanted(ItemStack tool) {
		return this.getRawLevel(tool) > enchantment.getMaxLevel();
	}

	/** Whether any ChromatiCraft enchantment on the stack is above its maximum (V33a's {@code cc_overenchant}). */
	public static boolean hasOverenchantment(ItemStack tool) {
		for (ChromaEnchants e : enchantmentList) {
			if (e.isOverenchanted(tool))
				return true;
		}
		return false;
	}

	/**
	 * The registry entries. Weight is 1 (the codec's floor; V33a's was 0, and it only weighs table and loot rolls,
	 * which none of these take part in); enchantability is 1.7.10's default {@code 1+10*level} to {@code +5};
	 * the anvil cost is 0, as V33a's weight 0 made it in 1.7.10's {@code ContainerRepair}.
	 */
	public static void bootstrap(BootstrapContext<Enchantment> context) {
		HolderGetter<Item> items = context.lookup(Registries.ITEM);
		for (ChromaEnchants e : enchantmentList) {
			ChromaticEnchantment ench = e.enchantment;
			context.register(e.key, Enchantment.enchantment(Enchantment.definition(
					items.getOrThrow(ench.getTarget().items), 1, ench.getMaxLevel(),
					Enchantment.dynamicCost(11, 10), Enchantment.dynamicCost(16, 10), 0, ench.getTarget().slots))
					.build(e.key.identifier()));
		}
	}

}
