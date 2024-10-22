#version 450

#define GROUP_SIZE 128

layout(local_size_x = GROUP_SIZE) in;

struct ComputeParameters {
    uint MAX_PARTICLES;
    uint MAX_EMITTERS;
    uint num_emitters;
    float dt; // Delta time
};

layout(std140, binding = 0) uniform ComputeParametersBuffer {ComputeParameters compute;};
layout(std430, binding = 3) buffer ParticleInicies {uint particle_allocator_buffer[];};

// every particle/ emitter
void main() {
    uint global_id = gl_GlobalInvocationID.x;

    if (global_id < compute.MAX_PARTICLES) {
        particle_allocator_buffer[global_id] = compute.MAX_PARTICLES - global_id - 1u;
    }
}