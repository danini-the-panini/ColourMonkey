import com.jogamp.opengl.util.Animator;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLProfile;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.awt.GLCanvas;
import javax.swing.JFrame;

import java.awt.BorderLayout;
import java.awt.event.*;

public class Main {
   
    
    public static void main( String [] args ) {
        final GLProfile glprofile = GLProfile.getDefault();
        final GLCapabilities glcapabilities = new GLCapabilities( glprofile );
        glcapabilities.setDoubleBuffered(true);
        final GLCanvas glcanvas = new GLCanvas( glcapabilities );
        
        final ColourMonkey app = new ColourMonkey();

        final JFrame jframe = new JFrame( "Colour Monkey!" ); 
        jframe.addWindowListener( new WindowAdapter() {
            @Override
            public void windowClosing( WindowEvent windowevent ) {
                jframe.dispose();
                System.exit( 0 );
            }
        });
        
        Animator animator = new Animator(glcanvas);
        
        glcanvas.addKeyListener(new KeyAdapter()
        {

            @Override
            public void keyPressed(KeyEvent e) {
                app.keyPressed(e);
            }

            @Override
            public void keyReleased(KeyEvent e)
            {
                app.keyReleased(e);
            }
            
        });
        
        MouseAdapter mouse = new MouseAdapter()
        {
            int cx = glcanvas.getWidth()/2;
            int cy = glcanvas.getHeight()/2;

            @Override
            public void mousePressed(MouseEvent e) {

                cx = e.getX();
                cy = e.getY();
            }

            @Override
            public void mouseDragged(MouseEvent e) {

                if (e.getX() == cx && e.getY() == cy) return;

                app.mouseMoved(cx-e.getX(), cy-e.getY());

                cx = e.getX();
                cy = e.getY();
            }
        };
        
        glcanvas.addMouseListener(mouse);
        glcanvas.addMouseMotionListener(mouse);

        glcanvas.addGLEventListener( new GLEventListener() {
            
            @Override
            public void reshape( GLAutoDrawable glautodrawable, int x, int y, int width, int height ) {
                app.reshape( glautodrawable.getGL().getGL4(), width, height );
            }
            
            @Override
            public void init( GLAutoDrawable glautodrawable ) {
                app.init( glautodrawable.getGL().getGL4() );
            }
            
            @Override
            public void dispose( GLAutoDrawable glautodrawable ) {
                app.cleanUp( glautodrawable.getGL().getGL4() );
            }
            
            @Override
            public void display( GLAutoDrawable glautodrawable ) {
                app.display( glautodrawable.getGL().getGL4());
            }
        });

        jframe.getContentPane().add( glcanvas, BorderLayout.CENTER );
        jframe.setSize( 1280, 768 );
        jframe.setVisible( true );
        
        animator.start();
    }
    
}
