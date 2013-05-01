#version 420
layout (binding = 7) uniform sampler2D sceneColour;

layout (location = 0) out vec4 colour;

uniform int screenWidth;
uniform int screenHeight;

uniform int ssaa;

vec2 ss2tex(vec2 scr)
{
    return new vec2(scr.x/float(screenWidth*ssaa),scr.y/float(screenHeight*ssaa));
}

void main()
{
    vec2 scrSSAA = gl_FragCoord.xy*float(ssaa);

    vec4 totalColour;
    float half = float(ssaa)*0.5f;
    float stepSize = 1.0f;
    float sq = ssaa*ssaa;

    vec2 offset;
    for (float i = -half; i < half; i += stepSize)
    for (float j = -half; j < half; j+= stepSize)
    {
        totalColour += texture(sceneColour, ss2tex(scrSSAA+vec2(i,j)))/sq;
    }
    

    colour = totalColour;
    //colour = texture(sceneColour, ss2tex(scrSSAA));
}

