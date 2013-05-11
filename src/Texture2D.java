
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import javax.imageio.ImageIO;
import javax.media.opengl.GL;
import javax.media.opengl.GL4;


public class Texture2D extends Texture
{
    public static Texture2D fromFile(GL4 gl, File file)
            throws IOException
    {
        return new Texture2D(gl, ImageIO.read(file));
    }
    
    private Texture2D(GL4 gl)
    {
        super(gl, GL.GL_TEXTURE_2D);
        bind(gl);
    }
    
    private void setup(GL4 gl, ByteBuffer data, int width, int height)
    {
        
        gl.glTexImage2D(GL.GL_TEXTURE_2D, 0, GL.GL_RGB,
                width, height, 0, GL.GL_RGB,
                GL.GL_UNSIGNED_BYTE,
                data);
    }
    
    public void generateMipmap(GL4 gl)
    {
        gl.glGenerateMipmap(GL.GL_TEXTURE_2D);
    }
    
    public Texture2D(GL4 gl, ByteBuffer data, int width, int height)
    {
        this(gl);
        
        setup(gl, data, width, height);
    }

    public Texture2D(GL4 gl, BufferedImage input)
    {
        this(gl);
        
        int[] w_h = new int[2];
        ByteBuffer data = imageToBytes(input, w_h);
        
        setup(gl, data, w_h[0], w_h[1]);
    }
    
    public Texture2D(GL4 gl, int width, int height)
    {
        this(gl, null, width, height);
    }
    
}
