#version 420

uniform vec3 sun;
uniform float time;

in vec3 dir;

layout (location = 0) out vec4 colour;

const vec3 skytopa = vec3(0.0f, 0.25f, 0.5f);
const vec3 skymida = vec3(0.875f, 0.796875f, 0.66796875f);
const vec3 skybota = vec3(0.91796875f, 0.640625, 0.3828125f);

const vec3 skytopb = vec3(0.0f, 0.0f, 0.0f);
const vec3 skymidb = vec3(0.01f, 0.025f, 0.05);
const vec3 skybotb = vec3(0.01f, 0.025f, 0.05);

const vec3 skytopc = vec3(0.0f, 0.25f, 0.5f);
const vec3 skymidc = vec3(0.875f, 0.796875f, 0.66796875f);
const vec3 skybotc = vec3(0.91796875f, 0.640625, 0.3828125f);

const vec3 skytopd = vec3(0.08984375f, 0.27734375f, 0.41796875f);
const vec3 skymidd = vec3(0.40625f, 0.65234375f, 0.66796875f);
const vec3 skybotd = vec3(0.78125f, 0.87890625f, 0.83203125f);

vec4 samplesky(vec3 dir)
{
    vec3 l = normalize(sun);

    vec3 sun_colour;

    sun_colour.r = pow(max(dot(-l,-dir),0),90.0f);
    sun_colour.g = pow(max(dot(-l,-dir),0),200.0f);
    sun_colour.b = pow(max(dot(-l,-dir),0),300.0f);

    vec3 skytop;
    vec3 skymid;
    vec3 skybot;
    float grad;

    if (time < 0.1)
    {
        grad = time/0.1;
        skytop = mix(skytopa,skytopb,grad);
        skymid = mix(skymida,skymidb,grad);
        skybot = mix(skybota,skybotb,grad);
    }
    else if (time > 0.40 && time < 0.5)
    {
        grad = (time-0.40)/0.1;
        skytop = mix(skytopb,skytopc,grad);
        skymid = mix(skymidb,skymidc,grad);
        skybot = mix(skybotb,skybotc,grad);
        
    }
    else if (time > 0.5 && time < 0.75)
    {
        grad = (time-0.5)/0.25;
        skytop = mix(skytopc,skytopd,grad);
        skymid = mix(skymidc,skymidd,grad);
        skybot = mix(skybotc,skybotd,grad);
        
    }
    else if (time > 0.75)
    {
        grad = (time-0.75)/0.25;
        skytop = mix(skytopd,skytopa,grad);
        skymid = mix(skymidd,skymida,grad);
        skybot = mix(skybotd,skybota,grad);
        
    }
    else
    {
        skytop = skytopb;
        skymid = skymidb;
        skybot = skybotb;
    }

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

