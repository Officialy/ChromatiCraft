# Native composed-scene ownership in 26.3

NeoForge's AFTER_TERRAIN key selects SubmitNodeCollection.afterTerrain. In the
26.3 constructor that field aliases oitTranslucent when improved transparency
is enabled. It is therefore not an independent forward phase in both modes.
A forward-only render type submitted there fails with a missing-OIT-pipelines
exception; adding scalar OIT pipelines would also change RGB screen/multiply
blend semantics, which consume already composed destination color.

The source reference is the project's generated Minecraft/NeoForge 26.3.0.76
source jar, net/minecraft/client/renderer/SubmitNodeCollection.java and
feature/FeatureRenderDispatcher.java. ScreenParticleRendering.PHASE is a separate
native phase included in grouping/draining/reset. DragonAPI executes it after
classic terrain or after improved transparency composition with scene depth.

The old AFTER_TERRAIN submissions in these nine accepted contexts now use that
owned phase: placed pylon, repeater, portal, turbo charger, structure controller,
Void Rift, donation pylon, Mob Sonar highlights and Luminous Cliffs fog. Existing
collector order values, geometry, textures, blend/depth pipelines, state capture,
and shader-pack choices are preserved. The helper name submitComposedScene
states the actual ownership instead of relying on the aliased phase name.

The isolated audit previously registered the donation pylon on its player. A
later improved-transparency water probe exposed the unsafe additive sprite when
that player was rendered. This is a separate phase issue from Memory Crystal
mesh/ownership source parity. Other ordinary custom-geometry callers remain
subject to the open D06 source-context review; this change is not a universal
fallback which silently reroutes arbitrary render types.

ComposedSceneClientChecks reproduces the native AFTER_TERRAIN/OIT alias and
invokes all seven forwarding helper overloads with real SubmitNodeStorage in
classic and improved modes. It also exercises actual donation-pylon extraction
and the placed-pylon core/glow branches, requiring orders -1 and 0 and empty OIT
queues. DonatorRenderClientChecks additionally rejects an incorrect phase in its
existing retained-geometry audit. The full client audit exercises native completed
world frames. These checks do not constitute pixel/shader-pack parity acceptance
for every migrated renderer; their separate source reviews remain in D06.
