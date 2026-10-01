package com.darkness.jujutsukaisenzone.mixin;

import com.darkness.jujutsukaisenzone.technique.TechniqueDamageSources;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * EpicFight / 原版近战无法换成 {@code NarrativeDamageSource}，
 * 用 {@link TechniqueDamageSources#projectDisplayType} 挂上的叙事 message_id 在此注入 {@link DamageSource#getMsgId()}。
 */
@Mixin(DamageSource.class)
public class DamageSourceMixin
{
    @Inject(method = "getMsgId", at = @At("HEAD"), cancellable = true)
    private void jjkz$narrativeDisplayMsgId(CallbackInfoReturnable<String> cir)
    {
        String override = TechniqueDamageSources.peekDisplayMsgId((DamageSource) (Object) this);
        if (override != null)
        {
            cir.setReturnValue(override);
        }
    }
}
