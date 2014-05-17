
import com.hackoeur.jglm.Mat4;
import com.hackoeur.jglm.Matrices;
import com.hackoeur.jglm.Vec3;
import com.jogamp.common.nio.Buffers;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
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
            
            branches.add(new Branch(gl, state.branch, parent, new Vec3(0,amount,0), state.dir,
                    tangent, bmesh));
            
            state.branch = branches.size()-1;
            state.pos = state.pos.add(state.dir.scale(amount));
            
            System.out.print(":F ");
        }
        
    }
    
    class Rotate implements Runnable
    {
        float amount;
        Vec3 axis;

        public Rotate(float amount, Vec3 axis)
        {
            this.amount = amount;
            this.axis = axis;
        }
        
        @Override
        public void run()
        {
            Mat4 rot = Matrices.rotate(Mat4.MAT4_IDENTITY, amount, axis);
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
    
    private static final Vec3 XAXIS = new Vec3(1,0,0),
            YAXIS = new Vec3(0,1,0), ZAXIS = new Vec3(0,0,1);
    
    private final Runnable F = new Forward(1.0f),
            _s = new SaveState(), s_ = new LoadState(),
            Lz = new Rotate(30.0f,ZAXIS),
            Rz = new Rotate(-30.0f, ZAXIS),
            Lx = new Rotate(30.0f,XAXIS),
            Rx = new Rotate(-30.0f, XAXIS),
            Ly = new Rotate(30.0f,YAXIS),
            Ry = new Rotate(-30.0f, YAXIS);
    
    public static final int VEC3_SIZE = (Float.SIZE*3)/8;
    public static final int MAT4_SIZE = (Float.SIZE*16)/8;
    public static final int INT_SIZE = (Integer.SIZE)/8;

    public Tree(GL4 gl, Shader shader)
    {
        this.gl = gl;
        
        bmesh = new WavefrontMesh(gl, "branch.obj");
        
        lsys = new LSystem(new Runnable[]{F});
        lsys.addRule(F, new Runnable[]{F, _s, Lz, F, Lz, F,s_, _s, Rz, F, Rz, F, s_, F,
                                    _s, Lx, F, Lx, F, s_, _s, Rx, F, Rx, F, s_});
        lsys.addRule(Lz, new Runnable[]{Lz, F});
        lsys.addRule(Rz, new Runnable[]{F, Rz});
        lsys.addRule(Lx, new Runnable[]{Lx, F});
        lsys.addRule(Rx, new Runnable[]{F, Rx});
        lsys.iterate(2);
        lsys.run();
        
        System.out.println("Num Branches: " + branches.size());
        
        ByteBuffer parentData = Buffers.newDirectByteBuffer(branches.size()*INT_SIZE);
        ByteBuffer originData = Buffers.newDirectByteBuffer(branches.size()*VEC3_SIZE);
        ByteBuffer axisData = Buffers.newDirectByteBuffer(branches.size()*VEC3_SIZE);
        ByteBuffer tangentData = Buffers.newDirectByteBuffer(branches.size()*VEC3_SIZE);
        ByteBuffer worldData = Buffers.newDirectByteBuffer(branches.size()*MAT4_SIZE);
        Vec3 v;
        for (Branch b : branches)
        {
            parentData.putInt(b.getParentID());
            v = b.getOrigin();
            originData.putFloat(v.getX());
            originData.putFloat(v.getY());
            originData.putFloat(v.getZ());
            v = b.getAxis();
            axisData.putFloat(v.getX());
            axisData.putFloat(v.getY());
            axisData.putFloat(v.getZ());
            v = b.getTangent();
            tangentData.putFloat(v.getX());
            tangentData.putFloat(v.getY());
            tangentData.putFloat(v.getZ());
            
            worldData.put(Buffers.copyFloatBufferAsByteBuffer(b.getLocalMat().getBuffer()));
        }
        
        parentData.flip();
        originData.flip();
        axisData.flip();
        tangentData.flip();
        worldData.flip();
        
        linkUniformBuffer("parent_block", parentData, shader);
        linkUniformBuffer("origin_block", originData, shader);
        linkUniformBuffer("axis_block", axisData, shader);
        linkUniformBuffer("tangent_block", tangentData, shader);
        linkUniformBuffer("world_block", worldData, shader);
        
        Utils.checkError(gl, "Tree");
    }
    
    private void linkUniformBuffer(String name, ByteBuffer buff, Shader shader)
    {
        int bindingPoint = 1;
        IntBuffer buffer = Buffers.newDirectIntBuffer(1);

        shader.bindUniformBlock(gl, name, bindingPoint);

        gl.glGenBuffers(1, buffer);
        int bufferVal = buffer.get();
        gl.glBindBuffer(GL4.GL_UNIFORM_BUFFER, bufferVal);

        gl.glBufferData(GL4.GL_UNIFORM_BUFFER, branches.size()*INT_SIZE, buff, GL4.GL_DYNAMIC_DRAW);
        gl.glBindBufferBase(GL4.GL_UNIFORM_BUFFER, bindingPoint, bufferVal);
    }
    
    private Shader treeShader;
    
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
