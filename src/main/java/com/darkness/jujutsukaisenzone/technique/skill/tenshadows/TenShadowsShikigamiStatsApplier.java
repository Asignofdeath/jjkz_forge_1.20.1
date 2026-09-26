package com.darkness.jujutsukaisenzone.technique.skill.tenshadows;

import com.darkness.jujutsukaisenzone.entity.combat.LivingCombatStats;
import com.darkness.jujutsukaisenzone.player.PlayerCombatPowerData;
import com.darkness.jujutsukaisenzone.registry.ModAttributes;
import com.darkness.jujutsukaisenzone.technique.skill.TenShadowsRitualLogic;
import com.darkness.jujutsukaisenzone.technique.skill.TenShadowsShikigami;
import com.darkness.jujutsukaisenzone.technique.skill.TenShadowsSummonLogic;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * 把式神面板写到实体：生命/护甲/双抗双穿 + {@link LivingCombatStats} 强度。
 */
public final class TenShadowsShikigamiStatsApplier
{
    public static final String TAG_APPLIED = "jujutsukaisenzone_ts_stats_applied";
    public static final String TAG_SNAPSHOT = "jujutsukaisenzone_ts_stats_snapshot";

    private static final UUID PHYS_RES_UUID = UUID.fromString("a1b2c3d4-1001-4000-8000-000000050001");
    private static final UUID TECH_RES_UUID = UUID.fromString("a1b2c3d4-1002-4000-8000-000000050002");
    private static final UUID PHYS_PEN_UUID = UUID.fromString("a1b2c3d4-1003-4000-8000-000000050003");
    private static final UUID TECH_PEN_UUID = UUID.fromString("a1b2c3d4-1004-4000-8000-000000050004");
    private static final String MODIFIER_NAME = "jjkz_ten_shadows_stats";

    /** 魔虚罗固定面板（不读 9 大 JSON，不跟主人缩放）。 */
    public static final TenShadowsShikigamiStatRow MAHORAGA_FIXED = new TenShadowsShikigamiStatRow(
            120.0D, 10.0D, 25.0D, 25.0D, 15.0D, 15.0D, 60, 60);

    private TenShadowsShikigamiStatsApplier()
    {
    }

    public static void applyUntamed(LivingEntity entity, TenShadowsShikigami shikigami)
    {
        if (entity == null || shikigami == null)
        {
            return;
        }
        TenShadowsShikigamiStatRow row = shikigami == TenShadowsShikigami.MAHORAGA
                ? MAHORAGA_FIXED
                : TenShadowsShikigamiStatsRegistry.untamed(shikigami);
        if (row == null)
        {
            row = TenShadowsShikigamiStatRow.EMPTY;
        }
        applyResolved(entity, row);
    }

    public static void applyTamed(LivingEntity entity, TenShadowsShikigami shikigami, @Nullable ServerPlayer owner)
    {
        if (entity == null || shikigami == null)
        {
            return;
        }
        if (shikigami == TenShadowsShikigami.MAHORAGA)
        {
            applyResolved(entity, MAHORAGA_FIXED);
            return;
        }
        TenShadowsShikigamiStatRow base = TenShadowsShikigamiStatsRegistry.tamedBase(shikigami);
        if (base == null)
        {
            base = TenShadowsShikigamiStatRow.EMPTY;
        }
        applyResolved(entity, resolveTamed(base, owner));
    }

    /** 区块重载等：从实体快照恢复面板。 */
    public static void reapplyFromEntity(LivingEntity entity)
    {
        if (entity == null)
        {
            return;
        }
        CompoundTag data = entity.getPersistentData();
        if (!data.getBoolean(TAG_APPLIED) || !data.contains(TAG_SNAPSHOT))
        {
            return;
        }
        applyResolved(entity, readSnapshot(data.getCompound(TAG_SNAPSHOT)));
    }

    public static boolean isTenShadowsStatEntity(LivingEntity entity)
    {
        if (entity == null)
        {
            return false;
        }
        CompoundTag data = entity.getPersistentData();
        return data.getBoolean(TAG_APPLIED)
                || data.getBoolean(TenShadowsRitualLogic.TAG_RITUAL)
                || TenShadowsSummonLogic.isFriendlyOwned(entity);
    }

