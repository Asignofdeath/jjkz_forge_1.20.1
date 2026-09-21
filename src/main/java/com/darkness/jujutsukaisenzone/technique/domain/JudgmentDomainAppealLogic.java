package com.darkness.jujutsukaisenzone.technique.domain;

import com.darkness.jujutsukaisenzone.effect.ConfiscationEffect;
import com.darkness.jujutsukaisenzone.effect.DeathSentenceEffect;
import com.darkness.jujutsukaisenzone.technique.skill.JudgmentCrimeLibrary;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 诛伏赐死上诉：领域关闭后 1 分钟内，被告可发起即时领域单人重审；无罪则撤销死刑/没收。
 */
public final class JudgmentDomainAppealLogic
{
    /** 上诉窗口期（tick），1 分钟 = 1200 tick。 */
    public static final int APPEAL_WINDOW_TICKS = 20 * 60;
    /** 非玩家被告自动上诉概率（%）。 */
    public static final double MOB_APPEAL_CHANCE_PERCENT = 35.0D;

    private record DeferredAppealCandidate(UUID defendantUuid, String crimeChinese)
    {
    }

    private record ActiveAppealWindow(
            UUID casterUuid,
            String crimeChinese,
            int remainingTicks,
            int mobAppealRollTick)
    {
    }

    private static final ConcurrentMap<UUID, List<DeferredAppealCandidate>> deferredByCaster =
            new ConcurrentHashMap<>();
    private static final ConcurrentMap<UUID, ActiveAppealWindow> activeWindowByDefendant =
            new ConcurrentHashMap<>();

    private JudgmentDomainAppealLogic()
    {
    }

    public static void deferAppealEligibility(
            ServerPlayer caster,
            LivingEntity defendant,
            String crimeChinese)
    {
        if (caster == null || defendant == null || JudgmentDomainTrialLogic.isAppealRetrialSession(caster))
        {
            return;
        }

        deferredByCaster.computeIfAbsent(caster.getUUID(), ignored -> new ArrayList<>())
                .add(new DeferredAppealCandidate(
                        defendant.getUUID(),
                        crimeChinese != null ? crimeChinese : ""));
    }

    public static void activateDeferredAppeals(ServerPlayer caster)
    {
        if (caster == null || JudgmentDomainTrialLogic.isAppealRetrialSession(caster))
        {
            return;
        }

        List<DeferredAppealCandidate> candidates = deferredByCaster.remove(caster.getUUID());
        if (candidates == null || candidates.isEmpty())
        {
            return;
        }

        RandomSource random = caster.getRandom();
        for (DeferredAppealCandidate candidate : candidates)
        {
            if (activeWindowByDefendant.containsKey(candidate.defendantUuid()))
            {
                continue;
            }

            LivingEntity defendant = resolveDefendant(caster, candidate.defendantUuid());
            if (defendant == null)
            {
                continue;
            }

            int mobRollTick = defendant instanceof ServerPlayer
                    ? -1
                    : random.nextInt(APPEAL_WINDOW_TICKS);
            activeWindowByDefendant.put(
                    candidate.defendantUuid(),
                    new ActiveAppealWindow(
                            caster.getUUID(),
                            candidate.crimeChinese(),
                            APPEAL_WINDOW_TICKS,
                            mobRollTick));

            if (defendant instanceof ServerPlayer playerDefendant)
            {
                playerDefendant.sendSystemMessage(Component.literal(
                                "你有 1 分钟内向聊天栏发送「上诉」，以请求对本次裁决进行单人重审。")
                        .withStyle(ChatFormatting.GOLD));
            }
        }
    }

    public static void clearForCaster(UUID casterUuid)
    {
        deferredByCaster.remove(casterUuid);
        activeWindowByDefendant.entrySet().removeIf(entry -> entry.getValue().casterUuid().equals(casterUuid));
    }

    public static void clearForDefendant(UUID defendantUuid)
    {
        activeWindowByDefendant.remove(defendantUuid);
    }

    public static void tick(ServerPlayer anyPlayer)
    {
        if (anyPlayer == null || anyPlayer.level().isClientSide())
        {
            return;
        }

        MinecraftServer server = anyPlayer.server;
        List<UUID> expired = new ArrayList<>();
        for (var entry : activeWindowByDefendant.entrySet())
        {
            UUID defendantUuid = entry.getKey();
            ActiveAppealWindow window = entry.getValue();
            LivingEntity defendant = resolveDefendant(server, window.casterUuid(), defendantUuid);
            if (defendant == null)
            {
                expired.add(defendantUuid);
                continue;
            }

            int elapsed = APPEAL_WINDOW_TICKS - window.remainingTicks();
            if (!(defendant instanceof ServerPlayer)
                    && window.mobAppealRollTick() >= 0
                    && elapsed == window.mobAppealRollTick())
            {
                if (defendant.getRandom().nextDouble() * 100.0D < MOB_APPEAL_CHANCE_PERCENT)
                {
                    tryExecuteAppeal(defendant, window);
                    expired.add(defendantUuid);
                    continue;
                }
            }

            if (window.remainingTicks() <= 1)
            {
                expired.add(defendantUuid);
                continue;
            }

            activeWindowByDefendant.put(
                    defendantUuid,
                    new ActiveAppealWindow(
                            window.casterUuid(),
                            window.crimeChinese(),
                            window.remainingTicks() - 1,
                            window.mobAppealRollTick()));
        }

        for (UUID defendantUuid : expired)
        {
            activeWindowByDefendant.remove(defendantUuid);
        }
    }

