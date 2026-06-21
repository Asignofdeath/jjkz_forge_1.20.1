package com.darkness.jujutsukaisenzone.technique.domain;

import com.darkness.jujutsukaisenzone.api.screen.ScreenShakeKind;
import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.blockentity.JJKZSkyblockEntity;
import com.darkness.jujutsukaisenzone.compat.EpicFightCompat;
import com.darkness.jujutsukaisenzone.effect.TechniqueMeltdownEffect;
import com.darkness.jujutsukaisenzone.entity.DemonSubduingShrineEntity;
import com.darkness.jujutsukaisenzone.entity.SpecialEffectEntity;
import com.darkness.jujutsukaisenzone.network.ModNetwork;
import com.darkness.jujutsukaisenzone.player.PlayerCursedEnergyData;
import com.darkness.jujutsukaisenzone.player.PlayerTechniqueData;
import com.darkness.jujutsukaisenzone.registry.ModEntities;
import com.darkness.jujutsukaisenzone.registry.ModSounds;
import com.darkness.jujutsukaisenzone.photon.OwnerOnlyShrineSlashFx;
import com.darkness.jujutsukaisenzone.technique.scaling.ShrineDomainSkillGrowth;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.Advancement;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 御厨子术式领域展开 — 伏魔神龛。
 */
public final class ShrineDomainExpansion extends AbstractTechniqueDomain
{
    private static final ShrineDomainExpansion INSTANCE = new ShrineDomainExpansion();

    private static final String DOMAIN_NAME = "伏魔神龛";
    /** 领域构建完成时使用的黑色天空盒。 */
    private static final ResourceLocation SHRINE_DOMAIN_INITIAL_SKYBOX_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(JuJutsuKaisenZone.MODID, "textures/environment/black");
    /** 领域构建完成 {@link #SHRINE_SKYBOX_SWAP_DELAY_TICKS} 刻后切换为此天空盒。 */
    private static final ResourceLocation SHRINE_DOMAIN_SKYBOX_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(JuJutsuKaisenZone.MODID, "textures/environment/shrine_cubemap");
    /** 领域构建完成后切换天空盒的延迟（tick），5.5 秒 = 110 tick。 */
    private static final int SHRINE_SKYBOX_SWAP_DELAY_TICKS = 110;
    /** 战前阶段时长（tick）：玩家/生物锁定与领域伤害开启，7 秒 = 140 tick。 */
    private static final int SHRINE_PRE_COMBAT_DURATION_TICKS = 140;
    /** 领域完成时，伏魔神龛生成在领域中心正北方向的直线距离（格）。 */
    private static final double SHRINE_SPAWN_NORTH_DISTANCE = 15.0D;
    /** 伏魔神龛固定朝向：面向南方（领域中心方向）。 */
    private static final float SHRINE_SPAWN_YAW = 0.0F;
    /** 领域完成时，施法者传送到神龛前方的距离（格）。 */
    private static final double CASTER_SPAWN_IN_FRONT_OF_SHRINE_DISTANCE = 5.0D;
    /** 领域展开蓄力时间（tick），5 秒 = 100 tick。 */
    private static final int CAST_DELAY_TICKS = 100;
    /** 蓄力剩余 40 tick 时开启风格滤镜（即蓄力开始后 60 tick = 3 秒） */
    private static final int CAST_FILTER_START_REMAINING = 40;
    /** 结界完成瞬间领域内攻击目标数 > 该值时触发「完美压制」反馈。 */
    private static final int PERFECT_SUPPRESSION_TARGET_THRESHOLD = 8;
    /** 「完美压制」金属音效相对默认播放音量。 */
    private static final float PERFECT_SUPPRESSION_IRON_SOUND_VOLUME = 5.0F;
    /** 领域展开演出音效对周围玩家的影响半径（格）。 */
    private static final double SHRINE_DOMAIN_EXPANSION_SOUND_RADIUS = 24.0D;
    /** 伏魔神龛旁特效实体挂载的 Photon（绑定实体失效时强制清除粒子）。 */
    private static final String SHRINE_DOMAIN_FX_COMMAND =
            "photon fx photon:shrine_domain_fx1a entity @s 0 0 0 0 0 0 1 1 1 0 true false none";
    /** 蓄力 5 秒结束后挂于施法者，auto rotate = look，跟随玩家视线朝向。 */
    private static final String SHRINE_DOMAIN_START_FX_COMMAND =
            "photon fx photon:shrine_domain_start_fx1a entity @s 0 0 0 0 0 0 1 1 1 0 false false look";
    private static final String SHRINE_DOMAIN_START_FX_REMOVE_COMMAND =
            "photon fx remove entity @s false photon:shrine_domain_start_fx1a";
    private static final String SHRINE_GHOST_FACE_FX_COMMAND = "photon fx photon:ghost_face1a entity @s";
    private static final String SHRINE_GHOST_FACE_FX_REMOVE_COMMAND =
            "photon fx remove entity @s false photon:ghost_face1a";
    /** 领域开始构建后，延迟在施法者前方生成 ghost_face 特效实体（tick），2 秒 = 40 tick。 */
    private static final int SHRINE_GHOST_FACE_SPAWN_DELAY_TICKS = 40;
    /** ghost_face 特效实体存续时长（tick），4 秒 = 80 tick。 */
    private static final int SHRINE_GHOST_FACE_DURATION_TICKS = 80;
    /** ghost_face 特效实体相对施法者水平前方的距离（格）。 */
    private static final double SHRINE_GHOST_FACE_FORWARD_DISTANCE = 4.0D;
    /** ghost_face 生成时屏幕平滑晃动持续 tick 数：40 tick = 2 秒 */
    private static final int GHOST_FACE_WOBBLE_DURATION_TICKS = 40;
    /** ghost_face 生成时屏幕平滑晃动最大振幅（度） */
    private static final float GHOST_FACE_WOBBLE_MAX_AMPLITUDE = 5.0F;
    private static final int SHRINE_DOMAIN_START_FX_DURATION_TICKS = 100;
    /** 领域存续期间对攻击目标的伤害间隔（刻）。 */
    private static final int SHRINE_SLASH_DAMAGE_INTERVAL_TICKS = 10;
    /** 领域内有攻击目标时，领域内玩家的持续屏幕振动幅度（度）。 */
    private static final float SHRINE_ATTACK_SCREEN_SHAKE_MAX_DEG = 0.8F;
    /** 强力斩击进行中的屏幕振动幅度（度）。 */
    private static final float POWER_SLASH_SCREEN_SHAKE_MAX_DEG = 2.0F;
    /** 锁定同一目标多久后触发强力斩击（3 秒 = 60 刻）。 */
    private static final int POWER_SLASH_LOCK_CHARGE_TICKS = 60;
    /** 强力斩击最长持续时间（8 秒 = 160 刻）。 */
    private static final int POWER_SLASH_DURATION_TICKS = 160;
    /** 强力斩击冷却（16 秒 = 320 刻）。 */
    private static final int POWER_SLASH_COOLDOWN_TICKS = 320;
    /** 强力斩击伤害间隔（刻）。 */
    private static final int POWER_SLASH_DAMAGE_INTERVAL_TICKS = 8;
    /** 强力斩击每次伤害额外消耗的咒力。 */
    private static final int POWER_SLASH_COST_PER_HIT = 4;

