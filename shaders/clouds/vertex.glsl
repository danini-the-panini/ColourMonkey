#version 420

layout (location=1) in vec3 position;
layout (location=2) in vec3 normal;

uniform mat4 projection;
uniform mat4 world;
uniform mat4 view;

uniform float radius;
uniform float cloud_height;

out vec3 w_position;
out vec3 w_eye;

void main()
{

    w_eye = (inverse(view) * vec4 (0, 0, 1, 1)).xyz;

    vec3 new_position = position;
    new_position.y = cloud_height;

    w_position = (world * vec4(new_position,1.0f)).xyz;

    gl_Position = projection * view * world * vec4(new_position,1.0f);
}

