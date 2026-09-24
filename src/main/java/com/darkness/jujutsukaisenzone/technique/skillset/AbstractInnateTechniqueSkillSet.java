package com.darkness.jujutsukaisenzone.technique.skillset;

import com.darkness.jujutsukaisenzone.compat.EpicFightCompat;
import com.darkness.jujutsukaisenzone.player.PlayerCursedEnergyData;
import com.darkness.jujutsukaisenzone.player.PlayerMaxOutputChargeData;
import com.darkness.jujutsukaisenzone.gameplay.ModRules;
import com.darkness.jujutsukaisenzone.technique.development.SkillUnlockLogic;
import com.darkness.jujutsukaisenzone.player.PlayerTechniqueData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.function.Predicate;

/**
 * 通用释放流程：战斗模式、解锁/等级、创造模式免耗免 CD、咒力与冷却。
 * <p>
 * 「最大输出」：按住槽位数字键蓄力，蓄满后松手释放；蓄力期间松手则取消。子类可覆写
 * {@link #getSlotMaxOutputChargeTicks}、{@link #castSkillSlotMaxOutput} 等；各槽位演出见 {@link #castSkillSlot}。
 */
public abstract class AbstractInnateTechniqueSkillSet implements IInnateTechniqueSkillSet
{
    /** 默认蓄力 3 秒（20 tick/s）。 */
    public static final int DEFAULT_MAX_OUTPUT_CHARGE_TICKS = 20 * 3;

    @Override
    public boolean acceptsClientHotbarInput(Player player)
    {
        return EpicFightCompat.isBattleMode(player)
                && techniqueId().equals(PlayerTechniqueData.getInnateTechnique(player));
    }

    @Override
    public final void useSkillSlot(ServerPlayer player, int slot)
    {
        if (!EpicFightCompat.isBattleMode(player))
        {
            return;
        }
        if (!techniqueId().equals(PlayerTechniqueData.getInnateTechnique(player)))
        {
            return;
        }
        castWithSlotEconomy(player, slot, () -> castSkillSlot(player, slot));
    }

    @Override
    public void useSpecialMove(ServerPlayer player, SpecialMoveInput input)
    {
        if (!EpicFightCompat.isBattleMode(player))
        {
            return;
        }
        if (!techniqueId().equals(PlayerTechniqueData.getInnateTechnique(player)))
        {
            return;
        }
        castSpecialMove(player, input);
    }

    @Override
    public int getMaxOutputChargeTicks(Player player, int slot)
    {
        if (slot < 1 || slot > 9)
        {
            return 0;
        }
        if (getSlotCooldownTag(slot) == null)
        {
            return 0;
        }
        if (!isSlotUnlocked(player, slot))
        {
            return 0;
        }
        if (getSlotSkillLevel(player, slot) <= 0)
        {
            return 0;
        }
        if (getSlotCost(slot) <= 0)
        {
            return 0;
        }
        return getSlotMaxOutputChargeTicks(slot);
    }

    /**
     * 支持最大输出的槽位蓄力时长；默认 {@link #DEFAULT_MAX_OUTPUT_CHARGE_TICKS}（3 秒）。
     */
    protected int getSlotMaxOutputChargeTicks(int slot)
    {
        return DEFAULT_MAX_OUTPUT_CHARGE_TICKS;
    }

    @Override
    public final void beginMaxOutputCharge(ServerPlayer player, int slot)
    {
        if (!EpicFightCompat.isBattleMode(player) || !techniqueId().equals(PlayerTechniqueData.getInnateTechnique(player)))
        {
            return;
        }
        if (PlayerMaxOutputChargeData.isCharging(player))
        {
            return;
        }
        if (getMaxOutputChargeTicks(player, slot) <= 0)
        {
            return;
        }
        if (!isSlotUnlocked(player, slot))
        {
            player.sendSystemMessage(Component.literal("该招式尚未解锁。"));
            return;
        }
        if (getSlotSkillLevel(player, slot) <= 0)
        {
            player.sendSystemMessage(Component.literal("该招式等级异常，无法释放。"));
            return;
        }
        long now = player.level().getGameTime();
        if (usesSkillCooldown(player))
        {
            int remain = getRemainingCooldownTicks(player, slot, now);
            if (remain > 0)
            {
                player.sendSystemMessage(Component.literal("招式冷却中：" + formatCooldownSeconds(remain) + " 秒"));
                return;
            }
        }
        if (!player.isCreative())
        {
            int need = getSlotCost(slot) * getMaxOutputCursedEnergyMultiplier();
            if (need > 0 && PlayerCursedEnergyData.getCurrent(player) < need)
            {
                player.sendSystemMessage(Component.literal("咒力不足，无法开始最大输出。"));
                return;
            }
        }

        PlayerMaxOutputChargeData.beginCharge(player, slot, now, techniqueId());
    }

