package com.darkness.jujutsukaisenzone.registry;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.effect.ConfiscationEffect;
import com.darkness.jujutsukaisenzone.effect.DeathSentenceEffect;
import com.darkness.jujutsukaisenzone.effect.ElectrifiedEffect;
import com.darkness.jujutsukaisenzone.effect.RushIntoTheZoneClimaxEffect;
import com.darkness.jujutsukaisenzone.effect.SinfulOneEffect;
import com.darkness.jujutsukaisenzone.effect.StarArmorBreakEffect;
import com.darkness.jujutsukaisenzone.effect.StarConfusionEffect;
import com.darkness.jujutsukaisenzone.effect.TechniqueMeltdownEffect;
import com.darkness.jujutsukaisenzone.effect.VulnerabilityEffect;
import com.darkness.jujutsukaisenzone.effect.ZoneEffect;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEffects
{
    public static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, JuJutsuKaisenZone.MODID);

    public static final RegistryObject<MobEffect> ZONE = EFFECTS.register("zone", ZoneEffect::new);
    public static final RegistryObject<MobEffect> TECHNIQUE_MELTDOWN =
            EFFECTS.register("technique_meltdown", TechniqueMeltdownEffect::new);
    public static final RegistryObject<MobEffect> RUSH_INTO_THE_ZONE_CLIMAX =
            EFFECTS.register("rush_into_the_zone_climax", RushIntoTheZoneClimaxEffect::new);
    public static final RegistryObject<MobEffect> DEATH_SENTENCE =
            EFFECTS.register("death_sentence", DeathSentenceEffect::new);
    public static final RegistryObject<MobEffect> CONFISCATION =
            EFFECTS.register("confiscation", ConfiscationEffect::new);
    public static final RegistryObject<MobEffect> SINFUL_ONE =
            EFFECTS.register("sinful_one", SinfulOneEffect::new);
    /** 星间飞行 · Mimosa 引爆：降护甲（另附原版发光）。 */
    public static final RegistryObject<MobEffect> STAR_ARMOR_BREAK =
            EFFECTS.register("star_armor_break", StarArmorBreakEffect::new);
    /** 星间飞行 · Ginan 引爆：短时混乱，非玩家实体随机转向。 */
    public static final RegistryObject<MobEffect> STAR_CONFUSION =
            EFFECTS.register("star_confusion", StarConfusionEffect::new);
    /** 鵺等雷系：感电占位（轻度减速）。 */
    public static final RegistryObject<MobEffect> ELECTRIFIED =
            EFFECTS.register("electrified", ElectrifiedEffect::new);
    /** 鵺等：易伤占位（受伤加深）。 */
    public static final RegistryObject<MobEffect> VULNERABILITY =
            EFFECTS.register("vulnerability", VulnerabilityEffect::new);

    private ModEffects()
    {
    }

    public static void register(IEventBus bus)
    {
        EFFECTS.register(bus);
    }
}
