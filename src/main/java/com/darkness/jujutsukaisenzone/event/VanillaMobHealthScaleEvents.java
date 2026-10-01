package com.darkness.jujutsukaisenzone.event;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.player.CursedGradeBonuses;
import com.darkness.jujutsukaisenzone.player.ServerOnlineHealthAverage;
import com.darkness.jujutsukaisenzone.technique.skill.tenshadows.TenShadowsShikigamiStatsApplier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.UUID;

/**
 * 原版（及第三方）生物随「服务器在线玩家平均最大生命」缩放。
 * <ul>
 *   <li>最大生命：线性 {@code × (均血 / 20)}</li>
 *   <li>造成伤害：亚线性 {@code × √(均血 / 20)}（近战 / 弹射 / 爆炸等凡经 {@link LivingHurtEvent} 的伤）</li>
 * </ul>
 * 仅敌对 / 中立生物参与；村民等友好生物不缩放。
 * 生命用 {@link AttributeModifier.Operation#MULTIPLY_TOTAL} 固定 UUID，不改写 base。
 * 本模组实体与十影式神不参与。无人在线时卸掉修饰符，恢复原版。
 */
@Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class VanillaMobHealthScaleEvents
{
    private static final UUID SCALE_UUID = UUID.fromString("6b2c9a01-4001-4000-8000-000000000001");
    private static final String SCALE_NAME = "jjkz_online_avg_health_scale";
    /** 刷出时写入的生命缩放系数，供出伤读 √scale，避免只改 ATTACK_DAMAGE 漏弹射物。 */
    private static final String TAG_HEALTH_SCALE = "jjkz_online_health_scale";

    private VanillaMobHealthScaleEvents()
    {
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onEntityJoinLevel(EntityJoinLevelEvent event)
    {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof LivingEntity living))
        {
            return;
        }
        if (!isExternalMob(living) || !isHostileOrNeutral(living))
        {
            return;
        }
        if (!(event.getLevel() instanceof ServerLevel serverLevel))
        {
            return;
        }
        applyScale(living, serverLevel.getServer());
    }

    /**
     * 在护甲结算前按 √生命缩放放大攻击方伤害；不改属性，避免与近战属性伤叠乘。
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLivingHurt(LivingHurtEvent event)
    {
        if (event.getEntity().level().isClientSide())
        {
            return;
        }
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker))
        {
            return;
        }
        if (!isExternalMob(attacker) || !isHostileOrNeutral(attacker))
        {
            return;
        }
        double healthScale = readHealthScale(attacker);
        if (healthScale <= 1.0001D)
        {
            return;
        }
        float factor = (float) Math.sqrt(healthScale);
        event.setAmount(event.getAmount() * factor);
    }

    private static boolean isExternalMob(LivingEntity living)
    {
        if (living instanceof Player)
        {
            return false;
        }
        if (!(living instanceof Mob))
        {
            return false;
        }
        if (TenShadowsShikigamiStatsApplier.isTenShadowsStatEntity(living))
        {
            return false;
        }
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(living.getType());
        if (id != null && JuJutsuKaisenZone.MODID.equals(id.getNamespace()))
        {
            return false;
        }
        return living.getAttribute(Attributes.MAX_HEALTH) != null;
    }

    /** 敌对（僵尸等）或中立（末影人 / 铁傀儡 / 狼等）；不含村民、牛等友好生物。 */
    private static boolean isHostileOrNeutral(LivingEntity living)
    {
        if (living instanceof Enemy || living instanceof Monster || living instanceof NeutralMob)
        {
            return true;
        }
        return living.getType().getCategory() == MobCategory.MONSTER;
    }

    private static void applyScale(LivingEntity living, MinecraftServer server)
    {
        AttributeInstance maxHealth = living.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth == null)
        {
            return;
        }

        float oldMax = living.getMaxHealth();
        float oldHealth = living.getHealth();

        maxHealth.removeModifier(SCALE_UUID);

        double scale = 1.0D;
        double avg = ServerOnlineHealthAverage.averageMaxHealth(server);
        if (avg > 0.0D)
        {
            scale = avg / CursedGradeBonuses.BASE_MAX_HEALTH;
            // MULTIPLY_TOTAL：最终 = base × (1 + amount)；要乘 scale 则 amount = scale - 1
            double amount = scale - 1.0D;
            if (Math.abs(amount) > 1.0E-4D)
            {
                maxHealth.addPermanentModifier(new AttributeModifier(
                        SCALE_UUID,
                        SCALE_NAME,
                        amount,
                        AttributeModifier.Operation.MULTIPLY_TOTAL));
            }
            else
            {
                scale = 1.0D;
            }
        }

        writeHealthScale(living, scale);

        float newMax = living.getMaxHealth();
        if (newMax <= 0.0F)
        {
            return;
        }
        float ratio = oldMax > 0.001F ? oldHealth / oldMax : 1.0F;
        living.setHealth(Mth.clamp(newMax * ratio, 1.0F, newMax));
    }

    private static void writeHealthScale(LivingEntity living, double scale)
    {
        CompoundTag data = living.getPersistentData();
        if (scale <= 1.0001D)
        {
            data.remove(TAG_HEALTH_SCALE);
            return;
        }
        data.putDouble(TAG_HEALTH_SCALE, scale);
    }

    private static double readHealthScale(LivingEntity living)
    {
        CompoundTag data = living.getPersistentData();
        if (data.contains(TAG_HEALTH_SCALE))
        {
            return Math.max(0.0D, data.getDouble(TAG_HEALTH_SCALE));
        }
        AttributeInstance maxHealth = living.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth == null)
        {
            return 1.0D;
        }
        AttributeModifier modifier = maxHealth.getModifier(SCALE_UUID);
        if (modifier == null)
        {
            return 1.0D;
        }
        return Math.max(0.0D, modifier.getAmount() + 1.0D);
    }
}
