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

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.alchemy.PotionContents;
import reika.chromaticraft.magic.CrystalPotionController;
import reika.chromaticraft.registry.ChromaItems;
import reika.chromaticraft.registry.CrystalElement;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * V33a {@code ItemCrystalPotion} (ChromaItems.POTION): the potions the Crystal Brewery makes for the colours vanilla has
 * no potion for -- water breathing, haste, jump boost, resistance and the better-saturation effect. It is the vanilla
 * potion bottle with its liquid in the element's colour.
 *
 * <p>V33a kept the colour and its extended, boosted and splash bits in the damage value. In 26.2 each colour is its own
 * item, the splash form a separate {@link ItemSplashCrystalPotion}, and the effect -- 3:00 normally, 8:00 extended,
 * level II boosted -- is written into {@code POTION_CONTENTS} (with the element colour) when a stack is made by
 * {@link #create}, so drinking, splashing and the effect tooltip are vanilla's. The effect is not a default component:
 * ChromatiCraft's own effects are not bound yet when items register.
 */
public class ItemCrystalPotion extends PotionItem {

	private final CrystalElement color;

	public ItemCrystalPotion(CrystalElement color, Properties properties) {
		super(properties);
		this.color = color;
	}

	public CrystalElement getColor() {
		return color;
	}

	/** V33a {@code getSubItems} (only colours needing a custom potion) and everything the Brewery can make. */
	public static boolean isCrystalPotionColor(CrystalElement color) {
		return CrystalPotionController.instance.requiresCustomPotion(color);
	}

	/** V33a {@code getEffects}: the colour's effect at 3:00, lengthened to 8:00 if extended, level II if boosted. */
	public static PotionContents contents(CrystalElement color, boolean extended, boolean boosted) {
		MobEffectInstance pot = CrystalPotionController.instance.getEffectFromColor(color, 3600, 0, false);
		List<MobEffectInstance> effects = new ArrayList<>();
		if (pot != null) {
			// V33a rebuilt the effect with the three-argument constructor for these, which is not ambient.
			if (extended)
				pot = new MobEffectInstance(pot.getEffect(), 9600, pot.getAmplifier());
			if (boosted)
				pot = new MobEffectInstance(pot.getEffect(), pot.getDuration(), 1);
			effects.add(pot);
		}
		return new PotionContents(Optional.empty(), Optional.of(color.color() & 0xFFFFFF), effects, Optional.empty());
	}

	public static ItemStack create(CrystalElement color, boolean splash, boolean extended, boolean boosted) {
		var holder = splash ? ChromaItems.SPLASH_CRYSTAL_POTIONS.get(color) : ChromaItems.CRYSTAL_POTIONS.get(color);
		ItemStack is = new ItemStack(holder.get());
		is.set(DataComponents.POTION_CONTENTS, contents(color, extended, boosted));
		return is;
	}

	/** Whether a crystal potion stack is the 8:00 form. */
	public static boolean isExtended(ItemStack is) {
		PotionContents pc = is.get(DataComponents.POTION_CONTENTS);
		return pc != null && !pc.customEffects().isEmpty() && pc.customEffects().getFirst().getDuration() > 3600;
	}

	/** Whether a crystal potion stack is the level II form. */
	public static boolean isBoosted(ItemStack is) {
		PotionContents pc = is.get(DataComponents.POTION_CONTENTS);
		return pc != null && !pc.customEffects().isEmpty() && pc.customEffects().getFirst().getAmplifier() > 0;
	}

	/** The seven V33a {@code getSubItems} variants of one colour. */
	public static List<ItemStack> creativeStacks(CrystalElement color) {
		return List.of(create(color, false, false, false), create(color, false, true, false),
				create(color, false, false, true), create(color, true, false, false), create(color, true, true, false),
				create(color, true, false, true), create(color, false, true, true));
	}

	@Override
	public ItemStack getDefaultInstance() {
		return create(color, false, false, false);
	}

	/** V33a: "&lt;Element&gt; Crystal Potion", whatever the effect. */
	@Override
	public Component getName(ItemStack is) {
		return Component.translatable(this.getDescriptionId());
	}

}
