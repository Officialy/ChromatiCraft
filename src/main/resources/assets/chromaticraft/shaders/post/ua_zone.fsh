#version 330

// V33a Shaders/uazone.frag, with the DragonAPI #import libraries it used inlined:
// lib_color (visual brightness), lib_effects (grayscaled blurred Sobel edge detection) and
// lib_noise (Ian McEwan / Ashima Arts 4D simplex noise, MIT licensed).

#moj_import <minecraft:globals.glsl>

layout(std140) uniform UaZone {
    vec4 Params; // x intensity, y time (player ticks), z sky brightness, w head yaw + pitch (degrees)
};

uniform sampler2D InSampler;

in vec2 texCoord;
out vec4 fragColor;

float getVisualBrightness(vec3 color) {
    return color.r * 0.2989 + color.g * 0.5870 + color.b * 0.1140;
}

vec4 getBlurredPixel(vec2 coord, float dx, float dy) {
    vec4 color = texture(InSampler, coord);
    color += texture(InSampler, coord + vec2(dx, 0));
    color += texture(InSampler, coord - vec2(dx, 0));
    color += texture(InSampler, coord + vec2(0, dy));
    color += texture(InSampler, coord - vec2(0, dy));
    color *= 0.2;
    return color;
}

vec4 getGrayBlurredPixel(vec2 coord, float dx, float dy) {
    float br = getVisualBrightness(getBlurredPixel(coord, dx, dy).rgb);
    return vec4(br, br, br, 1.0);
}

vec3 getGrayscaledBlurredEdgeDetectedColor(vec2 coord) {
    float w = 1.0 / ScreenSize.x;
    float h = 1.0 / ScreenSize.y;
    vec4 n[9];
    n[0] = getGrayBlurredPixel(coord + vec2(-w, -h), w, h);
    n[1] = getGrayBlurredPixel(coord + vec2(0.0, -h), w, h);
    n[2] = getGrayBlurredPixel(coord + vec2(w, -h), w, h);
    n[3] = getGrayBlurredPixel(coord + vec2(-w, 0.0), w, h);
    n[4] = getGrayBlurredPixel(coord, w, h);
    n[5] = getGrayBlurredPixel(coord + vec2(w, 0.0), w, h);
    n[6] = getGrayBlurredPixel(coord + vec2(-w, h), w, h);
    n[7] = getGrayBlurredPixel(coord + vec2(0.0, h), w, h);
    n[8] = getGrayBlurredPixel(coord + vec2(w, h), w, h);
    vec4 sobel_edge_h = n[2] + (2.0 * n[5]) + n[8] - (n[0] + (2.0 * n[3]) + n[6]);
    vec4 sobel_edge_v = n[0] + (2.0 * n[1]) + n[2] - (n[6] + (2.0 * n[7]) + n[8]);
    vec4 sobel = sqrt((sobel_edge_h * sobel_edge_h) + (sobel_edge_v * sobel_edge_v));
    return sobel.rgb;
}

vec4 permute(vec4 x) {
    return mod(((x * 34.0) + 1.0) * x, 289.0);
}

float permute(float x) {
    return floor(mod(((x * 34.0) + 1.0) * x, 289.0));
}

vec4 taylorInvSqrt(vec4 r) {
    return 1.79284291400159 - 0.85373472095314 * r;
}

float taylorInvSqrt(float r) {
    return 1.79284291400159 - 0.85373472095314 * r;
}

vec4 grad4(float j, vec4 ip) {
    const vec4 ones = vec4(1.0, 1.0, 1.0, -1.0);
    vec4 p, s;
    p.xyz = floor(fract(vec3(j) * ip.xyz) * 7.0) * ip.z - 1.0;
    p.w = 1.5 - dot(abs(p.xyz), ones.xyz);
    s = vec4(lessThan(p, vec4(0.0)));
    p.xyz = p.xyz + (s.xyz * 2.0 - 1.0) * s.www;
    return p;
}

