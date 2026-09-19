package reika.chromaticraft.render.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/**
 * V33a {@code TieredOreRenderer.renderGeode} geometry, shared by the chunk and item renderers.
 * The source builds sixteen randomly perturbed 8x8 surface patterns and chooses one from the
 * block coordinates; a fixed seed makes the modern bake reproducible while retaining the exact
 * source distributions, edge inset, six face transforms, UV orientation and full-bright ore skin.
 */
public final class TieredOreGeodeGeometry {
	private static final int SECTIONS = 8;
	private static final int PATTERNS = 16;
	private static final Pattern[] GEOMETRY = createPatterns();

	public record Vertex(float x, float y, float z, float u, float v) {}
	public record Quad(Direction face, List<Vertex> vertices) {
		public Quad {
			if (vertices.size() != 4) throw new IllegalArgumentException("A geode face needs four vertices");
			vertices = List.copyOf(vertices);
		}
	}
	public record Pattern(List<Quad> stone, List<Quad> ore) {
		public Pattern {
			stone = List.copyOf(stone);
			ore = List.copyOf(ore);
		}
	}

	private TieredOreGeodeGeometry() {}

	public static Pattern pattern(int index) {
		return GEOMETRY[Math.floorMod(index, PATTERNS)];
	}

	/** V33a's lookup repeats every sixteen blocks on each axis. */
	public static int patternAt(BlockPos pos) {
		int x = Math.floorMod(pos.getX(), 16);
		int y = Math.floorMod(pos.getY(), 16);
		int z = Math.floorMod(pos.getZ(), 16);
		int hash = (x * 31 + y) * 31 + z;
		hash ^= hash >>> 4;
		return hash & 15;
	}

	private static Pattern[] createPatterns() {
		Random random = new Random(0x4348524F4D41474CL);
		Pattern[] patterns = new Pattern[PATTERNS];
		for (int pattern = 0; pattern < PATTERNS; pattern++) {
			double[] stone = offsets(random, false);
			double[] ore = offsets(random, true);
			patterns[pattern] = build(stone, ore);
		}
		return patterns;
	}

	private static double[] offsets(Random random, boolean ore) {
		double[] values = new double[(SECTIONS + 1) * (SECTIONS + 1)];
		for (int i = 0; i <= SECTIONS; i++) {
			for (int k = 0; k <= SECTIONS; k++) {
				double di = Math.min(i, SECTIONS - i) / (double)SECTIONS;
				double dk = Math.min(k, SECTIONS - k) / (double)SECTIONS;
				double curve = Math.pow(di, 0.25) * Math.pow(dk, 0.25);
				double value = ore ? 0.75 + 0.25 * curve : 1 - 0.25 * curve;
				if (di > 0 && dk > 0) {
					double range = ore ? 0.125 : 0.03125;
					value += -range + random.nextDouble() * range * 2;
				}
				// Preserve V33a's original eight-wide indexing, including its shared row boundary.
				values[i * SECTIONS + k] = value;
			}
		}
		return values;
	}

	private static Pattern build(double[] stoneOffsets, double[] oreOffsets) {
		List<Quad> stone = new ArrayList<>(6 * SECTIONS * SECTIONS);
		List<Quad> ore = new ArrayList<>(6 * SECTIONS * SECTIONS);
		double step = 1D / SECTIONS;
		for (int i = 0; i < SECTIONS; i++) {
			double x0 = i * step;
			double x1 = x0 + step;
			for (int k = 0; k < SECTIONS; k++) {
				double z0 = k * step;
				double z1 = z0 + step;
				Vertex[] stoneTop = {
						vertex(x0, stoneOffsets[i * SECTIONS + k + 1], z1, x0, z1),
						vertex(x1, stoneOffsets[(i + 1) * SECTIONS + k + 1], z1, x0, z0),
						vertex(x1, stoneOffsets[(i + 1) * SECTIONS + k], z0, x1, z0),
						vertex(x0, stoneOffsets[i * SECTIONS + k], z0, x1, z1)
				};
				double oreX0 = i == 0 ? 0.2 : x0;
				double oreX1 = i == SECTIONS - 1 ? 0.8 : x1;
				double oreZ0 = k == 0 ? 0.2 : z0;
				double oreZ1 = k == SECTIONS - 1 ? 0.8 : z1;
				Vertex[] oreTop = {
						vertex(oreX0, oreOffsets[i * SECTIONS + k + 1], oreZ1, x0, z0),
						vertex(oreX1, oreOffsets[(i + 1) * SECTIONS + k + 1], oreZ1, x1, z0),
						vertex(oreX1, oreOffsets[(i + 1) * SECTIONS + k], oreZ0, x1, z1),
						vertex(oreX0, oreOffsets[i * SECTIONS + k], oreZ0, x0, z1)
				};
				for (Direction face : Direction.values()) {
					stone.add(face(face, stoneTop));
					ore.add(face(face, oreTop));
				}
			}
		}
		return new Pattern(stone, ore);
	}

	private static Vertex vertex(double x, double y, double z, double u, double v) {
		return new Vertex((float)x, (float)y, (float)z, (float)u, (float)v);
	}

	private static Quad face(Direction face, Vertex[] top) {
		boolean reverse = face == Direction.DOWN || face == Direction.EAST || face == Direction.NORTH;
		List<Vertex> vertices = new ArrayList<>(4);
		for (int i = 0; i < 4; i++) {
			Vertex source = top[reverse ? 3 - i : i];
			vertices.add(transform(face, source));
		}
		return new Quad(face, vertices);
	}

	private static Vertex transform(Direction face, Vertex v) {
		return switch (face) {
			case UP -> v;
			case DOWN -> new Vertex(v.x(), 1 - v.y(), v.z(), v.u(), v.v());
			case WEST -> new Vertex(1 - v.y(), v.x(), v.z(), v.u(), v.v());
			case EAST -> new Vertex(v.y(), v.x(), v.z(), v.u(), v.v());
			case SOUTH -> new Vertex(v.z(), v.x(), v.y(), v.u(), v.v());
			case NORTH -> new Vertex(v.z(), v.x(), 1 - v.y(), v.u(), v.v());
		};
	}
}
