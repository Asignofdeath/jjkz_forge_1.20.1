package com.darkness.jujutsukaisenzone.dismemberment;

import com.darkness.jujutsukaisenzone.compat.McgDismembermentCompat;
import com.darkness.jujutsukaisenzone.compat.McgPresence;
import com.darkness.jujutsukaisenzone.heaven.HeavenBalance;
import com.darkness.jujutsukaisenzone.technique.SkillDefenseResolver;
import com.darkness.jujutsukaisenzone.technique.se.CutMarkBaseBonusSeMechanic;
import com.darkness.jujutsukaisenzone.technique.se.CutMarkCapBonusSeMechanic;
import com.darkness.jujutsukaisenzone.technique.se.CutMarkInflictBonusSeMechanic;
import com.darkness.jujutsukaisenzone.technique.se.CutMarkRecoverySeMechanic;
import com.darkness.jujutsukaisenzone.technique.se.DismembermentBleedImmuneSeMechanic;
import com.darkness.jujutsukaisenzone.technique.se.SeMechanicRegistry;
import com.darkness.jujutsukaisenzone.technique.skill.RatioBodyPart;
import com.darkness.jujutsukaisenzone.technique.skillset.SkillDamageType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/**
 * 本模组断肢系统门面（上司）。
 * <p>
 * 通用行为：每肢切痕（上限随生命缩放）、加痕受护甲与体术抗性减免、满痕才可断肢。
 * 目标对策 SE（抬上限 / 快愈 / 不流血）经 {@link HeavenBalance#findActive}；
 * 攻击方进攻 SE（base 加成 / 加痕倍率）经装载表查询。
 */
public final class DismembermentSystem
{
    /** 切痕上限下限（生命再低也不低于此）。 */
    public static final int CUT_MARK_CAP_MIN = 8;
    /** 切痕上限绝对封顶（无抬上限 SE 时）。 */
    public static final int CUT_MARK_CAP_MAX = 64;
    /** 每点最大生命贡献的切痕上限。 */
    public static final float CUT_MARK_CAP_PER_MAX_HEALTH = 0.40F;
    /** 本次伤害换算切痕：每点伤害贡献。 */
    public static final float CUT_MARKS_PER_DAMAGE = 0.15F;
    /** 自然痊愈：每隔多少 tick 各肢切痕 -1（无快愈 SE 时）。 */
    public static final int CUT_MARK_DECAY_INTERVAL_TICKS = 40;

    private DismembermentSystem()
    {
    }

    /**
     * 该目标每肢切痕上限。
     * 基础：{@code clamp(ceil(maxHP×0.4), 8, 64)}；
     * 有 {@link CutMarkCapBonusSeMechanic}（玩家装载或咒灵天道）再加生命额外量，软顶可至 SE 声明值。
     */
    public static int cutMarkCap(LivingEntity target)
    {
        if (target == null)
        {
            return CUT_MARK_CAP_MIN;
        }
        float maxHp = target.getMaxHealth();
        int fromHealth = Mth.ceil(maxHp * CUT_MARK_CAP_PER_MAX_HEALTH);
        int hardMax = CUT_MARK_CAP_MAX;
        var bonus = HeavenBalance.findActive(target, CutMarkCapBonusSeMechanic.class);
        if (bonus.isPresent())
        {
            fromHealth += Mth.ceil(maxHp * bonus.get().extraCapPerMaxHealth());
            hardMax = Math.max(hardMax, bonus.get().softCapMax());
        }
        return Mth.clamp(fromHealth, CUT_MARK_CAP_MIN, hardMax);
    }

    public static int getCutMarks(LivingEntity target, RatioBodyPart part)
    {
        return CutMarkData.get(target, part);
    }

    public static boolean isCutMarkFull(LivingEntity target, RatioBodyPart part)
    {
        if (target == null || part == null || part == RatioBodyPart.TORSO)
        {
            return false;
        }
        return getCutMarks(target, part) >= cutMarkCap(target);
    }

