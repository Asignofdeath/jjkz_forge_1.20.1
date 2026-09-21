package com.darkness.jujutsukaisenzone.event;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.technique.domain.JudgmentDomainTrialLogic;
import com.darkness.jujutsukaisenzone.technique.skill.JudgmentAccuseSkillLogic;
import com.darkness.jujutsukaisenzone.technique.skill.JudgmentJudgeEntityLogic;
import com.darkness.jujutsukaisenzone.technique.skill.JudgmentSoundLogic;
import com.darkness.jujutsukaisenzone.technique.skill.JudgmentTrialSkillLogic;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 「审判」：受击打断、登出清理。
 */
@Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class JudgmentTrialEvents
{
    private JudgmentTrialEvents()
    {
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event)
    {
        if (event.getAmount() <= 0.0F || !(event.getEntity() instanceof ServerPlayer serverPlayer))
        {
            return;
        }
        JudgmentTrialSkillLogic.cancelOnHurt(serverPlayer);
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event)
    {
        if (event.getEntity() instanceof ServerPlayer serverPlayer)
        {
            JudgmentTrialSkillLogic.onLogout(serverPlayer);
            JudgmentAccuseSkillLogic.onLogout(serverPlayer);
            JudgmentDomainTrialLogic.onLogout(serverPlayer);
            JudgmentJudgeEntityLogic.clearPendingAngryAnimation(serverPlayer.getUUID());
            JudgmentSoundLogic.clearPendingDeathPenalty(serverPlayer.getUUID());
            JudgmentTrialSkillLogic.clearPendingVerdictTitle(serverPlayer.getUUID());
        }
    }
}
