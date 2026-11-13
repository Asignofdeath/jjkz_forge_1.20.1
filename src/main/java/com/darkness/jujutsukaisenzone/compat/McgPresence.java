package com.darkness.jujutsukaisenzone.compat;

import net.minecraftforge.fml.ModList;

/**
 * 检测 PPs Dismemberment（模组 ID {@code mcg}，显示名 Mob Carnage Gore）是否已安装。
 * <p>
 * 本类不引用 mcg API，可在未安装时安全加载。
 */
public final class McgPresence
{
    public static final String MOD_ID = "mcg";

    private McgPresence()
    {
    }

    public static boolean isLoaded()
    {
        return ModList.get().isLoaded(MOD_ID);
    }
}
