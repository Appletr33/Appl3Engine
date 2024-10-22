#version 450 core

// Particle structure matching the compute shader's output
struct Particle {
    vec4 pos;          // xyz for position, w for padding or other use
    vec4 vel;          // xyz for velocity, w for padding or other use
    vec4 lifeScaleTexture; // life, scale, texture, padding
};

// Read-only buffer containing particles
layout(std430, binding = 2) readonly buffer ParticleBuffer {
    Particle particles[];
};

// Uniforms
uniform int instanceSize;
uniform mat4 projViewMatrix;
uniform vec3 right;
uniform vec3 up;

// Outputs to the fragment shader
out vec2 texCoord;

void main() {
    // Calculate quad index and particle index
    uint quadIndex = uint(gl_VertexID) & 3u;
    uint particleIndex = gl_InstanceID * instanceSize + (gl_VertexID >> 2);

    // Fetch particle data
    Particle particle = particles[particleIndex];

    // Check if the particle is alive
    if (particle.lifeScaleTexture.x <= 0.0) {
        // Discard the vertex by moving it off-screen or setting zero size
        gl_Position = vec4(0.0, 0.0, 0.0, 0.0);
        texCoord = vec2(0.0, 0.0);
        return;
    }

    // Convert scale from uint to float
    float scale = particle.lifeScaleTexture.y;

    // Calculate vertex offset for the quad
    vec2 offset = vec2(
    (float((quadIndex & 1u) << 1u) - 1.0),
    (float((quadIndex & 2u) - 1.0))
    );
    vec3 vertexOffset = (right * offset.x + up * offset.y) * scale;

    // Compute the final position
    vec3 position = particle.pos.xyz + vertexOffset;

    // Transform to clip space
    gl_Position = projViewMatrix * vec4(position, 1.0);

    // Calculate texture coordinates
    texCoord = offset * 0.5 + 0.5;
}