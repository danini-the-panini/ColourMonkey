
import com.hackoeur.jglm.Mat4;
import com.hackoeur.jglm.Matrices;
import com.hackoeur.jglm.Vec3;
import javax.media.opengl.GL4;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author daniel
 */
public class Branch implements Drawable
{
    private final Branch parent;
    private final Vec3 origin;
    private final Vec3 axis;
    private final Vec3 tangent;
    
    private final Mesh mesh;
    
    public Branch(GL4 gl, Branch parent, Vec3 origin, Vec3 axis, Vec3 tangent, Mesh mesh)
    {
        this.parent = parent;
        this.origin = origin;
        this.axis = axis;
        this.tangent = tangent;
        this.mesh = mesh;
    }

    public Vec3 getAxis()
    {
        return axis;
    }
    
    public Mat4 getTreeMat()
    {
        if (parent == null)
        {
            return Mat4.MAT4_IDENTITY;
        }
        Mat4 mat = parent.getTreeMat();
        mat = Matrices.translate(mat, origin);
        mat = Matrices.rotate(mat, (float)Math.toDegrees(axis.angleInRadians(parent.axis)), tangent);
        return mat;
    }

    @Override
    public void draw(GL4 gl, Shader shader)
    {
        shader.updateUniform(gl, "tree_world", getTreeMat());
        
        mesh.draw(gl, shader);
        Utils.checkError(gl, "BranchDrawing");
    }
    
    
}
