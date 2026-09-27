package com.darkness.jujutsukaisenzone.technique.se;

import com.darkness.jujutsukaisenzone.item.CursedObjectGrade;
import net.minecraft.util.RandomSource;

import java.util.List;

/**
 * 产出时随机 SE 代价：<b>先均匀抽 {@link SePenaltyAmountGroup}（三档各 1/3），
 * 再在组内均匀抽落点</b>，最后按该组在 {@code grade} 下的闭区间掷 amount。
 * <p>
 * ⚠️ 不要在 13 条 {@link PanelStat} 上一把均匀抽：三组条数是 2 / 7 / 4，
 * 直接均匀会把「削体术 / 术式强度」压到约 15%、把「削百分比类」抬到约 54%。
 */
public final class SePenaltyAmountTable
{
    private SePenaltyAmountTable()
    {
    }

    public static SePenalty roll(CursedObjectGrade grade, RandomSource random)
    {
        SePenaltyAmountGroup[] groups = SePenaltyAmountGroup.values();
        SePenaltyAmountGroup group = groups[random.nextInt(groups.length)];
        List<PanelStat> stats = PanelStat.byAmountGroup(group);
        if (stats.isEmpty())
        {
            // 理论上不会发生：每个 group 至少挂了一条落点。兜底只是为了避免越界。
            stats = List.of(PanelStat.values());
        }
        PanelStat stat = stats.get(random.nextInt(stats.size()));
        int min = group.minInclusive(grade);
        int max = group.maxInclusive(grade);
        int amount = min + random.nextInt(Math.max(1, max - min + 1));
        return new SePenalty(stat, amount);
    }
}
