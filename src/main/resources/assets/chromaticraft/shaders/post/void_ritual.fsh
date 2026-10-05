#version 330
#extension GL_ARB_separate_shader_objects : require
uniform sampler2D InSampler;
layout(std140) uniform RitualPost { vec4 Sphere; vec4 Ring; vec4 Trap; vec4 Rotation; };
layout(location=0) in vec2 texCoord;
layout(location=0) out vec4 fragColor;
vec4 sphereColor(vec2 uv) {
    vec2 diff=uv-Sphere.xy; float distance=dot(diff,diff);
    float f=Sphere.w*max(0.0,1.0-abs(distance-Sphere.z)/max(0.00001,Ring.x));
    vec4 color=texture(InSampler,mix(uv,Sphere.xy,f)); color.rgb=mix(color.rgb,vec3(1.0),f/2.0); return color;
}
void main() {
    vec2 diff=texCoord-Trap.xy; float distance=dot(diff,diff);
    float cf=clamp(1.8-.875*distance*Trap.z,0.0,1.0),vf=Trap.w*clamp(1.0-distance*Trap.z,0.0,1.0);
    float angle=.125*vf*3.1416*2.0*Rotation.x/pow(max(.002,distance),.25);
    vec2 rotated=Trap.xy+mat2(cos(angle),sin(angle),-sin(angle),cos(angle))*diff;
    vec4 color=sphereColor(texCoord),warped=sphereColor(rotated);
    fragColor=vec4(mix(warped.rgb,color.rgb,max(0.0,abs(Rotation.x)-.5)*2.0*cf),color.a);
}
