
import com.jogamp.common.nio.Buffers;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import javax.media.opengl.GL;
import javax.media.opengl.GL4;

public class Quad extends Mesh
{

    private FloatBuffer vertices;
    private IntBuffer indices;

    public Quad(GL4 gl, float width, float height, float yoffset)
    {
        this(gl,width,height,yoffset,false);
    }
    
    public Quad(GL4 gl, float width, float height, float yoffset, boolean flipNormal)
    {
        super(gl);
        
        indexArraySize = 6;
        
        float norm = flipNormal ? -1.0f : 1.0f;
        
        vertices = Buffers.newDirectFloatBuffer(25);

        vertices.put(-width/2);
        vertices.put(yoffset);
        vertices.put(height/2);

        vertices.put(0.0f);
        vertices.put(norm);
        vertices.put(0.0f);

        vertices.put(width/2);
        vertices.put(yoffset);
        vertices.put(height/2);

        vertices.put(0.0f);
        vertices.put(norm);
        vertices.put(0.0f);

        vertices.put(width/2);
        vertices.put(yoffset);
        vertices.put(-height/2);

        vertices.put(0.0f);
        vertices.put(norm);
        vertices.put(0.0f);

        vertices.put(-width/2);
        vertices.put(yoffset);
        vertices.put(-height/2);

        vertices.put(0.0f);
        vertices.put(norm);
        vertices.put(0.0f);

        vertices.flip();

        indices = Buffers.newDirectIntBuffer(indexArraySize);

        indices.put(0);
        indices.put(flipNormal ? 2 : 1 );
        indices.put(flipNormal ? 1 : 2 );

        indices.put(0);
        indices.put(flipNormal ? 3 : 2 );
        indices.put(flipNormal ? 2 : 3 );

        indices.flip();

        IntBuffer vertexBuffer = Buffers.newDirectIntBuffer(1),
                indexBuffer = Buffers.newDirectIntBuffer(1);
        
        bind(gl);

        gl.glGenBuffers(1, vertexBuffer);
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vertexBuffer.get());

        gl.glBufferData(GL.GL_ARRAY_BUFFER, 24 * Float.SIZE / 8,
                vertices, GL.GL_STATIC_DRAW);

        gl.glGenBuffers(1, indexBuffer);
        gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, indexBuffer.get());

        gl.glBufferData(GL.GL_ELEMENT_ARRAY_BUFFER, indexArraySize * Integer.SIZE / 8,
                indices, GL.GL_STATIC_DRAW);
        
        gl.glEnableVertexAttribArray(Shader.POSITION_LOC);
        gl.glVertexAttribPointer(Shader.POSITION_LOC, 3, GL.GL_FLOAT, false,
                6 * Float.SIZE / 8, 0);
        
        gl.glEnableVertexAttribArray(Shader.NORMAL_LOC);
        gl.glVertexAttribPointer(Shader.NORMAL_LOC, 3, GL.GL_FLOAT, false,
                6 * Float.SIZE / 8, 3 * Float.SIZE / 8);
    }
}
