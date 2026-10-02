package com.darkness.jujutsukaisenzone.entity.behavior;

import com.darkness.jujutsukaisenzone.entity.SpecialEffectEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * 特效实体行为策略基类。
 * <p>
 * 每个术式效果独立一个子类，自持状态字段，通过 {@link #tick(SpecialEffectEntity)} 驱动。
 * NBT 序列化/反序列化由行为自行处理。
 * <p>
 * ⛔ <b>新技能先复用、再新建</b>：飞行 / 区域清场 / Photon 挂载 / 定时 等机制都已有公共实现
 * （{@code entity.util.SpecialEffectFlight}、{@code entity.util.AreaShape} + {@code entity.util.BlockErase}、
 * {@code photon.PhotonFxCommands}），别再逐字抄一份进新 Behavior。
 * 新增子类记得在 {@link #buildRegistry()} 登记 id。
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

    /**
     * id → 构造器 注册表。
     * <p>
     * 新增 Behavior 只在 {@link #buildRegistry()} 里加一行；⛔ 别再往 {@code createFromId} 里塞
     * {@code switch} 分支。id 重复会在类加载时直接抛异常——复制粘贴忘改 {@code ID} 是静默失败，
     * 必须炸出来。
     */
    private static final Map<String, Supplier<EffectBehavior>> REGISTRY = buildRegistry();

    private static Map<String, Supplier<EffectBehavior>> buildRegistry()
    {
        Map<String, Supplier<EffectBehavior>> map = new HashMap<>();
        register(map, LimitlessBlueBehavior.ID, LimitlessBlueBehavior::new);
        register(map, LimitlessRedBehavior.ID, LimitlessRedBehavior::new);
        register(map, LimitlessRedChargeBehavior.ID, LimitlessRedChargeBehavior::new);
        register(map, LimitlessPurpleBehavior.ID, LimitlessPurpleBehavior::new);
        register(map, ShrineKaiDelayBehavior.ID, ShrineKaiDelayBehavior::new);
        register(map, ShrineKaiArrivalBehavior.ID, ShrineKaiArrivalBehavior::new);
        register(map, ShrineHachiBehavior.ID, ShrineHachiBehavior::new);
        register(map, BlackFlashFxBehavior.ID, BlackFlashFxBehavior::new);
        register(map, BlackFlashShockwaveBehavior.ID, BlackFlashShockwaveBehavior::new);
        register(map, LimitlessRedArrowBehavior.ID, LimitlessRedArrowBehavior::new);
        register(map, LimitlessPurpleArrowBehavior.ID, LimitlessPurpleArrowBehavior::new);
        register(map, LimitlessRemoteMurasakiBehavior.ID, LimitlessRemoteMurasakiBehavior::new);
        register(map, LimitlessRemoteMurasakiStartOrbBehavior.ID, LimitlessRemoteMurasakiStartOrbBehavior::new);
        register(map, ShrineKaiArrowBehavior.ID, ShrineKaiArrowBehavior::new);
        register(map, ShrineFugaBehavior.ID, ShrineFugaBehavior::new);
        register(map, ShrineFugaBoomBehavior.ID, ShrineFugaBoomBehavior::new);
        return Map.copyOf(map);
    }

    private static void register(
            Map<String, Supplier<EffectBehavior>> map,
            String id,
            Supplier<EffectBehavior> factory)
    {
        if (map.put(id, factory) != null)
        {
            throw new IllegalStateException("EffectBehavior id 重复: " + id);
        }
    }

    /** 从 NBT id 还原行为实例；id 未注册时返回 {@code null}。 */
    @Nullable
    public static EffectBehavior createFromId(String id)
    {
        Supplier<EffectBehavior> factory = REGISTRY.get(id);
        return factory == null ? null : factory.get();
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
