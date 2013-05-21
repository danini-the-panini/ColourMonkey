
import com.hackoeur.jglm.Mat4;
import com.hackoeur.jglm.Matrices;
import com.hackoeur.jglm.Vec3;
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
    
    public Vec3 xAxis = new Vec3(1, 0, 0);
    public Vec3 yAxis = new Vec3(0, 1, 0);
    public Vec3 zAxis = new Vec3(0, 0, 1);

    /* Amount of rotation around the x axis. */
    public float xRot = 0.0f;
    /* Amount of rotation around the y axis. */
    public float yRot = 0.0f;
    /* Amount of rotation around the z axis. */
    public float zRot = 0.0f;

    /* Amount to scale the x axis by. */
    public float xScale = 1.0f;
    /* Amount to scale the y axis by. */
    public float yScale = 1.0f;
    /* Amount to scale the z axis by. */
    public float zScale = 1.0f;

    /* Amount to move on the x axis. */
    public float xMove = 0.0f;
    /* Amount to move on the y axis. */
    public float yMove = 0.0f;
    /* Amount to move on the z axis. */
    public float zMove = 0.0f;

    public Mesh(GL4 gl)
    {
        IntBuffer handleBuffer = Buffers.newDirectIntBuffer(1);

        gl.glGenVertexArrays(1, handleBuffer);
        handle = handleBuffer.get();
    }
    
    public Mat4 getWorldMatrix()
    {
        Mat4 monkeyWorld = new Mat4(1f);

        Vec3 translation = new Vec3(xMove, yMove, zMove);
        monkeyWorld = Matrices.translate(monkeyWorld, translation);
        
        monkeyWorld = Matrices.rotate(monkeyWorld, xRot, xAxis);
        monkeyWorld = Matrices.rotate(monkeyWorld, zRot, zAxis);
        monkeyWorld = Matrices.rotate(monkeyWorld, yRot, yAxis);

        Vec3 scales = new Vec3(xScale, yScale, zScale);
        monkeyWorld = Matrices.scale(monkeyWorld, scales);
        
        return monkeyWorld;
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
    
    public void drawPoints(GL4 gl)
    {
       bind(gl);
       
       gl.glDrawElements(GL.GL_POINTS, indexArraySize, GL.GL_UNSIGNED_INT, 0);
    }
    
    public void drawPatches(GL4 gl) {
       bind(gl);
       
       gl.glPatchParameteri(GL4.GL_PATCH_VERTICES, 3);
       gl.glDrawElements(GL4.GL_PATCHES, indexArraySize, GL.GL_UNSIGNED_INT, 0);
    }
}
