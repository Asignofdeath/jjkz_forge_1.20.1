package com.darkness.jujutsukaisenzone.technique;

import com.darkness.jujutsukaisenzone.technique.skillset.JudgmentTechniqueSkillSet;
import com.darkness.jujutsukaisenzone.technique.skillset.LimitlessTechniqueSkillSet;
import com.darkness.jujutsukaisenzone.technique.skillset.LoveRendezvousTechniqueSkillSet;
import com.darkness.jujutsukaisenzone.technique.skillset.RatioTechniqueSkillSet;
import com.darkness.jujutsukaisenzone.technique.skillset.ShrineTechniqueSkillSet;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 叙事技能族：同族成员共用适应键；每族带<strong>叙事强度</strong>（魔虚罗满层数）。
 * <p>
 * 未登记的 memberId 在 {@link #resolveFamilyId} 中视为单员族（familyId = 自身），
 * 叙事强度用 {@link #DEFAULT_NARRATIVE_POWER}。
 */
public final class NarrativeSkillFamilies
{
    /** 未在表中登记的族 / 粗环境键的默认叙事强度（亦即默认满层数）。 */
    public static final int DEFAULT_NARRATIVE_POWER = 5;

    private static final Map<String, String> MEMBER_TO_FAMILY;
    private static final Map<String, Integer> FAMILY_POWER;

    static
    {
        Map<String, String> members = new HashMap<>();
        Map<String, Integer> power = new HashMap<>();

        // —— 无下限 ——
        family(members, power, LimitlessTechniqueSkillSet.SKILL_BLUE, 5,
                LimitlessTechniqueSkillSet.SKILL_BLUE, "limitless_blue_shift");
        family(members, power, LimitlessTechniqueSkillSet.SKILL_RED, 6,
                LimitlessTechniqueSkillSet.SKILL_RED, "limitless_red_shift");
        family(members, power, LimitlessTechniqueSkillSet.SKILL_PURPLE, 10,
                LimitlessTechniqueSkillSet.SKILL_PURPLE,
                LimitlessTechniqueSkillSet.SKILL_REMOTE_PURPLE,
                "limitless_purple_shift");

        // —— 御厨子 ——
        family(members, power, ShrineTechniqueSkillSet.SKILL_KAI, 5,
                ShrineTechniqueSkillSet.SKILL_KAI);
        family(members, power, ShrineTechniqueSkillSet.SKILL_KAI_FLYOBJ, 6,
                ShrineTechniqueSkillSet.SKILL_KAI_FLYOBJ);
        family(members, power, ShrineTechniqueSkillSet.SKILL_HACHI, 7,
                ShrineTechniqueSkillSet.SKILL_HACHI);
        family(members, power, ShrineTechniqueSkillSet.SKILL_FUGA, 9,
                ShrineTechniqueSkillSet.SKILL_FUGA);
        family(members, power, "shrine_domain_slash", 10,
                "shrine_domain_slash", "shrine_domain_power_slash");

        // —— 十划 ——
        family(members, power, RatioTechniqueSkillSet.SKILL_RATIO_MARK, 5,
                RatioTechniqueSkillSet.SKILL_RATIO_MARK);
        family(members, power, RatioTechniqueSkillSet.SKILL_ICHIMONJI, 6,
                RatioTechniqueSkillSet.SKILL_ICHIMONJI);
        family(members, power, RatioTechniqueSkillSet.SKILL_IMPACT, 6,
                RatioTechniqueSkillSet.SKILL_IMPACT);
        family(members, power, RatioTechniqueSkillSet.SKILL_COLLAPSE, 8,
                RatioTechniqueSkillSet.SKILL_COLLAPSE);
        family(members, power, RatioTechniqueSkillSet.SKILL_OVERTIME_VOW, 4,
                RatioTechniqueSkillSet.SKILL_OVERTIME_VOW);
        family(members, power, RatioTechniqueSkillSet.SKILL_OVERTIME, 5,
                RatioTechniqueSkillSet.SKILL_OVERTIME);
        family(members, power, RatioTechniqueSkillSet.SKILL_ABSOLUTION, 8,
                RatioTechniqueSkillSet.SKILL_ABSOLUTION);

        // —— 恋之邂逅 ——
        family(members, power, LoveRendezvousTechniqueSkillSet.SKILL_STAR_MARK, 7,
                LoveRendezvousTechniqueSkillSet.SKILL_STAR_MARK);
        family(members, power, LoveRendezvousTechniqueSkillSet.SKILL_STAR_PULL, 6,
                LoveRendezvousTechniqueSkillSet.SKILL_STAR_PULL);
        family(members, power, LoveRendezvousTechniqueSkillSet.SKILL_STAR_GATE, 7,
                LoveRendezvousTechniqueSkillSet.SKILL_STAR_GATE,
                "love_rendezvous_star_gate_shift");
        family(members, power, LoveRendezvousTechniqueSkillSet.SKILL_STAR_FLIGHT, 8,
                LoveRendezvousTechniqueSkillSet.SKILL_STAR_FLIGHT);
        family(members, power, LoveRendezvousTechniqueSkillSet.SKILL_STAR_SLING, 6,
                LoveRendezvousTechniqueSkillSet.SKILL_STAR_SLING);
        family(members, power, LoveRendezvousTechniqueSkillSet.SKILL_STAR_CHAIN, 5,
                LoveRendezvousTechniqueSkillSet.SKILL_STAR_CHAIN);

        // —— 判决（伤害 + 机制分族）——
        family(members, power, JudgmentTechniqueSkillSet.SKILL_GAVEL, 6,
                JudgmentTechniqueSkillSet.SKILL_GAVEL,
                JudgmentTechniqueSkillSet.SKILL_GAVEL_FORMS);
        family(members, power, JudgmentTechniqueSkillSet.SKILL_TRIAL, 7,
                JudgmentTechniqueSkillSet.SKILL_TRIAL);
        family(members, power, JudgmentTechniqueSkillSet.SKILL_ACCUSE, 8,
                JudgmentTechniqueSkillSet.SKILL_ACCUSE);
        family(members, power, JudgmentTechniqueSkillSet.SKILL_VIEW_FILE, 3,
                JudgmentTechniqueSkillSet.SKILL_VIEW_FILE);
        family(members, power, JudgmentTechniqueSkillSet.SKILL_SENTENCING, 5,
                JudgmentTechniqueSkillSet.SKILL_SENTENCING);
        family(members, power, JudgmentTechniqueSkillSet.SKILL_AGGRAVATION, 5,
                JudgmentTechniqueSkillSet.SKILL_AGGRAVATION);
        family(members, power, "judgment_death_sentence", 9,
                "judgment_death_sentence");
        family(members, power, "judgment_confiscation", 8,
                "judgment_confiscation");
        family(members, power, "judgment_sinful_one", 7,
                "judgment_sinful_one");

        // —— 其它 ——
        family(members, power, "black_flash", 8, "black_flash");

        // 粗环境键（无 skillId 时）
        family(members, power, "melee", 4, "melee");
        family(members, power, "fire", 5, "fire");
        family(members, power, "explosion", 6, "explosion");
        family(members, power, "projectile", 5, "projectile");
        family(members, power, "fall", 4, "fall");
        family(members, power, "magic", 5, "magic");
        family(members, power, "generic", 4, "generic");

        MEMBER_TO_FAMILY = Collections.unmodifiableMap(members);
        FAMILY_POWER = Collections.unmodifiableMap(power);
    }

    private NarrativeSkillFamilies()
    {
    }

    /**
     * 将成员 id 解析为叙事族 id。
     * 未登记则返回自身（单员族）；空白返回 null。
     */
    @Nullable
    public static String resolveFamilyId(@Nullable String memberId)
    {
        if (memberId == null || memberId.isBlank())
        {
            return null;
        }
        String family = MEMBER_TO_FAMILY.get(memberId);
        return family != null ? family : memberId;
    }

    /**
     * 叙事强度（魔虚罗对该族满适应所需层数）。
     * 接受 memberId 或 familyId；未登记用 {@link #DEFAULT_NARRATIVE_POWER}。
     */
    public static int narrativePower(@Nullable String memberOrFamilyId)
    {
        String family = resolveFamilyId(memberOrFamilyId);
        if (family == null)
        {
            return DEFAULT_NARRATIVE_POWER;
        }
        return FAMILY_POWER.getOrDefault(family, DEFAULT_NARRATIVE_POWER);
    }

    public static Map<String, String> memberToFamilyView()
    {
        return MEMBER_TO_FAMILY;
    }

    public static Map<String, Integer> familyPowerView()
    {
        return FAMILY_POWER;
    }

    private static void family(
            Map<String, String> members,
            Map<String, Integer> power,
            String familyId,
            int narrativePower,
            String... memberIds)
    {
        if (familyId == null || familyId.isBlank())
        {
            throw new IllegalArgumentException("familyId blank");
        }
        if (narrativePower < 1)
        {
            throw new IllegalArgumentException("narrativePower < 1 for " + familyId);
        }
        members.put(familyId, familyId);
        Integer prevPower = power.put(familyId, narrativePower);
        if (prevPower != null && prevPower != narrativePower)
        {
            throw new IllegalStateException("family power redefined: " + familyId);
        }
        if (memberIds == null)
        {
            return;
        }
        for (String member : memberIds)
        {
            if (member == null || member.isBlank())
            {
                continue;
            }
            String previous = members.put(member, familyId);
            if (previous != null && !previous.equals(familyId))
            {
                throw new IllegalStateException(
                        "narrative member '" + member + "' mapped to both " + previous + " and " + familyId);
            }
        }
    }
}
