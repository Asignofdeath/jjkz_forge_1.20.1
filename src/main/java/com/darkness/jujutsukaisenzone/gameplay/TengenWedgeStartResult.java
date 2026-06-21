package com.darkness.jujutsukaisenzone.gameplay;

import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;

public enum TengenWedgeStartResult
{
    SUCCESS(null),
    NOT_INSIDE(Component.literal("须站在已构建完成的天元之楔结界内（setup 执行者）。")),
    STILL_EXPANDING(Component.literal("结界仍在构建中，请等待完成后再开始。")),
    ALREADY_STARTED(Component.literal("该天元之楔结界已在进行中。")),
    NOT_ENOUGH_WEDGES(Component.literal("结界内楔数量不足（需要 3 个，三阵营各一）。")),
    NOT_ENOUGH_SPAWNS(Component.literal("咒灵刷新点数量不足（至少需 "
            + TengenWedgeCurseSpiritSpawns.MIN_REQUIRED + " 个）。")),
    MISSING_FACTION_WEDGE(Component.literal("结界内缺少某一阵营的楔，无法开始。"));

    @Nullable
    private final Component failureMessage;

    TengenWedgeStartResult(@Nullable Component failureMessage)
    {
        this.failureMessage = failureMessage;
    }

    public boolean isSuccess()
    {
        return this == SUCCESS;
    }

    @Nullable
    public Component failureMessage()
    {
        return failureMessage;
    }
}
