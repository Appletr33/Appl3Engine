package org.example;

import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL43;

public class ShaderManager
{
    private final int programID;
    private int vertexShaderID;
    private int fragmentShaderID;
    private int computeShaderID;
    public final int WORKGROUP_SIZE = 256;

    public ShaderManager() throws Exception
    {
        programID = GL20.glCreateProgram();
        if(programID == 0)
            throw new Exception("Could not create Opengl Program");
    }

    public void createVertexShader(String shaderCode) throws Exception
    {
        vertexShaderID = createShader(shaderCode, GL20.GL_VERTEX_SHADER);
    }

    public void createFragmentShader(String shaderCode) throws Exception
    {
        fragmentShaderID = createShader(shaderCode, GL20.GL_FRAGMENT_SHADER);
    }

    public void createComputeShader(String shaderCode) throws Exception
    {
        computeShaderID = createShader(shaderCode, GL43.GL_COMPUTE_SHADER);
    }

    public int getUniformLocation(String uniform) throws Exception
    {
        int loc = GL20.glGetUniformLocation(programID, uniform);
        if (loc == -1)
            throw new Exception("[Error]: Uniform \t" + uniform + "\t Not found!");
        else
            return loc;
    }

    public int createShader(String shaderCode, int shaderType) throws Exception
    {
        int shaderID = GL20.glCreateShader(shaderType);
        if (shaderID == 0)
            throw new Exception("[Error]: Creating Shader Type: " +  shaderType);

        GL20.glShaderSource(shaderID, shaderCode);
        GL20.glCompileShader(shaderID);

        if (GL20.glGetShaderi(shaderID, GL20.GL_COMPILE_STATUS) == 0)
            throw new Exception("[Error]: compiling shader code failed: TYPE: " + shaderType + " Info " + GL20.glGetShaderInfoLog(shaderID, 1024));

        GL20.glAttachShader(programID, shaderID);

        return shaderID;
    }

    public void link() throws Exception
    {
        GL20.glLinkProgram(programID);
        if (GL20.glGetProgrami(programID, GL20.GL_LINK_STATUS) == 0)
            throw new Exception("[Error]: Linking shader code failed" + " Info " + GL20.glGetProgramInfoLog(programID, 1024));

        if (vertexShaderID != 0)
            GL20.glDetachShader(programID, vertexShaderID);

        if (fragmentShaderID != 0)
            GL20.glDetachShader(programID, fragmentShaderID);

        if (computeShaderID != 0)
            GL20.glDetachShader(programID, computeShaderID);

        GL20.glValidateProgram(programID);
        if (GL20.glGetProgrami(programID, GL20.GL_VALIDATE_STATUS) == 0)
            throw new Exception("[Error]: Unable to validate shader code" + GL20.glGetShaderInfoLog(programID, 1024));
    }

    public void bind()
    {
        GL20.glUseProgram(programID);
    }

    public void unbind()
    {
        GL20.glUseProgram(0);
    }

    public void cleanup()
    {
        unbind();
        if (programID != 0)
            GL20.glDeleteProgram(programID);
    }
}
