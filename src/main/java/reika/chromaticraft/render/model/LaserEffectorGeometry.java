package reika.chromaticraft.render.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import reika.chromaticraft.world.dimension.structure.LaserPuzzleLayout.EffectType;
import reika.dragonapi.libraries.ReikaDirectionHelper.CubeDirections;
import reika.dragonapi.libraries.rendering.ReikaColorAPI;

/**
 * V33a {@code LaserEffectorRenderer}, transcribed vertex for vertex into block-space quads.
 *
 * <p>The ISBRH built every device out of raw {@code TessellatorVertexList} quads around the block centre
 * (it translated by {@code (x+0.5, y, z+0.5)} first), coloured them with the beam colour unless a vertex
 * carried its own, then turned the whole central item about Y by {@code -facing.angle} plus a per-type
 * correction. Every coordinate, UV corner, colour and winding below is the source's. Sprites are named
 * rather than sampled from an atlas, so {@link LaserEffectorModel} can bind them to the real textures:
 * the slab base was vanilla's stone slab, the corner posts obsidian, and the device bodies the white cave
 * crystal ({@code crystal/crystal_outline}), oak planks and, for the rotation plinth, chiseled stone brick.
 *
 * <p>This class has no client dependency, so its geometry is checked by GameTest on the server.
 */
public final class LaserEffectorGeometry {

	public static final double SLAB = 0.33;

	/** Sprites the V33a renderer bound; {@link LaserEffectorModel} maps them to texture ids. */
	public enum Sprite {
		SLAB_TOP, SLAB_SIDE, OBSIDIAN, CRYSTAL, PLANKS, CHISELED, BASE,
		EMITTER_BACK, EMITTER_FRONT, TARGET_BACK, TARGET_FRONT
	}

	/** One vertex in block space; {@code u}/{@code v} are 0..1 within the sprite, colour is ARGB. */
	public record Vertex(double x, double y, double z, float u, float v, int argb) {}

	/**
	 * One quad. {@code shaded} is false for every face: V33a baked its own face brightness into the
	 * vertex colour, and drew raw tessellator quads unshaded. {@code overlay} marks the full-block
	 * render-pass-1 front overlay, which was alpha blended.
	 */
	public record Quad(Sprite sprite, Vertex a, Vertex b, Vertex c, Vertex d, boolean overlay,
			boolean boundary) {
		public List<Vertex> vertices() { return List.of(a, b, c, d); }
	}

	private LaserEffectorGeometry() {}

	/**
	 * @param color the effector's RGB bits (1 red, 2 green, 4 blue), as stored in its block state
	 * @param rotateable V33a {@code LaserEffectTile.isRotateable()}, which adds the chiseled plinth
	 */
	public static List<Quad> build(EffectType type, CubeDirections facing, int color,
			boolean fullBlock, boolean rotateable) {
		int rgb = renderColor(color);
		List<Quad> quads = new ArrayList<>();
		if (fullBlock)
			fullBlock(quads, type, facing, rgb);
		else
			halfBlock(quads, type, facing, rgb, rotateable);
		return Collections.unmodifiableList(quads);
	}

	/** V33a {@code RGBColorData.getRenderColor}. */
	public static int renderColor(int color) {
		if ((color & 7) == 0) return 0x101010;
		return ((color & 1) != 0 ? 0xFF0000 : 0) | ((color & 2) != 0 ? 0x00FF00 : 0)
				| ((color & 4) != 0 ? 0x0000FF : 0);
	}

	// ---------------------------------------------------------------------------------- half block

