#version 330

layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
    mat4 TextureMat;
};

uniform sampler2D Sampler0;

in vec2 texCoord;
in vec4 vertColor;
out vec4 fragColor;

void main() {
    vec4 tex = texture(Sampler0, texCoord);
    if (tex.a < 0.01) discard;
    fragColor = tex * vertColor * ColorModulator;
}