    public static boolean tryPlayerAppeal(ServerPlayer defendant, String message)
    {
        if (defendant == null || message == null || !message.trim().equals("上诉"))
        {
            return false;
        }

        ActiveAppealWindow window = activeWindowByDefendant.get(defendant.getUUID());
        if (window == null)
        {
            defendant.sendSystemMessage(Component.literal("当前无法发起上诉。").withStyle(ChatFormatting.RED));
            return false;
        }

        return tryExecuteAppeal(defendant, window);
    }

    private static boolean tryExecuteAppeal(LivingEntity defendant, ActiveAppealWindow window)
    {
        if (!(defendant.level() instanceof net.minecraft.server.level.ServerLevel defendantLevel))
        {
            return false;
        }
        MinecraftServer server = defendantLevel.getServer();
        ServerPlayer caster = server.getPlayerList().getPlayer(window.casterUuid());
        if (caster == null || !caster.isAlive())
        {
            if (defendant instanceof ServerPlayer playerDefendant)
            {
                playerDefendant.sendSystemMessage(Component.literal("上诉失败：审判者不在线。")
                        .withStyle(ChatFormatting.RED));
            }
            return false;
        }

        if (JudgmentDomainExpansion.getInstance().hasActiveCompletedDomain(caster)
                || JudgmentDomainExpansion.getInstance().isCasting(caster))
        {
            if (defendant instanceof ServerPlayer playerDefendant)
            {
                playerDefendant.sendSystemMessage(Component.literal("上诉失败：审判者正在展开或维持领域。")
                        .withStyle(ChatFormatting.RED));
            }
            return false;
        }

        activeWindowByDefendant.remove(defendant.getUUID());
        JudgmentCrimeLibrary.CrimeEntry crime = resolveAppealCrime(window.crimeChinese());
        if (!JudgmentDomainExpansion.getInstance().castAppealDomain(caster, defendant, crime))
        {
            if (defendant instanceof ServerPlayer playerDefendant)
            {
                playerDefendant.sendSystemMessage(Component.literal("上诉失败：无法展开重审领域。")
                        .withStyle(ChatFormatting.RED));
            }
            return false;
        }

        broadcastAppealOpened(caster, defendant);
        return true;
    }

    private static JudgmentCrimeLibrary.CrimeEntry resolveAppealCrime(String crimeChinese)
    {
        if (crimeChinese == null || crimeChinese.isBlank())
        {
            return new JudgmentCrimeLibrary.CrimeEntry("上诉重审", "Appeal Retrial");
        }
        return new JudgmentCrimeLibrary.CrimeEntry(crimeChinese, crimeChinese);
    }

    private static void broadcastAppealOpened(ServerPlayer caster, LivingEntity defendant)
    {
        Component message = Component.literal("【上诉】被告发起上诉，诛伏赐死即刻重开，展开单人审理。")
                .withStyle(ChatFormatting.GOLD);
        caster.sendSystemMessage(message);
        if (defendant instanceof ServerPlayer playerDefendant && !playerDefendant.getUUID().equals(caster.getUUID()))
        {
            playerDefendant.sendSystemMessage(message);
        }
    }

    public static void revokeDomainPunishments(LivingEntity defendant)
    {
        if (defendant == null)
        {
            return;
        }
        DeathSentenceEffect.remove(defendant);
        ConfiscationEffect.remove(defendant);
    }

    private static LivingEntity resolveDefendant(ServerPlayer caster, UUID defendantUuid)
    {
        return resolveDefendant(caster.server, caster.getUUID(), defendantUuid);
    }

    private static LivingEntity resolveDefendant(MinecraftServer server, UUID casterUuid, UUID defendantUuid)
    {
        ServerPlayer caster = server.getPlayerList().getPlayer(casterUuid);
        if (caster == null)
        {
            return null;
        }
        if (caster.getUUID().equals(defendantUuid))
        {
            return caster;
        }
        if (caster.serverLevel().getEntity(defendantUuid) instanceof LivingEntity living
                && living.isAlive()
                && !living.isRemoved())
        {
            return living;
        }
        return null;
    }
}
