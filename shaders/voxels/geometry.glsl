#version 420
layout (points) in;
layout (triangle_strip, max_vertices=36) out;

layout (binding=0) uniform isamplerBuffer lookup;

uniform mat4 projection;
uniform mat4 world;
uniform mat4 view;

uniform vec3 chunk_pos;

uniform int chunk_size;

out vec3 g_colour;

//
// Description : Array and textureless GLSL 2D/3D/4D simplex 
//               noise functions.
//      Author : Ian McEwan, Ashima Arts.
//  Maintainer : ijm
//     Lastmod : 20110822 (ijm)
//     License : Copyright (C) 2011 Ashima Arts. All rights reserved.
//               Distributed under the MIT License. See LICENSE file.
//               https://github.com/ashima/webgl-noise
// 

vec3 mod289(vec3 x) {
  return x - floor(x * (1.0 / 289.0)) * 289.0;
}

vec4 mod289(vec4 x) {
  return x - floor(x * (1.0 / 289.0)) * 289.0;
}

vec4 permute(vec4 x) {
     return mod289(((x*34.0)+1.0)*x);
}

vec4 taylorInvSqrt(vec4 r)
{
  return 1.79284291400159 - 0.85373472095314 * r;
}

float snoise(vec3 v)
{ 
    const vec2  C = vec2(1.0/6.0, 1.0/3.0) ;
    const vec4  D = vec4(0.0, 0.5, 1.0, 2.0);

  // First corner
    vec3 i  = floor(v + dot(v, C.yyy) );
    vec3 x0 =   v - i + dot(i, C.xxx) ;

  // Other corners
    vec3 g = step(x0.yzx, x0.xyz);
    vec3 l = 1.0 - g;
    vec3 i1 = min( g.xyz, l.zxy );
    vec3 i2 = max( g.xyz, l.zxy );

    //   x0 = x0 - 0.0 + 0.0 * C.xxx;
    //   x1 = x0 - i1  + 1.0 * C.xxx;
    //   x2 = x0 - i2  + 2.0 * C.xxx;
    //   x3 = x0 - 1.0 + 3.0 * C.xxx;
    vec3 x1 = x0 - i1 + C.xxx;
    vec3 x2 = x0 - i2 + C.yyy; // 2.0*C.x = 1/3 = C.y
    vec3 x3 = x0 - D.yyy;      // -1.0+3.0*C.x = -0.5 = -D.y

  // Permutations
    i = mod289(i); 
    vec4 p = permute( permute( permute( 
               i.z + vec4(0.0, i1.z, i2.z, 1.0 ))
             + i.y + vec4(0.0, i1.y, i2.y, 1.0 )) 
             + i.x + vec4(0.0, i1.x, i2.x, 1.0 ));

  // Gradients: 7x7 points over a square, mapped onto an octahedron.
  // The ring size 17*17 = 289 is close to a multiple of 49 (49*6 = 294)
    float n_ = 0.142857142857; // 1.0/7.0
    vec3  ns = n_ * D.wyz - D.xzx;

    vec4 j = p - 49.0 * floor(p * ns.z * ns.z);  //  mod(p,7*7)

    vec4 x_ = floor(j * ns.z);
    vec4 y_ = floor(j - 7.0 * x_ );    // mod(j,N)

    vec4 x = x_ *ns.x + ns.yyyy;
    vec4 y = y_ *ns.x + ns.yyyy;
    vec4 h = 1.0 - abs(x) - abs(y);

    vec4 b0 = vec4( x.xy, y.xy );
    vec4 b1 = vec4( x.zw, y.zw );

    //vec4 s0 = vec4(lessThan(b0,0.0))*2.0 - 1.0;
    //vec4 s1 = vec4(lessThan(b1,0.0))*2.0 - 1.0;
    vec4 s0 = floor(b0)*2.0 + 1.0;
    vec4 s1 = floor(b1)*2.0 + 1.0;
    vec4 sh = -step(h, vec4(0.0));

    vec4 a0 = b0.xzyw + s0.xzyw*sh.xxyy ;
    vec4 a1 = b1.xzyw + s1.xzyw*sh.zzww ;

    vec3 p0 = vec3(a0.xy,h.x);
    vec3 p1 = vec3(a0.zw,h.y);
    vec3 p2 = vec3(a1.xy,h.z);
    vec3 p3 = vec3(a1.zw,h.w);

  //Normalise gradients
    vec4 norm = taylorInvSqrt(vec4(dot(p0,p0), dot(p1,p1), dot(p2, p2), dot(p3,p3)));
    p0 *= norm.x;
    p1 *= norm.y;
    p2 *= norm.z;
    p3 *= norm.w;

  // Mix final noise value
    vec4 m = max(0.6 - vec4(dot(x0,x0), dot(x1,x1), dot(x2,x2), dot(x3,x3)), 0.0);
    m = m * m;
    return 42.0 * dot( m*m, vec4( dot(p0,x0), dot(p1,x1), 
                                  dot(p2,x2), dot(p3,x3) ) );
  }

/// END SIMPLEX NOISE FUNCTION ///

