package com.darkness.jujutsukaisenzone.technique.skill;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.compat.EpicFightCompat;
import com.darkness.jujutsukaisenzone.player.PlayerRatioBreakData;
import com.darkness.jujutsukaisenzone.player.PlayerTechniqueData;
import com.darkness.jujutsukaisenzone.registry.ModItemTags;
import com.darkness.jujutsukaisenzone.technique.TechniqueDamageSources;
import com.darkness.jujutsukaisenzone.technique.skillset.SkillDamageType;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 一文字斩：身前多段体术斩。消耗击破计数增加段数；利器基础段更多、间隔更短。
 * <p>
 * MVP 演出复用御厨子横斩动画；伤害由本类按刻驱动，不依赖动画关键帧。
 */
@Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class RatioIchimonjiLogic
{
    /** 四级体术基准点数（每段）。 */
    public static final float DAMAGE_PER_HIT = 5.0F;
    public static final double RANGE = 3.5D;
    public static final double HALF_WIDTH = 1.35D;
    /** 单次最多消耗的击破。 */
    public static final int MAX_BREAKS_CONSUMED = 4;
    public static final int BASE_HITS_BLUNT = 2;
    public static final int BASE_HITS_SHARP = 3;
    public static final int INTERVAL_BLUNT = 4;
    public static final int INTERVAL_SHARP = 3;

    private static final Map<UUID, SlashSession> SESSIONS = new ConcurrentHashMap<>();

    private RatioIchimonjiLogic()
    {
    }

    public static boolean isSlashing(ServerPlayer player)
    {
        return SESSIONS.containsKey(player.getUUID());
    }

    /**
     * 开始一文字斩。返回 false 时不应扣技能经济（调用方已在扣费前校验也可再防一次）。
     */
    public static boolean tryBegin(ServerPlayer player)
    {
        if (isSlashing(player))
        {
            player.displayClientMessage(
                    Component.literal("一文字斩进行中").withStyle(ChatFormatting.YELLOW),
                    true);
            return false;
        }

        boolean sharp = ModItemTags.isSharpWeapon(player.getMainHandItem());
        int consumed = PlayerRatioBreakData.consume(player, MAX_BREAKS_CONSUMED);
        int baseHits = sharp ? BASE_HITS_SHARP : BASE_HITS_BLUNT;
        int hits = baseHits + consumed;
        int interval = sharp ? INTERVAL_SHARP : INTERVAL_BLUNT;
        long now = player.level().getGameTime();

        SESSIONS.put(player.getUUID(), new SlashSession(hits, interval, now, sharp));

        EpicFightCompat.playSlashKai2Animation(player);
        player.displayClientMessage(
                Component.literal("一文字斩 ×" + hits + " 段"
                                + (consumed > 0 ? "（击破 -" + consumed + "）" : "")
                                + (sharp ? " · 利器" : ""))
                        .withStyle(ChatFormatting.GOLD),
                true);

        // 立刻第一刀，后续由 tick 推进
        performHit(player, SESSIONS.get(player.getUUID()));
        SlashSession after = SESSIONS.get(player.getUUID());
        if (after != null && after.hitsRemaining <= 0)
        {
            SESSIONS.remove(player.getUUID());
        }
        return true;
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide)
        {
            return;
        }
        if (!(event.player instanceof ServerPlayer player))
        {
            return;
        }
        SlashSession session = SESSIONS.get(player.getUUID());
        if (session == null)
        {
            return;
        }
        if (!player.isAlive() || player.isSpectator() || !PlayerTechniqueData.isRatio(player))
        {
            SESSIONS.remove(player.getUUID());
            return;
        }
        long now = player.level().getGameTime();
        if (now < session.nextHitGameTime)
        {
            return;
        }
        if (session.hitsRemaining <= 0)
        {
            SESSIONS.remove(player.getUUID());
            return;
        }
        performHit(player, session);
        if (session.hitsRemaining <= 0)
        {
            SESSIONS.remove(player.getUUID());
        }
    }

    private static void performHit(ServerPlayer player, SlashSession session)
    {
        session.hitsRemaining--;
        session.nextHitGameTime = player.level().getGameTime() + session.intervalTicks;

        ServerLevel level = player.serverLevel();
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        AABB box = player.getBoundingBox()
                .expandTowards(look.scale(RANGE))
                .inflate(HALF_WIDTH, 0.6D, HALF_WIDTH);

        int hitCount = 0;
        for (LivingEntity living : level.getEntitiesOfClass(
                LivingEntity.class,
                box,
                entity -> entity.isAlive()
                        && !entity.isRemoved()
                        && entity.isAttackable()
                        && !entity.getUUID().equals(player.getUUID())
                        && !entity.isSpectator()
                        && entity.isPickable()))
        {
            Vec3 to = living.getEyePosition().subtract(eye);
            double dist = to.length();
            if (dist < 0.01D || dist > RANGE + living.getBbWidth())
            {
                continue;
            }
            if (to.normalize().dot(look) < 0.45D)
            {
                continue;
            }
            if (TechniqueDamageSources.deal(living, player, DAMAGE_PER_HIT, SkillDamageType.MARTIAL))
            {
                hitCount++;
            }
        }

        Vec3 tip = eye.add(look.scale(RANGE * 0.65D));
        level.sendParticles(
                ParticleTypes.SWEEP_ATTACK,
                tip.x, tip.y, tip.z,
                1,
                0.0D, 0.0D, 0.0D,
                0.0D);
        level.sendParticles(
                ParticleTypes.CRIT,
                tip.x, tip.y, tip.z,
                6,
                0.25D, 0.15D, 0.25D,
                0.02D);
        level.playSound(
                null,
                player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP,
                SoundSource.PLAYERS,
                0.7F,
                session.sharp ? 1.15F : 0.95F);

        if (hitCount <= 0 && session.hitsRemaining == 0)
        {
            // 全程空挥不额外提示
        }
    }

    /** 登出 / 维度异常时清会话，避免残留。 */
    public static void clear(ServerPlayer player)
    {
        SESSIONS.remove(player.getUUID());
    }

    public static void clearAll()
    {
        SESSIONS.clear();
    }

    private static final class SlashSession
    {
        private int hitsRemaining;
        private final int intervalTicks;
        private long nextHitGameTime;
        private final boolean sharp;

        private SlashSession(int hits, int intervalTicks, long now, boolean sharp)
        {
            this.hitsRemaining = hits;
            this.intervalTicks = intervalTicks;
            // 第一刀由 tryBegin 立刻打，下一刀从 now+interval 起
            this.nextHitGameTime = now + intervalTicks;
            this.sharp = sharp;
        }
    }
}
