package com.darkness.jujutsukaisenzone.photon;

import com.darkness.jujutsukaisenzone.network.ModNetwork;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * 御厨子斩击 Photon：仅向施法者客户端同步，其他玩家不可见。
 */
public final class OwnerOnlyShrineSlashFx
{
    private OwnerOnlyShrineSlashFx()
    {
    }

    @Nullable
    public static ServerPlayer resolveOwnerPlayer(ServerLevel level, @Nullable UUID ownerUuid)
    {
        if (level.getServer() == null || ownerUuid == null)
        {
            return null;
        }
        return level.getServer().getPlayerList().getPlayer(ownerUuid);
    }

    public static void mount(ServerPlayer owner, Entity anchor, ResourceLocation fxId, OwnerOnlyPhotonFxParams params)
    {
        if (owner == null || !anchor.isAlive())
        {
            return;
        }
        ModNetwork.sendOwnerOnlyPhotonMount(owner, anchor.getId(), fxId, params);
    }

    public static void remove(ServerPlayer owner, Entity anchor, ResourceLocation fxId, boolean force)
    {
        if (owner == null)
        {
            return;
        }
        ModNetwork.sendOwnerOnlyPhotonRemove(owner, anchor.getId(), fxId, force);
    }

    public static void mountKaiArrival(ServerPlayer owner, Entity anchor, boolean horizontal)
    {
        mount(owner, anchor, horizontal ? ShrineSlashFxIds.SLASH_KAI_2 : ShrineSlashFxIds.SLASH_KAI_1, OwnerOnlyPhotonFxParams.KAI_SLASH);
    }

    public static void mountFlyobj(ServerPlayer owner, Entity anchor)
    {
        mount(owner, anchor, ShrineSlashFxIds.SLASH_KAI_FLYOBJ, OwnerOnlyPhotonFxParams.KAI_SLASH);
    }

    public static void removeFlyobj(ServerPlayer owner, Entity anchor)
    {
        remove(owner, anchor, ShrineSlashFxIds.SLASH_KAI_FLYOBJ, false);
    }

    public static void mountHachi(ServerPlayer owner, Entity target)
    {
        mount(owner, target, ShrineSlashFxIds.SLASH_HACHI, OwnerOnlyPhotonFxParams.HACHI);
    }

    public static void removeHachi(ServerPlayer owner, Entity target)
    {
        remove(owner, target, ShrineSlashFxIds.SLASH_HACHI, false);
    }

    public static void mountDomainSlash(ServerPlayer caster, LivingEntity target)
    {
        mount(caster, target, ShrineSlashFxIds.SHRINE_SLASH, OwnerOnlyPhotonFxParams.DOMAIN_DEFAULT);
    }

    public static void removeDomainSlash(ServerPlayer caster, LivingEntity target)
    {
        remove(caster, target, ShrineSlashFxIds.SHRINE_SLASH, false);
    }

    public static void mountDomainPowerSlash(ServerPlayer caster, LivingEntity target)
    {
        mount(caster, target, ShrineSlashFxIds.SHRINE_SLASH_POWER, OwnerOnlyPhotonFxParams.DOMAIN_DEFAULT);
    }

    public static void removeDomainPowerSlash(ServerPlayer caster, LivingEntity target)
    {
        remove(caster, target, ShrineSlashFxIds.SHRINE_SLASH_POWER, false);
    }
}
