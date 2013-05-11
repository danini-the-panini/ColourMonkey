
import com.jogamp.common.nio.Buffers;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;
import javax.media.opengl.GL4;

public abstract class Texture
{
    protected int handle;
    
    protected int target;
    
    
    public static final HashMap<Integer, Integer> texParamsDefault = new HashMap<Integer, Integer>();
    static
    {
        texParamsDefault.put(GL4.GL_TEXTURE_MAG_FILTER, GL4.GL_LINEAR);
        texParamsDefault.put(GL4.GL_TEXTURE_MIN_FILTER, GL4.GL_LINEAR);
        texParamsDefault.put(GL4.GL_TEXTURE_WRAP_R, GL4.GL_REPEAT);
        texParamsDefault.put(GL4.GL_TEXTURE_WRAP_S, GL4.GL_REPEAT);
        texParamsDefault.put(GL4.GL_TEXTURE_WRAP_T, GL4.GL_REPEAT);
    }

    public static final HashMap<Integer, Integer> texParamsMipMap = new HashMap<Integer, Integer>();
    static
    {
        texParamsMipMap.put(GL4.GL_TEXTURE_MAG_FILTER, GL4.GL_LINEAR_MIPMAP_LINEAR);
        texParamsMipMap.put(GL4.GL_TEXTURE_MIN_FILTER, GL4.GL_LINEAR_MIPMAP_LINEAR);
        texParamsMipMap.put(GL4.GL_TEXTURE_WRAP_R, GL4.GL_REPEAT);
        texParamsMipMap.put(GL4.GL_TEXTURE_WRAP_S, GL4.GL_REPEAT);
        texParamsMipMap.put(GL4.GL_TEXTURE_WRAP_T, GL4.GL_REPEAT);
    }

    public static final HashMap<Integer, Integer> texParamsFBO = new HashMap<Integer, Integer>();
    static
    {
        texParamsFBO.put(GL4.GL_TEXTURE_MAG_FILTER, GL4.GL_NEAREST);
        texParamsFBO.put(GL4.GL_TEXTURE_MIN_FILTER, GL4.GL_NEAREST);
        texParamsFBO.put(GL4.GL_TEXTURE_WRAP_R, GL4.GL_CLAMP_TO_EDGE);
        texParamsFBO.put(GL4.GL_TEXTURE_WRAP_S, GL4.GL_CLAMP_TO_EDGE);
        texParamsFBO.put(GL4.GL_TEXTURE_WRAP_T, GL4.GL_CLAMP_TO_EDGE);
    }
    
    public static final HashMap<Integer, Integer> texParamsSkyMap = new HashMap<Integer, Integer>();
    static
    {
        texParamsSkyMap.put(GL4.GL_TEXTURE_MAG_FILTER, GL4.GL_LINEAR);
        texParamsSkyMap.put(GL4.GL_TEXTURE_MIN_FILTER, GL4.GL_LINEAR);
        texParamsSkyMap.put(GL4.GL_TEXTURE_WRAP_R, GL4.GL_CLAMP_TO_EDGE);
        texParamsSkyMap.put(GL4.GL_TEXTURE_WRAP_S, GL4.GL_CLAMP_TO_EDGE);
        texParamsSkyMap.put(GL4.GL_TEXTURE_WRAP_T, GL4.GL_CLAMP_TO_EDGE);
    }
    
    public Texture(GL4 gl, int target)
    {
        this.target = target;
        
        IntBuffer handleBuffer = Buffers.newDirectIntBuffer(1);
        
        gl.glGenTextures(1, handleBuffer);
        
        handle = handleBuffer.get();
    }
    
    public void bind(GL4 gl)
    {
        gl.glBindTexture(target, handle);
    }
    
    public void use(GL4 gl, int slot)
    {
        gl.glActiveTexture(slot);
        bind(gl);
    }
    
    public static ByteBuffer imageToBytes(BufferedImage img, int[] w_h)
    {
        int width = img.getWidth();
        int height = img.getHeight();
        
        ByteBuffer data = Buffers.newDirectByteBuffer(width * img.getColorModel().getPixelSize()/8 * height);
        
        for (int y = 0; y < height; y++)
        {
            for (int x = 0; x < width; x++)
            {
                int RGB = img.getRGB(x, y);
                data.put((byte)(RGB >> 16));
                data.put((byte)((RGB >> 8) & 0xFF));
                data.put((byte)(RGB & 0xFF));
            }
        }
        
        data.flip();
        
        w_h[0] = width;
        w_h[1] = height;
        
        return data;
    }
    
    public void setParameter(GL4 gl, int parameter, int value)
    {
        gl.glTexParameteri(
                target,
                parameter,
                value);
    }
    
    public void setParameters(GL4 gl, Map<Integer,Integer> params)
    {
        for (Map.Entry<Integer,Integer> param :params.entrySet())
        {
            setParameter(gl, param.getKey(), param.getValue());
        }
    }

    public int getHandle()
    {
        return handle;
    }

    public int getTarget()
    {
        return target;
    }
}
