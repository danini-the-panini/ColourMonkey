
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
    private final int parent_id;
    private final Branch parent;
    private final float length;
    private final Vec3 origin;
    private final Vec3 axis;
    private final Vec3 tangent;
    
    private final Mesh mesh;
    
    public Branch(GL4 gl, int parent_id, Branch parent, float length, Vec3 origin, Vec3 axis, Vec3 tangent, Mesh mesh)
    {
        this.parent = parent;
        this.parent_id = parent_id;
        this.length = length;
        this.origin = origin;
        this.axis = axis;
        this.tangent = tangent;
        this.mesh = mesh;
    }

    public float getLength()
    {
        return length;
    }

    public Branch getParent()
    {
        return parent;
    }

    public int getParentID()
    {
        return parent_id;
    }

    public Vec3 getOrigin()
    {
        return origin;
    }
    public Vec3 getAxis()
    {
        return axis;
    }

    public Vec3 getTangent()
    {
        return tangent;
    }

    public Mat4 getTreeMat()
    {
        if (parent == null)
        {
            return Mat4.MAT4_IDENTITY;
        }
        return parent.getTreeMat().multiply(getLocalMat());
    }
    
    public Mat4 getLocalMat()
    {
        if (parent == null)
            return Mat4.MAT4_IDENTITY;
            
        Mat4 mat = Mat4.MAT4_IDENTITY;
        mat = Matrices.translate(mat, origin);
        if (!axis.equalsWithEpsilon(parent.axis))
        {
            mat = Matrices.rotate(mat, (float)Math.toDegrees(axis.angleInRadians(parent.axis)), tangent);
        }
        return mat;
    }

    @Override
    public void draw(GL4 gl, Shader shader)
    {
        shader.updateUniform(gl, "tree_world", getLocalMat());
        
        mesh.draw(gl, shader);
        Utils.checkError(gl, "BranchDrawing");
    }
    
    
}
