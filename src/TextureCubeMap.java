
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import javax.imageio.ImageIO;
import javax.media.opengl.GL;
import javax.media.opengl.GL4;


public class TextureCubeMap extends Texture
{
    public static TextureCubeMap fromFiles(GL4 gl, File[] files)
            throws IOException
    {
        BufferedImage[] images = new BufferedImage[6];
        for (int i = 0; i < 6; i++)
        {
            images[i] = ImageIO.read(files[i]);
        }
        
        return new TextureCubeMap(gl, images);
    }
    
    private TextureCubeMap(GL4 gl)
    {
        super(gl, GL.GL_TEXTURE_CUBE_MAP);
        bind(gl);
    }
    
    private void setup(GL4 gl, ByteBuffer[] data, int width, int height)
    {
        gl.glTexParameteri(
                GL.GL_TEXTURE_CUBE_MAP,
                GL.GL_TEXTURE_MAG_FILTER,
                GL.GL_LINEAR);
        gl.glTexParameteri(
                GL.GL_TEXTURE_CUBE_MAP,
                GL.GL_TEXTURE_MIN_FILTER,
                GL.GL_LINEAR);
        gl.glTexParameteri(
                GL.GL_TEXTURE_CUBE_MAP,
                GL4.GL_TEXTURE_WRAP_R,
                GL.GL_CLAMP_TO_EDGE);
        gl.glTexParameteri(
                GL.GL_TEXTURE_CUBE_MAP,
                GL.GL_TEXTURE_WRAP_S,
                GL.GL_CLAMP_TO_EDGE);
        gl.glTexParameteri(
                GL.GL_TEXTURE_CUBE_MAP,
                GL.GL_TEXTURE_WRAP_T,
                GL.GL_CLAMP_TO_EDGE);
        
        for (int i = 0; i < 6; i++)
        {
            gl.glTexImage2D(GL.GL_TEXTURE_CUBE_MAP_POSITIVE_X+i, 0, GL.GL_RGB,
                    width, height, 0, GL.GL_RGB,
                    GL.GL_UNSIGNED_BYTE,
                    data == null ? null : data[i]);
        }
    }
    
    public TextureCubeMap(GL4 gl, ByteBuffer[] data, int width, int height)
    {
        this(gl);
        
        setup(gl, data, width, height);
    }

    public TextureCubeMap(GL4 gl, BufferedImage[] input)
    {
        this(gl);
        
        ByteBuffer[] data = new ByteBuffer[6];
        int[] w_h = new int[2];
        for (int i = 0; i < 6; i++)
        {
            data[i] = imageToBytes(input[i], w_h);
        }
        
        setup(gl, data, w_h[0], w_h[1]);
    }
    
    public TextureCubeMap(GL4 gl, int width, int height)
    {
        this(gl, null, width, height);
    }
}
