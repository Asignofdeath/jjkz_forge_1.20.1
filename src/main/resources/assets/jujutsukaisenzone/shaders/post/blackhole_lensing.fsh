#version 150

// 黑洞_A —— 引力透镜片元着色器（移植自 Soul's Black Holes，Schwarzschild 几何）
//
//   Rs       = 史瓦西半径（事件视界）
//   r_photon = 1.5 Rs（光子球）
//   b_crit   = (3√3/2) Rs ≈ 2.598 Rs（临界撞影参数 = 阴影表观半径）
//
// 薄透镜偏折 δ = θ_E²/θ；近光子球处叠加 Darwin (1959) 强场对数发散项。
// UV 偏移方向朝外：光线弯折使源的表观位置远离透镜。

uniform sampler2D SceneTexture;
uniform sampler2D DepthTexture;
uniform vec2 ScreenSize;
uniform int NumBlackHoles;
uniform vec2 BlackHoleScreenPos[8];
uniform float BlackHoleScreenRadius[8]; // 史瓦西半径（屏幕坐标）
uniform float BlackHoleDepth[8];
uniform float LensingStrength;
uniform float NearPlane;
uniform float FarPlane;

in vec2 texCoord;
out vec4 fragColor;

const float B_CRIT = 2.598; // 3*sqrt(3)/2

float linearizeDepth(float depth) {
    float z = depth * 2.0 - 1.0;
    return (2.0 * NearPlane * FarPlane) / (FarPlane + NearPlane - z * (FarPlane - NearPlane));
}

void main() {
    vec2 uv = texCoord;
    vec2 totalOffset = vec2(0.0);
    bool insideHorizon = false;
    float photonGlow = 0.0;

    float aspect = ScreenSize.x / ScreenSize.y;
    float rawDepth = texture(DepthTexture, uv).r;
    float pixelDepth = linearizeDepth(rawDepth);

    for (int i = 0; i < NumBlackHoles && i < 8; i++) {
        vec2 bhPos = BlackHoleScreenPos[i];
        float rs = BlackHoleScreenRadius[i];

        if (rs <= 0.0) continue;

        float bhDepth = BlackHoleDepth[i];
        float shadowR = rs * B_CRIT;

        vec2 corrUV = vec2(uv.x * aspect, uv.y);
        vec2 corrBH = vec2(bhPos.x * aspect, bhPos.y);

        vec2 toCenter = corrBH - corrUV;
        float dist = length(toCenter);

        // 阴影内部 —— 纯黑
        if (dist < shadowR && pixelDepth >= bhDepth * 0.5) {
            insideHorizon = true;
            break;
        }

        vec2 radialDir = toCenter / max(dist, 0.0001);
        vec2 tangentDir = vec2(-radialDir.y, radialDir.x);

        // 深度衰减 —— 放得很宽，让黑洞前方的高亮前景（如喷流）也被透镜弯折
        float depthRatio = pixelDepth / max(bhDepth, 0.1);
        if (depthRatio < 0.1) continue;
        float depthFade = smoothstep(0.1, 0.4, depthRatio);

        // --- 引力偏折：弱场项 + 强场对数项 ---
        float thetaE = shadowR;
        float dist0 = max(dist, shadowR * 0.5);
        float weakField = (thetaE * thetaE) / dist0;

        // Darwin 强场项：dist → shadowR 时对数发散（0.55 系数贴近理论 Darwin 形式）
        float excess = max((dist - shadowR) / shadowR, 0.01);
        float strongField = shadowR * max(0.0, -log(excess) * 0.55);

        float deflection = (weakField + strongField) * LensingStrength * depthFade;
        deflection = min(deflection, shadowR * 4.0);

        vec2 radialOffset = -radialDir * deflection;

        // --- 阴影边缘的切向缠绕 ---
        float distInShadow = dist / shadowR;
        float tangentialStrength = 0.0;
        if (distInShadow > 0.7 && distInShadow < 5.0) {
            tangentialStrength = deflection * 0.75 * exp(-pow((distInShadow - 1.0) / 0.8, 2.0));
            tangentialStrength = min(tangentialStrength, shadowR * 1.6);
        }
        vec2 tangentOffset = tangentDir * tangentialStrength;

        vec2 offset = radialOffset + tangentOffset;
        offset.x /= aspect;
        totalOffset += offset;

        // --- 光子环光晕（b = b_crit 处）---
        float ringDist = abs(distInShadow - 1.0);
        photonGlow += smoothstep(0.18, 0.0, ringDist) * 0.55 * depthFade;

        // 二级环（绕行 1.5 圈的光）
        float ring2Dist = abs(distInShadow - 1.02);
        photonGlow += smoothstep(0.06, 0.0, ring2Dist) * 0.25 * depthFade;
    }

    if (insideHorizon) {
        fragColor = vec4(0.0, 0.0, 0.0, 1.0);
        return;
    }

    vec2 lensedUV = uv + totalOffset;

    // 采样越出屏幕的部分淡出为黑
    float fade = 1.0;
    if (lensedUV.x < 0.0 || lensedUV.x > 1.0 ||
        lensedUV.y < 0.0 || lensedUV.y > 1.0) {
        vec2 overshoot = max(vec2(0.0), max(-lensedUV, lensedUV - 1.0));
        fade = 1.0 - clamp(length(overshoot) * 10.0, 0.0, 1.0);
    }
    lensedUV = clamp(lensedUV, vec2(0.002), vec2(0.998));

    vec4 sceneColor = texture(SceneTexture, lensedUV);
    sceneColor.rgb *= fade;

    // 光子环光晕 —— 与吸积盘辐射一致的金色
    photonGlow = min(photonGlow, 0.55);
    sceneColor.rgb += vec3(0.75, 0.58, 0.28) * photonGlow;

    fragColor = sceneColor;
}
