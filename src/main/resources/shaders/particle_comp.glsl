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
PackedParticle packVec3(Particle p)
{
    return PackedParticle(p.pos.x, p.pos.y, p.pos.z);
}


layout(local_size_x = 256) in;
layout(std430, binding = 0) buffer ParticlesSSBO
{
    PackedParticle Particles[];
} particleSSBO;

void main()
{
    uint globalIndex = gl_GlobalInvocationID.x;
    Particle particle = unpackVec3(particleSSBO.Particles[globalIndex]);

    particleSSBO.Particles[globalIndex] = packVec3(particle);

}