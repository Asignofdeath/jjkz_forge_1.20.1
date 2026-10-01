package com.darkness.jujutsukaisenzone.technique.development;

import com.darkness.jujutsukaisenzone.registry.ModItems;
import com.darkness.jujutsukaisenzone.technique.development.SkillUpgradeCost.CursedObjectIngredient;
import com.darkness.jujutsukaisenzone.technique.skillset.JudgmentTechniqueSkillSet;
import com.darkness.jujutsukaisenzone.technique.skillset.LimitlessTechniqueSkillSet;
import com.darkness.jujutsukaisenzone.technique.skillset.LoveRendezvousTechniqueSkillSet;
import com.darkness.jujutsukaisenzone.technique.skillset.RatioTechniqueSkillSet;
import com.darkness.jujutsukaisenzone.technique.skillset.ShrineTechniqueSkillSet;
import com.darkness.jujutsukaisenzone.technique.skillset.TenShadowsTechniqueSkillSet;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.RegistryObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 主动技能升级消耗表：key = skillId，value = 从 1→2 … (MAX-1)→MAX 的四级消耗。
 * <p>
 * 每步为原版经验等级 + 2～5 种咒物（AND）。数值见产品配置表；本类只做注册。
 */
public final class SkillUpgradeCostRegistry
{
    private static final Map<String, List<SkillUpgradeCost>> COSTS = new HashMap<>();

    private SkillUpgradeCostRegistry()
    {
    }

    public static void bootstrap()
    {
        COSTS.clear();
        registerJudgment();
        registerLimitless();
        registerShrine();
        registerLoveRendezvous();
        registerRatio();
        registerTenShadows();
    }

