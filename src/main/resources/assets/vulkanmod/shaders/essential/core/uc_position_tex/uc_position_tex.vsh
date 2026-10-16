#version 450

layout(location = 0) in vec3 uc_Position;
layout(location = 1) in vec3 uc_UV0;

layout(binding = 0) uniform UniformBufferObject {
   mat4 MVP;
};

layout(location = 0) out vec2 texCoord0;

void main() {
    gl_Position = MVP * vec4(uc_Position, 1.0);

    texCoord0 = uc_UV0.xy;
}
