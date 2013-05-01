
import com.jogamp.common.nio.Buffers;
import java.nio.IntBuffer;
import javax.media.opengl.GL;
import javax.media.opengl.GL4;

public abstract class Mesh
{
    public static final int VERTICES_PER_TRIANGLE = 3;
    public static final int TRIANGLES_PER_QUAD = 2;
    public static final int VERTICES_PER_QUAD = VERTICES_PER_TRIANGLE * TRIANGLES_PER_QUAD;
    public static final int DIMESIONS = 3;

    public Mesh(GL4 gl)
    {
        IntBuffer handleBuffer = Buffers.newDirectIntBuffer(1);

        gl.glGenVertexArrays(1, handleBuffer);
        handle = handleBuffer.get();
    }
    
    
    protected int handle, indexArraySize;

    public void bind(GL4 gl)
    {
       gl.glBindVertexArray(handle);
    }
    
    public void draw(GL4 gl) {
       bind(gl);
       
       gl.glDrawElements(GL.GL_TRIANGLES, indexArraySize, GL.GL_UNSIGNED_INT, 0);
    }
    
    public void drawPatches(GL4 gl) {
       bind(gl);
       
       gl.glPatchParameteri(GL4.GL_PATCH_VERTICES, 3);
       gl.glDrawElements(GL4.GL_PATCHES, indexArraySize, GL.GL_UNSIGNED_INT, 0);
    }
}
