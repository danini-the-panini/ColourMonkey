
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
public interface Drawable
{
    public void draw(GL4 gl, Shader shader);
}
