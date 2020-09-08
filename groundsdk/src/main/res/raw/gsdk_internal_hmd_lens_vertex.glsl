#version 300 es

precision highp float;

// uniform location 0 reserved for sampler in fragment shader

// lens mesh sizing
layout (location = 1) uniform vec2 meshScale;
layout (location = 2) uniform vec2 texScale;
layout (location = 3) uniform vec3 chromaCorrection;

layout (location = 0) in vec2 position;
layout (location = 1) in vec2 texCoords;
layout (location = 2) in vec4 colorFilter;

out vec2 fragTexCoordsR;
out vec2 fragTexCoordsG;
out vec2 fragTexCoordsB;

out vec4 fragColorFilter;

const vec2 center = vec2(0.5);

void main() {
    vec2 scaledCoords = (vec2(texCoords.x, 1.0 - texCoords.y) - center) * texScale;
    fragTexCoordsR = scaledCoords * chromaCorrection.r + center;
    fragTexCoordsG = scaledCoords * chromaCorrection.g + center;
    fragTexCoordsB = scaledCoords * chromaCorrection.b + center;
    fragColorFilter = colorFilter;
    gl_Position = vec4(position * meshScale, 0.0, 1.0);
}
