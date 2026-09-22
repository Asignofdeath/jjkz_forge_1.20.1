package com.darkness.jujutsukaisenzone.player;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * 一条「星星标记」记录：被标记的目标 + 打在上面的那颗星。
 * <p>
 * 「目标唯一」：方块按维度 + 坐标判定，实体按维度 + uuid 判定（见 {@link #sameTargetAs(StarMark)}）。
 * 同一个目标身上至多一个标记，重复标记会覆盖它原有的星；而同一颗星可以打给任意多个不同目标。
 *
 * @param pos {@link Kind#BLOCK} 为被标记的方块坐标；{@link Kind#ITEM} / {@link Kind#ENTITY}
 *            为实体最后一次被解析到的坐标（用于区分「区块未加载」与「实体已消失」）
 */
public record StarMark(Kind kind, ResourceKey<Level> dimension, BlockPos pos,
                       @Nullable UUID entityId, StarMarkType type)
{
    public enum Kind
    {
        /** 方块 */
        BLOCK,
        /** 掉落物 */
        ITEM,
        /** 实体 */
        ENTITY
    }

    /** 目标类型的中文名，用于提示。 */
    public String kindName()
    {
        return switch (kind)
        {
            case BLOCK -> "方块";
            case ITEM -> "掉落物";
            case ENTITY -> "实体";
        };
    }

    /** 是否为「会动」的目标：方块是静态锚点，不参与力学移动。 */
    public boolean mobile()
    {
        return kind != Kind.BLOCK;
    }

    /** 目标身份是否相同：方块看维度 + 坐标，实体看维度 + uuid。 */
    public boolean sameTargetAs(@Nullable StarMark other)
    {
        if (other == null || kind != other.kind || !dimension.equals(other.dimension))
        {
            return false;
        }
        if (kind == Kind.BLOCK)
        {
            return pos.equals(other.pos);
        }
        return entityId != null && entityId.equals(other.entityId);
    }

    /** 刷新实体的最后已知坐标（不改变目标身份）。 */
    public StarMark withPos(BlockPos newPos)
    {
        return new StarMark(kind, dimension, newPos, entityId, type);
    }

    /** 覆盖该目标身上的星。 */
    public StarMark withType(StarMarkType newType)
    {
        return new StarMark(kind, dimension, pos, entityId, newType);
    }

    /** 目标描述，用于动作栏提示。 */
    public String targetLabel()
    {
        return switch (kind)
        {
            case BLOCK -> "方块 (" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + ")";
            case ITEM -> "掉落物";
            case ENTITY -> "实体";
        };
    }
}
