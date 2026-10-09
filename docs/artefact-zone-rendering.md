# Unknown Artefact zone rendering — Minecraft 26.3

The owner reported animated sharp outlines and mottled terrain, and explicitly requested
keeping the original effect with rendering fixes. V33a deliberately applies an animated
acid-green Sobel/noise overlay while the player occupies an artefact-ring chunk. This
repair preserves the original edge/noise formula, colour, timing, ramp-up and fade rates,
ten-tick signal lifetime, artefact spawning and gameplay. It adds no intensity setting
or arbitrary attenuation.

The port fed the shader `ReikaWorldHelper.getSunIntensity`. That helper converts the
clock angle to radians, then multiplies the radians by 2π again inside the cosine,
producing false day/night brightness cycles. The original shader used its world's sun
brightness. The 26.3 host now samples the camera's interpolated
`EnvironmentAttributes.SKY_LIGHT_FACTOR`, exactly as `LightmapRenderStateExtractor`
does. The actual 26.3 timeline and weather layers supply daylight, night, rain, thunder,
biome/dimension and datapack values. This avoids reconstructing an obsolete world-provider
clock and keeps shader brightness aligned with the rendered scene. Native 26.3 night
brightness is 0.24 rather than the old provider's 0.2; the shader's authored formula stays intact.

The blur/Sobel texel offsets and noise coordinate scaling now use
`textureSize(InSampler, 0)`, preserving the original one-source-pixel sampling even if
the postprocessing source differs from the global screen dimensions. The zone signal
also records its actual world identity. Another world's clock, a clock rollback or an
expired signal cannot keep the effect active; switching worlds clears the old envelope.
The original same-world fade remains intact.

`TestInstance - Automated Checks` runs a native client fixture after resource reload.
It verifies the host brightness against the camera's actual attribute probe, signal
world ownership, rollback, inclusive ten-tick expiry and reset. It submits the real UA
pipeline into a 224×112 source/output pair, independently of the window size, and reads
back four GPU frames: zero intensity, full daylight, full night and a later animation
time. Zero intensity preserves source colours, flat regions gain neither sharpening nor
noise, and the animated green edge effect remains stronger at night. It also temporarily
arms the actual AfterLevel/frame-graph/blit path in the disposable audit world, restoring
the prior envelope afterwards. Tests rotate uniform buffers and destroy their temporary
render targets. These fixtures are called only behind `dragonapi.auditClient` and remain
inactive in normal Play.

Validation passed in `build/artefact-zone-client.log` (1m 44s). Native measurements were
404762 summed green-channel gain in daylight, 583520 at night, and 3584 pixels changed
between animation times. The previous turbine, gear, railgun, water, dropper, sound and
DragonAPI client checks also passed. The packaged shader matches the authored resource;
normal Play argument hashes remain unchanged and automated checks remain disabled there.

The native client check does not certify the owner's exact external shader pack and
Distant Horizons configuration or the visual appearance of the reported scene. Restart
normal Play to load the Java host changes; the original moving green tracery remains
expected inside artefact zones. The shared DragonAPI helper's remaining climate/Cliffs
consumers need their own source/behavior review (backlog D63); this shader no longer uses it.
