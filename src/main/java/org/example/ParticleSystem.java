package org.example;

public class ParticleSystem {
    private ParticleRenderer particleRenderer;

    public void initializeRenderer()
    {
        try
        {
            particleRenderer = new ParticleRenderer();
        }
        catch (Exception e)
        {
            System.out.println("[ERROR]: Unable to create particle renderer");
            e.printStackTrace();
        }

        EngineManager.engineManager.updatesToRun.add(this::update);
    }


    public void update()
    {

    }
}
