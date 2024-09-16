package org.example.utils;

import jdk.jfr.internal.Utils;
import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBImage;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;



public class Loader {
    public static String loadShader(String filename) throws Exception
    {
        String result;
        try(InputStream in = Utils.class.getResourceAsStream(filename);
        Scanner scanner = new Scanner(in, StandardCharsets.UTF_8.name())) {
            result = scanner.useDelimiter("\\A").next();
        }
        return result;
    }

    // Method to load an image from the resources folder
    public static ByteBuffer loadImage(String fileName, int[] w, int[] h, int[] channels, boolean isTexture) throws Exception {

        if (isTexture)
            STBImage.stbi_set_flip_vertically_on_load(true);
        // Load the image file as InputStream from the resources folder
        try (InputStream inputStream = Utils.class.getResourceAsStream(fileName)) {
            if (inputStream == null) {
                throw new IllegalArgumentException("Image file not found: " + fileName);
            }

            // Read the InputStream into a ByteBuffer
            ByteBuffer imageBuffer;
            try (ReadableByteChannel rbc = Channels.newChannel(inputStream)) {
                imageBuffer = BufferUtils.createByteBuffer(8 * 1024);
                while (true) {
                    int bytes = rbc.read(imageBuffer);
                    if (bytes == -1) break;
                    if (imageBuffer.remaining() == 0) {
                        ByteBuffer newBuffer = BufferUtils.createByteBuffer(imageBuffer.capacity() * 2);
                        imageBuffer.flip();
                        newBuffer.put(imageBuffer);
                        imageBuffer = newBuffer;
                    }
                }
                imageBuffer.flip();
            }

            // Load the image from the ByteBuffer using STBImage
            ByteBuffer image = STBImage.stbi_load_from_memory(imageBuffer, w, h, channels, 4); // 4 for RGBA
            if (image == null) {
                throw new RuntimeException("Failed to load image: " + STBImage.stbi_failure_reason());
            }

            return image;  // This is the raw RGBA image data in a ByteBuffer
        }
    }
}
