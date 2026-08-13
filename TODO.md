# ChromatiCraft — next steps

Goal: **game start → working casting stands**. The mechanical chain is unbroken as of 2026-07-31
(see `PORTING.md` → "The tier-2 casting dependency chain"). What's left, roughly in order of value.

## 0. From in-game testing 2026-07-31 — triage

**Fixed:**
- [x] Manipulator right-click didn't start a craft — `BlockCastingTable.useItemOn` returned SUCCESS
      unconditionally, and vanilla only falls through to the held item's `useOn` on PASS, so the
      GUI-open swallowed the click. Returns PASS for a Manipulator now.
- [x] Progression was completely silent — `setPlayerStage` never notified. Ported V33a's
      PROGRESSNOTE packet + `ChromaSounds.GAINPROGRESS` at 0.5 volume with the 24-tick cooldown.
      The on-screen *note* is still deferred: its text comes from `progression.xml`, part of the
      unported XML pipeline the guide book needs.
- [x] Casting-stand inventory icon too high — the item renderer reused the in-world BER anchor,
      putting the model at y 0.875..1.595, above the unit cube. Bottom is now 0.14.
- [x] Cliff grass white on top — `grass_top_base.png` is greyscale (avg 143,143,143), like vanilla's
      grass top, and expects a biome tint; the model had no `tintindex` and no tint source was
      registered. Now a hand-built cube with `tintindex 0` on the up face plus vanilla's grassBlock
      tint source, wrapped in `LuminousCliffsColors`.

**Diagnosed, not yet fixed:**
- [x] ~~Crystalline stone beam Y variant.~~ Done 2026-07-31. The invented `AXIS` blockstate property
      is gone; `PylonStructureModel` reproduces V33a's `getIconIndex` neighbour rule instead —
      same-type neighbour on X takes `block_1-3`, on Z `block_1-2`, otherwise `block_1`, X checked
      first. Only beams ever consulted the axis, so the other fourteen types keep ordinary generated
      models; the blockstate is hand-authored (48 variants down to 16) because a custom model type
      cannot come out of `MultiVariantGenerator`.
- [x] ~~**Resonance ring and corner neighbour rules.**~~ Done 2026-07-31. `PylonStructureModel` now
      reproduces all of V33a's `getIconIndex` and selects **per face**, which the earlier whole-cube
      swap could not: the ring's rule differs between top/bottom and sides, and corners pick a
      four-way rotation per face. Rings use the X/Z pair first, then any vertical neighbour, then
      V33a's isolated-but-touching walk; corners match on the block alone, not the type. Icon indices
      come from V33a's `variants[]` table (BEAM/GLOWBEAM 3, CORNER 4, RESORING 2, doubled when
      glowing, with the glow overlay at `variants/2 + index`), and `getWrappedMeta` is honoured so a
      glow beam joins an ordinary one on top/bottom.

**Fixed with the client log (2026-07-31):**
- [x] ~~"No texture" on the glowing crystalline-stone types, item *and* placed block.~~ The log named
      it: `Cannot compute translucency out of bounds: [-1, -1, 17, 17]` from
      `SpriteContents.computeTransparency`, via `FaceBakery.computeMaterialTransparency`.

      The glow element is deliberately inflated to `-0.002 .. 16.002` to beat z-fighting with the
      base cube. When a face carries no explicit `uv`, the bakery *derives* one from the element
      bounds — here `[-1,-1,17,17]` — and `computeTransparency` rejects it, which aborts the whole
      model bake. Hence missing-texture in world and inventory, and only for the six `glows()` types,
      since they are the only ones with a second inflated element.

      Fix: emit `"uv": [0,0,16,16]` on every glow face. A sweep confirms no other generated model has
      an out-of-0..16 element without explicit UVs.

      Worth remembering: this parses fine and every static check passes — element bounds are legal
      (-16..32), textures exist, mcmeta is valid. It only fails at *bake*, so static inspection can
      never find it. Go to the client log first for render bugs of this shape.

## 0b. Renderers still needed (one coherent chunk)

All of these are V33a BER/ISBRH geometry that the port currently substitutes with a plain cube or a
flat icon. Best done as a single pass over the crystal family:

- [x] ~~cave crystals — **item** renderer~~ — done 2026-07-31. Geometry extracted to
      `render/model/CaveCrystalGeometry` and emitted through a `QuadSink`, so the block model bakes
      it into the chunk mesh and `render/item/CaveCrystalItemRenderer` writes the same quads to a
      `VertexConsumer` — the two cannot drift. Needs a `SpecialModelRenderer` because the shapes are
      arbitrary four-point polygons with hand-authored UVs (V33a immediate mode), not box elements.
- [x] ~~**crystal lamps / potion crystals**~~ — done 2026-07-31. The shared crystal model now takes
      an optional `base_texture`, so lamps and potion crystals draw the cave crystal's spikes plus
      V33a's stone plinth instead of the placeholder coloured cube; the item side goes through the
      same special renderer with the plinth. `below` is forced false whenever a base is drawn, which
      is V33a's `!renderBase() && blockBelow instanceof CrystalBlock`. The plinth is smooth stone
      because `renderBase` queries `getBaseBlock(..., UP)` for the side faces too, not just top and
      bottom. 32 hand-authored blockstates, and both blocks excluded from the datagen completeness
      check so nothing regenerates over them.
