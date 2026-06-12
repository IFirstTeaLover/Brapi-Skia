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
    float dist = texture(Sampler0, texCoord).r;
    float width = fwidth(dist) * 0.7;
    float alpha = smoothstep(0.5 - width, 0.5 + width, dist);
    if (alpha < 0.05) discard;
    fragColor = vec4(vertColor.rgb, vertColor.a * alpha) * ColorModulator;
}