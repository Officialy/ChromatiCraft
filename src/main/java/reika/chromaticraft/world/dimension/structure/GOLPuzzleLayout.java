package reika.chromaticraft.world.dimension.structure;

/** Pure, testable V33a Cellular-Automata dimensions and simultaneous B3/S23 transition rule. */
public record GOLPuzzleLayout(int radius, int width, int maxSelected, int requiredTrail) {

	public static GOLPuzzleLayout forDifficulty(int difficulty) {
		int radius = switch (Math.clamp(difficulty, 1, 3)) {
			case 1 -> 12;
			case 2 -> 16;
			default -> 24;
		};
		float fraction = switch (Math.clamp(difficulty, 1, 3)) {
			case 1 -> 0.8F;
			case 2 -> 0.875F;
			default -> 0.925F;
		};
		int width = radius * 2 + 1;
		return new GOLPuzzleLayout(radius, width, radius * 4, (int)(width * width * fraction));
	}

	public boolean[] step(boolean[] current) {
		if (current.length != width * width)
			throw new IllegalArgumentException("Expected " + (width * width) + " cells, got " + current.length);
		boolean[] next = new boolean[current.length];
		for (int z = 0; z < width; z++) for (int x = 0; x < width; x++) {
			int neighbors = 0;
			for (int dz = -1; dz <= 1; dz++) for (int dx = -1; dx <= 1; dx++) {
				int nx = x + dx;
				int nz = z + dz;
				if ((dx != 0 || dz != 0) && nx >= 0 && nx < width && nz >= 0 && nz < width
						&& current[nz * width + nx]) neighbors++;
			}
			boolean active = current[z * width + x];
			next[z * width + x] = active ? neighbors == 2 || neighbors == 3 : neighbors == 3;
		}
		return next;
	}

	public int index(int x, int z) {
		if (x < 0 || x >= width || z < 0 || z >= width)
			throw new IndexOutOfBoundsException(x + "," + z + " outside " + width + "x" + width);
		return z * width + x;
	}
}
