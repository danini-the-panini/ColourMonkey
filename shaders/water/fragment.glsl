#version 420

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

/// AND THE REST IS ALL MINE ///

layout (binding = 0) uniform samplerCube skymap;

layout (binding=6) uniform sampler2D reflection;

uniform vec3 sun;
uniform float time;

uniform int screenWidth;
uniform int screenHeight;

uniform float fog_start;
uniform float fog_end;

in vec3 g_normal;
in vec3 g_tangent;
in vec3 g_bitangent;
in vec3 g_colour;
in vec3 g_position;
in vec3 w_eye;

layout (location = 0) out vec4 colour;

vec3 samplesun(vec3 dir)
{
    vec3 l = normalize(sun);
    
    vec3 sun_colour;

    // different size radial gradients for each channel
    // makes the sun look "warm"
    sun_colour.r = pow(max(dot(-l,-dir),0),90.0f);
    sun_colour.g = pow(max(dot(-l,-dir),0),200.0f);
    sun_colour.b = pow(max(dot(-l,-dir),0),300.0f);

    return sun_colour;
}

float noise(vec3 v)
{
    float noiseval = (snoise(v)+1)/2;

    return noiseval;
}


void main()
{
    float x = gl_FragCoord.x/float(screenWidth);
    float y = gl_FragCoord.y/float(screenHeight);

    vec3 normal = g_normal;

    vec3 v = normalize(w_eye-g_position);

    vec3 old_ref = normalize(reflect(-v, normal));
    
    float epsilon = 0.0001f;

    float noise_scale = 0.005f;

    float nsample = snoise(vec3(g_position.xz,time));
    float f0 = (nsample+1)*noise_scale;
    float fx = (snoise(vec3(g_position.x+epsilon,g_position.z,time))+1)*noise_scale;
    float fy = (snoise(vec3(g_position.x,g_position.z+epsilon,time))+1)*noise_scale;
    float fz = (snoise(vec3(g_position.xz,time+epsilon))+1)*noise_scale;

    vec3 df = vec3((fx-f0)/epsilon, (fy-f0)/epsilon, (fz-f0)/epsilon);

    normal -= df;

    vec3 l = normalize(sun);
    vec3 r = normalize(reflect(-l,normal));

    vec3 ref = normalize(reflect(-v, normal));
    vec3 sun_colour = samplesun(ref).xyz;

    float yoffset = -noise_scale;
    float xoffset = -(nsample*2-1)*noise_scale*2;

    vec3 reflection = texture(reflection, vec2(x+xoffset, 1-(y+yoffset))).xyz;


    vec3 dir = w_eye - g_position;

    float dist = length(dir);
    dir /= -dist; // = normalize(dir);
    float fog_factor = 1-clamp((dist-fog_start)/(fog_end-fog_start),0,1);

    vec3 sky = texture(skymap, dir).rgb;

    vec3 water_tint = vec3(0.3046875f, 0.609375f, 0.55078125f);

    float tint_amount = 0.5f;
    float water_opacity = 0.5f;

    colour = vec4(mix(sky, mix(reflection,water_tint,tint_amount)+sun_colour, fog_factor),
        water_opacity+length(sun_colour));
}

