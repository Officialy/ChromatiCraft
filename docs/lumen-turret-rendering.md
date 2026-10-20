# Lumen Turret source rendering contract

The original RenderLumenTurret and ModelTurret were compared with the active
26.3 renderer. The seven model parts retain their original box geometry, texture
origins, pivots and rotations. Techne assigned mirror flags after constructing
the boxes; native construction therefore remains unmirrored. The body uses the
ordinary lit entity cutout profile.

A detached model uses the original scale 1.75 and Y translation .1875 before
the common model transform. It emits only the body: the original inventory pass
was -1, while sparkles belonged to world translucent pass 1.

World sparkles retain the original two atlas sprites (GLOWSECTION, CENTER),
half-sizes .125 and .09375, white color, source UV ordering and camera-facing
transform at (.5,.625,.5). Their shader is unlit with the original RGB screen
blend. They now use the existing native forward phase after terrain/classic or
after OIT composition/improved transparency, rather than entering weighted OIT.
The pose is captured before queuing work; callbacks do not retain mutable camera
orientation or block entity render state. No new phase or mixin is introduced.

Audit mesh checks cover all 168 body vertices and both complete sprite quads,
source sparkle positions and sizes, exact detached transform and replay after
camera and render-state mutation. The audit-only world observer delegates the
production renderer and requires completed body/sparkle draws in both native
transparency modes, then restores fixture blocks, support, camera and options.
Normal Play has no audit observer registration.

Final acceptance is recorded in the project ledger.

Final acceptance: native client passed in 4m16s with both turret markers and
the complete existing suite. Pinned evidence: build/d06-turret-proof.json.
D06 remains open.
