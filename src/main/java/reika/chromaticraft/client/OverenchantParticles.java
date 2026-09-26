package reika.chromaticraft.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.registry.ChromaEnchants;
import reika.chromaticraft.render.particle.ChromaParticle;
import reika.dragonapi.libraries.java.ReikaRandomHelper;
import reika.dragonapi.libraries.mathsci.ReikaVectorHelper;

/**
 * V33a {@code ChromaClientEventController.overenchantedTools}: while the player holds a tool carrying a ChromatiCraft
 * enchantment above its maximum (which only works at the maximum), small teal hex flares flicker beside the held
 * item in first person.
 */
@EventBusSubscriber(modid = ChromatiCraft.MODID, value = Dist.CLIENT)
public final class OverenchantParticles {

	private OverenchantParticles() {}

	@SubscribeEvent
	public static void overenchantedTools(ClientTickEvent.Post evt) {
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer ep = mc.player;
		if (ep == null || mc.isPaused() || !ChromaEnchants.hasOverenchantment(ep.getMainHandItem()))
			return;
		if (mc.options.getCameraType().isFirstPerson()) {
			Vec3 vec = ep.getLookAngle();
			Vec3 vec2 = ReikaVectorHelper.rotateVector(vec, 0, 53, 0);
			double d = 0.3;
			// 1.7.10's client player posY was eye height.
			double px = ReikaRandomHelper.getRandomPlusMinus(ep.getX()+vec2.x*d, 0.03125);
			double py = ReikaRandomHelper.getRandomPlusMinus(ep.getEyeY(), ep.getBbHeight()/4);
			double pz = ReikaRandomHelper.getRandomPlusMinus(ep.getZ()+vec2.z*d, 0.03125);
			ChromaParticle.spawnIconFade(ep.level(), px, py, pz, 0, 0, 0, 0x00A7A7, 10, 0.25F, "hexflare", true);
		}
	}

}
