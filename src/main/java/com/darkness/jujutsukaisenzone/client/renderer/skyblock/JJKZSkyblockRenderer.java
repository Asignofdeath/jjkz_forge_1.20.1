package com.darkness.jujutsukaisenzone.client.renderer.skyblock;

import com.darkness.jujutsukaisenzone.blockentity.JJKZSkyblockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.phys.Vec3;
import com.noodlegamer76.shadered.client.util.skyblock.SkyblockPass;

/**
 * 自定义天空盒方块的 BlockEntityRenderer。
 * <p>
 * 将方块渲染数据加入对应的 {@link JJKZSkyblockBatchManager} 中的 BatchData。
 */
public class JJKZSkyblockRenderer implements BlockEntityRenderer<JJKZSkyblockEntity>
{
    public JJKZSkyblockRenderer(BlockEntityRendererProvider.Context context)
    {
    }

    @Override
    public void render(JJKZSkyblockEntity entity, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer,
                       int packedLight, int packedOverlay)
    {
        /* 获取方块的纹理路径，并加入对应的 BatchData */
        var batchManager = JJKZSkyblockBatchManager.getInstance();
        var batchData = batchManager.getOrCreateBatchData(entity.getTextureFolder());

        /* 使用 NORMAL Pass 类型添加渲染数据 */
        batchData.add(
                entity.getPass(),
                entity.getBlockPos(),
                poseStack.last().pose(),
                false,
                1.0F
        );
    }

    @Override
    public boolean shouldRender(JJKZSkyblockEntity entity, Vec3 cameraPos)
    {
        /* 始终渲染自定义天空盒方块 */
        return true;
    }
}
