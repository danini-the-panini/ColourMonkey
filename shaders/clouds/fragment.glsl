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

vec4 mod289(vec4 x) {
  return x - floor(x * (1.0 / 289.0)) * 289.0; }

float mod289(float x) {
  return x - floor(x * (1.0 / 289.0)) * 289.0; }

vec4 permute(vec4 x) {
     return mod289(((x*34.0)+1.0)*x);
}

float permute(float x) {
     return mod289(((x*34.0)+1.0)*x);
}

vec4 taylorInvSqrt(vec4 r)
{
  return 1.79284291400159 - 0.85373472095314 * r;
}

float taylorInvSqrt(float r)
{
  return 1.79284291400159 - 0.85373472095314 * r;
}

vec4 grad4(float j, vec4 ip)
  {
  const vec4 ones = vec4(1.0, 1.0, 1.0, -1.0);
  vec4 p,s;

  p.xyz = floor( fract (vec3(j) * ip.xyz) * 7.0) * ip.z - 1.0;
  p.w = 1.5 - dot(abs(p.xyz), ones.xyz);
  s = vec4(lessThan(p, vec4(0.0)));
  p.xyz = p.xyz + (s.xyz*2.0 - 1.0) * s.www; 

  return p;
  }

// (sqrt(5) - 1)/4 = F4, used once below
#define F4 0.309016994374947451

float snoise(vec4 v)
  {
  const vec4  C = vec4( 0.138196601125011,  // (5 - sqrt(5))/20  G4
                        0.276393202250021,  // 2 * G4
                        0.414589803375032,  // 3 * G4
                       -0.447213595499958); // -1 + 4 * G4

// First corner
  vec4 i  = floor(v + dot(v, vec4(F4)) );
  vec4 x0 = v -   i + dot(i, C.xxxx);

// Other corners

// Rank sorting originally contributed by Bill Licea-Kane, AMD (formerly ATI)
  vec4 i0;
  vec3 isX = step( x0.yzw, x0.xxx );
  vec3 isYZ = step( x0.zww, x0.yyz );
//  i0.x = dot( isX, vec3( 1.0 ) );
  i0.x = isX.x + isX.y + isX.z;
  i0.yzw = 1.0 - isX;
//  i0.y += dot( isYZ.xy, vec2( 1.0 ) );
  i0.y += isYZ.x + isYZ.y;
  i0.zw += 1.0 - isYZ.xy;
  i0.z += isYZ.z;
  i0.w += 1.0 - isYZ.z;

  // i0 now contains the unique values 0,1,2,3 in each channel
  vec4 i3 = clamp( i0, 0.0, 1.0 );
  vec4 i2 = clamp( i0-1.0, 0.0, 1.0 );
  vec4 i1 = clamp( i0-2.0, 0.0, 1.0 );

  //  x0 = x0 - 0.0 + 0.0 * C.xxxx
  //  x1 = x0 - i1  + 1.0 * C.xxxx
  //  x2 = x0 - i2  + 2.0 * C.xxxx
  //  x3 = x0 - i3  + 3.0 * C.xxxx
  //  x4 = x0 - 1.0 + 4.0 * C.xxxx
  vec4 x1 = x0 - i1 + C.xxxx;
  vec4 x2 = x0 - i2 + C.yyyy;
  vec4 x3 = x0 - i3 + C.zzzz;
  vec4 x4 = x0 + C.wwww;

// Permutations
  i = mod289(i); 
  float j0 = permute( permute( permute( permute(i.w) + i.z) + i.y) + i.x);
  vec4 j1 = permute( permute( permute( permute (
             i.w + vec4(i1.w, i2.w, i3.w, 1.0 ))
           + i.z + vec4(i1.z, i2.z, i3.z, 1.0 ))
           + i.y + vec4(i1.y, i2.y, i3.y, 1.0 ))
           + i.x + vec4(i1.x, i2.x, i3.x, 1.0 ));

// Gradients: 7x7x6 points over a cube, mapped onto a 4-cross polytope
// 7*7*6 = 294, which is close to the ring size 17*17 = 289.
  vec4 ip = vec4(1.0/294.0, 1.0/49.0, 1.0/7.0, 0.0) ;

  vec4 p0 = grad4(j0,   ip);
  vec4 p1 = grad4(j1.x, ip);
  vec4 p2 = grad4(j1.y, ip);
  vec4 p3 = grad4(j1.z, ip);
  vec4 p4 = grad4(j1.w, ip);

// Normalise gradients
  vec4 norm = taylorInvSqrt(vec4(dot(p0,p0), dot(p1,p1), dot(p2, p2), dot(p3,p3)));
  p0 *= norm.x;
  p1 *= norm.y;
  p2 *= norm.z;
  p3 *= norm.w;
  p4 *= taylorInvSqrt(dot(p4,p4));

// Mix contributions from the five corners
  vec3 m0 = max(0.6 - vec3(dot(x0,x0), dot(x1,x1), dot(x2,x2)), 0.0);
  vec2 m1 = max(0.6 - vec2(dot(x3,x3), dot(x4,x4)            ), 0.0);
  m0 = m0 * m0;
  m1 = m1 * m1;
  return 49.0 * ( dot(m0*m0, vec3( dot( p0, x0 ), dot( p1, x1 ), dot( p2, x2 )))
               + dot(m1*m1, vec2( dot( p3, x3 ), dot( p4, x4 ) ) ) ) ;

  }

