
import com.jogamp.common.nio.Buffers;
import java.nio.IntBuffer;
import javax.media.opengl.GL4;

public class FrameBuffer
{
    private int handle;
    private int width, height;
    private int renderedTextureHandle;
    
    public FrameBuffer(GL4 gl, int width, int height)
    {
        this.width = width;
        this.height = height;
        
        IntBuffer handleBuf = Buffers.newDirectIntBuffer(1);
        gl.glGenFramebuffers(1, handleBuf);
        handle = handleBuf.get();
        System.out.println("FBO handle: " + handle);
        bind(gl);
        
        
        // The texture we're going to render to
        IntBuffer renderedTexture = Buffers.newDirectIntBuffer(1);
        gl.glGenTextures(1, renderedTexture);
        renderedTextureHandle = renderedTexture.get();

        // "Bind" the newly created texture : all future texture functions will modify this texture
        gl.glBindTexture(GL4.GL_TEXTURE_2D, renderedTextureHandle);

        // Give an empty image to OpenGL ( the last "0" )
        gl.glTexImage2D(GL4.GL_TEXTURE_2D, 0, GL4.GL_RGBA, width, height, 0, GL4.GL_RGBA, GL4.GL_UNSIGNED_BYTE, null);

        // Poor filtering. Needed !
        gl.glTexParameteri(GL4.GL_TEXTURE_2D, GL4.GL_TEXTURE_MAG_FILTER, GL4.GL_LINEAR);
        gl.glTexParameteri(GL4.GL_TEXTURE_2D, GL4.GL_TEXTURE_MIN_FILTER, GL4.GL_LINEAR);
        gl.glTexParameteri(GL4.GL_TEXTURE_2D, GL4.GL_TEXTURE_WRAP_R, GL4.GL_CLAMP_TO_EDGE);
        gl.glTexParameteri(GL4.GL_TEXTURE_2D, GL4.GL_TEXTURE_WRAP_S, GL4.GL_CLAMP_TO_EDGE);
        gl.glTexParameteri(GL4.GL_TEXTURE_2D, GL4.GL_TEXTURE_WRAP_T, GL4.GL_CLAMP_TO_EDGE);
        
        
        // Set "renderedTexture" as our colour attachement #0
        gl.glFramebufferTexture(GL4.GL_FRAMEBUFFER, GL4.GL_COLOR_ATTACHMENT0, renderedTextureHandle, 0);
        
        
        // depth buffer
        IntBuffer depthrenderBuffer = Buffers.newDirectIntBuffer(1);
        gl.glGenRenderbuffers(1, depthrenderBuffer);
        int depthrenderBufferHandle = depthrenderBuffer.get();
        
        gl.glBindRenderbuffer(GL4.GL_RENDERBUFFER, depthrenderBufferHandle);
        gl.glRenderbufferStorage(GL4.GL_RENDERBUFFER, GL4.GL_DEPTH_COMPONENT24, width, height);
        gl.glFramebufferRenderbuffer(GL4.GL_FRAMEBUFFER, GL4.GL_DEPTH_ATTACHMENT, GL4.GL_RENDERBUFFER, depthrenderBufferHandle);

        // Always check that our framebuffer is ok
        int status = gl.glCheckFramebufferStatus(GL4.GL_FRAMEBUFFER);
        if(status != GL4.GL_FRAMEBUFFER_COMPLETE)
        {
            System.out.println("FRAMEBUFFER ERROR! " + status);
        }
        else
        {
            System.out.println("Framebuffer Complete.");
        }
        
        unbind(gl);

    }
    
    public final void bind(GL4 gl)
    {
        gl.glBindFramebuffer(GL4.GL_FRAMEBUFFER, handle);
    }
    
    public static void unbind(GL4 gl)
    {
        gl.glBindFramebuffer(GL4.GL_FRAMEBUFFER, 0);
    }
    
    public final void use(GL4 gl)
    {
        bind(gl);
        
        // Set the list of draw buffers.
        int[] DrawBuffers = {GL4.GL_COLOR_ATTACHMENT0};
        gl.glDrawBuffers(1, DrawBuffers, 0); // "1" is the size of DrawBuffers
        
        gl.glClear(GL4.GL_COLOR_BUFFER_BIT | GL4.GL_DEPTH_BUFFER_BIT);
        
        gl.glViewport(0, 0, width, height);
    }
    
    public final void bindTexture(GL4 gl, int slot)
    {
        gl.glActiveTexture(slot);
        gl.glBindTexture(GL4.GL_TEXTURE_2D, renderedTextureHandle);
    }
}