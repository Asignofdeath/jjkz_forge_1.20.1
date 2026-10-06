package com.darkness.jujutsukaisenzone.talent;

import com.darkness.jujutsukaisenzone.network.ModNetwork;
import com.darkness.jujutsukaisenzone.player.CursedGradeBonuses;
import com.darkness.jujutsukaisenzone.player.JjkzPlayerDataStorage;
import com.darkness.jujutsukaisenzone.player.PlayerCursedEnergyData;
import com.darkness.jujutsukaisenzone.registry.ModAttributes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * 玩家走 {@link JjkzPlayerDataStorage}；其它生物走 persistent data。
 */
public final class InnateTalentData
{
    public static final String TAG_ROOT = "jujutsukaisenzone_innate_talent";
    public static final String TAG_ID = "id";
    public static final String TAG_SOURCE = "source";
    public static final String CLIENT_TAG_ID = "jujutsukaisenzone_innate_talent_id";
    public static final String CLIENT_TAG_SOURCE = "jujutsukaisenzone_innate_talent_source";

    private static final UUID IRON_BODY_UUID = UUID.fromString("6b2c9a01-7001-4000-8000-000000000001");
    private static final UUID AGILE_UUID = UUID.fromString("6b2c9a01-7002-4000-8000-000000000002");
    public static final double IRON_BODY_PHYSICAL_RESISTANCE = 30.0D;
    public static final double AGILE_SPEED_MULTIPLIER = 0.20D;

    private InnateTalentData()
    {
    }

    @Nullable
    public static InnateTalent get(LivingEntity entity)
    {
        if (entity instanceof Player player)
        {
            return getPlayer(player);
        }
        return InnateTalent.byId(entity.getPersistentData().getCompound(TAG_ROOT).getString(TAG_ID));
    }

    public static InnateTalentSource getSource(LivingEntity entity)
    {
        if (entity instanceof Player player)
        {
            return getPlayerSource(player);
        }
        return InnateTalentSource.byName(entity.getPersistentData().getCompound(TAG_ROOT).getString(TAG_SOURCE));
    }

    public static boolean is(LivingEntity entity, InnateTalent talent)
    {
        return talent != null && talent == get(entity);
    }

    public static boolean isEmpty(LivingEntity entity)
    {
        return get(entity) == null;
    }

    public static void set(LivingEntity entity, @Nullable InnateTalent talent, InnateTalentSource source)
    {
        if (entity == null)
        {
            return;
        }
        InnateTalentSource resolved = talent == null ? InnateTalentSource.NONE : source;
        if (entity instanceof ServerPlayer player)
        {
            JjkzPlayerDataStorage.PlayerDataEntry data = JjkzPlayerDataStorage.get(player).getOrCreate(player);
            data.innateTalentId = talent == null ? "" : talent.id();
            data.innateTalentSource = resolved.name();
            JjkzPlayerDataStorage.get(player).setDirty();
            refreshModifiers(player);
            CursedGradeBonuses.applyAll(player);
            PlayerCursedEnergyData.recalculateAfterGradeChange(player);
            syncToClient(player);
            return;
        }
        CompoundTag root = new CompoundTag();
        if (talent != null)
        {
            root.putString(TAG_ID, talent.id());
            root.putString(TAG_SOURCE, resolved.name());
        }
        entity.getPersistentData().put(TAG_ROOT, root);
        refreshModifiers(entity);
    }

    public static void applyClient(Player player, String id, String source)
    {
        player.getPersistentData().putString(CLIENT_TAG_ID, id == null ? "" : id);
        player.getPersistentData().putString(CLIENT_TAG_SOURCE, source == null ? "" : source);
    }

    public static void syncToClient(ServerPlayer player)
    {
        InnateTalent talent = getPlayer(player);
        InnateTalentSource source = getPlayerSource(player);
        ModNetwork.sendInnateTalentSync(
                player,
                talent == null ? "" : talent.id(),
                source.name());
    }

    /** 六眼：实扣 floor(原消耗 × 0.1)，允许 0。 */
    public static int billCursedEnergy(LivingEntity entity, int amount)
    {
        if (amount <= 0)
        {
            return 0;
        }
        if (!is(entity, InnateTalent.SIX_EYES))
        {
            return amount;
        }
        return (int) Math.floor(amount * 0.1D);
    }

    public static int geniusRequiredReads(LivingEntity entity, int base)
    {
        if (!is(entity, InnateTalent.GENIUS))
        {
            return base;
        }
        return Math.max(1, (base + 1) / 2);
    }

    public static int geniusHalveCost(LivingEntity entity, int value)
    {
        if (value <= 0 || !is(entity, InnateTalent.GENIUS))
        {
            return value;
        }
        return Math.max(1, (value + 1) / 2);
    }

    public static double geniusLearnChance(LivingEntity entity, double base)
    {
        return is(entity, InnateTalent.GENIUS) ? base * 2.0D : base;
    }

    public static int geniusDailyUpgradeGain(LivingEntity entity, int base)
    {
        return is(entity, InnateTalent.GENIUS) ? base * 2 : base;
    }

    public static void refreshModifiers(LivingEntity entity)
    {
        AttributeInstance physical = entity.getAttribute(ModAttributes.PHYSICAL_RESISTANCE.get());
        if (physical != null)
        {
            physical.removeModifier(IRON_BODY_UUID);
            if (is(entity, InnateTalent.IRON_BODY))
            {
                physical.addTransientModifier(new AttributeModifier(
                        IRON_BODY_UUID, "jjkz_talent_iron_body",
                        IRON_BODY_PHYSICAL_RESISTANCE, AttributeModifier.Operation.ADDITION));
            }
        }
        AttributeInstance speed = entity.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null)
        {
            speed.removeModifier(AGILE_UUID);
            if (is(entity, InnateTalent.AGILE))
            {
                speed.addTransientModifier(new AttributeModifier(
                        AGILE_UUID, "jjkz_talent_agile",
                        AGILE_SPEED_MULTIPLIER, AttributeModifier.Operation.MULTIPLY_TOTAL));
            }
        }
    }

    @Nullable
    private static InnateTalent getPlayer(Player player)
    {
        if (player instanceof ServerPlayer serverPlayer)
        {
            return InnateTalent.byId(JjkzPlayerDataStorage.get(serverPlayer).getOrCreate(serverPlayer).innateTalentId);
        }
        return InnateTalent.byId(player.getPersistentData().getString(CLIENT_TAG_ID));
    }

    private static InnateTalentSource getPlayerSource(Player player)
    {
        if (player instanceof ServerPlayer serverPlayer)
        {
            return InnateTalentSource.byName(
                    JjkzPlayerDataStorage.get(serverPlayer).getOrCreate(serverPlayer).innateTalentSource);
        }
        return InnateTalentSource.byName(player.getPersistentData().getString(CLIENT_TAG_SOURCE));
    }
}
