package reika.chromaticraft.world.biome;

import java.util.BitSet;
import java.util.SplittableRandom;
import java.util.function.IntBinaryOperator;

/** Seeded, non-overlapping surface regions. Coordinates and masks use Minecraft's four-block quarts. */
public final class LargeBiomeLayout {
    public static final int CELL_QUARTS = 448;
    public static final int CORE_QUARTS = 48;
    private static final int MARGIN = 96;
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
        return new Region(kind, x, z, 60 + random.nextInt(13), random.nextDouble() * Math.PI * 2);
    }

    private static long mix(long value) {
        value = (value ^ (value >>> 30)) * 0xbf58476d1ce4e5b9L;
        value = (value ^ (value >>> 27)) * 0x94d049bb133111ebL;
        return value ^ (value >>> 31);
    }

    public record Region(Kind kind, int x, int z, int radius, double phase) {
        public int extent() { return radius + 6; }

        public boolean contains(int quartX, int quartZ) {
            int dx = quartX - x, dz = quartZ - z;
            double angle = Math.atan2(dz, dx);
            double edge = radius + 4 * Math.sin(3 * angle + phase) + 2 * Math.sin(5 * angle - phase);
            return (long)dx * dx + (long)dz * dz <= edge * edge;
        }

        /**
         * Admission requires every quart in the 384-block-wide core to be eligible surface land.
         * Flooding only from its centre removes any detached land across an ocean/mushroom barrier.
         * The callback returns 0 for protected terrain, 1 for land and 2 for rivers/shores.
         */
        public Mask fit(IntBinaryOperator surface) {
            int extent = extent(), width = extent * 2 + 1;
            byte[] terrain = new byte[width * width];
            if (surface.applyAsInt(x, z) == 0)
                return null;
            for (int dz = -CORE_QUARTS; dz <= CORE_QUARTS; dz++) {
                for (int dx = -CORE_QUARTS; dx <= CORE_QUARTS; dx++) {
                    if (dx * dx + dz * dz > CORE_QUARTS * CORE_QUARTS)
                        continue;
                    int type = surface.applyAsInt(x + dx, z + dz);
                    if (type == 0)
                        return null;
                    terrain[(dz + extent) * width + dx + extent] = (byte)type;
                }
            }
            for (int dz = -extent; dz <= extent; dz++) {
                for (int dx = -extent; dx <= extent; dx++) {
                    if (dx * dx + dz * dz <= CORE_QUARTS * CORE_QUARTS || !contains(x + dx, z + dz))
                        continue;
                    int type = surface.applyAsInt(x + dx, z + dz);
                    terrain[(dz + extent) * width + dx + extent] = (byte)type;
                }
            }
            BitSet connected = new BitSet(terrain.length);
            int[] queue = new int[terrain.length];
            int head = 0, tail = 1;
            queue[0] = extent * width + extent;
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

    public record Mask(Region region, int width, byte[] terrain, BitSet connected) {
        public int surfaceAt(int quartX, int quartZ) {
            int dx = quartX - region.x() + region.extent();
            int dz = quartZ - region.z() + region.extent();
            if (dx < 0 || dz < 0 || dx >= width || dz >= width)
                return 0;
            int index = dz * width + dx;
            return connected.get(index) ? terrain[index] : 0;
        }
    }
}
