package com.darkness.jujutsukaisenzone.entity.ai;

import com.darkness.jujutsukaisenzone.entity.GojoSatoruEntity;
import com.darkness.jujutsukaisenzone.entity.combat.AiLimitlessCasts;
import com.darkness.jujutsukaisenzone.entity.combat.AiTechniqueStats;
import com.darkness.jujutsukaisenzone.event.LimitlessPurpleShiftSpecialEvents;
import com.darkness.jujutsukaisenzone.player.PlayerDomainModeData;
import com.darkness.jujutsukaisenzone.technique.domain.DomainExpansionRouter;
import com.darkness.jujutsukaisenzone.technique.domain.LimitlessDomainExpansion;
import com.darkness.jujutsukaisenzone.technique.skillset.LimitlessTechniqueSkillSet;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * 五条悟选招：朝目标看，按距离从已就绪的无下限技能里挑一招。
 * 施法走 {@link AiLimitlessCasts} → {@link LimitlessTechniqueSkillSet} 共用真相。
 */
public class GojoSatoruLimitlessCombatGoal extends Goal
{
    private static final int CAST_INTERVAL_TICKS = 20;
    private static final double RANGE_CLOSE = 8.0D;
    private static final double RANGE_MID = 18.0D;

    private final GojoSatoruEntity gojo;
    private int nextCastTick;

    public GojoSatoruLimitlessCombatGoal(GojoSatoruEntity gojo)
    {
        this.gojo = gojo;
        this.setFlags(EnumSet.of(Flag.LOOK));
    }

    @Override
    public boolean canUse()
    {
        return AiTechniqueStats.has(this.gojo) && this.gojo.getTarget() != null && this.gojo.getTarget().isAlive();
    }

    @Override
    public boolean canContinueToUse()
    {
        return this.canUse();
    }

    @Override
    public void start()
    {
        this.nextCastTick = 0;
    }

    @Override
    public void tick()
    {
        LivingEntity target = this.gojo.getTarget();
        if (target == null)
        {
            return;
        }
        this.gojo.getLookControl().setLookAt(target, 30.0F, 30.0F);
        if (this.isBusy())
        {
            return;
        }
        if (this.gojo.tickCount < this.nextCastTick)
        {
            return;
        }
        this.nextCastTick = this.gojo.tickCount + CAST_INTERVAL_TICKS;
        this.tryCastAt(target);
    }

    private boolean isBusy()
    {
        if (LimitlessPurpleShiftSpecialEvents.isActive(this.gojo)
                || AiLimitlessCasts.isRemoteMurasakiCharging(this.gojo))
        {
            return true;
        }
        return LimitlessDomainExpansion.getInstance().isCastingPending(this.gojo.getUUID());
    }

    private void tryCastAt(LivingEntity target)
    {
        double dist = this.gojo.distanceTo(target);
        Vec3 aim = target.getEyePosition();
        if (dist > RANGE_MID)
        {
            if (AiLimitlessCasts.isReady(this.gojo, LimitlessTechniqueSkillSet.SKILL_REMOTE_PURPLE)
                    && AiLimitlessCasts.tryBeginRemoteMurasaki(this.gojo))
            {
                return;
            }
            if (this.tryDomainIfFar(dist))
            {
                return;
            }
            if (AiLimitlessCasts.tryCastBlue(this.gojo, aim, true))
            {
                return;
            }
        }
        else if (dist > RANGE_CLOSE)
        {
            if (this.gojo.getRandom().nextBoolean()
                    && AiLimitlessCasts.tryCastPurple(this.gojo))
            {
                return;
            }
            if (AiLimitlessCasts.tryCastRed(this.gojo, false))
            {
                return;
            }
            if (AiLimitlessCasts.tryCastBlue(this.gojo, aim, false))
            {
                return;
            }
            this.tryDomainIfFar(dist);
        }
        else
        {
            if (this.gojo.getRandom().nextInt(4) == 0
                    && AiLimitlessCasts.tryCastPurpleShift(this.gojo))
            {
                return;
            }
            if (AiLimitlessCasts.tryCastRed(this.gojo, true))
            {
                return;
            }
            if (this.gojo.getRandom().nextBoolean()
                    && AiLimitlessCasts.tryCastDomain(this.gojo, PlayerDomainModeData.Mode.INSTANT))
            {
                return;
            }
            AiLimitlessCasts.tryCastBlue(this.gojo, aim, false);
        }
    }

    private boolean tryDomainIfFar(double dist)
    {
        if (DomainExpansionRouter.hasActiveDomain(this.gojo.getUUID()))
        {
            return false;
        }
        PlayerDomainModeData.Mode mode = dist > RANGE_MID
                ? PlayerDomainModeData.Mode.OPEN
                : PlayerDomainModeData.Mode.NORMAL;
        return AiLimitlessCasts.tryCastDomain(this.gojo, mode);
    }
}
