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
    /** {@code assets/jujutsukaisenzone/animmodels/animations/lookdown.json}，诛伏赐死领域构建完成 */
    public static AnimationAccessor<JjkzTechniqueCastAnimation> JUDGMENT_DOMAIN_LOOKDOWN;
    /** {@code assets/jujutsukaisenzone/animmodels/animations/swing_the_gavel.json}，开庭敲槌 */
    public static AnimationAccessor<JjkzTechniqueCastAnimation> SWING_THE_GAVEL;
    /** {@code assets/jujutsukaisenzone/animmodels/animations/flying_turn.json}，「飞行，穿越」起手（飞踢前摇） */
    public static AnimationAccessor<JjkzTechniqueCastAnimation> FLYING_TURN;
    /** {@code assets/jujutsukaisenzone/animmodels/animations/flying_turn_kick.json}，「飞行，穿越」踢击 */
    public static AnimationAccessor<JjkzTechniqueCastAnimation> FLYING_TURN_KICK;
    /**
     * {@code assets/jujutsukaisenzone/animmodels/animations/the_player_fell_to_the_ground.json}，
     * 主线 1.3 开场倒地（定格末帧）。
     */
    public static AnimationAccessor<JjkzHoldLastFrameAnimation> THE_PLAYER_FELL_TO_THE_GROUND;

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
        JUDGMENT_DOMAIN_LOOKDOWN = builder.nextAccessor(
                "lookdown",
                accessor -> new JjkzTechniqueCastAnimation(accessor, Armatures.BIPED));
        SWING_THE_GAVEL = builder.nextAccessor(
                "swing_the_gavel",
                accessor -> new JjkzTechniqueCastAnimation(accessor, Armatures.BIPED));
        FLYING_TURN = builder.nextAccessor(
                "flying_turn",
                accessor -> new JjkzTechniqueCastAnimation(accessor, Armatures.BIPED));
        FLYING_TURN_KICK = builder.nextAccessor(
                "flying_turn_kick",
                accessor -> new JjkzTechniqueCastAnimation(accessor, Armatures.BIPED));
        THE_PLAYER_FELL_TO_THE_GROUND = builder.nextAccessor(
                "the_player_fell_to_the_ground",
                accessor -> new JjkzHoldLastFrameAnimation(accessor, Armatures.BIPED));
    }
}
