package com.darkness.jujutsukaisenzone.technique.shikigami.tenshadows.ability;

import com.darkness.jujutsukaisenzone.technique.shikigami.ShikigamiAbility;
import com.darkness.jujutsukaisenzone.technique.shikigami.ShikigamiAbilitySource;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

/**
 * 蝦蟇 · 牵引（主动，Shift+3）：蛙把术师拉过去。
 */
public final class ToadPullLogic
{
    public static final String TAG_PULL_TICKS = "jujutsukaisenzone_toad_pull_ticks";
    public static final String TAG_PULL_TOAD = "jujutsukaisenzone_toad_pull_toad";

    private static final int PULL_TICKS = 12;
    private static final double SNAP_DISTANCE = 22.0D;

    private ToadPullLogic()
    {
    }

    public static boolean cast(ServerPlayer owner, ShikigamiAbility ability, ShikigamiAbilitySource source)
    {
        if (source != ShikigamiAbilitySource.PLAYER)
        {
            return false;
        }
        Mob toad = ToadSupport.firstToad(owner);
        if (toad == null)
        {
            owner.displayClientMessage(
                    Component.literal("蝦蟇 · 牵引：场上没有蝦蟇").withStyle(ChatFormatting.GRAY),
                    true);
            return false;
        }
        if (owner.getVehicle() != null)
        {
            owner.stopRiding();
        }

        ServerLevel level = owner.serverLevel();
        tongueParticles(level, toad.position().add(0.0D, 0.4D, 0.0D), owner.position().add(0.0D, 1.0D, 0.0D));

        double distSq = owner.distanceToSqr(toad);
        if (distSq > SNAP_DISTANCE * SNAP_DISTANCE)
        {
            /* 过远直接落到蛙旁 */
            owner.teleportTo(toad.getX(), toad.getY() + 0.2D, toad.getZ());
            owner.hurtMarked = true;
            owner.displayClientMessage(
                    Component.literal("蝦蟇 · 牵引").withStyle(ChatFormatting.GREEN),
                    true);
            return true;
        }

        owner.getPersistentData().putInt(TAG_PULL_TICKS, PULL_TICKS);
        owner.getPersistentData().putUUID(TAG_PULL_TOAD, toad.getUUID());
        owner.displayClientMessage(
                Component.literal("蝦蟇 · 牵引").withStyle(ChatFormatting.GREEN),
                true);
        return true;
    }

    public static void tickPlayer(ServerPlayer owner)
    {
        if (owner == null || owner.level().isClientSide)
        {
            return;
        }
        int remain = owner.getPersistentData().getInt(TAG_PULL_TICKS);
        if (remain <= 0 || !owner.getPersistentData().hasUUID(TAG_PULL_TOAD))
        {
            return;
        }
        remain--;
        if (remain <= 0)
        {
            clear(owner);
            return;
        }
        owner.getPersistentData().putInt(TAG_PULL_TICKS, remain);

        if (!(owner.level() instanceof ServerLevel level))
        {
            return;
        }
        var entity = level.getEntity(owner.getPersistentData().getUUID(TAG_PULL_TOAD));
        if (!(entity instanceof Mob toad) || !toad.isAlive() || !ToadSupport.isToad(toad))
        {
            clear(owner);
            return;
        }

        Vec3 dest = toad.position().add(0.0D, 0.15D, 0.0D);
        Vec3 delta = dest.subtract(owner.position());
        double distSq = delta.lengthSqr();
        if (distSq < 1.0D)
        {
            clear(owner);
            return;
        }
        double speed = remain <= 3 ? 1.35D : 0.95D;
        owner.setDeltaMovement(delta.normalize().scale(speed));
        owner.hurtMarked = true;
        owner.fallDistance = 0.0F;
        level.sendParticles(
                ParticleTypes.ITEM_SLIME,
                owner.getX(), owner.getY() + 1.0D, owner.getZ(),
                2, 0.1D, 0.1D, 0.1D, 0.0D);
    }

    private static void clear(ServerPlayer owner)
    {
        owner.getPersistentData().remove(TAG_PULL_TICKS);
        owner.getPersistentData().remove(TAG_PULL_TOAD);
    }

    private static void tongueParticles(ServerLevel level, Vec3 from, Vec3 to)
    {
        Vec3 delta = to.subtract(from);
        double len = delta.length();
        if (len < 1.0E-3D)
        {
            return;
        }
        int count = Math.max(10, (int) (len * 4.0D));
        Vec3 step = delta.scale(1.0D / count);
        Vec3 pos = from;
        for (int i = 0; i < count; i++)
        {
            level.sendParticles(ParticleTypes.ITEM_SLIME, pos.x, pos.y, pos.z, 1, 0.02D, 0.02D, 0.02D, 0.0D);
            pos = pos.add(step);
        }
    }
}
