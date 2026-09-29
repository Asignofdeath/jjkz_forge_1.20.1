package com.darkness.jujutsukaisenzone.technique.shikigami.tenshadows.ability;

import com.darkness.jujutsukaisenzone.technique.shikigami.ShikigamiAbility;
import com.darkness.jujutsukaisenzone.technique.shikigami.ShikigamiAbilitySource;
import com.darkness.jujutsukaisenzone.technique.skill.TenShadowsSummonLogic;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;

/**
 * 满象 · 水流（BOTH，Ctrl+5）：在目标/象脚下铺原版水，数秒后收回。
 */
public final class MaxElephantWaterFloodLogic
{
    public static final String TAG_TEMP_WATER = "jujutsukaisenzone_elephant_temp_water";
    private static final double AUTO_RANGE = 12.0D;
    private static final int WATER_LIFE_TICKS = 20 * 6;
    private static final int RADIUS = 2;

    private MaxElephantWaterFloodLogic()
    {
    }

    public static boolean cast(ServerPlayer owner, ShikigamiAbility ability, ShikigamiAbilitySource source)
    {
        Mob elephant = MaxElephantSupport.first(owner);
        if (elephant == null)
        {
            return false;
        }
        LivingEntity focus = resolveFocus(owner, elephant, source);
        if (source == ShikigamiAbilitySource.AUTO && focus == null)
        {
            return false;
        }
        BlockPos center = focus != null ? focus.blockPosition() : elephant.blockPosition();

        ServerLevel level = owner.serverLevel();
        int placed = 0;
        long expire = level.getGameTime() + WATER_LIFE_TICKS;
        ListTag list = elephant.getPersistentData().getList(TAG_TEMP_WATER, Tag.TAG_COMPOUND);

        for (int dx = -RADIUS; dx <= RADIUS; dx++)
        {
            for (int dz = -RADIUS; dz <= RADIUS; dz++)
            {
                if (dx * dx + dz * dz > RADIUS * RADIUS)
                {
                    continue;
                }
                BlockPos pos = findWaterSurface(level, center.offset(dx, 0, dz));
                if (pos == null)
                {
                    continue;
                }
                BlockState state = level.getBlockState(pos);
                if (!state.canBeReplaced(Fluids.WATER) && !state.isAir())
                {
                    continue;
                }
                if (!level.setBlock(pos, Blocks.WATER.defaultBlockState(), 3))
                {
                    continue;
                }
                CompoundTag entry = new CompoundTag();
                entry.putInt("x", pos.getX());
                entry.putInt("y", pos.getY());
                entry.putInt("z", pos.getZ());
                entry.putLong("expire", expire);
                list.add(entry);
                placed++;
            }
        }
        elephant.getPersistentData().put(TAG_TEMP_WATER, list);

        if (placed <= 0)
        {
            if (source == ShikigamiAbilitySource.PLAYER)
            {
                owner.displayClientMessage(
                        Component.literal("满象 · 水流：无法铺水").withStyle(ChatFormatting.GRAY),
                        true);
            }
            return false;
        }
        if (source == ShikigamiAbilitySource.PLAYER)
        {
            owner.displayClientMessage(
                    Component.literal("满象 · 水流").withStyle(ChatFormatting.AQUA),
                    true);
        }
        return true;
    }

    public static void tickElephant(Mob elephant)
    {
        if (elephant.level().isClientSide || !(elephant.level() instanceof ServerLevel level))
        {
            return;
        }
        if (!MaxElephantSupport.isMaxElephant(elephant))
        {
            return;
        }
        ListTag list = elephant.getPersistentData().getList(TAG_TEMP_WATER, Tag.TAG_COMPOUND);
        if (list.isEmpty())
        {
            return;
        }
        long now = level.getGameTime();
        ListTag remain = new ListTag();
        for (int i = 0; i < list.size(); i++)
        {
            CompoundTag entry = list.getCompound(i);
            BlockPos pos = new BlockPos(entry.getInt("x"), entry.getInt("y"), entry.getInt("z"));
            long expire = entry.getLong("expire");
            if (now < expire)
            {
                remain.add(entry);
                continue;
            }
            BlockState state = level.getBlockState(pos);
            if (state.is(Blocks.WATER) || state.getFluidState().is(Fluids.WATER))
            {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            }
        }
        if (remain.isEmpty())
        {
            elephant.getPersistentData().remove(TAG_TEMP_WATER);
        }
        else
        {
            elephant.getPersistentData().put(TAG_TEMP_WATER, remain);
        }
    }

    private static LivingEntity resolveFocus(ServerPlayer owner, Mob elephant, ShikigamiAbilitySource source)
    {
        LivingEntity target = elephant.getTarget();
        if (target != null && target.isAlive() && !TenShadowsSummonLogic.isOwnedBy(target, owner.getUUID()))
        {
            if (source == ShikigamiAbilitySource.AUTO
                    && elephant.distanceToSqr(target) > AUTO_RANGE * AUTO_RANGE)
            {
                return null;
            }
            return target;
        }
        if (source == ShikigamiAbilitySource.AUTO)
        {
            return null;
        }
        LivingEntity cross = com.darkness.jujutsukaisenzone.technique.skill.TenShadowsRitualLogic
                .resolveCrosshairLiving(owner, 28.0D);
        return cross;
    }

    /** 在柱上找可放水的空位（目标脚边或脚上一格）。 */
    private static BlockPos findWaterSurface(ServerLevel level, BlockPos hint)
    {
        BlockPos.MutableBlockPos cursor = hint.mutable();
        for (int i = 0; i < 4; i++)
        {
            BlockState state = level.getBlockState(cursor);
            if (state.canBeReplaced(Fluids.WATER) || state.isAir())
            {
                BlockPos below = cursor.below();
                BlockState belowState = level.getBlockState(below);
                if (!belowState.isAir() && belowState.getFluidState().isEmpty())
                {
                    return cursor.immutable();
                }
            }
            cursor.move(0, -1, 0);
        }
        return null;
    }
}
