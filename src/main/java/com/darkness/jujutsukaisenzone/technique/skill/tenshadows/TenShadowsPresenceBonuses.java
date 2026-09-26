package com.darkness.jujutsukaisenzone.technique.skill.tenshadows;

import com.darkness.jujutsukaisenzone.player.PlayerCombatPowerData;
import com.darkness.jujutsukaisenzone.player.PlayerTenShadowsRosterData;
import com.darkness.jujutsukaisenzone.registry.ModAttributes;
import com.darkness.jujutsukaisenzone.technique.skill.TenShadowsShikigami;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.registries.RegistryObject;

import java.util.List;
import java.util.UUID;

/**
 * 已调伏式神在场时给术师的增益。召/收/每秒刷新；用固定 UUID 修饰符避免叠层。
 */
public final class TenShadowsPresenceBonuses
{
    public static final int DIVINE_DOG_TECHNIQUE_POWER_EACH = 15;
    public static final double NUE_SPEED_MULTIPLIER = 0.20D;
    public static final double SERPENT_POWER_MULTIPLIER = 0.08D;
    public static final double ELEPHANT_TECH_PEN = 20.0D;
    public static final double TOAD_MARTIAL_FROM_TECHNIQUE = 0.10D;
    public static final double RABBIT_AGGRO_CHANCE = 0.25D;
    public static final double DEER_MAX_HEALTH = 20.0D;
    public static final double DEER_REVERSE_EFFICIENCY = 25.0D;
    public static final double OX_PHYS_PEN = 20.0D;
    public static final double TIGER_DOMAIN_CHARGE = 25.0D;
    public static final double MAHORAGA_ARMOR = 100.0D;

    private static final UUID NUE_SPEED_UUID = UUID.fromString("b2c3d4e5-2001-4000-8000-000000060001");
    private static final UUID ELEPHANT_TECH_PEN_UUID = UUID.fromString("b2c3d4e5-2002-4000-8000-000000060002");
    private static final UUID DEER_HEALTH_UUID = UUID.fromString("b2c3d4e5-2003-4000-8000-000000060003");
    private static final UUID DEER_REVERSE_UUID = UUID.fromString("b2c3d4e5-2004-4000-8000-000000060004");
    private static final UUID OX_PHYS_PEN_UUID = UUID.fromString("b2c3d4e5-2005-4000-8000-000000060005");
    private static final UUID TIGER_DOMAIN_UUID = UUID.fromString("b2c3d4e5-2006-4000-8000-000000060006");
    private static final UUID MAHORAGA_ARMOR_UUID = UUID.fromString("b2c3d4e5-2007-4000-8000-000000060007");
    private static final String MODIFIER_NAME = "jjkz_ten_shadows_presence";

    private TenShadowsPresenceBonuses()
    {
    }

    public static void refresh(ServerPlayer player)
    {
        if (player == null)
        {
            return;
        }
        clearModifiers(player);

        if (countAlive(player, TenShadowsShikigami.NUE) > 0)
        {
            addModifier(player, Attributes.MOVEMENT_SPEED, NUE_SPEED_UUID, NUE_SPEED_MULTIPLIER,
                    AttributeModifier.Operation.MULTIPLY_TOTAL);
        }
        if (countAlive(player, TenShadowsShikigami.MAX_ELEPHANT) > 0)
        {
            addModifier(player, ModAttributes.TECHNIQUE_PENETRATION, ELEPHANT_TECH_PEN_UUID, ELEPHANT_TECH_PEN,
                    AttributeModifier.Operation.ADDITION);
        }
        if (countAlive(player, TenShadowsShikigami.ROUND_DEER) > 0)
        {
            addModifier(player, Attributes.MAX_HEALTH, DEER_HEALTH_UUID, DEER_MAX_HEALTH,
                    AttributeModifier.Operation.ADDITION);
            addModifier(player, ModAttributes.REVERSE_TECHNIQUE_EFFICIENCY, DEER_REVERSE_UUID, DEER_REVERSE_EFFICIENCY,
                    AttributeModifier.Operation.ADDITION);
            float max = (float) player.getAttributeValue(Attributes.MAX_HEALTH);
            if (player.getHealth() > max)
            {
                player.setHealth(max);
            }
        }
        if (countAlive(player, TenShadowsShikigami.PIERCING_OX) > 0)
        {
            addModifier(player, ModAttributes.PHYSICAL_PENETRATION, OX_PHYS_PEN_UUID, OX_PHYS_PEN,
                    AttributeModifier.Operation.ADDITION);
        }
        if (countAlive(player, TenShadowsShikigami.TIGER_FUNERAL) > 0)
        {
            addModifier(player, ModAttributes.DOMAIN_GAUGE_CHARGE_EFFICIENCY, TIGER_DOMAIN_UUID, TIGER_DOMAIN_CHARGE,
                    AttributeModifier.Operation.ADDITION);
        }
        if (countAlive(player, TenShadowsShikigami.MAHORAGA) > 0)
        {
            addModifier(player, Attributes.ARMOR, MAHORAGA_ARMOR_UUID, MAHORAGA_ARMOR,
                    AttributeModifier.Operation.ADDITION);
        }

        PlayerCombatPowerData.syncToClient(player);
    }

