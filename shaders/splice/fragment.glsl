#version 420

uniform vec3 light;

in vec3 g_normal;
in vec3 g_colour;
in vec3 g_position;
in vec3 w_eye;

layout (location = 0) out vec4 colour;

void main()
{
    float ia = 0.3f;
    float id = 0.5f;
    float is = 0.1f;
    float s = 50.0f;

    vec3 v = normalize(w_eye-g_position);
    vec3 l = normalize(light-g_position);
    vec3 r = normalize(reflect(-l,g_normal));

    float ip = ia + max(dot(l,g_normal),0)*id + pow(max(dot(r,v),0),s)*is;

    vec3 col = vec3(pow(max(dot(v,g_normal),0.0f),0.7f)) * ip;

    colour = vec4(col, 1.0f);
}