    private static void registerJudgment()
    {
        register(JudgmentTechniqueSkillSet.SKILL_GAVEL,
                cost(5, o(ModItems.CURSED_OBJECT_RUSTY_POCKET_WATCH, 2), o(ModItems.CURSED_OBJECT_BLOODIED_BANDAGE, 1)),
                cost(8, o(ModItems.CURSED_OBJECT_RUSTY_POCKET_WATCH, 2), o(ModItems.CURSED_OBJECT_BROKEN_GETA, 2),
                        o(ModItems.CURSED_OBJECT_WARPED_MIRROR, 1)),
                cost(12, o(ModItems.CURSED_OBJECT_RUSTY_POCKET_WATCH, 3), o(ModItems.CURSED_OBJECT_WORN_DOLL, 1),
                        o(ModItems.CURSED_OBJECT_TORN_FAN, 2)),
                cost(16, o(ModItems.CURSED_OBJECT_RUSTY_POCKET_WATCH, 3), o(ModItems.CURSED_OBJECT_WORN_DOLL, 1),
                        o(ModItems.CURSED_OBJECT_BROKEN_DIARY, 1), o(ModItems.CURSED_OBJECT_DAMAGED_LEATHER_WATCH, 1)));

        register(JudgmentTechniqueSkillSet.SKILL_TRIAL,
                cost(6, o(ModItems.CURSED_OBJECT_BROKEN_GETA, 2), o(ModItems.CURSED_OBJECT_TORN_STUDENT_ID, 1)),
                cost(10, o(ModItems.CURSED_OBJECT_BROKEN_GETA, 2), o(ModItems.CURSED_OBJECT_TORN_FAN, 2),
                        o(ModItems.CURSED_OBJECT_DRIED_FLOWERS, 1)),
                cost(14, o(ModItems.CURSED_OBJECT_BROKEN_GETA, 3), o(ModItems.CURSED_OBJECT_CRACKED_PHONE, 1),
                        o(ModItems.CURSED_OBJECT_SINGLE_BENTO, 2)),
                cost(18, o(ModItems.CURSED_OBJECT_BROKEN_GETA, 3), o(ModItems.CURSED_OBJECT_CRACKED_PHONE, 1),
                        o(ModItems.CURSED_OBJECT_NAMELESS_PORTRAIT, 1), o(ModItems.CURSED_OBJECT_LOOSE_EARPLUGS, 1)));

        register(JudgmentTechniqueSkillSet.SKILL_ACCUSE,
                cost(6, o(ModItems.CURSED_OBJECT_TORN_FAN, 2), o(ModItems.CURSED_OBJECT_WARPED_MIRROR, 1)),
                cost(10, o(ModItems.CURSED_OBJECT_TORN_FAN, 2), o(ModItems.CURSED_OBJECT_BLOODIED_BANDAGE, 2),
                        o(ModItems.CURSED_OBJECT_TORN_STUDENT_ID, 1)),
                cost(14, o(ModItems.CURSED_OBJECT_TORN_FAN, 3), o(ModItems.CURSED_OBJECT_BROKEN_DIARY, 1),
                        o(ModItems.CURSED_OBJECT_COLD_WEDDING_RING, 1)),
                cost(18, o(ModItems.CURSED_OBJECT_TORN_FAN, 3), o(ModItems.CURSED_OBJECT_BROKEN_DIARY, 1),
                        o(ModItems.CURSED_OBJECT_CRACKED_URN, 1), o(ModItems.CURSED_OBJECT_OLD_PIPE, 1)));

        register(JudgmentTechniqueSkillSet.SKILL_VIEW_FILE,
                cost(5, o(ModItems.CURSED_OBJECT_TORN_STUDENT_ID, 2), o(ModItems.CURSED_OBJECT_SINGLE_BENTO, 1)),
                cost(8, o(ModItems.CURSED_OBJECT_TORN_STUDENT_ID, 2), o(ModItems.CURSED_OBJECT_CRACKED_PHONE, 1),
                        o(ModItems.CURSED_OBJECT_WARPED_MIRROR, 1)),
                cost(12, o(ModItems.CURSED_OBJECT_TORN_STUDENT_ID, 3), o(ModItems.CURSED_OBJECT_BROKEN_DIARY, 1),
                        o(ModItems.CURSED_OBJECT_DRIED_FLOWERS, 2)),
                cost(16, o(ModItems.CURSED_OBJECT_TORN_STUDENT_ID, 3), o(ModItems.CURSED_OBJECT_BROKEN_DIARY, 1),
                        o(ModItems.CURSED_OBJECT_NAMELESS_PORTRAIT, 1), o(ModItems.CURSED_OBJECT_CRACKED_ABACUS, 1)));
    }

