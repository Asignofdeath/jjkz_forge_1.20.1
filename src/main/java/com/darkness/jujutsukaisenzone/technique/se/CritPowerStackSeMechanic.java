package com.darkness.jujutsukaisenzone.technique.se;

/**
 * 暴击叠层强化窄口：造成体术 / 术式暴击后叠层，抬高体术与术式强度。
 * <p>
 * 调用方：{@code SeEffectEvents}（叠层）、{@code PlayerCombatPowerData}（读层数算加成）。
 * <p>
 * ⚠️ 同一窄口至多装载一条（见 {@link SeMechanicRegistry}）。
 */
public interface CritPowerStackSeMechanic extends SeMechanic
{
    /** 每层对体术 / 术式强度的比例加成（0.05 = +5%）。 */
    double bonusPerStack();

    int maxStacks();

    int durationTicks();
}
