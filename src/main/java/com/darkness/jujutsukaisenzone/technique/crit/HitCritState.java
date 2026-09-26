package com.darkness.jujutsukaisenzone.technique.crit;

import com.darkness.jujutsukaisenzone.technique.skillset.SkillDamageType;

import javax.annotation.Nullable;

/**
 * 本击暴击状态（服务端主线程、单次 {@code hurt} 调用栈内有效）。
 * <p>
 * 由乘区暴击写入；黑闪 / SE 在同一次 {@link net.minecraftforge.event.entity.living.LivingHurtEvent}
 * 的后续优先级读取。每次进入伤害管线 {@link #begin()}，结束时 {@link #clear()}。
 * <p>
 * 同时记下本击<b>暴击的分型</b>（{@link #critType()}）：SE 的 {@code ON_MARTIAL_CRIT} 与黑闪
 * 都只认体术系暴击，术式暴击不能顺带把它们点亮。
 */
public final class HitCritState
{
    private static final ThreadLocal<State> STATE = ThreadLocal.withInitial(State::new);

    private HitCritState()
    {
    }

    public static void begin()
    {
        STATE.get().reset();
    }

    public static void clear()
    {
        STATE.get().reset();
    }

    public static void markCrit(SkillDamageType critType, float varianceMultiplier)
    {
        State state = STATE.get();
        state.crit = true;
        state.critType = critType;
        state.varianceMultiplier = Math.max(1.0F, varianceMultiplier);
    }

    public static void markForced()
    {
        STATE.get().forced = true;
    }

    public static void markBlackFlash()
    {
        STATE.get().blackFlash = true;
    }

    public static boolean wasCrit()
    {
        return STATE.get().crit;
    }

    public static boolean wasForced()
    {
        return STATE.get().forced;
    }

    public static boolean wasBlackFlash()
    {
        return STATE.get().blackFlash;
    }

    /**
     * SE「暴击后」触发：本击暴击且未升级为黑闪。
     */
    public static boolean wasCritForEffects()
    {
        State state = STATE.get();
        return state.crit && !state.blackFlash;
    }

    public static float varianceMultiplier()
    {
        return STATE.get().varianceMultiplier;
    }

    /**
     * 本击暴击的分型；本击没有暴击时为 {@code null}。
     * <p>
     * 只认体术系（{@link SkillDamageType#MARTIAL}）的两个读者：{@code SeEffectEvents}
     * （{@code ON_MARTIAL_CRIT} 名义上就是体术暴击）与 {@code BlackFlashEvents}（黑闪是体术的延伸）。
     */
    @Nullable
    public static SkillDamageType critType()
    {
        State state = STATE.get();
        return state.crit ? state.critType : null;
    }

    private static final class State
    {
        boolean crit;
        boolean forced;
        boolean blackFlash;
        SkillDamageType critType;
        float varianceMultiplier = 1.0F;

        void reset()
        {
            crit = false;
            forced = false;
            blackFlash = false;
            critType = null;
            varianceMultiplier = 1.0F;
        }
    }
}
