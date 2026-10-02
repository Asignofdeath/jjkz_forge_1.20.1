package com.darkness.jujutsukaisenzone.entity.util;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/**
 * 特效技能可复用的「按区域遍历方块」工具。
 * <p>
 * 只统一「包围盒范围 + 形状相交判定 + 三层遍历」这段各处逐字重复的骨架；
 * 具体过滤（越界 / 空气 / 硬度）与处理（setBlock / destroyBlock / 是否掉落）仍由
 * 调用方在 {@link Visitor} 内自行决定，以保证与既有技能行为逐字一致。
 */
public final class BlockErase
{
    private BlockErase() {}

    /** 方块访问器；返回 {@code true} 表示提前结束整段遍历。 */
    @FunctionalInterface
    public interface Visitor
    {
        boolean visit(Level level, BlockPos pos);
    }

    /**
     * 遍历 {@code shape} 包围盒内、且与该形状相交的所有方块坐标（仅服务端调用）。
     * <p>
     * 位置以可变 {@link BlockPos} 复用传入，调用方须在回调内立即使用，不得跨 tick 持有。
     */
    public static void forEach(Level level, AreaShape shape, Visitor visitor)
    {
        AABB box = shape.bounds();
        int minX = Mth.floor(box.minX);
        int minY = Mth.floor(box.minY);
        int minZ = Mth.floor(box.minZ);
        int maxX = Mth.floor(box.maxX);
        int maxY = Mth.floor(box.maxY);
        int maxZ = Mth.floor(box.maxZ);

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        outer:
        for (int x = minX; x <= maxX; x++)
        {
            for (int y = minY; y <= maxY; y++)
            {
                for (int z = minZ; z <= maxZ; z++)
                {
                    if (!shape.intersectsBlock(x, y, z))
                    {
                        continue;
                    }
                    pos.set(x, y, z);
                    if (visitor.visit(level, pos))
                    {
                        break outer;
                    }
                }
            }
        }
    }
}
