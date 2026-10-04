package com.darkness.jujutsukaisenzone.quest;

import com.darkness.jujutsukaisenzone.locate.StructureLocateTarget;

import javax.annotation.Nullable;

/**
 * 主线任务定义。
 */
public record MainQuestDef(
        String id,
        String titleKey,
        String objectiveKey,
        String descriptionKey,
        /** COD 风格任务简报：组织 / 小队。 */
        String briefingOrgKey,
        /** COD 风格任务简报：地点。 */
        String briefingLocationKey,
        @Nullable String prerequisiteId,
        @Nullable StructureLocateTarget locateTarget)
{
    public boolean hasLocateTarget()
    {
        return this.locateTarget != null;
    }
}
