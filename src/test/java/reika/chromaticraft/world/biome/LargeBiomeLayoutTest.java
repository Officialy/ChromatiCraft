package reika.chromaticraft.world.biome;

import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayDeque;
import java.util.BitSet;
import org.junit.jupiter.api.Test;

class LargeBiomeLayoutTest {
    @Test
    void placementIsStableAcrossSeedsNegativeCoordinatesAndCellEdges() {
        for (long seed : new long[]{0, 1, -1, Long.MIN_VALUE, Long.MAX_VALUE}) {
            var layout = new LargeBiomeLayout(seed, 4, 10);
            var twin = new LargeBiomeLayout(seed, 4, 10);
            for (int x = -20; x <= 20; x++) for (int z = -20; z <= 20; z++) {
                var region = layout.candidate(x, z);
                assertEquals(region, twin.candidate(x, z));
                if (region == null) continue;
                assertEquals(x, Math.floorDiv(region.x() - region.extent(), LargeBiomeLayout.CELL_QUARTS));
                assertEquals(x, Math.floorDiv(region.x() + region.extent(), LargeBiomeLayout.CELL_QUARTS));
                assertEquals(z, Math.floorDiv(region.z() - region.extent(), LargeBiomeLayout.CELL_QUARTS));
                assertEquals(z, Math.floorDiv(region.z() + region.extent(), LargeBiomeLayout.CELL_QUARTS));
                int core = LargeBiomeLayout.CORE_QUARTS;
                for (int dx = -core; dx <= core; dx++) for (int dz = -core; dz <= core; dz++)
                    if (dx * dx + dz * dz <= core * core)
                        assertTrue(region.contains(region.x() + dx, region.z() + dz));
            }
        }
    }

    @Test
    void frequencyUsesConfigWeightsWithoutShrinkingRegions() {
        var normal = new LargeBiomeLayout(57, 4, 10);
        var rare = new LargeBiomeLayout(57, 1, 10);
        int cliffs = 0, rainbow = 0, rareCliffs = 0;
        for (int x = -128; x < 128; x++) for (int z = -128; z < 128; z++) {
            var region = normal.candidate(x, z);
            var other = rare.candidate(x, z);
            if (region != null && region.kind() == LargeBiomeLayout.Kind.CLIFFS) cliffs++;
            if (region != null && region.kind() == LargeBiomeLayout.Kind.RAINBOW) rainbow++;
            if (other != null && other.kind() == LargeBiomeLayout.Kind.CLIFFS) rareCliffs++;
        }
        assertTrue(cliffs > 12000 && cliffs < 14000, "default cliff admission: " + cliffs);
        assertTrue(rainbow > 31000 && rainbow < 34000, "default rainbow admission: " + rainbow);
        assertTrue(rareCliffs > 2800 && rareCliffs < 3700, "lower cliff admission: " + rareCliffs);
    }

    @Test
    void coastalSliversAreRejectedAndDetachedIslandsAreRemoved() {
        var region = new LargeBiomeLayout.Region(LargeBiomeLayout.Kind.CLIFFS, -400, 700, 72, .3);
        assertNull(region.fit((x, z) -> x < region.x() + 30 ? 1 : 0), "small coastal remnant");
        assertNull(region.fit((x, z) -> x == region.x() + 12 && z == region.z() ? 0 : 1),
                "even a protected quart inside the guaranteed core rejects the region");
        var mask = region.fit((x, z) -> x == region.x() + 53 ? 0 : x < region.x() ? 2 : 1);
        assertNotNull(mask);
        assertEquals(0, mask.surfaceAt(region.x() + 60, region.z()), "land beyond the ocean strip is detached");
        assertEquals(2, mask.surfaceAt(region.x() - 10, region.z()), "river/shore family is retained");
        int core = LargeBiomeLayout.CORE_QUARTS;
        for (int dx = -core; dx <= core; dx++) for (int dz = -core; dz <= core; dz++)
            if (dx * dx + dz * dz <= core * core)
                assertTrue(mask.surfaceAt(region.x() + dx, region.z() + dz) != 0);
        assertConnected(mask);
    }

    private static void assertConnected(LargeBiomeLayout.Mask mask) {
        int width = mask.width();
        var visited = new BitSet(width * width);
        var queue = new ArrayDeque<Integer>();
        int start = mask.connected().nextSetBit(0);
        visited.set(start); queue.add(start);
        while (!queue.isEmpty()) {
            int at = queue.remove();
            for (int next : new int[]{at % width > 0 ? at - 1 : -1,
                    at % width + 1 < width ? at + 1 : -1, at - width, at + width}) {
                if (next >= 0 && next < width * width && mask.connected().get(next) && !visited.get(next)) {
                    visited.set(next); queue.add(next);
                }
            }
        }
        assertEquals(mask.connected(), visited, "every accepted quart must be reachable from the main region");
    }
}
