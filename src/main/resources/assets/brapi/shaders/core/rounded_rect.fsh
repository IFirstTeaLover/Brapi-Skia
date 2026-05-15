#version 150

precision mediump float;

in vec2 texCoord;
out vec4 fragColor;

uniform vec2 RectSize;
uniform vec4 Color;
uniform float Radius;

float roundedBoxSDF(vec2 p, vec2 b, float r) {
    vec2 q = abs(p) - b + r;
    return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - r;
}

void main() {
    vec2 p = (texCoord - 0.5) * RectSize;

    float distance = roundedBoxSDF(p, RectSize / 2.0, Radius);

    float alpha = 1.0 - smoothstep(-0.2, 0.2, distance);

    fragColor = vec4(Color.rgb, Color.a * alpha);
}