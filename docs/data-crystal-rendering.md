# Dropped Memory Crystal rendering source contract

The source path is V33a Render/Item/DataCrystalRenderer.java, not the empty
Render/Entity/RenderDataCrystal shadow/fire subclass. DataCrystalRenderer calls
RenderDataNode.renderPrism(0, ..., 1, 1.25, 0, orange) and renderFlare(..., 1, true).
Local historical revision: ChromatiCraft 5cde0068c^.

The entity item helper's half scale combines with glScaled(3) for a 1.5-block
scale. The source EntityItem render-origin offset contributes .125, followed by
the scaled .125 translation. The centered prism tilts 27.5 degrees about Z and
90 degrees about X, or ticksExisted*90/(20+identityHashCode%10) while owned and
airborne. Spin captures the whole entity tick, not interpolated ageInTicks. Ownership is
the source stored item owner tag, independent of the entity thrower. This also
applies to freshly issued tower rewards before a player has thrown them.
The closed outline retains all ten points and the final duplicate side.

The caps disable texture sampling and use smooth color interpolation. They now
have a separate POSITION_COLOR screen draw: eighteen triangles, represented by
72 quad vertices. No texture patch approximates their colors. The top center
remains source blue even in the orange variant. The ten side quads retain their
original alternating texture strips and normal/orange texture selection.

The source disables both directional and lightmap lighting, disables alpha test,
and selects RGB screen blending (ONE / ONE_MINUS_SRC_COLOR). The cap, side and
flare draws therefore use raw unlit screen pipelines. All three retain disabled
depth writes. Caps/sides test ordinary scene depth. The visible normal crystal
flare disables depth testing, uses source yaw/pitch camera rotations without
camera roll, and retains its four original UVs and blue color. Orange crystals
omit the flare and retain their existing artealloy shader focus behavior.
All world draws belong to the native composed-scene screen phase. Deferred draws
capture their poses and variant before returning to the reusable render state.

DataCrystalClientChecks verifies eight real item-tag/thrower/landing extraction
cases and exercises 72 independent source mesh fixtures: both variants,
stationary/owned airborne spin, visible/hidden flare, three whole ticks and three
camera angles. It checks vertex coordinates, color interpolation endpoints,
side/flare UVs, source spin/remainder arithmetic, untextured/textured pipeline
choices, screen blend, depth state, GPU compilation and immutable replay.
DataCrystalSceneClientChecks delegates production entity extraction/rendering
and requires completed cap/side/flare callbacks for normal/orange variants in
classic and improved transparency. It restores its client-only entity, options,
camera and artealloy shader state. These are mesh/native scene checks; they do
not prove pixel-perfect framebuffer output or a complete artefact-alloy recipe.
