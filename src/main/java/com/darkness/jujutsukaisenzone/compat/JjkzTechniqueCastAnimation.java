package com.darkness.jujutsukaisenzone.compat;

import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.types.EntityState;
import yesman.epicfight.api.animation.types.MainFrameAnimation;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.client.animation.Layer;
import yesman.epicfight.api.client.animation.property.ClientAnimationProperties;
import yesman.epicfight.api.model.Armature;
import yesman.epicfight.main.EpicFightSharedConstants;

/**
 * 术式演出动画：高优先级播放，并暂停 living motion 切换，避免被走/跑/跳/飞动作打断。
 * 不锁定玩家移动输入。
 */
public final class JjkzTechniqueCastAnimation extends MainFrameAnimation
{
    public JjkzTechniqueCastAnimation(
            AnimationAccessor<? extends JjkzTechniqueCastAnimation> accessor,
            AssetAccessor<? extends Armature> armature)
    {
        super(EpicFightSharedConstants.GENERAL_ANIMATION_TRANSITION_TIME, accessor, armature);

        this.addProperty(ClientAnimationProperties.PRIORITY, Layer.Priority.HIGHEST);
        this.stateSpectrumBlueprint.clear()
                .newTimePair(0.0F, Float.MAX_VALUE)
                .addState(EntityState.UPDATE_LIVING_MOTION, false);
    }
}
