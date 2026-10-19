# Focus crystal lighting and original draw-state contracts

Target: Minecraft 26.3 / NeoForge 26.3.0.76. Original ChromatiCraft reference:
5cde0068c^, Render/TESR/RenderFocusCrystal.java, Models/ModelFocusCrystal.java,
Models/ModelFocusCrystalMessy.java and Models/ModelFocusCrystalBase.java.
Supporting source: Render/TESR/RenderRift.java and Render/ISBRH/CrystalGlowRenderer.java.
Original helper semantics are in DragonAPI de1bcf1f, Libraries/Rendering/ReikaRenderHelper.java.

## Directional light and lightmap are independent

The previous shared UNLIT_ENTITY_CUTOUT incorrectly documented disableEntityLighting
as disabling GL_LIGHTING. That helper disabled only the lightmap texture unit.
Reading the surrounding source gives these actual contracts:

| Draw | Directional light | Lightmap | Modern cutout profile |
| --- | --- | --- | --- |
| Focus base | Enabled before renderBase | Enabled; source packed world brightness | EntityLighting.LIT |
| Focus shards | Still enabled | Disabled immediately before the Techne model | EntityLighting.DIRECTIONAL_ONLY |
| Rift black box | GL_LIGHTING explicitly disabled | Retained; source full-bright vertices | EntityLighting.LIGHTMAP_ONLY |
| Crystal Glow inventory card | GL_LIGHTING explicitly disabled | Retained by the draw context; modern full-bright item vertices | EntityLighting.LIGHTMAP_ONLY |

DragonAPI's EntityLighting.cutout owns those switches per draw, plus explicit
culling, opaque alpha clipping, depth writes and sampler binding. All eight
profile/culling variants register before rendering. Unlike translucent profiles,
opaque cutouts stay in the native solid phase and need no OIT pipeline set.
The ambiguous Chroma pipeline/factory is removed after migrating all consumers.
The Rift change covers its black-box light profile; broader Rift glow acceptance
is not claimed by this milestone.

The focus base also restores its original upward normal magnitudes: 1 for the top,
0.5 for the bottom, 0.625 for the X sides and 0.75 for the Z sides, alongside its
original white/half/0.625/0.75 vertex colors. Every ENTITY vertex now also supplies
the explicit NO_OVERLAY attribute; disabling shader overlays does not remove
that required vertex-format element. Model vertices retain directional
shading even when ambient lightmap coordinates change.

## Model geometry

Both original focus models assign mirror=true only after constructing each
ModelBox. Those late field assignments do not mirror the already-built boxes.
The modern native model data therefore uses unmirrored boxes. An independent
source comparison matches all 36 ordered and 26 messy boxes, texture-coordinate
origins, pivots and rotations. Evidence: build/d06-focus-source-parity.json.
Shimmer still uses the original model-part and tile identity hashes and clock.

## World glow pass

The source disables both lighting systems, disables depth writes and applies
ADDITIVEDARK (ONE, ONE_MINUS_SRC_COLOR) around the bobber, edges, flare and beam.
Vanilla lightning/line RenderTypes do not express that complete draw contract.
Two explicit untextured Chroma pipelines retain the screen-color equation,
normal scene depth and no depth writes. Native line attributes retain their
per-vertex widths. Existing textured flare/beam geometry is preserved.

World glow geometry uses ScreenParticleRendering.submitGeometry, an explicit
native CustomFeatureRenderer submission into the existing forward screen-color
phase. That phase is part of native grouping, preparation, draining and reset;
it executes after translucent terrain in classic mode and after the OIT composite
in improved transparency. This applies the RGB screen equation to composed
scene color, rather than substituting scalar-alpha weighted OIT or an additive
lightning blend. No additional mixin, global lighting state or manual GPU draw
queue is introduced. Item solid rendering remains in its native cutout pass.

## Acceptance

FocusLightingClientChecks compiles all eight native cutout profiles and both
untextured glow pipelines, verifies shader flags and sampler/depth/blend choices,
and checks real native ENTITY meshes for all five focus tiers, both complete
shard models, original base normals and the Rift black box. The scene observer
delegates the production renderer unchanged and requires actual base/shard/
bobber/edge/flare callbacks followed by completed world frames in both native
transparency modes. Audit camera/options/client blocks are restored afterward.

Final verification results are recorded in the project ledger.


Final acceptance passed in 5m: build/d06-focus-final-client.log. Both native world
modes and the full client/water/bevel/reservoir/shutdown checks passed. The first
native mesh attempt caught the missing overlay attribute; the corrected base
passes the unchanged assertions. DragonAPI 260 and RotaryCraft 38 unit cases have
zero failures/errors/skips. All twelve current water screenshot means were
independently reproduced and pinned with logs/source hashes through
build/d06-focus-proof.json. Play launch argument hashes remain unchanged. Owner
Iris/LOD configuration acceptance and a connected-machine beam scene remain
separate; no such acceptance is claimed by this result.
