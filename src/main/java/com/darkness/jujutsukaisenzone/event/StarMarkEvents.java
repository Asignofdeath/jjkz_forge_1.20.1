package com.darkness.jujutsukaisenzone.event;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.network.ModNetwork;
import com.darkness.jujutsukaisenzone.player.PlayerStarMarkData;
import com.darkness.jujutsukaisenzone.technique.skill.StarFlightSkillLogic;
import com.darkness.jujutsukaisenzone.technique.skill.StarMarkRules;
import com.darkness.jujutsukaisenzone.technique.skill.StarMarkSkillLogic;
import com.darkness.jujutsukaisenzone.technique.skillset.LoveRendezvousTechniqueSkillSet;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * 「星星标记」的挂载点。
 * <p>
 * 服务端每 tick：解析标记（失效清理），随后结算三态力学。<br>
 * 两个打标入口：近战命中实体（{@link AttackEntityEvent}）、看向方块后按「星星标记」技能键
 * （在 {@code LoveRendezvousTechniqueSkillSet#useSkill} 里，不在这里）。
 * <p>
 * 打标之所以用「接触」而不是远距离视线点选，是因为后者等于隔空施法，与原作「必须接触对方咒力」不符。
 * 方块改用技能键也是同一个理由的延伸：右键会与放置方块打架，技能键则由服务端自算射线，不抢原版输入。
 * <p>
 * 喷漆贴花由客户端根据 {@code SyncStarMarksS2CPacket} 渲染；登录 / 换维度时补发同维度玩家的标记快照。
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
            /*
             * 「飞行，穿越」的位移结算挂在同一处：它是纯粹的服务端位移（每 tick 一次位置包），
             * 与标记力学没有耦合，所以放在 apply 之后单独走一步——穿越途中星域已被收掉，
             * 标记力学那一段本来也就只剩尾迹。
             */
            StarFlightSkillLogic.tick(player);
        }
    }

    /** 打标入口一：近战命中实体（掉落物不可标记）。 */
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
        if (target == null || target == player || target instanceof net.minecraft.world.entity.item.ItemEntity)
        {
            return;
        }
        LoveRendezvousTechniqueSkillSet.INSTANCE.tryApplyMark(
                player,
                StarMarkSkillLogic.markOfEntity(player, target),
                StarMarkSkillLogic.describeTarget(target));
    }

    /** 登录：把同维度所有施术者的标记快照推给新人。 */
    @SubscribeEvent
    public static void onLoggedIn(PlayerEvent.PlayerLoggedInEvent event)
    {
        if (!(event.getEntity() instanceof ServerPlayer player))
        {
            return;
        }
        for (ServerPlayer other : player.serverLevel().players())
        {
            ModNetwork.sendStarMarksSyncTo(player, other, PlayerStarMarkData.getAll(other));
        }
    }

    /** 换维度：向新维度广播自己的标记，并接收同维度他人的快照。 */
    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event)
    {
        if (!(event.getEntity() instanceof ServerPlayer player))
        {
            return;
        }
        ModNetwork.sendStarMarksSync(player, PlayerStarMarkData.getAll(player));
        for (ServerPlayer other : player.serverLevel().players())
        {
            if (other != player)
            {
                ModNetwork.sendStarMarksSyncTo(player, other, PlayerStarMarkData.getAll(other));
            }
        }
    }

    /** 登出：通知同维度客户端清掉该施术者的贴花。 */
    @SubscribeEvent
    public static void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent event)
    {
        if (!(event.getEntity() instanceof ServerPlayer player))
        {
            return;
        }
        ModNetwork.sendStarMarksSync(player, List.of());
    }
}
