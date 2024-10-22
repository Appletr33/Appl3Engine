package org.example;

import org.example.utils.Consts;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL43;
import org.lwjgl.opengl.GLUtil;
import org.lwjgl.system.Callback;
import org.lwjgl.system.MemoryUtil;

import java.util.ArrayList;
import java.util.List;

public class EngineManager {
    public static EngineManager engineManager;

    public static final long NANOSECOND = 1000000000L;

    private static int fps;
    private static double deltaTime = 0f;
    private boolean isRunning;

    private WindowManager window;
    private Renderer renderer;
    private Camera camera;
    private Callback GLDebugCallback;
    private int framesRendered = 0;
    private float timeRunning = 0.0f;

    public List<Runnable> updatesToRun = new ArrayList<>();

    private void init() throws Exception
    {
        engineManager = this;
        window = Main.getWindow();
        window.init();
        enableDebugOutput();
        setupCustomDebugMessageCallback();
        try {
            renderer = new Renderer();
        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public void start() throws Exception
    {
        init();
        if (isRunning)
            return;
        run();
    }

    public static void setupCustomDebugMessageCallback() {
        if (GL.getCapabilities().OpenGL43) {
            GL43.glDebugMessageCallback((source, type, id, severity, length, message, userParam) -> {
                String msg = MemoryUtil.memUTF8(message, length);

                // Log the message based on severity
                if (severity == GL43.GL_DEBUG_SEVERITY_HIGH) {
                    System.err.println("[OpenGL HIGH] " + msg);
                } else if (severity == GL43.GL_DEBUG_SEVERITY_MEDIUM) {
                    System.err.println("[OpenGL MEDIUM] " + msg);
                } else if (severity == GL43.GL_DEBUG_SEVERITY_LOW) {
                    System.out.println("[OpenGL LOW] " + msg);
                } else {
                    System.out.println("[OpenGL NOTIFICATION] " + msg);
                }
            }, 0);
        }
    }

    private void enableDebugOutput() {
        // Check if the OpenGL version supports debug output
        if (GL.getCapabilities().GL_ARB_debug_output || GL.getCapabilities().OpenGL43) {
            // Enable OpenGL debug output
            GL43.glEnable(GL43.GL_DEBUG_OUTPUT);
            GL43.glEnable(GL43.GL_DEBUG_OUTPUT_SYNCHRONOUS);  // Ensure immediate logging

            // Register the debug callback
            GLDebugCallback = GLUtil.setupDebugMessageCallback();

            System.out.println("OpenGL debug output enabled.");
        } else {
            System.err.println("OpenGL debug output not supported on this system.");
        }
    }

    private void run()
    {
        isRunning = true;
        long lastTime = System.nanoTime();

        // Create Camera
        camera = new Camera();
        window.setCursorCallback(camera);

        //Create a demo particle system
        ParticleSystem system = new ParticleSystem();
        system.initializeRenderer();

        while (isRunning)
        {
            long startTime = System.nanoTime();
            long passedTime = startTime - lastTime;
            lastTime = startTime;
            deltaTime = (double) (passedTime / 1_000_000.0);

            //System.out.println(deltaTime + "ms");

            if (window.windowShouldClose())
                stop();

            input();
            update();
            render();

            timeRunning += (float) deltaTime;
            framesRendered++;
            fps = (int) ( 1000.0f / deltaTime);
            window.setTitle(Consts.WINDOW_TITLE + " " + fps + " FPS" + "  AVG FRAME TIME: " + timeRunning / framesRendered  + "ms");

        }
        cleanup();
    }

    private void stop()
    {
        if (!isRunning)
            return;
        isRunning = false;
    }

    private void input()
    {
        if (window.isKeyPressed(GLFW.GLFW_KEY_TAB))
        {
            framesRendered = 0;
            timeRunning = 0.0f;
        }
    }

    private void render()
    {
        renderer.render();
        window.update();
    }

    private void update()
    {
        for (Runnable updateFunction : updatesToRun) {
            updateFunction.run();
        }
    }

    private void cleanup()
    {
        renderer.cleanup();
        window.cleanup();
        if (GLDebugCallback != null) {
            GLDebugCallback.free();
        }
        GLFW.glfwTerminate();
    }

    private int getFPS()
    {
        return fps;
    }

    public static float getDeltaTime()
    {
        return (float) deltaTime;
    }

}