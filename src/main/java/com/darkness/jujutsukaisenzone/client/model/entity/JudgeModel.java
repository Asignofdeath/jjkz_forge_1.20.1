package com.darkness.jujutsukaisenzone.client.model.entity;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.entity.JudgeEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

public class JudgeModel extends DefaultedEntityGeoModel<JudgeEntity>
{
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(JuJutsuKaisenZone.MODID, "textures/entity/judge_texture22222.png");

    public JudgeModel()
    {
        super(ResourceLocation.fromNamespaceAndPath(JuJutsuKaisenZone.MODID, "judge"));
        this.withAltAnimations(ResourceLocation.fromNamespaceAndPath(JuJutsuKaisenZone.MODID, "judge_appears_animation"));
        this.withAltAnimations(ResourceLocation.fromNamespaceAndPath(JuJutsuKaisenZone.MODID, "judge_angry_animation"));
    }

    @Override
    public ResourceLocation getTextureResource(JudgeEntity animatable)
    {
        return TEXTURE;
    }
}
