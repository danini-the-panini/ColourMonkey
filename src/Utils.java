
import com.jogamp.common.nio.Buffers;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import javax.imageio.ImageIO;
import javax.media.opengl.GL;
import javax.media.opengl.GL4;
import javax.media.opengl.GLException;

public class Utils {
    
    public static int
    findAttribute(GL4 gl, String name, int program)
    {
       return gl.glGetAttribLocation(program, name);
    }

    public static int
    findUniform(GL4 gl, String name, int program)
    {
       return gl.glGetUniformLocation(program, name);
    }
    
    static String
    loadFile(String fileName)
    {
       String file = "";
       
       BufferedReader br = null;
       try
       {
           br  = new BufferedReader(new FileReader(fileName));
           
           String line;
           while ((line = br.readLine()) != null )
           {
               file += line+"\n";
           }
       }
       catch (IOException e)
       {
           if (br != null)
           {
               try {
                   br.close();
               } catch (IOException ex) {}
           }
           
           //System.err.printf("Error loading file %s\n", fileName);
           return null;
       }
       
       return file;
    }
    
    static int
    loadShaderProgram(GL4 gl, String file, int shaderType)
    {
        String source = Utils.loadFile(file);
        
        if (source == null) return -1;
        
       /* First, a handle to a shader object of the appropriate type must be 
        * obtained. */
       int handle = gl.glCreateShader(shaderType);

       /* ...and set to be the current source in the OpenGL state machine. The first 
        * parameter here is a handle to a shader object, the second is the number of 
        * strings in the array which we provide next. The last parameter is an 
        * integer array indicating the length of these strings. The third parameter
        * may also contain the whole source code as one long string, while the 
        * fourth parameter may be NULL, to indicate that the string(s) is (are) NULL 
        * terminated. Since that is the case in our implementation, the arguments 
        * passed here correspond. */
       gl.glShaderSource(handle, 1, new String[]{source}, null);

       /* Third, we compile the shader program. */
       gl.glCompileShader(handle);
       
       /* Check if it compiled properly, if not, print any log information. */
       checkShaderLogInfo(gl, handle);
       
       return handle;
    }
    
    public static void checkShaderLogInfo(GL4 inGL, int inShaderObjectID) {
        IntBuffer tReturnValue = Buffers.newDirectIntBuffer(1);
        inGL.glGetShaderiv(inShaderObjectID, GL4.GL_COMPILE_STATUS, tReturnValue);
        if (tReturnValue.get(0) == GL.GL_FALSE) {
                inGL.glGetShaderiv(inShaderObjectID, GL4.GL_INFO_LOG_LENGTH, tReturnValue);
                final int length = tReturnValue.get(0);
                String out = null;
                if (length > 0) {
                    final ByteBuffer infoLog = Buffers.newDirectByteBuffer(length);
                    inGL.glGetShaderInfoLog(inShaderObjectID, infoLog.limit(), tReturnValue, infoLog);
                    final byte[] infoBytes = new byte[length];
                    infoLog.get(infoBytes);
                    out = new String(infoBytes);
                    System.out.print(out);
                }
                throw new GLException("Error during shader compilation:\n" + out + "\n");
            } 
    }
    
    public static void checkProgramLogInfo(GL4 inGL, int inShaderObjectID) {
        IntBuffer tReturnValue = Buffers.newDirectIntBuffer(1);
        inGL.glGetProgramiv(inShaderObjectID, GL4.GL_LINK_STATUS, tReturnValue);
        if (tReturnValue.get(0) == GL.GL_FALSE) {
                inGL.glGetProgramiv(inShaderObjectID, GL4.GL_INFO_LOG_LENGTH, tReturnValue);
                final int length = tReturnValue.get(0);
                String out = null;
                if (length > 0) {
                    final ByteBuffer infoLog = Buffers.newDirectByteBuffer(length);
                    inGL.glGetProgramInfoLog(inShaderObjectID, infoLog.limit(), tReturnValue, infoLog);
                    final byte[] infoBytes = new byte[length];
                    infoLog.get(infoBytes);
                    out = new String(infoBytes);
                    System.out.print(out);
                }
                throw new GLException("Error during shader linking:\n" + out + "\n");
            } 
    }
    
    public static ByteBuffer loadTexture(String fileName, int[] w_h)
            throws IOException
    {
        BufferedImage img = ImageIO.read(new File(fileName));
        
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
    
    public static BufferedImage resize(BufferedImage originalImage, final int IMG_WIDTH, final int IMG_HEIGHT){
 
	BufferedImage resizedImage = new BufferedImage(IMG_WIDTH, IMG_HEIGHT, originalImage.getType());
	Graphics2D g = resizedImage.createGraphics();
	g.drawImage(originalImage, 0, 0, IMG_WIDTH, IMG_HEIGHT, null);
	g.dispose();	
	g.setComposite(AlphaComposite.Src);
 
	g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
	RenderingHints.VALUE_INTERPOLATION_BICUBIC);
	g.setRenderingHint(RenderingHints.KEY_RENDERING,
	RenderingHints.VALUE_RENDER_QUALITY);
	g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
	RenderingHints.VALUE_ANTIALIAS_ON);
 
