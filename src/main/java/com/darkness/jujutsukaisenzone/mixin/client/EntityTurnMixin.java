package com.darkness.jujutsukaisenzone.mixin.client;

import com.darkness.jujutsukaisenzone.client.CgLookClampController;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** COD 式 CG 夹视角：在 {@link Entity#turn} 里截住鼠标增量。 */
@Mixin(Entity.class)
public class EntityTurnMixin
{
    @Inject(method = "turn", at = @At("HEAD"), cancellable = true)
    private void jjkz$clampCgLook(double yaw, double pitch, CallbackInfo ci)
    {
        if (CgLookClampController.consumeTurn((Entity) (Object) this, yaw, pitch))
        {
            ci.cancel();
        }
    }
}
