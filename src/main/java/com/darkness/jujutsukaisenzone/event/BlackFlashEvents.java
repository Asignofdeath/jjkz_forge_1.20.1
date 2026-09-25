package com.darkness.jujutsukaisenzone.event;

import com.darkness.jujutsukaisenzone.Config;
import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.compat.EpicFightCompat;
import com.darkness.jujutsukaisenzone.player.PlayerBlackFlashData;
import com.darkness.jujutsukaisenzone.player.PlayerCursedEnergyData;
import com.darkness.jujutsukaisenzone.player.PlayerCursedGradeData;
import com.darkness.jujutsukaisenzone.player.PlayerShrineBlackFlashMilestoneData;
import com.darkness.jujutsukaisenzone.player.PlayerTechniqueData;
import com.darkness.jujutsukaisenzone.technique.SkillDamageResolver;
import com.darkness.jujutsukaisenzone.technique.TechniqueDamageBoosts;
import com.darkness.jujutsukaisenzone.technique.development.SkillUnlockLogic;
import com.darkness.jujutsukaisenzone.technique.skillset.ShrineTechniqueSkillSet;
import com.darkness.jujutsukaisenzone.entity.SpecialEffectEntity;
import com.darkness.jujutsukaisenzone.entity.data.BlackFlashConfig;
import com.darkness.jujutsukaisenzone.effect.RushIntoTheZoneClimaxEffect;
import com.darkness.jujutsukaisenzone.registry.ModEffects;
import com.darkness.jujutsukaisenzone.registry.ModEntities;
import com.darkness.jujutsukaisenzone.registry.ModSounds;
import com.darkness.jujutsukaisenzone.network.ModNetwork;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;
import yesman.epicfight.world.damagesource.EpicFightDamageSource;
import yesman.epicfight.world.entity.eventlistener.DodgeSuccessEvent;
import yesman.epicfight.world.entity.eventlistener.PlayerEventListener.EventType;

import java.util.UUID;

/**
 * 黑闪（Black Flash）事件检测。
 * <p>
 * 监听四个触发源：
 * <ol>
 *   <li>体术命中 —— {@link LivingHurtEvent}，攻击者为战斗模式中的玩家，且攻击类型为体术</li>
 *   <li>击杀 —— {@link LivingDeathEvent}，攻击者为战斗模式中的玩家</li>
 *   <li>残血 —— 在命中时即时检查</li>
 *   <li>闪避体术 —— 通过 EpicFight 的 {@link DodgeSuccessEvent} 检测</li>
 * </ol>
 */
@Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class BlackFlashEvents
{
    /** 向 EpicFight 注册闪避事件监听器时使用的固定 UUID，避免重复注册。 */
    private static final UUID DODGE_EVENT_UUID = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890");

    /** ZONE 攻击伤害加成的属性修饰符 UUID。 */
    private static final UUID ZONE_ATTACK_MODIFIER_UUID = UUID.fromString("b2c3d4e5-f6a7-8901-bcde-f12345678901");

    private BlackFlashEvents()
    {
    }

    // ======== 体术命中检测 ========

    /**
     * 玩家在战斗模式下用体术攻击命中目标。
     * <p>
     * 在此判定黑闪触发概率并执行效果。
     * 注意：仅服务端执行（FORGE 总线自动分端）。
     */
    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event)
    {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide)
        {
            return;
        }

        // （Zone 伤害吸收已迁入 TechniqueDamageZoneEvents 统一乘区管线，判定与数值不变）

        // 取走（并清空）本事件之前记下的「强度放大前的原始伤害」，供黑闪取幂使用。
        // 必须在本方法最开头取：无论最终是否触发黑闪都要清空，避免残留值污染下一次结算。
        float rawBeforePower = SkillDamageResolver.consumeRawBeforePower(event.getAmount());

        // 受击打断：被体术命中的玩家连击清零（只对 EpicFight 战斗模式中的有效体术攻击生效）
        if (target instanceof ServerPlayer victim
                && EpicFightCompat.isBattleMode(victim)
                && event.getSource() instanceof EpicFightDamageSource efVictimSource
                && efVictimSource.isBasicAttack())
        {
            PlayerBlackFlashData.resetCombo(victim);
        }

        // 攻击者必须是 ServerPlayer
        if (!(event.getSource().getEntity() instanceof ServerPlayer attacker))
        {
            return;
        }

        // 必须在 EpicFight 战斗模式
        if (!EpicFightCompat.isBattleMode(attacker))
        {
            return;
        }

        // 必须是体术攻击（拳脚或武器近战）
        if (!(event.getSource() instanceof EpicFightDamageSource efSource) || !efSource.isBasicAttack())
        {
            return;
        }

        // 咒力要求：当前咒力不低于 100
        if (PlayerCursedEnergyData.getCurrent(attacker) < 100)
        {
            return;
        }

        // ---- 此时确认：玩家体术命中 ----

        // Step 1: 连击计数
        int comboWindow = Config.blackFlashComboWindowTicks;
        if (PlayerBlackFlashData.isInComboWindow(attacker, comboWindow))
        {
            PlayerBlackFlashData.incrementCombo(attacker);
        }
        else
        {
            PlayerBlackFlashData.setComboCount(attacker, 1);
        }
        PlayerBlackFlashData.setLastHitTick(attacker, attacker.tickCount);

        // Step 1.5: Rush Into The ZONE 最高潮 — 下一次体术攻击必定黑闪（无视触发冷却）
        if (PlayerBlackFlashData.hasGuaranteedBlackFlash(attacker))
        {
            RushIntoTheZoneClimaxEffect.consumeGuaranteedBlackFlash(attacker);
            triggerBlackFlash(attacker, target, event, rawBeforePower);
            return;
        }

        // Step 1.6: 检查黑闪触发冷却（触发后 2 秒内不可再次触发）
        if (PlayerBlackFlashData.isInTriggerCooldown(attacker, Config.blackFlashTriggerCooldownTicks))
        {
            return;
        }

        // Step 2: 计算概率
        double targetLowHpBonus = target.getMaxHealth() > 0.0F
                && target.getHealth() / target.getMaxHealth() <= Config.blackFlashTargetLowHpThreshold
                ? Config.blackFlashTargetLowHpBonus
                : 0.0D;
        double chance = PlayerBlackFlashData.calculateChance(
                attacker,
                Config.blackFlashBaseChance,
                Config.blackFlashComboBonusPerHit,
                Config.blackFlashKillBonus,
                Config.blackFlashLowHpThreshold,
                Config.blackFlashLowHpBonus,
                Config.blackFlashDodgeBonus,
                Config.blackFlashComboWindowTicks,
                Config.blackFlashKillWindowTicks,
                Config.blackFlashDodgeWindowTicks,
                Config.blackFlashZoneBonusI,
                Config.blackFlashZoneBonusII,
                Config.blackFlashZoneBonusIII,
                targetLowHpBonus
        );

        // Step 3: 判定是否触发黑闪
        if (attacker.getRandom().nextDouble() < chance)
        {
            triggerBlackFlash(attacker, target, event, rawBeforePower);
        }
    }

    // ======== 击杀检测 ========

    /**
     * 玩家击杀实体时记录时间戳，用于黑闪概率加成。
     */
    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event)
    {
        if (event.getEntity().level().isClientSide)
        {
            return;
        }

        if (!(event.getSource().getEntity() instanceof ServerPlayer attacker))
        {
            return;
        }

        if (!EpicFightCompat.isBattleMode(attacker))
        {
            return;
        }

        PlayerBlackFlashData.setLastKillTime(attacker, attacker.level().getGameTime());
    }

    // ======== 闪避检测（PlayerTick 中注册 EpicFight 监听器） ========

    /**
     * 在玩家 tick 中注册 EpicFight 的 {@link DodgeSuccessEvent} 监听器。
     * <p>
     * 每位玩家仅注册一次（通过 persistentData 标记防重复）。
     * 监听器会在玩家成功闪避体术攻击时记录时间戳。
     */
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

        // Zone 过期检测
        if (PlayerBlackFlashData.getZoneLevel(player) > 0
                && !PlayerBlackFlashData.isZoneActive(player))
        {
            removeZoneAttackBonus(player);
            player.removeEffect(ModEffects.ZONE.get());
            PlayerBlackFlashData.clearZone(player);
        }

        // Dodge 监听器注册（仅一次）
        if (PlayerBlackFlashData.isDodgeListenerRegistered(player))
        {
            return;
        }

        if (!EpicFightCompat.isBattleMode(player))
        {
            return;
        }

        ServerPlayerPatch patch = EpicFightCapabilities.getEntityPatch(player, ServerPlayerPatch.class);
        if (patch == null)
        {
            return;
        }

        patch.getEventListener().addEventListener(
                EventType.DODGE_SUCCESS_EVENT,
                DODGE_EVENT_UUID,
                (dodgeEvent) -> onDodgeSuccess(player, dodgeEvent)
        );

        PlayerBlackFlashData.setDodgeListenerRegistered(player, true);
    }

    /**
     * 玩家成功闪避一次体术攻击。
     */
    private static void onDodgeSuccess(ServerPlayer player, DodgeSuccessEvent event)
    {
        // 仅计入体术攻击的闪避
        if (event.getDamageSource() instanceof EpicFightDamageSource efSource && efSource.isBasicAttack())
        {
            PlayerBlackFlashData.setLastDodgeTick(player, player.tickCount);
        }
    }

    // ======== 登出清理 ========

    /** Rush Into The ZONE 最高潮：效果添加时立刻进入 ZONE III 并标记必定黑闪。 */
    @SubscribeEvent
    public static void onRushIntoTheZoneClimaxAdded(MobEffectEvent.Added event)
    {
        if (event.getEntity().level().isClientSide)
        {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player))
        {
            return;
        }
        if (event.getEffectInstance().getEffect() != ModEffects.RUSH_INTO_THE_ZONE_CLIMAX.get())
        {
            return;
        }
        RushIntoTheZoneClimaxEffect.applyZoneAndGuaranteedBlackFlash(player);
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event)
    {
        if (event.getEntity() instanceof ServerPlayer player)
        {
            PlayerBlackFlashData.resetAll(player);
            TechniqueDamageBoosts.clear(player);
        }
    }

    // ======== 黑闪触发 ========

    /**
     * 执行黑闪触发效果：伤害倍率 + Photon fx + ZONE 状态。
     */
    private static void triggerBlackFlash(ServerPlayer attacker, LivingEntity target, LivingHurtEvent event,
                                          float rawBeforePower)
    {
        // 伤害：2.5 次方（符合原著设定）。
        // ⚠️ 必须拿「强度放大之前」的原始伤害取幂，再把面板倍率线性乘回去：
        //    直接对已放大的伤害取幂，pow 会把面板倍率（特级最高 3 倍）一起指数放大成几十倍。
        float scaledAmount = event.getAmount();
        float powered = (float) Math.pow(rawBeforePower, Config.blackFlashDamageMultiplier);
        if (rawBeforePower > 0.0001F)
        {
            powered *= scaledAmount / rawBeforePower;
        }
        event.setAmount(powered);

        // 黑闪音效
        attacker.level().playSound(null,
                target.getX(), target.getY(), target.getZ(),
                ModSounds.BLACK_FLASH.get(), SoundSource.PLAYERS,
                1.0F, 1.0F);

        // 黑闪屏幕振动：2 秒
        ModNetwork.sendBlackFlashScreenShakeSync(
                attacker,
                BlackFlashConfig.SCREEN_SHAKE_DURATION_TICKS,
                BlackFlashConfig.SCREEN_SHAKE_MAX_DEGREES);

        // 黑闪奖励：恢复 500 咒力 + 20 咒术经验
        PlayerCursedEnergyData.setCurrent(attacker,
                PlayerCursedEnergyData.getCurrent(attacker) + 500);
        PlayerCursedGradeData.addExperience(attacker, 20);

        // 在目标位置生成 Photon 特效锚点实体
        if (attacker.level() instanceof ServerLevel serverLevel)
        {
            SpecialEffectEntity fx = ModEntities.SPECIAL_EFFECT_ENTITY.get().create(serverLevel);
            if (fx != null)
            {
                Vec3 pos = target.position().add(0, target.getBbHeight() * 0.5, 0);
                double dx = attacker.getX() - pos.x;
                double dz = attacker.getZ() - pos.z;
                double horizontalDist = Math.sqrt(dx * dx + dz * dz);
                float yawToAttacker = (float) Math.toDegrees(Math.atan2(-dx, dz));
                float pitchToAttacker = (float) -Math.toDegrees(Math.atan2(
                        attacker.getEyeY() - pos.y, horizontalDist));
                fx.moveTo(pos.x, pos.y, pos.z, yawToAttacker, pitchToAttacker);
                fx.configureBlackFlash(attacker);
                serverLevel.addFreshEntity(fx);
            }
        }

        // 黑闪冲击波：0.5 秒延迟后对攻击者 3 格范围内随机破坏方块并伤害生物
        SpecialEffectEntity shockwave = ModEntities.SPECIAL_EFFECT_ENTITY.get().create(attacker.serverLevel());
        if (shockwave != null)
        {
            shockwave.moveTo(attacker.getX(), attacker.getY(), attacker.getZ(), 0, 0);
            shockwave.configureBlackFlashShockwaveDelay(attacker);
            attacker.serverLevel().addFreshEntity(shockwave);
        }

        // ZONE 状态：连击 ≥ 门槛时触发，每级需 2 次黑闪升级，III 级不刷新
        int comboBeforeReset = PlayerBlackFlashData.getComboCount(attacker);
        if (comboBeforeReset >= Config.blackFlashZoneMinCombo)
        {
            int zoneLevel = PlayerBlackFlashData.getZoneLevel(attacker);
            if (zoneLevel >= PlayerBlackFlashData.MAX_ZONE_LEVEL)
            {
                // III 级：不刷新，自然到期
            }
            else if (PlayerBlackFlashData.isZoneActive(attacker))
            {
                // I/II 级：刷新持续时间，累计触发次数
                int newExpire = attacker.tickCount + Config.blackFlashZoneDurationTicks;
                PlayerBlackFlashData.incrementZoneTriggerCount(attacker);
                PlayerBlackFlashData.setZoneExpireTick(attacker, newExpire);
                attacker.addEffect(new MobEffectInstance(
                        ModEffects.ZONE.get(),
                        Config.blackFlashZoneDurationTicks,
                        zoneLevel - 1,
                        false, false, true
                ));
                // 触发 2 次 → 升级
                if (PlayerBlackFlashData.getZoneTriggerCount(attacker) >= PlayerBlackFlashData.ZONE_TRIGGERS_PER_LEVEL)
                {
                    PlayerBlackFlashData.levelUpZone(attacker, newExpire);
                    PlayerBlackFlashData.setZoneTriggerCount(attacker, 0);
                    int newLevel = PlayerBlackFlashData.getZoneLevel(attacker);
                    attacker.addEffect(new MobEffectInstance(
                            ModEffects.ZONE.get(),
                            Config.blackFlashZoneDurationTicks,
                            newLevel - 1,
                            false, false, true
                    ));
                }
            }
            else
            {
                // 首次进入 ZONE I
                int newExpire = attacker.tickCount + Config.blackFlashZoneDurationTicks;
                PlayerBlackFlashData.enterZone(attacker, newExpire);
                PlayerBlackFlashData.setZoneTriggerCount(attacker, 1);
                applyZoneAttackBonus(attacker);
                attacker.addEffect(new MobEffectInstance(
                        ModEffects.ZONE.get(),
                        Config.blackFlashZoneDurationTicks,
                        0,  // amplifier 0 = I
                        false, false, true
                ));
            }
        }

        // 触发后重置连击，防止概率雪崩
        PlayerBlackFlashData.resetCombo(attacker);

        // 记录触发时间，进入 2 秒冷却窗口
        PlayerBlackFlashData.setLastTriggerTick(attacker, attacker.tickCount);

        incrementShrineBlackFlashMilestoneIfEligible(attacker);
    }

    /** 御厨子：已解锁「解」切割后，每次黑闪计入术式开发里程碑。 */
    private static void incrementShrineBlackFlashMilestoneIfEligible(ServerPlayer player)
    {
        if (!PlayerTechniqueData.TECHNIQUE_SHRINE.equals(PlayerTechniqueData.getInnateTechnique(player)))
        {
            return;
        }
        SkillUnlockLogic.findSkillDefinition(PlayerTechniqueData.TECHNIQUE_SHRINE, ShrineTechniqueSkillSet.SKILL_KAI_FLYOBJ)
                .filter(skill -> SkillUnlockLogic.isSkillUnlocked(
                        player,
                        PlayerTechniqueData.TECHNIQUE_SHRINE,
                        ShrineTechniqueSkillSet.SKILL_KAI_FLYOBJ,
                        skill))
                .ifPresent(skill -> PlayerShrineBlackFlashMilestoneData.increment(player));
    }

    /** 应用 ZONE 状态：写入等级、攻击加成与 HUD 图标。 */
    public static void applyZoneState(ServerPlayer player, int level, int durationTicks)
    {
        int newExpire = player.tickCount + durationTicks;
        PlayerBlackFlashData.forceEnterZoneLevel(player, level, newExpire);
        applyZoneAttackBonus(player);
        player.addEffect(new MobEffectInstance(
                ModEffects.ZONE.get(),
                durationTicks,
                level - 1,
                false, false, true
        ));
    }

    /** 应用 ZONE 攻击伤害加成属性修饰符。 */
    private static void applyZoneAttackBonus(ServerPlayer player)
    {
        var attr = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attr == null)
        {
            return;
        }
        // 移除旧修饰符（防重复），再添加新修饰符
        attr.removeModifier(ZONE_ATTACK_MODIFIER_UUID);
        attr.addTransientModifier(new AttributeModifier(
                ZONE_ATTACK_MODIFIER_UUID,
                "Black Flash Zone attack bonus",
                Config.blackFlashZoneAttackBonus,
                AttributeModifier.Operation.MULTIPLY_BASE
        ));
    }

    /** 移除 ZONE 攻击伤害加成。 */
    private static void removeZoneAttackBonus(ServerPlayer player)
    {
        var attr = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attr != null)
        {
            attr.removeModifier(ZONE_ATTACK_MODIFIER_UUID);
        }
    }
}
