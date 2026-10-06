package com.darkness.jujutsukaisenzone.entity.combat;

import com.darkness.jujutsukaisenzone.event.LimitlessPurpleShiftSpecialEvents;
import com.darkness.jujutsukaisenzone.player.PlayerDomainModeData;
import com.darkness.jujutsukaisenzone.technique.domain.AiDomainCasterRegistry;
import com.darkness.jujutsukaisenzone.technique.domain.AiDomainModeData;
import com.darkness.jujutsukaisenzone.technique.skillset.LimitlessTechniqueSkillSet;
import com.darkness.jujutsukaisenzone.technique.skillset.TechniqueSkill;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * 无下限 AI 施法适配：扣咒力 / 写冷却后调用 {@link LimitlessTechniqueSkillSet} 共用施法真相。
 * <p>
 * 技能等级、数值、动画与机制不在这里另写一份；这里只负责「怎么放」。
 */
public final class AiLimitlessCasts
{
    public static final String TAG_REMOTE_MURASAKI_ENTITY = "jujutsukaisenzone_ai_remote_murasaki_uuid";

    private AiLimitlessCasts()
    {
    }

    public static boolean tryCastBlue(LivingEntity caster, Vec3 targetPos, boolean allowOwnerPull)
    {
        if (!tryPay(caster, LimitlessTechniqueSkillSet.SKILL_BLUE))
        {
            return false;
        }
        LimitlessTechniqueSkillSet.INSTANCE.castBlueNormal(caster, allowOwnerPull, targetPos);
        return true;
    }

    public static boolean tryCastRed(LivingEntity caster, boolean allowOwnerKnockback)
    {
        if (!tryPay(caster, LimitlessTechniqueSkillSet.SKILL_RED))
        {
            return false;
        }
        LimitlessTechniqueSkillSet.INSTANCE.castRedNormal(caster, allowOwnerKnockback);
        return true;
    }

    public static boolean tryCastPurple(LivingEntity caster)
    {
        if (!tryPay(caster, LimitlessTechniqueSkillSet.SKILL_PURPLE))
        {
            return false;
        }
        LimitlessTechniqueSkillSet.INSTANCE.castPurpleMiniNormal(caster);
        return true;
    }

    public static boolean tryCastPurpleShift(LivingEntity caster)
    {
        if (LimitlessPurpleShiftSpecialEvents.isActive(caster))
        {
            return false;
        }
        if (!tryPay(caster, LimitlessTechniqueSkillSet.SKILL_PURPLE))
        {
            return false;
        }
        LimitlessPurpleShiftSpecialEvents.begin(caster);
        return true;
    }

    public static boolean tryBeginRemoteMurasaki(LivingEntity caster)
    {
        if (isRemoteMurasakiCharging(caster))
        {
            return false;
        }
        if (!tryPay(caster, LimitlessTechniqueSkillSet.SKILL_REMOTE_PURPLE))
        {
            return false;
        }
        UUID uuid = LimitlessTechniqueSkillSet.INSTANCE.beginRemoteMurasakiHoldRelease(caster);
        if (uuid == null)
        {
            return false;
        }
        caster.getPersistentData().putUUID(TAG_REMOTE_MURASAKI_ENTITY, uuid);
        return true;
    }

    public static boolean tryCastDomain(LivingEntity caster, PlayerDomainModeData.Mode mode)
    {
        if (caster == null || caster.level().isClientSide)
        {
            return false;
        }
        AiDomainModeData.set(caster, mode == null ? PlayerDomainModeData.Mode.NORMAL : mode);
        return AiDomainCasterRegistry.castLimitlessDomain(caster);
    }

    public static boolean isReady(LivingEntity caster, String skillId)
    {
        TechniqueSkill skill = LimitlessTechniqueSkillSet.INSTANCE.skillDefinition(skillId);
        if (skill == null || !AiTechniqueStats.has(caster))
        {
            return false;
        }
        if (remainingCooldownTicks(caster, skill) > 0)
        {
            return false;
        }
        return AiTechniqueStats.cursedEnergyCurrent(caster) >= skill.cost();
    }

    public static boolean isRemoteMurasakiCharging(LivingEntity caster)
    {
        if (caster == null || !caster.getPersistentData().hasUUID(TAG_REMOTE_MURASAKI_ENTITY))
        {
            return false;
        }
        UUID uuid = caster.getPersistentData().getUUID(TAG_REMOTE_MURASAKI_ENTITY);
        if (caster.level() instanceof ServerLevel level && level.getEntity(uuid) != null)
        {
            return true;
        }
        caster.getPersistentData().remove(TAG_REMOTE_MURASAKI_ENTITY);
        return false;
    }

    public static void copyCooldownTagsTo(CompoundTag dest, LivingEntity from)
    {
        if (dest == null || from == null)
        {
            return;
        }
        CompoundTag data = from.getPersistentData();
        for (TechniqueSkill skill : LimitlessTechniqueSkillSet.INSTANCE.getConfigurableSkills())
        {
            String key = skill.cooldownTag();
            if (key != null && !key.isEmpty() && data.contains(key))
            {
                dest.putLong(key, data.getLong(key));
            }
        }
    }

    public static void restoreCooldownTags(LivingEntity to, CompoundTag src)
    {
        if (to == null || src == null)
        {
            return;
        }
        CompoundTag data = to.getPersistentData();
        for (TechniqueSkill skill : LimitlessTechniqueSkillSet.INSTANCE.getConfigurableSkills())
        {
            String key = skill.cooldownTag();
            if (key != null && !key.isEmpty() && src.contains(key))
            {
                data.putLong(key, src.getLong(key));
            }
        }
    }

    private static boolean tryPay(LivingEntity caster, String skillId)
    {
        TechniqueSkill skill = LimitlessTechniqueSkillSet.INSTANCE.skillDefinition(skillId);
        if (skill == null || !AiTechniqueStats.has(caster))
        {
            return false;
        }
        if (remainingCooldownTicks(caster, skill) > 0)
        {
            return false;
        }
        if (!AiTechniqueStats.tryConsume(caster, skill.cost()))
        {
            return false;
        }
        int cd = LimitlessTechniqueSkillSet.INSTANCE.skillCooldownTicksFor(caster, skill);
        if (cd > 0)
        {
            String key = skill.cooldownTag();
            if (key != null && !key.isEmpty())
            {
                caster.getPersistentData().putLong(key, caster.level().getGameTime() + cd);
            }
        }
        return true;
    }

    private static int remainingCooldownTicks(LivingEntity caster, TechniqueSkill skill)
    {
        String key = skill.cooldownTag();
        if (key == null || key.isEmpty())
        {
            return 0;
        }
        long expire = caster.getPersistentData().getLong(key);
        long now = caster.level().getGameTime();
        return expire > now ? (int) (expire - now) : 0;
    }
}