    private static void registerLimitless()
    {
        register(LimitlessTechniqueSkillSet.SKILL_BLUE,
                cost(6, o(ModItems.CURSED_OBJECT_WARPED_MIRROR, 2), o(ModItems.CURSED_OBJECT_RUSTY_POCKET_WATCH, 1)),
                cost(10, o(ModItems.CURSED_OBJECT_WARPED_MIRROR, 2), o(ModItems.CURSED_OBJECT_MELTED_ICE_PACK, 1),
                        o(ModItems.CURSED_OBJECT_DRIED_FLOWERS, 2)),
                cost(14, o(ModItems.CURSED_OBJECT_WARPED_MIRROR, 3), o(ModItems.CURSED_OBJECT_WORN_DOLL, 1),
                        o(ModItems.CURSED_OBJECT_BLOODIED_BANDAGE, 2)),
                cost(20, o(ModItems.CURSED_OBJECT_WARPED_MIRROR, 3), o(ModItems.CURSED_OBJECT_WORN_DOLL, 1),
                        o(ModItems.CURSED_OBJECT_COLD_WEDDING_RING, 1),
                        o(ModItems.CURSED_OBJECT_PROOF_OF_THE_STRONGEST, 1)));

        register(LimitlessTechniqueSkillSet.SKILL_RED,
                cost(6, o(ModItems.CURSED_OBJECT_BLOODIED_BANDAGE, 2), o(ModItems.CURSED_OBJECT_BROKEN_GETA, 1)),
                cost(10, o(ModItems.CURSED_OBJECT_BLOODIED_BANDAGE, 2), o(ModItems.CURSED_OBJECT_LOOSE_EARPLUGS, 1),
                        o(ModItems.CURSED_OBJECT_TORN_FAN, 2)),
                cost(14, o(ModItems.CURSED_OBJECT_BLOODIED_BANDAGE, 3), o(ModItems.CURSED_OBJECT_CRACKED_PHONE, 1),
                        o(ModItems.CURSED_OBJECT_SINGLE_BENTO, 2)),
                cost(20, o(ModItems.CURSED_OBJECT_BLOODIED_BANDAGE, 3), o(ModItems.CURSED_OBJECT_CRACKED_PHONE, 1),
                        o(ModItems.CURSED_OBJECT_CRACKED_URN, 1),
                        o(ModItems.CURSED_OBJECT_PROOF_OF_THE_STRONGEST, 1)));

        register(LimitlessTechniqueSkillSet.SKILL_PURPLE,
                cost(8, o(ModItems.CURSED_OBJECT_RUSTY_POCKET_WATCH, 2), o(ModItems.CURSED_OBJECT_TORN_FAN, 2)),
                cost(12, o(ModItems.CURSED_OBJECT_RUSTY_POCKET_WATCH, 2), o(ModItems.CURSED_OBJECT_BROKEN_GETA, 2),
                        o(ModItems.CURSED_OBJECT_OLD_PIPE, 1)),
                cost(16, o(ModItems.CURSED_OBJECT_RUSTY_POCKET_WATCH, 3), o(ModItems.CURSED_OBJECT_BROKEN_DIARY, 1),
                        o(ModItems.CURSED_OBJECT_WARPED_MIRROR, 2)),
                cost(24, o(ModItems.CURSED_OBJECT_RUSTY_POCKET_WATCH, 3), o(ModItems.CURSED_OBJECT_BROKEN_DIARY, 1),
                        o(ModItems.CURSED_OBJECT_NAMELESS_PORTRAIT, 1),
                        o(ModItems.CURSED_OBJECT_PROOF_OF_THE_STRONGEST, 1),
                        o(ModItems.CURSED_OBJECT_DAMAGED_LEATHER_WATCH, 1)));

        register(LimitlessTechniqueSkillSet.SKILL_REMOTE_PURPLE,
                cost(10, o(ModItems.CURSED_OBJECT_WARPED_MIRROR, 2), o(ModItems.CURSED_OBJECT_CRACKED_PHONE, 1),
                        o(ModItems.CURSED_OBJECT_TORN_STUDENT_ID, 1)),
                cost(14, o(ModItems.CURSED_OBJECT_WARPED_MIRROR, 3), o(ModItems.CURSED_OBJECT_BROKEN_DIARY, 1),
                        o(ModItems.CURSED_OBJECT_DRIED_FLOWERS, 2)),
                cost(18, o(ModItems.CURSED_OBJECT_WARPED_MIRROR, 3), o(ModItems.CURSED_OBJECT_COLD_WEDDING_RING, 1),
                        o(ModItems.CURSED_OBJECT_MELTED_ICE_PACK, 1), o(ModItems.CURSED_OBJECT_SINGLE_BENTO, 2)),
                cost(28, o(ModItems.CURSED_OBJECT_WARPED_MIRROR, 4), o(ModItems.CURSED_OBJECT_COLD_WEDDING_RING, 1),
                        o(ModItems.CURSED_OBJECT_NAMELESS_PORTRAIT, 1), o(ModItems.CURSED_OBJECT_CRACKED_URN, 1),
                        o(ModItems.CURSED_OBJECT_PROOF_OF_THE_STRONGEST, 1)));
    }

