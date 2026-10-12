package reika.chromaticraft.items.tools;

import static org.junit.jupiter.api.Assertions.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

class CaveRouteSearchTest {
    private static class Terrain implements CaveRouteSearch.Terrain {
        final Set<BlockPos> open = new HashSet<>();
        final Set<BlockPos> sky = new HashSet<>();
        int queries;
        public boolean accessible(BlockPos pos) { return true; }
        public boolean passable(BlockPos pos) { queries++; return open.contains(pos); }
        public boolean seesSky(BlockPos pos) { queries++; return sky.contains(pos); }
    }
    private static void finish(CaveRouteSearch search, CaveRouteSearch.Terrain terrain) {
        for (int i = 0; i < 10_000 && search.route().isEmpty()
                && (search.status() == CaveRouteSearch.Status.SEARCHING || search.status() == CaveRouteSearch.Status.RECONSTRUCTING); i++) search.advance(terrain, 7);
        assertFalse(search.status() == CaveRouteSearch.Status.SEARCHING || search.status() == CaveRouteSearch.Status.RECONSTRUCTING);
    }
    @Test void shortestRouteSurvivesTinySlicesAndReconstructionIsBudgeted() {
        var terrain = new Terrain();
        for (int x = 0; x <= 10; x++) { terrain.open.add(new BlockPos(x, 0, 0)); terrain.open.add(new BlockPos(x, 0, 1)); }
        terrain.sky.add(new BlockPos(10, 0, 0));
        var search = new CaveRouteSearch(BlockPos.ZERO, false, -64, 319, 1000);
        assertEquals(1, search.advance(terrain, 1)); assertTrue(search.route().isEmpty()); assertTrue(terrain.queries <= 12);
        while (search.status() == CaveRouteSearch.Status.SEARCHING) assertEquals(1, search.advance(terrain, 1));
        assertEquals(CaveRouteSearch.Status.RECONSTRUCTING, search.status());
        assertEquals(1, search.advance(terrain, 1)); assertTrue(search.route().isEmpty());
        finish(search, terrain);
        assertEquals(11, search.route().size()); assertEquals(BlockPos.ZERO, search.route().getFirst());
        assertEquals(new BlockPos(10, 0, 0), search.route().getLast());
        assertThrows(UnsupportedOperationException.class, () -> search.route().clear());
    }
    @Test void deepestModeChoosesTheGlobalMinimumWithoutAscendingAboveStart() {
        var terrain = new Terrain();
        terrain.open.addAll(List.of(BlockPos.ZERO, new BlockPos(0,-1,0), new BlockPos(0,-2,0),
                new BlockPos(1,0,0), new BlockPos(2,0,0), new BlockPos(2,-1,0), new BlockPos(2,-2,0), new BlockPos(2,-3,0),
                new BlockPos(-1,0,0), new BlockPos(-1,1,0), new BlockPos(-2,1,0), new BlockPos(-3,1,0),
                new BlockPos(-3,0,0), new BlockPos(-3,-1,0), new BlockPos(-3,-2,0), new BlockPos(-3,-3,0), new BlockPos(-3,-4,0)));
        var search = new CaveRouteSearch(BlockPos.ZERO, true, -64, 319, 1000); finish(search, terrain);
        assertEquals(new BlockPos(2,-3,0), search.route().getLast());
        assertTrue(search.route().stream().allMatch(pos -> pos.getY() <= 0));
    }
    @Test void inaccessibleChunksAreNeverQueriedAndClosedCavesTerminate() {
        var terrain = new Terrain() {
            public boolean accessible(BlockPos pos) { return pos.equals(BlockPos.ZERO); }
            public boolean passable(BlockPos pos) { fail("queried inaccessible terrain"); return false; }
            public boolean seesSky(BlockPos pos) { fail("queried inaccessible sky"); return false; }
        };
        var search = new CaveRouteSearch(BlockPos.ZERO, false, -64, 319, 1000); finish(search, terrain);
        assertEquals(CaveRouteSearch.Status.NO_ROUTE, search.status());
    }
    @Test void hugeOpenRegionsStopAtMemoryBoundWithoutClaimingADeepestResult() {
        var terrain = new Terrain() { public boolean passable(BlockPos pos) { return true; } };
        var search = new CaveRouteSearch(BlockPos.ZERO, true, -64, 319, 32); finish(search, terrain);
        assertEquals(CaveRouteSearch.Status.TOO_LARGE, search.status()); assertEquals(32, search.visitedCount()); assertTrue(search.route().isEmpty());
    }
    @Test void modernInclusiveBuildLimitsAndDeepHorizontalLimitAreRespected() {
        var terrain = new Terrain(); terrain.sky.add(new BlockPos(0,319,0));
        var search = new CaveRouteSearch(new BlockPos(0,318,0), false, -64,319,1000); finish(search,terrain);
        assertEquals(new BlockPos(0,319,0),search.route().getLast());
        terrain = new Terrain();
        for (int x=0;x<=401;x++) terrain.open.add(new BlockPos(x,0,0));
        terrain.open.add(new BlockPos(401,-1,0)); terrain.open.add(new BlockPos(400,-1,0));
        search = new CaveRouteSearch(BlockPos.ZERO,true,-1,319,1000); finish(search,terrain);
        assertEquals(new BlockPos(400,-1,0),search.route().getLast());
        assertTrue(search.route().stream().allMatch(pos -> pos.getX()<=400));
    }
}
