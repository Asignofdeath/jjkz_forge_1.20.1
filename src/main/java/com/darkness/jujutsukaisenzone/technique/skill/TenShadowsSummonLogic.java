package com.darkness.jujutsukaisenzone.technique.skill;

import com.darkness.jujutsukaisenzone.compat.EpicFightCompat;
import com.darkness.jujutsukaisenzone.player.PlayerTechniqueData;
import com.darkness.jujutsukaisenzone.player.PlayerTenShadowsContractData;
import com.darkness.jujutsukaisenzone.player.PlayerTenShadowsModeData;
import com.darkness.jujutsukaisenzone.player.PlayerTenShadowsRosterData;
import com.darkness.jujutsukaisenzone.technique.skill.tenshadows.TenShadowsPresenceBonuses;
import com.darkness.jujutsukaisenzone.technique.skill.tenshadows.TenShadowsShikigamiStatsApplier;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import com.darkness.jujutsukaisenzone.entity.JadeDogEntity;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 影之式神模式：按键召出 / 收回原版占位生物。
 */
public final class TenShadowsSummonLogic
{
    public static final String TAG_OWNER = "jujutsukaisenzone_ten_shadows_owner";
    public static final String TAG_INDEX = "jujutsukaisenzone_ten_shadows_index";

    private TenShadowsSummonLogic()
    {
    }

    /** 已调伏友方召出体（非调伏仪式敌对体）。 */
    public static boolean isFriendlyOwned(Entity entity)
    {
        if (!(entity instanceof LivingEntity living) || !living.getPersistentData().hasUUID(TAG_OWNER))
        {
            return false;
        }
        return !living.getPersistentData().getBoolean(TenShadowsRitualLogic.TAG_RITUAL);
    }

    public static boolean isOwnedBy(Entity entity, UUID ownerId)
    {
        if (ownerId == null || !isFriendlyOwned(entity))
        {
            return false;
        }
        return ownerId.equals(entity.getPersistentData().getUUID(TAG_OWNER));
    }

    @javax.annotation.Nullable
    public static UUID getOwnerId(Entity entity)
    {
        if (entity == null || !entity.getPersistentData().hasUUID(TAG_OWNER))
        {
            return null;
        }
        return entity.getPersistentData().getUUID(TAG_OWNER);
    }

    public static void toggleShikigami(ServerPlayer player, int shikigamiIndex)
    {
        if (!canControl(player))
        {
            return;
        }
        TenShadowsShikigami shikigami = TenShadowsShikigami.byIndex(shikigamiIndex);
        if (shikigami == null)
        {
            return;
        }
        if (!PlayerTenShadowsContractData.isTamed(player, shikigami))
        {
            player.displayClientMessage(
                    Component.literal("尚未调伏 · " + shikigami.displayName()).withStyle(ChatFormatting.YELLOW),
                    true);
            return;
        }
        pruneDead(player, shikigamiIndex);
        if (PlayerTenShadowsRosterData.hasAliveEntry(player, shikigamiIndex))
        {
            recall(player, shikigami);
            return;
        }
        summon(player, shikigami);
    }

    public static void dismissAll(ServerPlayer player)
    {
        dismissAll(player, true);
    }

    public static void dismissAll(ServerPlayer player, boolean announce)
    {
        if (player == null)
        {
            return;
        }
        Map<Integer, List<UUID>> snapshot = PlayerTenShadowsRosterData.snapshot(player);
        for (Map.Entry<Integer, List<UUID>> entry : snapshot.entrySet())
        {
            TenShadowsShikigami shikigami = TenShadowsShikigami.byIndex(entry.getKey());
            discardEntities(player.serverLevel(), entry.getValue());
            if (announce && shikigami != null)
            {
                player.displayClientMessage(
                        Component.literal("收回 · " + shikigami.displayName()).withStyle(ChatFormatting.GRAY),
                        true);
            }
        }
        PlayerTenShadowsRosterData.clearAll(player);
        TenShadowsPresenceBonuses.refresh(player);
    }

    private static boolean canControl(ServerPlayer player)
    {
        return EpicFightCompat.isBattleMode(player)
                && PlayerTechniqueData.isTenShadows(player)
                && PlayerTenShadowsModeData.isShikigamiMode(player)
                && player.isAlive()
                && !player.isSpectator();
    }

    private static void summon(ServerPlayer player, TenShadowsShikigami shikigami)
    {
        ServerLevel level = player.serverLevel();
        List<UUID> spawned = new ArrayList<>();
        Vec3 look = player.getLookAngle();
        Vec3 base = player.position().add(look.x * 1.5D, 0.0D, look.z * 1.5D);

        int count = shikigami.spawnCount();
        for (int i = 0; i < count; i++)
        {
            double ox = (count == 1) ? 0.0D : (i % 4) * 0.6D - 0.9D;
            double oz = (count == 1) ? 0.0D : (i / 4) * 0.6D;
            Entity entity = shikigami.placeholderType().create(level);
            if (!(entity instanceof LivingEntity living))
            {
                continue;
            }
            living.moveTo(base.x + ox, player.getY(), base.z + oz, player.getYRot(), 0.0F);
            prepareShikigami(player, living, shikigami, i);
            if (!level.addFreshEntity(living))
            {
                continue;
            }
            spawned.add(living.getUUID());
        }

        if (spawned.isEmpty())
        {
            player.displayClientMessage(
                    Component.literal("召唤失败 · " + shikigami.displayName()).withStyle(ChatFormatting.RED),
                    true);
            return;
        }
        PlayerTenShadowsRosterData.put(player, shikigami.index(), spawned);
        TenShadowsPresenceBonuses.refresh(player);
        player.displayClientMessage(
                Component.literal("召出 · " + shikigami.displayName()).withStyle(ChatFormatting.DARK_PURPLE),
                true);
    }

