#version 330

#moj_import <minecraft:dynamictransforms.glsl>

uniform sampler2D Sampler0;

in float sphericalVertexDistance;
in float cylindricalVertexDistance;
in vec2 texCoord0;
in vec4 vertexColor;

out vec4 fragColor;

// V33a alpha-blended particles: no fog, and DragonAPI's ParticleEngine alpha clip of 1/255 rather than
// vanilla's 0.1, so soft sprites fade out smoothly instead of ending in a hard rim.
void main() {
    vec4 color = texture(Sampler0, texCoord0) * vertexColor * ColorModulator;
    if (color.a < (1.0 / 255.0)) {
        discard;
    }
    fragColor = color;
}
