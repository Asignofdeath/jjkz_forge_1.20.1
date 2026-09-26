package com.darkness.jujutsukaisenzone.entity.combat;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

/**
 * 非玩家实体的体术/术式强度面板（十影式神等）。
 * <p>
 * 供 {@link com.darkness.jujutsukaisenzone.technique.SkillDamageResolver} 换算伤害；
 * 存在实体 persistent data，随实体保存。
 */
public final class LivingCombatStats
{
    public static final String TAG_PANEL = "jujutsukaisenzone_living_combat_panel";
    public static final String TAG_PHYSICAL = "physical_power";
    public static final String TAG_TECHNIQUE = "technique_power";

    private LivingCombatStats()
    {
    }

    public static boolean has(LivingEntity entity)
    {
        return entity != null && entity.getPersistentData().contains(TAG_PANEL);
    }

    public static void clear(LivingEntity entity)
    {
        if (entity != null)
        {
            entity.getPersistentData().remove(TAG_PANEL);
        }
    }

    public static void set(LivingEntity entity, int physicalPower, int techniquePower)
    {
        if (entity == null)
        {
            return;
        }
        CompoundTag panel = new CompoundTag();
        panel.putInt(TAG_PHYSICAL, Math.max(0, physicalPower));
        panel.putInt(TAG_TECHNIQUE, Math.max(0, techniquePower));
        entity.getPersistentData().put(TAG_PANEL, panel);
    }

    @Nullable
    public static Panel get(LivingEntity entity)
    {
        if (!has(entity))
        {
            return null;
        }
        CompoundTag panel = entity.getPersistentData().getCompound(TAG_PANEL);
        return new Panel(panel.getInt(TAG_PHYSICAL), panel.getInt(TAG_TECHNIQUE));
    }

    public static int physicalPower(LivingEntity entity)
    {
        Panel panel = get(entity);
        return panel == null ? -1 : panel.physicalPower();
    }

    public static int techniquePower(LivingEntity entity)
    {
        Panel panel = get(entity);
        return panel == null ? -1 : panel.techniquePower();
    }

    public record Panel(int physicalPower, int techniquePower)
    {
    }
}
