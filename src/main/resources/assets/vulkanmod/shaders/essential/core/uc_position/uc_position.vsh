#version 450

layout(location = 0) in vec3 uc_Position;

layout(binding = 0) uniform UniformBufferObject {
   mat4 MVP;
};

void main() {
    gl_Position = MVP * vec4(uc_Position, 1.0);
}
