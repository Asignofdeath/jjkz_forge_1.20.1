package com.darkness.jujutsukaisenzone.client.renderer.entity;

import com.darkness.jujutsukaisenzone.blackhole.BlackHolePhysics;
import com.darkness.jujutsukaisenzone.blackhole.BlackHoleVisualConfig;
import com.darkness.jujutsukaisenzone.entity.BlackHoleEntity;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * 黑洞_A 渲染器（移植自 Soul's Black Holes）。
 *
 * 四层几何全部用 POSITION_COLOR 顶点色，不依赖贴图：
 *  - 事件视界：细分球体，纯黑，不写颜色只写深度
 *  - 吸积盘：双面圆盘（ISCO → 40Rs），Shakura–Sunyaev 温度轮廓 →
 *    Planck 黑体色 → 多普勒增亮 → 引力红shift（Java 端逐顶点算好）
 *  - 透镜光环绕环：面向相机的 4 层环，模拟「盘面绕到黑洞背后被弯上来」
 *  - 相对论喷流：双极锥体 + 同步辐射配色
 *
 * render() 刻意为空：实际绘制走 {@code BlackHoleClientEvents} 在
 * AFTER_TRANSLUCENT_BLOCKS 阶段的 drawDeferred（需要相机矩阵）。
 */
public class BlackHoleRenderer extends EntityRenderer<BlackHoleEntity>
{
    private static final ResourceLocation TEXTURE =
            new ResourceLocation("minecraft", "textures/misc/white.png");

    /** RenderStateShard 的 shard 常量是 protected，须在子类内转公开后引用 */
    private static final class Shards extends RenderStateShard
    {
        static final RenderStateShard.ShaderStateShard POSITION_COLOR = POSITION_COLOR_SHADER;
        static final RenderStateShard.TransparencyStateShard NO_TRANSP = NO_TRANSPARENCY;
        static final RenderStateShard.TransparencyStateShard TRANSLUCENT = TRANSLUCENT_TRANSPARENCY;
        static final RenderStateShard.TransparencyStateShard LIGHTNING = LIGHTNING_TRANSPARENCY;
        static final RenderStateShard.DepthTestStateShard LEQUAL = LEQUAL_DEPTH_TEST;
        static final RenderStateShard.WriteMaskStateShard COLOR_DEPTH = COLOR_DEPTH_WRITE;
        static final RenderStateShard.WriteMaskStateShard COLOR = COLOR_WRITE;
        static final RenderStateShard.CullStateShard UNCULLED = NO_CULL;
        static final RenderStateShard.LightmapStateShard NO_LM = NO_LIGHTMAP;

        private Shards(String name, Runnable setup, Runnable clear)
        {
            super(name, setup, clear);
        }
    }

    private static final RenderType EVENT_HORIZON_TYPE = RenderType.create(
            "jujutsukaisenzone:blackhole_event_horizon",
            DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.TRIANGLES, 262144, false, false,
            RenderType.CompositeState.builder()
                    .setShaderState(Shards.POSITION_COLOR)
                    .setTransparencyState(Shards.NO_TRANSP)
                    .setDepthTestState(Shards.LEQUAL)
                    .setWriteMaskState(Shards.COLOR_DEPTH)
                    .setCullState(Shards.UNCULLED)
                    .setLightmapState(Shards.NO_LM)
                    .createCompositeState(false));

    private static final RenderType ACCRETION_DISK_TYPE = RenderType.create(
            "jujutsukaisenzone:blackhole_accretion_disk",
            DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.TRIANGLES, 524288, false, false,
            RenderType.CompositeState.builder()
                    .setShaderState(Shards.POSITION_COLOR)
                    .setTransparencyState(Shards.TRANSLUCENT)
                    .setDepthTestState(Shards.LEQUAL)
                    .setWriteMaskState(Shards.COLOR)
                    .setCullState(Shards.UNCULLED)
                    .setLightmapState(Shards.NO_LM)
                    .createCompositeState(false));

