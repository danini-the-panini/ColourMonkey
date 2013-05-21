#version 420

layout (location=1) in vec3 position;


uniform mat4 projection;
uniform mat4 world;
uniform mat4 view;

uniform int id;

out vec3 g_colour;

void main()
{
    g_colour = vec3(float((id >> 0) & 1), float((id >> 1) & 1), float((id >> 2) & 1));

    gl_Position = projection * view * world * vec4(position,1.0f);
}

