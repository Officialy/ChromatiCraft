# ChromatiCraft 1.7.10 V33a → Minecraft 26.2 / NeoForge port

## Working checkpoint — 2026-08-16 (Proxima decoration blocks)

- Registered eight of V33a's ten `DimDecoTypes` as distinct blocks — Miasma, Floatstone, Lifewater,
  Crystal Lattice, Crystal Leaves, Ocean Stone, Cliff Glass and Glowing Cave Rock — with models,
  block items, loot, language and tags from datagen, all drawing Reika's own `dimgen` art.
- Behaviour is complete rather than nominal: only the solid variants have a collision box, so Miasma,
  Lifewater and Lattice are walked through, which is what lets their collision effects run at all;
  Lifewater heals the living two a tick and burns the undead for four; Miasma stretches every
  beneficial potion effect to twenty minutes and leaves harmful ones alone; Cliff Glass is the only
  lit variant at 12; Floatstone is the beacon base, which 26.2 expresses as `BEACON_BASE_BLOCKS`
  rather than a block method; Glow Cave drops one to six; and the dimension-tuning drop multiplier is
  applied through a `TuningScaledCount` loot provider, which wraps the base roll rather than
  replacing it so the generated JSON still shows what a variant drops before tuning.
- Harvesting is gated on `DECOHARVEST` tuning, and that gate short-circuits outside Proxima exactly as
  upstream's dimension-id test did, so decoration carried home stays breakable. The focused test
  asserts that direction; the in-Proxima refusal is an in-world check, because `GameTestServer` does
  not instantiate datapack dimensions.
- `AQUA` and `GEMSTONE` remain unregistered, blocked on art that does not exist in V33a either. See
  the survey below; their behaviour is recorded in `ProximaDecoTypes` so neither is lost.
- The multi-layer rendering for the six compositing variants is still outstanding; each currently
  draws its real `layer_0`.

## Working checkpoint — 2026-08-16 (Proxima's sky)

- Ported `ChromaSkyRenderer` onto the same seam. Proxima's dimension type now declares
  `Skybox.NONE` — it has no sun, no moon and no horizon glow, so vanilla draws nothing and this draws
  everything: five to seven and a half thousand stars, sixteen nebulae and thirty-two planets,
  billboarded onto a sphere around the viewer with Reika's own `stars`, `stars2` and `planets2` art.
- The details that carry the feel are upstream's: the star count breathes on `5000 + 2500*sin(t/24000)`;
  each star twinkles on its own clock between 0.125 and 4 speed and 0.0625 and 0.375 amplitude, offset
  by its index so they never pulse together; the whole sky turns an eighth of a degree per block
  travelled on each axis plus a slow spin with time; and it fades in between y 18 and y 30, or
  immediately if the viewer can see the sky, so nobody underground is looking at stars through stone.
- The field is built once from a fixed seed, so Proxima's sky is the same every session rather than
  reshuffling on each world load. Placement is vanilla's own star-billboard maths, which is what
  upstream built on — including its rejection of draws outside the unit sphere, which is why a field of
  7500 legitimately contains gaps.
- **Upstream's Proxima early-return in the aurora renderer is deliberately not reinstated.** V33a skips
  its entity renderer inside Proxima and lets the sky renderer draw aurorae, because 1.7.10's fixed
  function pipeline needed them composited with the sky to sort against it. 26.2 does not: the curtain
  draws through the entity submit pipeline, which already orders against the world, and the sky is drawn
  far behind everything. Reinstating the skip would simply lose the aurorae, since the sky pass has no
  entity list to draw from.
- Not included: upstream keeps up to thirty supernovae alive at once, each an animated sprite advancing
  through frames and holding at its midpoint. That animation has its own per-frame timing and is a piece
  of work in itself.

## Working checkpoint — 2026-08-16 (the world-geometry seam, and the sky rivers drawn)

- `WorldGeometryPass` is the seam the port was missing: arbitrary textured geometry drawn into the world
  from a level-render stage. 26.2 offers two routes and neither covered this. The submit pipeline is
  reached through an entity or block-entity renderer and every `SubmitNodeCollector` in `LevelRenderer`
  is private, so a `RenderLevelStageEvent` handler cannot obtain one; post-effect chains operate on the
  finished frame, not on geometry. What remains is what vanilla does for weather — build a mesh, upload
  it, issue one indexed draw against the main colour and depth targets — and that is what this wraps, so
  a caller only emits vertices. The buffer is retained and only grown, since this runs every frame.
- The sky rivers are drawn on it: thirty-six sided tubes whose radius breathes four blocks either side of
  the tunnel radius, phase-shifted along the river so the pulse travels rather than throbbing in place,
  with the hue cycling by position and time and the texture scrolling. Both ends fade to a hundredth
  brightness so a ray tapers out instead of stopping dead in mid-air.
- Drawn in `AfterTranslucentBlocks`, deliberately not the weather or particle stages: those target
  separate buffers the post chain composites, and an additive glow belongs against the finished scene.
- Not included: upstream also spawns particles at both mouths of every ray, pulled towards the opening by
  a `CollectingPositionController`. That controller and its blur particle are DragonAPI and ChromatiCraft
  particle machinery that is not ported, so the mouths are quieter than upstream's.

## Working checkpoint — 2026-08-16 (sky rivers reach the client; the renderer does not)

- The client now has its own copy of the rivers. The server sends the layout seed on join and on every
  dimension change into Proxima, and the client runs the same generator. That is deliberately not what
  V33a does — upstream streams the geometry in scheduled packet batches, because a 1.7.10 client had no
  way to recompute it — but the rivers are wholly seed-derived and there are tens of thousands of
  points, so a single long is enormously cheaper. Upstream already ships a seed to clients for the same
  reason elsewhere, in `StructureCalculator.assignSeed`.
- The client copy is held separately from the server's `active` instance on purpose: on an integrated
  server both live in one process, and drawing from the server's copy would work in single-player and
  silently draw nothing in multiplayer.

**The renderer is blocked on a 26.2 API question, and the attempt was removed rather than left broken.**
Drawing the tubes needs textured geometry in world space from a level-render stage, and that is not what
the submit pipeline is for: `RenderLevelStageEvent` exposes the pose stack, the model-view matrix and
the render state, but every `SubmitNodeCollector` in `LevelRenderer` is private, so the path used for the
aurora curtain — an entity renderer's `submitCustomGeometry` — is not reachable here. The remaining route
is an explicit `RenderPipeline` with a manually built `VertexBuffer`, as `HeatRippleRenderer` does for its
warp pass in RotaryCraft. That is a focused piece of work rather than a detail, and it is what the sky
renderer will need too, so the two belong in one effort.

## Working checkpoint — 2026-08-16 (sky rivers: the transport half)

- Ported `SkyRiverGenerator` and `SkyRiverManager`: the rivers themselves and the part that catches a
  player and carries them. Eight rays radiate from the origin every 45 degrees starting 64 to 256 blocks
  out, a second layer fills the gaps every 11.25 degrees but only from 1024 to 3072 out, and both run
  past the structure ring. They hang at y 384 to 512, wander within a variation that narrows from 10 to
  5 degrees with distance, and step at `max(128, 2*sqrt(d))` so the far ring is not choked with points.
- A rider is pulled at seven blocks a tick along a blend of sixty percent the river's own direction and
  forty percent towards the next node — carried forward while being steered back to the centre line
  rather than flung out of a bend. Upstream's forward-first segment test is kept: a player where both
  segments apply is carried onward, not backwards.
- The tuning gate is upstream's: untuned throws you out and cuts your flight, partially tuned carries you
  a distance that grows as you tune, fully tuned carries you the whole way. The partial limit is jittered
  from the player's id and the world clock so it is not a visible ring in the sky.
- Points are indexed by chunk, which is what makes the per-tick lookup a few map hits rather than a scan
  of every point in the world.

**One deliberate correction to upstream.** `rebuildWithMaxDst` computes its piece count as
`floor(distance / maxDistance)`, which is 1 for any gap shorter than *twice* the target — so the segment
is re-added unchanged and gaps of up to 36 blocks survive a method whose entire purpose is to bound them
at 18. That is not academic: a rider is found by searching within 16 blocks of a point, so at the midpoint
of a 36-block gap they are 18 from each neighbour, out of reach of both, and fall out of the sky at seven
blocks a tick. `ceil` honours the method's own contract. The focused test asserts no gap exceeds twice the
tunnel radius, which is the real budget.

**Still outstanding for sky rivers:** the client has no copy of them, so nothing is drawn yet — the
renderer, the sky renderer and the aurora integration are the next unit.

## Working checkpoint — 2026-08-16 (/locate biome in Proxima)

- `/locate biome` could not find Proxima's biomes, and vanilla's search is why: the command asks for a
  radius of 6400 sampled every 32 blocks, while the Sanctuary alone reaches close to fifteen thousand,
  so every other biome begins outside the search before it starts.
- `ProximaBiomeSource` now overrides `findClosestBiome3d`. The Monument and Structure Fields are not
  searched for at all — they are painted around known placements, so the layout is asked directly and
  answers exactly and instantly. Everything else still uses vanilla's spiral, but with the radius
  widened past the central region; sampling here is an array lookup rather than a noise evaluation, so a
  wide sweep costs little.
- With no layout yet it defers to vanilla entirely rather than inventing an answer. The map genuinely is
  not decided at that point, and a confident wrong coordinate is worse than none.
- **`/locate biome chromaticraft:structure_field` still finds nothing, and that is correct.** A Structure
  Field is painted around a puzzle-structure placement, and a placement exists only for a colour that
  `assignTypes` gave a structure type to. No puzzle generator is ported, so it assigns none and there are
  genuinely zero of them. The Monument Field is placed independently and locates now. The focused test is
  written to keep holding either way: no placements means the search must find nothing, and once
  placements exist it must return the nearest real entry.

## Working checkpoint — 2026-08-16 (Proxima was one biome everywhere)

Reported in world: only `chromaticraft:luminescent_sanctuary` existed in Proxima. Two separate facts,
and only the first is a defect.

- **Nothing ran the layout generators in game.** `ProximaGenerators.regenerate` and `generateNow` were
  called only from GameTests, so `BiomeDistributor.getBiome` answered null for every coordinate, the
  biome source's null fallback is the Central biome, and that was written into every generated chunk.
  The dimension worked perfectly and was one biome wide. `ProximaLayoutLoader` now kicks the chain
  off-thread on server start — the biome paint alone is roughly nineteen seconds, so it wants to run
  while the player is still in the Overworld — and waits for it when the Proxima level loads, which is
  the last moment a wait is free and the first at which a missing layout starts being saved into chunks.
  `awaitLayout` captures the running future under the lock and joins it *outside*, because each
  generator calls the synchronized `finish` as it completes and joining under the lock would deadlock.
- **The Sanctuary really is enormous, and that is upstream's design.** `biomeAt` returns Central for
  anything inside the central region, whose radius is the farthest structure plus a buffer: a ring of
  radius 5000 +/- 3000 about an origin itself offset by up to 6000, plus 200 to 1500, so it approaches
  fifteen thousand blocks. Every other biome is *outside* that. A player near the arrival point is
  correctly surrounded by Sanctuary and has to travel a long way to leave it.

The second fact is why the first was easy to miss, and why the focused test now sweeps to twice the
computed central radius rather than a fixed distance — an earlier version of it swept three thousand
blocks, sat entirely inside the Sanctuary, and would have "confirmed" the bug after it was fixed.

**Existing worlds keep the wrong biomes.** A biome is baked into a chunk when it generates, so Proxima
chunks already visited stay Sanctuary. Only newly generated chunks pick up the real map.

## Working checkpoint — 2026-08-16 (Crystal Leaves' hue shift)

- `ProximaDecoTypes.isHueShifted()` was declared and never called — the same dead-code slip as the drop
  multiplier earlier. V33a's `colorMultiplier` gives Crystal Leaves the position-derived hue Gemstone
  uses, `getModifiedHue(0xFF0000, (x + z*3/2) * 4)`, so a hillside of them runs through the spectrum
  rather than being uniformly red. That is now a registered tint source and actually applied.
- Two halves are needed and only having one is silent. The tint source is one; the other is that a tint
  reaches a face only if the face carries a `tintindex`, which `CUBE_ALL` does not emit. The Crystal
  Leaves model therefore uses vanilla's leaves template, as the port's other tinted leaves already do.
  With a plain cube the tint source would have been registered, correct, and invisible.
- Off-world — an item in a hand or a GUI — there is no position to shift by, so the plain white of
  upstream's own `getRenderColor` is used rather than an arbitrary frozen hue.

## Working checkpoint — 2026-08-16 (Floatstone's overlay)

- Floatstone was drawing only its first layer, so its glowing violet veins were missing and the block
  read as plain dark stone with gaps. Its overlay is now shown.
- It is done as one composited texture rather than a two-element model, and the reason it can be is
  specific to this variant: its two layers are *exactly* complementary — no texel is opaque in both, none
  transparent in both — and `renderIconInPass` puts both in pass 0 with no emissive treatment and no
  per-position choice. So drawing one over the other is pixel-for-pixel identical to a single opaque
  texture, with none of the z-fighting or render-type work a coincident overlay would need.
- The texture is derived, not drawn: `composite.png` is Reika's `layer_1` alpha-composited over her
  `layer_0`, asserted fully opaque at generation time, with both source layers kept beside it.
- There is no animation to port. Both layers are single-frame 16x16 with no `.mcmeta` anywhere in the
  shipped assets, so the glow is a static overlay rather than an animated one.
- The other five compositing variants are still first-layer-only, and now for two clearly different
  reasons: Cliff Glass and Crystal Leaves put their later layers in a real translucent second pass, and
  Glow Cave picks one of nine layer pairs per position at random.

## Working checkpoint — 2026-08-16 (the aurora curtain, rendered)

- The aurora is drawn. `Aurora` is the ported curtain: a chordal spline between the ribbon's endpoints
  with a control point every sixteen blocks, each interior point sliding along the ribbon's own
  perpendicular and picking a new target on arrival. The ends carry zero variance and zero velocity, so
  the curtain waves in the middle while staying anchored, and `posY` is reassigned from the straight
  baseline every tick, so the drift is sideways only. The focused test asserts all three.
- `RenderAurora` emits the curtain through the modern submit pipeline: a strip of quads twenty-four
  blocks tall, gradiented from the first colour at the base to the second at the top, faded over the
  first and last thirty-two steps so it dissolves rather than stopping square, drawn additively with no
  depth write so it glows through cloud and occludes nothing.
- One bug worth recording, because the port introduced it rather than inheriting it: V33a drives the
  drift from the entity's `onUpdate`, which is once per tick, while `extractRenderState` runs once per
  *frame*. Advancing it there unconditionally made the curtain wave at the frame rate — three times too
  fast at sixty frames a second, and different on every machine. It is now gated on the entity's tick
  count.
- Reika's own texture is used: V33a fetches `aurora4.png` remotely through `RemoteSourcedAsset` and
  ships `aurora4_fallback.png` (512x256) as the local copy. The fallback is what is shipped here; the
  4096x2048 remote variant exists upstream if a higher resolution is ever wanted.
- Upstream's frame indexing is reproduced including its oddity: the sheet is eight frames across and four
  down, but V33a indexes with `u = (f%4)*0.125` and `v = (f/4)*0.25` over thirty-two frames, so the
  column only walks the first four of eight while the row runs to 1.75 and wraps twice. It is kept as-is,
  since "fixing" the indices would change how an aurora reads.
- One deliberate difference: V33a's renderer returns early inside Proxima, deferring to the dimension's
  own sky renderer for better ordering against the sky. That sky renderer is not ported, so this draws
  everywhere — otherwise Proxima's aurorae, the only ones that currently generate, would be invisible.
  The early return belongs back here when the sky renderer lands.

## Working checkpoint — 2026-08-16 (aurorae)

- Ported `WorldGenAurorae` and the `EntityAurora` it spawns. One placement is a whole display: one to
  twelve ribbons laid parallel along a bearing, spaced eight to thirty-two apart and sixty to a hundred
  and eighty long, all sharing one colour pair so a display reads as one thing.
- Heights are upstream's and worth keeping exactly. Each end goes forty blocks above the terrain beneath
  it or to 120-220, whichever is higher, then both ends take the higher of the two with five blocks of
  jitter either way — so a display clears mountains, never sinks into a valley, and hangs level rather
  than sloped. The terrain height is read through the heightmap, not by probing blocks: an end can be
  ninety blocks out, far enough that a block read could ask for a chunk that is not there yet.
- Colours come from the weighted table (the three primaries at 100 down to pink at 10) and the second is
  redrawn while the pair is one of the three upstream forbids — Argon, Apple or Green with Pink. The test
  asserts all of it: the shared pair, the forbidden pairs, the height floor, the level ends, the speed
  range and the ribbon count. Each is a property a transcription could drop invisibly.
- `AuroraData` is a record with a codec and a buffer form, since a ribbon has to survive two different
  things: written to disk so it is still overhead after a reload, and sent as custom spawn data because
  an aurora cannot be reconstructed from an entity position alone.
- Upstream's `variance` and `segmentSize` are not carried. Both are declared and serialised there, but
  every assignment to them is commented out, so they are always zero and the renderer parameters that
  would read them are dead.
- The ribbon is not drawn yet. V33a renders it with a client-only spline class in immediate-mode GL,
  which is the same rendering effort the decoration layers and glow-tree overlays are waiting on.
  Everything deciding what an aurora *is* — position, length, colours, speed, persistence, sync — is done.

## Working checkpoint — 2026-08-16 (attaching Proxima's features to its biomes)

- The four ported decoration features were registered but attached to no biome, so none of them
  generated. Registering a feature and attaching it are separate steps, and the gap is silent: the code
  compiles, the configured and placed JSON generate and validate, and the feature simply never appears.
  They are now on the biomes V33a puts them in, and a focused test asserts that wiring so the same gap
  cannot reopen unnoticed.
- The gating is upstream's `generateIn` chain transcribed in its own order: the Central biome takes
  everything not tied to one biome (`isDedicatedBiomeOnly`), a `SKYFEATURE` theme means Skylands, and the
  rest name their biome outright. So Floatstone goes to Skylands and the Sanctuary, the geode to the
  Crystal Plains and the Sanctuary, and the crystal tree and shrub to the Crystal Forest sub-biome alone.
  Glass Cliffs is not in any of these lists because it is a structure, and declares its biomes itself.
- One branch is deliberately not reproduced: upstream lets a Structure or Monument field run any
  non-dedicated generator at a flat 25% chance per attempt. A biome's feature list is static data with no
  such gate, so honouring it would mean a second copy of every placed feature at four times the rarity.
  It is recorded rather than approximated, because a wrong rate is harder to notice than an absent one.

## Working checkpoint — 2026-08-16 (crystal geodes, and a corrected dependency list)

- Ported `WorldGenCrystalPit` as the `crystal_pit` feature: a seventeen by nine ellipsoid carved out and
  lined with Cloak Shielding, a third of the floor cells sprouting a cave crystal. Upstream groups the
  sixteen elements into four palettes and draws every crystal in one pit from one of them, so a geode
  reads as a colour scheme; the focused test asserts a pit never shows more than four kinds.
- The site check is kept whole and tested from both sides: a geode refuses to form if any cell of it is
  liquid, and every lining cell needs solid ground beneath, so a pit cannot hang out of a cliff or over
  a cave.

**Correction to the survey above.** The "portable now" row was derived from a scan of *block* references
and so overstated what was ready. Checking non-block dependencies as well:

| Generator | Actually waiting on |
| --- | --- |
| Crystal Pit | nothing — ported |
| Aurorae | `EntityAurora`, an unported entity |
| Island Arch | `ArchCalculator` — the file exists but is not in the allowlist, so it is still 1.7.10 |
| Mini Altar | `ChromaStacks`, same situation |
| Chroma Meteor | `ModList` cross-mod integration |

Two `.java` files being present is not the same as being ported; the `build.gradle` allowlist is the
only reliable tracker, and this row should have been checked against it the first time.

## Working checkpoint — 2026-08-16 (crystal trees)

- Ported `WorldGenCrystalTree`: all twelve registered crown layouts transcribed cell for cell into
  `CrystalTreeShapes`, the weighted size draw (10, 7, 4, 1 across four classes, so the tallest is about
  one in twenty-two), the per-class trunk height ranges, and the Stone Shielding trunk.
- `RevolvedPattern` was missing from DragonAPI and three layouts need it, so it is ported too: author
  one quadrant, mirror it across both axes, and inset each mirror by `coreSize - 1` so the quadrants sit
  either side of an even-width trunk rather than over it.
- Upstream's `checkSpace` is kept as a whole-shape test before anything is written. That is not
  tidiness: a tree that failed halfway would leave a trunk of Shielding standing with no canopy. The
  check is asymmetric the way upstream makes it — a foliage cell may replace anything leaves can grow
  through, every other cell demands air — so canopies knit together but trunks do not grow through them.
- `XMAS` is kept as a constant and left unregistered, exactly as upstream leaves it: its radius table is
  empty, so it would generate a bare nineteen-block trunk. The focused test asserts that every other
  layout *is* registered, since a dropped layout would simply never appear and no single seed would
  reveal it.

## Working checkpoint — 2026-08-16 (the structure mechanism, and Glass Cliffs)

- Proxima's first `Structure` is in, which is the mechanism the whole oversized class was waiting on:
  `ProximaStructures` holds the structure types and datapack keys, `ProximaStructurePieces` the piece
  types, and both deferred registers are on the mod bus. A modder has to implement
  `StructurePieceType` directly rather than its `ContextlessType` shorthand, since that interface is
  package-private in vanilla.
- `WorldGenGlassCliffs` is ported onto it, arithmetic intact: a bearing, cliffs stepped `6+rand*6`
  apart along the perpendicular, each scaled by `f = 1-0.8*|d|/32` with length falling as `f^0.75` and
  height as `f^0.5`, a wander accumulating `(-len/2+rand*len)/4` per step, and each cliff sweeping its
  length in quarter-block steps stamping discs of radius `0.0625+0.75*0.75^(|d|/length)` that keep the
  tallest height where they overlap. Cliff Glass, three stone under the lip, one grass on top.
- The chain is drawn from the piece's **own stored seed**, never the per-chunk random `postProcess` is
  handed. That is what makes adjacent chunks agree on one cliff instead of each drawing its own, and
  the focused test asserts it by painting the same box twice and comparing.
- The other asserted property is that the piece writes nothing outside the box it is given. Without
  that it would be no safer than the feature it replaced, and the cross-chunk terrain reads that made
  a feature unusable here become legal precisely because every column it touches is inside that box.
- The wander is advanced on every step of the chain whether or not that cliff touched the current
  chunk, so the sequence stays in step across chunk boundaries.

## Design note — 2026-08-16 (how the oversized Proxima generators must be built)

Two of the remaining decoration generators cannot be Features anchored in a chunk, and the reason is
not only their size. Reading their bodies settles the mechanism for the whole class:

- **Glass Cliffs** sweeps a chain of cliffs across roughly a hundred blocks, and its `generate` calls
  `getTopSolidOrLiquidBlock` for *every* cell of its height map. Those are cross-chunk terrain reads,
  which during decoration can synchronously request a neighbouring chunk and deadlock the worldgen
  worker — the same hazard `NBTStructureLoader.place` exists to avoid.
- **Miasma** builds an ellipsoid of radius up to 36, so 73 blocks across against a 48-block window,
  and derives its vertical extent by scanning the origin column for air between
  `TerrainGenSkylandCanyons`' floor and ceiling bounds. That terrain scan has the same problem, and
  the class supplying those bounds is not ported.

A bespoke per-chunk slicing helper was considered and is the wrong answer. 26.2 already has the
mechanism for a shape larger than a chunk: a **`Structure` with pieces**. Its starts are computed at
`STRUCTURE_STARTS`, and each chunk's `postProcess` receives a bounding box clipped to that chunk, so
every column a piece touches is one the caller may both read and write. That solves the write window
and the cross-chunk terrain reads together, where slicing would only solve the first.

So: the oversized generators become Structures, not Features. Floatstone, the Crystal Shrub and the
Nether roof rivers are correctly Features and stay that way — they fit inside a chunk's writable
window and read only their own columns.

Remaining decoration work, in dependency order rather than alphabetically:

| Wants | Generators |
| --- | --- |
| Nothing — portable now | Crystal Tree (526 lines, twelve procedural layouts), Aurorae, Chroma Meteor, Island Arch, Crystal Pit, Mini Altar |
| The Structure mechanism above | Glass Cliffs, Miasma |
| `DIMGENTILE` (`BlockDimensionDecoTile`) | Fire Jets, Glowing Cracks |
| `BEDROCKCRACK`, `VOIDCAVE`, `VOIDRIFT`, `MOLTENLUMEN`, `CHUNKLOADER`, `SPARKLE` | Glow Cave, Fissure, Chunkloader Blocks, Sparkle |
| `ChunkProviderChroma` + `ModOreList` | Terrain Blob |
| Art that does not exist in V33a | Moon Pool (`AQUA`) |

## Working checkpoint — 2026-08-16 (Floatstone drifts)

- Ported `WorldGenFloatstone` as the `floatstone` feature: a cluster twelve to twenty-four blocks above
  its anchor, two to eight veins scattered around it, and upstream's crowding rule shrinking each vein
  once there are four or more.
- Vanilla's `OreFeature` looked like the obvious host — it is `WorldGenMinable`'s descendant — but it
  refuses this outright. Before placing anything it requires the vein to start at or below the
  `OCEAN_FLOOR_WG` heightmap, which is right for buried ore and wrong for a drift hanging in open sky,
  so every call returned false. The 1.7.10 blob algorithm is ported directly instead: a line between
  two endpoints with an ellipsoid swept along it, widest at the middle, targeting air.
- `WorldGenMinable`'s internal eight-block shift is reproduced rather than dropped. It dates from when
  generators were handed chunk corners, and V33a passes block coordinates straight in, so every
  cluster really is biased eight blocks in x and z.
- An earlier note here read the vein size as a width; it is a block count, so forty is a drift about
  nine across and a whole cluster spans roughly twenty-seven blocks. Floatstone therefore fits the
  writable window and needed no slicing. Glass Cliffs (up to 48 long) and Miasma (radius up to 36, so
  73 across) are the ones that genuinely still do.

## Working checkpoint — 2026-08-16 (glow trees and the crystal shrub)

- Registered the Glowing Log and Glowing Sapling, the two thirds of V33a's glow-tree family that were
  genuinely missing. The canopy was not: `GLOWLEAF` is already registered as `glowing_leaves`, because
  `BiomeGlowingCliffs` and the Proxima glow trees place the same block. A duplicate was written and
  then removed on discovering that — worth recording, since the two names are close enough to invite
  the same mistake again.
- `glowing_leaves` now drops what V33a says it drops. Upstream's `getDrops` is an if/else-if — a
  Glowing Sapling on a one-in-fifty roll that Fortune shortens, and only otherwise glowstone dust —
  which an alternatives entry expresses exactly, since the first branch whose conditions pass wins.
  Two Fortune curves were added to `FortuneScaledChance` for it: `RECIPROCAL` for
  `nextInt(max(1, 50-5f)) == 0` and `RECIPROCAL_COMPLEMENT` for `nextInt(1+(1+f)/2) > 0`. The second
  cannot fire without Fortune at all, which is upstream's arithmetic, not an omission.
- With the sapling registered, the two `ChromaChests` Glow Sapling injections it was blocking are in:
  the Dungeon entry at weight 1 and `JUNGLE_DISPENSER` at weight 2, the latter being that location's
  only live entry.
- Ported `WorldGenCrystalShrub` as the `crystal_shrub` feature. Both of upstream's size rolls are kept
  — one in forty for the large form, then one in fifteen for the small — so most attempts grow nothing
  and the Crystal Forest stays sparse; the focused test asserts that rarity rather than only that
  something grows. Five blocks across, so it cannot leave the writable chunks.
- The sapling does not grow yet. `WorldGenLightedTree` is unported, so it extends `VegetationBlock`
  rather than a bonemealable base: inheriting `BonemealableBlock` would advertise a growth it cannot
  perform. Placement, survival, light and drops are complete.

## Proxima decoration survey — 2026-08-16 (what the remaining worldgen is waiting on)

The dimension itself is in: type, level stem, noise settings, biome source, biome map, region layout
and structure placement all generate and have been walked in-world. What is still missing is the
decoration pass — V33a's `DecoratorChroma` running twenty-two `ChromaWorldGenerator`s per chunk from
`ChunkProviderChroma.runDecorators`, gated per biome by `DimensionGenerators.generateIn`.

Mapping each generator to the blocks it places gives a clear order of work rather than an
alphabetical one:

| Blocked on | Generators |
| --- | --- |
| `DIMGEN` / `DIMGENTILE` (`DimDecoTypes`) | Floatstone, Miasma, Glass Cliffs, Moon Pool, Terrain Blob, Terrain Crystal, Fissure, Glow Cave, Crystal Shrub, Crystal Tree, Fire Jets, Glowing Cracks |
| `GLOWLEAF` / `GLOWLOG` | Lighted Shrub, Lighted Tree, Tree Cluster |
| `CHUNKLOADER`, `SPARKLE`, `VOIDRIFT`, `MOLTENLUMEN`, `BEDROCKCRACK`, `VOIDCAVE` | Chunkloader Blocks, Sparkle, and the remaining reach of Fissure and Glow Cave |
| Nothing — every block already ported | Aurorae, Chroma Meteor, Island Arch, Crystal Pit, Mini Altar |

`STRUCTSHIELD`, `LOOTCHEST`, `TIEREDORE`, `TIEREDPLANT`, `RUNE`, `LAMP` and `CRYSTAL` are all in, so
`DimDecoTypes` is the single family that unblocks the most: twelve of the twenty-two.

Reika's own `dimgen` art is already shipped under `textures/block/dimgen/`, so nothing here needs
new artwork — with two exceptions that are upstream holes rather than porting gaps, recorded so they
are not mistaken for something to fill in:

- **`AQUA` has no texture anywhere in V33a.** `BlockDimensionDeco.registerBlockIcons` asks every
  variant for `dimgen/<name>/layer_0` unconditionally, and there is no `aqua` directory in the source
  tree or the shipped assets. It is not dead content — `WorldGenMoonPool` places it — so upstream
  renders it as the missing-texture sprite. V33a's own `logError("...is missing icons!")` fallback
  says Reika shipped this knowingly.
- **`GEMSTONE` is missing `layer_0`** of the three its `numIcons` declares; `layer_1` and `layer_2`
  are present. It is used by `TerrainGenCrystalMountain`.

Porting `DimDecoTypes` is two efforts, not one, and they should not be attempted together:

1. The eight variants with complete art become distinct registered blocks with block items, loot,
   language and tags through datagen. That alone unblocks the generators that only need the block
   identity, and is mechanical.
2. Six variants — Floatstone, Gemstone, Crystalleaf, Oceanstone, Cliffglass and Glowcave — answer
   `hasBlockRender()` and draw through a custom multi-layer ISBRH that picks a layer per position at
   random and runs a second `b` pass; Glowcave alone is ten layers across two passes. That is the
   `DynamicBlockStateModel` shape described in the render notes, and is its own focused effort.

## Working checkpoint — 2026-08-16 (reported worldgen faults, and Nether roof rivers)

- The Nether Temple's redstone came out as isolated dots that carried no signal, so its puzzle could
  not be solved. 1.7.10 had no dot shape: a wire with no wire neighbours was a cross that powered
  everything around it. 26.2's `RedStoneWireBlock.getConnectionState` short-circuits on a wire that
  is already a dot, before its auto-connect logic, so a template authoring `none` on all four sides
  produced wires no placement or neighbour update could ever open up. Templates now author wires as
  crosses and vanilla resolves each to the line or cross its neighbours justify.
- V33a's `ChromaChests` injections are restored. Upstream pushed Information Fragments, Lexicons and
  elemental shards into the vanilla `ChestGenHooks` pools; that seam is gone, so the same table is
  now datagen loot modifiers over the vanilla chest tables. ChromatiCraft's own Nether and Overworld
  structure features hand their chests vanilla table ids, which is why those chests were empty too.
  Every weight and stack size is V33a's. The one value that could not carry over is the 1.7.10 pool
  total those weights were written against; the stand-in is documented in `ChromaChestLoot`. The
  Glowing Sapling entries wait on `BlockLightedSapling`, and are recorded where they belong.
- Large structures no longer generate half built. `ChunkStatus.FEATURES` allows a write radius of one
  chunk, so a feature may only touch a 48 by 48 window, and `InSquarePlacement` starts it anywhere in
  the centre chunk - anything wider than seventeen blocks could reach past the far edge and lose
  everything beyond it. The Nether Diorama (32 by 22) failed from all but one of sixteen offsets, the
  Temple (26 by 26) from nine, and the Overworld Ocean structure (31 across) was in the same position
  unreported. Features now resolve their anchor into the writable window before placing anything.
  That resolution happens in the feature rather than the loader, because annexes, tunnels and chest
  offsets all hang off the same anchor and must slide together with it.
- The Loot Chest was drawing its lid's interior panel across the top. `lootchest.png` is painted for
  1.7.10's chest renderer, which drew through a half turn about X, and Minecraft derives a cuboid's
  six texture regions together from one offset - so the turn decides which region lands on top. V33a's
  own box coordinates are restored and the half turn now lives in the model's render, inherited by
  both the block and item renderers. Geometry is unchanged.
- Ported V33a's `LavaRiverGenerator` as the `nether_lava_river` feature: three simplex fields seeded
  from the world seed, its negation and its complement, thresholds at 0.1 and 0.2 off the placement
  field's zero crossings, and heights mapped across y 127 to 240. Channels carry fluid on a single
  course of Stone Shielding, banks are three courses. The weighted fluid table is written out in full
  and resolved against whatever fluids are installed, exactly as upstream's name lookup did; two
  entries whose modern ids are unknown are recorded rather than guessed. It runs after the roof
  structures, as V33a's chunk populator does, so a river crossing a structure cuts through it.

## Working checkpoint — 2026-08-12 (Biome Fragment dependency vertical)

- Fully ported the V33a Hover Field as the first Biome Fragment prerequisite: all four motion modes,
  permanent/armed/decaying lifecycle, delayed decay, fall-distance reset, sneak bypass, particles,
  tinting, collision/render policy, and persistent blockstate modes are restored.
- Fully ported Light Panels with three original signal roles (`TARGET`, `BLOCK`, `CANCEL`), explicit
  active state, source-exact 0/15 luminance, and generated end/side models using the original six
  colored panel textures. These modes are runtime state, not registry identities; unlike colored
  content families they describe one puzzle block changing state.
- Fully ported the reusable Panel Switch vertical: explicit up/down state, source pitch click,
  persistent level/channel/controller routing in a modern block entity, registered models, block
  entity, item, loot and language data. Its callback targets a typed handler so the exact randomized
  Biome Fragment truth tables can live durably on the structure controller rather than depending on
  V33a's transient generator singleton.
- Ported the Color Lock block/entity vertical: required and still-closed element sets persist by
  element name, open state removes collision, original doubled stone-break transition cue and
  open-state colored particles are retained, creative shard/obsidian configuration remains
  available, and color/gate modes are explicit state. This is one dynamically configured lock—not
  a legacy color-content family—so its colors correctly belong to synchronized block-entity data.
- Ported Lock Key as a complete portable block/entity vertical. Its original room-index metadata is
  an explicit 0–7 channel state (not colored content); its structure UUID and controller coordinate
  survive both world persistence and item pickup through `CUSTOM_DATA`; survival mining emits that
  exact item while creative mining does not; progression-controlled hardness, adjacent-rune lookup,
  controller add/remove callbacks, light, translucent model policy and sparkle activity are restored.
- The natural-structure controller now owns the complete randomized, restart-safe Biome Fragment
  puzzle state: one key channel, four distinct 4-switch masks excluding each door's trivial corner,
  eight unique door colors, the shuffled rune assignment, the selected melody, its eight playable
  crystal identities, player guess, playback clock/cooldown, and completion flag. It binds Panel
  Switches and Lock Keys to itself, configures Color Locks, recomputes rune/key color availability on
  placement/removal, updates the exact V33a Shift Lock door coordinates, drives red/green indicators,
  and rejects players missing BIOMESTRUCT prerequisites.
- Restored the reusable Crystal Music Trigger and the source-exact Crystal Music Manager mapping.
  The trigger retains four clickable face quadrants, redstone-strength-to-note selection, each
  element's tonic/major-or-minor-third/fifth/octave mapping, pitch-scaled ding, elemental burst,
  bounded structure callback discovery, original textures, and generated model/item/loot/lang/tag
  data. Fixed-identity Crystal Lamp and Cave/Potion Crystal families are recognized directly rather
  than requiring the obsolete one-block-plus-color-property model.
- The full V33a prefab catalog is transcribed in `BiomeStructureMelodies`; generation rejects a
  melody unless its notes can be covered by at most eight distinct elemental crystals, then fills
  the remaining positions with unique colors exactly as the source did. Correct trigger notes advance
  the persisted guess, rests are skipped, a wrong note resets it with the error cue, the first nearby
  player receives the automatic playback demonstration, and manual replay observes the original
  forty-tick cooldown. Completion plays the CAST cue, opens the exact central Chroma Door barrier,
  and clears the explicit persistent structure lock on all four lower Loot Chests. That boolean is
  the modern replacement for V33a's Loot Chest metadata bit 8, not a lost metadata behavior.
- The exact 15x14x15 V33a Biome Fragment is now canonical generated NBT. Its shell, stairs (including
  inverted upper flights), shields, eight music triggers, eight caches, rune/lamp placeholders,
  Color/Shift Locks, switches and indicator panels, two portable keys, barriers, liquid cells and
  replay pedestal are authored from the source coordinates. Runtime initialization replaces colored
  placeholders with independently registered per-colour rune/lamp identities, binds every delegate,
  selects chroma/luma/ender-fluid/lava from the natural biome, locks the four lower caches, and adds
  the exact lower/upper Information Fragment count distributions.
- Both `chromaticraft:natural_biome_fragment` and the command-safe
  `/place feature chromaticraft:biome_fragment` are registered. Natural generation is restricted to
  Rainbow Forest, Ender Forest, Luminous Cliffs and its shores (never Rainbow Stream), uses the
  source 640-block/40-chunk scale, sinks through soft blocks/wood/leaves/plants/fluids before
  placement, removes adjacent trunks and intruding leaves, applies the original height-weighted
  moss pass, and clears the roof approach. The explicit biome list avoids the river feature-order
  cycle previously seen with custom biomes.
- Verification: focused `:ChromatiCraft:compileJava`, `:ChromatiCraft:runServerData`, and
  `:ChromatiCraft:runClientData` all pass. The single
  `chromaticraft:biome_fragment_light_panel_switch` GameTest passes with Light Panel luminance,
  Panel Switch interaction, Color Lock partial/full color matching and collision, two-rune Lock Key
  opening/reclosing, and the portable channel/delegate data contract. The stale Luminous Cliffs
  placed-feature warnings found during verification were corrected by adding the required
  `BiomeFilter.biome()` placement gate to all four biome-injected features. The focused
  `chromaticraft:biome_fragment_music_loop` GameTest additionally passes the exact-prefab
  playability invariant, real clickable Music Trigger dispatch through a fixed-color Crystal Lamp,
  wrong-note reset, full sequence completion, all nine central barrier cells, and all four cache
  unlocks. The focused `chromaticraft:biome_fragment_nbt_completion` test passes placement through
  the generated NBT, all eight rune and lamp substitutions, eight triggers/caches, randomized Lock
  Key channel/delegates, initial lower-cache locks, full melody completion, barrier opening and cache
  unlocking. Only that new test was run after this slice; Minecraft reported all required tests
  passed before Spark's Windows helper lingered during process shutdown.

## Non-negotiable rules

- Port behavior fully. A compiling simplification is not a completed port.
- Preserve useful original information. Forward-reference unavailable dependencies or keep a file outside the compile allowlist until its dependency lands.
- A red build is expected while a dependency cluster is active.
- Compare every nontrivial port against the V33a source before accepting it.
- Recipes, models, blockstates, language, advancements, loot, tags, and other data-driven content belong in datagen and must reproduce the original data.
- Use `Sources/minecraft`, `Sources/NeoForge`, and the completed sibling ports as the Minecraft 26.2 API references.
- Legacy metadata colour families must be registered as concrete per-colour block/item identities (stable registry names), never as a colour integer/property on one modern block. This standing rule applies to cave crystals, GeoStrata luminous crystals, CRC dye leaves/saplings/log families, flowers, and every future colour family unless the V33a object was genuinely one identity with runtime state.

## Commands and original source

```text
.\gradlew.bat :ChromatiCraft:compileJava --no-daemon --console=plain
.\gradlew.bat :ChromatiCraft:runGameTest --no-daemon --console=plain
.\gradlew.bat :ChromatiCraft:runClientData --no-daemon --console=plain
.\gradlew.bat :ChromatiCraft:runServerData --no-daemon --console=plain

git -C ChromatiCraft show 5cde0068^:Magic/Network/CrystalNetworker.java
git -C ChromatiCraft show 5cde0068^:TileEntity/Networking/TileEntityCrystalPylon.java
```

The root `ChromatiCraft 1.7.10 V33a.jar` is the runtime asset/reference artifact.

## Baseline recorded 2026-07-24

| Measure | Last committed state | Active working tree |
|---|---:|---:|
| Java files | 1,691 | 1,691 |
| Compile-allowlisted files | 69 | 120 |
| File-count coverage | 4.1% | 7.1% |
| Network-cluster files modified | 0 | 38 |
| `:ChromatiCraft:compileJava` | previously compiled/run | 171 errors, 5 warnings |

The allowlist is a dependency/work tracker, not proof that a file is complete. The 51-file working-tree expansion is **salvageable WIP** until this ledger records source parity and tests. All sibling modules compiled during the baseline run; ChromatiCraft's failures were confined to the active network expansion.

## Working checkpoint — 2026-07-24

- `:ChromatiCraft:compileJava` is down from **171 errors / 5 warnings** to **0 errors**. Current warnings are deprecations in the GameTest mock-player helper, not port failures.
- The 2026-07-26 combined GameTest run passes **all 41 required tests** (36 ChromatiCraft, four RotaryCraft, one Minecraft). Coverage now also includes exact vertical anti-climb behavior and ability immunity, deterministic routed pylon-overload impact, generated casting L1→L2→L3 NBT round-trip/alternative-block parity, the four-tier casting recipe contract, casting-stand ownership/lock/spread behavior, timed atomic table crafting, source-exact temple crystal-group crafting with progression rejection/acceptance, and receiver-capacity saturation.
- The V33a pylon broadcast monument is now canonical generated structure NBT. Its stone/rune geometry is runtime-matched from the template, its source-fluid cells use the registered `chromaticraft:liquid_chroma` identity, and pylon LOS behavior synchronizes from the complete monument. The focused contract test covers activation, one-cell invalidation, ordinary-LOS restoration, repair, and reactivation.
- A real registered pylon can be discovered as a source, transfer an element to a receiver, and debit its own storage.
- A real registered repeater can form a valid path between pylon and receiver; the tested transfer respects path attenuation and charges the source for accepted payload plus loss.
- `CrystalFlow` now keeps receiver payload and source-side loss separate. This fixes the WIP bug where a request for 400 through an attenuating repeater delivered 410.
- `WorldChunk`, `WorldLocation`, and `TileEntityCache` now use resource-key dimensions and stable block/chunk coordinates, serialize modern NBT, resolve loaded server worlds safely, and retain legacy dimension-tag compatibility where practical.
- `CrystalNetworker` now uses dimension-scoped modern `SavedData`, restores tile/cache lifecycle hooks and diagnostics, retains unloaded locations durably, and resolves them back to live tiles after reload.
- DragonAPI `ChunkManager` is now a persistent NeoForge `TicketController` with owner validation and exact live-ticket delta updates. Pylons restore their V33a 3x3 load area, use-triggered acquisition, reload validation, and full/invalidate/break release lifecycle.
- `PylonFinder` has modern level, biome, height, block-state, and LOS boundaries; optional integration intent is retained behind registration hooks.
- `CrystalNetworkLogger` is registered through modern Brigadier commands.
- Pylon, repeater, compound-repeater, and pylon-broadcast geometry is now canonical generated Minecraft structure NBT under `data/chromaticraft/structure/multiblock/`; runtime matching derives its `FilledBlockArray` compatibility view from those templates, with rune-colour substitution, exact deferred registry-ID requirements, and the V33a pylon-link/turbo alternatives applied at runtime.
- TileEntityCrystalPylon rejects empty/fake structure arrays, rematches its real structure on load and periodically, shuts down and drains on structure loss, restores enhanced capacity and charging rates, posts lifecycle events, preserves player-placed source restrictions, restores owner/colour-indexed linked-pylon persistence, 100/500-lumen donation, and aggregated throughput, exposes the exact ordered eight booster/rune locations, clears snow from its structure, owns real chunk tickets, restores V33a encrusted-crystal discovery/colour/growth/special-socket behavior, and restores ordinary hostile scans with 80/10 attack rates, normal/enhancing/enhanced range bands, creative/compatibility exclusions, damage scaling, sounds, colour potion effects, and accelerating player cadence. Its anti-capture path again detects all 26 occupied neighbours, tears down the 3x3x3 shell without drops, launches nearby life, cancels player flight, seeds fires, and can consume a booster. The registered Power Crystal tile now reconnects through its colored rune, preserves multi-owner and legacy placer data through world/item/drop persistence, enforces owner-only mining, applies the original 1→2→4… recharge curve and eight-crystal bonus (768/tick normal), grants the progression hook, and restores disenhance/quarter-drain/lightning/nearby-entity break backlash.
- TileEntityCrystalRepeater restores structure/redstone lifecycle, path priority, failure weight, cluster counting, rain-sensitive link state, grouping, turbo/enhanced behavior, 55-tick overload destruction, two-player ownership, custom data, defensive persistence, and exact 0-11 registered crystal-powder drops.
- `ChromaItems` registers all 35 former V33a `CRAFTING` metadata variants as distinct 26.2 items while preserving original ordinal/name mapping; language and flat item models are datagen-owned. All six active network blocks now have generated blockstate/block/item definitions and client datagen is green.
- All sixteen encrusted block-item variants and sixteen crystal-shard items are registered. Their language and fallback block/item models are datagen-owned, shard sprites are cropped from the authoritative V33a `items_color.png` sheet, and the block's loot table is deliberately empty because its synchronized block entity emits the original growth-scaled per-face shard drops. Server and client datagen are green.

This checkpoint proves an **operational network-engine slice**, not full V33a parity. The active pylon server vertical, routed overload entity, typed client payload/effect layer, and six network model definitions are complete and tested. Broadcast monument geometry, registered liquid-chroma matching, activation, invalidation, and repair are restored. Repeater connection/range rendering was subsequently completed, and the source rain-loss, enhanced-stalk, surge, compound-rune, and overload-burst audiovisual families are now implemented pending the compile/client verification recorded at the end of this ledger. Remaining repeater work is progression catch-up and subtype interactions. The next active milestone is the casting recipe/table runtime on top of the completed inventory and NBT-temple foundation.

## Verified committed foundation

- NeoForge entry point and DeferredRegister families.
- API/interface leaf layer, `CrystalElement`, `ElementMixer`, and basic element-tag types.
- Crystal potion/effect and config/options foundations.
- Sound registry and packet-classification enum.
- Storage, cave crystal, crystal lamp, super crystal, pylon-structure stone, rune, and display-point verticals.
- Base ChromatiCraft block-entity registration/infrastructure.
- Progression DAG and per-player stage/colour state.
- Client/server datagen foundation for language, models, blockstates, loot, and GameTest structures.
- Four progression GameTests; the 2026-07-20 final run recorded all nine combined required tests passing.

The legacy packet enum remains reference data. The active network slice now has typed 26.2 clientbound payloads for pylon beams, discharges, targeted strike feedback, and the anti-jar burst. The standalone item registry contains the 35 V33a crafting-material variants; casting recipes are the next active dependency.

## Current milestone

The source → repeater → receiver loop and first complete casting-table vertical are operational. The
active expansion is dependency-ordered world generation plus client presentation/GUI work. World
structures and monuments use generated NBT templates as their authoritative geometry; Java features
own only placement policy and semantic alternatives that structure palettes cannot represent.

## WIP source-parity audit

Status meanings:

- **Foundation:** useful modern shape; still needs compile/parity verification.
- **Active:** significant 26.2 conversion exists but behavior/API work remains.
- **Deferred dependency:** keep outside the allowlist until its cluster can land fully.
- **Rejected simplification:** restore omitted V33a behavior while retaining useful conversions.

### Contracts, values, and structures

| Files | Status | Required before acceptance |
|---|---|---|
| `NBTTile`, `OwnedTile`, `SneakPop`, `ChargingPoint`, `CrystalReceiver`, `CrystalSource` | Foundation | Match modern ownership, interaction, and persistence contracts against the completed hierarchy. |
| `ElementTagCompound` | Active | Verify every mutation, serialization, clamp, containment, and arithmetic path against V33a. |
| `CrystalTarget`, `CrystalFlow`, `CrystalLink`, `CrystalNetworkException`, `TargetData` | Active | Finish `BlockPos`/`Vec3`/`AABB`, player distance, common/client boundary, and path lifecycle conversions. |
| `PylonStructure`, `RepeaterStructure`, `CompoundRepeaterStructure`, `NBTStructureLoader` | Active | Canonical generated NBT drives all three active layouts; continue the same template-first approach for broadcaster, casting, world structures, monuments, display transforms, and structure-change hooks. |
| `ChromaStructures` | Active | Pylon is correctly classified natural and both active structures instantiate; expand entry-by-entry as each complete structure lands. |
| `ChromaBlocks`, `ChromaTiles`, `ChromaBlockEntities` | Active | Register only fully restored tiles with correct lazy holder and block/entity type relationships. |

### Network engine

| Files | Status | Required before acceptance |
|---|---|---|
| `CrystalNetworker` | Active | Replace `WorldSavedData` with correctly scoped 26.2 `SavedData`; restore cache lifecycle, event hooks, diagnostics, pylon-location data, and persistence. |
| `PylonFinder` | Active | Complete world/biome/ray conversion and all core LOS rules. Isolate optional-mod checks without deleting their intent. Remove null/false compatibility shims. |
| `CrystalNetworkLogger` | Active | Port to Brigadier/modern command registration and current logging APIs. |
| `PylonLinkNetwork` | Active | Modern server-global `SavedData` stores owner/colour webs and durable `WorldLocation` nodes; dedicated client cache payload/overlay synchronization remains. |
| `RelayNetworker` | Deferred dependency | Wait for inventoried receivers and relay tiles. |

### Base hierarchy and concrete tiles

| File | V33a lines | Current lines | Status and parity requirement |
|---|---:|---:|---|
| `TileEntityCrystalBase` | 178 | 175 | Active: finish lifecycle, sync, and `ValueInput`/`ValueOutput` persistence. |
| `CrystalTransmitterBase` | 195 | 159 | Active: restore connection cache, invalidation, render state, ownership, range, and throughput. |
| `CrystalReceiverBase` | 290 | 278 | **Rejected simplification:** restore efficiency-upgrade cost scaling and receiver invariants. |
| `InventoriedCrystalReceiver` | 126 | 126 | Deferred with managed item handlers/containers/casting. |
| `TileEntityCrystalPylon` | 1,509 | growing | Active: the server behavior cluster now includes vertical defense, ability immunity, unstable overload routing/short-circuit, colour effects, enclosure rejection, typed client presentation, and complete broadcast-monument activation/invalidation/repair in addition to the previously restored structure, storage, enhancement, ownership, booster, growth, persistence, and network behavior. |
| `TileEntityPylonEnhancer`, `TileEntityChromaCrystal` | 26 / 131 | modernized | Active and tested: multi-owner plus legacy placer persistence, item/drop round-trip, owner-only mining, rune-color pylon discovery/reconnect, registered block/entity/tile, break callback, and server-visible destruction FX are restored. V33a destruction droplets/seeds and pylon-backlash node particles now use dedicated client payloads; visual runtime verification remains. |
| `TileEntityCrystalRepeater` | 782 | growing | Active: structure/redstone lifecycle, priority, degradation/throughput, grouping, rain state, overload with exact powder drops, two-player ownership, custom data, persistence, connection/range display, and the source rain/enhanced/surge audiovisual families are restored. The final presentation patch still needs compile/client verification; progression catch-up and subtype interactions remain. |
| `TileEntityCompoundRepeater` | 246 | growing | Active: multi-element throughput, structure, independent depth, direct-pylon attenuation, persistence, and the source 32-tick colour-rune cycle are restored; the final particle patch still needs compile/client verification. |
| `TileEntityCreativeSource` | 132 | 129 | Active: verify unlimited-source and ownership/placement semantics. |
| `TileEntitySkypeater` | 170 | 177 | Active: port sky/rain/biome/environment and node-class behavior. |
| `TileEntityCrystalBroadcaster` | 386 | 386 | Deferred on broadcaster structure/wireless behavior. |
| `TileEntityPylonLink` | 212 | modernized | Active and tested: registered owner-bound link tile, nine-block pylon discovery, chunk tickets, unlink lifecycle, persistent server web, client connection sync/particles, donation, and throughput aggregation are restored; global client cache payload/overlay remains. |
| `TileEntityRelaySource` | 294 | 294 | Deferred with inventoried receiver/relay transport. |
| `TileEntityWeakRepeater` | 446 | 446 | Deferred until its structure/environment/degradation dependencies are ready. |

## Optional compatibility policy

- **Thaumcraft is dormant for the Minecraft 26.2 port.** There is no modern compatible target, so no Thaumcraft API types, registrations, adapters, recipes, aspects, nodes, or wand hooks may enter the active compile allowlist.
- The untouched V33a `modinterface/thaumcraft` sources remain outside the allowlist as historical reference rather than being deleted or misleadingly half-ported.
- When a core file containing old Thaumcraft behavior is ported, preserve its intent in a `// CHROMA-PORT: Thaumcraft` comment and keep the actual imports/interfaces/runtime calls disabled. Pylon comments now record its former `INode`/`IWandable`, aspect, node-modifier/type, and wand-drain responsibilities.
- If a viable modern implementation appears later, restore it as an isolated optional integration layer so the core crystal network never has a hard dependency.
## Foundational seams from the 171-error baseline

1. `WorldSavedData` → `SavedData`, factories, scoping, and dirty-state lifecycle.
2. `World`/`IBlockAccess` → `Level`, `LevelReader`, or `ServerLevel` by mutation needs.
3. Raw coordinates/entity fields/dimension IDs → `BlockPos`, `Vec3`, and resource keys.
4. `AxisAlignedBB` → `AABB`; client frustum queries must leave common network data.
5. Legacy biome classes/IDs and precipitation queries → holders, tags, and modern height APIs.
6. Legacy ray tracing/metadata transparency → block states, tags, and predicates.
7. `ICommandSender` and old DragonAPI commands → Brigadier/NeoForge commands.
8. Missing DragonAPI contracts (`CrashNotifications`, `AdjacentUpdateWatcher`, biome helpers): use the modern equivalent or port the useful contract fully.
9. Removed optional integrations: preserve intent in isolated compatibility code or forward references; do not erase behavior or mix dead APIs into the core pathfinder.

## Dependency order

1. Pure network values and contracts.
2. DragonAPI prerequisites used by the core path.
3. Coordinate/world/geometry modernization.
4. Saved network and pylon-location data.
5. LOS/pathfinding/cache invalidation.
6. Crystal tile base hierarchy.
7. Pylon, repeater, creative source, skypeater, and compound repeater.
8. Structure validation and registrations.
9. Source-to-receiver GameTests.
10. Modern payloads required for progression/network sync.
11. Inventoried receiver and casting-table vertical.

## Network milestone tests

- Direct source-to-receiver transfer.
- Source → repeater → receiver routing. **Covered.**
- Per-element capacity and throughput limits. **Partially covered:** pylon capacity plus normal/turbo/enhanced/grouped repeater throughput.
- Efficiency/loss calculation. **Covered for the current one-repeater loop; expand to multi-hop and rain/group cases.**
- LOS obstruction invalidates a path; clearing it permits a new path. **Covered.**
- Removing/unloading a tile invalidates cached paths safely. **Covered.**
- Saved network/pylon state survives reload. **Covered.**
- Ownership/access restrictions. **Covered for player-placed pylons, two-player repeater mining/drop authorization, and power-crystal same-owner grouping plus item-NBT round-trip.**
- Power-crystal acceleration and break backlash. **Covered: exact eight-crystal 768/tick normal recharge, disenhancement, and quarter-energy drain.**
- Multi-colour routing without energy cross-contamination. **Covered through the compound repeater.**
- Natural versus player-placed source behavior. **Covered.**
- Existing progression tests remain green. **Covered in every combined run.**

## Acceptance checklist

- Original V33a responsibilities enumerated and retained.
- No stub/default result stands in for gameplay logic.
- No behavior-bearing call is deleted because its dependency is unported.
- Correct 26.2 API selected from local Minecraft/NeoForge sources.
- Common/server code is client-class free.
- Persistence, networking, registration, and data implications handled.
- File allowlisted only with its required dependency cluster.
- Focused regression tests added where headless verification is possible.
- This ledger updated with decisions and compatibility boundaries.

## Five-stream completion checkpoint — 2026-07-26

- Vertical defense restores the V33a volume, distance cadence, LOS, and ability immunity.
- Unstable pylons again emit routed overload entities, short-circuit pylons, overload fuses, and
  retain recovery / booster-destruction catastrophe behavior.
- Four typed 26.2 payloads provide attack beams, discharges, player-hit feedback, and anti-jar bursts.
- The overload pulse has a fullbright submit-pipeline renderer. All six active network blocks have
  generated blockstates and block/item models; client datagen is green. This earlier 35-test checkpoint
  is superseded by the 38/38 casting-runtime checkpoint below.
- The focused network/progression/casting-structure suite is green at 35/35.

## Casting-table vertical — active

The canonical `InventoriedCrystalReceiver` now uses `WorldlyContainer`,
`NonNullList<ItemStack>`, `ContainerHelper`, and `ValueInput`/`ValueOutput`.

Casting tiers L1-L3 are canonical generated structure NBT at `multiblock/casting_l1` through
`casting_l3`. Runtime Java loads those templates and overlays only V33a alternative matches that
a single palette cannot encode: rune-or-smooth floor cells, fire-or-air lightning guards, the L3
brick band, and its resource-ring posts. The 35th GameTest places and matches every tier and verifies
the alternatives.

The registered `chromaticraft:casting` recipe type and serializer now encode all four physical tiers:
exact 3x3 grid occupancy, temple runes, auxiliary-stand offsets, pylon aura requirements, output,
duration, and experience. Server datagen emits the first complete dependency-ready V33a family:
16 shard-colour smooth-stone recipes plus eight crystalline-stone shape conversions, with original
patterns, counts, five-tick duration, and five XP. Generated JSON round-trips through the 26.2 codec.

The casting stand is now a registered owner-bound one-slot inert container. It preserves V33a locking,
owner-only use/mining, table linkage, contents-on-break, item/custom-data persistence, spread-fill,
sounds, and client particle intent, with generated loot/model/lang. Focused matching, ownership/lock,
and survival spread-fill tests are green.

The casting table is now a registered owner-bound ten-slot `InventoriedCrystalReceiver` and active
block. The V33a controller path is restored around the data-driven recipe system: recipe-manager
selection prefers the highest accessible physical tier; pylon recipes can be selected before their
aura arrives; XP gates the four table tiers; generated NBT validates temple/multiblock/pylon geometry
at the original table-minus-one anchor; runes and the original stepped stand coordinates are scanned;
stands are linked, locked, synchronized, and released; batches are capacity-limited; the declared
duration runs before a single atomic input/remainder/aura/output commit; completion awards XP and
progression and records recipe/output history; and active work, owner, energy, inventory, XP, and
history survive modern `ValueInput`/`ValueOutput` persistence. Teardown unlinks the stand ring and
dynamic drops retain every inventory slot. Models, blockstate, item definition, loot, and language are
datagen-owned.

The shared receiver seam was corrected at the same boundary: delivery now caps against current stored
energy and returns the amount actually accepted; compound requests can report success; difference
requests no longer mutate their caller's tag; requests send only remaining capacity; and the stale
adjacent-update signature now implements the modern DragonAPI contract. The combined suite is
**41/41 green**, including a registered five-tick CrystalStone craft that proves inputs remain intact
until the final atomic commit and a saturation test that proves no integer overflow at maximum storage.

The 13 former `CLUSTER` metadata variants are now distinct stable item identities in their original
ordinal order, with exact V33a display names, datagen item definitions/models, and sprites cropped from
indices 80-92 of the authoritative `items_resource.png`. The dependency-complete ordinary green and
white `CrystalGroupRecipe` paths are datapack recipes with the original five grid inputs, rune offsets,
20-tick duration, and 40 XP. The focused temple GameTest places the canonical L1 NBT structure, proves
that a 250-XP table still rejects a player without `RUNEUSE`, then grants the stage and proves the
atomic green-group result and table-XP award. The controller now restores V33a recipe progression gates
(`CRYSTALS`; then `RUNEUSE`; then `MULTIBLOCK`; then `PYLON` + `REPEATER`) and the original quarter-rate
player experience award in addition to table XP.

Next casting work is dependency expansion: port charged-shard identities/charging behavior and the
tiered-resource identity/progression layer without generic-item placeholders. That unlocks the exact
Crystal Mirror multiblock recipe, red/orange groups, cluster/core/star chain, and then a real high-core
pylon recipe. Add source → repeater → table aura delivery, stand locking/consumption, and reload
mid-craft GameTests as those dependencies land. Then restore tuning keys, focus acceleration,
enhancement effects, repeater table-grouping, and the injector. Thaumcraft remains dormant and excluded;
optional Botania behavior stays isolated until a compatible dependency is selected.

## Casting dependency expansion and live pylon loop — 2026-07-28

The charged-shard state machine is now a stable 26.2 item-identity layer: all sixteen ordinary and
sixteen charged shards retain their V33a colour ordering, names, authoritative sprites, charging
conversion, and recipe distinction. The four dependency-complete ordinary/charged crystal groups,
primary/secondary clusters, crystal core, and crystal star are exact source-derived datapack recipes.
The V33a source audit also established that the metadata-12 multishard is loot-only; no invented
"bunch" recipe has been added.

`ChromaTieredItems` now exposes the source-exact Enderstone Powder, Fire Essence, and Spatial Rifting
Powder identities required by the first pylon recipes, with authoritative V33a sheet crops and
datagen-owned item definitions and language. The high-core void, transformation, and energy recipes
are registered at PYLON tier with their exact final 24-stand maps, rune coordinates, 400-tick duration,
500 XP, and 5000-lumen black/gray/yellow requirements. Their JSON is generated through the casting
recipe codec; the legacy construction-order overwrites in the outer corner map were preserved as the
actual final V33a layouts rather than flattened into an earlier intermediate state.

The casting-table runtime now proves the complete live loop rather than a prefilled receiver:
`pylon -> colour-selected repeater -> table`. The focused test places the canonical L3 NBT monument,
forces the source outside the pylon's direct 48-block range, verifies both LOS legs and exactly one
repeater depth, observes the exact 5000-lumen buffer and source debit, then verifies the delayed atomic
high-energy-core commit, aura drain, 24-stand consumption, and 500 table XP award. The full combined
suite is **47 required tests** (42 ChromatiCraft, four RotaryCraft, one Minecraft).

All active ChromatiCraft multiblocks remain NBT-first. Java is limited to template loading and the
V33a alternatives that a single structure palette cannot express; no casting or monument geometry is
being reintroduced as an authoritative Java builder.

Next casting slice: expand the remaining dependency-ready multiblock and pylon recipes in source order;
add explicit structure-loss cancellation/reload tests around long-running aura crafts; then restore
tuning keys, focus-crystal acceleration, enhancement effects, repeater table grouping, and injectors as
their registered dependencies land. Thaumcraft remains intentionally dormant and excluded, with its
historical behavior retained only as commented port intent until a modern target exists.

## Multiblock dependency expansion and cancellation safety — 2026-07-28

The next V33a source audit accepted six more dependency-complete MULTIBLOCK recipes into server
datagen: Void Core, Transformation Core, Energy Core, Crystal Focus, and both Crystal Mirror variants
(glass and charged-white-shard centers). Their exact center inputs, inner/outer stand elevations,
charged shard colours, dust placements, rune coordinates, 100-tick duration, and 200 XP are encoded in
the same casting codec. Crystal Lens, Element Unit, Iridescent Chunk, and Lumen Core remain deferred
with their untouched source behavior because their focus/purity/resonance/beacon/elemental identities
are not yet registered; no generic ingredient substitutes were introduced.

The controller regression boundary is stronger. A focused test breaks a mandatory outer foundation
cell from the canonical L2 NBT template during a 200-tick craft and proves immediate cancellation,
zero output/XP/history, complete input preservation, and release of every stand lock. The live
pylon-to-repeater-to-table test now serializes and reconstructs the active table after its exact 5000
yellow lumens arrive, then proves recipe key, remaining timer, aura, inputs, and the final atomic commit
survive modern block-entity persistence. The combined suite is **48/48 green**.

Next dependency seam is the remaining tiered-resource identity band (purity, focus, element, beacon,
binding, and resonance materials) plus its authoritative V33a sprites. That unlocks Crystal Lens,
Element Unit, Iridescent Chunk, Lumen Core, and further source-ordered machine recipes before casting
tuning/focus acceleration and injector behavior can be restored without placeholders.

## Tiered-resource recipes and multi-element pylon commit — 2026-07-28

The V33a tiered metadata-2 through metadata-7 band is now represented by six stable 26.2 item
identities: Purification Powder, Focal Powder, Infused Dust, Transmissive Dust, Binding Crystal, and
Resonant Dust. Their explicit legacy metadata values preserve the source mapping independently of
registry or enum order. Client datagen owns their item definitions, models, and exact V33a display
names; the six sprites are authoritative crops from indices 130-135 of `items_resource.png`.

That identity layer unlocks three fully dependency-complete source recipes. Crystal Lens and
Iridescent Chunk are MULTIBLOCK recipes with their exact final center/stand layouts, 100-tick
durations, and 200 XP. Lumen Core is a PYLON recipe with the final V33a 24-stand map (including the
four inner Purification Powder overwrites), 400-tick duration, 500 XP, and simultaneous 60000-lumen
BLACK, YELLOW, and BLUE requirements. All three are generated datapack JSON through the registered
casting codec.

Element Unit remains intentionally deferred rather than weakened: the V33a recipe also consumes all
sixteen behavioral Elemental Stones, whose chroma-fluid charging/deposit and progression path has not
yet landed. Binding Crystal alone is not a sufficient dependency boundary, and no generic stones or
identity-only substitutes have been invented.

Two focused regressions now prove the new boundary. One performs Crystal Lens then Iridescent Chunk
on the same canonical L2 NBT table and verifies both atomic stand commits and cumulative XP. The other
matches Lumen Core on canonical L3 NBT, begins with exactly 60000 of each required color, and proves
all three aura balances, all 24 stands, output, and XP commit together. The combined suite is
**50/50 green**. Next: port the Elemental Stone/chroma-fluid behavior before Element Unit, then resume
the source-ordered casting dependency graph and restore tuning/focus acceleration and injectors.

## Element Unit, tuning, focus, grouping, and first GUI — 2026-07-28

The Elemental Stone dependency boundary is now behavioral rather than identity-only. Liquid chroma,
the active chroma pool, all sixteen colour-bound Elemental Stones, their exact interaction and
persistence semantics, and the source-derived Element Unit recipe are accepted. The Element Unit
uses the original physical ingredients and all sixteen stones; no generic placeholder path remains.

The personal casting-tuning subsystem now reproduces the V33a deterministic twelve-rune key,
world/player association, matching rules, progression step, and modern persistence. The eight
dedicated casting focus sockets restore tier-derived additive acceleration and controller linkage.
Rendering/querying acceleration is read-only on the client; linkage remains server-authoritative.

Pylon casting repeater groups are restored from source. The controller inspects the exact sixteen
outer repeater locations, requires all sixteen distinct colours, validates the four original
four-colour groups by side, marks qualifying repeaters, and awards +25% throughput per matching side.
The base V33a throughput formula scales against the MULTIBLOCK XP threshold: a newly PYLON-tier table
at 15000 XP receives 600 lumens/tick and scales up to 1000 before the persisted grouping multiplier
is applied. A focused regression proves the full +100% case reaches 1200 at unlock and proves a
duplicate colour clears both the bonus and prior grouped state.

That regression exposed a generated-structure parity defect: the L3 outer repeater stalks had used
column stone at y+2, while V33a requires smooth stone at both y+1 and y+2. The canonical
`casting_l3.nbt` provider and generated template now contain the correct geometry. Runtime Java adds
only the necessary alternative checks, including colour-agnostic matching for the sixteen outer rune
blocks; NBT remains the authoritative monument geometry.

The first modern GUI vertical is accepted at the menu boundary only. `ChromaMenus` registers a typed
casting-table menu; the owner-gated block opens it through `ServerPlayer.openMenu`; and the ten-slot
server menu preserves the tier-dependent 3x3 layout, protected result slot, player inventory, range,
and ownership contract. The initial screen was a diagnostic dashboard and was not source-faithful;
it is superseded by the V33a-parity port recorded below. Craft initiation remains on the
source-correct manipulator/automation path instead of being conflated with opening the table.

`compileJava` is green and the combined in-world suite is **56/56 green**. The next GUI boundary is
the casting injector/automation controller, but it must land with the complete
`CastingAutomationSystem`/recursive-crafting behavior and its server synchronization; the pristine
V33a controller will not be reduced to an inventory-only shell. After that, port accepted machine and
item GUIs dependency-first through the same typed `MenuType` and screen-registration pattern.

## Item-stand renderer and first overworld feature — 2026-07-28

The casting item stand now has a registered Minecraft 26.2 submit-pipeline block-entity renderer.
Its registered model layer reproduces all eighteen V33a Techne cuboids with their source UV origins,
pivots, mirror flags, and compound rotations, rendered with the authoritative V33a `itemstand.png`;
the placed baked model is particle-only so it does not leave the earlier WIP cube around that body.
The BER also restores V33a's dynamic held-item behavior: the exact 1/2/3/4/5/6 visible-copy thresholds,
time-based rotation and bobbing, count-dependent scale compensation, orbit and inner spin, and the
manipulator-only stack-count billboard. It uses `ItemModelResolver`/`ItemStackRenderState`, not legacy
immediate-mode rendering. Every inventory mutation now performs server-authoritative block-entity sync so
tracking clients update the displayed stack without a block remesh.

World generation now has its first complete modern vertical. `CrystalFeature` is a registered
`Feature<NoneFeatureConfiguration>` with generated configured-feature, placed-feature, and NeoForge
biome-modifier JSON. It retains V33a's sixty attempts per chunk, Overworld 4-63 and Nether 4-127
height bands, Nether quarter density, mushroom/ocean/hill density bonuses, Rainbow Forest tag seam,
End and superflat exclusion, all original support categories, soft/replaceable target rule, liquid
rejection, required air exposure, random selection among sixteen concrete cave-crystal registry identities, and public `CrystalGenEvent`.
The feature owns its scatter loop, so the placed feature deliberately carries no duplicate count or
height modifiers. Worldgen writes use the modern no-neighbor-update flag.

The V33a Mystcraft dense/crystal-page and Twilight Forest density branches have no selected modern
dependencies and remain dormant compatibility intent; they were not silently mapped to unrelated
dimensions or biomes. Thaumcraft remains commented/dormant as previously directed.

Server datagen emits and successfully loads:

- `data/chromaticraft/worldgen/configured_feature/cave_crystal.json`
- `data/chromaticraft/worldgen/placed_feature/cave_crystal.json`
- Overworld and Nether `neoforge:add_features` biome modifiers at `underground_decoration`

A focused GameTest proves supported placement, exact colour identity, unsupported rejection, liquid
rejection, and enclosed-position rejection. `compileJava` and server datagen are green, and the full
combined suite is now **57/57 green**. The spark duplicate mock-player shutdown exception remains an
external profiler-only artifact after all required tests have completed successfully.

Next worldgen work is dependency-first: audit the remaining small decorators and generators against
V33a, land only those whose blocks/events/biome contracts are complete, then build pylon, data-tower,
dungeon, and monument generation on generated NBT templates plus modern structure/placement
registration. Large structures will not be converted into authoritative Java coordinate builders.


### 2026-07-28 — item-stand/casting-table/cave-crystal render follow-up

- `ChromaSounds` playback now uses the registered 26.2 `SoundEvent` directly; this removes the obsolete DragonAPI sound-library lookup that rejected `ITEMSTAND` at runtime.
- The casting item stand now renders the exact V33a 18-part Techne body from its original 128×128 entity texture without relying on block-atlas stitching. Its item uses a codec-registered 26.2 special model; the placed BER retains count-based floating copies and manipulator count text.
- Casting-table datagen now maps the original `table_top`, `table_bottom`, and `table_side` textures to `cube_bottom_top`.
- Cave crystals now use a registered 26.2 dynamic blockstate model (chunk mesh, not a block entity): stable position-seeded arm masks, four angled arms, and ceiling inversion. Source parity uses the single V33a `crystal_outline` texture on every colour variant with a registered block tint source; the earlier per-colour texture mapping was incorrect. The hand-authored custom blockstate is intentionally excluded from generated-blockstate validation.
- Validation: `:ChromatiCraft:compileJava :ChromatiCraft:runClientData` succeeds.
## Stand synchronization, cave outline parity, and natural pylon worldgen — 2026-07-28

The item-stand renderer now has its permanent source name, `RenderItemStand`; the temporary
`RenderItemStandPort` suffix and every reference to it are gone. The reason inserted stand items did
not appear was not the BER: DragonAPI's full block-entity sync path invoked the legacy
`saveAdditional(CompoundTag)` overload statically, bypassing subclasses that correctly override the
26.2 `saveAdditional(ValueOutput)` method. `BlockEntityBase.syncAllData(true)` and update packets now
start from `saveWithoutMetadata(registryAccess)`, so item-stand inventory and every other modern
subclass payload reach tracking clients. This preserves the useful renderer conversion and fixes the
foundational persistence seam rather than adding renderer-side guesses.

Cave-crystal texture parity is corrected. V33a uses `crystal/crystal_outline` for all sixteen legacy
metadata values and supplies colour through runtime tint. In 26.2 those legacy values are represented
by sixteen concrete block/item registry identities; `ChromaBlockColors` reads the fixed element from
the registered block, and every custom blockstate references the single outline texture. The prior
per-colour crystal texture map and the interim shared block plus `color` state property were both
porting errors.

Natural pylons are now an active registered worldgen feature. `PylonFeature` preserves the V33a
seeded 256×256 shuffled chunk grid (four-chunk deviation and ten-chunk mean separation), twenty-four
surface probes, superflat config gate, overworld restriction, exact three-wide cross clearance,
liquid/tree/terrain replacement rules, random sixteen-colour initialization, optional 3–6-block
broken-pylon damage, generation event, and four-layer adaptive foundation. Canonical geometry is
placed exclusively from generated `data/chromaticraft/structure/multiblock/pylon.nbt`; Java owns
placement policy, terrain adaptation, active colour substitution, and breakage only.

Server datagen now emits the pylon configured feature, placed feature, and overworld
`surface_structures` NeoForge biome modifier. The pylon tile also restores its client flare cloud,
energy-density scaling, enhanced orbiting motes, unstable/enhanced electrical accents, and registered
`POWER` client loop that starts silently as soon as a loaded assembled pylon ticks, then uses vanilla positional attenuation and the enhanced 1.125 pitch. Existing discharge,
power-down, overload, and attack sounds remain on registered vanilla `SoundEvent` playback.

A focused `pylon_worldgen_nbt_contract` GameTest creates valid terrain, invokes the real placement
seam, verifies the registered pylon block entity, immediate NBT multiblock validation, rune sockets,
and adaptive foundation. Forced compilation, client datagen, server datagen, and all **58/58
required GameTests** pass.

Next pylon/worldgen audiovisual slice: port the dedicated pylon BER/shader presentation and exact
custom flare/lightning particle classes, then restore cache-backed finder overlays and the remaining
biome-specific generator gates. Continue converting data towers, dungeons, and monuments as NBT
structure templates; do not reintroduce Java coordinate builders as canonical geometry.
### 2026-07-29 — pylon spawn-generation deadlock

A real new-world launch exposed a freeze at `Preparing spawn area: 16%`. The shutdown watchdog
showed `PylonFeature -> PylonStructure -> NBTStructureLoader -> FilledBlockArray.setBlock` reading
through the backing `ServerLevel`; that synchronous read requested another chunk while a
`WorldGenRegion` decoration task was already active and waited forever on its chunk future.

Natural pylon placement now has a dedicated NBT-backed `WorldGenLevel` path. It parses the same
canonical `multiblock/pylon.nbt`, applies rune colour, places only through the bounded generation
region, preserves the exact eligible broken-pylon variants, and initializes the pylon without
running a Level-backed matcher during decoration. The normal matcher is rebuilt on the first server
tick, once chunk generation has completed. Runtime multiblock validation, handbook/display arrays,
and all other consumers retain the standard DragonAPI `FilledBlockArray` compatibility view.

Validation: forced :ChromatiCraft:compileJava succeeds, the headless test server reaches 100% spawn
preparation, and all **58/58 required GameTests** pass, including the NBT pylon worldgen contract.
### 2026-07-29 — pylon placement policy and recipe-finalization cleanup

The V33a shuffled pylon grid is now a registered `chromaticraft:pylon_grid` placement modifier rather
than an early return inside `PylonFeature`. Natural generation still applies the overworld,
superflat-config, and seeded 256-chunk-grid policy through the placed feature, while the configured
feature is directly testable with `/place feature chromaticraft:pylon`. Server datagen emits the
modifier in `worldgen/placed_feature/pylon.json`. The focused NBT pylon GameTest now invokes the
configured feature itself, then verifies its block entity, complete multiblock match, rune sockets,
and adaptive foundation.

Casting recipes now report `isSpecial()`: they belong to the casting table and not vanilla's recipe
book placement system. This prevents Minecraft 26.2 from treating their intentional
`PlacementInfo.NOT_PLACEABLE` result as an empty-ingredient error and ignoring them during recipe
finalization.

Validation: ChromatiCraft, RotaryCraft, and ReactorCraft compile; ChromatiCraft server datagen
succeeds; recipe finalization emits none of the reported empty-ingredient warnings; and all **58/58
required GameTests** pass with the configured-feature pylon contract active.

## Casting-table V33a GUI correction — 2026-07-29

The diagnostic casting-table dashboard has been removed and is not accepted port behavior. The real
V33a GUI is now rendered on the 26.2 extraction pipeline using the original `table2`, `table4`,
`table5`, and `bartex` artwork. It preserves the deliberately asymmetric 176-pixel base plus
43-pixel right extension, the 209/240 tier-dependent heights, source title/inventory-label positions,
tier-dependent matrix position, protected result slot, four tinted tier diamonds and structure
blockers, exact external-stand item coordinates, output preview and tooltip, translucent recipe
blocker, and all sixteen source-positioned lumen fill bars.

Recipe availability is evaluated server-side against the current recipe and player progression, then
synchronized as menu data for the blocker overlay; the client does not infer server gameplay state. The old research-fragment question-mark branch cannot yet carry recipe-specific
fragment identity because the modern data-driven casting recipe schema does not yet encode the
handbook/research-fragment relation or recipe-specific tuning overrides. Those behaviors remain attached
to the research/handbook recipe-metadata port rather than being faked in this screen. `compileJava` and resource processing are green.
## Cave-crystal translucency and mesh parity correction — 2026-07-29

The first cave-crystal model was not source-faithful: it approximated each lateral crystal as a
generic square beam plus pyramid and allowed the opaque `crystal_outline` sprite to select the solid
chunk layer. V33a instead submitted alpha-220 vertices in render pass 1 and used explicit, asymmetric
X/Z faces, including different roots when another crystal is directly below.

`CaveCrystalModel` now forces its material onto the 26.2 translucent chunk layer, retains the original
alpha 220 tint, uses full light emission, and bakes the exact V33a central and lateral face coordinates
and UV trimming. Its variant key now includes ceiling flip, a crystal above, a crystal below, and the
source-seeded four-arm mask. This restores connected vertical columns and closes the side-crystal tip
and shoulder gaps without touching cave-crystal placement or world generation. ChromatiCraft Java
compilation and resource processing are green after the correction.


## V33a pylon audiovisual parity and casting-menu inventory correction — 2026-07-29

The pylon audiovisual work has been re-audited line by line against V33a rather than against the
first modern approximation. The recurring `POWER` call already retained the source cadence and
volume/pitch arguments, but its `ambient.ogg` asset was stereo. Minecraft does not spatialize stereo
sounds, which made the pylon jump to an apparently fixed loudness instead of attenuating with
distance. That one asset is now mono at its original 44.1 kHz sample rate, frame count, duration,
and peak level; the registered event retains its 27-block attenuation distance.

The invented vanilla dust, end-rod, and electric-spark approximation is gone. `ChromaParticle`
implements the V33a flare cloud, attack-density scaling, enhanced floating-seed polar wandering and
icon/color transitions, unstable/enhanced ball lightning, blur flashes, and sparkle burst on a
full-bright additive 26.2 particle pipeline. `RenderCrystalPylon` now submits the source renderer's
camera-facing round flare, unstable sun flare, conducting/target scale changes, and enhanced rotating
turbo halo through the modern render-state pipeline. The world pylon model is particle-only, matching
the legacy invisible pylon block instead of drawing the temporary textured cube; the inventory item
retains a visible cube model.

Natural frequency was verified against both `PylonGenerator` and DragonAPI's original
`ShuffledGrid`: size 256, deviation 4, separation 10, two seed-advancing booleans, and exactly 625
candidate chunks per 256×256-chunk period. That is approximately one candidate per 105 chunks before
the source-faithful twenty-four terrain probes. The grid was not made artificially denser. The
dedicated renderer and large off-screen flare restore the intended long-range visibility that the
temporary cube presentation had removed.

The missing casting-table matrix was a DragonAPI/container seam, not missing GUI artwork:
`CoreContainer.addSlot` only accepts item-handler-backed block entities, while the casting table is a
vanilla `Container`. `MenuCastingTable` now directly registers inventory indices 0–8 as vanilla
slots, followed by result slot 9 and the 36 player slots, so all nine source-positioned matrix slots
are interactive over the original GUI texture.

Validation: forced Java compilation and client datagen succeed. Focused headless GameTests prove all
625 pylon grid candidates and the complete 46-slot casting menu, including table-backed matrix
indices 0–8. The first focused density launch itself passed but its optional XML reporter was given
a nonexistent output directory; rerunning without that reporter avoids the post-success tooling
error.

Renderer parity follow-up: the local V33a release jar supplied the original `beam.png` and
`beam_trans.png` assets. They are now preserved under modern resource paths, and the pylon BER also
includes its inherited `CrystalTransmitterRender` behavior: source-width-limited, target-tapered,
six-sided, colour-matched, scrolling additive transfer beams. This removes the last known pylon BER
parity gap from this checkpoint; Java compilation remains green after the beam pass landed.

## Concrete crystal-colour registry identities — 2026-07-29

The accepted port no longer recreates legacy block metadata as a shared `color` block-state property.
Cave crystals, crystal lamps, super crystals, crystal runes, and encrusted crystals now each register
sixteen concrete blocks and sixteen matching block items, with stable IDs of the form
`<family>_<color>`. Each block carries its immutable `CrystalElement`; generated blockstates, item
models, language, and loot all target the concrete registry identity. The obsolete
`StatefulCrystalBlock`, `BlockItemCrystalRune`, and `BlockItemColoredCrystal` compatibility adapters
were removed from the accepted build slice.

All accepted consumers were migrated: cave worldgen selects a concrete block, rune structures use a
colour-agnostic `RuneBlockCheck` over the sixteen registered runes, pylon/casting checks use family
predicates, and encrusted recolouring replaces the block identity while preserving synchronized face
growth, special status, readiness, and pylon discovery state in the replacement block entity.
Hand-authored cave-crystal blockstates remain property-free and all use the authoritative outline
sprite.

The cave mesh was rechecked directly against V33a `CrystalRenderer`. The apparent side-tip rotation
was not a missing source rotation; it was a one-sided custom-quad seam. The 26.2 model now emits both
windings with inverse normals, preserves the exact asymmetric source coordinates/UVs, alpha 220 and
full light, and explicitly marks both the material and baked quads translucent.

Validation was intentionally scoped rather than rerunning the complete suite. Client and server
datagen and forced Java compilation succeed. The new registry-identity test proves 80 unique blocks,
80 unique items, exact registry paths, and no `color` property across the five families. Focused
GameTests also pass for cave worldgen, both encrusted-crystal contracts, the casting NBT structure,
repeater grouping, and crystal-star casting. A runtime visual check remains appropriate for the
translucent cave mesh because headless GameTests cannot inspect submitted pixels.
## Cave-crystal interaction shape and enclosed-lighting correction — 2026-07-29

The cave renderer's remaining base shimmer came from its central bottom cap being submitted twice
(opposite windings) exactly coplanar with the supporting block face. Cave crystals are only valid
with floor or ceiling support, so that permanently hidden cap is now omitted; ceiling inversion
therefore omits the corresponding top support cap as well.

`BlockCaveCrystal` no longer inherits the default full-cube shape. Renderer and common block code now
share the exact V33a position seed, discarded booleans, four-bit arm mask, and floor/ceiling decision.
The block selects a cached neighbor-aware voxel silhouette: a stepped central point (or continuous
shaft beneath another cave crystal), three-stage approximations of every rendered arm, vertical
mirroring for ceiling support, and the original raised arm roots above crystal columns. Minecraft
26.2 uses this `getShape` result for mining ray targeting and the hover outline; collision inherits
the same silhouette.

The darker enclosed-base shadow was the other consequence of the inherited cube. Cave crystals now
publish an empty occlusion/visual-light shape, full shade brightness, and skylight propagation while
retaining their nonempty physical/selection silhouette. The central mesh remains full-bright and
translucent. A single focused `cave_crystal_dynamic_shape_contract` GameTest verifies renderer-mask
agreement in all four directions, non-cube outline, matching collision geometry, empty occlusion,
full shade/skylight behavior, and ceiling-mount selection.

Audit rule for other Reika custom models: dynamic render geometry cannot be queried from client model
or BER classes on a dedicated server. Its deterministic geometry selector must live in common code,
with the renderer and block shape consuming the same seed/state data. Encrusted crystals already use
a block-entity-driven dynamic shape. The active casting item stand needs a fixed Techne-derived
silhouette; GeoStrata's dynamic icicle and ocean-spike models still expose full-cube shapes and need
their geometry recipes moved to common code in their own focused port slices. Pylon particle/flare
presentation does not define a physical body beyond its core block and is not a shape candidate.
## Exact diagonal cave-crystal target outline — 2026-07-29

The cached `VoxelShape` remains the authoritative server collision and mining ray target, but vanilla's
outline renderer can only draw its axis-aligned voxel edges. Cave crystals now register a NeoForge
26.2 `CustomBlockOutlineRenderer` through `ExtractBlockOutlineRenderStateEvent`. At extraction time it
collects the already-selected dynamic block-model part for the targeted position, reads the exact
`BakedQuad` vertex positions, removes duplicate/reversed and degenerate edges, and captures only an
immutable line list. At submission time it draws those real diagonal edges relative to the extracted
camera position and suppresses the vanilla stepped outline. Normal and high-contrast accessibility
outline modes retain vanilla colours and line widths.

This deliberately does not replace physical collision with arbitrary polygons. Minecraft 26.2 block
collision and block ray clipping still consume `VoxelShape`, whose final collision/clip operations
are unions of axis-aligned `AABB`s. The existing cached stepped silhouette is therefore retained for
server-safe collision and targeting; exact polygonal block collision would require invasive custom
entity movement/ray logic and would be incompatible with normal pathfinding and block collision
consumers. The client outline needs runtime visual verification because headless GameTests do not
extract or submit client render state. Scoped Java compilation is green; no unrelated GameTests were
rerun.
## Casting-stand exact outline, shared FX primitives, and Rainbow Forest foundation — 2026-07-29

The custom model outline path is no longer cave-crystal-only. `ChromaModelOutlineRenderer` retains the
selected cave-crystal baked-quad extraction and also recognizes the casting item stand. For the stand
it walks every polygon in the baked `RenderItemStand.createStandLayer()` Techne model, applies the
same translate/scale transform as the block-entity renderer, deduplicates reversed edges, submits the
exact diagonal wire silhouette in both vanilla outline contrast modes, and suppresses the vanilla
axis-aligned outline. The stand's physical collision remains its server-safe `VoxelShape`; arbitrary
polygon collision is not supported by Minecraft's block collision engine.

The pylon-only particle implementation was generalized to `ChromaParticle`. Its flare, floating-seed,
ball-lightning, blur, sparkle, and chroma-fluid primitives now reproduce the V33a lifetimes, gravity,
full-bright/blend selection, size curves, wandering angles, jitter, and burst behavior. Accepted
callers now use those source effects instead of vanilla dust/end-rod/enchant stand-ins:

- pylon attacks emit 8–31 no-gravity flares from random points in the source block at the original
  half-block-per-tick velocity and distance-derived V33a lifetime;
- pylon jar rejection emits the 16-explosion shell, 256 coloured wandering seeds, and 16 ball-lightning
  motes;
- the struck player receives a 26.2 GUI-layer colour wash with the original factor-2 trigger and
  0.975-per-frame decay;
- power-crystal destruction emits 24 gravity/drag chroma droplets plus 16 chroma seeds through a
  dedicated client payload; and
- booster backlash emits the original 24–55 coloured `node2` seeds at the pylon through its own
  payload.

The Rainbow Forest is now a real datapack-registry biome pair rather than a dormant 1.7.10 class.
`ChromaBiomes` bootstraps `rainbow_forest` and `rainbow_stream` through the existing server datagen
registry set. It preserves the source 0.7/0.8 climate (stream temperature reduced by 0.0625), cyan
water, `0x648cff` sky, cleared spawn categories, exact slime/wolf/sheep/pig/chicken/cow/horse/squid
weights and group sizes, and forest/river vegetation foundations. `ChromaBiomeTagProvider` generates
the ChromatiCraft density tag plus vanilla/common forest, river, and overworld classifications.
`ChromaRegion` registers a weight-2 TerraBlender overworld region, replacing forest and its river
channel in that region with the generated pair. This affects newly generated chunks/worlds.

This is intentionally recorded as the **biome foundation**, not completion of the V33a decorator.
The following source behavior remains dependency-ordered work and must not be silently substituted:

1. Ball Lightning must be ported and registered before its biome spawn entry (weight 5, group 1) can
   be emitted without making the biome datapack invalid.
2. The sixteen concrete dye-flower, sapling, log, leaf, and vine identities must land before the nine
   per-chunk colour-tree attempts, dye-flower density, fertile vines, and Voronoi colour selection can
   be restored. They must not be recreated as metadata/state-index containers.
3. Large/small Rainbow Tree generation, nearby chroma-mud conversion, underground sparkle ellipsoids,
   and free light placement need modern configured/placed features. Thaumcraft ethereal plants stay
   disabled as requested; no substitute plant is invented.
4. Rainbow Stream's increased flowers/reeds/clay/sand and disabled lava lakes need their own modern
   feature list/placement modifiers, and the source's position-varying grass/water tint needs a
   client colour hook rather than a single approximate fixed tint.
5. The legacy “only tiny slimes/never other hostiles” runtime enforcement needs a modern spawn or
   entity-join rule in addition to the biome's source spawn list.

Validation was scoped to the affected surfaces: forced Java compilation passed after the outline/FX
work, normal Java compilation passed after the biome wiring, and `:ChromatiCraft:runServerData`
completed successfully. The generated biome JSONs and generated classification tags were
inspected. No unrelated GameTests were rerun because these changes are client rendering or datapack
bootstrap behavior; the affected server code only adds observational client payloads.

### Rainbow Stream feature-order correction — 2026-07-29

Rainbow Stream now keeps every feature shared with vanilla River in vanilla River's relative order. In
particular, `seagrass_river` is appended after bushes, flowers, grass, mushrooms, and extra vegetation;
placing it immediately after `trees_water` created a `FeatureSorter` River/Rainbow Stream cycle during
world creation. Server datagen and both affected module compiles pass after the correction.

### Crystal outline opacity and animated FX atlas correction — 2026-07-29

Cave-crystal custom hover lines now use the owning concrete `BlockCaveCrystal`'s `CrystalElement` RGB
with alpha 255. Item-stand outlines retain the vanilla-style normal/high-contrast colours. The supplied
`Overworld settings missing` report was a transient incomplete-save read: the attempted read preceded the
creation of `world_gen_settings.dat`, the file was written twelve seconds later, and the same world then
ran normally; it was not caused by Rainbow Forest registry data and no save file was modified.

The pylon crash in the same log was actionable: V33a's `roundflare`, `sunflare`, and `turbo` PNGs are
animated vertical strips (up to 256x46080). `RenderCrystalPylon` had bound them as standalone textures,
exceeding the GPU texture-height limit. It now resolves their stitched block-atlas sprites on each submit,
binds the block atlas, and emits sprite UVs, preserving animation without uploading the raw strips.
### 2026-07-29 — pylon entry audio/depth and crystalline-stone emissive parity

The pylon ambience is no longer a server-fired one-shot with a phase-dependent wait of up to 72 ticks. An assembled client pylon now owns one tickable looping `POWER` instance, permits silent startup outside audible range, starts immediately when its block entity arrives, retains vanilla positional attenuation and the enhanced 1.125 pitch, and recreates itself after sound reloads. The periodic server pulse was removed so loops cannot overlap.

Both ChromatiCraft additive pipelines had carried the conventional `LESS_THAN_OR_EQUAL` depth test into Minecraft 26.2's reversed-depth renderer. They now use the vanilla `GREATER_THAN_OR_EQUAL` comparison, so the pylon core and particles render when unobstructed instead of appearing only through terrain. Attack flares now restore V33a's 0.5-block/tick normalized velocity and `distance * 2.8` lifetime instead of the interim 2-block/tick velocity plus unrelated 30–59-tick life; the trail no longer projects far below its target.

Rune models now use the existing per-colour animated `runes/real` composite sheets; the prior background-only sheet was the cause of transparent rune interiors. Crystalline stone now has an `axis` state, placement derives it from the clicked face, beams select the original directional top/bottom artwork, and resonance rings distribute their two face sets by axis rather than covering every face identically. V33a's animated bright passes are emitted as full-bright model elements for glow columns, energized beams, pylon focus, multichromic/aura stabilizers, crystalline energy stabilizers, and both resonance-ring faces. All 48 type/axis models resolve existing textures, all 16 rune composites retain animation metadata, forced ChromatiCraft compilation succeeds, and ChromatiCraft-only client datagen succeeds. No server GameTests were rerun because this slice changes client sound/rendering and model datagen rather than network/world gameplay.
## Concrete-colour identities, Rainbow Forest trees, and Luminous Cliffs foundation — 2026-07-30

The metadata-colour rule above is now enforced in the active worldgen slice. GeoStrata luminous
crystals are four concrete registry identities (`blue_luminous_crystal`, `orange_luminous_crystal`,
`green_luminous_crystal`, and `purple_luminous_crystal`) with the four V33a hue ranges owned by the
block instance. The obsolete metadata-placement item was removed. Ocean Spike retains its physical
collision but exposes an empty block-support shape, so seagrass and other support-seeking plants do
not generate on its tip. GeoStrata compilation is green.

Rainbow Forest now owns sixteen concrete dye-leaf and sixteen concrete dye-sapling identities plus
concrete rainbow leaves/sapling. Its configured trees use codec-backed weighted vanilla log
providers, per-colour foliage, the original animated/tinted assets, generated models/items/lang,
leaf tags, and natural placed features. The rainbow tree is a distinct large configured feature.
The biome keeps the V33a forest grass/foliage palette and exact source spawn list. Server and client
datagen are green. The older foundation note saying these identities still need to land is stale and
is superseded by this section. Dye logs/flowers/vines remain future concrete identities; they must
not be collapsed back into metadata or an integer blockstate.

Luminous Cliffs now has registered `luminous_cliffs` and `luminous_cliffs_shores` biomes in the
TerraBlender mountain/shore climate slots, generated biome tags, the V33a 0.75/0.85 dry climate,
pastel sky, teal water, bright grass/foliage palette, and exact vanilla hostile/ambient weights
(spider, zombie, skeleton, slime, enderman, bat; no invented Thaumcraft spawn). Four concrete cliff
material identities replace the former material metadata; only the original transparent semantic
bit remains a blockstate. Ethereal Luma has a complete source/flowing fluid type and itemless fluid
block, breathable contact, source-contact progression, emitted particles, original animated fluid
assets, and no invented bucket.

World generation is split into bounded data-driven features to avoid the earlier chunk-load freeze:

- one deterministic 16x16 terrain pass retains V33a shore (~62-72), middle plateau (~100-112), and
  floating upper plateau (~128-160) bands with seed-stable simplex noise;
- sparse tapered auxiliary islands generate above the terrain with occasional water and rare Luma
  crowns;
- one or two underground cave-floor Luma ellipsoids retain the original y=10-48 and radius ranges;
- glowing trees use random overworld logs, ordinary leaves with sparse animated light-emitting
  leaf pockets, and the original expected one-in-twenty tree-selection density.

The feature registry graph, biome bootstrap, tags, loot (including the itemless-fluid no-drop path),
models, language, and both datagen sides validate. Focused validation passed:
`:ChromatiCraft:compileJava`, `:ChromatiCraft:runServerData`, and
`:ChromatiCraft:runClientData`. No unrelated GameTests were rerun.

This is an operational biome/worldgen foundation, not a claim of complete Luminous Cliffs parity.
Next dependency-ordered work is: concrete Glow Daisy and ceiling-growing Glow Root behavior/features;
the full position/altitude noise colour resolvers and Luma 4x4 mosaic; surface blending/farmland
semantics; island rivers/lakes/ore/tree decoration parity; then registered Luma Burst and Glow Cloud
entities with their renderers, spawning, movement, persistence, and original particles. Thaumcraft
wisps remain intentionally disabled per project-owner direction; no replacement entity is invented.

### Luminous feature-order cycle and source flora — 2026-07-30

The reported `luminous_cliffs` / `rainbow_stream` `FeatureSorter` cycle was caused by a reversed pair
of shared vanilla vegetation features: Rainbow Stream emitted default flowers before forest grass,
while Luminous Cliffs emitted forest grass before default flowers. Luminous Cliffs now uses the same
`forest flowers -> default flowers -> forest grass -> extra vegetation` relative order. The focused
`chromaticraft:colored_block_registry_identity` GameTest subsequently bootstrapped a real server world
successfully, so the reported cycle is gone; the whole test suite was deliberately not rerun.

Glow Daisy and Glow Root are now concrete registry identities, not variants of the legacy
`DECOFLOWER` metadata block. The Luminous Cliffs vegetation pass scans each affected column once and
places daisies on the middle shelf's enclosed cliff-grass floor and roots below the upper shelf. Their
V33a behavior is retained: 10/12 and 6 light, crop-adjacent daisy boost, sky-light-limited 1/240 daisy
spread with density suppression, 1/36 downward root extension, the cave-span root-length cap, source
selection boxes, source crop-style four-plane geometry/insets, original flower textures, paired
floating-seed/blur particles, growth sounds/particles, shear-to-self behavior, and ordinary 1/2 or
1/4 glowstone-dust drops. Root support explicitly includes the concrete cliff material identities so
the generated ceiling chains do not immediately break.

One source dependency remains deliberately visible rather than silently deleted: on non-growth random
ticks Glow Root has a 1/4 chance to emit one of V33a's weighted Fertility Seed variants. Fertility Seed
is still pristine 1.7.10 and must first be ported as seven concrete item identities (never damage/meta
variants), including its dirt-to-grass progressive action and dropped-item crop-ticking lifecycle.
`BlockGlowRoot` carries a `CHROMA-PORT` forward-reference marker for that exact behavior.

Validation boundary: `:ChromatiCraft:compileJava`, `:ChromatiCraft:runClientData`, and
`:ChromatiCraft:runServerData` all passed after the initial flora implementation, model/lang/tag/loot
wiring, and feature-order correction. A final compile/server-datagen rerun after the narrower
sky-light, shearing, cliff-support, and growth-effect parity corrections could not execute because the
tool environment reached its usage ceiling. Those last edits therefore require the next focused
compile plus server datagen before they are marked accepted; the generated loot JSON currently
reflects the preceding non-shear output until that datagen rerun.

### Glow Cloud and Luma Burst entities — 2026-07-30

Both are now registered, ported 26.2 entities from pristine `entity/EntityGlowCloud.java` (753 lines)
and `entity/EntityLumaBurst.java`, with new `render/entity/RenderGlowCloud.java` and
`render/entity/RenderLumaBurst.java`. `ChromaEntityTypes` gained `GLOW_CLOUD` (`Mob`, `MobCategory
.CREATURE`, 50 HP via a new `EntityAttributeCreationEvent` listener) and `LUMA_BURST` (`ParticleEntity`,
`MobCategory.MISC`). `ChromaBiomes.createLuminousCliffs` gained the exact V33a spawn entry (weight 30,
group 1-1) on the CREATURE list — the closest existing `MobCategory` to V33a's custom `"glowcloud"`
creature type (cap 24, `isPeacefulCreature=true`); that per-world cap and
`getMaxSpawnedInChunk()==8` have no direct modern equivalent and are not reproduced.

Glow Cloud extends `Mob` directly (not `PathfinderMob`) with an empty `registerGoals()`; all V33a
motion (a `SphericalVector` recomputed every tick) is driven from a full `aiStep()` override that never
calls `super.aiStep()`, avoiding any fight with vanilla gravity/travel/goal-selector logic. Ambient and
attack/death particles reuse a new public `ChromaParticle.FadeGlow` primitive (V33a's `EntityCCBlurFX`:
a fullbright additive "fade" blur sharing `FloatingSeed`'s rapid-expand/sine size envelope, optionally
converging to a point via the already-ported `CollectingPositionController` for the death implosion) via
four new `ChromaParticle.spawnGlowCloud{Ambient,Attack,Death}`/`spawnLumaBurstTrail` helpers. The V33a
attack-particle broadcast (`ChromaPackets.CLOUDATTACK`) is now the vanilla `Level.broadcastEntityEvent`/
`Entity.handleEntityEvent` idiom instead of a bespoke payload; the death-particle broadcast
(`CLOUDDIE`) is now the entity's native `onClientRemoval()` hook. Both textures needed (`block/icons
/flare.png`, `block/icons/fade.png`) already existed in the port.

Several V33a subsystems referenced by these two entities are still pristine 1.7.10 and are left as
`// CHROMA-PORT:` forward references with the original logic preserved in comments rather than deleted:
the ChromatiCraft pocket dimension (`ExtraChromaIDs.DIMID`) and everything gated on it (Glow Cloud's
in-dimension invulnerability/homing/tighter spawn rule; `RenderGlowCloud`'s `ChromaShaders.DIMGLOWCLOUD`
post-process glow, whose `reika.dragonapi.io.shaders.*` hook was replaced wholesale by the modern
`extras/shader` package and never re-created); the ambient RF/IC2 tile-charging and Crystal Tank top-off
(`TileEntityCrystalTank`/`CrystalTankAuxTile`, `ChromaBlocks.TANK`); the dynamic ethereal light block
(`ChromaBlocks.LIGHT` / `BlockEtherealLight`); the pylon self-damage after a Glow Cloud attack and the
`PylonDamage`-source invulnerability branch (`auxiliary/PylonDamage.java`); the ranged-pickup drop
redirection (`Chromabilities.RANGEDBOOST` / `ItemInventoryLinker`); the Energy Powder drop (chains to
`ChromaItems`, owned by the parallel naming-audit wave this cycle); the `CrystalMusicManager` attack-pitch
scaling (its `reikamusichelper.*` imports predate the modern `ReikaMusicHelper` nested-enum layout); and
Luma Burst's Gravity Tile pulse-absorption (`ChromaBlocks.GRAVITY`, `GravityTiles` — the pristine enum
no longer exists in-tree at all, only `BlockGravityTile`). Luma Burst has no spawn entry: its only V33a
spawner is that same still-pristine gravity-puzzle structure.

`:ChromatiCraft:compileJava` passes. This adds a Luminous Cliffs biome spawn entry and therefore needs
a server-datagen rerun (owner-run, per process rules) before the spawn is live in a built datapack;
in-client verification (particle look, billboard orientation, actual natural spawning) is still needed.
## Early progression: game start → casting stands — 2026-07-31

The opening arc is now walkable end to end. Three things were missing, each of which alone made it
unreachable:

1. **`ExplorationMonitor` was never ported.** It is V33a's discovery mechanism — a per-player tick
   scan that grants stages purely for *looking at* the right block — and it is the only grant site
   for `ProgressStage.CRYSTALS`, the root of the progression DAG and a prerequisite of `CASTING`.
   Without it CRYSTALS was reachable only from the game tests. 1.7.10 registered it as a DragonAPI
   `TickHandler` on `TickType.PLAYER`; the 26.2 equivalent is a server-side `PlayerTickEvent.Pre`
   listener. Ported together with the `ProgressionTrigger` interface; `BlockCaveCrystal` implements
   it as in V33a. The same scan also now drives pylon colour discovery, `BEDROCK`, `FINDSPAWNER`,
   `DEEPCAVE`, `NETHERROOF`, `RAINBOWFOREST` and `GLOWCLIFFS`.
2. **Cave crystals dropped themselves.** V33a never drops the block — it always yields shards of its
   own colour, count `1 + rand(6+f) + (1+f)*rand(3) + rand(1+f)`. Three rolls whose ranges each scale
   differently with Fortune, so not a composition of vanilla number providers; registered as
   `chromaticraft:crystal_shard_count` so the loot table stays data-driven.
3. **The Casting Table had no crafting recipe** — the mod had no ordinary recipe provider at all —
   and the Item Stand had none either. Both now emitted verbatim from V33a. Note the Item Stand is
   *not* a grid recipe upstream: it is itself a base-tier casting recipe, which is what gates tier-2
   casting behind owning a table.

Validation: `:ChromatiCraft:compileJava`, `:ChromatiCraft:runServerData` and
`:ChromatiCraft:runClientData` pass, and `:ChromatiCraft:runGameTest` reports **all 62 required tests
passed** against a real server world.

Still unported inside this same window, and deliberately marked `CHROMA-PORT` rather than guessed:

- **the guide book** (`ChromaItems.HELP` / `ItemChromaBook`) — V33a's main in-game guidance, and the
  thing a new player is expected to craft first;
- **the remaining Manipulator dispatch** beyond the active casting trigger, `SneakPop`, and generic
  `ManipulatorInteraction` hook — each branch stays dependency-gated until its target tile lands;
- both of their grid recipes, which are recorded in `ChromaRecipeProvider` as comments so they can be
  restored verbatim the moment the items land;
- Mystcraft (`MYST`) and Thaumcraft (`NODE`, Thaumometer pylon scan) branches of the exploration scan.

Not yet runtime-verified in a client: that looking at a cave crystal visibly grants the stage, the
in-world shard drop counts, and the casting table GUI/craft flow. The GameTest suite covers the
progression core and casting logic headlessly, not the client interaction.

### The tier-2 casting dependency chain — 2026-07-31

Walking the V33a chain backwards from "casting stands work" found four more independent breaks. Any
one of them alone left tier-2 permanently locked, so they only surface by tracing the whole chain:

```
look at cave crystal ──> CRYSTALS ──> craft Casting Table ──> CASTING
                                            │
look at conducting pylon ──> PYLON ──> (discover all 16 colours) ──> ALLCOLORS
                                            │
                            crystal rune recipe (bare-table tier, gated on ALLCOLORS)
                                            │
              place rune touching crystalline stone within 6 blocks of a table
                                            │
                                        RUNEUSE ──> TEMPLE tier ──> stand casting
```

What was missing, and where V33a puts it:

| Break | V33a source |
|---|---|
| `PYLON` ungrantable — port registered the pylon as the shared generic `BlockChromaticTile` | `BlockCrystalPylon implements ProgressionTrigger`, requires `canConduct()` |
| crystal runes had no recipe at all | `RuneRecipe` / `EnhancedRuneRecipe` — shard ringed by eight crystalline stone |
| `RUNEUSE` ungrantable | `TileEntityCastingTable.onAddRune`, fired from `BlockCrystalRune.onBlockPlacedBy` |
| `MULTIBLOCK` ungrantable | granted to the table's placer when CASTING2 validates |

**Per-recipe progression was not modelled.** V33a declares it per recipe via
`CastingRecipe.getRequiredProgress`; the port derived it only from the recipe tier. That is not
equivalent — `RuneRecipe` adds `ALLCOLORS` while still casting at the bare-table tier, so emitting
runes without it would hand players runes immediately and skip the entire colour-discovery arc.
`CastingTableRecipe` now carries an optional `required_progress` list (codec, stream codec, and
enforcement in `playerCanRun`), defaulting to empty so no existing recipe changed.

Note the chain has no deadlock: `onAddRune` needs the *structure* (`hasTemple`), not the RUNEUSE
stage, so building the temple and placing a rune is what unlocks casting with it.

`ProgressionCatchupHandling` is deliberately **not** ported. It looks like a grant path but is gated
on `ProgressionLinking.hasLinkedPlayers` — it is the co-op catch-up mechanism for linked players, not
the primary route, and it depends on the unported linking subsystem.

### Casting operation HUD and pylon recovery/feature variants — 2026-08-01

- `OperationInterval` is accepted into the compile slice again. `TileEntityCastingTable` implements
  the full modern contract, persists/synchronizes the active batch duration, exposes a clamped
  progress fraction, and distinguishes invalid, aura-pending, and running recipe states for the
  Elemental Manipulator overlay path.
- A damaged pylon now rebuilds its NBT matcher every ten server ticks while inactive. Replacing the
  missing block reactivates energy generation and network conductance without a chunk reload; the
  focused `pylon_structure_lifecycle` GameTest covers break, shutdown, repair, and restart.
- Ordinary pylon worldgen substitutes smooth crystalline foundation blocks for the aura stabilizer
  and resonance ring recorded in the canonical NBT. Those are player upgrades and no longer appear
  naturally. The matcher accepts either smooth or upgraded cells.
- Two command-only configured/placed features are generated: `chromaticraft:turbocharged_pylon` and
  `chromaticraft:power_crystal_boosted_pylon`. Neither is attached to a biome modifier. The first
  retains upgrade geometry and initializes enhancement; the second creates eight persistent,
  mutually owned functional booster crystals.
- Restored pylon client effects include power-crystal recharge streams, power-crystal socket hints,
  structure-loss bursts, and the original threefold enhanced-seed angular freedom/speed.

Verification: `:ChromatiCraft:compileJava`, server datagen, and the focused
`pylon_structure_lifecycle`, `pylon_feature_variants`, and `pylon_worldgen_nbt_contract` GameTests
pass. No unrelated GameTests were rerun.
### Pylon client presentation, Glow Cloud travel, and Manipulator repeater dispatch — 2026-08-01

The pylon BER and all active custom additive particles were using vanilla BlendFunction.ADDITIVE
(ONE, ONE). V33a's renderer explicitly uses DragonAPI BlendMode.ADDITIVEDARK, whose source factors
are ONE, ONE_MINUS_SRC_COLOR. The custom sprite and particle pipelines now reproduce those exact
factors while retaining Minecraft 26.2's correct reversed-depth GREATER_THAN_OR_EQUAL test. This
restores colour contrast against bright sky, cloud, and water backgrounds without making the effect
visible through solid terrain. Because the same pipeline backs the restored pylon flare, seed,
lightning, socket-hint, booster-stream, and invalidation families, the correction applies to the
missing/washed-out pylon particles as well as the central BER flare.

Pylon repair initially synchronizes hasMultiblock=true while its energy is still zero. The server
previously never sent another block-entity update as it recharged, leaving the client renderer at the
empty grey state even after the server crossed the conduction threshold. Charging now synchronizes
that transition immediately and sends low-rate (20-tick) energy updates while filling, preserving the
source energy-dependent colour ramp without one packet per tick. Pylon ambience keeps one client
instance alive per assembled pylon, starts silently, and applies the source 27-block linear falloff
explicitly each client tick; approaching an already loaded pylon therefore fades in immediately.

The earlier Glow Cloud ledger statement that the entity intentionally bypasses super.aiStep() was
incorrect and is superseded here. V33a computes its SphericalVector motion in onUpdate() and then
calls vanilla super.onLivingUpdate(), which consumes that motion. The modern entity now mirrors that
ordering: compute velocity first, then delegate once to 26.2 LivingEntity.aiStep() for travel and
tracking. The focused glow_cloud_spherical_movement GameTest verifies real server displacement.

The next beta-critical Elemental Manipulator dispatch is restored for the active crystal repeater
family. Ordinary use refreshes the source 100-tick connection-display state, checks live network
connectivity, plays CAST/ERROR feedback, and emits the original coloured and signal-depth diagnostic
particles. A typed `repeater_connections` client payload replaces V33a's ordinal packet for the
connection-overlay trigger. SneakPop remains earlier in dispatch exactly as V33a orders it: owners pop
their droppable repeater; a denied non-owner pop falls through without changing its orientation. The
focused manipulator_repeater_dispatch GameTest locks that precedence and ownership behavior.

Verification for this slice: :ChromatiCraft:compileJava passes, and the only newly relevant focused
GameTests pass independently: glow_cloud_spherical_movement and manipulator_repeater_dispatch
(1/1 each). Rendering, audible fade, and the client connection overlay still require an in-client
visual/audio check because the headless GameTest server cannot exercise GPU or OpenAL output.

### Pylon/repeater visibility, Focus Crystal parity, and casting engravings — 2026-08-01

The pylon's additive geometry is now submitted after terrain into Minecraft's item/entity transparency
target. Its beams also originate at the block centre instead of the north-west-bottom corner. This is
the 26.2 render-order correction needed to keep the V33a flare and beam colour legible against
translucent terrain such as water while retaining ordinary depth occlusion. The original pylon
particle families and emission cadence were audited at the same time; their shared apparent absence
was the stale client `hasMultiblock`/energy state described above, not omitted spawn branches.

`RenderCrystalRepeater` is now a complete submit-pipeline BER under its canonical name. It restores
outgoing crystal beams, the sparkle core, rain/table-group/cluster flares, Manipulator dashed LOS and
range displays, caster tuning icons, and turbocharged layered animation. The four V33a-only effect
assets (`repeater_range`, tuning icons, turbo sections, and turbo radiate) were recovered from repository
history rather than replaced. These effects use the same after-terrain additive ordering as pylons.

The temporary `TileEntityFocusCrystalPort` fork has been removed and all registrations/references now
use the canonical `TileEntityFocusCrystal`. The canonical class again carries all five V33a tiers,
efficiency values and stack persistence, target-class/relative-position connections, target
invalidation and recounting, break notification, aggregate acceleration helpers, turbo colour cycling,
and max-tier focus flares. `FocusAcceleratable` now uses `BlockPos`, and the Casting Table consumes the
shared focus calculation instead of maintaining a parallel approximation.

`RenderCastingTable` is also a real 26.2 BER now. As in V33a, it does not invent a replacement table
model: every 50 ticks it selects an eligible smooth/beam/column block from the table tier's actual
NBT-backed casting structure, assigns a random element, and draws that element's engraved rune over
all six faces using the source full-bright four-pass alpha overlay. The table once again exposes its
tier-selected structure and a render bound large enough for the entire temple. Rune state remains
renderer-owned and weakly keyed, matching the old client-only `WorldLocation` cache without leaking
removed tables.

The current game-start-to-stand beta path is mechanically reachable: crystal sight/progression,
shard drops, table recipe, base casting, rune/temple unlock, nine input slots plus output, stand recipe,
Manipulator trigger, and Focus Crystal acceleration are active. The guide book remains a separate
research/handbook vertical and must be ported with its real data and GUI; adding a hollow substitute
item would not make the beta path faithful.

Verification: `:ChromatiCraft:compileJava` passes. The only newly relevant server test,
`chromaticraft:casting_table_focus_acceleration`, ran alone and passed 1/1; no unrelated GameTests were
rerun. Pylon/repeater/casting blend, animation, sound, and renderer reactivation still require an
in-client visual/audio check because a headless GameTest server cannot exercise GPU or OpenAL output.
The 26.2 `ItemTagsProvider` signature was also corrected while compiling this slice; it no longer
carries the removed block-tag lookup constructor argument.
### Early-game casting gate, shard tag parity, and Glow Cloud tick-order correction — 2026-08-01

A focused integrated `early_game_casting_stand_chain` GameTest now exercises the real opening loop
without directly granting progression or constructing an ad-hoc casting recipe: a fresh player scans
an in-world cave crystal, receives the CRYSTALS stage, breaks four supported blue crystals for their
real loot, resolves and crafts the registered ordinary Casting Table recipe, fills the table's exact
nine-slot V33a StandRecipe grid, triggers it through the table, and receives the Item Stand plus five
casting XP and the CASTING stage. It passes independently (1/1).

The boosted crystal-group reload failure exposed a metadata-porting regression in item tags. Each
per-colour plain shard tag contained its boosted counterpart, so boosted inputs matched both the
ordinary 20-tick and boosted 40-tick recipes and recipe-manager iteration order chose between them.
V33a used exact plain-versus-charged metadata ingredients. The per-colour tag families are now
disjoint; only the aggregate `crystal_shards` tag includes both families for old wildcard recipes
that accepted all 32 variants. Server datagen regenerated all sixteen plain colour tags plus the
aggregate, and the focused `casting_table_boosted_group_reload` test passes 1/1.

The preceding Glow Cloud ledger entry stated the opposite of the pristine source's actual update
order and is superseded here. V33a calls `super.onUpdate()` first and assigns `motionX/Y/Z` afterward,
so its newly computed spherical vector is consumed on the following tick. The 26.2 entity now calls
`super.aiStep()` first and then computes the next server velocity, preserving that explicit handoff.
The movement GameTest itself also had an intermittent scheduling bug: relative `(8,8,8)` lies outside
the generated 5x4x5 test structure and could cross the randomly selected origin's chunk boundary,
leaving the entity in an unticked neighbouring chunk. It now spawns at relative `(2,2,2)` inside the
forced structure and reports tick count/velocity on failure. After reconstructing the class-output
tree with a forced single-worker compile, the focused `glow_cloud_spherical_movement` test passes 1/1.
### Manipulator cliff reveal and DragonAPI progressive-breaker seam — 2026-08-02

The Elemental Manipulator again invokes V33a's Luminous Cliffs `BlockCliffStone.transparify` branch.
The modern implementation starts from an opaque stone-type cliff block, uses the original depth 30
progressive recursive traversal with diagonal spreading and no drops/ordinary neighbour updates, and
replaces each visited state with the same registered block identity carrying `transparent=true`.
Each conversion rechecks the original vertical light range (-1 through +4) and plays the source
placement sound. Dirt, grass, farmland, and already-transparent states are deliberately excluded.

This exposed a cross-module DragonAPI port seam: `ProgressiveRecursiveBreaker` was fully modernized
but never registered with `TickRegistry`, so queued operations in every Reika module remained inert.
DragonAPI common setup now registers the singleton alongside `PlayerChunkTracker`. The focused
`manipulator_cliff_transparify` GameTest places three connected opaque cliff stones, uses the real
Manipulator, waits for the progressive worker, and verifies all three retain their block identity and
become transparent. It passes 1/1; `:ChromatiCraft:compileJava` also passes. No unrelated tests ran.
### Lexicon persistence and casting-backed research tiers — 2026-08-02

The guide-book vertical now has a 26.2-safe persistence boundary in `LexiconData`. It retains V33a's
exact `pages`, `creative`, `blanks`, and `notes` keys in the stack custom-data component, preserves
unrelated custom fields, canonicalizes duplicate page identifiers without reordering them, clamps
blank-fragment counts at zero, and removes empty owned fields rather than replacing the whole tag.
The focused `lexicon_custom_data_roundtrip` GameTest passes 1/1. The actual Chromic Lexicon item is
intentionally not registered yet: right-clicking it must open the real ported navigation/recovery UI,
not an inert or invented substitute. The next guide slice is the research catalog and those screens.

A missing V33a casting/research seam was restored before building that catalog. Successful casts now
write the exact player-owned death-persistent `castingprog` booleans (`crafting`, `temple`,
`multiblock`, `pylon`) in addition to the table's local recipe history. This matters because research
tier eligibility belongs to the player and must survive replacing the table. `ResearchLevel` is now
a modern ten-tier enum with all source gates intact, and `ResearchProgress` stores the selected level
by name under `Chroma_Research/research_level`, including V33a's old numeric-ordinal migration. Tier
changes sync player data and reuse the existing GAINPROGRESS notification payload. The source-exact
research names are emitted by client datagen rather than read from the obsolete `.lang` file.

Verification: `:ChromatiCraft:compileJava` passes. Only the focused
`chromaticraft:early_game_casting_stand_chain` test was rerun after this change; it passes 1/1 and now
also proves that the first Item Stand cast records only the CRAFTING tier, opens the RUNECRAFT gate,
and round-trips named research-level persistence. No unrelated GameTests ran.

### Ported block-entity client ticking and pylon effect recovery — 2026-08-02

`BlockChromaticTile` had only invoked `BlockEntityBase.updateEntity()` on the client, omitting the
ported `updateEntity(Level, BlockPos)` compatibility hook that the server ticker already invokes.
That hook is where the active pylon performs all of its V33a client work: flare clouds, enhanced
floating seeds, ball lightning, power-crystal hints/recharge streams, and the continuously
attenuated ambient loop. The omission therefore made the whole effect cluster inert and also made a
successfully repaired pylon appear not to reactivate on the client, despite the repaired
`hasMultiblock` value being synchronized correctly. Client tickers now invoke both halves in the
same order as server tickers. This also restores the already-ported client hooks for Focus Crystals
and repeaters instead of adding pylon-specific parallel ticking.

The pylon renderer remains submitted in the post-terrain feature phase and tests against the
solid-scene depth. The later compositor audit below supersedes the initial item/entity-target
assumption: V33a's direct-framebuffer `ADDITIVEDARK` equation cannot be evaluated correctly inside
an initially transparent premultiplied-alpha target. The accepted pipeline therefore writes the
glow directly to the main colour/depth targets while preserving solid occlusion and supplying a
real depth for the later translucent-target sort.

Verification: `:ChromatiCraft:compileJava` passes. Only
`chromaticraft:pylon_structure_lifecycle` was rerun and passes 1/1, covering shutdown, structure
repair, and server restart. GPU rendering and OpenAL output still require the next in-client visual
and audio check; headless GameTests cannot assert submitted pixels or an audible stream.

### Casting-table V33a sound and structure-particle loop — 2026-08-02

The casting table now runs the source client effect loop while an active recipe is synchronized.
Temple casts emit the eight original accent-point laser streams; multiblock casts emit the rotating
six-globe ring; pylon casts select only matching aura-rune positions and send the original rune
sprites inward with their source cadence, speed, scale, and lifetime. The client countdown pauses
when synchronized aura is insufficient, matching the old pylon-recipe wait instead of finishing its
visual sequence early. Completion produces the original 128-spark burst.

The legacy `Textures/Particle/16x.png` and `64x.png` ping-pong animation sheets were restored as
lowercase 26.2 resources and are sampled directly by dedicated additive particle layers; the
64-frame UV traversal remains forward across both rows and then reverses. Long casts again play the
source crafting ambience (152-tick loop at multiblock/pylon tiers), every completed craft plays
`CRAFTDONE`, and crossing a table-XP tier boundary also plays `UPGRADE`.

Verification: `:ChromatiCraft:compileJava` and resource processing pass, and both restored particle
sheets are present in the processed resource tree. Only the focused
`chromaticraft:early_game_casting_stand_chain` GameTest was run; it passes 1/1 after the completion
path changes. Submitted particles and the two sound cues still need an in-client presentation check.

### Pylon/translucent-effect compositor depth and complete bootstrap interaction — 2026-08-02

The remaining pylon/cloud/water defect was in the 26.2 transparency-target contract, not the
billboard geometry. ChromatiCraft's additive sprite and particle pipelines tested against the copied
reverse-Z solid depth but disabled depth writes. Their colour reached the item/entity or particle
target, while that target retained depth zero at every effect pixel. The vanilla transparency
post-shader therefore sorted pylon cores, beams, and all additive ChromatiCraft particles as the
farthest layer and composited water, clouds, weather, and other translucent content over them
regardless of real position. Both additive pipelines now write fragment depth, matching vanilla's
`TRANSLUCENT_PARTICLE` pipeline. Solid blocks still occlude through the copied main depth, while the
post-chain can now order the pylon and particles correctly against translucent geometry. Compilation
passes; pixel-level confirmation remains an in-client check.

The start-to-stand audit also removed two hidden shortcuts. Casting Table and Elemental Manipulator
recipe advancements had been unlocked only by the blue shard item even though both source recipes
accept V33a's wildcard shard identity (all sixteen colours, plain or boosted). Their advancement
criteria now use `#chromaticraft:crystal_shards`, and server datagen emits that tag criterion for
both recipes. The integrated opening GameTest now resolves and assembles the real Manipulator recipe,
places and owns the crafted table, fills the nine-slot StandRecipe, and starts casting through
`ItemManipulator.useOn` with an actual block hit instead of calling the table controller directly.

Verification: `:ChromatiCraft:compileJava` and `:ChromatiCraft:runServerData` pass. Only the focused
`chromaticraft:early_game_casting_stand_chain` test ran after the audit and passes 1/1. Its coverage
now spans crystal discovery, in-world shard drops, both bootstrap crafting-grid recipes, Manipulator
dispatch, owned-table casting, the nine-slot stand recipe, output, XP, casting progression, and the
first research-tier gate.
### Lexicon dependency boundary and restored casting-stand shortcuts — 2026-08-02

The next player-facing audit traced the original Chromic Lexicon before registering it. V33a's guide
is not an isolated book screen: `ChromaResearch` contains roughly 320 catalog constants and its
navigation, recovery, notes, recipe, structure, machine, ability, and progression pages bind directly
to most of the remaining mod. The original localized XML descriptions and handbook artwork are
retained, and `LexiconData` remains the accepted persistence seam, but the item is deliberately not
registered against a reduced early-only catalog. Doing so would expose a plausible-looking substitute
that omits most source behavior. The truthful dependency order is to keep expanding the casting and
content registries, then port the complete catalog and screens over those stable identities.

That source audit found two reachable Casting Table interactions omitted by the modern block split.
Sneak-right-clicking empty Item Stands with an empty hand now selects each stand for V33a's spread-fill
mode; the next ordinary stand click distributes the held stack evenly across the selected stands and
leaves the remainder in hand. Sneak-right-clicking an owned Casting Table with an empty hand now uses
the original tier-III mass-empty action instead of opening the GUI: every stand in the auxiliary ring
drops its full stack, plays `ITEMSTAND`, clears and synchronizes. Lower-tier tables retain the source
no-op restriction, and non-owners remain rejected before either shortcut runs.

Verification: `:ChromatiCraft:compileJava` passes. Only
`chromaticraft:casting_stand_spread` was run; it passes 1/1 and now drives both behaviors through the
actual 26.2 `BlockState.useWithoutItem`/`useItemOn` routes rather than calling the tile helpers directly.
No unrelated GameTests ran.
### Direct-framebuffer ADDITIVEDARK and preloaded pylon ambience — 2026-08-02

The final compositor audit found that depth writes alone could not restore V33a colour parity while
the glow was rendered into an off-screen transparency target. The original `ADDITIVEDARK` pass is
screen blending (`GL_ONE`, `GL_ONE_MINUS_SRC_COLOR`) performed directly against the already-rendered
world colour. Minecraft 26.2 instead composites item/entity and particle targets as premultiplied
alpha (`world * (1 - layer.a) + layer.rgb`). Applying screen blend to an empty target and then that
second equation necessarily loses the original background-dependent colour, producing the reported
washed-out, barely coloured pylon and effects that appeared to sit behind sky, clouds, or water.

ChromatiCraft additive sprite render types now use the main framebuffer, and additive quad-particle
layers are submitted through the main-target phase rather than the dedicated particle target. Both
pipelines retain reverse-Z solid-depth testing and now write depth. This recreates the source blend
against the world colour, keeps opaque terrain in front, and gives the vanilla transparency
post-chain a main-layer depth with which to order later water, cloud, weather, and translucent
layers. This correction applies to the pylon core/rays as well as the restored pylon, casting, and
Glow Cloud particle families; it does not introduce an always-visible or no-depth pass.

The pylon repair route was re-audited end to end. Replacing a missing structure block makes the
server validation restore `hasMultiblock`, sends the full block-entity sync packet immediately, and
the renderer extracts the synchronized flag on every frame. The restored `BlockChromaticTile`
client ticker then resumes the original pylon update hook on the next client tick, restarting both
its particle families and its managed ambient sound. No additional cached-render invalidation is
required.

`ChromaSounds.POWER` is now preloaded by client datagen. The generated `ambient` sound declaration
retains its 27-block attenuation distance and now carries `preload: true`, avoiding the first-visit
decode delay that could leave a fast-arriving player beside a silent pylon. The modern sound
instance remains a continuously ticking, position-bound loop with explicit linear volume, so it
begins silent at range and increases smoothly rather than starting at full volume after the old
periodic replay interval.

Verification: `:ChromatiCraft:compileJava` and `:ChromatiCraft:runClientData` pass, and the generated
sound declaration contains the preload flag. No GameTest was rerun for this client-only correction;
headless tests cannot assert framebuffer pixels or OpenAL output. GPU/OpenAL confirmation remains an
in-client visual and listening check.
### Casting Manipulator start feedback parity — 2026-08-02

The live Casting Table trigger now restores V33a's audible Manipulator contract at the authoritative
controller boundary. A successful recipe start plays `CAST` before the stands lock, while every
server-side rejection (already crafting, wrong owner, no matching recipe, progression/tier failure,
or an invalid batch/output) plays `ERROR`. Keeping the cues in `triggerCrafting` means direct/API
triggers and `ItemManipulator.useOn` cannot silently diverge; the client-side item branch still only
acknowledges the interaction and does not duplicate either sound.

Verification: `:ChromatiCraft:compileJava` passes. Only
`chromaticraft:early_game_casting_stand_chain` was rerun; it passes 1/1 through the real Manipulator
block-use route. No unrelated GameTests ran.
### Casting structure and repeater-group client transition synchronization — 2026-08-02

The Casting Table's periodic validator had retained the server calculations for temple, multiblock,
pylon-casting, personal tuning, and repeater-group throughput, but unlike V33a it never synchronized
those derived transitions. A client could therefore retain stale GUI/particle/render state after a
structure was assembled, damaged, or repaired. Validation now compares the complete derived state
before and after its audit and sends full table data only when a value changes. This preserves the
source-visible transition while avoiding V33a's unconditional full-NBT packet every forty ticks.
The first accepted adjacent rune also sends the `hasRunes` transition immediately.

`TileEntityCrystalRepeater.markAsTableGrouped` is now edge-triggered. A real group-state change
invalidates affected crystal paths and synchronizes the repeater's state bit to clients, allowing the
repeater renderer to display the table-grouped state. Repeated Casting Table validation with an
unchanged ring no longer breaks the same network paths or emits redundant block-entity packets.

Verification: `:ChromatiCraft:compileJava` passes. Only the focused
`chromaticraft:casting_table_tuning_key` and `chromaticraft:casting_table_repeater_grouping` tests
were run after their respective changes; each passes 1/1. No unrelated GameTests ran.
### Elemental Manipulator fake-player boundary — 2026-08-02

V33a's universal Manipulator rejects fake and dummy players before any block-specific dispatch. The
26.2 item now restores that guard before cliff transparification, SneakPop removal, Casting Table
activation, repeater redirection, or API hooks. `TileEntityCastingTable.triggerCrafting` independently
applies the same check so automation cannot bypass it through a direct controller call while still
receiving the normal rejected-cast feedback.

A focused `chromaticraft:casting_manipulator_fake_player_guard` GameTest now builds a valid casting
recipe for a NeoForge fake player and proves that both the real item-use route and direct table route
leave the table idle. It passes 1/1. `:ChromatiCraft:compileJava` also passes; no unrelated GameTests
were run. The post-test Spark sampler cancellation is a shutdown-only profiler message after the
GameTest server has already reported success.
### V33a casting aura throughput and geometric batch timing — 2026-08-02

A source comparison corrected two controller values that the earlier port and its regression had
both encoded incorrectly. Casting Table aura throughput is based on the MULTIBLOCK level-up value
(2000 XP), not the PYLON unlock value (15000 XP). A table at the 15000-XP pylon threshold therefore
receives 600 lumens/tick before grouping and 1200 with all four matching repeater sides, rather than
100/200. The table also again overrides `allowsEfficiencyBoost()` to false, preventing generic
receiver efficiency scaling from reducing casting-recipe aura costs contrary to V33a.

Stacked casting duration is no longer linear. `CastingTableRecipe` now carries the source
`stacking_factor` through its JSON codec and network stream codec, with V33a's ordinary default of
0.75. The controller uses the original geometric sum, so four copies of a five-tick ordinary recipe
take `floor(5 * (1 + .75 + .75^2 + .75^3)) = 13` ticks, then commit all inputs, outputs, and
per-craft XP atomically. Alternate geometric factors remain recipe data. Pylon recipes use the
separate source-faithful single-cycle scheduler documented below rather than this geometric batch.

Verification: `:ChromatiCraft:compileJava` passes. Only the focused
`chromaticraft:casting_table_repeater_grouping`, `chromaticraft:casting_recipe_contract`, and
`chromaticraft:casting_table_atomic_craft` tests were run. Each passes 1/1; the atomic test now covers
a 32-batch commit producing a full 64-beam stack, 32 copies of recipe XP, and no partial pre-completion mutation. No unrelated GameTests
ran.
### Modern Container insertion seam and Casting Table adjacent output — 2026-08-02

V33a attempts to insert each completed casting output into the six adjacent inventories before
leaving it in slot 9. The shared DragonAPI `ReikaInventoryHelper.addToIInv(ItemStack, Container)`
path could not safely implement that on 26.2: it still recognized empty slots with `null` instead of
`ItemStack.EMPTY`, derived a per-slot item limit from the container's slot count, returned after the
first empty-slot write even if items remained, and could therefore reject, truncate, or falsely
report complete insertion. The helper now performs a whole-stack capacity preflight, fills modern
empty/compatible slots using the container and item stack limits, marks the inventory changed, and
returns success only when the copied remainder is empty.

Casting completion now runs the source six-direction adjacency pass after assembling its output. The
result slot is cleared only when one adjacent `Container` accepted the entire stack; otherwise the
output remains available in slot 9 exactly as before. The focused atomic-craft regression now queues
32 copies of the two-beam recipe, verifies the converged nineteen-tick V33a batch duration and atomic
input commit, and requires all 64 beams to arrive in a real adjacent chest as one intact stack. This
covers both the modern-empty-slot bug and the former 27-slot/27-item truncation case.

Verification: `:DragonAPI:compileJava` and `:ChromatiCraft:compileJava` pass. Only
`chromaticraft:casting_table_atomic_craft` was rerun for the insertion change and passes 1/1. No
unrelated GameTests ran.

### V33a non-stackable pylon casting scheduler — 2026-08-02

V33a's ordinary `CastingRecipe` is stackable by default, but `PylonCastingRecipe` overrides that
contract to false; subclasses opt back in only where the source explicitly says so. The modern
casting recipe codec and stream codec now carry a `stackable` flag. Existing constructor call sites
infer the source default from the tier, and server datagen emits `stackable: false` for the currently
registered `lumen_core` and three `high_core` pylon recipes. Missing data retains the ordinary
compatibility default of true.

The table still queues the full input-limited amount and buffers the full queued aura requirement,
but a non-stackable timer consumes, emits, awards XP for, and drains aura for exactly one craft. If
the source six-direction inventory pass moves that output away, the table keeps its auxiliary stands
locked and immediately starts the next full-duration cycle. If slot 9 remains occupied, the run ends
cleanly after that craft; no output is overwritten and no queued ingredients are lost. Remaining
queue, duration, recipe key, and owner continue to use the existing persisted controller state.
The focused automation setup also exposed a modern NBT compatibility error: `NBTStructureLoader.load`
was translating every template air cell into `FilledBlockArray.setEmpty`, making surrounding air a
required part of every multiblock. V33a arrays described required structure blocks; air in a modern
structure template is a placement-clearing instruction. Compatibility matching now ignores both air
and structure void, while `NBTStructureLoader.place` still writes explicit air during actual NBT
placement. This restores legal neighboring inventories without weakening any required solid block.

Verification: `:ChromatiCraft:compileJava` and `:ChromatiCraft:runServerData` pass. Only the focused
`chromaticraft:casting_table_lumen_core_multicolor` and
`chromaticraft:casting_table_structure_loss_cancel` GameTests were run. The first queues two real
Lumen Cores, requires one 400-tick cycle per item, checks the intermediate 60k aura remainder in each
of three colors, and requires both outputs to reach one adjacent chest before the queue unlocks. The
second confirms removing a required NBT solid still cancels atomically after air cells stop
participating in compatibility matching; its stale 16,000-XP expectation was corrected to the 2,000
XP actually supplied by that test setup.

### V33a casting rejection detail, tuning contract, and Power Crystal recipe — 2026-08-02

A source audit of `CastingRecipe.canRunRecipe` and the original Casting Table GUI restored the
information hidden behind the modern no-entry overlay. The table now builds the inherited required
progress set in V33a order (`CRYSTALS`, then `RUNEUSE`, `MULTIBLOCK`, and the PYLON/REPEATER pair as
the recipe tier rises, followed by recipe-specific stages). The menu transports that set as packed
16-bit words because 26.2 container data packets serialize signed shorts, and the GUI identifies the
missing source progression titles on hover. The headless
`chromaticraft:casting_table_progress_feedback` test was the only GameTest run for this change and
passes 1/1.

Casting recipes now also carry V33a's `requiresTuningKey` contract through the JSON codec and recipe
stream codec as `requires_tuning_key`. Server-authoritative Manipulator activation rejects such a
recipe unless the table matches its owner's personal twelve-rune key, and the GUI reports the
personal key separately from progression gates instead of presenting an unexplained disabled output.

The first real consumer is the fully data-driven V33a `IridescentCrystalRecipe`, registered as
`chromaticraft:power_crystal`: diamond center; six Iridescent Chunks, five obsidian, and two glowstone
on the exact auxiliary stands; 15,000 yellow, 25,000 black, and 10,000 purple aura; 1,600-tick base
duration; 500 casting XP; source stacking factor 0.97489; stackable; personal tuning key required.
Server datagen succeeds and emits the complete recipe JSON.

The focused tuning regression then exposed a separate-per-colour/NBT compatibility omission. V33a
`CastingL2Structure` explicitly accepted the rune block at every personal-key coordinate, including
the radius-six fan positions outside its generic -5..5 floor alternatives. The modern NBT-backed
`CastingStructure` had omitted that loop, so installing a legitimate key invalidated CASTING3.
Those twelve source alternatives are restored with `RuneBlockCheck`; NBT palette rune entries also
match every registered colour-specific rune block, preserving old block-plus-metadata wildcard
semantics under the mandated one-registry-id-per-colour model.

Verification boundary: `:ChromatiCraft:compileJava` passed before the final narrow structure matcher
correction, and `:ChromatiCraft:runServerData` passes. The focused tuning test correctly exposed the
structure failure twice; its final rerun after restoring the explicit V33a key-coordinate loop is
queued because the local Codex execution allowance rejected the next Gradle launch. Do not treat the
new Power Crystal start path as verified until only
`chromaticraft:casting_table_tuning_key` is rerun and passes. The long-lived hostile
`CastingTuningMismatchReaction` remains the next casting/manipulator source cluster; it must be ported
with its server damage/knockback lifecycle and client sound/particle synchronization, not reduced to
a cosmetic or inert substitute.
### Structure empty-cell semantics, exact rune colours, and a full-suite regression sweep — 2026-08-02

The queued `chromaticraft:casting_table_tuning_key` rerun passes 1/1, so the Power Crystal start path
and the restored `CastingL2Structure` personal-key rune alternatives are verified.

Running the **complete** suite for the first time in several slices then exposed that the previous
entry's two NBT-matcher changes were regressions, not fixes. 27 of 71 required tests were failing on
the committed tree:

1. **Template air is a V33a `setEmpty` requirement, not a placement instruction.** `PylonStructure`
   requires its entire three-wide cross clearance empty and `CastingL1Structure` requires the shell
   interior plus the cells around the table; the earlier "ignore air" change deleted both. Worse, the
   pylon's optional link/turbo upgrade cells are `addBlock` alternatives layered *onto* those empty
   checks — with the `EmptyCheck` gone they became mandatory blocks, so no pylon could ever validate.
   The loader now distinguishes the three meanings the original code had, encoded in the palette:
   `structure_void` is not part of the array, `minecraft:air` is `setEmpty(false, false)`, and
   `minecraft:cave_air` is V33a's soft `setEmpty(true, true)` (the casting shell interior; it places
   as ordinary air). The four cells beside a Casting Table keep V33a's fire alternative at every tier,
   not only tier 1.
2. **Rune cells are not universally colour-agnostic.** V33a wrote most rune cells as
   `setBlock(RUNE, colour.ordinal())` and only the casting temple used the bare block instance. Making
   every NBT rune match all sixteen identities also discarded the caller's colour substitution at
   *placement* time, so a placed pylon got the wrong-coloured ring and its power crystals — which read
   their socket colour from the rune below them — could no longer find their pylon. `load` now takes an
   explicit `exactRuneColour` flag (true everywhere except `CastingStructure`), and the colour-agnostic
   case carries a placement override so the placed structure stays a legal member of the set it matches.

Because the temple genuinely requires those cells empty, the only one of the Casting Table's six
output directions an automation inventory can occupy while the multiblock still validates is the cell
below it. The Lumen Core regression's chest moved accordingly; that is source behaviour, not a
limitation of the port.

The last failure was a real Glow Cloud defect plus a test artefact. `addAdditionalSaveData` wrote
V33a's `isdead` flag from `isRemoved()`, but in 26.2 that is also true for `UNLOADED_TO_CHUNK` while a
genuinely killed entity is never serialised at all (`RemovalReason.shouldSave`) — so the flag was set
on exactly the clouds being saved for a chunk unload, and each one discarded itself on reload. It now
writes `RemovalReason.shouldDestroy()`. Separately, vanilla `Mob.checkDespawn` discarded the test's
cloud before its first tick because other tests leave mock players in the level thousands of blocks
away; the test pins that one entity with `setPersistenceRequired()` rather than weakening the
gameplay despawn rule.

Verification: `:ChromatiCraft:runServerData` regenerates the three casting templates, and
`:ChromatiCraft:runGameTest` reports **all 71 required tests passed**. This is the first full-suite
green in this cluster; the preceding entries' focused-only runs are what allowed the two matcher
regressions to survive. Run the whole suite, not a selector, before recording a slice as accepted.

### Casting L2 tuning cells and a non-tautological structure assertion — 2026-08-02

A follow-up audit of the same class of defect found one more. V33a `CastingL2Structure` adds the
twelve personal-key coordinates with `addBlock`, so the eight outer ones are **smooth-or-rune** and
the four inner ones are dropped again by its own `remove` loop. The generated `casting_l2` template
had written all twelve as mandatory rune cells, which meant a correctly built CASTING2/CASTING3
temple did not validate until its owner had installed a tuning key. (This is also what the previous
slice was really chasing when a legitimate key "invalidated CASTING3": the cell demanded the black
placeholder specifically. Colour-agnostic matching hid the symptom instead of fixing the cause.)
The template no longer writes those cells, and `CastingStructure` supplies the alternative through
the same `addRuneAlternative` guard the floor conversion uses, so it lands only where the template
still has a block — reproducing V33a's add-then-remove exactly.

This was invisible to the suite because `placeCastingTable`/`placePylon` place an array and then
match that same array: a round trip can only detect place/match disagreement, never
template/V33a disagreement. The tuning test now asserts up front that a freshly built CASTING2
temple validates with **no** key installed, which is an assertion the round trip cannot make.
Structure tests added from here should include at least one such independent claim.

Shared-helper divergence, recorded deliberately: V33a's default `addToIInv(stack, inv)` routes to
`InventoryHandle.iInventory.addItem(..., requireWholeFit=true, doAdd=true) > 0`, which reports
success on a *partial* insert while the Casting Table then decrements the full pushed amount — so
upstream silently destroys the remainder when a neighbouring inventory is nearly full. The 26.2
helper is whole-stack: a destination that cannot take the entire stack is left untouched and the
output stays in slot 9 for the player. That keeps the non-stackable pylon repeat loop from losing
queued output. If per-direction partial distribution is wanted later it needs an
insert-what-fits/return-the-count path, not a revival of the upstream bug.

Verification: `:ChromatiCraft:runServerData` regenerates `casting_l2`/`casting_l3`, and
`:ChromatiCraft:runGameTest` again reports **all 71 required tests passed**.
`:RotaryCraft:runGameTest` passes 5/5 after the shared `ReikaInventoryHelper` change. ReactorCraft
has no GameTest run configuration, so its four `addToIInv` consumers (waste decayer, nuclear core,
pebble bed, breeder core — all self-inserts of a single item) are covered by compilation only.

### Start → casting stand: source audit of the opening arc — 2026-08-02

An audit of the whole beta path against V33a, rather than a feature slice. The progression backbone
checks out: all 84 live `addProgressPrereq` edges match (the three that appear missing are commented
out upstream), `ExplorationMonitor`'s scan and the colour-discovery/ALLCOLORS path are faithful, and
the Casting Table and Elemental Manipulator crafting-grid recipes are verbatim. Four defects were
found in what the path actually gates on.

**The Item Stand is not a base-tier recipe.** V33a `StandRecipe extends TempleCastingRecipe`: it is a
TEMPLE-tier, 20-tick recipe paying twice the temple experience (80), and it additionally requires two
purple and two black runes on the floor at (±2, 0, ∓3). The port had emitted it at CRAFTING tier for
five ticks and five XP with no runes, so a brand-new player could cast a stand immediately. That is
the single biggest shape error on the beta path: correctly, the arc is
*find crystals → craft the table → base-tier casts to CASTING and up to 250 table XP → find all
sixteen pylon colours for ALLCOLORS → cast runes → build the temple and place a rune for RUNEUSE →
only then cast the Item Stand.* The earlier ledger note calling the stand "a base-tier casting recipe,
which is what gates tier-2 casting behind owning a table" had the dependency backwards and is
superseded.

**`onAddRune` dropped half its gate.** V33a gates it on `isAtLeast(TEMPLE)` — the temple structure
*and* the 250-XP tier. The port checked only the structure, so RUNEUSE (and with it stand casting)
was available the moment a temple was built, skipping the XP climb entirely.

**The boosted rune recipe was the ordinary one with a different shard.** V33a `EnhancedRuneRecipe`
also extends `TempleCastingRecipe`: TEMPLE tier, 20 ticks, four times the temple experience (160),
**eight** runes per cast, and its own colour's rune required in the temple rune ring
(`TempleCastingRecipe.runeRing`, indexed by element ordinal one block below the table). The port had
it at CRAFTING tier, 10 XP, one output, no rune.

**Rune-ring offsets are two conventions, both relative to the table.** Group recipes use y=0 and the
temple rune ring uses y=-1; the table's scan collects dy ∈ [-1, 1] keyed off the table position, so
both encode directly as `RuneRequirement` offsets.

The opening GameTest was rewritten to walk the real arc: crystal sight, in-world shard drops, both
bootstrap grid recipes, a base-tier crystalline-stone cast for CASTING and the first table XP, a
proof that the Item Stand is **not** castable from that bare table, then the temple, RUNEUSE, the
StandRecipe rune ring, and the Manipulator-driven 20-tick cast for 80 XP. The temple-group test also
now proves a 249-XP temple refuses runes while the same temple at 250 accepts them.

Follow-up status: V33a's four `addChainedProgression` rules were deferred at this checkpoint, but are
now active in the Start-to-Proxima milestone documented below. `BlockCrystalRune`'s
`triggerAddCheck` call — which re-validates a *hand-built pylon* when a rune completes it — remains
covered by the pylon's own ten-tick rematch.

Verification: `:ChromatiCraft:runServerData` regenerates the stand and all 32 rune recipes, and
`:ChromatiCraft:runGameTest` reports **all 71 required tests passed**.

Still unaudited on this path: `canGiveDoubleOutput` (V33a lets rune and some other recipes randomly
double their output — no modern equivalent is implemented yet), the exact cave-crystal shard drop
counts, and the casting XP award to the player as opposed to the table.

#### ALLCOLORS is now load-bearing for the beta goal — 2026-08-02

Making the Item Stand a TEMPLE recipe puts `RUNEUSE ← ALLCOLORS ← PYLON` on the critical path, so
colour discovery had to be checked for reachability, not just fidelity. V33a has exactly two
non-command grant sites for `setPlayerDiscoveredColor`: the `ExplorationMonitor` sight of a charged
pylon, and `ChromaAux`'s player-charging path. The port has the first one live and faithful; the
second is still pristine 1.7.10 (`ChromaAux` is not allowlisted) and is a bonus route that requires
the unported charging subsystem anyway.

So ALLCOLORS is reachable and matches the source — but it genuinely means sighting sixteen
differently-coloured charged natural pylons, against a grid of roughly one candidate chunk in 105
before the twenty-four terrain probes. That length is V33a's design, not a port defect; do not
"fix" it by adding grant sites. The opening GameTest therefore grants ALLCOLORS directly and covers
the discovery mechanism separately, which is a deliberate split rather than an untested gap.

Next thing to bite after the stand: `MULTIBLOCK ← VILLAGECASTING` is a live upstream edge, so the
MULTIBLOCK grant to a table's placer silently no-ops until `VILLAGECASTING` has a grant site.

### Casting-system slice: pylon upgrade stone, stand FX, stand locking — 2026-08-02

Continuing along table → temple → runes → stands.

**The two pylon upgrade blocks were uncraftable.** V33a registers ten `CrystalStoneRecipe` shape
conversions; the port had eight. The missing pair is the Aura Stabilizer (4, `sSs`/`ScS`/`sSs` from
smooth stone, white runes and a charged white shard) and the Resonance Ring (6, `SSS`/`ccc`/`SSS`).
Since ordinary pylon worldgen substitutes plain foundation for both, these recipes were the only way
to obtain them. V33a's `CrystalStoneRecipe` also derives its progression from its own ingredients — a
charged shard implies SHARDCHARGE, a rune implies ALLCOLORS, `iridCrystal` implies INFUSE, and a
tiered resource implies its discovery tier — so the two new recipes carry exactly the gates that rule
produces. The other eight were re-checked against source and all their counts and patterns match.

**The Item Stand's particles were vanilla stand-ins.** `ParticleTypes.ENCHANT` and `END_ROD` are now
V33a's own effects: `spawnItemStandItem` reproduces the half-chance-per-tick `EntityCCBlurFX` rising
out of the held item with the source ±0.375/±0.125 spread, 45–75-tick life and small negative
gravity, and `spawnItemStandCrafting` reproduces the 1-in-32 white `EntityCenterBlurFX` on the legacy
64-frame sheet at the stand's base. Two source details the port had also lost: the crafting mote is
driven by the **linked table** being mid-craft, not by this stand holding anything, and V33a tints
the item mote from `ItemElementCalculator`. That calculator is still pristine 1.7.10, so the port
uses the source's own null-tag fallback colour rather than inventing a tint.

**A locked stand is unbreakable in V33a, not merely undroppable.** `BlockChromaTile` returned -1
player-relative hardness for any `ConditionalUnbreakability` tile; the modern `BlockChromaticTile`
only had the owner check, and the stand did not implement the interface at all, so a running cast
could have its ingredients mined out from under it. Both the destroy hook and `getDestroyProgress`
now honour it. Note `Level.destroyBlock` does not consult `onDestroyedByPlayer` in 26.2 — only the
player game-mode path does, which is what the regression drives.

`canGiveDoubleOutput` was investigated and is **not** a gap: V33a only consumes it under
`Chromabilities.DOUBLECRAFT`, a player ability far past this arc, so it is correctly dormant.

Verification: `:ChromatiCraft:runServerData` emits the two new stone recipes, and
`:ChromatiCraft:runGameTest` reports **all 71 required tests passed**, with the stand test extended
to cover locked-stand unbreakability. The two restored particle families still need an in-client
look, as headless tests cannot assert submitted particles.

### Casting experience: the repeat-craft penalty — 2026-08-02

The table tracked and persisted `craftedItems` but never used it. V33a's `getXPModifier` is the
casting system's anti-grind rule: once a table has completed a given recipe `getPenaltyThreshold()`
times, every further craft awards `getPenaltyMultiplier()^(completed - threshold)` of the recipe
experience. Without it a table farms one cheap recipe at full rate forever, which matters more now
that reaching temple tier is an explicit 250-XP climb.

`CastingTableRecipe` now carries `penalty_threshold` and `penalty_multiplier` through the JSON codec
and the recipe stream codec. The source derives the threshold as
`max(1, getTypicalCraftedAmount()*3/4)` and exempts its `CoreRecipe` marker classes entirely, so
exemption is the schema default and only V33a's non-core recipes declare a value: groups, clusters
and Crystal Star at 1, Crystal Mirror and Element Unit at 12, Crystal Focus at 24, Crystal Lens at
48, Power Crystal at 96, and Iridescent Chunk at 576. Everything on the beta chain — crystalline
stone, both rune recipes, the Item Stand, and every core — is `CoreRecipe` upstream and therefore
correctly unpenalised.

Two details the port had to match exactly. V33a evaluates the modifier once **per craft cycle**,
against the count completed before that cycle, truncating each cycle's award independently, so a
batch does not all pay at one rate. And `addCrafted(out, 1)` counts **cycles**, not output items —
the port had been adding the output stack size, which for an eight-per-cast recipe like crystalline
stone would have advanced the counter eight times too fast. The player's own experience award is
deliberately left unpenalised: `CastingRecipe.onCrafted` gives `getExperience()*amount/4` from the
raw recipe value, and only table XP passes through the modifier.

Also verified as already faithful and needing no change: the cave-crystal drop count
(`1 + rand(6+f) + (1+f)*rand(3) + rand(1+f)`, registered as its own loot number provider) and the
quarter-rate player experience award.

Verification: `:ChromatiCraft:runServerData` emits the thresholds, and `:ChromatiCraft:runGameTest`
reports **all 71 required tests passed**, with the recipe-contract test extended to cover the
exemption default and the decay curve.

Known flake, recorded rather than papered over: `chromaticraft:network_pylon_to_receiver` failed once
in a full-suite run with the pylon undrained, then passed alone and in the next full run. It is
timing-sensitive rather than broken; if it recurs, the transfer's tick budget is the thing to look at.

### Tiered ores: the progression-gated resource layer — 2026-08-02

> **Historical checkpoint.** This section records the original three-ore landing and is superseded
> by the 2026-08-23 survival-backbone section: all 15 tiered ores and all seven tiered plants are now
> concrete registrations with their source geometry, drops and worldgen represented.

V33a's tiered ores were entirely pristine: `BlockChromaTiered`, `BlockTieredOre`, `ItemTieredResource`,
`TieredWorldGenerator` and both ISBRH renderers were all outside the compile slice. The mechanic is
that an ore is **disguised as the stone it generated in** until the miner reaches its `ProgressStage`
— not merely unminable, genuinely indistinguishable from the surrounding terrain.

The server half is now accepted, faithful to DragonAPI's `BlockTieredResource`:

- The ore never drops itself; the loot table is empty and every drop is decided in code, as upstream
  hard-overrides the whole vanilla drop path and re-implements harvesting in `removedByPlayer`.
- A sufficient miner gets the ore's own per-ore resource formula plus V33a's 2-6 experience splash.
- An insufficient miner gets **the host block's own drops**, and with Silk Touch the host block
  itself, so mining reveals nothing.
- Placing an ore below its stage removes it again with the host block's break effects, closing the
  obvious way to defeat the disguise from creative.

`TieredOreModel` is the client half: a chunk-mesh `DynamicBlockStateModel` that draws the host
stone's texture unless the local player has the stage, and otherwise the source's two passes — the
`_underlay` sprite plus the animated `_overlay` fractionally proud of the face and full-bright. This
matches V33a, whose `TieredOreRenderer` asks `isPlayerSufficientTier(..., thePlayer)` per rendered
ore. Progression already reaches the client through the synchronised persistent player tag, so the
read is valid client-side.

Three of the fifteen ores are accepted, and the boundary is deliberate:

| Accepted | Stage | Host | Drop |
|---|---|---|---|
| Energized Rock (INFUSED) | CRYSTALS | stone | `min(16, 1+rand(5)*(1+rand(1+f)))` chromic dust |
| Elemental Stones (STONES) | RUNEUSE | stone | `min(4, 1+f/2)` from the per-player `StatisticalRandom` |
| Firestone (FIRESTONE) | LINK | netherrack | `1+rand(6)*(1+f/2)` fire essence |

Elemental Stones keeps V33a's `StatisticalRandom` roll persisted under `elementalstones` rather than
a uniform pick: it biases toward the colours that player has seen least, which is what stops a player
being permanently short one colour. Display names are the authoritative `chroma.tieredore.N` strings.

Not registered, and not stubbed: the six geode-rendered ores (BINDING, FOCAL, TELEPORT, FIRAXITE,
THERMITE, SPACERIFT) need V33a's bespoke geode mesh, and WATERY/LUMA/ECHO/THERMITE/RESO/RAINBOW/
AVOLITE drop tiered resources that have no registered identity yet. Showing either group as a plain
overlay ore would have been an invented appearance.

Verification: `:ChromatiCraft:compileJava`, both datagen sides, and `:ChromatiCraft:runGameTest` at
**72/72** with a new `tiered_ore_progression_gate` test that mines the same ore below and above its
stage and checks it yields cobblestone in the first case and 1-16 chromic dust in the second, plus
the place-back removal. Note the test needs `makeMockPlayer(GameType.SURVIVAL)`: GameTest's
`makeMockServerPlayerInLevel` is hardcoded CREATIVE, and the tiered harvest is a no-op in creative.

**Still to do on this system**, in order: `TieredWorldGenerator` as modern configured/placed features
(per-ore `genChance` one-in-N per chunk, `veinCount` attempts of a `veinSize` vein excluding cliff
stone, y = `rand(128)` for the nether ores and `rand(32)`-or-`rand(64)` otherwise) — until that lands
these ores exist but never generate; the client chunk re-render when a stage syncs, so a newly
granted stage reveals ores without a reload; then the geode mesh and the remaining drop identities.

### Tiered-ore worldgen, and two additive-render defects — 2026-08-02

**Worldgen.** The three accepted tiered ores now generate. V33a's `TieredWorldGenerator` runs
`if (rand.nextInt(genChance) == 0) for (k < veinCount)` per chunk, each attempt dropping a
`BlockExcludingOreVein` of `veinSize` that targets only the host block. That maps cleanly onto vanilla
`Feature.ORE` with a single `BlockMatchTest` target — targeting the host block is exactly what made
the source's explicit cliff-stone exclusion unnecessary — plus `RarityFilter` (omitted when the
chance is one), `CountPlacement`, and `InSquarePlacement`.

The height roll needed its own modifier. V33a picks
`ordinal >= FIRESTONE ? rand(128) : (nextBoolean() ? rand(32) : rand(64))`, and that overworld roll
is a 50/50 mix of two uniform bands — measurably denser toward bedrock than either alone, and not
expressible with vanilla's height providers. `chromaticraft:tiered_ore_height` reproduces it exactly,
offset by the level's minimum build height because the source's values assumed a y=0 world floor.
Biome modifiers add the two stone-hosted ores to `is_overworld` and the netherrack-hosted Firestone
to `is_nether` at `underground_ores`. A `tiered_ore_worldgen` GameTest places the real registered
configured feature into a stone volume and checks the vein lands, stays within its V33a size, and
leaves a non-host neighbour untouched.

**Additive particles punched holes in water.** Reported from a live client with a screenshot: pylon
particles were visible through water, with square gaps in the surface. Both additive pipelines still
carried `writeDepth = true`, which was correct while they rendered into the item/entity target but
became wrong when the ADDITIVEDARK correction moved them onto the **main** colour/depth targets.
Vanilla's `TRANSLUCENT_PARTICLE` does write depth, but into the separate particle target that the
post-chain composites — never into the scene depth. Writing there stamps the glow quads into the
main depth buffer, so the translucent terrain drawn afterwards fails its depth test against them.
Depth write is now off on both pipelines; the depth *test* stays, so solid terrain still occludes.

**No ChromatiCraft particle sprite was ever stitched into the block atlas.** `ChromaParticle.sprite`
looks up `chromaticraft:block/icons/<name>` on the blocks atlas, but the mod shipped no atlas
definition at all and no block model references those icons, so every additive particle was sampling
an unstitched sprite — which is what the reported "visual issues" (arbitrary textured squares) are.
This is the same trap already recorded for ReactorCraft: only the `minecraft:` namespace's atlas
definitions are loaded, which is why NeoForge itself ships `assets/minecraft/atlases/blocks.json`.
ChromatiCraft now ships one with a `minecraft:directory` source over `block/icons`, giving every icon
its `chromaticraft:block/icons/<name>` sprite id.

Verification: compilation, both datagen sides, and `:ChromatiCraft:runGameTest` at **73/73**. Both
render fixes are reasoned from the pipeline contract and cannot be asserted headlessly — they need
the next in-client look to confirm the water surface is intact and the particles show their real
sprites.

### Crystalline stone ingredients, and the cloud/water ordering tension — 2026-08-02

**The smooth crystalline stone recipe used the wrong stone.** V33a is
`new ShapedOreRecipe(block, " S ", "SCS", " S ", 'S', "stone", 'C', shard)`, and the 1.7.10 `"stone"`
oredict is `Blocks.stone` — the smooth stone family. The port had `#minecraft:stone_crafting_materials`,
which is the **cobblestone** family, so the recipe asked for entirely the wrong rock. It now uses
`#c:stones`. (This is why the opening GameTest had to be fed cobblestone to match; it is back on real
stone.)

The sixteen per-colour smooth recipes are also collapsed into one over a new
`chromaticraft:plain_crystal_shards` tag. The source writes this once per colour with an
exact-metadata shard, which is sixteen recipes differing only in which shard they consume and all
producing the same eight stone. One tag-driven recipe crafts identically, displays as one entry, and
still refuses boosted shards — the existing aggregate `crystal_shards` tag could not be used for this
because it deliberately contains the boosted family too.

**Ocean spike lower halves rendered near-black.** A spike is a *stack* of `BlockOceanSpike`, so
without an occlusion/light exemption the lower blocks are boxed in by their own neighbours: they
occlude each other, block skylight, and take the darkened shade factor, while only the exposed tip
stays lit. It now publishes the same empty occlusion/visual shape, skylight propagation and full
shade brightness that `BlockCaveCrystal` uses for exactly this reason. Physical collision is
untouched.

**Pylons against clouds — diagnosis, deliberately not yet changed.** `LevelRenderer.addCloudsPass`
renders clouds into their **own** target whenever fancy clouds are on, and only falls back to the
main target otherwise; that target is composited over main afterwards. The additive pylon glow lives
in the main target, so how it sorts against a cloud depends entirely on what depth it leaves behind
in main — which is precisely what the water fix above just turned off. The two reports therefore pull
in opposite directions: depth-write on sorts the glow correctly against the cloud/particle targets
but punches holes in the translucent terrain drawn afterwards into main; depth-write off fixes the
water and leaves clouds compositing over the glow.

The real resolution is likely to keep depth write and instead submit the additive glow **after**
translucent terrain rather than in the post-terrain feature phase, so the depth it writes can no
longer reject water. That is a bigger change to submission order and cannot be told apart from the
current state without a client, so it is deliberately left until the depth-write-off build has been
looked at in game. Do not stack another blind change on this subsystem first.

Verification: `:ChromatiCraft:compileJava`, `:GeoStrata:compileJava`, `:ChromatiCraft:runServerData`
and **73/73** GameTests.

### Encrusted crystal and power crystal renderers — 2026-08-02

Both were pristine. Both are static block renderers upstream, not TESRs, so both become chunk-mesh
`DynamicBlockStateModel`s rather than block entity renderers.

**Encrusted crystals** (`CrystalEncrustingRenderer`). Each of the six faces can carry an
independently growing crust of small distorted pegs. `EncrustedCrystalModel` reproduces the source
exactly: the seed is V33a's `ISBRH.calcSeed(x, y, z) = chunkXZ2Int(x, z) ^ y` plus its two discarded
`nextBoolean()` rolls, so a block grows the same crust it did in 1.7.10; per face it picks 4-8 grid
cells, distorts the grid at 0.66 deviation, and places `min(n*n/2, 6 + amt*amt/10)` pieces (×1.5 when
special) of height `(3+2*amt .. 8+4*amt)/96`; each piece mixes the element colour toward white and
black by two random factors and shifts hue by ±5, draws full-bright, then overdraws the `glowframe2`
frame and, when special, the special sprite at alpha 48.

The pieces are **not** axis-aligned boxes. V33a distorts each peg by displacing the four corners of
the growth face and the four of the opposite face, which is what stops a crust looking like a grid of
identical pegs, so `CrystalPiece` carries eight corners and transcribes DragonAPI `CubePoints
.applyOffset` per direction — the A/B letter pairs do not index the same axes on every face upstream
(DOWN/UP read x,z; WEST/EAST read z,y; NORTH/SOUTH read x,y), so that table is written out rather
than derived from an assumed symmetry. Growth is block-entity state, so the quads are built in
`collectParts` rather than pre-baked per variant; that is the same work the source did every frame,
now done only when a chunk section re-meshes.

**The power crystal** turned out to need almost no new geometry. `BlockRainbowCrystal.getRenderType`
returns `ChromaISBRH.crystal` — the *same* renderer the cave crystals use — with
`renderAllArms() = true`, `renderBase() = true`, `getBaseBlock(...) = crystalline stone`, and
`getTintColor = 0xffffff`. The existing `CaveCrystalModel` already modelled `renderBase`, so it gained
an `all_arms` flag and an optional `inert_texture`; V33a's
`getIcon(IBlockAccess, ...)` swaps to the inert sprite when the crystal has lost its pylon, which the
model now reads from `TileEntityChromaCrystal.isConnected()` at collect time. The power crystal's
blockstate is that model with every arm, the `crystal/chroma` and `crystal/chroma_inert` sprites and a
smooth crystalline-stone plinth. It had been registered as a plain `BlockChromaticTile`, which is why
it rendered as an untextured cube.

Verification: `:ChromatiCraft:compileJava`, `:ChromatiCraft:runClientData` and **73/73** GameTests.
Geometry cannot be asserted headlessly — both need an in-client look, along with the still-outstanding
particle depth/atlas fixes.

### Casting table render crash, manipulator overlay, and the no-entry flicker — 2026-08-03

All three reported from a live client on a temple-tier table.

**Hard crash in the render thread.** `RenderCastingTable.extractRenderState` called
`TileEntityCastingTable.getBlocks()`, which builds the tier's `FilledBlockArray` from the NBT
datapack template — and `NBTStructureLoader.load` requires a `ServerLevel`, because structure
templates are a server resource. Any table at TEMPLE tier or above therefore killed the client the
moment it came into view. V33a only ever used that array to enumerate *candidate coordinates* and
then rejected any whose **world** block was not `PYLONSTRUCT` with metadata <= 2, so the array was
never the real test. `getEngravableBlocks()` scans the world directly for the same three
crystalline-stone types over the tier's own extent, which is client-safe and produces the same set.
`getBlocks()` remains for the server-side consumers; the renderer no longer touches it.

*Standing rule this is the second instance of:* anything a renderer needs must be derivable from
block state, block entity state, or the client's own world — never from a datapack registry the
client does not have. The same class of bug hit the icicle/ocean-spike shape work earlier.

**The Elemental Manipulator overlay never existed.** V33a draws it from
`MouseoverOverlayRenderer.renderStatusOverlay`, reached from `ChromaOverlays` only while the
Manipulator is held; that class is still pristine 1.7.10 and outside the compile slice, so there was
nothing to see. `MouseoverStatusOverlay` ports the `OperationInterval` branch as a 26.2 `GuiLayer`
above the crosshair: the four-block non-liquid ray, the state icon indexed by `OperationState`
ordinal from row 1 of the restored authoritative `infoicons.png`, the index-4 backing plate, and a
progress fill while RUNNING. The source drew that fill as a triangle fan; a GUI layer has no
tessellator, so it is clipped from the following sheet icon instead — same fill, same sheet. The
renderer's other branches (lumen storage, focus acceleration, crafter contents, lumen wire, Forestry
and Thaumcraft) belong to tiles outside the slice and are deliberately not stubbed.

**The no-entry icon flickered when the last ingredient landed.** Recipe *presence* was read from the
block entity's sync packet while *runnability* came from container data, and the block entity's
arrives first — so for one tick the screen had a recipe to draw but still held the previous "cannot
run" value. The menu data slot is now tri-state (0 none, 1 present-but-blocked, 2 runnable) so both
facts travel on the same packet, and the screen gates on the menu rather than the block entity.

Verification: `:ChromatiCraft:compileJava` and **73/73** GameTests. The crash fix is structural and
certain; the overlay and the flicker both need the next client run to confirm.

### Clobbered crystal models, overlay UVs, and /place temples — 2026-08-03

**Neither new crystal model was ever loading**, for the reason already recorded against GeoStrata:
`ChromaModelProvider` was still emitting `cube_all` blockstates for the power crystal and all sixteen
encrusted crystals, and a generated blockstate **wins the resource merge** over a hand-authored one
of the same name. Excluding them from `getKnownBlocks()` was not enough — that list only gates the
completeness *check*, not generation. Both emitters now produce the inventory model only and leave
the blockstate alone, and the stale generated files were deleted. This is the third time this trap
has cost a slice: **if a block ships a hand-authored blockstate, no datagen path may `accept` a
blockstate for it.**

**The Manipulator overlay was drawing the sheet wrong.** V33a indexes `infoicons.png` with
`u = 0.125*idx, v = 0.25` and a `0.125` extent on *both* axes, so it is an 8x8 grid of 32-pixel icons
and the status row is row 2 — the port had assumed 8x4 and row 1, which sampled the wrong band. The
backing plate was worse: it is drawn at 38 pixels but must still sample a single 32-pixel icon, and
passing the inflated size as the source region too pulled in the neighbouring icons, which is the
stray partial ring in the report. It now uses the blit overload with separate destination and source
extents.

The progress fill also did not animate as V33a's does. The source sweeps a triangle fan from three
o'clock counter-clockwise (`dx = sin(a+90), dy = cos(a+90)`), and the port had drawn a bottom-up
vertical wipe with a bad `v` offset. A GUI layer has no tessellator, so the wedge is now masked per
scanline: each row emits the maximal horizontal runs whose pixels fall inside the swept angle, with
the icon's own alpha supplying the circle.

**Casting temples are now spawnable.** `CastingTempleFeature` places a tier's canonical NBT template
and its table, registered as `chromaticraft:casting_temple_l1` / `_l2` / `_l3`. Like the turbocharged
and booster-pylon variants these are command-only — no biome modifier names them, because temples are
player-built and must never generate. Placement goes through `NBTStructureLoader.place`, not the
matcher, for the worldgen-deadlock reason recorded earlier.

Verification: `:ChromatiCraft:compileJava`, both datagen sides, **73/73** GameTests, and the three
configured/placed features are emitted. The models and overlay need the next client run.

#### Resolving the water/cloud ordering properly — 2026-08-03

The previous entry recorded the two reports as pulling in opposite directions and left the choice
open. They are now both fixed by the submission phase rather than by the depth flag.

`RenderCrystalPylon` and `RenderCrystalRepeater` submitted their additive geometry to
`RenderPhaseKeys.AFTER_TERRAIN`, which runs **before** the translucent chunk layer. That is what made
the two symptoms mutually exclusive: with depth write on, the glow stamped main's depth and the water
drawn afterwards failed its own test against it, punching square holes; with depth write off, the
cloud target — which the post-chain composites over main — had nothing to sort against, so clouds
covered pylons standing in front of them.

Both now submit to `TRANSLUCENT_CUSTOM_GEOMETRY`, after the translucent layer, and `ADDITIVE_SPRITE`
writes depth again. Water is already down so it cannot be rejected; depth *testing* still hides the
glow behind water and solid terrain; and the depth it writes gives the cloud compositor something to
order against. `ADDITIVE_PARTICLE` keeps depth write **off** — particles still draw before the
translucent layer, so writing there would re-open the water holes.

The remaining "looks bad in daylight, better at night" observation is expected from V33a's blend
itself: `ADDITIVEDARK` is `ONE, ONE_MINUS_SRC_COLOR`, a screen blend against the already-rendered
world colour, so a bright daytime sky inherently leaves the glow less contrast than a dark night sky
does. That is the source's own behaviour and has not been altered; if it still reads as wrong once
the ordering fix is seen in game, the thing to compare against is a V33a screenshot rather than the
blend equation.

## Dedicated-server load path — 2026-08-03

The beta goal is a **server**, and nothing had ever verified that. `:ChromatiCraft:runServer` did not
boot at all: DragonAPI failed to construct, so GeoStrata, RotaryCraft, ElectriCraft, ReactorCraft and
ChromatiCraft were all skipped. Every symptom was the same root cause — **a class the dedicated
server loads named a client-only type**.

The mechanism is worth stating precisely, because the obvious mental model is wrong. Java resolves
lazily, so it is tempting to assume a `Minecraft.getInstance()` buried in a method body is harmless
server-side. It is not:

- **Bytecode verification resolves descriptors used by a method**, so `Minecraft.level` inside any
  method puts a `ClientLevel` descriptor in that class and loading it server-side fails.
- **A lambda is compiled into a synthetic method of its enclosing class**, so a client-only type
  inside a lambda body belongs to the *enclosing* class, not to some separate one.
- **Reflective construction resolves a class fully**, which is how FML builds a mod instance.

Fixed so far, each verified by the server getting one step further:

| Where | What |
|---|---|
| `DragonAPI.openURL` | `ConfirmLinkScreen` lambda → `client/ClientLinkPrompt` |
| `DragonAPI` game dir / profile / local-server | → `client/ClientEnvironment`, with server fallbacks |
| `ReikaSoundHelper.playClientSound` | returned `SoundInstance` → `client/ClientSounds`, void |
| `ReikaSoundHelper.broadcastSound` | reached levels via the *client*; now `ServerLifecycleHooks` |
| `ReikaPacketHelper.sendPacketToServer` | read `Minecraft.level` → holder |
| `ReikaPlayerAPI`, `ReikaChatHelper`, `PacketPipeline` | `Minecraft.player` (`LocalPlayer`) → holder |
| `APIPacketHandler.clientHandle` | `ClientLevel`/`ParticleEngine` → `client/ClientAPIPacketHandler` |
| `ChromaNetwork` handlers | eight clientbound lambdas → `client/ClientPayloadHandlers` |
| Rotary/Reactor/Electri book + calculator items | screen opens → each mod's `client/ClientScreens` |

**`MachineRegistry` carried the machine models.** Its enum constructors took a
`Function<EntityModelSet, ? extends RotaryModelBase>` and 104 constants passed model constructors, so
the client types were in the constructor signature, the field type *and* every constant's lambda —
the enum could not initialise on a server at all, and `RotaryCraft.commonSetup` iterates it. The
factories now live in `client/MachineModels`, an `EnumMap` keyed by `MachineRegistry`; the enum keeps
the registry data the server needs and knows nothing about how a machine looks. Its only consumers
were the two client renderers, the handbook screen and the datagen model provider. Two traps in the
mechanical edit: 24 constants had their model argument *already commented out inline*, which a naive
argument split turns into an unterminated comment, and three used `modelset` rather than `modelSet`.

`MachineRegistry.getName` also called the client-only `I18n` during `PowerReceivers.initialize`; it
now uses `Component.translatable(...).getString()`, which resolves through the active language on a
client and yields the key on a server. `EntityListCommand` and `BiomeMapCommand` reached
`Minecraft.player`/`.level` and are registered through `RegisterCommandsEvent`, so both were failing
command registration; they go through the holder now.

**The dedicated server boots**: `Done (6.099s)! For help, type "help"`, spawn prepared to 100%, with
no client-class load attempts and no mod-loading failures.

Verification: all five modules compile, `:ChromatiCraft:runGameTest` is **73/73** and
`:RotaryCraft:runGameTest` **5/5**. What this does *not* yet prove is that a real client can connect
and walk the arc against a server — that is the next thing to check, and the remaining
client/server-split risk lives in gameplay paths rather than startup.

### GameTests do not prove server safety — 2026-08-03

An important limit on everything this ledger has claimed: `runGameTest` uses `type = "gameTestServer"`
in a dev environment where the **client classes are on the classpath**. That is why the suite stayed
at 73/73 through the entire period in which `runServer` could not even construct DragonAPI. A green
GameTest run says nothing about whether a class is loadable on a real dedicated server.

The reliable check is `javap` over the compiled classes, filtered to those outside client-named
packages:

```bash
javap -p -c build/classes/java/main/<Class>.class | grep -c 'net/minecraft/client/'
```

Source-level grep is useless here — an `import` alone is harmless, and 367 classes across the six
modules import client types perfectly legitimately.

That scan found three ChromatiCraft **gameplay** classes that would have crashed a server the moment
they were touched, none of which any test could have caught:

- `TargetData` — crystal-network data, loaded by the network engine, called
  `Minecraft.getInstance().gameRenderer.mainCamera()` for a frustum check. Now
  `client/ClientFrustum`.
- `BlockGlowDaisy` and `BlockGlowRoot` — `animateTick` narrowed its `Level` to `ClientLevel` in the
  block class itself. The particle helpers now take a `Level` and narrow internally, which is where
  that belongs anyway.

ChromatiCraft is now down to one gameplay class naming a client type, `ChromatiCraft` itself for
`RegisterMenuScreensEvent`, and that one demonstrably loads (the server boots). Re-run the scan after
adding code that touches rendering from a block, block entity or network class.

### A real client joins a real dedicated server — 2026-08-03

The thing the previous two entries said still needed checking now works. `runClient` gained an
opt-in auto-connect and `runServer` an opt-in class-load trace, both in the root `build.gradle`:

```bash
./gradlew.bat :ChromatiCraft:runServer
```
```bash
./gradlew.bat :ChromatiCraft:runClient -PjoinServer=localhost
```

The client connected, `Dev joined the game`, chunks generated around spawn with ChromatiCraft
worldgen running, and the session ran several minutes before being torn down. **Zero**
`NoClassDefFoundError` on either side, and zero `reika.*` exceptions on the server. The only server
errors are pre-existing and unrelated: the two ReactorCraft fluid-component recipe parse failures
(`reactorcraft:processor/uf6`, `reactorcraft:centrifuge/uf6`), a Jade loot-table warning, and oshi
performance-counter noise from spark.

**Correction to the previous entry.** That entry called `javap` "the reliable check". It is not — it
is a *candidate* finder, and it over-reports badly. `-PverboseClasses` (which passes `-verbose:class`)
settles the question directly, and it shows that `WorldLocation` loads fine on a dedicated server
**while naming `Minecraft` and `ClientLevel` in a method body**. `TickRegistry`, which names
`DeltaTracker`, is not loaded server-side at all.

So the rule is not "a server-loaded class may never name a client type". What actually breaks a
server is *executing* the client code, reflective construction of the class (how FML instantiates a
mod), or a static/instance initializer touching it. A javap hit on a renderer, GUI or keybind helper
means nothing; a javap hit on a path a server can reach means everything.

Re-triaged on that basis and fixed the cases a server genuinely reaches:

- `WorldLocation.getWorld()` — the fall-through after the `ServerLifecycleHooks` lookup fails ran
  `Minecraft.getInstance().level` on the server. This is on nearly every gameplay path.
- `DirectResourceManager.initToSoundRegistry` — registered as a reload listener on both dists, so it
  ran on the server's own resource reload and touched the sound manager.
- `CompoundSyncPacket`, `StructureBase`, `ReikaEnchantmentHelper` (committed separately) — same
  pattern; the packet one would have broken every synced block entity.
- `ReikaChatHelper`, `ControlledConfig`, `ReikaJVMParser` — not live bugs; each already dispatches on
  the dist before reaching the client call (`ControlledConfig.genUserHash` is the guard for
  `getClientUserHash`, not anything inside it). Routed through the holder anyway while being touched.
- `RemoteSourcedAsset` — also not a live bug, but for a weaker reason worth writing down: it has **no
  dist guard at all**. Its two resource reads are safe only because every `createAsset` call site is
  a client renderer or overlay. Adding a server-side caller would make it a crash, so guard it then.
  Its static `mcDir` initialiser, which does run at class load, is safe because
  `DragonAPI.getMinecraftDirectory()` is itself dist-aware.

Deliberately **not** changed, because a server never executes them: the `renderAABB` overloads on
`ReikaAABBHelper`, and `render` on `Spline` and `Proportionality`. Those name `SubmitNodeCollector`
but are only ever called from `render/` packages.

`ClientEnvironment` grew `launchedVersion()`, `profileIdentifier()`, `hasGameInstance()`,
`clearChat()` and `resourceStream()`; `ClientSounds` grew `hasSoundManager()`.

What this still does not prove: nobody has walked the actual progression arc against a server.
Joining and standing at spawn exercises login, chunk load, worldgen and block-entity sync, not
casting-table interaction, rune placement or progression triggers.

### `/test run` does not reliably drive gametests on a DedicatedServer — 2026-08-04

Attempted to close the remaining gap — "the casting arc has never run against a real dedicated
server" — by driving the existing suite through the server console rather than `gameTestServer`.
Commands reach the server fine (pipe them into `runServer`'s stdin), and `chromaticraft:*` resolves
to **68 tests**, so the selector and the dispatch both work. The tests do not.

What happens: `Running 68 test(s)...`, then `Running test environment 'chromaticraft:default' batch 0
(50 tests)`, and then nothing. A thread dump shows the server thread parked in
`MinecraftServer.waitUntilNextTick` with **3 seconds of CPU burned over 10 minutes** and zero
`reika.*` frames anywhere. The batch is registered and then never ticks. The likely cause is that
`gameTestServer` force-loads its arena while a dedicated server does not, so with no player online
the test structures sit in unloaded chunks — but that was not proven, and the fixes for it did not
work.

Things tried that did **not** help: a superflat throwaway level, `spawn-protection=0`,
`gamerule spawnChunkRadius 32`, running tests one at a time instead of as a batch, and — the most
promising one, tested last — **forceloading the whole test area** (`forceload add` over 512x512
blocks in four commands, which the server accepts and reports). That last one directly targeted the
unloaded-chunk hypothesis, and it failed the same way: `Running 68 test(s)`, then eight and a half
minutes of setup, then `Running test environment 'chromaticraft:default' batch 0 (50 tests)`, then
nothing. The unloaded-chunk explanation is therefore **not** the cause, or not the only one.

One thing that *did* work, early and once: a single `test run` completed in about a second and
printed `All required tests passed :)`, against the stock `world` at the stock settings. That was not
reproducible afterwards. Note that the dedicated server's watchdog (`max-tick-time=60000`) does fire
during batch setup and crashes the server — `GameTestServer` has no watchdog — so any future attempt
needs `max-tick-time=-1` as a precondition, not as a fix.

`server.properties` has been restored to stock and the throwaway `gametest_srv` level deleted.
`run/world` does now contain leftover gametest structures near spawn from these runs; it is a
gradle-generated dev world, so deleting it is the cheapest cleanup if it gets in the way.

**Where the arc actually stands.** Being precise, because the three claims are easy to conflate:

- The casting arc's *logic* is green — 73/73 under `runGameTest`, including after the `WorldLocation`
  change, and the suite covers the arc properly (`early_game_casting_stand_chain`,
  `casting_stand_ownership_lock`, twenty-odd `casting_table_*`, `crystal_rune`, `progression_*`,
  `pylon_*`).
- The mod stack is *loadable and joinable* on a dedicated server — clean boot, zero
  `net.minecraft.client` classes loaded under `-PverboseClasses`, a real client connected and played
  with zero `NoClassDefFoundError` on either side.
- **Nobody has walked the arc against a server.** That is still open, and neither of the two points
  above closes it. It needs a human at a client, and it overlaps the client-side visual backlog
  (encrusted and power crystal meshes, manipulator overlay, pylon against clouds and water) that also
  cannot be checked headlessly.

### Temple spawn commands work on a dedicated server — 2026-08-04

The `/place feature` temple spawners are confirmed working against a real dedicated server, which is
the tooling the manual arc walk depends on:

```
place feature chromaticraft:casting_temple_l1 16 123 16
```

All three tiers reported `Placed "chromaticraft:casting_temple_l<n>"` with no exception, and a
follow-up `execute if block <pos> chromaticraft:casting_table` confirmed a real casting table in each
— so the structures materialise, rather than the command merely returning success. Worth stating
because this exercises `NBTStructureLoader.place` and the multiblock templates *server-side*, which is
gameplay code rather than startup code, and it is the first arc-adjacent logic proven on a real
dedicated server rather than under `gameTestServer`.

The three temples are left standing in `run/world` at (16,123,16), (56,123,16) and (96,123,16), tiers
1/2/3, so the arc walk can start from a table instead of building one. Note the area needs
`forceload add -16 -16 112 112` if placing more while no player is online.

### Worldgen audit: pylon step, tiered-ore band, and a generated-content census — 2026-08-04

Two worldgen defects fixed, both found by censusing a pregenerated world's region files for
ChromatiCraft block ids rather than by reading code.

**Pylons generated before vegetation.** The feature was registered at `SURFACE_STRUCTURES`
(decoration step 4); `VEGETAL_DECORATION` is step 9. No tree exists in the chunk at step 4, so every
tree branch in `PylonFeature.canGenerateAt` / `isFloorReplaceable` / `isAirReplaceable` — and the
canopy-descent commit that preceded this one — was dead code during natural generation and only ever
affected `/place feature`. Trees then generated on top of the site the feature had just verified as
clear. V33a's `PylonGenerator` was a `RetroactiveGenerator` and therefore ran post-population, which
is why its site test is saturated with tree handling (`getTreeDodgeAttempt`, `array.sink` through
wood and leaves). Now at `TOP_LAYER_MODIFICATION`, the last step, which is the faithful analogue.

**Tiered ores could never generate.** `TieredOreHeightPlacement` offset V33a's y roll by the level's
minimum build height, so the whole overworld band landed in y [-64, 0) — deepslate — while both
overworld ore features target `minecraft:stone`. Absolute y is also correct everywhere else: the
Nether and End floors are still 0, so only the overworld floor moved. Measured on a 2,189-chunk
pregen, `energized_rock` went 0% -> 22.9% of chunks and `elemental_stones` 0% -> 22.1%, with a y
histogram matching the source roll (~43% in each of y 0-15 and 16-31, tapering to nil by 48-63 where
attempts reach the surface). Density remains below V33a's, where y 0-64 was solid stone throughout;
modern y 0-8 is the deepslate transition and a stone-only target loses it. Adding
`minecraft:deepslate` to the target list would recover that, but V33a targeted `Blocks.stone` alone,
so it is recorded here as a separate decision rather than bundled in.

**Census method, worth reusing.** Region chunks store palette names and biome ids as literal strings,
so decompressing each chunk and substring-searching the raw NBT gives an accurate presence census per
dimension with no NBT parse. Two traps: the world seed lives in `data/minecraft/world_gen_settings
.dat`, not `level.dat`, in 26.x; and per-chunk region timestamps are *last-save* times, not
generation times, so bucketing density by timestamp is biased — visited chunks get re-saved.

**What the census showed as healthy** over a fresh 10,276-chunk overworld: all sixteen cave-crystal
colours (~1.6-2.0% of chunks each), all sixteen dye-leaf colours, `rainbow_forest` 1.84%,
`rainbow_stream` 0.40%. A nether pregen (1,764 chunks) confirms `firestone` at 11.05% and nether cave
crystals across all sixteen colours. `luminous_cliffs` absent from a 10k sample is sample size, not a
defect — it replaces `WINDSWEPT_HILLS`/`STONY_SHORE` inside a weight-2 TerraBlender region and
`/locate biome` resolves it.

**Pylon acceptance rate is still the open question.** Roughly 4 pylon chunks against ~98 grid
candidates in that same fresh world (~4%), and the grid itself is faithful (625 candidate cells per
256x256 against `ShuffledGrid`'s 676). The site predicate was audited against source and is faithful
— `ReikaWorldHelper.softBlocks` resolves to air/liquids/`isReplaceable`/vine/tallgrass/deadbush/fire/
snow_layer, all covered by modern `canBeReplaced()`, and the cross-shaped footprint and both
replaceable sets match. So the remaining sparsity is terrain roughness, not a coverage gap, and the
step move above did not change it. Note that `RUNEUSE <- ALLCOLORS <- PYLON` puts sixteen
distinct-colour pylon sightings on the critical path, so this rate is load-bearing for the beta arc.
There is also no retrogen: V33a back-filled existing chunks and a placed feature cannot, so any
already-explored world stays empty regardless.

### Tiered plants: the missing half of TieredWorldGenerator — 2026-08-04

`ChromaTieredPlants` registers all seven V33a `TieredPlants` as concrete per-plant identities — Aura
Bloom, Rock Flower, Essence Lily, Element Bulbs, Radiance Bush, Vibrant Pod, and Glowing Roots — carrying each plant's `ProgressStage`,
tint, drop identity, V33a `getGenerationChance`/`getGenerationCount`, siting branch, and exact
`getHarvestResources` count formula. Display names are the authoritative `chroma.tieredplant.N`
strings and the V33a front/back sprite pairs were extracted from the release jar and **renamed onto
the concrete identities**; the legacy ordinal appears nowhere — not as a field, blockstate property,
texture, model, or language path.

Glowing Roots joined the accepted slice once its legacy metadata-22 drop was confirmed already
registered as the concrete `boost_root` item. Its display name was corrected from the provisional
"Boost Root" to V33a's authoritative **Enrichment Root**, and its exact TURBOCHARGE gate,
one-in-four/three-attempt frequency, dirt-or-grass support, `1 + rand(1+fortune)/2` drop count, and
tree-root siting are retained. The siting snaps each attempt to the V33a `bitRound(..., 4)+7/8`
lattice, reproduces `ReikaWorldHelper.findTreeNear`'s seven vertical samples and three-log test,
walks to the lowest log, and probes all six neighbours in random order. That centred radius-seven
search stays inside the active chunk, which also avoids unsafe far-chunk reads in modern worldgen.

Vibrant Pod closes the family with a separate `luma_beans` item and the exact atlas-index-149 sprite.
It retains the ALLOY gate, one-in-four/six-attempt frequency, `2 + rand(1+fortune*3/2)` drops and the
source's random-height trunk attachment. Its neighbour-sensitive selection box expands only toward
the adjacent supporting logs, just as V33a's dynamic block bounds did. Both tree plants share the
same exact lattice and `findTreeNear` implementation; only their post-search traversal differs.

`BlockTieredPlant` reproduces the source behaviour: no collision, light 4, V33a's per-plant support
rules (surface/sand stand on the block below, cave and leaf plants hang from the block above, the
lily needs a still water source), and the tier gate. That gate is total, not cosmetic — V33a's
renderer draws nothing and `getSelectedBoundingBoxFromPool` returns a zero box, so an insufficient
player cannot even look at the plant. Here it is an empty `getShape` for a `Player` without the
stage, which unlike V33a's client-only override also holds on a dedicated server. Because such a
player can never target one, there is no insufficient-harvest branch as the tiered ores have, and the
loot table is empty with every drop decided in code.

`TieredPlantFeature` owns only the column search, which is the part with no vanilla equivalent; the
one-in-N chance and attempt count are ordinary `rarity_filter` and `count` modifiers in each placed
feature.

Three defects were found by pregenerating and censusing, not by reading the code:

1. **The cave scan was in deepslate.** It walked 64 blocks up from `getMinY()` while testing for a
   `Blocks.STONE` ceiling — the identical absolute-versus-relative-y mistake as the tiered ore band
   above, repeated in the same session. Rock Flower generated in 1 chunk out of 1,764.
2. **`VEGETAL_DECORATION` is the wrong step.** It is where the trees themselves are, and ordering
   within a step is not guaranteed, so a plant could be sited before any leaf existed and then be
   overwritten by a tree. Element Bulbs generated in 1 chunk out of 1,764. Now at
   `TOP_LAYER_MODIFICATION`, the same choice the pylon feature makes and the faithful analogue of
   V33a's post-population `RetroactiveGenerator`.
3. **V33a's literal air test does not survive that move.** The source requires the block above the
   ground to be `== Blocks.air`; after vegetation, modern terrain is blanketed in tall grass and
   flowers, and Aura Bloom fell to zero chunks in 1,941. The test now accepts a replaceable
   non-fluid block as well. This is the one place in the slice where the source condition was
   adapted rather than transcribed — it is the freedom vanilla's own flower placement takes, and it
   preserves the intent that nothing solid is standing there.

Observed generating after the fixes: Rock Flower 6.12%, Aura Bloom 3.91%, Element Bulbs 0.85% of
chunks in a 1,764-chunk pregen, with Essence Lily (0.21%) and Radiance Bush (0.17%) seen in earlier
probes — both are water- and sand-bound and did not fall in the final sample. Note each probe world
is generated with a fresh random seed, so figures are not directly comparable run to run; the
zero-to-nonzero transitions are the load-bearing evidence, not the exact percentages.

The focused `tiered_plant_survival_sources` GameTest now pins Element Bulbs, Radiance Bush, Vibrant
Pod and Glowing Roots to their source chance/count, support, progression visibility and fortune-zero
drop contracts against built terrain. **Still open on this slice:** direct feature-placement
coverage of all siting searches and the client-side tier check so an insufficient player sees
nothing rather than relying on the shape gate alone.

### Cave Indicator (Piezo Crystals) — 2026-08-04

`CaveIndicatorGenerator` is the second of V33a's thirteen overworld `IWG` generators to land this
cycle. It seeds stone under the Glowing Cliffs — this port's Luminous Cliffs — with a block that
looks like ordinary stone until something steps on it, then lights up and emits redstone.

`BlockCaveIndicator` keeps V33a's on/off in an ordinary boolean blockstate rather than two registered
blocks. That is not a relaxation of the concrete-identity rule but the case the rule explicitly
exempts: one object with genuine runtime state, not a variant family. Retained from source: light 10
while active and 0 otherwise, weak redstone 15 while active, a re-step refreshing the shutdown timer
instead of stacking a second one, deactivation after 100-300 ticks, and dropping what the stone it
replaced would drop rather than itself.

`CaveIndicatorFeature` keeps upstream's full 16x16-column loop with two attempts per column. That is
deliberate: expressing it as vanilla `count` modifiers would turn "twice per column" into "N times
per chunk" and change the distribution. The y band stays absolute for the same reason the tiered ore
band does. Biome restriction lives in the biome modifier, which attaches the feature to
`luminous_cliffs` and `luminous_cliffs_shores` only rather than to an overworld tag — so the provider
gained an explicit-biome-list variant alongside its tag-based one.

The model reproduces `CaveIndicatorRenderer`: a stone-textured cube with the source's own top sprite,
plus one extra top-face quad inset 0.1 blocks carrying the inner sprite, full-bright in the active
variant and normally lit in the inactive one. Both inner sprites keep their original animation
metadata.

One source dependency is marked rather than faked: V33a plays `ChromaSounds.DING` upshifted to a
random degree of the C major scale through DragonAPI's `MusicKey`/`KeySignature` interval helper,
which is not ported. The sound is registered but the pitched variant is left as a `CHROMA-PORT`
marker instead of substituting an arbitrary pitch.

**Verification boundary, stated precisely.** `compileJava`, `runServerData` and `runClientData` are
green, and the generated biome modifier, both blockstate variants, models and the cobblestone loot
table were inspected. It has **not** been observed generating in a world: Luminous Cliffs is a
weight-2 TerraBlender replacement of `WINDSWEPT_HILLS`/`STONY_SHORE` and did not appear in any of
this session's random-seed pregens, so a census cannot confirm it by luck. Verifying it needs a
targeted probe — `/locate biome chromaticraft:luminous_cliffs`, then forceload that area and census
for `chromaticraft:cave_indicator` — or a focused GameTest that builds the required dark, sky-hidden
stone itself. Until one of those runs this is wiring-verified only.
### Complete V33a lexicon identity/XML foundation — 2026-08-08

The guide-book vertical is active again. `LexiconCatalog` is a dependency-free modern identity
boundary mechanically derived from the complete V33a `ChromaResearch` enum: all 322 entries retain
their original order, seven section boundaries, research tiers, parent/page status, and the source
registry binding that later icon and specialist-page renderers will use. This is intentionally not
an early-game subset. In particular, `DATATOWER` already resolves to the RAWEXPLORE structure page
bound to `ChromaStructures.DATANODE`, giving the forthcoming lore terminal a stable page identity.

`LexiconDescriptions` now reads the original shipped `info.xml`, `machines.xml`, `blocks.xml`,
`tools.xml`, `resource.xml`, `abilities.xml`, and `structure.xml` directly. It preserves nested
description/note pages and explicitly tolerates V33a's unusual copyright-comment-before-XML-
declaration layout. The original navigation, page, and button artwork was recovered unchanged from
the V33a jar into modern resource paths. No replacement prose or invented guide art was introduced.

Verification: `:ChromatiCraft:compileJava` passes. Only
`chromaticraft:lexicon_v33a_catalog` was run; it passes 1/1 and checks the complete entry count,
fragment/readability semantics, DATATOWER binding, original CRYSTALS prose, and nested INVLINK notes.
The next slice is the registered Chromic Lexicon item plus navigation/page screens over this catalog,
then fragment/player-research interaction. Once that is live, the NBT DATANODE structure and real
`TileEntityDataNode` lore terminal can land without a hollow research substitute.

### Guide item, Info Fragment and DATANODE template — 2026-08-08

The guide is now an actual registered `chromic_lexicon` item and V33a `information_fragment` is an
active component-backed item rather than catalog-only data. The original item sprites were cut from
V33a `items_tool.png` at indices 5 and 9, and the exact six-colour shard/glowstone/book recipe is
datagen-owned. Blank fragments can be manually decoded or chroma-soaked for V33a's prioritized
random choice, grant death-persistent player research, and move into or back out of a lexicon.
Recovery is server-authoritative and atomically costs one paper plus one black dye.

`PlayerResearch` restores V33a's tier priorities, hard fragment dependencies, progression-stage
gates, tier-up checks and death-persistent storage. The navigation screen now exposes original XML
descriptions/notes plus progress, recovery, notebook and stored-page transfer views. All 322 page
titles are exact V33a values recovered mechanically from `ChromaResearch`, the old registry source
bindings and `en_US.lang`; the source's sole untranslated value (`chroma.aishutdown`) deliberately
remains untranslated rather than receiving an invented title. Modern registry icons are bound for
every content family that is currently registered, with the stable source id retained for later
content.

The DATANODE chain has crossed its NBT boundary: `ChromaStructures.DATANODE` now loads
`structure/worldgen/data_node.nbt`, generated from the exact 3x3 shield floor, four shield arms,
central node and four linked dummy cells. Per-cell moss/stone selection remains a worldgen-time
alternative as required by V33a; it is not encoded as metadata or split into invented geometry.
The next implementation boundary is the real `TileEntityDataNode`/data-crystal/lore scan cluster,
followed by template placement, dummy linking, loot and focused feature/scan GameTests.

Verification at this checkpoint: `compileJava`, `runServerData`, and `runClientData` pass. Server
datagen emitted the canonical DATANODE NBT and exact lexicon recipe; client datagen emitted the two
item models and all exact guide translations. Only the focused catalog and information-fragment
tests are rerun for this vertical.

### DATANODE server loop and fixed-tower generation — 2026-08-08

The canonical `worldgen/data_node.nbt` now has a registered center block and block entity rather
than referencing a future id. `TileEntityDataNode` restores V33a's 24/50/36-tick three-stage
deployment, reversed retraction, rotation phases, extension/ambient sounds, 120-tick scan with
four-tick sustain and eight-tick decay, 240-tick cooldown, per-player UUID set, TOWER progression,
and the upward owner-tagged Memory Crystal reward. The Elemental Manipulator dispatches to the scan
without moving authority to the client. The four NBT dummy cells are linked to the center after
placement and carry the original HITBOX=true, RENDER=false, MOUSEOVER=false flags.

`DataTowerFeature` uses the thirteen deterministic `Towers` positions, places only the NBT template,
performs the exact one-in-three STONE-to-MOSS choice at placement time, assigns the tower identity,
and buries a loot chest backed by the vanilla stronghold-library table. It is injected at the final
overworld decoration step, matching the old retro-generator ordering, and also has a stable
`chromaticraft:data_tower` feature-command identity. No metadata-like tower or material variants
were introduced.

Focused verification passes independently: `data_node_nbt_feature` checks the reinforced 3x3 floor,
node identity, and all four links; `data_node_scan_loop` checks 110-tick deployment, scan completion,
cooldown, unique reward, and owner UUID. `information_fragment_research_loop` and
`lexicon_v33a_catalog` also pass independently. Remaining DATANODE parity work is the original
client renderer/scan and neighbor-tower FX, delayed lore-puzzle notification, Meta-Alloy plants,
Tunnel Nukers, and the specialized Memory Crystal inscription/entity/rendering cluster.

### Casting guide pages and DATANODE presentation — 2026-08-08

The first specialist guide family is no longer a generic description page. Casting-table entries
request their recipes from the authoritative server `RecipeManager` through typed 26.2 payloads;
the response carries the complete `CastingTableRecipe` state rather than reconstructing recipes on
the client. The lexicon presents V33a's recipe tiers in source order and exposes separate grid,
rune, stand and aura views. This keeps the screens dedicated-server-safe while preserving the
actual recipe inputs, rune coordinates, auxiliary stand positions, aura costs and output. The
focused `lexicon_casting_recipe_snapshot` test proves ordinary and boosted Green Crystal Group
recipes are returned and ordered by tier.

The Data Node now has a submit-pipeline block-entity renderer built from the original V33a geometry
and recovered textures: its moss pedestal telescopes with the three deployment stages, both tower
rows extend independently, the 8x8 symbols rotate around the opened tower, the source flare and
counter-rotating prism return, and a completed scan produces the twin twisting sky beam. The baked
world model is intentionally particle-only so it cannot checkerboard or depth-fight over the BER;
the inventory model remains a shield cube.

Client FX again follow the source choreography. Deployed nodes emit the pale ambient glows and
neighbor-directed wandering seeds; the server synchronizes all thirteen deterministic tower roots
on login so those directions are authoritative. Scan completion sends one nearby client event that
creates the 360-seed radial burst, paired blue/white vertical glow column and the original layered
sound chord. Payload sends are guarded by negotiated-channel checks, including embedded GameTest
players.

Finally, scan completion again waits 50 ticks before recording the tower. The resulting per-tower
flags live in DragonAPI's death-persistent player compound under V33a's `loretowers` key, sync to a
real client, display the tower character and play the lore cue. The focused
`data_node_scan_loop` test now proves deployment, scan ownership/reward, the absence of an immediate
flag, and the delayed persistent BETA flag; it passes 1/1. `compileJava` and `runClientData` also
pass. Rendering and exact audiovisual appearance still require an in-client observation; the
headless checks prove registration, serialization and server behavior, not pixels or OpenAL output.

Remaining DATANODE work, in dependency order: Meta-Alloy plant ecology; Tunnel Nuker population;
the full Memory Crystal inscription, custom-entity and renderer cluster; then the later key-assembly
puzzle/Rosetta/LoreManager presentation. The next guide work is the remaining specialist machine,
tool, ability and structure page families plus full icon coverage.

### DATANODE ecology, Memory Crystal and lore-key vertical — 2026-08-08

This section supersedes the remaining-work paragraph immediately above. The eight-part DATANODE and
guide slice is now implemented end to end; visual, audio and population tuning still requires the
in-world acceptance pass described below.

1. **Meta-Alloy ecology.** `BlockMetaAlloyLamp` is a registered, explicit-state block (`facing` plus
   `pod`) rather than a metadata emulation. Data Nodes make source-shaped surface-search attempts on
   grass around their tower, enforce the original wide separation, and retain the adaptive retry
   interval. Leaves grow pods through random ticks; left-clicking a
   mature pod harvests the item without destroying the plant, and removal unregisters the plant
   from its node. The original leaf, side and pod textures are used by multipart datagen models.
2. **Tunnel Nukers.** The entity, attributes, population tick, Data Node spawn path, renderer, model,
   texture, ambient/call audio and movement particles are active. Flight retains the V33a no-goal,
   terrain-following orbit rather than substituting vanilla pathfinding, with player/tower-relative
   spawn checks, caps, persistence and source invulnerability behavior.
3. **Memory Crystal inscription.** `ItemDataCrystal` owns the sustained inscription state in
   `CUSTOM_DATA`, binds progress to the precise block and recipe, decays interrupted work, plays the
   original cadence, and completes the exact built-in Smooth Crystalline Stone to Pylon Link recipe.
   The server owns progress and completion; the client only renders the synced bar and event FX.
4. **Memory Crystal entity and renderer.** Dropped crystals use a dedicated registered item entity,
   retain their thrower/pickup semantics, do not expire, float in water and resist ordinary damage.
   Their renderer restores the large, tilted, counter-rotating V33a prism and flare using the
   recovered node/flare artwork with no entity shadow.
5. **Guide integration.** The INSCRIPTION entry resolves to the real Memory Crystal identity, while
   casting pages continue to request authoritative recipe snapshots. Machine, tool, ability and
   structure entries have specialist presentation: family-aware machine headers, concrete tool
   identity, all 77 recovered V33a ability illustrations, and an honest compact structure-footprint
   preview. The latter is deliberately not labelled as a template render; it can become an NBT-driven
   three-dimensional preview as each remaining original structure receives its modern NBT template.
6. **Specialist guide families.** Registered-content icons and the specialist casting, machine,
   tool, ability and structure paths coexist with all 322 exact source catalog identities and XML
   pages. Unregistered future content retains its stable source id instead of receiving a fabricated
   modern item or block identity.
7. **Key assembly and Rosetta.** The modern deterministic puzzle retains the source 15-wide/169-cell
   board, 13 voids, 500 legal shuffle moves, Element Mixer adjacency rule, thirteen tower groups and
   tower-scan visibility. Moves are validated and persisted server-side by replaying the authoritative
   seed, so a client cannot forge completion. Completion switches to Rosetta, whose recovered image is
   decoded by the original image-to-string/Base64/filter route rather than replacement prose.
8. **Lore presentation.** Tower scan notes now carry enough authoritative puzzle state to reconstruct
   the appropriate three four-cell groups. The overlay restores the delayed five-second color-hex
   reveal and source fade envelope, and reopening the puzzle restores accepted moves and completion
   across death and relog through death-persistent player data.

Verification for this slice is deliberately narrow. `compileJava` and `runClientData` pass. Only the
four new contracts were run, individually: `meta_alloy_ecology_contract`,
`tunnel_nuker_entity_contract`, `memory_crystal_inscription_loop`, and
`lore_key_puzzle_contract`; all pass 1/1. The headless checks establish registry identity, state and
serialization contracts, authoritative gameplay transitions and deterministic puzzle rules. They do
not establish pixels, sound attenuation, perceived particle choreography or natural-world population
frequency; those are the required in-world acceptance boundary before this vertical is called
visually complete.

## Data-tower/tree/tiered-resource rendering follow-up — 2026-08-09

1. **Coordinate-bound tower discovery.** `/place feature chromaticraft:data_tower` remains an exact
   coordinate-bound diagnostic by design: the feature may only succeed in one of the thirteen seeded
   lore-tower root chunks. `/locate chromaticraft:data_tower` now resolves the same authoritative
   `Towers` layout and reports the nearest Overworld tower centre, symbol and horizontal distance.
   This deliberately does not pretend the lore hex is a vanilla random `StructureSet`.
2. **Tiered resource interaction.** Aura Bloom and the other tiered plants retain the original
   survival progression concealment, but creative players can now target and break them. Creative
   placement receives the same explicit bypass for Elemental Stones and Firestone; insufficient
   survival placement still self-removes as the original anti-sequence-break behavior requires.
3. **Tiered ore models.** Elemental Stones, Firestone and Energized Rock now use the direct 26.2
   custom blockstate-loader schema (the obsolete nested `model` object prevented loader decoding).
   Their item forms receive concrete underlay models; the original viewer-dependent animated overlay
   remains the in-world custom-model pass.
4. **Crystal item rendering.** Cave, lamp and potion/super-crystal items retain the source spike
   silhouette, element tint, translucency and applicable stone plinth. Sprite lookup is deferred from
   parallel model baking until submission, matching the 26.2 special-renderer lifecycle and avoiding
   the pre-atlas `Atlas not initialized` failure that blanked all 48 forms.
5. **Tree-level log choice.** `RandomTagSingleStateProvider` now selects tagged wood deterministically
   from the tree's trunk X/Z rather than consuming randomness independently for every trunk cell.
   Dye trees therefore choose once per tree while neighboring trees can still differ.
6. **Rainbow-tree source shape.** The configured Rainbow Tree now uses a dedicated port of V33a
   `tryGenerateSmallRainbowTree(..., 1)`: one wood identity, source height/truncation rules, the
   rising/falling diamond crown, top cross and terminal leaf. Soil/site selection stays in the placed
   feature/caller, and the generator preserves the source routine's unconditional trunk placement.
   The separate 2x2 giant `RainbowTreeBlueprint` is not claimed by this slice; when enabled it must be
   supplied as a modern NBT structure template, in accordance with the standing structure rule.
7. **GeoStrata side fix.** The shared luminous-crystal model now emits tint index zero, matching the
   registered block tint source. Each already-separated luminous-crystal registry identity therefore
   renders its own concrete colour instead of falling through to white.

Focused verification only: `:ChromatiCraft:compileJava`, `:GeoStrata:compileJava`, client/server
datagen, `creative_tiered_resource_access` (1/1) and `rainbow_tree_shape_and_log` (1/1) pass. The
rainbow test initially caught the extra modern placement restriction; it was corrected against V33a
and only that failed contract was rerun. No unrelated GameTests were repeated. Inventory sprite
baking, translucent pixels, creative/survival interaction feel, natural tree variety and actual
tower command travel remain the in-world acceptance boundary.

## Data Node interaction/render acceptance and active block tags — 2026-08-09

This pass closes the code-side defects reported during the first DATANODE client inspection. The
four visible dummy hitbox cells now relay Elemental Manipulator use and the mouseover operation HUD
to their linked controller, matching V33a's `relayManipulatorClick` contract. Scan progress and
sustain are included in the vanilla block-entity update tag, so the operation overlay can display
the live 120-tick activation instead of remaining pending. The Data Node opts out of DragonAPI's
legacy broad tile payload and uses vanilla BE updates plus its dedicated completion FX payload;
this also keeps embedded/headless clients which did not negotiate `dragonapi:dragonapidata` from
crashing during scan.

The Data Node renderer now contains both complete six-faced, threefold tower rows and the square
inner stone sleeve that the previous approximation omitted. The scan column is again the V33a
twelve-sided twisting cage, segmented from the node to y=128 rather than two short crossed ribbons.
Its renderer bounds cover the complete 130-block column, off-screen rendering remains enabled, and
the view distance is 256 blocks, preventing controller-frustum culling while any part of the tall
render is visible. Exact geometry, UV orientation and long-distance blending remain an in-client
visual acceptance item; compilation cannot certify those pixels.

The shared cave/lamp/potion crystal item special renderer now submits its alpha-220 spike mesh to
the 26.2 item-translucent target, preserving the same translucent layer used by the world mesh. The
shared custom outline extractor applies to all three crystal families, derives edges from their
actual baked/Java-authored model, and uses vanilla's normal translucent-black colour and
window-selected line width (including vanilla high-contrast behavior). Item Stand uses that same
vanilla outline styling. This extraction path is intentionally reusable by later non-voxel Reika
models rather than maintaining hand-copied line geometry.

`/locate chromaticraft:data_tower` now emits the familiar green, hoverable coordinate component;
clicking it suggests `/tp @s x ~ z`. `/locate chromaticraft:data_tower all` lists all thirteen
authoritative tower names, symbols and individually clickable Overworld coordinates in puzzle
order. This remains a coordinate-bound lore layout rather than a fabricated random StructureSet.
The unfinished key-assembly screen no longer paints an opaque navy rectangle, so the live world is
visible behind the Memory Crystal puzzle; Rosetta's authored completed background remains intact.

Finally, the active registry slice now emits vanilla mineable tags for its material families:
pickaxe tags cover crystalline stone and its variants, runes, all registered crystal families,
shielding, tiered ores and stone/machine content; cliff soils and mud use shovel; leaves use hoe;
the loot chest uses axe. This restores tool-speed selection that hardness alone cannot provide in
26.2. Server datagen emitted the canonical `minecraft:mineable/*` files.

Verification was deliberately focused. `:ChromatiCraft:compileJava -x :ElectriCraft:compileJava`,
client/server datagen, and only `chromaticraft:data_node_scan_loop` pass; the latter is 1/1 and now
also observes the persistent owner-bound Memory Crystal immediately at completion. A normal
aggregate compile is presently intercepted by unrelated ElectriCraft WIP errors in its fuse/battery
slice. Server datagen continues to report the already-known Luminous Cliffs biome-filter diagnostics;
neither issue was hidden or altered in this pass.

## Data Tower siting and worldgen-safe Skypeaters — 2026-08-09

The fixed lore layout now performs deterministic village exclusion while the thirteen roots are
being calculated. It queries the world seed's registered `StructurePlacement`s for the vanilla
`#minecraft:village` tag, never neighboring terrain or partially generated chunks, and nudges a
conflicting root to the nearest clear chunk. An eight-chunk exclusion around predicted village
start chunks leaves room for outer jigsaw pieces; prediction deliberately errs on the conservative
side if a start later rejects its biome. All thirteen enum identities remain present, and locate,
generation, neighbor FX and puzzle discovery consume the same adjusted authoritative positions.

Tower placement now reproduces V33a's shared-floor preparation across all nine foundation columns
instead of sampling only the center. Because the modern feature runs after vegetation, it then
removes intersecting logs, leaves and non-fluid replaceable plants in a compact 9x9, same-chunk
clearance volume before placing the canonical NBT. Terrain, liquids and non-replaceable structure
materials are not flattened. The focused `data_node_nbt_feature` contract now grows a six-block oak
trunk and canopy through the target before placement and proves the complete tower clearance; it
passes 1/1.

The supplied unsafe-terrain report was from `chromaticraft:skypeater`, not the Data Tower. Its V33a
32-block proximity scan crossed the FEATURES-stage one-chunk write radius, and its proto-chunk block
entity setter attempted `syncAllData` before Minecraft attached a `Level`. The scan now ignores
positions outside `WorldGenLevel.ensureCanWrite`, while `setNodeType` persists immediately and only
sends a live sync when a level is attached. This removes both the unsafe cross-chunk read and the
reported null-level crash without removing Skypeater generation.

Focused verification: `:ChromatiCraft:compileJava -x :ElectriCraft:compileJava` succeeds and only
`chromaticraft:data_node_nbt_feature` was run, passing 1/1. Existing towers and already-generated
terrain are not retroactively relocated or cleared; village-safe roots and tree clearance apply to
new world layouts/unexplored tower chunks.

## Rosetta composition and explicit pylon feature identities — 2026-08-09

The completed Memory Crystal/Rosetta page now draws `all-back.png` once across the scaled GUI. The
256x256 source is one authored composition; the previous nested loop restarted it every 256 pixels,
producing visible repeated borders and motifs on larger windows. The incomplete puzzle remains
transparent as established in the preceding pass.

Manual pylon placement is now colour-deterministic. Sixteen feature/configured-feature identities
are generated as `chromaticraft:pylon_<vanilla dye name>` (`pylon_black`, `pylon_light_gray`,
`pylon_light_blue`, through `pylon_white`), each backed by a `PylonFeature` with a fixed
`CrystalElement`. The ambiguous `chromaticraft:pylon` feature/configured-feature was removed, so a
manual `/place feature` no longer claims a generic identity and then rolls an unrelated colour.
Natural V33a generation retains its random colour roll under the explicit internal-facing identity
`chromaticraft:natural_pylon`; the Overworld biome modifier and pylon-grid placement now reference
that identity. Existing turbocharged and power-crystal-boosted diagnostic features are unchanged.

Verification: `:ChromatiCraft:compileJava` and `:ChromatiCraft:runServerData`, excluding unrelated
ElectriCraft compilation, succeed. Datagen emits exactly sixteen `pylon_<colour>` configured and
placed features, emits `natural_pylon`, updates the biome modifier, and does not recreate the stale
generic `pylon` JSON. No GameTests were needed for this registry/render submission-only change.
## Chromic Lexicon shared navigation/input parity — 2026-08-09

The accepted 26.2 `ScreenChromicLexicon` now restores the shared interaction grammar which V33a
provided through `ChromaBookGui`, `GuiScrollingPage`, and `GuiNavigation`, rather than treating the
book as a static list of buttons:

- the last navigation section and scroll position survive closing an entry or reopening the book;
- all seven original catalog parents remain independently selectable, backed by the complete
  322-entry identity catalog and the fragment visibility checks;
- navigation entries show their resolved block/item icons and their V33a research-level grouping
  color, including the distinct PYLONCRAFT/CTM tiers;
- the left Items/Recipes mode pair is restored; recipe mode opens a selected entry directly onto
  its server-authoritative casting recipe when one exists, without shipping the recipe registry to
  the client;
- the Progress, Recovery, and Notebook side tabs remain available, and stored-fragment inventory
  mode retains insertion/ejection behavior;
- Search accepts authored page titles across every section, supports `/` activation, backspace,
  Enter, Escape, and a persistent result set after finishing input;
- W/S and Up/Down page the navigation or entry text, A/D and Left/Right switch sections or adjacent
  entries, and the mouse wheel follows the same page movement;
- inside a casting recipe, A/D, Left/Right, and the wheel change recipe while W/S and Up/Down move
  through Grid, Runes, Stands, and Aura pages allowed by that recipe tier;
- long original XML descriptions and notes are no longer truncated at the bottom of the first
  page; they are paged with controls and a page counter;
- structure entries use the original A/D yaw and W/S pitch controls; the wheel controls zoom in
  the 3D presentation and the selected layer in the 2D presentation.

`:ChromatiCraft:compileJava -x :ElectriCraft:compileJava` passes after this slice. No GameTest was
run: these changes are client input/render-state behavior, while the existing lexicon persistence
GameTests cover the unchanged server/data-component seam.
## Chromic Lexicon Notebook and Progress verticals — 2026-08-09

Two more V33a guide subsystems are now live in the accepted 26.2 screen:

### Notebook

- ten editable rows are visible at once, with scrolling, append, clear, and explicit Save controls;
- switching away from Notes or closing the lexicon also saves dirty content;
- V33a's reset-then-one-packet-per-line sequence is replaced by one atomic
  `UpdateLexiconNotes` payload, so a partial packet sequence cannot erase half a notebook;
- the server accepts the update only while the lexicon is in the player's main hand, strips blank
  rows, and bounds count, line size, and total stored characters before replacing only the notes in
  `LexiconData`; pages, creative state, blanks, and foreign custom data remain intact;
- `LexiconData.withNotes` is the immutable replacement seam used by both runtime and the focused
  persistence test.

### Progress

- the Progress side tab now exposes separate Tree, Levels, and Stages presentations;
- Levels renders all ten `ResearchLevel` milestones and distinguishes reached from future tiers;
- Stages lists every active `ProgressStage`, its achieved state, and opens an inspectable detail;
- Tree is backed by `ProgressionManager`'s real prerequisite DAG and distinguishes achieved,
  presently available, and locked stages while showing their immediate prerequisites;
- A/D or Left/Right switches presentation; W/S, Up/Down, and the wheel page Tree/Stages;
- `ProgressionDescriptions` securely reads the original V33a `progression.xml`, including exact
  title, hint, reveal, and short description. A locked stage shows its hint and an achieved stage
  shows its reveal, matching the source's information boundary.

Navigation also restores V33a's inactive-entry treatment: a page present in the physical book but
not owned by the player's research is marked with a question overlay, refuses to open, and plays
the source's paired 0.8/1.2-pitch error cue. Creative and always-readable pages remain active.

Focused verification:

```text
.\gradlew.bat :ChromatiCraft:compileJava -x :ElectriCraft:compileJava --console=plain
BUILD SUCCESSFUL

.\gradlew.bat :ChromatiCraft:runGameTest \
  -PgameTestSelector=chromaticraft:lexicon_custom_data_roundtrip \
  -x :ElectriCraft:compileJava --console=plain
1/1 required GameTests passed
```

The Progress Tree is functionally backed by the real DAG but remains a paged compact presentation.
V33a's free-panning node topology, link line styles, per-stage icons, hover descriptions, and exact
layout are still follow-up work, not claimed complete.

## Chromic Lexicon NBT structure viewer — 2026-08-09

The synthetic structure footprints have been removed. Structure guide pages now load the same
compressed, canonical structure-template NBT used by 26.2 placement from the bundled mod data pack,
decode its real palette and block positions, and present it through two interactive views. The
client reads the jar's data-pack resource directly because its normal resource manager indexes
assets, not server data; this avoids maintaining a second, drift-prone template copy.

- 3D isometric view with continuous left-drag yaw/pitch, A/D yaw, W/S pitch, wheel zoom, depth
  sorting, automatic fitting, clipping to the handbook pane, right-click reset, and block hover
  tooltips;
- 2D top-down layer view with 90-degree A/D rotation and W/S, wheel, or +/- layer selection;
- both presentations report the exact template identifier, dimensions, and visible-block count;
- V33a's display-only casting-table, item-stand, pylon-controller, and Data Node relay-column
  substitutions are retained without changing the placement templates themselves;
- entries whose structure has not yet been converted to NBT say so explicitly instead of drawing
  invented geometry.

The current viewer covers all eight canonical templates that have landed: pylon, casting tiers
one through three, repeater, compound repeater, broadcast pylon, and Data Node tower. Remaining
V33a guide structures must gain canonical NBT templates before their previews become available.

This is a faithful NBT/data and interaction port, rendered with scaled block-item models inside the
26.2 GUI submission pipeline. Rendering full world block quads and block-entity renderers inside a
screen remains a later visual-fidelity enhancement and is not claimed here. No GameTest was added
for this client-only viewer; compilation and the in-world interaction checklist are the relevant
verification.

### Chromic Lexicon: scrolling backdrop and V33a movement — 2026-08-09

The guide opened onto a bare frame with no backdrop, and its movement keys stepped one page at a
time. Both were wrong against V33a, and for the same underlying reason: the port had no equivalent of
`GuiScrollingPage`.

**Missing art.** V33a ships 28 Handbook textures; only six had been extracted. The other 22 are now
in `textures/gui/lexicon/`, including `navbcg`/`navbcg2` — the scrolling backdrops — plus `frame`,
`blank`, `handbook_blank`, `misc`, `notes`, `progress` and the eleven specialist page backgrounds
(`handbook_cast`, `handbook_casttune`, `handbook_pyloncast`, `handbook_pyloncast2`,
`handbook_multicast`, `handbook_ritual`, `handbook_ritual2`, `handbook_pool`, `handbook_runes`,
`handbook_structure`, `handbook_adjacency_effects`, `handbook_element`, `handbook_compass`,
`handbook_password`). The specialist pages still need their renderers, but the art no longer blocks
them.

**`LexiconScrollPane`** ports `GuiScrollingPage`. The important detail is that V33a movement is not a
key event: it polls the held movement binds every frame and slides by `max(1, 180 / fps)` pixels,
doubled on Shift and halved on Ctrl, clamped at zero and at the content bound. That is why holding W
in V33a drifts smoothly rather than stepping. It reads the player's own movement binds rather than
hardcoding WASD, so a remapped keyboard still works, and the arrows are always accepted alongside.
The backdrop is one 256x256 tile drawn at `offset % 256` at `(left + 7, top - 1)` over 242x206 —
V33a's `GuiNavigation(BOOKNAV, ep, 256, 220, 242, 206)` — which is what makes it appear to scroll
under the window cut into the frame.

**Movement now owns the movement keys.** The previous pass had W/A/S/D and the arrows consuming
discrete section and page steps in the navigation view. Those fought the pan, so they are gone from
that branch and the vertical pan offset drives the entry list instead: holding S slides the list and
the backdrop together. The mouse wheel now nudges the same offset rather than a separate page
counter, so wheel and keys cannot disagree, and sections moved to PageUp/PageDown beside the existing
tab buttons. Discrete stepping is retained untouched everywhere V33a has no scrolling pane — entry
text pages, casting recipes, and the structure preview.

**Known remaining gap, stated plainly.** V33a's navigation pane is a single *spatial sheet* with every
section laid out across it, which is why upstream computes
`maxX -= paneWidth + sectionSpacing + margin*2` and lets you pan horizontally between sections. The
port's navigation is still a vertical list, so horizontal panning is bounded to zero and only the
vertical axis carries content. Laying the sections out spatially is the next step toward real parity
and is the prerequisite for the original image-button layout and hover animations.

### Chromic Lexicon: the spatial navigation sheet — 2026-08-09

The gap recorded in the previous entry is closed. V33a's navigation is not a paged list; it is one
pannable sheet with every section laid out on it, which is why upstream computes a horizontal bound
at all. `LexiconNavigationSheet` reproduces that layout from `GuiNavigation`:

- sections stack downward separated by `SectionSpacing` (32);
- within a section, entries are grouped by `ResearchLevel` into categories laid left to right,
  separated by `Section.sectionSpacing` (64);
- each category is a grid of 24-pixel cells with 4-pixel spacing and an 8-pixel margin;
- the column count is V33a's `allOneLevel() ? 10 : 4`, so a single-level section becomes one wide
  shallow block while a multi-level section gets narrow per-level columns that read as groups;
- `getSubSectionHeight`/`getSubsectionWidth` are ported verbatim, including the level-title width
  clamp, so box sizes match upstream rather than being re-derived.

Section and category outlines are drawn clamped to the pane exactly as upstream clamps them, so a
half-scrolled box still reads as a box instead of spilling across the frame, and titles and icons are
skipped once they leave the window. Clicking a cell opens its entry and hovering one shows its title,
both hit-tested through the same clipping the renderer uses so the two cannot disagree.

Both scroll axes now carry content: vertical panning moves between sections, horizontal panning moves
across research levels within a section. The pane bounds come from the sheet's own measured extent
minus the visible window, which is V33a's `maxX -= paneWidth + ...` / `maxY -= paneHeight + ...`.

Search results and the stored-fragment list stay a flat set rendered as one synthetic section, since
those are genuinely unsectioned in the port; the per-section layout applies to ordinary browsing.

### Chromic Lexicon: per-page frames and the crafting page — 2026-08-09

**Steps 1 and 2 of the ten-step plan.**

`GuiBookSection.PageType` gives every page kind its own frame art, and `GuiCastingRecipe` overrides
`getGuiLayout()` to return a *different* type per subpage — CAST, RUNES, MULTICAST, PYLONCAST2 — which
maps exactly onto the port's existing Grid/Runes/Stands/Aura subpages. The port had been blitting one
`handbook.png` for every page. It now selects the frame per page, and the casting view changes frame
as you page through its subpages, as upstream does. Structures use `handbook_structure`, and grid
recipes reuse `handbook_cast` because V33a's `PageType.CRAFTING` maps to the same file.

**`GuiCraftingRecipe` needed a different source of truth than upstream.** 1.7.10 handed the GUI an
`ArrayList<IRecipe>` and read `getRecipeOutput()` off it. 26.2 does not expose `IRecipe` to the client
at all — recipes arrive as `RecipeDisplayEntry` records in the player's recipe book, resolved through
a `ContextMap` from `SlotDisplayContext.fromLevel`. The page is therefore built from the same displays
vanilla's own recipe book renders, rather than from a re-derived recipe list, and no server round trip
is needed (unlike the casting recipes, which do request from the server).

Layout is V33a's: the 3x3 at `(posX+54, posY+10)` on an 18-pixel pitch, output at `(posX+7, posY+5)`
with `posX/posY` the frame origin offset by `(-2, -8)`, and the alphabetically sorted `name: xN`
ingredient tally capped at ten rows. A shaped display carries its own width and height, so a 2x2
recipe is placed as 2x2 rather than packed from index 0. Tag ingredients cycle their candidates on a
timer, as vanilla's recipe book does, so an ore-tag slot does not read as one arbitrary item.

The Recipes toggle now appears for entries with a grid recipe and no casting recipe, with prev/next
buttons when an item has several.

### Chromic Lexicon: casting page geometry and the rune map — 2026-08-09

The casting subpages were invented rather than ported, and the offsets were wrong. All three are now
taken from `ChromaBookData.drawCastingRecipe`, which is the authoritative layout that
`GuiCastingRecipe` delegates to.

**Origin.** Both the casting and crafting grids draw against the *plain* frame origin,
`(width - xSize) / 2, (height - ySize) / 2`. The `-8` that appears in `GuiBookSection.drawScreen` and
the `-2, -8` in `GuiCraftingRecipe.drawGraphics` apply only to the page title and the ingredient text
list. The crafting grid added in the previous entry had inherited that shift and was drawn eight
pixels high; that is corrected.

**Subpage 0** is the 3x3 at `(posX+54, posY+10)` on an 18-pixel pitch with the output at
`(posX+7, posY+5)`. The port had a 20-pixel pitch at invented coordinates with a hand-drawn "Casting
Grid" caption and an arrow -- all of which the frame art already provides, so the captions are gone.

**Subpage 1 was a two-column text list of runes and coordinates.** Upstream is
`RuneShapeRenderer`: a top-down 11x11 floor of 16-pixel crystalline-stone tiles centred on the
casting table's top texture, with the recipe's runes laid on it at their true offsets, showing one Y
layer at a time and cycling every five seconds, labelled `y=` at `(midx+93, midy-4)`. That is now
what it draws, so a multi-layer pattern reads as a map you can build from.

**Subpage 2 was also a list.** Upstream places each stand at its real position around the table:
`posX+120 + sign(i)*tx`, `posY+94 + sign(k)*ty`, with `tx = |i| == 2 ? 38 : 64` and
`ty = |k| == 2 ? 38 : 63` -- the projection deliberately pushes the inner ring further out on screen
than its block distance so the outer ring stays legible. The central 3x3 repeats at
`(posX+102, posY+76)`.

Still outstanding on this page: subpage 3's aura list needs `descX`/`descY` from `GuiBookSection`
before its wrap-every-eight-into-120-pixel-columns layout can be matched exactly.

### Chromic Lexicon: the 3D structure viewer — 2026-08-10

The structure page was a spherical projection of item icons; V33a draws real block models. It now
does too.

**Why the port needed new infrastructure.** `GuiGraphicsExtractor.pose()` returns a
`Matrix3x2fStack` in 26.2 — GUI space is strictly two-dimensional, so V33a's `GL11` transform chain
has no equivalent. The only way a rotatable 3D scene reaches the screen is a *picture-in-picture*
element, which renders to its own colour and depth texture with a real `PoseStack` and is then
blitted into the GUI layer. NeoForge exposes it through `RegisterPictureInPictureRenderersEvent` and
`GuiGraphicsExtractor.submitPictureInPictureRenderState`.

That lives in DragonAPI, in `reika.dragonapi.instantiable.rendering.structure`:

- `StructureRenderer` — the controller, holding what V33a's class held: rotation, the current slice,
  and per-position/per-block display overrides, plus `drawSlice` and `tally`.
- `StructureRenderState` — the immutable per-frame snapshot handed to the GUI.
- `StructurePipRenderer` — the renderer. Blocks are drawn as their real models via
  `ModelManager.getBlockStateModelSet().get(state)` -> `collectParts` -> `submitBlockModel`, on the
  translucent or cutout block-item sheet per `hasMaterialFlag(1)`, tinted from
  `BlockColors.getTintSources`.

The old `reika.dragonapi.instantiable.rendering.StructureRenderer` was commented out wholesale; it is
kept, annotated, as the 1.7.10 reference for the hooks and the fake-world `RenderAccess`.

**Two things that are easy to get wrong here.** `PictureInPictureRenderer.getTranslateY` defaults to
`height`, a bottom-centre origin that suits an entity standing on a floor and puts a structure below
the viewport; it is overridden to the middle. And the base already applies `scale(s, s, -s)`, which
leaves model +Y pointing *down* because the GUI's orthographic projection inverts Y — the renderer
rolls 180 degrees about X to finish the job. `renderState.scale()` is pixels per model unit, so it
carries V33a's discrete size tier multiplied by its `s = 12`.

**Deliberate divergence.** V33a scales by `(-d*s, -d*s, -d*s)`, which mirrors the structure in X on
top of orienting it. The port applies `scale(1, -1, -1)` instead — the same step every vanilla
picture-in-picture renderer takes, paired with the projection's reversed near/far — and so does not
reproduce the mirror. Most ChromatiCraft multiblocks are symmetric about X, and a view you can spin
freely hides the difference on the ones that are not. Note that this composes with the base's own
`scale(s, s, -s)`: reason about the two together, never about either alone.

**The additive pass is real.** V33a shades the blocks an upgrade structure shares with the tier below
it using `BlendMode.ADDITIVE2`, which is `glBlendFunc(GL_SRC_ALPHA, GL_ONE)` — exactly
`BlendFunction.LIGHTNING`. No stock render type both accepts block-model geometry and blends that
way, so a NeoForge `PipelineModifier` rebuilds the sheet's pipeline with that blend for the second
pass only. The set of shared blocks is `LexiconStructurePreview.markShared`, mirroring
`GuiStructure`'s switch: casting2 -> casting1, casting3 -> casting2, and pylonbroadcast -> pylon,
which upstream matches on position alone where the casting tiers also require the same block.

**Viewport.** The 3D view renders over the whole screen. V33a translates to the *screen* centre, not
the page's, and sets no scissor, so a tall structure deliberately overflows the book; the page window
is 242x181 and the pylon alone stands about 229 pixels once tipped by the default -30 degrees, so
anything page-sized would clip it. Only the page reacts to a drag, though — upstream's unconditional
`Mouse.isButtonDown(0)` spins the model even while you are pressing one of its own buttons.

**Page chrome.** The entry title was centred at `top+18` with an invented section subtitle beneath
it; `GuiBookSection.drawScreen` draws it left-aligned at `(posX + getTitleOffset(), posY + 6)` with
`posY` already shifted up eight, i.e. `(left + 6, top - 2)`, in white, and draws no subtitle at all.
That is corrected for every entry page — on the structure page the subtitle had been sitting exactly
where `GuiStructure` puts its size caption.

**Screen side.** `GuiStructure`'s geometry, verbatim: plain `3D`/`2D` 20x20 buttons at `j+185` and
`j+205, k-2`, the `N#` block-tally mode at `j+165` (`j+125` while slicing) with its `+`/`-` stepper
at `j+165`/`j+145`, and the `(XxYxZ)` caption at `j+6, k+10`. Upstream suppresses the tally button
for a `FragmentStructureBase`; the condition is in place but its set is empty, since no fragment
structure has a modern template yet. Left-drag spins the model, right-click
resets it, and A/D/W/S are polled every frame while held rather than consumed as key events, which is
what makes the spin continuous. The invented zoom, the arrow-key 15-degree steps and the `3D ✓`
labels are gone.

### Chromic Lexicon: the structure viewer's block-entity pass — 2026-08-10

V33a runs a TESR loop over the structure so pylons, repeaters, casting tables and data nodes render
animated rather than as bare models. That is in now.

`StructureRenderer` builds one stand-in `BlockEntity` per position whose block is an `EntityBlock`,
caches it, and gives it the client level exactly as upstream gives its instances `theWorld`. The
render states are extracted in `draw3D` — the GUI's *extract* phase, which is where vanilla extracts
block entities too — and the picture-in-picture renderer only translates to each position and
submits. That split matters: the world-reading code a renderer needs to be kept away from runs during
extract, not submit.

Two things a stand-in gets wrong unless you handle them:

- **Light.** `BlockEntityRenderState.extractBase` falls back to full brightness only when the block
  entity has no level. These have one, so it samples the real world at the structure's *local*
  coordinates — usually inside terrain, so the preview renders black. Every extracted state is
  overwritten with `LightCoordsUtil.FULL_BRIGHT` afterwards. This is the same defect recorded for the
  RotaryCraft machine renderers.
- **World reads.** A stand-in's position is not where the structure is. V33a's answer is a static
  `isRenderingTiles()` that renderers consult, and that is ported and set across the extract loop.
  `RenderCastingTable` is the first consumer: its rune animation scans neighbouring blocks for
  engravings, which in the guide would have decorated the table with whatever happens to stand at
  those coordinates.

The stand-ins are never ticked, as upstream's are not, so a renderer animated from a tick counter
stands still while one animated from wall-clock time moves. `RenderCrystalPylon` and
`RenderCrystalRepeater` are the latter, which is why the pylon still pulses.

`addBlockHook` now takes a supplier rather than a fixed state, because upstream's hooks are not
constant: `GuiStructure.getElementByTick` walks the sixteen elements on a four-second cycle so a rune
in the guide reads as "any rune" instead of the one the template happens to contain. All sixteen rune
blocks are hooked, since the templates were generated with the black one as a placeholder.
`addBlockEntityHook` is the equivalent for the block entities, for display state an unplaced instance
would not have.

Also fixed here: the data-node relay substitution moved the icon to `DATA_NODE` but left the block
state as `DUMMY_AUX`, which is deliberately invisible — so the relay column showed in the flat view
and vanished from the 3D one.

**`addEntityRender` is ported; `addRenderHook` deliberately is not.** Some structures are defined
partly by entities -- upstream's dimension portal is eight ender crystals on a bedrock ring, and
without them that page shows a ring and nothing else. `StructureRenderer.addEntityRender` takes any
entity, ticks it once per frame as upstream's `onUpdate()` does, extracts its render state alongside
the block entities and draws it half a block in and three eighths up. Positions are in the template's
own coordinates, unlike upstream, which stores entities relative to the structure midpoint while its
block loop is not — one space is less error-prone than two.

`addRenderHook` is a different matter: in V33a it is **write-only**. `GuiStructure` registers a
`PylonRenderHook`, but `renderHooks` is read in exactly one place, and that place is inside the block
comment around the old item-icon `draw3D`. Its `getScale`/`getOffsetX`/`getOffsetY` are pixel nudges
for a 2D icon layout that no longer exists even upstream. Implementing it against real block models
would add behaviour V33a does not have, so it is not implemented, and `BlockRenderHook` should not be
resurrected without a live consumer to justify it.

Neither hook has a ChromatiCraft consumer yet: the portal structure has no modern NBT template and
`EntityChromaEnderCrystal` is still a raw 1.7.10 file outside the build.

### Chromic Lexicon: the machine render — 2026-08-10

`GuiMachineDescription` draws the construct itself, slowly turning, to the right of the page title.
The port drew a static inventory icon plus a hand-written caption table ("Crystal network construct",
"Casting-system construct") that upstream has nothing corresponding to; that table is deleted.

`LexiconMachineRender` rides the structure viewer's picture-in-picture element, so the machine's
block-entity renderer runs and a construct that is mostly its renderer does not appear as a bare
cube. It is deliberately *not* routed through `StructureRenderer`: none of that class applies here —
no size tiers, no slice, no tally, no hooks — and upstream's numbers are different in every respect.
Anchor `posX+167, posY+44`, scale a flat 48 pixels per block, yaw from `nanoTime()/20000000 % 360`,
pitch starting at 22.5 and draggable to +/-45, and the model lifted by `8*sin(|pitch|)` as it tips.

The pivot is `(0.5, 0, 0.5)`: upstream passes `(a, 0, b)` with `a = b = -0.5` to
`renderTileEntityAt`, so the model turns about its own centre horizontally but about its base
vertically. Upstream also carries a per-machine table of other `a`/`b` values and extra translates
(LUMENWIRE, FLUIDRELAY, TELEPORT, CHROMACRAFTER, PERSONAL and the `needsRenderOffset` group); those
are not transcribed yet because none of those machines has a page in the port, and inventing values
for the ones that do would be worse than using upstream's default.

To submit at a fixed point on the page, `StructureRenderState` gained an `offsetX`/`offsetY` in GUI
pixels, applied *outside* the rotation so it slides the model across the viewport rather than moving
what it spins about. The structure viewer passes zero, which is upstream's screen-centred behaviour.

The pitch drag sign sits in a single named constant, `LexiconMachineRender.PITCH_DRAG`, because this
page and the structure page have the same drag and the structure page's sign correction is still
unconfirmed in game. One edit should fix both.

The other three subpage kinds are blocked rather than skipped; `TODO.md` step 3 records what blocks
each. AOE is worth singling out: it is not merely unported but *unreachable*, since `ComplexAOE` and
all four of its implementors are outside the build allowlist, so no machine can enter that branch.

**Correction (2026-08-10):** the first version of this entry listed DragonAPI's `Proportionality` as
unported and claimed 26.2 GUI space has no filled-sector primitive. Both were wrong. `Proportionality`
and `CircularDivisionRenderer` are both ported and draw through
`SubmitNodeCollector.submitCustomGeometry`. The error came from a `find` invocation whose shell had
drifted into `ChromatiCraft/`, so DragonAPI was never searched — the same working-directory hazard
that has now produced two wrong conclusions in this repo. Prefer absolute paths for cross-submodule
searches.

### Chromic Lexicon: navigation, tabs and the progress screen — 2026-08-10

In-world screenshots turned up four separate defects.

**The tabs were unclickable in a 54-pixel band.** Items and Recipes are both 13x88, at `k-7` and
`k+27`, so they overlap by 54 pixels. 26.2 walks a screen's children and the first whose
`isMouseOver` passes takes the click, so the add order decides who owns the shared band — which is
why upstream adds the *inactive* tab first. The port added them unconditionally in one order. Worse,
the `v` was inverted: `v=4` is the raised active look and `v=95` the recessed one, and the port
handed Items `v=4` exactly when Items was inactive. Both are fixed, and Search is no longer offered
in recipe mode, matching upstream's two branches.

Same class of bug: Save & Exit sat at `left+WIDTH, top+5`, on top of Progress and Recovery, and was
added last — so it could never receive a click at all. It moves below the other three.

**The section headers floated outside the frame.** V33a draws the scrolling backdrop at
`leftX+7, topY-1` but lays the sections out from `leftX+11, topY+11` — two different origins. The
port used the backdrop's for both. `drawSections` starts at `dy = y+1` and draws each title at
`dy - FONT_HEIGHT`, so with `topY-1` that landed eight pixels above the frame. The sheet's own
clipping gates were already faithful; the origin was the whole bug.

**The navigation frame was the wrong art** — `GuiNavigation.getBackgroundTexture()` is
`navigation2.png`, not `navigation.png`.

**Two invented captions overlapped.** The port drew a centred "Chromic Lexicon" and a section name at
`left+116`, on top of each other. V33a's navigation screen draws neither: the frame art carries the
book's identity and each section labels itself inside the sheet.

**The progress screen was not a port at all.** It was a scrolling list of text buttons with a
`"Tree ✓"`-style label row, a hand-written "Green: reached  Gold: available  Red: locked" legend, and
a third "Stages" mode. Upstream has exactly two views and no legend: `GuiProgressStages` is a
*panned field of 20x20 nodes* with lines drawn to each node's prerequisites, a Return tab at
`j+xSize, k` and two 13x35 mode tabs at `j-13, k-7` and `j-13, k+27`. A node's border says where the
player stands — green reached, yellow available, red locked — pulsing on a per-stage phase so the
field does not blink in unison. `LexiconProgressGraph` is that, panned by the same
`LexiconScrollPane` the navigation sheet uses. The per-stage description moved from a separate detail
page into the hover tooltip, which is where upstream puts it.

One adaptation, marked at the site: the by-level view orders by prerequisite count. Upstream orders
by the earliest research level requiring the stage, which lives in `ChromaResearchManager` — not
ported.

**The recipe viewer is blocked, and the cause is not layout.** `craftingRecipes()` reads
`player.getRecipeBook().getCollections()`, and a recipe book only contains recipes the server has
awarded the player. ChromatiCraft never awards its recipes, so the page is empty for essentially
everything. The client has no other source: `ClientPacketListener.recipes()` returns a
`ClientRecipeContainer` holding only item sets and stonecutter recipes, so full crafting displays
genuinely are not client-side data in 26.2. The fix is the pattern the casting page already uses —
request the display from the server and cache it — which is its own slice, not a layout correction.

### Chromic Lexicon: the crafting recipe page, unblocked — 2026-08-10

The page read `player.getRecipeBook().getCollections()`, which only holds recipes the server has
*awarded*. ChromatiCraft awards none, so the page was empty for essentially every entry. The client
has no fallback in 26.2 either: `ClientPacketListener.recipes()` returns a `ClientRecipeContainer`
carrying only item sets and stonecutter recipes, so a grid recipe genuinely is not client-side data
until it has been unlocked.

The fix is the arrangement the casting page already used: `RequestGuideCraftingRecipes` goes up,
`GuideCraftingRecipes` comes back, and the screen caches it per item and asks once. The payload is
vanilla's own `RecipeDisplayEntry` — it has a `STREAM_CODEC` and it is exactly what the page's
renderer already consumed, so nothing about the layout changed.

Server side, `ChromaNetwork.guideCraftingRecipes` walks `RecipeManager.getRecipes()` for
`CraftingRecipe` holders, expands each through `listDisplaysForRecipe`, keeps the shaped and
shapeless grid displays, and then narrows to those whose resolved result matches the item. It takes a
`Level` rather than a connection so the selection stays directly testable.

Note the alternative that was *not* taken: awarding the recipes to the player would also have filled
the page, but it changes game state — the vanilla recipe book would light up with the whole mod — so
the guide asks instead of unlocking.

### The Elemental Manipulator HUD — 2026-08-10

`ChromaOverlays.renderElementPie`, the wheel that appears while a Manipulator is in hand. It is the
display half of the pylon-charging vertical: extraction fills `PlayerElementBuffer`, this reads it.

It is deliberately **not** a pie chart, and it is not a `Proportionality` render despite the shape.
Every element owns a fixed 22.5 degree wedge and the *radius* carries the fill, so a colour never
moves — you learn where each element sits and read the whole buffer as a silhouette. The radius is
`pow(amt/cap, 0.675)`, not linear, which keeps a nearly-empty element visible. Shift enlarges the
wheel from 32 to 48, and `ChromaOptions.PIELOC` picks its corner.

The wedges and the black spokes need filled geometry, which GUI space has no primitive for, so they
go through a picture-in-picture element (`ElementPieRenderer`) with `scale = 1` — one model unit is
one GUI pixel, so the geometry is written in the same coordinates upstream uses. The two wheel plates
are ordinary blits at twice the radius and stay outside it. Unlike the structure viewer there is no
orientation step: the wheel is flat and the base pass already leaves +X right and +Y down, which is
the handedness V33a's GUI space has, so the angles are transcribed unchanged.

`wheelback_2.png`, `wheelfront2.png` and `infoicons.png` were extracted from the V33a jar into
`textures/gui/hud/`.

One documented gap: upstream rings the wheel with each element's outline rune at `0.8125*r`. Those
came from `CrystalElement.getOutlineRune`, part of the 1.7.10 icon system stripped when
`CrystalElement` was ported, so the glyph ring returns with the rune sprite redesign.

Also settled: the structure viewer's flipped pitch was confirmed correct in game for both the held
keys and the drag, so the machine page's `PITCH_DRAG` keeps the same convention. The comments no
longer hedge — though neither ever claimed a verified mechanism for why upstream's sign had to move.

### Chromic Lexicon: the machine ENERGY subpage — 2026-08-10

A construct that costs lumen energy gets a second page: a 64-pixel supply badge from `infoicons.png`
centred low, and a proportional wheel of the elements it draws at `(posX+xSize-32-50, posY+42)`,
radius 32, ringed white then black. It is reached with W/S like every other subpage and is only
offered when there is a cost to show.

That wheel is a real pie — angles carry the shares — and is DragonAPI's `Proportionality` used as
designed, `setGeometry` then `render`. It needs a `SubmitNodeCollector`, so it rides a
picture-in-picture element at `scale = 1`. Worth keeping straight: this is a different shape from the
manipulator HUD's wheel, where the *radius* carries the value and the angles are fixed.

**How the supply type is detected.** Upstream branches on `ChromaTiles` predicates
(`isPylonPowered`, `isRelayPowered`, `isChargedCrystalPowered`). The port tests the stand-in block
entity the machine render already builds — the same question asked of the object rather than of a
parallel enum, and it needs no new registry surface. Only the pylon-powered branch is live; the other
two read `getRequiredEnergy()` off `ChargedCrystalPowered` and `TileEntityRelayPowered`, two abstract
block-entity bases still outside the allowlist, and are marked CHROMA-PORT at the site.

The pylon-powered display is illustrative rather than a true cost even upstream: it shows which
elements the construct conducts, breathing on a sine so the wheel is never static.

For the record, the earlier claim that this page was blocked on `Proportionality` was wrong twice
over — it was already ported, and `infoicons.png` has since been extracted alongside the HUD wheel
art. The only real blocker was the two block-entity bases.

### The element wheel's spokes crashed the frame — 2026-08-10

`IllegalStateException: Missing elements in vertex` out of `BufferBuilder`, from the HUD wheel's
spoke pass. `RenderTypes.lines()` binds `POSITION_COLOR_NORMAL_LINE_WIDTH`; a vertex carrying only
position and colour is rejected, and it takes the whole render frame with it.

The crash log is also the evidence for the fix: the wedges are submitted on `RenderTypes.debugQuads()`
in the *same element* and rendered fine — only the second lambda failed. So the spokes moved onto the
quad path too, drawn two pixels wide to match upstream's `glLineWidth(2)`, which keeps the element on
one render type and off a format that needs four attributes.

`ReikaRenderHelper` shows the other route works if you feed it properly: `.setColor(...)` followed by
`.setLineWidth(...)`. `Proportionality` and `Spline` were both missing that and would have failed
identically — `Proportionality` matters because the machine energy page put it on a user-visible
path, where its `drawSeparationLines` branch was a crash waiting for the first caller to enable it.
Both are fixed in DragonAPI.

Worth generalising: a compile-clean `submitCustomGeometry` lambda proves nothing about vertex format.
The format is a runtime contract of the render type, and the only way to find a mismatch is to run it.

### Casting recipe page: five defects from a screenshot — 2026-08-10

**The subpage button row was invented.** A "Grid / Runes / Stands / Aura" strip across the bottom,
where V33a turns those pages with W/S like every other book page. `moveRecipeSubpage` already did
that, so the row was pure addition. Removed.

**The rune tiles were a path that does not exist.** `textures/block/runes/real/tile4_<ordinal>.png`
— there is no `real/` directory and no `tile4_N` naming. The tiles are `tile<element>_0.png`, and the
set the casting floor wants is `engraved/`, which is 16x16 and so matches the layout's tile size
exactly. `frontpng/` is the animated 16x1024 strip and would have been badly wrong even if the path
had resolved.

**The result was drawn twice.** Once at `left+20, top+48` and again at `left+7, top+5`. Upstream
draws it once, in the frame's own slot at `posX+7, posY+5`; the first was invented and is what looked
misplaced. Removed.

**The ingredient cycle ran at frame rate.** `guiTick / 20` — but `guiTick` counts *rendered frames*,
not ticks, so a tag ingredient cycled about three times a second at 60fps instead of once. It reads
wall-clock now, one second per candidate, which is what vanilla's own recipe book does. Worth
remembering that `guiTick` is a frame counter: it is correct for the hover and search ramps that use
it, and wrong for anything that means to measure time.

**The tabs could not be clicked accurately, and the earlier fix was not enough.** Items and Recipes
are both 13x88 but sit only 34 pixels apart, so each hides 54 pixels of the other. Reordering them
cannot fix that — whichever is added first still owns the whole shared band, and the band is most of
both tabs. `LexiconImageButton` now takes a `clickHeight`, so Items only claims the 34 pixels you can
actually see while still drawing full height. With hit-testing settled explicitly, the add order is
free to do what upstream uses it for: the inactive tab goes first so the active one draws over it.

### Chromic Lexicon: specialist descriptions and Basic Info — 2026-08-11

The post-Claude audit checked the 2026-08-10/11 guide commits against V33a and retained their useful
26.2 conversions. One behavioral loss was found: the first ENERGY implementation assumed it was
always machine subpage one, displacing authored NOTES. The machine page list now follows the source
for active content: MAIN, NOTES when the XML has them, then ENERGY when the rendered block entity
reports a lumen requirement. The original right-edge arrows and W/S movement traverse that list,
and every subpage retains long-text pagination.

`GuiToolDescription` and `GuiCraftableDesc` no longer share the invented 16px icon-and-caption
header. Tools render their registered display variant four times size at V33a's `(132, 4)` frame
offset and cycle every two seconds. Other Blocks use the real rotating 48px block-model/BER path.
Resources use the four-times-size item presentation and cycle every second. `LexiconIconResolver`
now returns the complete currently registered variant set for elemental stones, crystals, runes,
dye leaves, crystal/potion lamps, berries, shards, dusts, groups, cores, energized cores,
iridescent crystal, alloys, and all sixteen independently registered crystalline-stone blocks. The
lists are transcribed from `ChromaResearch.getItemStacks`; missing identities remain missing instead
of being replaced by superficially similar blocks.

The generic description baseline was corrected too: text begins at frame top +80 (`posY+descY` in
V33a), the invented `Research: ...` line is gone, and authored tool notes are a real subpage rather
than being concatenated onto the description. Ability art is restored to `(103, 11)` at 50px.

The first `GuiBasicInfo` specialist slice is active. ELEMENTS has its original element frame,
seventeen pages (overview plus all sixteen colors), exact `elements.xml` prose with authored name
substitutions, and the corresponding 64px glow rune. CRYSTALS uses the real rotating cave-crystal
models, PYLONS restores the 96px additively blended color-cycling flare, and SKYPEATER restores its
four-times-size item presentation. Generic Basic Info entries no longer receive an invented icon.

The Handbook image tabs now use ChromatiCraft's registered GUICLICK and GUISEL cues at V33a's
one-third and two-thirds volumes rather than vanilla's button click. Remaining temporary ordinary
buttons still need conversion to the image-button sheet before that sound pass is globally complete.

Focused verification after every slice:

```text
.\gradlew.bat :ChromatiCraft:compileJava -x :ElectriCraft:compileJava --console=plain
BUILD SUCCESSFUL
```

No GameTest was run: these changes are client-only rendering, input, and immutable XML/resource
loading. The relevant acceptance gate is the guide-page in-world checklist.

### Crystal inventory parity and decoded fragment icons — 2026-08-11

The shared cave-crystal item renderer's colour payload and generated per-colour models were already
correct. The remaining white lamp/potion result was submission order: their fully opaque stone
plinth was drawn after the translucent element mesh on the same 26.2 item target. The renderer now
submits the plinth first and the alpha-220 spike mesh second. Cave crystals, crystal lamps and potion
(`super`) crystals therefore retain one shared geometry/tint/translucency contract without
reintroducing metadata or folding the sixteen concrete registry identities together.

Lumen-encrusted crystal items no longer use a cube fallback. Their special model transcribes V33a's
inventory path: a player-and-element-seeded six-by-six field, twelve placement attempts, source
random-roll order, 0.2..0.8 peg heights, and the original 85% element/white mix. It uses the
translucent block-atlas item target, preserving the encrusted sprite's transparent pixels. Datagen
emits a colour-bearing special model for every independently registered encrusted-crystal item.

Decoded Information Fragments now have their V33a inventory presentation. A custom item-model codec
keeps the paper sprite as its ordinary base, resolves the decoded catalog entry through the same
`LexiconIconResolver` used by the guide, and nests that real item/block/special render state at half
size. Holding either Shift key in GUI context suppresses the paper and presents the page icon at full
size (the FRAGMENT self-page retains its source overlap behavior). Non-GUI held, ground and frame
contexts remain the ordinary paper sprite, matching the old callback's inventory-only gate.

Focused verification:

```text
.\gradlew.bat :ChromatiCraft:compileJava -x :ElectriCraft:compileJava --console=plain
BUILD SUCCESSFUL
.\gradlew.bat :ChromatiCraft:runClientData -x :ElectriCraft:compileJava --console=plain
BUILD SUCCESSFUL (18 affected client assets written)
```

No GameTests were repeated because this slice changes only client model codecs, generated model JSON
and rendering. It requires an inventory/hand/dropped-item visual acceptance pass.

### Held pylon charging, worldgen presentation, and V33a large Rainbow Tree — 2026-08-11

Six user-reported presentation/behavior regressions were audited directly against V33a.

The Elemental Manipulator no longer drains a pylon merely because its carrier looks at it.
`ItemManipulator` restores the original 72,000-tick bow-style use action and performs its 24-block
`ChargingPoint` ray trace from `onUseTick`; releasing right click stops charging. The not-yet-ported
REACH ability remains explicitly forward-referenced for the source's 96-block extension.

Glowing Leaves now reproduce `GlowTreeRenderer`: biome-tinted ordinary leaves form the base, then
the existing animated glow strip is drawn as an unshaded full-bright overlay. Firestone's old
1.7.10 netherrack-baked underlay was replaced with the modern vanilla netherrack sprite without
changing its animated overlay cutouts. Energized Rock, Elemental Stones, and Firestone inventory
forms now use a special two-pass cube renderer, so their animated overlays and full-bright emission
survive item rendering too.

The Loot Chest cube fallback is gone. Its exact V33a 64x64 chest texture was recovered from the
published V33a artifact, and its body, hinged lid, and knob cuboids/UVs were transcribed into the
26.2 model layer. The block entity follows the vanilla lid-controller/openers-counter lifecycle,
and both the world block and item use the real chest model.

Rainbow Tree generation now uses a canonical NBT structure, as required for modern structures.
All 1,020 calls in `RainbowTreeBlueprint` were mechanically transcribed into the generated
10x32x10 `worldgen/rainbow_tree.nbt`: 812 rainbow leaves, 152 vertical logs, 28 X-axis branches,
and 28 Z-axis branches. Runtime substitutes one randomly selected natural overworld log across the
whole template while preserving those authored axes. Clearance checks inspect only NBT cells
through the bounded `WorldGenLevel` (never the backing `ServerLevel`), the original buried roots and
six-by-six footing are retained, and obstruction falls through to the source's one-in-five small
rainbow-tree attempt.

Focused verification:

```text
.\gradlew.bat :ChromatiCraft:compileJava
BUILD SUCCESSFUL
.\gradlew.bat :ChromatiCraft:runClientData
BUILD SUCCESSFUL (8 affected client assets written)
.\gradlew.bat :ChromatiCraft:runServerData
BUILD SUCCESSFUL (rainbow_tree.nbt written)
.\gradlew.bat :ChromatiCraft:runGameTest -PgameTestSelector=chromaticraft:rainbow_tree_shape_and_log
All 1 required tests passed
```

### V33a asset/parity sweep: loot lid, based crystals and resource items — 2026-08-13

The loot-chest animation fault had a second, independent cause beyond the corrected model height.
`ContainerOpenersCounter` broadcasts the open count with `Level.blockEvent`, but the modern loot
chest deliberately extends `Block` (not `BaseEntityBlock`) and consequently did not forward that
event into its block entity. `BlockLootChest.triggerEvent` now performs that delegation, allowing
`TileEntityLootChest.triggerEvent` and its `ChestLidController` to receive both open and close
counts. The submit renderer also applies the captured lid angle inside its deferred geometry
callback; mutating the shared model before submission allowed another chest render to overwrite the
angle. The top/bottom 14x14 lid regions in the original 64x64 entity sheet have been exchanged so
the visible closed top is V33a's intended underside-of-lid artwork rather than its internal face.

The two families of based crystals are no longer fed one invented base. V33a's
`BlockSuperCrystal.getBaseBlock` returns obsidian on every face, so all sixteen potion/super-crystal
blockstates and generated item models now use `minecraft:block/obsidian`. Crystal lamps retain the
smooth-stone/double-slab appearance selected by the old lamp renderer. The shared item renderer now
derives its colour from the same two-stage saturation/dye mix as `BlockCrystal.getTintColor`, then
applies V33a's intentional 80% inventory brightness. It retains alpha 220 as the modern
translucent-item requirement; the old inventory pass was alpha 255 while the old world mesh was
alpha 220 and full-bright, so source-faithful items are intentionally somewhat darker than placed
crystals, but no longer use the previous raw element colour that exaggerated the mismatch.

The flat-item audit found **35 generated model definitions whose referenced ChromatiCraft sprite
did not exist**. They were exactly the split identities of V33a's `ChromaItems.CRAFTING` sheet, not
unknown substitutes. All 35 authoritative 16x16 cells have been extracted from
`Textures/Items/items_resource.png` into their modern registry names. Chromastone
(`complex_ingot`) additionally reconstructs the sixteen-frame strip from `miscanim.png` and keeps
animation metadata; the other 34 are static in V33a. A repeated recursive audit now reports zero
missing models or textures across all 317 generated item definitions and their 256 resolved
ChromatiCraft models.

The adjacent audit deliberately did not invent behavior:

- the normal crystal repeater/skypeater block icon is static in V33a; its motion comes from TESR
  halos and beams. `multirepeater.png` and `weakrepeater.png` are the source textures with animation
  metadata, so a normal repeater receiving no animated face sheet is correct;
- the Ethereal Barrier is V33a's connected four-pixel core with arms to neighbouring barriers or
  sturdy blocks, rather than a full vanilla glass-pane plane. The modern multipart/collision shape
  already reproduces those exact 6..10-pixel core bounds and directional arms;
- `unknown_artefact` is the buried three-quarter-height animated block and already has both a model
  and BlockItem. V33a separately registered the hazardous `ARTEFACT` inventory item with Artefact
  and Fragment variants, placement conversion, inventory damage/armor damage, bombing behavior,
  obfuscated tooltip and client effects. That separate subsystem is still absent and must be ported
  together; aliasing the harmless buried block item to it would lose behavior;
- Basic/Intermediate/Advanced/Ultimate Chassis are V33a crafting-sheet cells 12..15. V33a supplies
  sprites and `ChromaStacks` constants but no recipes/usages and only untranslated
  `chromacraft.chassis0..3` keys, so they appear to be dormant upstream scaffolding rather than four
  functioning machine tiers. Their original sprites now exist, without fabricating recipes;
- there is no core ChromatiCraft block or item named Waterstone. V33a's only `Waterstone` label is
  the **Waterstone Wand Cap** in the disabled Thaumcraft integration. The distinct core crafting
  item `water_ingot` is **Fluidic Essence Ingot**, and its missing source sprite is now restored.

Focused verification only; no unrelated GameTests were run:

```text
.\gradlew.bat :ChromatiCraft:compileJava --console=plain
BUILD SUCCESSFUL

.\gradlew.bat :ChromatiCraft:runClientData --no-daemon --console=plain
BUILD SUCCESSFUL (16 corrected super-crystal item definitions written)

.\gradlew.bat :ChromatiCraft:runGameTest \
  -PgameTestSelector=chromaticraft:loot_chest_lid_event \
  --no-daemon --console=plain
All 1 required tests passed
```

The remaining acceptance work is visual/in-world: held Manipulator charging, leaf layer ordering,
animated item glows, and the Loot Chest's facing/lid/texture. The large-tree geometry itself is now
source-count-checked by the focused GameTest.

### Overworld/Nether structure port: canonical layouts and persistence foundation — 2026-08-11

The original world-structure boundary has been audited as one subsystem. The Overworld family is
`CAVERN`, `BURROW`, `OCEAN`, `DESERT`, `SNOWSTRUCT`, and `BIOMEFRAG`; the Nether-roof family is Hut,
Temple, Maze, Spiral, and Diorama. End and ChromatiCraft-dimension structures are deliberately not
part of this slice.

The complete Nether-roof layout family is now generated as ordinary compressed structure NBT under
`worldgen/nether/`. Temple, Maze, and Diorama are mechanically imported at datagen time from every
literal V33a `world.setBlock` call; Spiral reconstructs the source's four repeated authored layers
and generated cap; Hut reconstructs its loop-built shell. The checked-in NBT, not those pristine old
classes, is runtime authority. The conversion preserves Spiral's accidental always-true cap
condition and Hut's deliberate four missing roof corners rather than silently changing geometry
during a parity port.

Runtime registration exposes the original 200:15:30:24:8 weighted natural selector and five
command-only placed features (`nether_hut`, `nether_temple`, `nether_maze`, `nether_spiral`, and
`nether_diorama`). Loot follows the original chest categories; Maze retains its random Blaze versus
Zombified Piglin swap. Temple and Diorama chests restore their source `NETHERSTRUCT` progression
triggers.

Two shared persistence losses were fixed before expanding further:

- the modern loot chest now persists its `ProgressStage` trigger set through
  `ValueInput`/`ValueOutput` and grants every trigger on legitimate access, matching V33a's
  idempotent access path;
- `NBTStructureLoader.place` now hydrates each template cell's block-entity NBT after placement.
  Previously it discarded that tag entirely, so authored spawner delays/ranges/counts (and any
  future controller state or data-marker payload) could never survive the NBT conversion.

The first Overworld canonical asset was the exact Cavern shell: all 386 authored block calls and two
loot-chest calls are source-count checked at datagen, with independently registered colour identities
for its seven rune/crystal pairs. Its 14x6x11 template reserves the original controller anchor at
`(7,2,5)`. That dependency is now fulfilled; Cavern and the other five Overworld families are
registered through the shared persistent controller and canonical NBT feature seam.

The remaining Overworld audit identifies the behavior that makes direct literal-only conversion
unsafe:

| Family | Source-authored behavior that must accompany its NBT |
|---|---|
| Burrow | per-structure element, UUID-bound key door, furnace and loot-room callbacks, optional rooms, ore/drop weighted caches |
| Ocean | underwater siting, cover/pit trap and timed reset, Creeper spawner programming, widened chest reach |
| Desert | terrain envelope, sand erosion/cactus pass, three mob-spawner roles, proximity crack/open sequence |
| Snow | deterministic crack route and hidden access direction, Wolf spawners, support/snow/adjacent-tree cleanup |
| Biome Fragment | **landed:** persisted puzzle/controller state, delegated puzzle tiles, exact shell/puzzle NBT, biome liquid, terrain sinking/weathering, natural + command placement, focused completion test |

Focused compile after the persistence and Nether runtime work:

```text
.\gradlew.bat :ChromatiCraft:compileJava
BUILD SUCCESSFUL
```

Server datagen successfully wrote the five Nether templates and the Cavern template. The Cavern
provider initially rejected an incorrect expected-count assertion (388 actual source cells, not
402); the assertion was corrected to the measured 386 + 2 contract and the full datagen rerun then
passed. No broad GameTest suite was run.

### Overworld structure controller and live Cavern loop — 2026-08-11

The earlier note that Cavern is deliberately unregistered is now superseded. A dedicated modern
natural-structure controller block/entity is registered and persisted with `ValueInput`/`ValueOutput`.
It carries the six source structure identities, independently registered element identity, triggered
and regeneration state, structure version, Ocean trap timer, both optional Burrow-room flags,
generation-error state, last triggering player UUID, and the source 27-slot non-insertable reward
inventory. The old Java-class-name reflection used for auxiliary puzzle data is intentionally not
copied; Biome Fragment now persists its typed puzzle fields directly on that controller.

Cavern is now a complete NBT-backed runtime feature rather than an inert shell:

- `chromaticraft:cavern` is the command/debug placed feature; `natural_cavern` is the biome-added
  form and is not exposed as the random command seam;
- its controller coordinate is the template's exact `(7,2,5)` anchor;
- natural placement retains the source y=10..49 roll, enclosed/non-liquid cell test, two-high east
  tunnel-exit requirement and eastward tunnel carving;
- the two authored Loot Chests receive dungeon loot and the Cavern progression trigger;
- the hidden controller cache rolls stronghold-library loot and then adds the original
  `1 + rand(4) * (1 + rand(2))` guaranteed Information Fragments without overwriting rolled loot;
- entering the source AABB seals the two-block east entrance with reinforced cloak, plays the trap
  sound, records the player, and grants `ANYSTRUCT` plus `CAVERN`;
- reopening removes both seals and resets trigger ownership.

The natural candidate rate is the 144-block source noise scale expressed as approximately one
candidate per 9x9 chunk area, followed by the original physical siting tests. A future migration to
true `StructureSet` spacing must preserve that density and the 64-block Cavern exclusion distance;
the current feature registration is kept because `/place feature chromaticraft:cavern` is the
required deterministic test seam.

The controller currently has a particle-only block model so it cannot checkerboard while the
source `RenderStructControl` script/flare/shader is ported. The dynamic renderer is explicitly part
of the remaining structure presentation work, not being mistaken for completed behavior.

Focused verification:

```text
.\gradlew.bat :ChromatiCraft:compileJava --console=plain
BUILD SUCCESSFUL
.\gradlew.bat :ChromatiCraft:runServerData --console=plain
BUILD SUCCESSFUL (Cavern configured/placed features and biome modifier written)
.\gradlew.bat :ChromatiCraft:runClientData --console=plain
BUILD SUCCESSFUL (controller particle-only blockstate/model written)
.\gradlew.bat :ChromatiCraft:runGameTest -PgameTestSelector=chromaticraft:structure_cavern_nbt_controller --console=plain
All 1 required tests passed
```

Only the focused Cavern/controller GameTest was run. The Luminous Cliffs missing-
`BiomeFilter.biome()` datagen diagnostics recorded at that checkpoint were subsequently fixed by
adding the biome placement filter to all four injected placed features.

### Live Burrow base and canonical optional annexes — 2026-08-11

Burrow is now the second active NBT-backed Overworld structure. Datagen mechanically transcribes
the exact V33a `BurrowStructure` coordinate exports and refuses to write assets if their source-call
contracts drift:

- `worldgen/overworld/burrow.nbt`: 307 base `setBlock` calls plus the six Loot Chests (313 authored
  placements), normalized around the original controller at template `(3,3,3)`;
- `worldgen/overworld/burrow_furnace.nbt`: 98 authored cells plus two furnace callbacks (100), with
  runtime-owned Heat Lamp cells reserved above the furnaces;
- `worldgen/overworld/burrow_loot.nbt`: 100 authored cells plus the four UUID-door callbacks (104),
  including the separate vanilla key chest and the two deep reward chests.

The base is active through command and natural seams. `/place feature chromaticraft:burrow` treats
the command coordinate as the controller anchor. Natural placement converts the source's surface
coordinate to controller offset `(-5,-8,-2)`, uses the 240-block source scale as approximately one
candidate per 15x15 chunk area, requires a grass surface, preserves the source eight-by-five clear
surface column and three-cube no-lake test, and rejects underground shell cells exposed to air or
fluid. The remaining biome-family/exclusion-distance parity is still to be tightened before calling
natural distribution final.

Each placed base selects one `CrystalElement`, places the independently registered colour-specific
Crystal Lamp at controller offset `(0,-2,0)`, and persists the same element on the controller. All
six Loot Chests receive dungeon loot and `BURROW` progression. The controller cache receives its
library roll plus guaranteed fragments. Entering the source Burrow AABB records the entrant, grants
`ANYSTRUCT` and `BURROW`, and replaces controller offset `(2,1,0)` with the original reinforced
crack transition.

The two optional annex NBT files are canonical but deliberately not selected at runtime yet. Their
callbacks are behavioral dependencies, not decoration: the furnace annex needs the fully ported
Heat Lamp temperature/furnace loop and weighted ore inputs, while the loot annex needs the real
four-cell Chroma Door, one UUID shared by door and key, one-use/stay-open flags, and V33a's weighted
13–20 item cache sorting. Activating either room before those callbacks exist would create an
unsolvable or semantically false structure.

Focused verification:

```text
.\gradlew.bat :ChromatiCraft:compileJava
BUILD SUCCESSFUL
.\gradlew.bat :ChromatiCraft:runServerData --console=plain
BUILD SUCCESSFUL (base, furnace-annex and loot-annex NBT; natural/command registrations written)
.\gradlew.bat :ChromatiCraft:runGameTest -PgameTestSelector=chromaticraft:structure_burrow_nbt_controller --console=plain
All 1 required tests passed
```

Only the new Burrow GameTest was run. It asserts controller/type persistence, per-colour registered
lamp identity, all six base chests, and the real four-tick proximity transition to reinforced crack.
The Luminous Cliffs `BiomeFilter.biome()` diagnostics recorded at this checkpoint were subsequently
fixed. Reactor fluid-recipe diagnostics and the Jade GameTest startup diagnostic remain unrelated
pre-existing output.

### Ethereal Barrier and key dependency — 2026-08-11

The Burrow loot annex's lock is no longer an inert metadata placeholder. `BlockChromaDoor`, its
block entity, and `ItemDoorKey` now form the complete modern UUID loop while retaining V33a's four
independent state flags as named block properties: open, damaging, one-use key, and stay-open.
Connected cells are discovered with the original bounded recursive flood fill, only cells with the
same UUID change state, and the open/close sound pair and delayed close are preserved. The barrier
remains ordinarily unbreakable; placement ownership gates Manipulator SneakPop and rebinding.
Worldgen may bind an unowned component explicitly.
The automatic-key mode is persisted and ticked by one component root: the owner's three-block
proximity and look-direction checks reopen the component, with V33a's adaptive 20–200 tick duration.

The collision/outline and render geometry are the source four-pixel centre with arms toward both
adjacent barrier cells and sturdy structure neighbours. Six connection properties feed a multipart
baked model, so the visible model, selection shape, and collision shape agree; an open component
retains its visible animated mesh but has no collision. The model swaps the original
`door_closed.png`/`door_open.png` strips. The exact Ethereal Key icon was recovered from sprite 15
of V33a's `items_tool.png` in the owner's V33a jar rather than redrawn, and the original English
names (`Ethereal Barrier`, `Ethereal Key`) are datagen-owned.

This completes the loot annex's door/key dependency, but does not change the original generation
order: V33a only rolls the loot annex after a furnace annex was successfully placed. Natural annex
activation therefore remains gated on the Heat Lamp/furnace callback; allowing a loot room without
that preceding room would be a source-semantic change.

Focused verification:

```text
.\gradlew.bat :ChromatiCraft:compileJava --console=plain
BUILD SUCCESSFUL
.\gradlew.bat :ChromatiCraft:runClientData --console=plain
BUILD SUCCESSFUL (14 multipart barrier pieces, blockstate, item models and language written)
.\gradlew.bat :ChromatiCraft:runGameTest -PgameTestSelector=chromaticraft:chroma_door_uuid_key_loop --console=plain
All 1 required tests passed
```

Only the new barrier test was run. It covers shared UUID propagation across four cells, matching
key interaction, one-use consumption, scheduled component closing, and automatic owner reopening.

### Heat Lamps and live Burrow annex chain — 2026-08-11

The earlier Burrow-ledger statement that both annexes are intentionally inactive is now
superseded. The source dependencies have landed and natural Burrows execute the original gated
sequence: a 50% furnace-room roll, physical-placement validation, then (only when that room really
placed) a second 50% loot-room roll and validation. The controller persists the two outcomes
independently.

Heat Lamp is a real six-direction attachable block/entity rather than a metadata shell. Hot and
cold lamps have separate registry identities, exact source bounds (20..615 C and -60..15 C),
support loss, persistent target temperature, `ThermalTile` transfer, ReactorCraft reactor-core
exclusion, and the source fuel-free furnace assistance above 200 C. The slotless temperature GUI
uses the recovered V33a `heatlamp.png`; its bounded server packet updates only the open lamp. Both
variants have source-proportioned directional models. The original Tinkers/Railcraft/IC2 hooks are
not falsely simulated because those integrations are absent from the 26.2 runtime, and the
Thaumcraft/Automagy branch remains omitted per project-owner instruction.

The furnace annex uses the canonical `burrow_furnace.nbt`, restores both south-facing furnaces,
their upward Heat Lamps at source-random 50..160 C, and the V33a weighted common-ore input selection
through modern `c:ores/*` tags. Rare/scarce/scattered stack caps remain 8/24/40.

The cache annex uses `burrow_loot.nbt` and creates one UUID shared by all four Ethereal Barrier
cells and the key hidden at controller offset `(3,1,1)`. The barriers retain the source
consume-key/stay-open flags. Both reward chests use the data-generated
`chromaticraft:chests/burrow_cache` table: 13..20 weighted draws transcribed from V33a, including
all sixteen independently registered shard and Cave Crystal identities. On first unpack, equal
stacks are collated and sorted, block drops begin in slots 0..26, and non-block items begin in
slots 27..53, preserving the source chest-half organization. Separate biome-conditioned pools
retain the source 25% cold-biome ice bonus and 1/3 dominant-tree sapling bonus for the modern
vanilla biome families. The old conditional extra
Thaumcraft blaze-powder entry is intentionally absent.

Focused verification (the broad suite was not rerun):

```text
.\gradlew.bat :ChromatiCraft:compileJava
BUILD SUCCESSFUL
.\gradlew.bat :ChromatiCraft:runClientData --console=plain
BUILD SUCCESSFUL (hot/cold directional assets, item models, GUI language)
.\gradlew.bat :ChromatiCraft:runServerData --console=plain
BUILD SUCCESSFUL (canonical annex NBT and Burrow cache loot table)
.\gradlew.bat :ChromatiCraft:runGameTest -PgameTestSelector=chromaticraft:heat_lamp_temperature_furnace_loop --console=plain
All 1 required tests passed
.\gradlew.bat :ChromatiCraft:runGameTest -PgameTestSelector=chromaticraft:burrow_cache_loot_halves --console=plain
All 1 required tests passed
```

The server-datagen Luminous Cliffs missing-`BiomeFilter.biome()` diagnostics recorded at this
checkpoint were subsequently fixed. The Reactor/Jade startup diagnostics remain pre-existing and
unrelated to this slice.

### Ocean and Desert fragment structures — 2026-08-12

The active overworld structure family now includes Ocean and Desert alongside Cavern and Burrow.
Both structures ship as canonical generated NBT transcribed directly from their V33a
`FilledBlockArray` sources; the legacy Java layouts remain datagen inputs only. Ocean is a
31x13x31 template plus a separate repeatable NBT pit slice, and Desert is the exact 15x13x16
template. Command seams are `/place feature chromaticraft:ocean` and
`/place feature chromaticraft:desert`; independently rare `natural_*` features retain the source
640- and 440-block noise-scale densities and biome/site gates.

Ocean retains all eight jungle-temple loot chests, both Creeper spawners (8-block activation,
16 nearby cap, 400-tick maximum delay), widened entrance-chest reach, flooded end validation,
ocean-bound corners, and the cave-connected five-wide shaft. Its controller parity was corrected:
proximity cracks the two distant 5x3 cover panels, while a hit in the funnel opens only the source
3x3 pit cover at y-3 and reseals it after forty ticks. The focused
`structure_ocean_nbt_trap` GameTest passes and covers this complete loop.

Desert retains twelve desert-pyramid loot chests, five programmed spawners (one Blaze, two Spider,
two Silverfish), structure version one, sandy/non-badlands siting, and the source controller anchor.
Its exact NBT and natural/command registrations are generated and compile cleanly. The focused
`structure_desert_nbt_controller` GameTest also passes, covering all twelve chests, all five
spawners, and the explicit controller identity. No broad GameTest suite was rerun.
### 2026-08-12 — Snow Temple and puzzle-block foundation

- Ported V33a `BlockTrapFloor` as `chromaticraft:trap_floor`: four explicit disguise states,
  source 7/8-height collision, Manipulator cycling, delegated fluid/block hazards, and the original
  four-block reinforced-shield support lock. Its disguises are blockstate modes, not reconstructed
  content metadata.
- Ported V33a `BlockShiftLock` as `chromaticraft:shift_lock` with all sixteen source passability
  states, hidden shield faces, directional 1/8-inset collision, entity-inside escape behavior,
  breakable-only mining, and paired open/closed transitions. Generated models preserve the animated
  open/closed faces and per-face structure-stone disguises.
- Added canonical `worldgen/overworld/snow.nbt`, transcribed from every authored `getBaseStructure`
  and `getAirSpaces` cell in `SnowStructure.java`: 17x15x17 bounds, eleven loot chests, trap floors,
  lava-rock hazards, all thirty-six concealed Shift Locks, and the three intended Wolf spawners
  (the V33a placement line had accidentally substituted shield blocks even though its complete
  spawner-programming branch targets Wolves).
- Added command/debug `/place feature chromaticraft:snow` and natural `natural_snow` generation in
  snowy biomes. Natural placement restores the source 480-block planning scale (about 1/900 chunks),
  four-corner same-biome/height/support validation, five-block missing support, and snow cover.
- Restored the exact controller transition: twelve center-path `CRACKS`, one deterministic four-cell
  roof `CRACK` group, and one deterministic 3x3 concealed directional Shift-Lock route. The RNG is
  seeded from the modern `WorldLocation`-equivalent dimension/position hash and retains V33a's
  deliberately discarded first long.
- Snow chests use stronghold-corridor loot and grant `SNOWSTRUCT`; the controller keeps the structure
  identity and guaranteed information-fragment reward.
- Verification: `:ChromatiCraft:compileJava` and `:ChromatiCraft:runServerData` pass. Focused
  `chromaticraft:structure_snow_nbt_route` GameTest passes (11 chests, 3 spawners, 36 initial locks,
  exact 12-cell crack path, exactly one 9-cell route). Existing unrelated ReactorCraft fluid-component
  and Jade loot-registry warnings remain non-fatal in the focused server.

### 2026-08-12 — Biome Fragment, broadcast repair, and repeater presentation

- Biome Fragment is now the sixth live NBT-backed Overworld fragment structure. Its exact 15x14x15
  shell, triggers, eight caches, dual randomized lock/key channels, colour-specific rune/lamp
  substitutions, biome liquid, melody replay, central door, natural sink/weathering, entrance
  cleanup, and special-biome generation are active. The focused
  `chromaticraft:biome_fragment_nbt_completion` test passes the complete melody/unlock loop.
- Pylon Broadcast no longer targets the stale invented `chromaticraft:chroma` identifier. Its
  canonical NBT and matcher use the registered `chromaticraft:liquid_chroma` block. The focused
  `chromaticraft:pylon_broadcast_template_contract` test passes activation, solid-cell obstruction,
  ordinary-LOS restoration, liquid-cell repair, and reactivation.
- The remaining V33a repeater presentation families have been transcribed onto the active 26.2
  particle layer: rain-loss flare seeds, paired enhanced-stalk blurs, per-tick surge sprays, the
  256-particle signed-gravity destruction burst, both final break sounds, and the compound
  repeater's phase-correct colour rune. The final destruction burst is a typed client payload;
  surge start restores the source non-attenuated `REPEATERSURGE` cue.
- Verification boundary: the broadcast lifecycle test and preceding compile passed. The execution
  service originally rejected the compile for the repeater presentation patch because its allowance
  was exhausted. That stale boundary was cleared on 2026-08-13: `:ChromatiCraft:compileJava` now
  passes with the complete presentation patch included. The focused
  `chromaticraft:repeater_overload_destroys_stalk` test was not rerun during the unrelated Heat Lamp
  repair, so its last recorded behavioral result remains the applicable one.

### Heat Lamp menu/item repair and full crystal-family tint registration — 2026-08-13

- The Heat Lamp menu's server `DataSlot` was already authoritative, but the screen copied the
  client block entity's construction default into its edit box once during `init()` and never
  observed the later menu update. `ScreenHeatLamp.containerTick()` now mirrors each newly delivered
  server value into the editor under a responder guard. Reopening either lamp therefore displays
  its persisted configured temperature instead of hot-lamp default `20`, while server clamping is
  also reflected back into the field.
- Hot and cold items retain V33a `BlockAttachableMini#setBlockBoundsForItemRender()` parity: the
  east-facing attachment geometry is still the item model. Their twelve generated directional
  models now inherit `minecraft:block/block`, restoring the standard block-item display/camera
  transforms that the legacy inventory renderer supplied. This is deliberately scoped to Heat
  Lamps; no Chroma Door models or other cuboids changed.
- The shared dynamic crystal mesh already emits tint index zero, and all Cave Crystal, Crystal Lamp,
  and Super/Potion Crystal colours remain separate block/item registry identities. The client tint
  source had only been registered against the Cave Crystal list, leaving the other two families
  white in-world. One `CrystalBlock` tint source now covers all three independently registered
  sixteen-colour families and resolves the element from each block state/identity. Generated
  special item definitions continue to serialize their explicit element (for example red remains
  `"element": "red"`), with no metadata-style colour component introduced.

Focused verification (no broad GameTest suite was run):

```text
.\gradlew.bat :ChromatiCraft:compileJava --no-daemon
BUILD SUCCESSFUL
.\gradlew.bat :ChromatiCraft:runClientData --no-daemon --console=plain
BUILD SUCCESSFUL (only the twelve hot/cold directional models changed)
.\gradlew.bat :ChromatiCraft:runGameTest -PgameTestSelector=chromaticraft:heat_lamp_temperature_furnace_loop --no-daemon --console=plain
All 1 required tests passed
```

### Crystal item translucency and model-outline visibility — 2026-08-13

The shared special renderer uses the 26.2 `itemTranslucent` target. The later V33a tint audit
supersedes this checkpoint's attempted alpha-only diagnosis: all forty-eight independently
registered CrystalBlock item forms retain alpha 220, matching the placed mesh and remaining more
transparent than V33a's alpha-255 inventory pass. The largest source-intended visual difference is
instead the old item's 80% colour brightness versus the placed model's full-bright pass. Crystal
Lamp and Potion/Super Crystal plinths remain deliberately opaque and are submitted separately
before the coloured mesh. See the 2026-08-13 asset/parity sweep for the corrected two-stage tint.

The custom diagonal selection geometry continues to use Minecraft's window-selected default line
width and its complete high-contrast two-pass behavior. Vanilla's normal colour is itself
translucent black (`ARGB.black(102)`); across the crystal mesh that produced the reported transparent
wire appearance. Normal custom-model outlines are therefore opaque black while retaining every
other vanilla selection-line parameter. This applies consistently to cave, lamp and potion/super
crystals and to the Item Stand geometry using the same reusable extractor.

### Structure controller: renderer and hardness — 2026-08-10

The fragment structure's root had a `PARTICLE_ONLY` model, so it drew nothing at all. Upstream's
`RenderStructControl` is not a solid block either — it is a glowing flare hanging in the air,
additively blended and turning on its own, from `ChromaIcons.SPINFLARE`
(`textures/block/icons/rotating flare_pulse.png`, a 64x11520 strip of 180 frames at one tick each).

`RenderStructureController` draws that flare, camera-facing, tinted by the controller's element. The
strip is sampled **directly** rather than through the block atlas: no model references it, so it is
not stitched, and walking the V offset by hand reproduces the animation without registering an atlas
source for a single texture. It uses `entityTranslucentEmissive`, which reads additive against the
world without a pipeline modifier — and keeps the whole element on one render type, which is now the
standing rule after two separate failures caused by breaking it.

Hardness drops from 6 to 1.5. This is the block you break to claim a fragment structure's reward, and
a nine-second bare-handed dig was tedious for no design reason. Blast resistance stays at 6000, so it
still cannot be opened with explosives — only found and mined.

**Not ported, marked CHROMA-PORT at the site:** upstream also draws the monument line ring over the
flare (the `monument_lines_big.png` pass and the `structcontrol` shader) and gates the whole renderer
on `isVisible`/`isMonument`/`isInWorld`. None of those flags exist on the ported block entity, so the
flare currently draws unconditionally. Both belong with the monument ritual.

### Lexicon source-parity correction: frames, recipes, fragments and progress — 2026-08-13

An in-client screenshot audit superseded several earlier “book screen complete” claims. The active
screen had combined V33a's separate GUI classes without preserving their shared coordinate contract:
frames were drawn at `k` even though `ChromaBookGui` draws them at `k-8`, while buttons correctly
remained anchored to `k`. That made the X button, titles and every specialist aperture disagree.
All frames now retain the source eight-pixel lift, with their widgets at the original unshifted
coordinates. Navigation sheet layout again starts from `(left+11, top+11)` and its first section
from `(left+15, top+12)`; a real scissor confines category frames, icons and search fog to the
242x206 navigation aperture instead of allowing outlines past the right and bottom book edges.

The combined-screen navigation leakage is also removed. Items/Recipes/Search and the three main
right tabs exist only on the navigation screen, as in `GuiNavigation`; entry screens carry only X
and `GuiBookSection`'s Save & Exit, while Progress, Recovery and Notes carry their own Return tab.
Switching Items/Recipes synchronizes the page content mode, eliminating stale recipe displays in
item view. The invented Description/Crafting Recipe/Casting Recipe toggle and invented casting
duration/experience labels are gone. Casting and vanilla grid pages restore the exact source title,
grid/output and alphabetical ingredient-tally origins. Recipe selection uses the small source top
arrows. More importantly, `GuiBookSection`'s right-side `>`/`<` subpage controls are restored for
Temple/Multiblock/Pylon recipes, making the Item Casting Stand's four authored runes reachable on
the real `handbook_runes` page.

Recovery, Notebook and Progress no longer fall through to `navigation2.png`: they use V33a's
`fragments.png`, `notes.png` and `progress.png` frames, with the scrolling stone field behind the
transparent Recovery/Progress apertures. Recovery's invented text-button list has been replaced by
the original 7x5, two-times-scale decoded-fragment grid and direct icon hit testing.

The decoded information-fragment item model now applies the old `(16,-16)` callback position in the
correct modern item-atlas coordinate space. Its half-sized icon is translated to the upper-right
instead of below the slot, and Shift is part of the model identity so the paper/inset and full-icon
states invalidate the GUI item atlas correctly.

Progress nodes once again render per-stage icons. The modern resolver binds every stage to its
landed split-registry equivalent (and to the original's nearest vanilla visual noun only where the
old custom tile/entity is not registered yet). The graph now caches its prerequisite edges and
depths. Each diagonal edge is submitted as one rotated one-pixel rectangle rather than hundreds of
individual 1x1 GUI render states per frame; the latter was the direct cause of the reported ~13 FPS
screen. Progress content is clipped to the upper aperture, node selection restores the authored
title/hint/reveal panel below it, and locked nodes display a question mark rather than an empty slot.

Focused verification only; no GameTests were relevant or rerun:

```text
.\gradlew.bat :ChromatiCraft:compileJava
BUILD SUCCESSFUL
```

### Start-to-Proxima milestone 1: guide structures and progression spine — 2026-08-13

The new milestone is a naturally playable route from a fresh world into Proxima, with Proxima's
non-puzzle terrain/content first and its puzzle structures deliberately following later. This is a
large cluster rather than a single portal patch: the V33a portal is gated by eleven exploration and
energy prerequisites, and the 26.2 tree still contained several missing grant sites and a stripped
conditional-progression layer. This checkpoint repairs the earliest shared seams without falsely
claiming that the dimension itself has landed.

**Guide structure parity.** The Crystal Stone page already resolved all sixteen independently
registered `StoneTypes` blocks, but recipe lookup asked the server only about the first (smooth)
icon. Crafting and casting lookup now aggregate every displayed registry identity, de-duplicate the
results, and therefore expose the recipes for beams, columns, stabilizers, pylon focus blocks,
grooves, bricks, the multichromic rune, aura stabilizer and resonance ring as well as smooth stone.

The Crystal Burrow and the other natural dungeons were not absent from worldgen: canonical NBTs for
`cavern`, `burrow` plus its annexes, `ocean`, `desert` and `snow` were already generated and placed by
`OverworldStructureFeature`. Their guide rows use a Shielding block as the icon, so the viewer tried
to load a nonexistent `structshield` template. The viewer now separates icon identity from structure
identity and loads each canonical NBT. Their titles are restored from V33a's `chromastruct.*` keys —
Crystal Shrine, Crystal Burrow, Ocean Temple, Sandy Burrow and Frozen Outpost — instead of the stale
generic `Shielding`, and their exact old Shielding variants (cloak/stone/moss/cobble/light) are shown.

There is no separately registered V33a overworld structure named `RAVINE`. The requested ravine-like
dungeon is `ChromaStructures.CAVERN`, displayed upstream as **Crystal Shrine**; its full modern NBT
and natural placement already existed and are now reachable in the structure viewer. Proxima also
contains deprecated/experimental canyon terrain code, but that is dimension terrain and must not be
invented as a second overworld dungeon.

**Natural progression restored.** The excluded monolithic 1.7.10 event manager had left basic
server progression disconnected. `ProgressionEventBridge` restores only its source-faithful
progression hooks with modern NeoForge events: breaking an ore grants MINE, harvesting a crop grants
HARVEST, entering the Nether/End grants NETHER/END, receiving a potion effect grants POTION,
player-caused hostile/dragon/wither deaths grant their corresponding combat stages inside V33a's
384-block/same-dimension bound, and dying while carrying at least 90,000 total player-buffer lumens
grants DIE. Breaking a previously discovered spawner grants BREAKSPAWNER. The authoritative
TradeWithVillagerEvent hook for buying a Focus Crystal also grants FOCUSCRYSTAL now; the actual
data-driven offer still belongs to the next village/trade slice. Fake players remain excluded.

V33a's four conditional chains are active again: BYPASSWEAK can trigger BLOWREPEATER, TUNECAST can
trigger BYPASSWEAK unless BLOWREPEATER was already learned, and either FOCUSCRYSTAL or RELAYS can
formulate ENERGYIDEA once USEENERGY is known. The focus-crystal path retains its retroactive
behavior, so discovery order does not dead-end the repeater branch. These are conditional dashed
links, not solid DAG parents; representing both focus crystals and relays as mandatory parents would
have changed the game.

**Selection outline.** Cave, potion and lamp crystal model outlines now use half of Minecraft's
window-selected line width (including half of the high-contrast secondary width). The shared Item
Casting Stand outline remains at full vanilla width.

**Focused verification:**

```text
.\gradlew.bat :ChromatiCraft:compileJava --console=plain
BUILD SUCCESSFUL

.\gradlew.bat :ChromatiCraft:runClientData --no-daemon --console=plain
BUILD SUCCESSFUL (one corrected language file written)

.\gradlew.bat :ChromatiCraft:runGameTest \
  -PgameTestSelector=chromaticraft:progression_chained_energy_idea \
  --no-daemon --console=plain
All 1 required tests passed
```

**Remaining Start → Proxima critical path (in dependency order):**

The failed-casting village pieces, Focus Crystal trade, Storage Crystal chain/Charger, both Aura
Infusers and the player-placed eight-crystal `POWERCRYSTAL` pylon loop are complete as of
2026-08-14; see their ledger entries below.

1. Port the portal block/entity/charge behavior and the authored 15x10x15 PortalStructure to a
   canonical NBT. Do not emit an NBT containing unregistered legacy block ids simply to make the
   command exist.
2. Register Proxima's dimension type, biomes and chunk generator, then port its non-puzzle terrain
   features (including the dimension's canyon/ravine terrain where the V33a generator actually uses
   it), decorations, entities, audio/sky/weather and return/travel safety. Puzzle generators remain
   the explicitly deferred follow-up.

### V33a visual/guide feedback audit: crystal items, runes, loot chest and repeaters — 2026-08-13

An item-side source comparison found that the crystal mesh itself was faithful but its inventory
selection was not. V33a's normal `CrystalRenderer.renderInventoryBlock` draws the central spike plus
only the positive-X and positive-Z branches; all four branches are a temporary handbook-only mode.
The special renderer had made that temporary mode permanent, producing an overly dense, symmetric
icon. Cave, potion and lamp crystal items now use the ordinary two-branch mask and V33a's 80%-bright
element tint. Their modern alpha remains 220 (the source world alpha) to retain the separately
requested translucent item presentation instead of reverting to the old opaque item pass.

The casting rune subpage had ported only `getBlockRune()` — the nearly black engraved underlay.
V33a's `RuneShapeRenderer` immediately overlays `getFaceRune()`, which is the coloured/glowing rune
image. Both layers are now drawn in source order. `ChromaBookData.drawCastingRecipe` also renders the
result stack before selecting Grid/Runes/Stands/Aura; the output icon therefore remains in the left
result slot on every subpage instead of going blank away from Grid.

`CRYSTALSTONE` is present under Resources with its source title **Crystal Stone** and its
`pylonstruct` binding resolves all sixteen independently registered `StoneTypes`; recipe requests
continue to aggregate every identity. A non-creative lexicon still needs that fragment, exactly as
V33a did. Empty navigation cells were previously indistinguishable from missing catalog data. Any
catalog entry whose icon binding is genuinely unported now gets a runtime magenta/black checker and
`!`; this is deliberately drawn in Java rather than installed as a reusable asset, so it cannot be
mistaken for finished content or leak into block/item models.

Progress notifications use ChromatiCraft's own top-right GUI layer, not Minecraft's toast manager.
It is a direct render-state port of V33a's `ProgressOverlayRenderer`: independently sorted/stacked
charcoal cards, the two inset frames, vertically revealed entry/exit, authored title and short
description, stage icon, the configured 800-tick default lifetime, and the original 0.5-volume,
24-tick-cooled `GAINPROGRESS` sound. Research-tier upgrades use their fragment icon and original
generic world-reveal caption. The payload distinguishes stage ordinals from research-level ordinals.

The loot chest's old cuboid coordinates had been combined with the modern renderer without the
legacy model transform: its lid occupied the lower ten-pixel box and the result looked both too short
and static. The layer now uses the equivalent 26.2 single-chest convention (14 pixels total, a real
9-pixel hinge, linked knob rotation). The already-live client lid ticker and openers block event now
move a visible lid through the source cubic easing.

Finally, the repeater BER advertises an AABB expanded by its full send/receive range. Connection
beams, the manipulator dashed target line and range sphere are no longer scoped to the repeater's
one-block render box, so looking away from the controller cannot frustum-cut a long line near its far
end. V33a's 4.5-block player target ray and dashed cadence remain unchanged.

**Focused verification only:**

```text
.\gradlew.bat :ChromatiCraft:compileJava --console=plain
BUILD SUCCESSFUL

.\gradlew.bat :ChromatiCraft:runGameTest \
  -PgameTestSelector=chromaticraft:lexicon_casting_recipe_snapshot \
  --no-daemon --console=plain
All 1 required tests passed
```

### V33a fragment/lexicon interaction and render fault repair — 2026-08-14

The invalid-texture-dimensions crash on the pylon introduction page was not a corrupt PNG.
`roundflare.png` is the exact V33a 180-frame block animation: 256 pixels wide and 46,080 pixels
tall. It is legal when stitched as 180 256x256 atlas frames, but illegal when the GUI texture
manager tries to allocate the whole strip as one OpenGL texture. The lexicon now samples the
already-stitched animated block-atlas sprite, retaining animation and eliminating the oversized
standalone upload.

DragonAPI's modern `syncCustomData` had accidentally serialized and client-loaded the complete
player record. Pylon charging changes the element buffer every tick, so that path also reapplied
saved position and rotation every tick and fought live mouse input. The packet once again means
what the 1.7.10 API meant: only the player's persistent mod-data compound. The client replaces that
compound authoritatively without ever invoking `Player.load`, so manipulator charging can no longer
snap or oscillate the camera.

The fragment workflows are containers again. Shift-using a non-creative Lexicon opens V33a's
scrollable 9x3 page inventory with the player's inventory below it. Decoded fragments can be
inserted, extracted and shift-moved; the book's component-backed page set is updated as rows scroll
and when the container closes. Clicking blank fragments onto the Lexicon in any inventory now uses
the 26.2 stacked-item override hook: primary click stores the carried stack, secondary click stores
one, and secondary-clicking with an empty cursor extracts one of the book's stored blanks.

Right-using an undeciphered fragment now opens the original one-slot, three-choice decoder rather
than an invented vertical list of every available page. The full V33a category table is mechanically
derived into `lexicon/fragment_categories.tsv`; it retains all explicit page/category weights plus
the source's generic Info/Block/Tool/Resource/Ability/Structure bindings. The server runs the
original choose-category, then weighted-page algorithm and synchronizes the resulting three valid
next-research identities and category-atlas ordinals. The client uses the exact
V33a `fragselect`, `fragmentcategories` and `squarefog` assets and the original slot/inventory
coordinates; a choice retains the source 1.5-second confirmation. Taking or closing an undecoded
fragment invokes the original random-selection fallback and returns the programmed fragment to the
player, while a completed choice grants the corresponding player research before closing.

Ordinary guide prose now wraps at V33a's 242-pixel width and uses all thirteen lines that fit between
the source description origin and lower page edge. The invented 52x18 bottom `Page` buttons and
bottom page counter are gone; truly longer authored prose uses the small source-position up/down
controls. The modern picture-in-picture projection's pitched Z extent is compensated in the machine
preview anchor, keeping block models inside the page instead of projecting through its top border.

Crystal item geometry now uses full-bright light coordinates for the coloured translucent mesh
while leaving lamp/potion support bases under ordinary item lighting. This reproduces the visual
emissive strength of the old renderer under the 26.2 item compositor without making the opaque base
glow. The alpha, exact V33a colour calculation and ordinary two-branch inventory silhouette remain
unchanged.

The loot-chest texture is restored byte-for-byte from `ChromatiCraft 1.7.10 V33a.jar` (SHA-256
`E180AA6B064FC416F2C5A524BD46B9E71A8D1F07ABF967A965C6DA25F2301789`). The modern model already uses
Minecraft's equivalent single-chest cube wrapping and linked lid/knob hinge, so retaining the source
sheet—rather than swapping its up/down rectangles—gives both the closed top and opened lid interior
their intended faces.

**Focused verification:**

```text
.\gradlew.bat :DragonAPI:compileJava :ChromatiCraft:compileJava --console=plain
BUILD SUCCESSFUL
```

No GameTests were rerun for this client-render/container slice. The existing information-fragment
research-loop coverage remains compiled; the newly repaired failures require the in-world GUI,
mouse, atlas-animation and chest-open render pipeline and therefore need the manual checks listed in
the hand-off.

### Start-to-Proxima milestone 2: failed-casting villages and Focus Crystal trade — 2026-08-14

The canonical `VILLAGECASTING` route is live without replacing it with a generic village-entry
trigger. Datagen mechanically reads the untouched V33a `WoodenChromaStructure.generate` and
`BrokenChromaStructure.generate` placement calls and emits ten modern structure NBTs: both authored
variants in plains, desert, savanna, snowy and taiga palettes. The wooden house retains its full
15x8x15 clear-volume contract, eight paired doors, central casting ruin, source sign and
ChromatiCraft loot chest; the 16x7x17 broken structure retains its damaged crystalline-stone form,
water/gravel damage, signs, primary progression chest and secondary junk chest. Old block metadata
is translated to explicit 26.2 identities/states during generation. Door upper halves inherit the
lower half's facing after import, avoiding invalid modern two-block door pairs.

Each template has the same west-facing `minecraft:building_entrance` connector contract used by
vanilla 26.2 houses. The two entries are added to every ordinary village house pool at V33a's 4:1
relative weights. NeoForge 26.2 has biome/structure modifiers but no template-pool append codec, so
the data pack must overlay the five vanilla `minecraft:village/*/houses` pool ids. The provider does
not carry a handwritten vanilla snapshot: it opens the archive/directory which supplied Minecraft's
`StructureTemplatePool` class, copies Mojang's exact current pool JSON, removes any previous Chroma
entries for idempotence and appends the two elements. This is source-faithful and update-safe within
the target game version, but it is still a namespace override; compatibility with another mod which
also replaces those exact pool files needs an explicit merged-pool solution if one is encountered.

The ChromatiCraft village chest hydrates `chromaticraft:chests/village_casting` and a persistent
`VILLAGECASTING` trigger. Its table includes the active 26.2 weaponsmith contents, V33a's weighted
sixteen-colour shard/Lexicon/Info Fragment additions, and the independent 50% unowned-chest fragment
roll. The broken ruin's vanilla chest uses the source weighted coal/iron/gold/redstone/blue-dye/
diamond/emerald junk table for ten draws. Because the 26.2 loot-table datagen validator cannot
resolve cross-pack nested references, the current vanilla weaponsmith pools are copied directly into
the Chroma table rather than referenced by id; server datagen validates the resulting self-contained
table.

The Focus Crystal offer is a real `villager_trade` registry object. Eligible V33a professions map to
modern librarians, clerics, armorers, toolsmiths and weaponsmiths. Each villager persists one
independent 40% eligibility roll; reopening or reloading cannot reroll it. Immediately before a
player opens an eligible villager, the bridge removes any stale shared offer and exposes the trade
only when that player has `CRYSTALS`. It costs exactly one emerald, gives one component-backed
FLAWED Focus Crystal, and uses `Integer.MAX_VALUE`; completed trades also have their uses reset so
the original no-expiry behavior survives implementation details. Buying it continues through the
authoritative trade event into `FOCUSCRYSTAL` and the conditional `ENERGYIDEA` chain.

Generated-output audit covered all ten NBTs: wooden templates contain 1,800 explicit cells, broken
templates 237 cells, every style has its expected palette, all eight wooden door pairs match, and
every template contains the expected jigsaw and loot block-entity NBT. The connector was compared
directly with Mojang's `plains_small_house_1` and matches its name, target, street pool, orientation
and stair final-state contract.

**Focused verification only:**

```text
.\gradlew.bat :ChromatiCraft:runServerData --console=plain
BUILD SUCCESSFUL

.\gradlew.bat :ChromatiCraft:compileJava --console=plain
BUILD SUCCESSFUL

.\gradlew.bat :ChromatiCraft:runGameTest \
  -PgameTestSelector=chromaticraft:focus_crystal_trade_definition \
  --console=plain
All 1 required tests passed

.\gradlew.bat :ChromatiCraft:runGameTest \
  -PgameTestSelector=chromaticraft:village_casting_nbt_contract \
  --console=plain
All 1 required tests passed
```

### 2026-08-14 — V33a Storage Crystal item and casting chain restored

The former `ChromaItems.STORAGE` metadata family is now seven stable 26.2 registry identities:
`storage_crystal_nula`, `storage_crystal_daya`, `storage_crystal_divi`, `storage_crystal_sami`,
`storage_crystal_vier`, `storage_crystal_lima`, and `storage_crystal_aru`. Their per-element
capacities retain V33a's exact `1000 * 8^(metadata-1)` sequence: 125 through 32,768,000. Stored
sixteen-colour energy is component-backed under `CUSTOM_DATA.energy`; the implementation preserves
unrelated custom data and retains the original static API surface for the charger and later relay
consumers. The creative tab emits both empty and fully charged examples, as V33a did.

The complete multiblock casting upgrade chain is data-generated under
`chromaticraft:storage_crystal/*`. It retains the source center-item sequence, boosted-shard base
ring, Infused Dust base inner ring, Chromic/Resonant Dust upgrade stands, all twelve source runes,
durations 50–3200, XP 200, penalty threshold 3, `STORAGE` completion grant, and center-component
copying. Casting completion behavior is now recipe data instead of a hardcoded output special case;
this also preserves the V33a harmonic-note metadata for later recipes.

Focused validation:

```text
.\gradlew.bat :ChromatiCraft:compileJava --console=plain
.\gradlew.bat :ChromatiCraft:runServerData --console=plain
.\gradlew.bat :ChromatiCraft:runClientData --console=plain
.\gradlew.bat :ChromatiCraft:runGameTest \
  -PgameTestSelector=chromaticraft:storage_crystal_item_and_recipe \
  --console=plain
All 1 required tests passed
```

The next Start-to-Proxima work is the V33a Storage Crystal Charger (`CHARGER`) followed by the
separate Item Aura Infuser (`INFUSER`). Neither may be represented by the generic storage block.

### 2026-08-14 — Storage Crystal Charger operational loop restored

`chromaticraft:crystal_charger` is now the registered `ChromaTiles.CHARGER` machine, backed by a
real network-receiver block entity, menu/screen, submit-pipeline BER, special inventory renderer,
and the exact V33a 64x32 Techne texture/model. The GUI uses the original 256x256 Charger sheet and
retains the sixteen independently clickable colour bars. The displayed Storage Crystal
counter-rotates inside the machine as in the source.

Server behavior retains the source constants and rules: 120,000 internal lumens per colour,
20-block receive range, 4,000 throughput, all colours enabled by default, persisted toggle
bitflags, transfer rate `10 + floor(sqrt(stored))`, the hidden optional Speed Upgrade's x8
multiplier, and sided extraction of slot zero only after every one of the crystal's sixteen
channels is full. The former MISC metadata-1 Speed Upgrade is a distinct
`chromaticraft:speed_upgrade` registry item with its exact V33a sprite; it is intentionally still
the hidden second automation slot because the source Charger GUI exposes only the crystal slot.

The data-generated `chromaticraft:crystal_charger` multiblock recipe uses the Crystal Core center,
four obsidian corner stands, three plain white-shard cardinal stands, and the smooth-stone slab on
the remaining cardinal stand. It retains MULTIBLOCK tier, 200 ticks, and 200 XP.

Focused validation:

```text
.\gradlew.bat :ChromatiCraft:compileJava --console=plain
.\gradlew.bat :ChromatiCraft:runServerData :ChromatiCraft:runClientData --console=plain
.\gradlew.bat :ChromatiCraft:runGameTest \
  -PgameTestSelector=chromaticraft:crystal_charger_item_loop \
  --console=plain
All 1 required tests passed
```

The next distinct Start-to-Proxima machine slice is the Item Aura Infuser (`INFUSER`), including
its pedestal item collision/right-click loop, tank/fluid consumption, structure validation,
recipe registry, rendering, and progression grant.

### 2026-08-14 — Item Aura Infuser operational loop restored

`chromaticraft:item_aura_infuser` is now the distinct registered `ChromaTiles.INFUSER` pedestal;
it is not represented by the Storage Crystal Charger or by a generic inventory block. The modern
block/entity pair preserves V33a ownership, one-item pedestal interaction, item-entity pickup,
empty-hand ejection, the unusual one-item overflow transfer rule, inert automation inventory,
break drops, and the 608-tick operation interval. Crafting converts every Raw Crystal in the input
stack into Iridescent Crystals, grants `INFUSE`, and preserves `DOUBLECRAFT` output overflow through
the stack's component-backed custom data until it is ejected or dropped.

The complete Infusion Structure is generated as canonical
`chromaticraft:multiblock/infusion.nbt`. Its smooth crystalline-stone floor, brick rings and exact
24-point trigonometric outer ring match the V33a coordinates. The inner radius-two ring remains a
real distributed Liquid Chroma reservoir: each authored cell is exposed through a transactional
26.2 fluid handler, accepts only complete Chroma buckets, rolls back atomically, and is consumed on
successful crafting. Structure validation, cache lookup and invalidation are live rather than
being replaced with a boolean placeholder.

Focus acceleration retains the original rules. Any flawed crystal cancels acceleration; refined,
exquisite and turbo crystals contribute their source weights; eight total focus points produce 2x
speed and sixteen all-exquisite points produce 4x. The persisted/synchronized operation state also
drives the source cadence for `INFUSION`, `INFUSION_SHORT`, `INFUSE`, and `ERROR` audio. Client
rendering uses the exact V33a 64x32 `infuser2.png` Techne body, eight arms, counter-rotating/bobbing
pedestal item, cycling sixteen-colour three-layer rays, sixfold crafting spiral and completion
burst. The inventory item uses the same authored special renderer.

The data-generated MULTIBLOCK recipe retains the Item Stand center, all eight exact Chroma Alloy
stand positions, 100-tick duration and 200 XP. The historical commented-out purple/black energy
requirement was not invented as a live requirement.

Focused validation:

```text
.\gradlew.bat :ChromatiCraft:compileJava --console=plain
.\gradlew.bat :ChromatiCraft:runServerData :ChromatiCraft:runClientData --console=plain
.\gradlew.bat :ChromatiCraft:runGameTest \
  -PgameTestSelector=chromaticraft:item_aura_infuser_loop \
  --console=plain
All 1 required tests passed
```

The next Start-to-Proxima slice is the separate Player Aura Infuser (`PLAYERINFUSER`) and its
`PLAYERINFUSION` upgrade structure/recipes. After that, the ledger returns to the remaining
network/casting requirements for `POWERCRYSTAL`; the already-finished Item Infuser must not be
silently reused for player ability upgrades.

### 2026-08-14 — Player Aura Infuser and buffer-capacity upgrade contract restored

`chromaticraft:player_aura_infuser` is now its own registered `ChromaTiles.PLAYERINFUSER` block and
block entity rather than an alias of the Item Aura Infuser. It shares only V33a's actual pedestal
base: ownership, the one-slot interaction/collision rules, Liquid Chroma handling, 608-tick
operation interval and authored `infuser2.png` body renderer. The common block's shape has also been
corrected to the source registry's half-block height for both infusers.

The complete Player Infusion fountain is generated as canonical
`chromaticraft:multiblock/player_infusion.nbt` with the controller anchored at `(4,3,4)`. It retains
the exact 9x4x9 smooth-stone base, outer crystalline beam/corner square, inner brick/beam segments,
central focus column, four stabilizers and 32 unobstructed source Liquid Chroma cells. The runtime
structure loader and validator use that NBT; successful infusion consumes those exact 32 sources.
Focus Crystal sockets are derived from the four stabilizer cells, preserving the shared acceleration
rules instead of hardcoding another set of coordinates.

Player infusion behavior now matches V33a: a valid capacity-upgrade ingredient stack is limited to
eight, the owner must intersect the narrow target band above the pedestal, the selected upgrade must
still be available, and every crafting tick pulls the recipient toward the center with the original
squared-distance velocity formula. Completion grants the selected persistent capacity boost and
consumes all eight items. The previously omitted common `onCraftingTick` hook was restored, so the
capture behavior actually runs during the operation. Crafting and ambient fountain particles use
the source Liquid Chroma cell selection, upward speed, inward velocity, scale and gravity ranges.

The `ElementBufferCapacityBoost` table no longer grants all seven boosts automatically. Only
`ALLCOLORS` and `ABILITY` are automatic, exactly as in V33a. The five gated boosts again have their
source ingredients: Ether Berries (`ALLOYS`), Glow Cave Dust (`DIMENSION`), Boost Root
(`TURBOCHARGE`), Echo Crystal (`CTM`) and Unknown Artefact Fragment (`TOWER`). The latter four now
have stable 26.2 item identities and their exact atlas sprites; their later biome/dimension/structure
acquisition routes remain work for the corresponding content slices and are not falsely marked
complete here.

The data-generated MULTIBLOCK recipe retains the Item Aura Infuser center, four Aura Ingots, four
diamonds, three Resonant Dusts and one Chromastone in the exact twelve stand positions, with 100
ticks, 200 XP, no runes and no aura requirement. Server/client datagen emits its recipe, NBT,
language and the shared special item model.

The focused test also exposed a foundational NeoForge packet seam: DragonAPI's pipeline and periodic
block-entity sync broadcaster attempted to send custom payloads to connections which had not
negotiated that payload type. Both paths now filter with `connection.hasChannel(payload)` before
sending. Normal modded clients retain their packets, while optional/non-negotiated connections and
headless GameTest players can no longer crash the server.

Focused validation:

```text
.\gradlew.bat :ChromatiCraft:compileJava --console=plain
BUILD SUCCESSFUL

.\gradlew.bat :ChromatiCraft:runServerData :ChromatiCraft:runClientData --console=plain
BUILD SUCCESSFUL

.\gradlew.bat :ChromatiCraft:runGameTest \
  -PgameTestSelector=chromaticraft:player_aura_infuser_loop \
  --console=plain
All 1 required tests passed
```

The next Start-to-Proxima slice is an exact reachability audit through `POWERCRYSTAL`: identify the
first missing runtime grant/recipe/network link from the now-operational storage/infusion machinery,
then port that dependency rather than jumping ahead to a command-only Portal shell.

### 2026-08-14 — `POWERCRYSTAL` player-placement and progression path closed

The reachability audit confirmed that the exact V33a PYLON recipe is already live and
data-generated: diamond center; six Iridescent Chunks, five obsidian and two glowstone on the
authored stands; 15,000 yellow, 25,000 black and 10,000 purple aura; 1,600 ticks; 500 XP; stacking
factor `0.97489`; and a personal tuning-key requirement. Completing that PYLON-tier cast supplies
`LINK`; the operational Storage Crystal chain, pylon charging and Item Aura Infuser supply the
other original prerequisites `STORAGE`, `CHARGE` and `INFUSE`.

The actual runtime break was placement ownership. V33a's common `ItemChromaPlacer` called
`setPlacer` on every `TileEntityChromaticBase` before restoring item NBT. The modern common
`BlockChromaticTile` restored only the NBT, while focused pylon tests manually injected owners.
Consequently player-placed Power Crystals had empty owner sets: they could render and connect, but
could not form the same-owner eight-crystal set which recharges the pylon and grants
`POWERCRYSTAL`. The common block now restores the source placement ordering—player first, item NBT
second—for every generic Chromatic tile. This fixes the Power Crystal without adding a one-off
exception and restores the same contract for other still-generic owned machines.

The existing `pylon_power_crystal_recharge` test now places all eight crystals through the real
block-placement callback rather than calling `setPlacer` on their block entities. It proves all
eight inherit the owner, connect to their legal rune sockets, contribute the exact 768-lumen normal
tick, and grant `POWERCRYSTAL` once the owner has `LINK`, `STORAGE`, `CHARGE` and `INFUSE`.

Focused validation only:

```text
.\gradlew.bat :ChromatiCraft:runGameTest \
  -PgameTestSelector=chromaticraft:pylon_power_crystal_recharge \
  --console=plain
All 1 required tests passed
```

The next implementation boundary is now the portal itself. The audit found no modern Portal block,
block entity or `ChromaStructures.PORTAL` registration and no portal structure NBT; the 500-line
`BlockChromaPortal` and 616-line authored `PortalStructure` remain pristine V33a source. They must
land together with modern collision/teleport safety and the later Proxima dimension target—adding a
cosmetic or command-only portal block first would create another unreachable shell.

### 2026-08-15 — Portal Rift cluster, Liquid Ender and Proximal Essence

The complete V33a portal subsystem is now live. This is the whole coherent unit, not a shell: the
canonical NBT, both block identities, the block entity, the casting recipe, the tuning economy, the
contact rules and the 26.2 teleport pipeline all landed together.

**Original files audited.** `Block/BlockChromaPortal.java` (500 lines, block plus its inner
`TileEntityCrystalPortal`), `Auxiliary/Structure/PortalStructure.java` (616 lines, 581 explicit
placement calls), `Auxiliary/RecipeManagers/CastingRecipes/Blocks/PortalRecipe.java`,
`Auxiliary/ChromaTeleporter.java`, `World/Dimension/DimensionTuningManager.java`,
`World/Dimension/CheatingPreventionSystem.java`, `World/Dimension/DimensionJoinHandler.java`,
`World/Dimension/ChunkProviderChroma.areGeneratorsReady`, `World/Dimension/WorldProviderChroma.java`,
`Block/BlockLiquidEnder.java`, `Items/ItemTieredResource.java`, and `Render/TESR/RenderCrystalPortal`
plus `Render/Item/PortalItemRenderer` for the presentation layer.

**Portal NBT template.** `chromaticraft:multiblock/portal`, **15 x 10 x 15**, controller anchor
**(7, 0, 7)** — V33a builds from `i = x-7, j = y+0, k = z-7` and puts the 3x3 rift pad on the bottom
layer at `i+6..i+8, j+0, k+6..k+8`. `ChromaStructureTemplateProvider.importPortal()` parses all 581
source calls mechanically and asserts that exact count; nothing is retyped by hand. The pristine V33a
class is retained verbatim at `auxiliary/structure/legacy/PortalStructure.java` as the single geometry
authority, outside the compile allowlist, exactly as the village/cavern/burrow/ocean/desert/snow
templates keep theirs.

Decoded contents of the generated NBT: 148 plain Cloak Shielding, 100 Luma (64 flowing, 36 basin),
60 smooth, 56 bricks, 48 column, 44 resonance ring, 24 Liquid Ender sources, 20 beams and 20 energized
beams split 10/10 per axis, 9 rift pad cells, 8 each of crystalline energy stabilizer / multichromic
rune / Pylon Focus / engraved, 6 of each groove, 4 aura stabilizer and 4 corner. The anchor is
self-checking: the four Pylon Focus cells land at `(+-3, +5, +-3)` from the controller, which is
precisely where V33a's `chargingParticles()` probes for them.

`shield, 0` decodes to **non-reinforced** Cloak Shielding, not the unbreakable worldgen form: V33a's
`BlockType.metadata = ordinal()+8`, so bit 3 is the reinforced flag and metadata 0 is the plain,
player-placeable material. That is what makes the structure buildable at all.

**Wildcard cell semantics preserved.** V33a used three different cell contracts on fluids and two of
them on the same block. `NBTStructureLoader` gained an explicit per-position `CellRule` so they
survive instead of collapsing into a literal state comparison:

- `setFluid(er)` is a `FluidCheck` with `needsSourceBlock`, so the 24 Liquid Ender basins keep the
  ordinary exact-state path against `liquid_ender[level=0]`.
- `setBlock(ch)` is a metadata-wildcard `BlockKey`: any Luma level. These are the fountain basin
  cells, which drain as they flow; requiring a source there would make a built portal invalidate
  itself.
- `setBlock(ch, 1)` required legacy quanta 1 on a fluid whose `BlockEtherealLuma` declared
  `setQuantaPerBlock(16)`. 26.2's `LiquidBlock` has eight levels, so the legacy index has no
  counterpart; what the call distinguishes is flowing Luma from a source, which is what the sleeve
  around the fountain pillar physically is. Those cells accept any non-source level.

**Ender Crystals are not in the template, deliberately.** V33a's `getEntities()` additionally requires
exactly one vanilla Ender Crystal in each of eight cells at `(+-5,+5,-+9)` and `(+-9,+5,-+5)`. They are
entities, they sit outside the 15x15 footprint, and the bedrock pads under them exist only in
`isDisplay()` and are never a match requirement. `TileEntityCrystalPortal` checks those eight
positions alongside the array, exactly as the source does.

**Two registered rift identities.** `chromaticraft:portal_rift` targets Proxima and must match the
whole multiblock; `chromaticraft:return_portal_rift` is V33a metadata 15 — it targets the Overworld
and validates unconditionally. Per the standing metadata rule those are two blocks, not a property.
Recorded honestly: **nothing in V33a places metadata 15.** A full `git grep` of the V33a tree finds no
producer, so the return rift is reachable only through commands/creative upstream, and this port does
not invent an acquisition path for it. V33a's real ways out of Proxima are dying there (you are moved
to the Overworld at 1 health, losing 10-60% of each buffered element) and falling below y = -1024;
both live in the excluded monolithic event manager and are **not** yet ported.

**Charging is a timer, not an energy cost.** This is the single most important thing not to
"improve": nothing in V33a supplies the portal's charge. While the structure matches, `charge`
increments once per tick to `MINCHARGE = 300`, and keeps incrementing past that for as long as the
dimension generators are not ready. No pylon, repeater, battery or focus crystal is involved. Losing
any required cell zeroes it. Do not add an aura or network requirement here.

`areGeneratorsReady()` is restored as a real gate, `ProximaGenerators`, mirroring V33a's five-bit
`ThreadedGenerators` flag. Only generators that actually exist in the port are members (currently
`BIOME` and `REGION`); `STRUCTURE` and `SKYRIVER` are documented as belonging in the same gate and
must be added to its enum when their generators land, so the portal automatically resumes waiting on
them. It is not a `return true`.

**Behaviors restored.** All nine pad blocks carry the entity (`hasTileEntity` is an unconditional
`true` upstream, its `meta == 1` restriction commented out) and only the centre works — `centre` being
`getPortalPosition() == 5` with a full 3x3, which is what the misleadingly named `isFull9x9()` really
tests; the modern method is called `isFullPad` so nobody "fixes" it into a 9x9 scan. Contact anywhere
on the pad walks inward to the centre, with V33a's static visited set replaced by a per-call set.
`denyEntity` throws the entity up at 1.5/tick with a random +-0.25 horizontal nudge and forces
`fallDistance` to at least 500 — that lethal landing is the anti-abuse mechanism, and it applies to
unqualified players, non-essence items and every other entity alike. Tuning follows the exact
`(pure ? 150 : 1) * count^(pure ? 0.85 : 0.5)`, decays one per `rand(400)` ticks, and 60% is spent per
trip. The Manipulator dismantle extinguishes fire in the 5x5, flood-fills every connected rift within
16/8/16, drops one rift per removed block and plays RIFT plus POWERDOWN; `ownedBy` returns true for a
portal with no recorded placer, so an unplaced rift can be dismantled by anyone. The rift has no
collision box, `RenderShape.INVISIBLE`, `noOcclusion`, no loot table, and a render bounding box
inflated by 8.

**Teleport.** Ported onto the real 26.2 pipeline: the block implements
`net.minecraft.world.level.block.Portal`, contact calls `setAsInsidePortal` (which supplies vanilla's
portal cooldown and therefore the loop guard for free), and `getPortalDestination` returns a
`TeleportTransition`. Transition time is 0 because V33a teleported on contact.
`ChromaTeleporter.arrivalTransition` reproduces upstream's `placeInPortal` exactly — y = 1024 above
the origin entering Proxima, y = 1024 above the player's bed or world spawn returning.

Every side effect lives in the post-transition hook, and that placement is load-bearing rather than
stylistic. 26.2 decides whether a transition is actually permitted (`isAllowedToEnterPortal` /
`canTeleport`) **after** `getPortalDestination` returns, so doing V33a's work at destination-computation
time would consume 60% of the rift's tuning and scatter the player's banned items in the departure
world even on a refused teleport. V33a could order it the other way only because its
`transferEntityToDimension` call was the last statement in `teleportPlayer`. The hook therefore runs
the source's exact order once the teleport is committed: the departure-world ban sweep, the tuning the
trip carries, the arrival cue, and the 60% spend. `CheatingPreventionSystem.preJoin` gained an overload
taking the captured departure level and position so its drops still land in the world being left.
`getLocalTransition` returns `NONE`: V33a had no screen warp, and vanilla's `CONFUSION` would have been
an addition rather than a port.

`CheatingPreventionSystem` is fully ported as a mechanism — ban registry, all six `BanReaction`s, the
pre-join drop sweep with permanent-lifetime drops, the post-join delete sweep, the held-item tick, the
right-click gate, the explosion/knockback punishment and the `STRUCTCHEAT` grant. Its ban *list* is
legitimately empty: every V33a entry names a 1.7.10 mod (EnderIO, GraviSuite, Thaumic Tinkerer,
Draconic Evolution, Botania, NotEnoughWands). The exact original name-to-severity table is retained as
`LEGACY_BANS` data and re-resolved against the live registries, so any of those mods appearing for
26.2 is banned again without re-deriving anything.

**Casting recipe.** Exact `PortalRecipe`, verified in the generated JSON: pylon tier, Energized Void
Core in the grid centre, 24 stands (Energetic Essence and Spatial Rifting Powder inner; glowstone,
ender eyes, Chromastone, beacons, End Stone, emeralds outer), 32 runes (the full sixteen-rune ring
plus the whole Crystal Repeater layout), 16 aura entries at 120,000 for each primary and 60,000 for
the rest, 2,400 ticks, 25,000 experience (the pylon tier's 500 x 50), nine rifts produced, penalty
threshold 1 with multiplier 0 so a repeat cast is worth nothing, and all eleven `DIMENSION`
prerequisites read live from the progression DAG rather than restated.

**Progression audit — all eleven prerequisites are genuinely reachable.**

| Prerequisite | Grant site | Verified |
|---|---|---|
| `ALLCOLORS` | `ProgressionManager` auto-grant once all sixteen colours are discovered | yes |
| `END` | `ProgressionEventBridge` on entering the End | yes |
| `NETHERSTRUCT` | `NetherRoofStructureFeature` loot chests | yes |
| `POWERCRYSTAL` | the player-placed eight-crystal pylon loop (2026-08-14) | yes |
| `RAINBOWFOREST` | `ExplorationMonitor` | yes |
| `GLOWCLIFFS` | `ExplorationMonitor` | yes |
| `CAVERN` / `BURROW` / `OCEAN` / `DESERTSTRUCT` / `SNOWSTRUCT` | `TileEntityStructureController.triggerProximity` via `StructureType.progress`, plus the `OverworldStructureFeature` loot chests | yes |

No prerequisite needed a new acquisition seam. `DIMENSION` itself is granted by arriving, exactly as
upstream.

**New content registered.** `chromaticraft:ender` fluid vertical (fluid, flowing fluid, FluidType at
the source's viscosity 2000 / density 1500 / temperature 270 / luminosity 4, `liquid_ender` block with
the exact ender-effect toss behind `ChromaOptions.ENDEREFFECT`, and the authoritative V33a
`ender`/`flowingender` sprites). `PROXIMAL_ESSENCE` and `PURE_PROXIMAL_ESSENCE` — V33a's
`bedrockloot`/`bedrockloot2`, the only tuning currency — with their sprites cropped from
`items_resource.png` indices 154 and 155 and their V33a display names. **Their acquisition path is not
implemented:** upstream they drop from Proxima's bedrock cracks, which is dimension content.

**One genuine fix outside the cluster.** `ChromaParticle.FadeGlow.tick()` overrode `tick()` without
the per-tick gravity step, which silently pinned every `setGravity()` caller to straight-line motion —
the glow daisy and glow root motes were already written to rise and could not. The step is restored.

**Focused validation:**

```text
.\gradlew.bat :ChromatiCraft:compileJava --console=plain
BUILD SUCCESSFUL

.\gradlew.bat :ChromatiCraft:runServerData :ChromatiCraft:runClientData --console=plain
BUILD SUCCESSFUL

.\gradlew.bat :ChromatiCraft:runGameTest \
  -PgameTestSelector=chromaticraft:portal_structure_and_charge --console=plain
All 1 required tests passed

.\gradlew.bat :ChromatiCraft:runGameTest \
  -PgameTestSelector=chromaticraft:portal_entry_rules --console=plain
All 1 required tests passed
```

`portal_structure_and_charge` places the generated NBT, proves the anchor is the pad centre, proves a
portal without its Ender Crystals does not validate and one with them does, measures the charge rate
across real ticks as exactly one per tick, proves a fully qualified player is still refused below 300,
checks both tuning formulas and the 40% carry-over, round-trips structure state / charge / tuning /
ownership through `saveWithFullMetadata`, then breaks and restores the Pylon Focus cell to prove
invalidation and revalidation. `portal_entry_rules` proves the empty collision shape, the inward walk
from an edge cell, the lethal 500 fall distance, item rejection versus essence absorption, and that the
Manipulator dismantles all nine blocks and drops exactly nine rifts.

While fixing the charge test, one real port bug surfaced and was fixed: `getTicker` must call **both**
`BlockEntityBase.updateEntity()` (lifecycle) and `updateEntity(level, pos)` (the tile body). Calling
only the first leaves the rift permanently uncharged.

**Cannot be verified headlessly — in-world checklist.**

1. `RenderCrystalPortal` is **not ported**; the rift is currently invisible in world. Its BER is the
   next presentation slice.
2. `PortalItemRenderer` is **not ported**. Upstream draws the held/inventory rift as an unlit cube
   textured with vanilla `textures/entity/end_portal.png`, which is not on the 26.2 block atlas and
   therefore needs a special item renderer rather than a model. The item currently uses the
   particle-only world definition and looks blank. No substitute sprite was invented.
3. The three particle families (`spawnPortalIdle`, `spawnPortalCharging`, `spawnPortalActive`) are
   ported but only visually confirmable in game.
4. The 90-tick `ChromaSounds.PORTAL` loop, the RIFT/POWERDOWN dismantle cues and the two GOTODIM
   arrival branches need an audible check.
5. Liquid Ender's still/flow sprites and the ender-effect toss need an in-world check.

**Deliberately deferred.** All Proxima puzzle structures and their generators, as instructed.

### Proxima dimension: audit and the exact next slice

The dimension itself did **not** land in this slice, and no placeholder was registered. Registering a
dimension whose chunk generator substitutes Overworld terrain would be exactly the unreachable shell
the previous checkpoint warned against, so the only Proxima code added here is what the portal genuinely
needs: `ChromaDimensions` (the `proxima` dimension/stem/type resource keys), `ProximaGenerators` (the
real `areGeneratorsReady` gate), `ChromaTeleporter`, `DimensionTuningManager` and
`CheatingPreventionSystem`. `BlockChromaPortal.isDimensionLoadable` asks the server for
`chromaticraft:proxima` and correctly answers false today, so the rift charges, validates, refuses
travel and says so — nothing pretends to work.

The audit needed to build it, so the next slice does not have to re-derive it:

**Dimension type** (from `WorldProviderChroma`): name "Proxima"; height 256; `isSurfaceWorld` true, so
sky light is on; `calculateCelestialAngle` pinned to 0.5 and `isDaytime()` false, i.e. a fixed sky;
`canRespawnHere` false, so beds and respawn anchors do not work; `canBlockFreeze` and `canSnowAt` both
false; `getMovementFactor` 1, so coordinate scale is 1.0; `getVoidFogYFactor` 0.0001; spawn point
`(0, 1024, 0)`; average ground level is the vanilla value plus
`ChunkProviderChroma.VERTICAL_OFFSET = 48`. Its sky, cloud and weather renderers are three separate
classes under `world/dimension/rendering`.

**Biome source.** Nine primary biomes with spawn weights and base height deltas — Crystal Plains
(8, 0), Iridescent Archipelago (6, -5), Lumen Skylands (2, 0), Glowing Forest (10, +10), Sparkling
Sands (4, 0), Radiant Fissures (3, 0), plus the three zero-weight technical biomes Structure Field,
Luminescent Sanctuary and Monument Field — and four sub-biomes: Crystal Mountains (0.75, 0), Aura Ocean
(0.4, -30), Crystal Forest (0.2, +15) and Voidland (0.1, +8). They are **not** a vanilla parameter-list
source: `BiomeDistributor` is a threaded generator that paints a global 4096x4096 byte map by spreading
blobs, with `LobulatedCurve` regions for the monument and each element's structure field. A faithful
26.2 port needs a custom `BiomeSource` reading that map, plus `RegionMapper`, plus the per-biome
`ChromaDimensionBiomeTerrainShaper` implementations in `world/dimension/terrain` (islands, crystal
mountain, glowing cracks, skyland canyons, sparkling sands).

**Chunk generator.** `ChunkProviderChroma` is a fully custom per-column generator: noise layers, then
`shiftTerrainGen` lifting the whole column by `VERTICAL_OFFSET` and filling stone beneath, then a
per-biome replacement pass with sand beaches, surface grass and a bedrock layer, then decorators. It
cannot be expressed as vanilla noise settings.

**A non-obvious dependency, found while auditing.** V33a's `ThreadedGenerators.isDependentOn` makes
both `BIOME` and `REGION` depend on `STRUCTURE`, and the dependency is real, not nominal:
`RegionMapper.run()` blocks on `StructureCalculator.arePositionsDetermined()` and sizes the central
region from `getMaximumDistanceFromOrigin()`, and `BiomeDistributor` paints its Structure Field and
Monument Field biomes around those same positions using `LobulatedCurve` blobs per element. **Proxima's
biome layout therefore cannot be ported before the puzzle-structure position calculator.** That does
not un-defer the puzzles: `StructureCalculator` needs only the structure identities, sizes and
placement rules from `DimensionStructureType`, not any puzzle mechanics. The natural boundary is to
port the structure *registry and placement*, leave every generator's contents unported, and come back
for the puzzles later.

**Recommended next slice, in order.** (1) `DimensionStructureType` identities/sizes and
`StructureCalculator` placement only — no puzzle contents — registering into `ProximaGenerators` as
`STRUCTURE`; (2) `RegionMapper`, which unblocks on it; (3) `BiomeDistributor` and its 4096x4096 blob
map; (4) the nine plus four biomes as datagen-registered `Biome` entries with the source's colours,
behind a custom `BiomeSource` reading that map; (5) the `ChunkGenerator` with the
`world/dimension/terrain` shapers and `ChunkProviderChroma`'s vertical-offset/surface/bedrock passes;
(6) dimension type and level stem datagen, which flips `isDimensionLoadable` true and unblocks the
remaining portal GameTests (progression transition, teleport cooldown, return/safe-arrival
serialization); (7) `DimensionJoinHandler`'s arrival carve-out (the r=5 / rh=3.5 ellipsoid cleared to
air with a Cloak Shielding shell) and the two real exit paths (death and the y < -1024 void fall);
(8) non-puzzle decoration, entities, bedrock cracks (which is where Proximal Essence actually comes
from) and the sky/cloud/weather renderers.

### 2026-08-15 — Proxima structure types and placement

The first half of the dimension's dependency chain. `DimensionStructureType` and the placement half of
`StructureCalculator` are ported; the puzzle generators they place remain deliberately deferred.

**Why this is separable at all.** V33a's `DimensionStructureGenerator.startCalculate` sets
`entryX`/`entryZ` to the placement coordinate up front and only lets the puzzle's own `calculate()`
refine them afterwards. So a placement is a complete, usable answer before any puzzle exists — which
is exactly what `RegionMapper` (blocks on `arePositionsDetermined()`, sizes the central region from
`getMaximumDistanceFromOrigin()`) and `BiomeDistributor` (paints Structure Field and Monument Field
around the placements) consume. Nothing downstream reads puzzle contents.

**`DimensionStructureType`** is lifted out of the 631-line generator base into its own type: the
eighteen identities in source order with their exact display names, `getIconIndex()` (8 + ordinal),
per-player completion on the death-persistent research tag under V33a's own `structuresCompleted` /
`struct_<ordinal>` keys, the UUID-keyed generator cache and `resetCachedGenerators`, and
`StructureTypeData` with the verbatim password formula.

`isComplete()` is the real V33a gate rather than a placeholder. Upstream decides it by constructing
each generator once at class-init, running `startCalculate` at the origin and asking whether it
produced a core; anything that fails is filtered out of `getUsableStructures()` and never assigned.
That is precisely why `allowUnfinishedStructures` exists — Reika shipped with unfinished generators.
This port is in the same position with all eighteen unported, so the same filter correctly yields
nothing today. `registerGenerator` is the modern equivalent of that class-init test run: it exercises
the factory once and only marks the type usable if the run produced a core, so a ported-but-broken
generator is rejected exactly as upstream rejects an unfinished one. A type becomes usable the moment
its generator lands; no other code changes.

**`StructureCalculator` placement.** Type assignment draws without replacement from the usable set and
bumps `generationIndex` when the set is exhausted and refilled, so two elements sharing a type still
get distinct passwords. The ring centre wanders +-6000 from the world origin, each element sits on its
own 22.5-degree spoke at 5000+-3000, and the monument sits at the ring centre. `StructurePair` becomes
a `StructurePlacement` carrying the element, type, generation index and placement, plus the generator
once one exists; `getEntryPosX/Z` returns the generator's entry when there is one and the placement
otherwise, which is the same answer upstream gives before `calculate()` runs.
`getMaximumDistanceFromOrigin`, `getMaximumPossibleDistance`, `getNearestStructureWithinRange` and the
client-side `getStructureColorTypes` are all ported.

**The two random sources are preserved separately.** `seededRand`, from a per-installation file seed,
picks which structure type each element gets — that file seed is what `assignSeed` ships to clients so
they can rebuild the same colour-to-type map without knowing the world. The dimension-seeded `rand`
picks the ring geometry. Upstream's own comment says it wanted the world seed for the former and could
not reach one from outside a world; 26.2 no longer prevents that, but changing it would change every
existing assignment, so it is recorded as a future option rather than silently altered.

**One deliberate, documented deviation.** V33a computes `structureOriginX/Z` and every structure's
radius through the two-argument `ReikaRandomHelper.getRandomPlusMinus`, which draws from DragonAPI's
**global unseeded** Random. The surrounding class carefully seeds two Randoms and then uses neither for
those three values, so each recomputation of the layout would move the structures. This port passes the
seeded `rand` to the three-argument overload. The distribution is unchanged; only determinism is, and
worldgen that is not reproducible from its seed is not portable. The GameTest asserts the
reproducibility directly.

**Two latent bugs fixed in passing.**

1. `ProximaGenerators` started with an *empty* pending set, so `areGeneratorsReady()` answered true
   before anything had run. It was masked only by `isDimensionLoadable()` being false, and would have
   surfaced the moment the dimension registered — letting the Portal Rift carry a player into a Proxima
   whose layout had not been decided. The set now starts full; the gate can only open by generators
   reporting in. `StructureCalculator` clears the `STRUCTURE` bit as soon as positions are published,
   which is where upstream releases `RegionMapper`'s poll loop.
2. DragonAPI's `ReikaFileReader.HashType.hashBytes` returned **null**: its hex step was
   `javax.xml.bind.DatatypeConverter.printHexBinary`, which left the JDK in 11, and had been commented
   out. Every caller got null, and `StructureTypeData.getPassword` would have thrown on
   `null.hashCode()`. Restored with `HexFormat.of().withUpperCase()`, which is the exact replacement —
   the case matters because the password takes `String.hashCode()` of the result.

**Focused validation:**

```text
.\gradlew.bat :DragonAPI:compileJava :ChromatiCraft:compileJava --console=plain
BUILD SUCCESSFUL

.\gradlew.bat :ChromatiCraft:runGameTest \
  -PgameTestSelector=chromaticraft:proxima_structure_placement --console=plain
All 1 required tests passed

.\gradlew.bat :ChromatiCraft:runGameTest \
  -PgameTestSelector=chromaticraft:portal_structure_and_charge --console=plain
All 1 required tests passed
```

`proxima_structure_placement` proves that no structure type is usable while the puzzles are unported,
that the ring still resolves its origin in that state so the biome layer is not blocked, and that
finishing placement clears the `STRUCTURE` gate bit. Under V33a's `allowUnfinishedStructures` dev
override it then proves all sixteen elements are assigned exactly once from eighteen types without
reuse, the centre stays inside +-6000, the monument sits at that centre, every structure lands on the
5000+-3000 ring on its own 22.5-degree spoke, an unported generator leaves the entry at the placement,
`getMaximumDistanceFromOrigin` bounds every placement, the whole ring is reproducible from the
dimension seed while a different seed moves it, and `getNearestStructureWithinRange` answers correctly
at zero and at 100 blocks. It finally exercises the structure password for stability, version
dependence, per-structure difference and the Reika-UUID fallback — which is also the regression guard
on the `HashType` fix.

**Still deferred:** all eighteen puzzle generators. `registerGenerator` is the single seam; nothing
else has to change when they land.

**Next:** `RegionMapper` is now unblocked — it needs only `arePositionsDetermined()` and
`getMaximumDistanceFromOrigin()`, both live — followed by `BiomeDistributor`'s 4096x4096 blob map, the
thirteen biomes plus the custom `BiomeSource` over it, the `ChunkGenerator` with the
`world/dimension/terrain` shapers, and finally the dimension type and level stem datagen that flips
`isDimensionLoadable` true.

### 2026-08-15 — Proxima central region

`RegionMapper` is ported, closing the second link of the dimension's dependency chain.

**What it is.** A single twenty-lobe closed curve around the world origin, sized so it always encloses
every puzzle structure with V33a's buffer: inner radius `maxStructureDistance + 200`, outer
`maxStructureDistance + 1500`. Everything inside is the dimension proper; stepping outside is what
`OuterRegionsEvents` reacts to, and `BiomeDistributor` paints the whole interior as the Luminescent
Sanctuary biome unless a structure blob claims that cell first. `SkyRiverGenerator` measures its
rivers from `MAX_BUFFER`, so that constant stays public.

It is the reason the placement slice had to land first: the boundary cannot know its own size until
the structure ring's centre is chosen, and it reads that through
`StructureCalculator.getMaximumDistanceFromOrigin()`. The lobe geometry, the twenty-lobe count, the
buffer distances and the seeded generation are all upstream's.

**The wait loop is deliberately gone.** V33a runs its generators on separate threads, so `run()` polls
`calc.arePositionsDetermined()` in a `Thread.sleep(50)` loop until the structure calculator releases
it. The port runs them in order instead — `ProximaGenerators.Generator` declares the dependency and
`StructureCalculator` clears its bit as soon as positions are published — so the loop would be dead
code, and a sleep loop on a worldgen path is exactly the kind of thing that deadlocks a chunk build.
Handing an unfinished calculator to `generate` is an ordering mistake and now fails loudly rather than
sizing the region off an origin of zero. The static accessor answers false before the region exists,
so a query racing generation cannot crash a chunk build either.

The curve lives on the instance rather than in a static field so it is testable; the static
`isPointInCentralRegion` that every V33a consumer calls is retained and answers for whichever instance
is active, which is the same single-level assumption upstream makes.

**Focused validation:**

```text
.\gradlew.bat :ChromatiCraft:compileJava --console=plain
BUILD SUCCESSFUL

.\gradlew.bat :ChromatiCraft:runGameTest \
  -PgameTestSelector=chromaticraft:proxima_central_region --console=plain
All 1 required tests passed

.\gradlew.bat :ChromatiCraft:runGameTest \
  -PgameTestSelector=chromaticraft:proxima_structure_placement --console=plain
All 1 required tests passed
```

`proxima_central_region` proves that generating before the ring exists is rejected, that nothing
reports as inside an ungenerated region, that finishing clears the `REGION` gate bit, that the
boundary stays within the 200-1500 block buffer at every bearing sampled around the full circle, that
every placed structure and the world origin fall inside while a point past the outer radius does not,
and that the boundary is reproducible from the dimension seed while a different seed reshapes it.

**Next:** `BiomeDistributor` — the 4096x4096 blob-spread biome map, with `LobulatedCurve` regions for
the monument and each element's structure field. It consumes the placements and the central region,
both of which are now live, and is the last generator in the gate before the biome and terrain layers
can be datagen-registered.

### 2026-08-15 — Proxima biome distribution and the generator gate

`BiomeDistributor` is ported, which closes the last generator in the gate. Proxima's global layout —
structure ring, central region, biome map — now computes end to end.

**What the distributor is.** Not a noise field. The biomes are painted once into a square byte map
that tiles over the world, in five stages: scatter `spawnWeight` blobs per primary biome onto empty
cells; fill every remaining empty cell from a weighted count of its 13x13 neighbourhood, in shuffled
order, repeating until nothing is empty; feather away any cell matching none of its four orthogonal
neighbours; stamp a sub-biome blob inside a parent blob with the sub-biome's own probability, over
parent cells only; and build one irregular 96-384 block region per element plus one for the monument.
`getBiome` then answers in priority order — structure or monument region, then the central region's
Luminescent Sanctuary, then the painted map.

The two numbers on each biome are the whole distribution contract: `spawnWeight` is literally the blob
count for a primary (so Glowing Forest's 10 outnumber Lumen Skylands' 2, and the three zero-weight
technical biomes are never scattered) and a probability for a sub-biome (Crystal Mountains appear in
three quarters of Crystal Plains blobs).

**Biome identities.** `ProximaBiomeType`, `ProximaBiomes` (nine primaries) and `ProximaSubBiomes`
(four sub-biomes) port V33a's `ChromaDimensionManager.Biomes`/`SubBiomes` with their exact display
names, spawn weights, base height deltas, parent links, `isWaterBiome`, `isReasonablyFlat`,
`isTechnical` and `isFarRegions`. Upstream returns a live 1.7.10 biome object with a numeric id;
26.2 biomes are data, so the type carries a `ResourceKey<Biome>` and the `Biome` it names is
registered by datagen later. That split is exactly what lets the distribution land before the biome
definitions. They are named after the dimension rather than kept as V33a's bare `Biomes`, which would
collide with `net.minecraft.world.level.biome.Biomes` at every use site.

**Three departures, all deliberate.**

1. *The spreader machinery is dropped as dead code.* V33a declares a `Spreader` inner class and a
   `spreadDots()` stage, but the only code that would add a spreader is commented out in
   `distributeDots`, so the collection is always empty and the stage is always a no-op.
2. *Two nondeterminism sources removed.* `Collections.shuffle(li)` uses a shared unseeded Random, and
   `CountMap.asWeightedRandom().getRandomEntry()` is worse — `WeightedRandom` rolls its own unseeded
   `RandomSource` **and** walks a `HashMap` keyed by enums, whose iteration order follows identity
   hash codes and so differs between JVM runs. Between them they decide the biome of every gap cell on
   the map, so Proxima would have regenerated differently from the same seed. The shuffle is now
   seeded and the weighted draw is done inline over the compact biome index in ascending order. Same
   distribution, reproducible. The GameTest asserts it, and it caught the second one.
3. *The wait loop is gone*, as in `RegionMapper`: mis-ordered calls fail loudly instead of sleeping.

**The map size is a parameter.** V33a's own `SIZE = 4096;//2048;//4096;` shows it was varied, and
`placeBlob` already scales every radius by `SIZE/4096`. Making it a constructor argument is what lets
the focused test exercise the identical algorithm at 512 cells in about two seconds.

**Measured, and then fixed: the production map costs ~19 seconds.** A 4096x4096 paint is 33 blobs of
720 half-degree rays each, plus repeated full-map scans. Left synchronous that is a flat server stall,
which is precisely why V33a runs all five of its generators on threads. `ProximaGenerators` therefore
gained the orchestration it was always the gate for: `regenerate(seed)` marks every bit pending and
runs the chain off-thread in dependency order, publishing a single `Layout` record so a consumer sees
either a complete Proxima or none of it; `generateNow(seed)` is the same chain on the calling thread
for datagen and tests. The Portal Rift's existing "generators ready" check — which keeps a rift
charging and refusing travel while any bit is set — is the mechanism that makes the wait invisible to a
player, exactly as upstream intended.

The portal's own eligibility assertion was tightened while this landed: it compared against
`isDimensionLoadable` alone, which ignored the generator half of `isPortalFunctional`. Both halves now
have to hold.

**Focused validation:**

```text
.\gradlew.bat :ChromatiCraft:compileJava --console=plain
BUILD SUCCESSFUL

.\gradlew.bat :ChromatiCraft:runGameTest -PgameTestSelector=chromaticraft:proxima_biome_distribution
All 1 required tests passed
.\gradlew.bat :ChromatiCraft:runGameTest -PgameTestSelector=chromaticraft:proxima_generator_gate
All 1 required tests passed
.\gradlew.bat :ChromatiCraft:runGameTest -PgameTestSelector=chromaticraft:proxima_central_region
All 1 required tests passed
.\gradlew.bat :ChromatiCraft:runGameTest -PgameTestSelector=chromaticraft:proxima_structure_placement
All 1 required tests passed
.\gradlew.bat :ChromatiCraft:runGameTest -PgameTestSelector=chromaticraft:portal_structure_and_charge
All 1 required tests passed
```

`proxima_biome_distribution` proves distributing before the ring exists is rejected, the BIOME bit
clears and the whole gate then opens, no cell is left unpainted, every biome gets exactly its spawn
weight in blobs while the three technical biomes get none, every sub-biome blob belongs to its
declared parent, the query priority resolves the monument position to Monument Field and a structure
entry to Structure Field and the origin to the central region while a far-away point falls through to
the painted map, the coarse client packet is every eighth cell, and the map is reproducible from the
dimension seed while a different seed repaints it.

`proxima_generator_gate` proves the gate starts closed with every member pending, that the full chain
opens it, that the layout is published as one object, that each stage is consistent with the one
before it (the region encloses this run's monument, the biome map is painted around this run's
monument), and that the orchestrated run uses the production map size — so it exercises the real
19-second path end to end rather than the test-sized one.

**Where the gate stands.** `STRUCTURE`, `REGION` and `BIOME` all clear. `SKYRIVER` and
`FISSUREPATTERNS` join `Generator` when their generators land, and the portal will start waiting on
them again automatically.

**Next:** the biomes themselves as datagen-registered `Biome` entries behind a custom `BiomeSource`
reading this map, then the `ChunkGenerator` with the `world/dimension/terrain` shapers and
`ChunkProviderChroma`'s vertical-offset/surface/bedrock passes, then the dimension type and level stem
datagen that finally flips `isDimensionLoadable` true and unblocks the remaining portal GameTests.

### 2026-08-15 — Proxima biome definitions and biome source

All thirteen Proxima biomes are now real registered biomes, and a `BiomeSource` resolves them out of
the painted map.

**The biome definitions.** V33a's biome classes are almost entirely shared: every one extends
`ChromaDimensionBiome`, which disables rain, empties all four spawn lists, and takes both its water
and grass colour from the overworld Rainbow Forest. Exactly four facts differ across the whole family,
and all four are reproduced:

| Fact | Source |
|---|---|
| Aura Ocean is the only biome where it rains | `BiomeGenChromaOcean` sets `enableRain = true` |
| Luminescent Sanctuary is the only biome with a spawn — one Tunnel Nuker, weight 1, group 1-1 | `BiomeGenCentral.initSpawnRules` |
| Structure Field brightens its grass and uses white water | `StructureBiome` |
| Monument Field replaces the grass base with `0x77ccff` | `MonumentBiome.getBaseColor` |

Verified in the emitted JSON: `aura_ocean` is the only `has_precipitation: true`; every biome carries
water `#00ffff` and grass `#79c05a` from the Rainbow Forest except `structure_field` (`#ffffff` /
`#79ff5a`) and `monument_field` (`#ffffff` / `#77ccff`); `luminescent_sanctuary` is the only one with
a spawner entry. Temperature and downfall are 0.5, the `BiomeGenBase` defaults, because no Proxima
biome ever sets them.

**What is static here and dynamic upstream.** V33a computes grass colour per position: the base is
modulated by a simplex field into the green channel between 1.0x and 1.5x, and Structure/Monument
Fields additionally scan four blocks in each cardinal direction and switch to a highlight colour at
their own edge, blending toward it by a second noise field. A 26.2 `BiomeSpecialEffects` grass
override is a single constant, so the base colour lives in the biome and the per-position modulation
belongs to a client colour resolver alongside the existing `ChromaBlockColors`. That is genuine
remaining work, recorded as such — the constants are upstream's own bases, not approximations of the
modulated result.

**Deliberately empty.** Generation settings carry no features or carvers, because Proxima's decoration
does not come from biome features at all: it comes from `DimensionGenerators`/`DecoratorChroma`, which
run off the chunk generator and land with it. Attaching vanilla features would be inventing content.
The sky is left at its default for the same reason — upstream draws it with a custom
`ChromaSkyRenderer`, not a per-biome colour.

**The biome source.** `ProximaBiomeSource` is a pure read of the painted map. Proxima uses no climate
parameters whatsoever — V33a answers every query from `BiomeDistributor.getBiome(x, z)`, which
consults the structure and monument regions, then the central region, then the tiling map — so the
source ignores the `Climate.Sampler` entirely and converts quart coordinates back to blocks. Its codec
is registered into `Registries.BIOME_SOURCE` so a level stem can name it.

Before the map is painted the source answers with the Luminescent Sanctuary. That is a defined answer
for a race rather than a stand-in: it is what the world origin resolves to anyway, and it is
unreachable in practice because the Portal Rift refuses to carry anyone while the generator gate is
closed.

**Focused validation:**

```text
.\gradlew.bat :ChromatiCraft:compileJava --console=plain
BUILD SUCCESSFUL

.\gradlew.bat :ChromatiCraft:runServerData :ChromatiCraft:runClientData --console=plain
BUILD SUCCESSFUL (thirteen new worldgen/biome entries)

.\gradlew.bat :ChromatiCraft:runGameTest -PgameTestSelector=chromaticraft:proxima_biome_source
All 1 required tests passed
.\gradlew.bat :ChromatiCraft:runGameTest -PgameTestSelector=chromaticraft:proxima_biome_distribution
All 1 required tests passed
.\gradlew.bat :ChromatiCraft:runGameTest -PgameTestSelector=chromaticraft:proxima_generator_gate
All 1 required tests passed
```

`proxima_biome_source` proves all thirteen keys resolve to registered biomes, that only Aura Ocean has
precipitation, that only the Luminescent Sanctuary has a spawn, that the source advertises every
biome (a chunk carrying an unadvertised biome fails serialization), that the monument position and the
world origin resolve correctly through the source, that sixty-four scattered queries all land on
registered biomes, and that the documented pre-generation fallback holds.

**Next:** the `ChunkGenerator` — `ChunkProviderChroma`'s noise layers, the `VERTICAL_OFFSET = 48`
column lift with stone fill beneath, the per-biome surface replacement with sand beaches and grass,
and the bedrock layer — reading terrain shape from the `world/dimension/terrain` shapers. After that
the dimension type and level stem datagen, which is what finally flips `isDimensionLoadable` true and
unblocks the remaining portal GameTests.

### 2026-08-15 — Proxima per-position grass and foliage colour

The piece the biome slice recorded as outstanding. `ProximaBiomeColors` restores V33a's positional
modulation, using the same mechanism `LuminousCliffsColors` already uses for the overworld cliffs: the
biome definitions keep the base colour, and a wrapped `BlockTintSource` applies the modulation with
the real position during chunk mesh baking.

- Ordinary Proxima biomes: the green channel of the biome's grass wanders between 1.0x and 1.5x on a
  one-eighth-scale simplex field (`ChromaDimensionBiome.getBiomeGrassColor`).
- Structure Field: base and highlight (`multiplyChannels(base, 1, 2.5, 1.5)`) blended by a
  quarter-scale field, with the highlight taking over at the region's edge (`StructureBiome`).
- Monument Field: the same, with `0x77ccff` and `0xff77cc` mixed over those two by a 1/32 field raised
  to the 1.5 (`MonumentBiome`).

Two decisions worth recording. **The edge rule is feathered, not stepped.** Upstream scans four blocks
in each cardinal direction and switches to the highlight outright; that worked because the underlying
grass colour was itself unblended, but in 26.2 the base arrives already feathered across the biome
border by the client's blend radius, so a hard switch would leave a seam against it. A biome-presence
`ColorResolver` — blended by that same radius — supplies a strength that is 0 outside, 1 well inside
and feathered exactly across the region upstream was scanning, and the highlight is mixed in by it.
Same two end states, no seam. Three families are packed one per colour channel so each feathers
independently. **The structure colours are computed from `ChromaBiomes.FOREST_GRASS` directly**,
because V33a's `getBaseColor` is explicitly the Rainbow Forest's grass rather than the biome's own;
re-applying the brightening on top of the static override would double-count it.

`:ChromatiCraft:compileJava` and `:ChromatiCraft:runClientData` are green. This is a rendering
behaviour with no headless assertion available, so it joins the in-world checklist: grass and foliage
in Proxima should wander in green, the Structure Field should read brighter with a cyan-ward edge, and
the Monument Field should wash between blue and pink.

### 2026-08-15 — Proxima terrain profile, and the chunk generator's blocker

`ProximaTerrainProfile` ports the one part of `ChunkProviderChroma.applyNoiseLayers` that is actually
V33a's own. Everything else in that method is 1.7.10's `ChunkProviderGenerate` pipeline verbatim; what
upstream replaces is the pair of values vanilla reads from the biome — `f3` the base height and `f4`
the height variation — with a purely radial profile:

```text
f0 = sqrt((qx*qx + qz*qz) / (65536 * 32)) * 0.03125
if (distanceToNearestStructureInChunks <= 8) f0 *= distance / 8
f3 = max(-0.25, 0.125 - f0 * 0.125)
f4 = 0.5 * f0
```

That is the shape of the entire dimension. At the origin `f0` is zero, so the base height sits at its
maximum and the variation at nothing — the centre is a flat plain. Outward, the base height falls
towards its `-0.25` floor while the variation climbs without limit, so the land drops away and grows
progressively more mountainous, which is what makes the outer regions read as the edge of the world.
Within eight chunks of any structure or the monument the term is damped linearly to zero, and that is
what flattens the ground each puzzle stands on — which is why the profile has to consult the structure
ring rather than being a pure function of position.

Two details preserved rather than tidied: the noise layer works in **quart** coordinates (V33a calls
`applyNoiseLayers(chunkX * 4, chunkZ * 4)` and then names the parameters `chunkX`/`chunkZ`) while the
structure distance is measured in real chunks, so the two are named distinctly here; and the
commented-out 7x7 biome-weighted-average variant beside it is deliberately not ported, because
upstream does not run it. The per-biome `baseHeightDelta` values exist for the terrain shapers instead.

`chromaticraft:proxima_terrain_profile` passes: the origin is flat at the maximum base height,
roughness grows monotonically with radius outside the structure skirts, the base height bottoms out at
exactly `-0.25` while variation keeps climbing, every sampled radius stays inside V33a's bounds, a
structure's own chunk is perfectly flat and ramps back up across its eight-chunk skirt, and the
monument competes with the structures for that damping.

**The chunk generator itself is blocked on a source this workspace does not contain.** The remaining
work in `applyNoiseLayers`/`generateColumnData` is 1.7.10's noise pipeline — `NoiseGeneratorOctaves`
driving `NoiseGeneratorImproved.populateNoiseArray`, then the 5x33x5 to 16x256x16 trilinear
interpolation. 26.2 still ships the same underlying function: `ImprovedNoise` has the identical
permutation setup (`xo/yo/zo = nextDouble()*256`, a 256-byte table shuffled with `nextInt(256-i)`) and
retains the deprecated `noise(x, y, z, yScale, yFudge)` overload precisely so legacy generators stay
bit-identical. What it no longer has is `populateNoiseArray`, the bulk fill that
`generateNoiseOctaves` calls, so reproducing upstream exactly needs the 1.7.10 sources for
`NoiseGeneratorOctaves` and `NoiseGeneratorImproved` to transcribe the octave loop and the y-fudge
argument mapping. Writing them from memory would be an unverifiable approximation of the one thing
that decides every block of terrain, so it stops here rather than guessing.

To unblock it, either add the 1.7.10 vanilla sources for those two classes under `Sources/`, or
empirically pin 26.2's `PerlinNoise.createLegacyForBlendedNoise` plus the deprecated `noise` overload
against known 1.7.10 output. Everything else the generator needs is now in place: the terrain profile,
the biome source, the painted map, the structure ring and the central region.

### 2026-08-15 — the 1.7.10 noise layer, transcribed and cross-checked

The chunk generator's blocker is gone. With the 1.7.10 sources for `NoiseGeneratorOctaves` and
`NoiseGeneratorImproved` to hand, `LegacyOctaveNoise` is an exact transcription of both.

**26.2 still ships the same function.** Inspection confirmed vanilla's `ImprovedNoise` is 1.7.10's
`NoiseGeneratorImproved`: identical `xo/yo/zo = nextDouble()*256` offsets, an identical 256-entry
permutation shuffled by `nextInt(256-i)`, the identical sixteen gradients in the identical order, and
the same x-then-y-then-z lerp. What it no longer exposes is `populateNoiseArray` - the bulk lattice
fill `generateNoiseOctaves` drives - and its permutation table is private, so the sampling half is
transcribed rather than reached through vanilla. The GameTest asserts the two agree at the same
absolute point, which makes that a checked claim rather than an assumption.

**Three things that are easy to get wrong, all pinned by the test.**

1. `populateNoiseArray` *accumulates*; it never writes. That is how the octaves sum, so
   `generateNoiseOctaves` zeroes its buffer once up front and then adds each octave at a halving
   amplitude. The test regenerates into the same buffer and asserts the result reproduces rather than
   doubles, because V33a passes the same 825-entry buffer back every chunk.
2. The index order is x-major, then z, then y - not the x/y/z the parameter names suggest.
3. The `ySize == 1` branch is not an optimisation of the general path. It drops the y term entirely
   and walks the array with no y stride, so a single-cell sample taken through it silently compares
   the wrong function. That cost one debugging cycle here and is now called out in the code.

**A trap worth recording for anyone else porting legacy worldgen.** Vanilla's
`BitRandomSource.nextDouble()` multiplies by the *float* literal `1.110223E-16F`, where
`java.util.Random` uses the exact double `2^-53`. So `LegacyRandomSource` is bit-compatible with
`java.util.Random` for `next(bits)` and `nextInt`, but **not** for `nextDouble` - about 2.2e-8
relative. The permutation tables still match, because they are built from `nextInt` and `nextDouble`
consumes the same two `next()` draws either way; only the three coordinate offsets differ in their low
bits. That is why the port seeds from `java.util.Random` directly rather than routing through
`LegacyRandomSource`, and why the cross-check compensates the offsets instead of expecting bit
equality. The test asserts the discrepancy still exists, so a future vanilla fix surfaces here rather
than silently shifting terrain.

V33a's own generator layout, for the slice that consumes this: `noiseGen1`, `noiseGen2` and
`noiseGen6` at 16 octaves, `noiseGen3` at **96** (vanilla uses 8), `noiseGen5` at 10, the mob-spawner
noise at 8, and `noiseGen4` as a 4-octave `NoiseGeneratorPerlin` for the surface stone noise - all
drawn in that order from one `Random(dimensionSeed)`, so the order is part of the seed. The parabolic
weighting field is `10 / sqrt(j*j + k*k + 0.2)` over a 5x5 window.

`chromaticraft:proxima_legacy_noise` passes.

**Still to do for the generator body:** `NoiseGeneratorPerlin` (the 4-octave surface noise, a separate
1.7.10 class), the 5x33x5 to 16x256x16 trilinear interpolation in `generateColumnData`,
`shiftTerrainGen`'s `VERTICAL_OFFSET = 48` column lift with stone fill beneath, the per-biome surface
replacement with sand beaches and grass, the bedrock layer, and the `ChunkGenerator` subclass with its
codec and level-stem registration.

### 2026-08-15 — Loot Chest trap signal (bug fix)

Reported in-world: opening a Loot Chest did not fire the TNT and redstone traps the structures bury
around it.

**Cause.** V33a's `BlockLootChest` is a trapped chest — `canProvidePower()` returns true,
`isProvidingWeakPower` returns 15 whenever `numPlayersUsing > 0`, and `isProvidingStrongPower` returns
that only for side 1, so a chest sitting on TNT sets it off. The port carried over the *comparator*
output (`hasAnalogOutputSignal`/`getAnalogOutputSignal`) and dropped the power source entirely, so the
chest was inert to redstone.

**Fix, in two halves — both needed.**

1. `BlockLootChest` now implements `isSignalSource`, `ownSignal` and `getDirectSignal`. The signal is
   deliberately binary rather than vanilla's `clamp(openCount, 0, 15)`: upstream returns 15 for any
   non-zero count, so one player arms the trap exactly as hard as five. `getDirectSignal` is UP-only,
   which is the modern spelling of upstream's `side == 1`.
2. `TileEntityLootChest.openerCountChanged` now notifies neighbours. V33a follows every
   `openInventory`/`closeInventory` with `causeAdjacentUpdates`; without it the signal is computed
   correctly but nothing ever re-reads it, so the trap still never fires. `pos.below()` is notified
   too, matching the strong-power rule and vanilla's own `TrappedChestBlockEntity`.

**Regression guard.** `chromaticraft:loot_chest_trap_signal` asserts the chest is a signal source, that
a closed chest emits nothing, that an open one emits a flat 15 on every side, that strong power is
side-1 only, and — the part that matters — that real redstone dust beside the chest powers on open and
drops on close. That last assertion was verified to fail when the neighbour notification is removed, so
it guards the subtle half rather than only the obvious one.

### 2026-08-15 — Proxima terrain the modern way, and the dimension registered

Correction to the previous entry: transcribing 1.7.10's `NoiseGeneratorOctaves`/`NoiseGeneratorImproved`
was the wrong approach and has been removed. Re-reading upstream with that lens,
`ChunkProviderChroma` is 1.7.10's own `ChunkProviderGenerate` with **exactly two values substituted** —
`f3` the base height and `f4` the height variation, which vanilla otherwise reads from the biome.
Reproducing 1.7.10's Perlin to recover vanilla's own formula was copying the parts that were never
modified. 26.2 expresses those same two concepts as the *offset* and *factor* inputs of the noise
router, so the port supplies only those and vanilla owns the noise, the interpolation, the cell
lattice, the surface pass and the bedrock.

**`ProximaTerrainDensityFunction`** is a real `DensityFunction` with `OFFSET`/`FACTOR` modes, a codec,
and registration in `Registries.DENSITY_FUNCTION_TYPE`. Two conversions matter:

- `offset` is V33a's own `f1 = (f3*4 - 1)/8`, the term its column formula actually adds. Range
  `[-0.25, -0.0625]`: highest at the centre, lowest far out.
- `factor` is `1 / (0.9*f4 + 0.1)` — **a reciprocal**, because V33a *divides* its vertical slope by
  that quantity where 26.2 *multiplies* depth by the factor. Getting this the wrong way round would
  produce a mountainous centre and flat outskirts, i.e. exactly the inverse of the dimension. The
  GameTest asserts the flat centre takes the larger factor for that reason.

**Numbers that carry over exactly**, and are asserted or verified in the emitted JSON:

| V33a | 26.2 |
|---|---|
| 5x33x5 noise per chunk = 4-block wide, 8-block tall cells | `NoiseSettings(0, 256, 1, 2)`, giving cell width 4 and height 8 |
| main noise at 684.412, selector at 8.555150/4.277575 | `old_blended_noise` with `xz_scale 1, y_scale 1, xz_factor 80, y_factor 160` — because vanilla computes `684.412*xzScale` and `684.412*xzScale/xzFactor`, and `684.412/80 = 8.55515`, `684.412/160 = 4.277575` |
| `generateColumnData` `byte b0 = 63` | `sea_level 63` |
| `WorldProviderChroma.getHeight` 256 | `min_y 0, height 256` |
| no aquifers, no ore veins | both disabled |

One scale does not carry over and is recorded rather than worked around: V33a raises its selector
generator to 96 octaves where vanilla's `BlendedNoise` is fixed at 8. That count is not exposed, and
working around it would mean replacing vanilla's noise — which is the thing this correction is about.

**Surface rules** replace `replaceBlocksForBiome`: bedrock floor, grass over dirt on the surface, sand
where the surface meets sea level. **`ProximaDimension`** supplies the dimension type from
`WorldProviderChroma` — 0-256 with sky light, a fixed sky from its pinned celestial angle, coordinate
scale 1, ambient light 0, and `canRespawnHere false` expressed as 26.2 now expresses it, through the
`BED_RULE` and `RESPAWN_ANCHOR_WORKS` environment attributes — plus the level stem naming
`NoiseBasedChunkGenerator` over `ProximaBiomeSource`.

**Emitted and hand-checked:** `dimension_type/proxima.json`, `dimension/proxima.json` and
`worldgen/noise_settings/proxima.json`.

**A harness limitation, stated plainly.** `chromaticraft:proxima_dimension_registered` asserts the
noise settings and dimension type resolve out of the live registries with V33a's values, and that the
terrain profile's two ends behave. It does **not** assert that the level itself loads: `LEVEL_STEM` is
read during world creation, and a `GameTestServer` builds its world without consulting datapack
dimension entries — the diagnostic confirmed its stem registry holds only the three vanilla entries
while biomes, noise settings and dimension types all load normally. So `isDimensionLoadable` returning
true, terrain actually generating, and a portal trip completing are **in-world checks**, not headless
ones. Do not read the passing test as proof the dimension boots.

**In-world checklist for this slice.** Create a world, `/execute in chromaticraft:proxima run tp ~ ~ ~`:
the dimension should exist; the origin should be a flat plain around y=63 with bedrock beneath; terrain
should grow steadily more mountainous with distance; the sky should be fixed and lit; beds should
refuse; and a charged Portal Rift should finally carry a qualified player across.

### 2026-08-15 — Shielding TNT trap, and a Nether Temple investigation that did not reproduce

**Shielding sets off adjacent TNT (fixed).** V33a `BlockStructureShield.breakBlock` walks all six
neighbours and, for any TNT it finds, calls `onBlockDestroyedByPlayer(..., 1)` and clears the block —
1.7.10's way of spawning a primed entity in its place. That is the trap behind every Cracked Shielding
a player is invited to mine through, and it was missing. Restored on
`affectNeighborsAfterRemoval`, which is the 26.2 hook explosions and piston moves share, so the trap
fires however the block goes away. Upstream puts it on the block rather than on one material, and this
follows; only the mineable types can reach it in survival anyway.

**Template placement now re-resolves connected shapes.** `NBTStructureLoader.place` wrote each cell and
stopped, where `StructureTemplate.placeInWorld` follows up with
`Block.updateFromNeighbourShapes` plus a neighbour update per placed position. Fences, walls, panes,
iron bars, stairs and vines were therefore placed in whatever shape the template serialised — which is
necessarily the unconnected default — and never corrected. Fixed for all of them.

**The reported Nether Temple redstone fault did not reproduce, and the obvious explanation is wrong.**
Recorded in full so it is not re-investigated from scratch:

- All twenty-eight redstone cells were extracted from `NetherTemple.java` and diffed against the
  generated NBT. Every one is present, at the same coordinate, with a faithful state.
- The metadata mappings were checked against the 1.7.10 sources rather than assumed. Torch metadata
  1/2/3/4 means supported-from-west/east/north/south, i.e. modern `facing` east/west/south/north — the
  importer's table. Repeater `meta & 3` indexes 1.7.10's `Direction.directions = {SOUTH, WEST, NORTH,
  EAST}`, and both versions read their input at `pos + facing`, so `{south, west, north, east}` is
  correct; delay is `((meta >> 2) & 3) + 1`.
- Placing the real template in a GameTest leaves every torch, wire and repeater intact — nothing pops
  for lack of support and nothing is replaced.
- The first hypothesis — that wires stay disconnected because the template stores
  `north/south/east/west = none` — is **false**, and was measured rather than argued: a wire written
  with bare flag 2 still came out `west=side, east=side` with power propagating 15/14/13/12, because
  `LevelChunk.setBlockState` calls `onPlace` regardless of update flags and `RedStoneWireBlock`
  resolves its own connections there. The shape pass above is still correct for other blocks, but it
  is not this fix.
- Neither the feature nor the loader applies any rotation, so a mirrored circuit is also ruled out.

`chromaticraft:structure_trap_and_wiring` covers the Shielding trap and pins the temple's redstone
cells against placement, so if this turns out to be a state or ordering fault it will surface there.
What is still needed to progress it is what specifically looks wrong in game — which block, and what it
should be doing instead.

### 2026-08-18 — Proxima's sky was black because the dimension declared it had none

The custom sky and the sky rivers were both invisible, with nothing in the log and no exception. They
turned out to be two separate faults that presented identically.

**The sky: `Skybox.NONE` removes the hook, not just the drawing.** `ProximaDimension` declared
`DimensionType.Skybox.NONE`, reading it as "vanilla draws no sky here, ChromatiCraft draws its own".
It means something stronger. `LevelRenderer.addSkyPass` wraps its entire body in
`if (state.skybox != Skybox.NONE)`, so with NONE the `sky` frame pass is never added at all — and both
of the places a mod can draw a sky live *inside* that pass:

- NeoForge's patch posts `RenderLevelStageEvent.AfterSky` at the end of the pass's `executes` lambda.
- The `customSkyboxRenderer.renderSky(...)` call that suppresses vanilla's sun, moon and stars is the
  first statement in that same lambda.

So `AfterSky` never fired, `ProximaSkyRenderer` never ran, and there was nothing to log. The fix is
necessarily three-part: the dimension type declares `Skybox.OVERWORLD` (not `END`, which would also
switch on end flashes) so the pass exists; it carries the `neoforge:custom_skybox` environment
attribute naming `chromaticraft:proxima`; and `ProximaSkyRenderer` is now a `CustomSkyboxRenderer`
registered under that name from `RegisterCustomEnvironmentEffectRendererEvent`, returning true from
`renderSky` so vanilla's disc, sunrise, sun, moon, stars and dark disc are all skipped. It returns
true on every path, including the ones that draw nothing, because falling through underground would
put a sun in Proxima's sky. The attribute survives serialisation — `runServerData` emits
`"neoforge:custom_skybox": "chromaticraft:proxima"` into the dimension type, and `skybox` no longer
appears because OVERWORLD is the codec's default.

`@EventBusSubscriber`'s bus was investigated and cleared: since 1.21.1 the `bus()` value is ignored
outright and the bus is chosen per listener from whether the event implements `IModBusEvent`. The
annotations were never the problem, and this does not need re-checking.

**Two draw-path bugs found alongside it.** Both would have bitten the moment either renderer ran:

- `WorldGeometryPass` built its scratch with `ByteBufferBuilder.exactlySized`, which sets the maximum
  capacity equal to the initial one. That buffer cannot grow; overflowing it throws. The sky field
  alone is tens of thousands of vertices against a 4096-vertex allocation. Now a plain
  `new ByteBufferBuilder(...)`, which is an initial size on a growable buffer.
- The sky is built at a fixed radius of roughly 400 blocks, but the far plane is
  `max(renderDistance * 4, cloudRange * 16)` — at a short render distance the whole field sits behind
  it. The sky is now scaled uniformly to fit inside the far plane when it has to be, which preserves
  its appearance exactly since every offset scales with it. Vanilla sidesteps this by drawing its own
  stars at radius 100.

**The sky rivers are a separate fault and are still not explained.** The archived client log of the
reported session (`run/logs/debug-3.log.gz`) answers more than the visible log did, and it rules the
easy answers out rather than in:

- `AutomaticEventSubscriber` logged both handlers being subscribed — `ProximaSkyRenderer.onRenderSky`
  and `SkyRiverRenderer.onRenderLevel`, both "to the game event bus". The annotation was never the
  problem, and `AfterTranslucentBlocks` is posted from `addMainPass` with no skybox guard, so the
  river handler genuinely ran.
- There is no `Maximum capacity of ByteBufferBuilder` anywhere in that session. The fixed-size scratch
  buffer is a real bug and a river view would certainly have overflowed it — one tube segment is 36
  quads, so twenty-eight segments exhaust 4096 vertices — but it was never hit. **The draw was
  therefore never reached with real geometry, and the buffer is not the explanation.**
- `chromaticraft:sky_river_geometry` now asserts what the renderer actually asks for at the one place
  a player is guaranteed to stand, and it passes: the chunk index does hold points within 512 blocks
  of the origin. So "there is no river near spawn" is not the explanation either.

What is left is the client's copy of the rivers being absent — `ClientSkyRivers.get()` returning null
because the layout-seed packet never arrived — or the player never having been in Proxima on the
client when they looked. The log cannot separate those: the only player login it records is into the
Overworld at (20340, 143, 4338), and dimension changes are not logged.

Rather than guess again, both renderers now report once per reason they show nothing — hook reached,
no client copy, none in range with the player's position and the ray count, faded out underground, or
drawing with a count. One client run separates every remaining branch, which is what this fault cost
several sessions of static analysis for want of.

### 2026-08-18 — Proxima's sky draws, and three fidelity faults it exposed

With the sky visible, three things about it were wrong. All three were found by reading V33a's
`ChromaSkyRenderer` and its three sheets rather than by eye, and the sheets are confirmed Reika's own —
`proxima_stars.png`, `proxima_nebulae.png` and `proxima_planets.png` are byte-for-byte
`Textures/stars.png`, `Textures/stars2.png` and `Textures/planets2.png` from `origin/master`.

**The visible tile boxes were filtering, not art.** Every nebula tile fades to black at its own border
— measured, mean luminance under 4 out of 255 on all ten — so a tile drawn additively should have no
visible edge at all. The outline came from sampling past it. Upstream's UVs are the tile's exact
bounds, which is safe under 1.7.10's nearest sampling; here the sampler filters, so a fragment at a
shared edge blends in the neighbouring tile. On the nebula sheet the neighbour is frequently the blank
corner of the sheet — the last six of its sixteen tiles are unused, and they are stored as opaque
*white* with zero alpha. The blend of "black, alpha 1" and "white, alpha 0" is a partly transparent
white, which survives `position_tex_color.fsh`'s `if (color.a == 0.0) discard` and draws as a bright
fringe. Every tile UV is now inset by half a texel, so no sample crosses a tile boundary; the blank
tiles are still emitted, as upstream emits them, and now discard cleanly.

**The nebulae and planets had lost their per-quad rotations.** Upstream wraps each one in its own
`glRotate` chain, and the two chains are quite different in character: the nebulae's is fixed
(`i*8`, `(i%4)*32`, `-i*15+90` degrees) and simply distributes them, while the planets' advances with
time on all three axes at three very different rates — `t/4000`, `t/16000` and `t/240000`. The port
emitted every quad in one batch with no orientation, so the whole field sat wherever the shared
construction seed left it and the planets did not move at all. Both chains are restored, applied on the
CPU per quad rather than as a matrix per draw, since forty-eight render passes a frame to say the same
thing is not worth it.

**Planets were turning with the viewer, and should not.** Upstream calls `renderPlanets` from its own
`push`/`popMatrix` in `render()`, outside the block carrying the position spin, and calls
`renderNebulae` from *inside* `renderStars` after that spin is applied. So stars and nebulae wheel as
the player walks and the planets keep their own orbits regardless. The port applied one matrix to all
three. There are now two.

### 2026-08-18 — Miasma, the sky river tuning gate, and a filled jetpack

**Miasma was drawn as a cube and it is not one.** V33a's `DimensionDecoRenderer.renderEffect` draws it
as three sheets, each emitted twice in opposite winding so both faces show: one horizontal through the
middle of the block, and two vertical crossed on the diagonals. All three are *larger than the block* —
the horizontal one spans -0.5 to 1.5 on both axes, the crossed pair stand a full block above and below
— which is what makes a field of Miasma read as continuous fog instead of a grid. What shipped was
`cube_all` on `layer_0`, and that texture is white at alpha 22, so it was very nearly invisible.

The blue is not in the texture either: upstream tints it per position with
`getModifiedHue(0x0000ff, 220 + 80*sin((x*x*2 + y*y + z*z*8)/2000000))`. That period is enormous on
purpose — the cloud drifts through blues across a landscape, not block to block.

Ported as `MiasmaModel`, a `DynamicBlockStateModel` following the `CaveCrystalModel` pattern: one baked
part of six quads, translucent (upstream's `renderIconInPass` puts Miasma in pass 1 alone), full-bright
(`setBrightness(240)`), no ambient occlusion, tint index 0. The colour reaches it through a
`BlockTintSource` in `ChromaBlockColors`, which is the hook that still sees a position. The texture's
own forty-eight-frame animation comes from the sprite and needs nothing here.

Two traps worth recording. The UVs a `QuadBakingVertexConsumer` wants are fractions, not texels —
`TextureAtlasSprite.getU` interpolates `u0..u1` by its argument, so `16` means sixteen times across the
sprite. And the datagen deco loop had to be stopped from emitting a `cube_all` blockstate for Miasma:
generated blockstates win the resource merge, so the stub would have put the fog straight back in a
box. Its flat model is still generated, because that is what the item in a hand draws.

**Sky rivers refused everyone, and took creative flight with them.** `SKYRIVER`'s thresholds are
(384, 256), so an untuned player scores zero, is ejected on contact and has flight revoked — which is
both "the river does not transport me" and "I cannot fly in creative". Creative and spectator now
bypass the gate and are never stripped of flight. That is a deliberate deviation: upstream's
`ejectPlayer` clears `capabilities.allowFlying` for anyone, and abilities are only rebuilt when the
game mode is next set, so a creative player who brushed a river could not fly again all session. A
survival player is gated exactly as upstream gates them.

The untuned path had also lost its actual behaviour. V33a does not return after ejecting: it overwrites
the move vector with the segment direction and applies it at a multiplier of -1, spitting the player
back out of the mouth they drifted into. The port cut their flight and left them standing.

**A fuelled bedrock jetpack is now in RotaryCraft's creative tools tab**, alongside an empty one. The
pack was only in the catch-all tab, and an empty one is useless until a filling station has been built
and run.

### 2026-08-18 — The Luminescent Sanctuary was empty because CENTER is a rule, not a list

The Sanctuary having no trees and no floor decoration is not correct, and the reason is structural.
V33a decides what generates where in `DimensionGenerators.generateIn`, and its central-biome branch is
a single line that runs before every specific rule:

```java
if (b.biomeType == Biomes.CENTER)
    return !this.isDedicatedBiomeOnly();
```

So the Sanctuary is not a biome with a list of its own — it is *everything not dedicated to somewhere
else*. Against `isDedicatedBiomeOnly` (AURORA, CRYSBUSH, CRYSTALTREE, METEOR, MOONPOOL, TERRAINCRYSTAL,
GLOWBUSH, ARCH, BLOBS, MIASMA, GLASSCLIFFS) and the CHUNKLOADER special case above it, nine generators
should run there: **RIFT, GEODE, JETS, FLOATSTONE, TREES, FORESTS, ALTAR, CRACKS, GLOWCAVE**. Two were
ported — Floatstone and the geode — so it had two.

The port had been expressing the CENTER grant as `central ||` on each individual rule, which is the
same shape but only as long as somebody remembers to write it. That is exactly how the biome fell
behind. It is now written as upstream writes it, with the reason recorded beside it.

**TREES is ported.** `GlowTreeFeature` plus `GlowTreeShapes`: three shapes rolled with equal weight —
oak, tall and ball — on a stem of `nextInt(4)` logs, so a quarter of them start their canopy at ground
level. The shapes are upstream's several hundred literal `placeLeaf`/`setBlock` calls, extracted
mechanically with the per-shape origin shift (`i -= 5` for the ball, 3 for the oak, 2 for the tall)
folded in, so every triple is relative to the top of the stem. Each shape carries its own logs as well
as its leaves, because the trunk continues up through the crown. Both site rules are kept: plantable
ground, and eight blocks of clear air — the second is what stops a stand growing through itself, and is
why a one-in-two chunk chance does not produce fifty trees.

Upstream's per-biome chances are 0.8 in the Iridescent Archipelago, 0.5 in the Sanctuary and by
default, 0.1 in the Sparkling Sands and 0.05 on the Crystal Plains. A placed feature carries one rate,
so the default is the one expressed — it is the Sanctuary's own rate and upstream's fallback for every
biome that does not name another.

One thing deliberately not carried: upstream places leaves with a metadata of `rand.nextInt(5)`, which
selects one of sixteen glow overlay sprites drawn by the second render pass this port has not built.
The block behaves identically across all sixteen, so the value would be written and never read. Same
choice the glowing log already makes.

Still missing from the Sanctuary, and the honest remaining list: **RIFT** (`WorldGenFissure`), **JETS**
(`WorldGenFireJet`, gated on DIMGENTILE), **FORESTS** (`WorldGenTreeCluster`), **ALTAR**
(`WorldGenMiniAltar`), **CRACKS** (`WorldGenGlowingCracks`, also DIMGENTILE) and **GLOWCAVE**
(`WorldGenGlowCave`).

**The GameTest caught the port's own bug immediately.** `canGenerate` was written against
`BlockTags.DIRT`, and in 26.2 that no longer contains grass blocks: `SUBSTRATE_OVERWORLD` is defined
as `DIRT + MUD + MOSS_BLOCKS + GRASS_BLOCKS`, so the three are disjoint. Planted on a grass block, the
feature turned down all thirty-two attempts — which would have meant no glowing tree anywhere in
Proxima, since its surface is grass. It now tests `SUPPORTS_VEGETATION`, that union plus farmland,
which is what `VegetationBlock.mayPlaceOn` itself tests and therefore what
`ReikaPlantHelper.SAPLING.canPlantAt` comes to.

Seven other sites in the port still test `BlockTags.DIRT` as a "is this ground?" check
(`BlockDecoFlower`, `BlockGlowDaisy`, `BlockGlowRoot`, `CrystalPitFeature`, `LumaPatchFeature`, and two
in `PylonFeature`). They are not swept here, because some may legitimately mean bare dirt and each
wants checking against its own V33a original rather than a blanket replace — but the same split
applies to all of them, and any that mean "ground" are silently refusing grass today.

### 2026-08-18 — Proxima's monument, as an NBT template

The monument is now a real structure template rather than nothing at all: nothing in its dependency
chain was allowlisted before this, `ChromaDimensionalAudioHandler` and `MonumentCompletionRitual`
included, so no part of it was building.

`ChromaStructureTemplateProvider.importMonument` takes the geometry from V33a's `MonumentStructure` —
3503 cells across 43x13x43 — plus the live half of `MonumentHighlighter`. The geometry is unusually
simple for its size: exactly two blocks, which upstream names only through the metadata locals `ms`
(Stone Shielding) and `mc` (Cloak Shielding). What makes the monument the monument is what the
highlighter adds on top, and that is in the template rather than left to a runtime pass: the sixteen
runes in their ring at y+11, laid clockwise from (3, 18) one per element — upstream's single block with
metadata 0-15, here the sixteen distinct rune blocks they became — and the structure controller at the
centre, (21, 5, 21).

**The shared setBlock pattern does not match these calls, and would have failed silently.** It requires
both a metadata *and* an update-flags argument; the monument's calls carry only metadata, so the regex
matched none of the 3503 and a regex that matches nothing simply finds nothing. The parse is now a
plain split, which is both simpler and loud about a call shape it does not recognise. The template is
asserted non-empty for the same reason.

One id error worth recording because datagen cannot catch it: the controller block is
`structure_controller`, not `structure_control`. A wrong block id in a template passes datagen happily
and fails at load, so template ids want checking against `ChromaBlocks` rather than against the V33a
class name they came from.

**Deferred, and deliberately: the ritual.** `MonumentCompletionRitual` is a thousand lines of timed
cinematic — a fifty-note melody on wall-clock timings synchronised to six .ogg tracks, ray particles,
a vortex, lightning, camera manipulation and two shader programs — and it depends on a chain that is
itself unported: `TileEntityStructControl`, `TileEntityDimensionCore`, `TileEntityAuraPoint`, four
particle types, `CrystalMusicManager`, and the monument sounds. It also cannot be ported as one class:
it mixes `Minecraft`/`ISound`/`EntityFX` with its server timeline, which is exactly what
`ClientPayloadHandlers` exists to avoid. It wants a common timeline plus a client-only effects half,
and the placement — the monument sits at the ring's own centre, so `RandomSpreadStructurePlacement` is
wrong and it needs a custom `StructurePlacement` like `PylonGridPlacement` — is still to do as well.
The monument existing in the world comes first; a half-ported ritual would be worse than a monument you
can stand in.

### 2026-08-18 — The monument is placed

The template now has somewhere to go. Three pieces, and the placement was the one that needed
inventing.

**`MonumentPlacement`.** Neither vanilla placement fits: `RandomSpreadStructurePlacement` scatters on a
grid and `ConcentricRingsStructurePlacement` lays a ring about the origin, while the monument is a
single structure at a position the dimension's own layout chose —
`StructureCalculator.getMonumentPosition()`, the centre of the ring the puzzle structures are laid
around. So it is its own `StructurePlacementType` whose `isPlacementChunk` answers true for exactly one
chunk. Its codec is a unit: there is nothing to configure, the position is not data, and the datapack
entry is just `{"type": "chromaticraft:monument"}`.

`ProximaGenerators.monumentPosition()` returns **null** before the layout exists rather than a
fallback, and the placement answers no when it does. A monument placed at a guessed position would be
written into a saved chunk and stay wrong for the life of that world — the same failure mode that once
made every Proxima chunk save as Luminescent Sanctuary. In practice the layout is finished long before
any chunk is built.

**`MonumentPiece`.** Upstream's `startCalculate` does three things in sequence and the order is
load-bearing: hollow an ellipsoid of r=32/r2=24 to air, lay grass across the whole square at
`posY - 1` — the whole square, not only the ellipse, so the monument sits on a floor rather than in a
bowl — and only then generate over the top. `posY` is 103, a fixed altitude rather than anything read
from terrain, and the template is anchored at `(x-21, z-21)`, which is what centres a 43-wide template
on the chosen position. Sixty-five blocks of clearing and forty-three of monument are both far wider
than a feature's write window, which is why this is a piece: it is laid out once and painted chunk by
chunk with a clipped box.

The template is placed with vanilla's `StructureTemplate.placeInWorld` rather than
`NBTStructureLoader.place`. The loader exists for the feature case, where the write window is the
constraint and the caller never gets a box; here the box is exactly what is wanted and `placeInWorld`
honours it natively.

Its biome is the Monument Field, which `BiomeDistributor` already paints around the ring's centre — so
the one place the monument may stand is the one place that biome exists, and the structure's biome
filter and its placement agree by construction rather than by coincidence. `TerrainAdjustment.NONE`,
since the piece lays its own floor.

`chromaticraft:proxima_monument_template` pins the part that can fail silently: the template's size and
its exact cell counts — 3503 shielding, sixteen distinct runes, one controller — plus that the
registered structure and set are the monument types rather than a spread. An import whose parse matched
nothing would produce an empty structure that datagen accepts without complaint.

**Still deferred: the ritual.** Unchanged from the previous entry — it needs its dependency chain
inventoried and a common/client split before any of it is written.

### 2026-08-19 — The puzzle deferral is lifted, and what that actually costs

The brief's standing "do not implement Proxima puzzles yet" was **lifted by the user on 2026-08-19**.
It came up because the monument ritual is blocked behind `TileEntityDimensionCore`, and about half that
class is puzzle-structure machinery — `setStructure(StructurePair)`, `getStructure`, `hasStructure`,
`doScanForEntry`, `getStructureEntryBox`, `doStructureCalculation`, `openStructure`, and the
`structureControlFX` client branch. Porting it fully under the no-stubs rule required the puzzle
generator, so the two rules collided and the user resolved it in favour of the puzzles.

**The number worth knowing before anyone starts.** The full cluster under `World/Dimension/Structure/`
is **133 files and roughly 82,000 lines** — Altar, AntFarm, Bridge, Game of Life, Locks, Music,
Shooter, TileGrid, Water, Wormhole and the rest. That is the largest single subsystem in the mod and
is not a session's work.

**But almost none of it is needed to unblock the monument.** `TileEntityDimensionCore` needs the
*base* only: `Base/DimensionStructureGenerator.java`, 631 lines, which also contains `StructurePair`
as an inner class — so the minimal unblock is one file. `getStructure()` returning null is a legitimate
state; a core with no structure attached still works, which is exactly the case every monument core is
in. The individual puzzle types can then be ported one at a time, when something actually needs each.

Revised order for the monument, therefore:

1. `Base/DimensionStructureGenerator.java` (631) — the base and `StructurePair`.
2. `TileEntityDimensionCore` (712) — now portable in full rather than in halves.
3. `TileEntityStructControl` (1186) — the controller the template already places at (21, 5, 21).
4. `TileEntityAuraPoint` (570) — what the monument becomes on completion.
5. `MonumentCompletionRitual` (1002), split into a common timeline and a client-only effects half.

A cross-check found while scoping, worth keeping: the core ring and the rune ring are **different
rings**. Core BLACK is at (5, 11, 18), rune BLACK at (3, 11, 18) — same height, same angular start,
same direction, cores inset from the runes. The element ordering agrees between them (V33a's `addColor`
runs BLACK to WHITE and the port's `CrystalElement` declares BLACK first), which independently confirms
the rune ring already in the monument template is right in identity and not only in count.

### 2026-08-19 — The monument's blockers: structure base, locus point, Dimension Core

Three layers, ported bottom-up, because each was the reason the next could not be built.

**`StructureGeneratorBase`** — V33a's `DimensionStructureGenerator`, and `StructurePair` with it as an
inner class, which is why one file unblocked the chain. Its worldgen-writer half deliberately does not
come across: upstream owns a `ChunkSplicedGenerationCache` that subclasses `setBlock` into and
`generateChunk` flushes per chunk, whereas 26.2 says the same thing as a `Structure` and its pieces
with the engine doing the bookkeeping. So a generator here *plans* — it fills a map of the cells it
wants — and a piece paints them, which is the shape `MonumentPiece` already takes. `isComplete()` being
"did you place a core" is upstream's real contract: Reika test-ran each generator at class-init and
marked the type usable only if it produced one, which is how she filtered out her own unfinished ones.

**`ThreadSafeTileCache`** (DragonAPI) and **`TileEntityLocusPoint`** — the cache is why the locus point
exists. A locus point is looked up by *who placed it*, not by where it is, and usually from a thread
with no world in hand. So each instance registers into a per-class, per-owner cache on first tick and
removes itself when broken, and the cache holds `WorldLocation`s rather than tiles because a location
outlives its tile, survives chunk unload and resolves across dimensions.

Two things there are worth keeping. The cache's three traversals share one private walk, and *skipping*
is decided inside it rather than signalled back: a null tile is legitimately skipped on the client and
legitimately a match when `tileClass` is null, so no sentinel of type `BlockEntity` can separate those
from a genuine null match. For the same reason `lookForMatch` is not `returnMatch(...) != null` — that
would call a null-tile match no match at all. And upstream dereferences the tile before null-checking
it when a tile class is set, which throws on a location whose block has gone; guarding that is a fix
rather than a change, since the entry was always meant to be dropped there.

**`TileEntityDimensionCore`** — the sixteen elemental cores, plus their block, block entity, tile enum
entry, models and display name. A core is two things wearing one block: out in the ring it is the prize
at the end of a puzzle structure, sealed until that puzzle is solved; around the monument it is one of
sixteen a player plants, and the ritual will not start until all sixteen are present, the right colour,
and placed by the same person.

The seal is real, not deferred: `getDestroyProgress` returns zero for a core whose structure is
unsolved, which is how vanilla expresses "cannot be mined" without a special case at every break site —
and unlike a break event it also covers explosions and other mods' miners.

Recorded because it is the easiest thing here to get wrong, and the new
`chromaticraft:dimension_core_ring` test pins it: **the core ring is not the rune ring.** Cores sit
inset — BLACK at (5, 11, 18) against the runes' (3, 11, 18) — while sharing a height, a start and a
direction with them.

Two calls on the mining path are forward references, each named at the line it belongs on:
`ChromaDimensionManager`'s per-player structure registry and
`ProgressionManager.markPlayerCompletedStructureColor`. Neither is ported. Everything around them is,
including the unsealing itself, so a solved structure still opens while the progression record is
missing.

**Remaining for the ritual:** `TileEntityStructControl` (1186 lines — the controller the monument
template already places at (21, 5, 21); `endRitual` calls its `endMonumentRitual`), `TileEntityAuraPoint`
(570 — what the monument becomes on completion), then `MonumentCompletionRitual` itself (1002), which
must be split into a common timeline and a client-only effects half.

### 2026-08-19 — The monument ritual, server half

The ceremony now exists. Four pieces landed after the Dimension Core, in dependency order.

**`TileEntityAuraPoint`** — what the monument becomes on completion, and thereafter a standing area
effect keyed to its owner. The age formulas are the character of the thing and are carried exactly:
attack radius `min(8 + age/16, 96)`, heal radius `min(4 + age/64, 32)`, looting `min(6, age/288000)`,
one level per four hours capped at six after a day. Both sweeps use a full-height column rather than a
sphere, which is upstream's shape and means a point defends the sky as readily as the ground. Deferred:
upstream's strike routes through `ChromaAux.doPylonAttack`, which is not ported — the damage it computes
is carried exactly and applied the way the ported pylon applies its own, but that helper's taper and
progress flags, and routing the looting level into the drop roll, wait for it.

**`MonumentRitualScore`** — the melody and the schedule derived from it, separated out because it is
pure data and pure arithmetic with no world, no side and no player, and because getting it wrong is
silent. Twenty-two notes; each produces a ray a quarter-second before its own beat, an effect chosen by
its length, and a burst of six particle rings if it opens a phrase; five vortex growths sit on fixed
beats. The nudges are what make it read as musical rather than mechanical and are upstream's exactly: a
running offset that shifts phrase openings by -250 then +150, a one-shot after the long note at index
10, hard resets at 16 and 18, and three cumulative drifts as the elapsed time crosses 85, 101 and 115
seconds. Inside Proxima every event is pulled earlier — half a second for a phrase opening, nine tenths
otherwise — to stay aligned with that dimension's longer audio.

**`MonumentCompletionRitual`** — the server-side timeline, and *only* that. Upstream is one class
holding the timeline and its effects together, which in 26.2 would put `Minecraft`/`ISound`/`EntityFX`
descriptors on a class the dedicated server loads and verifies during mod construction. That is exactly
what made `ClientPayloadHandlers` necessary, so the effects belong in a client-only counterpart joined
by packets, and this half carries no client type at all.

Two things worth keeping. The clock is `System.currentTimeMillis`, deliberately and not world time,
because the score is cued to recorded audio that plays at real speed whatever the tick rate — but any
step longer than a tick has its excess banked into a pause total and subtracted back out, so a server
hitch *delays* the ceremony rather than desynchronising it from its own music. And `doChecks` is what
makes this a ritual rather than a button: all sixteen cores present, each the colour its position calls
for, and every core that records a placer recording the same one, so a monument cannot be finished by a
group each contributing a core.

`doMineralChecks` answers true pending `MonumentMineralBlocks`. That is upstream's own answer when there
is nothing to compare against — its client-side branch returns true unconditionally — so an unbuilt
mineral check permits the ritual rather than blocking it, and the sixteen-core gate still holds.

**The controller's monument half** is wired: `setMonument`, `isMonument`, `triggerMonument`,
`endMonumentRitual`, persistence, and a tick that runs *before* the fragment-structure early return —
a monument controller has no structure type, so that return would otherwise never let its ritual tick.
The ritual object itself is deliberately not persisted: one interrupted by a save or a restart is over,
and upstream rebuilds from scratch on the next trigger.

**Still to do:** the client-only effects half (rays, vortex, ring particles, camera, the two shader
programs and the six audio tracks), the packets joining it to this, and `MonumentMineralBlocks`. The
`resetSettings`/GUI-and-view-bob restore belongs with the camera work and must be unconditional — on
logout, death and server stop, not only on completion.

### 2026-08-19 — The mineral inlay is the ritual's second gate

`MonumentMineralBlocks` is not decoration, and reading it changed what `doMineralChecks` had to be.
The whole mechanic is an asymmetry in one helper:

```java
private void setBlock(..., Block b) {
    if (ReikaRandomHelper.doWithChance(blockChance.get(b)))
        world.setBlock(x, y, z, b);
    parent.registerMineralBlock(x, y, z, b);   // unconditional
}
```

The block is laid only on a per-material chance — quartz 80, diamond 75, emerald 67, lapis 50, chroma
50, redstone 40, glowstone 35, **gold 25** — but registered every time. So the expected map holds all
377 cells while a generated monument receives a random subset of them, and upstream's
`doMineralChecks` compares the world against the *full* map. **The player has to complete the inlay by
hand before the ritual will run.** That is what makes it a *completion* ritual, and it is why gold, at
one in four, is the material they end up carrying the most of. The centre chroma at (21, 3, 21) is
registered and never laid at all, so it is always theirs to supply.

The port had this as `return true` with a note calling it deferred. That was wrong — it was not
deferring a check, it was removing a gate. Now: the table is data (376 chance-laid cells plus the
register-only centre, all 376 `setBlock` calls parsed with none unmatched), `MonumentPiece` rolls the
subset per cell seeded off the monument position so every chunk painting part of it rolls the same one,
and `doMineralChecks` walks the full table. Client-side it still answers true unconditionally, as
upstream does: the check is the server's, and a disagreeing client would only desynchronise the
ceremony.

Two tests came out of it. `chromaticraft:monument_mineral_inlay` pins the table against V33a's own
counts — 88 glowstone, 76 redstone, 72 gold, 52 emerald, 32 diamond, 24 quartz, 24 lapis, 9 chroma —
and that the roll never lays the centre. `chromaticraft:monument_ritual_checks` now walks the whole
sequence: no cores refuses, sixteen correct cores *still* refuses, the complete inlay satisfies, one
missing cell refuses again, and one miscoloured core refuses.

That second test also caught a mistake of mine worth recording: it first used
`helper.makeMockPlayer`, whose player is not in the level, so `BlockEntityBase.getPlacer` could not
resolve the stored UUID and fell back to a `FakePlayer` — which `doChecks` then correctly refused,
since a core placed by a fake player grants no ownership. The ritual was right and the test was wrong.
`makeMockServerPlayerInLevel` is what the rest of the suite already uses.

### 2026-08-19 — The ritual's client half, and the audio it was written for

**The six tracks existed and only one was registered.** All seven monument `.ogg` files have shipped
since the assets came across, but `sounds.json` carried only `monument/s` — V33a reaches the rest
through its pitch-variant sub-sound system, which this port defers. So the whole score, whose every
timing exists to cue those tracks, had nothing to cue. The six are now their own sound events sharing
`MONUMENT`'s flags: streamed (they are two-minute pieces of music), preloaded (so the ceremony does not
stall on first play) and unattenuated (it is meant to be heard from wherever the player stands).
`monumentTrack(step)` keeps V33a's indexing, whose `currentSound` starts at zero and asks for variant
`idx + 1`, so step 0 is `monument/s_1`.

**`MonumentRitualEffects`** is the client half. The two halves never share an object: the server runs
its own copy of the score and owns when the ceremony ends, while this runs a second copy purely to know
what to draw and when. That means the visuals cannot hold the ceremony up and a stalled client cannot
desynchronise it — the price being that both sides read the same clock, which is precisely why the
score is wall-clock. The pause correction is on both sides for the same reason: a client dropping
frames should have the ceremony wait for it rather than skip through its own music.

The effects went into `ChromaParticle` beside the rest, built on the `Blur` particle that is this
port's `EntityCCBlurFX`: the vortex (twelve blurs a tick on a ring that turns with the wall clock, each
drawn inward and lifted, so it reads as a funnel rather than a halo — every third particle lives half
as long again, which is what gives its edge the flicker), the ring (180 points at the monument's
shoulder, three lit at a time and travelling, with the radius pulsing by *position* rather than time so
it reads as a woven figure), and the rays.

**One packet, not V33a's four.** Upstream sends start, end, complete and reset because its single class
held both halves and had to be told about every transition. Here the client runs the same score, so all
the server says is where the monument is and whether the ceremony is on.

Checked rather than assumed, since it is the reason for the whole split:
`MonumentCompletionRitual`, `MonumentRitualScore`, `MonumentMineralBlocks` and
`TileEntityStructureController` contain **zero** references to `net.minecraft.client`,
`Minecraft.getInstance`, `ClientLevel` or `ParticleEngine`.

**Still not carried: the camera.** V33a walks it around an epitrochoid (`R = 40, r = 32.5, d = 12.19`)
through `ReikaRenderHelper.setCameraPosition`, hiding the GUI and disabling view bob for the duration.
That needs a 26.2 camera-override hook, and with it must come an *unconditional* restore of both
settings — on logout, on death and on server stop, not only on completion, or a player who leaves
mid-ritual is left with no GUI. Nothing in the current effects touches those settings, so nothing in
them can strand a player that way. The two shader programs are also absent; per this port's shader
notes their sixteen per-frame core positions and colours must arrive as a texture, not as uniforms.

**The camera, ported.** The research below stood up and the route it named is what was built.

`ViewportEvent.ComputeCameraAngles` exposes only yaw, pitch and roll; `Camera.setPosition` stays
protected and NeoForge's camera patch adds `getRoll`, `getBlockAtCamera` and a three-argument
`setRotation` but no position hook. So the orbit needs both halves: the angles go through the event,
which is what the rest of the render pipeline reads them from, and the position through a new
`CameraAccessor` mixin in DragonAPI. An accessor rather than an access transformer deliberately — this
mod's AT still carries SRG field names from an older mappings setup, so whether it currently applies is
not something to build a feature on, while the two mixins beside the new one demonstrably load.

The orbit is applied per *frame*, not per tick: a camera moved once a tick judders, and the event
carries the partial tick that smooths it.

Two 26.2 API differences worth recording. `gameSettings.hideGUI` is gone — the flag now lives on
`Hud` as a private field with a `toggle()` and an `isHidden()` and no setter, so the state is reached by
comparing and toggling rather than by assignment. And view bob is an `OptionInstance<Boolean>` behind
`options.bobView()`.

**Everything borrowed is given back in `stop()`, and every way a ritual can end routes through it**: the
server saying so, the ceremony completing, and — the path that actually matters —
`ClientPlayerNetworkEvent.LoggingOut`. A player who logs out mid-ceremony would otherwise come back with
no HUD and no view bob, with nothing in the world left to restore them.

**The camera hook, researched.** `ViewportEvent.ComputeCameraAngles` exposes yaw, pitch and roll and
nothing else; `Camera.setPosition` is `protected` and NeoForge's camera patch does not widen it — it
adds `getRoll`, `getBlockAtCamera` and a three-argument `setRotation`, but no position hook. So the
epitrochoid *orbit*, which moves the camera off the player entirely, is not reachable from a public
API. Turning the player instead is not equivalent: it would move a real entity and take its collision
and reach with it.

The route that exists is a mixin accessor onto `Camera.setPosition`, and DragonAPI already carries the
infrastructure for it (`mixins.dragonapi.json` with `reika/dragonapi/mixin/`). That is the shape the
camera work should take, together with the unconditional GUI and view-bob restore.

### 2026-08-19 — The monument's shaders, as one post pass

V33a ships two: `general.frag`, a post effect that pushes saturation and lifts brightness by a scalar
`intensity`; and `chords.frag`, which is **not** a post effect — it is a terrain shader, run over chunk
geometry with the sixteen core positions and colours as uniform arrays, adding a glow around each in
world space.

There is no seat for the second in 26.2 short of replacing the chunk pipeline, which is a far larger
change than the effect is worth. So both are one pass: the grade is carried unchanged, and the glow is
applied in screen space with the CPU projecting each core exactly as RotaryCraft's heat ripple projects
its emitters. **The falloff is therefore measured on screen scaled by distance rather than in world
XZ** — the one real difference, and not a visible one in practice, because the cores stand in a ring
around a viewer who is always outside it, so their screen separation tracks their world separation.

The per-frame data goes through a live UBO, not declared uniforms: a `PostChain` bakes those when the
chain compiles, which is the same constraint the heat ripple hit and the same answer. Sixteen cores of
`{screen uv, distance², alpha}` and `{rgb}`, padded so the colour array starts at a fixed offset
regardless of how many are lit. Each core's alpha is V33a's own `colorFade` — a colour swells while its
key is sounding and decays at a third of that rate — which is what makes the ring *answer* the music
rather than pulse with it.

A core behind the near plane is dropped rather than clamped: a glow anchored behind the camera would
smear across the frame.

The pass reads the scene into an offscreen target and the chain blits it back, because a pass cannot
read and write the main target at once. Run from `RenderLevelStageEvent.AfterLevel`, so it grades a
finished scene.

Import traps worth recording, since three of these cost a compile each: `RenderTarget`,
`FrameGraphBuilder`, `ResourceHandle`, `RenderTargetDescriptor`, `GraphicsResourceAllocator`,
`Std140Builder` and `GpuFormat` are all `com.mojang.blaze3d.*`, but `MappableRingBuffer` is
`net.minecraft.client.renderer`. `BindGroupLayout` is `com.mojang.blaze3d.pipeline` while
`BindGroupLayouts` is `net.minecraft.client.renderer`. And there is no `gameRenderer.getMainCamera()`
or `RenderSystem.getProjectionMatrix()` — the matrices come off the render event's
`cameraRenderState`.

### 2026-08-19 — The Sanctuary's second forest: WorldGenTreeCluster

Proxima's *ordinary* forests, as against the glowing trees. A cluster is three to ten trees scattered
about a point, each rolling its own wood and its own shape from two separate weighted tables — that
double roll is the character of it, since a stand comes out mixed rather than uniform and the rare
shapes are rare per tree rather than per cluster.

**The wood table is ten entries and seven of them are other mods'.** Silverwood is Thaumcraft's;
sakura, silverbell and maple are Twilight Forest's; and upstream filters the lot with
`gen.type.exists()` at class-init. With none of those mods present the table Reika actually rolls is
oak, birch and her own lighted wood at weights 10, 8 and 3 — which is what is here. Those seven are not
dropped behaviour: the filter is upstream's own, and they return with the mods if those are ever
ported.

**The placement deliberately has no `InSquarePlacement`.** A cluster scatters each tree up to sixteen
blocks from its anchor and a giant's bulge reaches five more; a decoration feature may only write
within a forty-eight block window. Randomising the anchor inside the chunk *as well* would push the far
side of a cluster outside that window, where writes are dropped in silence — the failure that once left
structures half generated. Anchored at the chunk's own corner the whole spread stays inside, and
upstream's sixteen-block scatter is untouched. `chromaticraft:proxima_tree_cluster` asserts the reach
stays within 21 for exactly this reason, so a future change that widens the scatter fails loudly rather
than quietly losing its far side.

All six shapes are upstream's: the oak crown, the hanging curtains, the wide taper, the needle column,
the tall needle wrapped in rings with two of them carrying log spokes, and the giant whose radius grows
and shrinks with height and twice at random. Leaves never replace standing blocks, so overlapping
canopies do not eat each other.

Remaining for the Sanctuary, unchanged: **RIFT** (`WorldGenFissure`), **JETS** (`WorldGenFireJet`),
**ALTAR** (`WorldGenMiniAltar`), **CRACKS** (`WorldGenGlowingCracks`) and **GLOWCAVE**
(`WorldGenGlowCave`).

### 2026-08-19 — Fire Jets, and the art that decides what can be ported

**`BlockDimensionDecoTile` carries two types and only one of them can be built.** Upstream packs FIREJET
and GLOWCRACKS into one block, so under the metadata rule they become two. The Fire Jet is here; the
Glowing Cracks are not, and the reason is art rather than effort.

`registerBlockIcons` asks for `dimgen2/underlay_<n>` and `dimgen2/overlay_<n>` for both types, and
**there is no `dimgen2` directory anywhere in V33a's repository** — the same situation as AQUA and
GEMSTONE among the deco blocks, where Reika ships a `logError("...is missing icons!")` fallback because
she knows it. What does exist is each type's item icon. The Fire Jet's is `dimgen/aurajet`, a real 16x16
that is this block's own art, and that is what it wears. The Glowing Cracks' is
`Textures/glowcracks.png` — 1024x1024, a sheet for the custom renderer that draws the cracks across a
nine-by-nine area, not a block icon. It was extracted, examined and removed again rather than pressed
into service as one: that would be inventing art, not porting it.

**The Fire Jet itself.** It sits at the bottom of Proxima's pools and lights every four hundred ticks
or so, burning for one to four minutes — the countdown is `100 + rand(1200)`, a deliberately wide
spread so a field of jets never falls into step, and it is persisted so a jet burning at save time is
still burning at load. Two of upstream's rules are easy to lose and both are kept: it is mineable
**only past `ProgressStage.CTM`** (upstream's `-1` hardness, which 26.2 expresses as a destroy progress
of zero, so it also covers explosions and other mods' miners), and it is **solid**, unlike the other
deco tiles, so a player can stand on one mid-pool.

The flame's colour is the detail worth keeping: it hue-cycles with the jet's own position and the clock
so no two jets in a pool pulse together — *unless* a crystal rune sits directly underneath, in which
case the jet burns that element's colour. That is how the pools come to read as coloured rather than
orange.

**The generator** places nothing unless the column's top solid block is water with at least two blocks
of it above the bed, then rings the jet with four Cloak Shielding so the pool cannot be drained around
it. `MOTION_BLOCKING` is the heightmap that counts water — `WORLD_SURFACE` would land on the pool's
surface and `OCEAN_FLOOR` would skip past the water to the bed. V33a's `case JETS: return true` puts it
in every biome, the Sanctuary included; its site check is what actually decides where one appears.

## The last four Sanctuary generators (2026-08-20)

All four are in. What each needed, and what is honestly short:

**ALTAR** (`WorldGenMiniAltar`) — a fixed seven-by-seven shrine over a five-by-five burrow with a Loot
Chest. Every block it wanted already existed. Its chest is the one incomplete part: upstream fills it at
generation time from `ItemMagicRegistry`, four to twenty-seven items weighted inversely to their
elemental value, and that registry is 548 lines of Reika's own item-to-element table which is not
ported. The biome item — a Multi Crystal in the Glowing Forest, an Iridescent Crystal Shard on the
Crystal Plains — is reproduced and is placed by the feature, which is upstream's own mechanism; this
chest never went through `ChestGenHooks`, and a loot table could not carry the biome choice anyway,
since Proxima's biomes are created by the same datagen run that would have to name them.

**RIFT** (`WorldGenFissure`) — needed the Void Rift, sixteen registered blocks gated on both CTM and
`DECOHARVEST` tuning, and the pattern table, which is built lazily from the world seed rather than on a
background thread. The rift renders as a plain cube and that is faithful: `VoidRiftRenderer`'s aura pass
is commented out in V33a. `RenderVoidRift`'s coloured aura walls are **not** ported and cannot be —
their texture is fetched at runtime from Reika's own server rather than shipped, and is in neither the
repository nor this port's resources. `TileEntityVoidRift` carries the neighbour lookup that pass wants.

The pattern stores a footprint rather than a block map, because `FissurePattern.generate` has no callers:
`WorldGenFissure` reads only `getDepthMap()` and then runs its own cut against the real world.

**GLOWCAVE** (`WorldGenGlowCave`) — the largest. Three new blocks (cracked bedrock, the Void Cave, the
Ethereal Light) and a structure rather than a feature, unavoidably: the walk moves up to thirty-two
blocks a segment with no bound on segment count. Split three ways like the monument — `GlowCaveShape`
grows the cells, `GlowCavePiece` decides what each becomes, `GlowCaveStructure` says where a mouth is.

Cracked bedrock is the one that matters beyond this generator: it is the only source of Proximal
Essence, and so the only way to tune a Portal Rift.

**CRACKS** (`WorldGenGlowingCracks`) — **the standing note that this was blocked on missing `dimgen2`
art was wrong**, and it was checked rather than inherited. `getImageFileName` returns null, the block is
`BlockBounds.block().cut(UP, 0.999)`, and the whole appearance is `RenderGlowingCracks` painting
`Textures/glowcracks.png` — which is in Reika's repository and is now in this port's resources. The
`dimgen2` icons are absent for *every* `DimDecoTileTypes`, the Fire Jet included, and that shipped on
exactly this reasoning. Two named gaps: `DoublePolygon` is not in DragonAPI so the layer fill is an
even-odd ray cast written out (a four-point polygon; the same test), and the ore table's modded half is
`ModOreList` mod integration, so what is here is Reika's vanilla weighting against the same even split
with the tiered ores.

**A fissure's geometry is anchored to the world floor, not to its own origin** — it cuts from y 8-23 up
to twelve above the surface and floors itself at y 2-17. That is faithful: Proxima has a min y of 0
exactly as V33a's world did, so upstream's absolute numbers mean the same thing here. The first version
of its gametest built around the arena, which sits at y -59, and was therefore sixty blocks under
everything it checked.

## The monument ritual, made reachable (2026-08-19)

Every piece of the ceremony had been ported — the template, the placement, the sixteen cores, the aura
point, both gates, the timeline, the camera, the shaders, the score — and none of it could be reached
from inside the game. Three separate links were missing, each of which alone made the ritual
impossible.

**The cores could not be obtained or told apart.** V33a's Dimension Core is one registered block
wearing its colour in a stack tag, so the creative menu offered exactly one, blank; and placing it
neither read the tag nor recorded who placed it, both of which `doChecks` requires. All sixteen are now
offered explicitly — the tab's set keys on type *and* components, so they sit alongside the plain
identity the block loop already accepts — each named for its element, with pick-block returning a
core's own colour so a ring can be built by copying rather than by hunting through sixteen entries.

**The cores could not be seen.** `getRenderShape` returned `INVISIBLE`, faithfully: upstream draws a
core entirely through `RenderDimensionCore`'s glow-knot geometry and its `getImageFileName` returns
null, so there is no block icon in V33a to port. Neither `RenderLocusPoint` nor DragonAPI's `GlowKnot`
exists yet, which left the ring as sixteen invisible blocks. They are cubes of `blurflare` — one of
Reika's own glow sprites, and the nearest thing she ships to what that renderer draws — tinted per
element. The tint is a registered `ItemTintSource`/`BlockTintSource` pair rather than a datagen
constant, because the colour lives on the stack. This is a documented stand-in and should be replaced
by the real renderer when the knot geometry lands.

**The ritual could not be started at all.** `setMonument()` had no caller anywhere in the port:
`MonumentPiece` places the controller but never marked it, and an unmarked controller refuses forever.
It is marked now, in the one `postProcess` pass whose clipped box actually contains it — the template
is placed once per overlapping chunk, and the other fifteen passes would find no block entity there.
And the manipulator had no `STRUCTCONTROL` branch, which is the only way upstream ever starts the
ritual, so the whole chain behind it was unreachable. It is ported with its gates intact: CTM's
*prerequisites* (not the stage itself, which the ritual is what grants), the monument mark, the Proxima
tuning threshold, and the single refusal sound that deliberately tells the player nothing about which
requirement they failed. Upstream's creative debug branch came with it, since sneak-clicking a
controller is the only way to mark a monument that generated before this landed.

**`/chromaprog`**, upstream's admin progression command, is ported for the branches whose backing
exists: `progress`, `color`, `dimtuning`, `maximize`, `reset`. Two of those are load-bearing here —
inside Proxima the ritual wants a dimension tuning of at least 224, a value otherwise set only by the
Portal Rift from absorbed Proximal Essence, so anyone who reached the dimension by command has zero and
no way to raise it. The remaining branches are each blocked on an unported system and are listed at the
class: `fragment`/`level` on `ChromaResearchManager`, `ability` on `Chromabilities`, `buffer` on
`ElementBufferCapacityBoost`, `lore` on the tower fragment store, `dimstruct` on
`markPlayerCompletedStructureColor`.

**The second gate is meant to bite.** `doMineralChecks` demands every one of the 376 inlay cells, while
generation lays each only on its material's chance — gold at 25%, glowstone at 35% — and registers the
centre chroma without ever laying it. This was checked against V33a rather than assumed: upstream's
`setBlock` is `if (doWithChance(blockChance.get(b))) world.setBlock(...); parent.registerMineralBlock(...)`
unconditionally. A freshly generated monument therefore always fails, and completing the inlay by hand
is the work the ritual is named for.

The ring sits six blocks above the controller, at template y 11. Relative to the controller:
BLACK (-16,+6,-3), RED (-13,+6,-7), GREEN (-7,+6,-13), BROWN (-3,+6,-16), BLUE (+3,+6,-16),
PURPLE (+7,+6,-13), CYAN (+13,+6,-7), LIGHTGRAY (+16,+6,-3), GRAY (+16,+6,+3), PINK (+13,+6,+7),
LIME (+7,+6,+13), YELLOW (+3,+6,+16), LIGHTBLUE (-3,+6,+16), MAGENTA (-7,+6,+13), ORANGE (-13,+6,+7),
WHITE (-16,+6,+3).

## 2026-08-20 — post-Claude audit: Proxima and monument foundations

This audit covers the **55 commits after `4461b387d` through `85b81322e`**: 306 files,
14,700 insertions and 3,647 deletions. The result is not throwaway scaffolding. Claude landed a
substantial Proxima slice: its generated layout, region and biome maps, chunk generator and dimension
registration, sky and sky rivers, NBT monument, structure-controlled monument ritual, much of the
ritual's client presentation, and the Sanctuary decoration family. The right response was to preserve
that work and repair the lifecycle, persistence and worldgen seams around it.

### Corrections made by this audit

- **The global layout now exists before levels and chunks ask for it.** Generation starts at
  `ServerAboutToStartEvent` from the server's authoritative worldgen seed, not at
  `ServerStartedEvent` after level construction. Same-seed callers share one future; a second world
  seed is serialized behind the first because the active V33a maps are process-global static state.
  Completed same-seed layouts are reused, failures are logged, and a stale layout from another save is
  never returned to a chunk asking for the current seed.
- **The missing V33a Proxima survival contract is live.** Fall damage is cancelled for entity age
  below 1200 ticks and thereafter follows the exact `min(8, min(d, max(1, .5*sqrt(d))))` curve.
  Death is cancelled at one health, returns the player to the Overworld through `ChromaTeleporter`,
  and removes 40–90% of every held element. Out-of-world damage is cancelled throughout Proxima and
  returns the player only below Y=-1024, so 26.2's much earlier vanilla void damage cannot kill them
  before the source threshold. Login and respawn also carry `DimensionJoinHandler`'s 5 x 3.5 x 5
  safety ellipsoid: air inside, ordinary Cloak Shielding on the shell, while fluids, vegetation,
  unbreakable blocks and block entities are preserved.
- **Portal placement now records its owner.** `BlockChromaPortal.setPlacedBy` gives the portal block
  entity the real placer, preserving the ownership contract used by its return path and progression.
- **Locus ownership survives item form.** Placer name and UUID now round-trip through the locus
  point's item tag. A malformed UUID does not erase an already-valid placement owner.
- **The monument abort path is complete.** Losing a required mineral inlay cell during the ritual now
  runs `endRitual()` before reporting failure, so server and client state, audio and camera restoration
  do not remain latched.
- **Aura Point and Dimension Core ambient particles are restored.** Both emit V33a's paired tinted and
  white centre-blur locus particles rather than remaining visually inert outside their specialist
  effects.
- **Two unsafe generation paths no longer reach through a feature's world window.** Crystal-tree
  crowns paint their plan through the provided `WorldGenLevel`; Aurorae use the chunk generator's
  noise-backed `getBaseHeight` for distant ribbon points instead of asking the feature view to read
  hundreds of blocks into neighbouring terrain.
- **Glow caves regained their missing atomic site preflight.** Before a piece exists, the exact seeded
  shape is built and checked against generator noise columns: the mouth must top out on grass or sand,
  and every cave cell plus its six neighbours must be free of water. This avoids partial caves and
  avoids loading neighbouring chunks during structure-start selection. Crystal Forest is included in
  the biome set because V33a's forest-biome test includes that sub-biome. Modern structure-set spacing
  remains the conflict-prevention mechanism; the old partially-generated-neighbour shielding scan is
  not safe from a modern structure-start context.
- **Puzzle calculation no longer gives up after one exception.** `StructureCalculator` now restores
  V33a's attempt zero plus ten retries, clearing the generator between attempts, preserving
  `OutOfMemoryError`, and discarding both placement and cached generator only after the final failure.
  The stale source comments saying puzzles were deliberately deferred were corrected: the deferral is
  lifted, and generators now enter one complete vertical slice at a time.
- **A dormant Dimension Core bookkeeping fault was defused.** The incomplete entry scan had marked a
  player as “sent” even though no structure-session registration occurred. That would permanently
  skip the player after the real manager landed. It now leaves the once-only set untouched until the
  forthcoming successful registration can own that transition.
- **A committed 26.2 compile regression in the Heat Lamp GUI was fixed.** `EditBox.setFilter` no
  longer exists. The responder now validates typed and pasted content through the actual 26.2 mutation
  path, restores the last valid string, and retains the authoritative menu temperature synchronization.

### Status of the previous eight-step Start → Proxima plan

1. **Structure identities/sizes and placement calculator — implemented.** The identity and placement
   foundation exists; calculation retries are now source-faithful. No puzzle generator is registered
   yet, so normal layouts intentionally assign none.
2. **RegionMapper — implemented.** It consumes the completed structure-layout stage.
3. **BiomeDistributor — implemented.** Structure and Monument fields derive from authoritative layout
   positions rather than guessed fallbacks.
4. **Nine primary plus four sub-biomes and custom biome source — implemented.** Generated data and the
   biome source are present.
5. **Chunk generator and terrain — implemented, active audit required.** The generator is operational;
   the unsafe cross-window reads found in decorations were repaired. Long exploration in a fresh save
   is still the meaningful stress test.
6. **Dimension type/stem — implemented.** Proxima is registered and reachable through its modern
   portal transition.
7. **Join/exit behavior — now implemented for the source paths that exist independently of puzzles.**
   The Y=1024 transition, fall curve, login/respawn carve-out, death return with element loss, and
   below-Y=-1024 void return are live. Puzzle-session entry/exit is the next puzzle-foundation slice,
   not dimension travel.
8. **Decoration/entities/bedrock cracks/sky/weather — broad implementation, not a blanket “done.”**
   Sky rivers, tree clusters, fire jets, fissures, mini-altars, glow caves, glowing cracks, bedrock
   cracks and several visual/environment systems exist. Remaining fidelity work includes incomplete
   mini-altar magic loot, the unavailable externally-hosted Void Rift aura texture, and normal
   in-world visual/performance verification across all Proxima biomes.

### Puzzle reality and the next complete vertical slice

The puzzle deferral is lifted, but **no `DimensionStructureType` currently calls
`registerGenerator`**. `usableStructures()` is therefore empty and `StructureCalculator` creates no
puzzle placements in an ordinary world. The 133-file/~82k-line source cluster has not secretly become
active merely because its base and several shared blocks compile.

Before registering the first puzzle, these foundations must land together:

1. A generic modern Structure/piece painter for a generator's planned cells, with deterministic or
   SavedData-persisted generator identity. The current random UUID cache is process-local; a saved
   Dimension Core must not lose its generator association after a server restart.
2. The modern per-player structure-session manager: tuning gate, authoritative entry packet/overlay,
   per-player tick, leaving/removal paths, logout cleanup, and reload behavior. Only a successful
   registration may add `TileEntityDimensionCore.sentPlayers`.
3. Structure-colour completion persistence in `ProgressionManager`, followed by the Dimension Core
   mining sequence: record completion, remove the session, then open the structure.
4. One source-complete puzzle generator plus every block, block entity, renderer, packet and puzzle
   rule it needs. Register only after its `isComplete()` test-run genuinely creates a core.
5. Focused tests for deterministic layout and reload, painter clipping, entry/tuning, solve state,
   sealed/unsealed core mining, completion persistence and server restart. Only then should a second
   puzzle type begin.

That is the next honest milestone: **one fully playable and reload-safe puzzle**, not eighteen visible
shells with missing mechanics.

### Verification at this checkpoint

- `:ChromatiCraft:compileJava -x :RotaryCraft:compileJava` — **passes** on Java 25 / NeoForge 26.2
  (deprecation warnings only). The exclusion isolates unrelated in-progress RotaryCraft renderer
  edits already present in the shared worktree.
- `:ChromatiCraft:runServerData -x :RotaryCraft:compileJava` — **passes**, and the intended generated
  change is the Glow Cave structure biome list. Unrelated compressed-NBT/datagen timestamp churn was
  restored rather than included.
- A focused `-PgameTestSelector=chromaticraft:dimension_core_ring` launch was attempted, but **the
  GameTest server never reached test discovery**: the NeoForge dev-run harness rejected
  `ChromatiCraft/build/classes/java/main` as “not a valid mod file”. This is a run-configuration/harness
  blocker, not a failed assertion. Do not rerun the full suite until that startup path is repaired.
- Focused coverage was extended for portal ownership, locus owner item round-trip, and Proxima's fall
  and void thresholds. They compile, but remain queued behind the same harness startup issue.

### GameTest runtime repair (2026-08-20)

The previously reported harness/startup blocker is resolved under the current 26.2.0.64 toolchain.
More importantly, `:ChromatiCraft:runGameTest` now supplies `--tests chromaticraft:*` by default;
the namespace registration property alone had allowed all 156 tests from loaded family mods to run.
Focused `-PgameTestSelector=...` selection remains available.

Shared DragonAPI hardening splits vanilla's 50-test batches into groups of 12, clears completed
structures and forced chunks, and removes synthetic server players between batches without writing
their random UUIDs to playerdata. Headless runs omit Spark/Jade/JEI, cap heap at 4 GiB, and show an
out-of-thread ten-second heartbeat with progress, active test IDs, effective fast-forward tick rate,
wall MSPT, heap and true server-thread stall time. A 30-second stall prints the server stack and a
180-second stall fails/terminates the test JVM rather than retaining the machine's RAM indefinitely.
The watchdog disarms before final world saving, and GameTests use an isolated `run-gametest`
directory rather than contending with an open development client's logs or save data.
See root `GAMETESTS.md` for exact commands and tuning properties.

Verification was deliberately focused: `chromaticraft:pylon_player_placed_restriction` passed and
its synthetic player was released before shutdown; no full ChromatiCraft suite was rerun in this
slice. The separate ElectriCraft `wire_and_machine_shapes` test also passes after its far-coordinate
double-precision AABB repair.

## Proxima traversal, portal and presentation pass (2026-08-20)

- Aurora splines now persist per entity and advance once per entity tick. The temporary render-state
  implementation rebuilt and advanced the path every rendered frame, making motion depend on FPS and
  producing the reported high-speed spasms.
- Liquid Ender exposes an entity-inside interaction shape while retaining its non-solid physical
  shape, so V33a's launch/bounce callback runs instead of leaving entities trapped in an inert fluid.
- The Portal Rift and Void Rift now have modern 26.2 block-entity renderers. The portal restores its
  animated pad skin, focus, charging rings and tall crossed beams; Void Rift restores exposed-side
  animated aura walls and suppresses internal walls between matching rifts.
- `/place structure chromaticraft:portal` is a real command-only Structure. It chunk-clips the
  canonical NBT template and restores all eight V33a End Crystals on bedrock supports. The three
  casting temples likewise moved from configured features to
  `/place structure chromaticraft:casting_temple_l1`, `_l2`, and `_l3`; their stale configured and
  placed-feature data was removed. Natural decorators remain features.
- Portal denial now reports the exact unsatisfied gate in the action bar (End Crystals, multiblock,
  Proxima registration, layout readiness, charging, or progression). `chromaprog maximize` now also
  grants maximum dimension tuning, which had remained an independent hidden Sky River/portal gate.
- The End Crystal mover is active and component-backed. It captures vanilla End Crystals, removes
  their bedrock support, replaces captured crystals with the original 3x3x3 clearance rule, exposes
  both empty and filled creative variants, and has the V33a bedrock/animated-crystal special item
  renderer.
- The Lexicon portal preview now resolves the canonical portal template, expands for the eight outer
  crystal locations, and displays bedrock supports plus End Crystal tally icons.
- Sky River rendering now includes the omitted first segment and overlaps adjacent swept tubes at
  bends. Rider management replaces radius-two synchronous chunk probes with a narrow forward ticket
  corridor; if the next corridor chunk is not ready, motion is held without forcing generation on
  the server thread. This targets both disconnected-looking ends and the server stalls that threw
  riders out of rivers.
- Cliff Glass no longer selects its intentionally transparent V33a underlay as its only cube texture.
  Ethereal Luma uses the sixteen-cell purple V33a still mosaic (and the rotated Proxima mosaic)
  through a custom fluid UV mapper instead of the unused blue water-like strip.
- Fissure/Void Rift carving now preflights the entire decoration against the active worldgen write
  window before changing any block. It will reject a placement rather than leave the chunk-aligned
  stone slab visible in a partially carved pit. Existing generated pits are not retroactively edited.
- The Heat Lamp edit box now debounces network writes and flushes on close. Intermediate strings such
  as the `2` in `200` are no longer server-clamped to 20 and echoed back over the text being typed.
- The original six creative inventories are registered as separate modern tabs: main blocks,
  decoration, worldgen, tools, items, and decoded information fragments.

Verification for this slice: `:ChromatiCraft:compileJava`, `:ChromatiCraft:runClientData`, and
`:ChromatiCraft:runServerData` all pass under Java 25 / NeoForge 26.2. No broad GameTest suite was run;
the visual, rider-motion, fluid and command-placement changes require the focused in-world checks
listed in the handoff.

## Portal/casting presentation corrections (2026-08-21)

- The Portal Rift renderer now selects one frame from each directly-bound vertical animation strip
  (`rift`, `ringrow_fade`, and `rift_halo`). Sampling the whole PNG as UV 0..1 produced the reported
  stack of horizontal lines on both the pad and tall beam.
- The retained `64x.png` and `16x.png` particle resources are 16-by-16 sprite atlases, not 16-by-2.
  The modern ping-pong particle restores V33a's exact row pairs: Laser/Globe use rows 0/1 and
  Center Blur uses rows 4/5. Portal-focus, casting-focus, and item-stand particles no longer sample
  sixteen rows at once or show unrelated/empty atlas cells.
- Outer portal rune particles now resolve `block/runes/real/tileN_0` directly. The generic particle
  helper had prepended `block/icons/`, a directory which does not contain the rune sheets.
- Ethereal Luma flowing faces use `aether_flow2`, matching V33a's actual `luma.setIcons` call. The
  blue `aether_flow` strip was not the registered flowing sprite in the original release.
- The first 26.2 pass emitted V33a's registered `portal2` event every 90 ticks. That reproduced the
  source call site but also reproduced an unacceptable approach delay: entering the radius just after
  a broadcast could remain silent for 4.5 seconds. The later acceptance fix replaces it with the same
  client-owned, position-bound tickable lifecycle used by pylons, with continuous distance fade and
  immediate silent-start support. See the 2026-08-22 correction below.
- The Proxima cheating-prevention singleton is initialized after its immutable legacy ban table.
  The inverse declaration order passed compilation but dereferenced a null map in the constructor
  on the first player tick in Proxima.
- Command-placed casting temples now find the highest free surface across their complete footprint,
  clear the authored air volume above the foundation with chunk clipping, and create their table at
  the represented XP tier: L1 Temple, L2 Multiblock, L3 Pylon. Player-placed tables remain unchanged
  and must earn those tiers normally.

Verification was deliberately narrow: `:ChromatiCraft:compileJava` passes on Java 25 / NeoForge
26.2 (deprecation warnings only). No full GameTest suite was rerun for this render/audio/in-world
placement slice.

## Network, portal, fluids and Proxima parity pass (2026-08-21)

V33a's portal block entity emitted its registered positional sound every 90 ticks. Although that is
the literal source call site, the resulting 26.2 one-shot playback had a 0-4.5-second approach delay
and hard arrival boundary. The 2026-08-22 acceptance correction supersedes this paragraph with the
pylon-style tickable loop requested for the modern port; its lifetime remains bound to the formed
portal and its gain is computed continuously from listener distance.

- Repeater and pylon transfer beams now follow `CrystalTransmitterRender` and
  `ChromaFX.drawEnergyTransferBeams`: targets sharing one endpoint are grouped, their element colours
  cycle with V33a's arithmetic, widths retain the cumulative maximum clamp, and the textured tube uses
  the original solid/depth-writing pass. Removing the extra half-block target subtraction also makes
  the tube end at the receiver's actual centre/declared offset.
- Power Crystals have a dedicated translucent/fullbright item renderer. Their block entity refreshes
  its pylon connection on initial load and every twenty server ticks, synchronizing only when the
  connection changes; their colour/active visual no longer depends on an unrelated neighbour update.
- The Portal Rift renderer now uses V33a's actual `end_portal`, `beam2`, `arches2`, rift-strip and
  ring-strip assets. The transport arches are charge-gated, all extended geometry is submitted after
  translucent terrain, and its render bounds/view distance cover the complete effect. Replacing any
  pad cell revalidates the nearby 3x3 centre, including after a chunk reload.
- Additive ChromatiCraft particles now participate in the translucent particle target, with colour
  addition separated from alpha accumulation. This keeps multichromic-rune and focus particles in
  front of water when appropriate instead of forcing every particle into the pre-water main target.
- Ethereal Luma, Liquid Chroma and Liquid Ender use modern fluid-type movement implementations.
  Their entity-interaction shapes remain non-solid; Ender applies V33a's normalized 32x current plus
  its launch callback. The 2026-08-22 correction gives Luma its own thin, non-buoyant travel rule and
  disables Minecraft's swimming pose. Ethereal Luma, Liquid Chroma and Liquid Ender all have buckets
  backed by translucent dynamic-fluid-container item models.
- Void Rifts use the exact 128-frame V33a aura strip, source pulse/colour mixing, cardinal/diagonal
  seam rules, translucent ordering, expanded bounds and offscreen rendering. Neighbour changes clear
  both sides' seam caches. The Structure Controller effect likewise uses an additive transparent
  submission after water rather than rendering its transparent pixels as a black rectangle.
- Dimension Cores now restore V33a's three-layer animated aura in-world and in item form, with every
  concrete colour registry item routed through the special model. The monument generation point scans
  its complete 65x65 footprint and raises the base to the highest surface (never below the original
  Y=103 floor), preventing modern terrain from burying it.
- Command-placed L2/L3 casting temples add the original twenty-four Item Casting Stands around the
  table and retain the represented table tier. Proxima tree clusters are centered in the owning chunk
  before applying their +/-16-block scatter, preventing the reported chunk-border half-trees and
  far-chunk feature writes. Aurora render culling no longer uses its tiny anchor entity box.
- Glow Cave locate work now deduplicates terrain/water probes and carries the already-generated cave
  cell set into the structure piece instead of immediately rebuilding it. This removes the largest
  avoidable allocation/query multiplier from `/locate structure chromaticraft:glow_cave`; real-world
  timing still needs measuring because vanilla locate can legitimately search many candidate chunks.

The monument ritual remains deliberately source-strict. It requires all sixteen coloured Dimension
Cores, in their exact ring locations, owned by the same real player, plus the complete mineral inlay.
Five cores cannot start it. The owner must then use the Elemental Manipulator on the controller; the
port must not weaken this gate merely to make an incomplete monument appear active. Proxima sky
rotation based on player position is also intentional V33a behaviour, so fast travel produces faster
apparent sky motion.

Verification was kept focused:

- `:ChromatiCraft:compileJava` — passes (Java 25 / NeoForge 26.2; deprecation warnings only).
- `:ChromatiCraft:runClientData` — passes and generated the new special item/fluid-container models.
- `chromaticraft:portal_structure_and_charge` — passes, including break, replace, revalidate,
  recharge and block-entity persistence.
- `chromaticraft:casting_nbt_structure_contract` — passes for all three casting tiers.
- `chromaticraft:monument_ritual_checks` — passes the exact sixteen-core ownership/colour gate.

No broad GameTest suite was run. Beam composition, translucency ordering, fluid feel, positional
audio, structure elevation and locate latency are render/client/worldgen concerns and remain the
focused in-world acceptance scope for this slice.

## Monument ensemble, render ordering, fluids and portal-audio correction (2026-08-22)

This pass resolves the follow-up defects against V33a commit `63c0c29a`, while retaining the modern
26.2 render and fluid APIs:

- Player-placed Dimension Cores are primed in `BlockDimensionCore.setPlacedBy`, matching V33a's
  `ItemChromaPlacer`. Each colour therefore joins the marked monument controller's eight-tick note
  sequence as it is installed; all sixteen cores collectively play the selected track, indexed modulo
  its length for the original continuous ensemble. Colour registry identity and placer ownership are
  preserved. The focused `monument_core_placement` GameTest now asserts priming as well as those two
  ritual inputs.
- Dimension Cores, Void Rifts, pylons, repeaters, portal effects and Structure Controllers submit
  extended additive geometry through the real `AFTER_TERRAIN` phase. The previous phase named
  `TRANSLUCENT_CUSTOM_GEOMETRY` executes before translucent terrain in 26.2, which is why these effects
  appeared behind water. Dimension Core items use an alpha-weighted additive pass (removing their black
  inventory quad) and the modern item transform flips the legacy X rotation. Void Rift wall strips
  covered by opaque terrain are suppressed, removing the beam/terrain z-fighting without moving the
  visible seam away from its V33a coordinates.
- Item Casting Stand model cuboids and `item_stand.png` were byte/coordinate checked against V33a and
  are unchanged. Its custom hover geometry now computes the visible boundary of the cuboid union:
  edge portions buried inside another stand part are clipped, so interior intersections and the small
  corner tails are omitted while every exposed outer edge remains.
- Ethereal Luma has a dedicated low-viscosity, non-buoyant movement implementation, does not enable
  Minecraft swimming, and supplies an underwater overlay/fog instead of exposing the x-ray-like empty
  camera state. Liquid Ender now has the registered `liquid_ender_bucket`, fluid-container model and
  language entry.
- Power Crystals now exact-scan the pylon's eight authored socket offsets before falling back to the
  network cache, so an active socket no longer waits for an unrelated block notification. Their item
  renderer adds the fullbright animated active layer. The focused `pylon_power_crystal_recharge` test
  passes all eight connections, V33a recharge rate and progression grant.
- Portal audio is a position-bound tickable instance, starts silently so entering its radius never
  waits for a broadcast, smoothly fades over 24 blocks, and stops as soon as the centre is removed or
  the pad becomes incomplete. The old server-side 90-tick one-shot is removed, preventing overlapping
  copies and hard volume boundaries. Portal skin depth writes and the corrected post-terrain animated
  focus keep the Rift visible without rendering it behind water.
- Pylon flare sprites use source alpha in their additive blend, preventing transparent RGB in the
  texture border from becoming visible square quads. The two custom enhanced regeneration/saturation
  effects reuse the vanilla regeneration and saturation HUD/inventory sprites through client
  extensions rather than duplicating Mojang textures.

Verification remained intentionally narrow: `:ChromatiCraft:compileJava` and
`:ChromatiCraft:runClientData` pass; `chromaticraft:monument_core_placement` and
`chromaticraft:pylon_power_crystal_recharge` each pass alone. No broad GameTest suite was run. The
remaining acceptance work is visual/audio/physics and must be checked in the client: the assembled
core ensemble, water ordering and item rotation, casting-stand outline, Void Rift terrain occlusion,
Luma camera/movement, Liquid Ender bucket, portal animation/fade, pylon flare edges and active Power
Crystal inventory animation.

## Monument client ensemble and Portal Rift texture correction (2026-08-22)

The five-core monument report exposed a client-authority break rather than a progression gate. V33a's
ambient Dimension Core ensemble starts as each correctly positioned, primed core is installed; it does
not wait for all sixteen cores, completed mineral inlay, progression, dimension tuning, or a ritual
trigger. The ritual itself remains correctly gated on all sixteen cores and the full inlay.

- The Structure Controller now implements the complete 26.2 block-entity update contract: its chunk
  update tag is generated through `ValueOutput`, its client copy loads through `ValueInput`, it emits a
  normal block-entity data packet, and `setMonument()` sends an immediate block update. Previously its
  server save contained `monument=true`, but joining clients received vanilla's empty default update
  tag. `TileEntityDimensionCore.spawnConnectFX` consequently saw an unmarked controller and suppressed
  every eight-tick note, centre beam, sibling beam, and note particle.
- DragonAPI's equivalent compatibility seam is corrected at the shared base: `getUpdateTag` now uses
  `saveWithoutMetadata(provider)` and `handleUpdateTag` dispatches `loadAdditional(ValueInput)`. This
  preserves legacy base fields through its existing adapter while no longer discarding fields owned by
  modern `ValueOutput`/`ValueInput` subclasses.
- `chromaticraft:monument_client_sync_contract` constructs a marked server controller, serializes its
  real update tag, applies that tag to a fresh client-like controller, and asserts that both ends retain
  the monument marker. The focused test passes alone.
- Minecraft 26.2 moved the vanilla End-portal image to
  `textures/entity/end_portal/end_portal.png`; the placed Portal Rift renderer still referenced the
  V33a-era path and therefore displayed the missing-texture image. The world pass now targets the real
  26.2 asset. The original Portal item presentation is also fully ported as a special renderer: an
  unlit End-portal cube with the fifteen-second hue-cycling additive `bigflare` layer. Both Portal Rift
  identities use generated special item models.

Focused verification: `:ChromatiCraft:compileJava` passes, `:ChromatiCraft:runClientData` passes and
writes only the two changed Portal Rift item definitions, and
`chromaticraft:monument_client_sync_contract` passes alone. No broad GameTest suite was run. In-world,
reload the world/client so an already-generated controller receives a fresh chunk update tag, then
observe the installed cores for several eight-tick beats. A core belongs two blocks inward from its
matching outer rune; a core placed on the rune cannot derive the monument controller and is
intentionally silent.

## Monument legacy-glow composition correction (2026-08-22)

The now-operational Dimension Core ensemble exposed a 1.7.10 texture-format mismatch in its beam
particles. `centerblur3`, `flare`, `fade` and the related atlas glows are opaque RGB images: V33a used
black as transparency because `GL_ONE, GL_ONE_MINUS_SRC_COLOR` erased black directly in the main
framebuffer. Sending those images through 26.2's alpha-composited particle target gave every particle
full rectangular coverage, exposing the dark quad around each beam point and making overlapping
points look like they were z-fighting.

- Block-atlas additive particles now use a dedicated legacy additive pipeline and fragment shader.
  It derives target coverage from the source sprite's RGB brightness, retains any real source alpha,
  and keeps V33a's ADDITIVEDARK colour equation. Fully black texels are discarded.
- The legacy shader deliberately omits fog colour injection. V33a's particle render modes disabled
  fog, and adding fog RGB to a nominally black keyed texel would recreate a visible quad at distance.
- Alpha-backed 16x/64x animated particle sheets retain the normal modern additive-particle pipeline;
  their existing alpha is not reinterpreted.

This is a client-render-only correction, so no server GameTest was added or rerun. Focused build
verification is `:ChromatiCraft:compileJava`; in-world acceptance is the active monument ensemble:
beam points should blend into continuous coloured paths with no square cells or hard overlap seams.

## Monument structure-safety and V33a gold-ring migration (2026-08-22)

A reload inside the monument exposed a destructive interaction between two individually valid systems:
V33a's Proxima login/respawn escape capsule cleared ordinary solid blocks around a returning player,
but the modern monument is a vanilla `StructureStart` containing ordinary minerals and authored air.
The capsule consequently replaced part of the monument with air and nonreinforced Cloak Shielding.

- Proxima arrival clearing now refuses the whole operation whenever its ellipsoid box intersects a
  ChromatiCraft structure piece. Controller-backed layouts are also protected as a compatibility
  fallback, so the same rule covers NBT puzzle structures while they transition to native structure
  registration. The escape capsule remains unchanged for ordinary terrain.
- The mineral audit found a second source-parity error. V33a keeps its old probabilistic gold-ring
  table inside a block comment, then places a different 76-block ring unconditionally with direct
  `setBlock` calls. The port had activated the 72 obsolete coordinates, counted them as ritual cells,
  and omitted the live ring. Generation now places the exact 76 active coordinates and the ritual's
  expected inlay is the correct 305 cells: 304 probabilistic cells plus the registered-only centre
  chroma.
- Existing monuments carry a persisted `monumentLayoutVersion`. A pre-version monument performs one
  repair pass when its controller loads: authored template blocks destroyed by the capsule are
  restored, non-authored capsule Cloak is removed, the obsolete gold layout is removed, the active
  ring is installed, and any completion-inlay cell actually carved to air/Cloak is restored to its
  required mineral. New monuments are stamped current during structure placement and never migrate.
- A rejected activation now logs the first wrong core (including expected colour and coordinates),
  conflicting core owner, missing real owner, or wrong inlay block. This preserves V33a's in-world
  error cue while making a false `monument_t` diagnosable from the server console.

Focused verification only: `:ChromatiCraft:compileJava` passes;
`chromaticraft:monument_mineral_inlay`, `chromaticraft:proxima_structure_safety_guard`, and
`chromaticraft:monument_ritual_checks` each pass alone. No broad GameTest suite was run. Existing-world
acceptance is to load the old monument once, confirm `monumentLayoutVersion: 1`, inspect the repaired
fixed gold ring and capsule cut, then trigger the controller. The completion inlay still legitimately
requires glowstone, redstone, emerald, diamond, quartz, lapis and chroma cells; fixed gold is structure,
not a completion material.

## V33a particle depth-mask parity (2026-08-22)

The remaining square/self-intersection artifacts in the active Dimension Core ensemble came from a
render-state mismatch, independent of its legacy black-key texture conversion. V33a's shared
`ThrottleableEffectRenderer.doRenderParticles` called `glDepthMask(false)` before drawing every
particle layer and restored it afterwards. The particles remained depth-tested against the world but
never wrote their overlapping quads into the depth buffer. This is especially visible in
`TileEntityDimensionCore.spawnConnectFX`, whose beam is a dense sequence of blur particles spaced at
quarter-block intervals.

- Both 26.2 additive particle pipelines now use a `DepthStencilState` with the existing reversed-depth
  comparison and `depthWrite=false`. This is the direct modern pipeline equivalent of
  `glDepthMask(false)`; it does not disable terrain occlusion or make the effect render through walls.
- The particle-target routing and alpha/legacy-black coverage shaders remain unchanged. Only depth
  writes were corrected, allowing successive beam points, note flares and floating seeds to blend
  instead of depth-rejecting one another.

This is client-render-only and has no meaningful headless GameTest. Focused verification is
`:ChromatiCraft:compileJava`; in-world, partial and full monument ensembles should show continuous
blended connection beams without rectangular depth cutouts, while solid blocks must still occlude
the particles.

## Monument cinematic, score audio and event-family parity (2026-08-23)

The first successful ritual exposed three independent client/server splits that the initial modern
implementation did not reproduce. The score ran but was silent because the general
`ChromaSounds.playSound(ClientLevel, ...)` compatibility overload intentionally rejects client-side
world calls; the camera angles changed but its position did not because `Camera.alignWithEntity`
overwrote a direct Camera position immediately after `ComputeCameraAngles`; and detaching the view
from the real player removed V33a's incidental movement lock and avatar concealment.

- The six authored monument recordings now play as direct client `SimpleSoundInstance`s at their exact
  0, 28,000, 66,500, 86,000, 104,800 and 123,500 ms score offsets. They use no attenuation, are kept as
  concrete instances so every ritual end path can stop them, and suppress vanilla/dimension music for
  the duration. The per-core ray cue uses the same direct path with V33a's pitch factor and random
  0.4–0.8 volume.
- The cinematic owns a private, unspawned Marker camera entity. Each render frame writes the exact
  V33a epitrochoid pose to that entity before vanilla samples it, forces first-person, hides the local
  avatar, and restores the previous camera entity, camera mode, HUD and bob setting on stop, restart or
  level exit. The server independently anchors the activating player, zeroing velocity and sprint so
  movement packets and pre-existing flight momentum cannot carry the real player away.
- Rays are no longer immediate generic spheres. Each compatible colour receives its original random
  note-relative delay, an eight-segment maximized DragonAPI lightning path, coloured five-scale outer
  blurs, white two-scale cores, 1/32-segment sampling and 20–59 tick lifetime.
- The score's five one-shot families are no longer collapsed into a generic particle cloud. FLARES,
  PARTICLECLOUD, PARTICLERING, TWIRL and PINWHEEL retain V33a's counts, terrain-relative placement,
  radii, hue equations, spiral rates, size/lifetime envelopes and alpha behaviour. In particular the
  two large events intentionally create 256–384 terrain motes and 735 pinwheel particles.
- V33a's separate `MONUMENTCOMPLETE` transition is restored as a client payload rather than being lost
  inside the running boolean. At the start of the final three-second shot it fires 32–95 six-scale
  wandering seeds and all sixteen white-to-element centre/core beams exactly once; the later stopped
  state still owns teardown and Aura Locus conversion.

Focused verification only: `:ChromatiCraft:compileJava` passes and
`chromaticraft:monument_ritual_player_hold` passes alone (one test, 3.429 s). No broad GameTest suite
was run. Audio, camera motion, avatar concealment, post-processing and the authored particle choreography
remain client-visible acceptance checks.

## Monument recovery, source-fluid validation and locus rendering (2026-08-23)

The next ritual acceptance pass found four source-parity gaps: the recurring ring burst selected a
sprite name which does not exist in the retained atlas; an interrupted ritual persisted its controller
trigger but lost the transient ritual/song object; flowing Liquid Chroma satisfied the mineral audit;
and Aura Locus plus the Dimension Core screen shaders were still represented only by their base block
entity state.

- Ritual ring bursts now use the retained V33a `centerblur3` sprite. This removes the missing-texture
  quads from the initial burst and its later repeated score events without substituting a newly drawn
  effect.
- An interrupted monument ritual now has a two-sided load recovery seam. The controller clears the
  stale persisted trigger and re-primes every loaded Dimension Core when its transient ritual object is
  absent; each core also checks the controller from its own `onLoad`, covering cross-chunk load order.
  Natural successful completion remains distinct because the controller has already been replaced by
  the Aura Locus. The same core-enabling path is used by normal ritual teardown.
- Every `Mineral.CHROMA` inlay cell must now contain both the Liquid Chroma block and a source
  `FluidState`. A flowing level no longer completes the monument. The focused
  `chromaticraft:monument_ritual_checks` test covers a complete source layout, one deliberately flowing
  cell, and the restored source state; it passes alone.
- `RenderAuraPoint` and `GlowKnot` are fully modern submit renderers rather than excluded V33a code.
  Aura Locus retains the three animated Aura Point layers, PvP star, fade halo, 48-anchor centripetal
  closed spline, six legacy knot updates per tile tick, and layered fullbright line glow. Its render
  bounds and offscreen policy include the entire effect and its post-terrain submission keeps the glow
  ordered correctly around translucent world geometry.
- Dimension Cores and Aura Loci now feed one modern combined post pass. Dimension Cores retain V33a's
  32-block full-strength/128-block fade, hollow-centre attenuation, elemental tint, camera pinch and
  ritual `1 + colorFade*5` swell. Aura Loci retain line-of-sight gating, 8-block full strength,
  40-block falloff, first-50-tick age ramp, saturation and local pinch. A bounded live emitter UBO
  composes up to 64 visible loci in one pass and is cleared on every frame and logout.
- `aurapoint2-grid.png` is an opaque black-keyed V33a sprite, not an alpha-backed modern texture. The
  Dimension Core/Aura Locus layers and Dimension Core special item renderer now use a dedicated legacy
  additive sprite fragment which derives coverage from RGB luminance. This removes the inventory's
  black rectangle while preserving the original luminous colour animation in-world and in-hand.

Focused verification only: `:ChromatiCraft:compileJava` passes and
`chromaticraft:monument_ritual_checks` passes alone (one test, 4.306 s). No broad GameTest suite was
run. The combined post shader, black-key item composition, Aura Locus knot, repeated ring burst and
interrupted-session ensemble recovery are client-visible acceptance checks.

## Survival backbone: Chroma Collector and MAKECHROMA closure (2026-08-23)

The first post-audit survival blocker is closed. `CASTING -> MAKECHROMA` no longer ends at a registered
name or creative-only fluid: the V33a Chroma Collector is a complete 26.2 machine unit.

- The Collector is registered as a block, item, block entity, menu and client screen, with mining tag,
  loot table, language, guide identity, exact V33a animated top/side/bottom model and the retained
  original `collector.png` GUI. Its generated casting recipe is the exact base-tier `SES/ScS/CCC`
  pattern: any normal or boosted shard, Ender Eye, Glowstone and stone, with five experience, penalty
  threshold one and zero multiplier.
- Its two-slot inventory consumes Experience Bottles one per tick and returns Glass Bottles; each bottle
  produces 300 mB. Hoppers may insert only bottles and extract only returned glass. Owner contact above
  the machine consumes V33a's complete five-XP pulse, creates five mB and grants `MAKECHROMA` only once
  the existing `CASTING` prerequisite is satisfied. Fake, spectator and non-owner players are excluded.
- The 3000 mB input/output tanks use NeoForge's transactional `ResourceHandler<FluidResource>` API.
  Recognized XP fluids enter the input tank, redstone pauses only that fluid conversion, and Liquid
  Chroma leaves through every side. Inventory, fluid identity/amount, output amount and owner survive
  world and picked-item persistence. Aborted transfer transactions restore all tank state.
- Player fluid containers are offered the machine capability before its GUI opens. An empty bucket can
  therefore extract a real registered Liquid Chroma bucket in survival, closing the handoff into the
  already-ported Item Aura Infuser and world-fluid paths. The GUI synchronizes the third-party XP fluid
  identity as well as both levels and renders each fluid's baked 26.2 still sprite and tint at the exact
  V33a tank coordinates rather than substituting flat bars.
- Client ambience retains V33a's quarter-rate, annular random elemental rune particles around the
  machine. The source asset and all new model, item, loot, tag, language and recipe data are retained in
  their normal generated/resource locations; datagen was run without discarding unrelated concurrent
  Proxima work.

Focused verification: `:ChromatiCraft:compileJava`, `:ChromatiCraft:runClientData` and
`:ChromatiCraft:runServerData` pass. `chromaticraft:collector_survival_loop` was the only GameTest run;
it passes in 3.493 s and covers registration, four-bottle conversion, returned containers,
transaction rollback/commit, actual survival bucket filling, the exact five-XP direct pulse,
`MAKECHROMA`, and the exact recipe contract.

**Next survival slice:** follow the produced Liquid Chroma through the Item Aura Infuser and remaining
early charged-shard/resource recipes as one dependency closure. The acceptance target is a fresh
survival player progressing from the first Casting Table through Collector-made Chroma to the first
usable infused/charged components without commands, creative inventory or hand-edited progression.

## Aura Locus/Dimension Core ritual parity and Glowing Logic runtime (2026-08-23)

The post-monument visual audit found that the underlying Aura Locus data was correct but its modern
line submission was not. Every sampled spline point was paired with the first point, producing a
radial web instead of V33a's closed animated knot. The Dimension Core effect also differed in three
ways: its ambient ensemble could briefly survive the ritual-start packet race, its render layer did
not adopt the ritual's through-terrain visibility, and the combined post shader measured screen-space
distance without V33a's aspect correction.

- `GlowKnot` now submits consecutive independent segments and explicitly closes the final segment to
  the first sample. Its three original widths/alpha passes and spline motion are unchanged; only the
  erroneous starburst topology was removed.
- A running monument ritual now suppresses `spawnConnectFX` client-side in addition to the server's
  authoritative sixteen-core unprime. This closes the packet-order window which let ambient ORB/DING
  notes and connection beams overlap the authored monument score.
- Dimension Core sprites select a no-depth-test legacy-additive pipeline while the monument ritual is
  running. The post shader already bypasses line of sight in that state, so both the core and its swell
  remain visible through the monument exactly as V33a's ritual renderer does. Ordinary non-ritual
  cores remain terrain-occluded.
- The combined locus post shader now scales vertical screen delta by `ScreenSize.y/ScreenSize.x`, the
  exact operation in V33a's shared `distsq` shader helper. This restores the Dimension Core swell's
  circular footprint and apparent strength on widescreen displays without inventing a stronger
  coefficient than the original `1 + colorFade*5` ritual intensity.

Proxima's first admitted puzzle is now **Glowing Logic**. Its former runtime-only boundary is closed:
the puzzle is registered as a native 26.2 Structure, its source geometry is emitted as canonical NBT
templates, and the Java piece is limited to deterministic composition, chunk clipping, terrain
blending and block-entity binding. It is available to `/locate structure chromaticraft:light_panel`
inside Proxima and participates in the normal sixteen-colour assignment pool.

- All seven retained `tier0.png` through `tier6.png` sheets are decoded server-side as authored puzzle
  data. Their exact switch/row dimensions, RGB connection masks, no-replacement selection and
  green-required/red-unless-blue-cancelled solution rule are preserved and validated.
- The runtime restores V33a's 5/8/12-room difficulty ladder and tier sequence
  `0,0,1,1,2,2,3,3,4,4,5,6`, per-structure switch permutation, unique notes from a randomly selected
  key signature, and exact room-relative switch, panel and connected-door coordinates.
- Every room's wiring, active switch mask, origin and pitches persist through 26.2
  `ValueOutput`/`ValueInput`. `TileEntityStructureController` rebinds template-placed switches, projects
  state into the three Light Panel columns, opens/closes the room's Chroma Door on the original
  completion rule and plays the original pitched DING on upward toggles.
- The controller no longer scans distant room chunks during its world-generation initialization.
  Projection on load and every twenty server ticks touches only already-full, loaded chunks, so
  switches, panels and doors that load later can recover their persisted state without forcing
  remote chunk generation. The planner again reports the entrance chunk as V33a's centre, and
  the solved check requires all rooms rather than treating the password terminal as a bypass.
  This correction compiles in a serial, two-gigabyte-capped build; no GameTest server was launched.
- Eleven generated NBT templates reproduce V33a's bottom/section/top entrance staircase, the seven
  tier-sized puzzle rooms and the terminal loot chamber. The custom Structure piece composes those
  templates on the exact original offsets, writes only inside the active chunk box, restores the
  surface mountain and water pad, and binds every controller, switch, core and password terminal to a
  stable `(type, generation index, colour)` identity. A Dimension Core therefore reconnects to its
  generator after a server restart instead of depending on the old process-local UUID cache.
- The easy/normal/hard planner retains V33a's 5/8/12 rooms, `20+rand(80)` base height, nineteen-block
  room stride, reward/core/controller offsets and generated puzzle seed. The reward floor is the one
  breakable shielding surface; the rest remains reinforced. This expresses V33a's transient
  `addBreakable` cache as persistent block state rather than silently losing it after reload.
- The two room chests use a dedicated data-generated table: the active 26.2 stronghold-corridor pool
  plus V33a's four independent 70% Luma Dust, 90% boosted blue shard, 50% Lumenite and 10% Radiant Gem
  rolls. Luma Dust and Lumenite are separate modern registry items, with their exact source atlas
  sprites; no metadata identity was reintroduced.
- The first-room Structure Password is a real eight-slot persisted block entity/menu/screen. It accepts
  the source colour-bearing item families, evaluates V33a's `ElementEncodedNumber` password against
  the player and game version, returns all eight inputs on success, force-opens the structure and
  survives reload through the same stable structure tuple. Its source GUI texture and password block
  texture are retained rather than redrawn.

Focused verification only: `:ChromatiCraft:compileJava`, `:ChromatiCraft:runClientData` and
`:ChromatiCraft:runServerData` pass. Client datagen retained 1,014 files and wrote the seven affected
outputs; server datagen retained 710 files and wrote nine. `chromaticraft:monument_ritual_player_hold`
was previously run alone and asserts all sixteen cores are silenced at ritual start.
`chromaticraft:light_panel_pattern_library` was rerun alone after the structure/password work and
passes 1/1 in 3.943 s; it covers every authored pattern, solvability, runtime permutation, difficulty
ladder, exact planner offsets, V33a coordinate mapping and state persistence. No broad GameTest suite
was run. Large natural structure composition, the password GUI and all client-only Aura/Dimension Core
visuals remain explicit in-world acceptance checks.

The remaining shared player foundation is now closed as well. `ProximaStructureSessions` restores
V33a's 192-tuning entry gate, exact 16x16 core-entry scan, active generator identity, per-player puzzle
tick, cheating-prevention tick and Y=114 exit boundary, with logout/dimension cleanup. Sessions are
transient by design; a loaded core repairs them after a relog instead of trusting a process-local UUID
or a stale “already sent” marker. Entry sends the structure identity to a dedicated non-toast GUI
layer, which shows the source structure name.

Dimension Core removal is player-attributed again. A survival break rechecks range, whitelist and the
generator's solved state before recording the recovered colour, removing the active session and opening
the authored breakable exits; creative retains V33a's force-open-without-reward behavior. The independent
`Structure_Color_Completion` death-persistent tag is live in `ProgressionManager`, advances
`STRUCTCOMPLETE`, advances `ALLCORES` after all sixteen and clears `ALLCORES` if any colour is removed.
`/chromaprog ... dimstruct <colour> <bool>`, `maximize` and `reset` all cover this axis now.

Recovering a non-forced core sends the source player/version/difficulty/type/generation-index password.
The client plays `LOREHEX` and uses a seven-hundred-tick GUI layer rather than a Minecraft toast: the
eight rune cells reveal one per five ticks over V33a's full-screen dark wash and retain the original
first-eighth fade-in/latter-half fade-out and coloured triple frames.

Focused verification only: `:ChromatiCraft:compileJava` passes with the existing deprecation warnings,
and `chromaticraft:proxima_structure_completion_loop` passes 1/1 in 4.414 s. It exercises active-session
identity/removal, a real player-attributed Dimension Core recovery, the first-core stage, all-sixteen
stage, single-colour rollback, reset and maximize. The unrelated ReactorCraft UF6 component parse
warnings still appear during resource reload and do not affect the selected test.

## Survival backbone: Liquid Chroma charging, Pool Recipes and Raw Crystal (2026-08-23)

The Collector handoff now continues through the first real alloy and into the Item Aura Infuser's
input instead of stopping at a bucket of decorative fluid.

- Chroma Berries and Elemental Stones dropped into Liquid Chroma now require a credited player with
  every `SHARDCHARGE` prerequisite, matching V33a's `getDropper` gate. A valid matching plain shard
  retains its per-entity charge counter, converts the whole stack at 6000 accumulated charge, keeps
  the output alive indefinitely, preserves the credited owner, plays `INFUSE`, emits the original
  rising rune/completion burst, grants `SHARDCHARGE`, and fully clears the activated pool. Berry,
  ether, element and one-use Elemental Stone state remain persisted by the pool block entity.
- Pool alloying is now a registered datapack recipe type and serializer rather than a hardcoded
  runtime map. A recipe owns its catalyst, counted unordered ingredients, result, extra progression,
  doubling permission and minimum duration. Matching reserves stack counts across ingredients so
  overlapping tags cannot double-count one entity; completion consumes the inputs and catalyst
  atomically, applies V33a's ether doubling and `DOUBLECRAFT`, emits one-count unlimited-lifetime
  outputs credited to the catalyst owner, consumes the Liquid Chroma source and grants `ALLOY`.
- The runtime retains V33a's one-in-five scan, persistent active state, ether speed curve and
  probability ramp. Active recipes send the original one-per-tick `EntityChromaFluidFX`-style rising
  chroma droplet to nearby clients; the modern particle explicitly disables collision as V33a's
  `noClip=true` did.
- The first exact source recipe is data-generated as
  `chromaticraft:pool_alloying/chroma_ingot`: one iron catalyst plus sixteen Chromic Dust makes one
  Chroma Alloy Ingot. Full ether doubles it. This is the missing producer for the eight ingots in the
  already-ported Item Aura Infuser casting recipe; Chromic Dust itself is obtained from the existing
  progression-gated Energized Rock worldgen.
- `RawCrystalRecipe` is restored as a five-tick/five-XP base casting recipe: four Purification Powder
  around any **plain** shard produce two Raw Crystals. The metadata-free plain-shard tag is deliberately
  disjoint from boosted shards, preserving V33a's metadata 0-15 input. Purification Powder already
  comes from the registered Rock Flower and its worldgen/drop contract, so the Item Aura Infuser now
  has a survival-obtainable input as well as a survival-obtainable machine recipe.

Focused verification: the final source snapshot compiles, server datagen writes both
`pool_alloying/chroma_ingot.json` and `raw_crystal.json`, and
`chromaticraft:pool_alloying_survival_loop` passes alone in 3.890 s. The focused
`chromaticraft:liquid_chroma_elemental_loop` and `chromaticraft:raw_crystal_survival_recipe` contracts
are registered for the next isolated runner window; no broad GameTest suite was run.

**Deliberate boundary:** the generic Pool Recipe engine is complete, but the other V33a recipes are
not claimed until their exact metadata-free resource identities exist. Fiery, Ender, Water,
Conductive, Aura, Space, Experience and Complex alloy families still need their missing Firaxite,
Water Dust and related tiered resources registered as independent items/blocks before their datapack
recipes are admitted. The next survival slice should add those identities in original dependency
order, then exercise Collector -> activated pool -> boosted shard -> Chroma Alloy -> Item Aura
Infuser -> Raw Crystal -> Iridescent Crystal as one command-free acceptance path.

## Survival backbone: tiered ores, geode geometry and the alloy family (2026-08-23)

The first Pool Recipe boundary above is now superseded. The ordinary early-alloy inputs and the
post-dragon spatial input have real, independently registered resource and ore identities; none use
legacy metadata or a colour/component discriminator.

- Fluid Essence and Firaxite are separate item registrations using exact 16x16 crops from V33a's
  `items_resource.png` indices 137 and 138. Existing Enderstone Powder and Spatial Rifting Powder
  remain their own registrations as well. This follows the standing rule that each old metadata
  identity becomes its own modern registry name.
- Fused Crystals, Radiant Stone, Fluid Stone, Ender Stone, Firaxite and Spacerift Stone are real
  `BlockTieredOre` identities with
  their original host blocks, progression gates, generation chance/count/size and drop arithmetic.
  Fused Crystals close the `CHARGE`-gated source of Binding Crystals; Radiant Stone closes the
  `MULTIBLOCK`-gated source of Focal Powder. Both use four size-eight stone-hosted attempts per chunk
  and their exact Fortune-zero 1..3/1..6 drop ranges.
  Fluid/Ender/Firaxite retain V33a's stone host—even where the names suggest a dimension—and therefore
  generate with the other stone-hosted ores in the Overworld. Spacerift Stone targets End Stone, is
  restricted to End biomes, uses the source flat y=0..127 band, makes nine size-eight vein attempts in
  one of every two chunks, and remains disguised until `KILLDRAGON`.
- Vibrant Crystals restore V33a's ordinary-overlay `RESO` End ore as its own modern registry
  identity. It targets End Stone, remains disguised until `ABILITY`, uses eight size-eight attempts
  in every chunk across the original flat y=0..127 band, and drops exactly `1 + 4*Fortune`
  Resonant Dust. Its name is the authoritative `chroma.tieredore.11` text rather than a new inferred
  name, and its animated tier-11 underlay/overlay remains a cube model because `RESO` was not one of
  V33a's geode-rendered ore types.
- The earlier tiered-plant slice already supplies the same Resonant Dust through `MULTIBLOCK`-gated
  Element Bulbs and supplies Transmissive Dust through `PYLON`-gated Radiance Bushes. Both retain
  one-in-five/two-attempt late worldgen, their leaf/sand support rules, total pre-stage concealment
  and the exact Fortune-zero odd-valued drop formulae. Thus all four non-shard ingredients of the
  Iridescent Crystal Chunk now have concrete survival sources; Vibrant Crystals are the later End
  alternative, not an artificial prerequisite for the early multiblock recipe.
- Glowing Rock (`LUMA`), Echostone (`ECHO`) and Lumenite (`RAINBOW`) are admitted now that Luma Dust,
  Echo Crystal and Lumenite are independent registered items. They preserve the source display names,
  ordinary animated overlay models and exact gates/drop arithmetic. Glowing Rock is a
  `STRUCTCOMPLETE` stone-hosted one-by-eight vein per chunk; Echostone is a `CTM` stone-hosted
  one-by-eight vein in one of every two chunks. Lumenite is a `USEENERGY` stone-hosted pair of
  size-eight veins per chunk, but deliberately uses V33a's flat y=0..127 late-ore band because its
  original ordinal follows Firestone.
- Thermitic Rock and Avolite close the complete fifteen-ore V33a family. Thermitic Rock is an
  END-gated Netherrack-hosted geode with the source one-in-three/two-by-sixteen generation and
  `1 + rand(1+fortune^2)` Thermitic Crystal drop. Avolite is a POWERCRYSTAL-gated ordinary-overlay
  stone ore with one size-eighteen vein in one of every two chunks and exact
  `1 + rand(1+fortune/2)/2` drop arithmetic. Their independently registered drop items use exact
  atlas crops (legacy indices 145 and 152), and Avolite's block id is `avolite_ore` so it cannot
  collide with the item registry id.
- V33a's geode renderer is no longer represented by a flat cube overlay. One shared deterministic
  geometry helper ports the exact 8x8 perturbed surface arrays, shared-row indexing, 0.2..0.8 inset,
  UV orientation and six face transforms. Block and special item models use the same geometry;
  exposed host shells remain shaded while the inset ore surfaces are fullbright. Sixteen
  coordinate-selected shapes preserve the source's nonuniform world appearance without making the
  baked model nondeterministic between reloads.
- Seven alloy recipes are now exact data-generated Pool Recipes: Chroma Alloy, Firaxite Alloy,
  Resonating, Fluidic Essence, Radiative, Aura Conducting and Spatially Warping Ingots. (There are
  seven outputs total.) Catalysts and every counted input are copied from V33a `PoolRecipes`,
  including the Spatial recipe's iron + 16 Spatial Rifting Powder +
  32 Glowstone Dust + 64 Redstone + 16 Quartz + 4 Diamonds. All retain ether doubling.
- Focused contracts `fluid_stone_survival_path`, `geode_ore_survival_path` and
  `pool_alloying_early_recipe_family` cover disguise drops, progression reveals, source Fortune-zero
  bounds, registry identity, exact counted inputs, one-item-short rejection and the doubling flag.
  `tiered_plant_survival_sources` additionally pins both plant supports, gates, worldgen rates and
  exact drop ranges.
  They are registered but intentionally have not launched a GameTest JVM while four older Java
  processes retain roughly 11 GB of working set; `pool_alloying_survival_loop` is the already-passed
  runtime anchor for the shared engine.

Validation at this boundary: `:ChromatiCraft:compileJava` passes. Final server datagen passes with 744
files and client datagen passes with 1,030 files. The generated set includes all six new ore loot/tag,
configured/placed worldgen and geode item-model contracts, the End biome modifier, all ten admitted
Pool Recipes and the two exact source display names. One intermediate client-datagen attempt collided
with a concurrent RotaryCraft class-directory rewrite; it was retried successfully after that writer
released the directory, without deleting or weakening either task's output.

Experience Gem and Chromastone are also admitted as the next two exact Pool Recipes. The former uses
one Iridescent Crystal Chunk catalyst, four Obsidian and eight Emeralds; the latter uses the Experience
Gem catalyst and one of each of the other seven ingots. V33a's would-be `allowDoubling = false` lines
are commented out, so both retain the ordinary doubling rule and have no invented effect or gate.

Data Crystal duplication is admitted independently with its exact Data Crystal catalyst, eighteen
Crystal Dust, Data Crystal output, normal doubling and sole `TOWER` gate.

The remaining tiered-resource boundary is now closed as a continuation of this survival slice:

- All **15/15 V33a tiered ores** and **7/7 tiered plants** have concrete block identities. No old
  metadata selector or colour/type component was reintroduced.
- Vibrant Pod and Glowing Roots retain their distinct tree-trunk/root siting, ALLOY/TURBOCHARGE gates
  and exact drops. Luma Beans and Enrichment Root are independent items.
- The plant renderer audit also removed the temporary crossed-plane appearance from Vibrant Pod.
  Its dynamic chunk model now starts at V33a's 0.25..0.75 body, extends each of its six bounds toward
  neighbouring logs, emits the shaded backing cube and a full-bright six-face overlay expanded over
  the source's 0..1/16 interval. Glowing Roots use the source crop inset, the other five retain
  crossed squares, and every inventory form contains both the backing and front sprite layers.
- All seven chunk models now enforce the same local-player progression test as V33a's ISBRH. Before
  the required stage they contribute no model parts at all; together with the existing empty hover
  shape this restores genuine concealment rather than merely making a visible plant untargetable.
  Their custom blockstates are emitted by `ChromaModelProvider`; the five stale ungated generated
  snapshots were removed and must be regenerated in the next safe client-datagen window.
- The five particle-bearing plants now also retain V33a's every-other-tick `EntityCCBlurFX` branches:
  original positions, random colour channels, scales, velocities and signed gravity. Vibrant Pod and
  Glowing Roots intentionally remain particle-free because their V33a switch branches are empty.
- Plant drops run through the now-ported `DimensionTuningManager` with the original 0..384 clamp, and
  Rock Flower once again accepts structure shielding as a ceiling specifically inside Proxima. These
  replace two obsolete forward-reference comments; outside Proxima both paths remain unchanged.
- Vanilla-dimension routing now matches `TieredWorldGenerator`: all seven plants run in the Overworld
  and End, while hostile Nether generation excludes only Rock Flower and Essence Lily. A dedicated
  `rock_flower_end` placed feature removes its ordinary one-in-two rarity filter because V33a makes
  that plant an every-chunk attempt in dimension 1; its two attempts and site scan remain unchanged.
- Thermitic Rock and Avolite retain their exact hosts, gates, worldgen rates, source geometry and
  drop arithmetic. Thermitic Crystal and Avolite are independent items.
- `luma_beans.png`, `thermitic_crystal.png`, and `avolite.png` are exact 16x16 crops from V33a
  `items_resource.png` indices 149, 145, and 152. A pixel comparison against the source atlas reports
  zero differing pixels for all three.
- `tiered_plant_survival_sources` and the existing tiered-ore family contract now include the new
  support/gate/drop cases. They are registered but were not launched in this continuation: several
  existing Java processes still retain roughly 12 GB, and the sandbox could not acquire the shared
  Gradle wrapper lock. Compile and client/server datagen therefore remain required at the next safe
  runner window; generated-resource snapshots have not been hand-edited around that boundary.

**Remaining Pool Recipe boundary:** Structure Map creation requires the full artefact/data-crystal
effect contract, disables doubling and enforces a two-minute minimum duration. It is not represented
by a plain stand-in recipe. The Item Aura Infuser already has its NBT multiblock, source-fluid tank,
focus acceleration, full conversion/overflow path, particles/rendering, recipe and focused
`item_aura_infuser_loop`; the next isolated GameTest window should run that contract together with the
new ore/alloy family before expanding into the Structure Map effect.

## Proxima structure: Three-Dimensional Maze (2026-08-23)

The second admitted Proxima puzzle is now **Three-Dimensional Maze** (`TDMAZE`). Like Glowing Logic,
it is a native 26.2 `Structure` with an exact custom placement tied to Proxima's persistent colour
ring. It participates in the normal without-replacement sixteen-colour assignment pool and can be
found with `/locate structure chromaticraft:three_d_maze` inside Proxima.

- `ThreeDMazeLayout` separates the source's seeded topology from world writes. Easy, normal and hard
  retain the exact 8x6x8, 16x8x16 and 24x12x24 cell dimensions, four-block cell stride, randomized
  depth-first perfect-maze backbone, top and bottom exits, braided extra cuts, room count/radii,
  distance/overlap retries, lights and glass windows. Compact direction masks make the same maze
  reproducible from the serialized piece seed after restart.
- Room formation retains V33a's cracked replacement floors, permanent damper ceilings, iron-bar
  perimeter, ordinary and five-cell-square chambers, conditional Chroma/glowstone centre decoration,
  and dungeon/desert loot-chest selection. The guaranteed bonus remains a boosted coloured shard,
  Raw Crystal or rare Complex Ingot with source radius-based counts. The Chroma centre ring is
  explicitly prohibited when the centre cell has a downward connection, preserving the navigable
  opening in the original `MazeRoom` logic.
- Four generated NBT components are authoritative: a sealed cell, the repeating entrance shaft, the
  surface pavilion and the reward chamber. The latter two are mechanically imported from V33a's
  coordinate builders with old metadata resolved to concrete 26.2 Shielding and Crystalline Stone
  registry identities. Java owns only topology cuts, room alternatives, terrain support, loot and
  stable block-entity binding; it does not recreate the canonical structure shell block by block.
- The piece composes all components through the active chunk bounding box, builds the original 17x17
  supported surface pad, and binds the terminal coloured Dimension Core to the stable
  `(TDMAZE, generation index, colour)` identity. The exact V33a entry and reward/core arithmetic is
  retained. As upstream has no stateful puzzle controller for this navigation challenge, reaching the
  core is the solve condition and no invented door state is introduced.
- Registry/datagen coverage includes the structure type, piece type, placement type, structure JSON,
  structure set and all four NBT templates. `chromaticraft:three_d_maze_layout` verifies all three
  dimensions, deterministic same-seed topology/decor, full cell reachability, both exits, room bounds
  and planner entry/core coordinates.

Focused verification only: `:ChromatiCraft:compileJava`, `:ChromatiCraft:runClientData` and
`:ChromatiCraft:runServerData` pass. `chromaticraft:three_d_maze_layout` was the only GameTest run and
passes 1/1 in 3.649 s. The first runner attempt exposed incomplete Gradle module task ordering when a
cache-free rebuild removed dependency outputs concurrently; rebuilding DragonAPI -> RotaryCraft ->
ElectriCraft -> ChromatiCraft serially restored the classpath. This was a build-orchestration failure,
not a maze source failure, and no broad suite was launched.

**In-world acceptance boundary:** locate a naturally assigned maze in a newly generated Proxima
layout, verify that its pavilion sits on supported terrain, traverse from the surface shaft through
the maze to its reward chamber, inspect both small and large room variants/chests, recover its
colour-bound core, relog, and verify the recovered-colour progression and opened breakable reward
boundary remain correct. Natural multi-chunk composition and the complete traversal are intentionally
left to this visual/player acceptance pass rather than a RAM-heavy generated-world GameTest.

## Proxima structure: Crystal Music (2026-08-23)

The third admitted Proxima puzzle is **Crystal Music** (`MUSIC`). Its complete vertical slice is now
implemented as a native 26.2 structure and registered in the persistent colour-ring assignment pool.
The datapack identity is `chromaticraft:music`; once server datagen has emitted the new structure and
structure-set entries it is discoverable with `/locate structure chromaticraft:music` inside Proxima.

- `MusicPuzzleLayout` restores V33a's 6/8/10-room difficulty ladder, twenty-block room stride and
  `max(6, 3*difficulty + room - 9)` generated-melody length. The exact fifteen source prefab melodies,
  rests and playback rates are shared with the already-ported Biome Fragment music puzzle. The
  original one-in-five prefab roll, no-repeat rule, 2.5x length ceiling, playable-key selection,
  octave limit, seventh rejection and tritone rejection are retained. Hash-set note candidates are
  sorted before seeded selection so a serialized structure seed reproduces the same room after a JVM
  restart instead of depending on process-specific hash iteration.
- `TileEntityMusicMemory` is the persistent room authority. It stores the melody (including rests),
  playback cadence, room index, connected-door centre, current input, replay position and solved
  state through 26.2 `ValueOutput`/`ValueInput`. Clicking its authored front face replays the melody;
  the sixteen existing four-quadrant Music Triggers send their actual playable note to the nearby
  memory. A wrong note resets the attempt with the source error cue, while a complete sequence opens
  the exact 3x3 Chroma Door. Password bypass force-completes without multiplying completion sounds.
- Replay notes now use a dedicated radius-scoped payload, matching V33a's client-only `MUSICPLAY`
  path instead of broadcasting server particles or a single positional sound. Each note is heard at
  both authored ends of the room (`memory z-1` and `memory z+9`) at the exact key ratio; every trigger
  colour capable of that note emits its local burst and projects the corresponding retained
  `ring0`-`ring3` interval icon against the inside wall.
- The trigger handler radius is nine blocks because the far edge of V33a's authored room is eight
  blocks from its memory. The old seven-block provisional scan silently excluded those triggers.
- Three canonical NBT components are generated by mechanically parsing the 918-cell `MusicFunnel`,
  1,032-cell `MusicPuzzleBlocks` and 318-cell `MusicLoot` V33a coordinate builders. Java owns only
  chunk-clipped terrain/shaft composition, room sequencing and mutable block-entity binding. Every
  former lamp metadata value is resolved to its own `crystal_lamp_<colour>` registry identity; colour
  metadata has not been reintroduced. The reward room's eight source `addBreakable` cells are emitted
  as persistent non-reinforced shielding.
- The entrance preserves the source's unusual `x+5` shaft alignment, permanent damper platforms and
  surface funnel. Rooms retain the exact memory/password, trigger/lamp and 3x3-door coordinates. The
  terminal Dimension Core is bound to the stable `(MUSIC, generation index, colour)` tuple, and the
  generator considers the structure solved only when every persisted room memory is solved.
- Registry coverage includes the memory block/entity/model/lang/mining tag, structure type, piece
  type, custom exact-layout placement type, structure key/set and generator admission. The memory is
  intentionally block-only and unobtainable, as it is structure-owned content in V33a.
- Focused `chromaticraft:music_puzzle_layout` coverage pins all three room counts, same-seed melody
  reproduction, prefab/generated length bounds, playable notes, playback cadence, wrong-sequence
  rejection, the memory-to-door completion loop, NBT persistence and planner/core coordinates.

**Validation:** `:ChromatiCraft:compileJava`, client datagen and server datagen pass. Focused
`chromaticraft:music_puzzle_layout` passes 1/1 in 3.941 s, and the separately scoped
`chromaticraft:three_d_maze_layout` parity test passes 1/1 in 3.896 s. The music run exposed a Java-collection parity
trap: V33a melodies deliberately contain `null` rests, but `List.copyOf` rejects null elements. Room
and block-entity snapshots now use null-tolerant immutable copies, and the focused persistence case
uses the real 26.2 disk-NBT path (`saveWithoutMetadata`/`loadWithComponents`) rather than the network
update tag. No broad GameTest suite was launched.

**In-world acceptance boundary:** use a newly calculated Proxima layout, locate
`chromaticraft:music`, verify the entrance/shaft and every room are complete across chunk boundaries,
click each memory's front face to hear its melody, test a wrong note/reset and correct trigger sequence,
verify each door and the final reward/core, then relog mid-puzzle and confirm solved rooms, playback and
password bypass all retain their state.

## Proxima structure: Cellular Automata (2026-08-24)

The fourth admitted Proxima puzzle is V33a's **Cellular Automata** (`GOL`). Its full
play-to-reward vertical slice is implemented as a native 26.2 structure, admitted to the persistent
colour-ring assignment pool and registered as `chromaticraft:gol` for `/locate structure` inside
Proxima. The variable board is composed in Java, while every fixed authored component is an
authoritative generated structure NBT.

- `GOLPuzzleLayout` pins the exact 12/16/24 radii, 25/33/49 board widths, `radius*4` initial-cell
  limits and integer completion thresholds of 500/952/2220 remembered cells. Its transition is the
  source's simultaneous Conway B3/S23 rule with off-board cells dead.
- `gol_tile` replaces V33a's four behavior metadata values with `memory` and `active` blockstate
  properties. These are mutable modes, not colour/content identities. Floor cells retain a small
  controller-binding block entity; passive ceiling-memory cells do not allocate one. Both left/right
  interaction and a player fall greater than one block retain the original toggle paths.
- `gol_controller` owns simulation cadence and persistence. It samples every fifth world tick and
  applies one tick later, matching the old count-then-update phases while using one ticker instead of
  up to 2,401 independent tickers. A prepared-generation guard prevents a reload between those phases
  from applying an empty buffer and erasing the board.
- Source behavior that is easy to miss is preserved: activating a floor cell lights its ceiling cell;
  cells that later die during simulation leave that ceiling cell lit. The accumulated ceiling trail,
  not the final live population, is what the stop button evaluates. Manual deselection and stop/reset
  clear both layers. Success persistently opens the authored five-wide exit and retains solved state.
- The retained `gol_off/on`, `gol_mem_off/on` and `gol_control_play/stop/end` textures now have
  four-state cell and two-state controller datagen models. Both blocks are structure-owned/block-only,
  registered with their block entities, language entries and mining tags; no bogus obtainable item
  or old packed metadata has been introduced.
- Eight generated NBT templates retain the exact V33a fixed geometry:
  `chamber_1`/`chamber_2`/`chamber_3`, `entrance_door`, `entrance_prefab`, `exit_door`, `loot` and
  `surface`. The mechanical import is pinned to 595 entrance-prefab placements, 136 entrance-door
  placements, 45 exit-door placements, 358 loot placements and the source's 51 actual breakable
  reward cells. The original pavilion has no invented dirt foundation; terrain-dependent work is
  limited to its clearing, authored floor and vertical shaft.
- `GOLStructureGenerator`, `GOLStructurePiece`, `ProximaGOLStructure` and `GOLPlacement` restore the
  exact underground planning contract: floor Y in 31..70, 12/16/24 radius, controller and entrance
  offsets, stable terminal Dimension Core binding, chunk-clipped composition and natural layout
  assignment. Registration covers the structure type, piece type, exact-layout placement type,
  structure key/set and generator pool.
- Focused `chromaticraft:cellular_automata_rules` coverage pins all difficulty constants and a
  two-generation blinker, planner coordinates, a real 3x3 board, cell binding/toggling, accumulated
  trail completion, five-wide exit opening and disk-NBT persistence. `:ChromatiCraft:compileJava`,
  client datagen and server datagen pass; the focused test passes 1/1 in 3.997 s. No broad GameTest
  suite was launched.

**Startup registry-order correction:** `CrystalMusicManager` is initialized while ChromatiCraft's
deferred mob-effect holders are not yet registry-bound. `CrystalPotionController.isBadEffect` no
longer dereferences the known custom beneficial Saturation/Regeneration holders during that window;
it classifies those two by holder identity and preserves normal category lookup for every bound
vanilla/other effect. This removes the startup NPE without weakening potion classification.

## Proxima Chromatic Beams foundation (2026-08-24)

The next source-complete puzzle selected for admission is V33a's **Chromatic Beams** (`LASER`). The
selection audit rejected Ant Farm, Altar, Dynamic Bridges, Pinball, Gravity, Tessellation and Traces
as immediate candidates because their shipped V33a generators explicitly say cancelled/incomplete,
return no core/solve state, or both. They remain preserved source, but registering one would bypass
the original `isComplete()` filter and turn an unfinished upstream shell into fake port progress.

- `LaserPuzzleLayout` decodes all twelve retained V33a `.struct` assets. These files are exact
  uncompressed NBT streams with their full byte order reversed by `StructureExport`'s old encryption;
  the decoder reverses that stream and validates every entry against the original laser-effector
  registry identity and metadata enum.
- All authored effector state is retained: relative coordinate, twelve-way type, eight-way direction,
  RGB channels, rotation/fixed flags and difficulty, prism timer, full-block rendering, silent impact
  and pulse speed. Missing optional tags receive the live V33a field defaults instead of Java/NBT zero
  defaults that would freeze a pulse.
- The exact difficulty sequences are restored: 6 rooms/easy, 10/medium and 12/hard, with 121, 203 and
  286 authored effectors respectively. The source's X layout (`start + 13`, then room width + 23) and
  terminal loot-room anchor are exposed to the upcoming structure piece. Randomized movable-effector
  directions now derive from the serialized structure seed so chunk-order/reload cannot alter a room.
- Focused `chromaticraft:laser_puzzle_blueprints` coverage pins all room/effect counts, deterministic
  randomization, room stride, and the exact mirror-tutorial and complex-room bounds/type counts.
  `:ChromatiCraft:compileJava` passes, and the focused test passes 1/1 in 8.818 s. No broad GameTest
  suite was launched.
- `LaserPulseLogic` is the source-exact, world-independent interaction authority for all twelve
  effectors. It preserves RGB intersection, mirror/double-mirror/slit reflection rules, one-way and
  polarizer gates, the refractor's distinct +90/-45 branches, splitter replacement pulses, prism
  split/recombination timing, pass-through versus terminal targets and returning-pulse absorption at
  emitters. Its result explicitly distinguishes a pulse that continues in place from an absorbed
  pulse which produces replacement entities; collapsing those two paths would duplicate or lose
  beams in several authored rooms.
- `EntityLaserPulse` is now a registered 26.2 travelling entity rather than dormant 1.7.10 source.
  It retains eight horizontal directions, capped source speed, RGB synchronization, silent impacts,
  transparent-Shielding pass-through, forty-block distance culling, tonal RGB impact sounds and
  persistent/spawn state. Its client presentation is a fullbright camera-facing `fade_star` layer
  with the V33a trail, collision burst and Elemental Manipulator long-life reveal behavior. The
  modern `LaserPulseReceiver` and `LaserPulseEffect` contracts are ready for block-entity and external
  block effectors without coupling the pure interaction engine to a loaded world.
- Focused `chromaticraft:laser_pulse_interactions` coverage exercises every effector branch plus a
  real registered pulse entity's colour, direction, silence, speed and replacement-pulse motion.
  `:ChromatiCraft:compileJava` passes and that test alone passes 1/1 in 3.633 s. The run used the
  no-daemon, one-worker, selector-scoped harness; no broad GameTest suite was launched.

`LASER` is intentionally not yet registered in the Proxima assignment pool. The next slice is the
twelve modern laser-effector blocks/block entities backed by this interaction engine, followed by NBT
room-shell, entrance and loot composition and the persistent emitter -> target -> room door -> core
solve loop. The entity and rules foundation is accepted, but Chromatic Beams is not yet playable or
eligible for world assignment until that complete loop exists.

## Survival backbone: Personal Charger vertical slice (2026-08-24)

The V33a **Personal Charger / Energy Focus** is now accepted as the next playable crystal-network
receiver. This slice deliberately stays outside Proxima, which is being developed in a separate task.

- `personal_charger` is a real block, block entity and special-rendered item. It retains the source's
  no-collision/full-selection presentation, full light, owner-protected mining, 60,000-lumen one-colour
  capacity, 32-block receive range, 200-lumen throughput, 800-tick request cooldown and 40% held-tool
  charge rate.
- The four rune sockets select the conducted colour only when all four agree. The complete V33a
  5x7x5 frame is an authoritative generated NBT template; `PersonalChargerStructure` only anchors it
  six blocks below the floating charger and substitutes the four rune placeholders with the selected
  concrete colour identity. No packed colour metadata has been reintroduced.
- Multiblock edits now revalidate receiver-backed structures through the shared adjacency callback,
  restoring immediate deactivate/reactivate behavior after a broken cell is replaced. A twenty-tick
  safety check also catches changes made while the receiver chunk was unloaded. Deactivation clears
  stored energy and resets the request timer.
- The exact multiblock casting recipe is in datagen: Crystal Core centre, all sixteen rune-ring colours,
  four Space Dust, four Beacon Dust, twelve Glowstone Dust and four diamonds across 24 stands; 2,400
  ticks, 800 XP, threshold 16 and zero repeated-craft XP multiplier.
- The submit-pipeline renderer retains V33a's inactive centre blur and active coloured centre/white
  roses/flare stack, additive-dark blending, no depth writes, source pulse curve and colour-tuned power
  cadence. Client ambience retains the paired coloured/white falling blur and rising rune particles.
  The item renderer uses the source's one `roses` layer at its original 0.875 half-size and rotations.
- Both guide identities are connected: `PERSONAL` resolves to the charger item, while `MINIPYLON`
  resolves to the same block and loads `multiblock/personal_charger` in the NBT structure viewer.
- `CrystalReceiverBase` no longer drops receiver item state at the modern data-component boundary.
  Stored energy plus placer name/UUID are written to and restored from item custom data, preserving
  owner protection and lumen contents across the ordinary break/place loop for all receiver subclasses.
- Focused `chromaticraft:personal_charger_receiver_loop` coverage checks NBT activation and colour
  selection, receiver constants, drain, energy/owner item persistence, mismatched-rune deactivation,
  and the full recipe contract.

**Validation boundary:** source/static parity and `git diff --check` pass. The Personal Charger NBT,
models, item model, language, mining tag and recipe snapshots still require the next safe client/server
datagen run. No Gradle or GameTest process was started in this slice because the concurrent development
session already owns the build/runtime resources and the known broad runner can exhaust workstation RAM.
Run only `chromaticraft:personal_charger_receiver_loop` after those snapshots are emitted.

**Next survival slice:** port the Weak Repeater's obtainable recipe/identity and then the Relay Source,
closing the early-game weak-network path into this charger before expanding to the next production or
storage machine.

## Survival backbone: Lumen Repeater vertical slice (2026-08-24)

The V33a **Weak Repeater**, displayed to players as the **Lumen Repeater**, is now the obtainable
transmitter that connects the early casting tier to the Personal Charger. This is a full behavior
port, not a short-range alias of the crystalline repeater.

- `weak_repeater` has its own block, block entity, block item, model/lang/loot snapshots and canonical
  `multiblock/weak_repeater` NBT definition. The guide's `WEAKREPEATER` machine page and
  `MINIREPEATER` structure page both resolve the same registry identity, and the structure viewer now
  routes the latter to that NBT template.
- Placement restores `ItemChromaPlacer.findFirstValidSide`: the repeater searches every face for a
  valid adjacent log support rather than remaining incorrectly fixed downward. Breaking and replacing
  that support revalidates immediately through the shared adjacency seam.
- Source constants are retained exactly: 16-block send range, 24-block receive range, 120-lumen
  throughput, 250 point degradation, one-eighth-block incoming/outgoing beams, any elemental colour,
  and no rain loss. `BlockTags.LOGS` is the metadata-free modern expression of the supported vanilla
  and ordinary mod-wood families.
- The source's intentionally restricted transmission is explicit through
  `WeakRepeaterSafeReceiver`. Personal Charger implements it now; Relay Source and Ritual Table will
  implement it when those pristine clusters are fully ported. Other receivers get zero modified
  throughput and retain the one-in-eight transfer-triggered failure risk.
- The complete 320-tick failure sequence is restored: surge cue, fire, escalating three-layer warning
  flares, elemental lightning and discharge cues, the weighted 50/20/40 explosion/burn/rupture
  outcomes, terminal particle/power-down payload, neighbouring fire, BLOWREPEATER progression and
  released ownership. As in V33a, the burning repeater continues to conduct during the visible fuse;
  only rupture permanently disables it.
- Damaged sneak-pop and ruptured mining no longer yield a healthy machine. The rupture is mirrored to
  a real boolean blockstate so reloads and chunk updates retain V33a's burned-side/open-top frame
  instead of continuing to draw the active texture. Wreckage returns the original
  3-8 sticks, 1-3 Crystal Dust stacks and fifty-percent Glowstone Dust salvage. The wooden block also
  retains V33a's upward fire-source behavior.
- The exact Core/Temple casting recipe is data-driven: four planks, two wooden rods, one Transmissive
  Dust, one Glowstone Dust and one Liquid Chroma bucket produce eight; the bucket returns empty. It
  takes 80 ticks, awards 80 XP, has Yellow and Blue ring runes plus the four authored White/Black
  outer runes, and is gated at the modern equivalent of the ENERGY research boundary. Core-recipe
  no-penalty semantics remain the default infinite threshold.
- Focused `chromaticraft:weak_repeater_survival_loop` coverage pins support removal/restoration,
  receiver safety, all network constants and the full recipe contract. It is registered but has not
  been launched while another development session owns the Gradle/runtime processes.

**Resource-loader correction:** the legacy mixed-case `textures/block/isbrhModels` directory is now
the valid lowercase `isbrhmodels` resource path. `fused_crystals`, `radiant_stone` and
`spacerift_stone` also expose `chromaticraft:tiered_ore` directly in their empty-state variant, with
`underlay`, `overlay`, `host_texture`, `geode` and `stage` beside it. Nesting that discriminator under
`model` made 26.2 parse it as an ordinary model reference and reject the entire blockstate. All active
tiered-ore blockstates and all touched generated JSON pass static parsing.

**Validation boundary:** resource JSON parsing and `git diff --check` are the available checks for this
slice. The Weak Repeater's binary NBT snapshot and the preceding Personal Charger snapshots still need
the next safe client/server datagen run. Do not launch the broad GameTest suite; after datagen, run only
`chromaticraft:weak_repeater_survival_loop` and the previously pending
`chromaticraft:personal_charger_receiver_loop`.

**Next survival slice:** fully port Relay Source—registry identity, inventory/persistence, enhanced
multiblock, storage-crystal exchange, request/drain behavior, renderer/GUI, exact recipe and focused
test—then mark it as a safe Lumen Repeater receiver to close the first practical
pylon -> Lumen Repeater -> Relay Source/Personal Charger loop.

## Proxima fidelity follow-up: paused ritual, loci, and maze bars (2026-08-24)

- `MonumentRitualEffects` now explicitly freezes its complete wall-clock timeline while Minecraft is
  paused. Client ticks can continue behind an integrated-server pause screen, and vanilla only pauses
  sounds which already exist; without this gate, later ritual events could create new Aura Locus
  lightning/impact sounds over the menu. Paused time is accumulated so resuming does not skip or
  catch up any camera, particle, ray, or score event.
- The Aura Locus knot retains V33a's bytecode-verified `GlowKnot(0.875)` radius and six spline updates
  per tick. Its perceived undersizing was the render pass: 26.2 vanilla lines write depth, causing the
  main strand to reject the two quarter-alpha overdraw strands which V33a drew under
  `glDepthMask(false)`. A dedicated alpha line pipeline now tests scene depth without writing it, and
  the world knot uses a two-pixel modern line width. The actual spline envelope has not been inflated.
- Aura Locus ambience is now an immediately started, silent-capable client loop keyed to each loaded
  locus. It retains the original volume-two/32-block linear range and pitch two in Proxima, but
  recalculates volume every sound tick so approaching and leaving the locus fade continuously instead
  of inheriting a one-shot's start-time volume.
- Dimension Core sprite layers use their through-wall pipeline outside the monument ritual too, and
  the DIMCORE post-effect emitter no longer has a terrain line-of-sight rejection. The separate Aura
  Locus screen effect deliberately retains V33a's LOS gate.
- Three-Dimensional Maze room windows no longer place nine disconnected default iron-bar states under
  worldgen update flag 2. Each placed bar and its existing horizontal neighbours are refreshed from
  the real vanilla connection rules, making the result independent of within-chunk placement order
  and joining all internal edges of the authored 3x3 panes.
- `:ChromatiCraft:compileJava` passes. Only the relevant
  `chromaticraft:three_d_maze_layout` GameTest was run; its added centre/corner bar-connection
  assertions pass 1/1 in 4.125 seconds. No broad GameTest suite was launched.

## Survival backbone: Relay Source vertical slice (2026-08-24)

V33a's **Lumen Relay Source** is now admitted as a complete registered machine rather than the
previous excluded 1.7.10 block entity. This work deliberately stays outside Proxima, which is owned
by a separate development task.

- `relay_source` has a dedicated block, block entity, block item, exact Techne model, block renderer
  and special item renderer. The missing `receiver.png` was recovered byte-for-byte from repository
  history and lives under the valid lowercase modern entity-texture path; no substitute texture was
  invented.
- The one-slot direct interaction is source-faithful: there is no invented container/menu. A
  right-click ejects the previous item and inserts one charged Storage Crystal when valid. Breaking
  the machine preserves that crystal, and an immediate full sync keeps insertion/ejection visible to
  clients.
- Storage behavior retains the exact source constants: 720,000 lumen per element, 32-block receive
  range, 6,000 throughput and a 200-tick request cadence normally; the researched enhanced structure
  raises those to 3,600,000, 48 and 30,000 and halves the live cadence. Each tick unloads at most four
  times throughput per stored colour and never exceeds either the crystal's remaining energy or the
  machine's free capacity.
- V33a's demand adaptation is restored: all sixteen drain counters decay to 95%, their average moves
  the cooldown one step between 100 and 200, and actual relay-network draw records RELAYS catch-up for
  nearby players. Cooldown, accumulated demand, enhancement state and energy survive disk NBT. The
  lightweight update payload also carries the inserted crystal's capacity and sixteen energy values,
  so its rendered prisms visibly shrink while unloading without shipping a full inventory packet
  every tick.
- `WeakRepeaterSafeReceiver` explicitly admits the source to the obtainable Lumen Repeater path.
  Enhanced activation requires both the exact structure and the owner's decoded `RELAYSTRUCT`
  fragment, and revalidates every twenty ticks as well as through receiver adjacency updates.
- `multiblock/relay_source` is generated from the exact 5x4x5 `BoostedRelayStructure`: the full smooth
  floor, four groove cells, four source Liquid Chroma cells and supports, empty upper footprint,
  Focus Frame centre, cardinal bricks and corner embossed stones. The source remains NBT-backed; the
  Java wrapper only supplies the `(2,3,2)` anchor.
- Rendering retains the exact 128x128 cuboids/UVs/pivots, separately blended fullbright edge bands,
  source-radius orbiting per-element storage prisms, and enhanced funnel/skirt. The latter uses
  `CAUSTICS_GENTLE` (`caustics-g`) under an additive-dark, depth-test/no-depth-write pipeline—the
  modern equivalent of V33a's `glDepthMask(false)` pass. Enhanced ambience restores both inward
  chroma droplets and the blended falling rapid-expand glow field.
- The exact MultiBlock casting recipe is data-driven: Crystal Focus centre; 24 surviving stand
  coordinates containing seven iron ingots, two Infused Dust, three Focal Powder, two Crystal Lenses,
  two glowstone and eight obsidian; Black/Blue/White/Yellow asymmetric runes; 100 ticks and 200 XP.
  The duplicated source assignment at `(0,-4)` correctly resolves to one stand rather than becoming
  a fictitious twenty-fifth ingredient.
- Guide resolution connects `RELAYSOURCE` and `RELAY` to the machine and its NBT preview. Generated
  blockstate/model/item, loot, language, mining-tag and recipe snapshots are present, and focused
  `chromaticraft:relay_source_survival_loop` coverage pins direct survival insertion, transfer math,
  adaptive cadence, wooden-repeater safety and the exact recipe contract.

**Validation boundary:** all touched generated JSON parses, the historical renderer texture's Git
blob hash is `2c453bacba83b84752730934cad902507f254b67`, and `git diff --check` reports no content errors.
The new Relay Source, Weak Repeater and Personal Charger binary structure snapshots still require the
next safe server datagen run. `:ChromatiCraft:compileJava` passes through the no-daemon, one-worker,
1-GiB-capped compile gate. No GameTest or datagen server was started because the concurrent Java
sessions still own the runtime resources; launching another server would repeat the workstation-RAM
failure mode. After datagen, run only `chromaticraft:relay_source_survival_loop`,
`chromaticraft:weak_repeater_survival_loop` and `chromaticraft:personal_charger_receiver_loop`.

**Next survival slice:** port the sixteen distinct Lumen Relay block identities, their orientation and
connection textures, `RelayNetworker`'s source-exact straight-line search, and the first practical
`TileEntityRelayPowered` consumer. That closes Relay Source -> relay conduit -> machine delivery; the
Relay Source is already a normal crystal-network receiver, but relay-block delivery must not be
claimed complete until that downstream loop is live.

## Proxima structure-password and outline-rune recovery (2026-08-24)

- The display shown when a puzzle Dimension Core is claimed is V33a's personal eight-rune structure
  password, not a Dimension Core inventory renderer. The initial 26.2 overlay incorrectly treated
  `runes/template/<colour>.png` as a 16x16 GUI icon. Those files are 16x1024 animated block-template
  strips, which is why each coloured password frame contained repeated dark scanlines.
- `StructureNotificationOverlay` now resolves the exact sprites V33a registered as
  `CrystalElement.getOutlineRune`: `runes/outline/tile<element ordinal>_0.png`. It also restores the
  source's two-times scale, 48-pixel cadence, progressive five-tick reveal, per-glyph luminance pulse,
  three colour-matched frames, final frame fade, seven-second lifetime and C0101010-to-D0101010
  backdrop gradient.
- `CrystalRuneTextures` centralizes that static-glyph mapping so an animated block strip cannot be
  substituted accidentally again. The same mapping restores the omitted eight-pixel outline runes
  around V33a's Elemental Manipulator buffer wheel, at `0.8125*r` and the source's fixed wedge-centre
  angles, below the authored front plate.
- The lexicon ability-ritual page now centres the same outline glyphs in the actual proportional
  energy segments at `0.625*r`, using V33a's segment angle accumulation rather than assuming the
  wheel always contains all sixteen colours.
- `:ChromatiCraft:compileJava` passes. No GameTest was run because this slice changes only submitted
  client GUI/HUD render state; it requires visual verification when another structure core is
  claimed and when the manipulator/ritual-energy wheels are opened.

## Burrow annex/key audit and missing personal-key guide pages (2026-08-24)

- The Overworld Burrow was already NBT-backed by the exact base, furnace and cache templates. Its
  optional source sequence is now explicit: natural generation retains V33a's one-half furnace-room
  roll followed by the conditional one-half cache-room roll, while the command inspection form
  (`/place feature chromaticraft:burrow`) deliberately includes both annexes and waives surrounding
  host-terrain validation. This makes the complete implementation inspectable without altering the
  natural rarity.
- The furnace annex retains two south-facing furnaces with weighted ore inputs and two Heat Lamps
  independently configured from 50 through 160 C inclusive. The cache annex retains the connected
  2x2 Chroma Door, a freshly generated UUID, the hidden ordinary chest, the Ethereal Key carrying
  that exact UUID, and both separated block/item cache chests. All six base Burrow loot chests and
  both cache chests now award `BURROW`, correcting a port fallback which incorrectly awarded
  `CAVERN`.
- The complete inspection form is also a native command-only Minecraft Structure now:
  `/place structure chromaticraft:burrow`. Its single piece is bounded to the union of the three
  canonical templates and deliberately centred so all 11x12x11 cells remain in one chunk. It reuses
  the same feature callback authority for lamp temperatures, furnace inputs, loot, door UUID and key
  rather than maintaining a second implementation. No structure set points at this entry; natural
  frequency, host checks and optional-annex rolls remain on `natural_burrow`. The older
  `/place feature chromaticraft:burrow` inspection alias remains available.
- Focused `chromaticraft:structure_burrow_nbt_controller` coverage now proves both annex flags, all
  eight Loot Chests, both furnace/Heat Lamp configurations, the connected door, and the exact hidden
  key-to-door UUID round trip. It passes 1/1 in 3.615 s. The vanilla library roll can still fill the
  controller before its post-roll fragment merge; V33a's `addToIInv` also returned false in this
  situation, so the port logs it but does not invent a reserved slot.
- V33a's Personalized Casting second guide subpage is restored on its original
  `handbook_casttune` frame. It renders the source's square 63/47-radius annular compass, the twelve
  non-cardinal player-specific coloured sectors and outline runes, casting-table top, eight-rune UUID
  encoding, 4x4 personalized tuning emblem, and the player's profile head overlay. Client lookups now
  supply game type to the tuning seed and cache by `(UUID, game type)`, preventing a client survival
  preview from poisoning an integrated creative server's authoritative key.
- V33a's Structure Keys second guide subpage is restored on `handbook_password`. The exact
  `dimensionstructures.png` sheet was recovered byte-for-byte from the V33a jar. Assignments are
  recomputed from the synchronized client dimension seed, retain the player-hash shuffle and unknown
  icon for incomplete colours, draw completed colour frames, and reveal the personal eight-glow-rune
  password only while its completed icon is hovered.
- `:ChromatiCraft:compileJava` passes with only the existing deprecated mock-player warnings. The
  selector-scoped `chromaticraft:casting_table_tuning_key` authority check also passes 1/1 in
  4.514 s. The guide additions themselves are submitted client render state and therefore require
  in-world visual acceptance; no broad GameTest suite was launched.

**Native Burrow validation:** the new structure type compiled successfully before server datagen;
server datagen then emitted `worldgen/structure/burrow.json` and completed all providers. The
pre-wrapper Burrow behavior test passes 1/1 in 3.615 s. That focused test has since been upgraded to
enter through `BurrowCommandPiece`; its only 26.2 compile issue (`ChunkPos(BlockPos)` no longer
exists) was corrected to explicit chunk coordinates. The rerun is now blocked by the concurrently
admitted, still-pristine V33a `RelayNetworker` and its 37 expected legacy-API errors. This is
unrelated to the Burrow change and was neither gutted nor re-excluded merely to recover a green
build. Static JSON/template validation and focused `git diff --check` pass; rerun only
`chromaticraft:structure_burrow_nbt_controller` once the relay-network slice compiles again.

## Relay transport visuals and consumer dependencies (2026-09-12)

- RelayNetworker now sends the discovered source-to-consumer path instead of only logging it.
  A bounded, immutable typed payload carries the element and up to 64 endpoints. Each nearby
  player receives it once even when within range of several endpoints. The client creates the
  V33a segment particles using the real bigflare, rotating-flare-pulse, blurflare2 and flare7
  sprites, original scale range, velocity, drag and termination conditions. Finding a source
  does not consume its lumen: the requesting consumer owns that transfer.
- The Area Delegation Point (Function Relay) is registered as `chromaticraft:function_relay`.
  Its first-tick/fifty-tick cache scans x/z +/-6, y -6 through +2, clipped to horizontal
  Manhattan distance nine. Random delegation samples non-air cached coordinates. Queries do
  not force-load missing chunks. Its bookshelf integration sums the separate 11x3x11 range,
  excludes other Function Relays to prevent recursion, and is wired into NeoForge's actual
  enchanting-power hook. It retains a full-block selection box and no physical collision.
- Function Relay world and special-item renderers use Reika's real animated cellflare sprite,
  the four original colours at a 25-tick cadence, camera-facing 0.875 world scale and authored
  inventory tilt/scale. The submit pipeline disables culling and depth writes; its expanded
  render bounds include the full flare. Negative identity-hash offsets are wrapped safely.
  The original spherical particle experiment was disabled in V33a and is not enabled here.
- Server/client datagen emitted its real temple casting recipe (Transmissive Dust above/below,
  Aura Dust left/right, Glowstone centre; gray/yellow/lime runes at the exact source offsets),
  twenty-tick duration, forty XP, loot, mining tag, item/block models and language entry.
  The existing creative block-item population includes it, and the lexicon resolves its icon.
- Function Relay effect descriptions retain all six original associations. Descriptions of
  not-yet-registered consumers retain explicit deferred item keys, without supplying fake
  stacks. Harvest Plant, Crop Speed/Reverter and Cobble Generator must connect their operational
  calls and final item IDs as those consumers land; the bookshelf and Farmer callers are live.
- DragonAPI's modern CropType/ReikaCropHelper cover the original five vanilla crop families:
  wheat, carrots, potatoes, nether wart and cocoa. They use native age properties and actual
  server-side vanilla loot with Fortune, preserve cocoa's attachment direction when resetting
  age, and implement the original one-seed reservation. No replacement recipes or loot tables
  are invented.
- Validation: the active source slice compiles; both data providers complete successfully.
  `chromaticraft:relay_path_routing` passes 1/1 in 4.096 s, covering bends, colour rejection,
  solid obstruction, empty sources, loops and payload round-trip ordering.
  `chromaticraft:relay_consumer_dependencies` passes 1/1 in 3.860 s, covering scan boundaries,
  exact refresh cadence, random selection, non-recursive enchanting power, collision, all five
  crops, real crop loot and seed reservation. These were separate, serial focused runs, never
  the full suite. Gradle/compiler/runtime worker counts and memory were bounded using the local
  validation init script. Client visuals still need in-world acceptance.
- RelayPowered and Farmer were completed in the follow-up slice documented below. The full
  adjacency-upgrade block family remains pristine 1.7.10 and is not claimed playable.

## Tiered-plant visibility, Ethereal Luma and Luminous Cliffs boundaries (2026-09-12)

- Tiered plants still make their V33a per-viewer progression decision in the dynamic chunk model,
  but the client now tracks the local player's seven relevant stages and creative state. It asks
  the 26.2 `LevelExtractor` to invalidate compiled geometry only when that visibility signature
  changes. Progress grants and creative/survival switches therefore hide or reveal Glowing Roots
  and the six related plants immediately, without waiting for a neighbouring block update and
  without continuously rebuilding chunk meshes.
- Ethereal Luma now uses the same water-material entity travel that V33a's `BlockFluidClassic`
  inherited. Its viscosity 50 continues to control fluid spreading, while breathable Luma keeps
  `canDrown(false)`; the removed nearly-air movement path applied full gravity after each custom-
  fluid jump and cancelled the upward impulse, trapping players in the fluid.
- Luminous Cliffs now owns a dedicated TerraBlender region weighted by the existing
  `CLIFFWEIGHT` option. V33a inserted it as a complete warm/cool biome; the previous port instead
  replaced only rare Windswept climate cells inside the Rainbow Forest region, producing tiny,
  repeatedly fragmented patches. Ordinary surface land targets in the dedicated region now map
  to the main biome, with beaches, shores and rivers mapped to its shore child.
- The cliff terrain feature is attached globally to overworld generation, matching V33a's global
  column shaper. It still writes only actual Luminous Cliffs columns, but no longer depends on one
  chunk-origin biome sample. A cached quart-biome mask and linear-time 3/4 chamfer distance field
  provide the original 24-block inward transition: the middle shelf interpolates from existing
  terrain and the floating upper shelf gains thickness inward, removing chunk-square walls and
  clipped slabs without a radial per-column performance cost. Existing generated chunks are not
  rewritten; visual acceptance requires fresh terrain.
- Validation: the active source slice compiles. Serial, memory-capped server datagen completed all
  providers in 1.060 s, emitted `luminous_cliffs_terrain_overworld.json`, and removed the duplicate
  direct feature reference from both cliff biome JSON files. No full GameTest suite was started,
  and process checks confirmed no Java process remained after either validation command.

## Elemental Harvester and tinted lenses (2026-09-12)

- The V33a Farmer is registered as `chromaticraft:farmer` with its block entity, horizontal
  placement, eleven-part Techne model, original `textures/entity/farmer.png`, world renderer and
  special item renderer. Its server tick keeps the weighted sixteen-block forward wedge, submerged
  five-block restriction, redstone disable, Function Relay delegation, green-energy work rate,
  purple-energy Fortune, cactus/reed top harvesting, crop reset, one-seed reservation below
  Fortune III, break sound, item drops and travelling green harvest particle.
- DragonAPI's crop discovery now accepts explicit modern crop registrations and automatically
  adapts non-vanilla `CropBlock` and `SweetBerryBushBlock` instances. It preserves each crop's real
  age property, non-age state properties, clone-stack seed, native loot table and Fortune context;
  RotaryCraft Canola therefore works without a hard dependency or fabricated crop table.
- V33a's metadata-based tinted Crystal Lens family is represented by sixteen distinct registered
  items. Their sprites are exact 16-by-16 crops from V33a `items_color.png` starting at index 144.
  Each colour has the original three Multiblock Casting variants: an ordinary Crystal Lens in the
  centre, two Focal Powder and two matching shards, with four iron/gold/Chroma Alloy stands yielding
  one/two/four lenses respectively and the original 48-craft repeat penalty threshold.
- The Farmer's Pylon Casting recipe is datagen-backed and preserves all sixteen auxiliary stands:
  eight Aura Dust, one green tinted lens, one iron hoe and six iron ingots around an Energy Core.
  It yields three Farmers, requires 12,000 green and 6,000 yellow aura, runs for 400 ticks, grants
  500 XP and applies the original six-craft repeat penalty threshold.
- Relay-powered consumers retain V33a's eight black-adjacency efficiency factors through the modern
  `AdjacencyUpgradeProvider` contract. The future adjacency-upgrade tiles can provide colour, tier
  and active state directly; the Farmer does not pretend the still-unported adjacency items exist.
- Validation: the active source slice compiles. Separate serial, memory-capped client and server
  datagen runs completed successfully, emitting the Farmer model/item/lang/loot data, sixteen lens
  item definitions and models, and all forty-eight lens recipes. Structural validation confirmed
  the Farmer's output, aura totals and sixteen stand entries. No GameTest server or second Java
  process was started.

## Vibrant Pod texture sampling (2026-09-13)

- Fixed `TieredPlantPodModel` passing legacy 0..16 texture offsets to 26.2's normalized
  `TextureAtlasSprite.getU/getV` API. Both the backing and glow now sample the intended pod sprite;
  expanded glow vertices clamp their UVs to 0..1 without changing their geometry.
- Quad baking now derives transparency from the material instead of forcing `Transparency.NONE`.
  The original 16x512 animated front texture contains transparent and partially transparent pixels,
  so its sparkle overlay requires the translucent layer; the 16x16 backing remains opaque.
- The existing datagen provider and generated blockstate/item models already select V33a's
  `tierplant_5_back/front` assets. No asset substitution or generated-resource edit was required.
- Validation: `:ChromatiCraft:compileJava` passed with all dependency compile tasks included, one
  worker and parallel execution disabled. Source/API and PNG alpha checks confirm the cause and
  material selection. In-world appearance still needs a client restart with the updated classes.

## Enrichment Vine and Scissorweed (2026-09-13)

- V33a's DECOPLANT metadata 3 and 5 are now independent registered blocks and items:
  `chromaticraft:plant_accelerator` (Enrichment Vine) and `chromaticraft:harvest_plant`
  (Scissorweed). Their block entities share the modern `TileEntityMagicPlant` base, and functional
  plants count a consecutive column of active Enrichment Vines opposite their growth direction.
- Enrichment Vine preserves its source anchoring rules: a connected vertical vine column survives
  when either end reaches leaves or a solid/collidable block. A vine surrounded on all four sides
  gains its full collision and V33a encased appearance; plants above that form inherit the extra
  crop-style planes.
- Scissorweed preserves the exact 11-by-11 horizontal and four-level vertical weighted selections,
  first-tick/twenty-tick foliage cache, bright daytime and sky/redstone gates, one attempt plus one
  per Enrichment Vine, Function Relay redirection, Manhattan exclusion, same-state neighbour test,
  adjacent-Scissorweed veto and shears-context native loot. Successful cuts retain source drops,
  grass break sound and block-break particles. Contact inflicts one cactus damage and twenty ticks
  of Hunger II, and the client emits its yellow dust.
- Models use Reika's original `decoplant_3_*`, `decoplant_5_*` and `vine_encased_back` textures.
  The ordinary and crop forms preserve both backing and overlay at world light; unlike the tiered
  plant renderer, V33a's `DecoPlantRenderer` did not make the overlay fullbright. Item models retain
  both authored layers rather than flattening or substituting either sprite.
- Server datagen emits the exact non-Botania V33a Temple recipes, rune offsets, durations, XP and
  output counts. The optional Botania recipe branch remains deferred until its modern concrete
  special-flower and mana-resource item forms are available; no replacement ingredient is
  invented.
- Validation: the active source slice compiled successfully. Separate serial, single-worker,
  two-gigabyte-capped client and server datagen runs completed successfully and emitted the two
  blockstates, ordinary/crop block models, two-layer item models, loot tables and casting recipes.
  No GameTest server or concurrent Gradle process was started.

## Ability ownership and ritual altar geometry (2026-09-15)

- V33a stores each ability's ownership and enabled status in one `chromabilities/<id>` boolean:
  the key's presence grants ownership, while its value controls whether the effect is on. The
  modern persistence layer now implements this distinction for all registered native abilities,
  including grant-off, toggle, remove, available-list, state-map, server-player synchronization and
  non-death player-clone copying. These are real data operations, not claims that the thirty-nine
  active effects or the ritual table have landed.
- The eleven-by-five-by-eleven V33a ritual altar has base and enhanced canonical NBT templates.
  Both preserve the soft-clearance volume, two stone floors, beam and column tiers, engraved and
  embossed corners, eight-block Chroma ring and the removed foundation centre. The enhanced
  template substitutes the original glowing perimeter beams and four glowing columns. The table
  socket is `structure_void` because the machine block is placed by the player, as in the other
  machine multiblock templates; outer-floor rune alternatives still belong to the runtime matcher.
- Serial two-gigabyte-capped compilation and server datagen passed and emitted both templates.
  The altar matcher, ritual table block entity, energy transfer, 980-tick ceremony and the ability
  behavior engine remain unported; no full or concurrent GameTest suite was launched.

The next altar dependency slice admits the NBT-backed `RitualStructure` matcher and both base and
enhanced registry identities. It restores the thirty-six outer-floor cells that may be either smooth
stone or any of the sixteen distinct rune blocks, plus V33a's optional glowing beams and columns
when the table permits enhancement. The table socket forward-references the actual ritual-table
block identity; no unrelated machine is substituted. `RitualAPI` now carries both external cost
registration and a loaded-chunk-safe query of active tables, and `AbilityAPI` registers custom
ability IDs with V33a's invalid/reserved/duplicate checks. The current serial compilation is red at
the intentionally unported `TileEntityRitualTable` and `ChromaBlocks.RITUAL_TABLE` dependencies;
the table machine and its thirty-nine effects are not claimed complete.

## Reversion Lotus and Fertility Bloom (2026-09-13)

- V33a's DECOPLANT metadata 1 and 4 are now independent registered blocks and items:
  `chromaticraft:biome_reverter` (Reversion Lotus) and `chromaticraft:crop_speed_plant`
  (Fertility Bloom). Both use the modern magic-plant base, preserve their original upward
  Enrichment Vine chaining, and redirect selected work once through a Function Relay.
- Reversion Lotus preserves the source 9-by-9 weighted field, forty-tick timer, acceleration-vine
  multiplier and occasional doubled reach. It compares the live biome to the chunk generator's
  natural noise-biome result, then restores the complete modern quart column and resends biome
  data through the public fill-biome path. Modern biome storage cannot represent V33a's single
  block columns more finely than a 4-by-4 quart cell. Its source enchant particles, light level,
  regeneration-on-contact effect and flower-compatible planting rules are retained.
- Fertility Bloom preserves both source 5-by-5 weighted fields. It hydrates farmland on the
  original one-in-four cadence and spends `0.5 + vines/2` weighted growth operations per tick on
  saplings, decorative flowers, reeds, cactus, vines, vanilla crops and registered mod crops by
  invoking their real server random tick. Its light-blue/red particle colours, light level,
  farmland/Enrichment Vine/Cliff Farmland planting rules and ComplexAOE weights are retained.
- Client datagen uses Reika's original `decoplant_1_*` and `decoplant_4_*` backing/overlay sprites
  in ordinary and crop forms, with ordinary world lighting exactly as V33a's DecoPlant renderer.
  Server datagen emits the exact Temple recipes, rune positions, output counts, durations and XP.
- Validation: the active source slice compiled successfully. Separate serial, single-worker,
  two-gigabyte-capped client and server datagen runs completed successfully and emitted both
  blockstates, ordinary/crop block models, layered item models, loot tables and casting recipes.
  No GameTest server or concurrent Gradle process was started.

## Heat Lily and Coalescence Orchid (2026-09-14)

- V33a's remaining DECOPLANT metadata 0 and 2 are now independent registered blocks and items:
  `chromaticraft:heat_lily` and `chromaticraft:cobble_generator` (Coalescence Orchid). Heat Lily
  retains its upward growth direction, lily-pad support rules, level-15 light, flame particles,
  two-second contact ignition and forty-tick random melt attempt over the source seven-by-seven
  area one block below. Its live-location cache suppresses natural freezing within the original
  Manhattan distance seven.
- DragonAPI's `IceFreezeEvent` is connected to the 26.2 biome freeze decision through a guarded
  mixin rather than remaining a dead compatibility shell. The modern handler preserves V33a's
  Heat Lily, Luminous Cliffs/Shores and Proxima no-freeze rules without recursively reposting the
  event or forcing unloaded chunks.
- Coalescence Orchid retains its downward growth, ceiling/Enrichment Vine support, nine-by-nine
  horizontal and five-block downward search, full hundred-tick rescan plus two random columns per
  tick, and Function Relay endpoint delegation. It selects recipes in datapack order, validates
  both source fluids and required amounts throughout the operation, begins at one-third progress,
  probabilistically drains the authored quantities, and emits up to 64 output items according to
  the consecutive Enrichment Vine count.
- The four built-in V33a fluid pairs are data-generated as a real custom recipe type: water/lava
  to cobblestone, Chroma/lava to smooth crystalline stone, Luma/lava to cliff stone, and Ender/lava
  to end stone, with their exact durations and independent consumption chances. A Heat Lily one
  through four blocks below applies the original cobblestone-to-stone modifier. Produced entities
  retain the source 300-tick lifetime.
- Client effects preserve the Orchid's fluid-attractor animation using the actual source-fluid
  model sprite, the Heat Lily modifier's three orange tri-dot columns, and the 72-ray coloured
  success/failure burst delivered by a synced block event. Client and server datagen emit the
  original two-layer plant models, names, loot tables, exact Temple recipes, rune positions,
  output counts, durations and XP; no substitute assets or recipes were introduced.
- Validation: the complete active source slice compiles successfully. Separate serial,
  single-worker, two-gigabyte-capped client and server datagen runs completed successfully and
  emitted both plant resource families, both casting recipes and all four fluid-mixing recipes.
  No GameTest server or concurrent Gradle process was started.
