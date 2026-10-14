package reika.chromaticraft.world.biome;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.SplittableRandom;
import java.util.function.IntBinaryOperator;

/** Seeded, non-overlapping surface regions. Coordinates and masks use Minecraft's four-block quarts. */
public final class LargeBiomeLayout {
    public static final int CELL_QUARTS = 448;
    public static final int CORE_QUARTS = 48;
    /** Furthest quart a region shape can reach from its centre; also the inset keeping it inside its cell. */
    public static final int EXTENT = 128;
    /** V33a assigned biomes on a 1:256 grid; the outline is seeded on a 128-block lattice of this many quarts. */
    private static final int LATTICE = 32;
    private static final int MARGIN = EXTENT;
    private final long seed;
    private final double cliffsChance;
    private final double rainbowChance;

    public enum Kind { CLIFFS, RAINBOW }

    public LargeBiomeLayout(long seed, int cliffsWeight, int rainbowWeight) {
        this.seed = seed;
        double cliffs = Math.max(0, cliffsWeight) / 20.0;
        double rainbow = Math.max(0, rainbowWeight) / 20.0;
        double scale = Math.max(1, (cliffs + rainbow) / .95);
        cliffsChance = cliffs / scale;
        rainbowChance = rainbow / scale;
    }

    public Region candidate(int cellX, int cellZ) {
        long hash = mix(seed ^ 0x4348524F4D414C42L ^ mix(cellX) ^ Long.rotateLeft(mix(cellZ), 32));
        var random = new SplittableRandom(hash);
        double roll = random.nextDouble();
        if (roll >= cliffsChance + rainbowChance)
            return null;
        Kind kind = roll < cliffsChance ? Kind.CLIFFS : Kind.RAINBOW;
        int x = cellX * CELL_QUARTS + MARGIN + random.nextInt(CELL_QUARTS - 2 * MARGIN);
        int z = cellZ * CELL_QUARTS + MARGIN + random.nextInt(CELL_QUARTS - 2 * MARGIN);
        return new Region(kind, x, z, random.nextLong());
    }

    private static long mix(long value) {
        value = (value ^ (value >>> 30)) * 0xbf58476d1ce4e5b9L;
        value = (value ^ (value >>> 27)) * 0x94d049bb133111ebL;
        return value ^ (value >>> 31);
    }

    public record Region(Kind kind, int x, int z, long shapeSeed) {
        public int extent() { return EXTENT; }

        public boolean bounds(int quartX, int quartZ) {
            return Math.abs(quartX - x) <= EXTENT && Math.abs(quartZ - z) <= EXTENT;
        }

        /**
         * The outline is grown the way V33a's GenLayer stack grew any biome: coarse lattice points are
         * magnified by GenLayerZoom (random edge picks, mode-or-random corners) down to quart resolution,
         * then GenLayerSmooth. Lattice points are point samples, so each zoom keeps every parent point in
         * place and the filled lattice core (5x5 less corners) always covers the CORE_QUARTS disc. Seeded
         * lobes on the next ring make each region a lumpy, irregular blob instead of a disc.
         */
        public Shape shape() {
            var random = new SplittableRandom(shapeSeed);
            int reach = EXTENT / LATTICE, size = reach * 2 + 1;
            byte[] grid = new byte[size * size];
            for (int i = -2; i <= 2; i++)
                for (int k = -2; k <= 2; k++)
                    if (Math.abs(i) != 2 || Math.abs(k) != 2)
                        grid[(k + reach) * size + i + reach] = 1;
            int lobes = 2 + random.nextInt(4);
            var frontier = new ArrayList<Integer>();
            for (int n = 0; n < lobes; n++) {
                frontier.clear();
                for (int i = 1 - reach; i < reach; i++) {
                    for (int k = 1 - reach; k < reach; k++) {
                        int at = (k + reach) * size + i + reach;
                        if (grid[at] == 0 && (grid[at - 1] | grid[at + 1] | grid[at - size] | grid[at + size]) != 0)
                            frontier.add(at);
                    }
                }
                grid[frontier.get(random.nextInt(frontier.size()))] = 1;
            }
            for (int spacing = LATTICE; spacing > 1; spacing >>= 1) {
                grid = zoom(grid, size, random);
                size = size * 2 - 1;
            }
            grid = smooth(grid, size, random);
            var bits = new BitSet(grid.length);
            for (int i = 0; i < grid.length; i++) {
                int dx = i % size - EXTENT, dz = i / size - EXTENT;
                if (grid[i] != 0 || dx * dx + dz * dz <= CORE_QUARTS * CORE_QUARTS)
                    bits.set(i);
            }
            return new Shape(x, z, size, bits);
        }

