#version 420

in vec4 g_colour;

layout (location = 0) out vec4 colour;

void main()
{
    //colour = g_colour;
    colour = vec4(1.0, 0.0, 1.0, 1.0);
}

