#version 330
#extension GL_ARB_separate_shader_objects : require

#include <minecraft:globals.glsl>

const int MAX_LOCUS_POINTS = 64;

layout(std140) uniform LocusPoints {
    ivec4 PointCount;
    vec4 Focus[MAX_LOCUS_POINTS]; // xy screen UV, z squared viewer distance, w intensity
    vec4 Params[MAX_LOCUS_POINTS]; // rgb core colour, w > 0 core scale; w < 0 Aura Locus
};

uniform sampler2D InSampler;

layout(location = 0) in vec2 texCoord;
layout(location = 0) out vec4 fragColor;

vec3 rgb2hsb(vec3 c) {
    vec4 K = vec4(0.0, -1.0 / 3.0, 2.0 / 3.0, -1.0);
    vec4 p = mix(vec4(c.bg, K.wz), vec4(c.gb, K.xy), step(c.b, c.g));
    vec4 q = mix(vec4(p.xyw, c.r), vec4(c.r, p.yzx), step(p.x, c.r));
    float d = q.x - min(q.w, q.y);
    float e = 1.0e-10;
    return vec3(abs(q.z + (q.w - q.y) / (6.0 * d + e)), d / (q.x + e), q.x);
}

vec3 hsb2rgb(vec3 c) {
    vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
    vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
    return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
}

float noiseHash(vec2 point) {
    return fract(sin(dot(point, vec2(127.1, 311.7))) * 43758.5453);
}

float auraNoise(vec2 point) {
    vec2 cell = floor(point);
    vec2 blend = fract(point);
    blend = blend * blend * (3.0 - 2.0 * blend);
    float low = mix(noiseHash(cell), noiseHash(cell + vec2(1.0, 0.0)), blend.x);
    float high = mix(noiseHash(cell + vec2(0.0, 1.0)), noiseHash(cell + vec2(1.0)), blend.x);
    return mix(low, high, blend.y) * 2.0 - 1.0;
}

void main() {
    vec2 sampleUV = texCoord;
    float aspect = ScreenSize.y / ScreenSize.x;

    // The old shader registry ran one framebuffer pass per compound focus. Applying the UV pulls
    // in order recreates that composition in a single modern pass without reading/writing main.
    for (int i = 0; i < PointCount.x && i < MAX_LOCUS_POINTS; i++) {
        vec4 focus = Focus[i];
        vec4 params = Params[i];
        vec2 delta = focus.xy - sampleUV;
        delta.y *= aspect;
        float distv = dot(delta, delta);
        if (params.w > 0.0) {
            float scaleFactor = params.w;
            float vertexFalloff = clamp(3.5 - 40.0 * distv * focus.z, 0.0, 1.0);
            float f0 = max(focus.w * 0.5, focus.w - scaleFactor * 0.15);
            sampleUV = mix(sampleUV, focus.xy, f0 * vertexFalloff / 7.5);
        }
        else if (params.w > -1.5) {
            float vertexFalloff = clamp(3.0 - 400.0 * distv, 0.0, 1.0);
            sampleUV = mix(sampleUV, focus.xy, focus.w * vertexFalloff / 4.0);
        }
    }

    vec4 color = texture(InSampler, clamp(sampleUV, vec2(0.0), vec2(1.0)));

    for (int i = 0; i < PointCount.x && i < MAX_LOCUS_POINTS; i++) {
        vec4 focus = Focus[i];
        vec4 params = Params[i];
        vec2 delta = focus.xy - texCoord;
        delta.y *= aspect;
        float distv = dot(delta, delta);
        if (params.w > 0.0) {
            // V33a dimcore.frag, including its hollow centre and scale swell during the monument.
            float scaleFactor = params.w;
            float safeDistance = max(0.000001, distv * focus.z);
            float colorFalloff = max(0.0,
                    clamp(1.0 - 0.6 * distv * focus.z / scaleFactor, 0.0, 1.0)
                    - min(1.0, 0.04 / safeDistance));
            float f0 = max(focus.w * 0.5, focus.w - scaleFactor * 0.15);
            color.rgb += params.rgb * (f0 * colorFalloff * 0.55);
        }
        else if (params.w > -1.5) {
            // V33a auraloc.frag: tighten saturation around the point and pinch the centre.
            float colorFalloff = clamp(2.0 - 50.0 * distv, 0.0, 1.0);
            float amount = max(0.0, focus.w * colorFalloff - 0.01);
            vec3 hsb = rgb2hsb(color.rgb);
            hsb.y = min(1.0, hsb.y * 1.5);
            color.rgb = mix(color.rgb, hsb2rgb(hsb), amount);
        }
        else if (params.w > -2.5) {
            // V33a pylon.frag: luminance/element-colour wash plus a hue-preserving centre grade.
            vec3 pylon = rgb2hsb(params.rgb);
            vec3 hsl = rgb2hsb(clamp(color.rgb * (vec3(1.0) + params.rgb * focus.w / 2.5), 0.0, 1.0));
            vec3 shifted = hsb2rgb(vec3(pylon.x, (pylon.y + hsl.y) * 0.5, hsl.z));
            float brightness = dot(color.rgb, vec3(0.2126, 0.7152, 0.0722));
            shifted = mix(mix(vec3(brightness), params.rgb, 0.5), shifted,
                    min(1.0, pylon.y * 2.5 + 0.125));
            color.rgb = mix(color.rgb, shifted, clamp(focus.w, 0.0, 1.0));
            vec2 edge = abs(texCoord * 2.0 - 1.0);
            float interior = 1.0 - min(1.0, pow(pow(edge.x, 8.0) + pow(edge.y, 8.0), 0.125));
            float distortion = 1.0 + 0.25 * auraNoise(texCoord * vec2(ScreenSize.x / ScreenSize.y, 1.0) * 150.0);
            float haze = max(0.0, focus.z - (interior * 7.2 + 0.125) * distortion);
            float pixelSize = min(8.0, 1.0 / max(0.01, focus.z));
            vec2 pixel = floor(texCoord * ScreenSize / pixelSize + 0.5) * pixelSize / ScreenSize;
            vec2 direction = (vec2(0.5) - pixel) * 2.0;
            float time = float(PointCount.y) * 0.001 + 5.0;
            vec2 drift = direction * time * 0.12;
            float noise = auraNoise(pixel * ScreenSize * 0.09 + drift);
            haze = clamp(haze * (0.5 + 0.5 * min(1.0, noise * noise * noise * 3.0)), 0.0, 1.0);
            color.rgb = min(vec3(1.0), color.rgb + params.rgb * haze);
        }
        else {
            vec3 wash = mix(vec3(1.0), params.rgb, 0.35);
            color.rgb = mix(color.rgb, wash, clamp(focus.w * 0.8, 0.0, 1.0));
        }
    }

    fragColor = color;
}