float snoise4d(vec4 v) {
    const vec2 C = vec2(0.138196601125010504, 0.309016994374947451);
    vec4 i = floor(v + dot(v, C.yyyy));
    vec4 x0 = v - i + dot(i, C.xxxx);
    vec4 i0;
    vec3 isX = step(x0.yzw, x0.xxx);
    vec3 isYZ = step(x0.zww, x0.yyz);
    i0.x = isX.x + isX.y + isX.z;
    i0.yzw = 1.0 - isX;
    i0.y += isYZ.x + isYZ.y;
    i0.zw += 1.0 - isYZ.xy;
    i0.z += isYZ.z;
    i0.w += 1.0 - isYZ.z;
    vec4 i3 = clamp(i0, 0.0, 1.0);
    vec4 i2 = clamp(i0 - 1.0, 0.0, 1.0);
    vec4 i1 = clamp(i0 - 2.0, 0.0, 1.0);
    vec4 x1 = x0 - i1 + 1.0 * C.xxxx;
    vec4 x2 = x0 - i2 + 2.0 * C.xxxx;
    vec4 x3 = x0 - i3 + 3.0 * C.xxxx;
    vec4 x4 = x0 - 1.0 + 4.0 * C.xxxx;
    i = mod(i, 289.0);
    float j0 = permute(permute(permute(permute(i.w) + i.z) + i.y) + i.x);
    vec4 j1 = permute(permute(permute(permute(
                i.w + vec4(i1.w, i2.w, i3.w, 1.0))
            + i.z + vec4(i1.z, i2.z, i3.z, 1.0))
            + i.y + vec4(i1.y, i2.y, i3.y, 1.0))
            + i.x + vec4(i1.x, i2.x, i3.x, 1.0));
    vec4 ip = vec4(1.0 / 294.0, 1.0 / 49.0, 1.0 / 7.0, 0.0);
    vec4 p0 = grad4(j0, ip);
    vec4 p1 = grad4(j1.x, ip);
    vec4 p2 = grad4(j1.y, ip);
    vec4 p3 = grad4(j1.z, ip);
    vec4 p4 = grad4(j1.w, ip);
    vec4 norm = taylorInvSqrt(vec4(dot(p0, p0), dot(p1, p1), dot(p2, p2), dot(p3, p3)));
    p0 *= norm.x;
    p1 *= norm.y;
    p2 *= norm.z;
    p3 *= norm.w;
    p4 *= taylorInvSqrt(dot(p4, p4));
    vec3 m0 = max(0.6 - vec3(dot(x0, x0), dot(x1, x1), dot(x2, x2)), 0.0);
    vec2 m1 = max(0.6 - vec2(dot(x3, x3), dot(x4, x4)), 0.0);
    m0 = m0 * m0;
    m1 = m1 * m1;
    return 49.0 * (dot(m0 * m0, vec3(dot(p0, x0), dot(p1, x1), dot(p2, x2)))
            + dot(m1 * m1, vec2(dot(p3, x3), dot(p4, x4))));
}

void main() {
    float intensity = Params.x;
    float skyBright = Params.z;
    float head = Params.w;

    vec4 orig = texture(InSampler, texCoord);
    vec3 sobel = getGrayscaledBlurredEdgeDetectedColor(texCoord);
    float dT = Params.y * 0.01;
    vec2 pos = ScreenSize * texCoord * vec2(0.002, 0.0008);
    float n1 = snoise4d(vec4(pos.x, dT, pos.y, head * 0.02));
    float n2 = snoise4d(vec4(pos.x, dT - 2487.47193, pos.y, head * 0.017));
    float noiseVal = min(1.0, 4.0 * max(0.0, 0.15 - abs(min(n1, n2))));
    float ex = 2.5 + skyBright * 1.5;
    sobel.r = pow(sobel.r, ex);
    sobel.g = pow(sobel.g, ex);
    sobel.b = pow(sobel.b, ex);
    vec3 add = sobel.rgb * vec3(0.608, 0.757, 0.227) * (10.0 - skyBright * 7.5) * intensity * noiseVal;
    vec3 net = orig.rgb + add;
    fragColor = vec4(min(1.0, net.x), min(1.0, net.y), min(1.0, net.z), orig.a);
}