    /** 施法者 UUID → 本次领域生成的伏魔神龛实体 UUID */
    private final ConcurrentMap<UUID, UUID> shrineEntityByCaster = new ConcurrentHashMap<>();
    /** 施法者 UUID → 本次领域生成的伏魔神龛特效实体 UUID */
    private final ConcurrentMap<UUID, UUID> shrineFxEntityByCaster = new ConcurrentHashMap<>();
    /** 施法者 UUID → 应在施法者前方生成 ghost_face 特效实体的服务器 tick。 */
    private final ConcurrentMap<UUID, Integer> shrineGhostFaceSpawnAtServerTick = new ConcurrentHashMap<>();
    /** 施法者 UUID → 应清除 ghost_face 特效实体的服务器 tick。 */
    private final ConcurrentMap<UUID, Integer> shrineGhostFaceRemoveAtServerTick = new ConcurrentHashMap<>();
    /** 施法者 UUID → ghost_face 特效实体 UUID */
    private final ConcurrentMap<UUID, UUID> shrineGhostFaceEntityByCaster = new ConcurrentHashMap<>();
    /** 施法者 UUID → 蓄力风格滤镜是否已激活 */
    private final ConcurrentMap<UUID, Boolean> shrineCastFilterActiveByCaster = new ConcurrentHashMap<>();
    /** 施法者 UUID → 已挂上 shrine_slash 的领域内攻击目标 */
    private final ConcurrentMap<UUID, Set<UUID>> shrineSlashTargetsByCaster = new ConcurrentHashMap<>();
    /** 施法者 UUID → 距上次 shrine_slash 伤害的已过刻数 */
    private final ConcurrentMap<UUID, Integer> shrineSlashDamageTicksByCaster = new ConcurrentHashMap<>();
    /** 施法者 UUID → 当前正在接收攻击振动同步的玩家 */
    private final ConcurrentMap<UUID, Set<UUID>> shrineScreenShakeViewersByCaster = new ConcurrentHashMap<>();
    /** 施法者 UUID → 当前正在接收斩击循环音的玩家 */
    private final ConcurrentMap<UUID, Set<UUID>> shrineSlashLoopViewersByCaster = new ConcurrentHashMap<>();
    /** 施法者 UUID → Epic Fight 锁定报告 */
    private final ConcurrentMap<UUID, LockOnReport> lockOnReportByCaster = new ConcurrentHashMap<>();
    /** 施法者 UUID → 强力斩击状态 */
    private final ConcurrentMap<UUID, PowerSlashState> powerSlashStateByCaster = new ConcurrentHashMap<>();
    /** 正在蓄力中的玩家 UUID → 剩余 tick 数 */
    private final ConcurrentMap<UUID, Integer> castingDelayMap = new ConcurrentHashMap<>();
    /** 施法者 UUID → shrine_domain_start_fx1a 剩余存续 tick 数 */
    private final ConcurrentMap<UUID, Integer> shrineDomainStartFxTicksByCaster = new ConcurrentHashMap<>();
    /** 施法者 UUID → 应将天空盒切换为 {@link #SHRINE_DOMAIN_SKYBOX_TEXTURE} 的服务器 tick（含等于该 tick 时执行）。 */
    private final ConcurrentMap<UUID, Integer> shrineSkyboxSwapAtServerTick = new ConcurrentHashMap<>();
    /** 施法者 UUID → 应开启领域伤害的服务器 tick（含等于该 tick 时执行）。 */
    private final ConcurrentMap<UUID, Integer> shrineDomainDamageAtServerTick = new ConcurrentHashMap<>();
    /** 施法者 UUID → 领域伤害（含 shrine_slash / 强力斩击）已开启。 */
    private final Set<UUID> shrineDomainDamageStartedByCaster = ConcurrentHashMap.newKeySet();
    /** 领域构建完成至伤害开启前：领域内玩家移动锁定（玩家 UUID → 锁定状态）。 */
    private final ConcurrentMap<UUID, ShrinePreCombatPlayerLock> preCombatPlayerLockByPlayer = new ConcurrentHashMap<>();
    /** 领域构建完成至伤害开启前：领域内生物 AI 暂停（实体 UUID → 状态）。 */
    private final ConcurrentMap<UUID, ShrinePreCombatMobPause> preCombatMobPauseByEntity = new ConcurrentHashMap<>();

    private static final class LockOnReport
    {
        private boolean locking;
        private UUID targetUuid;
    }

    private static final class PowerSlashState
    {
        private int lockChargeTicks;
        private UUID lockChargeTargetUuid;
        private int activeTicksRemaining;
        private UUID activeTargetUuid;
        private int cooldownTicksRemaining;
        private int damageIntervalTicks;
    }

    private record ShrinePreCombatPlayerLock(UUID ownerUuid, Vec3 lockPos, float lockYaw, float lockPitch, int remainingTicks)
    {
        private ShrinePreCombatPlayerLock decreaseOneTick()
        {
            return new ShrinePreCombatPlayerLock(ownerUuid, lockPos, lockYaw, lockPitch, remainingTicks - 1);
        }
    }

    private record ShrinePreCombatMobPause(UUID ownerUuid, boolean aiPausedByThisLock, boolean originNoAi)
    {
    }

    public static ShrineDomainExpansion getInstance()
    {
        return INSTANCE;
    }

    private ShrineDomainExpansion()
    {
        setDomainName(DOMAIN_NAME);
    }

    @Override
    protected void applyDomainSpecificEffect(ServerPlayer player, BarrierInstance instance)
    {
        spawnShrineNorthOfDomainCenter(player, instance);
        ModNetwork.sendShrineDomainCmdCamSetup(player, player.position(), player.getYRot());
        scheduleShrineSkyboxTextureSwap(player);
        scheduleShrineDomainDamageStart(player);
        beginPreCombatLock(player, instance);
        ModNetwork.sendShrineDomainActiveSync(player, true);
        maybeAnnouncePerfectSuppression(player, instance);
    }

    /**
     * 伏魔神龛的施放逻辑：先进入蓄力状态，5 秒后才真正展开领域。
     * <p>
     * 如果玩家已在自己的领域内，立即解除领域（不经过蓄力）。
     */
    @Override
    public void cast(ServerPlayer player)
    {
        if (!player.isAlive() || !isInBattleMode(player))
        {
            return;
        }

        if (isPlayerInsideOwnDomain(player))
        {
            super.cast(player);
            return;
        }

        UUID uuid = player.getUUID();

        if (TechniqueMeltdownEffect.blockDomainExpansion(player))
        {
            return;
        }

        if (castingDelayMap.containsKey(uuid))
        {
            castingDelayMap.remove(uuid);
            EpicFightCompat.stopShrineDomainExpansionAnimation(player);
            stopShrineDomainExpansionSound(player);
            player.displayClientMessage(
                    Component.literal("「" + DOMAIN_NAME + "」已取消")
                            .withStyle(ChatFormatting.YELLOW),
                    true);
            return;
        }

        if (DomainExpansionRouter.isPositionInAnyDomain(player.serverLevel(), player.blockPosition()))
        {
            player.displayClientMessage(Component.literal("无法展开领域，因为领域对抗还没做").withStyle(ChatFormatting.RED), true);
            return;
        }
        if (!hasLearnedDomainExpansion(player))
        {
            player.displayClientMessage(Component.literal("尚未习得领域展开").withStyle(ChatFormatting.RED), true);
            return;
        }
        if (!hasLearnedTechnique(player))
        {
            return;
        }

        castingDelayMap.put(uuid, CAST_DELAY_TICKS);
        EpicFightCompat.playShrineDomainExpansionAnimation(player);
        broadcastShrineDomainExpansionSound(player, player.serverLevel());
        player.displayClientMessage(
                Component.literal("「" + DOMAIN_NAME + "」展开蓄力中...")
                        .withStyle(ChatFormatting.LIGHT_PURPLE),
                true);
    }

    private void tickCastingDelay(ServerPlayer player)
    {
        UUID uuid = player.getUUID();
        Integer remaining = castingDelayMap.get(uuid);
        if (remaining == null)
        {
            return;
        }

        if (!player.isAlive() || !isInBattleMode(player))
        {
            castingDelayMap.remove(uuid);
            stopShrineCastFilter(player);
            EpicFightCompat.stopShrineDomainExpansionAnimation(player);
            stopShrineDomainExpansionSound(player);
            player.displayClientMessage(
                    Component.literal("「" + DOMAIN_NAME + "」展开蓄力中断")
                            .withStyle(ChatFormatting.RED),
                    true);
            return;
        }

        if (remaining == CAST_FILTER_START_REMAINING + 1)
        {
            ModNetwork.sendShrineCastFilterSync(player, true);
            shrineCastFilterActiveByCaster.put(uuid, Boolean.TRUE);
        }

        int newRemaining = remaining - 1;
        if (newRemaining <= 0)
        {
            castingDelayMap.remove(uuid);
            stopShrineCastFilter(player);
            attachShrineDomainStartFxToPlayer(player);
            scheduleShrineGhostFaceFx(player);
            super.cast(player);
        }
        else
        {
            castingDelayMap.put(uuid, newRemaining);
        }
    }

    private boolean isPlayerInsideOwnDomain(ServerPlayer player)
    {
        Deque<BarrierInstance> queue = ACTIVE_DOMAINS.get(player.getUUID());
        if (queue == null || queue.isEmpty())
        {
            return false;
        }
        for (BarrierInstance instance : queue)
        {
            if (isPlayerInsideBarrier(player, instance))
            {
                return true;
            }
        }
        return false;
    }

    public boolean isCasting(ServerPlayer player)
    {
        return castingDelayMap.containsKey(player.getUUID());
    }

    public void clearCastingState(UUID uuid)
    {
        castingDelayMap.remove(uuid);
    }

    private static void broadcastShrineDomainExpansionSound(ServerPlayer caster, ServerLevel level)
    {
        double radiusSqr = SHRINE_DOMAIN_EXPANSION_SOUND_RADIUS * SHRINE_DOMAIN_EXPANSION_SOUND_RADIUS;
        SoundEvent sound = ModSounds.SHRINE_DOMAIN_EXPANSION.get();
        for (ServerPlayer nearbyPlayer : level.players())
        {
            if (nearbyPlayer.distanceToSqr(caster) > radiusSqr)
            {
                continue;
            }
            nearbyPlayer.playNotifySound(sound, SoundSource.PLAYERS, 1.0F, 1.0F);
        }
    }

    private static void stopShrineDomainExpansionSound(ServerPlayer caster)
    {
        ServerLevel level = caster.serverLevel();
        double radiusSqr = SHRINE_DOMAIN_EXPANSION_SOUND_RADIUS * SHRINE_DOMAIN_EXPANSION_SOUND_RADIUS;
        ClientboundStopSoundPacket stopExpansion = new ClientboundStopSoundPacket(
                ModSounds.SHRINE_DOMAIN_EXPANSION.get().getLocation(),
                SoundSource.PLAYERS);
        for (ServerPlayer nearbyPlayer : level.players())
        {
            if (nearbyPlayer.distanceToSqr(caster) > radiusSqr)
            {
                continue;
            }
            nearbyPlayer.connection.send(stopExpansion);
        }
    }

