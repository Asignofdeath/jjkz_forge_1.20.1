package com.darkness.jujutsukaisenzone.client.renderer;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import com.noodlegamer76.shadered.client.renderer.ComplexPassRenderer;
import com.noodlegamer76.shadered.client.util.skyblock.SkyBoxRenderer;
import com.noodlegamer76.shadered.client.util.skyblock.SkyblockPass;
import com.noodlegamer76.shadered.client.util.skyblock.SkyboxTranslation;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * 瞬发领域关闭后的十字残留窗口。
 * <p>
 * 十字几何仅作窗口；内部通过与 {@code jjkz_skyblock} 相同的立方体贴图 FBO +
 * {@code shadered:skyblock} 采样呈现领域空间。窗口始终面朝相机，并在平面内自转数圈后消失。
 * <p>
 * 变换说明：ComplexPass 绘制天空盒时 {@code ModelViewMat} 为单位矩阵，顶点需自带完整变换。
 * 等效于普通粒子 billboard：{@code View * T(world - camera) * camera.rotation() * Rz(spin)}，
 * 再在本地 XY 平面画十字。缺 View 会漂位；缺 {@code camera.rotation()} 则不再朝向摄像机。
 */
public final class InstantDomainCrossResidualRenderer
{
    /** 十字臂半长（格）——细长光线。 */
    private static final float ARM_HALF_LENGTH = 5.5F;
    /** 十字臂中心半宽（格）；两端收尖为 0。 */
    private static final float ARM_HALF_WIDTH = 0.18F;
    /** 起始姿态：+45°，看起来更接近原作倾斜十字。 */
    private static final float BASE_ROLL_RADIANS = (float) (Math.PI * 0.25D);

    private static boolean active;
    private static double worldX;
    private static double worldY;
    private static double worldZ;
    private static long startMillis;
    private static long totalMillis;
    private static float revolutions;
    private static ResourceLocation textureFolder;

    private InstantDomainCrossResidualRenderer()
    {
    }

    public static void play(
            double x,
            double y,
            double z,
            int durationTicks,
            float spinRevolutions,
            ResourceLocation skyboxTextureFolder)
    {
        active = true;
        worldX = x;
        worldY = y;
        worldZ = z;
        startMillis = System.currentTimeMillis();
        totalMillis = Math.max(durationTicks, 1) * 50L;
        revolutions = Math.max(spinRevolutions, 0.0F);
        textureFolder = skyboxTextureFolder;
    }

    public static boolean isActive()
    {
        if (!active)
        {
            return false;
        }
        if (System.currentTimeMillis() - startMillis >= totalMillis)
        {
            active = false;
            return false;
        }
        return true;
    }

    /**
     * 由 {@link com.darkness.jujutsukaisenzone.client.renderer.skyblock.JJKZSkyblockRenderPass}
     * 在天空盒几何通道中调用：先渲染立方体贴图到 FBO，再绘制十字窗口。
     */
    public static void renderInSkyblockPass(PoseStack levelPoseStack, ComplexPassRenderer renderer)
    {
        if (!isActive() || textureFolder == null)
        {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null)
        {
            return;
        }

        TextureTarget readTarget = renderer.getRenderBuffer();
        TextureTarget skyboxTarget = renderer.getWriteBuffer();

        skyboxTarget.bindWrite(true);
        levelPoseStack.pushPose();
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        SkyBoxRenderer.renderBlockSkybox(
                levelPoseStack,
                textureFolder,
                GameRenderer.getPositionTexColorShader(),
                new SkyboxTranslation()
        );
        levelPoseStack.popPose();
        readTarget.bindWrite(true);

        float progress = resolveProgress();
        float spinRadians = BASE_ROLL_RADIANS + progress * revolutions * ((float) (Math.PI * 2.0D));

        Camera camera = minecraft.gameRenderer.getMainCamera();
        Vec3 cameraPos = camera.getPosition();

        /*
         * View（关卡 PoseStack）保证落在正确视空间位置；
         * camera.rotation() 把本地 XY 对准摄像机（与 StarMark 头顶 billboard 相同）；
         * 再绕本地 Z 自转。
         */
        PoseStack poseStack = new PoseStack();
        poseStack.mulPoseMatrix(levelPoseStack.last().pose());
        poseStack.translate(worldX - cameraPos.x, worldY - cameraPos.y, worldZ - cameraPos.z);
        poseStack.mulPose(camera.rotation());
        poseStack.mulPose(Axis.ZP.rotation(spinRadians));

        ShaderInstance shader = SkyblockPass.NORMAL.getShader();
        if (shader == null)
        {
            return;
        }

        RenderSystem.getModelViewStack().pushPose();
        RenderSystem.getModelViewStack().setIdentity();
        RenderSystem.applyModelViewMatrix();

        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.setShader(() -> shader);
        shader.setSampler("Skybox", skyboxTarget.getColorTextureId());
        shader.setSampler("PassDepth", readTarget.getDepthTextureId());

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);

