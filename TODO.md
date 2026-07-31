# ChromatiCraft — next steps

Goal: **game start → working casting stands**. The mechanical chain is unbroken as of 2026-07-31
(see `PORTING.md` → "The tier-2 casting dependency chain"). What's left, roughly in order of value.

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
      tier-2 was. Find V33a's grant site (village-generated casting table?) and port it.
- [ ] **`DYETREE`** — V33a grants it in `block/dye/BlockDyeLeaf` (pristine); the ported
      `block/dye26/BlockDyeLeaf` dropped the hook.
- [ ] **`MAKECHROMA`** and **`ALLOY`** — check where V33a grants these and whether they gate
      anything in this window.

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
