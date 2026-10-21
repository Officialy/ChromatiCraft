# Ball Lightning rendering source contract

V33a Render/Entity/RenderBallLightning.java draws three ROUNDFLARE quads at radii
1, .67 and .33 with depth offsets 0, .001 and .002. Their colors are the current
render color, that RGB color OR 0x4c4c4c boosted by 1.5 with per-channel clamping,
and opaque white. The original atlas UV orientation is preserved.

The source disables GL_LIGHTING but retains the lightmap, supplies brightness
240 (block light 15, sky light 0), selects ADDITIVEDARK RGB screen blending and
disables depth writes. The old port used an unlit sprite shader which discarded
the light coordinates and submitted it into the ordinary translucent/OIT queue.
The renderer now uses EntityLighting.LIGHTMAP_ONLY.screen in the native composed
scene phase, retaining normal scene depth testing without depth writes.
The source roundflare PNG is fully opaque in alpha, so the entity screen shader's
zero-alpha rejection does not introduce a new cutoff on this texture.

V33a rotates about Y and X using the entity-to-camera polar bearing; camera roll
and view orientation do not select that bearing. Native 26.3 EntityRenderer state
positions interpolate placement, so the current entity position is captured
separately during extraction for the original bearing calculation. The removed
entity branch stays unrotated. At coincident entity/camera positions, where a
polar angle is undefined, the renderer keeps a finite unrotated pose.
The deferred callback captures its own pose, sprite and RGB color.

BallLightningClientChecks covers 384 independent source fixtures across all 16
colors, RGB/ARGB inputs, alive/removed branches and six bearings, including both
vertical axes and zero distance. It checks twelve vertices, UV/color/light,
independent placement/bearing extraction, screen shader/lightmap/depth declarations,
native GPU compilation, classic/improved native queue ownership and immutable
replay after caller/state/camera mutation.
BallLightningSceneClientChecks delegates the registered production renderer for
an actual client-only entity and requires completed world callbacks in classic
and improved transparency. It discards its entity and restores camera/options;
native ambient particles retain their normal production lifetime. These tests
do not claim final framebuffer pixel or external shader-pack parity.

Original source reference: ChromatiCraft 5cde0068c^ Render/Entity/RenderBallLightning.java.
Current API reference: generated Minecraft/NeoForge 26.3.0.76 source jar,
net/minecraft/client/renderer/entity/EntityRenderer.java.
