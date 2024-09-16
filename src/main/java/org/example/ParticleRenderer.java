package org.example;

import org.example.utils.Loader;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL43;
import org.lwjgl.opengl.GL45;
import org.lwjgl.system.MemoryStack;
import static org.lwjgl.opengl.GL30.*;
import org.lwjgl.system.MemoryUtil;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public class ParticleRenderer
{
    ShaderManager shader;
    ShaderManager computeShader;
    Particle[] particles;

    private int vaoId;
    private int ssboParticlesId;
    private int viewProjLocation;
    private int paddedParticleCount;
    private TextureAtlas textureAtlas;
    private int textureOffsetLocation;
    private int instanceSizePerQuadLocation;
    private int rightLocation;
    private int upLocation;

    private int eboId;
    int instanceSize = 32;
    private int indexCount;
    float quadHalfSize = 0.5f;

    public ParticleRenderer(Particle[] particles) throws Exception
    {
        Renderer.renderer.renderables.add(this::render);
        Renderer.renderer.cleanupCalls.add(this::cleanup);
        this.particles = particles;
        shader = new ShaderManager();
        computeShader = new ShaderManager();
        try {
            shader.createVertexShader(Loader.loadShader("/shaders/vertex.glsl"));
            shader.createFragmentShader(Loader.loadShader("/shaders/fragment.glsl"));
            shader.link();

            computeShader.createComputeShader(Loader.loadShader("/shaders/particle_comp.glsl"));
            computeShader.link();

        } catch (Exception e)
        {
            e.printStackTrace();
            System.exit(-1);
        }


        instanceSizePerQuadLocation = shader.getUniformLocation("instanceSize");
        int textureAtlasUniformLocation = shader.getUniformLocation("atlasHandle");
        int textureSizeLocation = shader.getUniformLocation("textureSize");
        textureOffsetLocation = shader.getUniformLocation("textureOffset");
        viewProjLocation = shader.getUniformLocation("projViewMatrix");
        rightLocation = shader.getUniformLocation("right");
        upLocation = shader.getUniformLocation("up");

        shader.bind();
        // Setup bind-less texture atlas
        textureAtlas = new TextureAtlas("/textures/particle_atlas.png", textureAtlasUniformLocation, 32);
        // Upload texture size
        glUniform2f(textureSizeLocation, textureAtlas.getTextureSizeX(), textureAtlas.getTextureSizeY());
        shader.unbind();

        // Dummy VAO
        vaoId = glGenVertexArrays();
        glBindVertexArray(vaoId);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 3 * Float.BYTES, 0);

        //Ebo
        eboId = glGenBuffers();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, eboId);

        // Generate indices for quads
        int[] indices = new int[instanceSize * 6]; // 6 indices per quad, 32 quads per instance
        int count = 0;

        for (int i = 0; i < instanceSize; i++) {
            // Each quad has 4 vertices, so indices need to point to 4 unique vertices
            int baseIndex = i * 4;

            // First triangle of the quad
            indices[count++] = baseIndex;
            indices[count++] = baseIndex + 1;
            indices[count++] = baseIndex + 2;

            // Second triangle of the quad
            indices[count++] = baseIndex + 1;
            indices[count++] = baseIndex + 3;
            indices[count++] = baseIndex + 2;
        }

        IntBuffer indicesBuffer = MemoryUtil.memAllocInt(indices.length);
        indicesBuffer.put(indices);
        indicesBuffer.flip();
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indicesBuffer, GL_STATIC_DRAW);
        indexCount = indices.length;

        // Don't forget to bind your VAO and associate the EBO with it
        glBindVertexArray(vaoId);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, eboId);
        glBindVertexArray(0);


        paddedParticleCount = particles.length + computeShader.WORKGROUP_SIZE - (particles.length % computeShader.WORKGROUP_SIZE);
        System.out.println("Padded Particle Count " + paddedParticleCount);

        // SSBO PREP PARTICLE DATA
        int particleBufferSize = paddedParticleCount * 3; // three float per position
        System.out.println("particleBufferSize : " + particleBufferSize);
        FloatBuffer particleBuffer = MemoryUtil.memAllocFloat(particleBufferSize);

        for (int i = 0; i < paddedParticleCount; i++)
        {
            if (i < particles.length)
            {
                particleBuffer.put(particles[i].x);
                particleBuffer.put(particles[i].y);
                particleBuffer.put(particles[i].z);
            }
            else
            {
                particleBuffer.put(0);
                particleBuffer.put(0);
                particleBuffer.put(0);
            }
        }

        particleBuffer.flip();

        // Prepare Particle Buffer
        ssboParticlesId = glGenBuffers();
        glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, ssboParticlesId);
        GL30.glBufferData(GL43.GL_SHADER_STORAGE_BUFFER, particleBuffer, GL30.GL_DYNAMIC_COPY);
        GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 0, ssboParticlesId);

        //clean up
        MemoryUtil.memFree(particleBuffer);
        glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, 0);

        System.out.println("Workgroup size: " + paddedParticleCount / computeShader.WORKGROUP_SIZE);
    }

    public void render()
    {
        // COMPUTE VERTEX POSITIONS
       /* computeShader.bind();

        GL43.glDispatchCompute(paddedParticleCount / 256, 1, 1);
        GL43.glMemoryBarrier(GL43.GL_SHADER_STORAGE_BARRIER_BIT);

        computeShader.unbind();*/


        glDepthMask(false);

        // RASTERIZE
        shader.bind();
        glBindVertexArray(vaoId); // Empty dummy VAO
        GL45.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, eboId);

        // UNIFORMS
        Matrix4f viewMatrix = Camera.camera.getViewMatrix();
        Matrix4f viewProjMatrix = Renderer.renderer.getProjViewMatrix();
        // Calculate the right and up vectors for billboarding
        Vector3f right = new Vector3f(viewMatrix.m00(), viewMatrix.m10(), viewMatrix.m20()).mul(quadHalfSize);
        Vector3f up = new Vector3f(viewMatrix.m01(), viewMatrix.m11(), viewMatrix.m21()).mul(quadHalfSize);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            // Load and set the view-projection matrix
            FloatBuffer viewProjBuffer = stack.mallocFloat(16);
            viewProjMatrix.get(viewProjBuffer);
            glUniformMatrix4fv(viewProjLocation, false, viewProjBuffer);

            // Load and set the right and up vectors
            FloatBuffer rightBuffer = stack.mallocFloat(3);
            rightBuffer.put(new float[]{right.x, right.y, right.z}).flip();
            glUniform3fv(rightLocation, rightBuffer);

            FloatBuffer upBuffer = stack.mallocFloat(3);
            upBuffer.put(new float[]{up.x, up.y, up.z}).flip();
            glUniform3fv(upLocation, upBuffer);
        }

        // select texture
        int atlasHeight = 8;
        float textureOffsetX = 7 * textureAtlas.getTextureSizeX();
        float textureOffsetY = (atlasHeight - 0 - 1) * textureAtlas.getTextureSizeY(); // Subtract 1 twice: once to convert to 0-based index, and once more to flip

        glUniform2f(textureOffsetLocation, textureOffsetX, textureOffsetY);
        glUniform1i(instanceSizePerQuadLocation, instanceSize);


        // BIND PARTICLE POSITIONS
        GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 0, ssboParticlesId);
        GL45.glDrawElementsInstanced(GL_TRIANGLES, instanceSize * 6, GL_UNSIGNED_INT, 0, paddedParticleCount / instanceSize);

        glBindVertexArray(0);

        shader.unbind();
        glDepthMask(true);
    }

    public void cleanup()
    {
        shader.cleanup();
        computeShader.cleanup();
        GL30.glDeleteBuffers(ssboParticlesId);
        glDeleteVertexArrays(vaoId);
    }

}
