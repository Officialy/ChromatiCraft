package reika.chromaticraft.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LightLayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.magic.potions.PotionVoidGaze.VoidGazeLevels;
import reika.chromaticraft.render.particle.ChromaParticle;

/**
 * The client half of Void Gaze.
 * <ul>
 * <li>V33a {@code ChromaClientEventController.createCaveParticles}: while CAVEPARTICLES is active, up to nine points a
 * tick (fewer at lower particle settings) within 12 blocks across and 8 up or down are tried, and each one in air with
 * no sky light gets a short white additive flare.</li>
 * <li>V33a called a level's {@code onFinish} from the effect's client tick on its last tick; 26.x effect ticks run on
 * the server only, so each level's {@code onStart}/{@code onFinish} is called here as it switches on or off.</li>
 * </ul>
 * <p>CHROMA-PORT: FACEFLIP drew the world with its front faces reversed ({@code glFrontFace(GL_CW)} around terrain,
 * with snow layers skipped). 26.3's render pipelines expose culling but not the winding order; this needs a
 * render-state hook around chunk-layer drawing, verified in a client, before it can return.
 */
@EventBusSubscriber(modid = ChromatiCraft.MODID, value = Dist.CLIENT)
public final class VoidGazeClient {

	private static final boolean[] wasActive = new boolean[VoidGazeLevels.list.length];

	private VoidGazeClient() {}

	@SubscribeEvent
	public static void tick(ClientTickEvent.Post evt) {
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer ep = mc.player;
		if (ep == null)
			return;
		for (VoidGazeLevels lvl : VoidGazeLevels.list) {
			boolean now = lvl.isActiveOnPlayer(ep);
			if (now != wasActive[lvl.ordinal()]) {
				if (now)
					lvl.onStart(ep);
				else
					lvl.onFinish(ep);
				wasActive[lvl.ordinal()] = now;
			}
		}
		if (!mc.isPaused() && VoidGazeLevels.CAVEPARTICLES.isActiveOnPlayer(ep))
			createCaveParticles(mc, ep);
	}

	private static void createCaveParticles(Minecraft mc, LocalPlayer ep) {
		RandomSource rand = ep.getRandom();
		int n = 9-mc.options.particles().get().ordinal()*2;
		for (int i = 0; i < n; i++) {
			double dx = ep.getX()+(rand.nextDouble()*2-1)*12;
			double dz = ep.getZ()+(rand.nextDouble()*2-1)*12;
			double dy = ep.getY()+(rand.nextDouble()*2-1)*8;
			BlockPos p = BlockPos.containing(dx, dy, dz);
			if (ep.level().getBlockState(p).isAir() && ep.level().getBrightness(LightLayer.SKY, p) == 0) {
				ChromaParticle.spawnIconFade(ep.level(), dx, dy, dz, 0, 0, 0, 0xffffff, 8, 1.5F, "flare", true, true);
			}
		}
	}

}