	return resizedImage;
    }
    
    

    static void loadSkyMap(GL4 gl) throws IOException
    {
        gl.glActiveTexture(GL.GL_TEXTURE0);
        
        IntBuffer texName = Buffers.newDirectIntBuffer(1);
        gl.glGenTextures(1, texName);
        gl.glBindTexture(GL.GL_TEXTURE_CUBE_MAP, texName.get());

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
                GL.GL_TEXTURE_WRAP_S,
                GL.GL_REPEAT);
        gl.glTexParameteri(
                GL.GL_TEXTURE_CUBE_MAP,
                GL.GL_TEXTURE_WRAP_T,
                GL.GL_REPEAT);

        int[] w_h = new int[2];

        ByteBuffer data = Utils.loadTexture("skybox/terrain_positive_x.png", w_h);
        gl.glTexImage2D(GL.GL_TEXTURE_CUBE_MAP_POSITIVE_X, 0, GL.GL_RGB,
                w_h[0], w_h[1], 0, GL.GL_RGB,
                GL.GL_UNSIGNED_BYTE, data);

        data = Utils.loadTexture("skybox/terrain_negative_x.png", w_h);
        gl.glTexImage2D(GL.GL_TEXTURE_CUBE_MAP_NEGATIVE_X, 0, GL.GL_RGB,
                w_h[0], w_h[1], 0, GL.GL_RGB,
                GL.GL_UNSIGNED_BYTE, data);

        data = Utils.loadTexture("skybox/terrain_positive_y.png", w_h);
        gl.glTexImage2D(GL.GL_TEXTURE_CUBE_MAP_POSITIVE_Y, 0, GL.GL_RGB,
                w_h[0], w_h[1], 0, GL.GL_RGB,
                GL.GL_UNSIGNED_BYTE, data);
        data = Utils.loadTexture("skybox/terrain_negative_y.png", w_h);
        gl.glTexImage2D(GL.GL_TEXTURE_CUBE_MAP_NEGATIVE_Y, 0, GL.GL_RGB,
                w_h[0], w_h[1], 0, GL.GL_RGB,
                GL.GL_UNSIGNED_BYTE, data);

        data = Utils.loadTexture("skybox/terrain_positive_z.png", w_h);
        gl.glTexImage2D(GL.GL_TEXTURE_CUBE_MAP_POSITIVE_Z, 0, GL.GL_RGB,
                w_h[0], w_h[1], 0, GL.GL_RGB,
                GL.GL_UNSIGNED_BYTE, data);
        
        data = Utils.loadTexture("skybox/terrain_negative_z.png", w_h);
        gl.glTexImage2D(GL.GL_TEXTURE_CUBE_MAP_NEGATIVE_Z, 0, GL.GL_RGB,
                w_h[0], w_h[1], 0, GL.GL_RGB,
                GL.GL_UNSIGNED_BYTE, data);
        
    }
    
    static void loadTexture(GL4 gl, ByteBuffer data, int width, int height, int slot)
    {
        gl.glActiveTexture(slot);
        
        IntBuffer texName = Buffers.newDirectIntBuffer(1);
        gl.glGenTextures(1, texName);
        gl.glBindTexture(GL.GL_TEXTURE_2D, texName.get());

        gl.glTexParameteri(
                GL.GL_TEXTURE_2D,
                GL.GL_TEXTURE_MAG_FILTER,
                GL.GL_LINEAR_MIPMAP_LINEAR);
        gl.glTexParameteri(
                GL.GL_TEXTURE_2D,
                GL.GL_TEXTURE_MIN_FILTER,
                GL.GL_LINEAR_MIPMAP_LINEAR);
        gl.glTexParameteri(
                GL.GL_TEXTURE_2D,
                GL.GL_TEXTURE_WRAP_S,
                GL.GL_REPEAT);
        gl.glTexParameteri(
                GL.GL_TEXTURE_2D,
                GL.GL_TEXTURE_WRAP_T,
                GL.GL_REPEAT);
        
        gl.glTexImage2D(GL.GL_TEXTURE_2D, 0, GL.GL_RGB,
                width, height, 0, GL.GL_RGB,
                GL.GL_UNSIGNED_BYTE,
                data);
        
        gl.glGenerateMipmap(GL.GL_TEXTURE_2D);

    }
    
    static void loadBuffer(GL4 gl, IntBuffer data, int size, int slot)
    {
        // TODO: probably not a good idea
        // as we should have some way of blowing it up eventually
        IntBuffer bufferHandlebuf = Buffers.newDirectIntBuffer(1);
        gl.glGenBuffers(1, bufferHandlebuf);
        int bufferHandle = bufferHandlebuf.get();
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, bufferHandle);
        
        gl.glBufferData(GL.GL_ARRAY_BUFFER, size, data,
                GL4.GL_STATIC_DRAW);
        
        gl.glActiveTexture(slot);
        
        IntBuffer texName = Buffers.newDirectIntBuffer(1);
        gl.glGenTextures(1, texName);
        gl.glBindTexture(GL4.GL_TEXTURE_BUFFER, texName.get());
        
        gl.glTexParameteri(
                GL.GL_TEXTURE_2D,
                GL.GL_TEXTURE_MAG_FILTER,
                GL.GL_NEAREST);
        gl.glTexParameteri(
                GL.GL_TEXTURE_2D,
                GL.GL_TEXTURE_MIN_FILTER,
                GL.GL_NEAREST);
        gl.glTexParameteri(
                GL.GL_TEXTURE_2D,
                GL.GL_TEXTURE_WRAP_S,
                GL.GL_CLAMP_TO_EDGE);
        gl.glTexParameteri(
                GL.GL_TEXTURE_2D,
                GL.GL_TEXTURE_WRAP_T,
                GL.GL_CLAMP_TO_EDGE);
        
        gl.glTexBuffer(GL4.GL_TEXTURE_BUFFER, GL4.GL_RGB32I, bufferHandle);
        System.out.println("Error? " + gl.glGetError());

    }
}