    @Override
    public final void cancelMaxOutputCharge(ServerPlayer player, int slot)
    {
        if (PlayerMaxOutputChargeData.getChargingSlot(player) != slot)
        {
            return;
        }
        PlayerMaxOutputChargeData.clear(player);
    }

    @Override
    public final void castMaxOutput(ServerPlayer player, int slot)
    {
        if (PlayerMaxOutputChargeData.getChargingSlot(player) != slot)
        {
            return;
        }
        if (!EpicFightCompat.isBattleMode(player) || !techniqueId().equals(PlayerTechniqueData.getInnateTechnique(player)))
        {
            PlayerMaxOutputChargeData.clear(player);
            return;
        }
        long now = player.level().getGameTime();
        int requiredTicks = getMaxOutputChargeTicks(player, slot);
        long heldTicks = now - PlayerMaxOutputChargeData.getChargeStartGameTick(player);
        PlayerMaxOutputChargeData.clear(player);
        if (requiredTicks <= 0 || heldTicks < requiredTicks)
        {
            return;
        }
        if (!isSlotUnlocked(player, slot))
        {
            return;
        }
        if (getSlotSkillLevel(player, slot) <= 0)
        {
            return;
        }
        if (usesSkillCooldown(player))
        {
            int remain = getRemainingCooldownTicks(player, slot, now);
            if (remain > 0)
            {
                player.sendSystemMessage(Component.literal("招式冷却中，最大输出未能完成。"));
                return;
            }
        }
        if (!player.isCreative())
        {
            int need = getSlotCost(slot) * getMaxOutputCursedEnergyMultiplier();
            if (need > 0 && !PlayerCursedEnergyData.tryConsume(player, need))
            {
                player.sendSystemMessage(Component.literal("咒力不足，最大输出未能完成。"));
                return;
            }
        }
        if (usesSkillCooldown(player))
        {
            setSlotCooldown(player, slot, now + getSlotCooldownTicks(player, slot));
        }

        castSkillSlotMaxOutput(player, slot);
    }

    protected abstract void castSkillSlot(ServerPlayer player, int slot);

    /** 最大输出释放时的槽位演出；默认与同槽位普通释放相同。 */
    protected void castSkillSlotMaxOutput(ServerPlayer player, int slot)
    {
        castSkillSlot(player, slot);
    }

    /** 子类实现具体特别招式；默认无操作。 */
    protected void castSpecialMove(ServerPlayer player, SpecialMoveInput input)
    {
    }

    /**
     * 与某槽位共享解锁、等级、咒力与冷却时，在 {@link #castSpecialMove} 里调用此方法再执行自定义演出。
     */
    protected final void castWithSlotEconomy(ServerPlayer player, int slot, Runnable cast)
    {
        castWithSlotEconomy(player, slot, cast, p -> isSlotUnlocked(p, slot));
    }

    /**
     * 与某槽位共享等级、咒力与冷却；解锁由 {@code isUnlocked} 单独判定（例如变体招式使用独立解锁字段）。
     */
    protected final void castWithSlotEconomy(ServerPlayer player, int slot, Runnable cast, Predicate<Player> isUnlocked)
    {
        if (!isUnlocked.test(player))
        {
            player.sendSystemMessage(Component.literal("该招式尚未解锁。"));
            return;
        }
        int skillLevel = getSlotSkillLevel(player, slot);
        if (skillLevel <= 0)
        {
            player.sendSystemMessage(Component.literal("该招式等级异常，无法释放。"));
            return;
        }
        long now = player.level().getGameTime();
        if (usesSkillCooldown(player))
        {
            int remain = getRemainingCooldownTicks(player, slot, now);
            if (remain > 0)
            {
                player.sendSystemMessage(Component.literal("招式冷却中：" + formatCooldownSeconds(remain) + " 秒"));
                return;
            }
        }
        if (!player.isCreative())
        {
            int cost = getSlotCost(slot);
            if (cost > 0 && !PlayerCursedEnergyData.tryConsume(player, cost))
            {
                player.sendSystemMessage(Component.literal("咒力不足，无法释放该招式。"));
                return;
            }
        }
        if (usesSkillCooldown(player))
        {
            setSlotCooldown(player, slot, now + getSlotCooldownTicks(player, slot));
        }

        cast.run();
    }

    // ======================== skillId 驱动（玩家可配置快捷栏） ========================

