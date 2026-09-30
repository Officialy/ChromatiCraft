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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.GuiLayer;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.items.tools.ItemPylonFinder;
import reika.chromaticraft.registry.ChromaItems;
import reika.chromaticraft.registry.CrystalElement;
import reika.chromaticraft.render.ChromaRenderPipelines;
import reika.chromaticraft.world.iwg.PylonGenerator;
import reika.chromaticraft.world.iwg.PylonGenerator.PylonEntry;
import reika.dragonapi.instantiable.data.immutable.WorldLocation;
import reika.dragonapi.instantiable.rendering.ColorBlendList;
import reika.dragonapi.instantiable.rendering.GuiShapes;
import reika.dragonapi.libraries.mathsci.ReikaEngLibrary;
import reika.dragonapi.libraries.mathsci.ReikaMathLibrary;
import reika.dragonapi.libraries.rendering.ReikaColorAPI;
import reika.dragonapi.libraries.rendering.ReikaRenderHelper;

/**
 * V33a {@code PylonFinderOverlay}: while the Pylon Compass is held (or, per V33a, sits in an Aura Pouch), the nearest
 * known pylon of every colour is marked on the HUD -- a rune box at its bearing when in view, an arrow at the screen
 * edge when not -- with its distance, turbocharge halo, power crystals, and a red slashed circle if its structure is
 * broken. The same layer shows the pylon-network bottleneck warnings the Bottleneck Finder raises.
 *
 * <p>Drawn on the GUI layer V33a used ({@code ElementType.HELMET}, now the camera overlays) with {@link GuiShapes} for
 * its triangles and line loops. 1.7.10 line widths were in screen pixels; here they are divided by the GUI scale.
 *
 * <p>CHROMA-PORT: V33a wrote the distance and link count in its HUD bitmap font ({@code ChromaFontRenderer} HUD); the
 * vanilla font stands in until those fonts are ported.
 */
public final class PylonFinderOverlay implements GuiLayer {

	public static final PylonFinderOverlay instance = new PylonFinderOverlay();