    public static TenShadowsShikigamiStatRow resolveTamed(TenShadowsShikigamiStatRow base, @Nullable ServerPlayer owner)
    {
        if (owner == null)
        {
            return base;
        }
        TenShadowsOwnerScaling s = TenShadowsShikigamiStatsRegistry.ownerScaling();
        int ownerPhys = Math.max(0, PlayerCombatPowerData.physicalPower(owner));
        int ownerTech = Math.max(0, PlayerCombatPowerData.techniquePower(owner));
        return new TenShadowsShikigamiStatRow(
                base.maxHealth() + ownerPhys * s.maxHealthPerOwnerPhysical(),
                base.armor() + ownerPhys * s.armorPerOwnerPhysical(),
                base.physicalResistance() + ownerPhys * s.physicalResistancePerOwnerPhysical(),
                base.techniqueResistance() + ownerTech * s.techniqueResistancePerOwnerTechnique(),
                base.physicalPenetration() + ownerPhys * s.physicalPenetrationPerOwnerPhysical(),
                base.techniquePenetration() + ownerTech * s.techniquePenetrationPerOwnerTechnique(),
                base.physicalPower() + (int) Math.round(ownerPhys * s.physicalPowerPerOwnerPhysical()),
                base.techniquePower() + (int) Math.round(ownerTech * s.techniquePowerPerOwnerTechnique()));
    }

    private static void applyResolved(LivingEntity entity, TenShadowsShikigamiStatRow row)
    {
        setBase(entity, Attributes.MAX_HEALTH, Math.max(1.0D, row.maxHealth()));
        setBase(entity, Attributes.ARMOR, Math.max(0.0D, row.armor()));
        entity.setHealth((float) Math.min(entity.getMaxHealth(), Math.max(1.0D, row.maxHealth())));

        setModifier(entity, ModAttributes.PHYSICAL_RESISTANCE, PHYS_RES_UUID, row.physicalResistance());
        setModifier(entity, ModAttributes.TECHNIQUE_RESISTANCE, TECH_RES_UUID, row.techniqueResistance());
        setModifier(entity, ModAttributes.PHYSICAL_PENETRATION, PHYS_PEN_UUID, row.physicalPenetration());
        setModifier(entity, ModAttributes.TECHNIQUE_PENETRATION, TECH_PEN_UUID, row.techniquePenetration());

        LivingCombatStats.set(entity, row.physicalPower(), row.techniquePower());

        CompoundTag data = entity.getPersistentData();
        data.putBoolean(TAG_APPLIED, true);
        data.put(TAG_SNAPSHOT, writeSnapshot(row));
    }

    private static CompoundTag writeSnapshot(TenShadowsShikigamiStatRow row)
    {
        CompoundTag tag = new CompoundTag();
        tag.putDouble("max_health", row.maxHealth());
        tag.putDouble("armor", row.armor());
        tag.putDouble("physical_resistance", row.physicalResistance());
        tag.putDouble("technique_resistance", row.techniqueResistance());
        tag.putDouble("physical_penetration", row.physicalPenetration());
        tag.putDouble("technique_penetration", row.techniquePenetration());
        tag.putInt("physical_power", row.physicalPower());
        tag.putInt("technique_power", row.techniquePower());
        return tag;
    }

    private static TenShadowsShikigamiStatRow readSnapshot(CompoundTag tag)
    {
        return new TenShadowsShikigamiStatRow(
                tag.getDouble("max_health"),
                tag.getDouble("armor"),
                tag.getDouble("physical_resistance"),
                tag.getDouble("technique_resistance"),
                tag.getDouble("physical_penetration"),
                tag.getDouble("technique_penetration"),
                tag.getInt("physical_power"),
                tag.getInt("technique_power"));
    }

    private static void setBase(LivingEntity entity, Attribute attribute, double value)
    {
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance != null)
        {
            instance.setBaseValue(value);
        }
    }

    private static void setModifier(LivingEntity entity, RegistryObject<Attribute> attribute, UUID uuid, double amount)
    {
        AttributeInstance instance = entity.getAttribute(attribute.get());
        if (instance == null)
        {
            return;
        }
        instance.removeModifier(uuid);
        if (Math.abs(amount) < 1.0E-6D)
        {
            return;
        }
        instance.addPermanentModifier(new AttributeModifier(
                uuid, MODIFIER_NAME, amount, AttributeModifier.Operation.ADDITION));
    }
}
