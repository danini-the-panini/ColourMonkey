import com.hackoeur.jglm.Vec3;
import com.jogamp.common.nio.Buffers;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import javax.media.opengl.GL;
import javax.media.opengl.GL4;

public class Terrain extends Mesh
{
    private BufferedImage heightmap;
    private FloatBuffer vertices;
    private IntBuffer indices;
    private int xGrid, yGrid;
    private float width,length,height;
    
    private float samplei(int u, int v)
    {
        if (u < 0) u = 0;
        if (v < 0) v = 0;
        if (u >= heightmap.getWidth()) u = heightmap.getWidth()-1;
        if (v >= heightmap.getHeight()) v = heightmap.getHeight()-1;
        return (float)(new Color(heightmap.getRGB(u, v)).getRed())/256.0f;
    }
    
    private float samplef(float u, float v)
    {
        if (u < 0) u = 0;
        if (u > 1) u = 1;
        if (v < 0) v = 0;
        if (v > 1) v = 1;
        
        u = u * heightmap.getWidth() - 0.5f;
        v = v * heightmap.getHeight() - 0.5f;
        int x = (int)(u);
        int y = (int)(v);
        float u_ratio = u - x;
        float v_ratio = v - y;
        float u_opposite = 1 - u_ratio;
        float v_opposite = 1 - v_ratio;
        
        return (samplei(x,y)   * u_opposite  + samplei(x+1,y)   * u_ratio) * v_opposite + 
                        (samplei(x,y+1) * u_opposite  + samplei(x+1,y+1) * u_ratio) * v_ratio;
    }
    
    private float h(int i, int j)
    {
        if (heightmap == null) return 0.0f;
        return samplef((float)i/(float)xGrid,(float)j/(float)yGrid);
    }
    
    public float getHeight(float x, float y)
    {
        x+=width/2;
        y+=length/2;
        return height * (samplef(x/width,y/length) - 0.5f);
    }
    
    public Vec3 getNormal(float x, float y)
    {
        final float QUAD_WIDTH = width/xGrid;
        final float QUAD_LENGTH = length/yGrid;
        
        float Hx = getHeight(x+QUAD_WIDTH, y) - getHeight(x-QUAD_WIDTH, y);
        Hx /= QUAD_WIDTH*2;

        float Hz = getHeight(x, y+QUAD_LENGTH) - getHeight(x, y-QUAD_LENGTH);
        Hz /= QUAD_LENGTH*2;

        return new Vec3(-Hx, 1.0f, -Hz).getUnitVector();
    }
    
    public Terrain(GL4 gl, float width, float length, float height,
            int xGrid, int yGrid, BufferedImage heightmap)
    {
        this(gl, width, length, height, xGrid, yGrid, heightmap, false);
    }
    
    public Terrain(GL4 gl, float width, float length, float height,
            int xGrid, int yGrid, BufferedImage heightmap, boolean pointsOnly)
    {
        super(gl);
        
        this.xGrid = xGrid;
        this.yGrid = yGrid;
        this.heightmap = heightmap;
        
        this.width = width;
        this.length = length;
        this.height = height;
        
        final float QUAD_WIDTH = width/xGrid;
        final float QUAD_LENGTH = length/yGrid;
        
        final int NUM_QUADS = (xGrid-1)*(yGrid-1);
        final int NUM_VERTICES = xGrid*yGrid;
        
        if (pointsOnly)
            indexArraySize = NUM_QUADS;
        else
            indexArraySize = NUM_QUADS * VERTICES_PER_QUAD;
        
        vertices = Buffers.newDirectFloatBuffer(NUM_VERTICES*DIMESIONS*2);
        
        float x, y, z;
        Vec3 normal;
        for (int i = 0; i < xGrid; i++)
        {
            for (int j = 0; j < yGrid; j++)
            {
                x = i*QUAD_WIDTH;
                y = height * h(i,j);
                z = j*QUAD_LENGTH;
                
                vertices.put(x-width/2); vertices.put(y-height/2); vertices.put(z-length/2);
                
                float Hx = h(i<xGrid-1 ? i+1 : i, j) - h(i>0 ? i-1 : i, j);
                if (i == 0 || i == xGrid-1)
                    Hx *= 2;
                Hx /= QUAD_WIDTH;

                float Hz = h(i, j<yGrid-1 ? j+1 : j) - h(i, j>0 ?  j-1 : j);
                if (j == 0 || j == yGrid-1)
                    Hz *= 2;
                Hz /= QUAD_LENGTH;

                normal = new Vec3(-Hx*height, 1.0f, -Hz*height).getUnitVector();
                vertices.put(normal.getX()); vertices.put(normal.getY()); vertices.put(normal.getZ());
            }
        }
        
        vertices.flip();
        
        indices = Buffers.newDirectIntBuffer(indexArraySize);
        
        for (int i = 0; i < xGrid-1; i++)
        {
            for (int j = 0; j < yGrid-1; j++)
            {
                indices.put(i+j*xGrid);
                if (!pointsOnly)
                {
                    indices.put(i+1+j*xGrid);
                    indices.put(i+1+(j+1)*xGrid);

                    indices.put(i+j*xGrid);
                    indices.put(i+1+(j+1)*xGrid);
                    indices.put(i+(j+1)*xGrid);
                }
            }
        }
        
        indices.flip();
        
        IntBuffer vertexBuffer = Buffers.newDirectIntBuffer(1),
                indexBuffer = Buffers.newDirectIntBuffer(1);

        bind(gl);

        gl.glGenBuffers(1, vertexBuffer);
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vertexBuffer.get());

        gl.glBufferData(GL.GL_ARRAY_BUFFER, NUM_VERTICES * DIMESIONS * 2 * Float.SIZE / 8,
                vertices, GL.GL_STATIC_DRAW);

        gl.glGenBuffers(1, indexBuffer);
        gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, indexBuffer.get());

        gl.glBufferData(GL.GL_ELEMENT_ARRAY_BUFFER, indexArraySize * Integer.SIZE / 8,
                indices, GL.GL_STATIC_DRAW);
        
        gl.glEnableVertexAttribArray(Shader.POSITION_LOC);
        gl.glVertexAttribPointer(Shader.POSITION_LOC, DIMESIONS, GL.GL_FLOAT, false,
                DIMESIONS * 2 * Float.SIZE / 8, 0);

        gl.glEnableVertexAttribArray(Shader.NORMAL_LOC);
        gl.glVertexAttribPointer(Shader.NORMAL_LOC, DIMESIONS, GL.GL_FLOAT, false,
                DIMESIONS * 2 * Float.SIZE / 8, DIMESIONS * Float.SIZE / 8);
    }
}
