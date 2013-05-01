#version 420

in vec3 v_colour;

layout (location = 0) out vec4 colour;

void main()
{
    colour = vec4(v_colour, 1.0f);
}

