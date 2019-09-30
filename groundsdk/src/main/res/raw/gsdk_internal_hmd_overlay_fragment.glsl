#version 300 es
#extension GL_OES_EGL_image_external_essl3 : require

precision highp float;

layout (location = 0) uniform samplerExternalOES sampler;

in vec2 fragTexCoords;

out vec4 fragColor;

void main() {
    fragColor = texture(sampler, fragTexCoords);
}