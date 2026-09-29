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

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import reika.chromaticraft.magic.ElementTagCompound;
import reika.chromaticraft.magic.PlayerElementBuffer;
import reika.chromaticraft.auxiliary.interfaces.DynamicallyGeneratedSubpage;
import reika.chromaticraft.magic.progression.ProgressStage;
import reika.chromaticraft.registry.CrystalElement;

/**
 * V33a {@code ItemWandBase}: a Chromatic tool whose uses draw a fixed per-element energy cost from the player's
 * lumen buffer. The cost is listed with Shift held and on the item's lexicon page. V33a returned an
 * enchantability of 0, which is 26.x's default for an item without the {@code enchantable} component.
 */
public abstract class ItemWandBase extends ItemChromaTool implements DynamicallyGeneratedSubpage {

	private final ElementTagCompound energy = new ElementTagCompound();

	public ItemWandBase(Properties properties) {
		super(properties);
	}

	protected final void addEnergyCost(CrystalElement e, int amt) {
		energy.addValueToColor(e, amt);
	}

	protected final void drainPlayer(Player ep, float factor) {
		PlayerElementBuffer.instance.removeFromPlayer(ep, energy.copy().scale(factor));
	}

	protected final void drainPlayer(Player ep) {
		PlayerElementBuffer.instance.removeFromPlayer(ep, energy);
	}

	public final boolean sufficientEnergy(Player ep) {
		return this.sufficientEnergy(ep, 1);
	}

	public final boolean sufficientEnergy(Player ep, float f) {
		return ep.getAbilities().instabuild || PlayerElementBuffer.instance.playerHas(ep, energy.copy().scale(f));
	}

	public ElementTagCompound getEnergy(float scale) {
		return energy.copy().scale(scale);
	}

	@Override
	public void appendHoverText(ItemStack is, TooltipContext context, TooltipDisplay display, Consumer<Component> li,
			TooltipFlag flag) {
		if (!energy.isEmpty()) {
			if (net.minecraft.client.Minecraft.getInstance().hasShiftDown()) {
				String[] parts = this.generateUsageData(true).split("\\n");
				for (String s : parts) {
					li.accept(Component.literal(s));
				}
			}
			else {
				li.accept(Component.translatable("item.chromaticraft.wand.hold_shift",
						Component.translatable("item.chromaticraft.pendant.shift").withStyle(ChatFormatting.GREEN))
						.withStyle(ChatFormatting.GRAY));
			}
		}
	}

	public String generateUsageData(boolean color) {
		StringBuilder sb = new StringBuilder();
		sb.append("Base usage requires: \n");
		for (CrystalElement e : energy.elementSet()) {
			sb.append((color ? e.getChatColorString() : "")+" "+e.displayName/*+": "+energy.getValue(e)*/+"\n");
		}
		return sb.toString();
	}

	protected static boolean canUseBoostedEffect(Player ep) {
		return ProgressStage.CTM.isPlayerAtStage(ep);
	}

	@Override
	public String getNotes(int subpage) {
		return this.generateUsageData(false);
	}

	@Override
	public int getMaxSubpage() {
		return 1;
	}

	@Override
	public boolean replaceOriginal() {
		return true;
	}

}
