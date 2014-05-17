
import com.hackoeur.jglm.Mat4;
import com.hackoeur.jglm.Vec3;
import com.hackoeur.jglm.Vec4;
import javax.media.opengl.GL4;
import java.util.HashMap;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author daniel
 */
public class Shader
{
    public static final int POSITION_LOC = 1, NORMAL_LOC = 2;

    protected int program;
    private int vertexShader;
    private int geometryShader;
    private int tessContShader;
    private int tessEvalShader;
    private int fragmentShader;
    
    private String[] feedbackVaryings = null;

    private HashMap<String,Integer> uniforms = new HashMap<String,Integer>();
    
    public Shader(GL4 gl, String name)
    {
        this(gl,name,null);
    }

    public Shader(GL4 gl, String name, String[] feedbackVaryings)
    {
        System.out.println("Loading shader " + name);

        vertexShader = Utils.loadShaderProgram(gl, "shaders/"+name+"/vertex.glsl", GL4.GL_VERTEX_SHADER);
        geometryShader = Utils.loadShaderProgram(gl, "shaders/"+name+"/geometry.glsl", GL4.GL_GEOMETRY_SHADER);
        tessContShader = Utils.loadShaderProgram(gl, "shaders/"+name+"/tesselation_control.glsl", GL4.GL_TESS_CONTROL_SHADER);
        tessEvalShader = Utils.loadShaderProgram(gl, "shaders/"+name+"/tesselation_evaluation.glsl", GL4.GL_TESS_EVALUATION_SHADER);
        fragmentShader = Utils.loadShaderProgram(gl, "shaders/"+name+"/fragment.glsl", GL4.GL_FRAGMENT_SHADER);

       /* Start by defining a shader program which acts as a container. */
       program = gl.glCreateProgram();

       /* Next, shaders are added to the shader program. */
       if (vertexShader != -1) gl.glAttachShader(program, vertexShader);
       if (geometryShader != -1) gl.glAttachShader(program, geometryShader);
       if (tessContShader != -1) gl.glAttachShader(program, tessContShader);
       if (tessEvalShader != -1) gl.glAttachShader(program, tessEvalShader);
       if (fragmentShader != -1) gl.glAttachShader(program, fragmentShader);
       
       // optional transform feedback
       if (feedbackVaryings != null)
       {
           gl.glTransformFeedbackVaryings(program, feedbackVaryings.length, feedbackVaryings, GL4.GL_INTERLEAVED_ATTRIBS);
       }

       /* Finally, the program must be linked. */
       gl.glLinkProgram(program);

       /* Check if it linked  properly. */
       Utils.checkProgramLogInfo(gl, program);
    }

    public void use(GL4 gl)
    {
        gl.glUseProgram(program);
    }

    public int getProgram()
    {
        return program;
    }

    public void setFeedbackVaryings(String[] feedbackVaryings)
    {
        this.feedbackVaryings = feedbackVaryings;
    }

    public int findUniform(GL4 gl, String name)
    {
       Integer uniform = uniforms.get(name);
       if (uniform == null)
       {
         uniform = gl.glGetUniformLocation(program, name);
         uniforms.put(name, uniform);
       }
       return uniform;
    }
    
    public void bindUniformBlock(GL4 gl, String name, int bindingPt)
    {
        gl.glUniformBlockBinding(program, findUniformBlock(gl, name), bindingPt);
    }
    
    
    public int findUniformBlock(GL4 gl, String name)
    {
       Integer uniform = uniforms.get(name);
       if (uniform == null)
       {
         uniform = gl.glGetUniformBlockIndex(program, name);
         uniforms.put(name, uniform);
       }
       return uniform;
    }

    public void updateUniform(GL4 gl, String name, Mat4 value)
    {
        int uniform = findUniform(gl, name);
        if (uniform == -1) return;
        gl.glUniformMatrix4fv(uniform, 1, false, value.getBuffer());
    }

    public void updateUniform(GL4 gl, String name, float value)
    {
        int uniform = findUniform(gl, name);
        if (uniform == -1) return;
        gl.glUniform1f(uniform, value);
    }

    public void updateUniform(GL4 gl, String name, Vec3 value)
    {
        int uniform = findUniform(gl, name);
        if (uniform == -1) return;
        gl.glUniform3fv(uniform, 1, value.getBuffer());
    }

    public void updateUniform(GL4 gl, String name, Vec4 value)
    {
        int uniform = findUniform(gl, name);
        if (uniform == -1) return;
        gl.glUniform4fv(uniform, 1, value.getBuffer());
    }

    public void updateUniform(GL4 gl, String name, int value)
    {
        int uniform = findUniform(gl, name);
        if (uniform == -1) return;
        gl.glUniform1i(uniform, value);
    }
}
