package com.darkness.jujutsukaisenzone.compat;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.blockentity.JJKZSkyblockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * 天空盒方块兼容层。
 * <p>
 * 封装对 JJKZ 自定义天空盒方块（jjkz_skyblock）的操作。
 * 所有天空盒效果统一使用 jjkz_skyblock，通过 ResourceLocation 设置纹理路径。
 */
public final class ShaderedCompat
{
    public static final String SHADERED_MODID = "shadered";

    /** Shadered 是否已加载的缓存（保留用于 isLoaded 检测） */
    private static Boolean loaded = null;

    private ShaderedCompat()
    {
    }

    /**
     * 检测 Shadered 模组是否已加载。
     * <p>
     * 结果会被缓存，避免重复查找。
     *
     * @return true 表示 Shadered 已安装
     */
    public static boolean isLoaded()
    {
        if (loaded == null)
        {
            loaded = net.minecraftforge.fml.ModList.get().isLoaded(SHADERED_MODID);
        }
        return loaded;
    }

    /**
     * 判断给定的 BlockState 是否为天空盒方块（包括自定义天空盒和 Shadered 天空盒）。
     *
     * @param state 方块状态
     * @return true 表示该方块是天空盒方块
     */
    public static boolean isSkyboxBlock(BlockState state)
    {
        if (isCustomSkyboxBlock(state))
        {
            return true;
        }
        if (!isLoaded())
        {
            return false;
        }
        ResourceLocation blockKey = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        if (blockKey == null)
        {
            return false;
        }
        if (!blockKey.getNamespace().equals(SHADERED_MODID))
        {
            return false;
        }
        /* 排除非天空盒方块（如 darkness_block、sky_emitter、space_compressor、render_tester） */
        String path = blockKey.getPath();
        return !path.equals("darkness_block")
                && !path.equals("sky_emitter")
                && !path.equals("space_compressor")
                && !path.equals("render_tester");
    }

    /**
     * 判断给定的 BlockState 是否为自定义天空盒方块（JJKZ 天空盒）。
     *
     * @param state 方块状态
     * @return true 表示该方块是 JJKZ 天空盒方块
     */
    public static boolean isCustomSkyboxBlock(BlockState state)
    {
        ResourceLocation blockKey = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        if (blockKey == null)
        {
            return false;
        }
        return blockKey.getNamespace().equals(JuJutsuKaisenZone.MODID)
                && blockKey.getPath().equals("jjkz_skyblock");
    }

    /**
     * 将领域地板层的领域方块替换为自定义天空盒方块。
     *
     * @param level           服务端世界
     * @param floorPositions  地板方块位置列表
     * @param domainBlock     领域方块（用于匹配需要替换的方块）
     * @param textureFolder   纹理文件夹路径
     * @param removedBlocks   被替换方块的记录（用于恢复）
     * @return 实际放置的自定义天空盒方块位置列表
     */
    public static List<BlockPos> placeCustomSkyboxFloor(ServerLevel level, List<BlockPos> floorPositions,
                                                        Block domainBlock, ResourceLocation textureFolder,
                                                        java.util.Map<BlockPos, BlockState> removedBlocks)
    {
        List<BlockPos> placedPositions = new ArrayList<>();

        /* 获取自定义天空盒方块状态 */
        var jjkzSkyblock = ForgeRegistries.BLOCKS.getValue(
                new ResourceLocation(JuJutsuKaisenZone.MODID, "jjkz_skyblock")
        );
        if (jjkzSkyblock == null)
        {
            return placedPositions;
        }
        BlockState customSkyboxState = jjkzSkyblock.defaultBlockState();

        for (BlockPos pos : floorPositions)
        {
            BlockState existing = level.getBlockState(pos);
            /* 只替换领域方块 */
            if (!existing.is(domainBlock))
            {
                continue;
            }

            /* 原地形若尚未登记，不要把施工中的领域方块记成「原地形」 */
            removedBlocks.putIfAbsent(pos.immutable(), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
            level.setBlock(pos, customSkyboxState, 3);

            /* 设置 BlockEntity 的纹理路径 */
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof JJKZSkyblockEntity jjkzBe)
            {
                jjkzBe.setTextureFolder(textureFolder);
            }

            placedPositions.add(pos.immutable());
        }

        return placedPositions;
    }

