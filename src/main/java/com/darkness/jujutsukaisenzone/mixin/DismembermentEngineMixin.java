package com.darkness.jujutsukaisenzone.mixin;

import de.anwender.mcg.anatomy.DismembermentEngine;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 掐死 PPs 在 {@link LivingHurtEvent} 上的被动断肢。
 * <p>
 * 断肢只能经 {@link com.darkness.jujutsukaisenzone.dismemberment.DismembermentSystem} 下令。
 */
@Mixin(DismembermentEngine.class)
public final class DismembermentEngineMixin
{
    private DismembermentEngineMixin()
    {
    }

    @Inject(method = "handleDamage", at = @At("HEAD"), cancellable = true, remap = false)
    private static void jjkz$blockPassiveSever(LivingHurtEvent event, CallbackInfo ci)
    {
        ci.cancel();
    }
}
