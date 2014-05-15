
import com.hackoeur.jglm.Mat4;
import com.hackoeur.jglm.Matrices;
import com.hackoeur.jglm.Vec3;
import java.util.ArrayList;
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
public class Tree implements Drawable
{
    private Mesh bmesh;
    
    class LState
    {
        Vec3 pos = Vec3.VEC3_ZERO;
        Vec3 dir = new Vec3(0, 1, 0);
        int branch = -1;
        LState previous = null;
    }
    
    LState state = new LState();
    
    class Forward implements Runnable
    {
        float amount;

        public Forward(float amount)
        {
            this.amount = amount;
        }
        
        @Override
        public void run()
        {
            Branch parent = state.branch == -1 ? null : branches.get(state.branch);
            Vec3 tangent = parent == null ? new Vec3(0,0,1) :
                    parent.getAxis().cross(state.dir).getUnitVector();
            
            if (tangent.equalsWithEpsilon(Vec3.VEC3_ZERO, 0.0001f))
                tangent = new Vec3(0,0,1);
            
            branches.add(new Branch(gl, parent, new Vec3(0,amount,0), state.dir,
                    tangent, bmesh));
            
            state.branch = branches.size()-1;
            state.pos = state.pos.add(state.dir.scale(amount));
            
            System.out.print(":F ");
        }
        
    }
    
    class Rotate implements Runnable
    {
        float amount;

        public Rotate(float amount)
        {
            this.amount = amount;
        }
        
        @Override
        public void run()
        {
            Mat4 rot = Matrices.rotate(Mat4.MAT4_IDENTITY, amount, new Vec3(0,0,1));
            state.dir = rot.multiply(state.dir.toDirection()).xyz().getUnitVector();
            
            
            System.out.print(amount < 0 ? ":R " : ":L ");
        }
        
    }
    
    class SaveState implements Runnable
    {

        @Override
        public void run()
        {
            LState newState = new LState();
            newState.pos = state.pos;
            newState.dir = state.dir;
            newState.branch = state.branch;
            newState.previous = state;
            
            state = newState;
            
            System.out.print(":< ");
        }
        
    }
    
    class LoadState implements Runnable
    {

        @Override
        public void run()
        {
            state = state.previous;
            
            System.out.print(":> ");
        }
        
    }
    
    private final LSystem lsys;

    private final ArrayList<Branch> branches = new ArrayList<Branch>();
    private GL4 gl;
    
    private final Runnable F = new Forward(1.0f), L = new Rotate(30.0f),
            R = new Rotate(-30.0f), _s = new SaveState(), s_ = new LoadState();

    public Tree(GL4 gl)
    {
        this.gl = gl;
        
        bmesh = new WavefrontMesh(gl, "branch.obj");
        
        lsys = new LSystem(new Runnable[]{F});
        lsys.addRule(F, new Runnable[]{F, _s, L, F, s_, F, _s, R, F, s_, F});
        lsys.addRule(L, new Runnable[]{L, F});
        lsys.addRule(R, new Runnable[]{F, R});
        lsys.iterate(3);
        lsys.run();
        
        System.out.println("Num Branches: " + branches.size());
        
        Utils.checkError(gl, "Tree");
    }
    
    @Override
    public void draw(GL4 gl, Shader shader)
    {
        
        int len = branches.size();
        for (int i = 0; i < len; i++)
        {
            shader.updateUniform(gl, "branch", i);
            branches.get(i).draw(gl, shader);
        }
    }
    
}
