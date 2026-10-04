package com.darkness.jujutsukaisenzone.mixin.client;

import net.minecraft.client.sounds.MusicManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 永久禁用原版 MusicManager，改由模组 BGM 导演接管。 */
@Mixin(MusicManager.class)
public class MusicManagerMixin
{
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void jjkz$disableVanillaMusic(CallbackInfo ci)
    {
        ((MusicManager) (Object) this).stopPlaying();
        ci.cancel();
    }
}
