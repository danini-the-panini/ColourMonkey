
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
    
    Shader shader;
    ArrayList<Mesh> meshes = new ArrayList<Mesh>();
    int meshIndex;
    Mesh currentMesh;
    
    Shader sbShader;
    NDCQuad ndcQuad;
    
    Shader waterShader, cloudShader;
    Grid water, clouds;
    
    FrameBuffer reflectBuffer;
    FrameBuffer postBuffer;
    FrameBuffer shadowBuffer;
    
    Shader postProcess, showoff;
    int ssaa = 2; // amount of SSAA to apply;
    
    int[] terrainTextures = new int[6];
    int[] grassTextures = new int[4];
    int cloudTexture;
    
    Shader tShader, tpShader;
    Terrain terrain;
    Terrain terrainPoints;

    /* Amount to rotate by in one step. */
    static final float ANGLE_DELTA = 60.0f;
    /* Amount to move / scale by in one step. */
    static final float DELTA = 5f;

    /* Amount of rotation around the x axis. */
    float xRot = -35.0f;
    /* Amount of rotation around the y axis. */
    float yRot = 0.0f;
    /* Amount of rotation around the z axis. */
    float zRot = 0.0f;
    
    float camRotX = 0.0f;
    float camRotY = 0.0f;

    /* Amount to scale the x axis by. */
    float xScale = 5.0f;
    /* Amount to scale the y axis by. */
    float yScale = 5.0f;
    /* Amount to scale the z axis by. */
    float zScale = 5.0f;

    /* Amount to move on the x axis. */
    float xMove = -30.0f;
    /* Amount to move on the y axis. */
    float yMove = -1.8f;
    /* Amount to move on the z axis. */
    float zMove = 3.0f;

    /* Camera location. */
    Vec3 cameraEye = new Vec3(2f, 2f, 2f);
    /* Vector the camera is looking along. */
    Vec3 cameraAt = new Vec3(0f, 0f, 0f);
    /* Up direction from the camera. */
    Vec3 cameraUp = new Vec3(0f, 1f, 0f);

    /* Vertical field-of-view. */
    float FOVY = 60.0f;
    /* Near clipping plane. */
    float NEAR = 1.0f;
    /* Far clipping plane. */
    float FAR = -100.0f;
    
    float water_level = -5.0f;
    
    float cloud_level = 150.0f;
    float cloud_speed = 0.5f;
    float cloud_density = 0.50f;
    
    float fog_start = 150.0f;
    float fog_end = 250.0f;
    
    Mat4 monkeyWorld;
           
    Mat4 view, projection, mirror_view;
    
    Mat4 lightView, lightProjection;
    
    Vec3 origin_sun = new Vec3(-2.0f,1.5f,5.0f);
    Vec3 sun = new Vec3(origin_sun);
    
    float daytime = 0.0f;
    
    Vec3 lightEye = new Vec3(sun).multiply(10),
            lightAt = new Vec3(0f, 0f, 0f),
            lightUp = new Vec3(0f, 1f, 0f);
    
    int shadowRes = 8192;
    
    Vec4 clipPlane = new Vec4(0.0f, 1.0f, 0.0f, 0.0f);
    Mat4 clipWorld = Matrices.translate(new Mat4(1.0f), new Vec3(0.0f, -water_level, 0.0f));
    
    boolean[] keys = new boolean[256];
    
    long lastUpdate;
    float time = 0.0f;
    
    boolean shadowToggle = true, ssaaToggle = false;
            
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

	Main.jframe.setTitle(String.format("Colour Monkey! FPS: %.2f",1.0f/delta));
        
        float step = DELTA;
        
        yRot += delta*50;
        updateMonkeyWorld();
        
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
        else if (keys[KeyEvent.VK_2])
        {
            daytime += 1.0f;
            updateSun();
        }
        else if (keys[KeyEvent.VK_3])
        {
            daytime -= 1.0f;
            updateSun();
        }
        
        updateView();
    }
    
    void render(GL4 gl)
    {
        /*shadowBuffer.use(gl);
        
            gl.glCullFace(GL4.GL_FRONT);
            renderScene(gl, lightView, lightProjection, false);
            gl.glCullFace(GL4.GL_BACK);
        
        reflectBuffer.use(gl);
        
            gl.glEnable(GL4.GL_CLIP_DISTANCE0);
            renderScene(gl, mirror_view, true);
            renderGrass(gl, mirror_view, projection);
            gl.glDisable(GL4.GL_CLIP_DISTANCE0);*/
        
        if (ssaaToggle)
        {
            postBuffer.use(gl);
        }
        else
        {
            FrameBuffer.unbind(gl);

            gl.glViewport(0, 0, w_width, w_height);
            gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
        }
        
            shadowBuffer.bindDepthBuffer(gl, GL4.GL_TEXTURE8);
        
            renderScene(gl, true);

            reflectBuffer.bindTexture(gl, 0, GL.GL_TEXTURE6);
        //    renderWater(gl, view, projection);
            
        //    renderGrass(gl, view, projection);
            
        if (ssaaToggle)
        {
            FrameBuffer.unbind(gl);

            gl.glViewport(0, 0, w_width, w_height);
            gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

                postBuffer.bindTexture(gl, 0, GL.GL_TEXTURE7);

                gl.glDisable(GL.GL_DEPTH_TEST);
                renderPostProcessing(gl);

                shadowBuffer.bindDepthBuffer(gl, GL.GL_TEXTURE7);
                renderShowoff(gl, w_width-110, w_height-110, 100, 100);
                gl.glEnable(GL.GL_DEPTH_TEST);
        }

        gl.glFlush();
    }
    
    void renderScene(GL4 gl, boolean full)
    {
        renderScene(gl, view, full);
    }
    void renderScene(GL4 gl, Mat4 camera, boolean full)
    {
        renderScene(gl, camera, projection, full);
    }
    void renderScene(GL4 gl, Mat4 camera, Mat4 proj, boolean full)
    {
//        renderSkybox(gl, camera, proj);
        renderTerrain(gl, camera, proj);
        //renderMesh(gl, camera, proj);
//        if (full) renderClouds(gl, camera, proj, time);
    }
    
    void renderPostProcessing(GL4 gl)
    {
        postProcess.use(gl);
        
        postProcess.updateUniform(gl, "screenWidth", w_width);
        postProcess.updateUniform(gl, "screenHeight", w_height);
        postProcess.updateUniform(gl, "ssaa", ssaa);
        
        ndcQuad.draw(gl);
    }
    
    void renderShowoff(GL4 gl, int x, int y, int width, int height)
    {
        showoff.use(gl);
    
        gl.glViewport(x, y, width, height);
        showoff.updateUniform(gl, "screenX", x);
        showoff.updateUniform(gl, "screenY", y);
        showoff.updateUniform(gl, "screenWidth", width);
        showoff.updateUniform(gl, "screenHeight", height);
        
        ndcQuad.draw(gl);
    }
    
    void renderSkybox(GL4 gl, Mat4 camera, Mat4 proj)
    {
        sbShader.use(gl);
        
        sbShader.updateUniform(gl, "view", camera);
        sbShader.updateUniform(gl, "projection", proj);
        sbShader.updateUniform(gl, "aspect", aspect);
        sbShader.updateUniform(gl, "sun", sun);
        
        ndcQuad.draw(gl);
    }
    
    void renderTerrain(GL4 gl, Mat4 camera, Mat4 proj)
    {
        tShader.use(gl);
        
        for (int i = 0; i < terrainTextures.length; i++)
        {
            gl.glActiveTexture(GL.GL_TEXTURE0+i);
            gl.glBindTexture(GL.GL_TEXTURE_2D, terrainTextures[i]);
        }
        
        tShader.updateUniform(gl, "world", new Mat4(1.0f));
        tShader.updateUniform(gl, "view", camera);
        tShader.updateUniform(gl, "projection", proj);
        
        tShader.updateUniform(gl, "lview", lightView);
        tShader.updateUniform(gl, "lprojection", lightProjection);
        
        tShader.updateUniform(gl, "sun", sun);
        tShader.updateUniform(gl, "shadowToggle", shadowToggle ? 1 : 0);
        
        tShader.updateUniform(gl, "fog_start", fog_start);
        tShader.updateUniform(gl, "fog_end", fog_end);
        
        tShader.updateUniform(gl, "clipPlane", clipPlane);
        tShader.updateUniform(gl, "clipWorld", clipWorld);
        
        terrain.draw(gl);
    }
    
    void renderGrass(GL4 gl, Mat4 camera, Mat4 proj)
    {
        gl.glEnable(GL.GL_BLEND);
        gl.glDisable(GL.GL_CULL_FACE);
        tpShader.use(gl);
        
        for (int i = 0; i < grassTextures.length; i++)
        {
            gl.glActiveTexture(GL.GL_TEXTURE0+i);
            gl.glBindTexture(GL.GL_TEXTURE_2D, grassTextures[i]);
        }
        
        tpShader.updateUniform(gl, "world", new Mat4(1.0f));
        tpShader.updateUniform(gl, "view", camera);
        tpShader.updateUniform(gl, "projection", proj);
        
        tpShader.updateUniform(gl, "lview", lightView);
        tpShader.updateUniform(gl, "lprojection", lightProjection);
        
        tpShader.updateUniform(gl, "sun", sun);
        tpShader.updateUniform(gl, "shadowToggle", shadowToggle ? 1 : 0);
        
        tpShader.updateUniform(gl, "fog_start", fog_start);
        tpShader.updateUniform(gl, "fog_end", fog_end);
        
        tpShader.updateUniform(gl, "water_level", water_level);
        
        tpShader.updateUniform(gl, "time", time);
        
        terrainPoints.drawPoints(gl);
        gl.glEnable(GL.GL_CULL_FACE);
        gl.glDisable(GL.GL_BLEND);
    }
    
    void renderMesh(GL4 gl, Mat4 camera, Mat4 proj)
    {
        shader.use(gl);
        
        shader.updateUniform(gl, "world", monkeyWorld);
        shader.updateUniform(gl, "view", camera);
        shader.updateUniform(gl, "projection", proj);
        
        shader.updateUniform(gl, "lview", lightView);
        shader.updateUniform(gl, "lprojection", lightProjection);
        
        shader.updateUniform(gl, "sun", sun);
        shader.updateUniform(gl, "shadowToggle", shadowToggle ? 1 : 0);
        
        currentMesh.draw(gl);
    }
    
    void renderClouds(GL4 gl, Mat4 camera, Mat4 proj, float time)
    {
        gl.glActiveTexture(GL.GL_TEXTURE0);
        gl.glBindTexture(GL.GL_TEXTURE_2D, cloudTexture);
        
        gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
        gl.glEnable(GL.GL_BLEND);
        cloudShader.use(gl);
        
        cloudShader.updateUniform(gl, "world", new Mat4(1.0f));
        cloudShader.updateUniform(gl, "view", camera);
        cloudShader.updateUniform(gl, "projection", proj);
        cloudShader.updateUniform(gl, "time", time);
        cloudShader.updateUniform(gl, "sun", sun);
        cloudShader.updateUniform(gl, "fog_start", fog_start*2);
        cloudShader.updateUniform(gl, "fog_end", fog_end*2);
        
        cloudShader.updateUniform(gl, "cloud_height", cloud_level);
        cloudShader.updateUniform(gl, "speed", cloud_speed);
        cloudShader.updateUniform(gl, "density", cloud_density);
        
        clouds.draw(gl);
        gl.glDisable(GL.GL_BLEND);
        
    }
    
    void renderWater(GL4 gl, Mat4 camera, Mat4 proj)
    {
        
        gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
        gl.glEnable(GL.GL_BLEND);
        waterShader.use(gl);
        
        waterShader.updateUniform(gl, "world", new Mat4(1.0f));
        waterShader.updateUniform(gl, "view", camera);
        waterShader.updateUniform(gl, "projection", proj);
        waterShader.updateUniform(gl, "sun", sun);
        waterShader.updateUniform(gl, "time", time);
        waterShader.updateUniform(gl, "fog_start", fog_start);
        waterShader.updateUniform(gl, "fog_end", fog_end);
        
        waterShader.updateUniform(gl, "screenWidth", w_width*(ssaaToggle?ssaa:1));
        waterShader.updateUniform(gl, "screenHeight", w_height*(ssaaToggle?ssaa:1));
        
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
        
        lightProjection = Matrices.ortho(-300, 300, -50, 50, 10f, 250); // TODO: light projection
        
        shadowBuffer = new FrameBuffer(gl, shadowRes, shadowRes, null, true);
        
        postBuffer = new FrameBuffer(gl, w_width*ssaa, w_height*ssaa,
                new int[]{GL4.GL_TEXTURE_2D}, false);
        postProcess = new Shader(gl, "postprocess");
        showoff = new Shader(gl, "showoff");
        
        // TODO: skymapBuffer = new FrameBuffer(gl, 1024, 1024, new int[]{ GL4.GL_TEXTURE_CUBE_MAP }, false);

        ndcQuad = new NDCQuad(gl);
        
        File meshDir = new File("meshes");
        String[] meshNames = meshDir.list();
        Arrays.sort(meshNames);
        for (int i = 0; i < meshNames.length; i++)
        {
            meshes.add(new WavefrontMesh(gl, meshNames[i]));
        }
        meshIndex = 0;
        currentMesh = meshes.get(meshIndex);
        
        shader = new Shader(gl, "monkey");
        
        waterShader = new Shader(gl, "water");
        water = new Grid(gl, 512, 512, 20, 20, water_level);
        
        cloudShader = new Shader(gl, "clouds");
        clouds = new Grid(gl, 2048, 2048, 256, 256, 0, true);
        
        reflectBuffer = new FrameBuffer(gl, w_width, w_height,
                new int[]{ GL4.GL_TEXTURE_2D }, false);
        
        sbShader = new Shader(gl, "skybox");
        
        tShader = new Shader(gl, "terrain");
        tpShader = new Shader(gl, "grass");
        
        try
        {
            BufferedImage heightmap = ImageIO.read(new File("textures/heightmap.png"));
            terrain = new Terrain(gl,
                    512, // width
                    512, // length
                    50, // elevation
                    512,512, // grid resolution
                    heightmap);
            
            // for the grass... this is kinda "duplicate code" but whatever... :/
            terrainPoints = new Terrain(gl,
                    512, // width
                    512, // length
                    50, // elevation
                    512,512, // grid resolution
                    heightmap, true);
        } catch (IOException ex)
        {
            System.err.println("Could not load heightmap: " + ex.getMessage());
        }
        
        try
        {
            int w_h[] = new int[2];
            ByteBuffer data = Utils.loadTexture("textures/Stone1.jpg", w_h);
            terrainTextures[0] = Utils.loadTexture(gl, data, w_h[0], w_h[1]);
            
            data = Utils.loadTexture("textures/Grass.jpg", w_h);
            terrainTextures[1] = Utils.loadTexture(gl, data, w_h[0], w_h[1]);
            
            data = Utils.loadTexture("textures/Stone1.jpg", w_h);
            terrainTextures[2] = Utils.loadTexture(gl, data, w_h[0], w_h[1]);
            
            data = Utils.loadTexture("textures/Stone1_N.jpg", w_h);
            terrainTextures[3] = Utils.loadTexture(gl, data, w_h[0], w_h[1]);
            
            data = Utils.loadTexture("textures/Grass_N.jpg", w_h);
            terrainTextures[4] = Utils.loadTexture(gl, data, w_h[0], w_h[1]);
            
            data = Utils.loadTexture("textures/Stone1_N.jpg", w_h);
            terrainTextures[5] = Utils.loadTexture(gl, data, w_h[0], w_h[1]);
            
            data = Utils.loadTexture("textures/LongGrass_Colour.jpg", w_h);
            grassTextures[0] = Utils.loadTexture(gl, data, w_h[0], w_h[1]);
            
            data = Utils.loadTexture("textures/LongGrass_Alpha.jpg", w_h);
            grassTextures[1] = Utils.loadTexture(gl, data, w_h[0], w_h[1]);
            
            data = Utils.loadTexture("textures/noise2D.png", w_h);
            grassTextures[2] = Utils.loadTexture(gl, data, w_h[0], w_h[1]);
            
            data = Utils.loadTexture("textures/noise.png", w_h);
            grassTextures[3] = Utils.loadTexture(gl, data, w_h[0], w_h[1]);

	    data = Utils.loadTexture("textures/fnoise.jpg", w_h);
            cloudTexture = Utils.loadTexture(gl, data, w_h[0], w_h[1]);

        } catch (IOException ex)
        {
            System.err.println("Could not load texture: " + ex.getMessage());
        }

        updateProjection(WINDOW_WIDTH, WINDOW_HEIGHT);
        updateView();
        updateMonkeyWorld();
        
        lastUpdate = System.nanoTime();
    }

    void keyPressed(KeyEvent ke)
    {
        char key = ke.getKeyChar();
        if (ke.getKeyCode() < keys.length)
            keys[ke.getKeyCode()] = true;

        switch (key)
        {
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
            case '1':
                shadowToggle = !shadowToggle;
                break;
            case '`':
                ssaaToggle = !ssaaToggle;
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
        
        updateSunView();
    }

    void updateMonkeyWorld()
    {
        monkeyWorld = new Mat4(1f);

        Vec3 translation = new Vec3(xMove, yMove, zMove);
        monkeyWorld = Matrices.translate(monkeyWorld, translation);

        Vec3 yAxis = new Vec3(0, 1, 0);
        monkeyWorld = Matrices.rotate(monkeyWorld, yRot, yAxis);
        
        Vec3 xAxis = new Vec3(1, 0, 0);
        monkeyWorld = Matrices.rotate(monkeyWorld, xRot, xAxis);

        Vec3 zAxis = new Vec3(0, 0, 1);
        monkeyWorld = Matrices.rotate(monkeyWorld, zRot, zAxis);

        Vec3 scales = new Vec3(xScale, yScale, zScale);
        monkeyWorld = Matrices.scale(monkeyWorld, scales);
    }
    
    void updateSun()
    {
        Mat4 sunWorld = new Mat4(1.0f);
        
        sunWorld = Matrices.rotate(sunWorld, daytime, new Vec3(1,0.2f,0).getUnitVector());
        
        Vec4 sun4 = sunWorld.multiply(new Vec4(origin_sun, 0.0f));
        
        sun = new Vec3(sun4.getX(),sun4.getY(), sun4.getZ());
        
        updateSunView();
    }
    
    void updateSunView()
    {
        //lightAt = new Vec3(cameraEye.getX(),0,cameraEye.getZ());
        //lightEye = sun.getUnitVector().multiply(50).add(lightAt);
        
        lightEye = sun.multiply(10);
        
        lightView = Matrices.lookAt(
                lightEye, lightAt, lightUp);
    }
}