    private static void registerShrine()
    {
        register(ShrineTechniqueSkillSet.SKILL_KAI,
                cost(5, o(ModItems.CURSED_OBJECT_SINGLE_BENTO, 2), o(ModItems.CURSED_OBJECT_BLOODIED_BANDAGE, 1)),
                cost(8, o(ModItems.CURSED_OBJECT_SINGLE_BENTO, 2), o(ModItems.CURSED_OBJECT_TORN_FAN, 2),
                        o(ModItems.CURSED_OBJECT_BROKEN_GETA, 1)),
                cost(12, o(ModItems.CURSED_OBJECT_SINGLE_BENTO, 3), o(ModItems.CURSED_OBJECT_WORN_DOLL, 1),
                        o(ModItems.CURSED_OBJECT_CHIPPED_CUTTING_BOARD, 1)),
                cost(16, o(ModItems.CURSED_OBJECT_SINGLE_BENTO, 3), o(ModItems.CURSED_OBJECT_WORN_DOLL, 1),
                        o(ModItems.CURSED_OBJECT_CHIPPED_CUTTING_BOARD, 1),
                        o(ModItems.CURSED_OBJECT_DAMAGED_LEATHER_WATCH, 1)));

        register(ShrineTechniqueSkillSet.SKILL_KAI_FLYOBJ,
                cost(6, o(ModItems.CURSED_OBJECT_WARPED_MIRROR, 2), o(ModItems.CURSED_OBJECT_DRIED_FLOWERS, 1)),
                cost(10, o(ModItems.CURSED_OBJECT_WARPED_MIRROR, 2), o(ModItems.CURSED_OBJECT_TORN_STUDENT_ID, 2),
                        o(ModItems.CURSED_OBJECT_RUSTY_POCKET_WATCH, 1)),
                cost(14, o(ModItems.CURSED_OBJECT_WARPED_MIRROR, 3), o(ModItems.CURSED_OBJECT_CRACKED_PHONE, 1),
                        o(ModItems.CURSED_OBJECT_CHIPPED_CUTTING_BOARD, 1)),
                cost(18, o(ModItems.CURSED_OBJECT_WARPED_MIRROR, 3), o(ModItems.CURSED_OBJECT_CRACKED_PHONE, 1),
                        o(ModItems.CURSED_OBJECT_CHIPPED_CUTTING_BOARD, 1),
                        o(ModItems.CURSED_OBJECT_LOOSE_EARPLUGS, 1)));

        register(ShrineTechniqueSkillSet.SKILL_HACHI,
                cost(8, o(ModItems.CURSED_OBJECT_BROKEN_GETA, 2), o(ModItems.CURSED_OBJECT_TORN_FAN, 2)),
                cost(12, o(ModItems.CURSED_OBJECT_BROKEN_GETA, 2), o(ModItems.CURSED_OBJECT_BROKEN_DIARY, 1),
                        o(ModItems.CURSED_OBJECT_BLOODIED_BANDAGE, 2)),
                cost(16, o(ModItems.CURSED_OBJECT_BROKEN_GETA, 3), o(ModItems.CURSED_OBJECT_COLD_WEDDING_RING, 1),
                        o(ModItems.CURSED_OBJECT_CHIPPED_CUTTING_BOARD, 1), o(ModItems.CURSED_OBJECT_CRACKED_ABACUS, 1)),
                cost(22, o(ModItems.CURSED_OBJECT_BROKEN_GETA, 3), o(ModItems.CURSED_OBJECT_COLD_WEDDING_RING, 1),
                        o(ModItems.CURSED_OBJECT_CHIPPED_CUTTING_BOARD, 2), o(ModItems.CURSED_OBJECT_CRACKED_URN, 1)));

        register(ShrineTechniqueSkillSet.SKILL_FUGA,
                cost(10, o(ModItems.CURSED_OBJECT_DRIED_FLOWERS, 2), o(ModItems.CURSED_OBJECT_SINGLE_BENTO, 2),
                        o(ModItems.CURSED_OBJECT_BLOODIED_BANDAGE, 1)),
                cost(14, o(ModItems.CURSED_OBJECT_DRIED_FLOWERS, 3), o(ModItems.CURSED_OBJECT_WORN_DOLL, 1),
                        o(ModItems.CURSED_OBJECT_WARPED_MIRROR, 2)),
                cost(18, o(ModItems.CURSED_OBJECT_DRIED_FLOWERS, 3), o(ModItems.CURSED_OBJECT_BROKEN_DIARY, 1),
                        o(ModItems.CURSED_OBJECT_CHIPPED_CUTTING_BOARD, 1), o(ModItems.CURSED_OBJECT_OLD_PIPE, 1)),
                cost(26, o(ModItems.CURSED_OBJECT_DRIED_FLOWERS, 4), o(ModItems.CURSED_OBJECT_BROKEN_DIARY, 1),
                        o(ModItems.CURSED_OBJECT_CHIPPED_CUTTING_BOARD, 2),
                        o(ModItems.CURSED_OBJECT_NAMELESS_PORTRAIT, 1),
                        o(ModItems.CURSED_OBJECT_MELTED_ICE_PACK, 1)));
    }

