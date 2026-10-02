package com.darkness.jujutsukaisenzone.client.renderer;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;
import java.io.IOException;

/**
 * 屏幕空间径向扭曲（调试用）core shader。
 */
@Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class SpaceWarpShaders
{
    public static final ResourceLocation SHADER_LOCATION =
            new ResourceLocation(JuJutsuKaisenZone.MODID, "space_warp");

    @Nullable
    private static ShaderInstance instance;

    private SpaceWarpShaders()
    {
    }

    @Nullable
    public static ShaderInstance get()
    {
        return instance;
    }

    @SubscribeEvent
    public static void onRegisterShaders(RegisterShadersEvent event) throws IOException
    {
        event.registerShader(
                new ShaderInstance(event.getResourceProvider(), SHADER_LOCATION, DefaultVertexFormat.POSITION_TEX),
                shader -> instance = shader
        );
    }
}
