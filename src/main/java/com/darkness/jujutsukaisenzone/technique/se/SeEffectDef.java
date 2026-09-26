package com.darkness.jujutsukaisenzone.technique.se;

import net.minecraft.world.item.Item;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

/**
 * SE（术式侵入）定义：咒物热装后的通用机制效果。
 */
public record SeEffectDef(
        String seId,
        Supplier<? extends Item> cursedObject,
        SeTrigger trigger,
        String mutexGroup,
        float procChance,
        int igniteSeconds)
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
        procChance = Math.max(0.0F, Math.min(1.0F, procChance));
        igniteSeconds = Math.max(0, igniteSeconds);
    }

    public static SeEffectDef of(
            String seId,
            RegistryObject<? extends Item> item,
            SeTrigger trigger,
            String mutexGroup,
            float procChance,
            int igniteSeconds)
    {
        return new SeEffectDef(seId, item, trigger, mutexGroup, procChance, igniteSeconds);
    }

    public Item item()
    {
        return cursedObject.get();
    }
}