- [ ] **power crystal / focus crystals** — these are *not* CrystalRenderer blocks: they are block
      entities with their own V33a TESRs (`RenderFocusCrystal`, and the power crystal's own), so they
      need BER ports rather than a share of the crystal geometry. Currently plain cubes.
- [ ] **retest whether looking at a cave crystal grants the stage** — it now plays a sound, so this
      is answerable directly.
- [ ] casting-table GUI: confirm the recipe-ready feedback shows. The plumbing looks correct —
      `writeSyncTag` sends `recipeOutput`, the screen draws a ghost output plus a `NO_ENTRY` overlay
      when `!canRunDisplayedRecipe()` — so retest now that the Manipulator works; if the ghost never
      appears, the recipe is not matching rather than the highlight being absent.

## 0c-bis. Pylon energy / casting — diagnosis 2026-07-31

Ruled out, with evidence, so nobody re-walks these:

- **Network tick is live.** `CrystalNetworker.tick` is registered via `TickRegistry` as
  `TickType.SERVER` + `canFire(START)`, and DragonAPI's `TickRegistry` is genuinely wired to
  `ServerTickEvent.Pre` — it is not a dead stub. Flows are processed.
- **Tiles register.** `TileEntityCrystalBase` calls `cachePosition()` from both `onFirstTick` and
  server-side `onLoad`.
- **The whole `magic/network` package is compiled** (wildcard include, line 210) and contains no
  `CHROMA-PORT` deferrals.
- **The structure templates are current.** All the `multiblock/*.nbt` files carry the post-split
  block ids (`crystalline_stone`, `crystalline_stone_beam`, ...), not the old `pylon_structure`.
  The remaining `ChromaBlocks.PYLONSTRUCT` references are all in files outside the allowlist.

Live lead, not yet confirmed in game: `TileEntityCrystalPylon.canConduct()` requires
`hasMultiblock`, which is set only by `validateMultiblock` matching `ChromaStructures.PYLON`.
`NBTStructureLoader` feeds `FilledBlockArray.setBlock` the **full BlockState**, so matching includes
`crystalline_stone_beam[axis=...]`. The pylon palette contains only `axis=x` beams. A worldgen pylon
is placed from that same template so it should self-match — but a **hand-built** pylon will only
validate if every beam ends up on the same axis the template recorded, and placement derives the
axis from the clicked face. Worth checking against V33a, which encoded beam direction in metadata
(1 = z, 2 = x) and so did constrain orientation too — meaning this may be faithful rather than a
regression.

Next concrete step: in game, stand at a pylon and check whether `hasMultiblock` is true (the pylon
renders its aura only when it is). That splits "structure never validates" from "flow never starts".

## 0d-bis. Manipulator craft-progress HUD — scoped 2026-08-01

Traced in V33a. This is **not** part of the big `ChromaOverlays` dispatcher work and does not need
`PlayerElementBuffer` or the ability system — it is a small, self-contained slice:

`ChromaOverlays` line ~163, on `ElementType.HELMET`:
```java
boolean manip = HoldingChecks.MANIPULATOR.isClientHolding();
if (manip) MouseoverOverlayRenderer.instance.renderTileOverlays(ep, gsc);
```
`renderTileOverlays` ray-traces 4 blocks, resolves `TileEntityDummyAux` to its linked tile, then for
`te instanceof OperationInterval` calls `renderStatusOverlay`, which draws a state icon plus the
progress arc from `Textures/infoicons.png` near the crosshair (`ar = 12`, offset from
`displayWidth/(gsc*2)`). It also renders a storage bar for `LumenTile` and an acceleration overlay
for `FocusAcceleratable`.

To port, in order:
1. `Auxiliary/Interfaces/OperationInterval` + its `OperationState` enum.
2. Implement it on `TileEntityCastingTable` (it already tracks `craftingTick` and the recipe
   duration, so the fraction is available).
3. A client `HoldingChecks.MANIPULATOR` equivalent.
4. `MouseoverOverlayRenderer.renderStatusOverlay` on a 26.2 HUD layer.

Note the earlier TODO entry saying "V33a has no craft progress bar" was about the casting **GUI** —
correct there, but the bar does exist as this world overlay.

## 0d. HUD — findings 2026-07-31

**Casting-table GUI: already at V33a parity.** Checked against
`5cde0068^:GUI/Tile/Inventory/GuiCastingTable.java`. V33a draws the tier diamonds with NO_ENTRY
overlays for unmet tiers, the per-element aura fill bars, and the output stack — and the port's
`ScreenCastingTable` already does all three. Two things V33a has that the port does not, both blocked:

- the "recipe missing progress" hint (`ChromaBookData.drawRecipeMissingProgress`), which needs the
  research/book subsystem;
- nothing else. **V33a has no craft progress bar here** — worth knowing before adding one.

The output *is* drawn solid in V33a via `drawItemStack`, not faded. So an empty output slot means the
recipe is not matching, which is the same root cause as "casting does not work" — not a render bug.

**World HUD (`ChromaOverlays`, 1056 lines): blocked, nothing portable yet.** It is a dispatcher over
~15 overlays — element pie, ability status, boosted health bar, kill-aura crosshair, pylon aura,
lore hexes, probe info, ping, transition/ore HUDs, progression notes. Every dependency is still
pristine 1.7.10: `PlayerElementBuffer`, `Chromabilities`, `ProgressOverlayRenderer`,
`FullScreenOverlayRenderer`, `MouseoverOverlayRenderer`.

Natural first slice, and the one players actually recognise as "the HUD":
`PlayerElementBuffer` (299 lines — player-attached elemental storage, so NBT via data attachments
plus client sync) then `ChromaOverlays.renderElementPie` (~line 816), which depends on essentially
nothing else beyond it and `CrystalElement`. Everything else in the HUD hangs off the ability system
and should wait for it.

## 0c. Known flaky test

Two now seen flaking, both timing-sensitive, both passing on re-run. Worth hardening before they
waste someone's afternoon:

- [ ] `chromaticraft:network_compound_repeater_multicolor` — "tick 6 attenuation"
- [ ] `chromaticraft:pylon_enclosure_rejection` — "on tick 0"

## 1. Verify the chain in a client — nothing below matters if this is broken

Everything so far is verified headlessly (62 GameTests + datagen). **Never runtime-verified in a
client.** Walk it once and note what breaks:

- [ ] look at a cave crystal → CRYSTALS granted
- [ ] mine crystals → shards drop, counts look like V33a (avg ~4.5 at Fortune 0)
- [ ] craft Casting Table + Elemental Manipulator
- [ ] table GUI opens; Manipulator right-click actually starts a craft
- [ ] cast Item Stands; stands place, render and hold items
- [ ] look at a conducting pylon → PYLON granted, colour discovered
- [ ] build the CASTING1 temple, place a rune → RUNEUSE granted, tier-2 recipes become available

## 2. Ungrantable stages still in/near the early arc

Same class of bug as PYLON/RUNEUSE — the stage exists and is gated on, but nothing grants it.
Confirmed to have **no live grant site** outside GameTests:

- [ ] **`VILLAGECASTING`** — a prerequisite of MULTIBLOCK, so tier-3 casting is blocked the way
      tier-2 was. Grant site located: `World/VillagersFailChromatiCraft.java` (lines 799 and 1122)
      — the village-generated casting structure. Depends on that village worldgen landing.
- [x] ~~**`DYETREE`**~~ — done 2026-07-31. V33a grants it in `removedByPlayer`; ported as
      `playerWillDestroy` on `block/dye26/BlockDyeLeaf`, creative-mode-guarded (V33a's `willHarvest`).
- [x] ~~**Dye-leaf drop table**~~ — done 2026-07-31. Full V33a `BlockDyeLeaf.getDrops`: shears/silk
      yield the leaf and nothing else (`onSheared`), otherwise five independent rolls and the leaf
      never drops — sapling `0.05*(1+f)`, apple `0.005*(1+f*5)`, vanilla dye `0.1*(1+f)`, rainbow
      sapling `0.0001*(1+f)^2`, and berries at `0.1*2^f` with the overflow-to-count loop. Needed two
      new registered loot types, `FortuneScaledChance` (linear/quadratic/exponential) and
      `ChromaBerryCount`. The dye branch turned out **not** to be blocked: V33a's
      `getVanillaDyeChance` defaults to 100 and `doWithChance(>=100)` always succeeds, so the vanilla
      dye is the only outcome on default config; the unported ItemCrystalDye branch is unreachable.
- [ ] **`MAKECHROMA`** — grant site located: `TileEntity/Acquisition/TileEntityCollector.java:123`
      (the Chroma Collector). Blocked on that machine being ported.
- [ ] **`ALLOY`** — grant site located: `Auxiliary/RecipeManagers/PoolRecipes.java:182`. Blocked on
      the chroma-pool recipe system, which is also what item 3 below needs — do them together.

## 3. Boosted shards / the chroma pool

`SHARDCHARGE` is granted from `ItemCrystalShard`, but confirm the whole loop actually works:
can a player make a chroma pool, charge shards in it, and get boosted shards? Boosted shards are
required by `EnhancedRuneRecipe` and every `crystal_group` recipe.

## 4. Guide book — the big one

Without it there is no in-game explanation of any of the above; a player has to already know the
structure layouts. ~8,000 lines, a subsystem in its own right. Current slicing:

- [x] recover and load the shipped V33a XML resources without replacement prose
- [x] preserve the complete 322-entry `ChromaResearch` identity/order/tier catalog
- [x] register the Chromic Lexicon and Information Fragment, including the exact book recipe,
      research ownership, fragment decoding, recovery and stored-page transfer
- [x] port the navigation, description, progress, recovery and notebook screens
- [x] port the casting-table specialist pages with server-authoritative grid/rune/stand/aura data
- [ ] finish specialist machine, tool, ability and structure pages and complete modern icon bindings
- [ ] port the Memory Crystal inscription/entity/rendering pages and the later lore puzzle/Rosetta
      presentation

## 5. Carried over — owner decisions outstanding

From `ISSUES_2026-07-30.md`:

- [ ] **dynamic teal/pink/clear water** — hard-blocked without a mixin (`FluidRenderer` picks the
      `FluidModel` per `FluidState`, never per position; vanilla water's model can't be replaced)
- [ ] **Luma 4×4 mosaic** — reachable, but needs sign-off: a position-aware block model costs Luma
      the fluid renderer's surface-height and flow animation

## 6. Luminous Cliffs parity, still open

Island rivers/lakes/ores/underground trees and remaining decorations; surface blending and farmland
semantics; the remaining Rainbow Forest decorator work.

## 7. Overworld worldgen sweep — goal and remaining generators

**Goal:** every one of V33a's thirteen overworld `IWG` generators is either *registered and observed
generating in a censused world*, or *recorded here with a named blocker*. No generator sits in the
tree half-ported, and none is called done on wiring alone.

"Observed generating" is the bar deliberately, because this sweep has already produced three defects
that compiled, datagen'd and reviewed clean and still generated nothing: the tiered-ore band sitting
in deepslate, the tiered-plant cave scan repeating that same mistake, and V33a's literal
`== Blocks.air` test failing once plants moved after vegetation. Reading the code did not catch any
of them; censusing a pregenerated world caught all three.

### Status

| Generator | State |
|---|---|
| `CrystalGenerator` (cave crystals) | done — all 16 colours, ~1.6-2.0% of chunks each |
| `ColorTreeGenerator` (dye trees) | done — all 16 colours |
| `LumaGenerator`, `GlowingCliffsAuxGenerator` | done — Luminous Cliffs vertical |
| `PylonGenerator` | generating, but see the acceptance-rate note in PORTING.md |
| `TieredWorldGenerator` — ores | done — 3 of 15 ores, ~26-28% of chunks |
| `TieredWorldGenerator` — plants | done — 5 of 7 plants; Essence Lily/Radiance Bush need a GameTest |
| `CaveIndicatorGenerator` | **wiring-verified only** — never observed generating (task #3) |
| `DecoFlowerGenerator` | not started — self-contained (task #1) |
| `UnknownArtefactGenerator` | not started — self-contained (task #2) |
| `WarpNodeGenerator` | blocked — warp/teleport subsystem |
| `SkypeaterGenerator` | blocked — `TileEntitySkypeater` not registered |
| `DataTowerGenerator` | blocked — data-tower tiles + `DataTowerStructure` |
| `DungeonGenerator` | **six-family runtime slice landed** — Cavern/Burrow/Ocean/Desert/Snow/Biome Fragment use canonical NBT, registered natural/command features and persistent controller behavior; remaining acceptance work is world-scale frequency/terrain observation plus exact per-family bonus-loot parity |

### Two traps this sweep keeps hitting — check both on every generator

1. **V33a y values are absolute in a bedrock-at-0 world.** Do not offset by `getMinY()`. The modern
   space below y 0 is deepslate, so anything targeting `minecraft:stone` there can never place.
   This bug shipped twice, an hour apart.
2. **V33a `IWG` generators were `RetroactiveGenerator`s and ran post-population.**
   `TOP_LAYER_MODIFICATION` is usually the faithful step — but the source's literal air checks were
   written for a chunk that had not been carpeted in grass and flowers yet, and fail there.

### Nether

The five V33a roof structures are now canonical generated NBT and registered as one natural weighted
feature plus named command variants: `nether_hut`, `nether_temple`, `nether_maze`, `nether_spiral`,
and `nether_diorama`. Their loot tables, mob choices, spawner tuning, and Temple/Diorama
`NETHERSTRUCT` progression triggers are restored. In-world frequency and individual command
placement still need acceptance testing. The separate lava-river/roof-bypass decoration work remains.

### DecoFlowerGenerator dependency map — 2026-08-04

An earlier entry in this file claimed all six remaining flowers were blocked on unregistered drop
identities. **That was wrong**, and it was wrong because the check grepped for V33a's variable names
(`teleDust`, `icyDust`, ...) instead of the ported identity names. All six drops are already
registered in `ChromaCraftingItems`, since the 35 former `CRAFTING` metadata variants landed as
distinct items:

| Flower | Drop | Registered as | Blocker |
|---|---|---|---|
| `LUMALILY` | `icyDust` | `ICY_DUST` (Frozen Grains) | **none — ready** |
| `SANOBLOOM` | `etherBerries` | `ETHER_BERRIES` | **none — ready** |
| `VOIDREED` | `voidDust` | `VOID_DUST` (Void Essence) | **none — ready** |
| `FLOWIVY` | `livingEssence` | `LIVING_ESSENCE` (Nature Fiber) | **none — ready** |
| `ENDERFLOWER` | `teleDust` | `TELEPORTATION_DUST` (Distortion Crystal) | Ender Forest biome |
| `RESOCLOVER` | `energyPowder` | `ENERGY_POWDER` (Energetic Essence) | Ender Forest biome |

`GLOWDAISY` and `GLOWROOT`, the other two members of the legacy `DECOFLOWER` family, are already
concrete registered identities and are done.

So four flowers can be ported immediately. The other two need `BiomeEnderForest` registered — the
class is in the tree, pristine, and `ChromaBiomes` has no Ender Forest entry. Port the biome, then
the two flowers; do not defer them.

Biome rules to preserve: `LUMALILY` snow biome with a grass top block and not hills; `SANOBLOOM`
jungle; `VOIDREED` swamp; `FLOWIVY` hills or `rootHeight >= 1` with a grass top block, excluding
Glowing Cliffs, and it is the only biome-tinted one. `VOIDREED` grows upward in runs of up to 4 and
`FLOWIVY` hangs downward in runs of up to 12; `FLOWIVY` is inset one block from the chunk edge
upstream specifically to stop chunk spilling.


### DecoFlower: all six ported, siting verified by GameTest — 2026-08-04

All six are registered concrete identities: Luma Lotus, Ether Berries, Void Reeds, Aura Ivy,
Enderflower and Resonant Clover, with V33a support rules, the burst-shaped generator (one-in-N chunk
roll, then n placements — usually 1, one time in five `1 + rand(4) + rand(6)`, up to 40 tries),
reed/ivy column runs, ivy's chunk-edge inset, per-flower biome modifiers, cross models tinted only
for Aura Ivy, and loot yielding each flower's registered resource rather than itself.

**Verified by `deco_flower_siting_contract`, not by census.** Three earlier pregens found no flowers
at all, and that turned out not to be a bug: censusing the biomes showed the sample contained 11
chunks of jungle and *no* snowy, swamp or windswept biomes whatsoever. A random seed is simply the
wrong instrument for biome-bound features — the test now builds each flower's required terrain and
asserts acceptance and rejection directly. The full suite is **74/74**.

One real bug did come out of those runs: Aura Ivy's siting. V33a skips the "drop out of the air" walk
for ivy, climbs to the top of the wall it found and requires air beneath, because ivy hangs off a
face and grows downward. The port ran it through the ordinary path, settling it in open air where its
adjacent-solid test can never pass.

### Ender Forest — biome and tree selector both ported

`ChromaBiomes.ENDER_FOREST` is registered with V33a's rainless setting, the enderman-dominated spawn
list (Enderman 10 against Creeper/Spider/Skeleton at 1 each, groups 1-4) and the forest palette, plus
a TerraBlender entry replacing `DARK_FOREST` — `FOREST` was already taken by Rainbow Forest. That
unblocked Enderflower and Resonant Clover, which are now ported, so all six deco flowers are in.

**The tree selector is ported.** `EnderForestTreeFeature` reproduces V33a's dynamic-weight draw: a
`Simplex3DGenerator` at frequency 1/30, seeded `(worldSeed << 17) + (worldSeed >> 43)`, with every
candidate's weight recomputed as `max(0, base + coefficient * noise)` — oak 25/+10, fancy oak 5/-2,
small ender oak 50/+20, large 10/-5, narrow 6/-1, and a "no tree" entry 0/+6 competing in the same
draw. That empty entry is why a vanilla weighted tree list cannot express this: species *and* density
vary together with one noise field, so a static list would reproduce the average and lose the
structure. The two vanilla entries are resolved from the configured-feature registry and placed for
real rather than skipped, so the mix is not skewed toward ender oaks.

`EnderOakFeature` ports the shape itself: a trunk of random height under a crown whose radius
random-walks upward and whose cross-section is a `LobulatedCurve` clipped by a per-tree height ratio,
optional branches at evenly spaced jittered azimuths laying logs along a polar vector with 8-20
leaves scattered per half-step, and the hanging leaf columns that drip from unenclosed edge leaves on
the lowest crown layer. It builds vanilla oak logs and leaves — the Ender Oak is a silhouette, not a
new wood type. All three V33a variants (small/large/narrow) carry their source parameters verbatim.

The biome's placed feature uses 7 attempts per chunk, V33a's 0.7x thinning of vanilla forest's 10.

### Overworld sweep: 12 of 13 — 2026-08-04

Done and compiling, with 74/74 required tests green throughout: cave crystals, dye trees, luma/cliffs
aux, pylons, tiered ores, tiered plants, Piezo Crystals, all six deco flowers, the Ender Forest biome
with its Ender Oak shape and noise-driven tree selector, Unknown Artefacts with the lore-tower ring,
Warp Nodes with the direction-addressed warp network, and Skypeaters.

Cross-module work this sweep pulled in: DragonAPI `HexGrid` (geometry ported in full; its four
immediate-mode drawing methods marked `DRAGONAPI-PORT` until the lore GUI lands), `Towers`, and
`WarpNetwork` as a modern per-level `SavedData`.

**`DataTowerGenerator` — the last one, and it is a subsystem, not a slice.** `Towers` was its main
blocker and is now ported, but the rest of the chain is untouched and pristine:

| Dependency | Lines | Note |
|---|---:|---|
| `TileEntityDataNode` | 537 | the lore terminal itself |
| `BlockLootChest` + its TE | 602 | four metadata variants, own inventory |
| `BlockStructureShield` | 297 | with the MOSS/STONE `BlockType` split |
| `LoreManager` | 254 | only `getTower`/`initTowers` are needed here |
| `TileEntityDummyAux` + `Flags` | ? | the four stacked hitbox blocks above the node |
| `ChromaStructures.DATANODE` | 55 | needs an NBT template plus the moss/stone runtime alternative |

The structure itself is small — a 3x3 shield floor, a four-block plus at y+1, the node at its centre,
and four linked dummy-aux blocks stacked above for the hitbox — but it wants the NBT-first treatment
the other multiblocks get, and the per-block 1-in-3 moss/stone roll is exactly the kind of runtime
alternative that stays in Java.

Roughly 1,700 lines across six files before the generator itself can be written. Comparable in size
to everything else in this sweep put together, and worth its own focused pass rather than being
tacked on the end of one.

### DataTower: chain corrected after porting into it — 2026-08-04

`BlockStructureShield` is ported (eight concrete materials plus a `reinforced` blockstate), which was
the first item on the list. Working into the rest changed the picture in two ways.

**`LoreManager` is not needed.** Its `getTower` is only `initTowers` followed by
`Towers.getTowerForChunk`, and both halves are already in the ported `Towers`. `Towers.getTower` now
exposes exactly that, so the generator never touches LoreManager — whose other half (lore-fragment
distribution, `KeyAssemblyPuzzle`, `ChromaResearchManager`, `ChromaOverlays`) belongs to the
guide-book vertical and would have been dragged in for nothing.

**`TileEntityDataNode` is the real gate, and it belongs to that same vertical.** Its 537 lines depend
on `EntityTunnelNuker`, DragonAPI's `StructureRenderer` and `KeyWatcher`, `ChromaPackets`,
`ChromaResearchManager` and the lore-fragment system. It is not a standalone tile that happens to be
unported; it is the lore terminal, and porting it means porting the research/handbook layer that
TODO item 4 already scopes as ~8,000 lines of its own.

So the honest remaining chain for `DataTowerGenerator` is:

| Piece | Lines | Gate |
|---|---:|---|
| `BlockStructureShield` | 297 | **done** |
| `TileEntityDummyAux` + `Flags` | small | the four stacked hitbox blocks |
| `BlockLootChest` + its TE | 602 | self-contained inventory block |
| `ChromaStructures.DATANODE` | 55 | NBT template + moss/stone runtime alternative |
| `TileEntityDataNode` | 537 | **blocked on the lore/research vertical** |

The first four are reachable now. The last is not, and no amount of ordering changes that — the data
node's whole purpose is to be a lore terminal, so a version of it without the research layer would be
the hollow substitute the rules forbid. DataTower should therefore land with, or after, the guide
book rather than as the tail of the worldgen sweep.
## Guide book → DATANODE active chain — 2026-08-08

- **Implemented:** complete 322-entry V33a catalog and XML loader; Chromic Lexicon and Information
  Fragment item/research loop; authoritative casting pages; specialist machine/tool/ability/structure
  presentations; all 77 original ability illustrations; canonical NBT DATANODE and dummy hitboxes;
  fixed-tower generation and loot; complete Data Node deploy/scan/cooldown/reward loop and BER/FX;
  delayed tower notes; Meta-Alloy ecology; Tunnel Nuker population; Memory Crystal inscription,
  custom entity and renderer; deterministic server-authoritative key assembly; Rosetta decode; and
  death-persistent lore presentation.
- **Focused verification:** `compileJava` and `runClientData` pass. The four new contracts
  `meta_alloy_ecology_contract`, `tunnel_nuker_entity_contract`,
  `memory_crystal_inscription_loop`, and `lore_key_puzzle_contract` pass individually. Older focused
  DATANODE, scan, fragment, catalog and casting contracts remain the established checkpoint and were
  intentionally not rerun wholesale.
- **Acceptance gate:** inspect the eight-part slice in a real client for exact rendering, audio,
  particle choreography, interaction feel and natural population rates. Headless tests cannot prove
  those properties.
- **After acceptance:** fix only observed parity defects, then continue the guide-bound gameplay
  catalog. As original structures are ported, create real modern NBT templates and replace their
  compact guide footprint with an NBT-driven three-dimensional preview. Do not invent previews or
  runtime geometry for structures that have not crossed that boundary.

## 2026-08-09 acceptance queue

- [ ] In an Overworld, run `/locate chromaticraft:data_tower`, teleport/travel to the returned X/Z,
  and confirm the corresponding generated tower exists. Keep `/place feature chromaticraft:data_tower`
  as an exact-root diagnostic; failure away from a seeded root is intentional.
- [ ] In creative, target and break Aura Bloom and the other tiered plants; place Elemental Stones
  and Firestone and confirm they persist. In a fresh insufficient survival player, confirm the
  progression concealment/removal behavior is unchanged.
- [ ] Inspect Elemental Stones, Firestone and Energized Rock both in inventory and in world: no
  checkerboard, correct host underlay, and the original animated/emissive overlay when unlocked.
- [ ] Inspect one cave crystal, lamp crystal and potion/super crystal in inventory, GUI, dropped-item
  and first-/third-person contexts. Confirm colour, translucency, spike geometry and stone base where
  applicable, with no atlas/model errors in the client log.
- [ ] Grow/generate several dye trees and rainbow trees. Each individual trunk must use one log
  species and neighboring trees may vary. An unobstructed rainbow-tree attempt should place the
  original 1,020-cell large tree from `worldgen/rainbow_tree.nbt`; obstruct its footprint to exercise
  the source's one-in-five small-tree fallback and verify its rising/falling diamond canopy.
- [ ] Inspect all four GeoStrata luminous-crystal block identities and verify blue, green, orange and
  purple world tinting instead of white.
- [ ] Aim the Elemental Manipulator at each visible Data Node body/hitbox cell. Confirm every cell
  relays to the controller and the mouseover operation ring advances continuously through the
  120-tick activation rather than remaining pending.
- [ ] Inspect both deploying Data Node tower rows and the square inner sleeve from every side and
  below. Inspect the twisting sky cage near the node, at medium distance and beyond 150 blocks;
  keep part of the model/beam visible while looking away from the controller and confirm it is not
  culled with the block entity.
- [ ] Confirm cave, lamp and potion/super-crystal blocks use exact model-shaped hover lines with
  vanilla colour and width, including Minecraft's high-contrast outline mode. Recheck those three
  item families on an Item Stand as well as the item contexts listed above.
- [ ] Run `/locate chromaticraft:data_tower`, click its green coordinate component and verify it
  inserts the safe teleport command. Run `/locate chromaticraft:data_tower all`; verify all thirteen
  puzzle-order names/symbols have unique, individually clickable coordinates.
- [ ] Open an incomplete Memory Crystal key puzzle and verify the world is visible behind the board;
  complete/Rosetta presentation must retain its authored background.
- [ ] Compare hand, diamond-pickaxe and netherite-pickaxe break speeds on ordinary and several
  variant Crystalline Stone blocks. Spot-check crystals, runes, shielding, cliff soils/leaves and
  the loot chest against their generated vanilla tool tags.
- [ ] In a new world or with previously unexplored tower chunks, use the `all` locate command and
  inspect any roots near forests or predicted villages. Towers should have a clean 9x9 vegetation
  envelope and should remain outside the village/jigsaw exclusion area. Existing generated towers
  are intentionally not rewritten in-place.
- [ ] Generate new Luminous Cliffs terrain containing Skypeaters and confirm there are no further
  `unsafe terrain read` reports or null-level `setNodeType` crashes during FEATURES generation.
- [ ] Reopen the solved Memory Crystal puzzle at multiple GUI scales/aspect ratios. The authored
  `all-back.png` composition must appear once across the page, with no repeated 256px seams.
- [ ] Run several explicit colour commands, including `/place feature chromaticraft:pylon_black`,
  `/place feature chromaticraft:pylon_light_blue`, and `/place feature chromaticraft:pylon_white`;
  each resulting pylon must have exactly the requested element. The removed generic
  `chromaticraft:pylon` identity should not autocomplete. Confirm naturally generated pylons still
  vary in colour through `chromaticraft:natural_pylon` worldgen.
## Chromic Lexicon in-world acceptance — shared navigation pass (2026-08-09)

- [ ] Reopen the lexicon after changing section and scrolling; verify it returns to that location.
- [ ] Verify all seven parents: Introduction, Constructs, Other Blocks, Tools, Resources, Abilities,
      and Structures; locked/missing fragments must remain absent.
- [ ] Check entry icons and tier-color bars at several progression levels.
- [ ] Use A/D and Left/Right to change sections/entries; use W/S, Up/Down, and the wheel to page.
- [ ] Search with the button and `/`, edit with Backspace, finish with Enter, and cancel capture with
      Escape. A search must include matching pages from every section without exposing locked pages.
- [ ] Select a known castable entry in Recipes mode and verify it opens the authoritative casting
      recipe after the server response; Items mode must open its description. In recipe view verify
      A/D or the wheel changes recipes and W/S changes Grid/Runes/Stands/Aura pages.
- [ ] Open a long XML-backed entry and verify all description/notes pages are readable and neither
      overlap the controls nor truncate.
- [ ] Open Pylon, Casting tiers 1–3, Repeater, Compound Repeater, Pylon Broadcast, and Data Tower
      structure pages. Verify each reports its canonical `chromaticraft` NBT identifier, correct
      dimensions, and real placed-block layout rather than a synthetic footprint.
- [ ] In 3D view, verify A/D yaw, W/S pitch, wheel zoom, continuous left-drag rotation, right-click
      reset, clipping, depth order, and block-name hover tooltips. In 2D view, verify A/D rotates by
      90 degrees and W/S, wheel, and +/- traverse every Y layer without leaving the valid range.
- [ ] Verify casting previews show the table and tier-appropriate item stands, pylon previews show
      their controller, and the Data Tower preview shows the complete vertical Data Node relay
      column rather than dropping its block-only auxiliary cells.
- [ ] Open a Structures entry whose NBT conversion has not landed. It must clearly report that the
      template is unavailable and must not invent a replacement layout.
- [ ] Follow-up: add canonical NBT templates for the remaining V33a structure-guide entries, then
      evaluate rendering full world block/BER models in-screen instead of scaled block-item models.
- [ ] Follow-up: complete the editable Notebook controls and packet/data-component persistence,
      plus full V33a progress-tree and fragment-recovery presentations.
## Guide Notebook/Progress in-world acceptance (2026-08-09)

- [ ] Notes: add more than ten rows, scroll both directions, edit old and new rows, Save, close and
      reopen the same held lexicon; verify exact nonblank content persists.
- [ ] Notes: switch directly from Notes to Guide without pressing Save, reopen Notes, and verify the
      dirty rows were saved automatically.
- [ ] Notes: use Clear and save; verify pages/fragments and creative state were not altered.
- [ ] Progress/Levels: verify all ten tiers appear and current/reached coloring matches the player.
- [ ] Progress/Stages: page with W/S and the wheel, open an achieved and an unachieved stage, and
      verify the achieved page uses reveal prose while the unachieved page uses hint prose.
- [ ] Progress/Tree: verify prerequisite names and green/available/locked coloring against known
      early chains such as CRYSTALS → CASTING and PYLON + CRYSTALS → CHARGE.
- [ ] Progress: verify A/D and Left/Right cycle Tree, Levels, and Stages.
- [ ] Put a physically stored page into a lexicon without granting its player research (command or
      data editing), confirm the red question overlay, refusal to open, and paired error sound.
- [ ] Follow-up: replace the compact Progress Tree with the exact free-panning V33a topology,
      connection line types, icons, hover cards, and research-tier grouping frames.

### Chromic Lexicon parity — next steps after the scrolling pass — 2026-08-09

Done: all 28 Handbook textures extracted, `LexiconScrollPane` (V33a `GuiScrollingPage`) with
frame-rate-normalised held-key panning and the `navbcg` scrolling backdrop, wheel and keys sharing one
offset.

Still missing, in dependency order:

1. ~~**Spatial navigation sheet.**~~ Done 2026-08-09 — `LexiconNavigationSheet` ports
   `GuiNavigation`'s section/category/grid layout, both pan axes carry content, and cells are
   clickable with hover titles, plus V33a's per-section and per-category hover brightening
   (`hoverTime` 0-20 tinting the outline, decaying every other frame) and the search fade
   (`searchAlpha` in/out at 0.05/0.1 per frame, dimming non-matches rather than hiding them).
   The fade uses a scrim rather than V33a's `squarefog.png` sprite; swap that in when the icon
   atlas work lands.
2. **Specialist page renderers**, now unblocked on art: machines, tools, craftable blocks/resources,
   abilities, rituals, adjacency cores, crafting, casting, alloying, pack changes, structures. Each
   has its own `handbook_*.png` background and its own `Gui*` source to compare against.
3. **NBT-template-backed structure previews** with layers and mouse-drag rotation.
4. **Progress Tree / Progress By Level / Progress Stages** presentations (`progress.png` is now
   available).
5. **Editable Notebook** with server-authoritative data-component persistence (`notes.png` available).
6. Search fading, hover tooltips and the original button sounds.

## 8. Chromic Lexicon — the next ten steps (2026-08-09)

Ordered so each unblocks the next. V33a `Gui*` source is the spec for every one; compare directly
rather than generalising one renderer across several pages.

1. ~~**Per-page backgrounds.**~~ Done 2026-08-09. `GuiBookSection.PageType` picks a `handbook_*.png` per page, and
   `GuiCastingRecipe` switches it *per subpage* (0 CAST, 1 RUNES, 2 MULTICAST, 3 PYLONCAST2) — which
   maps straight onto the port's existing Grid/Runes/Stands/Aura subpages. The port currently blits
   one `handbook.png` for everything. All 22 backgrounds are now extracted.
2. ~~**`GuiCraftingRecipe`**~~ Done 2026-08-09. — ordinary grid recipes for craftable entries, server-authoritative like
   the casting view already is. Uses PageType.CRAFTING, which reuses `handbook_cast.png`.
3. **`GuiMachineDescription`** — active-content pass done 2026-08-11. Upstream has four subpage kinds;
   MAIN, authored NOTES, and reachable ENERGY are ported in the original order.
   - [x] **MAIN** — the slowly turning model of the construct, at `posX+167, posY+44`, scale 48, yaw
         from `nanoTime()/20000000 % 360`, pitch starting at 22.5 and draggable to +/-45, with the
         model lifted by `8*sin(|pitch|)` as it tips. Rides the structure viewer's picture-in-picture
         element (`LexiconMachineRender`), so the block entity renderer runs and a construct that is
         mostly its renderer is not a bare cube. The invented "Crystal network construct" captions
         that stood here are gone; upstream draws no caption.
   - [x] **ENERGY** — done 2026-08-10. A 64-pixel supply badge from `infoicons.png` low on the page
         and a proportional wheel of the elements the construct draws, at `(posX+xSize-32-50,
         posY+42)` with radius 32, ringed white then black. The wheel is DragonAPI's
         `Proportionality` used as designed -- `setGeometry` then `render` -- on a picture-in-picture
         element, since it needs a `SubmitNodeCollector`. Reached with W/S like any other subpage, and
         only offered when the construct actually costs energy.

         Upstream branches on `ChromaTiles` predicates; the port tests the stand-in block entity
         instead, which asks the same question of the object rather than a parallel enum and needed no
         new registry. Only the pylon-powered branch is live: the other two read `getRequiredEnergy()`
         off `ChargedCrystalPowered` and `TileEntityRelayPowered`, two abstract block-entity bases not
         yet in the allowlist.
   - [ ] **AOE** — blocked, and *unreachable* rather than merely unported: `ComplexAOE` and all four
         of its implementors (`TileEntityFarmer`, `TileEntityBiomeReverter`, `TileEntityCropSpeedPlant`,
         `TileEntityHarvesterPlant`) are outside the build allowlist, so no machine can enter this
         branch. Do it when the plant/farmer machines land, not before.
   - [x] **NOTES** — authored XML notes are restored between MAIN and ENERGY. Function Relay's
         additional item/effect grid remains dependency-bound to its unported effect provider.
4. **`GuiToolDescription`** — active-content pass done 2026-08-11: exact four-times-size cycling
   item presentation, two-second interval, authored usage-note subpage, and arrow/W/S traversal.
   Aura Pouch's effect grid returns with that item/effect provider rather than being fabricated.
5. **`GuiCraftableDesc`** — active-content pass done 2026-08-11: rotating 48px block models for
   BLOCKS, four-times-size one-second cycling for RESOURCES, and exact registered V33a variant lists.
   Heat Lamp effect pages and Forestry bees remain tied to those content ports.
6. **`GuiPoolRecipe`** — chroma-pool/alloying display (PageType.POOL). Depends on the pool recipe
   system, so it lands with the ALLOY vertical.
7. ~~**`GuiAbilityDesc`**~~ done 2026-08-11. Its whole job is the ability's 50x50 art at
   `(leftX+103, topY+11)`. The port was already drawing it there but sampling it as if the file were
   256x256, so it showed the top-left fifth blown up; the files are 50x50 and upstream's
   `drawTexturedModalRect(.., 256, 256)` at scale `50/256` covers the whole image.
8. ~~**`GuiRitual`**~~ done 2026-08-11. Reached as the ability page's second subpage, turned with
   W/S like every other page. The `Proportionality` wheel at `descX+184, descY+52` radius 57.5 with
   `misc.png` laid over it for the rim, the Total Energy column at `descX+43`, and the gauge whose
   height is `125 * sqrt(total / maxAbilityTotalCost)` — square-rooted so a cheap ability is still
   legible beside an expensive one. The sixteen outline runes upstream rings the wheel with are the
   stripped `getOutlineRune` glyphs, same as the manipulator HUD's, and are marked CHROMA-PORT.
9. **`GuiAdjacencyDescription`** — adjacency cores (PageType.ADJACENCY).
10. ~~**`GuiNotes`**~~ done 2026-08-11. Persistence was already server-authoritative; what was wrong
    was the layout and the chrome. Ten lines on a twenty-pixel pitch at `(j+8, k+3+i*20)`, 240x19,
    and V33a's three-button stack down the left edge — `-` scroll up, `+` scroll down, `*` append —
    each 20x20 at `j-20`, plus the Return tab. Gone: an invented Save button (upstream writes on
    close, which the port already did), an invented Clear that could wipe every note at once with no
    upstream equivalent, and a "Notebook" title and "Lines N-M" counter printed onto what is drawn as
    a sheet of paper. A new line seeds as `-Add Notes-`, as upstream does, so a blank line reads as
    typable.

    One deliberate difference: upstream turns only the line being edited into a text field and draws
    the rest as text truncated at 45 characters. The port leaves all ten as borderless edit boxes —
    identical at rest, one click cheaper to start typing, and the only visible divergence is that a
    long unfocused line scrolls instead of ending in an ellipsis.

Carried alongside: swap the search scrim for V33a's `squarefog.png`. The image tabs now use the
original GUICLICK/GUISEL cues; temporary vanilla buttons still need conversion to the source sheet.

### Specialist-description in-world acceptance — 2026-08-11

- [ ] Open a registered Tool page. Its item must be 64px at the upper right, with no `Tool: ...`
      caption. If it has authored notes, W/S and the right-edge arrows must switch description/notes.
- [ ] Open Runes, Dye Leaves, Crystal Lamp, Potion Crystal, Crystal Stone, Shards, Plants, Groups,
      Cores, Energized Cores, Iridescent Crystal, and Alloying. Confirm every registered family
      cycles through its real independent registry identities, at two seconds for tools/blocks and
      one second for resources, without unrelated stand-in items.
- [ ] Open several Other Blocks pages and verify their real block model rotates at V33a's upper-page
      anchor. Models with block-entity renderers must include that renderer rather than becoming a
      bare inventory cube.
- [ ] Open a machine with authored notes and lumen ENERGY. Page order must be MAIN → NOTES → ENERGY;
      the NOTES page must not be overwritten by the energy wheel.
- [ ] Open Crystal Elements and traverse overview plus all sixteen element pages. Each must switch to
      `handbook_element.png`, show its exact 64px glow rune, and display the authored `elements.xml`
      prose with Kuro/Karmir/etc. substitutions.
- [ ] Open Crystals, Pylons, and Lumen Node under Introduction. Confirm the rotating cave crystal,
      smoothly color-cycling additive pylon flare, and 64px Skypeater presentation respectively.
- [ ] Hover and click the Items/Recipes/Search/Progress/Recovery/Notebook image tabs. Confirm the
      registered GUISEL/GUICLICK sounds play once and vanilla's button click does not layer over them.

### Lexicon 3D structure viewer — done (2026-08-10)

Ported. `DragonAPI reika.dragonapi.instantiable.rendering.structure` now holds the real thing:

- `StructureRenderer` keeps the state V33a's class kept — rotation (`rx = -30, ry = 45, rz = 0` by
  default), the current slice, and the per-position/per-block display overrides — plus `drawSlice`
  and `tally`;
- `StructurePipRenderer` + `StructureRenderState` do the drawing. 26.2 has no GL matrix stack in GUI
  space (`GuiGraphicsExtractor.pose()` is a `Matrix3x2fStack`), so the only route for a rotatable 3D
  scene is a picture-in-picture element: it renders to its own colour and depth texture with a real
  `PoseStack` and the result is blitted into the GUI layer. Registered through NeoForge's
  `RegisterPictureInPictureRenderersEvent`;
- blocks are **real block models**: `ModelManager.getBlockStateModelSet().get(state)` ->
  `collectParts` -> `submitBlockModel`, on the translucent or cutout block-item sheet according to
  `hasMaterialFlag(1)`, with tints from `BlockColors.getTintSources`;
- V33a's `BlendMode.ADDITIVE2` alpha pass is `glBlendFunc(GL_SRC_ALPHA, GL_ONE)`, i.e. exactly
  `BlendFunction.LIGHTNING`. There is no stock render type that takes block geometry and blends that
  way, so a NeoForge `PipelineModifier` rebuilds the sheet's pipeline with that blend for the second
  pass. The alpha set itself is `LexiconStructurePreview.markShared`, mirroring `GuiStructure`'s
  switch: casting2 -> casting1, casting3 -> casting2, pylonbroadcast -> pylon (position-only, as
  upstream);
- the discrete scale tiers are V33a's, keyed off `max(sizeY, hypot(sizeX, maxZ))` where `maxZ` is a
  **half**-extent because upstream's arrays are origin-centred.

One deliberate divergence: V33a scales by `(-d*s, -d*s, -d*s)`, which mirrors the structure in X on
top of orienting it. The port uses `scale(1, -1, -1)`, the step every vanilla picture-in-picture
renderer takes, and so drops the mirror; the multiblocks are near enough all X-symmetric and the view
spins freely anyway.

The screen side now matches `GuiStructure` too: plain `3D`/`2D` 20x20 buttons at `j+185`/`j+205,
k-2`, the `N#` block-tally mode at `j+165` (`j+125` while slicing) with its `+`/`-` stepper, the
`(XxYxZ)` caption at `j+6, k+10`, LMB-drag to spin, RMB to reset, A/D/W/S polled per frame while
held. The invented zoom, the arrow-key 15-degree steps and the `3D ✓` labels are gone.

The block-entity pass landed the same day: `StructureRenderer` builds a stand-in `BlockEntity` per
position, extracts its renderer state during the GUI extract phase, and the picture-in-picture pass
submits it. Light is forced to full brightness (the stand-in has a level, so `extractBase` would
otherwise sample the real world at the structure's local coordinates), and V33a's static
`isRenderingTiles()` is ported so renderers can skip world reads — `RenderCastingTable`'s rune scan
is the first consumer. Stand-ins are not ticked, as upstream's are not, so a renderer animated from
a tick counter stands still while one animated from wall-clock time moves.

`addEntityRender` landed too — any entity, ticked once per frame as upstream ticks it, extracted with
the block entities and drawn half a block in and three eighths up. `addRenderHook` was **not** ported
on purpose: it is write-only in V33a, its one read site being inside the comment around the old
item-icon `draw3D`, and its scale/offset are pixel nudges for a 2D layout that no longer exists.
Implementing it would add behaviour upstream does not have.

Neither has a consumer yet — the portal structure has no NBT template and `EntityChromaEnderCrystal`
is still a raw 1.7.10 file outside the build. Both are prerequisites for the portal page.

## Item rendering backlog — from in-game inventory screenshot, 2026-08-10

Five separate defects, each its own slice. Evidence gathered so far is recorded per item so none of
it has to be re-derived.

### 1. Crystal lamps and potion crystals render untinted — FIXED 2026-08-11

The first fix correctly moved both pieces onto `itemTranslucent`, but did not fix their order. The
opaque plinth was still submitted *after* the alpha-220 colour mesh, allowing its depth-writing faces
to replace the coloured pass with a white silhouette in the 26.2 item target. It is now submitted
first and the translucent, element-tinted spike mesh second. Generated JSON was audited across
colours and already carried the right concrete `element` value; this was never a registry/metadata
problem.

That also settles (2) -- the cave crystals were always translucent through this path, which is what
proved the path itself was fine.

Original investigation, kept because the eliminations are still useful:


**Ruled out, with evidence** — do not re-check these:
- The generated item model is correct and structurally identical to the cave crystal's, which *does*
  tint: `assets/chromaticraft/items/crystal_lamp_blue.json` carries
  `{"type":"minecraft:special","model":{"type":"chromaticraft:cave_crystal","element":"blue",...}}`.
  So the special renderer is bound and the element reaches it.
- `CrystalElement.getColor()` returns a plain `0xRRGGBB`, so `ARGB.color(220, rgb)` in
  `CaveCrystalItemRenderer.Unbaked.bake` composes a valid ARGB and is not being corrupted by an
  alpha already present in the source.
- `CaveCrystalGeometry.emitBase` draws a plinth of `BASE_HEIGHT`, not a full cube, so the smooth-stone
  base is not covering the crystal.

**What is left**: the tint is applied as vertex colour on `RenderTypes.itemTranslucent`. Either that
path drops vertex colour in the item target, or the item compositor flattens it — the renderer's own
comment already suspects the compositor. Next step is to compare against a vanilla special item
renderer that tints successfully, and to test whether `entityTranslucent` behaves differently here.
The cave crystals tint correctly through the *same* call, which is the strongest clue available: the
only difference between the two paths is the presence of `base_texture`, so start by rendering a lamp
with the base suppressed and see whether the tint returns.

### 2. Cave crystal, lamp and potion crystal items need transparency — FIXED 2026-08-11

All three families use the shared V33a spike renderer, `itemTranslucent`, and alpha 220. Lamp/potion
plinths remain opaque vertices on that same target and now precede the crystal pass.

### 3. Flower and plant items render wrong — FIXED 2026-08-10

Both the deco flowers and the tiered plants handed `ItemModelUtils.plainModel` their *block* model,
so the slot got two intersecting planes seen at the inventory's viewing angle -- a pair of slivers.
They now generate a `ModelTemplates.FLAT_ITEM` sprite model, which is what vanilla gives every one of
its own flowers. The tiered plants use their front layer, the one carrying the plant's shape. The
generated models come out with `force_translucent`, so the sprites keep their alpha.

Original note:


The tiered plants, deco flowers and similar draw as their block models in the slot, which for a
cross-shaped plant reads as two intersecting planes seen edge-on. V33a gives them flat item sprites.
Needs a per-plant item model pointing at a sprite rather than the block model; check whether the
sprites already ship (the recorded pattern is that textures are often present before the model is).

### 4. Lumen encrusted crystals need an item renderer — FIXED 2026-08-11

All sixteen concrete item identities now use a special translucent-atlas renderer transcribed from
`CrystalEncrustingRenderer.renderInventoryBlock`: player/element-seeded layout, discarded source
roll, 6x6 field, twelve attempts, 0.2..0.8 heights, and the element-to-white 0.85 tint. Transparent
texture pixels are preserved instead of displaying the former generated cube.

### 5. Info fragments need their icon and the shift-view feature — FIXED 2026-08-11

This is an item-render behavior, not a tooltip hook: V33a draws the decoded page's actual tab icon at
half size over the paper and, while Shift is held in an inventory GUI, replaces the paper with that
icon at full size. The custom 26.2 item model now builds a nested render state for the real registered
page icon, so block, item and special-renderer icons all work without a duplicate icon map.

## Fragment structure + item polish — in-world findings 2026-08-11

Working list; each is worked to completion in order.

- [ ] **Info fragment icons and shift view.** Each fragment should carry the small icon of the page
      it holds, and holding shift should show the page's full information. `InfoFragmentItemModel`
      exists; confirm the icon selection is wired and add the shift-held tooltip.
- [ ] **Loot chest model is wrong, and mis-rotated in structures.** Investigated 2026-08-11; the
      rotation half is narrowed, the model half is not started (and `RenderLootChest`/`ModelLootChest`
      are currently being edited by hand, so this was left alone rather than conflict with that).

      **Ruled out:** the renderer's transform is equivalent to vanilla's `ChestRenderer` --
      `translate(0.5, *, 0.5)`, `mulPose(Axis.YP.rotationDegrees(-facing.toYRot()))`, translate back.
      The port pivots at y=0 where vanilla uses y=0.5, which makes no difference for a rotation about
      Y. The blockstate correctly has no FACING dispatch, because the block is drawn by its block
      entity, not a baked model.

      **Where to look:** the facing a chest is *placed* with. `DataTowerFeature` writes
      `LOOT_CHEST.defaultBlockState()`, which is always NORTH -- it never sets FACING. Structure
      chests come from the NBT templates instead, so check whether those templates carry a facing at
      all; a template written from an unrotated capture would give every chest the same one. Compare
      against the facings V33a's structures use before changing anything.
- [x] **Snow structure: breaking the controller does not release the chests.** FIXED 2026-08-11.
      V33a's `TileEntityStructControl.breakBlock` rewrites every shield and loot chest in the
      structure to `meta % 8`, dropping metadata bit 3 -- the reinforced flag -- so the shell becomes
      ordinary mineable material. Nothing in the port ever cleared `REINFORCED`, so structures stayed
      sealed forever. `onControllerBroken` now sweeps the structure's bounds and clears it, hooked
      from `playerWillDestroy` (not `affectNeighborsAfterRemoval`, which runs after the block entity
      and therefore the structure type and bounds are gone). The chests themselves were never the
      blocker: they are open to anyone who has not claimed them, and it is the reinforced cap above
      each that holds them shut. Original text: Breaking the structure
      controller should unlock the loot — specifically it should allow breaking the block above a
      chest so the chest can be opened. Currently the chests stay sealed.
- [x] **Desert structure has no shielding crack.** FIXED 2026-08-11. `openDesert` placed the corner
      caps and the concealed lower columns but none of V33a's 32 crack blocks, which are the only way
      into the lower chamber -- so the structure had no entrance at all. They are placed on player
      proximity, not at generation, which is why this was invisible to any generation check. Upstream
      writes them against an anchor seven west, three down and seven north of the controller; the port
      expresses the same blocks relative to the controller. Original text: There is no way down to the bottom; upstream
      leaves a gap in the shielding as the entrance.

## The ability subsystem, and the two book pages behind it — scoped 2026-08-11

`GuiRitual` (step 8) and `GuiAbilityDesc` (step 7) both sit behind this, so it was surveyed as one
piece.

**What exists.** `ChromaAbilityData` is in the build but is *not* the subsystem — it is 43 lines
holding a pylon-immunity flag. The subsystem proper is three unported files:

| file | lines | what it is |
|---|---|---|
| `registry/Chromabilities` | 744 | the ability enum, its display names and its behaviour hooks |
| `auxiliary/ability/AbilityHelper` | 1847 | the engine: what each ability actually *does* |
| `auxiliary/recipemanagers/AbilityRituals` | 393 | the ritual aura costs per ability |

The `Ability` interface `Chromabilities` implements does not exist in the tree at all — not unported,
absent. It has to be written from upstream before any of the three will compile.

**The seam worth knowing: the book does not need the engine.** `GuiRitual` reads
`AbilityHelper.getElementsFor(a)`, and that method is only `tagMap.get(a).copy()` where `tagMap` was
filled from `AbilityRituals.instance.getAura(c)`. So the guide's whole data dependency is the enum
plus the aura table — roughly 1,100 lines — and the 1,847-line behaviour engine can follow later with
the abilities themselves. Port in that order:

1. ~~`Ability`~~ done 2026-08-11. Flattened out of `AbilityAPI` into `api/abilityapi/Ability.java`,
   which is the package `Chromabilities` already imports it by. Its `getTexturePath` +
   `getTextureReferenceClass` pair became a single `getTexture(boolean gray)` returning an
   `Identifier`, since 26.2 addresses textures by namespace rather than relative to a class's package.
   Forge's `TickEvent.Phase` is gone in NeoForge 26.2 (replaced by separate Pre/Post event classes),
   so `Phase { START, END }` is carried on the interface rather than dropped — an ability that must
   act before the world ticks is not interchangeable with one that acts after.
2. ~~`AbilityRituals`~~ done 2026-08-11. All 39 abilities and 138 aura entries, verified identical to
   upstream by re-parsing both. **Keyed by ability ID rather than by the enum**, because the costs are
   pure data and do not need the 744-line `Chromabilities`: the key is the upstream constant name,
   which is what `getID` derives from, so it binds to the enum the moment that lands and the book can
   read it now. Upstream's table-tracking half — the `WorldLocation` set of active ritual tables and
   its `RitualAPI` surface — is state for *performing* a ritual rather than describing one, and is
   deferred with the engine.
3. ~~`Chromabilities` as data only~~ done 2026-08-11. 744 lines down to 281: all 39 constants with
   their tick phase, client flag and mod gate, plus `costsPerTick` (19 abilities), `getMaxPower`
   (11) and `isPureEventDriven` (12), each verified against the original by re-parsing both sides.
   The behaviour methods are present and inert rather than commented out, so the type satisfies
   `Ability` and the book can hold a real reference; each is marked CHROMA-PORT at the engine seam.
   `isAvailableToPlayer` returns false deliberately — nothing should advertise itself as obtainable
   while the progression walk that decides it is unported. `getPowerDesc` is the one piece of text
   that cannot be written faithfully yet: upstream interpolates AbilityHelper's tuning constants.
   V33a's DragonAPI `ModList` became a plain mod id checked against `net.neoforged.fml.ModList`.
4. `GuiAbilityDesc`, then `GuiRitual`.
5. `AbilityHelper` — **data half done 2026-08-11**, effects half still outstanding.

   Ported: the progression map (26 gates over 23 abilities, verified identical to upstream) and the
   per-tick cost, which is the ritual aura scaled by 0.0008 with upstream's three per-ability
   multipliers and its CTM/DIMENSION progression scaling. `Chromabilities.isAvailableToPlayer` now
   answers from that instead of always returning false, so the guide gates abilities correctly.

   Still out: the thirty-nine effects and the tick and event handlers that drive them — 1,847 lines
   across 138 imports upstream, reaching into most of the mod. Until that lands an ability can be
   shown, gated and priced, but not used. One small dependency noted at the site: upstream applies
   `power(0.75)` to the running cost while an Efficiency Crystal is held, and `ItemEfficiencyCrystal`
   is unported.

**Two blockers `GuiRitual` shares with pages already ported**, worth solving once rather than three
times:

- ~~`descX`/`descY`~~ resolved 2026-08-11: they are `ChromaBookGui`'s `descX = 8, descY = 88`, not
  `GuiBookSection`'s — which is why looking for them in the latter kept coming up empty. Two
  constants were blocking three pages; the casting page's aura subpage can now use them too.
- `CrystalElement.getOutlineRune`. The ritual wheel rings its pie with the sixteen rune glyphs at
  `0.625 * r`, which is the same glyph ring the manipulator HUD's wheel is missing. Both come back
  with the rune sprite redesign.

Everything else `GuiRitual` needs is already in hand: `handbook_ritual2.png` is extracted, the
`Proportionality` wheel is proven on the machine ENERGY page, and the port already resolves
`textures/ability/<id>.png` for the abilities section's header art.
