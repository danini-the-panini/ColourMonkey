/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.hackoeur.jglm;

import java.nio.FloatBuffer;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Daniel
 */
public class Mat4Test
{
    

    /**
     * Test of multiply method, of class Mat4.
     */
    @Test
    public void testMultiply_Mat4()
    {
        System.out.println("multiply");
        Mat4 a = new Mat4(
                16, -23,  1,  -1,
                7,    2, 24, 123,
                34,  49,  4, -34,
                5,    8,  2,   3
        );
        Mat4 b = new Mat4(
                234, 233,  3,  44,
                 64,   2, 23,   4,
                 34,  42, 56, 444,
               -123,  12, 48,  78  
        );
        Mat4 expResult = new Mat4(
                5697, -4417, 5926, 28455,
                1840, -309, 212, -588,
                4962, 5598, 2154, 4560,
                138, 5829, 513, 201
        );
        Mat4 result = a.multiply(b);
        assertEquals(expResult, result);
    }
}