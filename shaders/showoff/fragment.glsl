#version 420
layout (binding = 7) uniform sampler2D showy;

layout (location = 0) out vec4 colour;

uniform int screenX;
uniform int screenY;
uniform int screenWidth;
uniform int screenHeight;

vec2 scr2tex(vec2 scr)
{
    return new vec2((scr.x-screenX)/float(screenWidth),(scr.y-screenY)/float(screenHeight));
}

void main()
{
    vec2 blah = scr2tex(gl_FragCoord.xy);

    colour = texture( showy, blah );
}