    protected TechniqueSkill findSkillById(String skillId)
    {
        for (TechniqueSkill s : getConfigurableSkills())
        {
            if (s.skillId().equals(skillId)) return s;
        }
        return null;
    }

    @Override
    public void useSkill(ServerPlayer player, String skillId)
    {
        if (!EpicFightCompat.isBattleMode(player)
                || !techniqueId().equals(PlayerTechniqueData.getInnateTechnique(player)))
        {
            return;
        }
        TechniqueSkill skill = findSkillById(skillId);
        if (skill == null) return;
        castWithSkillEconomy(player, skill, () -> castSkill(player, skillId));
    }

    /**
     * 特别招式默认链路：**先做两道前置收口，再按原技能的数值结算经济，最后交给
     * {@code castSpecialMoveBySkill} 执行演出**。
     * <p>
     * 收口顺序是这条链路的要害：{@code castWithSkillEconomy} 一进去就扣咒力 / 上冷却，之后再判就要不回来了
     * ——曾经的「Ctrl + 数字键白扣一次咒力」「变体未解锁照样扣费」都是把判断写在了它后面。
     * 现在两道收口都在它**之前**：
     * <ol>
     *   <li>{@link #acceptsSpecialMove}：该 skillId + 掩码组合有没有实现（默认按
     *       {@code getSpecialMoves(skillId)} 的声明判定，掩码需精确相等）——没有则静默忽略；</li>
     *   <li>{@link #isSpecialMoveUnlocked}：变体自身的解锁状态（默认跟随原技能）——未解锁则提示并忽略。</li>
     * </ol>
     * <p>
     * {@code skill} 由 {@code skillId} 反查而来，也就是「原技能」——特别招式没有自己的招式记录，
     * 所以等级 / 咒力 / 冷却天然沿用原技能（这正是「特别招式与原技能数值同步」的实现方式）。
     * <p>
     * <b>子类若要实现「不消耗咒力 / 不吃冷却」的特别招式</b>（清除、查询类动作），仍然必须覆写本方法，
     * 在进入 {@code castWithSkillEconomy} **之前**分流并 return——{@link #acceptsSpecialMove} 只回答
     * 「有没有实现」，不回答「要扣多少费」。请连同 {@link #acceptsSpecialMove} 一起覆写，否则那些组合
     * 会在第一道收口就被静默拦下。
     */
    @Override
    public void useSpecialMove(ServerPlayer player, String skillId, int modifierMask)
    {
        if (!EpicFightCompat.isBattleMode(player)
                || !techniqueId().equals(PlayerTechniqueData.getInnateTechnique(player)))
        {
            return;
        }
        TechniqueSkill skill = findSkillById(skillId);
        if (skill == null) return;
        if (!acceptsSpecialMove(player, skillId, modifierMask))
        {
            return;
        }
        if (!isSpecialMoveUnlocked(player, skillId, modifierMask))
        {
            player.sendSystemMessage(Component.literal("该招式尚未解锁。"));
            return;
        }
        castWithSkillEconomy(player, skill, () -> castSpecialMoveBySkill(player, skillId, modifierMask));
    }

    /**
     * 该 {@code skillId} + 修饰键组合是否有实现；在 {@link #castWithSkillEconomy} **之前**调用。
     * <p>
     * 默认按 {@link IInnateTechniqueSkillSet#getSpecialMoves(String)} 的声明判定，且要求掩码**精确相等**
     * ——声明 {@code MOD_SHIFT} 不会顺带放行 {@code Shift+Ctrl}。没有声明的组合一律静默忽略，不扣咒力也不上冷却。
     * <p>
     * ⚠️ 因此「先声明，后执行」是硬约定：<b>新增特别招式必须同时在 {@code getSpecialMoves} 里声明</b>，
     * 只在 {@code castSpecialMoveBySkill} 里写分支是不会被调用的（表现为按了没反应，而不是白扣费）。
     * <p>
     * 需要「免费」或「条件性放行」（如星间飞行的标记清除、打自身标记）的技能集，覆写
     * {@link #useSpecialMove(ServerPlayer, String, int)} 自行分流，并一并覆写本方法把那些组合放进来。
     */
    protected boolean acceptsSpecialMove(ServerPlayer player, String skillId, int modifierMask)
    {
        for (TechniqueSpecialMove move : getSpecialMoves(skillId))
        {
            if (move.modifierMask() == modifierMask)
            {
                return true;
            }
        }
        return false;
    }

