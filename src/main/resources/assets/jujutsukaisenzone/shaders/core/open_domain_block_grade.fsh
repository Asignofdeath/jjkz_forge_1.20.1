#version 150

uniform sampler2D DiffuseSampler;
uniform sampler2D DepthSampler;

uniform mat4 InvViewProjMat;
uniform vec3 DomainCenterRel;
uniform float DomainRadius;
uniform float Time;

in vec2 texCoord;
out vec4 fragColor;

float hash21(vec2 p) {
    p = fract(p * vec2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}

void main() {
    vec4 scene = texture(DiffuseSampler, texCoord);
    float depth = texture(DepthSampler, texCoord).r;

    // 天空 / 远平面：不改写
    if (depth >= 0.9995) {
        fragColor = scene;
        return;
    }

    vec4 clip = vec4(texCoord * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
    vec4 rel = InvViewProjMat * clip;
    if (abs(rel.w) < 1e-6) {
        fragColor = scene;
        return;
    }
    rel /= rel.w;

    // rel.xyz 与 DomainCenterRel 同为「相对相机」的世界偏移，避免再加 CameraPos 造成整球误判
    float dist = length(rel.xyz - DomainCenterRel);
    float edge = 2.5;
    float inside = 1.0 - smoothstep(DomainRadius - edge, DomainRadius + 0.5, dist);
    if (inside <= 0.001) {
        fragColor = scene;
        return;
    }

    float luma = dot(scene.rgb, vec3(0.299, 0.587, 0.114));
    // 冷白低饱和，保留纹理阴影（沿用你调弱后的方向）
    vec3 graded = scene.rgb * 0.35 + vec3(luma) * 0.45
                + vec3(0.06, 0.08, 0.12);

    float n = hash21(floor((rel.xz + DomainCenterRel.xz) * 2.0) + floor(Time * 3.0));
    graded += (n - 0.5) * 0.02;

    fragColor = vec4(mix(scene.rgb, clamp(graded, 0.0, 1.0), inside * 0.85), scene.a);
}
