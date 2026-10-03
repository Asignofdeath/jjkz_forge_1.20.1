package com.darkness.jujutsukaisenzone.technique;

import net.minecraft.core.Holder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

/**
 * 战斗语义仍用底层 {@link DamageType}（通常是 {@code jjkz:technique} 的穿甲 / 盾牌标签），
 * 但 {@link #getMsgId()} 返回叙事分型 ID，供 StylizedDamage 等按 message_id 选跳字样式。
 */
public final class NarrativeDamageSource extends DamageSource
{
    private final String narrativeMsgId;

    public NarrativeDamageSource(
            Holder<DamageType> combatType,
            @Nullable Entity directEntity,
            @Nullable Entity causingEntity,
            String narrativeMsgId)
    {
        super(combatType, directEntity, causingEntity);
        this.narrativeMsgId = narrativeMsgId;
    }

    /**
     * 叙事分型 id；若本击被 {@code TechniqueDamageSources#markCritDisplay} 叠了暴击版，优先返回叠加值。
     * <p>
     * ⛔ 必须在这里读叠加层：本方法<b>覆写</b>了 {@link DamageSource#getMsgId()}，而
     * {@code DamageSourceMixin} 注入的是父类那一个 —— 覆写会把 Mixin 完全挡掉，
     * 不在这里处理，暴击版 message_id 对本类构造的伤害源就永远不可见。
     * <p>
     * 叠加层只在一次 {@code hurt} → {@code LivingDamageEvent} 之间存活，栈外调用拿到的是
     * {@code null}，所以死亡播报等场景仍返回 {@link #narrativeMsgId}。
     */
    @Override
    public String getMsgId()
    {
        String overlay = TechniqueDamageSources.peekDisplayMsgId(this);
        return overlay != null ? overlay : narrativeMsgId;
    }

    public String narrativeMsgId()
    {
        return narrativeMsgId;
    }
}
