package com.darkness.jujutsukaisenzone.registry;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.block.JJKZSkyblock;
import com.darkness.jujutsukaisenzone.item.BarrierStudybookItem;
import com.darkness.jujutsukaisenzone.item.BrainRecoveryItem;
import com.darkness.jujutsukaisenzone.item.LimitlessTechniqueBinderItem;
import com.darkness.jujutsukaisenzone.Config;
import com.darkness.jujutsukaisenzone.item.LoveRendezvousTechniqueBinderItem;
import com.darkness.jujutsukaisenzone.item.RatioTechniqueBinderItem;
import com.darkness.jujutsukaisenzone.item.TenShadowsTechniqueBinderItem;
import com.darkness.jujutsukaisenzone.item.DomainStudybookItem;
import com.darkness.jujutsukaisenzone.item.ReverseStudybookItem;
import com.darkness.jujutsukaisenzone.item.SlashTechniqueSukunaBinderItem;
import com.darkness.jujutsukaisenzone.item.SmGavelWeaponItem;
import com.darkness.jujutsukaisenzone.item.SoulStudybookItem;
import com.darkness.jujutsukaisenzone.item.CursedObjectGrade;
import com.darkness.jujutsukaisenzone.item.CursedObjectItem;
import com.darkness.jujutsukaisenzone.item.TechniqueIntelligenceItem;
import com.darkness.jujutsukaisenzone.item.TrialTechniqueBinderItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems
{
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, JuJutsuKaisenZone.MODID);

    public static final RegistryObject<Item> INNATE_TECHNIQUE_LIMITLESS = ITEMS.register(
            "innate_technique_limitless",
            () -> new LimitlessTechniqueBinderItem(new Item.Properties().stacksTo(1))
    );

    /* 方块物品 */
    public static final RegistryObject<Item> JJKZ_SKYBLOCK = ITEMS.register(
            "jjkz_skyblock",
            () -> new BlockItem(ModBlocks.JJKZ_SKYBLOCK.get(), new Item.Properties())
    );

    /** 御厨子 */
    public static final RegistryObject<Item> SLASH_TECHNIQUE_SUKUNA = ITEMS.register(
            "slash_technique_sukuna",
            () -> new SlashTechniqueSukunaBinderItem(new Item.Properties().stacksTo(1))
    );

    /** 审判 */
    public static final RegistryObject<Item> TRIAL_TECHNIQUE = ITEMS.register(
            "trial_technique",
            () -> new TrialTechniqueBinderItem(new Item.Properties().stacksTo(1))
    );

    /** 星间飞行 */
    public static final RegistryObject<Item> LOVE_RENDEZVOUS = ITEMS.register(
            "love_rendezvous",
            () -> new LoveRendezvousTechniqueBinderItem(new Item.Properties().stacksTo(1))
    );

    /** 十划咒法 */
    public static final RegistryObject<Item> RATIO_TECHNIQUE = ITEMS.register(
            "ratio_technique",
            () -> new RatioTechniqueBinderItem(new Item.Properties().stacksTo(1))
    );

    /** 十种影法术 */
    public static final RegistryObject<Item> TEN_SHADOWS_TECHNIQUE = ITEMS.register(
            "ten_shadows_technique",
            () -> new TenShadowsTechniqueBinderItem(new Item.Properties().stacksTo(1))
    );

    /** 法槌（Epic Fight 武器） */
    public static final RegistryObject<Item> SM_GAVEL_WEAPON = ITEMS.register(
            "sm_gavel_weapon",
            () -> new SmGavelWeaponItem(new Item.Properties().stacksTo(1))
    );

    /** 处刑人之剑（Epic Fight 武器） */
    public static final RegistryObject<Item> EXECUTIONERS_SWORD = ITEMS.register(
            "executioners_sword",
            () -> new Item(new Item.Properties().stacksTo(1))
    );

    /** 咒力流动反转理论 */
    public static final RegistryObject<Item> REVERSE_STUDYBOOK = ITEMS.register(
            "reverse_studybook",
            () -> new ReverseStudybookItem(new Item.Properties())
    );

    /** 结界的构建与要术 */
    public static final RegistryObject<Item> BARRIER_STUDYBOOK = ITEMS.register(
            "barrier_studybook",
            () -> new BarrierStudybookItem(new Item.Properties())
    );

    /** 虚构空间的具象化 */
    public static final RegistryObject<Item> DOMAIN_STUDYBOOK = ITEMS.register(
            "domain_studybook",
            () -> new DomainStudybookItem(new Item.Properties())
    );

    /** 灵魂研究手册 */
    public static final RegistryObject<Item> SOUL_STUDYBOOK = ITEMS.register(
            "soul_studybook",
            () -> new SoulStudybookItem(new Item.Properties())
    );

    /** 术式情报 */
    public static final RegistryObject<Item> TECHNIQUE_INTELLIGENCE = ITEMS.register(
            "technique_intelligence",
            () -> new TechniqueIntelligenceItem(new Item.Properties())
    );

    /** 咒物：錆びた懐中時計 */
    public static final RegistryObject<Item> CURSED_OBJECT_RUSTY_POCKET_WATCH = ITEMS.register(
            "cursed_object_rusty_pocket_watch",
            () -> new CursedObjectItem(new Item.Properties().stacksTo(64), "生锈怀表", CursedObjectGrade.COMMON)
    );

    /** 咒物：折れた下駄 */
    public static final RegistryObject<Item> CURSED_OBJECT_BROKEN_GETA = ITEMS.register(
            "cursed_object_broken_geta",
            () -> new CursedObjectItem(new Item.Properties().stacksTo(64), "断掉的木屐", CursedObjectGrade.COMMON)
    );

    /** 咒物：破れた扇 */
    public static final RegistryObject<Item> CURSED_OBJECT_TORN_FAN = ITEMS.register(
            "cursed_object_torn_fan",
            () -> new CursedObjectItem(new Item.Properties().stacksTo(64), "破扇", CursedObjectGrade.COMMON)
    );

    /** 咒物：古ぼけた煙管 */
    public static final RegistryObject<Item> CURSED_OBJECT_OLD_PIPE = ITEMS.register(
            "cursed_object_old_pipe",
            () -> new CursedObjectItem(new Item.Properties().stacksTo(64), "旧烟管", CursedObjectGrade.COMMON)
    );

    /** 咒物：ひびのそろばん */
    public static final RegistryObject<Item> CURSED_OBJECT_CRACKED_ABACUS = ITEMS.register(
            "cursed_object_cracked_abacus",
            () -> new CursedObjectItem(new Item.Properties().stacksTo(64), "裂纹算盘", CursedObjectGrade.COMMON)
    );

    /** 咒物：歪んだ手鏡 */
    public static final RegistryObject<Item> CURSED_OBJECT_WARPED_MIRROR = ITEMS.register(
            "cursed_object_warped_mirror",
            () -> new CursedObjectItem(new Item.Properties().stacksTo(64), "歪手镜", CursedObjectGrade.COMMON)
    );

    /** 咒物：誰かの古びた人形 */
    public static final RegistryObject<Item> CURSED_OBJECT_WORN_DOLL = ITEMS.register(
            "cursed_object_worn_doll",
            () -> new CursedObjectItem(new Item.Properties().stacksTo(64), "某人的破旧玩偶", CursedObjectGrade.RARE)
    );

    /** 咒物：画面の割れた携帯 */
    public static final RegistryObject<Item> CURSED_OBJECT_CRACKED_PHONE = ITEMS.register(
            "cursed_object_cracked_phone",
            () -> new CursedObjectItem(new Item.Properties().stacksTo(64), "碎屏的手机", CursedObjectGrade.RARE)
    );

    /** 咒物：名前のない遺影 */
    public static final RegistryObject<Item> CURSED_OBJECT_NAMELESS_PORTRAIT = ITEMS.register(
            "cursed_object_nameless_portrait",
            () -> new CursedObjectItem(new Item.Properties().stacksTo(64), "无名遗像", CursedObjectGrade.SPECIAL)
    );

    /** 咒物：干からびた献花 */
    public static final RegistryObject<Item> CURSED_OBJECT_DRIED_FLOWERS = ITEMS.register(
            "cursed_object_dried_flowers",
            () -> new CursedObjectItem(new Item.Properties().stacksTo(64), "干枯的献花", CursedObjectGrade.COMMON)
    );

    /** 咒物：鍵の折れた日記 */
    public static final RegistryObject<Item> CURSED_OBJECT_BROKEN_DIARY = ITEMS.register(
            "cursed_object_broken_diary",
            () -> new CursedObjectItem(new Item.Properties().stacksTo(64), "钥匙断了的日记本", CursedObjectGrade.RARE)
    );

    /** 咒物：血のにじんだ絆創膏 */
    public static final RegistryObject<Item> CURSED_OBJECT_BLOODIED_BANDAGE = ITEMS.register(
            "cursed_object_bloodied_bandage",
            () -> new CursedObjectItem(new Item.Properties().stacksTo(64), "渗血的创可贴", CursedObjectGrade.COMMON)
    );

    /** 咒物：一人分の弁当箱 */
    public static final RegistryObject<Item> CURSED_OBJECT_SINGLE_BENTO = ITEMS.register(
            "cursed_object_single_bento",
            () -> new CursedObjectItem(new Item.Properties().stacksTo(64), "一人份便当盒", CursedObjectGrade.COMMON)
    );

    /** 咒物：冷めた結婚指輪 */
    public static final RegistryObject<Item> CURSED_OBJECT_COLD_WEDDING_RING = ITEMS.register(
            "cursed_object_cold_wedding_ring",
            () -> new CursedObjectItem(new Item.Properties().stacksTo(64), "冰凉的结婚戒指", CursedObjectGrade.RARE)
    );

    /** 咒物：破れた学生証 */
    public static final RegistryObject<Item> CURSED_OBJECT_TORN_STUDENT_ID = ITEMS.register(
            "cursed_object_torn_student_id",
            () -> new CursedObjectItem(new Item.Properties().stacksTo(64), "撕破的学生证", CursedObjectGrade.COMMON)
    );

    /** 咒物：割れそうな骨壺 */
    public static final RegistryObject<Item> CURSED_OBJECT_CRACKED_URN = ITEMS.register(
            "cursed_object_cracked_urn",
            () -> new CursedObjectItem(new Item.Properties().stacksTo(64), "裂痕累累的骨灰盒", CursedObjectGrade.SPECIAL)
    );

    /** 咒物：溶けかけの氷嚢（通用 SE「温和复位」载体） */
    public static final RegistryObject<Item> CURSED_OBJECT_MELTED_ICE_PACK = ITEMS.register(
            "cursed_object_melted_ice_pack",
            () -> new CursedObjectItem(new Item.Properties().stacksTo(64), "半融的冰袋", CursedObjectGrade.COMMON)
    );

    /** 咒物：詰め物の抜けた耳栓（通用 SE「钝感皮层」载体） */
    public static final RegistryObject<Item> CURSED_OBJECT_LOOSE_EARPLUGS = ITEMS.register(
            "cursed_object_loose_earplugs",
            () -> new CursedObjectItem(new Item.Properties().stacksTo(64), "填料脱落的耳塞", CursedObjectGrade.COMMON)
    );

    /** 咒物：欠けた俎板（御厨子专用 SE「收刀」载体） */
    public static final RegistryObject<Item> CURSED_OBJECT_CHIPPED_CUTTING_BOARD = ITEMS.register(
            "cursed_object_chipped_cutting_board",
            () -> new CursedObjectItem(new Item.Properties().stacksTo(64), "缺口的俎板", CursedObjectGrade.RARE)
    );

    /** 咒物：石の落ちた星形ピアス（爱与幽会专用 SE「推着走」载体） */
    public static final RegistryObject<Item> CURSED_OBJECT_STAR_EARRING = ITEMS.register(
            "cursed_object_star_earring",
            () -> new CursedObjectItem(new Item.Properties().stacksTo(64), "掉钻的星形耳环", CursedObjectGrade.RARE)
    );

    /** 咒物：最強の証明（无下限专用 SE「移除身旁豁免」载体） */
    public static final RegistryObject<Item> CURSED_OBJECT_PROOF_OF_THE_STRONGEST = ITEMS.register(
            "cursed_object_proof_of_the_strongest",
            () -> new CursedObjectItem(new Item.Properties().stacksTo(64), "最强的证明", CursedObjectGrade.SPECIAL)
    );

    /** 咒物：埋まった出勤簿（通用 SE「工作效率」载体） */
    public static final RegistryObject<Item> CURSED_OBJECT_FILLED_TIMESHEET = ITEMS.register(
            "cursed_object_filled_timesheet",
            () -> new CursedObjectItem(new Item.Properties().stacksTo(64), "填满的工时单", CursedObjectGrade.COMMON)
    );

    /** 咒物：壊れた革の腕時計（通用 SE「暴击叠层」载体） */
    public static final RegistryObject<Item> CURSED_OBJECT_DAMAGED_LEATHER_WATCH = ITEMS.register(
            "cursed_object_damaged_leather_watch",
            () -> new CursedObjectItem(new Item.Properties().stacksTo(64), "损坏的皮质手表", CursedObjectGrade.COMMON)
    );

    /** 咒物：両面宿儺の指 */
    public static final RegistryObject<Item> CURSED_OBJECT_SUKUNA_FINGER = ITEMS.register(
            "cursed_object_sukuna_finger",
            () -> new CursedObjectItem(new Item.Properties().stacksTo(20), "两面宿傩的手指", CursedObjectGrade.SPECIAL)
    );

    /** 清神糖：小幅回复脑损 */
    public static final RegistryObject<Item> BRAIN_SOOTHING_CANDY = ITEMS.register(
            "brain_soothing_candy",
            () -> new BrainRecoveryItem(
                    new Item.Properties().stacksTo(16),
                    () -> Config.domainBrainFoodHealMinor,
                    "item.jujutsukaisenzone.brain_soothing_candy.desc")
    );

    /** 安神丸：大幅回复脑损 */
    public static final RegistryObject<Item> BRAIN_CALMING_PILL = ITEMS.register(
            "brain_calming_pill",
            () -> new BrainRecoveryItem(
                    new Item.Properties().stacksTo(16),
                    () -> Config.domainBrainFoodHealMajor,
                    "item.jujutsukaisenzone.brain_calming_pill.desc")
    );

    private ModItems()
    {
    }

    public static void register(IEventBus eventBus)
    {
        ITEMS.register(eventBus);
    }
}
