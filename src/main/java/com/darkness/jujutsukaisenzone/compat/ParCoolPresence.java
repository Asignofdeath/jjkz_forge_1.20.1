package com.darkness.jujutsukaisenzone.compat;

import net.minecraftforge.fml.ModList;

/**
 * 检测 ParCool 与 Official Epic x ParCool 是否均已安装。
 * <p>
 * 本类不引用 ParCool API，可在未安装联动模组时安全加载。
 * 空战系统仅在 {@link #isAerialIntegrationAvailable()} 为 true 时启用。
 */
public final class ParCoolPresence
{
    private static final String MOD_PARCOOL = "parcool";
    private static final String MOD_EPIC_PARCOOL = "epicparcool";

    private ParCoolPresence()
    {
    }

    public static boolean isParCoolLoaded()
    {
        return ModList.get().isLoaded(MOD_PARCOOL);
    }

    public static boolean isEpicParCoolLoaded()
    {
        return ModList.get().isLoaded(MOD_EPIC_PARCOOL);
    }

    /** ParCool + Epic x ParCool 均已安装时，跑酷/空战联动才可用。 */
    public static boolean isAerialIntegrationAvailable()
    {
        return isParCoolLoaded() && isEpicParCoolLoaded();
    }
}
