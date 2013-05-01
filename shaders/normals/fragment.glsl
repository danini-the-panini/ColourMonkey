#version 420
layout (location=0) uniform samplerCube cubeMap;

in vec3 v_colour;
in vec3 v_ndc;
in vec3 v_ndc_norm;
in vec3 v_normal;
in vec3 w_eye;

layout (location = 0) out vec4 colour;

void main()
{
    float shade = 1.0f-(v_ndc.z+1.0f)/2.0f;
    vec3 col = v_normal;
    colour = vec4(col, 1.0f);
}

