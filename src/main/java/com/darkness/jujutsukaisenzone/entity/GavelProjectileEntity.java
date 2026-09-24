package com.darkness.jujutsukaisenzone.entity;

import com.darkness.jujutsukaisenzone.registry.ModEntities;
import com.darkness.jujutsukaisenzone.registry.ModItems;
import com.darkness.jujutsukaisenzone.technique.TechniqueDamageSources;
import com.darkness.jujutsukaisenzone.technique.skill.GavelFormStats;
import com.darkness.jujutsukaisenzone.technique.skillset.SkillDamageType;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 法槌投射物：右键即时射出，弹道类似箭（重力 + 高速），落点小范围爆炸（不破坏方块）。
 * <p>
 * 直击与爆炸均为<strong>术式</strong>伤害（审判术式能力）；手持近战挥击另算体术，不在本类。
 */
public class GavelProjectileEntity extends ThrowableItemProjectile
{
    /** 标准形态直射伤害（技能描述等仍引用此基准值）。 */
    public static final float BASE_DAMAGE = GavelFormStats.STANDARD_PROJECTILE_DAMAGE;
    /** 标准形态落点爆炸威力（不破坏方块）。 */
    public static final float IMPACT_EXPLOSION_POWER = GavelFormStats.STANDARD_EXPLOSION_POWER;

    public GavelProjectileEntity(EntityType<? extends GavelProjectileEntity> type, Level level)
    {
        super(type, level);
    }

    public GavelProjectileEntity(Level level, LivingEntity shooter)
    {
        super(ModEntities.GAVEL_PROJECTILE.get(), shooter, level);
    }

    @Override
    protected Item getDefaultItem()
    {
        return ModItems.SM_GAVEL_WEAPON.get();
    }

    @Override
    protected float getGravity()
    {
        return 0.05F;
    }

    @Override
    protected void onHitEntity(EntityHitResult result)
    {
        if (!this.level().isClientSide)
        {
            TechniqueDamageSources.runWithType(SkillDamageType.TECHNIQUE, () -> {
                detonateAt(result.getLocation());
                Entity hit = result.getEntity();
                Entity owner = this.getOwner();
                if (hit instanceof LivingEntity living && hit != owner)
                {
                    DamageSource source = this.damageSources().thrown(this, owner);
                    living.hurt(source, GavelFormStats.getProjectileDamage(this.getItem()));
                }
            });
        }
        this.discard();
    }

    @Override
    protected void onHitBlock(BlockHitResult result)
    {
        if (!this.level().isClientSide)
        {
            TechniqueDamageSources.runWithType(SkillDamageType.TECHNIQUE,
                    () -> detonateAt(result.getLocation()));
        }
        this.discard();
    }

    private void detonateAt(Vec3 impact)
    {
        ItemStack stack = this.getItem();
        this.level().explode(
                this,
                impact.x,
                impact.y,
                impact.z,
                GavelFormStats.getExplosionPower(stack),
                false,
                GavelFormStats.getExplosionInteraction(stack));
    }
}
