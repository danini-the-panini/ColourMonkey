
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
public class LSystem
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
                i++;
            }
            n--;
        }
    }
    
}
