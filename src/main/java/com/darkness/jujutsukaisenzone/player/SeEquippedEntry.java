package com.darkness.jujutsukaisenzone.player;

import com.darkness.jujutsukaisenzone.technique.se.PanelStat;
import com.darkness.jujutsukaisenzone.technique.se.SePenalty;
import net.minecraft.nbt.CompoundTag;

/**
 * 已装载 SE 的实例快照：装载时从咒物拷贝的特质 + 代价。
 */
public final class SeEquippedEntry
{
    public final String traitId;
    public final SePenalty penalty;

    public SeEquippedEntry(String traitId, SePenalty penalty)
    {
        this.traitId = traitId == null || traitId.isBlank()
                ? CursedEnergyTrait.NORMAL.id()
                : traitId;
        this.penalty = penalty == null ? new SePenalty(PanelStat.PHYSICAL_POWER, 0.0D) : penalty;
    }

    public static SeEquippedEntry fromTag(CompoundTag tag, String fallbackTraitId)
    {
        String traitId = tag.contains("trait")
                ? CursedEnergyTrait.byId(tag.getString("trait")).id()
                : fallbackTraitId;
        PanelStat stat = tag.contains("penaltyStat")
                ? PanelStat.find(tag.getString("penaltyStat"))
                : null;
        double amount = tag.contains("penaltyAmount") ? tag.getDouble("penaltyAmount") : 0.0D;
        SePenalty penalty = stat == null
                ? new SePenalty(PanelStat.PHYSICAL_POWER, 0.0D)
                : new SePenalty(stat, amount);
        return new SeEquippedEntry(traitId, penalty);
    }

    public CompoundTag toTag(String seId)
    {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", seId);
        tag.putString("trait", traitId);
        tag.putString("penaltyStat", penalty.stat().id());
        tag.putDouble("penaltyAmount", penalty.amount());
        return tag;
    }
}
