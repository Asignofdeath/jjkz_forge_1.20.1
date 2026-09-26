package com.darkness.jujutsukaisenzone.technique.shikigami;

import net.minecraft.network.chat.Component;

/**
 * 式神模式招式声明（与热键栏 {@code TechniqueSpecialMove} 平行，不继承原技能 CD）。
 *
 * @param abilityId         模式内唯一 id，如 {@code divine_dogs_track}
 * @param shikigamiSlotId   式神槽位 id（十种影为 {@code divine_dogs} 等）
 * @param castMode          主动 / 自动 / 皆可
 * @param modifierMask      主动触发修饰键；自动招为 0。掩码精确匹配（同 {@code SpecialMoveInput}）
 * @param displayName       展示名
 * @param cursedEnergyCost  咒力；0 表示不扣
 * @param cooldownTicks     成功后写入该式神公用 CD 桶的时长
 * @param autoPriority      AUTO/BOTH 优先级，越大越先试
 * @param effect            效果体
 */
public record ShikigamiAbility(
        String abilityId,
        String shikigamiSlotId,
        ShikigamiCastMode castMode,
        int modifierMask,
        Component displayName,
        int cursedEnergyCost,
        int cooldownTicks,
        int autoPriority,
        ShikigamiAbilityEffect effect)
{
    public static final int MOD_SHIFT = 1;
    public static final int MOD_CTRL = 2;
    public static final int MOD_ALT = 4;

    public ShikigamiAbility
    {
        if (abilityId == null || abilityId.isBlank())
        {
            throw new IllegalArgumentException("abilityId 不能为空");
        }
        if (shikigamiSlotId == null || shikigamiSlotId.isBlank())
        {
            throw new IllegalArgumentException("shikigamiSlotId 不能为空：" + abilityId);
        }
        if (castMode == null)
        {
            throw new IllegalArgumentException("castMode 不能为空：" + abilityId);
        }
        if (castMode.allowsPlayer() && modifierMask == 0)
        {
            throw new IllegalArgumentException("主动招必须指定修饰键：" + abilityId);
        }
        if (!castMode.allowsPlayer() && modifierMask != 0)
        {
            throw new IllegalArgumentException("纯自动招不应带修饰键：" + abilityId);
        }
        if (displayName == null)
        {
            throw new IllegalArgumentException("displayName 不能为空：" + abilityId);
        }
        if (cursedEnergyCost < 0 || cooldownTicks < 0)
        {
            throw new IllegalArgumentException("cost/cd 不能为负：" + abilityId);
        }
        if (effect == null)
        {
            throw new IllegalArgumentException("effect 不能为空：" + abilityId);
        }
    }
}
