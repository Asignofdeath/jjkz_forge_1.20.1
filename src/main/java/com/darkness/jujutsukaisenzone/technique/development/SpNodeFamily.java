package com.darkness.jujutsukaisenzone.technique.development;

import com.darkness.jujutsukaisenzone.registry.ModAttributes;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.Nullable;

/**
 * SP 节点模板族：强度走面板行，其余走 Attribute。
 */
public enum SpNodeFamily
{
    PHYSICAL_POWER(false, null),
    TECHNIQUE_POWER(false, null),
    BALANCED(false, null),
    BREAK_POTENCY(true, ModAttributes.BREAK_POTENCY),
    PHYSICAL_RESISTANCE(true, ModAttributes.PHYSICAL_RESISTANCE),
    TECHNIQUE_RESISTANCE(true, ModAttributes.TECHNIQUE_RESISTANCE),
    PHYSICAL_PENETRATION(true, ModAttributes.PHYSICAL_PENETRATION),
    TECHNIQUE_PENETRATION(true, ModAttributes.TECHNIQUE_PENETRATION),
    REVERSE_EFFICIENCY(true, ModAttributes.REVERSE_TECHNIQUE_EFFICIENCY),
    DOMAIN_CHARGE(true, ModAttributes.DOMAIN_GAUGE_CHARGE_EFFICIENCY),
    CRIT_CHANCE(true, ModAttributes.CRIT_CHANCE);

    private final boolean offAxis;
    @Nullable
    private final RegistryObject<Attribute> attribute;

    SpNodeFamily(boolean offAxis, @Nullable RegistryObject<Attribute> attribute)
    {
        this.offAxis = offAxis;
        this.attribute = attribute;
    }

    public boolean isOffAxis()
    {
        return offAxis;
    }

    public boolean isPowerRow()
    {
        return !offAxis;
    }

    @Nullable
    public RegistryObject<Attribute> attribute()
    {
        return attribute;
    }

    public String id()
    {
        return name().toLowerCase();
    }

    @Nullable
    public static SpNodeFamily byId(String id)
    {
        if (id == null || id.isBlank())
        {
            return null;
        }
        String key = id.trim().toLowerCase();
        for (SpNodeFamily family : values())
        {
            if (family.id().equals(key) || family.name().equalsIgnoreCase(key))
            {
                return family;
            }
        }
        return null;
    }
}
