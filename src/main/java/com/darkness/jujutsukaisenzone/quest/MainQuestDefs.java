package com.darkness.jujutsukaisenzone.quest;

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

    public static final MainQuestDef MAIN_1_1 = new MainQuestDef(
            MAIN_1_1_ID,
            "quest.jujutsukaisenzone.main_1_1.title",
            "quest.jujutsukaisenzone.main_1_1.objective",
            "quest.jujutsukaisenzone.main_1_1.description",
            "quest.jujutsukaisenzone.main_1_1.briefing.org",
            "quest.jujutsukaisenzone.main_1_1.briefing.location",
            null,
            StructureLocateTarget.NEAREST_VILLAGE);

    private static final Map<String, MainQuestDef> BY_ID = new LinkedHashMap<>();

    static
    {
        register(MAIN_1_1);
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

    public static Collection<MainQuestDef> all()
    {
        return Collections.unmodifiableCollection(BY_ID.values());
    }
}
