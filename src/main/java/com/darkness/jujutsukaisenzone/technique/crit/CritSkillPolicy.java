package com.darkness.jujutsukaisenzone.technique.crit;

import com.darkness.jujutsukaisenzone.technique.commonskill.CommonTechniqueSkillRegistry;
import com.darkness.jujutsukaisenzone.technique.development.SkillTreeNodeDef;
import com.darkness.jujutsukaisenzone.technique.development.TechniqueSkillTree;
import com.darkness.jujutsukaisenzone.technique.development.TechniqueSkillTreeRegistry;
import com.darkness.jujutsukaisenzone.technique.skillset.LimitlessTechniqueSkillSet;
import com.darkness.jujutsukaisenzone.technique.skillset.RatioTechniqueSkillSet;
import com.darkness.jujutsukaisenzone.technique.skillset.ShrineTechniqueSkillSet;
import com.darkness.jujutsukaisenzone.technique.skillset.TechniqueSkill;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 「不参与暴击」的技能声明——<b>展示层</b>，供技能悬停框标注用。
 * <p>
 * 与 {@code TechniqueDamageSources#critAllowed} 的分工：那一边管<b>运行时</b>放不放行
 * （默认口径是「体术可暴、术式不可暴」，单发术式招式由出伤点显式走 {@code dealCrit}），
 * 这里管<b>告诉玩家</b>。两边必须对齐，所以这份名单只收「按设计就不该暴击」的招式：
 * <ul>
 *   <li>高频 / 周期性出伤 —— 苍（每 4 tick）、捌（每 5 tick）、解（每 tick 扫柱高）、
 *       茈与超远程茈（每 tick 球内）、灶开爆炸（每 tick）</li>
 *   <li>坠落碎块 —— 瓦落瓦落（复合分型，本身不在 {@link CritRules#canCrit} 资格内）</li>
 * </ul>
 * ⚠️ 新增高频招式时把技能 id 加进来，并在出伤点确认它确实没有走 {@code dealCrit}。
 * 反过来，某个招式以后改成单次结算并放行暴击了，就要把它从这份名单里删掉。
 * <p>
 * 未列入的技能（十划、星弹、星间飞行、法槌、赫、星痕……）都不打这行标注。
 * <p>
 * <b>领域说明节点</b>另算（见 {@link #isNoCritInfoNode}）：领域不是可释放技能、没有 skillId，
 * 它是对应术式的说明节点，所以只能按 nodeId 定位。
 * <p>
 * <b>运行时放行出伤点清单</b>（本表必须与它们一致；新增技能时逐条对照，改一处就得看这里）：
 * <ul>
 *   <li>赫 爆炸 —— {@code LimitlessRedArrowBehavior#onHit}（单次爆炸，{@code runWithTypeCrit}）</li>
 *   <li>法槌 —— {@code GavelProjectileEntity#onHitEntity} / {@code onHitBlock}（撞击与爆炸各一次）</li>
 *   <li>星痕撞击 —— {@code StarMarkRules} 的球撞击结算（有 {@code IMPACT_COOLDOWN_TICKS} 去重）</li>
 *   <li>星痕爆裂 —— {@code StarMarkDetonation#detonate}</li>
 *   <li>黑闪冲击波 —— {@code BlackFlashShockwaveBehavior}（一次性 AoE，且不隶属于任何技能）</li>
 *   <li>体术系（星间飞行、十划一击 / 冲撞 / 冲 / 一文字）—— 分型默认即放行，无需声明</li>
 * </ul>
 */
public final class CritSkillPolicy
{
    /** 按设计不参与暴击的技能 id。 */
    private static final Set<String> NO_CRIT_SKILLS = Set.of(
            // —— 无下限 ——
            LimitlessTechniqueSkillSet.SKILL_BLUE,
            LimitlessTechniqueSkillSet.SKILL_PURPLE,
            LimitlessTechniqueSkillSet.SKILL_REMOTE_PURPLE,
            // —— 御厨子 ——
            ShrineTechniqueSkillSet.SKILL_KAI,
            ShrineTechniqueSkillSet.SKILL_KAI_FLYOBJ,
            ShrineTechniqueSkillSet.SKILL_HACHI,
            ShrineTechniqueSkillSet.SKILL_FUGA,
            // —— 十划 ——
            RatioTechniqueSkillSet.SKILL_COLLAPSE);

    /**
     * 按设计不参与暴击的<b>说明节点</b> nodeId。
     * <p>
     * 目前只有「伏魔神龛」：领域斩击是每 10 tick 的持续伤害（{@code ShrineDomainExpansion}
     * 的 {@code tickShrineSlashDamage}），属于高频出伤。其余三个领域不直接出伤，所以不列。
     */
    private static final Set<String> NO_CRIT_INFO_NODES = Set.of(
            ShrineTechniqueSkillSet.INFO_DOMAIN_FUMOSHINKAN);

    private CritSkillPolicy()
    {
    }

    /**
     * 该技能是否按设计不参与暴击（悬停框据此决定要不要打标注行）。
     * <p>
     * 未声明的技能一律返回 {@code false}：这行标注的语义是「这个招式是特例，系统不让它暴击」，
     * 而不是「本技能没有任何暴击可能」，所以不做「未声明 = 不参与」的反向推定。
     */
    public static boolean isNoCritSkill(String skillId)
    {
        return skillId != null && NO_CRIT_SKILLS.contains(skillId);
    }

    /** 该说明节点是否按设计不参与暴击（领域节点的 skillId 为空，它的悬停框走这条）。 */
    public static boolean isNoCritInfoNode(String nodeId)
    {
        return nodeId != null && NO_CRIT_INFO_NODES.contains(nodeId);
    }

    /**
     * 启动自检：两份声明名单必须与技能表对得上（由 {@code JuJutsuKaisenZone} 在全部技能集注册后调用）。
     * <p>
     * 查的是<b>字符串名单最典型的腐化方式</b>——拼错、改名后漏改、节点种类变了：
     * <ul>
     *   <li>名单里的技能 id 必须存在于已注册技能表（技能节点或公共技能）。拼错 ⇒ 那行标注
     *       <b>永远不出现</b>，界面上只是"少了一行字"，没有任何报错。</li>
     *   <li>名单里的说明节点必须真的是<b>领域</b>说明节点（靠
     *       {@code visibleWhen(UnlockCondition.hasDomainExpansion())} 判定）。条件被改成别的 ⇒
     *       悬停框里那行标注同样静默消失。</li>
     *   <li>两份名单都不许为空：清空等于把标注功能关掉，应当是有意的改动而不是手滑。</li>
     * </ul>
     * ⚠️ <b>本自检查不到的一件事</b>：「某个单发招式忘了走 {@code dealCrit}」——出伤点手里只有
     * owner、拿不到 skillId，运行时无法把伤害归到技能上，所以「新增的术式单发招式漏了放行」
     * 只能靠新增技能时人工对照「放行出伤点清单」（见 {@link #NO_CRIT_SKILLS} 上方的说明）。
     * <p>
     * 抛 {@link IllegalStateException} 与 {@code TechniqueSkillTree#validate} 同一风格：
     * 这类错误在开发期就该炸掉，别让它以"界面少一行字"的形式活到玩家那里。
     */
    public static void validate()
    {
        Set<String> knownSkillIds = new LinkedHashSet<>();
        Set<String> knownDomainInfoNodeIds = new LinkedHashSet<>();
        for (TechniqueSkillTree tree : TechniqueSkillTreeRegistry.all())
        {
            for (SkillTreeNodeDef node : tree.nodes())
            {
                if (node.skillId() != null && !node.skillId().isBlank())
                {
                    knownSkillIds.add(node.skillId());
                }
                if (node.isDomainInfoNode())
                {
                    knownDomainInfoNodeIds.add(node.nodeId());
                }
            }
        }
        // 公共技能（强化术等）不在技能树里，单独并进来
        for (TechniqueSkill skill : CommonTechniqueSkillRegistry.getConfigurableSkills())
        {
            knownSkillIds.add(skill.skillId());
        }

        if (NO_CRIT_SKILLS.isEmpty() || NO_CRIT_INFO_NODES.isEmpty())
        {
            throw new IllegalStateException("暴击声明表被清空了：CritSkillPolicy 的两份名单都至少要有内容，"
                    + "否则「不参与暴击」标注会静默消失");
        }
        if (knownSkillIds.isEmpty())
        {
            throw new IllegalStateException("暴击声明表自检时机不对：此刻技能表为空，"
                    + "本方法必须在全部技能集注册完成之后调用");
        }
        for (String skillId : NO_CRIT_SKILLS)
        {
            if (!knownSkillIds.contains(skillId))
            {
                throw new IllegalStateException("「不参与暴击」名单里是未知技能 id：" + skillId
                        + "——拼错了，或技能改名后这里没跟着改（后果是那行标注永不出现在界面上）");
            }
        }
        for (String nodeId : NO_CRIT_INFO_NODES)
        {
            if (!knownDomainInfoNodeIds.contains(nodeId))
            {
                throw new IllegalStateException("「不参与暴击」名单里的说明节点不是领域说明节点：" + nodeId
                        + "——领域节点靠 visibleWhen(UnlockCondition.hasDomainExpansion()) 判定，"
                        + "可见性条件改成别的就会掉出名单");
            }
        }
    }
}
