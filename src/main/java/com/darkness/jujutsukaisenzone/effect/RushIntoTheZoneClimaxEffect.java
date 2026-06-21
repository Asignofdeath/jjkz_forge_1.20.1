package com.darkness.jujutsukaisenzone.effect;

import com.darkness.jujutsukaisenzone.Config;
import com.darkness.jujutsukaisenzone.event.BlackFlashEvents;
import com.darkness.jujutsukaisenzone.player.PlayerBlackFlashData;
import com.darkness.jujutsukaisenzone.registry.ModEffects;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

/**
 * Rush Into The ZONE 最高潮：立刻进入 ZONE III，且下一次体术攻击必定黑闪。
 * <p>
 * 目前仅通过 {@code /jjkz rushIntoTheZoneClimax} 获取。
 * ZONE 实际加成与黑闪判定分别在 {@link BlackFlashEvents} 中处理。
 */
public final class RushIntoTheZoneClimaxEffect extends MobEffect
{
    /** HUD 显示上限；必定黑闪消耗后会提前移除效果。 */
    public static final int DURATION_TICKS = 20 * 60 * 60;

    public RushIntoTheZoneClimaxEffect()
    {
        super(MobEffectCategory.BENEFICIAL, 0xFF4400);
    }

    public static boolean isActive(LivingEntity entity)
    {
        return entity.hasEffect(ModEffects.RUSH_INTO_THE_ZONE_CLIMAX.get());
    }

    /** 赋予 ZONE III 并标记下一次体术攻击必定黑闪。 */
    public static void applyZoneAndGuaranteedBlackFlash(ServerPlayer player)
    {
        int durationTicks = Config.blackFlashZoneDurationTicks;
        BlackFlashEvents.applyZoneState(player, PlayerBlackFlashData.MAX_ZONE_LEVEL, durationTicks);
        PlayerBlackFlashData.setGuaranteedBlackFlash(player, true);
    }

    /** 作弊指令：赋予 Rush Into The ZONE 最高潮。 */
    public static void grant(ServerPlayer player)
    {
        player.addEffect(new MobEffectInstance(
                ModEffects.RUSH_INTO_THE_ZONE_CLIMAX.get(),
                DURATION_TICKS,
                0,
                false,
                false,
                true
        ));
    }

    /** 必定黑闪已消耗时移除效果与标记。 */
    public static void consumeGuaranteedBlackFlash(ServerPlayer player)
    {
        PlayerBlackFlashData.setGuaranteedBlackFlash(player, false);
        player.removeEffect(ModEffects.RUSH_INTO_THE_ZONE_CLIMAX.get());
    }
}
