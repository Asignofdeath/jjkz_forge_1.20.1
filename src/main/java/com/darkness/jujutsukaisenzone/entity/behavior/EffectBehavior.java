package com.darkness.jujutsukaisenzone.entity.behavior;

import com.darkness.jujutsukaisenzone.entity.SpecialEffectEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * 特效实体行为策略基类。
 * <p>
 * 每个术式效果独立一个子类，自持状态字段，通过 {@link #tick(SpecialEffectEntity)} 驱动。
 * NBT 序列化/反序列化由行为自行处理。
 */
public abstract class EffectBehavior
{
    /** 行为类型标识，用于 NBT 持久化和工厂恢复 */
    public abstract String id();

    /** 每 tick 调用，由宿主实体驱动 */
    public abstract void tick(SpecialEffectEntity entity);

    /** 在宿主实体 discard 前调用 */
    public void onDiscard(SpecialEffectEntity entity) {}

    // ==== NBT ====

    /** 将行为状态写入 tag（不含行为 id，id 由宿主实体管理） */
    public abstract void writeToNbt(CompoundTag tag);

    /** 从 tag 恢复行为状态 */
    public abstract void readFromNbt(CompoundTag tag);

    // ==== 工厂 ====

    /** 从 NBT id 还原行为实例 */
    @Nullable
    public static EffectBehavior createFromId(String id)
    {
        return switch (id)
        {
            case LimitlessBlueBehavior.ID -> new LimitlessBlueBehavior();
            case LimitlessRedBehavior.ID -> new LimitlessRedBehavior();
            case LimitlessRedChargeBehavior.ID -> new LimitlessRedChargeBehavior();
            case LimitlessPurpleBehavior.ID -> new LimitlessPurpleBehavior();
            case ShrineKaiDelayBehavior.ID -> new ShrineKaiDelayBehavior();
            case ShrineKaiArrivalBehavior.ID -> new ShrineKaiArrivalBehavior();
            case ShrineKaiFlyobjBehavior.ID -> new ShrineKaiFlyobjBehavior();
            case ShrineHachiBehavior.ID -> new ShrineHachiBehavior();
            case BlackFlashFxBehavior.ID -> new BlackFlashFxBehavior();
            case BlackFlashShockwaveBehavior.ID -> new BlackFlashShockwaveBehavior();
            case LimitlessRedArrowBehavior.ID -> new LimitlessRedArrowBehavior();
            case LimitlessPurpleArrowBehavior.ID -> new LimitlessPurpleArrowBehavior();
            case LimitlessRemoteMurasakiBehavior.ID -> new LimitlessRemoteMurasakiBehavior();
            case LimitlessRemoteMurasakiStartOrbBehavior.ID -> new LimitlessRemoteMurasakiStartOrbBehavior();
            case ShrineKaiArrowBehavior.ID -> new ShrineKaiArrowBehavior();
            case ShrineFugaBehavior.ID -> new ShrineFugaBehavior();
            case ShrineFugaBoomBehavior.ID -> new ShrineFugaBoomBehavior();
            default -> null;
        };
    }

    // ==== 工具 ====

    @Nullable
    protected static LivingEntity resolveOwner(SpecialEffectEntity entity, @Nullable UUID ownerUuid)
    {
        if (ownerUuid == null || !(entity.level() instanceof ServerLevel level)) return null;
        return level.getEntity(ownerUuid) instanceof LivingEntity living ? living : null;
    }

    @Nullable
    protected static ServerPlayer resolveOwnerPlayer(SpecialEffectEntity entity, @Nullable UUID ownerUuid)
    {
        if (ownerUuid == null || !(entity.level() instanceof ServerLevel level) || level.getServer() == null)
        {
            return null;
        }
        return level.getServer().getPlayerList().getPlayer(ownerUuid);
    }
}
