package com.darkness.jujutsukaisenzone.technique.shikigami.tenshadows;

import com.darkness.jujutsukaisenzone.technique.shikigami.ShikigamiAbility;
import com.darkness.jujutsukaisenzone.technique.shikigami.ShikigamiCastMode;
import com.darkness.jujutsukaisenzone.technique.shikigami.tenshadows.ability.DivineDogsBiteLogic;
import com.darkness.jujutsukaisenzone.technique.shikigami.tenshadows.ability.DivineDogsPounceLogic;
import com.darkness.jujutsukaisenzone.technique.shikigami.tenshadows.ability.DivineDogsTrackLogic;
import com.darkness.jujutsukaisenzone.technique.shikigami.tenshadows.ability.GreatSerpentAmbushLogic;
import com.darkness.jujutsukaisenzone.technique.shikigami.tenshadows.ability.mahoraga.MahoragaMeleeAutos;
import com.darkness.jujutsukaisenzone.technique.shikigami.tenshadows.ability.mahoraga.MahoragaSonicLogic;
import com.darkness.jujutsukaisenzone.technique.shikigami.tenshadows.ability.MaxElephantMeteorLogic;
import com.darkness.jujutsukaisenzone.technique.shikigami.tenshadows.ability.MaxElephantRamLogic;
import com.darkness.jujutsukaisenzone.technique.shikigami.tenshadows.ability.MaxElephantWaterFloodLogic;
import com.darkness.jujutsukaisenzone.technique.shikigami.tenshadows.ability.MaxElephantWaterJetLogic;
import com.darkness.jujutsukaisenzone.technique.shikigami.tenshadows.ability.NueDiveLogic;
import com.darkness.jujutsukaisenzone.technique.shikigami.tenshadows.ability.NueLightningLogic;
import com.darkness.jujutsukaisenzone.technique.shikigami.tenshadows.ability.NueRideLogic;
import com.darkness.jujutsukaisenzone.technique.shikigami.tenshadows.ability.RabbitEscapeBurstLogic;
import com.darkness.jujutsukaisenzone.technique.shikigami.tenshadows.ability.RabbitEscapeCageLogic;
import com.darkness.jujutsukaisenzone.technique.shikigami.tenshadows.ability.PiercingOxChargeLogic;
import com.darkness.jujutsukaisenzone.technique.shikigami.tenshadows.ability.RoundDeerNeutralizeLogic;
import com.darkness.jujutsukaisenzone.technique.shikigami.tenshadows.ability.RoundDeerPositiveEnergyLogic;
import com.darkness.jujutsukaisenzone.technique.shikigami.tenshadows.ability.TigerFuneralRoarLogic;
import com.darkness.jujutsukaisenzone.technique.shikigami.tenshadows.ability.ToadPullLogic;
import com.darkness.jujutsukaisenzone.technique.shikigami.tenshadows.ability.ToadRestrainLogic;
import com.darkness.jujutsukaisenzone.technique.skill.TenShadowsShikigami;
import net.minecraft.network.chat.Component;

import java.util.List;

/** 十种影式神能力表。 */
public final class TenShadowsAbilities
{
    public static final String TRACK = "divine_dogs_track";
    public static final String BITE = "divine_dogs_bite";
    public static final String POUNCE = "divine_dogs_pounce";

    public static final String NUE_LIGHTNING = "nue_lightning";
    public static final String NUE_RIDE = "nue_ride";
    public static final String NUE_DIVE = "nue_dive";

    public static final String SERPENT_AMBUSH = "great_serpent_ambush";

    public static final String TOAD_RESTRAIN = "toad_restrain";
    public static final String TOAD_PULL = "toad_pull";

    public static final String ELEPHANT_WATER_JET = "max_elephant_water_jet";
    public static final String ELEPHANT_WATER_FLOOD = "max_elephant_water_flood";
    public static final String ELEPHANT_METEOR = "max_elephant_meteor";
    public static final String ELEPHANT_RAM = "max_elephant_ram";

    public static final String RABBIT_BURST = "rabbit_escape_burst";
    public static final String RABBIT_CAGE = "rabbit_escape_cage";

