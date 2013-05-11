#version 420

layout (binding = 0) uniform samplerCube skymap;

layout (binding=1) uniform sampler2D grassColour;
layout (binding=2) uniform sampler2D grassAlpha;
layout (binding=3) uniform sampler2D noiseMap;

layout (location = 0) out vec4 colour;

uniform float fog_start;
uniform float fog_end;

uniform vec3 sun;

in vec3 w_eye;
in vec3 g_position;
in float lightLevel;
in vec2 texCoord;

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

    colour = vec4(mix(sky, texture(grassColour,texCoord).rgb*lightLevel, fog_factor), 1.0f);
}

