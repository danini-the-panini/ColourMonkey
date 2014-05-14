import com.hackoeur.jglm.*;
import com.hackoeur.jglm.support.FastMath;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
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
    static final int WINDOW_WIDTH = 1024;
    /**
     * The window's initial height.
     */
    static final int WINDOW_HEIGHT = 600;

    int w_width = WINDOW_WIDTH, w_height = WINDOW_HEIGHT;
    float aspect;

    Shader shinyShader;
    Mesh monkeyMesh;
    Transformation monkey = new Transformation();

    Shader tankShader;
    Mesh tankMesh;
    Transformation[] tanks = new Transformation[7];
    int chosenTank = 0;
    int moTank = -1;

    Shader sbShader, smShader;
    NDCQuad ndcQuad;

    Shader waterShader, cloudShader;
    Grid water, clouds;

    TextureCubeMap skyMap;
    FrameBuffer skymapBuffer;
    TextureCubeMap envMap;
    FrameBuffer envmapBuffer;
    Texture reflection;
    FrameBuffer reflectBuffer;
    Texture postTexture;
    FrameBuffer postBuffer;
    // Texture shadowMap; // contianed in FBO at the moment
    FrameBuffer shadowBuffer;

    Texture picking;
    FrameBuffer pickingBuffer;
    int clickX = -1, clickY = -1;
    int mouseX = 0, mouseY = 0;

    Shader postProcess, showoff, pickingShader;
    int ssaa = 2; // amount of SSAA to apply;

    int[] terrainTextures = new int[6];
    int[] grassTextures = new int[4];

    Shader tShader, tpShader;
    Terrain terrain;
    Terrain terrainPoints;

    /* Amount to rotate by in one step. */
    static final float ANGLE_DELTA = 60.0f;
    /* Amount to move / scale by in one step. */
    static final float DELTA = 5f;

    float camRotX = 0.0f;
    float camRotY = 0.0f;

    /* Camera location. */
    Vec3 cameraEye = new Vec3(-12.48743f,  0.64550f, -7.89169f);
    /* Vector the camera is looking along. */
    Vec3 cameraAt = new Vec3(-14.68075f, 1.16453f, -5.26110f);
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
    float cloud_density = 0.90f;

    float fog_start = 150.0f;
    float fog_end = 250.0f;

    Mat4 view, projection, mirror_view;

    Mat4 lightView, lightProjection;

    Vec3 origin_sun = new Vec3(-2.0f,0f,5.0f).getUnitVector();
    Vec3 sun = new Vec3(origin_sun);

    float daytime = 325.91772f;
    boolean skyMapChanged = true;

    Vec3 lightEye = sun.multiply(384),
            lightAt = new Vec3(0f, 0f, 0f),
            lightUp = new Vec3(0f, 1f, 0f);

    int shadowRes = 2048;

    Vec4 clipPlane = new Vec4(0.0f, 1.0f, 0.0f, 0.0f);
    Mat4 clipWorld = Matrices.translate(new Mat4(1.0f), new Vec3(0.0f, -water_level, 0.0f));

    boolean[] keys = new boolean[1024];

    long lastUpdate;
    float time = 0.0f;

    boolean shadowToggle = true, ssaaToggle = false, grassToggle = true,
            waterToggle = true, envToggle = true, cloudToggle = true;

    public static final long NANOS_PER_SECOND = 1000000000l;

    void cleanUp(GL4 gl)
    {
        // TODO: release meshes and shaders here. EEEK!!!
    }

    void display(GL4 gl)
    {
        update(gl);
        render(gl);
    }

    private float lastFPSUpdate = 0;
    void update(GL4 gl)
    {
        long nTime = System.nanoTime();
        long nanos = nTime-lastUpdate;
        lastUpdate = nTime;
        float delta = nanos/(float)NANOS_PER_SECOND;
        time += delta;
        lastFPSUpdate += delta;

	if (lastFPSUpdate > 1.0f)
        {
            Main.jframe.setTitle(String.format("Colour Monkey! FPS: %.2f",1.0f/delta));
            lastFPSUpdate -= 1.0f;
        }

        float step = DELTA;

        monkey.yRot += delta*5;

        if (clickX != -1)
        {
            System.out.println("Clickness! " + clickX + ", " + clickY);

            float[] pick = pickingBuffer.readPixel(gl, clickX, w_height-clickY, 0);

            System.out.println("Picked: " + Arrays.toString(pick));

            chosenTank = pickToId(pick);

            clickX = clickY = -1;
        }

        moTank = pickToId(pickingBuffer.readPixel(gl, mouseX, w_height-mouseY, 0));

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

        if (keys[KeyEvent.VK_O])
        {
            daytime += delta*33;
            if (daytime > 360)
                daytime = daytime - 360.0f;
            updateSun();
        }
        else if (keys[KeyEvent.VK_P])
        {
            daytime -= delta*33;
            if (daytime < 0)
                daytime = daytime + 360.0f;
            updateSun();
        }



        if (chosenTank != -1)
            updateTank(tanks[chosenTank], delta);

        updateView();
    }

    int pickToId(float[] pick)
    {

            int id = 0;
            for (int i = 0; i < 3; i++)
                if (pick[i] > 0.5) id |= 1 << i;
            return id -1;
    }

    void render(GL4 gl)
    {
        if (shadowToggle)
        {
            shadowBuffer.use(gl);

                gl.glCullFace(GL4.GL_FRONT);
                renderScene(gl, lightView, lightProjection);
                gl.glCullFace(GL4.GL_BACK);
        }


        if (skyMapChanged)
        {
            updateSkymap(gl);
            skyMapChanged = false;
        }

        if (envToggle)
            updateEnvMap(gl, monkeyMesh, monkey);

        if (waterToggle)
        {
            reflectBuffer.use(gl);

                gl.glEnable(GL4.GL_CLIP_DISTANCE0);
                renderScene(gl, mirror_view);
                if (grassToggle)
                    renderGrass(gl, mirror_view, projection);

                if (cloudToggle)
                    renderClouds(gl, mirror_view, projection, time);

                gl.glDisable(GL4.GL_CLIP_DISTANCE0);
        }

        pickingBuffer.use(gl);

            for (int i = 0; i < tanks.length; i++)
            {
                pickingShader.use(gl);
                pickingShader.updateUniform(gl, "id", i+1);
                renderMesh(gl, view, projection, tankMesh, tanks[i], pickingShader);
            }

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

            renderScene(gl);

            reflection.use(gl, GL.GL_TEXTURE6);
            renderWater(gl, view, projection);

            if (grassToggle)
                renderGrass(gl, view, projection);

            if (cloudToggle)
                renderClouds(gl, view, projection, time);

        if (ssaaToggle)
        {
            FrameBuffer.unbind(gl);

            gl.glViewport(0, 0, w_width, w_height);
            gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

                postTexture.use(gl, GL.GL_TEXTURE7);

                //gl.glDisable(GL.GL_DEPTH_TEST);
                renderPostProcessing(gl);

                //picking.use(gl, GL.GL_TEXTURE7);
                //renderShowoff(gl, w_width-110, w_height-110, 100, 100);
                //gl.glEnable(GL.GL_DEPTH_TEST);
        }

        gl.glFlush();
    }

    void renderScene(GL4 gl)
    {
        renderScene(gl, view);
    }
    void renderScene(GL4 gl, Mat4 camera)
    {
        renderScene(gl, camera, projection);
    }
    void renderScene(GL4 gl, Mat4 camera, Mat4 proj)
    {
        renderScene(gl, camera, proj, null);
    }
    void renderScene(GL4 gl, Mat4 camera, Mat4 proj, Mesh skip)
    {
        skyMap.use(gl, GL.GL_TEXTURE0);
        envMap.use(gl, GL.GL_TEXTURE9);
        renderSkymap(gl, camera, proj);
        renderTerrain(gl, camera, proj);
        if (skip != monkeyMesh)
        {
            renderMesh(gl, camera, proj, monkeyMesh, monkey, shinyShader);
        }
        for (int i = 0; i < tanks.length; i++)
        {
            tankShader.use(gl);
            tankShader.updateUniform(gl, "chosen", chosenTank == i ? 1 : 0);
            tankShader.updateUniform(gl, "mo", moTank == i ? 1 : 0);
            renderMesh(gl, camera, proj, tankMesh, tanks[i], tankShader);
        }
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

    void updateSkymap(GL4 gl)
    {
        Mat4 smproj = Matrices.perspective(90, 1, NEAR, FAR);
        for (int i = 0; i < 6; i++)
        {
            skymapBuffer.bindTexture(gl, GL4.GL_COLOR_ATTACHMENT0,
                    GL4.GL_TEXTURE_CUBE_MAP_POSITIVE_X+i, skyMap.getHandle());
            skymapBuffer.use(gl);

            Mat4 smview = Utils.lookAtCube(new Vec3(0,0,0), GL4.GL_TEXTURE_CUBE_MAP_POSITIVE_X+i);

            renderSkybox(gl, smview, smproj);
        }
    }

    void updateEnvMap(GL4 gl, Mesh mesh, Transformation t)
    {
        Mat4 emproj = Matrices.perspective(90, 1, NEAR, FAR);
        for (int i = 0; i < 6; i++)
        {
            envmapBuffer.bindTexture(gl, GL4.GL_COLOR_ATTACHMENT0,
                    GL4.GL_TEXTURE_CUBE_MAP_POSITIVE_X+i, envMap.getHandle());
            envmapBuffer.use(gl);

            Mat4 emview = Utils.lookAtCube(new Vec3(t.xMove, t.yMove, t.zMove), GL4.GL_TEXTURE_CUBE_MAP_POSITIVE_X+i);

            renderScene(gl, emview, emproj, mesh);

            if (cloudToggle)
                renderClouds(gl, emview, emproj, time);
        }
    }

    void renderSkybox(GL4 gl, Mat4 camera, Mat4 proj)
    {
        sbShader.use(gl);

        sbShader.updateUniform(gl, "view", camera);
        sbShader.updateUniform(gl, "projection", proj);
        sbShader.updateUniform(gl, "aspect", aspect);
        sbShader.updateUniform(gl, "sun", sun);
        sbShader.updateUniform(gl, "time", daytime/360.0f);

        ndcQuad.draw(gl);
    }

    void renderSkymap(GL4 gl, Mat4 camera, Mat4 proj)
    {
        smShader.use(gl);

        smShader.updateUniform(gl, "view", camera);
        smShader.updateUniform(gl, "projection", proj);
        smShader.updateUniform(gl, "aspect", aspect);

        ndcQuad.draw(gl);
    }

    void renderTerrain(GL4 gl, Mat4 camera, Mat4 proj)
    {
        tShader.use(gl);

        for (int i = 0; i < terrainTextures.length; i++)
        {
            gl.glActiveTexture(GL.GL_TEXTURE1+i);
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
            gl.glActiveTexture(GL.GL_TEXTURE1+i);
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

    void renderMesh(GL4 gl, Mat4 camera, Mat4 proj, Mesh mesh, Transformation t, Shader shader)
    {
        shader.use(gl);

        shader.updateUniform(gl, "world", t.getWorldMatrix());
        shader.updateUniform(gl, "view", camera);
        shader.updateUniform(gl, "projection", proj);

        shader.updateUniform(gl, "lview", lightView);
        shader.updateUniform(gl, "lprojection", lightProjection);

        shader.updateUniform(gl, "sun", sun);
        shader.updateUniform(gl, "shadowToggle", shadowToggle ? 1 : 0);

        shader.updateUniform(gl, "clipPlane", clipPlane);
        shader.updateUniform(gl, "clipWorld", clipWorld);

        mesh.draw(gl);
    }

    void renderClouds(GL4 gl, Mat4 camera, Mat4 proj, float time)
    {
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

        //shadowMap = new Texture2D(gl, null, shadowRes, shadowRes, false);
        shadowBuffer = new FrameBuffer(gl, shadowRes, shadowRes, true);

        postTexture = new Texture2D(gl, w_width*ssaa, w_height*ssaa);
        postTexture.setParameters(gl, Texture.texParamsFBO);
        postBuffer = new FrameBuffer(gl, w_width*ssaa, w_height*ssaa, false);
        postBuffer.bindTexture(gl, GL4.GL_COLOR_ATTACHMENT0,
                GL4.GL_TEXTURE_2D, postTexture.getHandle());

        postProcess = new Shader(gl, "postprocess");
        showoff = new Shader(gl, "showoff");

        skyMap = new TextureCubeMap(gl, null, 1024, 1024);
        skyMap.setParameters(gl, Texture.texParamsSkyMap);
        skymapBuffer = new FrameBuffer(gl, 1024, 1024, false);

        envMap = new TextureCubeMap(gl, null, 1024, 1024);
        envMap.setParameters(gl, Texture.texParamsSkyMap);
        envmapBuffer = new FrameBuffer(gl, 1024, 1024, false);

        pickingShader = new Shader(gl, "picking");

        ndcQuad = new NDCQuad(gl);

        monkeyMesh = new WavefrontMesh(gl, "monkey.obj");

        monkey.xScale = monkey.yScale = monkey.zScale = 5.0f;
        monkey.xMove = -30.0f;
        monkey.yMove = -0.65f;
        monkey.zMove = 3.0f;

        shinyShader = new Shader(gl, "shiny");

        tankMesh = new WavefrontMesh(gl, "tank.obj");

        tankShader = new Shader(gl, "monkey");

        waterShader = new Shader(gl, "water");
        water = new Grid(gl, 512, 512, 20, 20, water_level);

        cloudShader = new Shader(gl, "clouds");
        clouds = new Grid(gl, 2048, 2048, 256, 256, 0, true);

        reflection = new Texture2D(gl, null, w_width, w_height);
        reflection.setParameters(gl, Texture.texParamsFBO);
        reflectBuffer = new FrameBuffer(gl, w_width, w_height, false);
        reflectBuffer.bindTexture(gl, GL4.GL_COLOR_ATTACHMENT0,
                GL4.GL_TEXTURE_2D, reflection.getHandle());

        picking = new Texture2D(gl, w_width, w_height);
        picking.setParameters(gl, Texture.texParamsFBO);
        pickingBuffer = new FrameBuffer(gl, w_width, w_height, false);
        pickingBuffer.bindTexture(gl, GL4.GL_COLOR_ATTACHMENT0,
                GL4.GL_TEXTURE_2D, picking.getHandle());

        sbShader = new Shader(gl, "skybox");
        smShader = new Shader(gl, "skymap");

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
            gl.glActiveTexture(0);

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

        } catch (IOException ex)
        {
            System.err.println("Could not load texture: " + ex.getMessage());
        }

        for (int i = 0; i < tanks.length; i++)
        {
            tanks[i] = new Transformation();

            tanks[i].zMove = 20.0f;
            tanks[i].xMove = -20 - i*5;

            tanks[i].yRot = 180.0f;

            updateTank(tanks[i], 0);
        }

        updateProjection(WINDOW_WIDTH, WINDOW_HEIGHT);
        updateView();
        updateSun();

        lastUpdate = System.nanoTime();
    }

    void keyPressed(KeyEvent ke)
    {
        char key = ke.getKeyChar();
        if (ke.getKeyCode() < keys.length)
            keys[ke.getKeyCode()] = true;

        switch (key)
        {
            case KEY_ESCAPE:
                System.out.printf("Bye!\n");
                System.exit(0);
                break;
            case '1':
                ssaaToggle = !ssaaToggle;
                break;
            case '2':
                shadowToggle = !shadowToggle;
                break;
            case '3':
                grassToggle = !grassToggle;
                break;
            case '4':
                waterToggle = !waterToggle;
                break;
            case '5':
                envToggle = !envToggle;
                break;
            case '6':
                cloudToggle = !cloudToggle;
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
        mouseX = x;
        mouseY = y;
    }

    void mouseDragged(int x, int y)
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

    void mouseClicked(int x, int y)
    {
        clickX = x;
        clickY = y;

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

        lightEye = sun.multiply(384);

        lightView = Matrices.lookAt(
                lightEye, lightAt, lightUp);

        final float SIZE = 64;

        Vec4[] ps = {
            lightView.multiply(new Vec4(cameraEye.getX()-SIZE,-25,cameraEye.getZ()+SIZE,1)),
            lightView.multiply(new Vec4(cameraEye.getX()+SIZE,-25,cameraEye.getZ()+SIZE,1)),
            lightView.multiply(new Vec4(cameraEye.getX()+SIZE,-25,cameraEye.getZ()-SIZE,1)),
            lightView.multiply(new Vec4(cameraEye.getX()-SIZE,-25,cameraEye.getZ()-SIZE,1)),
            lightView.multiply(new Vec4(cameraEye.getX()-SIZE,25,cameraEye.getZ()+SIZE,1)),
            lightView.multiply(new Vec4(cameraEye.getX()+SIZE,25,cameraEye.getZ()+SIZE,1)),
            lightView.multiply(new Vec4(cameraEye.getX()+SIZE,25,cameraEye.getZ()-SIZE,1)),
            lightView.multiply(new Vec4(cameraEye.getX()-SIZE,25,cameraEye.getZ()-SIZE,1))
        };


        Vec4[] psBig = {
            lightView.multiply(new Vec4(-256,-25,256,1)),
            lightView.multiply(new Vec4(256,-25,256,1)),
            lightView.multiply(new Vec4(256,-25,-256,1)),
            lightView.multiply(new Vec4(-256,-25,-256,1)),
            lightView.multiply(new Vec4(-256,25,256,1)),
            lightView.multiply(new Vec4(256,25,256,1)),
            lightView.multiply(new Vec4(256,25,-256,1)),
            lightView.multiply(new Vec4(-256,25,-256,1))
        };

        float left = ps[0].getX(),
                right = ps[0].getX(),
                top = ps[0].getY(),
                bottom = ps[0].getY(),
                near = -psBig[0].getZ(),
                far = -psBig[0].getZ();

        for (int i = 1; i < ps.length; i++)
        {
            left = Math.min(left, ps[i].getX());
            right = Math.max(right, ps[i].getX());
            top = Math.max(top, ps[i].getY());
            bottom = Math.min(bottom, ps[i].getY());
            near = Math.min(near, -psBig[i].getZ());
            far = Math.max(far, -psBig[i].getZ());
        }

        left += 10; right-=10; top -= 10; bottom += 10;

        float Sx = 2.0f/(right-left),
                Sy = 2.0f/(top-bottom),
                Ox = -0.5f*(right+left)*Sx,
                Oy = -0.5f*(top+bottom)*Sy;

        lightProjection =
                /*new Mat4(
                    Sx, 0,  0,  0,
                    0,  Sy, 0,  0,
                    0,  0,  1,  0,
                    Ox, Oy, 0,  1
                ).multiply(*/
                    Matrices.ortho(left, right, bottom, top, near, far)
                //)
                ;

        skyMapChanged = true;
    }

    void updateTank(Transformation tank, float delta)
    {
        float angle = -(float)Math.toRadians(tank.yRot);
        float sin = (float)Math.sin(angle);
        float cos = (float)Math.cos(angle);
        float tdelta = 5*delta;

        if (keys[KeyEvent.VK_UP])
        {
            tank.xMove += -(tdelta*sin);
            tank.zMove += (tdelta*cos);
            //tank.zMove += tdelta;
        }
        else if (keys[KeyEvent.VK_DOWN])
        {
            tank.xMove -= -(tdelta*sin);
            tank.zMove -= (tdelta*cos);
            //tank.zMove -= tdelta;
        }
        if (keys[KeyEvent.VK_LEFT])
        {
            tank.yRot += tdelta*ANGLE_DELTA;
            //tank.xMove += tdelta;
        }
        else if (keys[KeyEvent.VK_RIGHT])
        {
            tank.yRot -= tdelta*ANGLE_DELTA;
            //tank.xMove -= tdelta;
        }
        tank.yMove = terrain.getHeight(tank.xMove, tank.zMove);
        Vec3 tankNormal = terrain.getNormal(tank.xMove, tank.zMove).multiply(0.5f);
        Vec3 tankNX = new Vec3(tankNormal.getX(),tankNormal.getY(),0.0f).getUnitVector();
        Vec3 tankNZ = new Vec3(0.0f,tankNormal.getY(),tankNormal.getZ()).getUnitVector();
        //tank.yAxis = tankNormal;
        tank.xRot = (float)FastMath.toDegrees(Math.asin(tankNZ.getZ()));
        tank.zRot = -(float)FastMath.toDegrees(Math.asin(tankNX.getX()));
    }
}
