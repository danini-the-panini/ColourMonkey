#version 420

layout (location=1) in vec3 position;
layout (location=2) in vec3 normal;

uniform mat4 projection;
uniform mat4 world;
uniform mat4 view;

out vec3 g_position;
out vec3 w_eye;

void main()
{
    w_eye = (inverse(view) * vec4 (0, 0, 1, 1)).xyz;

    g_position = (world * vec4(position,1.0f)).xyz;

    gl_Position = projection * view * world * vec4(position,1.0f);
}

