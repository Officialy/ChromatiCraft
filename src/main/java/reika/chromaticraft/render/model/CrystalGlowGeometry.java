package reika.chromaticraft.render.model;

import net.minecraft.core.Direction;

/**
 * V33a {@code CrystalGlowRenderer.renderWorldBlock}'s geometry: an octagonal plate of the base material and,
 * on it, a thinner octagonal crystal bar, both built for a glow standing on a floor (facing UP) and then
 * reflected onto the face it is really on with {@code TessellatorVertexList}'s invert/swap operations.
 *
 * <p>The quads are V33a's own vertex lists, in order: three top quads (a rectangle and the two chamfered ends),
 * then eight sides. There is no bottom face. Each reflection reversed the vertex order to keep faces outward,
 * so an odd number of them reverses a quad here too.
 */
public final class CrystalGlowGeometry {

	private static final double H = 0.03125;
	private static final double H2 = 0.0625;

	@FunctionalInterface
	public interface QuadSink {
		/** Four corners as {x, y, z, u, v}, u and v as fractions of the sprite. */
		void quad(double[][] corners);
	}

	private CrystalGlowGeometry() {}

	/** The base plate. {@code swapped}: the long axis lies along the renderer's reference direction. */
	public static void emitBase(Direction facing, boolean swapped, QuadSink sink) {
		double l = 0.1875;
		double w = 0.0875;
		double el = 0.075;
		double ew = 0.075;
		if (swapped)
			emitPrism(facing, w, l, ew, el, 0, H, sink);
		else
			emitPrism(facing, l, w, el, ew, 0, H, sink);
	}

	/** The crystal bar on top of the plate. */
	public static void emitLamp(Direction facing, boolean swapped, QuadSink sink) {
		double l = 0.1875;
		double w = 0.09375;
		double el = 0.03125;
		double ew = 0.03125;
		if (swapped)
			emitPrism(facing, w, l, ew, el, H, H + H2, sink);
		else
			emitPrism(facing, l, w, el, ew, H, H + H2, sink);
	}

	private static void emitPrism(Direction facing, double l, double w, double el, double ew, double yb, double yt,
			QuadSink sink) {
		double U0 = 0, U1 = 1, V0 = 0, V1 = 1;
		double YT = yt, YB = yb;
		double[][][] quads = {
			{{0.5-w-ew, YT, 0.5+l, U0, V1}, {0.5+w+ew, YT, 0.5+l, U1, V1}, {0.5+w+ew, YT, 0.5-l, U1, V0}, {0.5-w-ew, YT, 0.5-l, U0, V0}},
			{{0.5-w-ew, YT, 0.5-l, U0, V1}, {0.5+w+ew, YT, 0.5-l, U1, V1}, {0.5+w, YT, 0.5-l-el, U1, V0}, {0.5-w, YT, 0.5-l-el, U0, V0}},
			{{0.5-w, YT, 0.5+l+el, U0, V0}, {0.5+w, YT, 0.5+l+el, U1, V0}, {0.5+w+ew, YT, 0.5+l, U1, V1}, {0.5-w-ew, YT, 0.5+l, U0, V1}},
			{{0.5+w+ew, YB, 0.5-l, U0, V0}, {0.5+w+ew, YT, 0.5-l, U1, V0}, {0.5+w+ew, YT, 0.5+l, U1, V1}, {0.5+w+ew, YB, 0.5+l, U0, V1}},
			{{0.5-w-ew, YB, 0.5+l, U0, V1}, {0.5-w-ew, YT, 0.5+l, U1, V1}, {0.5-w-ew, YT, 0.5-l, U1, V0}, {0.5-w-ew, YB, 0.5-l, U0, V0}},
			{{0.5-w, YB, 0.5+l+el, U0, V0}, {0.5+w, YB, 0.5+l+el, U1, V0}, {0.5+w, YT, 0.5+l+el, U1, V1}, {0.5-w, YT, 0.5+l+el, U0, V1}},
			{{0.5-w, YT, 0.5-l-el, U0, V0}, {0.5+w, YT, 0.5-l-el, U1, V0}, {0.5+w, YB, 0.5-l-el, U1, V1}, {0.5-w, YB, 0.5-l-el, U0, V1}},
			{{0.5+w, YT, 0.5+l+el, U0, V0}, {0.5+w, YB, 0.5+l+el, U1, V0}, {0.5+w+ew, YB, 0.5+l, U1, V1}, {0.5+w+ew, YT, 0.5+l, U0, V1}},
			{{0.5-w-ew, YT, 0.5+l, U0, V1}, {0.5-w-ew, YB, 0.5+l, U1, V1}, {0.5-w, YB, 0.5+l+el, U1, V0}, {0.5-w, YT, 0.5+l+el, U0, V0}},
			{{0.5+w+ew, YT, 0.5-l, U0, V1}, {0.5+w+ew, YB, 0.5-l, U1, V1}, {0.5+w, YB, 0.5-l-el, U1, V0}, {0.5+w, YT, 0.5-l-el, U0, V0}},
			{{0.5-w, YT, 0.5-l-el, U0, V0}, {0.5-w, YB, 0.5-l-el, U1, V0}, {0.5-w-ew, YB, 0.5-l, U1, V1}, {0.5-w-ew, YT, 0.5-l, U0, V1}},
		};
		int reflections = switch (facing) {
			case UP -> 0;
			case DOWN, EAST, SOUTH -> 1;
			case NORTH, WEST -> 2;
		};
		for (double[][] quad : quads) {
			double[][] out = new double[4][];
			for (int i = 0; i < 4; i++)
				out[i] = transform(facing, quad[i]);
			if (reflections % 2 == 1) {
				double[][] rev = new double[4][];
				for (int i = 0; i < 4; i++)
					rev[i] = out[3 - i];
				out = rev;
			}
			sink.quad(out);
		}
	}

	/** V33a's per-facing switch: invertY; rotateYtoX; rotateYtoZ+invertZ; rotateYtoZ; rotateYtoX+invertX. */
	private static double[] transform(Direction facing, double[] p) {
		double x = p[0], y = p[1], z = p[2];
		switch (facing) {
			case DOWN -> y = 1 - y;
			case EAST -> { double t = x; x = y; y = t; }
			case NORTH -> { double t = z; z = y; y = t; z = 1 - z; }
			case SOUTH -> { double t = z; z = y; y = t; }
			case WEST -> { double t = x; x = y; y = t; x = 1 - x; }
			default -> {}
		}
		return new double[] {x, y, z, p[3], p[4]};
	}

}
