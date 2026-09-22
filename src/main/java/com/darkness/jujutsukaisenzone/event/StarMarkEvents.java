package com.darkness.jujutsukaisenzone.event;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.technique.skill.StarMarkRules;
import com.darkness.jujutsukaisenzone.technique.skill.StarMarkSkillLogic;
import com.darkness.jujutsukaisenzone.technique.skillset.LoveRendezvousTechniqueSkillSet;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * 「星星标记」的挂载点。
 * <p>
 * 服务端每 tick：解析标记（失效清理 + 火花表现），随后结算三态力学。<br>
 * 两个打标入口：近战命中实体（{@link AttackEntityEvent}）、看向方块后按「星星标记」技能键
 * （在 {@code LoveRendezvousTechniqueSkillSet#useSkill} 里，不在这里）。
 * <p>
 * 打标之所以用「接触」而不是远距离视线点选，是因为后者等于隔空施法，与原作「必须接触对方咒力」不符。
 * 方块改用技能键也是同一个理由的延伸：右键会与放置方块打架，技能键则由服务端自算射线，不抢原版输入。
 */
@Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class StarMarkEvents
{
    private StarMarkEvents()
    {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide)
        {
            return;
        }
        if (event.player instanceof ServerPlayer player)
        {
            List<StarMarkSkillLogic.ResolvedMark> resolved = StarMarkSkillLogic.resolveAndRender(player);
            StarMarkRules.apply(player, resolved);
        }
    }

    /** 打标入口一：近战命中实体（掉落物也走这一路）。 */
    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event)
    {
        if (event.getEntity().level().isClientSide())
        {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player))
        {
            return;
        }
        Entity target = event.getTarget();
        if (target == null || target == player)
        {
            return;
        }
        LoveRendezvousTechniqueSkillSet.INSTANCE.tryApplyMark(
                player,
                StarMarkSkillLogic.markOfEntity(player, target),
                StarMarkSkillLogic.describeTarget(target));
    }
}
