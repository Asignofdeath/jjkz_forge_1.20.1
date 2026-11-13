package com.darkness.jujutsukaisenzone.compat;

import com.darkness.jujutsukaisenzone.technique.skill.RatioBodyPart;
import de.anwender.mcg.anatomy.AnatomyProfile;
import de.anwender.mcg.anatomy.AnatomyProfiles;
import de.anwender.mcg.anatomy.BodyPart;
import de.anwender.mcg.anatomy.DismembermentData;
import de.anwender.mcg.entity.SeveredPartEntity;
import de.anwender.mcg.network.McgNetwork;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;

/**
 * PPs（mcg）断肢 API 调用层。仅在 {@link McgPresence#isLoaded()} 为 true 时由
 * {@link com.darkness.jujutsukaisenzone.dismemberment.DismembermentSystem} 加载本类。
 * <p>
 * 被动伤害断肢由 {@code DismembermentEngineMixin} 掐死；本类是唯一允许下令断肢的出口。
 */
public final class McgDismembermentCompat
{
    private McgDismembermentCompat()
    {
    }

    /**
     * 强制切断指定部位（含断肢实体、可选流血、客户端同步）。躯干等不可切部位返回 false。
     *
     * @param startBleed 是否启动 mcg 流血（亡灵仍不会流血）
     * @return 是否新切断了该部位
     */
    public static boolean sever(LivingEntity target, RatioBodyPart part, boolean startBleed)
    {
        if (target == null || part == null || target.level().isClientSide)
        {
            return false;
        }
        BodyPart mcgPart = toMcg(part);
        if (mcgPart == null)
        {
            return false;
        }
        AnatomyProfile profile = AnatomyProfiles.resolve(target).orElse(null);
        if (profile == null || !profile.canSever(mcgPart))
        {
            return false;
        }
        if (DismembermentData.isMissing(target, mcgPart))
        {
            return false;
        }
        if (!DismembermentData.sever(target, mcgPart))
        {
            return false;
        }
        if (startBleed && target.getMobType() != MobType.UNDEAD)
        {
            DismembermentData.startBleeding(target);
        }
        target.refreshDimensions();
        if (target.level() instanceof ServerLevel serverLevel)
        {
            SeveredPartEntity.spawn(serverLevel, target, mcgPart, profile.bodyPlan());
        }
        McgNetwork.sync(target);
        if (mcgPart == BodyPart.HEAD && profile.decapitationKills())
        {
            target.hurt(target.damageSources().genericKill(), Float.MAX_VALUE);
        }
        return true;
    }

    /** @deprecated 改用 {@link #sever(LivingEntity, RatioBodyPart, boolean)} */
    @Deprecated
    public static boolean sever(LivingEntity target, RatioBodyPart part)
    {
        return sever(target, part, true);
    }

    public static boolean isMissing(LivingEntity target, RatioBodyPart part)
    {
        BodyPart mcgPart = toMcg(part);
        return mcgPart != null && DismembermentData.isMissing(target, mcgPart);
    }

    private static BodyPart toMcg(RatioBodyPart part)
    {
        return switch (part)
        {
            case HEAD -> BodyPart.HEAD;
            case TORSO -> BodyPart.TORSO;
            case LEFT_ARM -> BodyPart.LEFT_ARM;
            case RIGHT_ARM -> BodyPart.RIGHT_ARM;
            case LEFT_LEG -> BodyPart.LEFT_LEG;
            case RIGHT_LEG -> BodyPart.RIGHT_LEG;
        };
    }
}
