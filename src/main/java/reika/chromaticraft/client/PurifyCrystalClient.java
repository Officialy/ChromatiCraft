/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.items.tools.powered.ItemPurifyCrystal;
import reika.chromaticraft.magic.ToolChargingSystem;
import reika.chromaticraft.render.particle.ChromaParticle;
import reika.dragonapi.instantiable.Orbit;
import reika.dragonapi.instantiable.particlecontroller.OrbitMotionController;
import reika.dragonapi.interfaces.PositionController;

/**
 * V33a {@code ItemPurifyCrystal.doHeldFX}, which ran in the client half of the item's inventory tick: each charged
 * crystal the player carries sends, one tick in eight, a mote round an orbit of radius 2.5 about them, the orbit's
 * inclination turning with the player's age.
 */
@EventBusSubscriber(modid = ChromatiCraft.MODID, value = Dist.CLIENT)
public final class PurifyCrystalClient {

	private PurifyCrystalClient() {}

	@SubscribeEvent
	public static void heldFX(ClientTickEvent.Post evt) {
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer ep = mc.player;
		if (ep == null || mc.isPaused())
			return;
		for (ItemStack is : ep.getInventory()) {
			if (is.getItem() instanceof ItemPurifyCrystal && ToolChargingSystem.instance.getCharge(is) > 0)
				doHeldFX(ep, ep.getRandom());
		}
	}

	private static void doHeldFX(Entity e, RandomSource itemRand) {
		if (itemRand.nextInt(2*4) == 0) {
			double r = 2.5;
			double ec = 0;//0.4+0.4*Math.sin(Math.toRadians(e.ticksExisted));
			double i = e.tickCount*4;//.ticksExisted%2 == 0 ? 60 : -60;
			double raan = 0;//(e.ticksExisted%2 == 0 ? 180 : 0)+RenderManager.instance.playerViewY;
			Orbit o = new Orbit(r, ec, i, raan, 0, 0);
			OrbitMotionController p = new OrbitMotionController(o, e.getX(), e.getY(), e.getZ()).trackEntity(e);
			p.thetaSpeed = 1.5+itemRand.nextDouble()*3.5;
			int l = 60+itemRand.nextInt(120);
			float s = (float)(1+(itemRand.nextDouble()*2-1)*0.25);
			// 1.7.10's client player posY was eye height, so the orbit centred on the eyes.
			double eye = e.getEyeY()-e.getY();
			PositionController atEyes = new PositionController() {
				@Override public void update(Entity ignored) { p.update(ignored); }
				@Override public double getPositionX(Entity ignored) { return p.getPositionX(ignored); }
				@Override public double getPositionY(Entity ignored) { return p.getPositionY(ignored)+eye; }
				@Override public double getPositionZ(Entity ignored) { return p.getPositionZ(ignored); }
			};
			ChromaParticle.spawnControlledGlow(e.level(), atEyes.getPositionX(null), atEyes.getPositionY(null),
					atEyes.getPositionZ(null), 0xffffff, l, s, atEyes, "centerblur3");
		}
	}

}
