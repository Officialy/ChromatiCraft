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
- [ ] **Crystalline stone beam Y variant.** Fully diagnosed — this is a *connected-texture* rule in
      V33a, not a placement axis. `BlockPylonStructure` has **no axis/orientation state at all**;
      `getIconIndex` picks the top/bottom art from neighbouring beams:
      X-neighbour → `block_1-3`, Z-neighbour → `block_1-2`, otherwise `block_1`. RESORING and CORNER
      have their own neighbour rules in the same method. The port invented an `AXIS` blockstate
      property (48 variants = 16 types x 3 axes), so appearance follows how you placed the block
      rather than what it connects to. Faithful fix: a `DynamicBlockStateModel` doing the neighbour
      scan — same pattern as the existing `CliffDirtModel` — and drop the `AXIS` property. Not
      started because it is a real chunk (all four neighbour rules + removing the property +
      regenerating 48 → 16 variants), not a one-liner.

**Blocked on a client log line — every static cause eliminated:**
- [ ] **"No texture" on the glowing crystalline-stone types, item *and* placed block.** Narrowed
      precisely: the reported blocks map to V33a ordinals **3, 4, 5, 14, 15** (Crystalline Energy
      Stabilizer, Energized Crystalline Stone Beam, Crystal Pylon Focus, Aura Stabilizer, Resonance
      Ring) — which together with 13 (Multichromic Rune) is exactly the six types `StoneTypes.glows()`
      returns true for, i.e. the ones whose model carries a second emissive element. Nothing else is
      affected, so it is the glow layer.

      Checked and ruled out, so do not redo these:
      - every referenced `block/pylon/block_*` PNG exists;
      - all nine animated PNGs have a valid `.mcmeta` with an exact frame ratio (16xN, N%16==0);
      - those `.mcmeta` files *do* reach `build/resources/main` (9 in src, 9 in build), so the atlas
        is not seeing a strip as one oversized non-square sprite;
      - every `#base_*` / `#glow_*` reference resolves against the model's own texture map;
      - the emissive element's `from -0.002` / `to 16.002` is inside vanilla's -16..32 bound, and
        `light_emission: 15` and `shade: false` are both legal fields in 26.2 `CuboidModelElement`;
      - the blockstate has all 48 variants and every one points at a model file that exists;
      - all five item models point at existing block models.

      **What would settle it in one step:** the client log from startup. A stitching failure names
      the sprite; a model parse failure names the model. Either line points straight at the cause.
      Grep the log for `chromaticraft` alongside `Missing`/`Unable to load`/`JsonParseException`.

## 0b. Renderers still needed (one coherent chunk)

All of these are V33a BER/ISBRH geometry that the port currently substitutes with a plain cube or a
flat icon. Best done as a single pass over the crystal family:

- [ ] cave crystals — **item** renderer (block render already exists via `CaveCrystalModel`)
- [ ] lumen-encrusted crystals — block + item renderer
- [ ] crystal lamps — block + item renderer
- [ ] potion crystals (`BlockSuperCrystal`) — block + item renderer
- [ ] power crystal — block + item renderer
- [ ] focus crystals — block + item renderer
- [ ] item casting stand — item renderer sits too high in the inventory; needs re-centering
      (`ItemStandItemRenderer`)
- [ ] **retest whether looking at a cave crystal grants the stage** — it now plays a sound, so this
      is answerable directly.
- [ ] casting-table GUI: confirm the recipe-ready feedback shows. The plumbing looks correct —
      `writeSyncTag` sends `recipeOutput`, the screen draws a ghost output plus a `NO_ENTRY` overlay
      when `!canRunDisplayedRecipe()` — so retest now that the Manipulator works; if the ghost never
      appears, the recipe is not matching rather than the highlight being absent.

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
- [ ] **Dye-leaf drop table** (found while doing DYETREE). Dye leaves currently `dropSelf` and
      nothing else, so dye trees yield no saplings, berries, dye, apples or rainbow saplings —
      berries in particular feed the pool/alloy chain. V33a `BlockDyeLeaf.getDrops`: sapling
      `0.05*(1+f)`, apple `0.005*(1+f*5)`, dye `0.1*(1+f)`, rainbow sapling `0.0001*(1+f)^2`, and
      berries at `0.1*2^f` with a count that grows as that chance overflows 1 — another compound
      formula needing its own number provider, like `CrystalShardCount`. Shearing drops the leaf
      block itself and nothing else. Note the dye branch is partly blocked: V33a rolls
      `getVanillaDyeChance` between a vanilla dye and ChromatiCraft's own dye item, which is
      unported.
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
