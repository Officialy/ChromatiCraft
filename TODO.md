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
structure layouts. ~8,000 lines, a subsystem in its own right. Suggested slicing:

- [ ] extract the V33a XML resources (`info.xml`, `machines.xml`, `progression.xml`, …) out of
      `ChromatiCraft 1.7.10 V33a.jar` into `src/main/resources` — they are **not** in the tree
- [ ] `ChromaResearch` (1.9k lines) — the entry registry everything else indexes
- [ ] `ChromaBookData` / `ChromaHelpData` — the XML loaders
- [ ] `ItemChromaBook` + NBT page storage, then the `gui/book/` family (4.7k lines, 19 GUIs)
- [ ] its grid recipe is already written out verbatim as a `CHROMA-PORT` comment in
      `ChromaRecipeProvider`

## 5. Carried over — owner decisions outstanding

From `ISSUES_2026-07-30.md`:

- [ ] **dynamic teal/pink/clear water** — hard-blocked without a mixin (`FluidRenderer` picks the
      `FluidModel` per `FluidState`, never per position; vanilla water's model can't be replaced)
- [ ] **Luma 4×4 mosaic** — reachable, but needs sign-off: a position-aware block model costs Luma
      the fluid renderer's surface-height and flow animation

## 6. Luminous Cliffs parity, still open

Island rivers/lakes/ores/underground trees and remaining decorations; surface blending and farmland
semantics; the remaining Rainbow Forest decorator work.
