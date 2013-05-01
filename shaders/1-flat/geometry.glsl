#version 420
layout (triangles) in;
layout (triangle_strip, max_vertices=3) out;

uniform mat4 projection;
uniform mat4 world;
uniform mat4 view;

uniform vec3 sun;

out vec3 v_colour;

void main()
{
    float ia = 0.1f;
    float id = 0.6f;
    float is = 0.3f;
    float s = 100.0f;

    vec3 eye = (inverse(view) * vec4 (0, 0, 1, 1)).xyz;

    vec3 normal = normalize(cross(gl_in[1].gl_Position.xyz - gl_in[0].gl_Position.xyz,
        gl_in[2].gl_Position.xyz - gl_in[0].gl_Position.xyz));
    vec3 position = (gl_in[0].gl_Position.xyz + gl_in[1].gl_Position.xyz + gl_in[2].gl_Position.xyz) * (1.0f/3.0f);

    vec3 n = normalize(world * vec4(normal,0.0f)).xyz;
    vec3 p = (world * vec4(position,1.0f)).xyz;

    vec3 v = normalize(eye-p);
    vec3 l = normalize(sun);
    vec3 r = normalize(reflect(-l,n));

    float ip = ia + max(dot(l,n),0)*id + pow(max(dot(r,v),0),s)*is;

    v_colour = position * ip;

    for (int i = 0; i < 3; i++)
    {
        gl_Position = projection * view * world * gl_in[i].gl_Position;
        EmitVertex();
    }
    EndPrimitive();
}

