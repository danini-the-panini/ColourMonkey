import com.jogamp.common.nio.Buffers;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import javax.media.opengl.GL;
import javax.media.opengl.GL4;

public class VoxelChunk extends Mesh
{
    private FloatBuffer vertices;

    public VoxelChunk(GL4 gl, int chunkSize)
    {
        super(gl);
        
        final int NUM_VERTICES = chunkSize*chunkSize*chunkSize;
        
        indexArraySize = NUM_VERTICES*DIMESIONS;
        
        vertices = Buffers.newDirectFloatBuffer(indexArraySize);
        
        for (int i = 0; i < chunkSize; i++)
        for (int j = 0; j < chunkSize; j++)
        for (int k = 0; k < chunkSize; k++)
        {
            vertices.put((float)i/(float)chunkSize);
            vertices.put((float)j/(float)chunkSize);
            vertices.put((float)k/(float)chunkSize);
        }
        
        vertices.flip();
        
        bind(gl);
        
        IntBuffer vertexBuffer = Buffers.newDirectIntBuffer(1);
        
        gl.glGenBuffers(1, vertexBuffer);
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vertexBuffer.get());

        gl.glBufferData(GL.GL_ARRAY_BUFFER, NUM_VERTICES * DIMESIONS * Float.SIZE / 8,
                vertices, GL.GL_STATIC_DRAW);
        
        gl.glEnableVertexAttribArray(Shader.POSITION_LOC);
        gl.glVertexAttribPointer(Shader.POSITION_LOC, DIMESIONS, GL.GL_FLOAT, false,
                DIMESIONS * Float.SIZE / 8, 0);
    }

    @Override
    public void draw(GL4 gl)
    {
        bind(gl);
        
        gl.glDrawArrays(GL.GL_POINTS, 0, indexArraySize);
    }
    
}
