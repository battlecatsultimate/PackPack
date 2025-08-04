#version 330

out vec4 FragColor;

in vec2 uvMap;
in vec2 vertexPos;
flat in vec2 startPos;

uniform int state;
uniform bool opposite;
uniform bool addMode;

uniform vec2 screenSize;

uniform bool dashMode;
uniform bool fillMode;

uniform uint pattern;
uniform float factor;

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
        vec4 color = texture(textureSampler, uvMap);

        if (addMode)
        {
            color.a = color.a * (color.r + color.g + color.b) / 3.0;
        }

        FragColor = color * alpha;
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
            vec2 filteredStep1 = vec2(step1.x, screenSize.y - step1.y);
            vec2 filteredStep2 = vec2(step2.x, screenSize.y - step2.y);

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
        if (dashMode && !fillMode)
        {
            vec2 dir = (vertexPos.xy - startPos.xy) * screenSize / 2.0;
            float dist = length(dir);

            uint bit = uint(round(dist / factor)) & 15U;

            if ((pattern & (1U << bit)) == 0U)
            {
                discard;
            }
        }

        if (color1 == color2)
        {
            FragColor = color1 * alpha;
        }
        else
        {
            vec2 filteredStep1 = vec2(step1.x, screenSize.y - step1.y);
            vec2 filteredStep2 = vec2(step2.x, screenSize.y - step2.y);

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