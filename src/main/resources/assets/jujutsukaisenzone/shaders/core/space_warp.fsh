#version 150

uniform sampler2D DiffuseSampler;

uniform vec2  CenterUV;
uniform float Intensity;
uniform float Radius;
uniform float Aspect;

in vec2 texCoord;
out vec4 fragColor;

void main() {
    if (Intensity <= 0.001 || Radius <= 0.001) {
        fragColor = texture(DiffuseSampler, texCoord);
        return;
    }

    // 按宽高比校正距离，避免横向被拉扁
    vec2 aspect = vec2(Aspect, 1.0);
    vec2 delta = (texCoord - CenterUV) * aspect;
    float dist = length(delta);

    float falloff = 1.0 - smoothstep(0.0, Radius, dist);
    falloff *= falloff;

    // 径向向中心拉扯（屏幕空间压缩）
    float pull = falloff * Intensity * 0.55;
    vec2 warped = texCoord - (delta / aspect) * pull;
    warped = clamp(warped, 0.001, 0.999);

    fragColor = texture(DiffuseSampler, warped);
}
