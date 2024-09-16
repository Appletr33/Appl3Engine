package org.example;

import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL11.*;

public class Renderer
{
    public static Renderer renderer;
    public Matrix4f projViewMatrix;

    public List<Runnable> renderables = new ArrayList<>();
    public List<Runnable> cleanupCalls = new ArrayList<>();

    public Renderer() throws Exception
    {
        renderer = this;
    }

    public void render()
    {
        glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        projViewMatrix  = new Matrix4f(Main.getWindow().getProjectionMatrix()).mul(Camera.camera.getViewMatrix());

        for (Runnable renderFunction : renderables) {
            renderFunction.run();
        }
    }

    public void cleanup()
    {
        for (Runnable cleanupCall : cleanupCalls) {
            cleanupCall.run();
        }
    }

    public Matrix4f getProjViewMatrix()
    {
        return projViewMatrix;
    }
}
