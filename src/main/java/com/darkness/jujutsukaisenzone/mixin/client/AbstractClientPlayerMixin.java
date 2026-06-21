package com.darkness.jujutsukaisenzone.mixin.client;

import com.darkness.jujutsukaisenzone.client.MimicryClientState;
import com.darkness.jujutsukaisenzone.player.mimicry.MimicryCharacterRegistry;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 拟态皮肤覆盖：拦截 {@link AbstractClientPlayer#getSkinTextureLocation()} 与 {@link AbstractClientPlayer#getModelName()}，
 * 使原版渲染与 Epic Fight {@code PPlayerRenderer} 共用同一套贴图来源。
 */
@Mixin(AbstractClientPlayer.class)
public class AbstractClientPlayerMixin
{
    @Inject(method = "getSkinTextureLocation", at = @At("HEAD"), cancellable = true)
    private void jjkz$overrideSkinTexture(CallbackInfoReturnable<ResourceLocation> cir)
    {
        AbstractClientPlayer self = (AbstractClientPlayer) (Object) this;
        MimicryClientState.getProfile(self.getUUID())
                .map(MimicryCharacterRegistry.MimicryProfile::skinTexture)
                .ifPresent(cir::setReturnValue);
    }

    @Inject(method = "getModelName", at = @At("HEAD"), cancellable = true)
    private void jjkz$overrideModelName(CallbackInfoReturnable<String> cir)
    {
        AbstractClientPlayer self = (AbstractClientPlayer) (Object) this;
        MimicryClientState.getProfile(self.getUUID()).ifPresent(profile ->
                cir.setReturnValue(profile.slim() ? "slim" : "default")
        );
    }
}
