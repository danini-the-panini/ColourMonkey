#version 420

in vec3 g_colour;

out vec4 colour;

void main()
{
    colour = vec4(g_colour, 1);
}

