package com.darkness.jujutsukaisenzone.technique.skill;

import com.darkness.jujutsukaisenzone.player.PlayerStarMarkData;
import com.darkness.jujutsukaisenzone.player.StarMark;
import com.darkness.jujutsukaisenzone.player.StarMarkType;
import com.darkness.jujutsukaisenzone.registry.ModEffects;
import com.darkness.jujutsukaisenzone.technique.TechniqueDamageSources;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * 星位等级叠满后的<b>引爆</b>结算。
 * <p>
 * 对已经满 {@link StarMark#MAX_STACKS} 级的目标再叠一次同一个标记即触发。引爆做四件事：
 * <ol>
 *   <li><b>消耗</b>——中心目标的标记被清零（不是回落到某一级），所以想再炸一次就必须从头叠满；</li>
 *   <li><b>单体伤害</b>——<b>只有爆心的原目标</b>受到 {@link #DETONATION_DAMAGE}（见 {@link #damageCore}）；</li>
 *   <li><b>赋效果</b>——半径 {@link #RADIUS} 格内的实体获得该星对应的负面效果（见 {@link #applyDebuff}）；</li>
 *   <li><b>传染</b>——同一范围内的实体若<b>尚未持有任何标记</b>，会被打上「同一颗星、星位等级 1」。
 *       域内走的是另一套放宽的规则，见文末「爱与幽会」一节。</li>
 * </ol>
 * <p>
 * <b>「伤害集中、效果扩散」</b>是这里的核心分工：伤害只落在爆心那一个目标身上，因为它引爆的是
 * <b>那颗星</b>而不是一片区域；而范围的表达交给第 3、4 步——负面效果与传染负责把影响铺开。
 * 若伤害也做成 AOE，引爆就成了清场技能，传染这一半的定位会被直接吃掉。
 * <p>
 * 两个刻意的收窄：
 * <ul>
 *   <li><b>不覆盖已有标记</b>：传染只落在「干净」的目标上。已持有别的星的目标若被顺手换星，
 *       等于引爆一次就破坏施术者自己精心布下的星阵，而且会让旁边的满级目标连锁引爆。</li>
 *   <li><b>不作用于施术者自己</b>：自己站在爆心也会被自己的效果命中，所以从波及名单里排除；
 *       伤害侧同样豁免（见 {@link #damageCore}）。</li>
 * </ul>
 * <p>
 * 方块标记同样可以引爆，只是方块本身没有生命值——它的爆心取方块锚点，
 * 伤害为零而效果与传染照常落到周围实体身上；掉落物不是 {@link LivingEntity}，同样不受伤。
 * <p>
 * <b>「爱与幽会」域内的传播是另一套规则</b>（细节见 {@link #spreadMark}）：干净目标直接拿满级、
 * 半径内已有的同星标记被一并拉满，于是被炸到的目标当场就成为下一个可燃点，链条才能连起来。
 * 切换点在 {@link #detonate} 内部按「爆心是否落在域内」自行判断，所以叠满触发 / 撞击触发 /
 * 飞行穿越触发这三条路径全部自动跟随，调用方不需要知情、也不需要各写一遍。
 * <p>
 * <b>爆心的原目标在两种模式下都不参与传染</b>：它的标记刚被消耗掉，若又被传染回来就成了
 * 「回到满级」而不是「清零」，等于站着不动就能被反复引爆。
 */
public final class StarMarkDetonation
{
    /** 赋效果、传染共用的作用半径（格）。 */
    public static final double RADIUS = 4.0D;

    /**
     * 引爆对<b>爆心那一个目标</b>造成的单体伤害（点）。
     * <p>
     * 引爆是这套术式前置成本最高的动作：叠满 {@link StarMark#MAX_STACKS} 级要在星位衰减的时限内连叠 5 次
     * （约 150 咒力），而且引爆会清空全部层数、必须从头再叠。所以它比「踢足球」的常态撞击更痛。
     * <p>
     * 参照系：黑闪冲击波 10 点、撞击 5 ~ 34 点（速度 × 12）、猪 10 血、玩家 20 血。取 22 ⇒
     * 一击毙猪、玩家去掉一整条命。
     * <p>
     * 刻意做成常数而不是「随星位递增」：引爆的触发条件本身就是 {@code stacks == MAX_STACKS}，
     * 星位在这个语境下恒为满级，没有变化空间。真正的成长曲线在「叠满要多久」上，不在伤害数字上。
     */
    public static final float DETONATION_DAMAGE = 22.0F;

    /** 负面效果的持续时间（5 秒）。 */
    public static final int EFFECT_DURATION_TICKS = 100;

    /**
     * Gacrux 的冰冻初值。
     * <p>
     * ⚠️ {@code TicksFrozen} 在离开细雪后<b>每 tick 减 2</b>，所以要「维持 5 秒」必须设两倍的值
     * （100 tick 的观感 → 200），否则实际只冻 2.5 秒。设值时取与当前值的较大者，
     * 避免同一目标被连续引爆时反而把冻结时间缩短。
     */
    private static final int FREEZE_TICKS = EFFECT_DURATION_TICKS * 2;

    /**
     * Gacrux 的定身等级：缓慢 VII。
     * <p>
     * 冰冻本身只减速、且对烈焰人等免疫实体完全无效，所以额外压一个近乎静止的缓慢等级，
     * 让「冰冻 / 定身」里的「定身」在任何目标身上都成立。
     */
    private static final int BIND_SLOW_AMPLIFIER = 6;

    private static final float BURST_VOLUME = 1.0F;
    private static final float BURST_PITCH = 1.4F;

    private StarMarkDetonation()
    {
    }

    /**
     * 引爆的结算结果，供调用方拼动作栏提示。
     *
     * @param damaged   是否真的对爆心目标造成了伤害。引爆在方块 / 掉落物上、爆心是施术者自己、
     *                  或实体已消失时为 {@code false}——这三种情况都只走效果与传染
     * @param affected  吃到负面效果的实体数（不含施术者）
     * @param spread    被传染（新打上标记）的实体数；域内给满级、域外给 1 级
     * @param evicted   因持有上限被顶掉的标记条数（见 {@code PlayerStarMarkData#MAX_MARKS}）——
     *                  传染是免费扩散、不受打标入口的经济约束，所以满员时它会顶掉施术者最早的几颗星
     * @param empowered 域内被一并拉满的「同星已有标记」条数（见 {@link #spreadMark}）；域外恒为 0
     */
    public record Result(boolean damaged, int affected, int spread, int evicted, int empowered)
    {
    }

    /**
     * 引爆一条满级标记：消耗标记、对爆心目标结算单体伤害、表现爆发、给范围内实体赋该星的负面效果、
     * 并把标记传染给范围内的其他目标。
     * <p>
     * 传染的规则<b>按爆心是否落在「爱与幽会」领域内自动分两套</b>——域外只给干净目标打 1 级（默认规则），
     * 域内给满级、并把已有的同星标记一并拉满（连锁规则）。调用方无需传任何标志。
     *
     * @param owner  施术者，标记的持有者；自身不会进入波及名单，也不会吃到伤害
     * @param mark   被引爆的标记（应当是 {@code stacks == MAX_STACKS} 的那条）
     * @param center 爆心（实体标记取其锚点，方块标记取方块锚点）
     */
    public static Result detonate(ServerPlayer owner, StarMark mark, Vec3 center)
    {
        ServerLevel level = owner.serverLevel();

        /* ① 消耗：标记清零，因此这不是「可以无限触发的开关」，而是一次性爆发 */
        PlayerStarMarkData.remove(owner, mark);

        /* ② 单体伤害：只有爆心那一个目标受伤，半径内的其他目标只吃效果与传染（见 damageCore） */
        boolean damaged = damageCore(level, owner, mark);

        /*
         * ③ 表现：星色尘土外扩 + 末影火花 + 一声清脆钟鸣。
         * 刻意不用原版的爆炸粒子与轰鸣——这是「星」的机制，音色偏清冷，与伤害量不挂钩。
         */
        StarMarkSkillLogic.spawnBurst(level, center, mark.type(), RADIUS);
        level.playSound(null, center.x, center.y, center.z,
                SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, BURST_VOLUME, BURST_PITCH);

        /* ④ 波及范围内的生物（LivingEntity 不含掉落物，掉落物既吃不了效果也不该被传染） */
        AABB area = new AABB(center, center).inflate(RADIUS, RADIUS, RADIUS);
        List<LivingEntity> victims = level.getEntitiesOfClass(
                LivingEntity.class, area, victim -> victim != owner && victim.isAlive());

        for (LivingEntity victim : victims)
        {
            applyDebuff(victim, mark.type());
        }

        /*
         * ⑤ 传染。域内规则的判据取爆心——也就是「这场爆炸实际发生在哪里」，与制导 / 撞击那条
         * 域内判定同一口径。它同时自带术式校验（换术式即失效），见 StarMarkRules#isRendezvousDomainActive。
         */
        boolean domainBroadcast = StarMarkRules.isRendezvousDomainActive(owner)
                && StarMarkRules.insideRendezvousDomain(owner, center);

        Spread spread = spreadMark(owner, mark, victims, domainBroadcast);
        return new Result(damaged, victims.size(), spread.applied(), spread.evicted(), spread.empowered());
    }

    /**
     * 爆心的<b>单体</b>伤害：只有被引爆的那个目标受伤，半径内的其他目标一概不碰。
     * <p>
     * 三类目标拿不到伤害，都不是异常：
     * <ul>
     *   <li><b>方块</b>——没有 {@link StarMark#entityId()}，方块本身也没有生命值可言；</li>
     *   <li><b>掉落物</b>——不是 {@link LivingEntity}；</li>
     *   <li><b>施术者自己</b>——Shift 可以把标记打在自己身上，叠满后同样能引爆；但引爆是自己
     *       主动触发的动作，不该反过来打伤自己。</li>
     * </ul>
     * 区块未加载、实体已消失时 {@code getEntity} 返回 null，同样跳过——那种标记会在下一 tick
     * 被常态逻辑剪掉，不需要在这里补救。
     *
     * @return 是否真的造成了伤害，供动作栏提示区分「打到了」与「没打到」
     */
    private static boolean damageCore(ServerLevel level, ServerPlayer owner, StarMark mark)
    {
        if (mark.entityId() == null)
        {
            return false;
        }
        Entity core = level.getEntity(mark.entityId());
        if (!(core instanceof LivingEntity living) || living == owner || !living.isAlive())
        {
            return false;
        }
        /*
         * 走术式伤害通道：术式击杀照常给咒术经验、掉术式情报、记罪恶者、判黑闪，且无视盾牌。
         * 它内部会吃掉原版击退，所以引爆只掉血、不推人——这正是「单体伤害」该有的样子。
         * 返回值沿用 hurt 的语义（伤害是否真正生效），因此无敌帧、创造模式目标等情况会被如实报成「没打到」。
         */
        return TechniqueDamageSources.deal(living, owner, DETONATION_DAMAGE);
    }

    /**
     * 按星种施加负面效果，<b>全部不附带伤害</b>——伤害只打爆心那一个目标，由 {@link #damageCore} 单独结算，
     * 波及名单里的这些目标只吃效果。
     * <p>
     * 除 Mimosa（发光 + 裂甲）与 Ginan（玩家另加视野扭曲）外，每颗星对应一种效果。
     */
    private static void applyDebuff(LivingEntity victim, StarMarkType type)
    {
        switch (type)
        {
            /* Imai 薄荷青：减速 */
            case IMAI -> add(victim, MobEffects.MOVEMENT_SLOWDOWN, 1);
            /* Acrux 樱粉：降攻击 */
            case ACRUX -> add(victim, MobEffects.WEAKNESS, 1);
            /* Mimosa 柠檬黄：发光 + 降护甲（后者是本模组的自定义效果，走属性修饰符） */
            case MIMOSA ->
            {
                add(victim, MobEffects.GLOWING, 0);
                add(victim, ModEffects.STAR_ARMOR_BREAK.get(), 0);
            }
            /* Ginan 薰衣草紫：短时混乱。玩家朝向由客户端权威，改不了，改用视野扭曲来体现 */
            case GINAN ->
            {
                add(victim, ModEffects.STAR_CONFUSION.get(), 0);
                if (victim instanceof Player)
                {
                    add(victim, MobEffects.CONFUSION, 0);
                }
            }
            /* Gacrux 冰蓝：冰冻 + 定身 */
            case GACRUX ->
            {
                victim.setTicksFrozen(Math.max(victim.getTicksFrozen(), FREEZE_TICKS));
                add(victim, MobEffects.MOVEMENT_SLOWDOWN, BIND_SLOW_AMPLIFIER);
            }
        }
    }

    /**
     * 把引爆的那颗星传染给范围内的其他目标。
     * <p>
     * <b>域外（默认规则）</b>：只落在「尚未持有任何标记」的干净目标上，星位等级 1（免费，不结算咒力）。
     * 已有标记的目标一律跳过——同星会当场再叠一层、异星会被换掉，两者都会让「一次引爆」的结果不可预期，
     * 而且会打乱施术者按星种划下的布阵。
     * <p>
     * <b>域内（{@code domainBroadcast}，「爱与幽会」）</b>：这是整套连锁的引擎，放宽两条：
     * <ol>
     *   <li>干净目标直接拿到<b>满级</b>。域外的「1 级」意味着被传染者离「可燃的满级」还差 4 次打标，
     *       链条在每一环都断在这里；直接给满级，被炸到的目标当场就成为下一个可燃点。</li>
     *   <li>已有的<b>同一颗星</b>的标记被一并<b>拉满</b>。否则「周围都标过一遍」之后传染再无落点，
     *       链条会在群体内部枯竭。异星仍然跳过（理由同上）。</li>
     * </ol>
     * 域内这条「拉满」必须走 {@link StarMark#maxed(long)}——它会把打标时刻一起刷新，
     * 否则衰减结算会在同一个 tick 里把满级按回原样。
     * <p>
     * <b>爆心的原目标在两种模式下都跳过</b>：它的标记刚刚被消耗掉，若又被传染回来就成了「回到满级」
     * 而不是「清零」，等于让人站着不动就能被反复引爆。方块标记的爆心不是实体、自然不在波及名单里，
     * 这条判断只对实体标记生效。
     * <p>
     * 传染照常走 {@link PlayerStarMarkData#put}，因此同样受持有上限约束——满员时它会顶掉施术者
     * 最久没打的那几条，这一点由 {@link Result#evicted()} 报给调用方，不做静默丢弃。
     *
     * @param domainBroadcast 是否走「爱与幽会」域内的连锁规则
     * @return 传染的结算：新打标几条、拉满几条、因上限顶掉几条
     */
    private static Spread spreadMark(ServerPlayer owner, StarMark source, List<LivingEntity> victims,
                                     boolean domainBroadcast)
    {
        long now = owner.level().getGameTime();
        int applied = 0;
        int empowered = 0;
        int evicted = 0;
        for (LivingEntity victim : victims)
        {
            StarMark probe = StarMarkSkillLogic.markOfEntity(owner, victim, source.type());
            /* 爆心的原目标：标记刚被消耗掉，不再传染回来 */
            if (probe.sameTargetAs(source))
            {
                continue;
            }
            StarMark existing = PlayerStarMarkData.findTarget(owner, probe);
            if (existing == null)
            {
                /* 干净目标：域内直接满级，域外仍是 1 级 */
                StarMark spread = domainBroadcast ? probe.maxed(now) : probe;
                evicted += PlayerStarMarkData.put(owner, spread).evicted().size();
                applied++;
                continue;
            }
            /* 已有标记：只有「域内 + 同星 + 尚未满级」才拉满；异星与已满级都不动 */
            if (!domainBroadcast || existing.type() != source.type()
                    || existing.stacks() >= StarMark.MAX_STACKS)
            {
                continue;
            }
            evicted += PlayerStarMarkData.put(owner, existing.maxed(now)).evicted().size();
            empowered++;
        }
        return new Spread(applied, empowered, evicted);
    }

    /**
     * 传染的结算。
     *
     * @param applied   新打上标记的实体数（域内给满级、域外给 1 级）
     * @param empowered 域内被一并拉满的同星已有标记数；域外恒为 0
     * @param evicted   因持有上限被顶掉的标记条数
     */
    private record Spread(int applied, int empowered, int evicted)
    {
    }

    /** 施加一个不显示粒子、但显示图标的负面效果。 */
    private static void add(LivingEntity victim, MobEffect effect, int amplifier)
    {
        victim.addEffect(new MobEffectInstance(
                effect, EFFECT_DURATION_TICKS, amplifier, false, false, true));
    }
}
