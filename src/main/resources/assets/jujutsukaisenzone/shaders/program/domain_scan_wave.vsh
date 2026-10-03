#version 150

/*
 * 无量空处「扫描波」后处理：全屏 quad 的顶点着色器。
 * 结构与原版 assets/minecraft/shaders/program/blit.vsh 完全一致 ——
 * PostPass 只会下发 ProjMat / OutSize（不会下发 ModelViewMat），照它写最稳。
 *
 * Position 是输出帧缓冲的**像素**坐标（0..OutSize），故 texCoord = Position.xy / OutSize。
 */

in vec4 Position;

uniform mat4 ProjMat;
uniform vec2 OutSize;

out vec2 texCoord;
out vec4 vPosition;

void main() {
    vec4 outPos = ProjMat * vec4(Position.xy, 0.0, 1.0);
    gl_Position = vec4(outPos.xy, 0.2, 1.0);
    vPosition = gl_Position;

    texCoord = Position.xy / OutSize;
}
