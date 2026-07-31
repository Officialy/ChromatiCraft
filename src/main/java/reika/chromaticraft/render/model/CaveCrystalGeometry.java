package reika.chromaticraft.render.model;

import org.joml.Vector3f;

/**
 * The V33a cave-crystal spike geometry, shared by the in-world block model and the inventory item
 * renderer so the two can never drift apart.
 *
 * <p>V33a drew this in {@code CrystalRenderer} as immediate-mode quads, which is why the shapes are
 * arbitrary four-point polygons with hand-authored UVs rather than axis-aligned boxes — they cannot
 * be expressed as model-JSON elements at all. Emitting through a {@link QuadSink} lets the block side
 * bake them into a {@code QuadCollection} while the item side writes them straight to a
 * {@code VertexConsumer}.
 */
public final class CaveCrystalGeometry {

	public static final int TEXTURE_SIZE = 16;

	/** One emitted quad: four points wound front-face, plus the computed normal. */
	@FunctionalInterface
	public interface QuadSink {
		void accept(Point[] points, Vector3f normal);
	}

	public record Point(Vector3f position, float u, float v) {}
	private record BodyUv(float u0, float v0, float u1, float v1) {}

	private CaveCrystalGeometry() {}

	/** The full V33a arm selection: central spike plus the four bit-masked side spikes. */
	public static void emit(QuadSink sink, int mask, boolean flip, boolean above, boolean below) {
		centralSpike(sink, above, flip);
		if ((mask & 8) != 0) xSpike(sink, .1875F, below, flip);
		if ((mask & 4) != 0) xSpike(sink, -.1875F, below, flip);
		if ((mask & 2) != 0) zSpike(sink, .1875F, below, flip);
		if ((mask & 1) != 0) zSpike(sink, -.1875F, below, flip);
	}

private static void centralSpike(QuadSink sink, boolean above, boolean flip) {
		float core = .15F;
		float height = above ? 1F : .8F;
		if (!above) {
		    pointQuad(sink, flip, false,
		            p(.5F-core,height,.5F-core,0,0), p(.5F-core,height,.5F+core,1,0),
		            p(.5F,1,.5F,1,1), p(.5F,1,.5F,1,1));
		    pointQuad(sink, flip, false,
		            p(.5F-core,height,.5F+core,0,0), p(.5F+core,height,.5F+core,1,0),
		            p(.5F,1,.5F,1,1), p(.5F,1,.5F,1,1));
		    pointQuad(sink, flip, false,
		            p(.5F,1,.5F,0,1), p(.5F,1,.5F,1,1),
		            p(.5F+core,height,.5F+core,1,0), p(.5F+core,height,.5F-core,0,0));
		    pointQuad(sink, flip, false,
		            p(.5F,1,.5F,0,1), p(.5F,1,.5F,1,1),
		            p(.5F+core,height,.5F-core,1,0), p(.5F-core,height,.5F-core,0,0));
		}

		BodyUv uv = bodyUv();
		pointQuad(sink, flip, false,
		        p(.5F-core,height,.5F-core,uv.u0,uv.v1), p(.5F+core,height,.5F-core,uv.u1,uv.v1),
		        p(.5F+core,0,.5F-core,uv.u1,uv.v0), p(.5F-core,0,.5F-core,uv.u0,uv.v0));
		pointQuad(sink, flip, false,
		        p(.5F-core,0,.5F+core,uv.u0,uv.v0), p(.5F+core,0,.5F+core,uv.u1,uv.v0),
		        p(.5F+core,height,.5F+core,uv.u1,uv.v1), p(.5F-core,height,.5F+core,uv.u0,uv.v1));
		pointQuad(sink, flip, false,
		        p(.5F+core,height,.5F-core,uv.u0,uv.v1), p(.5F+core,height,.5F+core,uv.u1,uv.v1),
		        p(.5F+core,0,.5F+core,uv.u1,uv.v0), p(.5F+core,0,.5F-core,uv.u0,uv.v0));
		pointQuad(sink, flip, false,
		        p(.5F-core,0,.5F-core,uv.u0,uv.v0), p(.5F-core,0,.5F+core,uv.u1,uv.v0),
		        p(.5F-core,height,.5F+core,uv.u1,uv.v1), p(.5F-core,height,.5F-core,uv.u0,uv.v1));

	}

private static void xSpike(QuadSink sink, float out, boolean below, boolean flip) {
		float core = .12F;
		float height = .55F;
		float rise = height/6F;
		float y = below ? .15F : -.05F;
		float tipHeight = .1F;
		int direction = out > 0 ? 1 : -1;
		boolean reverse = out > 0;
		float near = .5F + core*direction + out;
		float far = .5F + core*direction*3 + out;
		float tip = .5F + core*direction + out*2;

		pointQuad(sink, flip, reverse,
		        p(near,y+height+rise,.5F+core,0,0), p(near,y+height+rise,.5F-core,1,0),
		        p(tip,y+height+rise+tipHeight,.5F,1,1), p(tip,y+height+rise+tipHeight,.5F,1,1));
		pointQuad(sink, flip, reverse,
		        p(tip,y+height+rise+tipHeight,.5F,0,1), p(tip,y+height+rise+tipHeight,.5F,1,1),
		        p(far,y+height,.5F-core,1,0), p(far,y+height,.5F+core,0,0));
		pointQuad(sink, flip, reverse,
		        p(tip,y+height+rise+tipHeight,.5F,0,1), p(tip,y+height+rise+tipHeight,.5F,1,1),
		        p(far,y+height,.5F+core,1,0), p(near,y+height+rise,.5F+core,0,0));
		pointQuad(sink, flip, reverse,
		        p(near,y+height+rise,.5F-core,0,0), p(far,y+height,.5F-core,1,0),
		        p(tip,y+height+rise+tipHeight,.5F,1,1), p(tip,y+height+rise+tipHeight,.5F,1,1));

		BodyUv uv = bodyUv();
		float farScale = 3F;
		float baseScale = below ? 1F : farScale;
		float inset = below ? Math.abs(core*direction) : 0;
		float nearRise = below ? rise*4 : rise;
		float topRise = rise;
		pointQuad(sink, flip, reverse,
		        p(.5F+core*direction,y+nearRise,.5F-core,uv.u0,uv.v0),
		        p(.5F+core*direction*baseScale,y-inset,.5F-core,uv.u1,uv.v0),
		        p(.5F+core*direction*farScale+out,y+height,.5F-core,uv.u1,uv.v1),
		        p(.5F+core*direction+out,y+height+topRise,.5F-core,uv.u0,uv.v1));
		pointQuad(sink, flip, reverse,
		        p(.5F+core*direction+out,y+height+topRise,.5F+core,uv.u0,uv.v1),
		        p(.5F+core*direction*farScale+out,y+height,.5F+core,uv.u1,uv.v1),
		        p(.5F+core*direction*baseScale,y-inset,.5F+core,uv.u1,uv.v0),
		        p(.5F+core*direction,y+nearRise,.5F+core,uv.u0,uv.v0));
		pointQuad(sink, flip, reverse,
		        p(.5F+core*direction+out,y+height+topRise,.5F-core,uv.u0,uv.v1),
		        p(.5F+core*direction+out,y+height+topRise,.5F+core,uv.u1,uv.v1),
		        p(.5F+core*direction,y+nearRise,.5F+core,uv.u1,uv.v0),
		        p(.5F+core*direction,y+nearRise,.5F-core,uv.u0,uv.v0));
		pointQuad(sink, flip, reverse,
		        p(.5F+core*direction*baseScale,y-inset,.5F-core,uv.u0,uv.v0),
		        p(.5F+core*direction*baseScale,y-inset,.5F+core,uv.u1,uv.v0),
		        p(.5F+core*direction*farScale+out,y+height,.5F+core,uv.u1,uv.v1),
		        p(.5F+core*direction*farScale+out,y+height,.5F-core,uv.u0,uv.v1));
		if (!below) {
		    pointQuad(sink, flip, reverse,
		            p(.5F-core*direction*farScale+out*2.56F,y+rise,.5F-core,uv.u0,uv.v1),
		            p(.5F-core*direction*farScale+out*2.56F,y+rise,.5F+core,uv.u1,uv.v1),
		            p(.5F+core*direction*farScale,y,.5F+core,uv.u1,uv.v0),
		            p(.5F+core*direction*farScale,y,.5F-core,uv.u0,uv.v0));
		}
	}

private static void zSpike(QuadSink sink, float out, boolean below, boolean flip) {
		float core = .12F;
		float height = .55F;
		float rise = height/6F;
		float y = below ? .1F : -.1F;
		float tipHeight = .1F;
		int direction = out > 0 ? 1 : -1;
		boolean reverse = out < 0;
		float near = .5F + core*direction + out;
		float far = .5F + core*direction*3 + out;
		float tip = .5F + core*direction + out*2;

		pointQuad(sink, flip, reverse,
		        p(.5F+core,y+height+rise,near,0,0), p(.5F-core,y+height+rise,near,1,0),
		        p(.5F,y+height+rise+tipHeight,tip,1,1), p(.5F,y+height+rise+tipHeight,tip,0,1));
		pointQuad(sink, flip, reverse,
		        p(.5F,y+height+rise+tipHeight,tip,1,1), p(.5F,y+height+rise+tipHeight,tip,1,1),
		        p(.5F-core,y+height,far,1,0), p(.5F+core,y+height,far,0,0));
		pointQuad(sink, flip, reverse,
		        p(.5F,y+height+rise+tipHeight,tip,1,1), p(.5F,y+height+rise+tipHeight,tip,1,1),
		        p(.5F+core,y+height,far,1,0), p(.5F+core,y+height+rise,near,0,0));
		pointQuad(sink, flip, reverse,
		        p(.5F-core,y+height+rise,near,0,0), p(.5F-core,y+height,far,1,0),
		        p(.5F,y+height+rise+tipHeight,tip,1,1), p(.5F,y+height+rise+tipHeight,tip,0,1));

		BodyUv uv = bodyUv();
		float farScale = 3F;
		float baseScale = below ? 1F : farScale;
		float inset = below ? Math.abs(core*direction) : 0;
		float nearRise = below ? rise*4 : rise;
		float topRise = rise;
		pointQuad(sink, flip, reverse,
		        p(.5F-core,y+nearRise,.5F+core*direction,uv.u0,uv.v0),
		        p(.5F-core,y-inset,.5F+core*direction*baseScale,uv.u1,uv.v0),
		        p(.5F-core,y+height,.5F+core*direction*farScale+out,uv.u1,uv.v1),
		        p(.5F-core,y+height+topRise,.5F+core*direction+out,uv.u0,uv.v1));
		pointQuad(sink, flip, reverse,
		        p(.5F+core,y+height+topRise,.5F+core*direction+out,uv.u0,uv.v1),
		        p(.5F+core,y+height,.5F+core*direction*farScale+out,uv.u1,uv.v1),
		        p(.5F+core,y-inset,.5F+core*direction*baseScale,uv.u1,uv.v0),
		        p(.5F+core,y+nearRise,.5F+core*direction,uv.u0,uv.v0));
		pointQuad(sink, flip, reverse,
		        p(.5F-core,y+height+topRise,.5F+core*direction+out,uv.u0,uv.v1),
		        p(.5F+core,y+height+topRise,.5F+core*direction+out,uv.u1,uv.v1),
		        p(.5F+core,y+nearRise,.5F+core*direction,uv.u1,uv.v0),
		        p(.5F-core,y+nearRise,.5F+core*direction,uv.u0,uv.v0));
		pointQuad(sink, flip, reverse,
		        p(.5F-core,y-inset,.5F+core*direction*baseScale,uv.u0,uv.v0),
		        p(.5F+core,y-inset,.5F+core*direction*baseScale,uv.u1,uv.v0),
		        p(.5F+core,y+height,.5F+core*direction*farScale+out,uv.u1,uv.v1),
		        p(.5F-core,y+height,.5F+core*direction*farScale+out,uv.u0,uv.v1));
		if (!below) {
		    pointQuad(sink, flip, reverse,
		            p(.5F-core,y+rise+.0025F,.5F-core*direction*farScale+out*2.56F,uv.u0,uv.v1),
		            p(.5F+core,y+rise+.0025F,.5F-core*direction*farScale+out*2.56F,uv.u1,uv.v1),
		            p(.5F+core,y,.5F+core*direction*farScale,uv.u1,uv.v0),
		            p(.5F-core,y,.5F+core*direction*farScale,uv.u0,uv.v0));
		}
	}

