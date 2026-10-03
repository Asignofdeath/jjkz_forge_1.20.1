#version 150

/*
 * 无量空处「扫描波」—— 世界空间球壳扫描。
 *
 * 与旧的「全屏 quad 屏幕空间近似」的本质区别：这里拿主帧缓冲的**深度**逐像素反投影出
 * 真实的表面世界坐标，再算它到球心的距离。所以亮环只出现在「实际几何体到球心的距离
 * 恰好等于半径」的地方 —— 也就是贴在起伏地形上的等高线，而不是盖在屏幕上的一条线。
 *
 * 参考 JCraft 时停特效（jcraft:za_warudo）的球壳扫描与「扫过即去饱和」思路，
 * 去掉其色相反转与鱼眼折射：
 *   - 球壳处划出一圈亮环（贴地形的等高线）；
 *   - 球**内**（即已被扫过的区域）整体去饱和成灰，圈收回时随球体缩小自动复原 ——
 *     这是纯几何推导，不需要任何额外状态。
 *
 * 天空与未写深度的像素提前返回：没有几何体就没有扫描结果。
 */

uniform sampler2D DiffuseSampler;      // 输入帧缓冲（本 pass 的 inTarget 颜色）
uniform sampler2D DepthSampler;        // 主帧缓冲的深度（Java 每帧 copyDepthFrom 进来）
uniform mat4  InverseTransformMatrix;  // inv(ProjMat * ModelViewMat)：clip -> 相机相对世界
uniform vec3  CameraPosition;          // 相机世界坐标
uniform vec3  Center;                  // 球心世界坐标
uniform float Radius;                  // 当前半径（世界格）
uniform float Intensity;               // 总强度 0..1

in vec2 texCoord;
in vec4 vPosition;

out vec4 fragColor;

/** 本体色调：极淡的冷白。纯白读起来像描边线，掺一点青蓝才像能量。 */
const vec3 WAVE_TINT = vec3(0.80, 0.92, 1.00);

/** 被扫过区域（球内）的去饱和程度：1 = 完全灰度。想留一点色相就调到 0.8 上下。 */
const float GREY_STRENGTH = 1.0;

/** 环剖面：锐利核心 + 两侧柔晕。delta = 当前距离 - 环半径。 */
float ringProfile(float delta, float width) {
    float a = abs(delta);
    float core = 1.0 - smoothstep(0.0, width, a);
    float glow = 1.0 - smoothstep(0.0, width * 5.0, a);
    return core + glow * 0.30;
}

void main() {
    vec3 base = texture(DiffuseSampler, texCoord).rgb;

    float depth = texture(DepthSampler, texCoord).x;
    if (depth >= 1.0) {
        /* 天空 / 背景：没有几何体，扫描波不该在这里出现。 */
        fragColor = vec4(base, 1.0);
        return;
    }

    /* 屏幕像素 -> NDC -> 相机相对世界 -> 绝对世界坐标。
       世界渲染阶段的 ModelViewMat 是「相机相对」的（只含朝向、不含平移），
       所以反投影结果要再加回相机世界坐标。 */
    vec3 ndc = vPosition.xyz / vPosition.w;
    vec4 clipPos = vec4(ndc.xy, depth * 2.0 - 1.0, 1.0);
    vec4 relative = InverseTransformMatrix * clipPos;
    vec3 worldPos = relative.xyz / relative.w + CameraPosition;

    float dist = distance(worldPos, Center);

    /* 环带半宽随半径增长：固定 1 格宽在大半径下会细到亚像素而闪烁。 */
    float band = max(1.2, Radius * 0.045);

    /* 波阵面：主环 + 内侧 3 层回波，宽度递增、亮度递减，构成有厚度的波前。 */
    float ring = ringProfile(dist - Radius, band);
    for (int i = 1; i <= 3; i++) {
        float fi = float(i);
        ring += ringProfile(dist - (Radius - band * 1.7 * fi), band * (1.0 + fi * 0.35))
                * (1.0 - fi * 0.20);
    }

    /* 贴住波前的柔光：一道紧、一道宽，让环像在发光而不只是被描了边。 */
    float halo = (1.0 - smoothstep(0.0, band * 8.0, abs(dist - Radius))) * 0.22
               + (1.0 - smoothstep(0.0, band * 20.0, abs(dist - Radius))) * 0.10;

    float add = (ring + halo) * Intensity;

    /* 是否已被球壳扫过：球内 = 已扫过。过渡带取 1.2 倍环宽、落在环的外侧，
       所以「复原」正好发生在圈收回经过该像素的瞬间。 */
    float covered = 1.0 - smoothstep(0.0, band * 1.2, dist - Radius);

    /* 只对底色去饱和：向亮度插值即完全灰度（比 rgb2hsv 便宜，也不会因色相环回出偏色）。
       圈收回时球体缩小，像素依次退出球内 —— 复原不需要任何额外状态。 */
    float luma = dot(base, vec3(0.299, 0.587, 0.114));
    vec3 color = mix(base, vec3(luma), covered * GREY_STRENGTH * Intensity);

    /* 环叠在灰化**之后**，所以亮环保持冷白色调、不被灰掉。 */
    color += WAVE_TINT * add * 1.5;

    fragColor = vec4(color, 1.0);
}
