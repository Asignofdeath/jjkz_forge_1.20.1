package com.darkness.jujutsukaisenzone.event;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.compat.EpicFightCompat;
import com.darkness.jujutsukaisenzone.entity.SpecialEffectEntity;
import com.darkness.jujutsukaisenzone.entity.combat.AiTechniqueStats;
import com.darkness.jujutsukaisenzone.player.PlayerTechniqueData;
import com.darkness.jujutsukaisenzone.technique.skillset.LimitlessTechniqueSkillSet;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Shift+3 茈特别招式：无重力上升约 10 格（仅抬升 Y，不锁 X/Z）→ 与槽位 3 相同的茈蓄力/发射流程 → 结束缓降。
 * <p>
 * 流程对玩家与持有 {@link AiTechniqueStats} 的 AI 术师共用同一套 tag / 时序；玩家额外要求战斗模式与无下限术式。
 */
@Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class LimitlessPurpleShiftSpecialEvents
{
    private static final String TAG_STATE = "jujutsukaisenzone_limitless_purple_shift_state";
    private static final String TAG_TARGET_Y = "jujutsukaisenzone_limitless_purple_shift_target_y";
    private static final String TAG_RELEASE_AT = "jujutsukaisenzone_limitless_purple_shift_release_at";

    private static final int STATE_NONE = 0;
    private static final int STATE_RISING = 1;
    private static final int STATE_HOVER = 2;

    /** 约 20 tick 内升高 10 格。 */
    private static final double RISE_PER_TICK = 0.52D;
    private static final double RISE_TARGET_BLOCKS = 10.0D;
    private static final double Y_EPSILON = 0.02D;

    /** 茈流程结束后的缓降时长。 */
    private static final int SLOW_FALLING_DURATION_TICKS = 20 * 10;

    private LimitlessPurpleShiftSpecialEvents()
    {
    }

    /** 由扣费/冷却成功后调用（玩家槽位或 AI 选招）。 */
    public static void begin(LivingEntity caster)
    {
        if (getState(caster) != STATE_NONE)
        {
            abort(caster);
        }
        CompoundTag tag = caster.getPersistentData();
        tag.putInt(TAG_STATE, STATE_RISING);
        tag.putDouble(TAG_TARGET_Y, caster.getY() + RISE_TARGET_BLOCKS);
        caster.setNoGravity(true);
    }

    public static boolean isActive(LivingEntity caster)
    {
        return caster != null && getState(caster) != STATE_NONE;
    }

    private static int getState(LivingEntity caster)
    {
        return caster == null ? STATE_NONE : caster.getPersistentData().getInt(TAG_STATE);
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event)
    {
        LivingEntity living = event.getEntity();
        if (living.level().isClientSide || getState(living) == STATE_NONE)
        {
            return;
        }
        if (!canContinue(living))
        {
            abort(living);
            return;
        }
        if (!(living.level() instanceof ServerLevel level))
        {
            return;
        }
        CompoundTag t = living.getPersistentData();
        double targetY = t.getDouble(TAG_TARGET_Y);
        int st = getState(living);

        if (st == STATE_RISING)
        {
            if (living.getY() >= targetY - Y_EPSILON)
            {
                LimitlessTechniqueSkillSet.INSTANCE.castPurpleMiniNormal(living);
                t.putInt(TAG_STATE, STATE_HOVER);
                long releaseAt = level.getGameTime() + (long) SpecialEffectEntity.limitlessPurpleMiniChargeTicks() + 1L;
                t.putLong(TAG_RELEASE_AT, releaseAt);
            }
            else
            {
                double ny = Math.min(living.getY() + RISE_PER_TICK, targetY);
                if (living instanceof ServerPlayer sp)
                {
                    sp.teleportTo(level, sp.getX(), ny, sp.getZ(), sp.getYRot(), sp.getXRot());
                }
                else
                {
                    living.teleportTo(living.getX(), ny, living.getZ());
                }
            }
        }
        else if (st == STATE_HOVER)
        {
            if (level.getGameTime() >= t.getLong(TAG_RELEASE_AT))
            {
                complete(living);
            }
        }
    }

    private static boolean canContinue(LivingEntity living)
    {
        if (living instanceof ServerPlayer sp)
        {
            return EpicFightCompat.isBattleMode(sp) && PlayerTechniqueData.isLimitless(sp);
        }
        return AiTechniqueStats.has(living);
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event)
    {
        abort(event.getEntity());
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event)
    {
        if (event.getEntity() instanceof ServerPlayer sp)
        {
            abort(sp);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event)
    {
        if (event.getEntity() instanceof ServerPlayer sp)
        {
            abort(sp);
        }
    }

    /** 中断：清状态、恢复重力；不给予缓降。 */
    public static void abort(LivingEntity caster)
    {
        if (getState(caster) == STATE_NONE)
        {
            return;
        }
        clearTags(caster);
        caster.setNoGravity(false);
        caster.resetFallDistance();
    }

    private static void complete(LivingEntity caster)
    {
        clearTags(caster);
        caster.setNoGravity(false);
        caster.resetFallDistance();
        caster.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, SLOW_FALLING_DURATION_TICKS, 0, false, false, true));
    }

    private static void clearTags(LivingEntity caster)
    {
        CompoundTag tag = caster.getPersistentData();
        tag.remove(TAG_STATE);
        tag.remove(TAG_TARGET_Y);
        tag.remove(TAG_RELEASE_AT);
    }
}
