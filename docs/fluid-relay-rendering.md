# Fluid Link Anchor rendering contract (26.3)

Reference: V33a RenderFluidRelay, DragonAPI TruncatedCube and ChromaFX.renderBeam.
The anchor keeps its lit cutout model on each of the six tank faces. Its second
pass disables directional lighting while retaining lightmap value 240 (block 15,
sky 0). Cube fill, cube outline, link core and textured link planes all use the
source RGB screen blend, scene depth testing and no depth writes. World effects
use the composed-scene phase, outside OIT. Preview draws retain their collector.

The cube retains six octagonal fans and eight corner triangles, all source rim
loops and the distance-dependent line width. The visibility radius is the
original 24 blocks from the player, independently of camera mode. Auto-filter
hue uses the tile's ticks plus partial tick. Rotation and scale use one captured
wall-clock sample and retain Java remainder/hash arithmetic. TruncatedCube's new
light-bearing overloads leave callers using the existing raw-color overloads
unchanged. The material owns lightmap and directional-light intent separately.

Link extraction preserves the shared once-per-world-tick/partial-tick reveal
fade, held Manipulator/anchor check, smaller-identity-hash endpoint ownership,
source pulse and facing-dependent endpoints. The extracted beam list is immutable.
Each link submits its unfaded core color before its alpha/brightness-scaled
planes, both retaining lightmap 240. The plane geometry keeps whole-block sections
then the final remainder. Fully unlit callers such as the Void Monster Trap use
the context-independent ChromaBeam plane emitter with their own material.

Detached tiles and special items draw only the original raised 3x model. Item
bounds are collected from the transformed model geometry rather than a unit box.
World-bound structure previews do not query real-world connection neighbors at
stand-in coordinates.

The audit fixture generator compiles the original Java cube geometry methods
independently of the port. Client checks compare 108 facing/filter/tick/distance
cases, verify actual extraction and item/model bounds, source lightmap/color/
line widths and beam UV/alpha, immutable deferred replay, GPU compilation and
classic/improved queue ownership. Native scenes observe the placed model and
all four effect callbacks for six facing and two filter render-state fixtures
in both transparency modes. Link/facing/filter state is explicitly injected for
these scenes. This is not fluid-transfer gameplay, final pixel/shader-pack or
live inventory/handbook framebuffer acceptance.
