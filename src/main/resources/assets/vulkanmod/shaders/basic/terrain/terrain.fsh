#version 450

#include "light.glsl"
#include "fog.glsl"
#include "terrain_sampling.glsl"

layout(binding = 2) uniform sampler2D Sampler0;

layout(binding = 1) uniform UBO {
    vec4 FogColor;
    float FogStart;
    float FogEnd;
    float AlphaCutout;
    int UseRgss;
};

layout(location = 0) in float vertexDistance;
layout(location = 1) in vec4 vertexColor;
layout(location = 2) in vec2 texCoord0;

layout(location = 0) out vec4 fragColor;

void main() {
    vec2 pixelSize = 1.0 / vec2(textureSize(Sampler0, 0));
    vec4 color = (UseRgss == 1 ? sampleRGSS(Sampler0, texCoord0, pixelSize)
                              : sampleNearest(Sampler0, texCoord0, pixelSize)) * vertexColor;
    if (color.a < AlphaCutout) {
        discard;
    }
    fragColor = linear_fog(color, vertexDistance, FogStart, FogEnd, FogColor);
}
