package com.darkness.jujutsukaisenzone.technique.domain;

import com.darkness.jujutsukaisenzone.api.screen.ScreenShakeKind;
import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.compat.EpicFightCompat;
import com.darkness.jujutsukaisenzone.effect.TechniqueMeltdownEffect;
import com.darkness.jujutsukaisenzone.blockentity.JJKZSkyblockEntity;
import com.darkness.jujutsukaisenzone.entity.SpecialEffectEntity;
import com.darkness.jujutsukaisenzone.network.ModNetwork;
import com.darkness.jujutsukaisenzone.player.PlayerBrainDamageData;
import com.darkness.jujutsukaisenzone.player.PlayerDomainModeData;
import com.darkness.jujutsukaisenzone.player.PlayerTechniqueData;
import com.darkness.jujutsukaisenzone.registry.ModEntities;
import com.darkness.jujutsukaisenzone.registry.ModSounds;
import com.darkness.jujutsukaisenzone.technique.scaling.LimitlessDomainSkillGrowth;
import com.darkness.jujutsukaisenzone.technique.se.LimitlessNearCasterExemptSeMechanic;
import com.darkness.jujutsukaisenzone.technique.se.SeMechanicRegistry;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.Advancement;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 无下限术式领域展开 - 无量空处。
 * <p>
 * 继承自 {@link AbstractTechniqueDomain}，实现无下限术式专属领域。
 * <p>
 * 特殊行为：
 * <ul>
 *   <li>按下领域展开键后等待 2 秒（40 tick）再展开领域结界</li>
 *   <li>按下领域展开键时在玩家位置播放领域展开音效</li>
 * </ul>
 */
public final class LimitlessDomainExpansion extends AbstractTechniqueDomain
{
    // ==================== 单例实例 ====================

    private static final LimitlessDomainExpansion INSTANCE = new LimitlessDomainExpansion();

    public static LimitlessDomainExpansion getInstance()
    {
        return INSTANCE;
    }

    // ==================== 配置参数 ====================

    /** 领域展开蓄力时间（tick），3 秒 = 60 tick */
    private static final int CAST_DELAY_TICKS = 60;
    /** 白色滤镜总时长（tick），6 秒 = 120 tick（旧滤镜参数，保留供参考） */
    private static final int WHITE_OVERLAY_TOTAL_TICKS = 120;
    /**
     * Shader 白幕总时长：铺开约 2.1s；全白保持额外 2.2s（+44 tick）；开洞相对最初提速共 2 倍（66→17 tick）。
     * 合计 115 tick。与 {@code domain_white_curtain.fsh} 中 spreadEnd≈0.365、holdEnd≈0.852 对齐。
     */
    private static final int WHITE_CURTAIN_TOTAL_TICKS = WHITE_OVERLAY_TOTAL_TICKS + 44 - 49;
    /** 白色滤镜淡入时长（tick） */
    private static final int WHITE_OVERLAY_FADE_IN_TICKS = 12;
    /** 白色滤镜淡出时长（tick） */
    private static final int WHITE_OVERLAY_FADE_OUT_TICKS = 18;
    /** 白色滤镜峰值透明度（100%） */
    private static final float WHITE_OVERLAY_PEAK_ALPHA = 1.0F;
    /** 白色滤镜对周围玩家的影响半径（格） */
    private static final double WHITE_OVERLAY_RADIUS = 24.0D;
    /** 结界完成瞬间屏幕振动：持续客户端刻数与最大偏转角（度），同步范围同 {@link #WHITE_OVERLAY_RADIUS}。 */
    private static final int DOMAIN_COMPLETE_SCREEN_SHAKE_TICKS = 18;
    private static final float DOMAIN_COMPLETE_SCREEN_SHAKE_MAX_DEG = 2.5F;
    /** 结界完成 {@link #LOCK_PLAYER_DURATION_TICKS} 刻后全屏闪光参数（见 {@link ModNetwork#sendFullScreenWorldFlashAnimationSync}）。 */
    private static final int POST_DOMAIN_COMPLETION_FLASH_TOTAL_TICKS = 22;
    private static final int POST_DOMAIN_COMPLETION_FLASH_FADE_IN_TICKS = 3;
    private static final int POST_DOMAIN_COMPLETION_FLASH_FADE_OUT_TICKS = 14;
    private static final float POST_DOMAIN_COMPLETION_FLASH_PEAK_ALPHA = 1.0F;
    /** 球心环境音开始包同步半径（格），宜与 {@code sounds.json} 中 {@code attenuation_distance} 同量级。 */
    private static final double LIMITLESS_DOMAIN_AMBIENT_SOUND_SYNC_RADIUS = 64.0D;