	private static BodyUv bodyUv() {
		float v1 = 1F - 1F/TEXTURE_SIZE;
		float v0 = v1/(TEXTURE_SIZE*1.2F);
		float u0 = 1F/(TEXTURE_SIZE*2F);
		float u1 = 1F - (1F-u0)/(TEXTURE_SIZE*2F);
		return new BodyUv(u0, v0, u1, v1);
	}


	/** Vertical extent of the V33a base plinth, as a fraction of the block. */
	public static final float BASE_HEIGHT = 0.125F;

	/**
	 * V33a {@code CrystalRenderer.renderBase}: the stone plinth drawn under crystal lamps and potion
	 * crystals (cave crystals return {@code renderBase() == false} and get none). It is a 2-pixel-high
	 * slab whose faces carry the original flat shading — white top, 110 underside, 200 north/west and
	 * 170 south/east — baked in through {@link ShadedQuadSink} rather than lit by the block pipeline.
	 *
	 * <p>UVs span the whole base sprite except the side faces, which in V33a take only the top two
	 * texture rows so the plinth reads as a slab edge rather than a squashed full block.
	 */
	public static void emitBase(ShadedQuadSink sink, boolean flip) {
		float top = BASE_HEIGHT;
		float sideV = 2F/TEXTURE_SIZE;
		if (flip) {
			baseQuad(sink, 255, p(0,1,1,0,0), p(1,1,1,1,0), p(1,1,0,1,1), p(0,1,0,0,1));
			baseQuad(sink, 110, p(0,1-top,0,0,1), p(1,1-top,0,1,1), p(1,1-top,1,1,0), p(0,1-top,1,0,0));
			baseQuad(sink, 200, p(0,1,0,0,sideV), p(1,1,0,1,sideV), p(1,1-top,0,1,0), p(0,1-top,0,0,0));
			baseQuad(sink, 170, p(0,1-top,1,0,0), p(1,1-top,1,1,0), p(1,1,1,1,sideV), p(0,1,1,0,sideV));
			baseQuad(sink, 200, p(0,1-top,0,0,0), p(0,1-top,1,1,0), p(0,1,1,1,sideV), p(0,1,0,0,sideV));
			baseQuad(sink, 170, p(1,1,0,0,sideV), p(1,1,1,1,sideV), p(1,1-top,1,1,0), p(1,1-top,0,0,0));
		}
		else {
			baseQuad(sink, 255, p(0,top,1,0,1), p(1,top,1,1,1), p(1,top,0,1,0), p(0,top,0,0,0));
			baseQuad(sink, 110, p(0,0,0,0,0), p(1,0,0,1,0), p(1,0,1,1,1), p(0,0,1,0,1));
			baseQuad(sink, 200, p(0,top,0,0,sideV), p(1,top,0,1,sideV), p(1,0,0,1,0), p(0,0,0,0,0));
			baseQuad(sink, 170, p(0,0,1,0,0), p(1,0,1,1,0), p(1,top,1,1,sideV), p(0,top,1,0,sideV));
			baseQuad(sink, 200, p(0,0,0,0,0), p(0,0,1,1,0), p(0,top,1,1,sideV), p(0,top,0,0,sideV));
			baseQuad(sink, 170, p(1,top,0,0,sideV), p(1,top,1,1,sideV), p(1,0,1,1,0), p(1,0,0,0,0));
		}
	}

