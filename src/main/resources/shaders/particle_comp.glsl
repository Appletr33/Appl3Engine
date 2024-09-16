#version 450 core

layout(local_size_x = 256) in;

void main()
{
    uint globalIndex = gl_GlobalInvocationID.x;
}