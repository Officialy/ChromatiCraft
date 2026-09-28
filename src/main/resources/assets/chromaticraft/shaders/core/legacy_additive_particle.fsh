#version 330
#extension GL_ARB_separate_shader_objects : require

#include <minecraft:dynamictransforms.glsl>

uniform sampler2D Sampler0;

layout(location = 0) in float sphericalVertexDistance;
layout(location = 1) in float cylindricalVertexDistance;
layout(location = 2) in vec2 texCoord0;
layout(location = 3) in vec4 vertexColor;

layout(location = 0) out vec4 fragColor;

void main() {
    vec4 texel = texture(Sampler0, texCoord0);

    // Most V33a glow sprites are opaque RGB images whose black background was erased by
    // GL_ONE/GL_ONE_MINUS_SRC_COLOR. Turn their brightness into real coverage for the 26.2
    // transparency target. Preserve an existing alpha channel for newer atlas sprites too.
    float coverage = texel.a * max(texel.r, max(texel.g, texel.b));
    if (coverage < (0.5 / 255.0)) {
        discard;
    }

    vec4 modulation = vertexColor * ColorModulator;
    fragColor = vec4(texel.rgb * texel.a * modulation.rgb, coverage * modulation.a);
}
