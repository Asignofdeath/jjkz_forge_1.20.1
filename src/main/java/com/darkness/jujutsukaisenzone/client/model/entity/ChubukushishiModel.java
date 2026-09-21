package com.darkness.jujutsukaisenzone.client.model.entity;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.entity.ChubukushishiEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

public class ChubukushishiModel extends DefaultedEntityGeoModel<ChubukushishiEntity>
{
    public ChubukushishiModel()
    {
        super(ResourceLocation.fromNamespaceAndPath(JuJutsuKaisenZone.MODID, "chubukushishi"));
        this.withAltAnimations(ResourceLocation.fromNamespaceAndPath(JuJutsuKaisenZone.MODID, "chubukushishi_animation"));
    }
}
