package com.darkness.jujutsukaisenzone.player;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * 咒力特质：精 / 粗 / 属 / 常。
 * <p>
 * 用途：
 * <ul>
 *   <li><b>玩家</b>只存一个特质（{@link PlayerCursedEnergyTraitData}）；</li>
 *   <li><b>咒物实例</b>产出时从四者中随机写入 NBT（{@code CursedObjectSeTrait}）；
 *       装 SE 时玩家特质必须与该实例吻合——避免同一 SE 完全同质，并抬高获取难度。</li>
 * </ul>
 * <p>
 * ⚠️ <b>韧性条的克制表已经删除。</b>曾经这里定义了相克环；Break 效力现在是加速破韧的唯一通道。
 * <p>
 * 名字一律走语言键（{@link #translationKey()}），不要把中文写进常量名或存档值。
 */
public enum CursedEnergyTrait
{
    /** 精：致密、克制的咒力。 */
    REFINED,
    /** 粗：暴烈、外放的咒力。 */
    COARSE,
    /** 属：带属性的咒力。 */
    ATTRIBUTED,
    /** 常：无特质。未觉醒 / 尚未指定时的默认值。 */
    NORMAL;

    private static final String LANG_PREFIX = "gui." + JuJutsuKaisenZone.MODID + ".trait.";

    /** 存档 / 命令用的稳定 id（小写）。 */
    public String id()
    {
        return name().toLowerCase(Locale.ROOT);
    }

    public String translationKey()
    {
        return LANG_PREFIX + id();
    }

    public Component displayName()
    {
        return Component.translatable(translationKey());
    }

    /**
     * 能识别就返回对应特质，识别不了（null / 空白 / 拼错）返回 {@code null}。
     * <p>
     * 与 {@link #byId} 的区别只在于「解析不出时给什么」：本方法给 {@code null}，
     * 让调用方自己决定是报错还是忽略；{@link #byId} 给 {@link #NORMAL}，适合读存档。
     * 目前 {@link #byId} 是唯一调用方，需要区分「没写」和「写了常」的新调用点请用本方法。
     */
    @Nullable
    public static CursedEnergyTrait find(String id)
    {
        if (id == null || id.isBlank())
        {
            return null;
        }
        String normalized = id.trim().toLowerCase(Locale.ROOT);
        for (CursedEnergyTrait trait : values())
        {
            if (trait.id().equals(normalized))
            {
                return trait;
            }
        }
        return null;
    }

    /**
     * 解析存档或网络传来的 id。null / 空白 / 无法识别的值一律回落到 {@link #NORMAL}。
     */
    public static CursedEnergyTrait byId(String id)
    {
        CursedEnergyTrait found = find(id);
        return found == null ? NORMAL : found;
    }
}
