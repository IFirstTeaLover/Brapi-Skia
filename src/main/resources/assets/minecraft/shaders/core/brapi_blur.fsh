#version 330

layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
    mat4 TextureMat;
};

layout(std140) uniform BlurData {
    vec4 ScreenSize;     // xy = Full Screen width/height in pixels
    vec4 ComponentBounds;// Unused now, kept for UBO struct layout safety
    vec4 BlurParams;     // x = blur radius strength scalar
};

uniform sampler2D Sampler0; // The screen texture copy

in vec2 texCoord;
in vec4 vertColor;
out vec4 fragColor;

void main() {
    // 1. Calculate the size of a single pixel relative to the full screen
    vec2 texelSize = vec2(1.0) / ScreenSize.xy;

    // 2. Read the blur strength. Ensure it defaults to at least 1.0 if 0 is passed
    float radius = max(BlurParams.x, 1.0);

    // 3. Perform a 9-point spatial blur sample
    vec4 colorSum = texture(Sampler0, texCoord) * 0.24;

    vec2 offset = texelSize * radius;
    colorSum += texture(Sampler0, texCoord + vec2(offset.x, 0.0)) * 0.19;
    colorSum += texture(Sampler0, texCoord - vec2(offset.x, 0.0)) * 0.19;
    colorSum += texture(Sampler0, texCoord + vec2(0.0, offset.y)) * 0.19;
    colorSum += texture(Sampler0, texCoord - vec2(0.0, offset.y)) * 0.19;

    vec2 diagOffset = offset * 0.7071; // Adjust for diagonal distance balance
    colorSum += texture(Sampler0, texCoord + vec2(diagOffset.x,  diagOffset.y)) * 0.05;
    colorSum += texture(Sampler0, texCoord + vec2(-diagOffset.x,  diagOffset.y)) * 0.05;
    colorSum += texture(Sampler0, texCoord + vec2(diagOffset.x, -diagOffset.y)) * 0.05;
    colorSum += texture(Sampler0, texCoord + vec2(-diagOffset.x, -diagOffset.y)) * 0.05;

    // 4. Output the blurred result combined with the UI vertex tint settings
    fragColor = colorSum * vertColor;
}