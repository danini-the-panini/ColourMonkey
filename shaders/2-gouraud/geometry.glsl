#version 420
layout (triangles) in;
layout (triangle_strip, max_vertices=3) out;

uniform mat4 projection;
uniform mat4 world;
uniform mat4 view;

uniform vec3 sun;

in vec3 v_normal[];

out vec3 v_colour;

void main()
{
    float ia = 0.1f;
    float id = 0.6f;
    float is = 0.3f;
    float s = 50.0f;

    vec3 eye = (inverse(view) * vec4 (0, 0, 1, 1)).xyz;

    for (int i = 0; i < 3; i++)
    {
        vec3 position = gl_in[i].gl_Position.xyz;

        vec3 n = normalize(world * vec4(v_normal[i],0.0f)).xyz;
        vec3 p = (world * vec4(position,1.0f)).xyz;

        vec3 v = normalize(eye-p);
        vec3 l = normalize(sun);
        vec3 r = normalize(reflect(-l,n));

        float ip = ia + max(dot(l,n),0)*id + pow(max(dot(r,v),0),s)*is;

        v_colour = position * ip;

        gl_Position = projection * view * world * gl_in[i].gl_Position;
        EmitVertex();
    }
    EndPrimitive();
}

