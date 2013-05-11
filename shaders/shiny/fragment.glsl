#version 420

uniform vec3 sun;

in vec3 g_normal;
in vec3 g_tangent;
in vec3 g_bitangent;
in vec3 g_colour;
in vec3 g_position;
in vec3 w_eye;

layout (location = 0) out vec4 colour;

void main()
{
    float ia = 0.4f;
    float id = 0.6f;
   // float is = 1.0f;
    float s = 100.0f;

    vec3 v = normalize(w_eye-g_position);
    vec3 l = normalize(sun);
    vec3 r = normalize(reflect(-l,g_normal));

    float ip = ia + max(dot(l,g_normal),0)*id; //+ pow(max(dot(r,v),0),s)*is;

    vec3 ref = normalize(reflect(-v, g_normal));

    //vec4 env = texture(cubeMap, ref);

    vec4 env = samplesky(ref);

    vec3 col = (env.xyz);

    colour = vec4(col, 1.0f);
}