	/** A base quad plus its opposite winding, carrying V33a's flat per-face shade. */
	private static void baseQuad(ShadedQuadSink sink, int shade, Point... source) {
		Point[] points = source.clone();
		Vector3f normal = normal(points);
		sink.accept(points, normal, shade);
		Point[] back = {points[3], points[2], points[1], points[0]};
		sink.accept(back, new Vector3f(normal).negate(), shade);
	}

	/** Like {@link QuadSink} but carries V33a's flat 0-255 face shade. */
	@FunctionalInterface
	public interface ShadedQuadSink {
		void accept(Point[] points, Vector3f normal, int shade);
	}

	private static Point p(float x, float y, float z, float u, float v) {
		return new Point(new Vector3f(x, y, z), u, v);
	}

	/**
	 * Applies the ceiling flip and winding reversal, then emits the quad twice with opposite
	 * windings: the legacy immediate-mode quads were visible from either side, and the modern
	 * pipeline culls backfaces, so without the second winding the narrow branch tips vanish.
	 */
	private static void pointQuad(QuadSink sink, boolean flip, boolean reverse, Point... source) {
		Point[] points = new Point[4];
		for (int i = 0; i < 4; i++) {
			Point original = source[reverse ? 3-i : i];
			Vector3f position = new Vector3f(original.position);
			if (flip) position.y = 1F-position.y;
			points[i] = new Point(position, original.u, original.v);
		}
		if (flip) {
			Point swap = points[0]; points[0] = points[3]; points[3] = swap;
			swap = points[1]; points[1] = points[2]; points[2] = swap;
		}
		Vector3f normal = normal(points);
		sink.accept(points, normal);
		Point[] back = {points[3], points[2], points[1], points[0]};
		sink.accept(back, new Vector3f(normal).negate());
	}

	static Vector3f normal(Point[] points) {
		int[][] triangles = {{0,1,2}, {0,2,3}, {0,1,3}, {1,2,3}};
		for (int[] triangle : triangles) {
		    Vector3f a = points[triangle[0]].position;
		    Vector3f cross = new Vector3f(points[triangle[1]].position).sub(a)
		            .cross(new Vector3f(points[triangle[2]].position).sub(a));
		    if (cross.lengthSquared() > 1.0E-8F) return cross.normalize();
		}
		return new Vector3f(0, 1, 0);
	}
}
