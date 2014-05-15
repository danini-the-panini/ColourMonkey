#version 420

layout (location=1) in vec3 position;
layout (location=2) in vec3 normal;

uniform mat4 projection;
uniform mat4 world;
uniform mat4 view;

uniform mat4 lprojection;
uniform mat4 lview;

uniform mat4 clipWorld;
uniform vec4 clipPlane;

uniform int branch;
uniform mat4 tree_world;

out vec3 g_normal;
out vec3 g_position;
out vec3 w_eye;

out vec4 l_position;

const mat4 bias = mat4 (.5f, .0f, .0f, .0f,
                        .0f, .5f, .0f, .0f,
                        .0f, .0f, .5f, .0f,
                        .5f, .5f, .5f, 1.f);

void main()
{
    w_eye = (inverse(view) * vec4 (0, 0, 1, 1)).xyz;

    g_normal = normalize((world * tree_world * vec4(normal,0.0f)).xyz);

    g_position = (world * vec4(position,1.0f)).xyz;
    l_position = bias * lprojection * lview * world * tree_world * vec4(position,1.0f);
    gl_Position = projection * view * world * tree_world * vec4(position,1.0f);
    gl_ClipDistance[0] = dot(clipWorld * world * tree_world * vec4(position,1.0f), clipPlane);
}