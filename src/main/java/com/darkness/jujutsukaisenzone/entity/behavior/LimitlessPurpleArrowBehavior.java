package com.darkness.jujutsukaisenzone.entity.behavior;

import com.darkness.jujutsukaisenzone.entity.SpecialEffectEntity;
import com.darkness.jujutsukaisenzone.entity.data.LimitlessPurpleConfig;
import com.darkness.jujutsukaisenzone.entity.util.GeometryUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import com.darkness.jujutsukaisenzone.technique.SkillDamageResolver;
import com.darkness.jujutsukaisenzone.technique.TechniqueDamageSources;
import com.darkness.jujutsukaisenzone.technique.skillset.LimitlessTechniqueSkillSet;
import com.darkness.jujutsukaisenzone.technique.skillset.SkillDamageType;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * 茈普通释放箭：无重力、球体内删方块、持续高伤。
 */
public class LimitlessPurpleArrowBehavior extends EffectBehavior
{
    public static final String ID = "limitless_purple_arrow";

    @Nullable
    private UUID ownerUuid;
    private Vec3 velocity = Vec3.ZERO;

    @Override
    public String id() { return ID; }

    public void configure(@Nullable LivingEntity owner, Vec3 velocity)
    {
        this.ownerUuid = owner == null ? null : owner.getUUID();
        this.velocity = velocity;
    }

    @Override
    public void tick(SpecialEffectEntity entity)
    {
        if (!entity.level().hasChunkAt(entity.blockPosition()))
        {
            entity.discard();
            return;
        }

        entity.setNoGravity(true);
        entity.setDeltaMovement(this.velocity);
        entity.setPos(entity.position().add(this.velocity));

        eraseBlocksInSphere(entity);
        damageLivingInSphere(entity);
    }

    private void eraseBlocksInSphere(SpecialEffectEntity entity)
    {
        Level level = entity.level();
        if (level.isClientSide()) return;

        double cx = entity.getX();
        double cy = entity.getY();
        double cz = entity.getZ();
        double r = LimitlessPurpleConfig.ARROW_BLOCK_ERASE_RADIUS;
        double rSqr = r * r;
        int ri = Mth.ceil(r);
        int ox = Mth.floor(cx);
        int oy = Mth.floor(cy);
        int oz = Mth.floor(cz);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int dx = -ri; dx <= ri; dx++)
        {
            for (int dy = -ri; dy <= ri; dy++)
            {
                for (int dz = -ri; dz <= ri; dz++)
                {
                    pos.set(ox + dx, oy + dy, oz + dz);
                    if (level.isOutsideBuildHeight(pos) || !level.getWorldBorder().isWithinBounds(pos)) continue;
                    if (!GeometryUtils.blockCubeIntersectsSphere(cx, cy, cz, rSqr, pos.getX(), pos.getY(), pos.getZ())) continue;
                    if (level.getBlockState(pos).isAir()) continue;
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
                }
            }
        }
    }

    private void damageLivingInSphere(SpecialEffectEntity entity)
    {
        Level level = entity.level();
        if (level.isClientSide()) return;

        double cx = entity.getX();
        double cy = entity.getY();
        double cz = entity.getZ();
        double r = LimitlessPurpleConfig.ARROW_BLOCK_ERASE_RADIUS;
        double rSqr = r * r;
        AABB query = new AABB(cx, cy, cz, cx, cy, cz).inflate(r);

        LivingEntity owner = resolveOwner(entity, this.ownerUuid);
        float dmg = SkillDamageResolver.technique(owner, LimitlessPurpleConfig.ARROW_SPHERE_DAMAGE_PER_TICK);

        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, query))
        {
            if (!target.isAlive()) continue;
            if (!GeometryUtils.aabbIntersectsSphere(cx, cy, cz, rSqr, target.getBoundingBox())) continue;
            if (owner != null && target.getUUID().equals(owner.getUUID())) continue;

            DamageSource source = TechniqueDamageSources.techniqueSource(level, owner);
            TechniqueDamageSources.runWithType(SkillDamageType.TECHNIQUE, LimitlessTechniqueSkillSet.SKILL_PURPLE, () -> {
                if (target instanceof Player)
                {
                    target.hurt(source, dmg);
                }
                else
                {
                    target.hurt(source, dmg);
                    if (!target.isAlive()) target.discard();
                }
            });
        }
    }

    @Override
    public void writeToNbt(CompoundTag tag)
    {
        tag.putDouble("VelX", this.velocity.x);
        tag.putDouble("VelY", this.velocity.y);
        tag.putDouble("VelZ", this.velocity.z);
        if (this.ownerUuid != null) tag.putUUID("Owner", this.ownerUuid);
    }

    @Override
    public void readFromNbt(CompoundTag tag)
    {
        this.velocity = new Vec3(tag.getDouble("VelX"), tag.getDouble("VelY"), tag.getDouble("VelZ"));
        this.ownerUuid = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
    }
}
