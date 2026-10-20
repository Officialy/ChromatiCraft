# Crystal Plant source growth, lightmap and glow contract

Original CrystalPlantRenderer disables GL directional lighting and depth writes,
but leaves the lightmap enabled throughout pod and ripe-glow drawing. Both native
profiles therefore use LIGHTMAP_ONLY, not fully unlit sprites. The pod retains
ordinary translucent alpha blending and native OIT stages; ripe glow/sparkle use
the existing forward RGB screen phase after terrain/classic or after the OIT
composite/improved transparency. Source culling remains enabled and depth writes
remain disabled. Each draw captures the incoming scene packed light.

All 52 pod vertices retain the original dimensions, pointed surface, UV midpoint
mapping and three-quarter color saturation. The budding branch retains its .2
translation and .625 scale within the source (0,1,1) Y/Z-flipped frame, including
the 1/16 vertical offset. Source vertex RGBA explicitly uses alpha 255; the earlier
GL current-color alpha .25 does not replace the enabled vertex-color array.

A ripe plant's two quad layers retain source order, element color, .25 half-width
and center (.5,1.25,.5). The original transform flips Y/Z before positive camera
yaw and pitch. Native rendering now reproduces that sequence rather than a
different camera quaternion with inverted V coordinates. Both quads start at
(u0,v0) on the (-1,-1) corner, matching original drawGlow/drawSparkle.

Growth 2 emits no pod/glow; growth 1 emits the budding pod; growth 0 emits the
full pod and both ripe layers. Existing block-state growth and color identities
remain the authority. Atlas UV endpoints are copied into immutable records before
queuing work; callbacks retain neither live sprites, camera nor reusable render
state. Existing renderer bounds extend half a block upward to cover the pod/glow.

Independent numerical goldens from the original drawBulb expressions cover every
pod vertex and normalized UV. Native checks cover all 16 colors, three growth
stages, two packed light levels and three source camera cases (288 combinations),
exact pod and glow matrices, UV/color/alpha/lightmap data, phase/pipeline bindings
and deferred replay after caller pose/camera/state changes. Audit-only world
observers delegate production rendering, filter the exact fixture position and
require budding/ripe pod draws and
ripe glow/sparkle completion in both native transparency modes. Fixture dirt,
plant state, camera and options restore afterward. Observers only register for
Automated Checks; Native Play is unchanged.

Final acceptance and pinned evidence are recorded in the project ledger. D06
remains open for other source contexts.

Final full native acceptance passed in 7m57s with the fixture-position-filtered
observer, both Crystal Plant markers and all preceding client/shutdown checks.
Pinned evidence: build/d06-crystal-plant-proof.json. Native Play hashes unchanged.
