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

import java.util.Random;
import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import reika.chromaticraft.magic.artefact.UABombingEffects;
import reika.chromaticraft.render.particle.ChromaParticle;
import reika.dragonapi.instantiable.Interpolation;

/**
 * V33a {@code ItemUnknownArtefact} (metadata ARTIFACT): the whole relic, obtainable only by silk-touching
 * an artefact. Using it on the ground buries it again as the block, which is why it is that block's item.
 *
 * <p>It is hostile to carry. Each tick in an inventory it has a one-in-a-thousand chance to hurt its holder
 * (half the time a point of void damage, otherwise five points of armour wear and a sliver of damage) and a
 * one-in-two-hundred chance of UA bombing ({@link UABombingEffects}). Dropped it never despawns and seeps
 * the dark green cloud and white glints of the artefact field. Its tooltip is an ever-scrolling line of
 * obfuscated glyphs.
 *
 * <p>V33a also had {@code onAddedToInventory}, which bombed a container once it held enough artefacts
 * ({@link #chanceTable}); nothing in V33a called it, and it is kept here as the same public entry point.
 */
public class ItemUnknownArtefact extends BlockItem {

	private static final int RENDER_TEXT_MIN_WORD_LENGTH = 2;
	private static final int RENDER_TEXT_MAX_WORD_LENGTH = 8;
	private static final int RENDER_TEXT_LENGTH = 24;
	private static final int STEP_SPEED = 100 * 2;

	private static final Random rand = new Random();

	private static String renderText;
	private static String renderTextNext;
	private static int panLocation;
	private static long stepTime;

	private static final Interpolation chanceTable = new Interpolation(false);

	static {
		chanceTable.addPoint(2, 0);
		chanceTable.addPoint(3, 0.01);
		chanceTable.addPoint(4, 0.25);
		chanceTable.addPoint(12, 0.5);
		chanceTable.addPoint(16, 0.75);
		chanceTable.addPoint(24, 0.975);
		chanceTable.addPoint(32, 0.995);
		chanceTable.addPoint(64, 1.0);
	}

	public ItemUnknownArtefact(Block block, Properties properties) {
		super(block, properties.stacksTo(1));
	}

	@Override
	public int getEntityLifespan(ItemStack is, Level world) {
		return Integer.MAX_VALUE;
	}

	@Override
	public boolean onEntityItemUpdate(ItemStack is, ItemEntity ei) {
		if (ei.level().isClientSide())
			ChromaParticle.spawnUnknownArtefactField(ei.level(), ei.getX(), ei.getY(), ei.getZ(), false);
		return false;
	}

	@Override
	public void inventoryTick(ItemStack is, ServerLevel world, Entity e, EquipmentSlot slot) {
		if (rand.nextInt(1000) == 0) {
			if (rand.nextBoolean()) {
				e.hurtServer(world, world.damageSources().fellOutOfWorld(), 1);
			}
			else if (e instanceof LivingEntity living) {
				for (EquipmentSlot armor : EquipmentSlot.values()) {
					if (armor.getType() == EquipmentSlot.Type.HUMANOID_ARMOR) {
						ItemStack worn = living.getItemBySlot(armor);
						if (!worn.isEmpty() && worn.isDamageableItem())
							worn.hurtAndBreak(5, living, armor);
					}
				}
				e.hurtServer(world, world.damageSources().fellOutOfWorld(), 0.01F);
			}
		}
		if (rand.nextInt(200) == 0)
			UABombingEffects.instance.trigger(e);
	}

	/** V33a {@code onAddedToInventory}: enough artefacts in one container may set it off. */
	public static void onAddedToInventory(net.minecraft.world.Container inv, net.minecraft.world.level.block.entity.BlockEntity te) {
		int amt = 0;
		for (int i = 0; i < inv.getContainerSize(); i++) {
			if (inv.getItem(i).getItem() instanceof ItemUnknownArtefact)
				amt += inv.getItem(i).getCount();
		}
		double ch = chanceTable.getValue(amt);
		if (ch > 0 && rand.nextDouble() < ch)
			UABombingEffects.instance.trigger(te, inv);
	}

	@Override
	public void appendHoverText(ItemStack is, TooltipContext context, TooltipDisplay display,
			Consumer<Component> li, TooltipFlag flag) {
		if (System.currentTimeMillis() - stepTime >= STEP_SPEED) {
			cycleRenderText();
			stepTime = System.currentTimeMillis();
		}
		li.accept(Component.literal(getRenderText()).withStyle(ChatFormatting.OBFUSCATED));
	}

	private static String getRenderText() {
		return renderTextNext.substring(0, panLocation) + renderText.substring(panLocation);
	}

	private static void cycleRenderText() {
		if (renderText == null)
			renderText = generateNewRenderText();
		if (renderTextNext == null)
			renderTextNext = generateNewRenderText();
		panLocation++;
		if (panLocation >= RENDER_TEXT_LENGTH) {
			panLocation = 0;
			renderText = renderTextNext;
			renderTextNext = generateNewRenderText();
		}
	}

	/** V33a's word generator; note its inner loop never shortens {@code len}, so words run to the end. */
	private static String generateNewRenderText() {
		StringBuilder sb = new StringBuilder();
		while (sb.length() < RENDER_TEXT_LENGTH) {
			int len = Math.min(RENDER_TEXT_LENGTH - sb.length(),
					RENDER_TEXT_MIN_WORD_LENGTH + rand.nextInt(RENDER_TEXT_MAX_WORD_LENGTH - RENDER_TEXT_MIN_WORD_LENGTH + 1));
			while (len > 0 && sb.length() < RENDER_TEXT_LENGTH) {
				char c = (char)('/' + rand.nextInt('~' - '/' + 1));
				sb.append(c);
			}
			sb.append(" ");
		}
		sb.deleteCharAt(sb.length() - 1);
		return sb.toString();
	}
}
