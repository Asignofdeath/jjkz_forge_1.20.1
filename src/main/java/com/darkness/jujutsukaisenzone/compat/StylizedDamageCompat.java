package com.darkness.jujutsukaisenzone.compat;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.technique.TechniqueDamageSources;
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
    public static final String STYLE_COMPOSITE = "jjkz_composite";
    /** 暴击版：外观同基础样式，仅额外加 {@link #CRIT_SUFFIX} 后缀。 */
    public static final String STYLE_MARTIAL_CRIT = "jjkz_martial_crit";
    public static final String STYLE_TECHNIQUE_CRIT = "jjkz_technique_crit";

    /** 与数据包 message_id / {@code DamageSource#getMsgId()} 一致。 */
    public static final String MSG_MARTIAL = "jjkz.martial";
    public static final String MSG_TECHNIQUE = "jjkz.technique";
    public static final String MSG_COMPOSITE = "jjkz.composite";

    /**
     * 暴击版 message_id。<b>这里不写字面量，直接引用 {@link TechniqueDamageSources} 的同名常量</b> ——
     * 生产者（{@code TechniqueDamageSources#markCritDisplay}）与消费者（本类的选择器规则）
     * 一旦字符串不一致，表现只是「暴击没了 💥」，没有任何报错，所以不给漂移留口子。
     * <p>
     * 复合不列：{@code CritRules#canCrit} 不放行复合分型，不可能出现 {@code jjkz.composite.crit}。
     */
    public static final String MSG_MARTIAL_CRIT = TechniqueDamageSources.MSG_MARTIAL_CRIT;
    public static final String MSG_TECHNIQUE_CRIT = TechniqueDamageSources.MSG_TECHNIQUE_CRIT;

    /**
     * 加在暴击数字后的标记（{@code ✸} = U+2738）。
     * <p>
     * ⛔ <b>不能用 💥（U+1F4A5）</b>：本模组不自带字体资源，跳字走 Minecraft 默认字体，而默认字体链
     * （{@code include/unifont}）只覆盖<b>基本多文种平面</b>——辅助平面的 💥 一个字形都没有。
     * 原版对缺字的兜底是 {@code SpecialGlyphs.MISSING}（5×8 空心白框），所以用 💥 的效果是在数字后
     * 多出一个<b>方框</b>，而不是「什么都不显示」。
     * <p>
     * 想换：改这一个常量即可。Dingbats 区块已被证明能渲染（术式基础样式的 {@code ✦} U+2726 就属于
     * 该区块），备选 {@code ✹} U+2739 / {@code ✺} U+273A / {@code ❋} U+274B / {@code ⚡} U+26A1，
     * 或直接用汉字「爆」。
     * <p>
     * 想要真·爆炸图（彻底不受字体限制）只能走 {@code Style#icon}：对方用
     * {@code ResourceLocation.tryParse(icon)} 当贴图路径渲染，尺寸被拉伸成
     * {@code max(1, round(8 × fontSize))} 的正方形、颜色乘上样式色。那需要本模组自带一张 PNG 资源，
     * 代价比一个常量大得多。
     */
    public static final String CRIT_SUFFIX = " \u2738";

    /** 注册名形式（test 命令 / 部分文档写法）。 */
    public static final String ID_MARTIAL = "jujutsukaisenzone:martial";
    public static final String ID_TECHNIQUE = "jujutsukaisenzone:technique";
    public static final String ID_COMPOSITE = "jujutsukaisenzone:composite";

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

            api.createStyle(STYLE_COMPOSITE)
                    .color("#AB47BC")
                    .fontSize(1.2f)
                    .fontStyle("bold")
                    .shadow(true)
                    .outlineColor("#4A148C")
                    .register();

            // 暴击版：外观与基础样式完全一致，只多一个 CRIT_SUFFIX 后缀。
            // 暴击是逐击事件，而样式是静态的 ⇒ 只有在 message_id 上区分（见 markCritDisplay）。
            api.createStyle(STYLE_MARTIAL_CRIT)
                    .color("#FFD54F")
                    .fontSize(1.1f)
                    .fontStyle("bold")
                    .shadow(true)
                    .outlineColor("#5D4037")
                    .suffix(CRIT_SUFFIX)
                    .register();

            // 术式基础样式已带 ✦（分型标记），暴击版保留它再加 CRIT_SUFFIX ⇒「数字 ✦ ✸」。
            api.createStyle(STYLE_TECHNIQUE_CRIT)
                    .color("#42A5F5")
                    .fontSize(1.15f)
                    .fontStyle("bold")
                    .shadow(true)
                    .outlineColor("#0D47A1")
                    .suffix(" \u2726" + CRIT_SUFFIX)
                    .register();

            // 体术黄 / 术式蓝 / 复合紫。红色留给以后的绝对伤害，不在此注册。
            // 未投影成功的 EF / 原版近战 getMsgId 仍是 "player"，绑到体术黄以免白字。
            // ⛔ 暴击规则必须排在基础规则【之前】：选择器是顺序匹配，「*.crit」与基础 id 虽不冲突，
            //    但把最具体的放前面可避免将来加通配规则时被抢先命中。
            api.selectors()
                    .in("common")
                    .branch("common")
                    .match(MSG_MARTIAL_CRIT, "player.crit", "minecraft:player_attack.crit").style(STYLE_MARTIAL_CRIT)
                    .match(MSG_TECHNIQUE_CRIT).style(STYLE_TECHNIQUE_CRIT)
                    .match(MSG_COMPOSITE, ID_COMPOSITE).style(STYLE_COMPOSITE)
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
