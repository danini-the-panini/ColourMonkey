
import com.jogamp.common.nio.Buffers;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import javax.media.opengl.GL;
import javax.media.opengl.GL4;

/**
 * This class represents a Mesh loaded from a Wavefront OBJ file.
 *
 * @author daniel
 */
public class WavefrontMesh extends Mesh
{

    private FloatBuffer vertices;
    private IntBuffer indices;

    /**
     * Constructs a Mesh object from a specified wavefron OBJ file.
     * @param gl The GL implementation to use to bind the object.
     * @param name The filename to load from.
     */
    public WavefrontMesh(GL4 gl, String name)
    {
        super(gl);
        
        String fileName = "meshes/"+name;        


	// We need these because we don't know how many yet, so we can't allocate direct buffers yet
        ArrayList<Float> verts = new ArrayList<Float>(); // Read in vertices
        ArrayList<Float> norms = new ArrayList<Float>(); // Read in vertex normals. NOTE: these do not match 1:1 to vertices.
        ArrayList<Integer> inds = new ArrayList<Integer>(); // Vertex indices
        ArrayList<Integer> ninds = new ArrayList<Integer>(); // Vertex normal indices, these are necessary to match up with vertices

        BufferedReader br = null;
        try
        {
            br = new BufferedReader(new FileReader(fileName));

            String line;
            String[] list;
            while ((line = br.readLine()) != null)
            {
                if (line.startsWith("v "))
                {
                    list = line.split(" ");
		    // read X Y Z into vertex array
                    verts.add(Float.valueOf(list[1]));
                    verts.add(Float.valueOf(list[2]));
                    verts.add(Float.valueOf(list[3]));
                } else if (line.startsWith("vn "))
                {
                    list = line.split(" ");
		    // Read X Y Z into normal array
                    norms.add(Float.valueOf(list[1]));
                    norms.add(Float.valueOf(list[2]));
                    norms.add(Float.valueOf(list[3]));
                } else if (line.startsWith("f "))
                {
                    list = line.split(" ");
		    // Indices expected in the format "v//vn"
		    // TODO: need to modify this to account for "v", "v/vt" and "v/vt/vn"
                    inds.add(Integer.valueOf(list[1].substring(0, list[1].indexOf("/"))) - 1);
                    inds.add(Integer.valueOf(list[2].substring(0, list[2].indexOf("/"))) - 1);
                    inds.add(Integer.valueOf(list[3].substring(0, list[3].indexOf("/"))) - 1);

                    ninds.add(Integer.valueOf(list[1].substring(list[1].indexOf("/") + 2)) - 1);
                    ninds.add(Integer.valueOf(list[2].substring(list[2].indexOf("/") + 2)) - 1);
                    ninds.add(Integer.valueOf(list[3].substring(list[3].indexOf("/") + 2)) - 1);
                }
            }
        } catch (IOException e)
        {
            if (br != null)
            {
                try
                {
                    br.close();
                } catch (IOException ex)
                {
                }
            }

            System.err.printf("Error loading file %s\n", fileName);
        }

	// Now we use "ninds" to match up the vertex normals with their correct normals.
	// Because for each face "v//vn" it is not necessarily the case that v == vn. eg "1//2" means "vertex one has normal two".
	// ... and OpenGL can't handle this kind of representation (i.e. there's no such thing as a "Normal Index Array" separate from the vertex index array.)
	// This assumes there are exactly as many normals as there are vertices (i.e. no funny business)
	// TODO: need to be able to account for multiple normals per vertex, this could be done by duplicating the position with the alternate normal.

	// set up array for storing the "sorted" normals
        float[] normalArray = new float[verts.size()];
        boolean[] visited = new boolean[normalArray.length]; // array for keeping track of
        for (int i = 0; i < visited.length; i++)
        {
            visited[i] = false;
        }
	
	// ok now we actually match things up
        for (int i = 0; i < ninds.size(); i++)
        {
            int vi = inds.get(i);
            if (visited[vi]) // we can skip a normal we've already sorted out.
            {
                continue;
            }
            visited[vi] = true;
            int ni = ninds.get(i);

	    // since there are three components per index, we need to "scale" the index up.
            vi *= 3;
            ni *= 3;

            normalArray[vi] = norms.get(ni);
            normalArray[vi + 1] = norms.get(ni + 1);
            normalArray[vi + 2] = norms.get(ni + 2);
        }

	// here on now we put all the information we have gathered into buffer objects to send to JOGL...

        indexArraySize = inds.size();

        vertices = Buffers.newDirectFloatBuffer(verts.size() * 2);

        for (int i = 0; i < verts.size(); i += 3)
        {

            vertices.put(verts.get(i).floatValue());
            vertices.put(verts.get(i + 1).floatValue());
            vertices.put(verts.get(i + 2).floatValue());

            vertices.put(normalArray[i]);
            vertices.put(normalArray[i + 1]);
            vertices.put(normalArray[i + 2]);
        }

        vertices.flip();

        indices = Buffers.newDirectIntBuffer(inds.size());

        for (int i = 0; i < inds.size(); i++)
        {
            indices.put(inds.get(i).intValue());
        }

        indices.flip();

        IntBuffer vertexBuffer = Buffers.newDirectIntBuffer(1),
                indexBuffer = Buffers.newDirectIntBuffer(1);

        bind(gl);

        gl.glGenBuffers(1, vertexBuffer);
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vertexBuffer.get());

        gl.glBufferData(GL.GL_ARRAY_BUFFER, verts.size() * 2 * Float.SIZE / 8,
                vertices, GL.GL_STATIC_DRAW);

        gl.glGenBuffers(1, indexBuffer);
        gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, indexBuffer.get());

        gl.glBufferData(GL.GL_ELEMENT_ARRAY_BUFFER, inds.size() * Integer.SIZE / 8,
                indices, GL.GL_STATIC_DRAW);
        
        gl.glEnableVertexAttribArray(Shader.POSITION_LOC);
        gl.glVertexAttribPointer(Shader.POSITION_LOC, 3, GL.GL_FLOAT, false,
                6 * Float.SIZE / 8, 0);

        gl.glEnableVertexAttribArray(Shader.NORMAL_LOC);
        gl.glVertexAttribPointer(Shader.NORMAL_LOC, 3, GL.GL_FLOAT, false,
                6 * Float.SIZE / 8, 3 * Float.SIZE / 8);
    }
}