    public static void clear(ServerPlayer player)
    {
        if (player == null)
        {
            return;
        }
        clearModifiers(player);
        PlayerCombatPowerData.syncToClient(player);
    }

    public static int divineDogTechniqueBonus(ServerPlayer player)
    {
        return countAlive(player, TenShadowsShikigami.DIVINE_DOGS) * DIVINE_DOG_TECHNIQUE_POWER_EACH;
    }

    public static boolean hasSerpent(ServerPlayer player)
    {
        return countAlive(player, TenShadowsShikigami.GREAT_SERPENT) > 0;
    }

    public static boolean hasToad(ServerPlayer player)
    {
        return countAlive(player, TenShadowsShikigami.TOAD) > 0;
    }

    public static boolean hasRabbits(ServerPlayer player)
    {
        return countAlive(player, TenShadowsShikigami.RABBIT_ESCAPE) > 0;
    }

    /** 脱兔：尽量挑一只活的给怪改仇恨。 */
    public static LivingEntity pickRandomRabbit(ServerPlayer player)
    {
        List<UUID> ids = PlayerTenShadowsRosterData.get(player, TenShadowsShikigami.RABBIT_ESCAPE.index());
        if (ids.isEmpty())
        {
            return null;
        }
        ServerLevel level = player.serverLevel();
        java.util.ArrayList<LivingEntity> alive = new java.util.ArrayList<>();
        for (UUID id : ids)
        {
            Entity entity = level.getEntity(id);
            if (entity instanceof LivingEntity living && living.isAlive())
            {
                alive.add(living);
            }
        }
        if (alive.isEmpty())
        {
            return null;
        }
        return alive.get(player.getRandom().nextInt(alive.size()));
    }

    public static int countAlive(ServerPlayer player, TenShadowsShikigami shikigami)
    {
        if (player == null || shikigami == null)
        {
            return 0;
        }
        List<UUID> ids = PlayerTenShadowsRosterData.get(player, shikigami.index());
        if (ids.isEmpty())
        {
            return 0;
        }
        ServerLevel level = player.serverLevel();
        int count = 0;
        for (UUID id : ids)
        {
            Entity entity = level.getEntity(id);
            if (entity != null && entity.isAlive())
            {
                count++;
            }
        }
        return count;
    }

    private static void clearModifiers(ServerPlayer player)
    {
        removeModifier(player, Attributes.MOVEMENT_SPEED, NUE_SPEED_UUID);
        removeModifier(player, ModAttributes.TECHNIQUE_PENETRATION, ELEPHANT_TECH_PEN_UUID);
        removeModifier(player, Attributes.MAX_HEALTH, DEER_HEALTH_UUID);
        removeModifier(player, ModAttributes.REVERSE_TECHNIQUE_EFFICIENCY, DEER_REVERSE_UUID);
        removeModifier(player, ModAttributes.PHYSICAL_PENETRATION, OX_PHYS_PEN_UUID);
        removeModifier(player, ModAttributes.DOMAIN_GAUGE_CHARGE_EFFICIENCY, TIGER_DOMAIN_UUID);
        removeModifier(player, Attributes.ARMOR, MAHORAGA_ARMOR_UUID);
    }

    private static void addModifier(ServerPlayer player, Attribute attribute, UUID uuid, double amount,
                                    AttributeModifier.Operation operation)
    {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null || Math.abs(amount) < 1.0E-6D)
        {
            return;
        }
        instance.removeModifier(uuid);
        instance.addTransientModifier(new AttributeModifier(uuid, MODIFIER_NAME, amount, operation));
    }

    private static void addModifier(ServerPlayer player, RegistryObject<Attribute> attribute, UUID uuid, double amount,
                                    AttributeModifier.Operation operation)
    {
        addModifier(player, attribute.get(), uuid, amount, operation);
    }

    private static void removeModifier(ServerPlayer player, Attribute attribute, UUID uuid)
    {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance != null)
        {
            instance.removeModifier(uuid);
        }
    }

    private static void removeModifier(ServerPlayer player, RegistryObject<Attribute> attribute, UUID uuid)
    {
        removeModifier(player, attribute.get(), uuid);
    }
}
