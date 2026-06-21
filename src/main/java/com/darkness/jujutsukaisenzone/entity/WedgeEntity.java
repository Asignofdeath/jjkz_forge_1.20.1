package com.darkness.jujutsukaisenzone.entity;

import com.darkness.jujutsukaisenzone.player.PlayerFactionData;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.joml.Vector3f;

import javax.annotation.Nullable;

public class WedgeEntity extends Mob
{
    public static final double MAX_HEALTH = 100.0D;
    /** 每个阵营在同一维度内最多同时存在的楔数量。 */
    public static final int MAX_PER_FACTION_PER_DIMENSION = 1;
    private static final EntityDataAccessor<String> DATA_FACTION =
            SynchedEntityData.defineId(WedgeEntity.class, EntityDataSerializers.STRING);
    private static final String TAG_FACTION = "FactionId";
    private static final int PARTICLE_INTERVAL_TICKS = 4;
    private static final int PARTICLE_COUNT = 8;
    private static final float PARTICLE_SPREAD = 1.25F;
    private static final float DUST_SCALE = 1.15F;
    private static final Vector3f COLOR_SORCERER = new Vector3f(0.25F, 0.45F, 1.0F);
    private static final Vector3f COLOR_CURSE_USER = new Vector3f(1.0F, 0.2F, 0.2F);
    private static final Vector3f COLOR_CURSE_SPIRIT = new Vector3f(0.72F, 0.2F, 1.0F);

    public int time;

    public WedgeEntity(EntityType<? extends WedgeEntity> type, Level level)
    {
        super(type, level);
        this.blocksBuilding = true;
        this.time = this.random.nextInt(100000);
        this.setNoGravity(true);
        this.setNoAi(true);
    }

    public static AttributeSupplier.Builder createAttributes()
    {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, MAX_HEALTH);
    }

    @Override
    protected void defineSynchedData()
    {
        super.defineSynchedData();
        this.entityData.define(DATA_FACTION, "");
    }

    public void setFactionId(String factionId)
    {
        this.entityData.set(DATA_FACTION, factionId == null ? "" : factionId);
    }

    public String getFactionId()
    {
        return this.entityData.get(DATA_FACTION);
    }

    public static boolean isPlayableFaction(@Nullable String factionId)
    {
        if (factionId == null || factionId.isEmpty())
        {
            return false;
        }
        return PlayerFactionData.FACTION_SORCERER.equals(factionId)
                || PlayerFactionData.FACTION_CURSE_USER.equals(factionId)
                || PlayerFactionData.FACTION_CURSE_SPIRIT.equals(factionId);
    }

    @Nullable
    public static String normalizeSpawnFactionInput(@Nullable String raw)
    {
        String id = PlayerFactionData.normalizeFactionInput(raw);
        return isPlayableFaction(id) ? id : null;
    }

    public static int countInFaction(Level level, String factionId, @Nullable WedgeEntity exclude)
    {
        if (level.isClientSide() || !isPlayableFaction(factionId))
        {
            return 0;
        }
        AABB bounds = new AABB(
                -3.0E7D, level.getMinBuildHeight(), -3.0E7D,
                3.0E7D, level.getMaxBuildHeight(), 3.0E7D);
        return level.getEntitiesOfClass(WedgeEntity.class, bounds, entity ->
                entity.isAlive()
                        && entity != exclude
                        && factionId.equals(entity.getFactionId())).size();
    }

    @Nullable
    public static WedgeEntity findInFaction(Level level, String factionId)
    {
        if (level.isClientSide() || !isPlayableFaction(factionId))
        {
            return null;
        }
        AABB bounds = new AABB(
                -3.0E7D, level.getMinBuildHeight(), -3.0E7D,
                3.0E7D, level.getMaxBuildHeight(), 3.0E7D);
        for (WedgeEntity wedge : level.getEntitiesOfClass(WedgeEntity.class, bounds, WedgeEntity::isAlive))
        {
            if (factionId.equals(wedge.getFactionId()))
            {
                return wedge;
            }
        }
        return null;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag)
    {
        super.addAdditionalSaveData(tag);
        tag.putString(TAG_FACTION, this.getFactionId());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag)
    {
        super.readAdditionalSaveData(tag);
        this.setFactionId(tag.getString(TAG_FACTION));
    }

    @Override
    protected void registerGoals()
    {
    }

    @Nullable
    public static DustParticleOptions dustParticleForFaction(String factionId)
    {
        Vector3f color = switch (factionId)
        {
            case PlayerFactionData.FACTION_SORCERER -> COLOR_SORCERER;
            case PlayerFactionData.FACTION_CURSE_USER -> COLOR_CURSE_USER;
            case PlayerFactionData.FACTION_CURSE_SPIRIT -> COLOR_CURSE_SPIRIT;
            default -> null;
        };
        return color == null ? null : new DustParticleOptions(color, DUST_SCALE);
    }

    @Override
    public void tick()
    {
        super.tick();
        ++this.time;
        if (this.level().isClientSide || this.tickCount % PARTICLE_INTERVAL_TICKS != 0)
        {
            return;
        }

        String factionId = this.getFactionId();
        DustParticleOptions dust = dustParticleForFaction(factionId);
        if (dust == null || !(this.level() instanceof ServerLevel serverLevel))
        {
            return;
        }

        double x = this.getX();
        double y = this.getY() + this.getBbHeight() * 0.55D;
        double z = this.getZ();
        serverLevel.sendParticles(
                dust,
                x,
                y,
                z,
                PARTICLE_COUNT,
                PARTICLE_SPREAD,
                PARTICLE_SPREAD,
                PARTICLE_SPREAD,
                0.015D);
    }

    @Override
    public boolean isPushable()
    {
        return false;
    }

    @Override
    protected MovementEmission getMovementEmission()
    {
        return MovementEmission.NONE;
    }

    public boolean showsBottom()
    {
        return true;
    }

    @Override
    public boolean hurt(DamageSource source, float amount)
    {
        if (this.isInvulnerableTo(source))
        {
            return false;
        }
        return super.hurt(source, amount);
    }

    @Override
    public Iterable<ItemStack> getArmorSlots()
    {
        return java.util.Collections.emptyList();
    }

    @Override
    public ItemStack getItemBySlot(EquipmentSlot slot)
    {
        return ItemStack.EMPTY;
    }

    @Override
    public void setItemSlot(EquipmentSlot slot, ItemStack stack)
    {
    }

    @Override
    public HumanoidArm getMainArm()
    {
        return HumanoidArm.RIGHT;
    }
}