    /**
     * 特别招式自身的解锁判定；在 {@link #castWithSkillEconomy} **之前**调用，返回 {@code false} 时
     * 由 {@link #useSpecialMove(ServerPlayer, String, int)} 统一提示并忽略，因此不会浪费咒力与冷却。
     * <p>
     * 默认 {@code true}——原技能的解锁由 {@code castWithSkillEconomy} 负责。变体拥有独立解锁字段的技能集
     * （先例：无限术式苍 / 赫机动变体）覆写本方法，即可把「变体未解锁」也拦在扣费之前。
     */
    protected boolean isSpecialMoveUnlocked(ServerPlayer player, String skillId, int modifierMask)
    {
        return true;
    }

    /** 按技能 ID 释放（子类实现具体演出）。 */
    protected void castSkill(ServerPlayer player, String skillId)
    {
    }

    /**
     * 按技能 ID 释放特别招式（子类实现）。
     * <p>
     * 执行到这里时，组合已由 {@link #acceptsSpecialMove} / {@link #isSpecialMoveUnlocked} 放行，
     * 且**咒力 / 冷却 / 等级已按原技能结算**：本方法只负责「演出」，不负责经济，也**不需要**再写
     * {@code if (modifierMask != ...) return;} 之类的掩码收口——那是收口写晚了的旧写法，会在扣费之后才 return。
     * 唯一仍需在本方法里判的是「跟掩码无关的其它上下文」（例如玩家当前状态是否允许该演出）。
     */
    protected void castSpecialMoveBySkill(ServerPlayer player, String skillId, int modifierMask)
    {
    }

    @Override
    public boolean supportsMaxOutput(Player player, int slot)
    {
        return getMaxOutputChargeTicks(player, slot) > 0;
    }

    /** 判断某技能是否支持最大输出。 */
    public boolean supportsMaxOutputBySkill(Player player, String skillId)
    {
        TechniqueSkill skill = findSkillById(skillId);
        return skill != null && skill.maxOutputChargeTicks() > 0;
    }

    /** 按技能 ID 返回最大输出蓄力时长。 */
    public int getMaxOutputChargeTicksBySkill(Player player, String skillId)
    {
        TechniqueSkill skill = findSkillById(skillId);
        if (skill == null || skill.maxOutputChargeTicks() <= 0) return 0;
        if (!isSkillUnlocked(player, skill)) return 0;
        if (getSkillLevel(player, skill) <= 0) return 0;
        return skill.maxOutputChargeTicks();
    }

    /** 按技能 ID 开始最大输出蓄力。 */
    public void beginMaxOutputChargeBySkill(ServerPlayer player, String skillId, int slot)
    {
        TechniqueSkill skill = findSkillById(skillId);
        if (skill == null || skill.maxOutputChargeTicks() <= 0) return;
        if (!isSkillUnlocked(player, skill))
        {
            player.sendSystemMessage(Component.literal("该招式尚未解锁。"));
            return;
        }
        if (getSkillLevel(player, skill) <= 0)
        {
            player.sendSystemMessage(Component.literal("该招式等级异常，无法释放。"));
            return;
        }
        long now = player.level().getGameTime();
        if (usesSkillCooldown(player))
        {
            int remain = getRemainingSkillCooldownTicks(player, skill, now);
            if (remain > 0)
            {
                player.sendSystemMessage(Component.literal("招式冷却中：" + formatCooldownSeconds(remain) + " 秒"));
                return;
            }
        }
        if (!player.isCreative())
        {
            int need = skill.cost() * getMaxOutputCursedEnergyMultiplier();
            if (need > 0 && PlayerCursedEnergyData.getCurrent(player) < need)
            {
                player.sendSystemMessage(Component.literal("咒力不足，无法开始最大输出。"));
                return;
            }
        }
        PlayerMaxOutputChargeData.beginCharge(player, slot, now, techniqueId());
    }

    /** 按技能 ID 释放最大输出。 */
    public void castMaxOutputBySkill(ServerPlayer player, String skillId, int slot)
    {
        TechniqueSkill skill = findSkillById(skillId);
        if (skill == null) return;

        if (PlayerMaxOutputChargeData.getChargingSlot(player) != slot) return;
        long now = player.level().getGameTime();
        long heldTicks = now - PlayerMaxOutputChargeData.getChargeStartGameTick(player);
        PlayerMaxOutputChargeData.clear(player);
        if (skill.maxOutputChargeTicks() <= 0 || heldTicks < skill.maxOutputChargeTicks()) return;
        if (!isSkillUnlocked(player, skill)) return;
        if (getSkillLevel(player, skill) <= 0) return;
        if (usesSkillCooldown(player))
        {
            int remain = getRemainingSkillCooldownTicks(player, skill, now);
            if (remain > 0)
            {
                player.sendSystemMessage(Component.literal("招式冷却中，最大输出未能完成。"));
                return;
            }
        }
        if (!player.isCreative())
        {
            int need = skill.cost() * getMaxOutputCursedEnergyMultiplier();
            if (need > 0 && !PlayerCursedEnergyData.tryConsume(player, need))
            {
                player.sendSystemMessage(Component.literal("咒力不足，最大输出未能完成。"));
                return;
            }
        }
        if (usesSkillCooldown(player))
        {
            setSkillCooldown(player, skill, now + getSkillCooldownTicks(player, skill));
        }
        castSkill(player, skillId);
    }

