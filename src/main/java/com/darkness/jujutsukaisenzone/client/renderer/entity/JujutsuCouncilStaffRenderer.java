package com.darkness.jujutsukaisenzone.client.renderer.entity;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.entity.JujutsuCouncilStaffEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Steve 体型 + 玩家皮肤 UV（非 slim）；头顶常显岗位名。 */
public class JujutsuCouncilStaffRenderer
        extends HumanoidMobRenderer<JujutsuCouncilStaffEntity, PlayerModel<JujutsuCouncilStaffEntity>>
{
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(JuJutsuKaisenZone.MODID, "textures/entity/jujutsu_council_staff.png");

    public JujutsuCouncilStaffRenderer(EntityRendererProvider.Context context)
    {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(JujutsuCouncilStaffEntity entity)
    {
        return TEXTURE;
    }

    @Override
    protected boolean shouldShowName(JujutsuCouncilStaffEntity entity)
    {
        return entity.isCustomNameVisible();
    }
}
