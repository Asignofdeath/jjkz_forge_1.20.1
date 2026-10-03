#version 150

/*
 * 开放式无量空处：球内方块调色。
 * 深度反投影 → 只改球内可见表面；天空 / 球外原样。
 *
 * 视觉目标（相对旧版「全局灰蓝滤镜」）：
 *   - 球心更洗白、边缘更冷暗（径向层次）
 *   - 球壳贴地形细亮环（与扫描波同语言）
 *   - 世界空间噪声 / 微扫描线（信息过载，仍保留材质阴影）
 *   - 低频呼吸脉冲 + 轻微径向色差
 */

uniform sampler2D DiffuseSampler;
uniform sampler2D DepthSampler;

uniform mat4 InvViewProjMat;
uniform vec3 DomainCenterRel;
uniform float DomainRadius;
uniform float Time;

in vec2 texCoord;
out vec4 fragColor;

const vec3 COLD_TINT = vec3(0.78, 0.90, 1.00);
const vec3 WASH_TINT = vec3(0.96, 0.98, 1.00);

float hash21(vec2 p) {
    p = fract(p * vec2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}

float hash31(vec3 p) {
    p = fract(p * 0.1031);
    p += dot(p, p.yzx + 33.33);
    return fract((p.x + p.y) * p.z);
}

float valueNoise3(vec3 p) {
    vec3 i = floor(p);
    vec3 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    float n000 = hash31(i);
    float n100 = hash31(i + vec3(1.0, 0.0, 0.0));
    float n010 = hash31(i + vec3(0.0, 1.0, 0.0));
    float n110 = hash31(i + vec3(1.0, 1.0, 0.0));
    float n001 = hash31(i + vec3(0.0, 0.0, 1.0));
    float n101 = hash31(i + vec3(1.0, 0.0, 1.0));
    float n011 = hash31(i + vec3(0.0, 1.0, 1.0));
    float n111 = hash31(i + vec3(1.0, 1.0, 1.0));
    float nx00 = mix(n000, n100, f.x);
    float nx10 = mix(n010, n110, f.x);
    float nx01 = mix(n001, n101, f.x);
    float nx11 = mix(n011, n111, f.x);
    float nxy0 = mix(nx00, nx10, f.y);
    float nxy1 = mix(nx01, nx11, f.y);
    return mix(nxy0, nxy1, f.z);
}

float fbm3(vec3 p) {
    float v = 0.0;
    float a = 0.5;
    for (int i = 0; i < 4; i++) {
        v += a * valueNoise3(p);
        p = p * 2.07 + vec3(1.7, 9.2, 3.1);
        a *= 0.5;
    }
    return v;
}

/** 环剖面：锐利核心 + 柔晕（与 domain_scan_wave 同口径）。 */
float ringProfile(float delta, float width) {
    float a = abs(delta);
    float core = 1.0 - smoothstep(0.0, width, a);
    float glow = 1.0 - smoothstep(0.0, width * 4.5, a);
    return core + glow * 0.28;
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

    // rel / DomainCenterRel 均为相机相对偏移 → 差值是领域局部坐标（随相机稳定）
    vec3 local = rel.xyz - DomainCenterRel;
    float dist = length(local);
    float edge = 2.5;
    float inside = 1.0 - smoothstep(DomainRadius - edge, DomainRadius + 0.5, dist);
    if (inside <= 0.001) {
        fragColor = scene;
        return;
    }

    // 0 = 球心，1 = 边界内侧
    float radial = clamp(dist / max(DomainRadius, 1.0), 0.0, 1.0);
    float centerWash = 1.0 - smoothstep(0.0, 0.55, radial);
    float rimCold = smoothstep(0.55, 1.0, radial);

    float luma = dot(scene.rgb, vec3(0.299, 0.587, 0.114));
    // S 曲线：压中间、抬高光，保留材质明暗
    float shaped = luma * luma * (3.0 - 2.0 * luma);
    float contrast = mix(luma, shaped, 0.55);

    // 基础冷灰 + 球心洗白 + 边缘更冷
    vec3 graded = mix(scene.rgb, vec3(contrast), 0.72);
    graded = graded * mix(0.42, 0.28, rimCold) + COLD_TINT * mix(0.22, 0.38, rimCold);
    graded = mix(graded, WASH_TINT * (0.55 + contrast * 0.55), centerWash * 0.55);

    // 世界空间信息噪声（随 Time 缓慢漂移）
    vec3 nPos = local * 0.35 + vec3(0.0, Time * 0.35, Time * 0.18);
    float nBig = fbm3(nPos);
    float nFine = valueNoise3(local * 1.8 + vec3(Time * 1.1));
    float flicker = hash21(floor(local.xz * 3.0) + floor(Time * 8.0));
    graded += (nBig - 0.45) * 0.10 * COLD_TINT;
    graded += (nFine - 0.5) * 0.06;
    graded += (flicker - 0.5) * 0.045 * (0.35 + centerWash);

    // 横向微扫描线（屏幕空间，弱）
    float scan = sin(texCoord.y * 720.0 + Time * 18.0) * 0.5 + 0.5;
    graded += COLD_TINT * (scan - 0.5) * 0.035 * inside;

    // 球壳贴地形亮环
    float ring = ringProfile(dist - DomainRadius, 0.55) * inside;
    graded += COLD_TINT * ring * 0.55;

    // 低频呼吸：整体曝光微起伏
    float breath = 0.92 + 0.08 * sin(Time * 1.7);
    graded *= breath;

    // 轻微径向色差（屏幕空间，离开画面中心）
    vec2 fromCenter = texCoord - vec2(0.5);
    float aberrLen = length(fromCenter);
    vec2 aberrDir = aberrLen > 1e-4 ? fromCenter / aberrLen : vec2(0.0);
    float aberr = 0.0022 * inside * (0.4 + centerWash);
    float r = texture(DiffuseSampler, texCoord + aberrDir * aberr).r;
    float b = texture(DiffuseSampler, texCoord - aberrDir * aberr).b;
    graded.r = mix(graded.r, mix(graded.r, r, 0.35) + 0.04, inside * 0.5);
    graded.b = mix(graded.b, mix(graded.b, b, 0.35) + 0.06, inside * 0.5);

    float strength = inside * mix(0.78, 0.92, centerWash);
    fragColor = vec4(mix(scene.rgb, clamp(graded, 0.0, 1.0), strength), scene.a);
}
