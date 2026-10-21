# Monster Bait rendering source contract

V33a RenderMonsterBait draws nine horizontal cards using the
ROSEFLARE icon, radius .5, at y=-.0625 through +.0625 in .015625 steps. Each card
drifts by .0625*sin(t*.46) on X and .0625*sin(t*.78) on Z, where t is the whole
entity tick divided by 10 plus y*40. The camera-facing code in the legacy source
is commented out: these cards remain horizontal regardless of view orientation.
The renderer now captures whole ticks independently of native interpolated age.

The original per-layer colors come from EntityMonsterBait.getRenderColor(y): a
lifetime-based grey pulse when no monster is selected, or a pulse around the
selected monster's palette color. Restored the source .35F constant in the grey
pulse, preserving its float rounding. Lifespan/target gameplay is otherwise
unchanged. All nine colors are captured during extraction and cloned at submission.

The source disables directional lighting and culling, retains lightmap brightness
240 (block light 15 / sky light 0), selects RGB screen blending and disables depth
writes while retaining scene depth testing. The old port ignored the lightmap and
submitted through ordinary translucent/OIT custom geometry. The draw now uses
EntityLighting.LIGHTMAP_ONLY.screen in the owned composed-scene native phase.
That phase captures the caller pose for deferred drawing. The original roseflare
PNG has opaque alpha throughout, so zero-alpha rejection changes no source texels.

MonsterBaitClientChecks compares 112 source cases, each with all nine original
colors/drift coordinates, generated from retained original entity/helper bodies.
It performs actual factory-created entity extraction at three partial ticks for
336 native meshes, across lifetimes below/at/above the fade threshold and both
normal/targeted branches. It checks geometry, horizontal orientation, UV/color,
lightmap/blend/depth/culling, native GPU compilation, classic/improved forward
queue ownership and replay after state/entity/camera/pose mutation.
MonsterBaitSceneClientChecks observes registered production extraction and actual
nine-layer draws at full/fading lifetimes in classic/improved frames. It removes
its client-only entity and restores camera/transparency; ordinary particle
lifetimes remain in effect. These checks do not establish final framebuffer,
external shader-pack, or hostile-mob targeting gameplay acceptance.

Regenerate the source fixtures with JDK 25:

```powershell
java TestInstance/tools/MonsterBaitSourceGolden.java > TestInstance/src/main/resources/audit/monster-bait-golden.json
```

The generator retains original getBrightness/getRenderColor and color-helper
bodies. Its EntityMob record represents only the selected mob's RGB lookup input;
the native test independently verifies the real Zombie palette mapping 0x436d35.
Legacy references: ChromatiCraft 5cde0068c^ Render/Entity/RenderMonsterBait.java and
Entity/EntityMonsterBait.java, DragonAPI 08440dc4^ ReikaColorAPI/ReikaEntityHelper.
Current API reference: generated Minecraft/NeoForge 26.3.0.76 EntityType/EntityTypes.
