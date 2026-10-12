# Subterranean Route Finder server lag — 2026-10-12

Minecraft 26.3 / NeoForge 26.3.0.51-beta.

The port ran the whole breadth-first search synchronously inside Item.use. Each
search head copied its entire path and the queue removed its first ArrayList
element. Sneak mode additionally flood-filled the reachable region to discover
its minimum Y before doing a second search. World queries could synchronously
load/generate chunks. Large caves could therefore monopolize the server tick.

The item now starts a server-thread job. CaveRouteSearch uses a FIFO frontier and
one primitive predecessor entry per discovered block. It preserves shortest-route
BFS ordering and the original passable block families. Normal mode finds a sky
exit; sneak mode finds the deepest reachable point within 400 blocks on each
horizontal axis, never ascending above its start. The same exploration determines
the deepest point and its shortest path, eliminating the preliminary flood fill.
Reaching the world's absolute minimum Y can finish immediately. Vertical bounds
use the real inclusive 26.3 LevelHeightAccessor bounds, including negative Y.

Search, reconstruction and route validation are incremental. ServerTickEvent.Post
shares a 4 ms time target and 1,024 work units across all jobs and dimensions,
round-robin in small slices. A single world query/block update cannot be preempted,
so this is a cooperative budget, not a hard real-time guarantee. Waypoint writes
cost 64 units: at most sixteen block updates globally per tick. The original
every-fourth-position spacing, final endpoint, links and air-only placement remain.
Route validation rejects changed or unloaded terrain before placement.

Only already loaded chunks are queried; the finder neither requests generation
nor holds chunks loaded. Consequently a route outside loaded terrain requires
further exploration and another use. Memory is bounded to 262,144 discovered
positions per job and four concurrent jobs. Exceeding the bound reports failure,
without claiming that a partial scan proved the deepest point. Reusing the item
replaces that player's pending job. Logout, death, dimension changes, level unload
and server stop release pending jobs. Status/failure messages come from ChromaLang
datagen. Searches perform no world access from a worker thread.

Existing trail traversal also had unguarded linked-list loops. Overlapping or
corrupt links could cycle indefinitely on either side, including when clearing a
trail with the Manipulator. Traversal now visits each point once, stops at missing
or unloaded links, and uses a deque instead of repeated list-prefix insertion.

Validation:

- All eleven ChromatiCraft unit tests pass, including five new route regressions:
  shortest route/tiny budgets, budgeted reconstruction, deepest/no-ascent semantics,
  inaccessible terrain, oversized caves, inclusive height and horizontal bounds.
- All four native `chromaticraft:cave_route_*` GameTests pass: actual deferred item
  use/exit waypoints, sneak-mode deepest branch, cyclic/unloaded trail links, and
  replacement/death cancellation. The native server tick subscriber drives jobs.
- All eight accepted-class guards pass, covering 3,283 primary outputs; Chroma
  contributes 1,026. The development jar builds.
- Server and client datagen generate the resources; runtime messages are supplied
  by the language provider. Evidence is in root `build/cave-route-*.log`.

No normal Play arguments or user worlds are changed. Restart normal Play to load
the updated Java classes. The user's exact cave/save was not used as a benchmark;
validation establishes bounded work and the reported behavior in controlled worlds.