        /** GenLayerZoom over a point lattice: parent (i,k) becomes child (2i,2k); edges and centres are picked. */
        private static byte[] zoom(byte[] parent, int size, SplittableRandom random) {
            int child = size * 2 - 1;
            byte[] out = new byte[child * child];
            for (int k = 0; k < size; k++) {
                for (int i = 0; i < size; i++) {
                    byte a = parent[k * size + i];
                    out[2 * k * child + 2 * i] = a;
                    if (i + 1 < size)
                        out[2 * k * child + 2 * i + 1] = pick(random, a, parent[k * size + i + 1]);
                    if (k + 1 < size)
                        out[(2 * k + 1) * child + 2 * i] = pick(random, a, parent[(k + 1) * size + i]);
                    if (i + 1 < size && k + 1 < size)
                        out[(2 * k + 1) * child + 2 * i + 1] = modeOrRandom(random, a, parent[k * size + i + 1],
                                parent[(k + 1) * size + i], parent[(k + 1) * size + i + 1]);
                }
            }
            return out;
        }

        private static byte pick(SplittableRandom random, byte a, byte b) {
            return random.nextBoolean() ? a : b;
        }

        private static byte modeOrRandom(SplittableRandom random, byte a, byte b, byte c, byte d) {
            if (b == c && c == d) return b;
            if (a == b && a == c) return a;
            if (a == b && a == d) return a;
            if (a == c && a == d) return a;
            if (a == b && c != d) return a;
            if (a == c && b != d) return a;
            if (a == d && b != c) return a;
            if (b == c && a != d) return b;
            if (b == d && a != c) return b;
            if (c == d && a != b) return c;
            return switch (random.nextInt(4)) { case 0 -> a; case 1 -> b; case 2 -> c; default -> d; };
        }

        /** GenLayerSmooth: a point whose opposite neighbours agree on an axis takes that axis' value. */
        private static byte[] smooth(byte[] grid, int size, SplittableRandom random) {
            byte[] out = grid.clone();
            for (int k = 1; k + 1 < size; k++) {
                for (int i = 1; i + 1 < size; i++) {
                    int at = k * size + i;
                    byte west = grid[at - 1], east = grid[at + 1], north = grid[at - size], south = grid[at + size];
                    if (west == east && north == south)
                        out[at] = random.nextBoolean() ? west : north;
                    else if (west == east)
                        out[at] = west;
                    else if (north == south)
                        out[at] = north;
                }
            }
            return out;
        }

        /**
         * Admission requires every quart in the 384-block-wide core to be eligible surface land.
         * Flooding only from its centre removes any detached land across an ocean/mushroom barrier.
         * The callback returns 0 for protected terrain, 1 for land and 2 for rivers/shores.
         */
        public Mask fit(IntBinaryOperator surface) {
            if (surface.applyAsInt(x, z) == 0)
                return null;
            var shape = shape();
            int width = shape.width();
            byte[] terrain = new byte[width * width];
            for (int dz = -CORE_QUARTS; dz <= CORE_QUARTS; dz++) {
                for (int dx = -CORE_QUARTS; dx <= CORE_QUARTS; dx++) {
                    if (dx * dx + dz * dz > CORE_QUARTS * CORE_QUARTS)
                        continue;
                    int type = surface.applyAsInt(x + dx, z + dz);
                    if (type == 0)
                        return null;
                    terrain[(dz + EXTENT) * width + dx + EXTENT] = (byte)type;
                }
            }
            for (int at = shape.bits().nextSetBit(0); at >= 0; at = shape.bits().nextSetBit(at + 1)) {
                int dx = at % width - EXTENT, dz = at / width - EXTENT;
                if (dx * dx + dz * dz > CORE_QUARTS * CORE_QUARTS)
                    terrain[at] = (byte)surface.applyAsInt(x + dx, z + dz);
            }
            BitSet connected = new BitSet(terrain.length);
            int[] queue = new int[terrain.length];
            int head = 0, tail = 1;
            queue[0] = EXTENT * width + EXTENT;
            connected.set(queue[0]);
            while (head < tail) {
                int at = queue[head++];
                int column = at % width;
                for (int next : new int[]{column > 0 ? at - 1 : -1,
                        column + 1 < width ? at + 1 : -1, at - width, at + width}) {
                    if (next >= 0 && next < terrain.length && terrain[next] != 0 && !connected.get(next)) {
                        connected.set(next);
                        queue[tail++] = next;
                    }
                }
            }
            return new Mask(this, width, terrain, connected);
        }
    }

    /** A region's planned outline before terrain admission, centred in a (2 * EXTENT + 1)-quart square. */
    public record Shape(int x, int z, int width, BitSet bits) {
        public boolean contains(int quartX, int quartZ) {
            int dx = quartX - x + EXTENT, dz = quartZ - z + EXTENT;
            return dx >= 0 && dz >= 0 && dx < width && dz < width && bits.get(dz * width + dx);
        }

        /** Radius of the disc with the same area, in quarts. */
        public double equivalentRadius() {
            return Math.sqrt(bits.cardinality() / Math.PI);
        }
    }

    public record Mask(Region region, int width, byte[] terrain, BitSet connected) {
        public int surfaceAt(int quartX, int quartZ) {
            int dx = quartX - region.x() + EXTENT;
            int dz = quartZ - region.z() + EXTENT;
            if (dx < 0 || dz < 0 || dx >= width || dz >= width)
                return 0;
            int index = dz * width + dx;
            return connected.get(index) ? terrain[index] : 0;
        }
    }
}
