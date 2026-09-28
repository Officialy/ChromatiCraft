#version 330
#extension GL_ARB_separate_shader_objects : require

// V33a Shaders/artealloy/shock.frag then Shaders/artealloy/glow.frag, the two ARTEALLOY registry passes
// run in order in one pass (GLOW only recolours, so sampling SHOCK's output at the same pixel is exact).
// DragonAPI's lib_math distsq and lib_color rgb2hsb/hsb2rgb are inlined; getScreenPos(0,0,0), the
// projected Memory Crystal, arrives in the UBO.

#include <minecraft:globals.glsl>

layout(std140) uniform Artealloy {
    vec4 Glow;   // xy focus screen UV, z intensity, w squared viewer distance
    vec4 Shock;  // xy focus screen UV, z intensity, w ring size
    vec4 Params; // x time (player ticks), y ring thickness
};

uniform sampler2D InSampler;

layout(location = 0) in vec2 texCoord;
layout(location = 0) out vec4 fragColor;

float distsq(vec2 a, vec2 b) {
    float f = ScreenSize.y / ScreenSize.x;
    float dx = a.x - b.x;
    float dy = (a.y - b.y) * f;
    return dx * dx + dy * dy;
}

vec3 rgb2hsb(vec3 c) {
    vec4 K = vec4(0.0, -1.0 / 3.0, 2.0 / 3.0, -1.0);
    vec4 p = mix(vec4(c.bg, K.wz), vec4(c.gb, K.xy), step(c.b, c.g));
    vec4 q = mix(vec4(p.xyw, c.r), vec4(c.r, p.yzx), step(p.x, c.r));
    float d = q.x - min(q.w, q.y);
    float e = 1.0e-10;
    return vec3(abs(q.z + (q.w - q.y) / (6.0 * d + e)), d / (q.x + e), q.x);
}

// Function from Inigo Quilez, https://www.shadertoy.com/view/MsS3Wc
vec3 hsb2rgb(vec3 c) {
    vec3 rgb = clamp(abs(mod(c.x * 6.0 + vec3(0.0, 4.0, 2.0), 6.0) - 3.0) - 1.0, 0.0, 1.0);
    rgb = rgb * rgb * (3.0 - 2.0 * rgb);
    return c.z * mix(vec3(1.0), rgb, c.y);
}

vec4 shock(vec2 texcoord) {
    float intensity = Shock.z;
    vec2 coreXY = Shock.xy;
    float distv = distsq(coreXY, texcoord);
    float rdist = abs(distv - Shock.w);
    float fr = max(0.0, 1.0 - 0.75 * rdist / Params.y);
    float f = intensity * fr * 0.8;
    vec2 offset = texcoord - coreXY;
    vec2 distorted = texcoord - offset;
    vec2 texUV = mix(texcoord, distorted, f);
    vec4 color = texture(InSampler, texUV);
    vec3 hsb = rgb2hsb(color.rgb);
    hsb.y = 0.0;
    hsb.z = min(1.0, hsb.z * 1.5);
    color.rgb = mix(color.rgb, hsb2rgb(hsb), f);
    return color;
}

vec4 glow(vec2 texcoord, vec4 color) {
    float intensity = Glow.z;
    float time = Params.x;
    vec2 nodeXY = Glow.xy;
    float distv = distsq(nodeXY, texcoord);
    float d = 0.9 + 0.2 * sin(time * 0.043) + 0.04 * sin(time * 0.293);
    float distfac_color = max(0.0, min(1.25, d - 0.045 * distv * Glow.w));
    float cf = intensity * distfac_color * 0.8;
    vec3 hsb = rgb2hsb(color.rgb);
    // range from 20 to 45 degrees = 0.0555 to 0.125
    hsb.x = 0.09025 + 0.03475 * sin(time * 0.067) + 0.0041 * sin(time * 0.213) + 0.0019 * sin(time * 0.471);
    hsb.y = 1.0;
    float z1 = min(1.0, hsb.z * 1.5 + 0.375);
    float z2 = min(1.0, hsb.z * 1.75 + 0.5);
    hsb.z = mix(z1, z2, 0.5 + 0.5 * sin(time * 0.114));
    vec3 c2 = hsb2rgb(hsb);
    color.rgb = mix(color.rgb, c2, cf);
    return color;
}

void main() {
    vec4 color = Shock.z > 0.0 ? shock(texCoord) : texture(InSampler, texCoord);
    if (Glow.z > 0.0)
        color = glow(texCoord, color);
    fragColor = color;
}
