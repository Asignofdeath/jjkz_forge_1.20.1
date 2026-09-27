package com.darkness.jujutsukaisenzone.technique.se;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

/**
 * SE 目录筛选标签（装载元数据；与装载闸无关）。
 * <p>
 * 两轴：
 * <ul>
 *   <li>{@link #GENERIC} / {@link #SPECIALIZED}——与 {@link SeScope} 同构，启动自检强制一致。</li>
 *   <li>{@link #MODIFY} / {@link #ADDITIVE} / {@link #REMOVE}——改写、叠加、关掉既有机制；可并存。</li>
 * </ul>
 * UI 多选语义：轴内 OR，轴间 AND；某轴未选任何标签 = 该轴不过滤。
 */
public enum SeCatalogTag
{
    /** 通用：任意术式可装。 */
    GENERIC,
    /** 专用：绑定某一生得术式。 */
    SPECIALIZED,
    /** 修改：改写既有规则 / 数值曲线 / 兑换比。 */
    MODIFY,
    /** 增量：在既有流程上叠加新效果。 */
    ADDITIVE,
    /** 移除：关掉某条既有保险 / 豁免 / 分支。 */
    REMOVE;

    private static final Set<SeCatalogTag> SCOPE_AXIS = Collections.unmodifiableSet(
            EnumSet.of(GENERIC, SPECIALIZED));
    private static final Set<SeCatalogTag> MODE_AXIS = Collections.unmodifiableSet(
            EnumSet.of(MODIFY, ADDITIVE, REMOVE));

    public boolean isScopeAxis()
    {
        return SCOPE_AXIS.contains(this);
    }

    public boolean isModeAxis()
    {
        return MODE_AXIS.contains(this);
    }

    public String id()
    {
        return name().toLowerCase(Locale.ROOT);
    }

    public Component displayName()
    {
        return Component.translatable("gui." + JuJutsuKaisenZone.MODID + ".se.tag." + id());
    }

    @Nullable
    public static SeCatalogTag find(String id)
    {
        if (id == null || id.isBlank())
        {
            return null;
        }
        String normalized = id.trim().toLowerCase(Locale.ROOT);
        for (SeCatalogTag tag : values())
        {
            if (tag.id().equals(normalized))
            {
                return tag;
            }
        }
        return null;
    }

    public static Set<SeCatalogTag> scopeAxis()
    {
        return SCOPE_AXIS;
    }

    public static Set<SeCatalogTag> modeAxis()
    {
        return MODE_AXIS;
    }

    /**
     * 目录筛选：轴内 OR、轴间 AND；某轴未勾选 = 该轴不过滤。
     */
    public static boolean matchesFilter(Set<SeCatalogTag> itemTags, Set<SeCatalogTag> selected)
    {
        if (selected == null || selected.isEmpty())
        {
            return true;
        }
        if (itemTags == null || itemTags.isEmpty())
        {
            return false;
        }
        boolean scopeSelected = false;
        boolean scopeHit = false;
        boolean modeSelected = false;
        boolean modeHit = false;
        for (SeCatalogTag tag : selected)
        {
            if (tag.isScopeAxis())
            {
                scopeSelected = true;
                if (itemTags.contains(tag))
                {
                    scopeHit = true;
                }
            }
            else if (tag.isModeAxis())
            {
                modeSelected = true;
                if (itemTags.contains(tag))
                {
                    modeHit = true;
                }
            }
        }
        if (scopeSelected && !scopeHit)
        {
            return false;
        }
        if (modeSelected && !modeHit)
        {
            return false;
        }
        return true;
    }
}
