package com.darkness.jujutsukaisenzone.gameplay;

import com.darkness.jujutsukaisenzone.compat.ParCoolPresence;
import com.google.common.collect.ImmutableList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

/**
 * 模组全局规则（服务端全服生效，不针对单个玩家）。
 */
public final class ModRules
{
    private ModRules()
    {
    }

    /**
     * 指令/UI 仍称「空战」，实质为<strong>物理移动</strong>全服开关（非广义空战总开关）。
     * <p>
     * 原设计为 ParCool 驱动的惯性/身法类物理移动；实现难度大，该方向已暂停。
     * 当前战斗中的基础空战（如术式内暂时克服重力）在各自逻辑中实现，<strong>不依赖</strong> {@link #isEnabled()}。
     * <p>
     * 开启时仍要求服务端安装 ParCool + Official Epic x ParCool，供 {@link com.darkness.jujutsukaisenzone.compat.ParCoolAerialBridge}
     * 等物理移动联动将来恢复时使用。
     */
    public static final class AerialCombat
    {
        private static final String MOD_PARCOOL = "parcool";
        private static final String MOD_EPIC_PARCOOL = "epicparcool";

        private static volatile boolean enabled;

        private AerialCombat()
        {
        }

        public static boolean isEnabled()
        {
            return enabled;
        }

        /**
         * 尝试设置物理移动开关（指令文案仍为「空战」）。开启时要求服务端已安装 ParCool 与 Official Epic x ParCool。
         *
         * @return 失败原因；{@code null} 表示成功
         */
        @Nullable
        public static String trySetEnabled(boolean value)
        {
            if (value && !ParCoolPresence.isAerialIntegrationAvailable())
            {
                return "服务器未安装 ParCool 与 Official Epic x ParCool，无法开启空战。";
            }
            enabled = value;
            return null;
        }

        public static boolean isServerReady()
        {
            return ParCoolPresence.isAerialIntegrationAvailable();
        }

        public static boolean isParCoolOnServer()
        {
            return ParCoolPresence.isParCoolLoaded();
        }

        public static boolean isEpicParCoolOnServer()
        {
            return ParCoolPresence.isEpicParCoolLoaded();
        }

        /** 查询玩家客户端是否安装物理移动（ParCool 联动）所需模组。 */
        public static boolean hasPlayerMods(ServerPlayer player)
        {
            return hasMod(player, MOD_PARCOOL) && hasMod(player, MOD_EPIC_PARCOOL);
        }

        public static boolean hasPlayerParCool(ServerPlayer player)
        {
            return hasMod(player, MOD_PARCOOL);
        }

        public static boolean hasPlayerEpicParCool(ServerPlayer player)
        {
            return hasMod(player, MOD_EPIC_PARCOOL);
        }

        private static boolean hasMod(ServerPlayer player, String modId)
        {
            var connectionData = NetworkHooks.getConnectionData(player.connection.connection);
            if (connectionData == null)
            {
                return false;
            }
            ImmutableList<String> modList = connectionData.getModList();
            return modList != null && modList.contains(modId);
        }

        public static String serverStatusLine()
        {
            return "服务端 ParCool：" + yesNo(isParCoolOnServer())
                    + "，Official Epic x ParCool：" + yesNo(isEpicParCoolOnServer());
        }

        public static String playerStatusLine(ServerPlayer player)
        {
            return "客户端 ParCool：" + yesNo(hasPlayerParCool(player))
                    + "，Official Epic x ParCool：" + yesNo(hasPlayerEpicParCool(player));
        }

        private static String yesNo(boolean value)
        {
            return value ? "已安装" : "未安装";
        }
    }

    /** 术式技能无冷却（全服规则，作弊向）。 */
    public static final class SkillNoCooldown
    {
        private static volatile boolean enabled;

        private SkillNoCooldown()
        {
        }

        public static boolean isEnabled()
        {
            return enabled;
        }

        public static void setEnabled(boolean value)
        {
            enabled = value;
        }
    }

    /** 全局饱食度消耗倍率（全服规则）。 */
    public static final class HungerConsumption
    {
        public enum Mode
        {
            NONE(0.0f, "不消耗"),
            LOW(0.1f, "0.1倍"),
            VANILLA(1.0f, "原版");

            private final float multiplier;
            private final String zhLabel;

            Mode(float multiplier, String zhLabel)
            {
                this.multiplier = multiplier;
                this.zhLabel = zhLabel;
            }

            public float multiplier()
            {
                return multiplier;
            }

            public String zhLabel()
            {
                return zhLabel;
            }
        }

        private static volatile Mode mode = Mode.LOW;

        private HungerConsumption()
        {
        }

        public static Mode getMode()
        {
            return mode;
        }

        public static void setMode(Mode value)
        {
            mode = value == null ? Mode.LOW : value;
        }
    }
}
