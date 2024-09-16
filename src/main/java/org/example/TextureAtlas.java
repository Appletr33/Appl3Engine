package org.example;

import org.example.utils.Loader;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;

import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL45.*;  // For OpenGL 4.5+ functions
import static org.lwjgl.opengl.ARBBindlessTexture.*;  // Bindless texture extension

public class TextureAtlas
{
    private int textureID;
    private long textureHandle;
    private int width, height;
    private float textureSizeX, textureSizeY;
    private int stride;

    public TextureAtlas(String filePath, int textureUniformLocation, int stride)
    {
        this.stride = stride;
        ByteBuffer image;
        // Load image using stb_image
        try (MemoryStack stack = MemoryStack.stackPush())
        {
            int[] w = new int[1];
            int[] h = new int[1];
            int[] channels = new int[1];

            try
            {
                image = Loader.loadImage(filePath, w, h, channels, true);
            }
            catch (Exception e)
            {
                e.printStackTrace();
                return;
            }


            width = w[0];
            height = h[0];

            textureSizeX = (float) stride / width;
            textureSizeY = (float) stride / height;

            // Create OpenGL texture
            textureID = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, textureID);

            // Set texture parameters
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

            // Upload the texture data
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, image);

            // Generate mipmaps
            glGenerateMipmap(GL_TEXTURE_2D);

            textureHandle = glGetTextureHandleARB(textureID);
            glMakeTextureHandleResidentARB(textureHandle);
            glUniformHandleui64ARB(textureUniformLocation, textureHandle);

            GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
            STBImage.stbi_image_free(image);
        }
    }

    public long getHandle()
    {
        return textureHandle;
    }


    public void cleanup()
    {
        glMakeTextureHandleNonResidentARB(textureHandle);
        glDeleteTextures(textureID);
    }

    public int getWidth()
    {
        return width;
    }

    public int getStride()
    {
        return stride;
    }

    public int getHeight()
    {
        return height;
    }

    public float getTextureSizeX()
    {
        return textureSizeX;
    }

    public float getTextureSizeY()
    {
        return textureSizeY;
    }
}
