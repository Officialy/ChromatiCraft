# Fluid Distributor and Launch Pad draw contracts

Target: Minecraft 26.3 / NeoForge 26.3.0.76. Original references: ChromatiCraft
5cde0068c^, Render/TESR/RenderFluidDistributor.java, Base/RenderDistributorBase.java
and Render/TESR/RenderLaunchPad.java. The current preserved RenderDistributorBase
contains the same original halo geometry and draw-state operations.

## Fluid Distributor

The source draws only a blue lattice halo, not a solid model. Its base renderer
disables GL_LIGHTING but retains the lightmap with full-bright vertices and uses
ADDITIVEDARK (ONE, ONE_MINUS_SRC_COLOR), double-sided. Native rendering therefore
uses EntityLighting.LIGHTMAP_ONLY.screen with complete ENTITY vertex attributes.
World draws use the native forward screen-color phase; detached/item draws retain
native item collector ownership.

All six face bounds follow a=.0125, speed=3 degrees/tick and phase offsets of
60 degrees, as in the source. SOUTH, WEST and DOWN vertex/UV ordering had changed
in the accepted port; their original order is restored. Each face retains the
source full 0..1 lattice UV rectangle and color 0x2255ff. Pulse time is copied
before creating the deferred callback instead of reading the reusable state
while the callback executes. No model or gameplay behavior is removed.

## Launch Pad

The source fan is world-only, requires a valid frame and positive charge, and
explicitly disables both lighting systems. The native draw uses FULL_BRIGHT.screen,
depth testing/no depth write and culling. Source GL_FRONT_FACE=CW is represented
by reversing each source triangle into the native CCW upward front face, rather
than disabling culling and displaying the fan underneath the pad.

The three-block-square sweep remains at y=1.001, in five-degree steps through
chargeFraction*360, with the original square boundary radius and UV coordinates.
Each triangle is a quad with a repeated last corner. Charge and enhancement are
captured before deferred drawing.

The original enhanced renderer appends green and white rim sequences inside one
TRIANGLE_FAN begin/end pair. Its first center vertex remains green for both sets
of nondegenerate triangles. The port now retains that anchor color for the white
rim sequence too; separating the fan into native triangles must not replace its
shared anchor with white. Two bridge triangles between the original sequences
are degenerate and need no pixels. This records the source vertex data contract,
not a claim of automated pixel comparison against 1.7.10's inherited shade model.

## Server-owned structure state

The Launch Pad previously tried to run NBTStructureLoader on the client during
first-tick and adjacent-block validation. The modern loader requires ServerLevel
because structure templates belong to server data registries. checkForStructure
now runs on the server; clients retain the existing synchronized struct/enhanced/
charge fields and use them for rendering. The complete server validation, source
Ender-fluid-ring enhancement check, sync packets and launch logic remain intact.
No client-only placeholder result is generated.

## Acceptance

Two native dedicated tests use the real generated launch-pad template. They check
frame validation, invalidation and restoration; complete versus broken Ender-fluid
rings; and native save/update-tag loading of structure, enhancement and charge.

DistributorPadClientChecks uses real native ENTITY mesh buffers to check all six
halo positions/UV rectangles/colors, pulse capture across state reuse, detached
ownership, zero/partial/full and enhanced charge cases, positive top-face winding,
source enhanced anchor/rim colors, world-only/structure gating, synchronized client
state retention and deferred draw isolation.

DistributorPadSceneClientChecks delegates both production renderers unchanged and
requires their actual native callbacks followed by completed world frames in
classic and improved transparency. Fixture blocks/camera/options restore after
acceptance. Only Automated Checks registers these observers; Play stays separate.

Final results are recorded in the project ledger.


Final native client acceptance passed in 3m48s: build/d06-distributor-pad-client.log.
Both production world effects, native mesh/state goldens and all existing
Rift/Aurora/focus/bevel/reservoir/matrix/water/client/shutdown checks passed.
Two required dedicated tests passed in 1m05s with the exact selector
chromaticraft:launch_pad_* (build/d06-launch-pad-dedicated.log). No fresh full-family
server result is claimed. All twelve water screenshot means were independently
reproduced and pinned with logs/source hashes via build/d06-distributor-pad-proof.json.
Play argument hashes remain unchanged. Owner shader/LOD acceptance stays separate.
