package com.darkness.jujutsukaisenzone.compat;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;
import yesman.epicfight.world.capabilities.item.CapabilityItem;

public final class EpicFightCompat
{
    public static final float HEAVENLY_BURST_STAMINA_COST = 6.0F;
    public static final float AERIAL_MULTI_JUMP_STAMINA_COST = 2.0F;

    private EpicFightCompat()
    {
    }

    /**
     * 是否处于 Epic Fight 的战斗模式（EPICFIGHT）。
     */
    public static boolean isBattleMode(Player player)
    {
        PlayerPatch<?> patch = EpicFightCapabilities.getPlayerPatch(player);
        return patch != null && patch.isEpicFightMode();
    }

    /** 切换 Epic Fight 战斗模式；{@code battleMode=true} 为战斗模式，{@code false} 为原版模式。 */
    public static void setBattleMode(ServerPlayer player, boolean battleMode)
    {
        ServerPlayerPatch patch = EpicFightCapabilities.getEntityPatch(player, ServerPlayerPatch.class);
        if (patch == null)
        {
            return;
        }
        if (battleMode)
        {
            patch.toEpicFightMode(true);
        }
        else
        {
            patch.toVanillaMode(true);
        }
    }

    /**
     * 无量空处蓄力开始：播放非循环 Blender 导出动画（需已注册 {@link JjkzEpicFightAnimations#LIMITLESS_DOMAIN_CHARGE}）。
     */
    public static void playLimitlessDomainChargeAnimation(ServerPlayer player)
    {
        ServerPlayerPatch patch = EpicFightCapabilities.getEntityPatch(player, ServerPlayerPatch.class);
        if (patch != null && JjkzEpicFightAnimations.LIMITLESS_DOMAIN_CHARGE != null)
        {
            patch.playAnimationInstantly(JjkzEpicFightAnimations.LIMITLESS_DOMAIN_CHARGE);
        }
    }

    /**
     * 蓄力取消、被迫中断或非自愿清场时停止蓄力.pose 动画。
     * 注意：不要在蓄力倒计时正常结束调用 {@link com.darkness.jujutsukaisenzone.technique.domain.LimitlessDomainExpansion#tickCastingDelay super.cast}
     * 之前调用——否则长于蓄力窗口（如 8s）的动画会被截断为约 {@code CAST_DELAY_TICKS}。
     */
    public static void stopLimitlessDomainChargeAnimation(ServerPlayer player)
    {
        ServerPlayerPatch patch = EpicFightCapabilities.getEntityPatch(player, ServerPlayerPatch.class);
        if (patch != null && JjkzEpicFightAnimations.LIMITLESS_DOMAIN_CHARGE != null)
        {
            patch.stopPlaying(JjkzEpicFightAnimations.LIMITLESS_DOMAIN_CHARGE);
        }
    }

    /**
     * 伏魔神龛领域展开演出开始：播放非循环 Blender 导出动画（需已注册 {@link JjkzEpicFightAnimations#SHRINE_DOMAIN_EXPANSION}）。
     */
    public static void playShrineDomainExpansionAnimation(ServerPlayer player)
    {
        ServerPlayerPatch patch = EpicFightCapabilities.getEntityPatch(player, ServerPlayerPatch.class);
        if (patch != null && JjkzEpicFightAnimations.SHRINE_DOMAIN_EXPANSION != null)
        {
            patch.playAnimationInstantly(JjkzEpicFightAnimations.SHRINE_DOMAIN_EXPANSION);
        }
    }

    /**
     * 蓄力取消或被迫中断时停止伏魔神龛领域展开演出动画。
     * 正常蓄力结束、即将展开领域前不要调用，以免截断长于蓄力窗口的演出动画。
     */
    public static void stopShrineDomainExpansionAnimation(ServerPlayer player)
    {
        ServerPlayerPatch patch = EpicFightCapabilities.getEntityPatch(player, ServerPlayerPatch.class);
        if (patch != null && JjkzEpicFightAnimations.SHRINE_DOMAIN_EXPANSION != null)
        {
            patch.stopPlaying(JjkzEpicFightAnimations.SHRINE_DOMAIN_EXPANSION);
        }
    }

    /**
     * 诛伏赐死领域结界构建完成：播放 {@code lookdown} 玩家动画（需已注册 {@link JjkzEpicFightAnimations#JUDGMENT_DOMAIN_LOOKDOWN}）。
     */
    public static void playJudgmentDomainLookdownAnimation(ServerPlayer player)
    {
        ServerPlayerPatch patch = EpicFightCapabilities.getEntityPatch(player, ServerPlayerPatch.class);
        if (patch != null && JjkzEpicFightAnimations.JUDGMENT_DOMAIN_LOOKDOWN != null)
        {
            patch.playAnimationInstantly(JjkzEpicFightAnimations.JUDGMENT_DOMAIN_LOOKDOWN);
        }
    }

