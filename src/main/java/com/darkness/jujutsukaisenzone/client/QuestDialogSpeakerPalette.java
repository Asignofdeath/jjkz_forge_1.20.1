package com.darkness.jujutsukaisenzone.client;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import javax.annotation.Nullable;

/**
 * 对白「角色名：」→ 对主角态度。未登记的名字默认 {@link QuestDialogStance#NEUTRAL}。
 */
public final class QuestDialogSpeakerPalette
{
    private static final Map<String, QuestDialogStance> BY_NAME = new HashMap<>();

    static
    {
        // main_1_1 生存小队（中/英 lang 角色名）
        register("Kiro", QuestDialogStance.FRIENDLY);
        register("小石子", QuestDialogStance.FRIENDLY);
        register("XiaoShiZi", QuestDialogStance.FRIENDLY);
        register("你", QuestDialogStance.FRIENDLY);
        register("You", QuestDialogStance.FRIENDLY);
        // main_1_2 伏击
        register("三级咒灵甲", QuestDialogStance.HOSTILE);
        register("Grade 3 Spirit A", QuestDialogStance.HOSTILE);
    }

    private QuestDialogSpeakerPalette()
    {
    }

    public static void register(String speakerName, QuestDialogStance stance)
    {
        if (speakerName == null || speakerName.isBlank() || stance == null)
        {
            return;
        }
        BY_NAME.put(normalize(speakerName), stance);
    }

    public static QuestDialogStance stanceOf(@Nullable String speakerName)
    {
        if (speakerName == null || speakerName.isBlank())
        {
            return QuestDialogStance.NEUTRAL;
        }
        return BY_NAME.getOrDefault(normalize(speakerName), QuestDialogStance.NEUTRAL);
    }

    private static String normalize(String name)
    {
        return name.trim().toLowerCase(Locale.ROOT);
    }
}
