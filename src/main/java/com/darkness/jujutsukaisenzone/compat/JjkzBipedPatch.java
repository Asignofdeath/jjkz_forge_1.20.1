package com.darkness.jujutsukaisenzone.compat;

import com.google.common.collect.ImmutableMap;
import net.minecraft.world.entity.PathfinderMob;
import yesman.epicfight.api.animation.AnimationPlayer;
import yesman.epicfight.api.animation.Animator;
import yesman.epicfight.api.animation.LivingMotions;
import yesman.epicfight.api.animation.types.EntityState;
import yesman.epicfight.api.utils.datastruct.TypeFlexibleHashMap;
import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.world.capabilities.entitypatch.Factions;
import yesman.epicfight.world.capabilities.entitypatch.HumanoidMobPatch;
import yesman.epicfight.world.capabilities.item.CapabilityItem;
import yesman.epicfight.world.capabilities.item.CapabilityItem.WeaponCategories;
import yesman.epicfight.world.entity.ai.goal.CombatBehaviors;
import yesman.epicfight.world.entity.ai.goal.CombatBehaviors.Behavior;
import yesman.epicfight.world.entity.ai.goal.CombatBehaviors.BehaviorSeries;

/**
 * 本模组 EF 基础人形：玩家 BIPED 站走与徒手连段。自定义实体共用这一个类，不要再按角色开 Patch。
 */
public class JjkzBipedPatch extends HumanoidMobPatch<PathfinderMob>
{
    private static final CombatBehaviors.Builder<HumanoidMobPatch<?>> PLAYER_FIST =
            CombatBehaviors.<HumanoidMobPatch<?>>builder()
                    .newBehaviorSeries(
                            BehaviorSeries.<HumanoidMobPatch<?>>builder()
                                    .weight(100.0F)
                                    .canBeInterrupted(true)
                                    .looping(true)
                                    .nextBehavior(Behavior.<HumanoidMobPatch<?>>builder()
                                            .animationBehavior(Animations.FIST_AUTO1)
                                            .withinEyeHeight()
                                            .withinDistance(0.0D, 2.0D))
                                    .nextBehavior(Behavior.<HumanoidMobPatch<?>>builder()
                                            .animationBehavior(Animations.FIST_AUTO2)
                                            .withinEyeHeight()
                                            .withinDistance(0.0D, 2.0D))
                                    .nextBehavior(Behavior.<HumanoidMobPatch<?>>builder()
                                            .animationBehavior(Animations.FIST_AUTO3)
                                            .withinEyeHeight()
                                            .withinDistance(0.0D, 2.0D))
                    );

    public JjkzBipedPatch()
    {
        super(Factions.NEUTRAL);
    }

    @Override
    public void initAnimator(Animator animator)
    {
        super.initAnimator(animator);
        animator.addLivingAnimation(LivingMotions.IDLE, Animations.BIPED_IDLE);
        animator.addLivingAnimation(LivingMotions.WALK, Animations.BIPED_WALK);
        animator.addLivingAnimation(LivingMotions.RUN, Animations.BIPED_RUN);
        animator.addLivingAnimation(LivingMotions.CHASE, Animations.BIPED_RUN);
        animator.addLivingAnimation(LivingMotions.JUMP, Animations.BIPED_JUMP);
        animator.addLivingAnimation(LivingMotions.FALL, Animations.BIPED_FALL);
        animator.addLivingAnimation(LivingMotions.SWIM, Animations.BIPED_SWIM);
        animator.addLivingAnimation(LivingMotions.FLOAT, Animations.BIPED_FLOAT);
        animator.addLivingAnimation(LivingMotions.MOUNT, Animations.BIPED_MOUNT);
        animator.addLivingAnimation(LivingMotions.DEATH, Animations.BIPED_DEATH);
    }

    @Override
    protected void setWeaponMotions()
    {
        super.setWeaponMotions();
        this.weaponAttackMotions.put(
                WeaponCategories.FIST,
                ImmutableMap.of(CapabilityItem.Styles.COMMON, PLAYER_FIST));
        this.weaponAttackMotions.put(
                WeaponCategories.RANGED,
                ImmutableMap.of(CapabilityItem.Styles.COMMON, PLAYER_FIST));
    }

    @Override
    protected CombatBehaviors.Builder<HumanoidMobPatch<?>> getHoldingItemWeaponMotionBuilder()
    {
        CombatBehaviors.Builder<HumanoidMobPatch<?>> builder = super.getHoldingItemWeaponMotionBuilder();
        if (this.original.getMainHandItem().isEmpty())
        {
            return PLAYER_FIST;
        }
        return builder;
    }

    @Override
    public void updateMotion(boolean considerInaction)
    {
        if (this.isPlayingTechniqueCast())
        {
            this.currentLivingMotion = LivingMotions.INACTION;
            return;
        }
        super.commonAggressiveMobUpdateMotion(considerInaction);
    }

    /**
     * 术式片播放期间视为 inaction，避免 {@code AnimatedAttackGoal} 出拳把施法动画盖掉。
     */
    @Override
    public EntityState getEntityState()
    {
        EntityState base = super.getEntityState();
        if (!this.isPlayingTechniqueCast())
        {
            return base;
        }
        TypeFlexibleHashMap<EntityState.StateFactor<?>> copy = new TypeFlexibleHashMap<>(false);
        base.getStateMap().forEach(copy::put);
        EntityState locked = new EntityState(copy);
        locked.setState(EntityState.INACTION, true);
        locked.setState(EntityState.CAN_BASIC_ATTACK, false);
        locked.setState(EntityState.UPDATE_LIVING_MOTION, false);
        return locked;
    }

    private boolean isPlayingTechniqueCast()
    {
        Animator animator = this.getAnimator();
        if (animator == null)
        {
            return false;
        }
        AnimationPlayer player = animator.getPlayerFor(null);
        if (player == null || player.isEmpty() || player.isEnd())
        {
            return false;
        }
        return player.getAnimation().get() instanceof JjkzTechniqueCastAnimation;
    }
}