	private static final Identifier ID = Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "pylon_finder");
	private static final Identifier BOTTLENECK = Identifier.fromNamespaceAndPath(ChromatiCraft.MODID,
			"textures/gui/pylon_bottleneck.png");
	private static final Identifier DIAMOND = Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "block/icons/diamond");
	private static final Identifier ECLIPSEFLARE = Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "block/icons/eclipseflare");

	private static final ColorBlendList powerCrystalDiamondColor = new ColorBlendList(200).addAll(ChromaColorTiles.getChromaColorTiles());

	private final Collection<RenderEntry> renders = new ArrayList();

	private PylonFinderOverlay() {

	}

	public static void register(RegisterGuiLayersEvent event) {
		event.registerAbove(VanillaGuiLayers.CAMERA_OVERLAYS, ID, instance);
	}

	@Override
	public void render(GuiGraphicsExtractor g, DeltaTracker delta) {
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer ep = mc.player;
		if (ep == null)
			return;
		if (PylonGenerator.instance.canGenerateIn(ep.level())) {
			boolean compass = ep.getMainHandItem().is(ChromaItems.PYLON_FINDER.get())
					|| ep.getPersistentData().getLongOr(ItemPylonFinder.NBT_KEY, Long.MIN_VALUE) >= ep.level().getGameTime()-20
					|| reika.chromaticraft.items.tools.ItemAuraPouch.containsActive(ep,
							is -> is.is(ChromaItems.PYLON_FINDER.get()));
			if (compass || !renders.isEmpty()) {
				int h = g.guiHeight()/2;
				float yaw = ep.getYHeadRot()%360;
				float pitch = ep.getXRot()+90;
				if (yaw < 0)
					yaw += 360;
				int fov = ReikaRenderHelper.getRealFOV();
				if (compass) {
					for (int i = 0; i < CrystalElement.elements.length; i++) {
						CrystalElement e = CrystalElement.elements[i];
						PylonEntry c = PylonGenerator.instance.getNearestPylonSpawn(ep.level(), ep.getX(), ep.getEyeY(), ep.getZ(), e);
						if (c != null) {
							RenderPosition pos = new RenderPosition(c.location, ep, yaw, pitch, fov, h);
							new PylonDisplay(c).render(g, ep, pos, fov, h);
						}
					}
				}
				Iterator<RenderEntry> it = renders.iterator();
				while (it.hasNext()) {
					RenderEntry e = it.next();
					RenderPosition pos = new RenderPosition(e.location, ep, yaw, pitch, fov, h);
					e.render(g, ep, pos, fov, h);
					if (e.tick())
						it.remove();
				}
			}
		}
	}

	/**
	 * V33a {@code addBottleneckWarning}, raised from the Bottleneck Finder's network warnings; {@code level} is its
	 * {@code WarningLevels} ordinal (the row of the warning sheet). CHROMA-PORT: nothing calls this until the
	 * Bottleneck Finder and its {@code ChromaOverlays} packet are ported.
	 */
	public void addBottleneckWarning(WorldLocation loc, CrystalElement e, int level, boolean isThroughput) {
		renders.add(new BottleneckWarning(loc, e, level, isThroughput));
	}

	private static TextureAtlasSprite sprite(Identifier id) {
		return Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS).getSprite(id);
	}

	private static float lineWidth(float pixels) {
		return pixels/(float)Minecraft.getInstance().getWindow().getGuiScale();
	}

	private static abstract class RenderEntry {

		protected final WorldLocation location;

		protected final int lifespan;
		protected int age;

		protected RenderEntry(WorldLocation loc, int life) {
			location = loc;
			lifespan = life;
		}

		private boolean tick() {
			age++;
			return age >= lifespan;
		}

		protected abstract void render(GuiGraphicsExtractor g, LocalPlayer ep, RenderPosition pos, int fov, int h);

	}

	private static class PylonDisplay extends RenderEntry {

		private final PylonEntry loc;

		private PylonDisplay(PylonEntry c) {
			super(c.location, Integer.MAX_VALUE);
			loc = c;
		}

		@Override
		protected void render(GuiGraphicsExtractor g, LocalPlayer ep, RenderPosition pos, int fov, int h) {
			CrystalElement e = loc.color;
			TextureAtlasSprite rune = sprite(e.getFaceRuneSprite());
			int opaque = 0xFF000000 | e.getColor();
			int faint = (96 << 24) | (e.getColor() & 0xFFFFFF);
			float cy = (float)pos.cy;
			float thin = lineWidth(1);

			if (pos.phi >= 180 && 360-fov > pos.phi) {
				int cx = 10;
				GuiShapes.triangle(g, cx+10, cy+10, cx+10, cy-10, cx, cy, faint);
				GuiShapes.lineLoop(g, new float[] {cx, cy, cx+10, cy-10, cx+10, cy+10}, new int[] {opaque}, thin);
				GuiShapes.sprite(g, RenderPipelines.GUI_TEXTURED, rune, cx+10, cy-8, cx+26, cy+8, opaque);
			}
			else if (pos.phi < 180 && pos.phi > fov) {
				int cx = g.guiWidth()-10;
				GuiShapes.triangle(g, cx, cy, cx-10, cy-10, cx-10, cy+10, faint);
				GuiShapes.lineLoop(g, new float[] {cx, cy, cx-10, cy-10, cx-10, cy+10}, new int[] {opaque}, thin);
				GuiShapes.sprite(g, RenderPipelines.GUI_TEXTURED, rune, cx-26, cy-8, cx-10, cy+8, opaque);
			}
			else { //on screen, in FOV
				double w = g.guiWidth()/2D;
				float cx = (float)(w+1*w*Math.sin(Math.toRadians(pos.phi)));
				int boxFill = (32 << 24) | (e.getColor() & 0xFFFFFF);
				GuiShapes.quad(g, cx-8, cy+8, cx+8, cy+8, cx+8, cy-8, cx-8, cy-8, boxFill, boxFill, boxFill, boxFill);
				GuiShapes.lineLoop(g, new float[] {cx-8, cy+8, cx+8, cy+8, cx+8, cy-8, cx-8, cy-8}, new int[] {opaque}, thin);

				boolean link = loc.playerLink != null && loc.playerLink.equals(ep.getUUID());

				Font fr = Minecraft.getInstance().font;
				if (link) {
					double t = (System.currentTimeMillis()/250D)%360;
					double f1 = 0.5+0.5*Math.sin(t);
					double f2 = 0.5+0.5*Math.sin(t+90);
					double f3 = 0.5+0.5*Math.sin(t+180);
					double f4 = 0.5+0.5*Math.sin(t+270);
					int c1 = 0xFF000000 | ReikaColorAPI.mixColors(e.getColor(), 0, (float)f1);
					int c2 = 0xFF000000 | ReikaColorAPI.mixColors(e.getColor(), 0, (float)f2);
					int c3 = 0xFF000000 | ReikaColorAPI.mixColors(e.getColor(), 0, (float)f3);
					int c4 = 0xFF000000 | ReikaColorAPI.mixColors(e.getColor(), 0, (float)f4);
					GuiShapes.lineLoop(g, new float[] {cx-12, cy+12, cx+12, cy+12, cx+12, cy-12, cx-12, cy-12},
							new int[] {c1, c2, c3, c4}, thin);
					// CHROMA-PORT: PylonLinkNetwork has no client copy yet, so off a single-player server this reads 0.
					int count = Math.max(0, loc.getLinkedPylons(ep.level()));
					String s = String.valueOf(count);
					int sw = fr.width(s);
					g.text(fr, s, (int)cx+1-sw/2, (int)cy+16, 0xffffffff, false);
				}

				GuiShapes.sprite(g, RenderPipelines.GUI_TEXTURED, rune, cx-8, cy-8, cx+8, cy+8, opaque);

				if (!loc.hasStructure) {
					int red = (72 << 24) | 0xff0000;
					float lw = lineWidth(3.5F*(float)Minecraft.getInstance().getWindow().getGuiScale());
					double r = 15;
					ArrayList<Float> circle = new ArrayList();
					for (double d = 0; d < 360; d += 5) {
						double a = Math.toRadians(d);
						circle.add((float)(cx+r*Math.cos(a)));
						circle.add((float)(cy+r*Math.sin(a)));
					}
					float[] pts = new float[circle.size()];
					for (int i = 0; i < pts.length; i++)
						pts[i] = circle.get(i);
					GuiShapes.lineLoop(g, pts, new int[] {red}, lw);
					float r2 = (float)(r*0.707);
					GuiShapes.line(g, cx-r2, cy-r2, cx+r2, cy+r2, lw, red, red);
				}

				if (loc.isTurboCharged) {
					GuiShapes.sprite(g, ChromaRenderPipelines.GUI_ADDITIVE_DARK, sprite(ECLIPSEFLARE), cx-36, cy-36, cx+36, cy+36,
							opaque);
				}

				TextureAtlasSprite diamond = sprite(DIAMOND);
				for (var c2 : loc.getCrystals()) {
					int px = (c2.getX()-location.pos.getX())*4;
					int pz = (c2.getZ()-location.pos.getZ())*4;
					int color = 0xFF000000 | powerCrystalDiamondColor.getColor(System.currentTimeMillis()+c2.hashCode()/2);
					GuiShapes.sprite(g, RenderPipelines.GUI_TEXTURED, diamond, cx+px-4, cy+pz-4, cx+px+4, cy+pz+4, color);
				}

				int base = ReikaMathLibrary.intpow2(10, (int)ReikaMathLibrary.logbase(pos.dl, 10));
				int dist = ReikaMathLibrary.roundToNearestX(base, (int)Math.round(pos.dl));
				String unit = ReikaEngLibrary.getSIPrefix(dist);
				String s = String.format("%.0f%sm", ReikaMathLibrary.getThousandBase(dist), unit);
				int d = link ? 4 : 0;
				g.text(fr, s, (int)cx+11+d, (int)cy+11-d, 0xFF000000 | ReikaColorAPI.mixColors(0, e.getColor(), 0.67F), false);
				g.text(fr, s, (int)cx+10+d, (int)cy+10-d, 0xFF000000 | ReikaColorAPI.mixColors(0xffffff, e.getColor(), 0.8F), false);
			}
		}

	}

	private static class BottleneckWarning extends RenderEntry {

		private final boolean isThroughput;
		private final int level;
		private final CrystalElement color;

		private BottleneckWarning(WorldLocation loc, CrystalElement e, int w, boolean thru) {
			super(loc, 6000);
			level = w;
			isThroughput = thru;
			color = e;
		}

		@Override
		protected void render(GuiGraphicsExtractor g, LocalPlayer ep, RenderPosition pos, int fov, int h) {
			if (pos.phi >= 180 && 360-fov > pos.phi) {

			}
			else if (pos.phi < 180 && pos.phi > fov) {

			}
			else { //on screen, in FOV
				double w = g.guiWidth()/2D;
				double cx = w+1*w*Math.sin(Math.toRadians(pos.phi));
				double cy = pos.cy;
				double s = 8;

				float u = isThroughput ? 0 : 0.5F;
				float du = u+0.5F;
				float v = level/4F;
				float dv = v+0.25F;

				if (isThroughput) {
					cx -= 12;
					cy -= 12;
				}
				else {
					cx += 12;
					cy += 12;
				}

				int a = 192;
				if (age*2 >= lifespan) {
					a *= 2-age/lifespan;
				}
				// 1.7.10's Tessellator clamped the alpha channel.
				a = Math.min(a, 255);
				int c = (a << 24) | ReikaColorAPI.mixColors(color.getColor(), 0xffffff, 0.675F);
				GuiShapes.texture(g, RenderPipelines.GUI_TEXTURED, BOTTLENECK, (float)(cx-s), (float)(cy-s), (float)(cx+s),
						(float)(cy+s), u, v, du, dv, c);
			}

		}
	}

	private static class RenderPosition {

		private double phi;
		private double dl;
		private double cy;

		/** 1.7.10's client player stood at eye height, so the bearing is taken from the eyes. */
		private RenderPosition(WorldLocation loc, LocalPlayer ep, float yaw, float pitch, int fov, int h) {
			double dx = loc.pos.getX()+0.5-ep.getX();
			double dy = loc.pos.getY()+0.5-ep.getEyeY();
			double dz = loc.pos.getZ()+0.5-ep.getZ();

			dl = ReikaMathLibrary.py3d(dx, 0, dz);
			double arel = -Math.toDegrees(Math.atan2(dx, dz));
			double prel = 90-Math.toDegrees(Math.atan2(dy, dl));
			if (arel < 0)
				arel += 360;
			phi = arel-yaw;
			double theta = prel-pitch;
			if (phi < 0)
				phi += 360;
			cy = h+h*2*Math.sin(Math.toRadians(theta));
		}

	}
}
