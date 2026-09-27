/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.auxiliary.render;

import java.util.EnumMap;
import java.util.Iterator;
import java.util.Map.Entry;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.GuiLayer;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.registry.ChromaIcons;
import reika.chromaticraft.registry.ChromaSounds;
import reika.chromaticraft.registry.CrystalElement;

/**
 * V33a {@code ProbeInfoOverlayRenderer}: an Examination Crystals connectivity check shows, beside the crosshair, each
 * element's outline rune with a check (reaches a source), a cross (conducts it but no path) or a no-entry sign (does not
 * conduct it) -- the left column for the first eight elements, the right for the rest, staggered in and faded, for 480
 * frames. A result found on the server is sent to the probing player.
 */
public class ProbeInfoOverlayRenderer implements GuiLayer {

	public static final ProbeInfoOverlayRenderer instance = new ProbeInfoOverlayRenderer();

	private static final Identifier ID = Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "probe_connectivity");

	private static final int CONNECTIVITY_DURATION = 480;

	private final EnumMap<CrystalElement, PylonConnection> connectivityFlags = new EnumMap<>(CrystalElement.class);

	private ProbeInfoOverlayRenderer() {

	}

	/** V33a drew it from ChromaOverlays' HUD pass; the camera-overlays layer is the same place in the 26.2 HUD. */
	public static void register(RegisterGuiLayersEvent event) {
		event.registerAbove(VanillaGuiLayers.CAMERA_OVERLAYS, ID, instance);
	}

	@Override
	public void render(GuiGraphicsExtractor g, DeltaTracker delta) {
		if (!connectivityFlags.isEmpty()) {
			Iterator<Entry<CrystalElement, PylonConnection>> it = connectivityFlags.entrySet().iterator();
			boolean nonEmpty = false;

			while (it.hasNext()) {
				Entry<CrystalElement, PylonConnection> e = it.next();
				PylonConnection tick = e.getValue();
				if (tick.age == CONNECTIVITY_DURATION-5)
					Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(ChromaSounds.BOUNCE.getSoundEvent(), 2F, 0.25F));
				if (tick.shouldRender()) {
					this.renderConnectivityStatus(g, tick);
				}
				if (tick.age > 0)
					tick.age--;

				if (tick.age <= 0) {
					it.remove();
				}
				else {
					nonEmpty = true;
				}
			}

			if (!nonEmpty) {
				connectivityFlags.clear();
			}
		}
	}

	private void renderConnectivityStatus(GuiGraphicsExtractor g, PylonConnection pc) {
		int ar = 12;
		var atlas = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS);
		TextureAtlasSprite ico = atlas.getSprite((pc.valid ? (pc.connected ? ChromaIcons.CHECK : ChromaIcons.X) : ChromaIcons.NOENTER).getIcon());
		TextureAtlasSprite ico2 = atlas.getSprite(pc.color.getOutlineRuneSprite());

		int color = pc.getAlpha() << 24 | 0xffffff;

		int dx = pc.color.ordinal() >= 8 ? 20 : -40;
		int dy = (pc.color.ordinal()%8)*15-65;

		int ox = dx+g.guiWidth()/2+ar-8;
		int oy = dy+g.guiHeight()/2+ar-8;

		int r = 12;
		int dw = 7;
		g.blitSprite(RenderPipelines.GUI_TEXTURED, ico, ox+dw, oy, r, r, color);
		g.blitSprite(RenderPipelines.GUI_TEXTURED, ico2, ox-dw, oy, r, r, color);
	}

	/**
	 * Client only: records a result. V33a's method also sent it from the server; that half is
	 * {@code ChromaNetwork.sendProbeConnectivity}, since this class (a GUI layer) must never load on a dedicated server.
	 */
	public void markConnectivity(CrystalElement e, boolean connected, boolean valid) {
		connectivityFlags.put(e, new PylonConnection(e, connected, valid));
	}

	private static class PylonConnection {

		public final CrystalElement color;
		public final boolean connected;
		public final boolean valid;

		private int age;

		private PylonConnection(CrystalElement e, boolean conn, boolean v) {
			color = e;
			age = CONNECTIVITY_DURATION+(e.ordinal()%8)*6;
			connected = conn;
			valid = v;
		}

		public boolean shouldRender() {
			return age >= -CONNECTIVITY_DURATION && age <= CONNECTIVITY_DURATION;
		}

		public int getAlpha() {
			int ret = 255;
			int t = Math.abs(age);
			if (t < 20) {
				ret = Math.min(255, 13*t);
			}
			else if (t > CONNECTIVITY_DURATION-20) {
				t = t-(CONNECTIVITY_DURATION-20);
				ret = Math.min(255, 255-12*t);
			}
			return Math.max(0, ret);
		}

	}

}
