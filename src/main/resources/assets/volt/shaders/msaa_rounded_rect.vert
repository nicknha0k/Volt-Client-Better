#version 330 core

layout (location = 0) in vec2 aPos;
layout (location = 1) in vec2 aTexCoord;

uniform mat4 uProjection;
uniform mat4 uModelView;
uniform vec2 uResolution;
uniform vec2 uPosition;
uniform vec2 uSize;

out vec2 vTexCoord;
out vec2 vScreenPos;
out vec2 vResolution;

void main() {
    vec2 scaledPos = aPos * uSize * 0.5;
    vec2 worldPos = scaledPos + uPosition + uSize * 0.5;
    
    gl_Position = uProjection * uModelView * vec4(worldPos, 0.0, 1.0);
    
    vTexCoord = aTexCoord;
    
    vScreenPos = worldPos;
    
    vResolution = uResolution;
}