    private static void registerLoveRendezvous()
    {
        register(LoveRendezvousTechniqueSkillSet.SKILL_STAR_MARK,
                cost(5, o(ModItems.CURSED_OBJECT_DRIED_FLOWERS, 2), o(ModItems.CURSED_OBJECT_WARPED_MIRROR, 1)),
                cost(8, o(ModItems.CURSED_OBJECT_DRIED_FLOWERS, 2), o(ModItems.CURSED_OBJECT_COLD_WEDDING_RING, 1),
                        o(ModItems.CURSED_OBJECT_TORN_STUDENT_ID, 1)),
                cost(12, o(ModItems.CURSED_OBJECT_DRIED_FLOWERS, 3), o(ModItems.CURSED_OBJECT_STAR_EARRING, 1),
                        o(ModItems.CURSED_OBJECT_RUSTY_POCKET_WATCH, 2)),
                cost(16, o(ModItems.CURSED_OBJECT_DRIED_FLOWERS, 3), o(ModItems.CURSED_OBJECT_STAR_EARRING, 1),
                        o(ModItems.CURSED_OBJECT_WORN_DOLL, 1), o(ModItems.CURSED_OBJECT_DAMAGED_LEATHER_WATCH, 1)));

        register(LoveRendezvousTechniqueSkillSet.SKILL_STAR_PULL,
                cost(6, o(ModItems.CURSED_OBJECT_COLD_WEDDING_RING, 1), o(ModItems.CURSED_OBJECT_BLOODIED_BANDAGE, 2)),
                cost(10, o(ModItems.CURSED_OBJECT_COLD_WEDDING_RING, 1), o(ModItems.CURSED_OBJECT_BROKEN_GETA, 2),
                        o(ModItems.CURSED_OBJECT_SINGLE_BENTO, 1)),
                cost(14, o(ModItems.CURSED_OBJECT_COLD_WEDDING_RING, 1), o(ModItems.CURSED_OBJECT_STAR_EARRING, 1),
                        o(ModItems.CURSED_OBJECT_TORN_FAN, 2)),
                cost(18, o(ModItems.CURSED_OBJECT_COLD_WEDDING_RING, 2), o(ModItems.CURSED_OBJECT_STAR_EARRING, 1),
                        o(ModItems.CURSED_OBJECT_CRACKED_PHONE, 1), o(ModItems.CURSED_OBJECT_LOOSE_EARPLUGS, 1)));

        register(LoveRendezvousTechniqueSkillSet.SKILL_STAR_GATE,
                cost(6, o(ModItems.CURSED_OBJECT_TORN_STUDENT_ID, 2), o(ModItems.CURSED_OBJECT_WARPED_MIRROR, 1)),
                cost(10, o(ModItems.CURSED_OBJECT_TORN_STUDENT_ID, 2), o(ModItems.CURSED_OBJECT_BROKEN_DIARY, 1),
                        o(ModItems.CURSED_OBJECT_DRIED_FLOWERS, 2)),
                cost(14, o(ModItems.CURSED_OBJECT_TORN_STUDENT_ID, 3), o(ModItems.CURSED_OBJECT_STAR_EARRING, 1),
                        o(ModItems.CURSED_OBJECT_WORN_DOLL, 1)),
                cost(20, o(ModItems.CURSED_OBJECT_TORN_STUDENT_ID, 3), o(ModItems.CURSED_OBJECT_STAR_EARRING, 1),
                        o(ModItems.CURSED_OBJECT_NAMELESS_PORTRAIT, 1), o(ModItems.CURSED_OBJECT_OLD_PIPE, 1)));

        register(LoveRendezvousTechniqueSkillSet.SKILL_STAR_FLIGHT,
                cost(8, o(ModItems.CURSED_OBJECT_WARPED_MIRROR, 2), o(ModItems.CURSED_OBJECT_RUSTY_POCKET_WATCH, 2)),
                cost(12, o(ModItems.CURSED_OBJECT_WARPED_MIRROR, 2), o(ModItems.CURSED_OBJECT_COLD_WEDDING_RING, 1),
                        o(ModItems.CURSED_OBJECT_MELTED_ICE_PACK, 1)),
                cost(16, o(ModItems.CURSED_OBJECT_WARPED_MIRROR, 3), o(ModItems.CURSED_OBJECT_STAR_EARRING, 1),
                        o(ModItems.CURSED_OBJECT_BROKEN_DIARY, 1), o(ModItems.CURSED_OBJECT_SINGLE_BENTO, 2)),
                cost(24, o(ModItems.CURSED_OBJECT_WARPED_MIRROR, 3), o(ModItems.CURSED_OBJECT_STAR_EARRING, 2),
                        o(ModItems.CURSED_OBJECT_CRACKED_URN, 1), o(ModItems.CURSED_OBJECT_CRACKED_ABACUS, 1)));
    }

