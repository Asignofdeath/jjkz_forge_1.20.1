package com.darkness.jujutsukaisenzone.quest.mainline;

import com.darkness.jujutsukaisenzone.locate.StructureLocateTarget;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.annotation.Nullable;

/**
 * 主线任务表。解锁规则：完成前序（偶发附加条件以后再加）。
 */
public final class MainQuestDefs
{
    public static final String MAIN_1_1_ID = "main_1_1";
    public static final String MAIN_1_2_ID = "main_1_2";
    public static final String MAIN_1_3_ID = "main_1_3";

    public static final MainQuestDef MAIN_1_1 = new MainQuestDef(
            MAIN_1_1_ID,
            "quest.jujutsukaisenzone.main_1_1.title",
            "quest.jujutsukaisenzone.main_1_1.objective",
            "quest.jujutsukaisenzone.main_1_1.description",
            "quest.jujutsukaisenzone.main_1_1.briefing.org",
            "quest.jujutsukaisenzone.main_1_1.briefing.location",
            null,
            StructureLocateTarget.NEAREST_VILLAGE);

    public static final MainQuestDef MAIN_1_2 = new MainQuestDef(
            MAIN_1_2_ID,
            "quest.jujutsukaisenzone.main_1_2.title",
            "quest.jujutsukaisenzone.main_1_2.objective.explore",
            "quest.jujutsukaisenzone.main_1_2.description",
            "quest.jujutsukaisenzone.main_1_2.briefing.org",
            "quest.jujutsukaisenzone.main_1_2.briefing.location",
            MAIN_1_1_ID,
            null);

    public static final MainQuestDef MAIN_1_3 = new MainQuestDef(
            MAIN_1_3_ID,
            "quest.jujutsukaisenzone.main_1_3.title",
            "quest.jujutsukaisenzone.main_1_3.objective",
            "quest.jujutsukaisenzone.main_1_3.description",
            "quest.jujutsukaisenzone.main_1_3.briefing.org",
            "quest.jujutsukaisenzone.main_1_3.briefing.location",
            MAIN_1_2_ID,
            null);

    private static final Map<String, MainQuestDef> BY_ID = new LinkedHashMap<>();

    static
    {
        register(MAIN_1_1);
        register(MAIN_1_2);
        register(MAIN_1_3);
    }

    private MainQuestDefs()
    {
    }

    private static void register(MainQuestDef def)
    {
        BY_ID.put(def.id(), def);
    }

    @Nullable
    public static MainQuestDef byId(@Nullable String id)
    {
        if (id == null || id.isEmpty())
        {
            return null;
        }
        return BY_ID.get(id);
    }

    /**
     * 完成 {@code completedId} 后应解锁并自动追踪的下一环（表内第一条以前序匹配者）。
     */
    @Nullable
    public static MainQuestDef nextAfter(@Nullable String completedId)
    {
        if (completedId == null || completedId.isEmpty())
        {
            return null;
        }
        for (MainQuestDef def : BY_ID.values())
        {
            if (completedId.equals(def.prerequisiteId()))
            {
                return def;
            }
        }
        return null;
    }

    public static Collection<MainQuestDef> all()
    {
        return Collections.unmodifiableCollection(BY_ID.values());
    }
}
