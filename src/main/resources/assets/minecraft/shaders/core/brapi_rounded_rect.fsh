#version 330

layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
    mat4 TextureMat;
};

layout(std140) uniform RectData {
    float rectX;
    float rectY;
    float rectW;
    float rectH;
    float r1;
    float r2;
    float r3;
    float r4;
    float strokeWidth;
};

in vec4 vertColor;
out vec4 fragColor;

void main() {
    vec2 pixelPos = vec2(gl_FragCoord.x - rectX, gl_FragCoord.y - rectY);
    vec2 halfSize = vec2(rectW, rectH) * 0.5;
    vec2 pos = pixelPos - halfSize;

    float radius = pos.x < 0.0
        ? (pos.y < 0.0 ? r4 : r1)
        : (pos.y < 0.0 ? r3 : r2);

    vec2 q = abs(pos) - halfSize + radius;
    float dist = length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - radius;

    float alpha = 1.0 - smoothstep(-1.0, 0.0, dist);

    // if strokeWidth > 0 then discard pixels inside the inner edge
    if (strokeWidth > 0.0) {
        float innerDist = dist + strokeWidth;
        float innerAlpha = 1.0 - smoothstep(-1.0, 0.0, innerDist);
        alpha = alpha - innerAlpha;
    }

    if (alpha <= 0.0) discard;

    vec4 color = vertColor * ColorModulator;
    fragColor = vec4(color.rgb, color.a * alpha);
}