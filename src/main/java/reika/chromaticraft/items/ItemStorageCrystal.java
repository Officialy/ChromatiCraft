/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.items;

import java.util.function.Consumer;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;

import reika.chromaticraft.magic.ElementTagCompound;
import reika.chromaticraft.magic.progression.ProgressStage;
import reika.chromaticraft.registry.ChromaItems;
import reika.chromaticraft.registry.CrystalElement;
import reika.chromaticraft.registry.StorageCrystalTier;

/**
 * V33a's portable sixteen-colour lumen battery.
 *
 * <p>The old item used metadata 0-6 for Nula through Aru. In 26.2 every tier is a distinct
 * registered item and only the genuinely dynamic energy payload remains in custom data. The
 * static methods intentionally retain the original call surface so the charger, relay source and
 * charged-machine cluster can be ported without inventing an adapter API.</p>
 */
public final class ItemStorageCrystal extends Item {

	private static final String ENERGY_TAG = "energy";

	private final StorageCrystalTier tier;

	public ItemStorageCrystal(StorageCrystalTier tier, Properties properties) {
		super(properties.stacksTo(1));
		this.tier = tier;
	}

	public StorageCrystalTier tier() {
		return tier;
	}

	@Override
	public void inventoryTick(ItemStack stack, ServerLevel level, Entity owner, EquipmentSlot slot) {
		if (owner instanceof Player player)
			ProgressStage.STORAGE.stepPlayerTo(player);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
			Consumer<Component> tooltip, TooltipFlag flag) {
		ElementTagCompound stored = getStoredTags(stack);
		for (CrystalElement element : CrystalElement.elements) {
			int value = stored.getValue(element);
			if (value > 0)
				tooltip.accept(Component.literal(element.displayName + ": " + value));
		}
	}

	public static boolean isStorageCrystal(ItemStack stack) {
		return !stack.isEmpty() && stack.getItem() instanceof ItemStorageCrystal;
	}

	public static StorageCrystalTier getTier(ItemStack stack) {
		return stack.getItem() instanceof ItemStorageCrystal crystal ? crystal.tier : null;
	}

	public static int getCapacity(ItemStack stack) {
		StorageCrystalTier tier = getTier(stack);
		return tier != null ? tier.capacity() : 0;
	}

	public static void addEnergy(ItemStack stack, CrystalElement element, int value) {
		if (!isStorageCrystal(stack) || element == null || value <= 0)
			return;
		int capacity = getCapacity(stack);
		int current = getStoredEnergy(stack, element);
		writeEnergy(stack, element, (int)Math.min(capacity, (long)current + value));
	}

	public static void removeEnergy(ItemStack stack, CrystalElement element, int value) {
		if (!isStorageCrystal(stack) || element == null || value <= 0)
			return;
		writeEnergy(stack, element, Math.max(0, getStoredEnergy(stack, element) - value));
	}

	public static int getStoredEnergy(ItemStack stack, CrystalElement element) {
		if (!isStorageCrystal(stack) || element == null)
			return 0;
		return energyTag(stack).getIntOr(element.name(), 0);
	}

	public static int getTotalEnergy(ItemStack stack) {
		int total = 0;
		for (CrystalElement element : CrystalElement.elements)
			total += getStoredEnergy(stack, element);
		return total;
	}

	public static ElementTagCompound getStoredTags(ItemStack stack) {
		ElementTagCompound result = new ElementTagCompound();
		for (CrystalElement element : CrystalElement.elements) {
			int value = getStoredEnergy(stack, element);
			if (value > 0)
				result.setTag(element, value);
		}
		return result;
	}

	public static int getSpace(CrystalElement element, ItemStack stack) {
		return Math.max(0, getCapacity(stack) - getStoredEnergy(stack, element));
	}

	/** A charger may extract the crystal only once every one of its sixteen channels is full. */
	public static boolean isFull(ItemStack stack) {
		if (!isStorageCrystal(stack))
			return false;
		for (CrystalElement element : CrystalElement.elements) {
			if (getSpace(element, stack) > 0)
				return false;
		}
		return true;
	}

	public static ItemStack fullStack(StorageCrystalTier tier) {
		ItemStack stack = ChromaItems.storageCrystalStack(tier);
		for (CrystalElement element : CrystalElement.elements)
			addEnergy(stack, element, tier.capacity());
		return stack;
	}

	private static CompoundTag energyTag(ItemStack stack) {
		CustomData custom = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
		return custom.copyTag().getCompoundOrEmpty(ENERGY_TAG);
	}

	private static void writeEnergy(ItemStack stack, CrystalElement element, int value) {
		CompoundTag root = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
		CompoundTag energy = root.getCompoundOrEmpty(ENERGY_TAG).copy();
		if (value > 0)
			energy.putInt(element.name(), value);
		else
			energy.remove(element.name());
		root.put(ENERGY_TAG, energy);
		stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
	}
}
