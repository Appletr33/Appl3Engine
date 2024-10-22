package org.example;

import org.example.utils.Loader;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;
import org.lwjgl.system.MemoryStack;
import static org.lwjgl.opengl.GL30.*;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;


public class ParticleRenderer
{
    int GROUP_SIZE = 128;

    ShaderManager shader;
    ShaderManager initializationShader;
    ShaderManager emissionShader;
    ShaderManager simulationShader;

    // ComputeParametersBuffer (UBO)
    private int computeParametersBuffer;
    // ParticleBuffer (SSBO)
    private int particleBuffer;
    // EmitterBuffer (SSBO)
    private int emitterBuffer;
    // ParticleIndices (Allocator Buffer) (SSBO)
    private int particleIndicesBuffer;
    // ComputeStateBuffer (SSBO)
    private int computeStateBuffer;

    private int vaoId;
    private int viewProjLocation;
    private TextureAtlas textureAtlas;
    private int textureOffsetLocation;
    private int instanceSizePerQuadLocation;
    private int rightLocation;
    private int upLocation;

    private int eboId;
    int instanceSize = 32;
    float quadHalfSize = 0.5f;

    // ComputeParameters structure
    class ComputeParameters {
        int MAX_PARTICLES;
        int MAX_EMITTERS;
        int num_emitters;
        float dt;
    }

    // Particle structure
    class Particle {
        Vector4f pos;          // xyz for position, w for padding or other use
        Vector4f vel;          // xyz for velocity, w for padding or other use
        Vector4f lifeScaleTexture; // life, scale, texture, padding
    }

    class Emitter {
        Vector4f pos;          // xyz for position, w for padding or other use
        Vector4f vel;          // xyz for velocity, w for padding or other use
        Vector4f lifeTypeScale; // life, scale, texture, padding
    }

    // ComputeState structure
    class ComputeState {
        int num_particles;
        int NEW_PARTICLES;
        int pad;
        int padd;
    }

    ComputeParameters computeParams;

    public ParticleRenderer() throws Exception
    {
        //Renderer.renderer.renderables.add(this::render);
        Renderer.renderer.cleanupCalls.add(this::cleanup);


        // Initialize Shaders
        shader = new ShaderManager();
        initializationShader = new ShaderManager();
        emissionShader = new ShaderManager();
        simulationShader = new ShaderManager();

        shader.createVertexShader(Loader.loadShader("/shaders/vertex.glsl"));
        shader.createFragmentShader(Loader.loadShader("/shaders/fragment.glsl"));
        shader.link();

        // SETUP BUFFERS
        initializationShader.createComputeShader(Loader.loadShader("/shaders/particle_emit.glsl"));
        initializationShader.link();

        // COMPUTE EMISSION
        emissionShader.createComputeShader(Loader.loadShader("/shaders/particle_init.glsl"));
        emissionShader.link();

        // SIMULATION
        simulationShader.createComputeShader(Loader.loadShader("/shaders/particle_simulate.glsl"));
        simulationShader.link();

        // VERTEX UNIFORMS
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

        // Don't forget to bind your VAO and associate the EBO with it
        glBindVertexArray(vaoId);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, eboId);
        glBindVertexArray(0);


        // INITIALIZE PARTICLE SYSTEM COMPUTE SHADERS
        computeParametersBuffer = GL15.glGenBuffers();
        particleBuffer = GL15.glGenBuffers();
        emitterBuffer = GL15.glGenBuffers();
        particleIndicesBuffer = GL15.glGenBuffers();
        computeStateBuffer = GL15.glGenBuffers();

        // INITIALIZE COMPUTE PARAMETERS
        computeParams = new ComputeParameters();
        computeParams.MAX_PARTICLES = 16384;
        computeParams.MAX_EMITTERS = 1;
        computeParams.dt = 0.0f;
        computeParams.num_emitters = 0;

        ByteBuffer cpBuffer = BufferUtils.createByteBuffer(16);
        cpBuffer.order(ByteOrder.nativeOrder());
        cpBuffer.putInt(computeParams.MAX_PARTICLES);
        cpBuffer.putInt(computeParams.MAX_EMITTERS);
        cpBuffer.putInt(computeParams.num_emitters);
        cpBuffer.putFloat(computeParams.dt);
        cpBuffer.flip();

