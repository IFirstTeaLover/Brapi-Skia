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
    // RED8 atlas - alpha comes from red channel
    float alpha = texture(Sampler0, texCoord).r;
    if (alpha < 0.01) discard;
    fragColor = vec4(vertColor.rgb, vertColor.a * alpha) * ColorModulator;
}