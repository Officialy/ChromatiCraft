package reika.chromaticraft.render.model;

import net.minecraft.core.Direction;

import reika.dragonapi.instantiable.GridDistortion;

/**
 * The eight corners of one crystal piece, the modern stand-in for DragonAPI's {@code CubePoints}.
 *
 * <p>It cannot be an AABB: V33a distorts the piece by displacing the four corners of the growth face
 * and the four of the opposite face independently, along the two axes in that face's plane, which is
 * what stops a crust from looking like a grid of identical pegs. Corner order matches the source's
 * {@code x1y1z1 ... x2y2z2} naming so the offset table below transcribes directly.
 */
final class CrystalPiece {

	/** [corner][axis], corner index = (x2 ? 1 : 0) | (y2 ? 2 : 0) | (z2 ? 4 : 0). */
	private final double[][] corners = new double[8][3];

	private CrystalPiece(double x1, double y1, double z1, double x2, double y2, double z2) {
		for (int i = 0; i < 8; i++) {
			corners[i][0] = (i & 1) == 0 ? x1 : x2;
			corners[i][1] = (i & 2) == 0 ? y1 : y2;
			corners[i][2] = (i & 4) == 0 ? z1 : z2;
		}
	}

	/** V33a renderCrystalPiece's per-direction side positions. */
	static CrystalPiece forSide(Direction side, int a, int b, double w, double h) {
		double a1 = a * w;
		double a2 = a1 + w;
		double b1 = b * w;
		double b2 = b1 + w;
		return switch (side) {
			case DOWN -> new CrystalPiece(a1, 0, b1, a2, h, b2);
			case UP -> new CrystalPiece(a1, 1 - h, b1, a2, 1, b2);
			case WEST -> new CrystalPiece(0, b1, a1, h, b2, a2);
			case EAST -> new CrystalPiece(1 - h, b1, a1, 1, b2, a2);
			case NORTH -> new CrystalPiece(a1, b1, 0, a2, b2, h);
			case SOUTH -> new CrystalPiece(a1, b1, 1 - h, a2, b2, 1);
		};
	}

	private static final int X = 0;
	private static final int Y = 1;
	private static final int Z = 2;

	/**
	 * DragonAPI {@code CubePoints.applyOffset}, transcribed per direction. The A/B letter pairs do
	 * not index the same axes on every face in the source -- DOWN/UP read (x, z), WEST/EAST read
	 * (z, y), NORTH/SOUTH read (x, y) -- so this is written out rather than derived, to avoid
	 * inventing a symmetry the original does not have.
	 *
	 * <p>Each row is {corner index, axis to displace, which A offset, which B offset}, where the
	 * offset selectors are the corner's sign on the two lettered axes.
	 */
	void applyOffset(Direction side, GridDistortion.OffsetGroup off) {
		switch (side) {
			case DOWN -> displace(off, new int[] {0, 1, 4, 5}, X, Z, X, Z);
			case UP -> displace(off, new int[] {2, 3, 6, 7}, X, Z, X, Z);
			case WEST -> displace(off, new int[] {0, 2, 4, 6}, Y, Z, Z, Y);
			case EAST -> displace(off, new int[] {1, 3, 5, 7}, Y, Z, Z, Y);
			case NORTH -> displace(off, new int[] {0, 1, 2, 3}, X, Y, X, Y);
			case SOUTH -> displace(off, new int[] {4, 5, 6, 7}, X, Y, X, Y);
		}
	}

	/**
	 * @param faceCorners the four corners on the growth face
	 * @param axisA       the axis the A offsets displace
	 * @param axisB       the axis the B offsets displace
	 * @param letter1     the axis whose sign selects the first M/P letter
	 * @param letter2     the axis whose sign selects the second M/P letter
	 */
	private void displace(GridDistortion.OffsetGroup off, int[] faceCorners,
			int axisA, int axisB, int letter1, int letter2) {
		for (int corner : faceCorners) {
			boolean first = ((corner >> letter1) & 1) == 1;
			boolean second = ((corner >> letter2) & 1) == 1;
			corners[corner][axisA] += pick(off.offsetAMM, off.offsetAPM, off.offsetAMP, off.offsetAPP, first, second);
			corners[corner][axisB] += pick(off.offsetBMM, off.offsetBPM, off.offsetBMP, off.offsetBPP, first, second);
		}
	}

	private static double pick(double mm, double pm, double mp, double pp, boolean first, boolean second) {
		if (first)
			return second ? pp : pm;
		return second ? mp : mm;
	}

	/** V33a CubePoints.clamp: distortion may not push a piece outside its own block. */
	void clamp() {
		for (double[] corner : corners)
			for (int axis = 0; axis < 3; axis++)
				corner[axis] = Math.clamp(corner[axis], 0D, 1D);
	}

	double get(int corner, int axis) {
		return corners[corner][axis];
	}

	/** Corner indices of one face, wound counter-clockwise seen from outside the piece. */
	static int[] face(Direction side) {
		return switch (side) {
			case DOWN -> new int[] {4, 5, 1, 0};
			case UP -> new int[] {2, 3, 7, 6};
			case NORTH -> new int[] {3, 1, 0, 2};
			case SOUTH -> new int[] {6, 4, 5, 7};
			case WEST -> new int[] {2, 0, 4, 6};
			case EAST -> new int[] {7, 5, 1, 3};
		};
	}
}
