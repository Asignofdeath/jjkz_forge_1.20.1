package com.darkness.jujutsukaisenzone.registry;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModSounds
{
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, JuJutsuKaisenZone.MODID);

    public static final RegistryObject<SoundEvent> LIMITLESS_BLUE_INHALE = register("jutsushiki.limitless.blue_inhale");
    /** 赫前摇蓄力音效（{@code assets/.../sounds/jutsushiki/limitless/aka_short_charge}）。 */
    public static final RegistryObject<SoundEvent> LIMITLESS_AKA_SHORT_CHARGE = register("jutsushiki.limitless.aka_short_charge");
    /** 茈（迷你）箭发射时在施法者处播放。 */
    public static final RegistryObject<SoundEvent> LIMITLESS_MURASAKI_MINI = register("jutsushiki.limitless.murasaki_mini");
    /** 超远程虚式茈起手时在施法者处播放（{@code assets/.../sounds/jutsushiki/limitless/remote_murasaki}）。 */
    public static final RegistryObject<SoundEvent> LIMITLESS_REMOTE_MURASAKI = register("jutsushiki.limitless.remote_murasaki");
    /** 无下限术式领域展开「无量空处」蓄力时在施法者处播放。 */
    public static final RegistryObject<SoundEvent> LIMITLESS_DOMAIN_EXPANSION = register("domain.limitless_domain_expantion");
    /** 无量空处结界完成 280 刻后于球心循环播放直至领域关闭（客户端环境音效通道，非音乐）。 */
    public static final RegistryObject<SoundEvent> LIMITLESS_DOMAIN_CENTER_AMBIENT = register("domain.limitless_domain_sound");
    /** 无量空处「完美控制」反馈（{@code assets/.../sounds/sound_effects/iron_sound}）。 */
    public static final RegistryObject<SoundEvent> SOUND_EFFECTS_IRON_SOUND = register("sound_effects.iron_sound");
    /** 「完美控制」时在领域球心播放的一次性环境音效（{@code assets/.../sounds/domain/limitless_domain_perfect}）。 */
    public static final RegistryObject<SoundEvent> LIMITLESS_DOMAIN_PERFECT_AMBIENT = register("domain.limitless_domain_perfect");
    /** 伏魔神龛领域展开演出时在施法者处播放。 */
    public static final RegistryObject<SoundEvent> SHRINE_DOMAIN_EXPANSION = register("domain.shrine_domain_expension");
    /** 伏魔神龛领域内普通斩击伤害时播放。 */
    public static final RegistryObject<SoundEvent> SHRINE_SLASH = register("domain.shrine_slash");
    /** 伏魔神龛领域释放强力斩击时播放。 */
    public static final RegistryObject<SoundEvent> SHRINE_SLASH_POWER = register("domain.shrine_slash_power");
    /** 伏魔神龛领域内击杀生物时播放。 */
    public static final RegistryObject<SoundEvent> SLASH_KILL = register("domain.slash_kill");
    /** 伏魔神龛「完美压制」反馈（{@code assets/.../sounds/domain/shrine_perfect}）。 */
    public static final RegistryObject<SoundEvent> SHRINE_DOMAIN_PERFECT = register("domain.shrine_perfect");
    /** 解箭发射音效（{@code assets/.../sounds/jutsushiki/shrine/slash_kai_1_fire}）。 */
    public static final RegistryObject<SoundEvent> SHRINE_SLASH_KAI_1_FIRE = register("jutsushiki.shrine.slash_kai_1_fire");
    /** 解箭命中生物音效（{@code assets/.../sounds/jutsushiki/shrine/slash_kai_1_hit}）。 */
    public static final RegistryObject<SoundEvent> SHRINE_SLASH_KAI_1_HIT = register("jutsushiki.shrine.slash_kai_1_hit");
    /** 解箭横斩发射音效（{@code assets/.../sounds/jutsushiki/shrine/slash_kai_2_fire}）。 */
    public static final RegistryObject<SoundEvent> SHRINE_SLASH_KAI_2_FIRE = register("jutsushiki.shrine.slash_kai_2_fire");
    /** 解箭横斩命中生物音效（{@code assets/.../sounds/jutsushiki/shrine/slash_kai_2_hit}）。 */
    public static final RegistryObject<SoundEvent> SHRINE_SLASH_KAI_2_HIT = register("jutsushiki.shrine.slash_kai_2_hit");
    /** 「解」切割发射音效（{@code assets/.../sounds/jutsushiki/shrine/slash_kai_flyobj}）。 */
    public static final RegistryObject<SoundEvent> SHRINE_SLASH_KAI_FLYOBJ = register("jutsushiki.shrine.slash_kai_flyobj");
    /** 捌斩击音效（{@code assets/.../sounds/jutsushiki/shrine/slash_hachi}）。 */
    public static final RegistryObject<SoundEvent> SHRINE_SLASH_HACHI = register("jutsushiki.shrine.slash_hachi");
    /** 「灶」开蓄力音效（{@code assets/.../sounds/jutsushiki/shrine/fuga_start}）。 */
    public static final RegistryObject<SoundEvent> SHRINE_FUGA_START = register("jutsushiki.shrine.fuga_start");
    /** 「灶」开发射音效（{@code assets/.../sounds/jutsushiki/shrine/fuga_fire}）。 */
    public static final RegistryObject<SoundEvent> SHRINE_FUGA_FIRE = register("jutsushiki.shrine.fuga_fire");
    /** 「灶」开爆炸音效（{@code assets/.../sounds/jutsushiki/shrine/fuga_boom}）。
     * 须用固定听距注册：{@link SoundEvent#createVariableRangeEvent} 服务端仅向 16 格内玩家发包，
     * {@code sounds.json} 的 {@code attenuation_distance} 无法改变广播半径。 */
    public static final RegistryObject<SoundEvent> SHRINE_FUGA_BOOM =
            SOUND_EVENTS.register("jutsushiki.shrine.fuga_boom", () -> SoundEvent.createFixedRangeEvent(
                    ResourceLocation.fromNamespaceAndPath(JuJutsuKaisenZone.MODID, "jutsushiki.shrine.fuga_boom"),
                    55.0F));
    /** 黑闪触发音效（{@code assets/.../sounds/sound_effects/black_flash}）。 */
    public static final RegistryObject<SoundEvent> BLACK_FLASH = register("sound_effects.black_flash");
    /** 宿傩拟态语音「hmph」（{@code assets/.../sounds/character/sukuna/hmph}）。 */
    public static final RegistryObject<SoundEvent> SUKUNA_HMPH = register("character.sukuna.hmph");
    /** 宿傩拟态语音「kai」（{@code assets/.../sounds/character/sukuna/kai}）。 */
    public static final RegistryObject<SoundEvent> SUKUNA_KAI = register("character.sukuna.kai");
    /** 宿傩拟态语音「灶」（{@code assets/.../sounds/character/sukuna/fuga}）。 */
    public static final RegistryObject<SoundEvent> SUKUNA_FUGA = register("character.sukuna.fuga");
    /** 宿傩拟态语音「ぞんがい楽しめたぞ」（{@code assets/.../sounds/character/sukuna/zongai_tanoshimeta}）。 */
    public static final RegistryObject<SoundEvent> SUKUNA_ZONGAI_TANOSHIMETA =
            register("character.sukuna.zongai_tanoshimeta");

    private ModSounds()
    {
    }

    public static void register(IEventBus bus)
    {
        SOUND_EVENTS.register(bus);
    }

    private static RegistryObject<SoundEvent> register(String id)
    {
        return SOUND_EVENTS.register(id, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(JuJutsuKaisenZone.MODID, id)));
    }
}
