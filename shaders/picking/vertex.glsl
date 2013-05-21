#version 420

layout (location=1) in vec3 position;


uniform mat4 projection;
uniform mat4 world;
uniform mat4 view;

uniform int id;

out vec4 g_colour;

void main()
{
    g_colour = vec4(float(id & 1), float((id >> 1) & 1), float((id >> 2) & 1), (id >> 3) & 1);

    gl_Position = projection * view * world * vec4(position,1.0f);
}