    /**
     * 在结界外壳内侧放置自定义天空盒方块层。
     *
     * @param level          服务端世界
     * @param shellBlocks    球壳方块位置列表
     * @param center         球心位置
     * @param radius         球壳半径
     * @param textureFolder  纹理文件夹路径
     * @param removedBlocks  被替换方块的记录（用于恢复），传入 null 则不记录
     * @return 实际放置的自定义天空盒方块位置列表
     */
    public static List<BlockPos> placeCustomSkyboxLayer(ServerLevel level, List<BlockPos> shellBlocks,
                                                        BlockPos center, int radius,
                                                        ResourceLocation textureFolder,
                                                        @Nullable java.util.Map<BlockPos, BlockState> removedBlocks)
    {
        List<BlockPos> placedPositions = new ArrayList<>();

        /* 获取自定义天空盒方块状态 */
        var jjkzSkyblock = ForgeRegistries.BLOCKS.getValue(
                new ResourceLocation(JuJutsuKaisenZone.MODID, "jjkz_skyblock")
        );
        if (jjkzSkyblock == null)
        {
            return placedPositions;
        }
        BlockState customSkyboxState = jjkzSkyblock.defaultBlockState();

        for (BlockPos shellPos : shellBlocks)
        {
            /* 从球壳位置向球心方向偏移 1 格 */
            int dx = shellPos.getX() - center.getX();
            int dy = shellPos.getY() - center.getY();
            int dz = shellPos.getZ() - center.getZ();
            double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (length < 0.001)
            {
                continue;
            }
            int innerX = shellPos.getX() - (int) Math.round(dx / length);
            int innerY = shellPos.getY() - (int) Math.round(dy / length);
            int innerZ = shellPos.getZ() - (int) Math.round(dz / length);
            BlockPos innerPos = new BlockPos(innerX, innerY, innerZ);

            /* 跳过与球壳方块重叠的位置 */
            if (shellBlocks.contains(innerPos))
            {
                continue;
            }

            BlockState existingState = level.getBlockState(innerPos);

            /* 跳过已有天空盒方块 */
            if (isSkyboxBlock(existingState))
            {
                continue;
            }

            /* 记录被替换的方块：领域壳等同施工临时方块，不能记成原地形 */
            if (removedBlocks != null && !existingState.isAir())
            {
                ResourceLocation existingKey = ForgeRegistries.BLOCKS.getKey(existingState.getBlock());
                boolean isDomainBlock = existingKey != null
                        && existingKey.getNamespace().equals(JuJutsuKaisenZone.MODID)
                        && existingKey.getPath().equals("domain_block");
                if (!isDomainBlock && !isSkyboxBlock(existingState))
                {
                    removedBlocks.putIfAbsent(innerPos.immutable(), existingState);
                }
                else
                {
                    removedBlocks.putIfAbsent(innerPos.immutable(),
                            net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
                }
            }

            level.setBlock(innerPos, customSkyboxState, 3);

            /* 设置 BlockEntity 的纹理路径 */
            BlockEntity be = level.getBlockEntity(innerPos);
            if (be instanceof JJKZSkyblockEntity jjkzBe)
            {
                jjkzBe.setTextureFolder(textureFolder);
            }

            placedPositions.add(innerPos);
        }

        return placedPositions;
    }

    /**
     * 移除指定位置列表中的天空盒方块。
     * <p>
     * 仅移除属于天空盒方块的方块，其他方块不受影响。
     * 如果 removedBlocks 中有该位置的原方块记录，则恢复原方块；否则设为空气。
     *
     * @param level           服务端世界
     * @param skyboxPositions 天空盒方块位置列表
     * @param removedBlocks   被替换方块的记录（用于恢复），传入 null 则直接设为空气
     */
    public static void removeSkyboxLayer(ServerLevel level, List<BlockPos> skyboxPositions,
                                          @Nullable java.util.Map<BlockPos, BlockState> removedBlocks)
    {
        for (BlockPos pos : skyboxPositions)
        {
            BlockState state = level.getBlockState(pos);
            if (!isSkyboxBlock(state))
            {
                continue;
            }

            if (removedBlocks != null && removedBlocks.containsKey(pos))
            {
                level.setBlock(pos, removedBlocks.get(pos), 3);
            }
            else
            {
                level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
            }
        }
    }
}
