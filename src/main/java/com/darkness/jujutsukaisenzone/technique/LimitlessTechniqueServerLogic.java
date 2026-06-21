package com.darkness.jujutsukaisenzone.technique;

import com.darkness.jujutsukaisenzone.technique.skillset.LimitlessTechniqueSkillSet;
import com.darkness.jujutsukaisenzone.technique.skillset.TechniqueSkillSetRegistry;
import net.minecraft.server.level.ServerPlayer;

/**
 * 兼容入口：无下限术式逻辑已迁至 {@link LimitlessTechniqueSkillSet}，经 {@link TechniqueSkillSetRegistry} 分发。
 */
public final class LimitlessTechniqueServerLogic
{
    public static final int CAST_RANGE_TIER_MIN = LimitlessTechniqueSkillSet.CAST_RANGE_TIER_MIN;
    public static final int CAST_RANGE_TIER_MAX = LimitlessTechniqueSkillSet.CAST_RANGE_TIER_MAX;

    private LimitlessTechniqueServerLogic()
    {
    }

    public static void useSkillSlot(ServerPlayer player, int slot)
    {
        TechniqueSkillSetRegistry.useSkillSlot(player, slot);
    }

    public static int adjustCastRangeTier(ServerPlayer player, int delta)
    {
        return TechniqueSkillSetRegistry.adjustCastRangeTier(player, delta);
    }

    public static int getCastRangeTier(ServerPlayer player)
    {
        return TechniqueSkillSetRegistry.getCastRangeTier(player);
    }
}