    /**
     * 客户端同步 Epic Fight 锁定目标；仅在自己伏魔神龛领域存续期内生效。
     */
    public void updateLockOnTarget(ServerPlayer caster, boolean locking, UUID targetUuid)
    {
        if (!hasLearnedTechnique(caster) || !isCasterInsideOwnActiveDomain(caster))
        {
            return;
        }
        if (!hasShrineDomainDamageStarted(caster))
        {
            return;
        }

        LockOnReport report = lockOnReportByCaster.computeIfAbsent(caster.getUUID(), key -> new LockOnReport());
        report.locking = locking;
        report.targetUuid = locking ? targetUuid : null;

        if (!locking || targetUuid == null)
        {
            return;
        }

        PowerSlashState state = powerSlashStateByCaster.get(caster.getUUID());
        if (state == null || state.activeTicksRemaining <= 0 || targetUuid.equals(state.activeTargetUuid))
        {
            return;
        }

        BarrierInstance instance = getActiveBarrierInstance(caster);
        if (instance == null)
        {
            return;
        }

        ServerLevel level = caster.server.getLevel(instance.getDimension());
        if (level == null)
        {
            return;
        }

        LivingEntity newTarget = resolvePowerSlashTarget(level, caster.getUUID(), instance, targetUuid);
        if (newTarget != null)
        {
            retargetActivePowerSlash(caster, level, state, newTarget);
        }
    }

    @Override
    public void tickMaintain(ServerPlayer player)
    {
        tickCastingDelay(player);
        tickShrineGhostFaceFx(player);
        tickShrineDomainStartFx(player);
        tickShrineSkyboxTextureSwap(player);
        tickShrineDomainDamageStart(player);
        tickShrinePreCombatLock(player);
        clearSlashFxForTargetsOutsideDomain(player);
        captureNewDomainAttackTargets(player);
        tickShrineSlashDamage(player);
        tickShrineDomainScreenShake(player);
        tickShrinePowerSlash(player);
        tickShrineSlashLoop(player);
        super.tickMaintain(player);
    }

    @Override
    protected void onBarrierClosed(ServerPlayer player, BarrierInstance instance)
    {
        stopShrineCastFilter(player);
        clearShrineSlashFxForCaster(player);
        stopShrineDomainScreenShakeForCaster(player);
        stopShrineSlashLoopForCaster(player);
        endPowerSlashForCaster(player, player.serverLevel());
        clearPowerSlashStateForCaster(player);
        ModNetwork.sendShrineDomainActiveSync(player, false);
        ModNetwork.sendShrinePowerSlashChargeSync(player, false, 0.0F);
        removeShrineForCaster(player);
        removeShrineGhostFaceForCaster(player);
        removeShrineDomainStartFxFromPlayer(player);
        endPreCombatLock(player);
        shrineSkyboxSwapAtServerTick.remove(player.getUUID());
        shrineDomainDamageAtServerTick.remove(player.getUUID());
        shrineGhostFaceSpawnAtServerTick.remove(player.getUUID());
        shrineGhostFaceRemoveAtServerTick.remove(player.getUUID());
        shrineDomainDamageStartedByCaster.remove(player.getUUID());
        shrineSlashDamageTicksByCaster.remove(player.getUUID());
        lockOnReportByCaster.remove(player.getUUID());
        super.onBarrierClosed(player, instance);
    }

    private void spawnShrineNorthOfDomainCenter(ServerPlayer player, BarrierInstance instance)
    {
        ServerLevel level = player.serverLevel();
        removeShrineForCaster(player);

        DemonSubduingShrineEntity shrine = ModEntities.DEMON_SUBDUING_SHRINE.get().create(level);
        if (shrine == null)
        {
            return;
        }

        Vec3 spawnPos = computeSpawnPositionNorthOfDomainCenter(player, instance);
        shrine.moveTo(spawnPos.x, spawnPos.y, spawnPos.z, SHRINE_SPAWN_YAW, 0.0F);
        if (!level.addFreshEntity(shrine))
        {
            return;
        }

        shrineEntityByCaster.put(player.getUUID(), shrine.getUUID());
        teleportCasterInFrontOfShrine(player, spawnPos);
        spawnShrineDomainFxAtShrine(level, shrine, player);
    }

    private static void teleportCasterInFrontOfShrine(ServerPlayer player, Vec3 shrinePos)
    {
        Vec3 teleportPos = shrinePos.add(
                horizontalFacingFromYaw(SHRINE_SPAWN_YAW).scale(CASTER_SPAWN_IN_FRONT_OF_SHRINE_DISTANCE));
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0.0F;
        player.connection.teleport(teleportPos.x, teleportPos.y, teleportPos.z, SHRINE_SPAWN_YAW, player.getXRot());
    }

    private void attachShrineDomainStartFxToPlayer(ServerPlayer player)
    {
        performPhotonCommand(player, SHRINE_DOMAIN_START_FX_COMMAND);
        shrineDomainStartFxTicksByCaster.put(player.getUUID(), SHRINE_DOMAIN_START_FX_DURATION_TICKS);
        applyShrineDomainStartBlindness(player, SHRINE_DOMAIN_START_FX_DURATION_TICKS);
    }

    private void tickShrineDomainStartFx(ServerPlayer player)
    {
        if (!shrineDomainStartFxTicksByCaster.containsKey(player.getUUID()))
        {
            return;
        }
        applyShrineDomainStartBlindness(player, SHRINE_DOMAIN_START_FX_DURATION_TICKS);
    }

    private void removeShrineDomainStartFxFromPlayer(ServerPlayer player)
    {
        shrineDomainStartFxTicksByCaster.remove(player.getUUID());
        player.removeEffect(MobEffects.BLINDNESS);
        performPhotonCommand(player, SHRINE_DOMAIN_START_FX_REMOVE_COMMAND);
    }

