package com.darkness.jujutsukaisenzone.technique.skillset;

import com.darkness.jujutsukaisenzone.technique.development.TechniqueSkillTree;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * 一种生得术式对应的「招式组」：数字键槽位、最大输出（长按蓄力后松手释放）、特别招式、可选施法距离档等均由实现定义。
 */
public interface IInnateTechniqueSkillSet
{
    String techniqueId();

    /** 客户端是否应把 1~3（当前招式组内）作为该术式的招式输入（通常：战斗模式 + 当前生得术式匹配）。 */
    boolean acceptsClientHotbarInput(Player player);

    /**
     * 是否处理「修饰键 + 数字键」的特别招式输入；默认与 {@link #acceptsClientHotbarInput} 相同。
     */
    default boolean acceptsClientSpecialMoveInput(Player player)
    {
        return acceptsClientHotbarInput(player);
    }

    void useSkillSlot(ServerPlayer player, int slot);

    /**
     * 特别招式：不占独立 1~9 槽位，由修饰键与关联槽位组合触发（如槽位变体、苍+赫联动茈等）。
     */
    default void useSpecialMove(ServerPlayer player, SpecialMoveInput input)
    {
    }

    /** 蓄力时长（tick）；0 表示该槽不支持「最大输出」。模板见 {@link AbstractInnateTechniqueSkillSet}。 */
    default int getMaxOutputChargeTicks(Player player, int slot)
    {
        return 0;
    }

    default boolean supportsMaxOutput(Player player, int slot)
    {
        return getMaxOutputChargeTicks(player, slot) > 0;
    }

    /** 相对槽位基础咒力消耗的倍数（默认 2 = 双倍咒力）。 */
    default int getMaxOutputCursedEnergyMultiplier()
    {
        return 2;
    }

    /** 客户端按下槽位键、开始蓄力时调用。 */
    default void beginMaxOutputCharge(ServerPlayer player, int slot)
    {
    }

    /** 蓄力未满松手时调用。 */
    default void cancelMaxOutputCharge(ServerPlayer player, int slot)
    {
    }

    /** 蓄力完成并松手时调用。 */
    default void castMaxOutput(ServerPlayer player, int slot)
    {
    }

    default boolean supportsCastRangeTiers()
    {
        return false;
    }

    /** 调整施法距离档位；不支持时返回 {@link #getCastRangeTier} 的默认值。 */
    default int adjustCastRangeTier(ServerPlayer player, int delta)
    {
        return getCastRangeTier(player);
    }

    default int getCastRangeTier(ServerPlayer player)
    {
        return 4;
    }

    /** 槽位技能在 GUI 中展示的名称；默认返回 null 表示不展示。 */
    default String getSlotDisplayName(int slot)
    {
        return null;
    }

    /** 槽位技能图标的资源路径（如 "textures/gui/skill_icon_1.png"）；默认返回 null。 */
    default String getSlotIconPath(int slot)
    {
        return null;
    }

    /**
     * 返回该术式所有可配置到快捷栏的技能列表。
     * GUI 配置界面据此展示可拖拽的技能图标。
     */
    default List<TechniqueSkill> getConfigurableSkills()
    {
        return Collections.emptyList();
    }

    /**
     * 已解锁技能是否应出现在技能快捷栏配置界面（及可被拖入栏位）。
     * 默认与解锁状态一致；部分技能可要求额外上下文（如宿傩拟态）。
     */
    default boolean isSkillVisibleInSkillBar(Player player, TechniqueSkill skill)
    {
        return true;
    }

    /** 术式开发技能解锁树；无树时返回空。 */
    default Optional<TechniqueSkillTree> getSkillTree()
    {
        return Optional.empty();
    }

    /**
     * 按技能 ID 释放技能（配合快捷栏配置使用）。
     * 默认实现回退到无操作。
     */
    default void useSkill(ServerPlayer player, String skillId)
    {
    }

    /**
     * 按技能 ID 释放特别招式（修饰键 + 槽位触发的变体）。
     * 默认实现无操作。
     */
    default void useSpecialMove(ServerPlayer player, String skillId, int modifierMask)
    {
    }

    /** 按技能 ID 判断是否支持最大输出。 */
    default boolean supportsMaxOutputBySkill(Player player, String skillId)
    {
        return false;
    }

    /** 按技能 ID 获取最大输出蓄力时长（tick）。 */
    default int getMaxOutputChargeTicksBySkill(Player player, String skillId)
    {
        return 0;
    }

    /** 按技能 ID 开始最大输出蓄力。 */
    default void beginMaxOutputChargeBySkill(ServerPlayer player, String skillId, int slot)
    {
    }

    /** 按技能 ID 释放最大输出。 */
    default void castMaxOutputBySkill(ServerPlayer player, String skillId, int slot)
    {
    }

    /** 长按蓄力、松手释放类技能的蓄力时长（tick）；0 表示不支持。 */
    default int getHoldReleaseChargeTicksBySkill(Player player, String skillId)
    {
        return 0;
    }

    /** 按下技能键时开始长按蓄力。 */
    default void beginHoldReleaseChargeBySkill(ServerPlayer player, String skillId, int slot)
    {
    }

    /** 蓄力未满松手时取消。 */
    default void cancelHoldReleaseChargeBySkill(ServerPlayer player, String skillId, int slot)
    {
    }

    /** 蓄力满后松手发射。 */
    default void releaseHoldReleaseChargeBySkill(ServerPlayer player, String skillId, int slot)
    {
    }
}
