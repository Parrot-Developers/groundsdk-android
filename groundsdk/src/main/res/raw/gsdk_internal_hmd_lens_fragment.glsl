#version 300 es

precision highp float;

layout (location = 0) uniform sampler2D sampler;

in vec2 fragTexCoordsR;
in vec2 fragTexCoordsG;
in vec2 fragTexCoordsB;
in vec4 fragColorFilter;

out vec4 fragColor;

void main() {
    fragColor = vec4(vec3(0.0), 1.0);

    if (fragTexCoordsR.x >= 0.0 && fragTexCoordsR.y >= 0.0 && fragTexCoordsR.x <= 1.0 && fragTexCoordsR.y <= 1.0) {
        fragColor.r = texture(sampler, fragTexCoordsR).r;
    }

    if (fragTexCoordsG.x >= 0.0 && fragTexCoordsG.y >= 0.0 && fragTexCoordsG.x <= 1.0 && fragTexCoordsG.y <= 1.0) {
        fragColor.g = texture(sampler, fragTexCoordsG).g;
    }

    if (fragTexCoordsB.x >= 0.0 && fragTexCoordsB.y >= 0.0 && fragTexCoordsB.x <= 1.0 && fragTexCoordsB.y <= 1.0) {
        fragColor.b = texture(sampler, fragTexCoordsB).b;
    }

    fragColor *= fragColorFilter;
}
