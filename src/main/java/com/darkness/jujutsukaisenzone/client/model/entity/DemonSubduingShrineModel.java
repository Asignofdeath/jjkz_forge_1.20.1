package com.darkness.jujutsukaisenzone.client.model.entity;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.entity.DemonSubduingShrineEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

public class DemonSubduingShrineModel extends DefaultedEntityGeoModel<DemonSubduingShrineEntity>
{
    public DemonSubduingShrineModel()
    {
        super(new ResourceLocation(JuJutsuKaisenZone.MODID, "demon_subduing_shrine"));
        this.withAltAnimations(new ResourceLocation(JuJutsuKaisenZone.MODID, "shrine_rises"));
    }
}
