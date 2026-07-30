# ChromatiCraft 1.7.10 V33a → Minecraft 26.2 / NeoForge port

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
- The V33a pylon broadcast monument is now canonical generated structure NBT. Its stone/rune geometry is runtime-matched from the template, exact chroma-fluid cells use registry-ID checks, and pylon LOS behavior synchronizes from the complete monument. Until the modern `chromaticraft:chroma` fluid block lands, those cells are structure-void markers and the upgrade deliberately fails closed instead of accepting an invented substitute.
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

This checkpoint proves an **operational network-engine slice**, not full V33a parity. The active pylon server vertical, routed overload entity, typed client payload/effect layer, and six network model definitions are complete and tested. Broadcast monument geometry and fail-closed state detection are restored, with positive activation waiting only on the real chroma-fluid registration. Remaining repeater work is subtype interactions plus client connection/range display, surge payloads, sounds, and particles. The next active milestone is the casting recipe/table runtime on top of the completed inventory and NBT-temple foundation.

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
| `TileEntityCrystalPylon` | 1,509 | growing | Active: the server behavior cluster now includes vertical defense, ability immunity, unstable overload routing/short-circuit, colour effects, enclosure rejection, and typed client presentation in addition to the previously restored structure, storage, enhancement, ownership, booster, growth, persistence, and network behavior. Positive broadcast activation still awaits chroma fluid. |
| `TileEntityPylonEnhancer`, `TileEntityChromaCrystal` | 26 / 131 | modernized | Active and tested: multi-owner plus legacy placer persistence, item/drop round-trip, owner-only mining, rune-color pylon discovery/reconnect, registered block/entity/tile, break callback, and server-visible destruction FX are restored. V33a destruction droplets/seeds and pylon-backlash node particles now use dedicated client payloads; visual runtime verification remains. |
| `TileEntityCrystalRepeater` | 782 | growing | Active: structure/redstone lifecycle, priority, degradation/throughput, grouping, rain state, overload with exact powder drops, two-player ownership, custom data, and persistence are restored and tested; client display, surge audiovisual/payload, progression catch-up, and subtype interactions remain. |
| `TileEntityCompoundRepeater` | 246 | 178 | Active: restore multi-element throughput, structure, and repeater behavior. |
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
The base V33a throughput formula is exact: a PYLON-tier table begins at 100 lumens/tick and scales up
to 1000 before the persisted grouping multiplier is applied. A focused regression proves the full
+100% case and proves a duplicate colour clears both the bonus and prior grouped state.

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
- **the Manipulator** (`ChromaItems.TOOL` / `ItemManipulator`) — the inspect/link tool;
- both of their grid recipes, which are recorded in `ChromaRecipeProvider` as comments so they can be
  restored verbatim the moment the items land;
- Mystcraft (`MYST`) and Thaumcraft (`NODE`, Thaumometer pylon scan) branches of the exploration scan.

Not yet runtime-verified in a client: that looking at a cave crystal visibly grants the stage, the
in-world shard drop counts, and the casting table GUI/craft flow. The GameTest suite covers the
progression core and casting logic headlessly, not the client interaction.
