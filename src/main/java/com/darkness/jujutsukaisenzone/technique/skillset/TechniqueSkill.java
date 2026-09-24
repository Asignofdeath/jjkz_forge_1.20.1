package com.darkness.jujutsukaisenzone.technique.skillset;

import net.minecraft.network.chat.Component;

/**
 * 描述一个可配置到快捷栏的术式技能。
 * <p>
 * 每个 {@link IInnateTechniqueSkillSet} 通过 {@link IInnateTechniqueSkillSet#getConfigurableSkills()} 返回其所有可用技能。
 * 玩家在 {@code SkillBarConfigScreen} 中将技能拖到快捷栏位（1~9）完成配置。
 * <p>
 * {@link #defaultSlot()} 仅用于首次加载时自动填充空栏位的默认值，不影响后续玩家自由拖拽配置。
 */
public final class TechniqueSkill
{
    private final String skillId;
    private final int defaultSlot;
    private final Component displayName;
    private final String iconPath;
    private final int cost;
    private final int cooldownTicks;
    private final int maxOutputChargeTicks;
    private final String cooldownTag;
    private final String unlockTag;
    private final String levelTag;
    private final SkillDamageType damageType;

    /**
     * 默认分型为 {@link SkillDamageType#TECHNIQUE}（术式）。
     * 只有体术 / 复合技能才需要改用
     * {@link #TechniqueSkill(String, int, Component, String, int, int, int, String, String, String, SkillDamageType)}。
     */
    public TechniqueSkill(
            String skillId,
            int defaultSlot,
            Component displayName,
            String iconPath,
            int cost,
            int cooldownTicks,
            int maxOutputChargeTicks,
            String cooldownTag,
            String unlockTag,
            String levelTag)
    {
        this(skillId, defaultSlot, displayName, iconPath, cost, cooldownTicks,
                maxOutputChargeTicks, cooldownTag, unlockTag, levelTag, SkillDamageType.TECHNIQUE);
    }

    public TechniqueSkill(
            String skillId,
            int defaultSlot,
            Component displayName,
            String iconPath,
            int cost,
            int cooldownTicks,
            int maxOutputChargeTicks,
            String cooldownTag,
            String unlockTag,
            String levelTag,
            SkillDamageType damageType)
    {
        this.damageType = damageType;
        this.skillId = skillId;
        this.defaultSlot = defaultSlot;
        this.displayName = displayName;
        this.iconPath = iconPath;
        this.cost = cost;
        this.cooldownTicks = cooldownTicks;
        this.maxOutputChargeTicks = maxOutputChargeTicks;
        this.cooldownTag = cooldownTag;
        this.unlockTag = unlockTag;
        this.levelTag = levelTag;
    }

    public String skillId()
    {
        return skillId;
    }

    /** 该技能默认绑定的槽位编号（1~9），方便新玩家无需手动配置。 */
    public int defaultSlot()
    {
        return defaultSlot;
    }

    public Component displayName()
    {
        return displayName;
    }

    public String iconPath()
    {
        return iconPath;
    }

    public int cost()
    {
        return cost;
    }

    public int cooldownTicks()
    {
        return cooldownTicks;
    }

    /** 最大输出蓄力时长（tick）；0 表示该技能不支持最大输出。 */
    public int maxOutputChargeTicks()
    {
        return maxOutputChargeTicks;
    }

    public String cooldownTag()
    {
        return cooldownTag;
    }

    public String unlockTag()
    {
        return unlockTag;
    }

    public String levelTag()
    {
        return levelTag;
    }

    /** 该技能的伤害分型，默认 {@link SkillDamageType#TECHNIQUE}。 */
    public SkillDamageType damageType()
    {
        return damageType;
    }
}
