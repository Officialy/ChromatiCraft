# Personal Charger source rendering contract

V33a RenderPersonalCharger draws either one inactive CENTER layer or three active
layers (CENTER, ROSES_WHITE, BIGFLARE). Its phase is exactly
(tile.hashCode() + milliseconds / 5000D) % 360, with Java remainder and the source
radian sine harmonics: .875 + .25 sin(t) + .125 sin(4t) + .0625 sin(16t).
The old port used the position hash without remainder. The last layer is scaled
by .75 in every branch: the inactive single layer therefore has half-width .375,
not .5. Last layers are white; earlier layers use the source tile/preview color.

World layers translate to block center, scale uniformly, then rotate by negative
camera yaw and positive pitch. Source Z offsets -.005*i are inside the scale and
camera rotation, rather than unscaled offsets in billboard space. Source UVs
start at (u0,v0) on the (-1,-1) corner; the previous port inverted V.

World draws use the existing native forward RGB screen phase after terrain in
classic mode or after OIT composition in improved transparency. Their fully
unlit profile bypasses directional lighting and the lightmap, tests scene depth
and does not write it. All retained layer images have opaque alpha; their RGB
content needs no brightness-to-coverage approximation. Deferred callbacks own
scalar UVs, color, scale/offset and native copied poses, not live sprites or state.

Source synthetic structure previews force all three active layers, counter-rotate
by the captured preview angles and cycle element colors every four seconds.
Real-data previews keep the actual tile color. The modern StructureRenderer now
exposes that real/synthetic distinction and restores prior flags and rotation
values after nested stand-in extraction, including failures. Personal Charger
captures those values during extraction and does not read global preview state
from deferred callbacks.

Source item rendering disables directional lighting while retaining the lightmap
and depth writes. EntityLighting screen profiles now expose an independent depth-
write option; the existing two-argument method keeps its original no-write default.
All 16 lighting/cull/depth declarations are registered. The actual special-item
renderer passes its native packed light, preserves its existing center and
45-degree Y / -45-degree X orientation, and reports extents for all four actual
rotated vertices. Detached BER drawing follows the source origin-centered item
branch. Both use the .875 ROSES layer and remain owned by the item collector.

Native checks cover pulse samples (including negative phases), extraction-time
tile-hash/remainder capture, active/inactive matrices, layered sizes/depth/UV/color,
caller pose/camera/state mutation, real/synthetic and actual nested preview
extraction, exception restoration, special-item light/bounds and detached geometry.
All 16 screen profiles are GPU-compiled. Audit-only observers delegate production
rendering and require every layer in inactive/active completed world frames in
both native transparency modes, then restore fixture/support/camera/options.

Final acceptance is recorded in the project ledger; D06 remains open.

Final native acceptance passed in 4m02s with both Personal Charger markers and
the full preceding suite; DragonAPI 260 unit cases reran clean. Pinned evidence:
build/d06-personal-proof.json. Native Play argument hashes remain unchanged.
