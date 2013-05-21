
import com.jogamp.common.nio.Buffers;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import javax.imageio.ImageIO;
import javax.media.opengl.GL4;

public class FrameBuffer
{
    private int handle;
    private int width, height;
    private boolean hasDepth;
    private int depthTextureHandle;
    
    public FrameBuffer(GL4 gl, int width, int height, boolean depthUseTexture)
    {
        this.hasDepth = depthUseTexture;
        
        this.width = width;
        this.height = height;
        
        IntBuffer handleBuf = Buffers.newDirectIntBuffer(1);
        gl.glGenFramebuffers(1, handleBuf);
        handle = handleBuf.get();
        System.out.println("FBO handle: " + handle);
        bind(gl);
        
        // depth buffer
        
        if (depthUseTexture)
        {
            // The texture we're going to render to
            IntBuffer depthTexture = Buffers.newDirectIntBuffer(1);
            gl.glGenTextures(1, depthTexture);
            depthTextureHandle = depthTexture.get();

            // "Bind" the newly created texture : all future texture functions will modify this texture
            gl.glBindTexture(GL4.GL_TEXTURE_2D, depthTextureHandle);

            // Give an empty image to OpenGL ( the last "0" )
            gl.glTexImage2D(GL4.GL_TEXTURE_2D, 0, GL4.GL_DEPTH_COMPONENT24, width, height, 0, GL4.GL_DEPTH_COMPONENT, GL4.GL_UNSIGNED_BYTE, null);

            // Poor filtering. Needed !
            gl.glTexParameteri(GL4.GL_TEXTURE_2D, GL4.GL_TEXTURE_MAG_FILTER, GL4.GL_LINEAR);
            gl.glTexParameteri(GL4.GL_TEXTURE_2D, GL4.GL_TEXTURE_MIN_FILTER, GL4.GL_LINEAR);
            gl.glTexParameteri(GL4.GL_TEXTURE_2D, GL4.GL_TEXTURE_WRAP_R, GL4.GL_CLAMP_TO_EDGE);
            gl.glTexParameteri(GL4.GL_TEXTURE_2D, GL4.GL_TEXTURE_WRAP_S, GL4.GL_CLAMP_TO_EDGE);
            gl.glTexParameteri(GL4.GL_TEXTURE_2D, GL4.GL_TEXTURE_WRAP_T, GL4.GL_CLAMP_TO_EDGE);


            gl.glFramebufferTexture(GL4.GL_FRAMEBUFFER, GL4.GL_DEPTH_ATTACHMENT, depthTextureHandle, 0);
        }
        else
        {
            IntBuffer depthrenderBuffer = Buffers.newDirectIntBuffer(1);
            gl.glGenRenderbuffers(1, depthrenderBuffer);
            int depthrenderBufferHandle = depthrenderBuffer.get();

            gl.glBindRenderbuffer(GL4.GL_RENDERBUFFER, depthrenderBufferHandle);
            gl.glRenderbufferStorage(GL4.GL_RENDERBUFFER, GL4.GL_DEPTH_COMPONENT24, width, height);
            gl.glFramebufferRenderbuffer(GL4.GL_FRAMEBUFFER, GL4.GL_DEPTH_ATTACHMENT, GL4.GL_RENDERBUFFER, depthrenderBufferHandle);
        }

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
    
    public final void bindTexture(GL4 gl, int attachment, int texTarget, int texHandle)
    {
        bind(gl);
        gl.glFramebufferTexture2D(GL4.GL_FRAMEBUFFER, attachment, texTarget, texHandle, 0);
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
        this.use(gl, new int[]{GL4.GL_COLOR_ATTACHMENT0});
    }
    
    public final void use(GL4 gl, int[] DrawBuffers)
    {
        gl.glDrawBuffers(1, DrawBuffers, 0); // "1" is the size of DrawBuffers
        
        gl.glClear(GL4.GL_COLOR_BUFFER_BIT | GL4.GL_DEPTH_BUFFER_BIT);
        
        gl.glViewport(0, 0, width, height);
    }
    
    public final void bindDepthBuffer(GL4 gl, int slot)
    {
        gl.glActiveTexture(slot);
        gl.glBindTexture(GL4.GL_TEXTURE_2D, depthTextureHandle);
    }
    
    public final boolean hasDepthTexture()
    {
        return hasDepth;
    }
    
    public byte[] readPixel(GL4 gl, int x, int y, int a)
    {
          ByteBuffer pixelsRGB = Buffers.newDirectByteBuffer(4);
          
          bind(gl);
          
          gl.glReadBuffer(GL4.GL_COLOR_ATTACHMENT0+a);
          gl.glPixelStorei(GL4.GL_PACK_ALIGNMENT, 1);
          
          gl.glReadPixels(
                      x,                    // GLint x
                      y,                    // GLint y
                      1,                     // GLsizei width
                      1,              // GLsizei height
                      GL4.GL_RGBA,              // GLenum format
                      GL4.GL_UNSIGNED_BYTE,        // GLenum type
                      pixelsRGB);               // GLvoid *pixels
          
          byte[] result = new byte[4];
          pixelsRGB.get(result);
          
          return result;
    }
    
    public void writeBufferToFile(GL4 gl, File outputFile, int a) throws IOException {

          bind(gl);
                  
          ByteBuffer pixelsRGB = Buffers.newDirectByteBuffer(width * height * 3);

          gl.glReadBuffer(GL4.GL_COLOR_ATTACHMENT0+a);
          gl.glPixelStorei(GL4.GL_PACK_ALIGNMENT, 1);

          gl.glReadPixels(0,                    // GLint x
                      0,                    // GLint y
                      width,                     // GLsizei width
                      height,              // GLsizei height
                      GL4.GL_RGB,              // GLenum format
                      GL4.GL_UNSIGNED_BYTE,        // GLenum type
                      pixelsRGB);               // GLvoid *pixels

          int[] pixelInts = new int[width * height];

          // Convert RGB bytes to ARGB ints with no transparency. Flip image vertically by reading the
          // rows of pixels in the byte buffer in reverse - (0,0) is at bottom left in OpenGL.

          int p = width * height * 3; // Points to first byte (red) in each row.
          int q;                  // Index into ByteBuffer
          int i = 0;                  // Index into target int[]
          int w3 = width*3;         // Number of bytes in each row

          for (int row = 0; row < height; row++) {
                p -= w3;
                q = p;
                for (int col = 0; col < width; col++) {
                      int iR = pixelsRGB.get(q++);
                      int iG = pixelsRGB.get(q++);
                      int iB = pixelsRGB.get(q++);

                      pixelInts[i++] = 0xFF000000
                                  | ((iR & 0x000000FF) << 16)
                                  | ((iG & 0x000000FF) << 8)
                                  | (iB & 0x000000FF);
                }

          }

          BufferedImage bufferedImage =
                new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

          bufferedImage.setRGB(0, 0, width, height, pixelInts, 0, width);

        ImageIO.write(bufferedImage, "PNG", outputFile);

    }
}
