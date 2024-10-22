#version 450

#define GROUP_SIZE 128

layout(local_size_x = GROUP_SIZE) in;

struct ComputeParameters {
    uint MAX_PARTICLES;
    uint MAX_EMITTERS;
    uint num_emitters;
    float dt; // Delta time
};

struct ComputeState {
    uint num_particles;
    uint NEW_PARTICLES;
    uint pad[2];
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



void main() {
    uint global_id = gl_GlobalInvocationID.x;

    if (global_id < state.num_particles) {
        // Check if the particle is active
        if (particles[global_id].lifeScaleTexture.x > 0.0) {
            // Update position based on velocity
            particles[global_id].pos.xyz += particles[global_id].vel.xyz * compute.dt;

            // Update lifetime
            particles[global_id].lifeScaleTexture.x -= compute.dt;

            // Check if the particle has expired
            if (particles[global_id].lifeScaleTexture.x <= 0.0) {
                particles[global_id].lifeScaleTexture.x = 0.0;

                // Remove particle index
                uint new_index = atomicAdd(state.NEW_PARTICLES, 0xFFFFFFFFu);
                particle_allocator_buffer[compute.MAX_PARTICLES - new_index] = global_id;
            }
        }
    }
}