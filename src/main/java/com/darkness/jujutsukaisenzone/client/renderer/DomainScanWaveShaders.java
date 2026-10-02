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
 * 领域展开「扫描波」core shader 的注册与句柄。
 */
@Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class DomainScanWaveShaders
{
    public static final ResourceLocation SHADER_LOCATION =
            new ResourceLocation(JuJutsuKaisenZone.MODID, "domain_scan_wave");

    @Nullable
    private static ShaderInstance instance;

    private DomainScanWaveShaders()
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
