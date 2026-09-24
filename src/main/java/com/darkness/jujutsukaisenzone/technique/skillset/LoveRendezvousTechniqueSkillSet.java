package com.darkness.jujutsukaisenzone.technique.skillset;

import com.darkness.jujutsukaisenzone.compat.EpicFightCompat;
import com.darkness.jujutsukaisenzone.player.PlayerStarMarkData;
import com.darkness.jujutsukaisenzone.player.PlayerTechniqueData;
import com.darkness.jujutsukaisenzone.player.StarMark;
import com.darkness.jujutsukaisenzone.player.StarMarkType;
import com.darkness.jujutsukaisenzone.technique.development.SkillTreeNodeDef;
import com.darkness.jujutsukaisenzone.technique.development.SkillUnlockLogic;
import com.darkness.jujutsukaisenzone.technique.development.TechniqueSkillTree;
import com.darkness.jujutsukaisenzone.technique.development.UnlockCondition;
import com.darkness.jujutsukaisenzone.technique.skill.StarFlightSkillLogic;
import com.darkness.jujutsukaisenzone.technique.skill.StarMarkDetonation;
import com.darkness.jujutsukaisenzone.technique.skill.StarMarkRules;
import com.darkness.jujutsukaisenzone.technique.skill.StarMarkSkillLogic;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * 星间飞行（Love Rendezvous）术式招式组。
 * <p>
 * 槽位 1「星星标记」：近战命中实体 / 看向方块后按技能键即打上「当前标记」，咒力在打标那一刻结算一次。
 * 对同一目标重复打<b>同一颗星</b>会叠加星位等级（上限 {@link StarMark#MAX_STACKS} 级），打<b>另一颗星</b>
 * 则是更换（等级归 1）；叠满之后再叠一次即<b>引爆</b>——消耗掉全部层数、对爆心的原目标结算一次
 * <b>单体</b>伤害（方块 / 掉落物 / 施术者自己除外）、给半径内其他实体赋该星对应的负面效果、
 * 并把标记传染给尚未被标记的目标，见 {@link StarMarkDetonation}。
 * 槽位 2「牵引」：把当前标记的所有目标拉向自己，持续 5 秒。
 * 槽位 3「禁行」：展开星域，域内跨序之星无法靠近。星域<b>没有时限</b>，展开后一直生效，
 * 半径随「结界半径」调节模式（见 {@link StarMarkRules#gateRadius(ServerPlayer)}）。
 * 槽位 4「飞行，穿越」：与目标身上一颗<b>星位叠满</b>的标记相距 20 格以内时，先收掉全部星域，
 * 再以高速<b>追踪平移</b>直取目标；撞上后先引爆旧印记，随即结算冲撞伤害并补上「当前选中」的那颗印记，
 * 最后看这颗新印记与施术者自身标记是否跨序——是才把目标弹飞（永远是施术者弹飞目标）。
 * 见 {@link StarFlightSkillLogic}。
 * <p>
 * 三态力学（同标记吸引 / 相邻无效果 / 跨序逐级解锁）见 {@link StarMarkRules}。
 * <p>
 * 不占槽位的特别招式见 {@link #SPECIAL_MOVES}：目前只有「禁行（原地）」——<b>Shift + 技能键</b>
 * 把星域锚在释放那一刻的坐标，不随施术者移动；经济与跟随版同源（照常扣 60 咒力、吃同一份冷却）。
 * <p>
 * 标记的三种衍生输入：
 * <ul>
 *   <li><b>Shift + 技能键</b>：把当前标记打在自己身上；自己已有同一颗星时改为<b>自我清除</b>（免费）。</li>
 *   <li><b>Ctrl + 技能键</b>：<b>定向清除</b>当前标记这一颗星的全部标记（含自身），并中断以它为引的牵引。</li>
 *   <li><b>快速双击 Shift</b>：准星命中的已标记目标 → 单独清除；未命中 → 清除自己持有的全部标记。
 *       双击只能在客户端检测（服务端拿不到 Shift 的原始按键状态），见 {@link #clearByCrosshair(ServerPlayer)}。</li>
 * </ul>
 * <p>
 * 星域的两个衍生输入：
 * <ul>
 *   <li><b>Shift + 技能键</b>：「禁行（原地）」，圆心锚在释放点。</li>
 *   <li><b>Ctrl + 技能键</b>：<b>解除星域</b>（跟随 / 固定一并解除），免费、不吃冷却。</li>
 * </ul>
 */
public final class LoveRendezvousTechniqueSkillSet extends AbstractInnateTechniqueSkillSet
{
    public static final LoveRendezvousTechniqueSkillSet INSTANCE = new LoveRendezvousTechniqueSkillSet();

    public static final String SKILL_STAR_MARK = "love_rendezvous_star_mark";
    public static final String SKILL_STAR_PULL = "love_rendezvous_star_pull";
    public static final String SKILL_STAR_GATE = "love_rendezvous_star_gate";
    public static final String SKILL_STAR_FLIGHT = "love_rendezvous_star_flight";

    private static final int SLOT_STAR_MARK = 1;
    private static final int SLOT_STAR_PULL = 2;
    private static final int SLOT_STAR_GATE = 3;
    /** 技能栏每 3 个槽位为一组，所以第 4 个技能落在第二组的第 1 格。 */
    private static final int SLOT_STAR_FLIGHT = 4;

    private static final int STAR_MARK_COST = 30;
    private static final int STAR_MARK_COOLDOWN_TICKS = 0;
    private static final int STAR_PULL_COST = 40;
    private static final int STAR_PULL_COOLDOWN_TICKS = 8 * 20;
    private static final int STAR_GATE_COST = 60;
    private static final int STAR_GATE_COOLDOWN_TICKS = 30 * 20;
    private static final int STAR_FLIGHT_COST = 80;
    private static final int STAR_FLIGHT_COOLDOWN_TICKS = 45 * 20;

    private static final String ICON_STAR_MARK = "textures/gui/technique/star_mark.png";
    /** 牵引 / 禁行暂时复用星标图标，待补美术后替换。 */
    private static final String ICON_STAR_PULL = ICON_STAR_MARK;
    private static final String ICON_STAR_GATE = ICON_STAR_MARK;
    /** 「飞行，穿越」同样暂时复用星标图标，待补美术后替换。 */
    private static final String ICON_STAR_FLIGHT = ICON_STAR_MARK;

    private static final List<TechniqueSkill> SKILLS = List.of(
            new TechniqueSkill(SKILL_STAR_MARK, SLOT_STAR_MARK, Component.literal("星星标记"), ICON_STAR_MARK,
                    STAR_MARK_COST, STAR_MARK_COOLDOWN_TICKS, 0,
                    tag("cooldown", SLOT_STAR_MARK), tag("unlock", SLOT_STAR_MARK), tag("level", SLOT_STAR_MARK)),
            new TechniqueSkill(SKILL_STAR_PULL, SLOT_STAR_PULL, Component.literal("牵引"), ICON_STAR_PULL,
                    STAR_PULL_COST, STAR_PULL_COOLDOWN_TICKS, 0,
                    tag("cooldown", SLOT_STAR_PULL), tag("unlock", SLOT_STAR_PULL), tag("level", SLOT_STAR_PULL)),
            new TechniqueSkill(SKILL_STAR_GATE, SLOT_STAR_GATE, Component.literal("禁行"), ICON_STAR_GATE,
                    STAR_GATE_COST, STAR_GATE_COOLDOWN_TICKS, 0,
                    tag("cooldown", SLOT_STAR_GATE), tag("unlock", SLOT_STAR_GATE), tag("level", SLOT_STAR_GATE)),
            new TechniqueSkill(SKILL_STAR_FLIGHT, SLOT_STAR_FLIGHT, Component.literal("飞行，穿越"), ICON_STAR_FLIGHT,
                    STAR_FLIGHT_COST, STAR_FLIGHT_COOLDOWN_TICKS, 0,
                    tag("cooldown", SLOT_STAR_FLIGHT), tag("unlock", SLOT_STAR_FLIGHT), tag("level", SLOT_STAR_FLIGHT)));

    private static final TechniqueSkillTree SKILL_TREE = TechniqueSkillTree.builder(PlayerTechniqueData.TECHNIQUE_LOVE_RENDEZVOUS)
            .node(SkillTreeNodeDef.skill("love_rendezvous_star_mark", SKILL_STAR_MARK).autoGranted().build())
            /*
             * 说明节点：把「星星标记」描述里那三段修饰键操作搬出来——它们全是静态文案
             * （不含任何 %s），正好适合放进说明节点，让技能描述本身只留机制与数值。
             * 说明节点不参与连线与解锁判定，requires 在这里只是自动布局的锚点，
             * 但 visibilityConditions 必须给（{@code TechniqueSkillTree#validate} 强制要求）。
             */
            .node(SkillTreeNodeDef.info("love_rendezvous_star_mark_controls")
                    .titleKey("skill_tree.jujutsukaisenzone.love_rendezvous_star_mark_controls.title")
                    .iconPath(ICON_STAR_MARK)
                    .descriptionKey("skill_tree.jujutsukaisenzone.love_rendezvous_star_mark_controls.desc")
                    .requires("love_rendezvous_star_mark")
                    .visibleWhen(UnlockCondition.alwaysVisible())
                    .build())
            .node(SkillTreeNodeDef.skill("love_rendezvous_star_pull", SKILL_STAR_PULL)
                    .requires("love_rendezvous_star_mark")
                    .intelCost(200)
                    .build())
            .node(SkillTreeNodeDef.skill("love_rendezvous_star_gate", SKILL_STAR_GATE)
                    .requires("love_rendezvous_star_pull")
                    .intelCost(300)
                    .build())
            /*
             * 说明节点：「星星大碰撞！」——把星域展开的弹射、制导与撞击伤害这三件事讲成一条玩法。
             * 同样必须是纯静态文案（说明节点的描述走无参翻译，不吃 %s），
             * 因此所有数值都写死成「越小 / 越高」这类定性描述，不引用 {@code TechniqueSkillDescriptions}。
             */
            .node(SkillTreeNodeDef.info("love_rendezvous_star_gate_collision")
                    .titleKey("skill_tree.jujutsukaisenzone.love_rendezvous_star_gate_collision.title")
                    .iconPath(ICON_STAR_GATE)
                    .descriptionKey("skill_tree.jujutsukaisenzone.love_rendezvous_star_gate_collision.desc")
                    .requires("love_rendezvous_star_gate")
                    .visibleWhen(UnlockCondition.alwaysVisible())
                    .build())
            /*
             * 终结技「飞行，穿越」：把上面几件素材（自身标记 / 目标印记 / 跨序关系）拧成一次出手。
             * 挂在「禁行」之后，因为它末段弹飞的判据正是「跨序之星」——这套关系由星域那一步教会；
             * 但前置条件只要求「目标星位叠满」，跨不跨序由玩家切换当前标记来决定，
             * 即「这一脚踢不踢得飞」，见 StarFlightSkillLogic。
             */
            .node(SkillTreeNodeDef.skill("love_rendezvous_star_flight", SKILL_STAR_FLIGHT)
                    .requires("love_rendezvous_star_gate")
                    .intelCost(400)
                    .build())
            .build();

    /**
     * 不占独立槽位的特别招式（修饰键 + 技能键）。
     * <p>
     * 数值沿用原技能（等级 / 咒力 / 冷却 = 原技能那一份），因此这里只声明「挂在哪个原技能上、叫什么名字」；
     * 开发树节点由 {@code TechniqueSpecialMoveAttacher} 自动派生，不需要手写。图标留空 = 复用原技能图标。
     */
    private static final List<TechniqueSpecialMove> SPECIAL_MOVES = List.of(
            new TechniqueSpecialMove("love_rendezvous_star_gate_shift", SKILL_STAR_GATE, SpecialMoveInput.MOD_SHIFT,
                    Component.literal("禁行（原地）"), null));

    static
    {
        SKILL_TREE.validateAgainstSkills(SKILLS);
    }

    @Override
    public List<TechniqueSpecialMove> getSpecialMoves(String baseSkillId)
    {
        return SPECIAL_MOVES.stream()
                .filter(move -> move.baseSkillId().equals(baseSkillId))
                .toList();
    }

    private LoveRendezvousTechniqueSkillSet()
    {
    }

    /** 按项目命名约定生成 tag：{@code jujutsukaisenzone_love_rendezvous_<kind>_slot_<slot>}。 */
    private static String tag(String kind, int slot)
    {
        return "jujutsukaisenzone_love_rendezvous_" + kind + "_slot_" + slot;
    }

    private static TechniqueSkill skillForSlot(int slot)
    {
        return slot >= 1 && slot <= SKILLS.size() ? SKILLS.get(slot - 1) : null;
    }

    @Override
    public String techniqueId()
    {
        return PlayerTechniqueData.TECHNIQUE_LOVE_RENDEZVOUS;
    }

    @Override
    public List<TechniqueSkill> getConfigurableSkills()
    {
        return SKILLS;
    }

    @Override
    public Optional<TechniqueSkillTree> getSkillTree()
    {
        return Optional.of(SKILL_TREE);
    }

    // ======================== 释放 ========================

    @Override
    public void useSkill(ServerPlayer player, String skillId)
    {
        if (!EpicFightCompat.isBattleMode(player)
                || !techniqueId().equals(PlayerTechniqueData.getInnateTechnique(player)))
        {
            return;
        }
        TechniqueSkill skill = findSkillById(skillId);
        if (skill == null)
        {
            return;
        }
        if (SKILL_STAR_MARK.equals(skillId))
        {
            /* 打标本身在「接触」那一刻结算咒力 / 冷却（见 tryApplyMark）。
               实体走近战命中，方块走技能键——视线由服务端自行射线，客户端只报「按了键」。
               没瞄到方块时退化为报当前状态，不消耗。 */
            BlockPos looked = StarMarkSkillLogic.lookedAtBlock(player);
            if (looked == null)
            {
                notifyStarMarkStatus(player);
                return;
            }
            tryApplyMark(player, StarMarkSkillLogic.markOfBlock(player, looked),
                    StarMarkSkillLogic.describeBlock(looked));
            return;
        }
        if (SKILL_STAR_PULL.equals(skillId))
        {
            castWithSkillEconomy(player, skill, () -> StarMarkRules.activatePull(player));
            return;
        }
        if (SKILL_STAR_GATE.equals(skillId))
        {
            castWithSkillEconomy(player, skill, () -> StarMarkRules.activateGate(player));
            return;
        }
        if (SKILL_STAR_FLIGHT.equals(skillId))
        {
            /*
             * 前置条件（自身标记 / 目标星位叠满 / 20 格内）必须在**扣费之前**校验：
             * pickTarget 返回 null 时已经在动作栏报明原因，这里直接 return，不进 castWithSkillEconomy——
             * 否则「没瞄到符合条件的星」会白扣 80 咒力 + 白上一次 45 秒冷却。
             * 「跨序」不在前置里：它决定的是末段弹不弹飞，不影响这次出手能不能成行。
             * 目标在同一 tick 内不会失效，所以扣费之后 cast 里不再重复这套校验。
             */
            StarFlightSkillLogic.Target target = StarFlightSkillLogic.pickTarget(player);
            if (target == null)
            {
                return;
            }
            castWithSkillEconomy(player, skill, () -> StarFlightSkillLogic.cast(player, target));
            return;
        }
        super.useSkill(player, skillId);
    }

    /**
     * 特别招式分派：修饰键组合在这里**先于经济结算**分流。
     * <p>
     * 默认链路（{@link AbstractInnateTechniqueSkillSet#useSpecialMove(ServerPlayer, String, int)}）会先走
     * {@code castWithSkillEconomy} 结算解锁 / 等级 / 冷却 / 咒力，之后才轮到 {@link #castSpecialMoveBySkill}，
     * 所以「免费」的清除动作必须抢在扣费之前返回，否则会白扣一次咒力。
     * <p>
     * 打自身标记则相反：它与「星星标记」同源，照常扣 30 咒力、走同一份解锁与等级，因此放行到默认链路。
     * <p>
     * 「禁行（原地）」同理放行：它与「禁行」同源，照常扣 60 咒力、吃同一份冷却，唯一差别是星域圆心固定在释放点。
     * <p>
     * 免费的关闭动作在这里前置分流：<b>Ctrl + 技能键</b>解除星域（跟随 / 固定一并解除）。
     * 星域已取消时限，这是唯一的主动关闭手段，且不消耗咒力、不吃冷却。
     */
    @Override
    public void useSpecialMove(ServerPlayer player, String skillId, int modifierMask)
    {
        boolean starMark = SKILL_STAR_MARK.equals(skillId);
        boolean starGate = SKILL_STAR_GATE.equals(skillId);
        if (!starMark && !starGate)
        {
            super.useSpecialMove(player, skillId, modifierMask);
            return;
        }
        if (!EpicFightCompat.isBattleMode(player)
                || !techniqueId().equals(PlayerTechniqueData.getInnateTechnique(player))
                || !player.isAlive() || player.isSpectator())
        {
            return;
        }

        if (starMark)
        {
            if (modifierMask == SpecialMoveInput.MOD_CTRL)
            {
                clearSelectedType(player);
                return;
            }
            /* 只认「仅 Shift」；Shift+Ctrl、Alt 等组合直接忽略，避免走默认链路白扣一次经济 */
            if (modifierMask != SpecialMoveInput.MOD_SHIFT)
            {
                return;
            }
            if (hasOwnMark(player))
            {
                clearOwnMark(player);
                return;
            }
        }
        else
        {
            if (modifierMask == SpecialMoveInput.MOD_CTRL)
            {
                /* 免费动作：解除星域，不扣咒力、不上冷却，必须抢在默认链路之前分流 */
                boolean closed = StarMarkRules.deactivateGate(player);
                player.displayClientMessage(
                        Component.literal(closed ? "★禁行 · 星域解除" : "★禁行 · 当前没有生效中的星域"), true);
                return;
            }
            if (modifierMask != SpecialMoveInput.MOD_SHIFT)
            {
                /* 「禁行（原地）」照常扣费，因此放行到默认链路，只收口修饰键 */
                return;
            }
        }
        super.useSpecialMove(player, skillId, modifierMask);
    }

    /**
     * 本术式的特别招式组合声明。
     * <p>
     * {@code star_gate} 的 Shift 已经写进 {@link #SPECIAL_MOVES}，基类默认推导即可放行；
     * 但 {@code star_mark} 的 Shift（打自身标记 / 已有同星时改为自我清除）与 Ctrl（定向清除）、
     * {@code star_gate} 的 Ctrl（解除星域）都是**复用原技能行为、不进开发树**的输入，
     * 必须在这里单独放行，否则会在基类第一道收口被静默拦下。
     */
    @Override
    protected boolean acceptsSpecialMove(ServerPlayer player, String skillId, int modifierMask)
    {
        if (SKILL_STAR_MARK.equals(skillId))
        {
            return modifierMask == SpecialMoveInput.MOD_SHIFT || modifierMask == SpecialMoveInput.MOD_CTRL;
        }
        if (SKILL_STAR_GATE.equals(skillId) && modifierMask == SpecialMoveInput.MOD_CTRL)
        {
            return true;
        }
        return super.acceptsSpecialMove(player, skillId, modifierMask);
    }

    /**
     * 打标入口：近战命中实体（{@code StarMarkEvents}）与技能键命中方块（{@link #useSkill}）都走这里。
     * <p>
     * 内含战斗模式与术式校验，并复用星标技能的解锁 / 咒力 / 冷却结算。三种情形的<b>经济结算完全一致</b>
     * （都算「打一次标」），区别只在写入哪条记录：
     * <ol>
     *   <li>没有标记 / 挂的是别的星 → 1 级新标记（换星即从 1 级重新叠起）；</li>
     *   <li>同一颗星但未叠满 → <b>叠加</b>一层；</li>
     *   <li>同一颗星且已叠满 → <b>引爆</b>，消耗全部层数并波及 / 传染周围，见 {@link StarMarkDetonation}。</li>
     * </ol>
     * 因此「重复标记」不再是空操作——它是有收益的正向行为，这里不再有「无需重复标记」的早退。
     * <p>
     * 写入统一走 {@link PlayerStarMarkData#put}：新目标会占用一个持有位，超出
     * {@link PlayerStarMarkData#MAX_MARKS} 时顶掉「最久没被打标」的那条（杀生樱式），
     * 并把被顶掉的目标报在动作栏里。
     */
    public void tryApplyMark(ServerPlayer player, StarMark target, String targetName)
    {
        if (!EpicFightCompat.isBattleMode(player)
                || !techniqueId().equals(PlayerTechniqueData.getInnateTechnique(player)))
        {
            return;
        }
        if (player.isSpectator() || !player.isAlive())
        {
            return;
        }

        StarMarkType selected = PlayerStarMarkData.getSelected(player);
        StarMark existing = PlayerStarMarkData.findTarget(player, target);
        TechniqueSkill skill = findSkillById(SKILL_STAR_MARK);
        if (skill == null)
        {
            return;
        }

        if (existing != null && existing.type() == selected)
        {
            if (existing.stacks() >= StarMark.MAX_STACKS)
            {
                castWithSkillEconomy(player, skill, () -> detonateMark(player, existing, targetName));
                return;
            }
            castWithSkillEconomy(player, skill, () -> stackMark(player, target, existing, targetName));
            return;
        }
        castWithSkillEconomy(player, skill, () -> applyMark(player, target, targetName, existing));
    }

    private void applyMark(ServerPlayer player, StarMark target, String targetName, @Nullable StarMark existing)
    {
        PlayerStarMarkData.PutOutcome outcome = PlayerStarMarkData.put(player, target);
        String suffix = existing == null ? "" : "（已覆盖 " + existing.type().displayName() + "）";
        player.displayClientMessage(
                Component.literal(target.type().displayName() + " · 已标记" + targetName + suffix
                        + evictionNote(player, outcome)), true);
    }

    /**
     * 持有上限的顶掉提示（杀生樱式：最多 {@link PlayerStarMarkData#MAX_MARKS} 条，
     * 新的顶掉最久没打的那条）。
     * <p>
     * 只在真的顶掉了东西时追加，所以正常打标（没满员）看不到这句。
     */
    private static String evictionNote(ServerPlayer player, PlayerStarMarkData.PutOutcome outcome)
    {
        if (outcome.evicted().isEmpty())
        {
            return "";
        }
        StringBuilder builder = new StringBuilder(
                "；已达上限 " + PlayerStarMarkData.MAX_MARKS + " 条，顶掉最旧的 ");
        for (int i = 0; i < outcome.evicted().size(); i++)
        {
            if (i > 0)
            {
                builder.append('、');
            }
            builder.append(labelOf(player, outcome.evicted().get(i)));
        }
        return builder.toString();
    }

    /** 叠加：同一颗星 +1 级，按目标身份原地替换记录（{@code target} 与 {@code existing} 同目标）。 */
    private void stackMark(ServerPlayer player, StarMark target, StarMark existing, String targetName)
    {
        int stacks = existing.stacks() + 1;
        PlayerStarMarkData.put(player, target.withStacks(stacks));
        player.displayClientMessage(
                Component.literal(existing.type().displayName() + " · " + targetName
                        + " 星位 " + stacks + "/" + StarMark.MAX_STACKS), true);
    }

    /**
     * 引爆：叠满之后再叠一次，标记被消耗掉，无法原地重复触发。
     * <p>
     * 爆心取标记的锚点——实体标记用实体头顶的锚点，方块标记用方块上方锚点，与火花表现同源，
     * 因此爆炸看起来就发生在那个标记光点上，而不是目标的脚下。
     * <p>
     * 伤害只落在爆心那一个目标身上，且方块 / 掉落物 / 施术者自己拿不到伤害，
     * 所以提示里按 {@link StarMarkDetonation.Result#damaged()} 决定要不要报这个数。
     */
    private void detonateMark(ServerPlayer player, StarMark existing, String targetName)
    {
        Vec3 center = StarMarkSkillLogic.anchorOf(existing, resolveAnchor(player, existing));
        StarMarkDetonation.Result result = StarMarkDetonation.detonate(player, existing, center);
        player.displayClientMessage(
                Component.literal("★" + existing.type().displayName() + " 星位满溢 · " + targetName + " 引爆"
                        + (result.damaged()
                        ? "；单体 " + (int) StarMarkDetonation.DETONATION_DAMAGE + " 伤害" : "")
                        + "；波及 " + result.affected() + " 个目标"
                        + (result.spread() > 0 ? "，标记传染 " + result.spread() + " 个" : "")
                        + (result.evicted() > 0 ? "，顶掉最旧 " + result.evicted() + " 条" : "")), true);
    }

    /**
     * 取爆心用的实体：只有实体 / 掉落物标记才有。解析不到（区块未加载、实体已消失）时返回 null，
     * 由 {@link StarMarkSkillLogic#anchorOf} 退回按最后一次已知坐标估算高度。
     */
    @Nullable
    private static Entity resolveAnchor(ServerPlayer player, StarMark mark)
    {
        return mark.entityId() == null ? null : player.serverLevel().getEntity(mark.entityId());
    }

    private void notifyStarMarkStatus(ServerPlayer player)
    {
        StarMarkType selected = PlayerStarMarkData.getSelected(player);
        int count = PlayerStarMarkData.countOfType(player, selected);
        int total = PlayerStarMarkData.count(player);
        player.displayClientMessage(
                Component.literal("★当前标记 " + selected.displayName() + " · 本星 " + count
                        + " 个、共 " + total + "/" + PlayerStarMarkData.MAX_MARKS
                        + " 个目标；近战命中实体 / 看向方块按技能键即可打标"
                        + StarMarkRules.unlockStatus(player)),
                true);
    }

    // ======================== 特别招式：自身标记与清除 ========================

    /**
     * Shift + 技能键的演出部分，两条互不相干：
     * <ul>
     *   <li><b>星星标记</b>：把「当前标记」打在自己身上。</li>
     *   <li><b>禁行</b>：把星域锚在释放点（原地版），不随施术者移动。</li>
     * </ul>
     * <p>
     * 经济（咒力 / 冷却 / 解锁 / 等级）已由基类按各自的原技能结算完毕，这里只负责写入状态。
     * 自己身上的标记与他者**结构完全一致**——同一条 {@link StarMark} 记录、走 {@code ENTITY} 分支，
     * 因此在力学统计、准星单独清除、定向清除里都按普通标记对待。
     */
    @Override
    protected void castSpecialMoveBySkill(ServerPlayer player, String skillId, int modifierMask)
    {
        if (modifierMask != SpecialMoveInput.MOD_SHIFT)
        {
            return;
        }
        if (SKILL_STAR_GATE.equals(skillId))
        {
            StarMarkRules.activateFixedGate(player);
            return;
        }
        if (!SKILL_STAR_MARK.equals(skillId))
        {
            return;
        }
        StarMark mark = StarMarkSkillLogic.markOfEntity(player, player);
        StarMark existing = PlayerStarMarkData.findTarget(player, mark);
        PlayerStarMarkData.PutOutcome outcome = PlayerStarMarkData.put(player, mark);
        String suffix = existing == null ? "" : "（已覆盖 " + existing.type().displayName() + "）";
        player.displayClientMessage(
                Component.literal(mark.type().displayName() + " · 已标记自身" + suffix
                        + evictionNote(player, outcome)), true);
    }

    /** 施术者自己身上是否已有标记（任意一颗星）。 */
    private static boolean hasOwnMark(ServerPlayer player)
    {
        return PlayerStarMarkData.findTarget(player, StarMarkSkillLogic.markOfEntity(player, player)) != null;
    }

    /** 自我清除：给自己打相同标记即视为清除标记，免费、不覆盖、不消耗。 */
    private void clearOwnMark(ServerPlayer player)
    {
        StarMark own = StarMarkSkillLogic.markOfEntity(player, player);
        StarMark existing = PlayerStarMarkData.findTarget(player, own);
        if (existing == null)
        {
            return;
        }
        PlayerStarMarkData.remove(player, own);
        player.displayClientMessage(
                Component.literal("★已清除自身标记：" + existing.type().displayName()), true);
    }

    /**
     * 定向清除：移除「当前标记」这一颗星的全部标记（含自己身上那颗）。
     * <p>
     * 牵引以当前标记为引（{@link StarMarkRules} 每 tick 读取当前选中星），引被清掉后只会空转，因此一并中断。
     */
    private void clearSelectedType(ServerPlayer player)
    {
        StarMarkType selected = PlayerStarMarkData.getSelected(player);
        int removed = PlayerStarMarkData.removeByType(player, selected);
        if (removed <= 0)
        {
            player.displayClientMessage(
                    Component.literal("★" + selected.displayName() + " 当前没有标记"), true);
            return;
        }
        boolean pulled = StarMarkRules.clearPull(player);
        player.displayClientMessage(
                Component.literal("★定向清除 " + selected.displayName() + " · 共 " + removed + " 个标记"
                        + (pulled ? "，牵引已中断" : "")), true);
    }

    /**
     * 快速双击 Shift 的清除：准星命中已标记目标时单独清除该目标，否则清除自己持有的全部标记。
     * <p>
     * 由 {@code StarMarkClearC2SPacket} 调用——双击只能在客户端检测，所以服务端这里重新做一次
     * 战斗模式与生得术式校验，再自行按准星判定分支，客户端不参与目标选择。
     */
    public void clearByCrosshair(ServerPlayer player)
    {
        if (!EpicFightCompat.isBattleMode(player)
                || !techniqueId().equals(PlayerTechniqueData.getInnateTechnique(player))
                || !player.isAlive() || player.isSpectator())
        {
            return;
        }

        StarMark target = StarMarkSkillLogic.pickMarkedTarget(player);
        if (target != null)
        {
            StarMark existing = PlayerStarMarkData.findTarget(player, target);
            PlayerStarMarkData.remove(player, target);
            player.displayClientMessage(
                    Component.literal("★已清除 " + labelOf(player, target) + " 的标记"
                            + (existing == null ? "" : "（" + existing.type().displayName() + "）")), true);
            return;
        }

        int removed = PlayerStarMarkData.clearAll(player);
        if (removed <= 0)
        {
            player.displayClientMessage(Component.literal("★当前没有标记"), true);
            return;
        }
        /* 全部清掉后牵引已无目标可拉，一并中断 */
        StarMarkRules.clearPull(player);
        player.displayClientMessage(Component.literal("★已清除全部标记 · 共 " + removed + " 个"), true);
    }

    /** 清除提示用的目标描述：实体优先取当下解析到的名字，取不到再退回泛化描述。 */
    private static String labelOf(ServerPlayer player, StarMark mark)
    {
        if (mark.kind() != StarMark.Kind.BLOCK && mark.entityId() != null)
        {
            Entity entity = player.serverLevel().getEntity(mark.entityId());
            if (entity != null)
            {
                return StarMarkSkillLogic.describeTarget(entity);
            }
        }
        return mark.targetLabel();
    }

    @Override
    protected void castSkillSlot(ServerPlayer player, int slot)
    {
        // 释放统一走 skillId 驱动的 useSkill
    }

    // ======================== 槽位属性 ========================

    @Override
    protected int getSlotCost(int slot)
    {
        TechniqueSkill skill = skillForSlot(slot);
        return skill == null ? 0 : skill.cost();
    }

    @Override
    protected int getSlotCooldownTicks(int slot)
    {
        TechniqueSkill skill = skillForSlot(slot);
        return skill == null ? 0 : skill.cooldownTicks();
    }

    @Override
    protected boolean isSlotUnlocked(Player player, int slot)
    {
        TechniqueSkill skill = skillForSlot(slot);
        return skill != null && SkillUnlockLogic.isSkillUnlocked(player, techniqueId(), skill.skillId(), skill);
    }

    @Override
    protected int getSlotSkillLevel(Player player, int slot)
    {
        TechniqueSkill skill = skillForSlot(slot);
        if (skill == null)
        {
            return 0;
        }
        String key = skill.levelTag();
        if (key == null || key.isEmpty())
        {
            return 0;
        }
        if (!player.getPersistentData().contains(key))
        {
            return 1;
        }
        return Math.max(1, player.getPersistentData().getInt(key));
    }

    @Override
    protected String getSlotCooldownTag(int slot)
    {
        TechniqueSkill skill = skillForSlot(slot);
        return skill == null ? null : skill.cooldownTag();
    }

    @Override
    public String getSlotDisplayName(int slot)
    {
        TechniqueSkill skill = skillForSlot(slot);
        return skill == null ? null : skill.displayName().getString();
    }

    @Override
    public String getSlotIconPath(int slot)
    {
        TechniqueSkill skill = skillForSlot(slot);
        return skill == null ? null : skill.iconPath();
    }
}
