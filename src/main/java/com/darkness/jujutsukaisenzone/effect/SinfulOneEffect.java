package com.darkness.jujutsukaisenzone.effect;

import com.darkness.jujutsukaisenzone.player.PlayerCrimeRecordData;
import com.darkness.jujutsukaisenzone.registry.ModEffects;
import com.darkness.jujutsukaisenzone.technique.shikigami.tenshadows.ability.mahoraga.MahoragaAdaptation;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

/**
 * 罪恶者：被审判判定为【有罪】后获得；击杀中立 / 友好生物亦会获得；累计挖掉 50 颗原木、
 * 超过 10 颗矿石方块等行为亦会获得；持有该状态的目标再次被审判时必定【有罪】。
 */
public final class SinfulOneEffect extends MobEffect
{
    /** 持续时间：8 分钟。 */
    public static final int DURATION_TICKS = 20 * 60 * 8;

    public SinfulOneEffect()
    {
        super(MobEffectCategory.HARMFUL, 0x4A235A);
    }

    public static boolean isActive(LivingEntity entity)
    {
        return entity.hasEffect(ModEffects.SINFUL_ONE.get());
    }

    public static void apply(LivingEntity target)
    {
        apply(target, null, null);
    }

    public static void apply(LivingEntity target, PlayerCrimeRecordData.Category category, String description)
    {
        apply(target, category, description, null);
    }

    public static void apply(
            LivingEntity target,
            PlayerCrimeRecordData.Category category,
            String description,
            BlockPos recordPosition)
    {
        if (isActive(target))
        {
            return;
        }
        if (category == PlayerCrimeRecordData.Category.JUDGMENT_TECHNIQUE
                && MahoragaAdaptation.tryEngage(target, "judgment_sinful_one"))
        {
            return;
        }
        target.addEffect(new MobEffectInstance(
                ModEffects.SINFUL_ONE.get(),
                DURATION_TICKS,
                0,
                false,
                false,
                true));

        if (category != null && description != null && !description.isBlank()
                && target instanceof ServerPlayer player)
        {
            BlockPos pos = recordPosition != null ? recordPosition : player.blockPosition();
            long dayTime = player.level().getDayTime();
            PlayerCrimeRecordData.addRecord(player, category, dayTime, pos, description);
        }
    }
}
