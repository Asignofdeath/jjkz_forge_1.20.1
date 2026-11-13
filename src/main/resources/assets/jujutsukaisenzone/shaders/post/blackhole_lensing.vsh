#version 150

// 黑洞_A —— 引力透镜顶点着色器：全屏三角形直通

in vec2 Position;

out vec2 texCoord;

void main() {
    // Position 在 [0,1] 区间，映射到 [-1,1] 裁剪空间
    gl_Position = vec4(Position * 2.0 - 1.0, 0.0, 1.0);
    texCoord = Position;
}
