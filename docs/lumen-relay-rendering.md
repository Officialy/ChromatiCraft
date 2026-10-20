# Lumen Relay source mesh and lighting contract

This renderer ports the Lumen Relay branch of V33a RelayRenderer (ISBRH), with
BlockLumenRelay bounds and TessellatorVertexList coordinate transforms as source.
Floating relay behavior belongs to its separate renderer and is unchanged.

The original renderer explicitly writes brightness 240 for both terrain passes;
it does not supply model directional lighting. All three native draws use the
FULL_BRIGHT profile. The stone pass retains 48 vertices, including the original
middle surface and repeated top surface. The crystal pass retains 20 vertices,
white color with alpha 212, and the original crystal_outline icon returned by
CrystalTypeBlock.getIcon(0,0). The previous crystal_32 icon was incorrect.

The original pass 1 uses standard terrain transparency, with no additive blend
selection. Crystal and connectivity therefore use the native translucent/OIT
profile, not the forward screen-color phase. Connectivity uses alpha 255 and
retains the original per-world-face UV ordering and input marker selection.

Geometry retains the original asymmetric mount heights: h=.3125 for positive
mounts and .25 for negative mounts. The source vertex-list inversion and X/Y or
Y/Z coordinate swaps, including winding reversals, are reproduced exactly.
Connectivity is constructed in world axes around the original direction-dependent
center; it is not rotated as a generic box. Its half-width remains .1875.

Each submission captures immutable scalar vertices and UV coordinates. The
callbacks read neither a live block entity, reusable render state nor atlas sprite.
Native captured pose ownership is retained.

The independent audit golden resource was derived from original vertex expressions
and vertex-list transforms and contains all six mounts. Native ENTITY meshes for
all 36 mount/input combinations are compared vertex-by-vertex for positions, UVs,
white/alpha and full-bright packed light; mesh sizes are 48/20/24. Deferred replay
is compared after facing/light/render-state/camera mutation. An audit-only world
observer delegates production rendering and requires all three actual draws in
completed classic and improved-transparency frames, restoring fixture state.

Final acceptance and pinned evidence are recorded in the project ledger. D06
remains open for other source contexts and glow renderers.

## Shared particle acceptance exposed by the world fixture

The first scene run reproduced a missing OIT pipeline crash from an actual relay
ambient Blur. All five ChromaParticle layer declarations now use DragonAPI's
native custom-particle profile factory. Additive terrain and animated sheets are
recognized screen layers and go through the existing forward phase; ordinary
alpha terrain particles carry complete native OIT pipeline sets. Through-wall
particles retain their separate depth-disabled profile.

Original EntityBlurFX, EntityLaserFX and EntityGlobeFX render flags were checked:
all disable fog and directional/lightmap shading; Blur clips only additive
particles, while Laser/Globe disable alpha clipping. Those exact masks are
asserted by the audit (29 terrain screen, 21 through-wall screen, 13 animated
screen sheets, 9 ordinary alpha). This also removes the old shader's brightness-
to-coverage approximation from these particle draws. The source RGB screen law
is handled by the existing native forward particle phase instead.

The world observer deliberately emits a real relay ambient Blur in each mode;
it does not rely on random display ticks to exercise the crashing layer.

Final acceptance passed in 7m10s: build/d06-relay-acceptance-client.log.
Both relay markers and the complete existing client suite passed with exact
source particle flags and forced screen/alpha particles. Pinned proof:
build/d06-relay-proof.json.
