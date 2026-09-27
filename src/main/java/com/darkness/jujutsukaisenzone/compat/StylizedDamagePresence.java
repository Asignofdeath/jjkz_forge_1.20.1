package com.darkness.jujutsukaisenzone.compat;

import net.minecraftforge.fml.ModList;

/**
 * 检测 StylizedDamage（别样的伤害跳字）是否已安装。
 * <p>
 * 本类不引用 StylizedDamage API，可在未安装时安全加载。
 */
public final class StylizedDamagePresence
{
    public static final String MOD_ID = "stylizeddamage";

    private StylizedDamagePresence()
    {
    }

    public static boolean isLoaded()
    {
        return ModList.get().isLoaded(MOD_ID);
    }
}
