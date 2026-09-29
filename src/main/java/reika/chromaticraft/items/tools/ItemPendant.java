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

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import org.jspecify.annotations.Nullable;

import reika.chromaticraft.base.CrystalBlock;
import reika.chromaticraft.magic.CrystalPotionController;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.registry.CrystalElement;

/**
 * V33a {@code ItemPendant} ("Pendant"), one item per element where V33a used metadata. Carried anywhere in the
 * inventory it keeps its colour's crystal effect on the player, at the cave crystal's level; Purple instead
 * multiplies crystal XP and Black lowers lumen buffer costs, through {@link #getActivePendantLevel}. Every tick it
 * stamps the player's {@code pendant_status} data with the time, which is how those consumers see it.
 */
public class ItemPendant extends Item {

	private static final String ROOT_KEY = "pendant_status";

	private final CrystalElement color;

	public ItemPendant(CrystalElement color, Item.Properties properties) {
		super(properties.stacksTo(1));
		this.color = color;
	}

	public CrystalElement getColor() {
		return color;
	}

	@Override
	public void appendHoverText(ItemStack is, TooltipContext context, TooltipDisplay display, Consumer<Component> li,
			TooltipFlag flag) {
		if (net.minecraft.client.Minecraft.getInstance().hasShiftDown()) {
			switch(color) {
				case PURPLE:
					li.accept(Component.translatable("item.chromaticraft.pendant.effect.purple"));
					break;
				case BLACK:
					li.accept(Component.translatable("item.chromaticraft.pendant.effect.black"));
					break;
				default:
					CrystalBlock b = (CrystalBlock)(this.isEnhanced(is) ? ChromaBlocks.superCrystal(color).get()
							: ChromaBlocks.caveCrystal(color).get());
					int lvl = b.getPotionLevel(color);
					li.accept(Component.literal(CrystalPotionController.instance.getEffectName(color, lvl > 0)));
					li.accept(Component.translatable("item.chromaticraft.pendant.level", lvl+1));
					break;
			}
		}
		else {
			li.accept(Component.translatable("item.chromaticraft.pendant.hold_shift",
					Component.translatable("item.chromaticraft.pendant.shift").withStyle(ChatFormatting.GREEN))
					.withStyle(ChatFormatting.GRAY));
		}
	}

	/** V33a {@code onUpdate}. 26.x ticks inventory items on the server only, where the effects and the stamps live. */
	@Override
	public final void inventoryTick(ItemStack is, ServerLevel world, Entity e, @Nullable EquipmentSlot slot) {
		boolean en = this.isEnhanced(is);
		int level = en ? 2 : 0;
		if (e instanceof Player ep) {
			long bits = world.getGameTime() & (~1L);
			bits |= en ? 1 : 0;
			getRootStorage(e).putLong(color.name(), bits);
			if (color == CrystalElement.BLACK || color == CrystalElement.PURPLE) {

			}
			else {
				int dura = en ? 6000 : color == CrystalElement.BLUE ? 3 : 100;
				MobEffectInstance pot = CrystalPotionController.instance.getEffectFromColor(color, dura, level, false);
				if (pot == null || color == CrystalElement.BLUE || !ep.hasEffect(pot.getEffect())) {
					CrystalPotionController.instance.applyEffectFromColor(dura, level, ep, color, true);
				}
			}
			// V33a NOPARTICLES: ReikaEntityHelper.setNoPotionParticles, now PendantEvents.hideEffectParticles.
			this.onTick(is, world, ep, slot);
		}
	}

	protected void onTick(ItemStack is, ServerLevel world, Player ep, @Nullable EquipmentSlot slot) {

	}

	private static CompoundTag getRootStorage(Entity e) {
		CompoundTag data = e.getPersistentData();
		CompoundTag root = data.getCompound(ROOT_KEY).orElse(null);
		if (root == null) {
			root = new CompoundTag();
			data.put(ROOT_KEY, root);
		}
		return root;
	}

	/** Whether the enhanced effect is active */
	protected boolean isEnhanced(ItemStack is) {
		return false;
	}

	/**
	 * 0 for basic, 1 for enhanced, -1 for none. As in V33a the result is really "stamped within the last second":
	 * the enhanced bit it stores in the stamp is never read back, and any pendant stamped an earlier tick reports 1.
	 */
	public static int getActivePendantLevel(Player ep, CrystalElement e) {
		long val = getRootStorage(ep).getLongOr(e.name(), 0);
		long dur = ep.level().getGameTime()-val;
		return dur <= 20 ? (int)(dur > 0 ? 1 : 0) : -1;
	}

	/** Whether any pendant has been carried within V33a's one-second window. */
	public static boolean hasActivePendant(Player ep) {
		for (CrystalElement e : CrystalElement.elements) {
			if (getActivePendantLevel(ep, e) >= 0)
				return true;
		}
		return false;
	}

}
