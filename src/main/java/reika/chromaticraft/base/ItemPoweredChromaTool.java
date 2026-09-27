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

import java.util.Locale;
import java.util.function.Consumer;

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

import reika.chromaticraft.magic.ToolChargingSystem;
import reika.chromaticraft.magic.ToolChargingSystem.EntityChargingTool;
import reika.chromaticraft.magic.interfaces.ChargingPoint;
import reika.chromaticraft.magic.interfaces.PoweredItem;
import reika.chromaticraft.registry.CrystalElement;
import reika.chromaticraft.render.particle.ChromaParticle;

/**
 * V33a {@code ItemPoweredChromaTool}: a tool that runs on its charge of one element. It drops as an
 * {@link EntityChargingTool} that never despawns and fills near a crystal source; while activated it spends charge each
 * tick its {@link #doTick} reports work (never for creative players). The creative tab lists it empty and full.
 */
public abstract class ItemPoweredChromaTool extends ItemChromaTool implements PoweredItem {

	public ItemPoweredChromaTool(Properties properties) {
		super(properties);
	}

	public abstract CrystalElement getColor(ItemStack is);
	public abstract int getMaxCharge();
	public abstract int getChargeStates();

	protected abstract boolean isActivated(Player e, ItemStack is, boolean held);
	public abstract int getChargeConsumptionRate(Player e, Level world, ItemStack is);
	protected abstract boolean doTick(ItemStack is, Level world, Player e, boolean held);

	@Override
	public float getPlayerChargeCoefficient(Player ep, ChargingPoint te, ItemStack is) {
		return 0.05F;
	}

	@Override
	public int getChargeRate(ItemStack is, int base) {
		return base;
	}

	@Override
	public int getChargeState(float frac) {
		return Math.round((this.getChargeStates()-1)*frac);
	}

	@Override
	public void appendHoverText(ItemStack is, TooltipContext context, TooltipDisplay display, Consumer<Component> li,
			TooltipFlag flag) {
		li.accept(Component.literal(String.format(Locale.ROOT, "Energy: %.2f%s",
				100F*ToolChargingSystem.instance.getCharge(is)/this.getMaxCharge(), "%")));
	}

	/** V33a {@code onUpdate}; in 26.2 the inventory tick runs on the server only, where the charge lives. */
	@Override
	public final void inventoryTick(ItemStack is, ServerLevel world, Entity e, EquipmentSlot slot) {
		if (e instanceof Player ep) {
			boolean held = slot == EquipmentSlot.MAINHAND;
			if (this.isActivated(ep, is, held)) {
				if (ToolChargingSystem.instance.getCharge(is) > 0) {
					if (this.doTick(is, world, ep, held) && !ep.isCreative()) {
						ToolChargingSystem.instance.removeCharge(is, this.getChargeConsumptionRate(ep, world, is), ep);
					}
				}
			}
		}
	}

	@Override
	public final int getEntityLifespan(ItemStack is, Level world) {
		return Integer.MAX_VALUE;
	}

	@Override
	public final boolean hasCustomEntity(ItemStack stack) {
		return true;
	}

	@Override
	public final Entity createEntity(Level world, Entity location, ItemStack itemstack) {
		return new EntityChargingTool(world, (ItemEntity)location, itemstack);
	}

	@Override
	public final boolean onEntityItemUpdate(ItemStack is, ItemEntity ei) {
		return ToolChargingSystem.instance.tickItem(ei);
	}

	@Override
	public void doChargeFX(ItemEntity ei, int charge) {
		doStandardChargeFX(ei, charge);
	}

	/**
	 * V33a: a fully charged tool throws a turning six-armed wheel of its colour; a partly charged one spits one
	 * colliding blur in a random direction each tick.
	 */
	public static void doStandardChargeFX(ItemEntity ei, int charge) {
		ItemStack is = ei.getItem();
		PoweredItem pi = (PoweredItem)is.getItem();
		ChromaParticle.spawnPoweredItemCharge(ei.level(), ei.getX(), ei.getY(), ei.getZ(), ei.getAge(),
				pi.getColor(is).getColor(), charge == pi.getMaxCharge());
	}

	public boolean hasChargeStates() {
		return true;
	}

	@Override
	public String getNotes(int subpage) {
		return "Requires charging with "+this.getColor(new ItemStack(this)).displayName+" ambient energy to be used";
	}

	@Override
	public int getMaxSubpage() {
		return 1;
	}

	@Override
	public boolean replaceOriginal() {
		return false;
	}

}
