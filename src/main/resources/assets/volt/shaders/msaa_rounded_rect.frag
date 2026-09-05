#version 330 core

in vec2 vTexCoord;
in vec2 vScreenPos;
in vec2 vResolution;

uniform vec4 uColor;
uniform vec2 uPosition;
uniform vec2 uSize;
uniform float uRadius;
uniform float uBorderWidth;
uniform vec4 uBorderColor;
uniform int uSamples;

out vec4 FragColor;

float roundedRectSDF(vec2 pos, vec2 size, float radius) {
    vec2 d = abs(pos) - size + radius;
    return length(max(d, 0.0)) + min(max(d.x, d.y), 0.0) - radius;
}

float sampleCoverage(vec2 pos, vec2 size, float radius, float borderWidth) {
    float pixelSize = 1.0 / max(vResolution.x, vResolution.y);
    float coverage = 0.0;
    int samples = max(1, uSamples);
    
    vec2 sampleOffsets[16] = vec2[](
        vec2(-0.375, -0.125), vec2(0.125, -0.375), vec2(-0.125, 0.375), vec2(0.375, 0.125),
        vec2(-0.25, -0.25), vec2(0.25, -0.25), vec2(-0.25, 0.25), vec2(0.25, 0.25),
        vec2(-0.4375, -0.0625), vec2(-0.1875, -0.4375), vec2(0.0625, -0.1875), vec2(0.4375, -0.3125),
        vec2(-0.3125, 0.1875), vec2(-0.0625, 0.4375), vec2(0.1875, 0.0625), vec2(0.3125, 0.3125)
    );
    
    for (int i = 0; i < samples; i++) {
        vec2 samplePos = pos + sampleOffsets[i] * pixelSize;
        float dist = roundedRectSDF(samplePos, size, radius);
        
        if (borderWidth > 0.0) {
            float outerDist = dist;
            float innerDist = roundedRectSDF(samplePos, size - vec2(borderWidth), max(0.0, radius - borderWidth));
            
            if (outerDist <= 0.0 && innerDist > 0.0) {
                coverage += 1.0;
            }
        } else {
            if (dist <= 0.0) {
                coverage += 1.0;
            }
        }
    }
    
    return coverage / float(samples);
}

float smoothCoverage(vec2 pos, vec2 size, float radius, float borderWidth) {
    float dist = roundedRectSDF(pos, size, radius);
    float pixelSize = 1.0 / max(vResolution.x, vResolution.y);
    
    if (borderWidth > 0.0) {
        float outerDist = dist;
        float innerDist = roundedRectSDF(pos, size - vec2(borderWidth), max(0.0, radius - borderWidth));
        
        float outerAlpha = 1.0 - smoothstep(-pixelSize, pixelSize, outerDist);
        float innerAlpha = 1.0 - smoothstep(-pixelSize, pixelSize, innerDist);
        
        return outerAlpha - innerAlpha;
    } else {
        return 1.0 - smoothstep(-pixelSize, pixelSize, dist);
    }
}

void main() {
    vec2 localPos = vTexCoord * uSize;
    
    vec2 centerPos = localPos - uSize * 0.5;
    float distance = roundedRectSDF(centerPos, uSize * 0.5, uRadius);
    
    float alpha = 1.0 - smoothstep(-1.0, 1.0, distance);
    
    vec4 finalColor = (uBorderWidth > 0.0) ? uBorderColor : uColor;
    
    FragColor = vec4(finalColor.rgb, finalColor.a * alpha);
    
    if (FragColor.a < 0.001) {
        discard;
    }
}