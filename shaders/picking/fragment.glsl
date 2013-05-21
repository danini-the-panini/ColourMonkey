#version 420

in vec3 g_colour;

layout (location = 0) out vec4 colour;

void main()
{
    colour = vec4(g_colour,1.0f);
    //colour = vec4(1.0, 0.0, 1.0, 1.0);
}

