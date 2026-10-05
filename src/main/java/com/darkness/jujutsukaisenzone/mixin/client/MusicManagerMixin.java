package com.darkness.jujutsukaisenzone.mixin.client;

import net.minecraft.client.sounds.MusicManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 永久禁用原版 MusicManager 点播。
 * 只 cancel tick，不要每拍 {@code stopPlaying()}：那会把模组走 {@code SoundSource.MUSIC} 的曲一起掐掉。
 */
@Mixin(MusicManager.class)
public class MusicManagerMixin
{
    @Unique
    private boolean jjkz$vanillaStopped;

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void jjkz$disableVanillaMusic(CallbackInfo ci)
    {
        if (!this.jjkz$vanillaStopped)
        {
            ((MusicManager) (Object) this).stopPlaying();
            this.jjkz$vanillaStopped = true;
        }
        ci.cancel();
    }
}
