import com.jogamp.common.nio.Buffers;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import javax.media.opengl.GL;
import javax.media.opengl.GL4;

public class Grid extends Mesh
{
    private FloatBuffer vertices;
    private IntBuffer indices;
    
    public Grid(GL4 gl, float width, float height,
            int xGrid, int yGrid, float yOffset)
    {
        this(gl,width,height,xGrid,yGrid,yOffset,false);
    }
    
    public Grid(GL4 gl, float width, float height,
            int xGrid, int yGrid, float yOffset, boolean flipNormal)
    {
        super(gl);
        
        final float QUAD_WIDTH = width/xGrid;
        final float QUAD_LENGTH = height/yGrid;
        
        final int NUM_QUADS = (xGrid-1)*(yGrid-1);
        final int NUM_VERTICES = xGrid*yGrid;
        
        indexArraySize = NUM_QUADS * VERTICES_PER_QUAD;
        
        vertices = Buffers.newDirectFloatBuffer(NUM_VERTICES*DIMESIONS*2);
        
        float x, z;
        for (int i = 0; i < xGrid; i++)
        {
            for (int j = 0; j < yGrid; j++)
            {
                x = i*QUAD_WIDTH;
                z = j*QUAD_LENGTH;
                
                vertices.put(x-width/2); vertices.put(yOffset); vertices.put(z-height/2);
                
                vertices.put(0); vertices.put(flipNormal ? -1.0f : 1.0f); vertices.put(0);
            }
        }
        
        vertices.flip();
        
        indices = Buffers.newDirectIntBuffer(indexArraySize);
        
        int a;
        for (int i = 0; i < xGrid-1; i++)
        {
            for (int j = 0; j < yGrid-1; j++)
            {
                a = i+1+j*xGrid;
                
                indices.put(i+j*xGrid);
                if (!flipNormal) indices.put(a);
                indices.put(i+1+(j+1)*xGrid);
                if (flipNormal) indices.put(a);
                
                a = i+1+(j+1)*xGrid;
                
                indices.put(i+j*xGrid);
                if (!flipNormal) indices.put(a);
                indices.put(i+(j+1)*xGrid);
                if (flipNormal) indices.put(a);
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
