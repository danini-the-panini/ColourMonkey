#version 420

layout (binding=8) uniform sampler2D shadowMap;

uniform vec3 sun;

in vec3 g_normal;
in vec3 g_colour;
in vec3 g_position;
in vec3 w_eye;

uniform int shadowToggle;

layout (location = 0) out vec4 colour;

in vec4 l_position;

float shadowed(vec2 v, float dist)
{
    return texture(shadowMap, v).z < dist ? 1 : 0;
}

void main()
{
    // perspective division for the light position
    vec4 ssLightPos = l_position / l_position.w;

    float ia = 0.2f;
    float id = 0.7f;
    float is = 0.1f;
    float s = 10.0f;

    vec3 v = normalize(w_eye-g_position);
    vec3 l = normalize(sun);
    vec3 r = normalize(reflect(-l,g_normal));

    float epsilon = 0.0002;

    vec2[] offsets = vec2[](
        vec2(0,1),vec2(0,-1),vec2(1,0),vec2(-1,0),
        vec2(1,1),vec2(1,-1),vec2(-1,-1),vec2(-1,1)
    );

    float shadow = 1.0;
    float ip = ia;

    ip = ia + max(dot(l,g_normal),0)*id + pow(max(dot(r,v),0),s)*is;

    // only shadow if sun is above horizon
    if (sun.y > 0)
    {

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

    vec3 col = g_colour * ip;

    colour = vec4(col, 1.0f);
}

