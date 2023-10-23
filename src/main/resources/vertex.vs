#version 330

layout (location = 0) in vec2 position;
layout (location = 1) in vec2 uv;

out vec2 uvMap;
out vec2 vertexPos;
flat out vec2 startPos;

uniform int state;

uniform mat4 projection;
uniform mat4 matrix;

void main()
{
    if (state == 0 || state == 1 || state == 2)
    {
        vec4 pos = projection * matrix * vec4(position, 0.0, 1.0);
        gl_Position = pos;
        vertexPos = pos.xy / pos.w;
        startPos = vertexPos;
    }
    else
    {
        gl_Position = vec4(position, 0.0, 1.0);
    }

    if (state == 1 || state == 2 || state == 3)
    {
        uvMap = uv;
    }
}