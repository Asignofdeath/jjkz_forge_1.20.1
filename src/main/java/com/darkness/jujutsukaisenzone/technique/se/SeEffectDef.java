package com.darkness.jujutsukaisenzone.technique.se;

import net.minecraft.world.item.Item;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

/**
 * SE（术式侵入）装载元数据：咒物、触发形态、互斥、质量、术式 scope。
 * <p>
 * 效果逻辑在 {@link SeMechanic}（见 {@link SeMechanicRegistry}），不在本 record 里塞烟管字段。
 * <p>
 * 装载闸：咒物实例特质须吻合、术式 scope、质量。
 * <b>代价与特质均在咒物产出时写入实例</b>，不在本定义里写死。
 * <p>
 * 互斥有两层：本类的 {@code mutexGroup} 是设计师<b>额外</b>声明的冲突；
 * 另外同一「窄口」的 SE 由装载层<b>自动互斥</b>（见 {@code SeMechanicRegistry#hasNarrowInterfaceConflict}），
 * 不用也不能在这里写。
 */
public record SeEffectDef(
        String seId,
        Supplier<? extends Item> cursedObject,
        SeTrigger trigger,
        String mutexGroup,
        int quality,
        SeScope scope)
{
    public SeEffectDef
    {
        if (seId == null || seId.isBlank())
        {
            throw new IllegalArgumentException("seId");
        }
        if (cursedObject == null)
        {
            throw new IllegalArgumentException("cursedObject");
        }
        if (trigger == null)
        {
            throw new IllegalArgumentException("trigger");
        }
        mutexGroup = mutexGroup == null ? "" : mutexGroup;
        if (quality < 1)
        {
            throw new IllegalArgumentException("quality 必须 >= 1（0 质量的 SE 等于免费强度）：" + seId);
        }
        if (scope == null)
        {
            throw new IllegalArgumentException("scope：" + seId);
        }
    }

    public static SeEffectDef of(
            String seId,
            RegistryObject<? extends Item> item,
            SeTrigger trigger,
            String mutexGroup,
            int quality,
            SeScope scope)
    {
        return new SeEffectDef(seId, item, trigger, mutexGroup, quality, scope);
    }

    public Item item()
    {
        return cursedObject.get();
    }
}
