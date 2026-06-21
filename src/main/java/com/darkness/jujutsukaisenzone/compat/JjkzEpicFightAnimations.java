package com.darkness.jujutsukaisenzone.compat;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.AnimationManager.AnimationBuilder;
import yesman.epicfight.api.animation.AnimationManager.AnimationRegistryEvent;
import yesman.epicfight.gameasset.Armatures;

/**
 * 本模组 Epic Fight 自定义动画注册；资源路径为
 * {@code assets/&lt;modid&gt;/animmodels/animations/&lt;path&gt;.json}。
 */
@Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, bus = Bus.MOD)
public final class JjkzEpicFightAnimations
{
    /** {@code assets/jujutsukaisenzone/animmodels/animations/epicfight_animation_wuliang.json}，非循环 */
    public static AnimationAccessor<JjkzTechniqueCastAnimation> LIMITLESS_DOMAIN_CHARGE;
    /** {@code assets/jujutsukaisenzone/animmodels/animations/fukumamizushi.json}，非循环 */
    public static AnimationAccessor<JjkzTechniqueCastAnimation> SHRINE_DOMAIN_EXPANSION;
    /** {@code assets/jujutsukaisenzone/animmodels/animations/slash_kai_1.json}，非循环 */
    public static AnimationAccessor<JjkzTechniqueCastAnimation> SLASH_KAI_1;
    /** {@code assets/jujutsukaisenzone/animmodels/animations/slash_kai_2.json}，非循环（横斩） */
    public static AnimationAccessor<JjkzTechniqueCastAnimation> SLASH_KAI_2;
    /** {@code assets/jujutsukaisenzone/animmodels/animations/slash_hachi.json}，非循环 */
    public static AnimationAccessor<JjkzTechniqueCastAnimation> SLASH_HACHI;
    /** {@code assets/jujutsukaisenzone/animmodels/animations/fuga.json}，「灶」开长按蓄力 */
    public static AnimationAccessor<JjkzTechniqueCastAnimation> FUGA_CHARGE;
    /** {@code assets/jujutsukaisenzone/animmodels/animations/aka_short.json}，「赫」发射前摇 */
    public static AnimationAccessor<JjkzTechniqueCastAnimation> AKA_SHORT;
    /** {@code assets/jujutsukaisenzone/animmodels/animations/remote_murasaki_start.json}，超远程虚式茈起手 */
    public static AnimationAccessor<JjkzTechniqueCastAnimation> REMOTE_MURASAKI_START;
    /** {@code assets/jujutsukaisenzone/animmodels/animations/remote_murasaki_fire.json}，超远程虚式茈发射 */
    public static AnimationAccessor<JjkzTechniqueCastAnimation> REMOTE_MURASAKI_FIRE;

    private JjkzEpicFightAnimations()
    {
    }

    @SubscribeEvent
    public static void registerAnimations(AnimationRegistryEvent event)
    {
        event.newBuilder(JuJutsuKaisenZone.MODID, JjkzEpicFightAnimations::build);
    }

    private static void build(AnimationBuilder builder)
    {
        LIMITLESS_DOMAIN_CHARGE = builder.nextAccessor(
                "epicfight_animation_wuliang",
                accessor -> new JjkzTechniqueCastAnimation(accessor, Armatures.BIPED));
        SHRINE_DOMAIN_EXPANSION = builder.nextAccessor(
                "fukumamizushi",
                accessor -> new JjkzTechniqueCastAnimation(accessor, Armatures.BIPED));
        SLASH_KAI_1 = builder.nextAccessor(
                "slash_kai_1",
                accessor -> new JjkzTechniqueCastAnimation(accessor, Armatures.BIPED));
        SLASH_KAI_2 = builder.nextAccessor(
                "slash_kai_2",
                accessor -> new JjkzTechniqueCastAnimation(accessor, Armatures.BIPED));
        SLASH_HACHI = builder.nextAccessor(
                "slash_hachi",
                accessor -> new JjkzTechniqueCastAnimation(accessor, Armatures.BIPED));
        FUGA_CHARGE = builder.nextAccessor(
                "fuga",
                accessor -> new JjkzTechniqueCastAnimation(accessor, Armatures.BIPED));
        AKA_SHORT = builder.nextAccessor(
                "aka_short",
                accessor -> new JjkzTechniqueCastAnimation(accessor, Armatures.BIPED));
        REMOTE_MURASAKI_START = builder.nextAccessor(
                "remote_murasaki_start",
                accessor -> new JjkzTechniqueCastAnimation(accessor, Armatures.BIPED));
        REMOTE_MURASAKI_FIRE = builder.nextAccessor(
                "remote_murasaki_fire",
                accessor -> new JjkzTechniqueCastAnimation(accessor, Armatures.BIPED));
    }
}