    public static final String DEER_POSITIVE = "round_deer_positive_energy";
    public static final String DEER_NEUTRALIZE = "round_deer_neutralize";

    public static final String OX_CHARGE = "piercing_ox_charge";

    public static final String TIGER_ROAR = "tiger_funeral_roar";

    public static final String MAHORAGA_SONIC = "mahoraga_sonic";
    public static final String MAHORAGA_SLAM = "mahoraga_slam";
    public static final String MAHORAGA_LAUNCH = "mahoraga_launch";
    public static final String MAHORAGA_THROW = "mahoraga_throw";
    public static final String MAHORAGA_PUNCH = "mahoraga_punch_combo";
    public static final String MAHORAGA_SWORD = "mahoraga_sword_combo";
    public static final String MAHORAGA_WHEEL = "mahoraga_wheel_slash";

    private static final String DOGS = TenShadowsShikigami.DIVINE_DOGS.id();
    private static final String NUE = TenShadowsShikigami.NUE.id();
    private static final String SERPENT = TenShadowsShikigami.GREAT_SERPENT.id();
    private static final String TOAD = TenShadowsShikigami.TOAD.id();
    private static final String ELEPHANT = TenShadowsShikigami.MAX_ELEPHANT.id();
    private static final String RABBIT = TenShadowsShikigami.RABBIT_ESCAPE.id();
    private static final String DEER = TenShadowsShikigami.ROUND_DEER.id();
    private static final String OX = TenShadowsShikigami.PIERCING_OX.id();
    private static final String TIGER = TenShadowsShikigami.TIGER_FUNERAL.id();
    private static final String MAHORAGA = TenShadowsShikigami.MAHORAGA.id();

    private static final int TRACK_CD = 20 * 8;
    private static final int BITE_CD = 20 * 4;
    private static final int POUNCE_CD = 20 * 6;

    private static final int NUE_LIGHTNING_CD = 20 * 8;
    private static final int NUE_DIVE_CD = 20 * 6;

    private static final int SERPENT_AMBUSH_CD = 20 * 10;

    private static final int TOAD_RESTRAIN_CD = 20 * 5;
    private static final int TOAD_PULL_CD = 20 * 6;

    private static final int ELEPHANT_JET_CD = 20 * 6;
    private static final int ELEPHANT_FLOOD_CD = 20 * 8;
    private static final int ELEPHANT_METEOR_CD = 20 * 14;
    private static final int ELEPHANT_RAM_CD = 20 * 4;

    private static final int RABBIT_BURST_CD = 20 * 12;
    private static final int RABBIT_CAGE_CD = 20 * 8;

    private static final int DEER_POSITIVE_CD = 20 * 6;
    private static final int DEER_NEUTRALIZE_CD = 20 * 5;

    private static final int OX_CHARGE_CD = 20 * 8;

    private static final int TIGER_ROAR_CD = 20 * 10;

    private static final int MAHORAGA_SONIC_CD = 20 * 8;
    private static final int MAHORAGA_SLAM_CD = 20 * 6;
    private static final int MAHORAGA_LAUNCH_CD = 20 * 5;
    private static final int MAHORAGA_THROW_CD = 20 * 6;
    private static final int MAHORAGA_PUNCH_CD = 20 * 5;
    private static final int MAHORAGA_SWORD_CD = 20 * 5;
    private static final int MAHORAGA_WHEEL_CD = 20 * 7;

