#version 450

#define GROUP_SIZE 128

layout(local_size_x = GROUP_SIZE) in;

// Structures
struct ComputeParameters {
    uint MAX_PARTICLES;
    uint MAX_EMITTERS;
    uint num_emitters;
    float dt; // Delta time
};

// Particle structure matching the compute shader's output
struct Particle {
    vec4 pos;          // xyz for position, w for padding or other use
    vec4 vel;          // xyz for velocity, w for padding or other use
    vec4 lifeScaleTexture; // life, scale, texture, padding
};

// Particle structure matching the compute shader's output
struct Emitter {
    vec4 pos;          // xyz for position, w for padding or other use
    vec4 vel;          // xyz for velocity, w for padding or other use
    vec4 lifeTypeScale; // life, scale, texture, padding
};

struct ComputeState {
    uint num_particles;
    uint NEW_PARTICLES;
    uint pad[2];
};

// Buffer Bindings
layout(std140, binding = 0) uniform ComputeParametersBuffer {
    ComputeParameters compute;
};

layout(std430, binding = 1) buffer ComputeStateBuffer {
    ComputeState state;
};

layout(std430, binding = 2) buffer ParticleBuffer {
    Particle particles[];
};

layout(std430, binding = 3) buffer ParticleIndices {
    uint particle_allocator_buffer[];
};

layout(std430, binding = 4) buffer EmitterBuffer {
    Emitter emitters[];
};



// Shared variables within a workgroup
shared Emitter sharedEmitter;
shared uint sharedNumParticles;
shared uint sharedNewIndex;

// Function to generate a pseudo-random float based on an index
float get_random(uint index, uint seed) {
    // Simple pseudo-random generator (e.g., Linear Congruential Generator)
    uint a = 1664525u;
    uint c = 1013904223u;
    uint m = 0xFFFFFFFFu;
    uint rand = (a * (seed + index) + c) & m;
    return float(rand) / float(m);
}

// Function to generate a random direction within a sphere
vec3 get_random_sphere(uint index, float radius, uint seed) {
    float theta = get_random(index, seed) * 6.28318530718; // 2 * PI
    float phi = acos(2.0 * get_random(index, seed) - 1.0);
    float r = get_random(index, seed) * radius;

    float sinPhi = sin(phi);
    float x = r * sinPhi * cos(theta);
    float y = r * sinPhi * sin(theta);
    float z = r * cos(phi);

    return vec3(x, y, z);
}

//every emitter
void main() {
    uint global_id = gl_GlobalInvocationID.x;
    uint local_id = gl_LocalInvocationIndex;
    uint group_id = gl_WorkGroupID.x;

    // Ensure we don't process more emitters than available
    if(group_id >= compute.num_emitters) {
        return;
    }

    // Thread 0 loads emitter data into shared memory
    if(local_id == 0u) {
        sharedEmitter = emitters[group_id];

        // Example spawn rate calculation
        // This should be replaced with your actual spawn rate logic
        // For demonstration, let's assume each emitter spawns a fixed number of particles per frame
        uint spawnRate = 10u; // Number of particles to spawn per frame (can be dynamic)
        sharedNumParticles = spawnRate;

        // Allocate space for new particles using an atomic operation
        // This ensures that each workgroup gets a unique range of indices
        uint oldNewParticles = atomicAdd(state.num_particles, sharedNumParticles);
        sharedNewIndex = oldNewParticles;

        // Check if we exceed the maximum number of particles
        if(sharedNewIndex + sharedNumParticles > compute.MAX_PARTICLES) {
            // Clamp the number of particles to spawn to prevent overflow
            sharedNumParticles = compute.MAX_PARTICLES - sharedNewIndex;
        }
    }

    // Ensure all threads see the updated shared variables
    memoryBarrierShared();
    barrier();

    // Each thread handles a subset of the particles to spawn
    uint particlesPerThread = (sharedNumParticles + GROUP_SIZE - 1u) / GROUP_SIZE;
    uint start = local_id * particlesPerThread;
    uint end = min(start + particlesPerThread, sharedNumParticles);

    for(uint i = start; i < end; i++) {
        uint particleIndex = sharedNewIndex + i;

        if(particleIndex >= compute.MAX_PARTICLES) {
            // Safety check to prevent out-of-bounds access
            continue;
        }

        // Initialize particle properties
        Particle newParticle;

        // Position: Emitter position plus some random offset within a sphere
        uint seed = group_id * 1000u + i; // Example seed (can be more complex)
        vec3 randomOffset = get_random_sphere(particleIndex, 1.0f, seed); // Radius = 1.0f
        newParticle.pos.xyz = sharedEmitter.pos.xyz + randomOffset;

        // Velocity: Emitter velocity plus some random variation
        newParticle.vel.xyz = sharedEmitter.vel.xyz + vec3(
            get_random(particleIndex, seed) * 0.1f - 0.05f,
            get_random(particleIndex, seed) * 0.1f - 0.05f,
            get_random(particleIndex, seed) * 0.1f - 0.05f
        );

        // Lifetime: Mean lifetime with some randomness
        newParticle.lifeScaleTexture.x = sharedEmitter.lifeTypeScale.x * (0.8f + get_random(particleIndex, seed) * 0.4f); // 80% to 120% of mean_life

        // Scale: Random scale between 1 and 5
        newParticle.lifeScaleTexture.y = 1u + uint(get_random(particleIndex, seed) * 4.0f);

        // Texture: Random texture index (assuming you have multiple textures)
        newParticle.lifeScaleTexture.z = uint(get_random(particleIndex, seed) * 10.0f); // 0 to 9

        // Write the new particle to the particle buffer
        particles[particleIndex] = newParticle;
    }

    // Optionally, you can update the emitter's velocity or other properties here
    // For example, applying some damping or external forces
}