    protected final void castWithSkillEconomy(ServerPlayer player, TechniqueSkill skill, Runnable cast)
    {
        if (!isSkillUnlocked(player, skill))
        {
            player.sendSystemMessage(Component.literal("该招式尚未解锁。"));
            return;
        }
        int skillLevel = getSkillLevel(player, skill);
        if (skillLevel <= 0)
        {
            player.sendSystemMessage(Component.literal("该招式等级异常，无法释放。"));
            return;
        }
        long now = player.level().getGameTime();
        if (usesSkillCooldown(player))
        {
            int remain = getRemainingSkillCooldownTicks(player, skill, now);
            if (remain > 0)
            {
                player.sendSystemMessage(Component.literal("招式冷却中：" + formatCooldownSeconds(remain) + " 秒"));
                return;
            }
        }
        if (!player.isCreative())
        {
            int cost = skill.cost();
            if (cost > 0 && !PlayerCursedEnergyData.tryConsume(player, cost))
            {
                player.sendSystemMessage(Component.literal("咒力不足，无法释放该招式。"));
                return;
            }
        }
        if (usesSkillCooldown(player))
        {
            setSkillCooldown(player, skill, now + getSkillCooldownTicks(player, skill));
        }
        cast.run();
    }

    protected boolean isSkillUnlocked(Player player, TechniqueSkill skill)
    {
        return SkillUnlockLogic.isSkillUnlocked(player, techniqueId(), skill.skillId(), skill);
    }

    protected int getSkillLevel(Player player, TechniqueSkill skill)
    {
        String key = skill.levelTag();
        if (key == null || key.isEmpty()) return 0;
        if (!player.getPersistentData().contains(key)) return 1;
        return Math.max(1, player.getPersistentData().getInt(key));
    }

    private int getRemainingSkillCooldownTicks(ServerPlayer player, TechniqueSkill skill, long now)
    {
        String key = skill.cooldownTag();
        if (key == null || key.isEmpty()) return 0;
        long expire = player.getPersistentData().getLong(key);
        return expire > now ? (int) (expire - now) : 0;
    }

    private void setSkillCooldown(ServerPlayer player, TechniqueSkill skill, long expireTick)
    {
        String key = skill.cooldownTag();
        if (key != null && !key.isEmpty())
        {
            player.getPersistentData().putLong(key, expireTick);
        }
    }

    // ======================== 旧槽位抽象方法（保留向后兼容） ========================

    protected abstract int getSlotCost(int slot);

    protected int getSlotCooldownTicks(Player player, int slot)
    {
        return getSlotCooldownTicks(slot);
    }

    protected int getSkillCooldownTicks(Player player, TechniqueSkill skill)
    {
        return skill.cooldownTicks();
    }

    protected abstract int getSlotCooldownTicks(int slot);

    protected abstract boolean isSlotUnlocked(Player player, int slot);

    protected abstract int getSlotSkillLevel(Player player, int slot);

    protected abstract String getSlotCooldownTag(int slot);

    public abstract String getSlotDisplayName(int slot);

    public abstract String getSlotIconPath(int slot);

    private int getRemainingCooldownTicks(ServerPlayer player, int slot, long now)
    {
        String key = getSlotCooldownTag(slot);
        if (key == null)
        {
            return 0;
        }
        long expire = player.getPersistentData().getLong(key);
        return expire > now ? (int) (expire - now) : 0;
    }

    private void setSlotCooldown(ServerPlayer player, int slot, long expireTick)
    {
        String key = getSlotCooldownTag(slot);
        if (key != null)
        {
            player.getPersistentData().putLong(key, expireTick);
        }
    }

    protected static boolean usesSkillCooldown(Player player)
    {
        return !player.isCreative() && !ModRules.SkillNoCooldown.isEnabled();
    }

    private static String formatCooldownSeconds(int ticks)
    {
        double sec = ticks / 20.0D;
        return String.format(java.util.Locale.ROOT, "%.1f", sec);
    }
}
