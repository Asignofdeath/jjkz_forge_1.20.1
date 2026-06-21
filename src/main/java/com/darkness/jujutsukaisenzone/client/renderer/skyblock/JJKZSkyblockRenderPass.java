package com.darkness.jujutsukaisenzone.client.renderer.skyblock;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.noodlegamer76.shadered.client.renderer.ComplexPassRenderer;
import com.noodlegamer76.shadered.client.util.RenderCube;
import com.noodlegamer76.shadered.client.util.RenderableComplexPass;
import com.noodlegamer76.shadered.client.util.PassType;
import com.noodlegamer76.shadered.client.util.RenderStage;
import com.noodlegamer76.shadered.client.util.skyblock.SkyBoxRenderer;
import com.noodlegamer76.shadered.client.util.skyblock.SkyblockBatchData;
import com.noodlegamer76.shadered.client.util.skyblock.SkyblockPass;
import com.noodlegamer76.shadered.client.util.skyblock.SkyboxTranslation;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import java.util.Map;

/**
 * 自定义天空盒渲染通道。
 * <p>
 * 遍历所有已注册的纹理类型，分别渲染天空盒几何和方块。
 * 每帧渲染完成后清空所有 BatchData。
 */
public class JJKZSkyblockRenderPass implements RenderableComplexPass
{
    @Override
    public PassType getType()
    {
        /* 使用 GEOMETRY 通道类型渲染天空盒几何 */
        return PassType.GEOMETRY;
    }

    @Override
    public void render(RenderStage stage, PoseStack poseStack, int renderTick, float partialTick)
    {
        ComplexPassRenderer renderer = ComplexPassRenderer.getInstance();
        TextureTarget readTarget = renderer.getRenderBuffer();
        TextureTarget skyboxTarget = renderer.getWriteBuffer();

        JJKZSkyblockBatchManager batchManager = JJKZSkyblockBatchManager.getInstance();
        Map<ResourceLocation, SkyblockBatchData> allBatchData = batchManager.getAllBatchData();

        /* 遍历所有纹理类型，分别渲染 */
        for (Map.Entry<ResourceLocation, SkyblockBatchData> entry : allBatchData.entrySet())
        {
            ResourceLocation textureFolder = entry.getKey();
            SkyblockBatchData batchData = entry.getValue();

            /* 渲染天空盒几何 */
            skyboxTarget.bindWrite(true);
            poseStack.pushPose();

            RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
            SkyBoxRenderer.renderBlockSkybox(
                    poseStack,
                    textureFolder,
                    GameRenderer.getPositionTexColorShader(),
                    new SkyboxTranslation()
            );

            poseStack.popPose();
            readTarget.bindWrite(true);

            /* 渲染该纹理类型的所有方块 */
            for (SkyblockPass pass : SkyblockPass.values())
            {
                net.minecraft.client.renderer.ShaderInstance shader = pass.getShader();
                shader.setSampler("Skybox", skyboxTarget.getColorTextureId());
                shader.setSampler("PassDepth", readTarget.getDepthTextureId());

                RenderCube.renderSkyBlocks(
                        batchData.get(pass),
                        false,
                        shader
                );
            }
        }

        /* 本帧渲染完成，清空所有 BatchData 以准备下一帧 */
        batchManager.clearAll();
    }
}