        Matrix4f matrix = poseStack.last().pose();
        emitCrossArm(buffer, matrix, true);
        emitCrossArm(buffer, matrix, false);

        tesselator.end();

        RenderSystem.enableCull();
        RenderSystem.getModelViewStack().popPose();
        RenderSystem.applyModelViewMatrix();
    }

    private static float resolveProgress()
    {
        long elapsed = System.currentTimeMillis() - startMillis;
        return Math.min(1.0F, elapsed / (float) totalMillis);
    }

    /**
     * 细长菱形臂：两端尖、中心最宽，类似光线/星芒。
     *
     * @param horizontal true 为沿本地 X 的水平臂，false 为沿本地 Y 的竖直臂
     */
    private static void emitCrossArm(BufferBuilder buffer, Matrix4f matrix, boolean horizontal)
    {
        float tip = ARM_HALF_LENGTH;
        float mid = ARM_HALF_WIDTH;
        float x0;
        float y0;
        float x1;
        float y1;
        float x2;
        float y2;
        float x3;
        float y3;
        if (horizontal)
        {
            /* 左尖 → 下宽 → 右尖 → 上宽 */
            x0 = -tip;
            y0 = 0.0F;
            x1 = 0.0F;
            y1 = -mid;
            x2 = tip;
            y2 = 0.0F;
            x3 = 0.0F;
            y3 = mid;
        }
        else
        {
            /* 下尖 → 右宽 → 上尖 → 左宽 */
            x0 = 0.0F;
            y0 = -tip;
            x1 = mid;
            y1 = 0.0F;
            x2 = 0.0F;
            y2 = tip;
            x3 = -mid;
            y3 = 0.0F;
        }

        /* 正面 */
        putQuad(buffer, matrix, x0, y0, 0.0F, x1, y1, 0.0F, x2, y2, 0.0F, x3, y3, 0.0F);
        /* 背面 */
        putQuad(buffer, matrix, x0, y0, 0.0F, x3, y3, 0.0F, x2, y2, 0.0F, x1, y1, 0.0F);
    }

    private static void putQuad(
            BufferBuilder buffer,
            Matrix4f matrix,
            float x0, float y0, float z0,
            float x1, float y1, float z1,
            float x2, float y2, float z2,
            float x3, float y3, float z3)
    {
        buffer.vertex(matrix, x0, y0, z0).uv(0.0F, 0.0F).color(255, 255, 255, 255).endVertex();
        buffer.vertex(matrix, x1, y1, z1).uv(1.0F, 0.0F).color(255, 255, 255, 255).endVertex();
        buffer.vertex(matrix, x2, y2, z2).uv(1.0F, 1.0F).color(255, 255, 255, 255).endVertex();
        buffer.vertex(matrix, x3, y3, z3).uv(0.0F, 1.0F).color(255, 255, 255, 255).endVertex();
    }
}
