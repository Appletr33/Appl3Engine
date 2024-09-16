package org.example;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

public class Camera {
    public static Camera camera;

    private Matrix4f viewMatrix;
    private Vector3f position;
    private Vector3f front;
    private Vector3f up;
    private Vector3f worldUp;
    private float speed = 0.05f;

    private float yaw = 90.0f;   // Yaw starts facing towards -Z axis
    private float pitch = 0.0f;   // Pitch is level at start
    private float sensitivity = 0.1f; // Mouse sensitivity

    public Camera()
    {
        camera = this;
        EngineManager.engineManager.updatesToRun.add(this::update);

        position = new Vector3f(0.0f, 0.0f, -3.0f);  // Initial position
        front = new Vector3f(0.0f, 0.0f, 1.0f);  // Front direction
        up = new Vector3f(0.0f, 1.0f, 0.0f);    // Up vector
        worldUp = up;

        Vector3f target = new Vector3f(position).add(front);  // Target point
        viewMatrix = new Matrix4f().lookAt(position, target, up);
    }

    private void updateCameraVectors() {
        // Calculate the new front vector
        front.x = (float) Math.cos(Math.toRadians(yaw)) * (float) Math.cos(Math.toRadians(pitch));
        front.y = (float) Math.sin(Math.toRadians(pitch));
        front.z = (float) Math.sin(Math.toRadians(yaw)) * (float) Math.cos(Math.toRadians(pitch));
        front.normalize();
    }

    public void processMouseMovement(double xOffset, double yOffset) {
        xOffset *= sensitivity;
        yOffset *= sensitivity;

        yaw += (float) xOffset;
        pitch += (float) yOffset;

        // Constrain the pitch to avoid the camera flipping
        if (pitch > 89.0f) pitch = 89.0f;
        if (pitch < -89.0f) pitch = -89.0f;

        updateCameraVectors(); // Update the front, right, and up vectors
    }

    public void update()
    {
        float cameraSpeed = speed * EngineManager.getDeltaTime();

        if (Main.getWindow().isKeyPressed(GLFW.GLFW_KEY_LEFT_SHIFT)) {
            cameraSpeed *= 10;
        }
        if (Main.getWindow().isKeyPressed(GLFW.GLFW_KEY_W)) {
            position.add(new Vector3f(front).mul(cameraSpeed));
        }
        if (Main.getWindow().isKeyPressed(GLFW.GLFW_KEY_S)) {
            position.sub(new Vector3f(front).mul(cameraSpeed));
        }
        if (Main.getWindow().isKeyPressed(GLFW.GLFW_KEY_A)) {
            Vector3f right = new Vector3f(front).cross(up).normalize();
            position.sub(right.mul(cameraSpeed));
        }
        if (Main.getWindow().isKeyPressed(GLFW.GLFW_KEY_D)) {
            Vector3f right = new Vector3f(front).cross(up).normalize();
            position.add(right.mul(cameraSpeed));
        }
        if (Main.getWindow().isKeyPressed(GLFW.GLFW_KEY_SPACE)) {
            position.add(new Vector3f(up).mul(cameraSpeed)); // Move up
        }
        if (Main.getWindow().isKeyPressed(GLFW.GLFW_KEY_LEFT_CONTROL)) {
            position.sub(new Vector3f(up).mul(cameraSpeed)); // Move down
        }

        viewMatrix = new Matrix4f().lookAt(position, new Vector3f(position).add(front), up);
    }

    public Matrix4f getViewMatrix()
    {
        return viewMatrix;
    }
}