	private static void halfBlock(List<Quad> quads, EffectType type, CubeDirections facing, int rgb,
			boolean rotateable) {
		double h = SLAB + (type == EffectType.TARGET || type == EffectType.TARGET_THRU ? 0.55 : 0.33);
		double w = 0.0625;
		double out = 0.1875;

		slab(quads);

		// Obsidian corner posts, coloured by the tessellator's opaque white.
		for (int i = -1; i <= 1; i += 2) {
			for (int k = -1; k <= 1; k += 2) {
				boolean invert = Math.signum(i) != Math.signum(k);
				VertexList vt = new VertexList();
				Sprite o = Sprite.OBSIDIAN;
				vt.add(out * i, h, (out + w) * k, o, 0, 1);
				vt.add((out + w) * i, h, (out + w) * k, o, 1, 1);
				vt.add((out + w) * i, h, out * k, o, 1, 0);
				vt.add(out * i, h, out * k, o, 0, 0);
				vt.add((out + w) * i, h, out * k, o, 1, 1);
				vt.add((out + w) * i, SLAB, out * k, o, 1, 0);
				vt.add(out * i, SLAB, out * k, o, 0, 0);
				vt.add(out * i, h, out * k, o, 0, 1);
				vt.add(out * i, h, (out + w) * k, o, 0, 1);
				vt.add(out * i, SLAB, (out + w) * k, o, 0, 0);
				vt.add((out + w) * i, SLAB, (out + w) * k, o, 1, 0);
				vt.add((out + w) * i, h, (out + w) * k, o, 1, 1);
				vt.add(out * i, h, out * k, o, 0, 1);
				vt.add(out * i, SLAB, out * k, o, 0, 0);
				vt.add(out * i, SLAB, (out + w) * k, o, 1, 0);
				vt.add(out * i, h, (out + w) * k, o, 1, 1);
				vt.add((out + w) * i, h, (out + w) * k, o, 1, 1);
				vt.add((out + w) * i, SLAB, (out + w) * k, o, 1, 0);
				vt.add((out + w) * i, SLAB, out * k, o, 0, 0);
				vt.add((out + w) * i, h, out * k, o, 0, 1);
				if (invert)
					vt.reverse();
				vt.emit(quads, 0xFFFFFFFF);
			}
		}

		centralItem(quads, type, facing, rgb, rotateable, SLAB, h, out);
	}

	/** The stone-slab base, with V33a's explicit per-face brightness (0.4/1/0.7/0.55). */
	private static void slab(List<Quad> quads) {
		double y = SLAB;
		float vTop = (float)(1 - y);
		int down = grey(0.4F), up = grey(1F), ns = grey(0.7F), ew = grey(0.55F);
		Sprite top = Sprite.SLAB_TOP, side = Sprite.SLAB_SIDE;
		// RenderBlocks.renderFaceYNeg / YPos / ZNeg / ZPos / XNeg / XPos, with renderMaxY = slab:
		// side faces sample the lowest third of the slab-side icon.
		quads.add(quad(top, true, v(0, 0, 1, 0, 1, down), v(0, 0, 0, 0, 0, down),
				v(1, 0, 0, 1, 0, down), v(1, 0, 1, 1, 1, down)));
		quads.add(quad(top, false, v(1, y, 1, 1, 1, up), v(1, y, 0, 1, 0, up),
				v(0, y, 0, 0, 0, up), v(0, y, 1, 0, 1, up)));
		quads.add(quad(side, true, v(0, y, 0, 1, vTop, ns), v(1, y, 0, 0, vTop, ns),
				v(1, 0, 0, 0, 1, ns), v(0, 0, 0, 1, 1, ns)));
		quads.add(quad(side, true, v(0, y, 1, 0, vTop, ns), v(0, 0, 1, 0, 1, ns),
				v(1, 0, 1, 1, 1, ns), v(1, y, 1, 1, vTop, ns)));
		quads.add(quad(side, true, v(0, y, 1, 1, vTop, ew), v(0, y, 0, 0, vTop, ew),
				v(0, 0, 0, 0, 1, ew), v(0, 0, 1, 1, 1, ew)));
		quads.add(quad(side, true, v(1, 0, 1, 0, 1, ew), v(1, 0, 0, 1, 1, ew),
				v(1, y, 0, 1, vTop, ew), v(1, y, 1, 0, vTop, ew)));
	}

