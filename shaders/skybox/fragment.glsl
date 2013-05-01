#version 420
//layout (location=0) uniform samplerCube cubeMap;

uniform vec3 sun;

in vec3 dir;

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

void main()
{
    colour = samplesky(normalize(dir));
}

