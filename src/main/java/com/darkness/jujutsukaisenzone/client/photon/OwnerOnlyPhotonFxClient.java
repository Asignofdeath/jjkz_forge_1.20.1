package com.darkness.jujutsukaisenzone.client.photon;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.photon.OwnerOnlyPhotonFxParams;
import com.lowdragmc.photon.client.fx.EntityEffect;
import com.lowdragmc.photon.client.fx.FXHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 在施法者本地客户端播放 Photon 特效，替代服务端 {@code photon fx} 全员广播。
 */
@Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, value = Dist.CLIENT)
public final class OwnerOnlyPhotonFxClient
{
    private static final int MAX_MOUNT_RETRIES = 20;
    private static final List<PendingMount> PENDING_MOUNTS = new ArrayList<>();

    private OwnerOnlyPhotonFxClient()
    {
    }

    public static void mount(int entityId, ResourceLocation fxId, OwnerOnlyPhotonFxParams params)
    {
        Minecraft minecraft = Minecraft.getInstance();
        Level level = minecraft.level;
        if (level == null)
        {
            queuePending(entityId, fxId, params);
            return;
        }

        Entity entity = level.getEntity(entityId);
        if (entity == null)
        {
            queuePending(entityId, fxId, params);
            return;
        }

        applyMount(level, entity, fxId, params);
    }

    public static void remove(int entityId, boolean force, @Nullable ResourceLocation fxId)
    {
        Minecraft minecraft = Minecraft.getInstance();
        Level level = minecraft.level;
        if (level == null)
        {
            return;
        }

        Entity entity = level.getEntity(entityId);
        if (entity == null)
        {
            return;
        }

        var effects = EntityEffect.CACHE.get(entity);
        if (effects == null)
        {
            return;
        }

        var iterator = effects.iterator();
        while (iterator.hasNext())
        {
            var effect = iterator.next();
            if (fxId != null && !fxId.equals(effect.getFx().getFxLocation()))
            {
                continue;
            }
            iterator.remove();
            var runtime = effect.getRuntime();
            if (runtime != null && runtime.isAlive())
            {
                runtime.destroy(force);
            }
        }

        if (effects.isEmpty())
        {
            EntityEffect.CACHE.remove(entity);
        }
    }

    @SubscribeEvent
    public static void onClientTickEnd(TickEvent.ClientTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END || PENDING_MOUNTS.isEmpty())
        {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        Level level = minecraft.level;
        if (level == null)
        {
            return;
        }

        Iterator<PendingMount> iterator = PENDING_MOUNTS.iterator();
        while (iterator.hasNext())
        {
            PendingMount pending = iterator.next();
            Entity entity = level.getEntity(pending.entityId);
            if (entity == null)
            {
                pending.retries++;
                if (pending.retries >= MAX_MOUNT_RETRIES)
                {
                    iterator.remove();
                }
                continue;
            }

            applyMount(level, entity, pending.fxId, pending.params);
            iterator.remove();
        }
    }

    private static void applyMount(Level level, Entity entity, ResourceLocation fxId, OwnerOnlyPhotonFxParams params)
    {
        var fx = FXHelper.getFX(fxId);
        if (fx == null)
        {
            return;
        }

        EntityEffect.AutoRotate autoRotate = autoRotateFromOrdinal(params.autoRotateOrdinal());
        var effect = new EntityEffect(fx, level, entity, autoRotate);
        effect.setOffset(params.offset().x, params.offset().y, params.offset().z);
        effect.setRotation(params.rotation().x, params.rotation().y, params.rotation().z);
        effect.setScale(params.scale().x, params.scale().y, params.scale().z);
        effect.setDelay(params.delay());
        effect.setForcedDeath(params.forcedDeath());
        effect.setAllowMulti(params.allowMulti());
        effect.start();
    }

    private static EntityEffect.AutoRotate autoRotateFromOrdinal(int ordinal)
    {
        EntityEffect.AutoRotate[] values = EntityEffect.AutoRotate.values();
        if (ordinal < 0 || ordinal >= values.length)
        {
            return EntityEffect.AutoRotate.NONE;
        }
        return values[ordinal];
    }

    private static void queuePending(int entityId, ResourceLocation fxId, OwnerOnlyPhotonFxParams params)
    {
        PENDING_MOUNTS.add(new PendingMount(entityId, fxId, params));
    }

    private static final class PendingMount
    {
        private final int entityId;
        private final ResourceLocation fxId;
        private final OwnerOnlyPhotonFxParams params;
        private int retries;

        private PendingMount(int entityId, ResourceLocation fxId, OwnerOnlyPhotonFxParams params)
        {
            this.entityId = entityId;
            this.fxId = fxId;
            this.params = params;
        }
    }
}
