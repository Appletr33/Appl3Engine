package org.example;

import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.system.MemoryUtil;

public class WindowManager
{
    public static final float FOV = (float) Math.toRadians(60);
    public static final float Z_NEAR = 0.01f;
    public static final float Z_FAR = 1000f;

    private final String title;

    private int width, height;
    private long window;

    private boolean resize, vSync;

    private final Matrix4f projectionMatrix;

    private double lastMouseX, lastMouseY; // Initial mouse position (center of screen)

    private boolean mouseLocked = false;


    public WindowManager(String title, int width, int height, boolean vSync)
    {
        this.title = title;
        this.width = width;
        this.height = height;
        this.vSync = vSync;
        projectionMatrix = new Matrix4f();
    }

    public void init()
    {
        GLFWErrorCallback.createPrint(System.err).set();

        System.out.println("Initializing GLFW...");
        if (!GLFW.glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }
        System.out.println("GLFW initialized successfully");

        GLFW.glfwDefaultWindowHints();
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GL11.GL_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GL11.GL_TRUE);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 4);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 5);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_FORWARD_COMPAT, GL11.GL_TRUE);

        // Check if the operating system is macOS
        if (System.getProperty("os.name").toLowerCase().contains("mac")) {
            System.out.println("Running Mac Configurations");
            GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_FORWARD_COMPAT, GL11.GL_TRUE); // macOS-specific
            GLFW.glfwWindowHint(GLFW.GLFW_COCOA_RETINA_FRAMEBUFFER, GLFW.GLFW_FALSE); // macOS-specific
            GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_DEBUG_CONTEXT, GLFW.GLFW_TRUE); // For debugging
        }


        boolean maximized = false;
        if (width == 0 || height == 0)
        {
            width = 100;
            height = 100;
            GLFW.glfwWindowHint(GLFW.GLFW_MAXIMIZED, GLFW.GLFW_TRUE);
            maximized = true;
        }
        window = GLFW.glfwCreateWindow(width, height, title, MemoryUtil.NULL, MemoryUtil.NULL);
        if (window == MemoryUtil.NULL)
            throw new RuntimeException("Failed to create window");

        GLFW.glfwSetFramebufferSizeCallback(window, (window, width, height) ->
        {
            this.width = width;
            this.height = height;
            this.setResize(true);
        });

        GLFW.glfwSetKeyCallback(window, (window, key, scancode, action, mods) -> {
           if (key == GLFW.GLFW_KEY_ESCAPE && action == GLFW.GLFW_RELEASE)
           {
               if (mouseLocked)
               {
                   GLFW.glfwSetInputMode(window, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL);
                   mouseLocked = false;
               }
               else
               {
                   GLFW.glfwSetWindowShouldClose(window, true);
               }
           }

        });
        if (maximized)
        {
            GLFW.glfwMaximizeWindow(window);
        }
        else
        {
            GLFWVidMode vidMode = GLFW.glfwGetVideoMode(GLFW.glfwGetPrimaryMonitor());
            GLFW.glfwSetWindowPos(window, (vidMode.width() - width) / 2, (vidMode.height() - height) / 2);
        }

        GLFW.glfwMakeContextCurrent(window);

        GLFW.glfwSwapInterval(0);
        if (isvSync())
            GLFW.glfwSwapInterval(1);


        GLFW.glfwShowWindow(window);
        GLFW.glfwFocusWindow(window);

        GL.createCapabilities();

        GL11.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_STENCIL_TEST);
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_BACK);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        GL20.glViewport(0, 0, width, height);

        lastMouseX = (double) width / 2;
        lastMouseY = (double) height / 2;

        GLFW.glfwSetMouseButtonCallback(window, (window, button, action, mods) ->
        {
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && action == GLFW.GLFW_PRESS)
            {
                GLFW.glfwSetInputMode(window, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED); // Lock the cursor back on left-click
                mouseLocked = true;
            }
        });

        // Register the window resize callback
        GLFW.glfwSetFramebufferSizeCallback(window, (windowHandle, width, height) ->
        {
            GL20.glViewport(0, 0, width, height);  // Adjust viewport when window is resized
        });

        updateProjectMatrix();

        if (!GL.getCapabilities().OpenGL33) {
            throw new IllegalStateException("OpenGL 3.3 is required but not supported on this system.");
        }

        System.out.println("Window Initialized!");
    }

    public void update()
    {
        GLFW.glfwSwapBuffers(window);
        GLFW.glfwPollEvents();
    }

    public void cleanup()
    {
        GLFW.glfwDestroyWindow(window);
    }

    public void SetClearColor(float r, float g, float b, float a)
    {
        GL11.glClearColor(r, g, b, a);
    }

    public boolean isKeyPressed(int keycode)
    {
        return GLFW.glfwGetKey(window, keycode) == GLFW.GLFW_PRESS;
    }

    public boolean windowShouldClose()
    {
        return GLFW.glfwWindowShouldClose(window);
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title)
    {
        GLFW.glfwSetWindowTitle(window, title);
    }

    public boolean isResize() {
        return resize;
    }

    public void setResize(boolean resize) {
        this.resize = resize;
    }

    public boolean isvSync() {
        return vSync;
    }

    public void setvSync(boolean vSync) {
        this.vSync = vSync;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public long getWindow() {
        return window;
    }

    public Matrix4f getProjectionMatrix() {
        return projectionMatrix;
    }

    public Matrix4f updateProjectMatrix()
    {
        float aspectRatio = (float) width / height;
        return projectionMatrix.setPerspective(FOV, aspectRatio, Z_NEAR, Z_FAR);
    }

    public Matrix4f updateProjectMatrix(Matrix4f matrix, int width, int height)
    {
        float aspectRatio = (float) width / height;
        return projectionMatrix.setPerspective(FOV, aspectRatio, Z_NEAR, Z_FAR);
    }

    public void setCursorCallback(Camera camera)
    {
        GLFW.glfwSetInputMode(window, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED);
        mouseLocked = true;

        GLFW.glfwSetCursorPosCallback(window, (window, xpos, ypos) ->
        {
            double xOffset = xpos - lastMouseX;
            double yOffset = lastMouseY - ypos; // Inverted since y-coordinates go from bottom to top

            lastMouseX = xpos;
            lastMouseY = ypos;
            camera.processMouseMovement(xOffset, yOffset);
        });

    }
}
