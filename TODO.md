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
- [ ] **lamps / potion crystals / power crystal / focus crystals** — groundwork done, wiring left.
      V33a draws these with the *same* spikes as the cave crystal plus a base plinth, decided by
      `CrystalRenderedBlock.renderBase()` (cave crystal false; lamp, super/potion crystal and rainbow
      crystal true). `CaveCrystalGeometry.emitBase` now reproduces that plinth exactly — 2px slab,
      V33a's flat per-face shading (255 top / 110 underside / 200 north+west / 170 south+east), and
      the side faces taking only the top two texture rows. `getBaseBlock` picks the sprite:
      `Blocks.stone` for the side faces, `double_stone_slab` for top/bottom, or `STRUCTSHIELD` meta 1
      when the crystal is unmineable.

      Remaining: generalise `CaveCrystalModel` to take a `renderBase` flag plus the base texture
      (it is currently hardcoded to the no-base cave-crystal case), register dynamic block models and
      special item renderers for `crystal_lamp` and `super_crystal`, and point their datagen at them
      instead of `crystalColourBlocks`' placeholder coloured cube. Power crystal and focus crystals
      are separate: they are block entities with their own V33a TESRs (`RenderFocusCrystal`), not
      `CrystalRenderer` blocks.
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
