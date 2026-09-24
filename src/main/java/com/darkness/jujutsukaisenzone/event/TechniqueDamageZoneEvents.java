package com.darkness.jujutsukaisenzone.event;

import com.darkness.jujutsukaisenzone.Config;
import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.compat.EpicFightCompat;
import com.darkness.jujutsukaisenzone.effect.DeathSentenceEffect;
import com.darkness.jujutsukaisenzone.player.PlayerBlackFlashData;
import com.darkness.jujutsukaisenzone.technique.TechniqueDamageBoosts;
import com.darkness.jujutsukaisenzone.technique.TechniqueDamageSources;
import com.darkness.jujutsukaisenzone.technique.skill.JudgmentWeaponLogic;
import com.darkness.jujutsukaisenzone.technique.skillset.SkillDamageType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;
import yesman.epicfight.world.damagesource.EpicFightDamageSource;

/**
 * 伤害乘区统一结算管线（乘区系统第一期）。
 * <p>
 * 所有伤害的最终落地都过 {@link LivingHurtEvent}——术式（{@code TechniqueDamageSources.deal}
 * → {@code LivingEntity#hurt}）与 EpicFight 体术在此汇合，这里就是全模组唯一的乘区汇合点。
 * 各乘区按固定顺序施加，取代散落各处的 {@code event.setAmount}：
 * <ol>
 *   <li>施术者增强区 —— {@link TechniqueDamageBoosts} 的分型匹配「下一击增伤」</li>
 *   <li>目标减免区 —— ZONE 状态减伤（自 {@code BlackFlashEvents} 迁入，判定与数值不变）</li>
 * </ol>
 * 黑闪的 {@code pow(x, 2.5)} 仍留在 {@code BlackFlashEvents}（它不是纯数值，还带特效与奖励），
 * 且必然晚于本管线执行 ⇒ 黑闪区放大所有前置乘区，这是设计预期。
 * <p>
 * ⚠️ 优先级次序（勿随意改动）：处刑人之剑的必杀覆盖在 {@link EventPriority#HIGHEST}
 * （它是对伤害量的<b>改写</b>，必须先于一切乘区）；本管线 {@link EventPriority#HIGH}；
 * 罪恶者增伤 / 黑闪 / 蓄力打断在默认优先级。乘法交换律保证迁移前后数值一致。
 * <p>
 * 后续新乘区（易伤、克制、标签抗性……）一律加进本类并写明顺序，<b>不要</b>再到别的
 * 监听器里裸写 {@code setAmount} 乘算。
 */
@Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class TechniqueDamageZoneEvents
{
    private TechniqueDamageZoneEvents()
    {
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLivingHurt(LivingHurtEvent event)
    {
        if (event.getEntity().level().isClientSide)
        {
            return;
        }

        LivingEntity target = event.getEntity();

        // —— 区 1：施术者增强（分型匹配的「下一击增伤」）——
        if (event.getSource().getEntity() instanceof ServerPlayer attacker)
        {
            SkillDamageType type = resolveDamageType(attacker, target, event.getSource());
            if (type != null)
            {
                float bonus = TechniqueDamageBoosts.consume(attacker, type);
                if (bonus > 0.0F)
                {
                    event.setAmount(event.getAmount() * (1.0F + bonus));
                }
            }
        }

        // —— 区 2：目标减免（ZONE 状态）——
        // 迁自 BlackFlashEvents#onLivingHurt 开头，判定与数值原样保留：
        // 处于 ZONE 的战斗模式玩家，受到的一切伤害按配置比例减免。
        if (target instanceof ServerPlayer zonePlayer
                && EpicFightCompat.isBattleMode(zonePlayer)
                && PlayerBlackFlashData.isZoneActive(zonePlayer))
        {
            event.setAmount(event.getAmount() * (float) (1.0D - Config.blackFlashZoneDefenseBonus));
        }
    }

    /**
     * 判定这一击的叙事分型（与 DamageSource 通道解耦）：
     * <ol>
     *   <li>出伤点显式写入的 ThreadLocal（{@code deal} / {@code runWithType}）</li>
     *   <li>处刑人之剑对【死刑】目标的挥击 → 复合</li>
     *   <li>必定黑闪标记下的 EpicFight 基础攻击 → 复合</li>
     *   <li>EpicFight 基础攻击 → 体术</li>
     *   <li>赫击飞方块砸人 → 术式</li>
     *   <li>其余 → null（不吃分型匹配）</li>
     * </ol>
     * 随机黑闪在默认优先级才掷骰，本管线此时尚不知会否触发，故随机黑闪仍按体术消耗加成；
     * 必定黑闪与处刑必杀在此即可标复合。
     */
    @Nullable
    static SkillDamageType resolveDamageType(ServerPlayer attacker, LivingEntity target, DamageSource source)
    {
        SkillDamageType tagged = TechniqueDamageSources.currentDamageType();
        if (tagged != null)
        {
            return tagged;
        }
        if (isExecutionersComposite(attacker, target, source))
        {
            return SkillDamageType.COMPOSITE;
        }
        if (source instanceof EpicFightDamageSource efSource && efSource.isBasicAttack())
        {
            if (PlayerBlackFlashData.hasGuaranteedBlackFlash(attacker))
            {
                return SkillDamageType.COMPOSITE;
            }
            return SkillDamageType.MARTIAL;
        }
        if (source.getDirectEntity() instanceof FallingBlockEntity falling
                && falling.getPersistentData().getBoolean(TechniqueDamageSources.TAG_RED_DEBRIS))
        {
            return SkillDamageType.TECHNIQUE;
        }
        return null;
    }

    private static boolean isExecutionersComposite(ServerPlayer attacker, LivingEntity target, DamageSource source)
    {
        if (!DeathSentenceEffect.isActive(target))
        {
            return false;
        }
        if (!JudgmentWeaponLogic.isWieldingExecutionersSword(attacker))
        {
            return false;
        }
        // 处刑必杀叠在挥击上：EF 基础攻击，或原版玩家近战直击
        if (source instanceof EpicFightDamageSource efSource && efSource.isBasicAttack())
        {
            return true;
        }
        return source.getDirectEntity() == attacker;
    }
}
