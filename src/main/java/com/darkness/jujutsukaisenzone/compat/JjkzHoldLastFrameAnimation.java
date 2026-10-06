package com.darkness.jujutsukaisenzone.compat;

import com.mojang.datafixers.util.Pair;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.property.AnimationProperty.StaticAnimationProperty;
import yesman.epicfight.api.animation.types.EntityState;
import yesman.epicfight.api.animation.types.MainFrameAnimation;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.client.animation.Layer;
import yesman.epicfight.api.client.animation.property.ClientAnimationProperties;
import yesman.epicfight.api.model.Armature;
import yesman.epicfight.main.EpicFightSharedConstants;

/**
 * 非循环演出动画：播完后把时间钉在略小于 {@code totalTime}，从而定格末帧（不进入 {@code isEnd}、不被 living motion 顶掉）。
 */
public final class JjkzHoldLastFrameAnimation extends MainFrameAnimation
{
    public JjkzHoldLastFrameAnimation(
            AnimationAccessor<? extends JjkzHoldLastFrameAnimation> accessor,
            AssetAccessor<? extends Armature> armature)
    {
        super(EpicFightSharedConstants.GENERAL_ANIMATION_TRANSITION_TIME, accessor, armature);

        this.addProperty(ClientAnimationProperties.PRIORITY, Layer.Priority.HIGHEST);
        this.stateSpectrumBlueprint.clear()
                .newTimePair(0.0F, Float.MAX_VALUE)
                .addState(EntityState.UPDATE_LIVING_MOTION, false)
                .addState(EntityState.INACTION, true);
        this.addProperty(
                StaticAnimationProperty.ELAPSED_TIME_MODIFIER,
                (self, entitypatch, speed, prevElapsedTime, elapsedTime) -> {
                    float total = self.getTotalTime();
                    // 钉在 total 会被 isEnd 判定为播完并卸层，living idle 立刻把人拉回站姿。
                    float hold = Math.max(0.0F, total - 0.001F);
                    if (elapsedTime >= hold)
                    {
                        return Pair.of(hold, hold);
                    }
                    return Pair.of(prevElapsedTime, elapsedTime);
                });
    }
}