    private static void recall(ServerPlayer player, TenShadowsShikigami shikigami)
    {
        List<UUID> ids = PlayerTenShadowsRosterData.get(player, shikigami.index());
        discardEntities(player.serverLevel(), ids);
        PlayerTenShadowsRosterData.clearSlot(player, shikigami.index());
        TenShadowsPresenceBonuses.refresh(player);
        player.displayClientMessage(
                Component.literal("收回 · " + shikigami.displayName()).withStyle(ChatFormatting.GRAY),
                true);
    }

    private static void pruneDead(ServerPlayer player, int shikigamiIndex)
    {
        List<UUID> ids = PlayerTenShadowsRosterData.get(player, shikigamiIndex);
        if (ids.isEmpty())
        {
            return;
        }
        ServerLevel level = player.serverLevel();
        List<UUID> alive = new ArrayList<>();
        for (UUID id : ids)
        {
            Entity entity = level.getEntity(id);
            if (entity != null && entity.isAlive())
            {
                alive.add(id);
            }
        }
        if (alive.isEmpty())
        {
            PlayerTenShadowsRosterData.clearSlot(player, shikigamiIndex);
        }
        else if (alive.size() != ids.size())
        {
            PlayerTenShadowsRosterData.put(player, shikigamiIndex, alive);
        }
    }

    private static void discardEntities(ServerLevel level, List<UUID> ids)
    {
        for (UUID id : ids)
        {
            Entity entity = level.getEntity(id);
            if (entity != null)
            {
                entity.discard();
            }
        }
    }

    private static void prepareShikigami(ServerPlayer owner, LivingEntity entity, TenShadowsShikigami shikigami, int spawnIndex)
    {
        String name = shikigami.displayName();
        if (shikigami == TenShadowsShikigami.DIVINE_DOGS)
        {
            name = spawnIndex == 0 ? "玉犬·白" : "玉犬·黑";
        }
        entity.setCustomName(Component.literal(name));
        entity.setCustomNameVisible(true);
        CompoundTag data = entity.getPersistentData();
        data.putUUID(TAG_OWNER, owner.getUUID());
        data.putInt(TAG_INDEX, shikigami.index());

        if (entity instanceof Mob mob)
        {
            mob.setNoAi(false);
            mob.setPersistenceRequired();
        }
        if (entity instanceof JadeDogEntity jadeDog)
        {
            jadeDog.setWhiteVariant(spawnIndex == 0);
            jadeDog.applyMaturityFromOwner(owner);
            jadeDog.setTame(true);
            jadeDog.setOwnerUUID(owner.getUUID());
            jadeDog.setOrderedToSit(false);
        }
        else if (entity instanceof Wolf wolf)
        {
            wolf.setTame(true);
            wolf.setOwnerUUID(owner.getUUID());
            wolf.setOrderedToSit(false);
            wolf.setCollarColor(spawnIndex == 0
                    ? net.minecraft.world.item.DyeColor.WHITE
                    : net.minecraft.world.item.DyeColor.BLACK);
        }
        if (entity instanceof Hoglin hoglin)
        {
            hoglin.setImmuneToZombification(true);
        }
        if (entity instanceof Ravager)
        {
            // 占位：防止立刻冲撞主人，后续再换自定义 AI
            entity.setDeltaMovement(Vec3.ZERO);
        }
        TenShadowsShikigamiStatsApplier.applyTamed(entity, shikigami, owner);
    }

    /**
     * 在已有名册上追加召出（脱兔爆发等）。槽位为空时新建名册。
     *
     * @return 实际刷出数量
     */
    public static int appendSummons(ServerPlayer player, TenShadowsShikigami shikigami, int count, Vec3 center)
    {
        if (player == null || shikigami == null || count <= 0 || center == null)
        {
            return 0;
        }
        ServerLevel level = player.serverLevel();
        List<UUID> roster = new ArrayList<>(PlayerTenShadowsRosterData.get(player, shikigami.index()));
        int spawned = 0;
        for (int i = 0; i < count; i++)
        {
            double angle = (Math.PI * 2.0D * i) / count;
            double ox = Math.cos(angle) * 1.4D;
            double oz = Math.sin(angle) * 1.4D;
            Entity entity = shikigami.placeholderType().create(level);
            if (!(entity instanceof LivingEntity living))
            {
                continue;
            }
            living.moveTo(center.x + ox, center.y, center.z + oz, player.getYRot(), 0.0F);
            prepareShikigami(player, living, shikigami, roster.size() + i);
            if (!level.addFreshEntity(living))
            {
                continue;
            }
            roster.add(living.getUUID());
            spawned++;
        }
        if (spawned > 0)
        {
            PlayerTenShadowsRosterData.put(player, shikigami.index(), roster);
            TenShadowsPresenceBonuses.refresh(player);
        }
        return spawned;
    }
}

