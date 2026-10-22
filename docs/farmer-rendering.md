# Farmer rendering source contract

The 26.3 renderer preserves V33a RenderFarmer's four BLUEFIRE UV rotations,
two RIFTHALO sides, two SPARKLE sides and final SPARKLE front quad. SPARKLE maps
to sparkle2. Facing transforms are equivalent to the original translated Y
rotations. The opaque Techne model retains its ordinary model submission.

The original renderer disables GL_LIGHTING, selects ADDITIVEDARK (RGB screen),
and supplies tessellator brightness 240. Disabling directional lighting leaves
the lightmap active: block-light 15 and sky-light 0. The old port's sprite shader
ignored these coordinates. EntityLighting.LIGHTMAP_ONLY.screen now samples the
lightmap without directional lighting and preserves source RGB screen blending.

TileEntityChromaticBase.shouldRenderInPass delegates to ChromaTiles.renderInPass1;
FARMER is absent from that registry switch. Its glow therefore belongs to pass 0
and retains depth writes. World draws use SolidModelRendering's native solid phase, at order 1 after
the body at order 0 and before world transparency. The shared item helper retains ordinary item collector ownership
and depth writes. Deferred draws capture the pose and sprite references before
submission rather than looking up a later atlas during callback execution.

FarmerClientChecks exercises all four actual registered renderer orientations
and the item helper against independently transcribed source coordinates/UVs.
It checks 36 glow vertices per case, exact green/alpha and packed light 240,
lightmap binding, shader flags, RGB screen blending, depth writes, collector
ownership, native pipeline compilation, and replay after caller/state mutation.
This mesh/pipeline check does not by itself prove final world framebuffer pixels
or a complete Farmer harvesting operation; those require separate acceptance.

Local historical sources: ChromatiCraft 5cde0068c^ Render/TESR/RenderFarmer.java,
Base/TileEntity/TileEntityChromaticBase.java, Registry/ChromaTiles.java and
Registry/ChromaIcons.java. No original recipes or gameplay behavior changed.
