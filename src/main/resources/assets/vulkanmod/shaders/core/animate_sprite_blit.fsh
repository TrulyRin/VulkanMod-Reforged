#version 330

layout(std140) uniform SpriteAnimationInfo {
    mat4 ProjectionMatrix;
    mat4 SpriteMatrix;
    float UPadding;
    float VPadding;
    int MipMapLevel;
};

uniform sampler2D Sprite;

in vec2 texCoord0;

out vec4 fragColor;

void main() {
    vec4 color = textureLod(Sprite, texCoord0, MipMapLevel);
    fragColor = color;
}
