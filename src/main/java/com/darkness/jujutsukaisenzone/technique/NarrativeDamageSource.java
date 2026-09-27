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

    @Override
    public String getMsgId()
    {
        return narrativeMsgId;
    }

    public String narrativeMsgId()
    {
        return narrativeMsgId;
    }
}
