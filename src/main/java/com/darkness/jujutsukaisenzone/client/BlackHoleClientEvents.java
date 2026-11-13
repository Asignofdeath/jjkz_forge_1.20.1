package com.darkness.jujutsukaisenzone.client;

import com.darkness.jujutsukaisenzone.blackhole.BlackHoleVisualConfig;
import com.darkness.jujutsukaisenzone.blackhole.LensingPostProcessor;
import com.darkness.jujutsukaisenzone.client.renderer.entity.BlackHoleRenderer;
import com.darkness.jujutsukaisenzone.entity.BlackHoleEntity;
import com.darkness.jujutsukaisenzone.registry.ModEntities;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.Comparator;
import java.util.List;

/**
 * 黑洞_A 客户端渲染事件：
 *  - AFTER_TRANSLUCENT_BLOCKS：延迟绘制事件视界 / 吸积盘 / 喷流 / 光子环绕
 *  - AFTER_WEATHER：全屏引力透镜后处理
 */
@Mod.EventBusSubscriber(modid = com.darkness.jujutsukaisenzone.JuJutsuKaisenZone.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class BlackHoleClientEvents
{
    private static final LensingPostProcessor LENSING_PROCESSOR = new LensingPostProcessor();
    private static final double SEARCH_RANGE = 1024.0;

    private BlackHoleClientEvents()
    {
    }

    @SubscribeEvent
    public static void onDrawBlackHoles(RenderLevelStageEvent event)
    {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS)
        {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null)
        {
            return;
        }
        net.minecraft.client.renderer.entity.EntityRenderer<?> raw = mc.getEntityRenderDispatcher().getRenderer(
                new BlackHoleEntity(ModEntities.BLACK_HOLE_A.get(), mc.level));
        if (!(raw instanceof BlackHoleRenderer))
        {
            return;
        }
        BlackHoleRenderer bhr = (BlackHoleRenderer) raw;
        Vec3 camPos = event.getCamera().getPosition();
        float partialTick = event.getPartialTick();
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        for (Entity e : mc.level.entitiesForRendering())
        {
            if (!(e instanceof BlackHoleEntity))
            {
                continue;
            }
            BlackHoleEntity bh = (BlackHoleEntity) e;
            double lx = Mth.lerp(partialTick, bh.xOld, bh.getX());
            double ly = Mth.lerp(partialTick, bh.yOld, bh.getY());
            double lz = Mth.lerp(partialTick, bh.zOld, bh.getZ());
            poseStack.pushPose();
            poseStack.translate(lx - camPos.x, ly - camPos.y, lz - camPos.z);
            bhr.drawDeferred(bh, 0.0F, partialTick, poseStack, buffers, 0xF000F0);
            poseStack.popPose();
        }
        buffers.endBatch();
    }

    @SubscribeEvent
    public static void onApplyLensing(RenderLevelStageEvent event)
    {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_WEATHER)
        {
            return;
        }
        if (!BlackHoleVisualConfig.ENABLE_LENSING)
        {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null)
        {
            return;
        }
        if (!LENSING_PROCESSOR.isInitialized() && !LENSING_PROCESSOR.hasFailed())
        {
            LENSING_PROCESSOR.init();
        }
        if (!LENSING_PROCESSOR.isInitialized())
        {
            return;
        }

        Vec3 cameraPos = event.getCamera().getPosition();
        AABB searchArea = new AABB(
                cameraPos.x - SEARCH_RANGE, cameraPos.y - SEARCH_RANGE, cameraPos.z - SEARCH_RANGE,
                cameraPos.x + SEARCH_RANGE, cameraPos.y + SEARCH_RANGE, cameraPos.z + SEARCH_RANGE);
        List<BlackHoleEntity> blackHoles = mc.level.getEntitiesOfClass(BlackHoleEntity.class, searchArea);
        if (blackHoles.isEmpty())
        {
            LENSING_PROCESSOR.setActiveBlackHoles(0);
            return;
        }
        blackHoles.sort(Comparator.comparingDouble(bh -> bh.position().distanceToSqr(cameraPos)));

        Matrix4f modelViewMat = new Matrix4f(event.getPoseStack().last().pose());
        Matrix4f projMat = new Matrix4f(event.getProjectionMatrix());
        float fov = mc.options.fov().get().floatValue();
        int activeCount = 0;
        for (BlackHoleEntity bh : blackHoles)
        {
            if (activeCount >= BlackHoleVisualConfig.MAX_LENSED_BLACK_HOLES)
            {
                break;
            }
            Vec3 bhPos = bh.position();
            float distToCamera = (float) bhPos.distanceTo(cameraPos);
            if (distToCamera > SEARCH_RANGE)
            {
                continue;
            }
            Vector3f relPos = new Vector3f(
                    (float) (bhPos.x - cameraPos.x),
                    (float) (bhPos.y - cameraPos.y),
                    (float) (bhPos.z - cameraPos.z));
            float[] screenCoords = LensingPostProcessor.projectToScreen(relPos, modelViewMat, projMat);
            if (screenCoords == null)
            {
                continue;
            }
            float screenRadius = LensingPostProcessor.screenSpaceRadius(
                    bh.getSchwarzschildRadius(), distToCamera, fov);
            LENSING_PROCESSOR.setBlackHoleData(activeCount, screenCoords[0], screenCoords[1], screenRadius, distToCamera);
            activeCount++;
        }
        LENSING_PROCESSOR.setActiveBlackHoles(activeCount);
        if (activeCount > 0)
        {
            LENSING_PROCESSOR.apply();
        }
    }
}