// density function for terrain
float density(vec3 v)
{
    float density = -v.y;
    
    density += 0.5f*snoise(v);

    return density;
}

uniform int[] e1 = int[12](0,1,2,3,4,5,6,7,0,1,2,3);
uniform int[] e2 = int[12](1,2,3,0,5,6,7,4,4,5,6,7);

uniform vec3[] rp = vec3[8](
    vec3(0,0,0),
    vec3(0,1,0),
    vec3(1,1,0),
    vec3(1,0,0),
    vec3(0,0,1),
    vec3(0,1,1),
    vec3(1,1,1),
    vec3(1,0,1)
);

uniform int[] cube_points = int[](
    0,1,2, 0,2,3,
    0,3,4, 3,7,4,
    0,5,1, 0,4,5,
    3,2,6, 3,6,7,
    1,5,2, 2,5,6,
    4,6,5, 4,7,6
);

uniform int case_to_numpolys[] = int[256](
    0,3,3,6,3,6,6,9,3,6,
    6,9,6,9,9,6,3,6,6,9,
    6,9,9,12,6,9,9,12,9,12,
    12,9,3,6,6,9,6,9,9,12,
    6,9,9,12,9,12,12,9,6,9,
    9,6,9,12,12,9,9,12,12,9,
    12,15,15,6,3,6,6,9,6,9,
    9,12,6,9,9,12,9,12,12,9,
    6,9,9,12,9,12,12,15,9,12,
    12,15,12,15,15,12,6,9,9,12,
    9,12,6,9,9,12,12,15,12,15,
    9,6,9,12,12,9,12,15,9,6,
    12,15,15,12,15,6,12,3,3,6,
    6,9,6,9,9,12,6,9,9,12,
    9,12,12,9,6,9,9,12,9,12,
    12,15,9,6,12,9,12,9,15,6,
    6,9,9,12,9,12,12,15,9,12,
    12,15,12,15,15,12,9,12,12,9,
    12,15,15,12,12,9,15,6,15,12,
    6,3,6,9,9,12,9,12,12,15,
    9,12,12,15,6,9,9,6,9,12,
    12,15,12,15,15,6,12,9,15,12,
    9,6,12,3,9,12,12,15,12,15,
    9,12,12,15,15,6,9,12,6,3,
    6,9,9,6,9,12,6,3,9,6,
    12,3,6,3,3,0
);

ivec3 tex_lookup(int i, int j)
{
    return texelFetch(lookup, i*5+j).rgb;
}

// find where the "zero point" is between a and b
float get_zero(float a, float b)
{
    return -a/(b-a);
}

void main()
{
    float d = 0.03125f; // TODO: why doesn't (1.0f/chunk_size) work?

    // position of each point of the cube in world space
    vec3 p[8];
    for (int i = 0; i < 8; i++)
    {
        p[i] = gl_in[0].gl_Position.xyz + chunk_pos + d*rp[i];
    }

    float v[8];
    
    // gather density values for the cube
    // and logically concatenate bits to create lookup case
    int c = 0;
    for (int i = 0; i < 8; i++)
    {
        v[i] = density(p[i]);
        if (v[i] > 0)
            c = c | (1 << i);
    }

    // now we build the triangles...

    //*
    ivec3 tri; // triangle indices
    for (int i = 0; i < 5; i++)
    {
        tri = tex_lookup(c, i);

        if (tri.x < 0)
            break;

        //g_colour = vec3(c/256.0f);
        if (tri.r < 0)
            g_colour = vec3(1,0,1);
        else
            g_colour = vec3(tri)/11.0f;

        // lerp between the two points
        // at the approx. "zero point" between them.
        gl_Position = projection * view * world
            * vec4(gl_in[0].gl_Position.xyz + chunk_pos + d*mix(
                rp[e1[tri.x]],rp[e2[tri.x]],
                get_zero(v[e1[tri.x]],v[e2[tri.x]])
                ),1);
        EmitVertex();

        gl_Position = projection * view * world
            * vec4(gl_in[0].gl_Position.xyz + chunk_pos + d*mix(
                rp[e1[tri.y]],rp[e2[tri.y]],
                get_zero(v[e1[tri.y]],v[e2[tri.y]])
                ),1);
        EmitVertex();

        gl_Position = projection * view * world
            * vec4(gl_in[0].gl_Position.xyz + chunk_pos + d*mix(
                rp[e1[tri.z]],rp[e2[tri.z]],
                get_zero(v[e1[tri.z]],v[e2[tri.z]])
                ),1);
        EmitVertex();
        EndPrimitive();
    }
    /* */

    // Debug triangles
    /*
    //g_colour = vec3(c/256.0f);
    if (c == 0 || c == 255) // ignore corner cases
    {
        return;
    }
    for (int i = 0; i < cube_points.length(); i+=3)
    {
            for (int j = 0; j < 3; j++)
            {
                g_colour = rp[cube_points[i+j]];

                gl_Position = projection * view * world
                    * vec4(gl_in[0].gl_Position.xyz + chunk_pos + d*rp[cube_points[i+j]],1);
                EmitVertex();
            }

            EndPrimitive();
    }
    /* */
}

