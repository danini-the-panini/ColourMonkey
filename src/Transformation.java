
import com.hackoeur.jglm.Mat4;
import com.hackoeur.jglm.Matrices;
import com.hackoeur.jglm.Vec3;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author daniel
 */
public class Transformation
{
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
    
    
    
    public Mat4 getWorldMatrix()
    {
        Mat4 world = new Mat4(1f);

        Vec3 translation = new Vec3(xMove, yMove, zMove);
        world = Matrices.translate(world, translation);
        
        world = Matrices.rotate(world, xRot, xAxis);
        world = Matrices.rotate(world, zRot, zAxis);
        world = Matrices.rotate(world, yRot, yAxis);

        Vec3 scales = new Vec3(xScale, yScale, zScale);
        world = Matrices.scale(world, scales);
        
        return world;
    }
}
