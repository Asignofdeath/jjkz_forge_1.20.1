package com.darkness.jujutsukaisenzone.technique.skill;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

/**
 * 「审判」有罪后的处罚类型（用于屏幕标题演出）。
 */
public enum JudgmentVerdictType
{
    CONFISCATION(0, "Confiscation", "コンフィスケーション"),
    DEATH_SENTENCE(1, "Death Penalty", "デスペナルティー"),
    DEATH_SENTENCE_WITH_CONFISCATION(2, "Death Penalty with Confiscation", "コンフィスケーション付きデスペナルティー");

    private final int id;
    private final String mainTitle;
    private final String subTitle;

    JudgmentVerdictType(int id, String mainTitle, String subTitle)
    {
        this.id = id;
        this.mainTitle = mainTitle;
        this.subTitle = subTitle;
    }

    public int id()
    {
        return id;
    }

    public String mainTitle()
    {
        return mainTitle;
    }

    public String subTitle()
    {
        return subTitle;
    }

    /** 领域审判聊天栏裁决文案。 */
    public String chineseLabel()
    {
        return switch (this)
        {
            case CONFISCATION -> "没收";
            case DEATH_SENTENCE -> "死刑";
            case DEATH_SENTENCE_WITH_CONFISCATION -> "附加了没收的死刑";
        };
    }

    public Component subTitleComponent()
    {
        ChatFormatting color = switch (this)
        {
            case DEATH_SENTENCE -> ChatFormatting.RED;
            case DEATH_SENTENCE_WITH_CONFISCATION -> ChatFormatting.DARK_PURPLE;
            default -> ChatFormatting.WHITE;
        };
        return Component.literal(subTitle).withStyle(color);
    }

    public static JudgmentVerdictType fromId(int id)
    {
        for (JudgmentVerdictType type : values())
        {
            if (type.id == id)
            {
                return type;
            }
        }
        return CONFISCATION;
    }
}