    /**
     * 「开始审判」开庭演出：播放 {@code swing_the_gavel} 玩家动画。
     */
    public static void playSwingTheGavelAnimation(ServerPlayer player)
    {
        ServerPlayerPatch patch = EpicFightCapabilities.getEntityPatch(player, ServerPlayerPatch.class);
        if (patch != null && JjkzEpicFightAnimations.SWING_THE_GAVEL != null)
        {
            patch.playAnimationInstantly(JjkzEpicFightAnimations.SWING_THE_GAVEL);
        }
    }

    /**
     * 解（御厨子术式槽位1）招式演出：播放非循环斩击动画（竖斩）。
     */
    public static void playSlashKai1Animation(ServerPlayer player)
    {
        ServerPlayerPatch patch = EpicFightCapabilities.getEntityPatch(player, ServerPlayerPatch.class);
        if (patch != null && JjkzEpicFightAnimations.SLASH_KAI_1 != null)
        {
            patch.playAnimationInstantly(JjkzEpicFightAnimations.SLASH_KAI_1);
        }
    }

    /**
     * 解（御厨子术式槽位1）招式演出：播放非循环斩击动画（横斩）。
     */
    public static void playSlashKai2Animation(ServerPlayer player)
    {
        ServerPlayerPatch patch = EpicFightCapabilities.getEntityPatch(player, ServerPlayerPatch.class);
        if (patch != null && JjkzEpicFightAnimations.SLASH_KAI_2 != null)
        {
            patch.playAnimationInstantly(JjkzEpicFightAnimations.SLASH_KAI_2);
        }
    }

    /**
     * 捌（御厨子术式）：播放非循环斩击动画。
     */
    public static void playSlashHachiAnimation(ServerPlayer player)
    {
        ServerPlayerPatch patch = EpicFightCapabilities.getEntityPatch(player, ServerPlayerPatch.class);
        if (patch != null && JjkzEpicFightAnimations.SLASH_HACHI != null)
        {
            patch.playAnimationInstantly(JjkzEpicFightAnimations.SLASH_HACHI);
        }
    }

    /**
     * 停止解招式演出动画。
     */
    public static void stopSlashKai1Animation(ServerPlayer player)
    {
        ServerPlayerPatch patch = EpicFightCapabilities.getEntityPatch(player, ServerPlayerPatch.class);
        if (patch != null && JjkzEpicFightAnimations.SLASH_KAI_1 != null)
        {
            patch.stopPlaying(JjkzEpicFightAnimations.SLASH_KAI_1);
        }
    }

    /**
     * 「赫」发射前摇：播放 {@link JjkzEpicFightAnimations#AKA_SHORT}。
     */
    public static void playLimitlessAkaShortAnimation(ServerPlayer player)
    {
        ServerPlayerPatch patch = EpicFightCapabilities.getEntityPatch(player, ServerPlayerPatch.class);
        if (patch != null && JjkzEpicFightAnimations.AKA_SHORT != null)
        {
            patch.playAnimationInstantly(JjkzEpicFightAnimations.AKA_SHORT);
        }
    }

    /**
     * 超远程虚式茈起手：播放 {@link JjkzEpicFightAnimations#REMOTE_MURASAKI_START}。
     */
    public static void playRemoteMurasakiStartAnimation(ServerPlayer player)
    {
        ServerPlayerPatch patch = EpicFightCapabilities.getEntityPatch(player, ServerPlayerPatch.class);
        if (patch != null && JjkzEpicFightAnimations.REMOTE_MURASAKI_START != null)
        {
            patch.playAnimationInstantly(JjkzEpicFightAnimations.REMOTE_MURASAKI_START);
        }
    }

    /**
     * 超远程虚式茈起手取消或进入发射阶段时立即停止起手动画。
     */
    public static void stopRemoteMurasakiStartAnimation(ServerPlayer player)
    {
        ServerPlayerPatch patch = EpicFightCapabilities.getEntityPatch(player, ServerPlayerPatch.class);
        if (patch != null && JjkzEpicFightAnimations.REMOTE_MURASAKI_START != null)
        {
            patch.stopPlaying(JjkzEpicFightAnimations.REMOTE_MURASAKI_START);
        }
    }

    /**
     * 超远程虚式茈发射：播放 {@link JjkzEpicFightAnimations#REMOTE_MURASAKI_FIRE}。
     */
    public static void playRemoteMurasakiFireAnimation(ServerPlayer player)
    {
        ServerPlayerPatch patch = EpicFightCapabilities.getEntityPatch(player, ServerPlayerPatch.class);
        if (patch != null && JjkzEpicFightAnimations.REMOTE_MURASAKI_FIRE != null)
        {
            patch.playAnimationInstantly(JjkzEpicFightAnimations.REMOTE_MURASAKI_FIRE);
        }
    }

