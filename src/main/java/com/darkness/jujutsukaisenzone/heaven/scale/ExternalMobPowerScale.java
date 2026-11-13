package com.darkness.jujutsukaisenzone.heaven.scale;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.player.CursedGradeBonuses;
import com.darkness.jujutsukaisenzone.technique.skill.tenshadows.TenShadowsShikigamiStatsApplier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.UUID;

/**
 * 外模敌对/中立生物随在线均血缩放（生命线性、出伤亚线性）。
 * <p>
 * 本模实体与十影式神不参与；无人在线时卸掉修饰符。
 */
public final class ExternalMobPowerScale
{
    public static final String ADAPTER_ID = "external_mob_power";

    private static final UUID SCALE_UUID = UUID.fromString("6b2c9a01-4001-4000-8000-000000000001");
    private static final String SCALE_NAME = "jjkz_online_avg_health_scale";
    private static final String TAG_HEALTH_SCALE = "jjkz_online_health_scale";

    private ExternalMobPowerScale()
    {
    }

    public static boolean shouldScale(LivingEntity living)
    {
        return isExternalMob(living) && isHostileOrNeutral(living);
    }

    public static void applyOnJoin(LivingEntity living, MinecraftServer server)
    {
        if (!shouldScale(living) || server == null)
        {
            return;
        }
        applyScale(living, server);
    }

    /**
     * @return 生命缩放系数；≤1 表示不放大出伤
     */
    public static double readHealthScale(LivingEntity living)
    {
        CompoundTag data = living.getPersistentData();
        if (data.contains(TAG_HEALTH_SCALE))
        {
            return Math.max(0.0D, data.getDouble(TAG_HEALTH_SCALE));
        }
        AttributeInstance maxHealth = living.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth == null)
        {
            return 1.0D;
        }
        AttributeModifier modifier = maxHealth.getModifier(SCALE_UUID);
        if (modifier == null)
        {
            return 1.0D;
        }
        return Math.max(0.0D, modifier.getAmount() + 1.0D);
    }

    private static boolean isExternalMob(LivingEntity living)
    {
        if (living instanceof Player)
        {
            return false;
        }
        if (!(living instanceof Mob))
        {
            return false;
        }
        if (TenShadowsShikigamiStatsApplier.isTenShadowsStatEntity(living))
        {
            return false;
        }
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(living.getType());
        if (id != null && JuJutsuKaisenZone.MODID.equals(id.getNamespace()))
        {
            return false;
        }
        return living.getAttribute(Attributes.MAX_HEALTH) != null;
    }

    private static boolean isHostileOrNeutral(LivingEntity living)
    {
        if (living instanceof Enemy || living instanceof Monster || living instanceof NeutralMob)
        {
            return true;
        }
        return living.getType().getCategory() == MobCategory.MONSTER;
    }

    private static void applyScale(LivingEntity living, MinecraftServer server)
    {
        AttributeInstance maxHealth = living.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth == null)
        {
            return;
        }

        float oldMax = living.getMaxHealth();
        float oldHealth = living.getHealth();

        maxHealth.removeModifier(SCALE_UUID);

        double scale = 1.0D;
        double avg = OnlinePowerProbe.averageMaxHealth(server);
        if (avg > 0.0D)
        {
            scale = avg / CursedGradeBonuses.BASE_MAX_HEALTH;
            double amount = scale - 1.0D;
            if (Math.abs(amount) > 1.0E-4D)
            {
                maxHealth.addPermanentModifier(new AttributeModifier(
                        SCALE_UUID,
                        SCALE_NAME,
                        amount,
                        AttributeModifier.Operation.MULTIPLY_TOTAL));
            }
            else
            {
                scale = 1.0D;
            }
        }

        writeHealthScale(living, scale);

        float newMax = living.getMaxHealth();
        if (newMax <= 0.0F)
        {
            return;
        }
        float ratio = oldMax > 0.001F ? oldHealth / oldMax : 1.0F;
        living.setHealth(Mth.clamp(newMax * ratio, 1.0F, newMax));
    }

    private static void writeHealthScale(LivingEntity living, double scale)
    {
        CompoundTag data = living.getPersistentData();
        if (scale <= 1.0001D)
        {
            data.remove(TAG_HEALTH_SCALE);
            return;
        }
        data.putDouble(TAG_HEALTH_SCALE, scale);
    }
}
