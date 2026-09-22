package com.darkness.jujutsukaisenzone.technique.skillset;

import net.minecraft.network.chat.Component;

/**
 * 特别招式的声明：一种**不占独立快捷栏槽位**、由「修饰键 + 技能键」触发的派生招式。
 * <p>
 * 与 {@link TechniqueSkill} 的区别：
 * <ul>
 *   <li>{@link TechniqueSkill} = 可拖入 1~9 栏位的独立技能，有自己的一份等级 / 咒力 / 冷却 / 解锁字段；</li>
 *   <li>{@code TechniqueSpecialMove} = 依附在某个原技能上的变体，**没有自己的基础字段**：
 *       等级 / 咒力 / 冷却 / 解锁一律继承 {@link #baseSkillId()} 指向的原技能
 *       （通则见 {@link IInnateTechniqueSkillSet#useSpecialMove}）。</li>
 * </ul>
 * <p>
 * 技能集通过 {@link IInnateTechniqueSkillSet#getSpecialMoves(String)} 按原技能声明，
 * 注册时由 {@code TechniqueSpecialMoveAttacher} 自动挂到术式开发树的对应技能节点下
 * （连线到原技能、默认复用原技能图标），因此新增特别招式不需要手写技能树节点。
 *
 * @param variantId    特别招式自身 id，需在所在术式内唯一（用于节点 id 与将来的独立数据字段）
 * @param baseSkillId  所依附的原技能 skillId（图标 / 数值 / 解锁的来源）
 * @param modifierMask 触发修饰键掩码，取值见 {@link SpecialMoveInput#MOD_SHIFT} 等
 * @param displayName  开发树中的展示名；为 {@code null} 时回退为「原技能名（Shift）」
 * @param iconPath     图标路径；为 {@code null} 或空白时复用原技能图标
 */
public record TechniqueSpecialMove(
        String variantId,
        String baseSkillId,
        int modifierMask,
        Component displayName,
        String iconPath)
{
    public TechniqueSpecialMove
    {
        if (variantId == null || variantId.isBlank())
        {
            throw new IllegalArgumentException("特别招式 variantId 不能为空");
        }
        if (baseSkillId == null || baseSkillId.isBlank())
        {
            throw new IllegalArgumentException("特别招式必须指定 baseSkillId：" + variantId);
        }
        if (modifierMask == 0)
        {
            throw new IllegalArgumentException("特别招式必须指定修饰键：" + variantId);
        }
    }

    public boolean hasOwnIcon()
    {
        return iconPath != null && !iconPath.isBlank();
    }

    /** 修饰键显示名，如 {@code Shift} / {@code Ctrl} / {@code Shift+Ctrl}。 */
    public String modifierLabel()
    {
        StringBuilder builder = new StringBuilder();
        appendModifier(builder, SpecialMoveInput.MOD_SHIFT, "Shift");
        appendModifier(builder, SpecialMoveInput.MOD_CTRL, "Ctrl");
        appendModifier(builder, SpecialMoveInput.MOD_ALT, "Alt");
        return builder.toString();
    }

    private void appendModifier(StringBuilder builder, int flag, String label)
    {
        if ((modifierMask & flag) == 0)
        {
            return;
        }
        if (builder.length() > 0)
        {
            builder.append('+');
        }
        builder.append(label);
    }
}
