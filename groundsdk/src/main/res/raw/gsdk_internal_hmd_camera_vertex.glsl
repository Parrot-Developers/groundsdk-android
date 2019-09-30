#version 300 es

precision highp float;

// uniform location 0 reserved for sampler in fragment shader

// camera texture device-specific transform
layout (location = 1) uniform mat4 camTransform;

// camera quad scaling (used to maintain proper source aspect ratio)
layout (location = 2) uniform vec2 camScale;

layout (location = 0) in vec2 position;
layout (location = 1) in vec2 texCoords;

out vec2 fragTexCoords;

const vec2 center = vec2(0.5);

void main() {
    fragTexCoords = (camTransform * vec4(texCoords.x, 1.0 - texCoords.y, 0.0, 1.0)).xy;
    gl_Position = vec4(camScale * position, 0.0, 1.0);
}