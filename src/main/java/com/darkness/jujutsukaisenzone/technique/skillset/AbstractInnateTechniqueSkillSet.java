package com.darkness.jujutsukaisenzone.technique.skillset;

import com.darkness.jujutsukaisenzone.compat.EpicFightCompat;
import com.darkness.jujutsukaisenzone.player.PlayerCursedEnergyData;
import com.darkness.jujutsukaisenzone.player.PlayerMaxOutputChargeData;
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
        if (!player.isCreative())
        {
            int remain = getRemainingCooldownTicks(player, slot, now);
            if (remain > 0)
            {
                player.sendSystemMessage(Component.literal("招式冷却中：" + formatCooldownSeconds(remain) + " 秒"));
                return;
            }
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
        if (!player.isCreative())
        {
            int remain = getRemainingCooldownTicks(player, slot, now);
            if (remain > 0)
            {
                player.sendSystemMessage(Component.literal("招式冷却中，最大输出未能完成。"));
                return;
            }
            int need = getSlotCost(slot) * getMaxOutputCursedEnergyMultiplier();
            if (need > 0 && !PlayerCursedEnergyData.tryConsume(player, need))
            {
                player.sendSystemMessage(Component.literal("咒力不足，最大输出未能完成。"));
                return;
            }
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
        if (!player.isCreative())
        {
            long now = player.level().getGameTime();
            int remain = getRemainingCooldownTicks(player, slot, now);
            if (remain > 0)
            {
                player.sendSystemMessage(Component.literal("招式冷却中：" + formatCooldownSeconds(remain) + " 秒"));
                return;
            }
            int cost = getSlotCost(slot);
            if (cost > 0 && !PlayerCursedEnergyData.tryConsume(player, cost))
            {
                player.sendSystemMessage(Component.literal("咒力不足，无法释放该招式。"));
                return;
            }
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
        castWithSkillEconomy(player, skill, () -> castSpecialMoveBySkill(player, skillId, modifierMask));
    }

    /** 按技能 ID 释放（子类实现具体演出）。 */
    protected void castSkill(ServerPlayer player, String skillId)
    {
    }

    /** 按技能 ID 释放特别招式变体（子类实现）。 */
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
        if (!player.isCreative())
        {
            int remain = getRemainingSkillCooldownTicks(player, skill, now);
            if (remain > 0)
            {
                player.sendSystemMessage(Component.literal("招式冷却中：" + formatCooldownSeconds(remain) + " 秒"));
                return;
            }
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
        if (!player.isCreative())
        {
            int remain = getRemainingSkillCooldownTicks(player, skill, now);
            if (remain > 0)
            {
                player.sendSystemMessage(Component.literal("招式冷却中，最大输出未能完成。"));
                return;
            }
            int need = skill.cost() * getMaxOutputCursedEnergyMultiplier();
            if (need > 0 && !PlayerCursedEnergyData.tryConsume(player, need))
            {
                player.sendSystemMessage(Component.literal("咒力不足，最大输出未能完成。"));
                return;
            }
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
        if (!player.isCreative())
        {
            long now = player.level().getGameTime();
            int remain = getRemainingSkillCooldownTicks(player, skill, now);
            if (remain > 0)
            {
                player.sendSystemMessage(Component.literal("招式冷却中：" + formatCooldownSeconds(remain) + " 秒"));
                return;
            }
            int cost = skill.cost();
            if (cost > 0 && !PlayerCursedEnergyData.tryConsume(player, cost))
            {
                player.sendSystemMessage(Component.literal("咒力不足，无法释放该招式。"));
                return;
            }
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

    private static String formatCooldownSeconds(int ticks)
    {
        double sec = ticks / 20.0D;
        return String.format(java.util.Locale.ROOT, "%.1f", sec);
    }
}
