
import com.hackoeur.jglm.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import javax.imageio.ImageIO;
import javax.media.opengl.GL;
import javax.media.opengl.GL4;
import javax.media.opengl.GL4bc;

public class ColourMonkey
{

    /**
     * Character code for the escape key in GLUT.
     */
    static final int KEY_ESCAPE = 27;
    /**
     * The window's initial width.
     */
    static final int WINDOW_WIDTH = 1280;
    /**
     * The window's initial height.
     */
    static final int WINDOW_HEIGHT = 768;
    
    int w_width = WINDOW_WIDTH, w_height = WINDOW_HEIGHT;
    float aspect;
    
    ArrayList<Shader> shaders = new ArrayList<Shader>();
    int shaderIndex;
    Shader currentShader;
    ArrayList<Mesh> meshes = new ArrayList<Mesh>();
    int meshIndex;
    Mesh currentMesh;
    
    Shader sbShader;
    SkyBox ndcQuad;
    
    Shader waterShader, cloudShader;
    Grid water, clouds;
    
    FrameBuffer reflectBuffer;
    FrameBuffer postBuffer;
    
    Shader postProcess;
    int ssaa = 4; // amount of SSAA to apply;
    
    /*VoxelShader vShader;
    public static final int ci = 2, cj = 2, ck = 2;
    VoxelChunk[] chunks;*/
    
    Shader tShader;
    Terrain terrain;

    /* Amount to rotate by in one step. */
    static final float ANGLE_DELTA = 60.0f;
    /* Amount to move / scale by in one step. */
    static final float DELTA = 5f;

    /* Amount of rotation around the x axis. */
    float xRot = 0.0f;
    /* Amount of rotation around the y axis. */
    float yRot = 0.0f;
    /* Amount of rotation around the z axis. */
    float zRot = 0.0f;
    
    float camRotX = 0.0f;
    float camRotY = 0.0f;

    /* Amount to scale the x axis by. */
    float xScale = 1.0f;
    /* Amount to scale the y axis by. */
    float yScale = 1.0f;
    /* Amount to scale the z axis by. */
    float zScale = 1.0f;

    /* Amount to move on the x axis. */
    float xMove = 0.0f;
    /* Amount to move on the y axis. */
    float yMove = 0.0f;
    /* Amount to move on the z axis. */
    float zMove = 0.0f;

    /* Camera location. */
    Vec3 cameraEye = new Vec3(2f, 2f, 2f);
    /* Vector the camera is looking along. */
    Vec3 cameraAt = new Vec3(0f, 0f, 0f);
    /* Up direction from the camera. */
    Vec3 cameraUp = new Vec3(0f, 1f, 0f);

    /* Vertical field-of-view. */
    float FOVY = 90.0f;
    /* Near clipping plane. */
    float NEAR = 1.0f;
    /* Far clipping plane. */
    float FAR = -100.0f;
    
    float water_level = -5.0f;
    
    float cloud_level = 50.0f;
    float cloud_speed = 0.5f;
    float cloud_density = 0.90f;
    
    float fog_start = 150.0f;
    float fog_end = 250.0f;
    
    Mat4 world, view, projection;
    Mat4 mirror_view;
    
    Vec3 sun = new Vec3(-2.0f,1.5f,5.0f);
    
    Vec4 clipPlane = new Vec4(0.0f, 1.0f, 0.0f, 0.0f);
    Mat4 clipWorld = Matrices.translate(new Mat4(1.0f), new Vec3(0.0f, -water_level, 0.0f));
    
    boolean[] keys = new boolean[256];
    
    long lastUpdate;
    float time = 0.0f;
            
    public static final long NANOS_PER_SECOND = 1000000000l;
    
    void cleanUp(GL4 gl)
    {
        // TODO: release meshes and shaders here. EEEK!!!
    }

    void display(GL4 gl)
    {
        update();
        render(gl);
    }
    