        // Upload to the uniform buffer
        GL43.glBindBuffer(GL43.GL_UNIFORM_BUFFER, computeParametersBuffer);
        GL43.glBufferSubData(GL43.GL_UNIFORM_BUFFER, 0, cpBuffer);
        GL43.glBindBuffer(GL43.GL_UNIFORM_BUFFER, 0);


       /* // INITIALIZE PARTICLE BUFFER
        int maxParticles = computeParams.MAX_PARTICLES;
        int particleStructSize = 48; // 48 bytes per particle

        // [TODO] save and load particles here

        GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, particleBuffer);
        GL15.glBufferData(GL43.GL_SHADER_STORAGE_BUFFER, (long) maxParticles * particleStructSize, GL15.GL_DYNAMIC_DRAW);
        GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, 0);


        // INITIALIZE PARTICLE INDICES (Allocator Buffer)

        GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, particleIndicesBuffer);
        GL15.glBufferData(GL43.GL_SHADER_STORAGE_BUFFER, (long)maxParticles * 4, GL15.GL_DYNAMIC_DRAW); //* 4 -> unsigned int
        GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, 0);



        // INITIALIZE EMITTER BUFFER
        int maxEmitters = computeParams.MAX_EMITTERS;
        int emitterStructSize = 48; // 48 bytes per particle

        // [TODO] save and load particles here

        GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, emitterBuffer);
        GL15.glBufferData(GL43.GL_SHADER_STORAGE_BUFFER, (long) maxEmitters * emitterStructSize, GL15.GL_DYNAMIC_DRAW);
        GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, 0);


        // INITIALIZE COMPUTE STATE BUFFER
        ComputeState computeState = new ComputeState();
        computeState.num_particles = 0;
        computeState.NEW_PARTICLES = 0;

        IntBuffer computeStateData = BufferUtils.createIntBuffer(4);
        computeStateData.put(new int[]{computeState.num_particles, computeState.NEW_PARTICLES, 0, 0});
        computeStateData.flip();

        GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, computeStateBuffer);
        GL15.glBufferData(GL43.GL_SHADER_STORAGE_BUFFER, computeStateData, GL15.GL_DYNAMIC_DRAW);
        GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, 0);

        // Initialize Buffers on GPU
        initializationShader.bind();

        GL30.glBindBufferBase(GL43.GL_UNIFORM_BUFFER, 0, computeParametersBuffer);
        GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 3, particleIndicesBuffer);

        GL43.glDispatchCompute(computeParams.MAX_PARTICLES / GROUP_SIZE, 1, 1);
        GL43.glMemoryBarrier(GL43.GL_SHADER_STORAGE_BARRIER_BIT);

        initializationShader.unbind();*/
    }

    public void render()
    {

        // UPDATE EMITTERS ON CPU

        Emitter[] emitters = new Emitter[computeParams.MAX_EMITTERS];
        for (int i = 0; i < computeParams.MAX_EMITTERS; i++) {
            Emitter e = new Emitter();
            e.pos = new Vector4f(0f);
            e.vel = new Vector4f(0f);
            e.lifeTypeScale = new Vector4f(100.0f, 1.0f, 1.0f, 1.0f);
            emitters[i] = e;
        }


        // UPDATE COMPUTE PARAMETERS
        computeParams.dt = EngineManager.getDeltaTime();
        computeParams.num_emitters = 1;


        GL30.glBindBufferBase(GL43.GL_UNIFORM_BUFFER, 0, computeParametersBuffer);
        GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 1, computeStateBuffer);
        GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 2, particleBuffer);
        GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 3, particleIndicesBuffer);
        GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 4, emitterBuffer); // uploaded each frame [TODO] NOT DO THAT IMPLEMENT EMITTERS ON GPU?


