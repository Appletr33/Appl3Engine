#version 450 core

in vec2 texCoord;

uniform sampler2D atlasHandle;
uniform vec2 textureOffset;
uniform vec2 textureSize;

out vec4 fragColor;

void main()
{
    // Flip the Y coordinate
    vec2 flippedTexCoord = vec2(texCoord.x, 1.0 - texCoord.y);

    vec2 adjustedTexCoord = textureOffset + (flippedTexCoord * textureSize);
    fragColor = texture(atlasHandle, adjustedTexCoord);
}