#version 420
layout (triangles) in;
layout (triangle_strip, max_vertices=3) out;

uniform mat4 projection;
uniform mat4 world;
uniform mat4 view;

in vec3 v_normal[];

out vec3 g_normal;
out vec3 g_tangent;
out vec3 g_bitangent;
out vec3 g_position;
out vec3 g_colour;
out vec3 w_eye;

vec3 lerp(vec3 a, vec3 b, float w)
{
    return a*(1.0f-w) + b*w;
}

void main()
{

    w_eye = (inverse(view) * vec4 (0, 0, 1, 1)).xyz;

    for (int i = 0; i < 3; i++)
    {
        g_normal = normalize(world * vec4(v_normal[i],0.0f)).xyz;

        g_tangent = lerp(
            vec3(g_normal.z, 0.0f, -g_normal.x),
            vec3(g_normal.y, -g_normal.x, 0.0f),
            abs(g_normal.y));
        g_tangent = normalize(g_tangent);
        g_bitangent = cross(g_normal, g_tangent);

        g_colour = gl_in[i].gl_Position.xyz;
        g_position = (world * gl_in[i].gl_Position).xyz;

        gl_Position = projection * view * world * gl_in[i].gl_Position;
        EmitVertex();
    }
    EndPrimitive();
}