    /** 领域名称 */
    private static final String DOMAIN_NAME = "无量空处";
    /** 无量空处内壁 / 地板天空盒层使用的纹理（对应 assets/.../textures/environment/stat_sky1）。 */
    private static final ResourceLocation LIMITLESS_DOMAIN_SKYBOX_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(JuJutsuKaisenZone.MODID, "textures/environment/stat_sky1");
    /** 结界完成 {@link #LOCK_PLAYER_DURATION_TICKS} 刻后切换为 stat_sky2（与演出锁定时长一致）。 */
    private static final ResourceLocation LIMITLESS_DOMAIN_SKYBOX_TEXTURE_AFTER_280 =
            ResourceLocation.fromNamespaceAndPath(JuJutsuKaisenZone.MODID, "textures/environment/stat_sky2");
    /** 无量空处完成后特效实体相对球心在 south 方向的距离（格） */
    private static final double LIGHT_LINE_SOUTH_DISTANCE = 22.0D;
    /** 结界完成 {@link #LOCK_PLAYER_DURATION_TICKS} 刻后球心水墨 Photon（绑定实体失效时强制清除粒子）。 */
    private static final String PHOTON_INKWASH_COMMAND =
            "photon fx photon:limitless_inkwash entity @s 0 0 0 0 0 0 1 1 1 0 true false none";
    /** 完成演出时锁定施法者时长（tick） */
    private static final int LOCK_PLAYER_DURATION_TICKS = 280;
    /** Minecraft 世界坐标中 north 方向对应的固定 Yaw。 */
    private static final float NORTH_YAW = 180.0F;
    /** Minecraft 世界坐标中 south 方向对应的固定 Yaw。 */
    private static final float SOUTH_YAW = 0.0F;
    /** 传送锚点：球心 north 方向基础距离（格）。 */
    private static final double MOB_RELOCATE_NORTH_DISTANCE = 15.0D;
    /** 排除施法者附近生物的半径（格）。 */
    private static final double EXCLUDE_CASTER_RADIUS = 2.0D;
    /** 结界完成瞬间登记为领域锁定目标的生物数 {@literal >} 该值时触发「完美控制」反馈。 */
    private static final int PERFECT_CONTROL_LOCKED_TARGET_THRESHOLD = 5;
    /** 「完美控制」{@link ModSounds#SOUND_EFFECTS_IRON_SOUND} 相对默认播放音量（500%）。 */
    private static final float PERFECT_CONTROL_IRON_SOUND_VOLUME = 5.0F;
    /** 生物分散摆放的最小间距（格）。 */
    private static final double MOB_SCATTER_SPACING = 1.75D;
    /** 领域关闭后延续锁定的最长时间（tick），90 秒。 */
    private static final int POST_DOMAIN_LOCK_MAX_TICKS = 90 * 20;
    /** 纯符号型乱码字符池。 */
    private static final char[] GARBLED_SYMBOL_CHARS = (
            "⍅⌇⍉⏚⋉⍟⎋⍜⍥⌿⍁⍂⍛⎅⌖⍢⍩⎇⍨⌾⍴⍶⋓⍫⍣⌌⌭⍊⍎⌁⌆⍼"
    ).toCharArray();
    /** 混合乱码（字母数字符号）字符池。 */
    private static final char[] GARBLED_MIXED_CHARS = (
            "ÃÇ¤ºªÆØßµñþæ¿¡€£¥§©®™†‡°•·¶⁄¡‹›ﬁﬂ‰‡·¶•‹›ﬁﬂ‡°·0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    ).toCharArray();
    /** 纯中文乱码（编码错乱风）字符池。 */
    private static final char[] GARBLED_CN_CHARS = (
            "鎻愬彇澶辫触锛岃绋嬪簭寮傚父鍐呭瓨涓嶈冻锛堬紵锛夊嵎璧锋潵"
    ).toCharArray();
    /** 程序崩溃风格固定片段。 */
    private static final String[] GARBLED_CRASH_LINES = new String[]{
            "0x7F8A3B2C: 0xDEADBEEF 0xCAFEBABE 0x0D15EA5E 0x8BADF00D",
            "▒▒▒▒▒▒▒▒▒▒ █▓▒░ █▓▒░ █▓▒░ [NULL][SIGSEGV]"
    };
    /** 乱码类型数量。 */
    private static final int GARBLED_STYLE_COUNT = 4;

    // ==================== 蓄力状态管理 ====================

    /** 正在蓄力中的玩家 UUID → 剩余 tick 数 */
    private final ConcurrentMap<UUID, Integer> castingDelayMap = new ConcurrentHashMap<>();
    /** 完成演出后被锁定玩家 UUID → 锁定状态 */
    private final ConcurrentMap<UUID, PlayerLockState> playerLockMap = new ConcurrentHashMap<>();
    /** 领域内被锁定生物 UUID -> 锁定状态（直到该施法者领域关闭）。 */
    private final ConcurrentMap<UUID, MobLockState> mobLockMap = new ConcurrentHashMap<>();
    /**
     * 领域已关闭、但仍需延续锁定的目标（累计领域内锁定 tick ×3，上限 {@link #POST_DOMAIN_LOCK_MAX_TICKS}）。
     */
    private final ConcurrentMap<UUID, PostDomainMobLockState> postDomainMobLockMap = new ConcurrentHashMap<>();
    /**
     * 领域完成时处于施法者 {@link #EXCLUDE_CASTER_RADIUS} 格内、未被转移的非玩家生物：
     * 与施法者相同演出锁定 {@link #LOCK_PLAYER_DURATION_TICKS} tick（朝北、固定位姿、暂停 AI）。
     */
    private final ConcurrentMap<UUID, NearCasterMobLockState> nearCasterMobLockMap = new ConcurrentHashMap<>();
    /**
     * 无量空处本次领域会话的施法目标白名单：领域<strong>完成瞬间</strong>在施法者 {@link #EXCLUDE_CASTER_RADIUS} 格内的生物 UUID。
     * 直至领域关闭前，这些实体永远不会被登记为领域锁定目标（与当前是否仍贴近施法者无关）。
     */
    private final ConcurrentMap<UUID, Set<UUID>> limitlessSessionMobWhitelist = new ConcurrentHashMap<>();
    /**
     * 施法者 UUID → 应将天空盒切换为 {@link #LIMITLESS_DOMAIN_SKYBOX_TEXTURE_AFTER_280} 的服务器 tick（含等于该 tick 时执行）。
     */
    private final ConcurrentMap<UUID, Integer> limitlessSkyboxSwapAtServerTick = new ConcurrentHashMap<>();
    /**
     * 施法者 UUID → 允许再次按键解除 {@link #DOMAIN_NAME} 的最早服务器 tick（领域构建完成后再过 {@link #LOCK_PLAYER_DURATION_TICKS} 刻）。
     * 构建动画进行中另由 {@link BarrierInstance#isExpanding()} 拦截。
     */
    private final ConcurrentMap<UUID, Integer> limitlessDomainDismissUnlockServerTick = new ConcurrentHashMap<>();
    /** 用于每服务器 tick 只处理一次全服无量空处副作用（延续锁定、近身演出锁定等）。 */
    private static int lastPostDomainLockProcessedServerTick = -1;

    // ==================== 构造 ====================

    private LimitlessDomainExpansion()
    {
        setDomainName(DOMAIN_NAME);
    }

    // ==================== 实现抽象方法 ====================

    @Override
    protected void applyDomainSpecificEffect(ServerPlayer player, BarrierInstance instance)
    {
        /* 瞬发：不搬生物、不上演出锁，只做天空盒与慢镜头。 */
        if (instance.isAutoCloseOnComplete() || instance.isSkyboxOnlyExpand())
        {
            return;
        }
        broadcastLimitlessDomainCompleteScreenShake(player, player.serverLevel());
        broadcastDomainWhiteCurtainElevate(player, player.serverLevel());
        ModNetwork.sendLimitlessDomainCmdCamSetupA(player, player.position());
        spawnLimitlessLightLineEntity(player, instance);
        populateLimitlessSessionMobWhitelist(player);
        int domainLockedTargets = relocateAndLockDomainMobs(player, instance);
        maybeAnnouncePerfectControl(player, instance, domainLockedTargets);
        lockSessionWhitelistedCreatures(player);
        scheduleLimitlessSkyboxTextureSwapAfterCutscene(player);
        scheduleLimitlessDomainDismissUnlockAfterPerformance(player);
    }

    @Override
    protected boolean hasLearnedTechnique(ServerPlayer player)
    {
        return PlayerTechniqueData.isLimitless(player);
    }

    @Override
    public String getTechniqueType()
    {
        return "limitless_domain";
    }

    @Override
    protected ResourceLocation getDomainSkyboxTexture()
    {
        return LIMITLESS_DOMAIN_SKYBOX_TEXTURE;
    }

    @Override
    public ResourceLocation resolveInnateDomainSkyboxTexture(ServerPlayer player)
    {
        return LIMITLESS_DOMAIN_SKYBOX_TEXTURE_AFTER_280;
    }

    @Override
    protected double getMaintainCostReduction(ServerPlayer player)
    {
        return LimitlessDomainSkillGrowth.maintainCostReduction(player);
    }

    @Override
    protected void onBarrierClosed(ServerPlayer player, BarrierInstance instance)
    {
        ServerLevel level = player.getServer().getLevel(instance.getDimension());
        if (level == null)
        {
            return;
        }
        /*
         * 咒力耗尽 / 崩解 / 离开范围等非自愿解除，且在演出窗口内（展开动画未结束，或构建完成后未满 280 刻）：
         * 立刻解除施法者、白名单单位、领域内锁定目标的所有演出锁与客户端同步，并停止展开音效与蓄力白色滤镜。
         */
        if (isLimitlessForcedCloseDuringPerformance(player))
        {
            MinecraftServer server = level.getServer();
            if (server != null)
            {
                releaseAllLimitlessPerformanceLocks(player, server);
            }
            cancelLimitlessChargeClientPresentation(player);
        }
        broadcastLimitlessDomainAmbientSoundStop(level, player.getUUID());
        removeSpecialEffectEntitiesInsideBarrierSphere(level, instance);
        super.onBarrierClosed(player, instance);
    }

    /** 与 {@link #canDismissLimitlessDomainNow} 相反：仍处于「不可主动解除」的演出阶段时视为被迫关闭需清场。 */
    private boolean isLimitlessForcedCloseDuringPerformance(ServerPlayer player)
    {
        return !canDismissLimitlessDomainNow(player);
    }

    /**
     * 解除无量空处本场演出的全部锁定：施法者镜头/移动/按键、白名单生物与玩家的 north 锁、领域转移目标的 south 锁。
     */
    private void releaseAllLimitlessPerformanceLocks(ServerPlayer caster, MinecraftServer server)
    {
        UUID casterUuid = caster.getUUID();
        ServerLevel serverLevel = caster.serverLevel();

        playerLockMap.remove(casterUuid);
        ModNetwork.sendPlayerInputLockSync(caster, false);

        Set<UUID> whitelist = limitlessSessionMobWhitelist.get(casterUuid);
        if (whitelist != null)
        {
            for (UUID targetId : whitelist)
            {
                if (playerLockMap.remove(targetId) != null)
                {
                    ServerPlayer sp = server.getPlayerList().getPlayer(targetId);
                    if (sp != null)
                    {
                        ModNetwork.sendPlayerInputLockSync(sp, false);
                    }
                }
                NearCasterMobLockState nearState = nearCasterMobLockMap.remove(targetId);
                if (nearState != null)
                {
                    releaseNearCasterMobLock(targetId, nearState, server);
                }
            }
        }

        List<UUID> mobToRemove = new ArrayList<>();
        for (Map.Entry<UUID, MobLockState> entry : mobLockMap.entrySet())
        {
            if (!entry.getValue().ownerUuid().equals(casterUuid))
            {
                continue;
            }
            releaseMobLock(entry.getKey(), entry.getValue(), serverLevel);
            mobToRemove.add(entry.getKey());
        }
        for (UUID id : mobToRemove)
        {
            mobLockMap.remove(id);
        }
    }

    // ==================== 覆盖施放逻辑（延迟蓄力） ====================

    @Override
    protected void configureDomainInstance(ServerPlayer player, BarrierInstance instance)
    {
        if (!PlayerDomainModeData.isInstant(player))
        {
            return;
        }
        /* 瞬发：只展开天空盒，不放结界壳；铺完后保持完成态 2 秒再关。 */
        instance.setSkyboxOnlyExpand(true);
        instance.setAutoCloseOnComplete(true);
        /* 跳过演出，直接用演出后的第二套天空盒（stat_sky2）。 */
        instance.setCustomSkyboxTexture(LIMITLESS_DOMAIN_SKYBOX_TEXTURE_AFTER_280);
    }

    /**
     * 无量空处的施放逻辑：先进入蓄力状态，2 秒后才真正展开领域。
     * <p>
     * 按下领域展开键时：
     * <ol>
     *   <li>在玩家位置播放领域展开音效</li>
     *   <li>进入蓄力状态，显示蓄力提示</li>
     *   <li>蓄力期间每 tick 由 {@link #tickCastingDelay(ServerPlayer)} 驱动倒计时</li>
     *   <li>蓄力期间再次按领域键可取消本次展开</li>
     *   <li>蓄力完成后调用 {@code super.cast(player)} 真正展开领域</li>
     * </ol>
     * 如果玩家已在自己的领域内，立即解除领域（不经过蓄力）。
     * <p>
     * 瞬发模式：跳过蓄力与领域演出，只播天空盒展开（不放结界壳方块）；
     * 构建完成后保持约 2 秒，期间对半径 32 内全体生物施加隐藏最高档缓慢与挖掘疲劳，再自动关闭。
     */
    @Override
    public void cast(ServerPlayer player)
    {
        if (!player.isAlive() || !isInBattleMode(player))
        {
            return;
        }

        /* 如果玩家已在自己领域内 → 直接解除，不需要蓄力 */
        if (isPlayerInsideOwnDomain(player))
        {
            if (!canDismissLimitlessDomainNow(player))
            {
                player.displayClientMessage(
                        Component.literal("「" + DOMAIN_NAME + "」暂无法解除").withStyle(ChatFormatting.RED),
                        true);
                return;
            }
            super.cast(player);
            return;
        }

        UUID uuid = player.getUUID();

        if (TechniqueMeltdownEffect.blockDomainExpansion(player))
        {
            return;
        }

        /* 瞬发：跳过蓄力与演出，直接走结界展开 */
        if (PlayerDomainModeData.isInstant(player))
        {
            if (castingDelayMap.containsKey(uuid))
            {
                castingDelayMap.remove(uuid);
                EpicFightCompat.stopLimitlessDomainChargeAnimation(player);
                cancelLimitlessChargeClientPresentation(player);
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
            super.cast(player);
            return;
        }

        /* 蓄力中再次按下领域键 → 取消本次展开 */
        if (castingDelayMap.containsKey(uuid))
        {
            castingDelayMap.remove(uuid);
            EpicFightCompat.stopLimitlessDomainChargeAnimation(player);
            cancelLimitlessChargeClientPresentation(player);
            player.displayClientMessage(
                    Component.literal("「" + DOMAIN_NAME + "」已取消")
                            .withStyle(ChatFormatting.YELLOW),
                    true);
            return;
        }

        /* 前置条件校验（与父类 cast 一致，但提前检查以避免蓄力后失败） */
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

        /* 播放领域展开音效 */
        ServerLevel level = player.serverLevel();
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.LIMITLESS_DOMAIN_EXPANSION.get(), SoundSource.PLAYERS,
                1.0F, 1.0F);

        /*
         * 蓄力开场白幕：用 DomainWhiteCurtain shader 代替旧 WhiteOverlay 滤镜。
         * 影响范围同原先：施法者周围 WHITE_OVERLAY_RADIUS 格内玩家。
         */
        double radiusSqr = WHITE_OVERLAY_RADIUS * WHITE_OVERLAY_RADIUS;
        for (ServerPlayer nearbyPlayer : level.players())
        {
            if (nearbyPlayer.distanceToSqr(player) > radiusSqr)
            {
                continue;
            }
            ModNetwork.sendDomainWhiteCurtainSync(nearbyPlayer, WHITE_CURTAIN_TOTAL_TICKS);
        }

        /* 进入蓄力状态 */
        castingDelayMap.put(uuid, CAST_DELAY_TICKS);
        EpicFightCompat.playLimitlessDomainChargeAnimation(player);
        /*player.displayClientMessage(
                 Component.literal("「" + DOMAIN_NAME + "」展开蓄力中...")
                        .withStyle(ChatFormatting.LIGHT_PURPLE), true);*/
    }

    /**
     * 蓄力取消时：停止 {@link ModSounds#LIMITLESS_DOMAIN_EXPANSION} 在客户端的播放，并清除与开始蓄力时同范围玩家的白色滤镜。
     */
    private static void cancelLimitlessChargeClientPresentation(ServerPlayer caster)
    {
        EpicFightCompat.stopLimitlessDomainChargeAnimation(caster);
        ServerLevel level = caster.serverLevel();
        ClientboundStopSoundPacket stopExpansion = new ClientboundStopSoundPacket(
                ModSounds.LIMITLESS_DOMAIN_EXPANSION.get().getLocation(),
                SoundSource.PLAYERS);
        for (ServerPlayer sp : level.players())
        {
            sp.connection.send(stopExpansion);
        }
        double radiusSqr = WHITE_OVERLAY_RADIUS * WHITE_OVERLAY_RADIUS;
        for (ServerPlayer nearby : level.players())
        {
            if (nearby.distanceToSqr(caster) > radiusSqr)
            {
                continue;
            }
            ModNetwork.sendWhiteOverlaySync(nearby, false, 0.0F);
        }
    }

    // ==================== 蓄力 tick 驱动 ====================

    /**
     * 每tick驱动蓄力倒计时，蓄力完成后真正展开领域。
     * <p>
     * 应在 {@link #tickMaintain(ServerPlayer)} 中调用。
     *
     * @param player 玩家
     */
    public void tickCastingDelay(ServerPlayer player)
    {
        UUID uuid = player.getUUID();
        Integer remaining = castingDelayMap.get(uuid);
        if (remaining == null)
        {
            return;
        }

        /* 蓄力期间玩家死亡或脱离战斗模式 → 中断蓄力 */
        if (!player.isAlive() || !isInBattleMode(player))
        {
            castingDelayMap.remove(uuid);
            EpicFightCompat.stopLimitlessDomainChargeAnimation(player);
            player.displayClientMessage(
                    Component.literal("「无量空处」展开蓄力中断")
                            .withStyle(ChatFormatting.RED), true);
            return;
        }

        int newRemaining = remaining - 1;
        if (newRemaining <= 0)
        {
            castingDelayMap.remove(uuid);
            /*
             * 不在此处 stop Epic Fight 蓄力动画：剪辑可长于蓄力时长（例如 8s JSON），否则会在约 CAST_DELAY_TICKS 处被截断并显得前半段「极慢」。
             */
            /* 蓄力完成，调用父类 cast 真正展开领域 */
            super.cast(player);
        }
        else
        {
            castingDelayMap.put(uuid, newRemaining);
        }
    }

    /**
     * 检查指定玩家是否正在蓄力中。
     */
    public boolean isCasting(ServerPlayer player)
    {
        return castingDelayMap.containsKey(player.getUUID());
    }

    /**
     * 清除指定玩家的蓄力状态（玩家断线等情况）。
     */
    public void clearCastingState(UUID uuid)
    {
        castingDelayMap.remove(uuid);
    }

    // ==================== 覆盖 tickMaintain 以驱动蓄力 ====================

    @Override
    public void tickMaintain(ServerPlayer player)
    {
        tickPostDomainLocksOncePerServerTick(player);
        boolean instant = hasInstantDomainInstance(player);
        if (!instant)
        {
            tickPlayerLockState(player);
            captureNewDomainIntruders(player);
            tickLockedMobs(player);
        }
        /* 驱动蓄力倒计时 */
        tickCastingDelay(player);
        /* 正常的领域维持逻辑 */
        super.tickMaintain(player);
        if (!instant)
        {
            tickLimitlessSkyboxTextureSwap(player);
        }
        clearOwnerMobLocksIfDomainClosed(player);
    }

    /** 当前活跃领域是否为瞬发（天空盒-only / 构建完即关）。 */
    private boolean hasInstantDomainInstance(ServerPlayer player)
    {
        java.util.Deque<BarrierInstance> queue = ACTIVE_DOMAINS.get(player.getUUID());
        if (queue == null || queue.isEmpty())
        {
            return false;
        }
        for (BarrierInstance instance : queue)
        {
            if (instance.isAutoCloseOnComplete() || instance.isSkyboxOnlyExpand())
            {
                return true;
            }
        }
        return false;
    }

    // ==================== 辅助方法 ====================

    /**
     * 检查玩家是否在自己的领域内（用于判断是否直接解除领域）。
     */
    private boolean isPlayerInsideOwnDomain(ServerPlayer player)
    {
        java.util.Deque<BarrierInstance> queue = ACTIVE_DOMAINS.get(player.getUUID());
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

    /**
     * 结界构建完成瞬间：对施法者 {@link #WHITE_OVERLAY_RADIUS} 格内玩家同步客户端屏幕振动。
     */
    private static void broadcastLimitlessDomainCompleteScreenShake(ServerPlayer caster, ServerLevel level)
    {
        double radiusSqr = WHITE_OVERLAY_RADIUS * WHITE_OVERLAY_RADIUS;
        for (ServerPlayer nearby : level.players())
        {
            if (nearby.distanceToSqr(caster) > radiusSqr)
            {
                continue;
            }
            ModNetwork.sendScreenShake(nearby, ScreenShakeKind.DECAY, DOMAIN_COMPLETE_SCREEN_SHAKE_TICKS, DOMAIN_COMPLETE_SCREEN_SHAKE_MAX_DEG);
        }
    }

    /**
     * 结界/天空盒完成后：将范围内玩家正在播放的 shader 白幕提升到 AFTER_LEVEL，避免开洞被天空盒盖住。
     */
    private static void broadcastDomainWhiteCurtainElevate(ServerPlayer caster, ServerLevel level)
    {
        double radiusSqr = WHITE_OVERLAY_RADIUS * WHITE_OVERLAY_RADIUS;
        for (ServerPlayer nearby : level.players())
        {
            if (nearby.distanceToSqr(caster) > radiusSqr)
            {
                continue;
            }
            ModNetwork.sendDomainWhiteCurtainElevateSync(nearby);
        }
    }

    /*
     * 无量空处构建完成后的固定演出：
     * 1) 以世界绝对坐标系为基准，在球心 south 方向 22 格放置特效锚点；
     * 2) 锁定施法者视角到 north 并冻结移动 280 tick。
     * 业务意图：确保镜头构图和 Photon 朝向不受玩家当时朝向影响，演出可重复且稳定。
     */
    private void spawnLimitlessLightLineEntity(ServerPlayer player, BarrierInstance instance)
    {
        ServerLevel level = player.serverLevel();
        SpecialEffectEntity entity = ModEntities.SPECIAL_EFFECT_ENTITY.get().create(level);
        if (entity == null)
        {
            return;
        }

        BlockPos center = instance.getCenter();
        double spawnX = center.getX() + 0.5D;
        double spawnY = player.getY();
        double spawnZ = center.getZ() + 0.5D + LIGHT_LINE_SOUTH_DISTANCE;

        entity.moveTo(spawnX, spawnY, spawnZ, NORTH_YAW, 0.0F);
        if (!level.addFreshEntity(entity))
        {
            return;
        }

        CommandSourceStack commandSource = entity.createCommandSourceStack()
                .withPermission(2)
                .withSuppressedOutput();
        level.getServer().getCommands().performPrefixedCommand(commandSource, "photon fx photon:limitless_light_line entity @s");

        playerLockMap.put(player.getUUID(), new PlayerLockState(new Vec3(player.getX(), player.getY(), player.getZ()), player.getXRot(), LOCK_PLAYER_DURATION_TICKS));
        ModNetwork.sendPlayerInputLockSync(player, true);
    }

    /**
     * 每 tick 维持“朝向 north + 禁止移动”的强制状态。
     * <p>
     * 重要副作用：玩家尝试移动或转头会被服务器在下一 tick 拉回固定位置与朝向。
     */
    private void tickPlayerLockState(ServerPlayer player)
    {
        UUID uuid = player.getUUID();
        PlayerLockState lockState = playerLockMap.get(uuid);
        if (lockState == null)
        {
            return;
        }

        if (!player.isAlive() || lockState.remainingTicks() <= 0)
        {
            playerLockMap.remove(uuid);
            ModNetwork.sendPlayerInputLockSync(player, false);
            return;
        }

        Vec3 lockPos = lockState.lockPos();
        float lockXRot = lockState.lockXRot();
        player.setDeltaMovement(Vec3.ZERO);
        player.hurtMarked = true;
        player.connection.teleport(lockPos.x, lockPos.y, lockPos.z, NORTH_YAW, lockXRot);

        playerLockMap.put(uuid, lockState.decreaseOneTick());
    }

    /**
     * 将领域内生物转移到球心 north 方向约 15 格区域，并锁定朝向 south 与移动能力。
     * <p>
     * 排除规则：SpecialEffectEntity、施法者、领域完成时快照的 {@link #limitlessSessionMobWhitelist} 内生物。
     * 领域存续期间，新进入球内且非排除对象的目标由 {@link #captureNewDomainIntruders(ServerPlayer)} 每 tick 补捕获。
     */
    /**
     * @return 本次登记为无量空处领域锁定（转移）目标的存活生物数量（结界完成瞬间快照）。
     */
    private int relocateAndLockDomainMobs(ServerPlayer caster, BarrierInstance instance)
    {
        ServerLevel level = caster.serverLevel();
        BlockPos center = instance.getCenter();
        int radius = instance.getRadius();
        double radiusSqr = (double) radius * (double) radius;
        UUID casterUuid = caster.getUUID();

        AABB queryBox = new AABB(
                center.getX() - radius, center.getY() - radius, center.getZ() - radius,
                center.getX() + radius + 1.0D, center.getY() + radius + 1.0D, center.getZ() + radius + 1.0D
        );
        List<LivingEntity> candidates = level.getEntitiesOfClass(LivingEntity.class, queryBox, entity ->
        {
            if (entity == null || !entity.isAlive())
            {
                return false;
            }
            if (entity instanceof SpecialEffectEntity)
            {
                return false;
            }
            if (entity.getUUID().equals(casterUuid))
            {
                return false;
            }
            if (isExcludedFromBeingLimitlessDomainMobTarget(caster, casterUuid, entity))
            {
                return false;
            }
            return isInsideDomainSphere(entity.position(), center, radiusSqr);
        });
        if (candidates.isEmpty())
        {
            return 0;
        }

        Vec3 anchor = new Vec3(center.getX() + 0.5D, caster.getY(), center.getZ() + 0.5D - MOB_RELOCATE_NORTH_DISTANCE);
        List<Vec3> targetPositions = buildScatterPositions(anchor, candidates.size());
        for (int i = 0; i < candidates.size(); i++)
        {
            LivingEntity target = candidates.get(i);
            Vec3 targetPos = targetPositions.get(i);
            registerLivingEntityInDomainMobLock(caster, target, targetPos, level);
        }
        return candidates.size();
    }

    private static void maybeAnnouncePerfectControl(ServerPlayer caster, BarrierInstance instance, int domainLockedTargetCount)
    {
        if (domainLockedTargetCount <= PERFECT_CONTROL_LOCKED_TARGET_THRESHOLD)
        {
            return;
        }
        ServerLevel level = caster.serverLevel();
        BlockPos center = instance.getCenter();
        double cx = center.getX() + 0.5D;
        double cy = center.getY() + 0.5D;
        double cz = center.getZ() + 0.5D;
        level.playSound(null, cx, cy, cz, ModSounds.LIMITLESS_DOMAIN_PERFECT_AMBIENT.get(), SoundSource.AMBIENT, 1.0F, 1.0F);
        level.playSound(null, caster.getX(), caster.getY(), caster.getZ(), ModSounds.SOUND_EFFECTS_IRON_SOUND.get(), SoundSource.PLAYERS, PERFECT_CONTROL_IRON_SOUND_VOLUME, 1.0F);
        caster.displayClientMessage(Component.literal("完美控制").withStyle(ChatFormatting.GOLD), false);
        grantAdvancement(caster, "perfect_control");
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

    /**
     * 领域展开完成后，任何进入球内且非排除的生物都会被补登记为领域锁定目标。
     * <p>
     * 排除：施法者本人、{@link SpecialEffectEntity}、本次领域会话快照白名单 {@link #limitlessSessionMobWhitelist}。
     * 已被本施法者锁定的目标会跳过；已被其他施法者锁定的实体不抢夺。
     */
    private void captureNewDomainIntruders(ServerPlayer owner)
    {
        java.util.Deque<BarrierInstance> queue = ACTIVE_DOMAINS.get(owner.getUUID());
        if (queue == null || queue.isEmpty())
        {
            return;
        }
        BarrierInstance instance = queue.peekLast();
        if (instance == null)
        {
            return;
        }
        /* 瞬发领域不转移、不锁定生物位置。 */
        if (instance.isAutoCloseOnComplete() || instance.isSkyboxOnlyExpand())
        {
            return;
        }
        ServerLevel level = owner.serverLevel();
        if (!instance.getDimension().equals(level.dimension()))
        {
            return;
        }
        /* 展开动画在本 tick 的 super.tickMaintain 中才结束；此前白名单未写入，补捕获会误伤施法者身边单位。 */
        if (instance.isExpanding())
        {
            return;
        }
        BlockPos center = instance.getCenter();
        int radius = instance.getRadius();
        double radiusSqr = (double) radius * (double) radius;
        UUID ownerUuid = owner.getUUID();

        int nextScatterIndex = countMobLocksForOwner(ownerUuid);
        Vec3 anchor = new Vec3(center.getX() + 0.5D, owner.getY(), center.getZ() + 0.5D - MOB_RELOCATE_NORTH_DISTANCE);

        AABB queryBox = new AABB(
                center.getX() - radius, center.getY() - radius, center.getZ() - radius,
                center.getX() + radius + 1.0D, center.getY() + radius + 1.0D, center.getZ() + radius + 1.0D
        );
        List<LivingEntity> inside = level.getEntitiesOfClass(LivingEntity.class, queryBox, entity ->
        {
            if (entity == null || !entity.isAlive())
            {
                return false;
            }
            if (entity instanceof SpecialEffectEntity)
            {
                return false;
            }
            if (entity.getUUID().equals(ownerUuid))
            {
                return false;
            }
            if (isExcludedFromBeingLimitlessDomainMobTarget(owner, ownerUuid, entity))
            {
                return false;
            }
            return isInsideDomainSphere(entity.position(), center, radiusSqr);
        });

        for (LivingEntity target : inside)
        {
            if (mobLockMap.containsKey(target.getUUID()))
            {
                continue;
            }
            Vec3 targetPos = buildScatterPositions(anchor, nextScatterIndex + 1).get(nextScatterIndex);
            nextScatterIndex++;
            registerLivingEntityInDomainMobLock(owner, target, targetPos, level);
        }
    }

    private int countMobLocksForOwner(UUID ownerUuid)
    {
        int n = 0;
        for (MobLockState s : mobLockMap.values())
        {
            if (s.ownerUuid().equals(ownerUuid))
            {
                n++;
            }
        }
        return n;
    }

    /**
     * 领域完成瞬间：根据施法者当前位置，将 {@link #EXCLUDE_CASTER_RADIUS} 格内存活生物（除施法者、特效实体）记入本会话白名单。
     * <p>
     * 装载了「移除身旁豁免」SE 时跳过：身旁单位不再幸免。
     */
    private void populateLimitlessSessionMobWhitelist(ServerPlayer caster)
    {
        Set<UUID> set = limitlessSessionMobWhitelist.computeIfAbsent(caster.getUUID(), k -> ConcurrentHashMap.newKeySet());
        set.clear();
        if (removesNearCasterExempt(caster))
        {
            return;
        }
        ServerLevel level = caster.serverLevel();
        double excludeRadiusSqr = EXCLUDE_CASTER_RADIUS * EXCLUDE_CASTER_RADIUS;
        AABB queryBox = caster.getBoundingBox().inflate(EXCLUDE_CASTER_RADIUS, EXCLUDE_CASTER_RADIUS, EXCLUDE_CASTER_RADIUS);
        List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class, queryBox, entity ->
        {
            if (entity == null || !entity.isAlive())
            {
                return false;
            }
            if (entity instanceof SpecialEffectEntity)
            {
                return false;
            }
            if (entity.getUUID().equals(caster.getUUID()))
            {
                return false;
            }
            return entity.distanceToSqr(caster) <= excludeRadiusSqr;
        });
        for (LivingEntity entity : nearby)
        {
            set.add(entity.getUUID());
        }
    }

    private boolean isLimitlessSessionWhitelisted(UUID casterUuid, UUID entityUuid)
    {
        Set<UUID> set = limitlessSessionMobWhitelist.get(casterUuid);
        return set != null && set.contains(entityUuid);
    }

    /**
     * 是否不得登记为无量空处领域锁定（转移）目标：快照白名单内；或本会话尚未写入白名单键时按施法者身边距离临时保护。
     * <p>
     * 装载了「移除身旁豁免」SE 时恒为 false。
     */
    private boolean isExcludedFromBeingLimitlessDomainMobTarget(ServerPlayer caster, UUID casterUuid, LivingEntity entity)
    {
        if (removesNearCasterExempt(caster))
        {
            return false;
        }
        if (isLimitlessSessionWhitelisted(casterUuid, entity.getUUID()))
        {
            return true;
        }
        if (!limitlessSessionMobWhitelist.containsKey(casterUuid))
        {
            return entity.distanceToSqr(caster) <= EXCLUDE_CASTER_RADIUS * EXCLUDE_CASTER_RADIUS;
        }
        return false;
    }

    /** 是否因 SE 关掉身旁豁免（只认装载了对应窄口的施法者）。 */
    private static boolean removesNearCasterExempt(ServerPlayer caster)
    {
        return SeMechanicRegistry.findEquipped(caster, LimitlessNearCasterExemptSeMechanic.class)
                .map(LimitlessNearCasterExemptSeMechanic::removesNearCasterExempt)
                .orElse(false);
    }

    /**
     * 登记为无量空处领域锁定单位（构建完成后 280 刻内：南侧散布固定位；之后仅失明、暂停 AI、玩家输入锁等，不再传送与锁朝南）。
     */
    private void registerLivingEntityInDomainMobLock(ServerPlayer caster, LivingEntity target, Vec3 targetPos, ServerLevel level)
    {
        if (isExcludedFromBeingLimitlessDomainMobTarget(caster, caster.getUUID(), target))
        {
            return;
        }
        MinecraftServer server = level.getServer();
        int serverTick = server != null ? server.getTickCount() : 0;
        boolean enforceAnchorPose = !isLimitlessDomainMobAnchorPoseExpired(caster.getUUID(), serverTick);
        PostDomainMobLockState lingeringPost = postDomainMobLockMap.remove(target.getUUID());
        if (lingeringPost != null && server != null)
        {
            releasePostDomainMobLock(target.getUUID(), lingeringPost, server);
        }
        NearCasterMobLockState lingeringNear = nearCasterMobLockMap.remove(target.getUUID());
        if (lingeringNear != null && server != null)
        {
            releaseNearCasterMobLock(target.getUUID(), lingeringNear, server);
        }
        if (enforceAnchorPose)
        {
            lockEntityToSouth(target, targetPos);
        }
        boolean aiPausedByThisLock = false;
        boolean originNoAi = false;
        if (target instanceof Mob mob)
        {
            originNoAi = mob.isNoAi();
            if (!originNoAi)
            {
                mob.setNoAi(true);
                aiPausedByThisLock = true;
            }
        }
        if (target instanceof ServerPlayer targetPlayer)
        {
            ModNetwork.sendPlayerInputLockSync(targetPlayer, true);
        }
        applyLockBlindness(target);
        mobLockMap.put(target.getUUID(), new MobLockState(caster.getUUID(), targetPos, aiPausedByThisLock, originNoAi, 0));
    }

    /**
     * 对领域完成时记入白名单的生物施加与施法者相同的 {@link #LOCK_PLAYER_DURATION_TICKS} tick 演出锁定
     * （朝北、位置与俯仰角固定；非玩家暂停 AI）。白名单实体之后离开施法者身边也不会变为领域锁定目标。
     */
    private void lockSessionWhitelistedCreatures(ServerPlayer caster)
    {
        ServerLevel level = caster.serverLevel();
        MinecraftServer server = level.getServer();
        Set<UUID> whitelist = limitlessSessionMobWhitelist.get(caster.getUUID());
        if (whitelist == null || whitelist.isEmpty())
        {
            return;
        }
        for (UUID targetId : whitelist)
        {
            if (!(level.getEntity(targetId) instanceof LivingEntity target) || !target.isAlive())
            {
                continue;
            }
            Vec3 lockPos = new Vec3(target.getX(), target.getY(), target.getZ());
            float lockXRot = target.getXRot();
            PostDomainMobLockState lingeringPost = postDomainMobLockMap.remove(target.getUUID());
            if (lingeringPost != null && server != null)
            {
                releasePostDomainMobLock(target.getUUID(), lingeringPost, server);
            }
            NearCasterMobLockState lingeringNear = nearCasterMobLockMap.remove(target.getUUID());
            if (lingeringNear != null && server != null)
            {
                releaseNearCasterMobLock(target.getUUID(), lingeringNear, server);
            }
            if (target instanceof ServerPlayer sp)
            {
                playerLockMap.put(sp.getUUID(), new PlayerLockState(lockPos, lockXRot, LOCK_PLAYER_DURATION_TICKS));
                ModNetwork.sendPlayerInputLockSync(sp, true);
                continue;
            }
            boolean originNoAi = false;
            boolean aiPausedByThisLock = false;
            if (target instanceof Mob mob)
            {
                originNoAi = mob.isNoAi();
                if (!originNoAi)
                {
                    mob.setNoAi(true);
                    aiPausedByThisLock = true;
                }
            }
            nearCasterMobLockMap.put(target.getUUID(), new NearCasterMobLockState(
                    lockPos, lockXRot, LOCK_PLAYER_DURATION_TICKS, aiPausedByThisLock, originNoAi));
            applyCasterStyleLockToNonPlayerLiving(target, lockPos, lockXRot);
        }
    }

    /**
     * 领域（含天空盒）完成时登记：在 {@link #LOCK_PLAYER_DURATION_TICKS} 个服务器 tick 后将内壁 jjkz_skyblock 换为 stat_sky2。
     */
    private void scheduleLimitlessSkyboxTextureSwapAfterCutscene(ServerPlayer caster)
    {
        MinecraftServer server = caster.getServer();
        if (server == null)
        {
            return;
        }
        limitlessSkyboxSwapAtServerTick.put(caster.getUUID(), server.getTickCount() + LOCK_PLAYER_DURATION_TICKS);
    }

    /**
     * 领域构建完成瞬间登记：再过 {@link #LOCK_PLAYER_DURATION_TICKS} 刻才允许施法者按键解除领域。
     */
    private void scheduleLimitlessDomainDismissUnlockAfterPerformance(ServerPlayer caster)
    {
        MinecraftServer server = caster.getServer();
        if (server == null)
        {
            return;
        }
        limitlessDomainDismissUnlockServerTick.put(caster.getUUID(), server.getTickCount() + LOCK_PLAYER_DURATION_TICKS);
    }

    /**
     * 构建动画进行中不可解除；构建完成后须再等 {@link #LOCK_PLAYER_DURATION_TICKS} 刻。
     */
    private boolean canDismissLimitlessDomainNow(ServerPlayer player)
    {
        MinecraftServer server = player.getServer();
        if (server == null)
        {
            return true;
        }
        UUID uuid = player.getUUID();
        java.util.Deque<BarrierInstance> queue = ACTIVE_DOMAINS.get(uuid);
        BarrierInstance instance = queue != null ? queue.peekLast() : null;
        if (instance != null && instance.isExpanding())
        {
            return false;
        }
        Integer unlockAt = limitlessDomainDismissUnlockServerTick.get(uuid);
        return unlockAt == null || server.getTickCount() >= unlockAt;
    }

    /**
     * 与 {@link #scheduleLimitlessDomainDismissUnlockAfterPerformance} 同一时刻起：
     * {@link #mobLockMap} 目标不再被强制传送到球心北侧散布位，也不再锁朝南朝向；失明、乱码聊天、AI/输入锁等不受影响。
     */
    private boolean isLimitlessDomainMobAnchorPoseExpired(UUID casterUuid, int serverTick)
    {
        Integer unlockAt = limitlessDomainDismissUnlockServerTick.get(casterUuid);
        return unlockAt != null && serverTick >= unlockAt;
    }

    /**
     * 到达预定 tick 时，将当前无量空处实例内所有天空盒方块与修补用纹理改为 stat_sky2，
     * 在球心放置 {@link SpecialEffectEntity} 执行 {@code photon:limitless_inkwash}，
     * 并对球心 {@link #WHITE_OVERLAY_RADIUS} 格内玩家播放一次全屏世界层闪光（方块与实体之上、HUD 之下）；
     * 对球心附近玩家开始循环播放 {@link ModSounds#LIMITLESS_DOMAIN_CENTER_AMBIENT}（20% 音量，领域关闭时停止）。
     */
    private void tickLimitlessSkyboxTextureSwap(ServerPlayer owner)
    {
        UUID ownerUuid = owner.getUUID();
        Integer swapAt = limitlessSkyboxSwapAtServerTick.get(ownerUuid);
        if (swapAt == null)
        {
            return;
        }
        MinecraftServer server = owner.getServer();
        if (server == null || server.getTickCount() < swapAt)
        {
            return;
        }
        java.util.Deque<BarrierInstance> queue = ACTIVE_DOMAINS.get(ownerUuid);
        if (queue == null || queue.isEmpty())
        {
            limitlessSkyboxSwapAtServerTick.remove(ownerUuid);
            limitlessDomainDismissUnlockServerTick.remove(ownerUuid);
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
        applyLimitlessSkyboxTextureToInstance(level, instance, LIMITLESS_DOMAIN_SKYBOX_TEXTURE_AFTER_280);
        spawnLimitlessInkwashAnchorAtSphereCenter(level, instance);
        sendPostDomainCompletionWhiteFlashNearSphereCenter(level, instance);
        broadcastLimitlessDomainAmbientSoundStart(level, instance, ownerUuid);
        limitlessSkyboxSwapAtServerTick.remove(ownerUuid);
    }

    /** 对领域球心附近玩家同步全屏闪光（{@link ModNetwork#sendFullScreenWorldFlashAnimationSync}）。 */
    private static void sendPostDomainCompletionWhiteFlashNearSphereCenter(ServerLevel level, BarrierInstance instance)
    {
        BlockPos c = instance.getCenter();
        double cx = c.getX() + 0.5D;
        double cy = c.getY() + 0.5D;
        double cz = c.getZ() + 0.5D;
        double radiusSqr = WHITE_OVERLAY_RADIUS * WHITE_OVERLAY_RADIUS;
        for (ServerPlayer nearby : level.players())
        {
            double dx = nearby.getX() - cx;
            double dy = nearby.getY() - cy;
            double dz = nearby.getZ() - cz;
            if (dx * dx + dy * dy + dz * dz > radiusSqr)
            {
                continue;
            }
            ModNetwork.sendFullScreenWorldFlashAnimationSync(
                    nearby,
                    POST_DOMAIN_COMPLETION_FLASH_TOTAL_TICKS,
                    POST_DOMAIN_COMPLETION_FLASH_FADE_IN_TICKS,
                    POST_DOMAIN_COMPLETION_FLASH_FADE_OUT_TICKS,
                    POST_DOMAIN_COMPLETION_FLASH_PEAK_ALPHA
            );
        }
    }

    private static void broadcastLimitlessDomainAmbientSoundStart(ServerLevel level, BarrierInstance instance, UUID casterUuid)
    {
        BlockPos c = instance.getCenter();
        double cx = c.getX() + 0.5D;
        double cy = c.getY() + 0.5D;
        double cz = c.getZ() + 0.5D;
        double radiusSqr = LIMITLESS_DOMAIN_AMBIENT_SOUND_SYNC_RADIUS * LIMITLESS_DOMAIN_AMBIENT_SOUND_SYNC_RADIUS;
        for (ServerPlayer nearby : level.players())
        {
            double dx = nearby.getX() - cx;
            double dy = nearby.getY() - cy;
            double dz = nearby.getZ() - cz;
            if (dx * dx + dy * dy + dz * dz > radiusSqr)
            {
                continue;
            }
            ModNetwork.sendLimitlessDomainAmbientSoundStart(nearby, casterUuid, cx, cy, cz);
        }
    }

    private static void broadcastLimitlessDomainAmbientSoundStop(ServerLevel level, UUID casterUuid)
    {
        for (ServerPlayer nearby : level.players())
        {
            ModNetwork.sendLimitlessDomainAmbientSoundStop(nearby, casterUuid);
        }
    }

    /** 球心锚点 + Photon 水墨扩散（与 {@link #LOCK_PLAYER_DURATION_TICKS} 后天空盒切换同刻）。 */
    private static void spawnLimitlessInkwashAnchorAtSphereCenter(ServerLevel level, BarrierInstance instance)
    {
        SpecialEffectEntity entity = ModEntities.SPECIAL_EFFECT_ENTITY.get().create(level);
        if (entity == null)
        {
            return;
        }
        BlockPos center = instance.getCenter();
        double x = center.getX() + 0.5D;
        double y = center.getY() + 0.5D;
        double z = center.getZ() + 0.5D;
        entity.moveTo(x, y, z, SOUTH_YAW, 0.0F);
        if (!level.addFreshEntity(entity))
        {
            return;
        }
        CommandSourceStack commandSource = entity.createCommandSourceStack()
                .withPermission(2)
                .withSuppressedOutput();
        MinecraftServer server = level.getServer();
        if (server != null)
        {
            server.getCommands().performPrefixedCommand(commandSource, PHOTON_INKWASH_COMMAND);
        }
    }

    /**
     * 与 {@link #isPlayerInsideBarrier(ServerPlayer, BarrierInstance)} 相同的球内判定：移除领域内全部特效实体。
     */
    private static void removeSpecialEffectEntitiesInsideBarrierSphere(ServerLevel level, BarrierInstance instance)
    {
        BlockPos c = instance.getCenter();
        int radius = instance.getRadius();
        double cx = c.getX() + 0.5D;
        double cy = c.getY() + 0.5D;
        double cz = c.getZ() + 0.5D;
        double radiusSquared = (double) radius * (double) radius;
        AABB box = new AABB(cx - radius, cy - radius, cz - radius, cx + radius, cy + radius, cz + radius);
        for (SpecialEffectEntity entity : level.getEntitiesOfClass(SpecialEffectEntity.class, box))
        {
            if (!entity.isAlive())
            {
                continue;
            }
            double dx = entity.getX() - cx;
            double dy = entity.getY() - cy;
            double dz = entity.getZ() - cz;
            if (dx * dx + dy * dy + dz * dz < radiusSquared)
            {
                entity.discard();
            }
        }
    }

    private static void applyLimitlessSkyboxTextureToInstance(ServerLevel level, BarrierInstance instance, ResourceLocation texture)
    {
        for (BlockPos pos : instance.getSkyboxBlocks())
        {
            if (level.getBlockEntity(pos) instanceof JJKZSkyblockEntity be)
            {
                be.setTextureFolder(texture);
            }
        }
        instance.setCustomSkyboxTexture(texture);
    }

    /**
     * 每 tick 维持领域锁定目标：构建完成起 280 刻内强制南侧散布位与朝南姿态，之后不再传送与锁朝向，仍施加失明等直至领域关闭。
     */
    private void tickLockedMobs(ServerPlayer owner)
    {
        ServerLevel level = owner.serverLevel();
        UUID ownerUuid = owner.getUUID();
        MinecraftServer server = level.getServer();
        int serverTick = server != null ? server.getTickCount() : 0;
        boolean enforceAnchorPose = !isLimitlessDomainMobAnchorPoseExpired(ownerUuid, serverTick);
        List<UUID> toRemove = new ArrayList<>();
        for (Map.Entry<UUID, MobLockState> entry : mobLockMap.entrySet())
        {
            MobLockState lockState = entry.getValue();
            if (!lockState.ownerUuid().equals(ownerUuid))
            {
                continue;
            }
            if (!(level.getEntity(entry.getKey()) instanceof LivingEntity target) || !target.isAlive())
            {
                releaseMobLock(entry.getKey(), lockState, level);
                toRemove.add(entry.getKey());
                continue;
            }
            if (isExcludedFromBeingLimitlessDomainMobTarget(owner, ownerUuid, target))
            {
                releaseMobLock(entry.getKey(), lockState, level);
                toRemove.add(entry.getKey());
                continue;
            }
            if (enforceAnchorPose)
            {
                lockEntityToSouth(target, lockState.lockPos());
            }
            applyLockBlindness(target);
            if (target instanceof Mob mob && lockState.aiPausedByThisLock())
            {
                mob.setNoAi(true);
            }
            if (target instanceof ServerPlayer targetPlayer)
            {
                sendGarbledDomainChat(targetPlayer);
            }
            mobLockMap.put(entry.getKey(), lockState.withIncrementedDomainTicks());
        }
        for (UUID uuid : toRemove)
        {
            mobLockMap.remove(uuid);
        }
    }

    /**
     * 施法者领域关闭后，释放该施法者关联的全部被锁定生物。
     * 转入 {@link #postDomainMobLockMap} 时：若关闭时刻已过「构建完成 +280 刻」解锁点，则延续阶段不再传送/锁朝南，仅保留失明、乱码、AI/输入锁直至计时结束。
     */
    private void clearOwnerMobLocksIfDomainClosed(ServerPlayer owner)
    {
        java.util.Deque<BarrierInstance> queue = ACTIVE_DOMAINS.get(owner.getUUID());
        if (queue != null && !queue.isEmpty())
        {
            return;
        }
        limitlessSessionMobWhitelist.remove(owner.getUUID());
        limitlessSkyboxSwapAtServerTick.remove(owner.getUUID());
        MinecraftServer closeServer = owner.getServer();
        int serverTickAtClose = closeServer != null ? closeServer.getTickCount() : 0;
        Integer dismissUnlockAt = limitlessDomainDismissUnlockServerTick.remove(owner.getUUID());
        boolean anchorPoseExpiredAtClose = dismissUnlockAt != null && serverTickAtClose >= dismissUnlockAt;
        List<UUID> toRelease = new ArrayList<>();
        for (Map.Entry<UUID, MobLockState> entry : mobLockMap.entrySet())
        {
            if (!entry.getValue().ownerUuid().equals(owner.getUUID()))
            {
                continue;
            }
            MobLockState state = entry.getValue();
            /* 后遗症脑损：停留秒数（tick/20）；仅玩家；无提示。 */
            LivingEntity locked = findLivingEntity(owner.getServer(), entry.getKey());
            if (locked instanceof ServerPlayer victim)
            {
                PlayerBrainDamageData.addFromLimitlessDomainDwell(victim, state.domainLockElapsedTicks(), false);
            }
            int postTicks = Math.min(state.domainLockElapsedTicks() * 3, POST_DOMAIN_LOCK_MAX_TICKS);
            if (postTicks > 0)
            {
                boolean enforcePostAnchorPose = !anchorPoseExpiredAtClose;
                postDomainMobLockMap.put(entry.getKey(), new PostDomainMobLockState(
                        state.lockPos(),
                        postTicks,
                        state.aiPausedByThisLock(),
                        state.originNoAi(),
                        enforcePostAnchorPose
                ));
            }
            else
            {
                releaseMobLock(entry.getKey(), state, owner.serverLevel());
            }
            toRelease.add(entry.getKey());
        }
        for (UUID uuid : toRelease)
        {
            mobLockMap.remove(uuid);
        }
    }

    private void lockEntityToSouth(LivingEntity entity, Vec3 lockPos)
    {
        entity.setDeltaMovement(Vec3.ZERO);
        entity.hurtMarked = true;
        entity.teleportTo(lockPos.x, lockPos.y, lockPos.z);
        entity.setYRot(SOUTH_YAW);
        entity.setYBodyRot(SOUTH_YAW);
        entity.setYHeadRot(SOUTH_YAW);
        if (entity instanceof ServerPlayer serverPlayer)
        {
            serverPlayer.connection.teleport(lockPos.x, lockPos.y, lockPos.z, SOUTH_YAW, serverPlayer.getXRot());
        }
    }

    /**
     * 持续刷新短时失明，确保被锁定目标在锁定期内保持失明状态。
     */
    private void applyLockBlindness(LivingEntity entity)
    {
        entity.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 12, 0, false, false, false));
    }

    /**
     * 向被锁定玩家聊天框持续灌入乱码文本，强化“无量空处”精神压制感。
     */
    private void sendGarbledDomainChat(ServerPlayer player)
    {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int segmentCount = 2 + random.nextInt(3); // 2~4 段随机拼接
        StringBuilder builder = new StringBuilder(128);
        for (int i = 0; i < segmentCount; i++)
        {
            if (i > 0)
            {
                builder.append("  ");
            }
            int style = random.nextInt(GARBLED_STYLE_COUNT);
            switch (style)
            {
                case 0 -> appendRandomChars(builder, GARBLED_SYMBOL_CHARS, 10, 20, random);
                case 1 -> appendRandomChars(builder, GARBLED_MIXED_CHARS, 12, 24, random);
                case 2 -> builder.append(GARBLED_CRASH_LINES[random.nextInt(GARBLED_CRASH_LINES.length)]);
                default -> appendRandomChars(builder, GARBLED_CN_CHARS, 8, 18, random);
            }
        }
        player.sendSystemMessage(Component.literal(builder.toString()).withStyle(ChatFormatting.DARK_PURPLE));
    }

    /**
     * 从指定字符池随机取样并拼接到消息中。
     */
    private void appendRandomChars(StringBuilder builder, char[] sourceChars, int minLen, int maxLen, ThreadLocalRandom random)
    {
        int len = minLen + random.nextInt(maxLen - minLen + 1);
        for (int i = 0; i < len; i++)
        {
            builder.append(sourceChars[random.nextInt(sourceChars.length)]);
        }
    }

    /**
     * 每服务器 tick 至多执行一次：延续锁定、近身演出锁定等（不依赖施法者是否在线）。
     */
    private void tickPostDomainLocksOncePerServerTick(ServerPlayer player)
    {
        MinecraftServer server = player.getServer();
        if (server == null)
        {
            return;
        }
        int tick = server.getTickCount();
        if (tick == lastPostDomainLockProcessedServerTick)
        {
            return;
        }
        lastPostDomainLockProcessedServerTick = tick;
        tickPostDomainLocks(server);
        tickNearCasterMobLocks(server);
    }

    private void tickPostDomainLocks(MinecraftServer server)
    {
        if (postDomainMobLockMap.isEmpty())
        {
            return;
        }
        List<UUID> toRemove = new ArrayList<>();
        for (Map.Entry<UUID, PostDomainMobLockState> entry : postDomainMobLockMap.entrySet())
        {
            UUID uuid = entry.getKey();
            PostDomainMobLockState state = entry.getValue();
            LivingEntity target = findLivingEntity(server, uuid);
            if (target == null || !target.isAlive())
            {
                releasePostDomainMobLock(uuid, state, server);
                toRemove.add(uuid);
                continue;
            }
            int remainingAfter = state.remainingTicks() - 1;
            if (state.enforceAnchorPose())
            {
                lockEntityToSouth(target, state.lockPos());
            }
            applyLockBlindness(target);
            if (target instanceof Mob mob && state.aiPausedByThisLock())
            {
                mob.setNoAi(true);
            }
            if (target instanceof ServerPlayer targetPlayer)
            {
                sendGarbledDomainChat(targetPlayer);
            }
            if (remainingAfter <= 0)
            {
                releasePostDomainMobLock(uuid, state, server);
                toRemove.add(uuid);
            }
            else
            {
                postDomainMobLockMap.put(uuid, state.withRemainingTicks(remainingAfter));
            }
        }
        for (UUID uuid : toRemove)
        {
            postDomainMobLockMap.remove(uuid);
        }
    }

    private static LivingEntity findLivingEntity(MinecraftServer server, UUID uuid)
    {
        for (ServerLevel level : server.getAllLevels())
        {
            Entity entity = level.getEntity(uuid);
            if (entity instanceof LivingEntity living)
            {
                return living;
            }
        }
        return null;
    }

    private void tickNearCasterMobLocks(MinecraftServer server)
    {
        if (nearCasterMobLockMap.isEmpty())
        {
            return;
        }
        List<UUID> toRemove = new ArrayList<>();
        for (Map.Entry<UUID, NearCasterMobLockState> entry : nearCasterMobLockMap.entrySet())
        {
            UUID uuid = entry.getKey();
            NearCasterMobLockState state = entry.getValue();
            LivingEntity target = findLivingEntity(server, uuid);
            if (target == null || !target.isAlive() || target instanceof ServerPlayer)
            {
                releaseNearCasterMobLock(uuid, state, server);
                toRemove.add(uuid);
                continue;
            }
            int remainingAfter = state.remainingTicks() - 1;
            applyCasterStyleLockToNonPlayerLiving(target, state.lockPos(), state.lockXRot());
            if (target instanceof Mob mob && state.aiPausedByThisLock())
            {
                mob.setNoAi(true);
            }
            if (remainingAfter <= 0)
            {
                releaseNearCasterMobLock(uuid, state, server);
                toRemove.add(uuid);
            }
            else
            {
                nearCasterMobLockMap.put(uuid, state.withRemainingTicks(remainingAfter));
            }
        }
        for (UUID uuid : toRemove)
        {
            nearCasterMobLockMap.remove(uuid);
        }
    }

    private void applyCasterStyleLockToNonPlayerLiving(LivingEntity entity, Vec3 lockPos, float lockXRot)
    {
        entity.setDeltaMovement(Vec3.ZERO);
        entity.hurtMarked = true;
        entity.teleportTo(lockPos.x, lockPos.y, lockPos.z);
        entity.setYRot(NORTH_YAW);
        entity.setYBodyRot(NORTH_YAW);
        entity.setYHeadRot(NORTH_YAW);
        entity.setXRot(lockXRot);
    }

    private void releaseNearCasterMobLock(UUID targetUuid, NearCasterMobLockState state, MinecraftServer server)
    {
        LivingEntity target = findLivingEntity(server, targetUuid);
        if (target instanceof Mob mob && state.aiPausedByThisLock())
        {
            mob.setNoAi(state.originNoAi());
        }
    }

    private void releasePostDomainMobLock(UUID targetUuid, PostDomainMobLockState state, MinecraftServer server)
    {
        LivingEntity target = findLivingEntity(server, targetUuid);
        if (target instanceof Mob mob && state.aiPausedByThisLock())
        {
            mob.setNoAi(state.originNoAi());
        }
        if (target instanceof ServerPlayer targetPlayer)
        {
            ModNetwork.sendPlayerInputLockSync(targetPlayer, false);
        }
        else
        {
            ServerPlayer onlinePlayer = server.getPlayerList().getPlayer(targetUuid);
            if (onlinePlayer != null)
            {
                ModNetwork.sendPlayerInputLockSync(onlinePlayer, false);
            }
        }
    }

    /**
     * 释放被锁定目标：恢复 AI（仅恢复本次锁定主动改动的对象）并解除玩家输入锁。
     */
    private void releaseMobLock(UUID targetUuid, MobLockState lockState, ServerLevel fallbackLevel)
    {
        if (fallbackLevel.getEntity(targetUuid) instanceof LivingEntity target)
        {
            if (target instanceof Mob mob && lockState.aiPausedByThisLock())
            {
                mob.setNoAi(lockState.originNoAi());
            }
            if (target instanceof ServerPlayer targetPlayer)
            {
                ModNetwork.sendPlayerInputLockSync(targetPlayer, false);
            }
        }
        else
        {
            ServerPlayer onlinePlayer = fallbackLevel.getServer().getPlayerList().getPlayer(targetUuid);
            if (onlinePlayer != null)
            {
                ModNetwork.sendPlayerInputLockSync(onlinePlayer, false);
            }
        }
    }

    private boolean isInsideDomainSphere(Vec3 entityPos, BlockPos center, double radiusSqr)
    {
        double dx = entityPos.x - (center.getX() + 0.5D);
        double dy = entityPos.y - (center.getY() + 0.5D);
        double dz = entityPos.z - (center.getZ() + 0.5D);
        return dx * dx + dy * dy + dz * dz < radiusSqr;
    }

    /**
     * 生成“north 方向附近”的分散站位，避免多个生物叠在同一方块。
     */
    private List<Vec3> buildScatterPositions(Vec3 anchor, int count)
    {
        List<Vec3> positions = new ArrayList<>(count);
        for (int i = 0; i < count; i++)
        {
            if (i == 0)
            {
                positions.add(anchor);
                continue;
            }
            double angle = Math.toRadians(i * 137.508D);
            double distance = MOB_SCATTER_SPACING * Math.sqrt(i);
            double x = anchor.x + Math.cos(angle) * distance;
            double z = anchor.z + Math.sin(angle) * distance;
            positions.add(new Vec3(x, anchor.y, z));
        }
        return positions;
    }

    private record PlayerLockState(Vec3 lockPos, float lockXRot, int remainingTicks)
    {
        private PlayerLockState decreaseOneTick()
        {
            return new PlayerLockState(lockPos, lockXRot, remainingTicks - 1);
        }
    }

    private record MobLockState(UUID ownerUuid, Vec3 lockPos, boolean aiPausedByThisLock, boolean originNoAi, int domainLockElapsedTicks)
    {
        private MobLockState withIncrementedDomainTicks()
        {
            return new MobLockState(ownerUuid, lockPos, aiPausedByThisLock, originNoAi, domainLockElapsedTicks + 1);
        }
    }

    private record PostDomainMobLockState(Vec3 lockPos, int remainingTicks, boolean aiPausedByThisLock, boolean originNoAi, boolean enforceAnchorPose)
    {
        private PostDomainMobLockState withRemainingTicks(int ticks)
        {
            return new PostDomainMobLockState(lockPos, ticks, aiPausedByThisLock, originNoAi, enforceAnchorPose);
        }
    }

    private record NearCasterMobLockState(Vec3 lockPos, float lockXRot, int remainingTicks, boolean aiPausedByThisLock, boolean originNoAi)
    {
        private NearCasterMobLockState withRemainingTicks(int ticks)
        {
            return new NearCasterMobLockState(lockPos, lockXRot, ticks, aiPausedByThisLock, originNoAi);
        }
    }
}
