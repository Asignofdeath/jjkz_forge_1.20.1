package com.darkness.jujutsukaisenzone.technique.skillset;

/**
 * 特别招式输入：修饰键组合 + 所关联的招式栏位（客户端 1~3 映射到当前组的槽位 1~9）。
 * <p>
 * 与 {@link IInnateTechniqueSkillSet#useSkillSlot} 的“纯槽位释放”区分：纯数字键走槽位包；带修饰键时走 {@link IInnateTechniqueSkillSet#useSpecialMove(ServerPlayer, SpecialMoveInput)}。
 * <p>
 * 特别招式不占独立槽位，其等级 / 咒力 / 冷却**沿用该栏位配置的原技能**（详见
 * {@link IInnateTechniqueSkillSet#useSpecialMove(ServerPlayer, SpecialMoveInput)}），
 * 因此 {@link #baseSlot()} 既是「触发用哪一行键」，也是「数值取自哪个技能」的依据。
 */
public record SpecialMoveInput(int modifierMask, int baseSlot)
{
    public static final int MOD_SHIFT = 1;
    public static final int MOD_CTRL = 2;
    public static final int MOD_ALT = 4;

    public SpecialMoveInput {
        if (baseSlot < 1 || baseSlot > 9)
        {
            throw new IllegalArgumentException("baseSlot must be 1..9");
        }
    }

    public boolean hasShift()
    {
        return (this.modifierMask & MOD_SHIFT) != 0;
    }

    public boolean hasCtrl()
    {
        return (this.modifierMask & MOD_CTRL) != 0;
    }

    public boolean hasAlt()
    {
        return (this.modifierMask & MOD_ALT) != 0;
    }
}
