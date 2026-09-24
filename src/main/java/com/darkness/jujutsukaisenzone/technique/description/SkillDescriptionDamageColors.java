package com.darkness.jujutsukaisenzone.technique.description;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

/**
 * 技能描述正文里，把「术式伤害」「体术伤害」等短语上色（术式蓝、体术黄）。
 * <p>
 * 只改显示，不改 lang 原文；在 tooltip 拼行时对已解析的 {@link Component} 再扫一遍字符串。
 */
public final class SkillDescriptionDamageColors
{
    private static final Style BASE = Style.EMPTY.withColor(ChatFormatting.DARK_GRAY);
    private static final Style TECHNIQUE = Style.EMPTY.withColor(ChatFormatting.BLUE);
    private static final Style MARTIAL = Style.EMPTY.withColor(ChatFormatting.YELLOW);

    /** 长词优先，避免「术式伤害」拆不开「术式单体伤害」。 */
    private static final Phrase[] PHRASES = {
            new Phrase("术式单体伤害", TECHNIQUE),
            new Phrase("术式冲撞伤害", TECHNIQUE),
            new Phrase("术式伤害", TECHNIQUE),
            new Phrase("体术冲撞伤害", MARTIAL),
            new Phrase("体术伤害", MARTIAL),
            new Phrase("technique single-target damage", TECHNIQUE),
            new Phrase("technique damage", TECHNIQUE),
            new Phrase("martial impact damage", MARTIAL),
            new Phrase("martial damage", MARTIAL),
    };

    private SkillDescriptionDamageColors()
    {
    }

    /**
     * 将描述渲染为带分型着色的组件；无命中短语时等价于整段深灰。
     */
    public static Component apply(Component description)
    {
        if (description == null)
        {
            return Component.empty();
        }
        String text = description.getString();
        if (text.isEmpty())
        {
            return description.copy().withStyle(BASE);
        }

        MutableComponent result = Component.empty();
        int i = 0;
        while (i < text.length())
        {
            Phrase hit = findAt(text, i);
            if (hit != null)
            {
                result.append(Component.literal(hit.text()).withStyle(hit.style()));
                i += hit.text().length();
                continue;
            }
            int next = nextPhraseIndex(text, i);
            result.append(Component.literal(text.substring(i, next)).withStyle(BASE));
            i = next;
        }
        return result;
    }

    private static Phrase findAt(String text, int index)
    {
        for (Phrase phrase : PHRASES)
        {
            if (text.startsWith(phrase.text(), index))
            {
                return phrase;
            }
        }
        return null;
    }

    private static int nextPhraseIndex(String text, int from)
    {
        int best = text.length();
        for (Phrase phrase : PHRASES)
        {
            int at = text.indexOf(phrase.text(), from);
            if (at >= 0 && at < best)
            {
                best = at;
            }
        }
        return best;
    }

    private record Phrase(String text, Style style)
    {
    }
}
