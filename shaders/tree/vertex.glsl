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
uniform float noise;

uniform float time;

const int MAXB = 256;

layout (std430) uniform parent_block {
  int parents[MAXB];
};
layout (std430) uniform origin_block {
  vec3 origins[MAXB];
};
layout (std430) uniform axis_block {
  vec3 axes[MAXB];
};
layout (std430) uniform tangent_block {
  vec3 tangents[MAXB];
};
layout (std430) uniform world_block {
  mat4 worlds[MAXB];
};
layout (std430) uniform tree_world_block {
  mat4 tree_worlds[MAXB];
};

out vec3 g_normal;
out vec3 g_position;
out vec3 w_eye;
out vec3 g_colour;

out vec4 l_position;

const mat4 bias = mat4 (.5f, .0f, .0f, .0f,
                        .0f, .5f, .0f, .0f,
                        .0f, .0f, .5f, .0f,
                        .5f, .5f, .5f, 1.f);

const vec3 colours[] = {
  vec3(1,0,1), vec3(1,0,0), vec3(0,1,0),vec3(0,0,1),vec3(1,1,0),vec3(0,1,1),vec3(1,1,1),vec3(0.3f,0.3f,0.3f)
};

float rand(vec2 n)
{
  return 0.5 + 0.5 *
     fract(sin(dot(n.xy, vec2(12.9898, 78.233)))* 43758.5453);
}

vec4 quatAxisAngle(vec3 axis, float angle)
{
  angle *= 0.5;
  float s = sin(angle);
  float c = cos(angle);

  return vec4(s*normalize(axis), c);
}

mat4 toMat4(vec4 q)
{
  // Converts this quaternion to a rotation matrix.
  //
  // | 1 - 2(y^2 + z^2) 2(xy + wz)       2(xz - wy)       0 |
  // | 2(xy - wz)       1 - 2(x^2 + z^2) 2(yz + wx)       0 |
  // | 2(xz + wy)       2(yz - wx)       1 - 2(x^2 + y^2) 0 |
  // | 0                0                0                1 |

  float x2 =  q.x + q.x;
  float y2 =  q.y + q.y;
  float z2 =  q.z + q.z;
  float xx =  q.x * x2;
  float xy =  q.x * y2;
  float xz =  q.x * z2;
  float yy =  q.y * y2;
  float yz =  q.y * z2;
  float zz =  q.z * z2;
  float wx =  q.w * x2;
  float wy =  q.w * y2;
  float wz =  q.w * z2;

  return mat4(
          1 - (yy + zz), xy - wz,       xz + wy,       0,
          xy + wz,       1 - (xx + zz), yz - wx,       0,
          xz - wy,       yz + wx,       1 - (xx + yy), 0,
          0,             0,             0,             1
  );
}

vec4 slerp(vec4 from, vec4 to, float t)
{
  vec4 to1;
  float omega, cosom, sinom, scale0, scale1;
  // calc cosine
  cosom = from.x * to.x + from.y * to.y + from.z * to.z
          + from.w * to.w;
  // adjust signs (if necessary)
  if (cosom < 0.0)
  {
      cosom = -cosom;
      to1.x = -to.x;
      to1.y = -to.y;
      to1.z = -to.z;
      to1.w = -to.w;
  } else
  {
      to1.x = to.x;
      to1.y = to.y;
      to1.z = to.z;
      to1.w = to.w;
  }
  // calculate coefficients
  if ((1.0f - cosom) > 0.00001f)
  {
      // standard case (slerp)
      omega = acos(cosom);
      sinom = sin(omega);
      scale0 = sin((1.0f - t) * omega) / sinom;
      scale1 = sin(t * omega) / sinom;
  } else
  {
  // "from" and "to" quaternions are very close
      //  ... so we can do a linear interpolation
      scale0 = 1.0f - t;
      scale1 = t;
  }
  // calculate final values
  return vec4(
          scale0 * from.x + scale1 * to1.x,
          scale0 * from.y + scale1 * to1.y,
          scale0 * from.z + scale1 * to1.z,
          scale0 * from.w + scale1 * to1.w
  );
}

vec4 qmult(vec4 a, vec4 b)
{
  float y0 = a.w * b.w - a.x * b.x - a.y * b.y - a.z * b.z;
  float y1 = a.w * b.x + a.x * b.w + a.y * b.z - a.z * b.y;
  float y2 = a.w * b.y - a.x * b.z + a.y * b.w + a.z * b.x;
  float y3 = a.w * b.z + a.x * b.y - a.y * b.x + a.z * b.w;
  return vec4(y1, y2, y3, y0);
}


// bend branch function from GPU Gems 3, Chapter 6
mat4 bendBranch(vec3 pos,
                  vec3 branchOrigin,
                  float  branchNoise,
                  vec3 windDir,
                  vec3 windTangent,
                  float  windPower)
{
  vec3 branchPos = pos - branchOrigin;

  float towards = dot(normalize(vec3(branchPos.x,0,branchPos.z)), vec3(1,0,0));
  float facing  = dot(normalize(vec3(branchPos.x,0,branchPos.z)), windDir);

  float a = cos(time + branchNoise);
  float b = cos((time-1) + branchNoise);

  float oldA = a;
  a = -0.5f * a;
  b *= windPower;
  a = mix(oldA * windPower, a * windPower, clamp(1-facing,0.0f,1.0f));

  vec4 rot1 = quatAxisAngle(windTangent, a);
  vec4 rot2 = quatAxisAngle(vec3(0,1,0), b);

  return toMat4(slerp(rot1,rot2, 1-abs(facing)));
}

mat4 bendTree(int b)
{
  mat4 w = mat4(1.0f);
  int i = 0;

  while (b > 0)
  {
    w = worlds[b] * bendBranch((tree_worlds[b]*vec4(axes[b],1)).xyz,
      (tree_worlds[b]*vec4(origins[b],1)).xyz, noise,
      vec3(1.0f,0.0f,0.0f), vec3(0.0f,0.0f,1.0f), 0.1f) * w;

    b = parents[b];
  }

  return w;
}

void main()
{
    w_eye = (inverse(view) * vec4 (0, 0, 1, 1)).xyz;

    mat4 new_world = world * bendTree(branch);//worlds[parents[parents[branch]]] * worlds[parents[branch]] * worlds[branch];

    g_normal = normalize((new_world *  vec4(normal,0.0f)).xyz);
    g_position = (new_world * vec4(position,1.0f)).xyz;
    l_position = bias * lprojection * lview * new_world * vec4(position,1.0f);
    g_colour = vec3(0.3f,0.2f,0.0f);//colours[branch%colours.length()];
    gl_Position = projection * view * new_world * vec4(position,1.0f);
    gl_ClipDistance[0] = dot(clipWorld * new_world * vec4(position,1.0f), clipPlane);
}
