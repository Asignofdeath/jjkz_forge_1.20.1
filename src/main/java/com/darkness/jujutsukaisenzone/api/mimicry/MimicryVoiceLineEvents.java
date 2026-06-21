package com.darkness.jujutsukaisenzone.api.mimicry;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;

/**
 * 本模组内置的拟态台词触发点常量。其他模组请自行 {@link MimicryVoiceLineEvent#of} 定义 event id。
 */
public final class MimicryVoiceLineEvents
{
    /** 「解」挥砍（竖斩 / 横斩） */
    public static final MimicryVoiceLineEvent SHRINE_KAI_SLASH =
            MimicryVoiceLineEvent.of(JuJutsuKaisenZone.MODID, "shrine_kai_slash");
    /** 「灶」开发射 */
    public static final MimicryVoiceLineEvent SHRINE_FUGA_LAUNCH =
            MimicryVoiceLineEvent.of(JuJutsuKaisenZone.MODID, "shrine_fuga_launch");
    /** 「灶」开爆炸结束（伤害与方块清除完毕） */
    public static final MimicryVoiceLineEvent SHRINE_FUGA_BOOM_END =
            MimicryVoiceLineEvent.of(JuJutsuKaisenZone.MODID, "shrine_fuga_boom_end");

    private MimicryVoiceLineEvents()
    {
    }
}
