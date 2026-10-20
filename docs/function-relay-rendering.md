# Function Relay source transform and screen-blend contract

V33a RenderFunctionRelay draws a single CELLFLARE quad with ADDITIVEDARK, disabled
lightmap and directional lighting, no depth writes and no culling. The retained
sprite is fully opaque in alpha across every animation frame; RGB carries its
black background and flare detail. The active native sprite pipeline uses that
raw texture/color with the source RGB screen equation, depth test and no writes.
World draws use the existing native forward screen phase after terrain/classic
or after OIT composition/improved transparency. Item draws remain owned by the
native special-item collector.

World geometry exactly retains the original matrix sequence: translate (0,1,1),
scale (1,-1,-1), translate (.5,.5,.5), scale .875, rotate by camera yaw about Y,
then pitch about X. The previous quaternion/negative-Y transform differed from
the source. Native CameraRenderState's explicit yaw/pitch values replace the old
RenderManager fields; no live camera is retained by the draw callback.

Item geometry keeps the already-correct center/flip, -45 degree Y and -30 degree
X rotations, and scale 1.125 (source .75 times 1.5). The detached BER now chooses
this same item transform, with extraction-time clock color. Actual special-item
rendering still delegates through FunctionRelayItemRenderer.

The four source colors and 25-tick transitions remain unchanged. Existing signed
time normalization remains safe for long-running counters and negative hashes.
Each callback captures scalar UV endpoints and color; the native submission owns
its copied pose. No atlas sprite, reusable render state, clock or camera is read
at replay time.

Native mesh checks compare six yaw/pitch cases against analytic source matrix
coordinates, plus exact raw UVs/color, phase/pipeline, signed color-cycle anchors,
actual special-item and detached item meshes, and replay after caller pose,
camera and render-state mutation. An audit-only observer delegates production
world rendering and requires a completed forward sprite draw in each native
transparency mode. Its fixture/support/camera/options are restored afterward.

Final acceptance is recorded in the project ledger. D06 remains open.

The first run stopped before these checks in the existing file-input resource-
reload audit. Its 600-frame timeout varied with rendering speed while the reload
future remained pending. That audit now uses a monotonic per-phase deadline:
120 seconds for reload and 30 seconds for capture/decoder completion. All resource
content, ownership, cleanup and error-path assertions remain required. The final
full run includes this correction; no acceptance check is skipped.

Scope clarification: the original Function Relay explicitly disables directional
lighting, the lightmap and depth writes. It does not itself toggle fog; the prior
candidate ledger note claiming an explicit source fog disable was inaccurate.
Inherited fog-context review remains part of the broader open D06 work.

Final full native acceptance passed in 4m22s, including file-input completion,
both Function Relay markers and all preceding client/shutdown checks. Evidence:
build/d06-function-relay-proof.json. Native Play argument hashes unchanged.
