# Relay Source source rendering contract

The original RenderRelaySource, ModelRelaySource and prepareGeoDraw(false) helper
were compared with the active renderer. All fourteen model parts retain source
boxes, UV origins, pivots and rotations. Legacy mirror assignments occurred after
box construction, so the baked native geometry remains unmirrored. Body and edge
parts now draw in the explicit source order. The retained relay_source.png is
byte-identical to the original receiver.png.

The eleven-part body remains normally lit cutout geometry (264 vertices). The
three edge bands are 72 untextured, unlit, opaque color vertices with depth writes:
prepareGeoDraw(false) disables texture and lighting, rather than selecting screen
blending. The previous textured additive edge path was incorrect. Its native
POSITION_COLOR pipeline is registered separately. World edge color retains the
source tick-based palette; the actual special item uses the source tick-zero
palette instead of forcing white. It keeps item collector ownership.

Stored-energy crystals use the original crystal_outline sprite. Each crystal
retains four side quads and all eight cap triangles from the two original fans.
The fans are emitted as native quads with a degenerate second triangle, retaining
all positions, UVs, winding and coverage (48 native vertices). The previous port
omitted half of each tip and assigned generic UVs. Orbit/spin use t % 360, bobbing
uses full time, and element spacing retains original integer division 360/count,
including the nonuniform 7- and 16-element source rings.

Crystals use fully unlit, culled screen blending with the inherited depth-write
state. Enhanced caustics use the source nine quads and per-corner UVs (36 vertices),
fully unlit screen blending and explicitly disabled depth writes. Both submit
through the existing native forward screen phase after terrain/classic or after
OIT composition/improved transparency. Their images are opaque in alpha; no RGB
brightness-to-coverage approximation is needed. Deferred geometry retains scalar
UVs and immutable crystal records, not mutable sprites or reusable state lists.
Detached rendering emits the body and edge model without enhanced world effects;
structure-preview extraction retains the source effects branch.

Independent numerical goldens derived from original vertex expressions cover all
48 crystal and 36 caustic vertices. Native checks compare 0/1/7/16 element rings at
five clock positions including modulo boundaries; source orbit/bob/tips/UV/color,
all model vertex counts, opaque/no-texture edge state, deferred replay after state
reuse, production special-item baking/color/ownership and detached gating. An
audit-only observer delegates production submissions and requires actual body,
edge, seven complete crystal shells and optional caustic callbacks in completed
normal/enhanced world frames in both native transparency modes. Fixture cells,
support, camera and options are restored.

Final acceptance passed in 3m52s: build/d06-relay-source-client.log. Both Relay
Source markers and the complete preceding client/shutdown suite passed. D06
remains open; pinned evidence is recorded in the project ledger.

Pinned proof: build/d06-relay-source-proof.json. Twelve water PNG means were
independently reproduced; the existing 9-byte composition tolerance passed in
both modes, with zero opaque occlusion difference. Native Play hashes unchanged.