	private static void centralItem(List<Quad> quads, EffectType type, CubeDirections facing, int rgb,
			boolean rotateable, double slab, double h, double out) {
		int a = 255;
		out += 0.03125;
		h -= 0.03125;
		int c = rgb;
		int ang = -facing.angle;
		Sprite cr = Sprite.CRYSTAL;
		Sprite pl = Sprite.PLANKS;
		VertexList vt = new VertexList();
		switch (type) {
			case COLORIZER -> {
				a = 210;
				c = ReikaColorAPI.mixColors(c, 0xffffff, 0.75F);
				vt.add(-out, h, out, cr, 0, 1);
				vt.add(out, h, out, cr, 1, 1);
				vt.add(out, h, -out, cr, 1, 0);
				vt.add(-out, h, -out, cr, 0, 0);
				vt.add(-out, h, -out, cr, 0, 1);
				vt.add(out, h, -out, cr, 1, 1);
				vt.add(out, slab, -out, cr, 1, 0);
				vt.add(-out, slab, -out, cr, 0, 0);
				vt.add(-out, slab, out, cr, 0, 0);
				vt.add(out, slab, out, cr, 1, 0);
				vt.add(out, h, out, cr, 1, 1);
				vt.add(-out, h, out, cr, 0, 1);
				vt.add(-out, slab, -out, cr, 0, 0);
				vt.add(-out, slab, out, cr, 1, 0);
				vt.add(-out, h, out, cr, 1, 1);
				vt.add(-out, h, -out, cr, 0, 1);
				vt.add(out, h, -out, cr, 0, 1);
				vt.add(out, h, out, cr, 1, 1);
				vt.add(out, slab, out, cr, 1, 0);
				vt.add(out, slab, -out, cr, 0, 0);
			}
			case EMITTER -> {
				int c2 = ReikaColorAPI.mixColors(c, 0xffffff, 0.675F);
				c = 0xffffff;
				vt.add(-out, h, out, pl, 0, 1);
				vt.add(out, h, out, pl, 1, 1);
				vt.add(out, h, -out, pl, 1, 0);
				vt.add(-out, h, -out, pl, 0, 0);
				vt.add(-out, h, -out, pl, 0, 1);
				vt.add(out, h, -out, pl, 1, 1);
				vt.add(out, slab, -out, pl, 1, 0);
				vt.add(-out, slab, -out, pl, 0, 0);
				vt.add(-out, slab, out, pl, 0, 0);
				vt.add(out, slab, out, pl, 1, 0);
				vt.add(out, h, out, pl, 1, 1);
				vt.add(-out, h, out, pl, 0, 1);
				vt.add(-out, slab, -out, pl, 0, 0);
				vt.add(-out, slab, out, pl, 1, 0);
				vt.add(-out, h, out, pl, 1, 1);
				vt.add(-out, h, -out, pl, 0, 1);
				vt.add(out, h, -out, cr, 0, 1, c2);
				vt.add(out, h, out, cr, 1, 1, c2);
				vt.add(out, slab, out, cr, 1, 0, c2);
				vt.add(out, slab, -out, cr, 0, 0, c2);
			}
			case MIRROR, DOUBLEMIRROR, SLITMIRROR -> {
				ang = ang - 90;
				double t = 0.03125;
				double sp = type == EffectType.SLITMIRROR ? 0.09375 : 0;
				Sprite back = type == EffectType.DOUBLEMIRROR ? cr : pl;
				mirrorPlate(vt, out, h, slab, t, sp, cr, back);
				if (type == EffectType.SLITMIRROR)
					mirrorPlate(vt, out, h, slab, t, -sp, back, cr);
			}
			case ONEWAY -> {
				a = 210;
				vt.add(-out, h, out, cr, 0, 1);
				vt.add(out, h, out / 2, cr, 1, 1);
				vt.add(out, h, -out / 2, cr, 1, 0);
				vt.add(-out, h, -out, cr, 0, 0);
				vt.add(out, slab, -out / 2, cr, 0, 0);
				vt.add(out, h, -out / 2, cr, 1, 0);
				vt.add(out, h, out / 2, cr, 1, 1);
				vt.add(out, slab, out / 2, cr, 0, 1);
				vt.add(-out, slab, out, cr, 0, 1);
				vt.add(-out, h, out, cr, 1, 1);
				vt.add(-out, h, -out, cr, 1, 0);
				vt.add(-out, slab, -out, cr, 0, 0);
				vt.add(-out, h, out, pl, 0, 1);
				vt.add(-out, slab, out, pl, 0, 0);
				vt.add(out, slab, out / 2, pl, 1, 0);
				vt.add(out, h, out / 2, pl, 1, 1);
				vt.add(out, h, -out / 2, pl, 1, 1);
				vt.add(out, slab, -out / 2, pl, 1, 0);
				vt.add(-out, slab, -out, pl, 0, 0);
				vt.add(-out, h, -out, pl, 0, 1);
			}
			case POLARIZER -> {
				double t = 0.03125 / 2;
				double spm = 0.1875;
				for (double sp = -spm; sp <= spm; sp += spm / 2) {
					vt.add(-out, slab, t + sp, pl, 0, 0);
					vt.add(out, slab, t + sp, pl, 1, 0);
					vt.add(out, h, t + sp, pl, 1, 1);
					vt.add(-out, h, t + sp, pl, 0, 1);
					vt.add(-out, h, -t + sp, pl, 0, 1);
					vt.add(out, h, -t + sp, pl, 1, 1);
					vt.add(out, slab, -t + sp, pl, 1, 0);
					vt.add(-out, slab, -t + sp, pl, 0, 0);
					plateRim(vt, out, h, slab, t, sp, pl);
				}
			}
			case PRISM -> {
				a = 210;
				ang = ang - 90;
				for (int i = 0; i < 360; i += 90) {
					VertexList vb = new VertexList();
					Sprite p = i == 0 ? cr : pl;
					double outl = out / 1.25;
					double outi = out / 6;
					vb.add(0, h, out, p, 0, 0, 0xffffff);
					vb.add(outl, h, out, p, 1, 0, 0xffffff);
					vb.add(0, h, outi, p, 1, 1, 0xffffff);
					vb.add(-outl, h, out, p, 0, 1, 0xffffff);
					int sc = switch (i) {
						case 90 -> 0xa0a0ff;
						case 180 -> 0xa0ffa0;
						case 270 -> 0xffa0a0;
						default -> 0xffffff;
					};
					vb.add(-outl, h, out, cr, 0, 1, sc);
					vb.add(-outl, slab, out, cr, 0, 0, sc);
					vb.add(outl, slab, out, cr, 1, 0, sc);
					vb.add(outl, h, out, cr, 1, 1, sc);
					vb.add(outl, h, out, cr, 1, 1, 0xffffff);
					vb.add(outl, slab, out, cr, 1, 0, 0xffffff);
					vb.add(0, slab, outi, cr, 0, 0, 0xffffff);
					vb.add(0, h, outi, cr, 0, 1, 0xffffff);
					vb.add(0, h, outi, cr, 0, 1, 0xffffff);
					vb.add(0, slab, outi, cr, 0, 0, 0xffffff);
					vb.add(-outl, slab, out, cr, 1, 0, 0xffffff);
					vb.add(-outl, h, out, cr, 1, 1, 0xffffff);
					if (i > 0)
						vb.rotateY(i);
					vt.addAll(vb);
				}
			}
			case REFRACTOR -> {
				a = 210;
				ang = ang + 180;
				vt.add(-out, h, out, cr, 0, 1);
				vt.add(0, h, 0, cr, 1, 1);
				vt.add(out, h, -out, cr, 1, 0);
				vt.add(-out, h, -out, cr, 0, 0);
				vt.add(-out, h, -out, cr, 0, 1);
				vt.add(out, h, -out, cr, 1, 1);
				vt.add(out, slab, -out, cr, 1, 0);
				vt.add(-out, slab, -out, cr, 0, 0);
				vt.add(-out, slab, -out, pl, 0, 0);
				vt.add(-out, slab, out, pl, 1, 0);
				vt.add(-out, h, out, pl, 1, 1);
				vt.add(-out, h, -out, pl, 0, 1);
				vt.add(out, h, -out, cr, 0, 1);
				vt.add(-out, h, out, cr, 1, 1);
				vt.add(-out, slab, out, cr, 1, 0);
				vt.add(out, slab, -out, cr, 0, 0);
			}
			case SPLITTER -> {
				ang = ang - 90;
				a = 210;
				vt.add(-out, h, -out, cr, 0, 0);
				vt.add(-out, h, 0, cr, 0, 1);
				vt.add(0, h, out, cr, 1, 1);
				vt.add(0, h, -out, cr, 1, 0);
				vt.add(0, h, -out, cr, 1, 0);
				vt.add(0, h, out, cr, 1, 1);
				vt.add(out, h, 0, cr, 0, 1);
				vt.add(out, h, -out, cr, 0, 0);
				vt.add(-out, h, -out, cr, 0, 1);
				vt.add(out, h, -out, cr, 1, 1);
				vt.add(out, slab, -out, cr, 1, 0);
				vt.add(-out, slab, -out, cr, 0, 0);
				vt.add(0, slab, out, cr, 0, 0);
				vt.add(out, slab, 0, cr, 1, 0);
				vt.add(out, h, 0, cr, 1, 1);
				vt.add(0, h, out, cr, 0, 1);
				vt.add(0, h, out, cr, 0, 1);
				vt.add(-out, h, 0, cr, 1, 1);
				vt.add(-out, slab, 0, cr, 1, 0);
				vt.add(0, slab, out, cr, 0, 0);
				vt.add(-out, h, 0, pl, 1, 1);
				vt.add(-out, h, -out, pl, 0, 1);
				vt.add(-out, slab, -out, pl, 0, 0);
				vt.add(-out, slab, 0, pl, 1, 0);
				vt.add(out, slab, 0, pl, 1, 0);
				vt.add(out, slab, -out, pl, 0, 0);
				vt.add(out, h, -out, pl, 0, 1);
				vt.add(out, h, 0, pl, 1, 1);
			}
			case TARGET -> {
				c = 0xffffff;
				vt.add(out, h, -out, pl, 0, 1);
				vt.add(out, h, out, pl, 1, 1);
				vt.add(out, slab, out, pl, 1, 0);
				vt.add(out, slab, -out, pl, 0, 0);
				vt.add(out, slab, -out, pl, 0, 0);
				vt.add(out, slab, out, pl, 1, 0);
				vt.add(out, h, out, pl, 1, 1);
				vt.add(out, h, -out, pl, 0, 1);
				vt.add(-out, h, -out, pl, 0, 1);
				vt.add(out, h, -out, pl, 1, 1);
				vt.add(out, slab, -out, pl, 1, 0);
				vt.add(-out, slab, -out, pl, 0, 0);
				vt.add(-out, slab, -out, pl, 0, 0);
				vt.add(out, slab, -out, pl, 1, 0);
				vt.add(out, h, -out, pl, 1, 1);
				vt.add(-out, h, -out, pl, 0, 1);
				vt.add(-out, slab, out, pl, 0, 0);
				vt.add(out, slab, out, pl, 1, 0);
				vt.add(out, h, out, pl, 1, 1);
				vt.add(-out, h, out, pl, 0, 1);
				vt.add(-out, h, out, pl, 0, 1);
				vt.add(out, h, out, pl, 1, 1);
				vt.add(out, slab, out, pl, 1, 0);
				vt.add(-out, slab, out, pl, 0, 0);
			}
			case TARGET_THRU -> {
				// V33a commented out the pass-through target's central item; RenderLaserTarget draws it.
			}
		}
		if (!type.isOmnidirectional()) {
			if (rotateable) {
				double th = 0.0625;
				vt.offset(0, th, 0);
				double s = 0.225;
				Sprite ch = Sprite.CHISELED;
				vt.add(-s, slab + th, s, ch, 0, 1, 0xffffff);
				vt.add(s, slab + th, s, ch, 1, 1, 0xffffff);
				vt.add(s, slab + th, -s, ch, 1, 0, 0xffffff);
				vt.add(-s, slab + th, -s, ch, 0, 0, 0xffffff);
				vt.add(-s, slab + th, -s, ch, 0, 1);
				vt.add(s, slab + th, -s, ch, 1, 1);
				vt.add(s, slab, -s, ch, 1, 0);
				vt.add(-s, slab, -s, ch, 0, 0);
				vt.add(-s, slab, s, ch, 0, 0);
				vt.add(s, slab, s, ch, 1, 0);
				vt.add(s, slab + th, s, ch, 1, 1);
				vt.add(-s, slab + th, s, ch, 0, 1);
				vt.add(-s, slab, -s, ch, 0, 0);
				vt.add(-s, slab, s, ch, 1, 0);
				vt.add(-s, slab + th, s, ch, 1, 1);
				vt.add(-s, slab + th, -s, ch, 0, 1);
				vt.add(s, slab + th, -s, ch, 0, 1);
				vt.add(s, slab + th, s, ch, 1, 1);
				vt.add(s, slab, s, ch, 1, 0);
				vt.add(s, slab, -s, ch, 0, 0);
			}
			vt.rotateY(ang);
		}
		vt.emit(quads, (a << 24) | (c & 0xffffff));
	}

