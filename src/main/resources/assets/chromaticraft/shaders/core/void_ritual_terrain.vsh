#version 330
#extension GL_ARB_separate_shader_objects : require

#include <minecraft:fog.glsl>
#include <minecraft:globals.glsl>
#include <minecraft:projection.glsl>
#include <minecraft:sample_lightmap.glsl>
#include <minecraft:terrainglobals.glsl>
#ifndef MULTIDRAW_TERRAIN
    #include <minecraft:chunksection.glsl>
#endif

layout(location = 0) in vec3 Position;
layout(location = 1) in vec4 Color;
layout(location = 2) in vec2 UV0;
layout(location = 3) in ivec2 UV2;
#ifdef MULTIDRAW_TERRAIN
layout(location = 4) in ivec3 ChunkPosition;
layout(location = 5) in float ChunkVisibility;
#endif

#ifndef OIT_ALPHA_ONLY
uniform sampler2D Sampler2;
#endif

layout(location = 0) out float sphericalVertexDistance;
layout(location = 1) out float cylindricalVertexDistance;
layout(location = 2) out vec4 vertexColor;
layout(location = 3) out vec2 texCoord0;
layout(location = 4) out float chunkVisibility;

layout(std140) uniform RitualTerrain { vec4 RitualFocus; vec4 Wave; vec4 CurlStretch; vec4 Application; };
void main() {
    vec3 pos = Position + (ChunkPosition - CameraBlockPos) + CameraOffset;
    vec3 diff = pos-RitualFocus.xyz;
    float distanceXZ=length(diff.xz),intensity=RitualFocus.w;
    float waveFraction=max(0.0,1.0-abs(distanceXZ-Wave.x)/max(0.00001,Wave.y));
    pos.y+=Wave.z*intensity*waveFraction+CurlStretch.y*distanceXZ*intensity;
    pos.xz=mix(pos.xz,RitualFocus.xz,CurlStretch.x*intensity);
    diff=(pos-RitualFocus.xyz)*mix(1.0,CurlStretch.z,intensity*max(0.0,1.0-distanceXZ/max(0.00001,CurlStretch.w)));
    pos.xz=mix(pos.xz,(RitualFocus.xyz+diff).xz,Application.x);
    pos.y-=(CurlStretch.z-1.0)*Application.x;
    gl_Position = ProjMat * ModelViewMat * vec4(pos, 1.0);

    sphericalVertexDistance = fog_spherical_distance(pos);
    cylindricalVertexDistance = fog_cylindrical_distance(pos);
    #ifndef OIT_ALPHA_ONLY
    vertexColor = Color * min(sample_lightmap(Sampler2, UV2)*1.5+vec4(0.25),vec4(1.0));
    #else
    vertexColor = Color;
    #endif
    texCoord0 = UV0;

    const float chunkFullyVisibleRange = 16.0;
    float dist = length(pos);
    chunkVisibility = mix(1.0, ChunkVisibility, clamp((dist - chunkFullyVisibleRange) / chunkFullyVisibleRange, 0.0, 1.0));
}
