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

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;
import reika.chromaticraft.base.ItemPoweredChromaTool;
import reika.chromaticraft.magic.ToolChargingSystem;
import reika.chromaticraft.magic.interfaces.ChargingPoint;
import reika.chromaticraft.magic.interfaces.PoweredItem;
import reika.chromaticraft.registry.ChromaOptions;
import reika.chromaticraft.registry.CrystalElement;

import java.util.Locale;
import java.util.function.Consumer;

/**
 * V33a {@code ItemBoostedPendant} ("Enhanced Pendant"): a Pendant at amplifier 2 with a five-minute effect. With
 * {@code POWEREDPENDANTS} (off by default) it holds 60 hours of charge, runs down one per tick, is enhanced only
 * while charged, and charges on the ground like the powered tools; otherwise it is always enhanced and needs none.
 */
public class ItemBoostedPendant extends ItemPendant implements PoweredItem {

	private static final int MAX_CHARGE = 20*3600*60; //60h

	public ItemBoostedPendant(CrystalElement color, Properties properties) {
		super(color, properties);
	}

	@Override
	protected boolean isEnhanced(ItemStack is) {
		return !ChromaOptions.POWEREDPENDANTS.getState() || ToolChargingSystem.instance.getCharge(is) > 0;
	}

	@Override
	protected void onTick(ItemStack is, ServerLevel world, Player ep, @Nullable EquipmentSlot slot) {
		if (ChromaOptions.POWEREDPENDANTS.getState()) {
			//ToolChargingSystem.instance.tryChargeFromPlayer(is, ep);
			ToolChargingSystem.instance.removeCharge(is, this.getChargeConsumptionRate(ep, world, is), ep);
		}
	}

	@Override
	public void appendHoverText(ItemStack is, TooltipContext context, TooltipDisplay display, Consumer<Component> li,
			TooltipFlag flag) {
		super.appendHoverText(is, context, display, li, flag);
		if (ChromaOptions.POWEREDPENDANTS.getState())
			li.accept(Component.literal(String.format(Locale.ROOT, "Energy: %.2f%s",
					100F*ToolChargingSystem.instance.getCharge(is)/this.getMaxCharge(), "%")));
	}

	/** V33a {@code addNBTForCreative}: the creative-tab stack comes fully charged when charging is required. */
	public ItemStack creativeStack() {
		return ChromaOptions.POWEREDPENDANTS.getState()
				? ToolChargingSystem.instance.getChargedItem(this, MAX_CHARGE) : new ItemStack(this);
	}

	@Override
	public final boolean onEntityItemUpdate(ItemStack is, ItemEntity ei) {
		return ChromaOptions.POWEREDPENDANTS.getState() ? ToolChargingSystem.instance.tickItem(ei)
				: super.onEntityItemUpdate(is, ei);
	}

	@Override
	public final int getEntityLifespan(ItemStack is, Level world) {
		return ChromaOptions.POWEREDPENDANTS.getState() ? Integer.MAX_VALUE : super.getEntityLifespan(is, world);
	}

	@Override
	public final boolean hasCustomEntity(ItemStack stack) {
		return ChromaOptions.POWEREDPENDANTS.getState();
	}

	@Override
	public final @Nullable Entity createEntity(Level world, Entity location, ItemStack itemstack) {
		return ChromaOptions.POWEREDPENDANTS.getState()
				? new ToolChargingSystem.EntityChargingTool(world, (ItemEntity)location, itemstack)
				: super.createEntity(world, location, itemstack);
	}

	@Override
	public int getMaxCharge() {
		return MAX_CHARGE;
	}

	@Override
	public boolean hasChargeStates() {
		return false;
	}

	@Override
	public int getChargeStates() {
		return 0;
	}

	@Override
	public int getChargeState(float frac) {
		return 0;
	}

	@Override
	public int getChargeConsumptionRate(Player e, Level world, ItemStack is) {
		return 1;
	}

	@Override
	public int getChargeRate(ItemStack is, int base) {
		return base*3/2;
	}

	@Override
	public void doChargeFX(ItemEntity ei, int charge) {
		ItemPoweredChromaTool.doStandardChargeFX(ei, charge);
	}

	@Override
	public CrystalElement getColor(ItemStack is) {
		return this.getColor();
	}

	@Override
	public float getPlayerChargeCoefficient(Player ep, ChargingPoint te, ItemStack is) {
		return 0.5F;
	}
	/*
	@Override
	public float getPlayerBufferExtractionValue(ItemStack is) {
		return 40;
	}*/

	@Override
	public String getNotes(int subpage) {
		return ChromaOptions.POWEREDPENDANTS.getState() ? "Enhanced pendants require charging with the corresponding crystal element" : "";
	}

	@Override
	public int getMaxSubpage() {
		return 1;
	}

	public boolean replaceOriginal() {
		return false;
	}

}
