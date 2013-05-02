
import com.jogamp.common.nio.Buffers;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import javax.media.opengl.GL;
import javax.media.opengl.GL4;

public class NDCQuad extends Mesh
{

    private FloatBuffer vertices;
    private IntBuffer indices;

    public NDCQuad(GL4 gl)
    {
        super(gl);
        
        indexArraySize = 6;
        
        vertices = Buffers.newDirectFloatBuffer(12);

        vertices.put(-1.0f);
        vertices.put(1.0f);
        vertices.put(0.99f);

        vertices.put(1.0f);
        vertices.put(1.0f);
        vertices.put(0.99f);

        vertices.put(1.0f);
        vertices.put(-1.0f);
        vertices.put(0.99f);

        vertices.put(-1.0f);
        vertices.put(-1.0f);
        vertices.put(0.99f);

        vertices.flip();

        indices = Buffers.newDirectIntBuffer(indexArraySize);

        indices.put(0);
        indices.put(2);
        indices.put(1);

        indices.put(0);
        indices.put(3);
        indices.put(2);

        indices.flip();

        IntBuffer vertexBuffer = Buffers.newDirectIntBuffer(1),
                indexBuffer = Buffers.newDirectIntBuffer(1);
        
        bind(gl);

        gl.glGenBuffers(1, vertexBuffer);
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vertexBuffer.get());

        gl.glBufferData(GL.GL_ARRAY_BUFFER, 12 * Float.SIZE / 8,
                vertices, GL.GL_STATIC_DRAW);

        gl.glGenBuffers(1, indexBuffer);
        gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, indexBuffer.get());

        gl.glBufferData(GL.GL_ELEMENT_ARRAY_BUFFER, indexArraySize * Integer.SIZE / 8,
                indices, GL.GL_STATIC_DRAW);
        
        gl.glEnableVertexAttribArray(Shader.POSITION_LOC);
        gl.glVertexAttribPointer(Shader.POSITION_LOC, 3, GL.GL_FLOAT, false,
                3 * Float.SIZE / 8, 0);
    }
}
