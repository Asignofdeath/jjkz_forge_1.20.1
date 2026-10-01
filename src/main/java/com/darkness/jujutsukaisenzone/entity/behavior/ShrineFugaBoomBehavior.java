package com.darkness.jujutsukaisenzone.entity.behavior;

import com.darkness.jujutsukaisenzone.api.mimicry.MimicryVoiceLineEvents;
import com.darkness.jujutsukaisenzone.api.mimicry.MimicryVoiceLines;
import com.darkness.jujutsukaisenzone.entity.SpecialEffectEntity;
import com.darkness.jujutsukaisenzone.entity.data.ShrineFugaConfig;
import com.darkness.jujutsukaisenzone.entity.util.GeometryUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import com.darkness.jujutsukaisenzone.player.PlayerTechniqueData;
import com.darkness.jujutsukaisenzone.technique.SkillDamageResolver;
import com.darkness.jujutsukaisenzone.technique.TechniqueDamageSources;
import com.darkness.jujutsukaisenzone.technique.development.SkillUpgradeLogic;
import com.darkness.jujutsukaisenzone.technique.skillset.ShrineTechniqueSkillSet;
import com.darkness.jujutsukaisenzone.technique.skillset.SkillDamageType;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * 「灶」开爆炸：落点挂载 photon:fuga_boom，对半径内生物持续造成巨额伤害，并由内向外逐层将方块替换为空气。
 */
public class ShrineFugaBoomBehavior extends EffectBehavior
{
    public static final String ID = "shrine_fuga_boom";

    @Nullable
    private UUID ownerUuid;
    private int remainingLifeTicks = -1;
    private int remainingDamageTicks = -1;
    private boolean photonMounted;
    /** 已完成空气替换的内层球形半径（格）。 */
    private double blockClearInnerRadius;
    private boolean blockClearFinished;

    @Override
    public String id()
    {
        return ID;
    }

    public void configure(@Nullable LivingEntity owner)
    {
        this.ownerUuid = owner == null ? null : owner.getUUID();
        this.remainingLifeTicks = ShrineFugaConfig.BOOM_LIFETIME_TICKS;
        this.remainingDamageTicks = ShrineFugaConfig.BOOM_LIFETIME_TICKS;
        this.photonMounted = false;
        this.blockClearInnerRadius = 0.0D;
        this.blockClearFinished = false;
    }

    @Override
    public void tick(SpecialEffectEntity entity)
    {
        if (!(entity.level() instanceof ServerLevel level))
        {
            return;
        }

        if (!this.photonMounted)
        {
            mountBoomPhoton(entity);
            this.photonMounted = true;
        }

        tickBlockClear(entity, level);
        if (this.remainingDamageTicks > 0)
        {
            damageLivingInSphere(entity, level);
            this.remainingDamageTicks--;
        }

        this.remainingLifeTicks--;
        if (this.remainingLifeTicks <= 0 && this.blockClearFinished)
        {
            entity.discard();
        }
    }

    /** 从爆炸中心逐层向外，将壳层内方块直接替换为空气（无掉落、无破坏动画）。 */
    private void tickBlockClear(SpecialEffectEntity entity, ServerLevel level)
    {
        if (this.blockClearFinished)
        {
            return;
        }

        double maxRadius = ShrineFugaConfig.BOOM_BLOCK_BREAK_RADIUS;
        if (this.blockClearInnerRadius >= maxRadius)
        {
            this.blockClearFinished = true;
            return;
        }

        double cx = entity.getX();
        double cy = entity.getY();
        double cz = entity.getZ();
        double shellOuter = Math.min(maxRadius, this.blockClearInnerRadius + ShrineFugaConfig.BOOM_BLOCK_CLEAR_SHELL_STEP);
        double innerSqr = this.blockClearInnerRadius * this.blockClearInnerRadius;
        double outerSqr = shellOuter * shellOuter;
        int budget = ShrineFugaConfig.BOOM_BLOCK_CLEAR_MAX_BLOCKS_PER_TICK;
        int cleared = clearBlockShell(level, cx, cy, cz, innerSqr, outerSqr, budget);

        if (cleared < budget)
        {
            this.blockClearInnerRadius = shellOuter;
            if (this.blockClearInnerRadius >= maxRadius)
            {
                this.blockClearFinished = true;
            }
        }
    }

