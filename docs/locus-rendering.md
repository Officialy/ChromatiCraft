# Locus, Aura Point and GlowKnot source rendering contract

Source references are V33a RenderLocusPoint, RenderDimensionCore, RenderAuraPoint,
GlowKnot and DragonAPI Spline.render / BlendMode. The native target is 26.3.

The animated 8-by-10 core sheet uses ADDITIVE2: SRC_ALPHA, ONE. It retains three
source sizes (1, .75, .5), vertex alpha (255, 191, 127), and the original separate
RGB attenuation by alpha/255. It retains the source's explicit brightness 240
and lightmap sampling while disabling directional lighting. This is distinct
from screen blending, which the PvP flare and knot fade use. The retained sheet
is 1024x1280 and opaque in alpha; no brightness-derived coverage is introduced.

World core geometry uses source -camera-yaw / camera-pitch and the extra roll
180+atan2(center.x-camera.x, center.z-camera.z). All three layers share that
position-based roll. The raw Aura render color drives its core; only the flare
and knot apply the source .875 saturation adjustment. The PvP flare retains
3.5+sin((ticks+partial)/32), original UV order and explicit source brightness 240.

GlowKnot captures a copied list of immutable spline positions during extraction.
The three world strokes use ordinary alpha with source widths 1, 5, 10 and alpha
255, 63, 63 for the normal opaque Aura color. Each native independent segment has
a real normalized tangent. Consecutive duplicate samples are omitted so the
native expanded-line shader never normalizes a zero vector. Closed source
interpolation already repeats its starting endpoint; no extra visible segment is
invented. The detached branch keeps its single 1.5px stroke and disabled depth
test. Callers can supply the original handbook's 2.5px stroke and +.1875 pose when
that excluded legacy guide renderer is ported; the explicit submit API retains
that context rather than dropping it.

The fade is part of the knot draw. Its original screen blend, .05 local Z, 1.25
radius, alpha 160, RGB attenuation and bottom-left-first UV order are retained.
Its world orientation uses source center-to-camera polar angles, independently
of the core camera orientation. The inherited tile light is captured for knot
strokes/fade, which have no explicit source brightness override. Source knot
updates remain six steps per world tick. A client tick owner keeps already-created
knots advancing while culled; weak tile keys and level/removal cleanup prevent
cross-world retention. Detached extraction advances one source step.

Every world draw belongs to DragonAPI's native forward phase, after terrain in
classic transparency and after OIT composition with improved transparency.
Source depth writes are disabled. Dimension Core/Aura core and world knot depth
tests are disabled during the monument ritual; ordinary cores are occluded by
terrain. Dimension Core post effects retain the original LOS gate outside the
ritual. The former unconditional through-wall behavior was not source behavior.

The Dimension Core special item retains native item collector ownership and the
source 45-degree Y / -30-degree X rotations, recentered for the native item model.
The full detached TESR helper separately preserves source offsets. Native GUI
items draw to an atlas before a premultiplied-alpha blit: sending additive core
RGB through that default blit would darken the backdrop. Narrow client Mixins
select an RGB ONE/ONE atlas blit only when the baked item identity contains a
DimensionCoreItemRenderer. Oversized items use the same rule. All other models
retain their native blit. No global shader replacement, luminance-to-alpha
conversion or unrelated item state mutation is used. The core's wall-clock
animation marks its native model state animated, invalidating cached atlas cells.

Six world pipelines cover the three source blend equations with ordinary/ritual
depth tests; a separate GUI additive blit pipeline preserves the final emitted RGB.
Native acceptance checks compare 1536 source core meshes, deterministic closed
knot/fade geometry, scalar UV/color/light captures, real special items and replay
after state reuse. GUI checks resolve all sixteen actual models, draw them into
an owned native GPU atlas, inspect transformed normal/oversized blits, and check a
foreign vanilla model. Scene observers require fixture-specific completed Aura
and Dimension Core draws under both transparency modes, with PvP on/off and
injected ritual render-state branches. They do not claim to run the full monument
ritual. Disposable fixture blocks, camera, options and owned GUI resources restore.

Final native acceptance passed in 6m48s: build/d06-locus-final-client.log. All
three Locus markers, preceding client regressions and shutdown cleanup passed.
The fixtures exposed and fixed ARGB alpha carry-over in explicit core/fade alpha.
Seven new GPU pipelines compiled; normal/oversized GUI Mixins were exercised.
Pinned proof, originals, native runtime manifest, source/asset hashes and water
pixel regressions: build/d06-locus-proof.json. D06 remains open.