	/** One mirror plate: front face, back face, then its top and two end rims in planks. */
	private static void mirrorPlate(VertexList vt, double out, double h, double slab, double t,
			double sp, Sprite front, Sprite back) {
		vt.add(-out, slab, t + sp, front, 0, 0);
		vt.add(out, slab, t + sp, front, 1, 0);
		vt.add(out, h, t + sp, front, 1, 1);
		vt.add(-out, h, t + sp, front, 0, 1);
		vt.add(-out, h, -t + sp, back, 0, 1);
		vt.add(out, h, -t + sp, back, 1, 1);
		vt.add(out, slab, -t + sp, back, 1, 0);
		vt.add(-out, slab, -t + sp, back, 0, 0);
		plateRim(vt, out, h, slab, t, sp, Sprite.PLANKS);
	}

	private static void plateRim(VertexList vt, double out, double h, double slab, double t, double sp,
			Sprite rim) {
		vt.add(-out, h, t + sp, rim, 0, 1);
		vt.add(out, h, t + sp, rim, 1, 1);
		vt.add(out, h, -t + sp, rim, 1, 0);
		vt.add(-out, h, -t + sp, rim, 0, 0);
		vt.add(out, slab, -t + sp, rim, 0, 0);
		vt.add(out, h, -t + sp, rim, 0, 1);
		vt.add(out, h, t + sp, rim, 1, 1);
		vt.add(out, slab, t + sp, rim, 1, 0);
		vt.add(-out, slab, t + sp, rim, 1, 0);
		vt.add(-out, h, t + sp, rim, 1, 1);
		vt.add(-out, h, -t + sp, rim, 0, 1);
		vt.add(-out, slab, -t + sp, rim, 0, 0);
	}

