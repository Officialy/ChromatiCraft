# Colored Altar rendering source contract

The 26.3 renderer retains the original 21 metal parts, 24 crystal parts and
twelve random orbits. Metal uses the native solid phase with ordinary alpha
blending and depth writes. Crystals use the composed-scene phase with ordinary
alpha and no depth writes in the world. Both inherit the supplied lightmap;
neither uses directional lighting or an alpha cutout. Item and structure-preview
model portions retain depth writes and bypass world distance culling.

The original LODModelPart pixel-volume thresholds and render-distance multiplier
apply separately to each model part. The edit-shortcut modifier bypasses that
culling. The player's position determines distance, independently of the camera.

Orbit trails use whole render ticks, the original Orbit four-argument visual
method, and the original particle-setting/distance step. At a step of 16 the
source loop produces tails -24 and -8 without a head: no extra head is appended.
Each selected orbit captures its angles and then advances them once per visible
extraction. Submission does not advance animation. A head draws each of its six
faces and eight corners immediately followed by its outline, because ordinary
alpha blending makes this order observable. Cubes retain packed light 240,
distance-dependent outline width, source fill fade and edge alpha.

Items retain the original shared, fixed random time offset and authored item
transforms, rebased into native block-item coordinates. Bounds derive from the
actual transformed model. World bounds include the maximum source orbit
excursion and rotated cube radius. The altar particle sprite is stitched by the
atlas datagen provider from the existing entity texture.

ColoredAltarSourceGolden.java transcribes the historical Java cube, Orbit and
model/LOD inputs without importing production code. Its fixture covers 210 LOD
cases, 30 orbit cases, motion and synthetic color strips. ColoredAltarClientChecks
uses 90 real extractions, checks source vertex buffers, material/lightmap and
phase ownership, and verifies all 16 item colors and authored part coordinates.
The synthetic strips test color interpolation and renderer plumbing; they do not
claim to reproduce random ColorVariance generation. ColoredAltarSceneClientChecks
observes real placed-altar callbacks in all three particle settings with classic
and improved transparency. These checks do not establish framebuffer pixel or
shader-pack parity, nor altar discharge/collapse gameplay acceptance.

Historical sources: ChromatiCraft 5cde0068c^ Render/TESR/RenderColoredAltar.java,
Models/ModelColoredAltar.java, Render/Item/AltarItemRenderer.java and
Base/ChromaRenderBase.java; DragonAPI 08440dc4^ Instantiable/Orbit.java,
Instantiable/Effects/TruncatedCube.java and Instantiable/Rendering/LODModelPart.java.
The unused physical-time Orbit overload remains a separate follow-up: its
inherited recursive body must not be used to change the visual orbit contract.