    private static void registerRatio()
    {
        register(RatioTechniqueSkillSet.SKILL_RATIO_MARK,
                cost(5, o(ModItems.CURSED_OBJECT_CRACKED_ABACUS, 1), o(ModItems.CURSED_OBJECT_TORN_STUDENT_ID, 2)),
                cost(8, o(ModItems.CURSED_OBJECT_CRACKED_ABACUS, 1), o(ModItems.CURSED_OBJECT_RUSTY_POCKET_WATCH, 2),
                        o(ModItems.CURSED_OBJECT_BROKEN_GETA, 1)),
                cost(12, o(ModItems.CURSED_OBJECT_FILLED_TIMESHEET, 1), o(ModItems.CURSED_OBJECT_CRACKED_ABACUS, 1),
                        o(ModItems.CURSED_OBJECT_WARPED_MIRROR, 2)),
                cost(16, o(ModItems.CURSED_OBJECT_FILLED_TIMESHEET, 2), o(ModItems.CURSED_OBJECT_CRACKED_PHONE, 1),
                        o(ModItems.CURSED_OBJECT_BLOODIED_BANDAGE, 2)));

        register(RatioTechniqueSkillSet.SKILL_ICHIMONJI,
                cost(6, o(ModItems.CURSED_OBJECT_TORN_FAN, 2), o(ModItems.CURSED_OBJECT_SINGLE_BENTO, 1)),
                cost(10, o(ModItems.CURSED_OBJECT_TORN_FAN, 2), o(ModItems.CURSED_OBJECT_FILLED_TIMESHEET, 1),
                        o(ModItems.CURSED_OBJECT_DRIED_FLOWERS, 1)),
                cost(14, o(ModItems.CURSED_OBJECT_TORN_FAN, 3), o(ModItems.CURSED_OBJECT_FILLED_TIMESHEET, 1),
                        o(ModItems.CURSED_OBJECT_WORN_DOLL, 1)),
                cost(18, o(ModItems.CURSED_OBJECT_TORN_FAN, 3), o(ModItems.CURSED_OBJECT_FILLED_TIMESHEET, 2),
                        o(ModItems.CURSED_OBJECT_BROKEN_DIARY, 1),
                        o(ModItems.CURSED_OBJECT_DAMAGED_LEATHER_WATCH, 1)));

        register(RatioTechniqueSkillSet.SKILL_IMPACT,
                cost(6, o(ModItems.CURSED_OBJECT_BROKEN_GETA, 2), o(ModItems.CURSED_OBJECT_BLOODIED_BANDAGE, 1)),
                cost(10, o(ModItems.CURSED_OBJECT_BROKEN_GETA, 2), o(ModItems.CURSED_OBJECT_FILLED_TIMESHEET, 1),
                        o(ModItems.CURSED_OBJECT_WARPED_MIRROR, 1)),
                cost(14, o(ModItems.CURSED_OBJECT_BROKEN_GETA, 3), o(ModItems.CURSED_OBJECT_FILLED_TIMESHEET, 1),
                        o(ModItems.CURSED_OBJECT_CRACKED_PHONE, 1)),
                cost(18, o(ModItems.CURSED_OBJECT_BROKEN_GETA, 3), o(ModItems.CURSED_OBJECT_FILLED_TIMESHEET, 2),
                        o(ModItems.CURSED_OBJECT_COLD_WEDDING_RING, 1), o(ModItems.CURSED_OBJECT_LOOSE_EARPLUGS, 1)));

        register(RatioTechniqueSkillSet.SKILL_COLLAPSE,
                cost(8, o(ModItems.CURSED_OBJECT_RUSTY_POCKET_WATCH, 2), o(ModItems.CURSED_OBJECT_BROKEN_GETA, 2)),
                cost(12, o(ModItems.CURSED_OBJECT_RUSTY_POCKET_WATCH, 2), o(ModItems.CURSED_OBJECT_FILLED_TIMESHEET, 1),
                        o(ModItems.CURSED_OBJECT_TORN_FAN, 2)),
                cost(16, o(ModItems.CURSED_OBJECT_RUSTY_POCKET_WATCH, 3), o(ModItems.CURSED_OBJECT_FILLED_TIMESHEET, 1),
                        o(ModItems.CURSED_OBJECT_BROKEN_DIARY, 1), o(ModItems.CURSED_OBJECT_SINGLE_BENTO, 2)),
                cost(22, o(ModItems.CURSED_OBJECT_RUSTY_POCKET_WATCH, 3), o(ModItems.CURSED_OBJECT_FILLED_TIMESHEET, 2),
                        o(ModItems.CURSED_OBJECT_NAMELESS_PORTRAIT, 1), o(ModItems.CURSED_OBJECT_OLD_PIPE, 1)));

        register(RatioTechniqueSkillSet.SKILL_OVERTIME,
                cost(8, o(ModItems.CURSED_OBJECT_FILLED_TIMESHEET, 1), o(ModItems.CURSED_OBJECT_TORN_STUDENT_ID, 2),
                        o(ModItems.CURSED_OBJECT_SINGLE_BENTO, 1)),
                cost(12, o(ModItems.CURSED_OBJECT_FILLED_TIMESHEET, 2), o(ModItems.CURSED_OBJECT_BLOODIED_BANDAGE, 2),
                        o(ModItems.CURSED_OBJECT_WARPED_MIRROR, 1)),
                cost(16, o(ModItems.CURSED_OBJECT_FILLED_TIMESHEET, 2), o(ModItems.CURSED_OBJECT_WORN_DOLL, 1),
                        o(ModItems.CURSED_OBJECT_DAMAGED_LEATHER_WATCH, 1), o(ModItems.CURSED_OBJECT_DRIED_FLOWERS, 2)),
                cost(24, o(ModItems.CURSED_OBJECT_FILLED_TIMESHEET, 3), o(ModItems.CURSED_OBJECT_CRACKED_PHONE, 1),
                        o(ModItems.CURSED_OBJECT_CRACKED_URN, 1), o(ModItems.CURSED_OBJECT_CRACKED_ABACUS, 1)));

        register(RatioTechniqueSkillSet.SKILL_ABSOLUTION,
                cost(10, o(ModItems.CURSED_OBJECT_BROKEN_DIARY, 1), o(ModItems.CURSED_OBJECT_TORN_FAN, 2),
                        o(ModItems.CURSED_OBJECT_TORN_STUDENT_ID, 1)),
                cost(14, o(ModItems.CURSED_OBJECT_BROKEN_DIARY, 1), o(ModItems.CURSED_OBJECT_FILLED_TIMESHEET, 2),
                        o(ModItems.CURSED_OBJECT_BROKEN_GETA, 2)),
                cost(18, o(ModItems.CURSED_OBJECT_BROKEN_DIARY, 1), o(ModItems.CURSED_OBJECT_FILLED_TIMESHEET, 2),
                        o(ModItems.CURSED_OBJECT_COLD_WEDDING_RING, 1), o(ModItems.CURSED_OBJECT_RUSTY_POCKET_WATCH, 2)),
                cost(28, o(ModItems.CURSED_OBJECT_BROKEN_DIARY, 2), o(ModItems.CURSED_OBJECT_FILLED_TIMESHEET, 3),
                        o(ModItems.CURSED_OBJECT_NAMELESS_PORTRAIT, 1), o(ModItems.CURSED_OBJECT_CRACKED_URN, 1),
                        o(ModItems.CURSED_OBJECT_DAMAGED_LEATHER_WATCH, 1)));
    }

