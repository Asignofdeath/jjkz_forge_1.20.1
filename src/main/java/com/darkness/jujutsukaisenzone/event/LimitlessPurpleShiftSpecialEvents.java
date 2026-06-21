package com.darkness.jujutsukaisenzone.event;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.compat.EpicFightCompat;
import com.darkness.jujutsukaisenzone.entity.SpecialEffectEntity;
import com.darkness.jujutsukaisenzone.player.PlayerTechniqueData;
import com.darkness.jujutsukaisenzone.technique.skillset.LimitlessTechniqueSkillSet;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Shift+3 茈特别招式：无重力上升约 10 格（仅抬升 Y，不锁 X/Z）→ 与槽位 3 相同的茈蓄力/发射流程 → 结束缓降。
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

    /** 由 {@link LimitlessTechniqueSkillSet#castSpecialMove} 在扣费/冷却成功后调用。 */
    public static void begin(ServerPlayer player)
    {
        if (getState(player) != STATE_NONE)
        {
            abort(player);
        }
        CompoundTag tag = player.getPersistentData();
        tag.putInt(TAG_STATE, STATE_RISING);
        tag.putDouble(TAG_TARGET_Y, player.getY() + RISE_TARGET_BLOCKS);
        player.setNoGravity(true);
    }

    public static boolean isActive(ServerPlayer player)
    {
        return getState(player) != STATE_NONE;
    }

    private static int getState(ServerPlayer player)
    {
        return player.getPersistentData().getInt(TAG_STATE);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer sp))
        {
            return;
        }
        int st = getState(sp);
        if (st == STATE_NONE)
        {
            return;
        }
        if (!EpicFightCompat.isBattleMode(sp) || !PlayerTechniqueData.isLimitless(sp))
        {
            abort(sp);
            return;
        }

        ServerLevel level = sp.serverLevel();
        CompoundTag t = sp.getPersistentData();
        double targetY = t.getDouble(TAG_TARGET_Y);

        if (st == STATE_RISING)
        {
            // 只沿当前位置抬升 Y，X/Z 完全由玩家移动决定；已达/超过目标高度则进入蓄力（不向下拉回）。
            if (sp.getY() >= targetY - Y_EPSILON)
            {
                LimitlessTechniqueSkillSet.INSTANCE.castPurpleMiniNormal(sp);
                t.putInt(TAG_STATE, STATE_HOVER);
                // +1：与实体首刻扣减蓄力 tick 对齐，避免悬停提前结束导致茈未发射。
                long releaseAt = level.getGameTime() + (long) SpecialEffectEntity.limitlessPurpleMiniChargeTicks() + 1L;
                t.putLong(TAG_RELEASE_AT, releaseAt);
            }
            else
            {
                double ny = Math.min(sp.getY() + RISE_PER_TICK, targetY);
                sp.teleportTo(level, sp.getX(), ny, sp.getZ(), sp.getYRot(), sp.getXRot());
            }
        }
        else if (st == STATE_HOVER)
        {
            if (level.getGameTime() >= t.getLong(TAG_RELEASE_AT))
            {
                complete(sp);
            }
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event)
    {
        if (event.getEntity() instanceof ServerPlayer sp)
        {
            abort(sp);
        }
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
    public static void abort(ServerPlayer player)
    {
        if (getState(player) == STATE_NONE)
        {
            return;
        }
        clearTags(player);
        player.setNoGravity(false);
        player.resetFallDistance();
    }

    private static void complete(ServerPlayer player)
    {
        clearTags(player);
        player.setNoGravity(false);
        player.resetFallDistance();
        player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, SLOW_FALLING_DURATION_TICKS, 0, false, false, true));
    }

    private static void clearTags(Player player)
    {
        CompoundTag tag = player.getPersistentData();
        tag.remove(TAG_STATE);
        tag.remove(TAG_TARGET_Y);
        tag.remove(TAG_RELEASE_AT);
    }
}
