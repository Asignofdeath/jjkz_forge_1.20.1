package com.darkness.jujutsukaisenzone.registry;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.entity.DemonSubduingShrineEntity;
import com.darkness.jujutsukaisenzone.entity.SpecialEffectEntity;
import com.darkness.jujutsukaisenzone.entity.WedgeEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntities
{
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, JuJutsuKaisenZone.MODID);

    public static final RegistryObject<EntityType<SpecialEffectEntity>> SPECIAL_EFFECT_ENTITY =
            ENTITY_TYPES.register("special_effect_entity",
                    () -> EntityType.Builder.of(SpecialEffectEntity::new, MobCategory.CREATURE)
                            .sized(0.4F, 0.7F)
                            .clientTrackingRange(10)
                            .build("special_effect_entity"));

    /** 伏魔神龛 */
    public static final RegistryObject<EntityType<DemonSubduingShrineEntity>> DEMON_SUBDUING_SHRINE =
            ENTITY_TYPES.register("demon_subduing_shrine",
                    () -> EntityType.Builder.<DemonSubduingShrineEntity>of(DemonSubduingShrineEntity::new, MobCategory.MISC)
                            .sized(10.0F, 8.0F)
                            .clientTrackingRange(16)
                            .updateInterval(5)
                            .fireImmune()
                            .build("demon_subduing_shrine"));

    /** 天元之楔 */
    public static final RegistryObject<EntityType<WedgeEntity>> WEDGE =
            ENTITY_TYPES.register("wedge",
                    () -> EntityType.Builder.<WedgeEntity>of(WedgeEntity::new, MobCategory.MISC)
                            .sized(2.0F, 2.0F)
                            .clientTrackingRange(16)
                            .updateInterval(Integer.MAX_VALUE)
                            .build("wedge"));

    private ModEntities()
    {
    }

    public static void register(IEventBus bus)
    {
        ENTITY_TYPES.register(bus);
    }
}