    void update()
    {
        long nTime = System.nanoTime();
        long nanos = nTime-lastUpdate;
        lastUpdate = nTime;
        float delta = nanos/(float)NANOS_PER_SECOND; 
        time += delta;
        
        float step = DELTA;
        
        if (keys[KeyEvent.VK_SHIFT]) step *= 5;
        
        Vec3 d = cameraAt.subtract(cameraEye).getUnitVector().multiply(step*delta);
        Vec3 r = d.cross(cameraUp).getUnitVector().multiply(step*delta);

        if (keys[KeyEvent.VK_W])
        {
           cameraEye = cameraEye.add(d);
           cameraAt = cameraAt.add(d);
        }
        else if (keys[KeyEvent.VK_S])
        {
           cameraEye = cameraEye.subtract(d);
           cameraAt = cameraAt.subtract(d);
        }

        if (keys[KeyEvent.VK_D])
        {
           cameraEye = cameraEye.add(r);
           cameraAt = cameraAt.add(r);
        }
        else if (keys[KeyEvent.VK_A])
        {
           cameraEye = cameraEye.subtract(r);
           cameraAt = cameraAt.subtract(r);
        }
        
        updateView();
    }
    
    void render(GL4 gl)
    {
        reflectBuffer.use(gl);
        
            gl.glEnable(GL4.GL_CLIP_DISTANCE0);
            renderScene(gl, mirror_view);
            gl.glDisable(GL4.GL_CLIP_DISTANCE0);
        
        postBuffer.use(gl);
        
            renderScene(gl, view);

            reflectBuffer.bindTexture(gl, GL.GL_TEXTURE6);
            renderWater(gl, view);
            
        FrameBuffer.unbind(gl);
        
            gl.glViewport(0, 0, w_width, w_height);
            gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

            postBuffer.bindTexture(gl, GL.GL_TEXTURE7);

            renderPostProcessing(gl);

        gl.glFlush();
    }
    
    void renderScene(GL4 gl, Mat4 camera)
    {
        renderSkybox(gl, camera);
        renderTerrain(gl, camera);
        renderMesh(gl, camera);
        renderClouds(gl, camera, time);
    }
    
    void renderPostProcessing(GL4 gl)
    {
        postProcess.use(gl);
        
        postProcess.updateUniform(gl, "screenWidth", w_width);
        postProcess.updateUniform(gl, "screenHeight", w_height);
        postProcess.updateUniform(gl, "ssaa", ssaa);
        
        ndcQuad.draw(gl);
    }
    
    void renderSkybox(GL4 gl, Mat4 camera)
    {
        sbShader.use(gl);
        
        sbShader.updateUniform(gl, "projection", projection);
        sbShader.updateUniform(gl, "view", camera);
        sbShader.updateUniform(gl, "aspect", aspect);
        sbShader.updateUniform(gl, "sun", sun);
        
        ndcQuad.draw(gl);
    }
    
    void renderTerrain(GL4 gl, Mat4 camera)
    {
        tShader.use(gl);
        
        tShader.updateUniform(gl, "world", world);
        tShader.updateUniform(gl, "view", camera);
        tShader.updateUniform(gl, "projection", projection);
        tShader.updateUniform(gl, "sun", sun);
        tShader.updateUniform(gl, "fog_start", fog_start);
        tShader.updateUniform(gl, "fog_end", fog_end);
        tShader.updateUniform(gl, "clipPlane", clipPlane);
        tShader.updateUniform(gl, "clipWorld", clipWorld);
        
        terrain.draw(gl);
    }
    
    void renderMesh(GL4 gl, Mat4 camera)
    {
        currentShader.use(gl);
        
        currentShader.updateUniform(gl, "world", world);
        currentShader.updateUniform(gl, "view", camera);
        currentShader.updateUniform(gl, "projection", projection);
        currentShader.updateUniform(gl, "sun", sun);
        
        currentMesh.draw(gl);
    }
    
    void renderClouds(GL4 gl, Mat4 camera, float time)
    {
        
        
        gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
        gl.glEnable(GL.GL_BLEND);
        cloudShader.use(gl);
        
        cloudShader.updateUniform(gl, "world", world);
        cloudShader.updateUniform(gl, "view", camera);
        cloudShader.updateUniform(gl, "projection", projection);
        cloudShader.updateUniform(gl, "time", time);
        cloudShader.updateUniform(gl, "sun", sun);
        cloudShader.updateUniform(gl, "fog_start", fog_start);
        cloudShader.updateUniform(gl, "fog_end", fog_end);
        
        cloudShader.updateUniform(gl, "cloud_height", cloud_level);
        cloudShader.updateUniform(gl, "speed", cloud_speed);
        cloudShader.updateUniform(gl, "density", cloud_density);
        
        clouds.draw(gl);
        gl.glDisable(GL.GL_BLEND);
        
    }
    
