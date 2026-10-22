#version 460

layout (binding = 0) uniform ExternalLodUniforms {
    mat4 ExternalLodCombinedMatrix;
    vec4 ExternalLodModelOffsetAndYOffset;
    vec4 ExternalLodRenderParams;
};

layout (binding = 1) uniform sampler2D uLightMap;

layout (location = 0) in uvec4 Position;
layout (location = 1) in vec4 Color;
layout (location = 2) in vec4 Padding;

layout (location = 0) out vec4 vertexColor;
layout (location = 1) out vec3 vertexWorldPos;
layout (location = 2) out vec4 vertexPackedPos;

void main() {
    vec3 modelOffset = ExternalLodModelOffsetAndYOffset.xyz;
    float worldYOffset = ExternalLodModelOffsetAndYOffset.w;
    float microOffset = ExternalLodRenderParams.x;
    bool isWhiteWorld = ExternalLodRenderParams.z != 0.0;

    vertexPackedPos = vec4(Position);
    vertexWorldPos = vec3(Position.xyz) + modelOffset;

    uint meta = Position.w;
    uint micro = (meta & 0xFF00u) >> 8u;

    float mx = (micro & 1u) != 0u ? microOffset : 0.0;
    mx = (micro & 2u) != 0u ? -mx : mx;
    float mz = (micro & 16u) != 0u ? microOffset : 0.0;
    mz = (micro & 32u) != 0u ? -mz : mz;

    vertexWorldPos.x += mx;
    vertexWorldPos.z += mz;

    uint lights = meta & 0xFFu;
    uint skyLight = lights & 15u;
    uint blockLight = lights >> 4u;
    vec4 lightMapColor = texelFetch(uLightMap, ivec2(int(blockLight), int(skyLight)), 0);

    vertexColor = isWhiteWorld ? lightMapColor : Color * lightMapColor;

    vec4 clip = ExternalLodCombinedMatrix * vec4(vertexWorldPos, 1.0);
    clip.z = (clip.z + clip.w) * 0.5;
    gl_Position = clip;
}
