#version 450 core

struct Particle
{
    vec3 pos;
};

struct PackedParticle
{
    float x, y, z;
};

Particle unpackVec3(PackedParticle p)
{
    return Particle(vec3(p.x, p.y, p.z));
}

layout(std430, binding = 0) readonly buffer ParticlesSSBO
{
    PackedParticle Particles[];
} particleSSBO;

uniform int instanceSize;
uniform mat4 projViewMatrix;
uniform vec3 right;
uniform vec3 up;

out vec2 texCoord;

void main()
{
    int quadIndex = gl_VertexID & 3;
    int positionIndex = gl_InstanceID * instanceSize + (gl_VertexID >> 2);
    vec3 position = unpackVec3(particleSSBO.Particles[positionIndex]).pos;

    vec2 offset = vec2(float((quadIndex & 1) << 1) - 1.0, float((quadIndex & 2) - 1));
    vec3 vertexOffset = right * offset.x + up * offset.y;

    gl_Position = projViewMatrix * vec4(position + vertexOffset, 1.0);
    texCoord = offset * 0.5 + 0.5;
}