    void renderWater(GL4 gl, Mat4 camera)
    {
        
        gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
        gl.glEnable(GL.GL_BLEND);
        waterShader.use(gl);
        
        waterShader.updateUniform(gl, "world", world);
        waterShader.updateUniform(gl, "view", camera);
        waterShader.updateUniform(gl, "projection", projection);
        waterShader.updateUniform(gl, "sun", sun);
        waterShader.updateUniform(gl, "time", time);
        waterShader.updateUniform(gl, "fog_start", fog_start);
        waterShader.updateUniform(gl, "fog_end", fog_end);
        
        waterShader.updateUniform(gl, "screenWidth", w_width*ssaa);
        waterShader.updateUniform(gl, "screenHeight", w_height*ssaa);
        
        water.draw(gl);
        gl.glDisable(GL.GL_BLEND);
    }

    void init(GL4 gl)
    {
        gl.glEnable(GL4bc.GL_VERTEX_ARRAY);
        gl.glEnable(GL.GL_DEPTH_TEST);
        gl.glEnable(GL.GL_CULL_FACE);
        gl.glEnable(GL.GL_MULTISAMPLE);
        //gl.glEnable(GL.GL_TEXTURE_CUBE_MAP);
        //gl.glPolygonMode( GL.GL_FRONT_AND_BACK, GL4.GL_LINE );
        
        gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        
        postBuffer = new FrameBuffer(gl, w_width*ssaa, w_height*ssaa);
        postProcess = new Shader(gl, "postprocess");

        ndcQuad = new SkyBox(gl);
        
        File meshDir = new File("meshes");
        String[] meshNames = meshDir.list();
        Arrays.sort(meshNames);
        for (int i = 0; i < meshNames.length; i++)
        {
            meshes.add(new WavefrontMesh(gl, meshNames[i]));
        }
        meshIndex = 0;
        currentMesh = meshes.get(meshIndex);
        
        File shaderDir = new File("shaders");
        String[] shaderNames = shaderDir.list();
        Arrays.sort(shaderNames);
        for (int i = 0; i < shaderNames.length; i++)
        {
            if ("skybox".equals(shaderNames[i]) || "terrain".equals(shaderNames[i])
                    || "voxels".equals(shaderNames[i]) || "water".equals(shaderNames[i]) )
                continue;
            shaders.add(new Shader(gl, shaderNames[i]));
        }
        shaderIndex = 0;
        currentShader = shaders.get(shaderIndex);
        
        waterShader = new Shader(gl, "water");
        water = new Grid(gl, 512, 512, 20, 20, water_level);
        
        cloudShader = new Shader(gl, "clouds");
        clouds = new Grid(gl, 2048, 2048, 256, 256, 0, true);
        
        reflectBuffer = new FrameBuffer(gl, w_width, w_height);
        
        sbShader = new Shader(gl, "skybox");
        
        /*chunks = new VoxelChunk[ci*cj*ck];
        for (int i = 0; i < chunks.length; i++)
        {
            chunks[i] = new VoxelChunk(gl, 32); // TODO: It's that blasted MAGIC NUMBER again!!??
        }
        
        vShader = new VoxelShader(gl, "voxels");*/
        
        tShader = new Shader(gl, "terrain");
        
        try
        {
            BufferedImage heightmap = ImageIO.read(new File("textures/heightmap.png"));
            terrain = new Terrain(gl,
                    512, // width
                    512, // length
                    50, // elevation
                    512,512, // grid resolution
                    heightmap);
        } catch (IOException ex)
        {
            System.err.println("Could not load heightmap: " + ex.getMessage());
        }
        
        try
        {
            int w_h[] = new int[2];
            ByteBuffer data = Utils.loadTexture("textures/Stone1.jpg", w_h);
            Utils.loadTexture(gl, data, w_h[0], w_h[1], GL.GL_TEXTURE0);
            
            data = Utils.loadTexture("textures/Grass.jpg", w_h);
            Utils.loadTexture(gl, data, w_h[0], w_h[1], GL.GL_TEXTURE1);
            
            data = Utils.loadTexture("textures/Stone1.jpg", w_h);
            Utils.loadTexture(gl, data, w_h[0], w_h[1], GL.GL_TEXTURE2);
            
            data = Utils.loadTexture("textures/Stone1_N.jpg", w_h);
            Utils.loadTexture(gl, data, w_h[0], w_h[1], GL.GL_TEXTURE3);
            
            data = Utils.loadTexture("textures/Grass_N.jpg", w_h);
            Utils.loadTexture(gl, data, w_h[0], w_h[1], GL.GL_TEXTURE4);
            
            data = Utils.loadTexture("textures/Stone1_N.jpg", w_h);
            Utils.loadTexture(gl, data, w_h[0], w_h[1], GL.GL_TEXTURE5);
        } catch (IOException ex)
        {
            System.err.println("Could not load texture: " + ex.getMessage());
        }

        updateProjection(WINDOW_WIDTH, WINDOW_HEIGHT);
        updateView();
        updateWorld();
        
        lastUpdate = System.nanoTime();
    }

