#version 420

layout (binding = 0) uniform samplerCube skymap;

in vec3 dir;

layout (location = 0) out vec4 colour;

void main()
{
    colour = texture(skymap, normalize(dir));
}

