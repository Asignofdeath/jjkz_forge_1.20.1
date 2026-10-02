#version 150

in vec2 texCoord;
out vec4 fragColor;

uniform vec3  Center;      // 扫描波球心（世界坐标）
uniform vec3  CameraPos;   // 相机世界坐标
uniform mat4  InvViewProj; // clip -> world 的逆矩阵
uniform float Radius;      // 当前半径（世界格）
uniform float Intensity;   // 总强度 0..1
uniform float Time;        // 自本波开始以来的秒数

/**
 * 本体色调：极淡的冷白。无量空处是「虚空 / 宇宙」意象，纯白偏素，
 * 掺一点点青蓝即可让辉光读起来像「能量」而不是「几何线」。
 */
const vec3 WAVE_TINT = vec3(0.80, 0.92, 1.00);

/**
 * 环剖面：锐利核心 + 两侧柔和光晕。比单纯的方波带更有「光」的质感。
 * delta 为「当前距离 - 环半径」（带符号）。
 */
float ringProfile(float delta, float width) {
    float a = abs(delta);
    float core = smoothstep(width, 0.0, a);
    float glow = smoothstep(width * 4.5, 0.0, a) * 0.32;
    return core + glow;
}

/* 廉价一维值噪声：给波前加不规则扰动，避免完美的圆。 */
float hash11(float p) {
    p = fract(p * 0.1031);
    p *= p + 33.33;
    p *= p + p;
    return fract(p);
}

float valueNoise(float x) {
    float i = floor(x);
    float f = fract(x);
    float u = f * f * (3.0 - 2.0 * f);
    return mix(hash11(i), hash11(i + 1.0), u);
}

void main() {
    if (Radius <= 0.0 || Intensity <= 0.0) {
        discard;
    }

    vec2 ndc = texCoord * 2.0 - 1.0;

    /* 由 NDC 反投影出视线方向（近平面 -> 远平面）。 */
    vec4 nearClip = InvViewProj * vec4(ndc, -1.0, 1.0);
    vec4 farClip  = InvViewProj * vec4(ndc,  1.0, 1.0);
    vec3 nearWorld = nearClip.xyz / nearClip.w;
    vec3 farWorld  = farClip.xyz  / farClip.w;
    vec3 rd = normalize(farWorld - nearWorld);

    /* 环带半宽随半径增长：固定 1 格宽在大半径下会细到亚像素而闪烁。 */
    float band = max(1.2, Radius * 0.045);

    /* 时间脉动：让波前有「呼吸」而不是恒亮的硬线。 */
    float pulse = 0.86 + 0.14 * sin(Time * 20.0);

    float ring = 0.0;   // 所有锐环的叠加
    float halo = 0.0;   // 所有柔光 / 纹理的叠加

    /* ---- A. 地面扫描 ----
       视线与「球心所在水平面」求交 —— 这里能拿到真实世界交点，
       所以除了环，还能做多层回波、同心细环与放射条纹。
       站在球心的施法者也能看见（球壳轮廓在球心处不可见，见 B 的权重）。 */
    if (rd.y < -1.0e-4) {
        float t = (Center.y - CameraPos.y) / rd.y;
        if (t > 0.0 && t < 1.0e6) {
            vec3 hit = CameraPos + rd * t;
            vec2 delta = hit.xz - Center.xz;
            float dist = length(delta);
            float ang = atan(delta.y, delta.x);

            /* 波前扰动：两个频率叠加，破坏完美圆形。 */
            float wobble = valueNoise(ang * 2.6 + Time * 0.8) - 0.5
                         + valueNoise(ang * 6.3 - Time * 0.5) - 0.5;
            float distW = dist + wobble * band * 1.4;

            /* 1) 多层回波：主波前之后拖 3 环、之前带 2 环，构成有厚度的波阵面。 */
            ring += ringProfile(distW - Radius, band);
            for (int i = 1; i <= 3; i++) {
                float fi = float(i);
                ring += ringProfile(distW - (Radius - band * 1.7 * fi),
                                    band * (1.0 + fi * 0.35)) * (1.0 - fi * 0.20);
            }
            for (int i = 1; i <= 2; i++) {
                float fi = float(i);
                ring += ringProfile(distW - (Radius + band * 1.7 * fi),
                                    band * (1.0 - fi * 0.15)) * (1.0 - fi * 0.32);
            }

            /* 2) 贴住波前的柔光：让它像在发光，而不只是被描了边。 */
            halo += smoothstep(band * 10.0, 0.0, abs(distW - Radius)) * 0.16;

            /* 3) 同心「无限」细环：每 8 格一圈，越靠外越淡。 */
            float cell = fract(dist / 8.0);
            float lineD = min(cell, 1.0 - cell) * 8.0;
            halo += smoothstep(0.55, 0.0, lineD) * 0.10
                    * smoothstep(Radius * 1.10, Radius * 0.30, dist);

            /* 4) 放射条纹：随时间缓慢旋转，像能量自球心流出。 */
            float spokes = pow(abs(sin(ang * 9.0 + Time * 0.9)), 10.0);
            halo += spokes * 0.20 * smoothstep(Radius, Radius * 0.12, dist);

            /* 5) 地面整体泛光：波前内侧的柔光底。 */
            halo += smoothstep(Radius, Radius * 0.50, dist) * 0.07;
        }
    }

    /* ---- B. 球壳轮廓 ----
       视线到球心的垂直距离 = 球体在屏幕上的角半径（照 za_warudo.fsh 口径，
       但 core shader 读不到主帧缓冲，故无折射 / 补色）。
       相机贴近球心时球体自内部不可见，故按距离淡出。 */
    vec3 oc = Center - CameraPos;
    float tca = dot(oc, rd);
    float distSphere = sqrt(max(dot(oc, oc) - tca * tca, 0.0));
    float sphereWeight = smoothstep(1.0, 6.0, length(oc));
    if (sphereWeight > 0.002) {
        ring += sphereWeight * (
                  ringProfile(distSphere - Radius, band)
                + ringProfile(distSphere - (Radius - band * 2.5), band * 0.55) * 0.75
                + ringProfile(distSphere - (Radius + band * 2.5), band * 0.55) * 0.45);

        /* 球体屏幕圆盘内部的柔光：让「球」而不只是「环」可见。 */
        halo += sphereWeight * smoothstep(Radius * 1.02, Radius * 0.45, distSphere) * 0.10;
    }

    /* ---- 合成 ----
       先把锐环（带脉动）与柔光相加，再用 1-exp(-x) 做软饱和：
       多层叠加时不会整片爆白，但波前中心依旧能顶到 1。 */
    float total = (ring * pulse + halo) * 1.35;
    float alpha = (1.0 - exp(-max(total, 0.0) * 1.8)) * Intensity;
    if (alpha < 0.004) {
        discard;
    }
    fragColor = vec4(WAVE_TINT, alpha);
}
