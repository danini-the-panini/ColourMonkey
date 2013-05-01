#version 420
layout (binding=0) uniform sampler2D tex1;
layout (binding=1) uniform sampler2D tex2;
layout (binding=2) uniform sampler2D tex3;
layout (binding=3) uniform sampler2D norm1;
layout (binding=4) uniform sampler2D norm2;
layout (binding=5) uniform sampler2D norm3;

//
// Description : Array and textureless GLSL 2D simplex noise function.
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

vec2 mod289(vec2 x) {
  return x - floor(x * (1.0 / 289.0)) * 289.0;
}

vec3 permute(vec3 x) {
  return mod289(((x*34.0)+1.0)*x);
}

float snoise(vec2 v)
  {
  const vec4 C = vec4(0.211324865405187,  // (3.0-sqrt(3.0))/6.0
                      0.366025403784439,  // 0.5*(sqrt(3.0)-1.0)
                     -0.577350269189626,  // -1.0 + 2.0 * C.x
                      0.024390243902439); // 1.0 / 41.0
// First corner
  vec2 i  = floor(v + dot(v, C.yy) );
  vec2 x0 = v -   i + dot(i, C.xx);

// Other corners
  vec2 i1;
  //i1.x = step( x0.y, x0.x ); // x0.x > x0.y ? 1.0 : 0.0
  //i1.y = 1.0 - i1.x;
  i1 = (x0.x > x0.y) ? vec2(1.0, 0.0) : vec2(0.0, 1.0);
  // x0 = x0 - 0.0 + 0.0 * C.xx ;
  // x1 = x0 - i1 + 1.0 * C.xx ;
  // x2 = x0 - 1.0 + 2.0 * C.xx ;
  vec4 x12 = x0.xyxy + C.xxzz;
  x12.xy -= i1;

// Permutations
  i = mod289(i); // Avoid truncation effects in permutation
  vec3 p = permute( permute( i.y + vec3(0.0, i1.y, 1.0 ))
		+ i.x + vec3(0.0, i1.x, 1.0 ));

  vec3 m = max(0.5 - vec3(dot(x0,x0), dot(x12.xy,x12.xy), dot(x12.zw,x12.zw)), 0.0);
  m = m*m ;
  m = m*m ;

// Gradients: 41 points uniformly over a line, mapped onto a diamond.
// The ring size 17*17 = 289 is close to a multiple of 41 (41*7 = 287)

  vec3 x = 2.0 * fract(p * C.www) - 1.0;
  vec3 h = abs(x) - 0.5;
  vec3 ox = floor(x + 0.5);
  vec3 a0 = x - ox;

// Normalise gradients implicitly by scaling m
// Approximation of: m *= inversesqrt( a0*a0 + h*h );
  m *= 1.79284291400159 - 0.85373472095314 * ( a0*a0 + h*h );

// Compute final noise value at P
  vec3 g;
  g.x  = a0.x  * x0.x  + h.x  * x0.y;
  g.yz = a0.yz * x12.xz + h.yz * x12.yw;
  return 130.0 * dot(m, g);
}

// END OF SOMEONE'S NOISE FUNCTION I GIT FROM THE INTERTUBES

uniform vec3 sun;

in vec3 g_normal;
in vec3 g_position;
in vec3 w_eye;

layout (location = 0) out vec4 colour;

void main()
{


    // Determine the blend weights for the 3 planar projections.  
    // N_orig is the vertex-interpolated normal vector.  
    vec3 blend_weights = abs( g_normal.xyz );   // Tighten up the blending zone:  
    blend_weights = (blend_weights - 0.2f) * 7.0f;  
    blend_weights = max(blend_weights, 0.0f);      // Force weights to sum to 1.0 (very important!)  
    blend_weights /= (blend_weights.x + blend_weights.y + blend_weights.z ).xxx;   
    // Now determine a color value and bump vector for each of the 3  
    // projections, blend them, and store blended results in these two  
    // vectors:  
    vec4 blended_color; // .w hold spec value  
    vec3 blended_bump_vec; 
    float tex_scale = 0.6f;
    float tex_scale2 = 0.2f;
    {  
        // Compute the UV coords for each of the 3 planar projections.  
        // tex_scale (default ~ 1.0) determines how big the textures appear.  
        vec2 coord1 = g_position.yz * tex_scale2;  
        vec2 coord2 = g_position.zx * tex_scale;  
        vec2 coord3 = g_position.xy * tex_scale2;  
        // This is where you would apply conditional displacement mapping.  
        //if (blend_weights.x > 0) coord1 = . . .  
        //if (blend_weights.y > 0) coord2 = . . .  
        //if (blend_weights.z > 0) coord3 = . . .  
        // Sample color maps for each projection, at those UV coords.  
        vec4 col1 = texture(tex1, coord1);
        vec4 col2 = texture(tex2, coord2);  
        vec4 col3 = texture(tex3, coord3);  
        // Sample bump maps too, and generate bump vectors.  
        // (Note: this uses an oversimplified tangent basis.)  
        vec2 bumpFetch1 = texture(norm1,coord1).xy - 0.5;  
        vec2 bumpFetch2 = texture(norm2,coord2).xy - 0.5;  
         vec2 bumpFetch3 = texture(norm3,coord3).xy - 0.5;  
         vec3 bump1 = vec3(0, bumpFetch1.x, bumpFetch1.y);  
         vec3 bump2 = vec3(bumpFetch2.y, 0, bumpFetch2.x);  
         vec3 bump3 = vec3(bumpFetch3.x, bumpFetch3.y, 0);

         // Finally, blend the results of the 3 planar projections.  
        blended_color = col1.xyzw * blend_weights.xxxx +  
                        col2.xyzw * blend_weights.yyyy +  
                        col3.xyzw * blend_weights.zzzz;  

        blended_bump_vec = bump1.xyz * blend_weights.xxx +  
                           bump2.xyz * blend_weights.yyy +  
                           bump3.xyz * blend_weights.zzz;
    }  
    // Apply bump vector to vertex-interpolated normal vector.  
    vec3 N_for_lighting =  normalize(g_normal + blended_bump_vec);  

    float ia = 0.1f;
    float id = 0.6f;
    float is = 0.3f;
    float s = 50.0f;

    vec3 v = normalize(w_eye-g_position);
    vec3 l = normalize(sun);
    vec3 r = normalize(reflect(-l,N_for_lighting));

    float dist = length(w_eye - g_position);
    float fog_factor = 1-clamp((dist-150)/100,0,1);

    float ip = ia + max(dot(l,N_for_lighting),0)*id + pow(max(dot(r,v),0),s)*is;

    colour = vec4(blended_color.xyz * ip, fog_factor);
}