	// ---------------------------------------------------------------------------------- full block

	/**
	 * V33a {@code renderFullBlock}: the base texture on every face except the one the device faces
	 * (a target faces the opposite way), which draws the type's front texture in pass 0 and its
	 * colour-tinted overlay in pass 1. Only the emitter and target ever had front art — V33a
	 * registered icons for the other ten types that the jar never shipped — so those keep the base
	 * texture rather than an invented front. No authored room sets {@code fullblock}.
	 */
	private static void fullBlock(List<Quad> quads, EffectType type, CubeDirections facing, int rgb) {
		int front = -1;
		if (facing.isCardinal()) {
			front = switch (facing) {
				case NORTH -> 2;
				case SOUTH -> 3;
				case WEST -> 4;
				default -> 5;
			};
			if (type == EffectType.TARGET)
				front = front ^ 1;
		}
		Sprite back = switch (type) {
			case EMITTER -> Sprite.EMITTER_BACK;
			case TARGET -> Sprite.TARGET_BACK;
			default -> null;
		};
		Sprite overlay = switch (type) {
			case EMITTER -> Sprite.EMITTER_FRONT;
			case TARGET -> Sprite.TARGET_FRONT;
			default -> null;
		};
		float[] shade = {0.4F, 1F, 0.7F, 0.7F, 0.55F, 0.55F};
		for (int i = 0; i < 6; i++) {
			boolean side = i == front && back != null;
			quads.add(cubeFace(i, side ? back : Sprite.BASE, grey(shade[i]), 0, false));
			if (side) {
				int f = (int)(shade[i] * 255);
				int r = (rgb >> 16 & 255) * f / 255, g = (rgb >> 8 & 255) * f / 255, b = (rgb & 255) * f / 255;
				quads.add(cubeFace(i, overlay, 0xFF000000 | r << 16 | g << 8 | b, 0.001, true));
			}
		}
	}

