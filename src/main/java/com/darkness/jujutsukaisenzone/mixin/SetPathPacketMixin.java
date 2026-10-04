package com.darkness.jujutsukaisenzone.mixin;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import team.creative.cmdcam.common.packet.SetPathPacket;
import team.creative.cmdcam.common.scene.CamScene;
import team.creative.cmdcam.server.CMDCamServer;
import team.creative.creativecore.common.util.registry.exception.RegistryException;

/**
 * CMDCam 的 {@code cam save} 在服务端可用时要求 {@code hasPermissions(4)}。
 * 演出用 save/load 不能依赖玩家开作弊，这里跳过该检查。
 */
@Mixin(value = SetPathPacket.class, remap = false)
public class SetPathPacketMixin
{
    @Shadow
    public String id;

    @Shadow
    public CompoundTag nbt;

    @Inject(method = "executeServer", at = @At("HEAD"), cancellable = true)
    private void jjkz$saveSceneWithoutOp(ServerPlayer player, CallbackInfo ci)
    {
        try
        {
            CamScene path = new CamScene(this.nbt);
            CMDCamServer.set(player.level(), this.id, path);
            player.sendSystemMessage(Component.translatable("scene.save", this.id));
        }
        catch (RegistryException e)
        {
            e.printStackTrace();
            player.sendSystemMessage(Component.translatable("scenes.save_fail"));
        }
        ci.cancel();
    }
}
