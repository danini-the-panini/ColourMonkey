#version 420

uniform mat4 projection;
uniform mat4 world;
uniform mat4 view;

layout (location=1) in vec3 position;
layout (location=2) in vec3 normal;

out vec3 v_colour;
out vec3 v_normal;
out vec3 v_ndc;
out vec3 v_ndc_norm;
out vec3 w_eye;

void main()
{
   vec3 w_eye = (inverse(view) * vec4 (0, 0, 0, 1)).xyz;
   v_normal = normal;
   vec4 v_position = projection * view * world * vec4(position, 1.0f);
   v_ndc_norm = normalize((projection * view * world * vec4(v_normal, 0.0f)).xyz);
   v_colour = position;
   v_ndc = v_position.xyz;
   gl_Position = v_position;
}

