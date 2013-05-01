#version 420

layout (location=1) in vec3 position;
layout (location=2) in vec3 normal;

out float gl_ClipDistance[1];

uniform mat4 projection;
uniform mat4 world;
uniform mat4 clipWorld;
uniform mat4 view;
uniform vec4 clipPlane;

out vec3 g_normal;
out vec3 g_position;
out vec3 w_eye;

void main()
{
    w_eye = (inverse(view) * vec4 (0, 0, 1, 1)).xyz;

    g_normal = normalize(world * vec4(normal,0.0f)).xyz;

    g_position = (world * vec4(position,1.0f)).xyz;

    gl_Position = projection * view * world * vec4(position,1.0f);
    gl_ClipDistance[0] = dot(clipWorld * vec4(position,1.0f), clipPlane);
}