    private static final RenderType JET_TYPE = RenderType.create(
            "jujutsukaisenzone:blackhole_jet",
            DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.TRIANGLES, 262144, false, true,
            RenderType.CompositeState.builder()
                    .setShaderState(Shards.POSITION_COLOR)
                    .setTransparencyState(Shards.LIGHTNING)
                    .setDepthTestState(Shards.LEQUAL)
                    .setWriteMaskState(Shards.COLOR)
                    .setCullState(Shards.UNCULLED)
                    .setLightmapState(Shards.NO_LM)
                    .createCompositeState(false));

    private static final int SPHERE_SUBDIVISIONS = 4;
    private static final float PEAK_TEMPERATURE = 6500.0F;
    private static final float LUMINOSITY_GAIN = 4.0F;
    private static final float EXPOSURE = 1.3F;

    private float lastLobeAngle = 0.0F;

    public BlackHoleRenderer(EntityRendererProvider.Context context)
    {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(BlackHoleEntity entity)
    {
        return TEXTURE;
    }

    @Override
    public boolean shouldRender(BlackHoleEntity entity, Frustum frustum, double camX, double camY, double camZ)
    {
        // 阴影 + 吸积盘 + 喷流范围很大，交给 drawDeferred 自行决定
        return true;
    }

    @Override
    public void render(BlackHoleEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight)
    {
        // 空：由 BlackHoleClientEvents 的 AFTER_TRANSLUCENT_BLOCKS 阶段调用 drawDeferred
    }

    /** 实际绘制入口（AFTER_TRANSLUCENT_BLOCKS 阶段） */
    public void drawDeferred(BlackHoleEntity entity, float entityYaw, float partialTick,
                             PoseStack poseStack, MultiBufferSource bufferSource, int packedLight)
    {
        float schwarzschildRadius = entity.getSchwarzschildRadius();
        float shadowRadius = (float) entity.getShadowRadius();
        float age = (float) entity.tickCount + partialTick;
        float iscoRadius = (float) entity.getIscoRadius();
        boolean minecraftify = BlackHoleVisualConfig.MINECRAFTIFY;

        Vec3 cam = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        double dx = cam.x - entity.getX();
        double dy = cam.y - entity.getY();
        double dz = cam.z - entity.getZ();

        // 多普勒亮瓣方位角（跟随相机方位连续旋转，不做角度跳变）
        float rawLobe = (float) Math.atan2(dx, dz) + 1.5707964F;
        float prev = this.lastLobeAngle;
        float delta = rawLobe - prev;
        while (delta > (float) Math.PI)
        {
            delta -= (float) Math.PI * 2;
        }
        while (delta <= (float) -Math.PI)
        {
            delta += (float) Math.PI * 2;
        }
        float dopplerLobeAngle = prev + delta;
        this.lastLobeAngle = dopplerLobeAngle;

        // 相机基向量（用于面向相机的光环绕）
        Vector3f camRight = new Vector3f(1.0F, 0.0F, 0.0F);
        Vector3f camUp = new Vector3f(0.0F, 1.0F, 0.0F);
        double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        float viewTilt = 0.0F;
        if (len >= 1.0E-6)
        {
            Vector3f view = new Vector3f((float) (dx / len), (float) (dy / len), (float) (dz / len));
            viewTilt = Math.abs(view.y);
            Vector3f worldUp = new Vector3f(0.0F, 1.0F, 0.0F);
            Vector3f right = new Vector3f();
            worldUp.cross(view, right);
            if (right.lengthSquared() < 1.0E-8F)
            {
                camRight.set(1.0F, 0.0F, 0.0F);
                camUp.set(0.0F, 0.0F, 1.0F);
            }
            else
            {
                right.normalize();
                Vector3f up = new Vector3f();
                view.cross(right, up);
                up.normalize();
                camRight.set(right);
                camUp.set(up);
            }
        }

        if (BlackHoleVisualConfig.ENABLE_ACCRETION_DISK)
        {
            poseStack.pushPose();
            this.renderAccretionDisk(poseStack, bufferSource, schwarzschildRadius, iscoRadius, age, dopplerLobeAngle);
            poseStack.popPose();
            poseStack.pushPose();
            this.renderJets(poseStack, bufferSource, schwarzschildRadius, age);
            poseStack.popPose();
        }
        poseStack.pushPose();
        this.renderEventHorizon(poseStack, bufferSource, shadowRadius);
        poseStack.popPose();
        poseStack.pushPose();
        Matrix4f matrix = poseStack.last().pose();
        VertexConsumer wrapConsumer = bufferSource.getBuffer(ACCRETION_DISK_TYPE);
        this.renderLensingWrap(wrapConsumer, matrix, shadowRadius, camRight, camUp, viewTilt);
        poseStack.popPose();
    }

    private void renderEventHorizon(PoseStack poseStack, MultiBufferSource bufferSource, float shadowR)
    {
        VertexConsumer consumer = bufferSource.getBuffer(EVENT_HORIZON_TYPE);
        Matrix4f matrix = poseStack.last().pose();
        float[] verts = BlackHoleSphereBuilder.getVertices(SPHERE_SUBDIVISIONS);
        int[] indices = BlackHoleSphereBuilder.getIndices(SPHERE_SUBDIVISIONS);
        for (int i = 0; i < indices.length; i += 3)
        {
            int i0 = indices[i];
            int i1 = indices[i + 1];
            int i2 = indices[i + 2];
            vc(consumer, matrix, verts[i0 * 3] * shadowR, verts[i0 * 3 + 1] * shadowR, verts[i0 * 3 + 2] * shadowR, 0, 0, 0, 255);
            vc(consumer, matrix, verts[i1 * 3] * shadowR, verts[i1 * 3 + 1] * shadowR, verts[i1 * 3 + 2] * shadowR, 0, 0, 0, 255);
            vc(consumer, matrix, verts[i2 * 3] * shadowR, verts[i2 * 3 + 1] * shadowR, verts[i2 * 3 + 2] * shadowR, 0, 0, 0, 255);
        }
    }

    private void renderAccretionDisk(PoseStack poseStack, MultiBufferSource bufferSource,
                                     float rs, float iscoR, float time, float lobeAngle)
    {
        float outerR = rs * 40.0F;
        int segments = BlackHoleVisualConfig.ACCRETION_DISK_SEGMENTS;
        int rings = BlackHoleVisualConfig.ACCRETION_DISK_RINGS;
        VertexConsumer consumer = bufferSource.getBuffer(ACCRETION_DISK_TYPE);
        Matrix4f matrix = poseStack.last().pose();
        // 上下两个薄层，模拟有厚度的盘
        this.renderDiskLayer(consumer, matrix, rs, iscoR, outerR, segments, rings, lobeAngle, -rs * 0.06F, 1.0F, time);
        this.renderDiskLayer(consumer, matrix, rs, iscoR, outerR, segments, rings, lobeAngle, rs * 0.06F, 1.0F, time);
    }

    private void renderDiskLayer(VertexConsumer consumer, Matrix4f matrix, float rs, float innerR, float outerR,
                                 int segments, int rings, float lobeAngle, float yOffset, float brightnessScale, float time)
    {
        float iscoR = rs * 3.0F;
        for (int ring = 0; ring < rings; ring++)
        {
            float t0 = (float) ring / (float) rings;
            float t1 = (float) (ring + 1) / (float) rings;
            float r0 = innerR + (outerR - innerR) * t0;
            float r1 = innerR + (outerR - innerR) * t1;
            double TWO_PI = Math.PI * 2;
            // 开普勒角速度：omega ∝ r^(-3/2)，ISCO 处 omega = 0.126
            double k0 = Math.pow(iscoR / Math.max(r0, iscoR * 0.9F), 1.5);
            double k1 = Math.pow(iscoR / Math.max(r1, iscoR * 0.9F), 1.5);
            double r0Raw = (double) time * 0.126 * k0;
            double r1Raw = (double) time * 0.126 * k1;
            float rotPos0 = (float) (r0Raw % TWO_PI);
            float rotPos1 = (float) (r1Raw % TWO_PI);
            float rotDop0 = (float) ((r0Raw - (double) lobeAngle) % TWO_PI);
            float rotDop1 = (float) ((r1Raw - (double) lobeAngle) % TWO_PI);

            for (int seg = 0; seg < segments; seg++)
            {
                float baseA0 = (float) ((double) seg * 2.0 * Math.PI / (double) segments);
                float baseA1 = (float) ((double) (seg + 1) * 2.0 * Math.PI / (double) segments);
                float a00 = baseA0 + rotPos0;
                float a01 = baseA1 + rotPos0;
                float a10 = baseA0 + rotPos1;
                float a11 = baseA1 + rotPos1;
                float d00 = baseA0 + rotDop0;
                float d01 = baseA1 + rotDop0;
                float d10 = baseA0 + rotDop1;
                float d11 = baseA1 + rotDop1;
                float x00 = r0 * cos(a00);
                float z00 = r0 * sin(a00);
                float x01 = r0 * cos(a01);
                float z01 = r0 * sin(a01);
                float x10 = r1 * cos(a10);
                float z10 = r1 * sin(a10);
                float x11 = r1 * cos(a11);
                float z11 = r1 * sin(a11);
                float p00 = particleBrightness(baseA0, r0);
                float p01 = particleBrightness(baseA1, r0);
                float p10 = particleBrightness(baseA0, r1);
                float p11 = particleBrightness(baseA1, r1);
                int[] c00 = this.computeVertexColor(r0, rs, iscoR, outerR, d00, brightnessScale * p00);
                int[] c01 = this.computeVertexColor(r0, rs, iscoR, outerR, d01, brightnessScale * p01);
                int[] c10 = this.computeVertexColor(r1, rs, iscoR, outerR, d10, brightnessScale * p10);
                int[] c11 = this.computeVertexColor(r1, rs, iscoR, outerR, d11, brightnessScale * p11);
                float xc = 0.25F * (x00 + x01 + x10 + x11);
                float zc = 0.25F * (z00 + z01 + z10 + z11);
                int cR = c00[0] + c01[0] + c10[0] + c11[0] >> 2;
                int cG = c00[1] + c01[1] + c10[1] + c11[1] >> 2;
                int cB = c00[2] + c01[2] + c10[2] + c11[2] >> 2;
                int cA = c00[3] + c01[3] + c10[3] + c11[3] >> 2;
                vc(consumer, matrix, x00, yOffset, z00, c00);
                vc(consumer, matrix, x10, yOffset, z10, c10);
                vc(consumer, matrix, xc, yOffset, zc, cR, cG, cB, cA);
                vc(consumer, matrix, x10, yOffset, z10, c10);
                vc(consumer, matrix, x11, yOffset, z11, c11);
                vc(consumer, matrix, xc, yOffset, zc, cR, cG, cB, cA);
                vc(consumer, matrix, x11, yOffset, z11, c11);
                vc(consumer, matrix, x01, yOffset, z01, c01);
                vc(consumer, matrix, xc, yOffset, zc, cR, cG, cB, cA);
                vc(consumer, matrix, x01, yOffset, z01, c01);
                vc(consumer, matrix, x00, yOffset, z00, c00);
                vc(consumer, matrix, xc, yOffset, zc, cR, cG, cB, cA);
            }
        }
    }

    private static float particleBrightness(float baseAngle, float radius)
    {
        return 1.0F + 0.015F * (float) Math.sin(baseAngle);
    }

    /** Shakura–Sunyaev 温度轮廓 → 引力红shift → 多普勒增亮 → 黑体色 → 亮度压缩 */
    private int[] computeVertexColor(float r, float rs, float iscoR, float outerR, float dopplerAngle, float brightnessScale)
    {
        float ssProfile;
        float rRatio = r / iscoR;
        if (r <= iscoR)
        {
            ssProfile = 0.0F;
        }
        else
        {
            float radialFalloff = (float) Math.pow(rRatio, -0.75);
            float innerBoundary = (float) Math.pow(1.0 - Math.sqrt(iscoR / r), 0.25);
            ssProfile = radialFalloff * innerBoundary;
        }
        float emittedTemp = PEAK_TEMPERATURE * ssProfile;
        float redshift = Math.min((float) BlackHolePhysics.gravitationalRedshift(rs, r), 10.0F);
        float redshiftedTemp = emittedTemp / (1.0F + redshift);
        float physicalBeta = (float) Math.sqrt((double) rs / (2.0 * (double) Math.max(r, rs)));
        float beta = Math.min(0.7F, physicalBeta * 1.2F);
        float gamma = (float) (1.0 / Math.sqrt(1.0 - (double) (beta * beta)));
        float cosAngle = (float) Math.cos(dopplerAngle);
        float dopplerD = 1.0F / (gamma * (1.0F - beta * cosAngle));
        float tempShift = (float) Math.pow(dopplerD, 1.3);
        float observedTemp = redshiftedTemp * tempShift;
        float intensityBoost = dopplerD * dopplerD;
        float[] rgb = observedTemp < 800.0F ? new float[] {0.0F, 0.0F, 0.0F} : BlackHolePhysics.blackBodyColor(observedTemp);
        float luminosity = ssProfile * intensityBoost * brightnessScale * LUMINOSITY_GAIN;
        float lumIn = rgb[0] * 0.2126F + rgb[1] * 0.7152F + rgb[2] * 0.0722F;
        float scaledLum = lumIn * luminosity * EXPOSURE;
        float lumOut = scaledLum / (1.0F + scaledLum);
        float scale = lumIn > 1.0E-4F ? lumOut / lumIn : 0.0F;
        float finalR = Math.min(1.0F, rgb[0] * scale);
        float finalG = Math.min(1.0F, rgb[1] * scale);
        float finalB = Math.min(1.0F, rgb[2] * scale);
        float radialPos = (r - iscoR) / (outerR - iscoR);
        float innerFade = smoothstep(0.0F, 0.01F, radialPos);
        float outerFade = 1.0F - smoothstep(0.65F, 1.0F, radialPos);
        float density = innerFade * outerFade;
        float lumWeight = Math.max(0.45F, Math.min(1.0F, lumOut + 0.2F));
        int alpha = (int) Math.max(0.0F, Math.min(245.0F, density * lumWeight * 245.0F));
        return new int[] {(int) (finalR * 255.0F), (int) (finalG * 255.0F), (int) (finalB * 255.0F), alpha};
    }

    /**
     * 透镜光环绕环：阴影边缘一圈全亮 + 俯仰角越贴边侧越亮的 3 层外环，
     * 模拟吸积盘绕到黑洞背面的部分被弯上来。
     */
    private void renderLensingWrap(VertexConsumer consumer, Matrix4f matrix, float shadowR,
                                   Vector3f right, Vector3f up, float viewTilt)
    {
        int segments = 72;
        float edgeOnness = 1.0F - viewTilt;
        float wrapFade = smoothstep(0.05F, 0.4F, edgeOnness);
        this.renderCameraFacingRing(consumer, matrix, right, up, shadowR * 1.0F, shadowR * 1.09F, segments, 10500.0F, 6.0F, 255);
        if (wrapFade <= 0.01F)
        {
            return;
        }
        this.renderCameraFacingRing(consumer, matrix, right, up, shadowR * 1.09F, shadowR * 1.22F, segments, 7500.0F, 3.0F, (int) (160.0F * wrapFade));
        this.renderCameraFacingRing(consumer, matrix, right, up, shadowR * 1.22F, shadowR * 1.38F, segments, 6000.0F, 1.8F, (int) (110.0F * wrapFade));
        this.renderCameraFacingRing(consumer, matrix, right, up, shadowR * 1.38F, shadowR * 1.5F, segments, 4800.0F, 1.0F, (int) (55.0F * wrapFade));
    }

    private void renderCameraFacingRing(VertexConsumer consumer, Matrix4f matrix,
                                        Vector3f right, Vector3f up, float innerR, float outerR,
                                        int segments, float tempKelvin, float lumGain, int alphaCap)
    {
        for (int i = 0; i < segments; i++)
        {
            float a0 = (float) ((double) i * 2.0 * Math.PI / (double) segments);
            float a1 = (float) ((double) (i + 1) * 2.0 * Math.PI / (double) segments);
            float c0 = cos(a0);
            float s0 = sin(a0);
            float c1 = cos(a1);
            float s1 = sin(a1);
            float ix0 = right.x * innerR * c0 + up.x * innerR * s0;
            float iy0 = right.y * innerR * c0 + up.y * innerR * s0;
            float iz0 = right.z * innerR * c0 + up.z * innerR * s0;
            float ix1 = right.x * innerR * c1 + up.x * innerR * s1;
            float iy1 = right.y * innerR * c1 + up.y * innerR * s1;
            float iz1 = right.z * innerR * c1 + up.z * innerR * s1;
            float ox0 = right.x * outerR * c0 + up.x * outerR * s0;
            float oy0 = right.y * outerR * c0 + up.y * outerR * s0;
            float oz0 = right.z * outerR * c0 + up.z * outerR * s0;
            float ox1 = right.x * outerR * c1 + up.x * outerR * s1;
            float oy1 = right.y * outerR * c1 + up.y * outerR * s1;
            float oz1 = right.z * outerR * c1 + up.z * outerR * s1;
            int[] col0 = this.wrapColor(c0, tempKelvin, lumGain, alphaCap);
            int[] col1 = this.wrapColor(c1, tempKelvin, lumGain, alphaCap);
            vc(consumer, matrix, ix0, iy0, iz0, col0);
            vc(consumer, matrix, ox0, oy0, oz0, col0);
            vc(consumer, matrix, ix1, iy1, iz1, col1);
            vc(consumer, matrix, ix1, iy1, iz1, col1);
            vc(consumer, matrix, ox0, oy0, oz0, col0);
            vc(consumer, matrix, ox1, oy1, oz1, col1);
        }
    }

    private int[] wrapColor(float cosAngle, float tempKelvin, float lumGain, int alphaCap)
    {
        float beta = 0.72F;
        float gamma = (float) (1.0 / Math.sqrt(1.0 - (double) (beta * beta)));
        float dopplerD = 1.0F / (gamma * (1.0F - beta * cosAngle));
        float observedTemp = tempKelvin * (float) Math.pow(dopplerD, 1.8);
        float[] rgb = BlackHolePhysics.blackBodyColor(observedTemp);
        float intensityBoost = dopplerD * dopplerD * dopplerD * dopplerD;
        float lumIn = rgb[0] * 0.2126F + rgb[1] * 0.7152F + rgb[2] * 0.0722F;
        float scaled = lumIn * intensityBoost * lumGain;
        float lumOut = scaled / (1.0F + scaled);
        float scale = lumIn > 1.0E-4F ? lumOut / lumIn : 0.0F;
        int r = (int) Math.min(255.0F, rgb[0] * scale * 255.0F);
        int g = (int) Math.min(255.0F, rgb[1] * scale * 255.0F);
        int b = (int) Math.min(255.0F, rgb[2] * scale * 255.0F);
        int a = (int) Math.max(0.0F, Math.min((float) alphaCap, lumOut * (float) alphaCap));
        return new int[] {r, g, b, a};
    }

    private void renderJets(PoseStack poseStack, MultiBufferSource bufferSource, float rs, float age)
    {
        float jetLength = rs * 16.0F;
        float baseY = rs * 1.2F;
        int strips = 20;
        VertexConsumer consumer = bufferSource.getBuffer(JET_TYPE);
        Matrix4f matrix = poseStack.last().pose();
        float jetBaseOmega = 0.126F;
        float jetBaseTwist = age * jetBaseOmega;
        float[][] layers = new float[][] {{1.8F, 0.35F}, {0.55F, 1.0F}};
        float tAge = age;
        for (float[] layer : layers)
        {
            float halfW = rs * layer[0];
            float bScale = layer[1];
            for (int side = -1; side <= 1; side += 2)
            {
                for (int plane = 0; plane < 2; plane++)
                {
                    for (int i = 0; i < strips; i++)
                    {
                        float t0 = (float) i / (float) strips;
                        float t1 = (float) (i + 1) / (float) strips;
                        float y0 = (float) side * (baseY + t0 * jetLength);
                        float y1 = (float) side * (baseY + t1 * jetLength);
                        float puff0 = 1.0F + 0.5F * t0 * (float) Math.sin(tAge * 0.18F + t0 * 9.0F + (float) side * 1.3F + (float) plane * 1.9F)
                                + 0.25F * t0 * (float) Math.sin(tAge * 0.41F + t0 * 17.0F);
                        float puff1 = 1.0F + 0.5F * t1 * (float) Math.sin(tAge * 0.18F + t1 * 9.0F + (float) side * 1.3F + (float) plane * 1.9F)
                                + 0.25F * t1 * (float) Math.sin(tAge * 0.41F + t1 * 17.0F);
                        float w0 = halfW * puff0;
                        float w1 = halfW * puff1;
                        float kinkX0 = rs * 0.35F * t0 * (float) Math.sin(tAge * 0.15F + t0 * 5.0F + (float) side * 2.1F);
                        float kinkZ0 = rs * 0.35F * t0 * (float) Math.cos(tAge * 0.11F + t0 * 4.0F + (float) side * 1.7F);
                        float kinkX1 = rs * 0.35F * t1 * (float) Math.sin(tAge * 0.15F + t1 * 5.0F + (float) side * 2.1F);
                        float kinkZ1 = rs * 0.35F * t1 * (float) Math.cos(tAge * 0.11F + t1 * 4.0F + (float) side * 1.7F);
                        float flick0 = 0.6F + 0.4F * (float) Math.sin(tAge * 0.22F + t0 * 6.0F + (float) plane) * (float) Math.cos(tAge * 0.09F + t0 * 3.0F + (float) side);
                        float flick1 = 0.6F + 0.4F * (float) Math.sin(tAge * 0.22F + t1 * 6.0F + (float) plane) * (float) Math.cos(tAge * 0.09F + t1 * 3.0F + (float) side);
                        int[] c0 = this.jetColor(t0, bScale * flick0);
                        int[] c1 = this.jetColor(t1, bScale * flick1);
                        float ax0;
                        float az0;
                        float ax1;
                        float az1;
                        if (plane == 0)
                        {
                            ax0 = w0;
                            az0 = 0.0F;
                            ax1 = w1;
                            az1 = 0.0F;
                        }
                        else
                        {
                            ax0 = 0.0F;
                            az0 = w0;
                            ax1 = 0.0F;
                            az1 = w1;
                        }
                        vc(consumer, matrix, -ax0 + kinkX0, y0, -az0 + kinkZ0, c0);
                        vc(consumer, matrix, ax0 + kinkX0, y0, az0 + kinkZ0, c0);
                        vc(consumer, matrix, ax1 + kinkX1, y1, az1 + kinkZ1, c1);
                        vc(consumer, matrix, -ax0 + kinkX0, y0, -az0 + kinkZ0, c0);
                        vc(consumer, matrix, ax1 + kinkX1, y1, az1 + kinkZ1, c1);
                        vc(consumer, matrix, -ax1 + kinkX1, y1, -az1 + kinkZ1, c1);
                    }
                }
            }
        }
    }

    private int[] jetColor(float t, float brightnessScale)
    {
        float temp = 11000.0F - t * 6500.0F;
        float[] rgb = BlackHolePhysics.blackBodyColor(temp);
        float envelope = smoothstep(0.0F, 0.1F, t) * (1.0F - smoothstep(0.6F, 1.0F, t));
        float intensity = envelope * brightnessScale;
        float lumIn = rgb[0] * 0.2126F + rgb[1] * 0.7152F + rgb[2] * 0.0722F;
        float scaled = lumIn * intensity * 3.0F;
        float lumOut = scaled / (1.0F + scaled);
        float s = lumIn > 1.0E-4F ? lumOut / lumIn : 0.0F;
        int r = (int) Math.min(255.0F, rgb[0] * s * 255.0F * 1.3F);
        int g = (int) Math.min(255.0F, rgb[1] * s * 255.0F * 1.3F);
        int b = (int) Math.min(255.0F, rgb[2] * s * 255.0F * 1.3F);
        int a = (int) Math.min(220.0F, lumOut * 220.0F);
        return new int[] {r, g, b, a};
    }

    private static void vc(VertexConsumer c, Matrix4f m, float x, float y, float z, int[] col)
    {
        c.vertex(m, x, y, z).color(col[0], col[1], col[2], col[3]).endVertex();
    }

    private static void vc(VertexConsumer c, Matrix4f m, float x, float y, float z, int r, int g, int b, int a)
    {
        c.vertex(m, x, y, z).color(r, g, b, a).endVertex();
    }

    private static float cos(float angle)
    {
        return (float) Math.cos(angle);
    }

    private static float sin(float angle)
    {
        return (float) Math.sin(angle);
    }

    private static float smoothstep(float edge0, float edge1, float x)
    {
        float t = Math.max(0.0F, Math.min(1.0F, (x - edge0) / (edge1 - edge0)));
        return t * t * (3.0F - 2.0F * t);
    }
}