    public static final List<ShikigamiAbility> ALL = List.of(
            new ShikigamiAbility(
                    TRACK,
                    DOGS,
                    ShikigamiCastMode.ACTIVE,
                    ShikigamiAbility.MOD_SHIFT,
                    Component.literal("玉犬 · 追踪"),
                    10,
                    TRACK_CD,
                    0,
                    DivineDogsTrackLogic::cast),
            new ShikigamiAbility(
                    BITE,
                    DOGS,
                    ShikigamiCastMode.AUTO,
                    0,
                    Component.literal("玉犬 · 撕咬"),
                    0,
                    BITE_CD,
                    10,
                    DivineDogsBiteLogic::cast),
            new ShikigamiAbility(
                    POUNCE,
                    DOGS,
                    ShikigamiCastMode.AUTO,
                    0,
                    Component.literal("玉犬 · 扑击"),
                    0,
                    POUNCE_CD,
                    20,
                    DivineDogsPounceLogic::cast),
            new ShikigamiAbility(
                    NUE_LIGHTNING,
                    NUE,
                    ShikigamiCastMode.BOTH,
                    ShikigamiAbility.MOD_SHIFT,
                    Component.literal("鵺 · 雷击"),
                    15,
                    NUE_LIGHTNING_CD,
                    10,
                    NueLightningLogic::cast),
            new ShikigamiAbility(
                    NUE_RIDE,
                    NUE,
                    ShikigamiCastMode.ACTIVE,
                    ShikigamiAbility.MOD_CTRL,
                    Component.literal("鵺 · 骑乘"),
                    0,
                    0,
                    0,
                    NueRideLogic::cast),
            new ShikigamiAbility(
                    NUE_DIVE,
                    NUE,
                    ShikigamiCastMode.AUTO,
                    0,
                    Component.literal("鵺 · 俯冲"),
                    0,
                    NUE_DIVE_CD,
                    20,
                    NueDiveLogic::cast),
            new ShikigamiAbility(
                    SERPENT_AMBUSH,
                    SERPENT,
                    ShikigamiCastMode.BOTH,
                    ShikigamiAbility.MOD_SHIFT,
                    Component.literal("大蛇 · 突袭"),
                    18,
                    SERPENT_AMBUSH_CD,
                    10,
                    GreatSerpentAmbushLogic::cast),
            new ShikigamiAbility(
                    TOAD_RESTRAIN,
                    TOAD,
                    ShikigamiCastMode.AUTO,
                    0,
                    Component.literal("蝦蟇 · 牵制"),
                    0,
                    TOAD_RESTRAIN_CD,
                    10,
                    ToadRestrainLogic::cast),
            new ShikigamiAbility(
                    TOAD_PULL,
                    TOAD,
                    ShikigamiCastMode.ACTIVE,
                    ShikigamiAbility.MOD_SHIFT,
                    Component.literal("蝦蟇 · 牵引"),
                    12,
                    TOAD_PULL_CD,
                    0,
                    ToadPullLogic::cast),
            new ShikigamiAbility(
                    ELEPHANT_WATER_JET,
                    ELEPHANT,
                    ShikigamiCastMode.BOTH,
                    ShikigamiAbility.MOD_SHIFT,
                    Component.literal("满象 · 水柱"),
                    14,
                    ELEPHANT_JET_CD,
                    20,
                    MaxElephantWaterJetLogic::cast),
            new ShikigamiAbility(
                    ELEPHANT_WATER_FLOOD,
                    ELEPHANT,
                    ShikigamiCastMode.BOTH,
                    ShikigamiAbility.MOD_CTRL,
                    Component.literal("满象 · 水流"),
                    16,
                    ELEPHANT_FLOOD_CD,
                    10,
                    MaxElephantWaterFloodLogic::cast),
            new ShikigamiAbility(
                    ELEPHANT_METEOR,
                    ELEPHANT,
                    ShikigamiCastMode.ACTIVE,
                    ShikigamiAbility.MOD_SHIFT | ShikigamiAbility.MOD_CTRL,
                    Component.literal("满象 · 陨石坠"),
                    28,
                    ELEPHANT_METEOR_CD,
                    0,
                    MaxElephantMeteorLogic::cast),
            new ShikigamiAbility(
                    ELEPHANT_RAM,
                    ELEPHANT,
                    ShikigamiCastMode.AUTO,
                    0,
                    Component.literal("满象 · 顶击"),
                    0,
                    ELEPHANT_RAM_CD,
                    30,
                    MaxElephantRamLogic::cast),
            new ShikigamiAbility(
                    RABBIT_BURST,
                    RABBIT,
                    ShikigamiCastMode.ACTIVE,
                    ShikigamiAbility.MOD_SHIFT,
                    Component.literal("脱兔 · 爆发"),
                    20,
                    RABBIT_BURST_CD,
                    0,
                    RabbitEscapeBurstLogic::cast),
            new ShikigamiAbility(
                    RABBIT_CAGE,
                    RABBIT,
                    ShikigamiCastMode.ACTIVE,
                    ShikigamiAbility.MOD_CTRL,
                    Component.literal("脱兔 · 囚笼"),
                    14,
                    RABBIT_CAGE_CD,
                    0,
                    RabbitEscapeCageLogic::cast),
            new ShikigamiAbility(
                    DEER_NEUTRALIZE,
                    DEER,
                    ShikigamiCastMode.AUTO,
                    0,
                    Component.literal("圆鹿 · 中和"),
                    0,
                    DEER_NEUTRALIZE_CD,
                    20,
                    RoundDeerNeutralizeLogic::cast),
            new ShikigamiAbility(
                    DEER_POSITIVE,
                    DEER,
                    ShikigamiCastMode.AUTO,
                    0,
                    Component.literal("圆鹿 · 正向能量"),
                    0,
                    DEER_POSITIVE_CD,
                    10,
                    RoundDeerPositiveEnergyLogic::cast),
            new ShikigamiAbility(
                    OX_CHARGE,
                    OX,
                    ShikigamiCastMode.BOTH,
                    ShikigamiAbility.MOD_SHIFT,
                    Component.literal("贯牛 · 蓄力冲锋"),
                    18,
                    OX_CHARGE_CD,
                    10,
                    PiercingOxChargeLogic::cast),
            new ShikigamiAbility(
                    TIGER_ROAR,
                    TIGER,
                    ShikigamiCastMode.BOTH,
                    ShikigamiAbility.MOD_SHIFT,
                    Component.literal("虎葬 · 咆哮"),
                    20,
                    TIGER_ROAR_CD,
                    10,
                    TigerFuneralRoarLogic::cast),
            new ShikigamiAbility(
                    MAHORAGA_SONIC,
                    MAHORAGA,
                    ShikigamiCastMode.BOTH,
                    ShikigamiAbility.MOD_SHIFT,
                    Component.literal("魔虚罗 · 声波攻击"),
                    25,
                    MAHORAGA_SONIC_CD,
                    8,
                    MahoragaSonicLogic::cast),
            new ShikigamiAbility(
                    MAHORAGA_WHEEL,
                    MAHORAGA,
                    ShikigamiCastMode.AUTO,
                    0,
                    Component.literal("魔虚罗 · 劈径斩"),
                    0,
                    MAHORAGA_WHEEL_CD,
                    35,
                    MahoragaMeleeAutos::castWheelSlash),
            new ShikigamiAbility(
                    MAHORAGA_SWORD,
                    MAHORAGA,
                    ShikigamiCastMode.AUTO,
                    0,
                    Component.literal("魔虚罗 · 连续挥剑"),
                    0,
                    MAHORAGA_SWORD_CD,
                    30,
                    MahoragaMeleeAutos::castSwordCombo),
            new ShikigamiAbility(
                    MAHORAGA_PUNCH,
                    MAHORAGA,
                    ShikigamiCastMode.AUTO,
                    0,
                    Component.literal("魔虚罗 · 连续拳击"),
                    0,
                    MAHORAGA_PUNCH_CD,
                    25,
                    MahoragaMeleeAutos::castPunchCombo),
            new ShikigamiAbility(
                    MAHORAGA_SLAM,
                    MAHORAGA,
                    ShikigamiCastMode.AUTO,
                    0,
                    Component.literal("魔虚罗 · 砸地"),
                    0,
                    MAHORAGA_SLAM_CD,
                    20,
                    MahoragaMeleeAutos::castSlam),
            new ShikigamiAbility(
                    MAHORAGA_LAUNCH,
                    MAHORAGA,
                    ShikigamiCastMode.AUTO,
                    0,
                    Component.literal("魔虚罗 · 击飞"),
                    0,
                    MAHORAGA_LAUNCH_CD,
                    18,
                    MahoragaMeleeAutos::castLaunch),
            new ShikigamiAbility(
                    MAHORAGA_THROW,
                    MAHORAGA,
                    ShikigamiCastMode.AUTO,
                    0,
                    Component.literal("魔虚罗 · 投掷攻击"),
                    0,
                    MAHORAGA_THROW_CD,
                    15,
                    MahoragaMeleeAutos::castThrow));

    private TenShadowsAbilities()
    {
    }
}
