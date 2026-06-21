package com.darkness.jujutsukaisenzone.client.renderer.skyblock;

import com.noodlegamer76.shadered.client.util.skyblock.SkyblockBatchData;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 自定义天空盒 BatchData 管理器。
 * <p>
 * 管理不同纹理路径对应的 {@link SkyblockBatchData} 实例。
 * 每个纹理路径对应一个独立的 BatchData，用于在渲染时区分不同纹理类型的天空盒方块。
 */
public final class JJKZSkyblockBatchManager
{
    /** 单例实例 */
    private static final JJKZSkyblockBatchManager INSTANCE = new JJKZSkyblockBatchManager();

    /** 纹理路径 → SkyblockBatchData 的映射 */
    private final Map<ResourceLocation, SkyblockBatchData> batchDataMap = new ConcurrentHashMap<>();

    private JJKZSkyblockBatchManager()
    {
    }

    public static JJKZSkyblockBatchManager getInstance()
    {
        return INSTANCE;
    }

    /**
     * 获取指定纹理路径对应的 SkyblockBatchData，如果不存在则创建。
     *
     * @param textureFolder 纹理文件夹路径
     * @return 对应的 SkyblockBatchData 实例
     */
    public SkyblockBatchData getOrCreateBatchData(ResourceLocation textureFolder)
    {
        return batchDataMap.computeIfAbsent(textureFolder, k -> new SkyblockBatchData());
    }

    /**
     * 获取所有已注册的纹理路径和对应的 BatchData。
     *
     * @return 纹理路径 → SkyblockBatchData 的映射视图
     */
    public Map<ResourceLocation, SkyblockBatchData> getAllBatchData()
    {
        return batchDataMap;
    }

    /**
     * 清空所有 BatchData。
     * <p>
     * 应在每帧渲染完成后调用。
     */
    public void clearAll()
    {
        for (SkyblockBatchData data : batchDataMap.values())
        {
            data.clear();
        }
    }
}