    private static void applyShrineDomainStartBlindness(ServerPlayer player, int durationTicks)
    {
        player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, durationTicks, 0, false, false, false));
    }

    private void scheduleShrineSkyboxTextureSwap(ServerPlayer caster)
    {
        MinecraftServer server = caster.getServer();
        if (server == null)
        {
            return;
        }
        shrineSkyboxSwapAtServerTick.put(caster.getUUID(), server.getTickCount() + SHRINE_SKYBOX_SWAP_DELAY_TICKS);
    }

    private void tickShrineSkyboxTextureSwap(ServerPlayer owner)
    {
        UUID ownerUuid = owner.getUUID();
        Integer swapAt = shrineSkyboxSwapAtServerTick.get(ownerUuid);
        if (swapAt == null)
        {
            return;
        }

        MinecraftServer server = owner.getServer();
        if (server == null || server.getTickCount() < swapAt)
        {
            return;
        }

        Deque<BarrierInstance> queue = ACTIVE_DOMAINS.get(ownerUuid);
        if (queue == null || queue.isEmpty())
        {
            shrineSkyboxSwapAtServerTick.remove(ownerUuid);
            return;
        }

        BarrierInstance instance = queue.peekLast();
        if (instance == null || instance.isExpanding())
        {
            return;
        }

        ServerLevel level = owner.serverLevel();
        if (!instance.getDimension().equals(level.dimension()))
        {
            return;
        }

        applyShrineSkyboxTextureToInstance(level, instance, SHRINE_DOMAIN_SKYBOX_TEXTURE);
        shrineSkyboxSwapAtServerTick.remove(ownerUuid);
    }

    private void scheduleShrineDomainDamageStart(ServerPlayer caster)
    {
        MinecraftServer server = caster.getServer();
        if (server == null)
        {
            return;
        }
        shrineDomainDamageAtServerTick.put(caster.getUUID(), server.getTickCount() + SHRINE_PRE_COMBAT_DURATION_TICKS);
    }

    private void tickShrineDomainDamageStart(ServerPlayer owner)
    {
        UUID ownerUuid = owner.getUUID();
        Integer damageAt = shrineDomainDamageAtServerTick.get(ownerUuid);
        if (damageAt == null || hasShrineDomainDamageStarted(owner))
        {
            return;
        }

        MinecraftServer server = owner.getServer();
        if (server == null || server.getTickCount() < damageAt)
        {
            return;
        }

        Deque<BarrierInstance> queue = ACTIVE_DOMAINS.get(ownerUuid);
        if (queue == null || queue.isEmpty())
        {
            shrineDomainDamageAtServerTick.remove(ownerUuid);
            return;
        }

        BarrierInstance instance = queue.peekLast();
        if (instance == null || instance.isExpanding())
        {
            return;
        }

        ServerLevel level = owner.serverLevel();
        if (!instance.getDimension().equals(level.dimension()))
        {
            return;
        }

        beginShrineDomainDamage(owner, instance);
        shrineDomainDamageAtServerTick.remove(ownerUuid);
    }

    private void beginShrineDomainDamage(ServerPlayer owner, BarrierInstance instance)
    {
        endPreCombatLock(owner);
        shrineDomainDamageStartedByCaster.add(owner.getUUID());
        markSlashOnDomainEntities(owner, instance);
    }

    private int getPreCombatLockRemainingTicks(UUID ownerUuid, MinecraftServer server)
    {
        Integer damageAt = shrineDomainDamageAtServerTick.get(ownerUuid);
        if (damageAt == null)
        {
            return SHRINE_PRE_COMBAT_DURATION_TICKS;
        }
        return Math.max(1, damageAt - server.getTickCount());
    }

    private void beginPreCombatLock(ServerPlayer caster, BarrierInstance instance)
    {
        capturePreCombatPlayersInDomain(caster, instance);
        capturePreCombatMobsInDomain(caster, instance);
    }

    private void tickShrinePreCombatLock(ServerPlayer owner)
    {
        if (hasShrineDomainDamageStarted(owner))
        {
            return;
        }

        BarrierInstance instance = getActiveBarrierInstance(owner);
        if (instance == null || instance.isExpanding())
        {
            return;
        }

        capturePreCombatPlayersInDomain(owner, instance);
        tickPreCombatPlayerLocksForOwner(owner);
        capturePreCombatMobsInDomain(owner, instance);
    }

    private void capturePreCombatPlayersInDomain(ServerPlayer owner, BarrierInstance instance)
    {
        ServerLevel level = owner.serverLevel();
        if (!instance.getDimension().equals(level.dimension()))
        {
            return;
        }

        BlockPos center = instance.getCenter();
        int radius = instance.getRadius();
        double radiusSqr = (double) radius * radius;
        UUID ownerUuid = owner.getUUID();

        for (ServerPlayer player : findPlayersInsideDomain(level, center, radius, radiusSqr))
        {
            UUID playerUuid = player.getUUID();
            if (preCombatPlayerLockByPlayer.containsKey(playerUuid))
            {
                continue;
            }

            MinecraftServer server = owner.getServer();
            int lockTicks = server == null
                    ? SHRINE_PRE_COMBAT_DURATION_TICKS
                    : getPreCombatLockRemainingTicks(ownerUuid, server);
            preCombatPlayerLockByPlayer.put(playerUuid, new ShrinePreCombatPlayerLock(
                    ownerUuid,
                    player.position(),
                    player.getYRot(),
                    player.getXRot(),
                    lockTicks));
            ModNetwork.sendPlayerInputLockSync(player, true);
        }
    }

    private void tickPreCombatPlayerLocksForOwner(ServerPlayer owner)
    {
        UUID ownerUuid = owner.getUUID();
        List<UUID> lockedPlayerUuids = new ArrayList<>();
        for (Map.Entry<UUID, ShrinePreCombatPlayerLock> entry : preCombatPlayerLockByPlayer.entrySet())
        {
            if (entry.getValue().ownerUuid().equals(ownerUuid))
            {
                lockedPlayerUuids.add(entry.getKey());
            }
        }

        MinecraftServer server = owner.getServer();
        if (server == null)
        {
            return;
        }

        for (UUID playerUuid : lockedPlayerUuids)
        {
            ServerPlayer player = server.getPlayerList().getPlayer(playerUuid);
            if (player != null)
            {
                tickPreCombatPlayerLock(player);
            }
        }
    }

    private void tickPreCombatPlayerLock(ServerPlayer player)
    {
        UUID uuid = player.getUUID();
        ShrinePreCombatPlayerLock lockState = preCombatPlayerLockByPlayer.get(uuid);
        if (lockState == null)
        {
            return;
        }

        if (!player.isAlive() || lockState.remainingTicks() <= 0)
        {
            preCombatPlayerLockByPlayer.remove(uuid);
            ModNetwork.sendPlayerInputLockSync(player, false);
            return;
        }

        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0.0F;
        player.hurtMarked = true;
        player.connection.teleport(
                lockState.lockPos().x, lockState.lockPos().y, lockState.lockPos().z,
                lockState.lockYaw(), lockState.lockPitch());
        preCombatPlayerLockByPlayer.put(uuid, lockState.decreaseOneTick());
    }

    private void capturePreCombatMobsInDomain(ServerPlayer owner, BarrierInstance instance)
    {
        ServerLevel level = owner.serverLevel();
        if (!instance.getDimension().equals(level.dimension()))
        {
            return;
        }

        BlockPos center = instance.getCenter();
        int radius = instance.getRadius();
        double radiusSqr = (double) radius * radius;
        UUID ownerUuid = owner.getUUID();

        for (Mob mob : findPreCombatPauseTargets(level, ownerUuid, center, radius, radiusSqr))
        {
            pausePreCombatMobAi(ownerUuid, mob);
        }
    }

    private static List<Mob> findPreCombatPauseTargets(
            ServerLevel level, UUID casterUuid, BlockPos center, int radius, double radiusSqr)
    {
        AABB queryBox = new AABB(
                center.getX() - radius, center.getY() - radius, center.getZ() - radius,
                center.getX() + radius + 1.0D, center.getY() + radius + 1.0D, center.getZ() + radius + 1.0D);
        return level.getEntitiesOfClass(Mob.class, queryBox, entity ->
        {
            if (!entity.isAlive() || entity.getUUID().equals(casterUuid))
            {
                return false;
            }
            if (entity instanceof SpecialEffectEntity)
            {
                return false;
            }
            return isInsideDomainSphere(entity.position(), center, radiusSqr);
        });
    }

    private void pausePreCombatMobAi(UUID ownerUuid, Mob mob)
    {
        UUID entityUuid = mob.getUUID();
        if (preCombatMobPauseByEntity.containsKey(entityUuid))
        {
            return;
        }

        boolean originNoAi = mob.isNoAi();
        boolean aiPausedByThisLock = false;
        if (!originNoAi)
        {
            mob.setNoAi(true);
            aiPausedByThisLock = true;
        }
        preCombatMobPauseByEntity.put(entityUuid, new ShrinePreCombatMobPause(ownerUuid, aiPausedByThisLock, originNoAi));
    }

    private void endPreCombatLock(ServerPlayer caster)
    {
        UUID casterUuid = caster.getUUID();
        MinecraftServer server = caster.getServer();
        Iterator<Map.Entry<UUID, ShrinePreCombatPlayerLock>> playerLockIterator = preCombatPlayerLockByPlayer.entrySet().iterator();
        while (playerLockIterator.hasNext())
        {
            Map.Entry<UUID, ShrinePreCombatPlayerLock> entry = playerLockIterator.next();
            if (!entry.getValue().ownerUuid().equals(casterUuid))
            {
                continue;
            }

            if (server != null)
            {
                ServerPlayer lockedPlayer = server.getPlayerList().getPlayer(entry.getKey());
                if (lockedPlayer != null)
                {
                    ModNetwork.sendPlayerInputLockSync(lockedPlayer, false);
                }
            }
            playerLockIterator.remove();
        }

        ServerLevel level = caster.serverLevel();
        Iterator<Map.Entry<UUID, ShrinePreCombatMobPause>> iterator = preCombatMobPauseByEntity.entrySet().iterator();
        while (iterator.hasNext())
        {
            Map.Entry<UUID, ShrinePreCombatMobPause> entry = iterator.next();
            if (!entry.getValue().ownerUuid().equals(casterUuid))
            {
                continue;
            }

            Entity entity = level.getEntity(entry.getKey());
            if (entity instanceof Mob mob && entry.getValue().aiPausedByThisLock())
            {
                mob.setNoAi(entry.getValue().originNoAi());
            }
            iterator.remove();
        }
    }

    private boolean hasShrineDomainDamageStarted(ServerPlayer owner)
    {
        return shrineDomainDamageStartedByCaster.contains(owner.getUUID());
    }

    private static void applyShrineSkyboxTextureToInstance(ServerLevel level, BarrierInstance instance, ResourceLocation texture)
    {
        for (BlockPos pos : instance.getSkyboxBlocks())
        {
            if (level.getBlockEntity(pos) instanceof JJKZSkyblockEntity skyblockEntity)
            {
                skyblockEntity.setTextureFolder(texture);
            }
        }
        instance.setCustomSkyboxTexture(texture);
    }

    private static void performPhotonCommand(ServerPlayer player, String command)
    {
        ServerLevel level = player.serverLevel();
        if (level.getServer() == null)
        {
            return;
        }
        CommandSourceStack commandSource = player.createCommandSourceStack()
                .withPermission(2)
                .withSuppressedOutput();
        level.getServer().getCommands().performPrefixedCommand(commandSource, command);
    }

    private static void performPhotonCommand(Entity entity, String command)
    {
        if (!(entity.level() instanceof ServerLevel level) || level.getServer() == null)
        {
            return;
        }
        CommandSourceStack commandSource = entity.createCommandSourceStack()
                .withPermission(2)
                .withSuppressedOutput();
        level.getServer().getCommands().performPrefixedCommand(commandSource, command);
    }

    private void scheduleShrineGhostFaceFx(ServerPlayer caster)
    {
        MinecraftServer server = caster.getServer();
        if (server == null)
        {
            return;
        }
        shrineGhostFaceSpawnAtServerTick.put(caster.getUUID(), server.getTickCount() + SHRINE_GHOST_FACE_SPAWN_DELAY_TICKS);
    }

    private void tickShrineGhostFaceFx(ServerPlayer owner)
    {
        UUID ownerUuid = owner.getUUID();
        MinecraftServer server = owner.getServer();
        if (server == null)
        {
            return;
        }

        Integer spawnAt = shrineGhostFaceSpawnAtServerTick.get(ownerUuid);
        if (spawnAt != null && server.getTickCount() >= spawnAt)
        {
            shrineGhostFaceSpawnAtServerTick.remove(ownerUuid);
            if (owner.isAlive())
            {
                spawnShrineGhostFaceEntity(owner, server);
            }
        }

        Integer removeAt = shrineGhostFaceRemoveAtServerTick.get(ownerUuid);
        if (removeAt != null && server.getTickCount() >= removeAt)
        {
            shrineGhostFaceRemoveAtServerTick.remove(ownerUuid);
            removeShrineGhostFaceForCaster(owner);
        }
    }

    private void spawnShrineGhostFaceEntity(ServerPlayer player, MinecraftServer server)
    {
        removeShrineGhostFaceForCaster(player);

        ServerLevel level = player.serverLevel();
        SpecialEffectEntity fxEntity = ModEntities.SPECIAL_EFFECT_ENTITY.get().create(level);
        if (fxEntity == null)
        {
            return;
        }

        Vec3 forward = horizontalFacingFromYaw(player.getYRot()).scale(SHRINE_GHOST_FACE_FORWARD_DISTANCE);
        Vec3 spawnPos = player.position().add(forward);
        fxEntity.moveTo(spawnPos.x, spawnPos.y, spawnPos.z, player.getYRot(), 0.0F);
        if (!level.addFreshEntity(fxEntity))
        {
            return;
        }

        performPhotonCommand(fxEntity, SHRINE_GHOST_FACE_FX_COMMAND);
        ModNetwork.sendScreenShake(player, ScreenShakeKind.SMOOTH_WOBBLE, GHOST_FACE_WOBBLE_DURATION_TICKS, GHOST_FACE_WOBBLE_MAX_AMPLITUDE);
        removeShrineDomainStartFxFromPlayer(player);
        UUID casterUuid = player.getUUID();
        shrineGhostFaceEntityByCaster.put(casterUuid, fxEntity.getUUID());
        shrineGhostFaceRemoveAtServerTick.put(casterUuid, server.getTickCount() + SHRINE_GHOST_FACE_DURATION_TICKS);
    }

    private void removeShrineGhostFaceForCaster(ServerPlayer player)
    {
        UUID casterUuid = player.getUUID();
        shrineGhostFaceSpawnAtServerTick.remove(casterUuid);
        shrineGhostFaceRemoveAtServerTick.remove(casterUuid);

        UUID fxUuid = shrineGhostFaceEntityByCaster.remove(casterUuid);
        if (fxUuid == null)
        {
            return;
        }

        Entity fxEntity = player.serverLevel().getEntity(fxUuid);
        if (fxEntity != null)
        {
            performPhotonCommand(fxEntity, SHRINE_GHOST_FACE_FX_REMOVE_COMMAND);
            fxEntity.discard();
        }
    }

    private void spawnShrineDomainFxAtShrine(ServerLevel level, DemonSubduingShrineEntity shrine, ServerPlayer player)
    {
        SpecialEffectEntity fxEntity = ModEntities.SPECIAL_EFFECT_ENTITY.get().create(level);
        if (fxEntity == null)
        {
            return;
        }

        Vec3 shrinePos = shrine.position();
        fxEntity.moveTo(shrinePos.x, shrinePos.y, shrinePos.z, shrine.getYRot(), 0.0F);
        if (!level.addFreshEntity(fxEntity))
        {
            return;
        }

        CommandSourceStack commandSource = fxEntity.createCommandSourceStack()
                .withPermission(2)
                .withSuppressedOutput();
        level.getServer().getCommands().performPrefixedCommand(commandSource, SHRINE_DOMAIN_FX_COMMAND);
        shrineFxEntityByCaster.put(player.getUUID(), fxEntity.getUUID());
    }

    /** 领域完成瞬间：对球内除施法者外的生物挂上 shrine_slash。 */
    private void markSlashOnDomainEntities(ServerPlayer caster, BarrierInstance instance)
    {
        if (instance.isExpanding())
        {
            return;
        }

        ServerLevel level = caster.serverLevel();
        if (!instance.getDimension().equals(level.dimension()))
        {
            return;
        }

        Set<UUID> marked = shrineSlashTargetsByCaster.computeIfAbsent(caster.getUUID(), key -> ConcurrentHashMap.newKeySet());
        UUID casterUuid = caster.getUUID();
        BlockPos center = instance.getCenter();
        int radius = instance.getRadius();
        double radiusSqr = (double) radius * radius;

        for (LivingEntity target : findDomainAttackTargets(level, casterUuid, center, radius, radiusSqr))
        {
            tryApplyShrineSlashFx(caster, marked, level, target);
        }
    }

    /** 领域存续期间：补挂新进入球内的攻击目标。 */
    private void captureNewDomainAttackTargets(ServerPlayer owner)
    {
        if (!hasShrineDomainDamageStarted(owner))
        {
            return;
        }

        Deque<BarrierInstance> queue = ACTIVE_DOMAINS.get(owner.getUUID());
        if (queue == null || queue.isEmpty())
        {
            return;
        }

        BarrierInstance instance = queue.peekLast();
        if (instance == null || instance.isExpanding())
        {
            return;
        }

        ServerLevel level = owner.serverLevel();
        if (!instance.getDimension().equals(level.dimension()))
        {
            return;
        }

        Set<UUID> marked = shrineSlashTargetsByCaster.computeIfAbsent(owner.getUUID(), key -> ConcurrentHashMap.newKeySet());
        UUID casterUuid = owner.getUUID();
        BlockPos center = instance.getCenter();
        int radius = instance.getRadius();
        double radiusSqr = (double) radius * radius;

        for (LivingEntity target : findDomainAttackTargets(level, casterUuid, center, radius, radiusSqr))
        {
            tryApplyShrineSlashFx(owner, marked, level, target);
        }
    }

    /** 领域存续期间：已标记目标离开球体时移除 shrine_slash 并取消标记。 */
    private void clearSlashFxForTargetsOutsideDomain(ServerPlayer owner)
    {
        Deque<BarrierInstance> queue = ACTIVE_DOMAINS.get(owner.getUUID());
        if (queue == null || queue.isEmpty())
        {
            return;
        }

        BarrierInstance instance = queue.peekLast();
        if (instance == null || instance.isExpanding())
        {
            return;
        }

        Set<UUID> marked = shrineSlashTargetsByCaster.get(owner.getUUID());
        if (marked == null || marked.isEmpty())
        {
            return;
        }

        ServerLevel level = owner.serverLevel();
        if (!instance.getDimension().equals(level.dimension()))
        {
            return;
        }

        BlockPos center = instance.getCenter();
        int radius = instance.getRadius();
        double radiusSqr = (double) radius * radius;

        var iterator = marked.iterator();
        while (iterator.hasNext())
        {
            UUID targetUuid = iterator.next();
            Entity entity = level.getEntity(targetUuid);
            if (!(entity instanceof LivingEntity living) || !living.isAlive())
            {
                iterator.remove();
                continue;
            }

            if (!isInsideDomainSphere(living.position(), center, radiusSqr))
            {
                removeShrineSlashFx(owner, living);
                iterator.remove();
            }
        }
    }

    /** 领域存续期间：每 10 刻对球内攻击目标造成 1.5 颗心伤害。 */
    private void tickShrineSlashDamage(ServerPlayer owner)
    {
        if (!hasShrineDomainDamageStarted(owner))
        {
            return;
        }

        Deque<BarrierInstance> queue = ACTIVE_DOMAINS.get(owner.getUUID());
        if (queue == null || queue.isEmpty())
        {
            return;
        }

        BarrierInstance instance = queue.peekLast();
        if (instance == null || instance.isExpanding())
        {
            return;
        }

        int ticks = shrineSlashDamageTicksByCaster.compute(owner.getUUID(), (key, value) -> value == null ? 1 : value + 1);
        if (ticks < SHRINE_SLASH_DAMAGE_INTERVAL_TICKS)
        {
            return;
        }
        shrineSlashDamageTicksByCaster.put(owner.getUUID(), 0);

        ServerLevel level = owner.serverLevel();
        if (!instance.getDimension().equals(level.dimension()))
        {
            return;
        }

        UUID casterUuid = owner.getUUID();
        BlockPos center = instance.getCenter();
        int radius = instance.getRadius();
        double radiusSqr = (double) radius * radius;

        boolean dealtDamage = false;
        for (LivingEntity target : findDomainAttackTargets(level, casterUuid, center, radius, radiusSqr))
        {
            if (dealShrineSlashDamage(owner, level, target))
            {
                dealtDamage = true;
            }
        }

        if (dealtDamage)
        {
            ensureShrineSlashLoop(owner, level, center, radius, radiusSqr);
        }
        else if (isShrineSlashLoopActive(owner) && !isShrineDomainStillDamaging(owner, level, instance, casterUuid))
        {
            stopShrineSlashLoopForCaster(owner);
        }
    }

    private void ensureShrineSlashLoop(ServerPlayer owner, ServerLevel level, BlockPos center, int radius, double radiusSqr)
    {
        if (isShrineSlashLoopActive(owner))
        {
            return;
        }

        startShrineSlashLoop(owner, level, center, radius, radiusSqr);
    }

    /** 强力斩击仍在对有效目标造成伤害时，不因普通斩击空窗而停止循环音。 */
    private boolean isShrineDomainStillDamaging(ServerPlayer owner, ServerLevel level, BarrierInstance instance, UUID casterUuid)
    {
        if (!isPowerSlashActive(casterUuid))
        {
            return false;
        }

        PowerSlashState state = powerSlashStateByCaster.get(casterUuid);
        if (state == null || state.activeTargetUuid == null)
        {
            return false;
        }

        return resolvePowerSlashTarget(level, casterUuid, instance, state.activeTargetUuid) != null;
    }

    private void tickShrineSlashLoop(ServerPlayer owner)
    {
        if (!isShrineSlashLoopActive(owner))
        {
            return;
        }

        BarrierInstance instance = getActiveBarrierInstance(owner);
        if (instance == null)
        {
            stopShrineSlashLoopForCaster(owner);
            return;
        }

        ServerLevel level = owner.server.getLevel(instance.getDimension());
        if (level == null)
        {
            stopShrineSlashLoopForCaster(owner);
            return;
        }

        BlockPos center = instance.getCenter();
        int radius = instance.getRadius();
        double radiusSqr = (double) radius * radius;
        syncShrineSlashLoopViewers(owner, level, center, radius, radiusSqr);
    }

    private boolean isShrineSlashLoopActive(ServerPlayer owner)
    {
        Set<UUID> viewers = shrineSlashLoopViewersByCaster.get(owner.getUUID());
        return viewers != null && !viewers.isEmpty();
    }

    private void startShrineSlashLoop(ServerPlayer owner, ServerLevel level, BlockPos center, int radius, double radiusSqr)
    {
        shrineSlashLoopViewersByCaster.computeIfAbsent(owner.getUUID(), key -> ConcurrentHashMap.newKeySet());
        syncShrineSlashLoopViewers(owner, level, center, radius, radiusSqr);
    }

    private void syncShrineSlashLoopViewers(ServerPlayer owner, ServerLevel level, BlockPos center, int radius, double radiusSqr)
    {
        UUID casterUuid = owner.getUUID();
        Set<UUID> viewers = shrineSlashLoopViewersByCaster.get(casterUuid);
        if (viewers == null)
        {
            return;
        }

        Set<UUID> insideUuids = new HashSet<>();
        for (ServerPlayer player : findPlayersInsideDomain(level, center, radius, radiusSqr))
        {
            insideUuids.add(player.getUUID());
            if (viewers.add(player.getUUID()))
            {
                ModNetwork.sendShrineSlashLoopSoundStart(player, casterUuid);
            }
        }

        for (UUID viewerUuid : new HashSet<>(viewers))
        {
            if (insideUuids.contains(viewerUuid))
            {
                continue;
            }

            ServerPlayer player = level.getServer().getPlayerList().getPlayer(viewerUuid);
            if (player != null)
            {
                ModNetwork.sendShrineSlashLoopSoundStop(player, casterUuid);
            }
            viewers.remove(viewerUuid);
        }
    }

    private void stopShrineSlashLoopForCaster(ServerPlayer caster)
    {
        UUID casterUuid = caster.getUUID();
        Set<UUID> viewers = shrineSlashLoopViewersByCaster.remove(casterUuid);
        if (viewers == null || viewers.isEmpty())
        {
            return;
        }

        for (UUID viewerUuid : viewers)
        {
            ServerPlayer player = caster.serverLevel().getServer().getPlayerList().getPlayer(viewerUuid);
            if (player != null)
            {
                ModNetwork.sendShrineSlashLoopSoundStop(player, casterUuid);
            }
        }
    }

    private boolean dealShrineSlashDamage(ServerPlayer owner, ServerLevel level, LivingEntity target)
    {
        if (!target.isAlive())
        {
            return false;
        }

        if (!target.hurt(owner.damageSources().magic(), ShrineDomainSkillGrowth.slashDamage(owner)))
        {
            return false;
        }

        if (!target.isAlive())
        {
            playShrineDomainSoundAtEntity(level, target, ModSounds.SLASH_KILL.get());
        }
        return true;
    }

    private boolean dealPowerSlashDamage(ServerPlayer owner, ServerLevel level, LivingEntity target)
    {
        if (!target.isAlive())
        {
            return false;
        }

        if (!target.hurt(owner.damageSources().magic(), ShrineDomainSkillGrowth.powerSlashDamage(owner)))
        {
            return false;
        }

        if (!target.isAlive())
        {
            playShrineDomainSoundAtEntity(level, target, ModSounds.SLASH_KILL.get());
        }
        return true;
    }

    private static void playShrineDomainSoundAtEntity(ServerLevel level, LivingEntity entity, SoundEvent sound)
    {
        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), sound, SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    private static void playShrineDomainSoundForPlayer(ServerPlayer player, SoundEvent sound)
    {
        player.playNotifySound(sound, SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    /**
     * 领域内有攻击目标时，对领域内所有玩家（含施法者）同步持续屏幕振动；
     * 无目标或玩家离开领域后停止。
     */
    private void tickShrineDomainScreenShake(ServerPlayer owner)
    {
        if (!hasShrineDomainDamageStarted(owner))
        {
            stopShrineDomainScreenShakeForCaster(owner);
            return;
        }

        Deque<BarrierInstance> queue = ACTIVE_DOMAINS.get(owner.getUUID());
        if (queue == null || queue.isEmpty())
        {
            return;
        }

        BarrierInstance instance = queue.peekLast();
        if (instance == null || instance.isExpanding())
        {
            return;
        }

        ServerLevel level = owner.serverLevel();
        if (!instance.getDimension().equals(level.dimension()))
        {
            return;
        }

        BlockPos center = instance.getCenter();
        int radius = instance.getRadius();
        double radiusSqr = (double) radius * radius;
        UUID casterUuid = owner.getUUID();
        boolean shouldShake = !findDomainAttackTargets(level, casterUuid, center, radius, radiusSqr).isEmpty();
        float shakeAmplitude = isPowerSlashActive(casterUuid)
                ? POWER_SLASH_SCREEN_SHAKE_MAX_DEG
                : SHRINE_ATTACK_SCREEN_SHAKE_MAX_DEG;
        List<ServerPlayer> insidePlayers = findPlayersInsideDomain(level, center, radius, radiusSqr);
        Set<UUID> viewers = shrineScreenShakeViewersByCaster.computeIfAbsent(casterUuid, key -> ConcurrentHashMap.newKeySet());
        Set<UUID> insideUuids = new HashSet<>();

        for (ServerPlayer player : insidePlayers)
        {
            insideUuids.add(player.getUUID());
            if (shouldShake)
            {
                ModNetwork.sendScreenShake(player, ScreenShakeKind.SUSTAINED, 0, shakeAmplitude, true);
            }
            else
            {
                ModNetwork.sendScreenShake(player, ScreenShakeKind.SUSTAINED, 0, 0.0F, false);
            }
        }

        for (UUID viewerUuid : new HashSet<>(viewers))
        {
            if (insideUuids.contains(viewerUuid))
            {
                continue;
            }

            ServerPlayer player = level.getServer().getPlayerList().getPlayer(viewerUuid);
            if (player != null)
            {
                ModNetwork.sendScreenShake(player, ScreenShakeKind.SUSTAINED, 0, 0.0F, false);
            }
            viewers.remove(viewerUuid);
        }

        viewers.clear();
        if (shouldShake)
        {
            viewers.addAll(insideUuids);
        }
    }

    private void stopShrineDomainScreenShakeForCaster(ServerPlayer caster)
    {
        Set<UUID> viewers = shrineScreenShakeViewersByCaster.remove(caster.getUUID());
        if (viewers == null || viewers.isEmpty())
        {
            return;
        }

        for (UUID viewerUuid : viewers)
        {
            ServerPlayer player = caster.serverLevel().getServer().getPlayerList().getPlayer(viewerUuid);
            if (player != null)
            {
                ModNetwork.sendScreenShake(player, ScreenShakeKind.SUSTAINED, 0, 0.0F, false);
            }
        }
    }

    private void stopShrineCastFilter(ServerPlayer player)
    {
        UUID uuid = player.getUUID();
        if (shrineCastFilterActiveByCaster.remove(uuid) != null)
        {
            ModNetwork.sendShrineCastFilterSync(player, false);
        }
    }

    private void maybeAnnouncePerfectSuppression(ServerPlayer caster, BarrierInstance instance)
    {
        BlockPos center = instance.getCenter();
        int radius = instance.getRadius();
        double radiusSqr = (double) radius * radius;
        ServerLevel level = caster.serverLevel();
        int targetCount = findDomainAttackTargets(level, caster.getUUID(), center, radius, radiusSqr).size();
        if (targetCount <= PERFECT_SUPPRESSION_TARGET_THRESHOLD)
        {
            return;
        }
        double cx = center.getX() + 0.5D;
        double cy = center.getY() + 0.5D;
        double cz = center.getZ() + 0.5D;
        level.playSound(null, cx, cy, cz, ModSounds.SHRINE_DOMAIN_PERFECT.get(), SoundSource.AMBIENT, 1.0F, 1.0F);
        level.playSound(null, caster.getX(), caster.getY(), caster.getZ(), ModSounds.SOUND_EFFECTS_IRON_SOUND.get(), SoundSource.PLAYERS, PERFECT_SUPPRESSION_IRON_SOUND_VOLUME, 1.0F);
        caster.displayClientMessage(Component.literal("完美压制").withStyle(ChatFormatting.GOLD), false);
        grantAdvancement(caster, "perfect_suppression");
    }

    private static void grantAdvancement(ServerPlayer player, String name)
    {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(JuJutsuKaisenZone.MODID, name);
        Advancement advancement = player.server.getAdvancements().getAdvancement(id);
        if (advancement == null)
        {
            return;
        }
        var progress = player.getAdvancements();
        for (String criterion : advancement.getCriteria().keySet())
        {
            if (!progress.getOrStartProgress(advancement).isDone())
            {
                progress.award(advancement, criterion);
            }
        }
    }

    private List<ServerPlayer> findPlayersInsideDomain(ServerLevel level, BlockPos center, int radius, double radiusSqr)
    {
        List<ServerPlayer> insidePlayers = new ArrayList<>();
        for (ServerPlayer player : level.players())
        {
            if (isInsideDomainSphere(player.position(), center, radiusSqr))
            {
                insidePlayers.add(player);
            }
        }
        return insidePlayers;
    }

    private List<LivingEntity> findDomainAttackTargets(ServerLevel level, UUID casterUuid, BlockPos center, int radius, double radiusSqr)
    {
        AABB queryBox = new AABB(
                center.getX() - radius, center.getY() - radius, center.getZ() - radius,
                center.getX() + radius + 1.0D, center.getY() + radius + 1.0D, center.getZ() + radius + 1.0D
        );
        return level.getEntitiesOfClass(LivingEntity.class, queryBox, entity -> isShrineDomainAttackTarget(casterUuid, entity, center, radiusSqr));
    }

    private static boolean isShrineDomainAttackTarget(UUID casterUuid, LivingEntity entity, BlockPos center, double radiusSqr)
    {
        if (!entity.isAlive())
        {
            return false;
        }
        if (entity.getUUID().equals(casterUuid))
        {
            return false;
        }
        if (entity instanceof SpecialEffectEntity)
        {
            return false;
        }
        return isInsideDomainSphere(entity.position(), center, radiusSqr);
    }

    private void tryApplyShrineSlashFx(ServerPlayer caster, Set<UUID> marked, ServerLevel level, LivingEntity target)
    {
        if (isPowerSlashTarget(caster.getUUID(), target.getUUID()))
        {
            return;
        }

        if (marked.contains(target.getUUID()))
        {
            return;
        }

        applyShrineSlashFx(caster, target);
        marked.add(target.getUUID());
    }

    private static void applyShrineSlashFx(ServerPlayer caster, LivingEntity target)
    {
        OwnerOnlyShrineSlashFx.mountDomainSlash(caster, target);
    }

    /** 领域关闭时移除已标记目标身上的 shrine_slash 特效。 */
    private void clearShrineSlashFxForCaster(ServerPlayer caster)
    {
        Set<UUID> marked = shrineSlashTargetsByCaster.remove(caster.getUUID());
        if (marked == null || marked.isEmpty())
        {
            return;
        }

        ServerLevel level = caster.serverLevel();
        for (UUID targetUuid : marked)
        {
            Entity entity = level.getEntity(targetUuid);
            if (entity instanceof LivingEntity living && living.isAlive())
            {
                removeShrineSlashFx(caster, living);
            }
        }
    }

    private static void removeShrineSlashFx(ServerPlayer caster, LivingEntity target)
    {
        OwnerOnlyShrineSlashFx.removeDomainSlash(caster, target);
    }

    private void tickShrinePowerSlash(ServerPlayer owner)
    {
        if (!hasShrineDomainDamageStarted(owner))
        {
            return;
        }

        BarrierInstance instance = getActiveBarrierInstance(owner);
        if (instance == null)
        {
            return;
        }

        ServerLevel level = owner.server.getLevel(instance.getDimension());
        if (level == null)
        {
            return;
        }

        UUID casterUuid = owner.getUUID();
        PowerSlashState state = powerSlashStateByCaster.computeIfAbsent(casterUuid, key -> new PowerSlashState());

        if (state.cooldownTicksRemaining > 0)
        {
            state.cooldownTicksRemaining--;
            syncPowerSlashChargeHud(owner, false, 0.0F);
            return;
        }

        if (state.activeTicksRemaining > 0)
        {
            tickActivePowerSlash(owner, level, instance, state);
            return;
        }

        LockOnReport report = lockOnReportByCaster.get(casterUuid);
        if (report == null || !report.locking || report.targetUuid == null)
        {
            resetPowerSlashLockCharge(owner, state);
            return;
        }

        LivingEntity target = resolvePowerSlashTarget(level, casterUuid, instance, report.targetUuid);
        if (target == null)
        {
            resetPowerSlashLockCharge(owner, state);
            return;
        }

        if (state.lockChargeTargetUuid == null || !state.lockChargeTargetUuid.equals(target.getUUID()))
        {
            state.lockChargeTicks = 0;
            state.lockChargeTargetUuid = target.getUUID();
        }

        state.lockChargeTicks++;
        syncPowerSlashChargeHud(owner, true, (float) state.lockChargeTicks / (float) POWER_SLASH_LOCK_CHARGE_TICKS);

        if (state.lockChargeTicks >= POWER_SLASH_LOCK_CHARGE_TICKS)
        {
            startPowerSlash(owner, level, state, target);
        }
    }

    private void tickActivePowerSlash(ServerPlayer owner, ServerLevel level, BarrierInstance instance, PowerSlashState state)
    {
        UUID casterUuid = owner.getUUID();
        LockOnReport report = lockOnReportByCaster.get(casterUuid);
        if (report != null && report.locking && report.targetUuid != null
                && !report.targetUuid.equals(state.activeTargetUuid))
        {
            LivingEntity switchedTarget = resolvePowerSlashTarget(level, casterUuid, instance, report.targetUuid);
            if (switchedTarget != null)
            {
                retargetActivePowerSlash(owner, level, state, switchedTarget);
            }
        }

        LivingEntity target = resolvePowerSlashTarget(level, casterUuid, instance, state.activeTargetUuid);
        if (target == null)
        {
            endPowerSlashForCaster(owner, level);
            state.cooldownTicksRemaining = POWER_SLASH_COOLDOWN_TICKS;
            return;
        }

        state.activeTicksRemaining--;
        state.damageIntervalTicks++;

        if (state.damageIntervalTicks >= POWER_SLASH_DAMAGE_INTERVAL_TICKS)
        {
            state.damageIntervalTicks = 0;
            if (!PlayerCursedEnergyData.tryConsume(owner, POWER_SLASH_COST_PER_HIT))
            {
                endPowerSlashForCaster(owner, level);
                state.cooldownTicksRemaining = POWER_SLASH_COOLDOWN_TICKS;
                return;
            }
            if (dealPowerSlashDamage(owner, level, target))
            {
                BlockPos center = instance.getCenter();
                int radius = instance.getRadius();
                double radiusSqr = (double) radius * radius;
                ensureShrineSlashLoop(owner, level, center, radius, radiusSqr);
            }
        }

        if (state.activeTicksRemaining <= 0)
        {
            endPowerSlashForCaster(owner, level);
            state.cooldownTicksRemaining = POWER_SLASH_COOLDOWN_TICKS;
        }
    }

    private void startPowerSlash(ServerPlayer owner, ServerLevel level, PowerSlashState state, LivingEntity target)
    {
        state.lockChargeTicks = 0;
        state.lockChargeTargetUuid = null;
        state.activeTicksRemaining = POWER_SLASH_DURATION_TICKS;
        state.activeTargetUuid = target.getUUID();
        state.damageIntervalTicks = 0;
        removeShrineSlashFx(owner, target);
        applyPowerSlashFx(owner, target);
        playShrineDomainSoundForPlayer(owner, ModSounds.SHRINE_SLASH_POWER.get());
        if (target instanceof ServerPlayer lockedPlayer)
        {
            playShrineDomainSoundForPlayer(lockedPlayer, ModSounds.SHRINE_SLASH_POWER.get());
        }
        syncPowerSlashChargeHud(owner, false, 0.0F);
    }

    /** 强力斩击进行中切换锁定目标：保留剩余持续时间，仅转移特效与伤害对象。 */
    private void retargetActivePowerSlash(ServerPlayer owner, ServerLevel level, PowerSlashState state, LivingEntity newTarget)
    {
        UUID newTargetUuid = newTarget.getUUID();
        if (newTargetUuid.equals(state.activeTargetUuid))
        {
            return;
        }

        UUID oldTargetUuid = state.activeTargetUuid;
        if (oldTargetUuid != null)
        {
            Entity oldEntity = level.getEntity(oldTargetUuid);
            if (oldEntity instanceof LivingEntity oldLiving)
            {
                removePowerSlashFx(owner, oldLiving);
                restoreNormalSlashFxForTargetIfMarked(owner, level, oldLiving);
            }
        }

        state.activeTargetUuid = newTargetUuid;
        removeShrineSlashFx(owner, newTarget);
        applyPowerSlashFx(owner, newTarget);
    }

    private void endPowerSlashForCaster(ServerPlayer owner, ServerLevel level)
    {
        PowerSlashState state = powerSlashStateByCaster.get(owner.getUUID());
        if (state == null || state.activeTargetUuid == null)
        {
            return;
        }

        Entity entity = level.getEntity(state.activeTargetUuid);
        if (entity instanceof LivingEntity living)
        {
            removePowerSlashFx(owner, living);
            restoreNormalSlashFxForTargetIfMarked(owner, level, living);
        }

        state.activeTicksRemaining = 0;
        state.activeTargetUuid = null;
        state.damageIntervalTicks = 0;
        syncPowerSlashChargeHud(owner, false, 0.0F);
    }

    private boolean isPowerSlashActive(UUID casterUuid)
    {
        PowerSlashState state = powerSlashStateByCaster.get(casterUuid);
        return state != null && state.activeTicksRemaining > 0;
    }

    private boolean isPowerSlashTarget(UUID casterUuid, UUID targetUuid)
    {
        PowerSlashState state = powerSlashStateByCaster.get(casterUuid);
        return state != null && state.activeTicksRemaining > 0 && targetUuid.equals(state.activeTargetUuid);
    }

    /** 强力斩击结束后：若目标仍在标记列表且位于领域内，恢复其普通 shrine_slash 特效。 */
    private void restoreNormalSlashFxForTargetIfMarked(ServerPlayer caster, ServerLevel level, LivingEntity target)
    {
        Set<UUID> marked = shrineSlashTargetsByCaster.get(caster.getUUID());
        if (marked == null || !marked.contains(target.getUUID()))
        {
            return;
        }

        BarrierInstance instance = getActiveBarrierInstance(caster);
        if (instance == null)
        {
            return;
        }

        BlockPos center = instance.getCenter();
        int radius = instance.getRadius();
        double radiusSqr = (double) radius * radius;
        if (isShrineDomainAttackTarget(caster.getUUID(), target, center, radiusSqr))
        {
            applyShrineSlashFx(caster, target);
        }
    }

    private void resetPowerSlashLockCharge(ServerPlayer owner, PowerSlashState state)
    {
        if (state.lockChargeTicks > 0 || state.lockChargeTargetUuid != null)
        {
            state.lockChargeTicks = 0;
            state.lockChargeTargetUuid = null;
            syncPowerSlashChargeHud(owner, false, 0.0F);
        }
    }

    private void clearPowerSlashStateForCaster(ServerPlayer caster)
    {
        powerSlashStateByCaster.remove(caster.getUUID());
    }

    private LivingEntity resolvePowerSlashTarget(ServerLevel level, UUID casterUuid, BarrierInstance instance, UUID targetUuid)
    {
        if (targetUuid == null)
        {
            return null;
        }

        Entity entity = level.getEntity(targetUuid);
        if (!(entity instanceof LivingEntity living))
        {
            return null;
        }

        BlockPos center = instance.getCenter();
        int radius = instance.getRadius();
        double radiusSqr = (double) radius * radius;
        if (!isShrineDomainAttackTarget(casterUuid, living, center, radiusSqr))
        {
            return null;
        }

        return living;
    }

    private BarrierInstance getActiveBarrierInstance(ServerPlayer owner)
    {
        Deque<BarrierInstance> queue = ACTIVE_DOMAINS.get(owner.getUUID());
        if (queue == null || queue.isEmpty())
        {
            return null;
        }

        BarrierInstance instance = queue.peekLast();
        if (instance == null || instance.isExpanding())
        {
            return null;
        }

        return instance;
    }

    private static void applyPowerSlashFx(ServerPlayer caster, LivingEntity target)
    {
        OwnerOnlyShrineSlashFx.mountDomainPowerSlash(caster, target);
    }

    private static void removePowerSlashFx(ServerPlayer caster, LivingEntity target)
    {
        OwnerOnlyShrineSlashFx.removeDomainPowerSlash(caster, target);
    }

    private static void syncPowerSlashChargeHud(ServerPlayer owner, boolean visible, float progress)
    {
        ModNetwork.sendShrinePowerSlashChargeSync(owner, visible, progress);
    }

    private static boolean isInsideDomainSphere(Vec3 entityPos, BlockPos center, double radiusSqr)
    {
        double dx = entityPos.x - (center.getX() + 0.5D);
        double dy = entityPos.y - (center.getY() + 0.5D);
        double dz = entityPos.z - (center.getZ() + 0.5D);
        return dx * dx + dy * dy + dz * dz < radiusSqr;
    }

    private static Vec3 computeSpawnPositionNorthOfDomainCenter(ServerPlayer player, BarrierInstance instance)
    {
        BlockPos center = instance.getCenter();
        return new Vec3(
                center.getX() + 0.5D,
                player.getY(),
                center.getZ() + 0.5D - SHRINE_SPAWN_NORTH_DISTANCE);
    }

    private void removeShrineForCaster(ServerPlayer player)
    {
        UUID shrineUuid = shrineEntityByCaster.remove(player.getUUID());
        if (shrineUuid != null)
        {
            Entity entity = player.serverLevel().getEntity(shrineUuid);
            if (entity != null)
            {
                entity.discard();
            }
        }

        UUID fxUuid = shrineFxEntityByCaster.remove(player.getUUID());
        if (fxUuid != null)
        {
            Entity fxEntity = player.serverLevel().getEntity(fxUuid);
            if (fxEntity != null)
            {
                fxEntity.discard();
            }
        }
    }

    /**
     * 施法者在自己「伏魔神龛」领域内单击空格时，传送到神龛碰撞箱顶部朝向对应方向的边缘。
     *
     * @param facingYaw 按下空格瞬间的朝向（水平）
     * @return 是否成功传送
     */
    public boolean tryTeleportCasterToShrineTop(ServerPlayer player, float facingYaw)
    {
        if (!hasLearnedTechnique(player) || !isCasterInsideOwnActiveDomain(player))
        {
            return false;
        }

        DemonSubduingShrineEntity shrine = getActiveShrineForCaster(player);
        if (shrine == null)
        {
            return false;
        }

        Vec3 teleportPos = computeShrineTopEdgePosition(shrine.getBoundingBox(), facingYaw);
        player.teleportTo(teleportPos.x, teleportPos.y, teleportPos.z);
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0.0F;
        return true;
    }

    /** 根据水平朝向，计算碰撞箱顶面矩形上对应方向的边缘点。 */
    private static Vec3 computeShrineTopEdgePosition(AABB box, float facingYaw)
    {
        double centerX = (box.minX + box.maxX) * 0.5D;
        double centerZ = (box.minZ + box.maxZ) * 0.5D;
        double halfX = (box.maxX - box.minX) * 0.5D;
        double halfZ = (box.maxZ - box.minZ) * 0.5D;

        Vec3 horizontalFacing = horizontalFacingFromYaw(facingYaw);
        double dirX = horizontalFacing.x;
        double dirZ = horizontalFacing.z;

        double scale = Double.POSITIVE_INFINITY;
        if (Math.abs(dirX) > 1.0E-6D)
        {
            scale = Math.min(scale, halfX / Math.abs(dirX));
        }
        if (Math.abs(dirZ) > 1.0E-6D)
        {
            scale = Math.min(scale, halfZ / Math.abs(dirZ));
        }

        if (!Double.isFinite(scale))
        {
            return new Vec3(centerX, box.maxY, centerZ);
        }

        return new Vec3(centerX + dirX * scale, box.maxY, centerZ + dirZ * scale);
    }

    private static Vec3 horizontalFacingFromYaw(float yRot)
    {
        double yawRad = Math.toRadians(yRot);
        return new Vec3(-Math.sin(yawRad), 0.0D, Math.cos(yawRad));
    }

    private boolean isCasterInsideOwnActiveDomain(ServerPlayer player)
    {
        Deque<BarrierInstance> queue = ACTIVE_DOMAINS.get(player.getUUID());
        if (queue == null || queue.isEmpty())
        {
            return false;
        }

        for (BarrierInstance instance : queue)
        {
            if (!instance.isExpanding() && isPlayerInsideBarrier(player, instance))
            {
                return true;
            }
        }
        return false;
    }

    private DemonSubduingShrineEntity getActiveShrineForCaster(ServerPlayer player)
    {
        UUID shrineUuid = shrineEntityByCaster.get(player.getUUID());
        if (shrineUuid == null)
        {
            return null;
        }

        Entity entity = player.serverLevel().getEntity(shrineUuid);
        if (entity instanceof DemonSubduingShrineEntity shrine && shrine.isAlive())
        {
            return shrine;
        }
        return null;
    }

    @Override
    protected boolean hasLearnedTechnique(ServerPlayer player)
    {
        return PlayerTechniqueData.isShrine(player);
    }

    @Override
    public String getTechniqueType()
    {
        return "shrine_domain";
    }

    @Override
    protected ResourceLocation getDomainSkyboxTexture()
    {
        return SHRINE_DOMAIN_INITIAL_SKYBOX_TEXTURE;
    }
}
