# Void Monster Trap rendering contract (26.3)

The V33a `Render/TESR/RenderVoidMonsterTrap.java` and `ChromaFX.renderBeam` are the
source references. The renderer keeps their RGB screen blend, fully unlit sprite
and core materials, scene depth testing, disabled depth writes, and double-sided
geometry. World geometry uses DragonAPI's composed-scene phase; item and structure
preview geometry belongs to its calling preview collector.

The outer translate(0,1,1) and scale(1,-1,-1) apply to both glows and tethers.
World glows use source camera yaw/pitch Euler rotations, without camera roll.
Structure previews keep source renderRY/-renderRX, world icon sizes, .4375 scale
and the additional .625 layer multiplier. They retain tile ticks plus partial
rather than replacing them with wall-clock time. Detached items use the overworld
icons, item sizes, -45/-30 orientation and .5 scale. The animation clock is sampled
once per extraction or item submission; deferred callbacks do not read live state.
The Nether preview override selects icons independently of the actual tile's
Nether flag, which controls the original rotating layer.

Tethers retain three splines, their original point order, whole-tick color phase,
per-segment .4 and per-spline .9 phase increments, and double radius
`.25 + .5 * (1 - distance / 16)`, including zero and negative values. Local endpoint
coordinates and width axes pass through the original outer transform. Each beam
has its screen-blended core before its planes. Planes cover each whole block and
then the final fraction, with the original Y/X/Z order and UVs; zero-length beams
emit no planes. `ChromaBeam.planes` exposes this existing geometry independently
of material/phase selection. Its Fluid Relay submission behavior is unchanged.

The client audit checks 120 context/time/flash cases, actual registered renderer
extraction, a real three-spline tether, world-bound preview flags and clocks,
all glow vertices/colors/UVs, signed-width beam geometry, deferred replay and
native classic/improved queue ownership. Six placed-trap scenes execute the three
glow families and a remainder beam in both transparency modes. The structured and
Nether scenes inject render-state fixtures; they do not construct the ritual or
prove its gameplay. Item/preview coverage is mesh/collector coverage, not a live
handbook or inventory framebuffer comparison. Final pixels, shader packs, trap
LOS post-processing, and new dedicated-server/gameplay acceptance remain outside
this rendering check.
