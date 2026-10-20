# Adjacency source rendering contract

The V33a AdjacencyUpgradeRenderer and all sixteen AdjacencyUpgrades color pairs
are the source reference. World rendering belongs entirely to pass 1. Four-pixel
opaque rings retain their lightmap while directional lighting and texture are
disabled. A custom 26.3 native line vertex format carries position, color, normal,
line width and packed light; its shader extends the native line shader with the
lightmap sample. No screen blend is applied to ring colors.

World rings and the trimmed glow submit through DragonAPI's native forward phase
after terrain in classic transparency and after composition in improved
transparency. Both test depth without writing it. Item rings and crossed glows
keep native item collector ownership and write depth. The glow uses lightmap-only
screen blending, without culling. Item caller light is retained.

The depth distinction comes from the complete legacy caller, not the absence of
a depth-mask statement in the tile renderer. The official Forge 1.7.10
[EntityRenderer patch](https://github.com/MinecraftForge/MinecraftForge/blob/1.7.10/patches/minecraft/net/minecraft/client/renderer/EntityRenderer.java.patch)
inserts pass-1 entities before depthMask(true). Read-only JDK javap disassembly of
the locally installed vanilla client confirms that the entity/block-entity loop
and tile dispatcher do not restore that mask. The jar SHA-1 matches its installed
version manifest (e80d9b3bf5085002218d4be59e668bac718abbc6). This also corrects the
Relay Source crystal profile, which inherits the same disabled world depth writes.
The evidence uses JDK static disassembly; it is not REA Evidence.

Animation preserves the original millisecond clock plus position seed, tier
speed, remainder arithmetic, sequential X/Y/Z rotations and three independent
triangular pulses. Redstone stopping divides the clock by six, slows only the
first pulse further and uses greyscale. World rings have source radii .25,
.28125 and .3125; items scale the rings by 1.5. World glow keeps the original
root flip and explicit camera yaw/pitch; items keep the two original crossed
transforms. Each glow trims one sixteenth from every sprite edge.

Extraction captures one immutable animation record. Deferred geometry retains
scalar UV, color, light and copied poses. No shared sprite or render-state data
is read by the queued callbacks.

Acceptance checks include 288 independent numeric animation fixtures, expanded
native line meshes and glow vertices, source matrices/UV/color/light/width,
item typed tier and caller light, and replay after state/pose reuse. The native
scene observer is restricted to the exact disposable fixture position and
requires all four draws in completed running/stopped frames under classic and
improved transparency. Fixture blocks, camera and options are restored.

Full native acceptance passed in 8m40s: build/d06-adjacency-client-rerun.log.
Both adjacency markers, all preceding client checks, all 24 existing item models
and shutdown cleanup passed. Pinned numeric/source/legacy-caller evidence and
water pixel regressions are recorded in build/d06-adjacency-proof.json.
D06 remains open; next is the shared Aura Point/Dimension Core/Locus cluster.
