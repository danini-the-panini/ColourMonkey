
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
public class LSystem<T>
{
    HashMap<T, T[]> rules = new HashMap<T, T[]>();
    T[] state;

    public LSystem(T[] start)
    {
        state = start;
    }
    
    public void addRule(T from, T[] to)
    {
        rules.put(from, to);
    }

    @Override
    public String toString()
    {
        return Arrays.toString(state);
    }
    
    public void iterate(int n)
    {
        while (n > 0)
        {
            int i = 0;
            while (i < state.length)
            {
                T from = state[i];
                T[] replacement = rules.get(from);
                if (replacement != null)
                {
                    T[] newState = (T[])Array.newInstance(from.getClass(), state.length+replacement.length-1);
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
    
    public static void main(String[] args)
    {
        LSystem<Character> lsys = new LSystem<Character>(new Character[]{'F'});
        lsys.addRule('F', new Character[]{'<','L','F','>','<','R','F','>','F'});
        System.out.println(lsys);
        lsys.iterate(1);
        System.out.println(lsys);
        lsys.iterate(1);
        System.out.println(lsys);
    }
    
}