	private static Quad cubeFace(int face, Sprite sprite, int argb, double lift, boolean overlay) {
		double lo = -lift, hi = 1 + lift;
		return switch (face) {
			case 0 -> new Quad(sprite, v(0, lo, 1, 0, 1, argb), v(0, lo, 0, 0, 0, argb),
					v(1, lo, 0, 1, 0, argb), v(1, lo, 1, 1, 1, argb), overlay, !overlay);
			case 1 -> new Quad(sprite, v(1, hi, 1, 1, 1, argb), v(1, hi, 0, 1, 0, argb),
					v(0, hi, 0, 0, 0, argb), v(0, hi, 1, 0, 1, argb), overlay, !overlay);
			case 2 -> new Quad(sprite, v(0, 1, lo, 1, 0, argb), v(1, 1, lo, 0, 0, argb),
					v(1, 0, lo, 0, 1, argb), v(0, 0, lo, 1, 1, argb), overlay, !overlay);
			case 3 -> new Quad(sprite, v(0, 1, hi, 0, 0, argb), v(0, 0, hi, 0, 1, argb),
					v(1, 0, hi, 1, 1, argb), v(1, 1, hi, 1, 0, argb), overlay, !overlay);
			case 4 -> new Quad(sprite, v(lo, 1, 1, 1, 0, argb), v(lo, 1, 0, 0, 0, argb),
					v(lo, 0, 0, 0, 1, argb), v(lo, 0, 1, 1, 1, argb), overlay, !overlay);
			default -> new Quad(sprite, v(hi, 0, 1, 0, 1, argb), v(hi, 0, 0, 1, 1, argb),
					v(hi, 1, 0, 1, 0, argb), v(hi, 1, 1, 0, 0, argb), overlay, !overlay);
		};
	}