/*        // Create a ByteBuffer with std140 layout (16 bytes for ComputeParameters)
        ByteBuffer cpBuffer = BufferUtils.createByteBuffer(16);
        cpBuffer.order(ByteOrder.nativeOrder());
        cpBuffer.putInt(computeParams.MAX_PARTICLES);
        cpBuffer.putInt(computeParams.MAX_EMITTERS);
        cpBuffer.putInt(computeParams.num_emitters);
        cpBuffer.putFloat(computeParams.dt);
        cpBuffer.flip();

        GL43.glBindBuffer(GL43.GL_UNIFORM_BUFFER, computeParametersBuffer);
        GL43.glBufferSubData(GL43.GL_UNIFORM_BUFFER, 0, cpBuffer);
        GL43.glBindBuffer(GL43.GL_UNIFORM_BUFFER, 0);


        int emitterSize = 16 * 3; // 3 vec4s per emitter
        ByteBuffer emitterBufferData = BufferUtils.createByteBuffer(computeParams.MAX_EMITTERS * emitterSize);
        emitterBufferData.order(ByteOrder.nativeOrder());

        for (Emitter e : emitters) {
            emitterBufferData.putFloat(e.pos.x).putFloat(e.pos.y).putFloat(e.pos.z).putFloat(e.pos.w);
            // vel
            emitterBufferData.putFloat(e.vel.x).putFloat(e.vel.y).putFloat(e.vel.z).putFloat(e.vel.w);
            // lifeTypeScale
            emitterBufferData.putFloat(e.lifeTypeScale.x).putFloat(e.lifeTypeScale.y)
                    .putFloat(e.lifeTypeScale.z).putFloat(e.lifeTypeScale.w);

        } emitterBufferData.flip();

        // Upload to the SSBO
        GL43.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, emitterBuffer);
        GL43.glBufferSubData(GL43.GL_SHADER_STORAGE_BUFFER, 0, emitterBufferData);
        GL43.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, 0);

        emissionShader.bind();
        GL43.glDispatchCompute((int)Math.ceil(1.0 / GROUP_SIZE) , 1, 1); // only 1 emitter hardcoded currently
        GL43.glMemoryBarrier(GL43.GL_SHADER_STORAGE_BARRIER_BIT);
        emissionShader.unbind();


        simulationShader.bind();
        GL43.glDispatchCompute(computeParams.MAX_PARTICLES / GROUP_SIZE, 1, 1);
        GL43.glMemoryBarrier(GL43.GL_SHADER_STORAGE_BARRIER_BIT);
        simulationShader.unbind();*/



        // UPLOAD EMITTERS TO GPU

        // EMIT PARTICLES

        // UPDATE PARTICLES

        // RASTERIZE PARTICLES


        glDepthMask(false);

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
        long timeInMs = System.currentTimeMillis();
        double timeInSeconds = timeInMs / 100.0;
        double sinValue = Math.sin(timeInSeconds);
        int min = 0;
        int max = 16;
        int sineWaveRange = (int) ((sinValue + 1) / 2 * (max - min) + min);

        Vector2f textOff = textureAtlas.getTextureOffset(sineWaveRange);
        // upload texture choice
        glUniform2f(textureOffsetLocation, textOff.x, textOff.y);
        glUniform1i(instanceSizePerQuadLocation, instanceSize);


        // BIND PARTICLE POSITIONS
        GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 2, particleBuffer);
        GL45.glDrawElementsInstanced(GL_TRIANGLES, instanceSize * 6, GL_UNSIGNED_INT, 0, computeParams.MAX_PARTICLES / instanceSize);

        glBindVertexArray(0);

        shader.unbind();
        glDepthMask(true);
    }

    public void cleanup()
    {
        shader.cleanup();
        initializationShader.cleanup();
        emissionShader.cleanup();
        simulationShader.cleanup();

        GL30.glDeleteBuffers(particleBuffer);
        GL30.glDeleteBuffers(particleIndicesBuffer);
        GL30.glDeleteBuffers(computeStateBuffer);
        GL30.glDeleteBuffers(computeParametersBuffer);
        GL30.glDeleteBuffers(emitterBuffer);

        glDeleteVertexArrays(vaoId);
        GL30.glDeleteBuffers(eboId);
    }

    private int ceilDiv(int x, int y) {
        return (x + y - 1) / y;
    }
}
