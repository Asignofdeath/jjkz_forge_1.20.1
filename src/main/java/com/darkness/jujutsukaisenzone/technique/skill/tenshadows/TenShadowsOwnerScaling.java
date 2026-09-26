package com.darkness.jujutsukaisenzone.technique.skill.tenshadows;

/**
 * 已调伏式神相对主人强度的加算系数。
 */
public record TenShadowsOwnerScaling(
        double physicalPowerPerOwnerPhysical,
        double techniquePowerPerOwnerTechnique,
        double maxHealthPerOwnerPhysical,
        double armorPerOwnerPhysical,
        double physicalResistancePerOwnerPhysical,
        double techniqueResistancePerOwnerTechnique,
        double physicalPenetrationPerOwnerPhysical,
        double techniquePenetrationPerOwnerTechnique)
{
    public static final TenShadowsOwnerScaling DEFAULT = new TenShadowsOwnerScaling(
            0.35D, 0.35D, 0.12D, 0.02D, 0.02D, 0.03D, 0.02D, 0.02D);
}
