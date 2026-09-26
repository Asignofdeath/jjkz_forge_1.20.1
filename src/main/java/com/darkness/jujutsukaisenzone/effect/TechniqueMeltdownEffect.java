package com.darkness.jujutsukaisenzone.effect;

import com.darkness.jujutsukaisenzone.registry.ModEffects;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

/**
 * 术式熔断：领域关闭后一段时间内无法使用术式技能与领域展开。
 * <p>
 * 仅用作状态标记与 HUD 显示；拦截逻辑在
 * {@link com.darkness.jujutsukaisenzone.technique.skillset.TechniqueSkillSetRegistry}
 * 与 {@link com.darkness.jujutsukaisenzone.technique.domain.AbstractDomainExpansion#cast} 中处理。
 */
public class TechniqueMeltdownEffect extends MobEffect
{
    /** 领域关闭后熔断持续时间：60 秒。 */
    public static final int DURATION_TICKS = 20 * 60;

    public TechniqueMeltdownEffect()
    {
        super(MobEffectCategory.HARMFUL, 0x4A0E0E);
    }

    public static boolean isActive(LivingEntity entity)
    {
        return entity.hasEffect(ModEffects.TECHNIQUE_MELTDOWN.get());
    }

    /** 领域关闭时赋予施术者术式熔断。 */
    public static void applyOnDomainClosed(ServerPlayer player)
    {
        player.addEffect(new MobEffectInstance(
                ModEffects.TECHNIQUE_MELTDOWN.get(),
                DURATION_TICKS,
                0,
                false,
                false,
                true
        ));
        player.displayClientMessage(
                Component.literal("术式熔断（60秒）")
                        .withStyle(ChatFormatting.DARK_RED),
                true
        );
    }

    /** 清除术式熔断（反转修复等）。 */
    public static void clear(ServerPlayer player)
    {
        player.removeEffect(ModEffects.TECHNIQUE_MELTDOWN.get());
    }

    public static boolean blockTechniqueSkillUse(ServerPlayer player)
    {
        if (!isActive(player))
        {
            return false;
        }
        player.displayClientMessage(
                Component.literal("术式熔断中，无法使用术式技能")
                        .withStyle(ChatFormatting.RED),
                true
        );
        return true;
    }

    public static boolean blockDomainExpansion(ServerPlayer player)
    {
        if (!isActive(player))
        {
            return false;
        }
        player.displayClientMessage(
                Component.literal("术式熔断中，无法展开领域")
                        .withStyle(ChatFormatting.RED),
                true
        );
        return true;
    }
}