    /**
     * 对某肢施加切痕。
     * <p>
     * 原始量 = {@code baseMarks} + {@code damageDealt} × {@link #CUT_MARKS_PER_DAMAGE}；
     * 再乘 {@code (1 - 护甲减伤) × (1 - 体术抗性减伤)}；向下取整后累加，不超过该肢上限。
     *
     * @return 实际加上的切痕层数
     */
    public static int applyCutMarks(
            @Nullable LivingEntity attacker,
            LivingEntity target,
            RatioBodyPart part,
            float baseMarks,
            float damageDealt)
    {
        if (target == null || part == null || !target.isAlive() || part == RatioBodyPart.TORSO)
        {
            return 0;
        }
        if (isMissing(target, part))
        {
            return 0;
        }
        float effectiveBase = Math.max(0.0F, baseMarks);
        if (attacker instanceof Player attackerPlayer)
        {
            var baseBonus = SeMechanicRegistry.findEquipped(
                    attackerPlayer, CutMarkBaseBonusSeMechanic.class);
            if (baseBonus.isPresent())
            {
                effectiveBase += Math.max(0.0F, baseBonus.get().bonusBaseMarks());
            }
        }
        float raw = effectiveBase + Math.max(0.0F, damageDealt) * CUT_MARKS_PER_DAMAGE;
        if (raw <= 0.0F)
        {
            return 0;
        }
        double armor = SkillDefenseResolver.armorReduction(target, attacker);
        double martial = SkillDefenseResolver.resistanceReduction(
                target, SkillDamageType.MARTIAL, attacker);
        double multiplier = (1.0D - armor) * (1.0D - martial);
        multiplier = Math.max(SkillDefenseResolver.multiplierFloor(), Math.min(1.0D, multiplier));
        float addF = raw * (float) multiplier;
        if (attacker instanceof Player attackerPlayer)
        {
            var inflict = SeMechanicRegistry.findEquipped(
                    attackerPlayer, CutMarkInflictBonusSeMechanic.class);
            if (inflict.isPresent())
            {
                addF *= Math.max(0.0F, inflict.get().inflictMultiplier());
            }
        }
        int add = Mth.floor(addF);
        if (add <= 0)
        {
            return 0;
        }
        int cap = cutMarkCap(target);
        int current = CutMarkData.get(target, part);
        int room = Math.max(0, cap - current);
        int applied = Math.min(add, room);
        if (applied <= 0)
        {
            return 0;
        }
        CutMarkData.set(target, part, current + applied);
        return applied;
    }

    /**
     * 尝试断肢：该肢切痕须已满；成功后清痕并调 mcg。
     * 目标若有 {@link DismembermentBleedImmuneSeMechanic}（装载或天道）则不启动流血。
     * 对咒灵断肢成功会抬本维天道断肢压力。
     */
    public static boolean trySever(@Nullable LivingEntity attacker, LivingEntity target, RatioBodyPart part)
    {
        if (target == null || part == null || !target.isAlive())
        {
            return false;
        }
        if (part == RatioBodyPart.TORSO)
        {
            return false;
        }
        if (!isCutMarkFull(target, part))
        {
            return false;
        }
        if (!McgPresence.isLoaded())
        {
            return false;
        }
        boolean startBleed = !isBleedImmune(target);
        if (!McgDismembermentCompat.sever(target, part, startBleed))
        {
            return false;
        }
        CutMarkData.set(target, part, 0);
        HeavenBalance.onCurseSpiritSevered(target);
        return true;
    }

    /** @deprecated 改用 {@link #trySever(LivingEntity, LivingEntity, RatioBodyPart)} */
    @Deprecated
    public static boolean trySever(LivingEntity target, RatioBodyPart part)
    {
        return trySever(null, target, part);
    }

    public static boolean isMissing(LivingEntity target, RatioBodyPart part)
    {
        if (target == null || part == null || !McgPresence.isLoaded())
        {
            return false;
        }
        return McgDismembermentCompat.isMissing(target, part);
    }

    public static boolean isBleedImmune(LivingEntity target)
    {
        return HeavenBalance.findActive(target, DismembermentBleedImmuneSeMechanic.class).isPresent();
    }

    /** 服务端 tick：有切痕时按间隔自然减层（快愈 SE 缩短间隔 / 加大减量）。 */
    public static void tickDecay(LivingEntity living)
    {
        if (living == null || living.level().isClientSide || !CutMarkData.hasAny(living))
        {
            return;
        }
        int interval = CUT_MARK_DECAY_INTERVAL_TICKS;
        int amount = 1;
        var recovery = HeavenBalance.findActive(living, CutMarkRecoverySeMechanic.class);
        if (recovery.isPresent())
        {
            interval = Math.max(1, recovery.get().decayIntervalTicks());
            amount = Math.max(1, recovery.get().decayAmount());
        }
        if ((living.tickCount % interval) != 0)
        {
            return;
        }
        CutMarkData.decayAll(living, amount);
    }
}