    private static void registerTenShadows()
    {
        register(TenShadowsTechniqueSkillSet.SKILL_HAND_SHADOW,
                cost(5, o(ModItems.CURSED_OBJECT_WARPED_MIRROR, 2), o(ModItems.CURSED_OBJECT_WORN_DOLL, 1)),
                cost(8, o(ModItems.CURSED_OBJECT_WARPED_MIRROR, 2), o(ModItems.CURSED_OBJECT_DRIED_FLOWERS, 2),
                        o(ModItems.CURSED_OBJECT_BROKEN_GETA, 1)),
                cost(12, o(ModItems.CURSED_OBJECT_WARPED_MIRROR, 3), o(ModItems.CURSED_OBJECT_BROKEN_DIARY, 1),
                        o(ModItems.CURSED_OBJECT_LOOSE_EARPLUGS, 1)),
                cost(16, o(ModItems.CURSED_OBJECT_WARPED_MIRROR, 3), o(ModItems.CURSED_OBJECT_BROKEN_DIARY, 1),
                        o(ModItems.CURSED_OBJECT_NAMELESS_PORTRAIT, 1), o(ModItems.CURSED_OBJECT_MELTED_ICE_PACK, 1)));
    }

    private static void register(String skillId, SkillUpgradeCost... steps)
    {
        if (steps.length != SkillLevelRules.MAX_LEVEL - 1)
        {
            throw new IllegalArgumentException(
                    "skill " + skillId + " needs exactly " + (SkillLevelRules.MAX_LEVEL - 1) + " upgrade steps");
        }
        COSTS.put(skillId, List.of(steps));
    }

    private static SkillUpgradeCost cost(int vanillaXpLevels, CursedObjectIngredient... ingredients)
    {
        return SkillUpgradeCost.of(vanillaXpLevels, ingredients);
    }

    private static CursedObjectIngredient o(RegistryObject<? extends Item> item, int count)
    {
        return CursedObjectIngredient.of(item, count);
    }

    public static boolean isUpgradeable(String skillId)
    {
        return COSTS.containsKey(skillId);
    }

    /**
     * 当前等级升到下一级的消耗；已满级或不在表中返回 empty。
     */
    public static Optional<SkillUpgradeCost> costForNextLevel(String skillId, int currentLevel)
    {
        List<SkillUpgradeCost> steps = COSTS.get(skillId);
        if (steps == null || steps.isEmpty())
        {
            return Optional.empty();
        }
        int clamped = SkillLevelRules.clampLevel(currentLevel);
        if (clamped >= SkillLevelRules.MAX_LEVEL)
        {
            return Optional.empty();
        }
        int index = clamped - 1;
        if (index < 0 || index >= steps.size())
        {
            return Optional.empty();
        }
        return Optional.of(steps.get(index));
    }
}