// END OF SOMEONE'S NOISE FUNCTION I GIT FROM THE INTERTUBES

uniform vec3 sun;
uniform float time;
uniform float fog_start;
uniform float fog_end;
uniform float speed;
uniform float density;

in vec3 w_position;
in vec3 w_eye;

layout (location = 0) out vec4 colour;

const vec3 skytop = vec3(0.08984375f, 0.27734375f, 0.41796875f);
const vec3 skymid = vec3(0.40625f, 0.65234375f, 0.66796875f);
const vec3 skybot = vec3(0.78125f, 0.87890625f, 0.83203125f);

vec4 samplesky(vec3 dir)
{
    vec3 l = normalize(sun);

    vec3 sun_colour;

    sun_colour.r = pow(max(dot(-l,-dir),0),90.0f);
    sun_colour.g = pow(max(dot(-l,-dir),0),200.0f);
    sun_colour.b = pow(max(dot(-l,-dir),0),300.0f);

    vec3 sky;
    if (dir.y > 0)
    {
        sky = mix(skymid, skytop, dir.y);
    }
    else
    {
        sky = mix(skymid, skybot, -dir.y);
    }

    return vec4(clamp(sky+sun_colour,0,1),1.0f);
}

float mnoise(vec3 v, float t)
{

    v *= 0.01;

    float iterations = 5.0;
    float nstep = 2.0;

    float noise = 0.0;
    float sscale = 1.0;

    for (float i = 0.0; i < iterations; i+=1.0)
    {
        noise += clamp(snoise(vec4(v*sscale,t)),0,1)/sscale;
        sscale *= nstep;
    }
	
    noise /= iterations;

    return noise;
}

const float FLUFFINESS = 0.05; // TODO: make this a uniform
const float BLURRINESS = 0.02; // TODO: make this a uniform


vec4 gradient(float v, float glow)
{
    float invglow = 1-glow;

    if (v < density-BLURRINESS)
    {
        return mix(vec4(vec3(0.0),1.0),vec4(vec3(0.7+0.3*invglow),1.0), v/(density-BLURRINESS));
    }
    else if (v < density)
    {
        return mix(vec4(vec3(0.7+0.3*invglow),1.0), vec4(1.0,1.0,1.0,0.7), 
                        clamp((v-density+BLURRINESS)/BLURRINESS,0.0,1.0));
    }
    else if (v < density+FLUFFINESS)
    {
        return mix(vec4(1.0,1.0,1.0,0.7),vec4(1.0,1.0,1.0,0.0),
                        clamp((v-density)/FLUFFINESS,0.0,1.0));
    }
    return vec4(1.0,1.0,1.0,0.0);
}

void main()
{
    vec3 dir = w_eye - w_position;
    float dist = length(dir);
    dir /= -dist;

    vec3 lookup_pos = w_position;
    lookup_pos.x += time*speed*0.01;

    float lookup_time = time*0.01f;

    float noise = mnoise(lookup_pos, lookup_time);

    float fog_factor = 1-clamp((dist-fog_start)/(fog_end-fog_start),0,1);

    vec4 sky = samplesky(dir);
    vec3 l = normalize(sun);
    float glow = pow(max(dot(l,dir),0),10);

    vec4 grad = gradient(1.0-noise, glow);

    colour = vec4(mix (sky.xyz, vec3(grad.rgb), fog_factor), grad.a);
    //colour = vec4(vec3(noise+foo,0,noise+foo),fog_factor*0.5f);
}

