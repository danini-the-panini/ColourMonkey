#version 420

layout (binding = 0) uniform samplerCube skymap;

layout (binding=1) uniform sampler2D grassColour;
layout (binding=2) uniform sampler2D grassAlpha;
layout (binding=3) uniform sampler2D noiseMap;
layout (binding=8) uniform sampler2D shadowMap;

layout (location = 0) out vec4 colour;

uniform float fog_start;
uniform float fog_end;

uniform mat4 world;
uniform mat4 lprojection;
uniform mat4 lview;

uniform int shadowToggle;
uniform vec3 sun;

in float lightLevel;
in vec3 w_eye;
in vec3 g_position;
in vec3 g_normal;
in vec2 texCoord;

const mat4 bias = mat4 (.5f, .0f, .0f, .0f,
                        .0f, .5f, .0f, .0f,
                        .0f, .0f, .5f, .0f,
                        .5f, .5f, .5f, 1.f);

float shadowed(vec2 v, float dist)
{
    return texture(shadowMap, v).z < dist ? 1 : 0;
}

float ia = 0.2f;

float getIP(vec3 pos, vec3 norm)
{
    vec4 ssLightPos = bias * lprojection * lview * world * vec4(pos,1.0f);

    vec3 v = normalize(w_eye-pos);
    vec3 l = normalize(sun);
    vec3 r = normalize(reflect(-l,norm));

    float epsilon = 0.0002f;

    vec2[] offsets = vec2[](
        vec2(0,1),vec2(0,-1),vec2(1,0),vec2(-1,0),
        vec2(1,1),vec2(1,-1),vec2(-1,-1),vec2(-1,1)
    );

    float shadow = 1.0;
    float ip = ia;

    // only shadow if sun is above horizon
    if (sun.y > 0)
    {

        ip = lightLevel;

        shadow = shadowed(ssLightPos.xy, ssLightPos.z);

        for (int i = 0; i < offsets.length(); i++)
        {
            shadow += shadowed(ssLightPos.xy + epsilon*offsets[i], ssLightPos.z);
        }
        for (int i = 0; i < offsets.length(); i++)
        {
            shadow += shadowed(ssLightPos.xy + 2*epsilon*offsets[i], ssLightPos.z);
        }

        shadow /= offsets.length()*2+1;

        bool inShadow = shadowToggle == 1 && ssLightPos.x > 0 && ssLightPos.y > 0 && ssLightPos.x < 1 && ssLightPos.y < 1;

        if (inShadow)
            ip = mix (ip, ia, shadow);
    }

    return ip * ( 1.0f );
}

void main()
{

    vec3 dir = w_eye - g_position;

    float dist = length(dir);
    dir /= -dist; // = normalize(dir);
    float fog_factor = 1-clamp((dist-fog_start)/(fog_end-fog_start),0,1);

    vec3 sky = texture(skymap,dir).rgb;

    float alpha = texture(grassAlpha,texCoord).r;

    if (alpha < 0.5f)
        discard;

    colour = vec4(mix(sky, texture(grassColour,texCoord).rgb*getIP(g_position,g_normal), fog_factor), 1.0f);
}

