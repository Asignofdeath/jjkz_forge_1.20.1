package com.darkness.jujutsukaisenzone.registry;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.entity.ChubukushishiEntity;
import com.darkness.jujutsukaisenzone.entity.CourtSeatEntity;
import com.darkness.jujutsukaisenzone.entity.CutsceneActorEntity;
import com.darkness.jujutsukaisenzone.entity.DemonSubduingShrineEntity;
import com.darkness.jujutsukaisenzone.entity.Grade3CurseSpiritAEntity;
import com.darkness.jujutsukaisenzone.entity.JudgeEntity;
import com.darkness.jujutsukaisenzone.entity.GavelProjectileEntity;
import com.darkness.jujutsukaisenzone.entity.JadeDogEntity;
import com.darkness.jujutsukaisenzone.entity.JujutsuCouncilStaffEntity;
import com.darkness.jujutsukaisenzone.entity.MahoragaEntity;
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

    /** 审判者 */
    public static final RegistryObject<EntityType<JudgeEntity>> JUDGE =
            ENTITY_TYPES.register("judge",
                    () -> EntityType.Builder.<JudgeEntity>of(JudgeEntity::new, MobCategory.MISC)
                            .sized(9.0F, 8.5F)
                            .clientTrackingRange(16)
                            .updateInterval(5)
                            .fireImmune()
                            .build("judge"));

    /** 法庭座位 */
    public static final RegistryObject<EntityType<CourtSeatEntity>> COURT_SEAT =
            ENTITY_TYPES.register("court_seat",
                    () -> EntityType.Builder.<CourtSeatEntity>of(CourtSeatEntity::new, MobCategory.MISC)
                            .sized(2.0F, 1.5F)
                            .clientTrackingRange(16)
                            .updateInterval(5)
                            .fireImmune()
                            .build("court_seat"));

    /** 处刑架圈景观 */
    public static final RegistryObject<EntityType<ChubukushishiEntity>> EXECUTION_SCAFFOLD_CIRCLE =
            ENTITY_TYPES.register("chubukushishi",
                    () -> EntityType.Builder.<ChubukushishiEntity>of(ChubukushishiEntity::new, MobCategory.MISC)
                            .sized(18.0F, 15.0F)
                            .clientTrackingRange(16)
                            .updateInterval(5)
                            .fireImmune()
                            .build("chubukushishi"));

    /** 法槌投射物 */
    public static final RegistryObject<EntityType<GavelProjectileEntity>> GAVEL_PROJECTILE =
            ENTITY_TYPES.register("gavel_projectile",
                    () -> EntityType.Builder.<GavelProjectileEntity>of(GavelProjectileEntity::new, MobCategory.MISC)
                            .sized(0.5F, 0.5F)
                            .clientTrackingRange(4)
                            .updateInterval(10)
                            .build("gavel_projectile"));

    /** 十种影法·玉犬 */
    public static final RegistryObject<EntityType<JadeDogEntity>> JADE_DOG =
            ENTITY_TYPES.register("jade_dog",
                    () -> EntityType.Builder.<JadeDogEntity>of(JadeDogEntity::new, MobCategory.CREATURE)
                            .sized(0.7F, 0.95F)
                            .clientTrackingRange(10)
                            .build("jade_dog"));

    /** 十种影法·魔虚罗 */
    public static final RegistryObject<EntityType<MahoragaEntity>> MAHORAGA =
            ENTITY_TYPES.register("mahoraga",
                    () -> EntityType.Builder.<MahoragaEntity>of(MahoragaEntity::new, MobCategory.CREATURE)
                            .sized(1.2F, 2.9F)
                            .clientTrackingRange(10)
                            .fireImmune()
                            .build("mahoraga"));

    /** 天元之楔 */
    public static final RegistryObject<EntityType<WedgeEntity>> WEDGE =
            ENTITY_TYPES.register("wedge",
                    () -> EntityType.Builder.<WedgeEntity>of(WedgeEntity::new, MobCategory.MISC)
                            .sized(2.0F, 2.0F)
                            .clientTrackingRange(16)
                            .updateInterval(Integer.MAX_VALUE)
                            .build("wedge"));

    /** 咒术议会工作人员（Steve 体型站桩联络 NPC） */
    public static final RegistryObject<EntityType<JujutsuCouncilStaffEntity>> JUJUTSU_COUNCIL_STAFF =
            ENTITY_TYPES.register("jujutsu_council_staff",
                    () -> EntityType.Builder.<JujutsuCouncilStaffEntity>of(
                                    JujutsuCouncilStaffEntity::new, MobCategory.MISC)
                            .sized(0.6F, 1.8F)
                            .clientTrackingRange(10)
                            .fireImmune()
                            .build("jujutsu_council_staff"));

    /** CG / 任务演出 Geo 演员（无 AI，不进存档） */
    public static final RegistryObject<EntityType<CutsceneActorEntity>> CUTSCENE_ACTOR =
            ENTITY_TYPES.register("cutscene_actor",
                    () -> EntityType.Builder.<CutsceneActorEntity>of(
                                    CutsceneActorEntity::new, MobCategory.MISC)
                            .sized(0.6F, 1.8F)
                            .clientTrackingRange(16)
                            .updateInterval(1)
                            .fireImmune()
                            .build("cutscene_actor"));

    /** 三级咒灵甲（暂用尸壳模型占位） */
    public static final RegistryObject<EntityType<Grade3CurseSpiritAEntity>> GRADE_3_CURSE_SPIRIT_A =
            ENTITY_TYPES.register("grade_3_curse_spirit_a",
                    () -> EntityType.Builder.<Grade3CurseSpiritAEntity>of(
                                    Grade3CurseSpiritAEntity::new, MobCategory.MONSTER)
                            .sized(0.6F, 1.95F)
                            .clientTrackingRange(8)
                            .build("grade_3_curse_spirit_a"));

    private ModEntities()
    {
    }

    public static void register(IEventBus bus)
    {
        ENTITY_TYPES.register(bus);
    }
}
