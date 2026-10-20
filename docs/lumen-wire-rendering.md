# Lumen Wire model lighting and immutable effect frames

Target: Minecraft 26.3 / NeoForge 26.3.0.76. Original source: ChromatiCraft
5cde0068c^, Render/TESR/RenderLumenWire.java and Models/ModelLumenWire.java;
DragonAPI de1bcf1f, Instantiable/Rendering/FXCollection.java and
Libraries/Rendering/ReikaRenderHelper.java.

## Source draw contexts

The source renders nine model parts under the ordinary TESR lighting state, then
calls pushTESRLightingState for Shape2 alone. That helper explicitly disables
GL_LIGHTING and the lightmap, so the core requires an independent FULL_BRIGHT
cutout profile; passing maximum packed light through the body's lit shader still
leaves directional shading and is not equivalent. Native body and core now have
separate submissions. No shared ModelPart.visible toggle remains during drawing.

All ten source boxes, UV origins, pivots and rotations match an independent
comparison (build/d06-wire-source-parity.json). The legacy mirror fields were
assigned after ModelBox construction; native boxes are unmirrored. Body draw
order follows the source's nine-part list and the core remains last. Six original
world mounting transforms are retained. The detached renderer retains the source
2.5x/5-16 raised transform, while the existing item adapter keeps its established
centering and uses the same split body/core submission.

The tile's original render bounds span both connected endpoints. NeoForge 26.3's
IBlockEntityRendererExtension defaults to the renderer's unit cube and does not
consult the tile's legacy bounds method. RenderLumenWire now forwards those
source bounds explicitly, so visibility can include glints when the base leaves
the frustum. The audit observer delegates bounds as well as drawing.

## FXCollection extraction ownership

The mutable collection remains owned by client tick/extraction code. addEffect,
addEffectWithVelocity and update retain their original behavior. Snapshot captures
only scalar positions, radius, color and UVs into immutable records and a copied,
unmodifiable list; no BasicFX, live list or TextureAtlasSprite is retained by the
captured frame. Existing immediate render delegates through a fresh snapshot.
Deferred callers must capture during extraction rather than queue the live
collection. The other preserved legacy consumers remain excluded until their
complete subsystem ports and are not converted to stubs.

Preserved math: ordinary radius .1*size*sin(180*ticks/lifespan), rapid expansion's
original integer-division threshold and decay branch, moving-position update
before age/expiry, opaque render color, original four-corner UV ordering and no
new interpolation. Snapshot rendering accepts the caller's captured camera basis.
The wire copies CameraRenderState.orientation before creating its callback; later
camera/state/collection mutation cannot change the submitted glints.

The original FXCollection.render(true) disables both lighting systems and depth
writes and selects ADDITIVEDARK. Wire glints retain the existing unlit sprite
pipeline but submit through the native forward screen-color phase, after terrain
in classic mode or after the OIT composite in improved transparency. This avoids
feeding their RGB screen equation into scalar-alpha weighted OIT. Item/detached
wire models draw body/core without world glints. No new mixin or manual frame
queue is introduced.

## Acceptance

LumenWireClientChecks uses real registered renderer/model resources and native
mesh buffers. It checks ordinary/rapid envelopes, integer threshold, velocity,
expiry, snapshot/list ownership; all six mounts and complete body/core/glint
meshes; lit body versus fully unlit core profiles; native forward ownership;
replay after collection/camera/render-state mutation; detached activation; and
renderer bounds spanning the synced remote endpoint.

The audit scene delegates production rendering unchanged and requires actual
body/core/glint callbacks followed by completed world frames in both native
transparency modes. Its client blocks/support/camera/options are restored and
observers register only in Automated Checks. Normal Play remains isolated.

Final acceptance is recorded in the project ledger.

Final native acceptance: 4m07s, build/d06-wire-final-client.log; both wire
markers and the complete existing suite passed. DragonAPI 260 unit cases rerun
clean. Evidence is pinned in build/d06-wire-proof.json. D06 remains open.
