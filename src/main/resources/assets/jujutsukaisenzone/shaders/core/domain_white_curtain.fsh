#version 150

in vec2 texCoord;
out vec4 fragColor;

uniform float Progress;
uniform float Aspect;

float hash21(vec2 p) {
    p = fract(p * vec2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}

float valueNoise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    float a = hash21(i);
    float b = hash21(i + vec2(1.0, 0.0));
    float c = hash21(i + vec2(0.0, 1.0));
    float d = hash21(i + vec2(1.0, 1.0));
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(a, b, u.x) + (c - a) * u.y * (1.0 - u.x) + (d - b) * u.x * u.y;
}

float fbm(vec2 p) {
    float v = 0.0;
    float a = 0.5;
    for (int i = 0; i < 5; i++) {
        v += a * valueNoise(p);
        p *= 2.03;
        a *= 0.5;
    }
    return v;
}

void main() {
    vec2 uv = texCoord * 2.0 - 1.0;
    uv.x *= Aspect;

    float r = length(uv);
    float ang = atan(uv.y, uv.x);

    float spreadEnd = 0.365;
    float holdEnd = 0.852;
    float u = clamp(Progress, 0.0, 1.0);

    float spreadT = clamp(u / spreadEnd, 0.0, 1.0);
    float spreadRadius = mix(0.0, 1.85, spreadT * spreadT);
    float cover = 1.0 - smoothstep(spreadRadius - 0.18, spreadRadius + 0.02, r);

    if (u > spreadEnd) {
        cover = max(cover, 1.0);
    }

    float hole = 0.0;
    if (u > holdEnd) {
        float holeT = (u - holdEnd) / max(1.0 - holdEnd, 0.0001);

        // 角向高频：一圈针状尖刺
        float ang01 = ang / 6.2831853;
        float lobeA = valueNoise(vec2(ang01 * 18.0, 1.7 + holeT));
        float lobeB = valueNoise(vec2(ang01 * 42.0 + 2.3, 4.1));
        float lobeC = valueNoise(vec2(ang01 * 96.0 - 1.1, 8.8));
        float spikes = pow(lobeA, 2.2) * 0.42
                     + pow(lobeB, 3.5) * 0.28
                     + pow(lobeC, 5.0) * 0.16;

        // 空间噪声：大块撕边 + 中频褶皱 + 细砂
        float nBig = fbm(uv * 3.2 + vec2(2.7, 1.3));
        float nMid = fbm(uv * 9.5 - vec2(0.8, 3.1) + vec2(holeT * 0.4, 0.0));
        float nFine = valueNoise(uv * 26.0 + vec2(5.2, -3.4));
        float nGrit = valueNoise(uv * 54.0 - vec2(1.6, 7.2));

        float jagged = (nBig - 0.42) * 0.55
                     + (nMid - 0.5) * 0.38
                     + (nFine - 0.5) * 0.18
                     + (nGrit - 0.5) * 0.08
                     + spikes;

        float radius = 0.06 + holeT * holeT * 2.25;
        float shape = r - radius - jagged * (0.65 + holeT * 0.35);

        // 硬切边：几乎不羽化，保留白色毛刺轮廓（无深色描边）
        float edge = 0.008;
        hole = 1.0 - smoothstep(-edge, edge, shape);
    }

    float whiteAlpha = cover * (1.0 - hole);
    vec3 col = vec3(1.0);
    float fadeOut = 1.0 - smoothstep(0.92, 1.0, u);
    float alpha = whiteAlpha * fadeOut;

    if (alpha < 0.01) {
        discard;
    }
    fragColor = vec4(col, alpha);
}