    /**
     * 「灶」开长按蓄力开始：播放 {@link JjkzEpicFightAnimations#FUGA_CHARGE}。
     */
    public static void playFugaChargeAnimation(ServerPlayer player)
    {
        ServerPlayerPatch patch = EpicFightCapabilities.getEntityPatch(player, ServerPlayerPatch.class);
        if (patch != null && JjkzEpicFightAnimations.FUGA_CHARGE != null)
        {
            patch.playAnimationInstantly(JjkzEpicFightAnimations.FUGA_CHARGE);
        }
    }

    /**
     * 「灶」开发射或蓄力取消时立即停止蓄力动画。
     */
    public static void stopFugaChargeAnimation(ServerPlayer player)
    {
        ServerPlayerPatch patch = EpicFightCapabilities.getEntityPatch(player, ServerPlayerPatch.class);
        if (patch != null && JjkzEpicFightAnimations.FUGA_CHARGE != null)
        {
            patch.stopPlaying(JjkzEpicFightAnimations.FUGA_CHARGE);
        }
    }

    /**
     * 「飞行，穿越」起手：播放 {@link JjkzEpicFightAnimations#FLYING_TURN}（飞踢前摇）。
     * <p>
     * 起手动画铺满整段飞行（约 26 帧 ≈ 0.43 秒，与最长飞行时长同量级），
     * 抵达时由 {@link #stopFlyingTurnAnimation} 停掉再切踢击。
     */
    public static void playFlyingTurnAnimation(ServerPlayer player)
    {
        ServerPlayerPatch patch = EpicFightCapabilities.getEntityPatch(player, ServerPlayerPatch.class);
        if (patch != null && JjkzEpicFightAnimations.FLYING_TURN != null)
        {
            patch.playAnimationInstantly(JjkzEpicFightAnimations.FLYING_TURN);
        }
    }

    /** 停止「飞行，穿越」起手动画（穿越中止或切到踢击时调用）。 */
    public static void stopFlyingTurnAnimation(ServerPlayer player)
    {
        ServerPlayerPatch patch = EpicFightCapabilities.getEntityPatch(player, ServerPlayerPatch.class);
        if (patch != null && JjkzEpicFightAnimations.FLYING_TURN != null)
        {
            patch.stopPlaying(JjkzEpicFightAnimations.FLYING_TURN);
        }
    }

    /** 「飞行，穿越」踢击：抵达目标时播放 {@link JjkzEpicFightAnimations#FLYING_TURN_KICK}。 */
    public static void playFlyingTurnKickAnimation(ServerPlayer player)
    {
        ServerPlayerPatch patch = EpicFightCapabilities.getEntityPatch(player, ServerPlayerPatch.class);
        if (patch != null && JjkzEpicFightAnimations.FLYING_TURN_KICK != null)
        {
            patch.playAnimationInstantly(JjkzEpicFightAnimations.FLYING_TURN_KICK);
        }
    }

    public static boolean tryConsumeHeavenlyBurstStamina(Player player)
    {
        ServerPlayerPatch patch = EpicFightCapabilities.getEntityPatch(player, ServerPlayerPatch.class);
        if (patch == null)
        {
            return false;
        }

        float actualCost = patch.getModifiedStaminaConsume(HEAVENLY_BURST_STAMINA_COST);
        if (!patch.hasStamina(actualCost))
        {
            return false;
        }

        patch.resetActionTick();
        patch.setStamina(patch.getStamina() - actualCost);
        return true;
    }

    public static boolean tryConsumeAerialMultiJumpStamina(Player player)
    {
        ServerPlayerPatch patch = EpicFightCapabilities.getEntityPatch(player, ServerPlayerPatch.class);
        if (patch == null)
        {
            return false;
        }

        float actualCost = patch.getModifiedStaminaConsume(AERIAL_MULTI_JUMP_STAMINA_COST);
        if (!patch.hasStamina(actualCost))
        {
            return false;
        }

        patch.resetActionTick();
        patch.setStamina(patch.getStamina() - actualCost);
        return true;
    }

    /** 物品是否注册了 Epic Fight 武器 capability。 */
    public static boolean isEpicFightWeapon(ItemStack stack)
    {
        return EpicFightCapabilities.getItemCapability(stack).isPresent();
    }

    /**
     * 法槌形态变更后刷新 Epic Fight 武器类型。
     * EF 在 capability 首次挂载时即固定类型，仅改 NBT 不会更新，需换栈并通知 PlayerPatch。
     */
    public static void refreshGavelWeaponHeldItem(
            ServerPlayer player,
            InteractionHand hand,
            ItemStack oldStack,
            ItemStack newStack)
    {
        if (!isBattleMode(player))
        {
            return;
        }
        ServerPlayerPatch patch = EpicFightCapabilities.getEntityPatch(player, ServerPlayerPatch.class);
        if (patch == null)
        {
            return;
        }
        CapabilityItem oldCap = patch.getHoldingItemCapability(hand);
        CapabilityItem newCap = EpicFightCapabilities.getItemStackCapability(newStack);
        patch.updateHeldItem(oldCap, newCap, oldStack, newStack, hand);
        patch.modifyLivingMotionByCurrentItem(true);
    }
}
