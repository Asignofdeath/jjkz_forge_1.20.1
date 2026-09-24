package com.darkness.jujutsukaisenzone.technique.domain;

import com.darkness.jujutsukaisenzone.player.PlayerTechniqueData;
import com.darkness.jujutsukaisenzone.technique.skill.StarMarkRules;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.Deque;

/**
 * 星间飞行术式领域展开 —— 爱与幽会。
 * <p>
 * 与其它术式领域同源（{@link AbstractTechniqueDomain}）：跟随施法者、固定半径
 * {@code AbstractDomainExpansion.DOMAIN_RADIUS}、咒力耗尽或离开范围即崩解。本类只负责
 * 「开 / 关」——领域成形时登记作用域，关闭或换术式时撤销。数值与判定全部留在
 * {@link StarMarkRules} 与 {@code StarMarkDetonation}，与「星域（禁行）」那套保持同一份口径。
 * <p>
 * <b>领域效果只有一条：把「引爆的传播」升级成连锁。</b>
 * <p>
 * 域外引爆只能把标记传染给半径内「尚未被标记」的干净目标、而且只给 1 级——被炸到的目标离
 * 「可燃的满级」还差 4 次打标，链条在每一环都断在这里，所以域外的表现是「逐个清场」而不是连锁。
 * 域内放宽两条：
 * <ol>
 *   <li>传染给干净目标时<b>直接给满级</b>——被炸到的目标当场成为下一个可燃点；</li>
 *   <li>半径内已有的<b>同一颗星</b>的标记被一并<b>拉满</b>——否则「周围都标过一遍」之后
 *       传染再无落点，链条会在群体内部枯竭。</li>
 * </ol>
 * 于是「炸开一个 → 周围立刻冒出若干可燃点 → 再炸下一个」这条链才连得起来。
 * <p>
 * ⚠️ 规则切换点在 {@code StarMarkDetonation#detonate} 内部按<b>爆心是否在域内</b>自行判断，
 * 因此<b>所有</b>引爆路径（叠满触发、撞击触发、飞行穿越触发）都自动走对应的那一套，
 * 不需要每个调用点各写一遍。
 * <p>
 * 另有一条「星星大碰撞」强化：域内飞行的球改为<b>完全制导</b>（见 {@code StarMarkRules#homeBalls}）。
 * 它存在的意义是让上面那条链的每一环都不落空——连锁靠一次成功的撞击推进，命中率若是随机的，
 * 雪球会自己散掉。（注意球在飞行中豁免芝诺墙，所以必中不会被跨序斥力吃掉。）
 * <p>
 * 演出与视觉表现<i>刻意留空</i>：领域共用黑色天空盒，没有专属粒子 / 音效 / 动画。
 */
public final class LoveRendezvousDomainExpansion extends AbstractTechniqueDomain
{
    private static final LoveRendezvousDomainExpansion INSTANCE = new LoveRendezvousDomainExpansion();

    public static LoveRendezvousDomainExpansion getInstance()
    {
        return INSTANCE;
    }

    /** 领域名称 */
    private static final String DOMAIN_NAME = "爱与幽会";

    private LoveRendezvousDomainExpansion()
    {
        setDomainName(DOMAIN_NAME);
    }

    // ==================== 领域完成 / 存续 / 关闭 ====================

    /**
     * 结界（含天空盒）构建完成：登记领域作用球体。
     * <p>
     * 只登记，不做任何「立刻生效一次」的结算——本领域是<b>事件驱动</b>的（引爆时才传播），
     * 不存在需要预热的状态。域内规则的所有入口都读这份作用域，登记即生效。
     */
    @Override
    protected void applyDomainSpecificEffect(ServerPlayer player, BarrierInstance instance)
    {
        StarMarkRules.activateRendezvousDomain(player, sphereCenter(instance), instance.getRadius());
    }

    @Override
    public void tickMaintain(ServerPlayer player)
    {
        /* 先让父类维持领域（扣咒力、离域崩解、推进展开动画），再处理本领域的作用域 */
        super.tickMaintain(player);

        if (!hasLearnedTechnique(player))
        {
            /*
             * 换了生得术式：这道强化不再属于他。域本身照旧由父类那套规则收尾（离域 / 咒力耗尽），
             * 但域内规则必须立刻停——它们读的是领域作用域，不是术式本身。
             */
            StarMarkRules.deactivateRendezvousDomain(player);
            return;
        }

        Deque<BarrierInstance> queue = ACTIVE_DOMAINS.get(player.getUUID());
        if (queue == null || queue.isEmpty())
        {
            /*
             * 域已经不在了。这条分支同时兜住父类那条「维度取不到 → 静默丢弃实例」的路径：
             * 那种情况下 onBarrierClosed 不会被调用，作用域会留下来变成幽灵状态
             * （表现为「没有领域却仍然必中 / 仍然连锁」）。
             */
            StarMarkRules.deactivateRendezvousDomain(player);
        }
    }

    @Override
    protected void onBarrierClosed(ServerPlayer player, BarrierInstance instance)
    {
        StarMarkRules.deactivateRendezvousDomain(player);
        super.onBarrierClosed(player, instance);
    }

    /** 领域方块中心：与父类的球内判定、其它术式领域同一口径（方块坐标 + 0.5）。 */
    private static Vec3 sphereCenter(BarrierInstance instance)
    {
        BlockPos center = instance.getCenter();
        return new Vec3(center.getX() + 0.5D, center.getY() + 0.5D, center.getZ() + 0.5D);
    }

    // ==================== 契约实现 ====================

    @Override
    protected boolean hasLearnedTechnique(ServerPlayer player)
    {
        return PlayerTechniqueData.isLoveRendezvous(player);
    }

    @Override
    public String getTechniqueType()
    {
        return "love_rendezvous_domain";
    }
}
