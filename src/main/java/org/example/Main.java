package org.example;

import org.example.utils.Consts;
import org.lwjgl.Version;

public class Main
{
    private static WindowManager window;
    private static EngineManager engine;

    public static void main(String[] args)
    {
        System.out.println("[INFO]: Using LWJGL VERSION: " + Version.getVersion());
        window = new WindowManager(Consts.WINDOW_TITLE, 1600, 900, false);
        engine = new EngineManager();
        try
        {
            engine.start();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public static WindowManager getWindow()
    {
        return window;
    }
}