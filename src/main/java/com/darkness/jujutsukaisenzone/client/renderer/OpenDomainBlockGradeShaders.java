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
 * 开放式无量空处：球内方块深度遮罩后处理 shader。
 */
@Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class OpenDomainBlockGradeShaders
{
    public static final ResourceLocation SHADER_LOCATION =
            new ResourceLocation(JuJutsuKaisenZone.MODID, "open_domain_block_grade");

    @Nullable
    private static ShaderInstance instance;

    private OpenDomainBlockGradeShaders()
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