    void keyPressed(KeyEvent ke)
    {
        char key = ke.getKeyChar();
        if (ke.getKeyCode() < keys.length)
            keys[ke.getKeyCode()] = true;

        switch (key)
        {
            case ',':
                shaderIndex--;
                if (shaderIndex < 0) shaderIndex = shaders.size()-1;
                currentShader = shaders.get(shaderIndex);
                break;
            case '.':
                shaderIndex = (shaderIndex+1)%shaders.size();
                currentShader = shaders.get(shaderIndex);
                break;
            case '[':
                meshIndex--;
                if (meshIndex < 0) meshIndex = meshes.size()-1;
                currentMesh = meshes.get(meshIndex);
                break;
            case ']':
                meshIndex = (meshIndex+1)%meshes.size();
                currentMesh = meshes.get(meshIndex);
                break;
            case KEY_ESCAPE:
                System.out.printf("Bye!\n");
                System.exit(0);
                break;
            default:
                break;
        }
    }
    
    void keyReleased(KeyEvent ke)
    {
        if (ke.getKeyCode() < keys.length)
            keys[ke.getKeyCode()] = false;
    }

    void mouseMoved(int x, int y)
    {
        final float LEFT_RIGHT_ROT = (2.0f*(float)x/(float)w_width) * ANGLE_DELTA;
        final float UP_DOWN_ROT = (2.0f*(float)y/(float)w_height) * ANGLE_DELTA;

        Vec3 tempD = cameraAt.subtract(cameraEye);
        Vec4 d = new Vec4(tempD.getX(), tempD.getY(), tempD.getZ(), 0.0f);

        Vec3 right = tempD.cross(cameraUp);

        Mat4 rot = new Mat4(1.0f);
        rot = Matrices.rotate(rot, UP_DOWN_ROT, right);
        rot = Matrices.rotate(rot, LEFT_RIGHT_ROT, cameraUp);

        d = rot.multiply(d);

        cameraAt = cameraEye.add(new Vec3(d.getX(),d.getY(),d.getZ()));
        
        updateView();

    }

    void reshape(GL4 gl, int newWidth, int newHeight)
    {
        w_width = newWidth;
        w_height = newHeight;
        
        /* Correct the viewport. */
        gl.glViewport(0, 0, newWidth, newHeight);

        /* Correct the projection matrix. */
        updateProjection(newWidth, newHeight);

        /* Note the new width and height of the window. */
        System.out.printf("Resized. New width = %d and new height = %d.\n", newWidth, newHeight);

        /* Redraw the scene if the window changed. */
        //glutPostRedisplay();
    }

    void updateProjection(int width, int height)
    {
        aspect = (float) width / (float) height;

        projection = Matrices.perspective(FOVY, aspect, NEAR, FAR);
    }

    void updateView()
    {
        view = Matrices.lookAt(cameraEye, cameraAt, cameraUp);
        
        mirror_view = Matrices.lookAt(
                new Vec3(cameraEye.getX(),2*water_level-cameraEye.getY(),cameraEye.getZ()),
                new Vec3(cameraAt.getX(),2*water_level-cameraAt.getY(),cameraAt.getZ()),
                cameraUp);
    }

    void updateWorld()
    {
        world = new Mat4(1f);

        /*Vec3 translation = new Vec3(xMove, yMove, zMove);
        world = Matrices.translate(world, translation);

        Vec3 xAxis = new Vec3(1, 0, 0);
        world = Matrices.rotate(world, xRot, xAxis);

        Vec3 yAxis = new Vec3(0, 1, 0);
        world = Matrices.rotate(world, yRot, yAxis);

        Vec3 zAxis = new Vec3(0, 0, 1);
        world = Matrices.rotate(world, zRot, zAxis);

        Vec3 scales = new Vec3(xScale, yScale, zScale);
        world = Matrices.scale(world, scales);*/
    }
}
