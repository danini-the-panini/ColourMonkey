
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.HashMap;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author daniel
 */
public class LSystem implements Runnable
{
    HashMap<Runnable, Runnable[]> rules = new HashMap<Runnable, Runnable[]>();
    Runnable[] state;

    public LSystem(Runnable[] start)
    {
        state = start;
    }
    
    public void addRule(Runnable from, Runnable[] to)
    {
        rules.put(from, to);
    }

    @Override
    public String toString()
    {
        return Arrays.toString(state);
    }

    public Runnable[] getState()
    {
        return state;
    }
    
    public void iterate(int n)
    {
        while (n > 0)
        {
            int i = 0;
            while (i < state.length)
            {
                Runnable from = state[i];
                Runnable[] replacement = rules.get(from);
                if (replacement != null)
                {
                    Runnable[] newState = new Runnable[state.length+replacement.length-1];
                    System.arraycopy(state, 0, newState, 0, i);
                    System.arraycopy(replacement, 0, newState, i, replacement.length);
                    System.arraycopy(state, i+1, newState, i+replacement.length, state.length-i-1);
                    state = newState;
                    i += replacement.length;
                }
                else i++;
            }
            n--;
        }
    }

    @Override
    public void run()
    {
        for (Runnable r : state)
            r.run();
    }
    
    static class x implements Runnable
    {
        char z;

        public x(char z)
        {
            this.z = z;
        }

        @Override
        public void run()
        {
            System.out.print(z);
        }
        
        
    }
    
    private static final Runnable F = new x('F'), L = new x('L'),
            R = new x('R'), _s = new x('<'), s_ = new x('>');
    
    public static void main(String[] args)
    {
        LSystem lsys = new LSystem(new Runnable[]{F});
        lsys.addRule(F, new Runnable[]{F, _s, L, F, s_, F, _s, R, F, s_, F});
        lsys.addRule(L, new Runnable[]{L, F});
        lsys.addRule(R, new Runnable[]{F, R});
        lsys.run(); System.out.println();
        lsys.iterate(1);
        lsys.run(); System.out.println();
        lsys.iterate(1);
        lsys.run(); System.out.println();
    }
    
}
