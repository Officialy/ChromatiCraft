#version 330

#moj_import <minecraft:globals.glsl>

// The monument completion ritual's screen effect. Ported from V33a's Shaders/monument/general.frag
// and chords.frag, which were two shaders and are one pass here.
//
// general.frag was a post effect already: it pushes saturation and lifts brightness by a scalar
// "intensity", and that half is carried unchanged.
//
// chords.frag was not. It was a terrain shader, run over chunk geometry with the sixteen core
// positions and colours as uniform arrays, adding a glow around each in world space. 26.2 has no
// equivalent seat -- replacing the chunk pipeline to reach it would be a far larger change than the
// effect is worth -- so the glow is applied here instead, with the CPU projecting each core to screen
// space exactly as RotaryCraft's heat ripple projects its emitters. The falloff is therefore measured
// in screen space scaled by distance rather than in world XZ. That is the one real difference, and it
// is not visible in practice: the cores stand in a ring around a viewer who is always outside it, so
// their screen separation tracks their world separation closely.

const int MAX_CORES = 16;

layout(std140) uniform MonumentCores {
    ivec4 CoreCount;               // .x = number of populated entries
    vec4 Intensity;                // .x = general.frag's intensity, 0 when the ritual is not running
    vec4 Focus[MAX_CORES];         // .xy = screen UV, .z = squared distance to viewer, .w = alpha
    vec4 CoreColor[MAX_CORES];     // .rgb = the element's colour
};

uniform sampler2D InSampler;

in vec2 texCoord;
out vec4 fragColor;

// V33a lib_color: perceptual brightness, not the arithmetic mean.
float getVisualBrightness(vec3 rgb) {
    return dot(rgb, vec3(0.299, 0.587, 0.114));
}

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

// V33a chords.frag addGlow, with the distance measured on screen and scaled by the core's own
// distance to the viewer, so a nearer core glows across more of the screen for the same world radius.
vec4 addGlow(vec4 base, vec2 uv, vec4 focus, vec3 color) {
    float aspect = ScreenSize.x / max(1.0, ScreenSize.y);
    vec2 diff = vec2((uv.x - focus.x) * aspect, uv.y - focus.y);
    float screenDist = length(diff) * sqrt(max(1.0, focus.z));
    float f = clamp(focus.w - 0.1 * pow(max(0.0, screenDist - 2.5 * focus.w), 0.85), 0.0, 1.0);
    base.rgb = min(vec3(1.0), base.rgb + f * color);
    base.a = max(base.a, f);
    return base;
}

void main() {
    vec4 color = texture(InSampler, texCoord);
    float intensity = Intensity.x;

    // general.frag: saturation up, brightness lifted by its own greyscale so lit areas bloom hardest.
    float gs = getVisualBrightness(color.rgb);
    vec3 hsb = rgb2hsb(color.rgb);
    hsb.z = min(1.0, hsb.z * (gs + max(0.0, hsb.y - 0.5) * 0.5) * 2.0);
    hsb.y = min(1.0, hsb.y * 1.25);
    vec3 res = mix(color.rgb, hsb2rgb(hsb), intensity * 0.6);

    // chords.frag: the cores' glow, hue-shifted into the scene rather than painted over it.
    vec4 glow = vec4(0.0);
    for (int i = 0; i < CoreCount.x && i < MAX_CORES; i++) {
        glow = addGlow(glow, texCoord, Focus[i], CoreColor[i].rgb);
    }
    if (glow.a > 0.0) {
        vec3 glowHsb = rgb2hsb(glow.rgb);
        vec3 sceneHsb = rgb2hsb(res);
        sceneHsb.x = glowHsb.x;
        sceneHsb.y = sceneHsb.y * 0.5 + glowHsb.y * 0.5;
        sceneHsb.z = getVisualBrightness(res);
        res = mix(res, hsb2rgb(sceneHsb), min(0.2, glow.a)) + glow.rgb;
    }

    fragColor = vec4(res, color.a);
}
