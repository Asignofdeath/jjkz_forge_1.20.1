package com.darkness.jujutsukaisenzone.technique;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;

import java.util.IdentityHashMap;

/**
 * 术式伤害的统一入口。
 * <p>
 * 此前所有术式伤害都直接写 {@code damageSources().magic()}——这个 DamageSource 的
 * {@code directEntity} / {@code causingEntity} 都是 null，于是原版 {@code LivingEntity#hurt}
 * 里的归因段被整段跳过：{@code getLastHurtByMob()} / {@code getLastHurtByPlayer()} 永远是 null，
 * {@code DamageSource#getEntity()} 也永远是 null。表现出来就是「伤害打出来了，但没有任何地方
 * 知道是谁打的」：
 * <ul>
 *   <li>{@code CursedGradeKillEvents#resolveKiller} 三条判据（getEntity / getDirectEntity /
 *       getLastHurtByMob）全部落空 ⇒ 术式击杀不给咒术经验</li>
 *   <li>{@code TechniqueIntelligenceDropEvents} / {@code SinfulOneKillEvents} / {@code BlackFlashEvents}
 *       一律只看 {@code getSource().getEntity()} ⇒ 术式击杀也不掉术式情报、不记罪恶者、不判黑闪</li>
 * </ul>
 * 这里只补归因：伤害类型原样复用 {@code MAGIC} 的 {@link DamageSource#typeHolder()}，
 * 穿甲 / 抗性 / 免疫 / 药水 / 难度加成判定一律不变，变的只有「谁打的」。
 * <p>
 * ⚠️ 原版 {@code LivingEntity#hurt} 里归因和击退写在同一段（都读 {@code getEntity()}），
 * 所以补上归因就会连带出 0.4 击退与「把施术者记成攻击者」。击退用
 * {@link #onLivingKnockBack} 精确取消（见 {@link #deal}）；「记成攻击者」保留——
 * 生物被术式打中后开始仇恨施术者，这是原版语义，同时
 * {@code CursedGradeKillEvents} 也依赖 {@code getLastHurtByMob()} 这条兜底。
 * <p>
 * ⚠️ 刻意<b>不</b>填 {@code directEntity}：{@code getSourcePosition()} 只在
 * {@code isDamageSourceBlocked}（举盾格挡）里被用到，填了就等于让盾牌能挡下术式，
 * 这是设定外的改动。保持 null ⇒ 术式依旧无视盾牌。
 */
@Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class TechniqueDamageSources
{
    /**
     * 正在结算术式伤害的目标。键用 IdentityHashMap（实体相等即同一对象），
     * 只在 {@link #deal} 调用栈存活期间有值，服务端线程独占。
     */
    private static final ThreadLocal<IdentityHashMap<LivingEntity, Boolean>> KNOCKBACK_GUARD =
            ThreadLocal.withInitial(IdentityHashMap::new);

    private TechniqueDamageSources()
    {
    }

    /**
     * 对目标造成一次术式伤害（带施术者归因，且不产生击退）。
     *
     * @param target 受击目标
     * @param owner  施术者；为 null 时退回改造前的无归因 {@code magic()}
     *               （此时原版也不会触发击退，无需额外处理）
     * @param amount 伤害量
     * @return 与 {@link LivingEntity#hurt} 一致：伤害是否真正生效
     */
    public static boolean deal(LivingEntity target, @Nullable LivingEntity owner, float amount)
    {
        if (owner == null)
        {
            return target.hurt(target.damageSources().magic(), amount);
        }

        DamageSource source = new DamageSource(owner.damageSources().magic().typeHolder(), null, owner);

        IdentityHashMap<LivingEntity, Boolean> guard = KNOCKBACK_GUARD.get();
        guard.put(target, Boolean.TRUE);
        try
        {
            return target.hurt(source, amount);
        }
        finally
        {
            guard.remove(target);
        }
    }

    /**
     * 取消术式伤害带来的原版 0.4 击退。
     * <p>
     * 原版 {@code LivingEntity#hurt} 在归因非空时会调 {@code knockback(0.4F, dx, dz)}，
     * 而 Forge 在 {@code knockback} 入口就发出可取消的 {@link LivingKnockBackEvent}
     * （补丁：{@code if(event.isCanceled()) return;}）⇒ 借这个入口精确取消，
     * 改动只覆盖 {@link #deal} 调用栈内的那一次击退（标记在 finally 里移除），
     * 近战 / 弹射物 / EpicFight 自己的位移一律不受影响。
     */
    @SubscribeEvent
    public static void onLivingKnockBack(LivingKnockBackEvent event)
    {
        if (KNOCKBACK_GUARD.get().containsKey(event.getEntity()))
        {
            event.setCanceled(true);
        }
    }
}
