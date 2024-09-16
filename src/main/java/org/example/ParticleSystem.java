package org.example;

public class ParticleSystem {
    private Particle[] particles;
    private ParticleRenderer particleRenderer;

    private void initializeRenderer()
    {
        try
        {
            particleRenderer = new ParticleRenderer(particles);
        }
        catch (Exception e)
        {
            System.out.println("[ERROR]: Unable to create particle renderer");
            e.printStackTrace();
        }

        EngineManager.engineManager.updatesToRun.add(this::update);
    }

    public void initializeGrid(int gridSize)
    {
        particles = new Particle[gridSize * gridSize * gridSize];

        int count = 0;
        for (int y = 0; y < gridSize; y++)
        {
            for (int z = 0; z < gridSize; z++)
            {
                for (int x = 0; x < gridSize; x++)
                {
                    Particle p = new Particle(x,y,z);
                    particles[count++] = p;
                }
            }
        }


        initializeRenderer();
    }

    public void initializeSquare(int perimeter)
    {
        particles = new Particle[perimeter * perimeter];

        int count = 0;
        for (int y = 0; y < perimeter; y++)
        {
            for (int x = 0; x < perimeter; x++)
            {
                Particle p = new Particle(x, y,0.0f);
                particles[count++] = p;
            }
        }


        initializeRenderer();
    }

    public void update()
    {

    }
}
