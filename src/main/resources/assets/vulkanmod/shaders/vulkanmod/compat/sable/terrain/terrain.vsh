#version 450

#include "light.glsl"
#include "fog.glsl"

layout(location = 0) in vec3 Position;
layout(location = 1) in vec4 Color;
layout(location = 2) in vec2 UV0;
layout(location = 3) in ivec2 UV2;
layout(location = 4) in vec3 Normal;

layout(binding = 0) uniform UniformBufferObject {
    mat4 MVP;
    mat4 ModelViewMat;
    vec3 ChunkOffset;
    float SableSkyLightScale;
    vec3 Light0_Direction;
    float SableEnableNormalLighting;
    vec3 Light1_Direction;
    float SableShadowsEnabled;
};

layout(binding = 3) uniform sampler2D Sampler2;

layout(location = 0) out float vertexDistance;
layout(location = 1) out vec4 vertexColor;
layout(location = 2) out vec2 texCoord0;

void main() {
    vec3 drawPosition = Position + ChunkOffset;
    gl_Position = MVP * vec4(drawPosition, 1.0);
    vertexDistance = fog_distance(drawPosition, 0);

    ivec2 lightCoordinates = bitfieldExtract(UV2, 4, 8);
    lightCoordinates.y = int(round(float(lightCoordinates.y) * clamp(SableSkyLightScale, 0.0, 1.0)));
    vec4 lightColor = texelFetch(Sampler2, lightCoordinates, 0);
    vertexColor = Color * lightColor;

    if (SableEnableNormalLighting > 0.5) {
        vec3 transformedNormal = normalize(mat3(ModelViewMat) * Normal);
        vertexColor = minecraft_mix_light(Light0_Direction, Light1_Direction, transformedNormal, vertexColor);
    }

    texCoord0 = UV0;
}
