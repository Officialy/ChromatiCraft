# Rift and Aurora native glow rendering

Target: Minecraft 26.3 / NeoForge 26.3.0.76. Original references are ChromatiCraft
5cde0068c^, Render/TESR/RenderRift.java and World/Dimension/Rendering/Aurora.java.
The preserved legacy Aurora source is also available under
src/main/java/reika/chromaticraft/world/dimension/rendering/Aurora.java.

## Rift draw context

The original Rift disables GL_LIGHTING but does not disable the lightmap. Both
the face rune and halo explicitly supply full-bright light coordinates. Therefore
they use EntityLighting.LIGHTMAP_ONLY.screen, retaining the lightmap sampler and
bypassing directional shading. The previous position/UV/color sprite shader
silently discarded this distinction. The opaque box already uses LIGHTMAP_ONLY
cutout. The source getGlowRune maps to the modern element's getIcon (the glow-rune
identifier), including the lime rune and the original face-specific UV reversal.

EntityLighting.screen declares independent directional/lightmap choices for all
four profiles and both culling modes. The blend is ONE, ONE_MINUS_SRC_COLOR,
opaque scene depth is tested and depth writes remain disabled. Its 1/255 alpha
threshold retains zero-alpha rejection for the Rift's byte-alpha textures and
opaque vertex colors without adding the cutout profile's 0.1 threshold. This is
an explicit forward draw; it must not be sent through scalar-alpha OIT.

World Rift glows submit native CustomFeatureRenderer nodes through the existing
ScreenParticleRendering forward phase, after translucent terrain in classic mode
and after the OIT composite in improved transparency. That phase owns normal
batching, preparation, draining/reset and frame lifetime. No additional mixin,
manual frame queue or global graphics-state mutation is introduced.

The Rift's item adapter keeps native item-collector ownership. Its halo has
three shells (0, .0125, .025); world rendering has five through .05. A detached
block-entity render also uses the source item branch and omits the face marker,
even when a direction exists in saved data. World hue, marker direction and world
membership are captured during extraction. All ENTITY vertices supply overlay,
lightmap and normal attributes required by the format.

The original Rift inherits its depth-mask state from the enclosing render pass;
this change preserves the accepted port's explicit depth-tested/no-depth-write
glow behavior rather than inventing a new local fixed-function toggle.

## Aurora draw context

The original Aurora disables both GL_LIGHTING and the lightmap, enables RGB
screen blending and draws both sides of a 24-block curtain. Its native unlit
sprite shader retains that behavior. Its world draw now uses the same owned
forward phase rather than entering weighted OIT with an incompatible blend.

The original frame sequence remains: 32 frames at 150 milliseconds, u=(f%4)/8,
v=(f/4)/4, including wrapping rows and the original column/gradient/fade quirks.
The frame is captured during extraction and copied into the submitted callback;
workers do not re-read the wall clock while replaying one frame. Existing copied
curve positions, endpoint heights, colors and entity-relative coordinates stay
stable if the reusable state advances to another entity or another tick.

The entity renderer remains active in Proxima, preserving the existing port's
choice to draw the ribbon through the native entity path. Server-side endpoints,
colors, persistence and spawn data are unchanged. The old entity comment claiming
that the ribbon was still undrawn has been corrected.

## Acceptance

RiftAuroraClientChecks exercises actual native mesh buffers: all six marker
planes, source hue, five world shells, three item shells and detached-item branch
ownership. It compiles all eight native screen-lighting profile/culling variants
and verifies blend, sampler and depth choices. It reuses Aurora's render state,
changes the source entity data, crosses a 150ms clock boundary and verifies that
an older deferred mesh and UV frame remain byte-identical. The curtain retains
its original 24-block height.

WorldGlowSceneClientChecks delegates both production renderers unchanged and
observes box/rune/aura/curtain callbacks followed by completed world frames in
classic and improved transparency. Its client-only fixture uses the real Rift
sync-tag API and an assigned-ID inert Aurora, and restores blocks, entity,
camera and options. Observers register only in Automated Checks; normal Play
renderers and launch arguments remain separate.

Final verification results are recorded in the project ledger.


Final acceptance passed in 5m01s: build/d06-rift-aurora-final-client.log. Both world
modes, mesh/phase/clock tests and the complete focus/bevel/reservoir/matrix/water/
client/shutdown suite passed. DragonAPI 260 and RotaryCraft 38 unit cases have
zero failures/errors/skips. All twelve water screenshot means were independently
reproduced; proof/logs/PNGs/source hashes are pinned through
build/d06-rift-aurora-proof.json. Play launch argument hashes remain unchanged.
The initial fixture needed explicit entity-ID assignment before renderer
extraction, as required by 26.3; the final run has no entity-ID, shader, pipeline
or vertex-attribute error. Owner Iris/LOD acceptance remains separate.