	// ----------------------------------------------------------------------------------- helpers

	private static int grey(float f) {
		int g = (int)(f * 255);
		return 0xFF000000 | g << 16 | g << 8 | g;
	}

	private static Vertex v(double x, double y, double z, float u, float v, int argb) {
		return new Vertex(x, y, z, u, v, argb);
	}

	private static Quad quad(Sprite sprite, boolean boundary, Vertex a, Vertex b, Vertex c, Vertex d) {
		return new Quad(sprite, a, b, c, d, false, boundary);
	}

	/**
	 * DragonAPI's {@code TessellatorVertexList}, keeping the per-vertex sprite and optional colour that
	 * the 26.3 port of that class does not expose. Coordinates are relative to the block's bottom
	 * centre, as the ISBRH's translation made them.
	 */
	private static final class VertexList {
		private final List<double[]> pos = new ArrayList<>();
		private final List<Object[]> attr = new ArrayList<>();

		void add(double x, double y, double z, Sprite sprite, float u, float v) {
			pos.add(new double[] {x, y, z});
			attr.add(new Object[] {sprite, u, v, null});
		}

		void add(double x, double y, double z, Sprite sprite, float u, float v, int rgb) {
			pos.add(new double[] {x, y, z});
			attr.add(new Object[] {sprite, u, v, 0xFF000000 | rgb});
		}

		void addAll(VertexList other) {
			pos.addAll(other.pos);
			attr.addAll(other.attr);
		}

		void reverse() {
			Collections.reverse(pos);
			Collections.reverse(attr);
		}

		void offset(double dx, double dy, double dz) {
			for (double[] p : pos) {
				p[0] += dx;
				p[1] += dy;
				p[2] += dz;
			}
		}

		/**
		 * {@code rotateNonOrthogonal(0, deg, 0)} about the origin: DragonAPI's euler-321 matrix applied
		 * through {@code multiplyVectorByMatrix}, i.e. x' = x cos - z sin, z' = x sin + z cos. With
		 * {@code -facing.angle} this turns the east-built item to face its {@link CubeDirections}.
		 */
		void rotateY(double degrees) {
			double r = Math.toRadians(degrees);
			double cos = Math.cos(r), sin = Math.sin(r);
			for (double[] p : pos) {
				double x = p[0], z = p[2];
				p[0] = x * cos - z * sin;
				p[2] = x * sin + z * cos;
			}
		}

		void emit(List<Quad> quads, int defaultArgb) {
			for (int i = 0; i + 3 < pos.size(); i += 4) {
				Vertex[] q = new Vertex[4];
				for (int j = 0; j < 4; j++) {
					double[] p = pos.get(i + j);
					Object[] at = attr.get(i + j);
					int argb = at[3] != null ? (Integer)at[3] : defaultArgb;
					q[j] = new Vertex(p[0] + 0.5, p[1], p[2] + 0.5, (Float)at[1], (Float)at[2], argb);
				}
				quads.add(new Quad((Sprite)attr.get(i)[0], q[0], q[1], q[2], q[3], false, false));
			}
		}
	}
}
