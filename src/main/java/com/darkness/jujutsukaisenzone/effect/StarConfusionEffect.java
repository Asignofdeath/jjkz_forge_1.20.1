package com.darkness.jujutsukaisenzone.effect;

import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * 「星乱」：短时混乱，方位感被星力搅乱。属于 Ginan 薰衣草紫那颗星的引爆效果。
 * <p>
 * <b>玩家不在这里处理</b>：玩家朝向由客户端权威，服务端改 yaw 会被客户端下发的转向包立刻覆盖。
 * 玩家端的混乱表现由引爆时另外附加的原版 {@link net.minecraft.world.effect.MobEffects#CONFUSION}
 * （视野扭曲）承担，本效果只对非玩家实体做随机转向。
 * <p>
 * 三重作用叠加，缺一不可：
 * <ul>
 *   <li>随机朝向——观感上的「乱转」；</li>
 *   <li>随机位移脉冲——只改朝向不改位移的话，怪物看起来像在原地打转而不是真的走错路；</li>
 *   <li>间歇清空攻击目标——让 AI 真的「迷路」，否则它会立刻重新锁定并照直追过来。</li>
 * </ul>
 */
public final class StarConfusionEffect extends MobEffect
{
    /** 随机位移脉冲的大小，与 {@code StarMarkRules} 的推力同量级（增量速度，不覆盖既有速度）。 */
    private static final double STRAY_IMPULSE = 0.08D;
    /** 每若干 tick 松开一次攻击目标；不能每 tick 都松，否则怪物会完全站住不动，那是定身不是混乱。 */
    private static final int TARGET_DROP_INTERVAL = 4;

    public StarConfusionEffect()
    {
        super(MobEffectCategory.HARMFUL, 0xC0A8FF);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier)
    {
        return true;
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier)
    {
        if (entity.level().isClientSide || entity instanceof Player)
        {
            return;
        }
        RandomSource random = entity.getRandom();

        /* 随机朝向：覆盖 AI 的 look 控制结果 */
        float yaw = entity.getYRot() + (random.nextFloat() - 0.5F) * 180.0F;
        entity.setYRot(yaw);
        entity.setYHeadRot(yaw);

        /* 随机方向的位移脉冲：与项目其它推拉一致，走增量速度 + hurtMarked */
        Vec3 impulse = new Vec3(random.nextDouble() - 0.5D, 0.0D, random.nextDouble() - 0.5D);
        if (impulse.lengthSqr() > 1.0E-6D)
        {
            entity.setDeltaMovement(entity.getDeltaMovement().add(impulse.normalize().scale(STRAY_IMPULSE)));
            entity.hurtMarked = true;
        }

        /* 间歇打断追击 */
        if (entity instanceof Mob mob && random.nextInt(TARGET_DROP_INTERVAL) == 0)
        {
            mob.setTarget(null);
        }
    }
}