    private static int clearBlockShell(
            ServerLevel level,
            double cx,
            double cy,
            double cz,
            double innerRadiusSqr,
            double outerRadiusSqr,
            int maxBlocks)
    {
        int cleared = 0;
        int ri = Mth.ceil(Math.sqrt(outerRadiusSqr));
        int ox = Mth.floor(cx);
        int oy = Mth.floor(cy);
        int oz = Mth.floor(cz);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        outer:
        for (int dx = -ri; dx <= ri; dx++)
        {
            for (int dy = -ri; dy <= ri; dy++)
            {
                for (int dz = -ri; dz <= ri; dz++)
                {
                    pos.set(ox + dx, oy + dy, oz + dz);
                    if (level.isOutsideBuildHeight(pos) || !level.getWorldBorder().isWithinBounds(pos))
                    {
                        continue;
                    }
                    if (!GeometryUtils.blockCubeIntersectsSphere(cx, cy, cz, outerRadiusSqr, pos.getX(), pos.getY(), pos.getZ()))
                    {
                        continue;
                    }
                    if (innerRadiusSqr > 0.0D
                            && GeometryUtils.blockCubeIntersectsSphere(cx, cy, cz, innerRadiusSqr, pos.getX(), pos.getY(), pos.getZ()))
                    {
                        continue;
                    }
                    var state = level.getBlockState(pos);
                    if (state.isAir() || state.getDestroySpeed(level, pos) < 0.0F)
                    {
                        continue;
                    }
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                    if (++cleared >= maxBlocks)
                    {
                        break outer;
                    }
                }
            }
        }
        return cleared;
    }

    private void damageLivingInSphere(SpecialEffectEntity entity, ServerLevel level)
    {
        double cx = entity.getX();
        double cy = entity.getY();
        double cz = entity.getZ();
        double radius = ShrineFugaConfig.BOOM_RADIUS;
        double radiusSqr = radius * radius;
        AABB query = new AABB(cx, cy, cz, cx, cy, cz).inflate(radius);

        LivingEntity owner = resolveOwner(entity, this.ownerUuid);
        // 强度面板换算：调用 throttled hurt 的站点无法走 deal，这里手动包一层。
        float ratio = SkillUpgradeLogic.scaleRatio(
                owner,
                PlayerTechniqueData.TECHNIQUE_SHRINE,
                ShrineTechniqueSkillSet.SKILL_FUGA,
                ShrineFugaConfig.BOOM_DAMAGE_PER_TICK);
        float damage = SkillDamageResolver.technique(owner, ratio);

        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, query))
        {
            if (!target.isAlive())
            {
                continue;
            }
            if (!GeometryUtils.aabbIntersectsSphere(cx, cy, cz, radiusSqr, target.getBoundingBox()))
            {
                continue;
            }
            if (owner != null && target.getUUID().equals(owner.getUUID()))
            {
                continue;
            }

            DamageSource source = TechniqueDamageSources.techniqueSource(level, owner);
            if (target instanceof Player)
            {
                TechniqueDamageSources.runWithType(SkillDamageType.TECHNIQUE, ShrineTechniqueSkillSet.SKILL_FUGA,
                        () -> target.hurt(source, damage));
            }
            else
            {
                TechniqueDamageSources.runWithType(SkillDamageType.TECHNIQUE, ShrineTechniqueSkillSet.SKILL_FUGA,
                        () -> target.hurt(source, damage));
                if (!target.isAlive())
                {
                    target.discard();
                }
            }
        }
    }

    private static void mountBoomPhoton(SpecialEffectEntity entity)
    {
        if (entity.getServer() == null)
        {
            return;
        }
        CommandSourceStack source = entity.createCommandSourceStack().withPermission(2).withSuppressedOutput();
        entity.getServer().getCommands().performPrefixedCommand(source, ShrineFugaConfig.PHOTON_BOOM_COMMAND);
    }

    @Override
    public void onDiscard(SpecialEffectEntity entity)
    {
        LivingEntity owner = resolveOwner(entity, this.ownerUuid);
        if (owner instanceof ServerPlayer player)
        {
            MimicryVoiceLines.tryPlay(player, MimicryVoiceLineEvents.SHRINE_FUGA_BOOM_END);
        }

        if (!this.photonMounted || entity.getServer() == null)
        {
            return;
        }
        CommandSourceStack source = entity.createCommandSourceStack().withPermission(2).withSuppressedOutput();
        entity.getServer().getCommands().performPrefixedCommand(source, ShrineFugaConfig.PHOTON_BOOM_REMOVE_COMMAND);
        this.photonMounted = false;
    }

    @Override
    public void writeToNbt(CompoundTag tag)
    {
        tag.putInt("RemainingLife", this.remainingLifeTicks);
        tag.putInt("RemainingDamage", this.remainingDamageTicks);
        tag.putBoolean("PhotonMounted", this.photonMounted);
        tag.putDouble("BlockClearInnerRadius", this.blockClearInnerRadius);
        tag.putBoolean("BlockClearFinished", this.blockClearFinished);
        if (this.ownerUuid != null)
        {
            tag.putUUID("Owner", this.ownerUuid);
        }
    }

    @Override
    public void readFromNbt(CompoundTag tag)
    {
        this.remainingLifeTicks = tag.contains("RemainingLife") ? tag.getInt("RemainingLife") : -1;
        this.remainingDamageTicks = tag.contains("RemainingDamage") ? tag.getInt("RemainingDamage") : this.remainingLifeTicks;
        this.photonMounted = tag.getBoolean("PhotonMounted");
        this.blockClearInnerRadius = tag.contains("BlockClearInnerRadius") ? tag.getDouble("BlockClearInnerRadius") : 0.0D;
        this.blockClearFinished = tag.getBoolean("BlockClearFinished");
        this.ownerUuid = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
    }
}
