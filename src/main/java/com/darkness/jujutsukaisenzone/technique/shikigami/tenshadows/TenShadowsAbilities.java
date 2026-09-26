package com.darkness.jujutsukaisenzone.technique.shikigami.tenshadows;

import com.darkness.jujutsukaisenzone.technique.shikigami.ShikigamiAbility;
import com.darkness.jujutsukaisenzone.technique.shikigami.ShikigamiCastMode;
import com.darkness.jujutsukaisenzone.technique.shikigami.tenshadows.ability.DivineDogsBiteLogic;
import com.darkness.jujutsukaisenzone.technique.shikigami.tenshadows.ability.DivineDogsPounceLogic;
import com.darkness.jujutsukaisenzone.technique.shikigami.tenshadows.ability.DivineDogsTrackLogic;
import com.darkness.jujutsukaisenzone.technique.skill.TenShadowsShikigami;
import net.minecraft.network.chat.Component;

import java.util.List;

/** 十种影式神能力表（首批：玉犬三招占位）。 */
public final class TenShadowsAbilities
{
    public static final String TRACK = "divine_dogs_track";
    public static final String BITE = "divine_dogs_bite";
    public static final String POUNCE = "divine_dogs_pounce";

    private static final String DOGS = TenShadowsShikigami.DIVINE_DOGS.id();

    /** 玉犬公用 CD：主动追踪略长。 */
    private static final int TRACK_CD = 20 * 8;
    private static final int BITE_CD = 20 * 4;
    private static final int POUNCE_CD = 20 * 6;

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
                    DivineDogsPounceLogic::cast));

    private TenShadowsAbilities()
    {
    }
}
