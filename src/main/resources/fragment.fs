#version 330

out vec4 FragColor;

in vec2 uvMap;

uniform int state;
uniform bool opposite;

uniform float height;
uniform float alpha;

uniform vec2 step1;
uniform vec2 step2;

uniform vec4 color1;
uniform vec4 color2;

uniform sampler2D textureSampler;

void main()
{
    if (state == 1)
    {
        FragColor = texture(textureSampler, uvMap) * alpha;
    }
    else if (state == 2)
    {
        float textureColor = texture(textureSampler, uvMap).x;

        if (color1 == color2)
        {
            FragColor = color1 * alpha * textureColor;
        }
        else
        {
            vec2 filteredStep1 = vec2(step1.x, height - step1.y);
            vec2 filteredStep2 = vec2(step2.x, height - step2.y);

            vec2 direction = filteredStep2 - filteredStep1;
            vec2 targetDirection = gl_FragCoord.xy - filteredStep1;

            float len = length(direction);
            float targetLength = dot(direction, targetDirection);

            float portion = min(1.0, max(0.0, targetLength/ (len * len)));

            FragColor = mix(color1, color2, smoothstep(0.0, 1.0, portion)) * alpha * textureColor;
        }
    }
    else if (state == 3)
    {
        FragColor = texture(textureSampler, uvMap);
    }
    else
    {
        if (color1 == color2)
        {
            FragColor = color1 * alpha;
        }
        else
        {
            vec2 filteredStep1 = vec2(step1.x, height - step1.y);
            vec2 filteredStep2 = vec2(step2.x, height - step2.y);

            vec2 direction = filteredStep2 - filteredStep1;
            vec2 targetDirection = gl_FragCoord.xy - filteredStep1;

            float len = length(direction);
            float targetLength = dot(direction, targetDirection);

            float portion = min(1.0, max(0.0, targetLength/ (len * len)));

            FragColor = mix(color1, color2, smoothstep(0.0, 1.0, portion)) * alpha;
        }
    }

    if (state != 3 && opposite)
    {
        FragColor = 1.0 * alpha - FragColor;
    }
}