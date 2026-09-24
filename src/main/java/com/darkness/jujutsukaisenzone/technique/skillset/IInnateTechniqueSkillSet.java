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
     * 特别招式：**不占独立 1~9 槽位**的独立技能，由「修饰键 + 技能键」触发（如槽位变体、苍+赫联动茈等）。
     * <p>
     * <b>数值同步（通则）</b>：特别招式没有自己的招式记录，它的等级 / 咒力 / 冷却一律沿用**所依附的原技能**
     * ——即 {@link SpecialMoveInput#baseSlot()} 指向的栏位、经快捷栏配置解析出的那个 skillId 所对应的
     * 招式数值。所以「特别招式多少级、吃多少咒力、转多久冷却」默认就等于原技能，不需要（也无法）单独配置。
     * <p>
     * <b>唯一可独立的是解锁</b>：用
     * {@code AbstractInnateTechniqueSkillSet#castWithSlotEconomy(ServerPlayer, int, Runnable, java.util.function.Predicate)}
     * 换掉解锁判定即可让变体拥有自己的解锁字段（先例：无限术式苍/赫机动变体、Shift+3 茈升空悬停变体）。
     * <p>
     * <b>两道前置收口</b>。{@code AbstractInnateTechniqueSkillSet} 在结算经济**之前**先问两件事：
     * {@code acceptsSpecialMove}（该 skillId + 掩码组合有没有实现，默认按
     * {@link #getSpecialMoves(String)} 的声明<b>精确</b>匹配，声明 Shift 不会顺带放行 Shift+Ctrl）
     * 与 {@code isSpecialMoveUnlocked}（变体自身的解锁）。答否就直接 return，
     * <b>不扣咒力、不上冷却</b>。由此得出两条硬约定：
     * <ul>
     *   <li><b>先声明，后执行</b>：只在 {@code castSpecialMoveBySkill} 里写分支、却不在
     *       {@code getSpecialMoves} 里声明，等于这个组合不存在（按了没反应，但至少不会白扣费）。
     *       反过来说，{@code castSpecialMoveBySkill} 里也不该再写掩码收口——那已经是扣费之后。</li>
     *   <li><b>免费 / 条件性放行的条目</b>（清除、查询类，如星间飞行的标记清除与打自身标记）：
     *       子类要覆写 {@code useSpecialMove(ServerPlayer, String, int)} 抢先分流并 return，
     *       <b>同时</b>覆写 {@code acceptsSpecialMove} 把那些组合放进来；绝不能只靠
     *       {@code castSpecialMoveBySkill} 里的条件判断——那时费用已经扣掉了。</li>
     * </ul>
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
     * 返回以 {@code baseSkillId} 为原技能的特别招式声明列表（不占槽位的修饰键变体）。
     * <p>
     * 这些变体不需要在 {@link #getSkillTree()} 里手写节点：注册时
     * {@code TechniqueSpecialMoveAttacher} 会为每个声明派生一个「原技能 → 特别招式」的节点，
     * 用于在术式开发界面中展示。变体没有自己的基础字段，等级 / 咒力 / 冷却 / 解锁跟随原技能
     * （通则见 {@link #useSpecialMove(ServerPlayer, SpecialMoveInput)}）。
     *
     * @param baseSkillId 原技能 skillId
     * @return 该原技能下的特别招式；无则返回空列表
     */
    default List<TechniqueSpecialMove> getSpecialMoves(String baseSkillId)
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
     * 按技能 ID 释放特别招式（修饰键 + 技能键触发的独立技能）。
     * 默认实现无操作。
     * <p>
     * 数值同步与两道前置收口见 {@link #useSpecialMove(ServerPlayer, SpecialMoveInput)}。
     * 注意：默认链路由 {@code AbstractInnateTechniqueSkillSet} 在**结算咒力 / 冷却之后**才调用本方法，
     * 所以本方法只该写演出；需要「免费」的条目请在子类覆写本方法做前置分流，并同步覆写
     * {@code acceptsSpecialMove}。
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
