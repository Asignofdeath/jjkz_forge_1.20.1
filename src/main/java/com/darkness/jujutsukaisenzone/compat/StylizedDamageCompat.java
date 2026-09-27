package com.darkness.jujutsukaisenzone.compat;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.mojang.logging.LogUtils;
import com.stylizeddamage.common.api.StylizedDamageAPI;
import com.stylizeddamage.common.config.ConfigManager;
import com.stylizeddamage.common.selector.SelectorConfig;
import com.stylizeddamage.common.selector.SelectorEngine;
import org.slf4j.Logger;

/**
 * StylizedDamage API 调用层。仅在 {@link StylizedDamagePresence#isLoaded()} 为 true 时由调用方加载本类。
 * <p>
 * 对方 Forge 端初始化 / reload 时只把 <b>config</b> 选择器装进 {@link SelectorEngine}，
 * {@code api.selectors().register()} 只写入旁路 Map。因此注册后必须调用
 * {@link #rebuildSelectorEngine()}，用 {@link StylizedDamageAPI#buildFinalSelectorConfig} 合并后再
 * {@link StylizedDamageAPI#initialize} 换上新引擎，否则永远走默认白字。
 */
public final class StylizedDamageCompat
{
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final String STYLE_MARTIAL = "jjkz_martial";
    public static final String STYLE_TECHNIQUE = "jjkz_technique";

    /** 与数据包 message_id / {@code DamageSource#getMsgId()} 一致。 */
    public static final String MSG_MARTIAL = "jjkz.martial";
    public static final String MSG_TECHNIQUE = "jjkz.technique";

    /** 注册名形式（test 命令 / 部分文档写法）。 */
    public static final String ID_MARTIAL = "jujutsukaisenzone:martial";
    public static final String ID_TECHNIQUE = "jujutsukaisenzone:technique";

    private static boolean registered;

    private StylizedDamageCompat()
    {
    }

    /**
     * 注册样式 + 选择器，并重建对方 SelectorEngine（幂等：样式只写一次，引擎每次可重建）。
     */
    public static void registerIfNeeded()
    {
        if (!StylizedDamagePresence.isLoaded())
        {
            return;
        }
        if (!StylizedDamageAPI.isInitialized())
        {
            LOGGER.warn("[{}] StylizedDamageAPI not initialised yet; deferring style registration.",
                    JuJutsuKaisenZone.MODID);
            return;
        }

        StylizedDamageAPI api = StylizedDamageAPI.getInstance();

        if (!registered)
        {
            api.createStyle(STYLE_MARTIAL)
                    .color("#FFD54F")
                    .fontSize(1.1f)
                    .fontStyle("bold")
                    .shadow(true)
                    .outlineColor("#5D4037")
                    .register();

            api.createStyle(STYLE_TECHNIQUE)
                    .color("#42A5F5")
                    .fontSize(1.15f)
                    .fontStyle("bold")
                    .shadow(true)
                    .outlineColor("#0D47A1")
                    .suffix(" \u2726")
                    .register();

            // 只保留体术黄 / 术式蓝。复合紫、黑闪红不再注册。
            // EF 基础攻击 / 原版近战的 getMsgId 仍是 "player"（反射投影常失败），
            // 一并绑到体术黄字，避免拳击一直走默认白字。
            api.selectors()
                    .in("common")
                    .branch("common")
                    .match(MSG_TECHNIQUE, ID_TECHNIQUE).style(STYLE_TECHNIQUE)
                    .match(MSG_MARTIAL, ID_MARTIAL, "player", "minecraft:player_attack").style(STYLE_MARTIAL)
                    .register();

            registered = true;
            LOGGER.info("[{}] Registered StylizedDamage styles for narrative damage types.", JuJutsuKaisenZone.MODID);
        }

        rebuildSelectorEngine();
    }

    /**
     * 把 API 选择器合并进 config 选择器并替换正在使用的引擎。
     * 对方 reload 后引擎会丢 API 规则，需再次调用本方法（或 {@link #registerIfNeeded()}）。
     */
    public static void rebuildSelectorEngine()
    {
        if (!StylizedDamagePresence.isLoaded() || !StylizedDamageAPI.isInitialized())
        {
            return;
        }
        try
        {
            StylizedDamageAPI api = StylizedDamageAPI.getInstance();
            SelectorConfig merged = api.buildFinalSelectorConfig(
                    ConfigManager.getInstance().getConfig().selectors());
            merged.expandTags();
            SelectorEngine engine = new SelectorEngine(merged);
            StylizedDamageAPI.initialize(api.getStyleLoader(), engine);
            LOGGER.info("[{}] Rebuilt StylizedDamage SelectorEngine with API selectors.", JuJutsuKaisenZone.MODID);
        }
        catch (RuntimeException e)
        {
            LOGGER.error("[{}] Failed to rebuild StylizedDamage SelectorEngine", JuJutsuKaisenZone.MODID, e);
        }
    }

    /**
     * 应用服务端下发的联动开关。开启/关闭都不卸载样式；开启时确保引擎已合并选择器。
     */
    public static void applyServerEnabled(boolean enabled)
    {
        if (enabled)
        {
            registerIfNeeded();
        }
    }

    public static boolean isRegistered()
    {
        return registered;
    }
}
