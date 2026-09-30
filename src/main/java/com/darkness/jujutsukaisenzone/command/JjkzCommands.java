package com.darkness.jujutsukaisenzone.command;

import com.darkness.jujutsukaisenzone.effect.RushIntoTheZoneClimaxEffect;
import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.advancement.HeavenlyRestrictionAdvancements;
import com.darkness.jujutsukaisenzone.entity.DemonSubduingShrineEntity;
import com.darkness.jujutsukaisenzone.entity.SpecialEffectEntity;
import com.darkness.jujutsukaisenzone.entity.WedgeEntity;
import com.darkness.jujutsukaisenzone.gameplay.ModRules;
import com.darkness.jujutsukaisenzone.gameplay.CurseSpiritSpawnGrade;
import com.darkness.jujutsukaisenzone.gameplay.TengenWedgeBarrierLogic;
import com.darkness.jujutsukaisenzone.gameplay.TengenWedgeCurseSpiritSpawns;
import com.darkness.jujutsukaisenzone.gameplay.TengenWedgeStartResult;
import com.darkness.jujutsukaisenzone.player.CombatPowerStats;
import com.darkness.jujutsukaisenzone.player.CursedGradeBonuses;
import com.darkness.jujutsukaisenzone.player.JjkzPlayerDataDisplay;
import com.darkness.jujutsukaisenzone.player.PlayerCombatPowerData;
import com.darkness.jujutsukaisenzone.player.PlayerCursedEnergyData;
import com.darkness.jujutsukaisenzone.player.PlayerCursedGradeData;
import com.darkness.jujutsukaisenzone.player.PlayerFactionData;
import com.darkness.jujutsukaisenzone.player.PlayerHealthData;
import com.darkness.jujutsukaisenzone.player.PlayerMeltdownRepairData;
import com.darkness.jujutsukaisenzone.player.PlayerReverseTechniqueData;
import com.darkness.jujutsukaisenzone.technique.MeltdownRepairLearnLogic;
import com.darkness.jujutsukaisenzone.player.PlayerTenShadowsContractData;
import com.darkness.jujutsukaisenzone.player.PlayerTechniqueData;
import com.darkness.jujutsukaisenzone.player.PlayerTechniqueIntelData;
import com.darkness.jujutsukaisenzone.technique.skill.TenShadowsShikigami;
import com.darkness.jujutsukaisenzone.player.PlayerSpData;
import com.darkness.jujutsukaisenzone.player.PlayerSpNodeData;
import com.darkness.jujutsukaisenzone.player.PlayerSeLoadoutData;
import com.darkness.jujutsukaisenzone.player.CursedEnergyTrait;
import com.darkness.jujutsukaisenzone.player.PlayerCursedEnergyTraitData;
import com.darkness.jujutsukaisenzone.player.PlayerQualityPoolData;
import com.darkness.jujutsukaisenzone.technique.se.SeEffectRegistry;
import com.darkness.jujutsukaisenzone.player.BarrierTechniqueLevel;
import com.darkness.jujutsukaisenzone.player.PlayerBarrierTechniqueData;
import com.darkness.jujutsukaisenzone.player.PlayerSeanceTechniqueData;
import com.darkness.jujutsukaisenzone.player.PlayerStrengthenTechniqueData;
import com.darkness.jujutsukaisenzone.player.SeanceTechniqueLevel;
import com.darkness.jujutsukaisenzone.player.StrengthenTechniqueLevel;
import com.darkness.jujutsukaisenzone.technique.DomainExpansionServerLogic;
import com.darkness.jujutsukaisenzone.technique.domain.JudgmentDomainTrialLogic;
import com.darkness.jujutsukaisenzone.network.ModNetwork;
import com.darkness.jujutsukaisenzone.compat.StylizedDamageIntegrationRegistrar;
import com.darkness.jujutsukaisenzone.registry.ModEntities;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class JjkzCommands
{
    /** 教程分类 →（主题 → 说明文字） */
    private static final Map<String, Map<String, String>> ASK_CATEGORIES = new LinkedHashMap<>();

    static
    {
        Map<String, String> gettingStarted = new LinkedHashMap<>();
        gettingStarted.put("阵营选择", "首次登录点击聊天栏选择；也可用 /jjkz select。需要教程帮助时使用 /jjkz ask。");
        gettingStarted.put("生得术式", "选择术师/咒灵阵营后随机觉醒。");
        gettingStarted.put("天与咒缚", "咒力换超强肉体；肉体换咒力的咒缚可能在后续加入。");
        gettingStarted.put("咒力", "蓝条。");
        gettingStarted.put("咒术等级", "击杀生物升级。");
        gettingStarted.put("查看个人数据（常用）", "/jjkz data 可查看阵营、咒力、术式、各技能等级等");
        ASK_CATEGORIES.put("入门必看", gettingStarted);

        Map<String, String> combatGuide = new LinkedHashMap<>();
        combatGuide.put("战斗模式", "进入 Epic Fight 战斗模式后，体术与术式输入才会生效；黑闪、招式槽、释放情绪等皆依赖此状态。");
        combatGuide.put("招式组", "九个招式槽分三组，Tab+1/2/3 切换当前组，1/2/3 释放组内招式；支持蓄力的槽位长按后可最大输出。");
        combatGuide.put("技能快捷配置", "默认 I 键打开配置界面，将术式开发中已解锁的招式拖入九个槽位，战斗时即由此调用。");
        combatGuide.put("术式开发", "默认 Y 键打开技能树，满足前置与术式情报门槛即可解锁新招式；情报作门槛，解锁后不消耗。");
        combatGuide.put("释放情绪", "默认 Z 键按住蓄力，在战斗模式下将情绪转化为咒力回流。");
        combatGuide.put("特别招式", "Shift/Ctrl/Alt 配合数字键触发的变体招式，不占独立槽位，具体组合因生得术式而异。");
        combatGuide.put("快捷键",
                """
                默认键位可于「咒术回战：ZONE」改绑，组合逻辑不变。
                界面：Y 术式开发，I 技能栏配置。
                通用：G 按住反转术式，Ctrl+G 按住修补结界/领域；B 结界术，Alt+B 领域展开（已在自有领域内则解除），Shift+B 结界规则；O 降灵拟态，Z 按住释放情绪。
                招式：1/2/3 释放，Tab+1/2/3 切组，Shift/Ctrl/Alt+数字键为特别招式。
                调节模式：中键循环切换（施法距离/结界半径；生得术式为星间飞行时另有「星星标记」），侧键4/5调档或切换标记；三个键均可改键。
                星间飞行：槽位1 星标——近战命中实体 / 看向方块按技能键即打上当前标记（同一目标至多一个；超出 32 格、换维度、方块被破坏或目标消失即解除）；槽位2 牵引——把当前标记的目标拉向自己；槽位3 禁行——展开星域，域内之星必须按顺序经过才能靠近下一颗（接触星点解锁它和它的邻居，逐级推进；五颗走完后不再受限），无时限，半径与结界共用「结界半径」档位，Shift+技能键把星域锚在释放点，Ctrl+技能键解除星域。
                战斗模式切换见 Epic Fight 设置。X 键预留未实装。""".stripIndent().trim());
        ASK_CATEGORIES.put("战斗方法", combatGuide);

        Map<String, String> techniqueLearning = new LinkedHashMap<>();
        techniqueLearning.put("反转术式的习得方法",
                "在拥有一定咒力总量的基础上，游走于生死边缘时容易顿悟，可学习《咒力流动反转理论》提升顿悟成功率");
        techniqueLearning.put("结界术的习得方法",
                "将咒力铺成一方封闭的帐，需要先读懂空间的语法。《结界的构建与要术》读至三遍，念头才会在醒来的那一刻成形。");
        techniqueLearning.put("降灵术的习得方法",
                "生者借亡者残响短暂拟合他术——此道须在地狱研读《灵魂研究手册》，待咒力与残响在意识里重合三遍，门扉自开。");
        techniqueLearning.put("领域展开的习得方法",
                "须先能构筑结界，且已觉醒生得术式；在入眠或濒死之际，意识有时会沉入自己的「生得领域」。研读《虚构空间的具象化》可让那次漂流更容易发生。");
        ASK_CATEGORIES.put("技法学习", techniqueLearning);

        Map<String, String> itemAcquisition = new LinkedHashMap<>();
        itemAcquisition.put("四本理论书",
                "《咒力流动反转理论》《结界的构建与要术》《虚构空间的具象化》《灵魂研究手册》——可在世界各地的结构宝箱中偶然发现，亦可向大师级图书管理员购得；每次补货随机陈列一种，以金锭或钻石交换，每轮限购一本。");
        itemAcquisition.put("术式情报",
                "击杀怪物时有概率掉落；对已觉醒生得术式者，右键使用可纳入术式开发所用的情报（每次二十点）。");
        ASK_CATEGORIES.put("物品获取", itemAcquisition);
    }

    private JjkzCommands()
    {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event)
    {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(
                Commands.literal("jjkz")
                        .then(Commands.literal("data")
                                .executes(ctx -> executeShowPlayerData(ctx.getSource().getPlayerOrException())))
                        .then(Commands.literal("domainTrial")
                                .then(Commands.literal("start")
                                        .executes(ctx ->
                                        {
                                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                                            if (!JudgmentDomainTrialLogic.tryStartTrial(player))
                                            {
                                                player.sendSystemMessage(Component.literal("当前无法开始审判。"));
                                                return 0;
                                            }
                                            return 1;
                                        })))
                        .then(buildAskBranch())
                        .then(Commands.literal("select")
                                .then(Commands.argument("faction", StringArgumentType.word())
                                        .executes(ctx ->
                                        {
                                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                                            String arg = StringArgumentType.getString(ctx, "faction");
                                            return executeSelect(player, arg);
                                        })))
                        .then(buildFactionCheatBranch())
                        .then(buildCurseLevelCheatBranch())
                        .then(buildReverseTechniqueCheatBranch())
                        .then(buildMeltdownRepairCheatBranch())
                        .then(buildCursedEnergyCheatBranch())
                        .then(buildTechniqueIntelCheatBranch())
                        .then(buildCombatPowerBranch())
                        .then(buildSpCheatBranch())
                        .then(buildSpNodeCheatBranch())
                        .then(buildSeCheatBranch())
                        .then(buildTraitCheatBranch())
                        .then(buildBarrierTechniqueCheatBranch())
                        .then(buildSeanceTechniqueCheatBranch())
                        .then(buildStrengthenTechniqueCheatBranch())
                        .then(buildDomainExpansionCheatBranch())
                        .then(buildTenShadowsCheatBranch())
                        .then(buildSpecialEffectBranch())
                        .then(buildDemonSubduingShrineBranch())
                        .then(buildTengenWedgeBranch())
                        .then(buildWedgeSpawnBranch())
                        .then(buildCurseSpiritSpawnBranch())
                        .then(buildWhiteOverlayBranch())
                        .then(buildWhiteCurtainBranch())
                        .then(buildRushIntoTheZoneClimaxBranch())
                        .then(buildModRulesBranch())
                        .then(buildDomainSphereDebugBranch())
        );
    }

    private static int executeSelect(ServerPlayer player, String arg)
    {
        if (PlayerFactionData.hasFaction(player))
        {
            player.sendSystemMessage(Component.literal("你已选择过阵营，如需修改请使用作弊指令。"));
            return 0;
        }
        String id = PlayerFactionData.normalizeFactionInput(arg);
        if (id == null)
        {
            player.sendSystemMessage(Component.literal("未知的阵营，请从聊天中点击选项。"));
            return 0;
        }
        PlayerFactionData.setFaction(player, id);
        player.sendSystemMessage(Component.literal("你已加入阵营：【" + PlayerFactionData.getDisplayName(id) + "】"));
        return 1;
    }

    /**
     * 查看当前玩家核心存档数据（统一持久化方案下的实时结果）。
     */
    private static int executeShowPlayerData(ServerPlayer player)
    {
        JjkzPlayerDataDisplay.sendToViewer(player, player);
        return 1;
    }

    /**
     * 作弊切换阵营：使用字面量子命令并列注册。输入 {@code /jjkz faction } 后按 Tab 可在候选间切换并回车执行，无需手打长串。
     */
    private static LiteralArgumentBuilder<CommandSourceStack> buildFactionCheatBranch()
    {
        LiteralArgumentBuilder<CommandSourceStack> faction = Commands.literal("faction")
                .requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS));
        faction.then(factionLiteral("sorcerer", PlayerFactionData.FACTION_SORCERER));
        faction.then(factionLiteral("咒术师", PlayerFactionData.FACTION_SORCERER));
        faction.then(factionLiteral("curse_user", PlayerFactionData.FACTION_CURSE_USER));
        faction.then(factionLiteral("诅咒师", PlayerFactionData.FACTION_CURSE_USER));
        faction.then(factionLiteral("curse_spirit", PlayerFactionData.FACTION_CURSE_SPIRIT));
        faction.then(factionLiteral("咒灵", PlayerFactionData.FACTION_CURSE_SPIRIT));
        faction.then(factionLiteral("civilian", PlayerFactionData.FACTION_CIVILIAN));
        faction.then(factionLiteral("平民", PlayerFactionData.FACTION_CIVILIAN));
        return faction;
    }

    private static LiteralArgumentBuilder<CommandSourceStack> factionLiteral(String literal, String factionId)
    {
        return Commands.literal(literal).executes(ctx -> executeFactionCheatById(ctx.getSource().getPlayerOrException(), factionId));
    }

    private static int executeFactionCheatById(ServerPlayer player, String factionId)
    {
        PlayerFactionData.setFaction(player, factionId);
        player.sendSystemMessage(Component.literal("阵营已设为：【" + PlayerFactionData.getDisplayName(factionId) + "】"));
        return 1;
    }

    /**
     * {@code /jjkz curseLevel set <四级|三级|…>}，按 Tab 选择等级，将咒术经验设为对应阈值。
     */
    private static LiteralArgumentBuilder<CommandSourceStack> buildCurseLevelCheatBranch()
    {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("curseLevel")
                .requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS));
        LiteralArgumentBuilder<CommandSourceStack> set = Commands.literal("set");
        set.then(curseLevelLiteral("四级", "四级"));
        set.then(curseLevelLiteral("三级", "三级"));
        set.then(curseLevelLiteral("二级", "二级"));
        set.then(curseLevelLiteral("一级", "一级"));
        set.then(curseLevelLiteral("特级", "特级"));
        set.then(curseLevelLiteral("grade_4", "四级"));
        set.then(curseLevelLiteral("grade_3", "三级"));
        set.then(curseLevelLiteral("grade_2", "二级"));
        set.then(curseLevelLiteral("grade_1", "一级"));
        set.then(curseLevelLiteral("special", "特级"));
        root.then(set);
        return root;
    }

    private static LiteralArgumentBuilder<CommandSourceStack> curseLevelLiteral(String literal, String rankKey)
    {
        return Commands.literal(literal).executes(ctx -> executeCurseLevelSet(ctx.getSource().getPlayerOrException(), rankKey));
    }

    private static int executeCurseLevelSet(ServerPlayer player, String rankKey)
    {
        int xp = PlayerCursedGradeData.experienceForCheatRank(rankKey);
        if (xp < 0)
        {
            player.sendSystemMessage(Component.literal("未知的等级。"));
            return 0;
        }
        PlayerCursedGradeData.setExperience(player, xp);
        int rank = PlayerCursedGradeData.getRank(player);
        player.sendSystemMessage(Component.literal(
                "咒术经验已设为 " + xp + "（咒术等级：" + PlayerCursedGradeData.getRankDisplayName(rank) + "）"));
        return 1;
    }

    /** 作弊习得反转术式：{@code /jjkz reverseTechnique grant} */
    private static LiteralArgumentBuilder<CommandSourceStack> buildReverseTechniqueCheatBranch()
    {
        return Commands.literal("reverseTechnique")
                .requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("grant")
                        .executes(ctx ->
                        {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            PlayerReverseTechniqueData.setLearned(player, true);
                            player.sendSystemMessage(Component.literal("已习得反转术式（当前为作弊获取）。"));
                            return 1;
                        }));
    }

    /**
     * 作弊解锁/撤销修复术式熔断：{@code /jjkz meltdownRepair grant|revoke}
     * <p>
     * 习得前置：须已学反转术式（与领域展开须先学结界术同型）。
     */
    private static LiteralArgumentBuilder<CommandSourceStack> buildMeltdownRepairCheatBranch()
    {
        return Commands.literal("meltdownRepair")
                .requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("grant")
                        .executes(ctx ->
                        {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            if (PlayerMeltdownRepairData.hasLearned(player))
                            {
                                player.sendSystemMessage(Component.literal("你已经习得了修复术式熔断。"));
                                return 0;
                            }
                            if (!MeltdownRepairLearnLogic.meetsPrerequisite(player))
                            {
                                player.sendSystemMessage(Component.literal("需要先习得反转术式才能解锁修复术式熔断。"));
                                return 0;
                            }
                            PlayerMeltdownRepairData.setLearned(player, true);
                            player.sendSystemMessage(Component.literal("已习得修复术式熔断（当前为作弊获取）。"));
                            return 1;
                        }))
                .then(Commands.literal("revoke")
                        .executes(ctx ->
                        {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            if (!PlayerMeltdownRepairData.hasLearned(player))
                            {
                                player.sendSystemMessage(Component.literal("你尚未习得修复术式熔断。"));
                                return 0;
                            }
                            PlayerMeltdownRepairData.setLearned(player, false);
                            player.sendSystemMessage(Component.literal("已撤销修复术式熔断。"));
                            return 1;
                        }));
    }

    /** 作弊设置咒力总量：{@code /jjkz cursedEnergy set <值>} */
    private static LiteralArgumentBuilder<CommandSourceStack> buildCursedEnergyCheatBranch()
    {
        return Commands.literal("cursedEnergy")
                .requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("set")
                        .then(Commands.argument("value", IntegerArgumentType.integer(0))
                                .executes(ctx ->
                                {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    int value = IntegerArgumentType.getInteger(ctx, "value");
                                    PlayerCursedEnergyData.setBaseMaxForCheat(player, value);
                                    CursedGradeBonuses.applyAll(player);
                                    if (PlayerCursedEnergyData.isHeavenlyRestricted(player))
                                    {
                                        HeavenlyRestrictionAdvancements.grant(player);
                                    }
                                    player.sendSystemMessage(Component.literal(
                                            "咒力总量已设为 " + PlayerCursedEnergyData.getMax(player)
                                                    + "（剩余：" + PlayerCursedEnergyData.getCurrent(player) + "）"));
                                    return 1;
                                })))
                // 作弊填满咒力：将当前咒力设置为总量，并同步到客户端更新GUI
                .then(Commands.literal("fill")
                        .executes(ctx ->
                        {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            int max = PlayerCursedEnergyData.getMax(player);
                            PlayerCursedEnergyData.setCurrent(player, max);
                            ModNetwork.sendCursedEnergySync(player, max, max);
                            player.sendSystemMessage(Component.literal("咒力已填满至 " + max + "。"));
                            return 1;
                        }));
    }

    /** 作弊增加术式情报：{@code /jjkz techniqueIntel add <数量>} */
    private static LiteralArgumentBuilder<CommandSourceStack> buildTechniqueIntelCheatBranch()
    {
        return Commands.literal("techniqueIntel")
                .requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("add")
                        .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                .executes(ctx ->
                                {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    int amount = IntegerArgumentType.getInteger(ctx, "amount");
                                    return executeTechniqueIntelAdd(player, amount);
                                })))
                .then(Commands.literal("get")
                        .executes(ctx -> executeTechniqueIntelGet(ctx.getSource().getPlayerOrException())));
    }

    private static int executeTechniqueIntelAdd(ServerPlayer player, int amount)
    {
        String techniqueId = PlayerTechniqueData.getInnateTechnique(player);
        if (techniqueId.isBlank() || PlayerTechniqueData.TECHNIQUE_NONE.equals(techniqueId))
        {
            player.sendSystemMessage(Component.literal("当前未选择生得术式，无法增加术式情报。"));
            return 0;
        }
        PlayerTechniqueIntelData.addIntel(player, techniqueId, amount);
        int total = PlayerTechniqueIntelData.getIntel(player, techniqueId);
        player.sendSystemMessage(Component.literal(
                "已为术式【" + techniqueId + "】增加 " + amount + " 点术式情报（当前：" + total + "）。"));
        return 1;
    }

    private static int executeTechniqueIntelGet(ServerPlayer player)
    {
        String techniqueId = PlayerTechniqueData.getInnateTechnique(player);
        if (techniqueId.isBlank() || PlayerTechniqueData.TECHNIQUE_NONE.equals(techniqueId))
        {
            player.sendSystemMessage(Component.literal("当前未选择生得术式。"));
            return 0;
        }
        int total = PlayerTechniqueIntelData.getIntel(player, techniqueId);
        player.sendSystemMessage(Component.literal(
                "术式【" + techniqueId + "】术式情报：" + total));
        return 1;
    }

    /**
     * 战斗强度面板：{@code /jjkz stats} 查看本人面板与构成明细；
     * {@code /jjkz stats add <physical|technique|体术|术式> <数值>} 增减修炼加成
     * （数值可为负，需要 GM 权限）。
     */
    private static LiteralArgumentBuilder<CommandSourceStack> buildSpCheatBranch()
    {
        return Commands.literal("sp")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("add")
                        .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                .executes(ctx ->
                                {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    int amount = IntegerArgumentType.getInteger(ctx, "amount");
                                    PlayerSpData.add(player, amount);
                                    player.sendSystemMessage(Component.literal(
                                            "SP +" + amount + "（可用 " + PlayerSpData.getAvailable(player)
                                                    + " / 总量 " + PlayerSpData.getTotal(player) + "）"));
                                    return 1;
                                })))
                .then(Commands.literal("get")
                        .executes(ctx ->
                        {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            player.sendSystemMessage(Component.literal(
                                    "SP 可用=" + PlayerSpData.getAvailable(player)
                                            + " 总量=" + PlayerSpData.getTotal(player)
                                            + " 解放已花=" + PlayerSpData.getSpentLiberation(player)
                                            + " 节点占用=" + PlayerSpData.getAllocatedToNodes(player)));
                            return 1;
                        }));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> buildSpNodeCheatBranch()
    {
        return Commands.literal("spNode")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("unlock")
                        .then(Commands.argument("instanceId", StringArgumentType.string())
                                .executes(ctx ->
                                {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    String instanceId = StringArgumentType.getString(ctx, "instanceId");
                                    var result = PlayerSpNodeData.tryUnlock(player, instanceId);
                                    player.sendSystemMessage(Component.literal("SP节点解锁结果: " + result
                                            + "（质量 " + PlayerQualityPoolData.occupied(player)
                                            + "/" + PlayerQualityPoolData.cap(player)
                                            + "，宽限 +" + PlayerQualityPoolData.allowance(player) + "）"));
                                    return result == PlayerSpNodeData.SpNodeUnlockResult.SUCCESS ? 1 : 0;
                                })))
                .then(Commands.literal("reset")
                        .executes(ctx ->
                        {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            PlayerSpNodeData.resetAll(player);
                            player.sendSystemMessage(Component.literal(
                                    "已重置 SP 节点，SP 已退回（可用 " + PlayerSpData.getAvailable(player) + "）"));
                            return 1;
                        }))
                .then(Commands.literal("refresh")
                        .executes(ctx ->
                        {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            var result = com.darkness.jujutsukaisenzone.player.SpNodeShelfLogic
                                    .tryPaidRefresh(player);
                            player.sendSystemMessage(Component.literal("SP货架刷新: " + result));
                            return result == com.darkness.jujutsukaisenzone.player
                                    .SpNodeShelfLogic.RefreshResult.SUCCESS ? 1 : 0;
                        }))
                .then(Commands.literal("resync")
                        .executes(ctx ->
                        {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            com.darkness.jujutsukaisenzone.player.SpNodeShelfLogic
                                    .syncWithExperience(player);
                            player.sendSystemMessage(Component.literal(
                                    "已按经验同步货架：tier=" + PlayerSpNodeData.getTier(player)
                                            + " 质量=" + PlayerQualityPoolData.cap(player)
                                            + " 宽限=" + PlayerQualityPoolData.allowance(player)
                                            + " 在售=" + PlayerSpNodeData.getOffers(player).size()
                                            + " 已点=" + PlayerSpNodeData.getLit(player).size()));
                            return 1;
                        }))
                .then(Commands.literal("list")
                        .executes(ctx ->
                        {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            player.sendSystemMessage(Component.literal(
                                    "tier=" + PlayerSpNodeData.getTier(player)
                                            + " 质量 " + PlayerQualityPoolData.occupied(player)
                                            + "/" + PlayerQualityPoolData.cap(player)
                                            + "（宽限 +" + PlayerQualityPoolData.allowance(player) + "）"));
                            player.sendSystemMessage(Component.literal("已点亮:"));
                            for (var node : PlayerSpNodeData.getLit(player))
                            {
                                player.sendSystemMessage(Component.literal(
                                        "  " + node.instanceId().substring(0, 8)
                                                + " " + node.family().id()
                                                + " cost=" + node.spCost()
                                                + " phys+" + node.physicalBonus()
                                                + " tech+" + node.techniqueBonus()
                                                + " attr+" + node.attributeBonus()));
                            }
                            player.sendSystemMessage(Component.literal("在售:"));
                            for (var node : PlayerSpNodeData.getOffers(player))
                            {
                                player.sendSystemMessage(Component.literal(
                                        "  " + node.instanceId()
                                                + " " + node.family().id()
                                                + " cost=" + node.spCost()
                                                + " phys+" + node.physicalBonus()
                                                + " tech+" + node.techniqueBonus()
                                                + " attr+" + node.attributeBonus()));
                            }
                            return 1;
                        }));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> buildSeCheatBranch()
    {
        return Commands.literal("se")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("equip")
                        .then(Commands.argument("seId", StringArgumentType.word())
                                .executes(ctx ->
                                {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    String seId = StringArgumentType.getString(ctx, "seId");
                                    var result = PlayerSeLoadoutData.tryEquip(player, seId);
                                    player.sendSystemMessage(Component.literal(
                                            "SE 装载结果: " + result
                                                    + " 已装载=" + PlayerSeLoadoutData.getEquippedIds(player)
                                                    + " 质量=" + PlayerQualityPoolData.occupied(player)
                                                    + "/" + PlayerQualityPoolData.cap(player)
                                                    + "（宽限 +" + PlayerQualityPoolData.allowance(player) + "）"));
                                    return result == PlayerSeLoadoutData.SeEquipResult.SUCCESS ? 1 : 0;
                                })))
                .then(Commands.literal("unequip")
                        .then(Commands.argument("seId", StringArgumentType.word())
                                .executes(ctx ->
                                {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    String seId = StringArgumentType.getString(ctx, "seId");
                                    var result = PlayerSeLoadoutData.tryUnequip(player, seId);
                                    player.sendSystemMessage(Component.literal(
                                            "SE 卸下结果: " + result
                                                    + " 已装载=" + PlayerSeLoadoutData.getEquippedIds(player)));
                                    return result == PlayerSeLoadoutData.SeEquipResult.SUCCESS ? 1 : 0;
                                })))
                .then(Commands.literal("list")
                        .executes(ctx ->
                        {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            player.sendSystemMessage(Component.literal(
                                    "当前已装载 SE: " + PlayerSeLoadoutData.getEquippedIds(player)));
                            player.sendSystemMessage(Component.literal("质量占用 "
                                    + PlayerQualityPoolData.occupied(player) + "/" + PlayerQualityPoolData.cap(player)
                                    + "（宽限 +" + PlayerQualityPoolData.allowance(player)
                                    + "，超载 " + PlayerQualityPoolData.overload(player) + "）"));
                            player.sendSystemMessage(Component.literal(
                                    "当前咒力特质: " + PlayerCursedEnergyTraitData.getTrait(player).id()));
                            for (var def : SeEffectRegistry.all())
                            {
                                player.sendSystemMessage(Component.literal(
                                        "  " + def.seId() + " item=" + def.item()
                                                + " trigger=" + def.trigger()
                                                + " quality=" + def.quality()
                                                + " scope=" + def.scope()));
                            }
                            for (var e : PlayerSeLoadoutData.getEquippedMap(player).entrySet())
                            {
                                var p = e.getValue().penalty;
                                player.sendSystemMessage(Component.literal(
                                        "  equipped " + e.getKey()
                                                + " trait=" + e.getValue().traitId
                                                + " penalty=" + p.stat().id()
                                                + " -" + p.amount()));
                            }
                            return 1;
                        }));
    }

    /**
     * 咒力特质调试分支。
     * <p>
     * 特质是 SE 装载门卡（咒物实例随机特质须与玩家吻合）。
     * 产生规则（觉醒随机 / 可洗）留给后续设计，这里只提供「直接指定」便于测装载链。
     */
    private static LiteralArgumentBuilder<CommandSourceStack> buildTraitCheatBranch()
    {
        return Commands.literal("trait")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("set")
                        .then(Commands.argument("trait", StringArgumentType.word())
                                .suggests((ctx, builder) ->
                                {
                                    for (CursedEnergyTrait trait : CursedEnergyTrait.values())
                                    {
                                        builder.suggest(trait.id());
                                    }
                                    return builder.buildFuture();
                                })
                                .executes(ctx ->
                                {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    String raw = StringArgumentType.getString(ctx, "trait");
                                    CursedEnergyTrait trait = CursedEnergyTrait.byId(raw);
                                    PlayerCursedEnergyTraitData.setTrait(player, trait);
                                    player.sendSystemMessage(Component.literal(
                                            "咒力特质设为 " + trait.id() + "（输入 " + raw + "）"));
                                    return 1;
                                })))
                .then(Commands.literal("get")
                        .executes(ctx ->
                        {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            player.sendSystemMessage(Component.literal(
                                    "当前咒力特质: " + PlayerCursedEnergyTraitData.getTrait(player).id()));
                            return 1;
                        }));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> buildCombatPowerBranch()
    {
        LiteralArgumentBuilder<CommandSourceStack> add = Commands.literal("add")
                .requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS));
        add.then(powerAddLiteral("physical", true));
        add.then(powerAddLiteral("体术", true));
        add.then(powerAddLiteral("technique", false));
        add.then(powerAddLiteral("术式", false));

        return Commands.literal("stats")
                .executes(ctx -> executeCombatPowerShow(ctx.getSource().getPlayerOrException()))
                .then(add);
    }

    private static LiteralArgumentBuilder<CommandSourceStack> powerAddLiteral(String literal, boolean physical)
    {
        return Commands.literal(literal)
                .then(Commands.argument("amount", IntegerArgumentType.integer(-10000, 10000))
                        .executes(ctx -> executeCombatPowerAdd(
                                ctx.getSource().getPlayerOrException(),
                                physical ? IntegerArgumentType.getInteger(ctx, "amount") : 0,
                                physical ? 0 : IntegerArgumentType.getInteger(ctx, "amount"))));
    }

    private static int executeCombatPowerShow(ServerPlayer player)
    {
        CombatPowerStats stats = PlayerCombatPowerData.resolve(player);
        player.sendSystemMessage(Component.literal("体术强度: " + stats.physicalPower()
                + "　术式强度: " + stats.techniquePower()));
        player.sendSystemMessage(Component.literal("[体术强度构成]"));
        for (CombatPowerStats.Contribution contribution : stats.physicalBreakdown())
        {
            player.sendSystemMessage(Component.literal("  · " + contribution.label() + ": " + contribution.amount()));
        }
        player.sendSystemMessage(Component.literal("[术式强度构成]"));
        for (CombatPowerStats.Contribution contribution : stats.techniqueBreakdown())
        {
            player.sendSystemMessage(Component.literal("  · " + contribution.label() + ": " + contribution.amount()));
        }
        return 1;
    }

    private static int executeCombatPowerAdd(ServerPlayer player, int physicalDelta, int techniqueDelta)
    {
        PlayerCombatPowerData.addBonus(player, physicalDelta, techniqueDelta);
        CombatPowerStats stats = PlayerCombatPowerData.resolve(player);
        player.sendSystemMessage(Component.literal("已调整强度 → 体术 " + stats.physicalPower()
                + "　术式 " + stats.techniquePower()));
        return 1;
    }

    /** 调试领域天空盒：{@code /jjkz domainSphere place <天空盒路径>} / {@code place <半径> <天空盒路径>} / {@code close} */
    private static LiteralArgumentBuilder<CommandSourceStack> buildDomainSphereDebugBranch()
    {
        LiteralArgumentBuilder<CommandSourceStack> place = Commands.literal("place")
                .then(Commands.argument("radius", IntegerArgumentType.integer(4, 64))
                        .then(Commands.argument("skybox", StringArgumentType.greedyString())
                                .executes(ctx ->
                                {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    int radius = IntegerArgumentType.getInteger(ctx, "radius");
                                    String skybox = StringArgumentType.getString(ctx, "skybox");
                                    return DomainSphereDebugLogic.place(player, skybox, radius) ? 1 : 0;
                                })))
                .then(Commands.argument("skybox", StringArgumentType.greedyString())
                        .executes(ctx ->
                        {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            String skybox = StringArgumentType.getString(ctx, "skybox");
                            return DomainSphereDebugLogic.place(player, skybox, DomainSphereDebugLogic.defaultRadius()) ? 1 : 0;
                        }));

        return Commands.literal("domainSphere")
                .requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(place)
                .then(Commands.literal("close")
                        .executes(ctx ->
                        {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            return DomainSphereDebugLogic.close(player) ? 1 : 0;
                        }));
    }

    /** 召唤伏魔神龛：{@code /jjkz summonDemonSubduingShrine} */
    private static LiteralArgumentBuilder<CommandSourceStack> buildDemonSubduingShrineBranch()
    {
        return Commands.literal("summonDemonSubduingShrine")
                .requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .executes(ctx ->
                {
                    CommandSourceStack source = ctx.getSource();
                    ServerLevel level = source.getLevel();
                    Vec3 pos = source.getPosition();

                    DemonSubduingShrineEntity entity = ModEntities.DEMON_SUBDUING_SHRINE.get().create(level);
                    if (entity == null)
                    {
                        source.sendFailure(Component.literal("召唤失败：伏魔神龛实体创建失败。"));
                        return 0;
                    }

                    entity.moveTo(pos.x, pos.y, pos.z, source.getRotation().y, 0.0F);
                    level.addFreshEntity(entity);
                    source.sendSuccess(() -> Component.literal("已召唤伏魔神龛。"), true);
                    return 1;
                });
    }

    /** 召唤特殊效果实体：{@code /jjkz summonSpecialEffect} */
    private static LiteralArgumentBuilder<CommandSourceStack> buildSpecialEffectBranch()
    {
        return Commands.literal("summonSpecialEffect")
                .requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .executes(ctx ->
                {
                    CommandSourceStack source = ctx.getSource();
                    ServerLevel level = source.getLevel();
                    Vec3 pos = source.getPosition();

                    SpecialEffectEntity entity = ModEntities.SPECIAL_EFFECT_ENTITY.get().create(level);
                    if (entity == null)
                    {
                        source.sendFailure(Component.literal("召唤失败：实体创建失败。"));
                        return 0;
                    }

                    entity.moveTo(pos.x, pos.y, pos.z, source.getRotation().y, 0.0F);
                    level.addFreshEntity(entity);
                        source.sendSuccess(() -> Component.literal("已召唤 SpecialEffectEntity。"), true);
                    return 1;
                });
    }

    /** 生成阵营楔：{@code /jjkz wedge <咒术师|诅咒师|咒灵>} */
    private static LiteralArgumentBuilder<CommandSourceStack> buildWedgeSpawnBranch()
    {
        LiteralArgumentBuilder<CommandSourceStack> wedge = Commands.literal("wedge")
                .requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS));
        wedge.then(wedgeFactionLiteral("sorcerer", PlayerFactionData.FACTION_SORCERER));
        wedge.then(wedgeFactionLiteral("咒术师", PlayerFactionData.FACTION_SORCERER));
        wedge.then(wedgeFactionLiteral("curse_user", PlayerFactionData.FACTION_CURSE_USER));
        wedge.then(wedgeFactionLiteral("诅咒师", PlayerFactionData.FACTION_CURSE_USER));
        wedge.then(wedgeFactionLiteral("curse_spirit", PlayerFactionData.FACTION_CURSE_SPIRIT));
        wedge.then(wedgeFactionLiteral("咒灵", PlayerFactionData.FACTION_CURSE_SPIRIT));
        return wedge;
    }

    private static LiteralArgumentBuilder<CommandSourceStack> wedgeFactionLiteral(String literal, String factionId)
    {
        return Commands.literal(literal).executes(ctx -> executeSpawnWedge(ctx.getSource(), factionId));
    }

    private static int executeSpawnWedge(CommandSourceStack source, String factionId)
    {
        ServerLevel level = source.getLevel();
        WedgeEntity existing = WedgeEntity.findInFaction(level, factionId);
        if (existing != null)
        {
            existing.kill();
        }

        WedgeEntity wedge = ModEntities.WEDGE.get().create(level);
        if (wedge == null)
        {
            source.sendFailure(Component.literal("楔生成失败：实体创建失败。"));
            return 0;
        }

        wedge.setFactionId(factionId);
        Vec3 pos = source.getPosition();
        wedge.moveTo(pos.x, pos.y, pos.z, source.getRotation().y, 0.0F);
        wedge.setHealth(wedge.getMaxHealth());
        if (!level.addFreshEntity(wedge))
        {
            source.sendFailure(Component.literal("楔生成失败：无法加入世界（该阵营在本维度可能已有楔）。"));
            return 0;
        }

        String factionName = PlayerFactionData.getDisplayName(factionId);
        source.sendSuccess(() -> Component.literal("已生成【" + factionName + "】之楔。"), true);
        return 1;
    }

    /** 咒灵刷新点：{@code /jjkz curseSpiritSpawn set <三级|二级|一级|特级>} */
    private static LiteralArgumentBuilder<CommandSourceStack> buildCurseSpiritSpawnBranch()
    {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("curseSpiritSpawn")
                .requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS));
        LiteralArgumentBuilder<CommandSourceStack> set = Commands.literal("set");
        set.then(curseSpiritSpawnGradeLiteral("三级", CurseSpiritSpawnGrade.GRADE_3));
        set.then(curseSpiritSpawnGradeLiteral("二级", CurseSpiritSpawnGrade.GRADE_2));
        set.then(curseSpiritSpawnGradeLiteral("一级", CurseSpiritSpawnGrade.GRADE_1));
        set.then(curseSpiritSpawnGradeLiteral("特级", CurseSpiritSpawnGrade.SPECIAL));
        set.then(curseSpiritSpawnGradeLiteral("grade_3", CurseSpiritSpawnGrade.GRADE_3));
        set.then(curseSpiritSpawnGradeLiteral("grade_2", CurseSpiritSpawnGrade.GRADE_2));
        set.then(curseSpiritSpawnGradeLiteral("grade_1", CurseSpiritSpawnGrade.GRADE_1));
        set.then(curseSpiritSpawnGradeLiteral("special", CurseSpiritSpawnGrade.SPECIAL));
        root.then(set);
        return root;
    }

    private static LiteralArgumentBuilder<CommandSourceStack> curseSpiritSpawnGradeLiteral(
            String literal,
            CurseSpiritSpawnGrade grade)
    {
        return Commands.literal(literal).executes(ctx -> executeSetCurseSpiritSpawn(ctx.getSource(), grade));
    }

    private static int executeSetCurseSpiritSpawn(CommandSourceStack source, CurseSpiritSpawnGrade grade)
    {
        if (!(source.getEntity() instanceof ServerPlayer player))
        {
            source.sendFailure(Component.literal("咒灵刷新点须由玩家设置。"));
            return 0;
        }

        TengenWedgeBarrierLogic.TengenWedgeBarrier barrier = TengenWedgeBarrierLogic.findOwnedBarrierInside(player);
        if (barrier == null)
        {
            source.sendFailure(Component.literal("须站在已构建完成的天元之楔结界内（setup 执行者）。"));
            return 0;
        }

        Vec3 pos = source.getPosition();
        if (!TengenWedgeCurseSpiritSpawns.addSpawnPoint(barrier, pos, grade))
        {
            source.sendFailure(Component.literal("刷新点位置不在结界内。"));
            return 0;
        }

        int total = TengenWedgeCurseSpiritSpawns.countForBarrier(barrier);
        source.sendSuccess(
                () -> Component.literal("已设置【" + grade.getDisplayName() + "】咒灵刷新点（当前共 "
                        + total + " 个，至少需 " + TengenWedgeCurseSpiritSpawns.MIN_REQUIRED + " 个）。"),
                true);
        return 1;
    }

    /** 天元之楔结界部署：{@code /jjkz tengen_wedge setup|close} */
    private static LiteralArgumentBuilder<CommandSourceStack> buildTengenWedgeBranch()
    {
        return Commands.literal("tengen_wedge")
                .requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("setup")
                        .executes(ctx ->
                        {
                            CommandSourceStack source = ctx.getSource();
                            if (!(source.getEntity() instanceof ServerPlayer player))
                            {
                                source.sendFailure(Component.literal("天元之楔结界须由玩家执行 setup。"));
                                return 0;
                            }
                            ServerLevel level = source.getLevel();
                            BlockPos center = BlockPos.containing(source.getPosition());

                            if (!TengenWedgeBarrierLogic.setup(level, center, player))
                            {
                                source.sendFailure(Component.literal("天元之楔结界部署失败。"));
                                return 0;
                            }

                            source.sendSuccess(
                                    () -> Component.literal("已开始构建天元之楔结界（半径 "
                                            + TengenWedgeBarrierLogic.BARRIER_RADIUS + "，中心 "
                                            + center.getX() + ", " + center.getY() + ", " + center.getZ() + "）"),
                                    true);
                            return 1;
                        }))
                .then(Commands.literal("close")
                        .executes(ctx ->
                        {
                            CommandSourceStack source = ctx.getSource();
                            if (!(source.getEntity() instanceof ServerPlayer player))
                            {
                                source.sendFailure(Component.literal("天元之楔结界须由 setup 执行者关闭。"));
                                return 0;
                            }
                            if (!TengenWedgeBarrierLogic.close(player))
                            {
                                source.sendFailure(Component.literal("未找到你在当前维度部署的天元之楔结界。"));
                                return 0;
                            }
                            source.sendSuccess(() -> Component.literal("已关闭天元之楔结界，结界壁已清除并复原方块。"), true);
                            return 1;
                        }))
                .then(Commands.literal("start")
                        .executes(ctx ->
                        {
                            CommandSourceStack source = ctx.getSource();
                            if (!(source.getEntity() instanceof ServerPlayer player))
                            {
                                source.sendFailure(Component.literal("天元之楔须由 setup 执行者开始。"));
                                return 0;
                            }
                            TengenWedgeStartResult result = TengenWedgeBarrierLogic.start(player);
                            if (!result.isSuccess())
                            {
                                Component message = result.failureMessage();
                                source.sendFailure(message != null ? message : Component.literal("无法开始天元之楔。"));
                                return 0;
                            }
                            source.sendSuccess(() -> Component.literal("天元之楔已开始，状态：进行中。"), true);
                            return 1;
                        }));
    }

    /**
     * <p>
     * 业务意图：通过命令快速验证“方块在白层下、生物在白层上”的视觉关系。
     */
    private static LiteralArgumentBuilder<CommandSourceStack> buildWhiteOverlayBranch()
    {
        return Commands.literal("whiteOverlay")
                .requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("on")
                        .executes(ctx -> executeWhiteOverlaySet(ctx.getSource().getPlayerOrException(), true, 0.4F)))
                .then(Commands.literal("off")
                        .executes(ctx -> executeWhiteOverlaySet(ctx.getSource().getPlayerOrException(), false, 0.0F)))
                .then(Commands.literal("alpha")
                        .then(Commands.argument("value", IntegerArgumentType.integer(0, 100))
                                .executes(ctx ->
                                {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    int value = IntegerArgumentType.getInteger(ctx, "value");
                                    return executeWhiteOverlaySet(player, true, value / 100.0F);
                                })));
    }

    private static int executeWhiteOverlaySet(ServerPlayer player, boolean enabled, float alpha)
    {
        ModNetwork.sendWhiteOverlaySync(player, enabled, alpha);
        if (!enabled)
        {
            player.sendSystemMessage(Component.literal("白色滤镜已关闭。"));
            return 1;
        }
        int alphaPercent = Math.round(alpha * 100.0F);
        player.sendSystemMessage(Component.literal("白色滤镜已开启，透明度：" + alphaPercent + "%。"));
        return 1;
    }

    /**
     * 调试领域白幕 shader：{@code /jjkz whiteCurtain play [ticks]}。
     */
    private static LiteralArgumentBuilder<CommandSourceStack> buildWhiteCurtainBranch()
    {
        return Commands.literal("whiteCurtain")
                .requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("play")
                        .executes(ctx -> executeWhiteCurtainPlay(ctx.getSource().getPlayerOrException(), 115))
                        .then(Commands.argument("ticks", IntegerArgumentType.integer(1, 400))
                                .executes(ctx -> executeWhiteCurtainPlay(
                                        ctx.getSource().getPlayerOrException(),
                                        IntegerArgumentType.getInteger(ctx, "ticks")))));
    }

    private static int executeWhiteCurtainPlay(ServerPlayer player, int durationTicks)
    {
        ModNetwork.sendDomainWhiteCurtainSync(player, durationTicks);
        player.sendSystemMessage(Component.literal("已播放领域白幕演出（" + durationTicks + " tick）。"));
        return 1;
    }

    /** 作弊设置降灵术等级：{@code /jjkz seanceTechnique set <等级>} */
    private static LiteralArgumentBuilder<CommandSourceStack> buildSeanceTechniqueCheatBranch()
    {
        return Commands.literal("seanceTechnique")
                .requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("set")
                        .then(seanceTechniqueLevelLiteral("unlearned", "未习得", SeanceTechniqueLevel.UNLEARNED))
                        .then(seanceTechniqueLevelLiteral("beginner", "初习", SeanceTechniqueLevel.BEGINNER))
                        .then(seanceTechniqueLevelLiteral("mastered", "掌握", SeanceTechniqueLevel.MASTERED))
                        .then(seanceTechniqueLevelLiteral("complete", "完全掌握", SeanceTechniqueLevel.COMPLETE)))
                .then(Commands.literal("get")
                        .executes(ctx ->
                        {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            SeanceTechniqueLevel level = PlayerSeanceTechniqueData.getLevel(player);
                            int reductionPercent = (int) (level.getMaintainCostReduction() * 100);
                            player.sendSystemMessage(Component.literal("当前降灵术等级：" + getSeanceLevelDisplayName(level) + "（维持消耗减免：" + reductionPercent + "%）"));
                            return 1;
                        }));
    }

    /** 作弊设置强化术等级：{@code /jjkz strengthenTechnique set <等级>} */
    private static LiteralArgumentBuilder<CommandSourceStack> buildStrengthenTechniqueCheatBranch()
    {
        return Commands.literal("strengthenTechnique")
                .requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("set")
                        .then(strengthenTechniqueLevelLiteral("unlearned", "未习得", StrengthenTechniqueLevel.UNLEARNED))
                        .then(strengthenTechniqueLevelLiteral("beginner", "初习", StrengthenTechniqueLevel.BEGINNER))
                        .then(strengthenTechniqueLevelLiteral("mastered", "掌握", StrengthenTechniqueLevel.MASTERED))
                        .then(strengthenTechniqueLevelLiteral("complete", "完全掌握", StrengthenTechniqueLevel.COMPLETE)))
                .then(Commands.literal("get")
                        .executes(ctx ->
                        {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            StrengthenTechniqueLevel level = PlayerStrengthenTechniqueData.getLevel(player);
                            player.sendSystemMessage(Component.literal("当前强化术等级：" + getStrengthenLevelDisplayName(level)));
                            return 1;
                        }));
    }

    /** 作弊设置结界术等级：{@code /jjkz barrierTechnique set <等级>} */
    private static LiteralArgumentBuilder<CommandSourceStack> buildBarrierTechniqueCheatBranch()
    {
        return Commands.literal("barrierTechnique")
                .requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("set")
                        .then(barrierTechniqueLevelLiteral("unlearned", "未习得", BarrierTechniqueLevel.UNLEARNED))
                        .then(barrierTechniqueLevelLiteral("beginner", "初习", BarrierTechniqueLevel.BEGINNER))
                        .then(barrierTechniqueLevelLiteral("mastered", "掌握", BarrierTechniqueLevel.MASTERED))
                        .then(barrierTechniqueLevelLiteral("complete", "完全掌握", BarrierTechniqueLevel.COMPLETE)))
                .then(Commands.literal("get")
                        .executes(ctx ->
                        {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            BarrierTechniqueLevel level = PlayerBarrierTechniqueData.getLevel(player);
                            int reductionPercent = (int) (level.getMaintainCostReduction() * 100);
                            player.sendSystemMessage(Component.literal("当前结界术等级：" + getBarrierLevelDisplayName(level) + "（维持消耗减免：" + reductionPercent + "%）"));
                            return 1;
                        }));
    }

    /**
     * 作弊解锁/撤销领域展开：{@code /jjkz domainExpansion grant|revoke}
     * <p>
     * 设置领域名称：{@code /jjkz domainExpansion setName <名称>}
     */
    private static LiteralArgumentBuilder<CommandSourceStack> buildDomainExpansionCheatBranch()
    {
        return Commands.literal("domainExpansion")
                .requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("grant")
                        .executes(ctx ->
                        {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            DomainExpansionServerLogic logic = DomainExpansionServerLogic.getInstance();
                            if (logic.hasLearnedDomainExpansion(player))
                            {
                                player.sendSystemMessage(Component.literal("你已经习得了领域展开。"));
                                return 0;
                            }
                            // 前置条件：需要先习得结界术
                            if (!PlayerBarrierTechniqueData.hasLearned(player))
                            {
                                player.sendSystemMessage(Component.literal("需要先习得结界术才能解锁领域展开。"));
                                return 0;
                            }
                            logic.setLearnedDomainExpansion(player, true);
                            player.sendSystemMessage(Component.literal("已习得领域展开（当前为作弊获取）。"));
                            return 1;
                        }))
                .then(Commands.literal("revoke")
                        .executes(ctx ->
                        {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            DomainExpansionServerLogic logic = DomainExpansionServerLogic.getInstance();
                            if (!logic.hasLearnedDomainExpansion(player))
                            {
                                player.sendSystemMessage(Component.literal("你尚未习得领域展开。"));
                                return 0;
                            }
                            logic.setLearnedDomainExpansion(player, false);
                            player.sendSystemMessage(Component.literal("已撤销领域展开。"));
                            return 1;
                        }))
                .then(Commands.literal("setName")
                        .then(Commands.argument("name", StringArgumentType.greedyString())
                                .executes(ctx ->
                                {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    String name = StringArgumentType.getString(ctx, "name");
                                    DomainExpansionServerLogic logic = DomainExpansionServerLogic.getInstance();
                                    logic.setDomainName(name);
                                    player.sendSystemMessage(Component.literal("领域名称已设为：「" + name + "」"));
                                    return 1;
                                })))
                .then(Commands.literal("setCost")
                        .then(Commands.argument("value", IntegerArgumentType.integer(0))
                                .executes(ctx ->
                                {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    int cost = IntegerArgumentType.getInteger(ctx, "value");
                                    DomainExpansionServerLogic logic = DomainExpansionServerLogic.getInstance();
                                    logic.setDomainExpansionCost(cost);
                                    player.sendSystemMessage(Component.literal("领域展开消耗咒力已设为：" + cost));
                                    return 1;
                                })))
                .then(Commands.literal("setMaintainCost")
                        .then(Commands.argument("value", IntegerArgumentType.integer(0))
                                .executes(ctx ->
                                {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    int cost = IntegerArgumentType.getInteger(ctx, "value");
                                    DomainExpansionServerLogic logic = DomainExpansionServerLogic.getInstance();
                                    logic.setDomainMaintainCost(cost);
                                    player.sendSystemMessage(Component.literal("维持领域消耗咒力已设为：" + cost));
                                    return 1;
                                })))
                .then(Commands.literal("get")
                        .executes(ctx ->
                        {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            DomainExpansionServerLogic logic = DomainExpansionServerLogic.getInstance();
                            boolean learned = logic.hasLearnedDomainExpansion(player);
                            String name = logic.getDomainName();
                            int expandCost = logic.getDomainExpansionCost();
                            int maintainCost = logic.getDomainMaintainCost();
                            player.sendSystemMessage(Component.literal("========== 领域展开 =========="));
                            player.sendSystemMessage(Component.literal("习得状态: " + (learned ? "已习得" : "未习得")));
                            player.sendSystemMessage(Component.literal("领域名称: " + (name.isEmpty() ? "（未设置）" : "「" + name + "」")));
                            player.sendSystemMessage(Component.literal("展开消耗咒力: " + expandCost));
                            player.sendSystemMessage(Component.literal("维持消耗咒力: " + maintainCost + "/秒"));
                            return 1;
                        }));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> strengthenTechniqueLevelLiteral(
            String literal,
            String displayName,
            StrengthenTechniqueLevel level)
    {
        return Commands.literal(literal).executes(ctx -> executeStrengthenTechniqueSet(ctx.getSource().getPlayerOrException(), displayName, level));
    }

    private static int executeStrengthenTechniqueSet(ServerPlayer player, String displayName, StrengthenTechniqueLevel level)
    {
        PlayerStrengthenTechniqueData.setLevel(player, level);
        player.sendSystemMessage(Component.literal("强化术等级已设为：" + displayName));
        return 1;
    }

    private static String getStrengthenLevelDisplayName(StrengthenTechniqueLevel level)
    {
        return switch (level)
        {
            case UNLEARNED -> "未习得";
            case BEGINNER -> "初习";
            case MASTERED -> "掌握";
            case COMPLETE -> "完全掌握";
        };
    }

    private static LiteralArgumentBuilder<CommandSourceStack> seanceTechniqueLevelLiteral(String literal, String displayName, SeanceTechniqueLevel level)
    {
        return Commands.literal(literal).executes(ctx -> executeSeanceTechniqueSet(ctx.getSource().getPlayerOrException(), displayName, level));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> barrierTechniqueLevelLiteral(String literal, String displayName, BarrierTechniqueLevel level)
    {
        return Commands.literal(literal).executes(ctx -> executeBarrierTechniqueSet(ctx.getSource().getPlayerOrException(), displayName, level));
    }

    private static int executeSeanceTechniqueSet(ServerPlayer player, String displayName, SeanceTechniqueLevel level)
    {
        PlayerSeanceTechniqueData.setLevel(player, level);
        int reductionPercent = (int) (level.getMaintainCostReduction() * 100);
        player.sendSystemMessage(Component.literal("降灵术等级已设为：" + displayName + "（维持消耗减免：" + reductionPercent + "%）"));
        return 1;
    }

    private static int executeBarrierTechniqueSet(ServerPlayer player, String displayName, BarrierTechniqueLevel level)
    {
        PlayerBarrierTechniqueData.setLevel(player, level);
        int reductionPercent = (int) (level.getMaintainCostReduction() * 100);
        player.sendSystemMessage(Component.literal("结界术等级已设为：" + displayName + "（维持消耗减免：" + reductionPercent + "%）"));
        return 1;
    }

    private static String getBarrierLevelDisplayName(BarrierTechniqueLevel level)
    {
        return switch (level)
        {
            case UNLEARNED -> "未习得";
            case BEGINNER -> "初习";
            case MASTERED -> "掌握";
            case COMPLETE -> "完全掌握";
        };
    }

    private static String getSeanceLevelDisplayName(SeanceTechniqueLevel level)
    {
        return switch (level)
        {
            case UNLEARNED -> "未习得";
            case BEGINNER -> "初习";
            case MASTERED -> "掌握";
            case COMPLETE -> "完全掌握";
        };
    }

    /**
     * 模组文字教程：{@code /jjkz ask <分类> <主题>}，按 Tab 选择分类与主题后在聊天栏显示说明。
     */
    private static LiteralArgumentBuilder<CommandSourceStack> buildAskBranch()
    {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("ask")
                .executes(ctx ->
                {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    player.sendSystemMessage(Component.literal(
                            "【JJKZ 教程】输入分类名以查看主题，按 Tab 选择。可用分类：" + String.join("、", ASK_CATEGORIES.keySet())));
                    return 1;
                });
        for (Map.Entry<String, Map<String, String>> categoryEntry : ASK_CATEGORIES.entrySet())
        {
            root.then(buildAskCategoryBranch(categoryEntry.getKey(), categoryEntry.getValue()));
        }
        return root;
    }

    private static LiteralArgumentBuilder<CommandSourceStack> buildAskCategoryBranch(
            String category, Map<String, String> topics)
    {
        LiteralArgumentBuilder<CommandSourceStack> categoryNode = Commands.literal(category)
                .executes(ctx ->
                {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    player.sendSystemMessage(Component.literal(
                            "【" + category + "】按 Tab 选择主题：" + String.join("、", topics.keySet())));
                    return 1;
                });
        for (Map.Entry<String, String> topicEntry : topics.entrySet())
        {
            String topic = topicEntry.getKey();
            String answer = topicEntry.getValue();
            categoryNode.then(Commands.literal(topic).executes(ctx ->
            {
                ServerPlayer player = ctx.getSource().getPlayerOrException();
                return executeAsk(player, category, topic, answer);
            }));
        }
        return categoryNode;
    }

    private static int executeAsk(ServerPlayer player, String category, String topic, String answer)
    {
        player.sendSystemMessage(Component.literal("【" + category + " · " + topic + "】"));
        for (String line : answer.split("\n"))
        {
            if (!line.isBlank())
            {
                player.sendSystemMessage(Component.literal(line.strip()));
            }
        }
        return 1;
    }

    /**
     * 模组规则（全服生效）：{@code /jjkz modRules ...}
     * <ul>
     *   <li>{@code aerialCombat} — 空战开关与模组检测</li>
     *   <li>{@code skillNoCooldown} / {@code 技能无冷却} — 术式技能无冷却</li>
     *   <li>{@code hungerConsumption} / {@code 饱食度} — 全局饱食度消耗倍率</li>
     *   <li>{@code stylizedDamage} / {@code 跳字联动} — StylizedDamage 样式联动</li>
     * </ul>
     */
    private static LiteralArgumentBuilder<CommandSourceStack> buildModRulesBranch()
    {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("modRules")
                .executes(ctx ->
                {
                    CommandSourceStack source = ctx.getSource();
                    source.sendSuccess(() -> Component.literal("========== 模组规则（全服） =========="), false);
                    source.sendSuccess(() -> Component.literal(
                            "空战：" + (ModRules.AerialCombat.isEnabled() ? "已开启" : "已关闭")), false);
                    source.sendSuccess(() -> Component.literal(ModRules.AerialCombat.serverStatusLine()), false);
                    source.sendSuccess(() -> Component.literal(
                            "术式技能无冷却：" + (ModRules.SkillNoCooldown.isEnabled() ? "已开启" : "已关闭")), false);
                    source.sendSuccess(() -> Component.literal(
                            "饱食度消耗倍率：" + ModRules.HungerConsumption.getMode().zhLabel()), false);
                    source.sendSuccess(() -> Component.literal(
                            "StylizedDamage 跳字联动：" + (ModRules.StylizedDamageStyles.isEnabled() ? "已开启" : "已关闭")), false);
                    source.sendSuccess(() -> Component.literal(ModRules.StylizedDamageStyles.serverStatusLine()), false);
                    return 1;
                });
        root.then(buildModRulesAerialCombatBranch("aerialCombat"));
        root.then(buildModRulesAerialCombatBranch("空战"));
        root.then(buildModRulesSkillNoCooldownBranch("skillNoCooldown"));
        root.then(buildModRulesSkillNoCooldownBranch("技能无冷却"));
        root.then(buildModRulesHungerConsumptionBranch("hungerConsumption"));
        root.then(buildModRulesHungerConsumptionBranch("饱食度"));
        root.then(buildModRulesStylizedDamageBranch("stylizedDamage"));
        root.then(buildModRulesStylizedDamageBranch("跳字联动"));
        return root;
    }

    private static LiteralArgumentBuilder<CommandSourceStack> buildModRulesAerialCombatBranch(String literal)
    {
        LiteralArgumentBuilder<CommandSourceStack> aerial = Commands.literal(literal)
                .requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS));
        aerial.then(buildModRulesAerialCombatSetBranch());
        aerial.then(Commands.literal("check")
                .executes(ctx -> executeModRulesAerialCombatCheck(ctx.getSource().getPlayerOrException())));
        aerial.executes(ctx -> executeModRulesAerialCombatGet(ctx.getSource().getPlayerOrException()));
        return aerial;
    }

    private static LiteralArgumentBuilder<CommandSourceStack> buildModRulesAerialCombatSetBranch()
    {
        LiteralArgumentBuilder<CommandSourceStack> set = Commands.literal("set");
        set.then(modRulesAerialCombatLiteral("on", true));
        set.then(modRulesAerialCombatLiteral("off", false));
        set.then(modRulesAerialCombatLiteral("开启", true));
        set.then(modRulesAerialCombatLiteral("关闭", false));
        return set;
    }

    private static LiteralArgumentBuilder<CommandSourceStack> modRulesAerialCombatLiteral(String literal, boolean enabled)
    {
        return Commands.literal(literal).executes(ctx -> executeModRulesAerialCombatSet(ctx.getSource(), enabled));
    }

    private static int executeModRulesAerialCombatSet(CommandSourceStack source, boolean enabled)
    {
        String failure = ModRules.AerialCombat.trySetEnabled(enabled);
        if (failure != null)
        {
            source.sendFailure(Component.literal(failure));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("空战：" + (enabled ? "已开启" : "已关闭")), true);
        return 1;
    }

    private static int executeModRulesAerialCombatGet(ServerPlayer player)
    {
        player.sendSystemMessage(Component.literal("空战：" + (ModRules.AerialCombat.isEnabled() ? "已开启" : "已关闭")));
        player.sendSystemMessage(Component.literal(ModRules.AerialCombat.serverStatusLine()));
        player.sendSystemMessage(Component.literal(ModRules.AerialCombat.playerStatusLine(player)));
        if (ModRules.AerialCombat.isEnabled() && !ModRules.AerialCombat.hasPlayerMods(player))
        {
            player.sendSystemMessage(Component.literal("提示：你已开启空战，但客户端未安装全部所需模组，空战功能可能无法正常使用。"));
        }
        return 1;
    }

    private static int executeModRulesAerialCombatCheck(ServerPlayer player)
    {
        player.sendSystemMessage(Component.literal("========== 空战模组检测 =========="));
        player.sendSystemMessage(Component.literal(ModRules.AerialCombat.serverStatusLine()));
        player.sendSystemMessage(Component.literal(ModRules.AerialCombat.playerStatusLine(player)));
        boolean ready = ModRules.AerialCombat.isServerReady() && ModRules.AerialCombat.hasPlayerMods(player);
        player.sendSystemMessage(Component.literal("空战可用：" + (ready ? "是" : "否")));
        return 1;
    }

    private static LiteralArgumentBuilder<CommandSourceStack> buildModRulesSkillNoCooldownBranch(String literal)
    {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal(literal)
                .requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS));
        root.then(modRulesSkillNoCooldownLiteral("on", true));
        root.then(modRulesSkillNoCooldownLiteral("off", false));
        root.then(modRulesSkillNoCooldownLiteral("开启", true));
        root.then(modRulesSkillNoCooldownLiteral("关闭", false));
        root.executes(ctx ->
        {
            boolean enabled = ModRules.SkillNoCooldown.isEnabled();
            ctx.getSource().sendSuccess(() -> Component.literal(
                    "术式技能无冷却（全服）：" + (enabled ? "已开启" : "已关闭")), false);
            return 1;
        });
        return root;
    }

    private static LiteralArgumentBuilder<CommandSourceStack> modRulesSkillNoCooldownLiteral(String literal, boolean enabled)
    {
        return Commands.literal(literal).executes(ctx ->
        {
            ModRules.SkillNoCooldown.setEnabled(enabled);
            ctx.getSource().sendSuccess(() -> Component.literal(
                    "术式技能无冷却（全服）：" + (enabled ? "已开启" : "已关闭")), true);
            return 1;
        });
    }

    private static LiteralArgumentBuilder<CommandSourceStack> buildModRulesHungerConsumptionBranch(String literal)
    {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal(literal)
                .requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS));
        LiteralArgumentBuilder<CommandSourceStack> set = Commands.literal("set");
        set.then(modRulesHungerModeLiteral("none", ModRules.HungerConsumption.Mode.NONE));
        set.then(modRulesHungerModeLiteral("low", ModRules.HungerConsumption.Mode.LOW));
        set.then(modRulesHungerModeLiteral("vanilla", ModRules.HungerConsumption.Mode.VANILLA));
        set.then(modRulesHungerModeLiteral("不消耗", ModRules.HungerConsumption.Mode.NONE));
        set.then(modRulesHungerModeLiteral("0.1倍", ModRules.HungerConsumption.Mode.LOW));
        set.then(modRulesHungerModeLiteral("原版", ModRules.HungerConsumption.Mode.VANILLA));
        root.then(set);
        root.executes(ctx ->
        {
            ModRules.HungerConsumption.Mode mode = ModRules.HungerConsumption.getMode();
            ctx.getSource().sendSuccess(() -> Component.literal(
                    "饱食度消耗倍率（全服）：" + mode.zhLabel()), false);
            return 1;
        });
        return root;
    }

    private static LiteralArgumentBuilder<CommandSourceStack> modRulesHungerModeLiteral(
            String literal,
            ModRules.HungerConsumption.Mode mode)
    {
        return Commands.literal(literal).executes(ctx ->
        {
            ModRules.HungerConsumption.setMode(mode);
            ctx.getSource().sendSuccess(() -> Component.literal(
                    "饱食度消耗倍率（全服）：" + mode.zhLabel()), true);
            return 1;
        });
    }

    private static LiteralArgumentBuilder<CommandSourceStack> buildModRulesStylizedDamageBranch(String literal)
    {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal(literal)
                .requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS));
        root.then(buildModRulesStylizedDamageSetBranch());
        root.then(Commands.literal("check")
                .executes(ctx -> executeModRulesStylizedDamageCheck(ctx.getSource().getPlayerOrException())));
        root.executes(ctx -> executeModRulesStylizedDamageGet(ctx.getSource().getPlayerOrException()));
        return root;
    }

    private static LiteralArgumentBuilder<CommandSourceStack> buildModRulesStylizedDamageSetBranch()
    {
        LiteralArgumentBuilder<CommandSourceStack> set = Commands.literal("set");
        set.then(modRulesStylizedDamageLiteral("on", true));
        set.then(modRulesStylizedDamageLiteral("off", false));
        set.then(modRulesStylizedDamageLiteral("开启", true));
        set.then(modRulesStylizedDamageLiteral("关闭", false));
        return set;
    }

    private static LiteralArgumentBuilder<CommandSourceStack> modRulesStylizedDamageLiteral(String literal, boolean enabled)
    {
        return Commands.literal(literal).executes(ctx -> executeModRulesStylizedDamageSet(ctx.getSource(), enabled));
    }

    private static int executeModRulesStylizedDamageSet(CommandSourceStack source, boolean enabled)
    {
        String failure = ModRules.StylizedDamageStyles.trySetEnabled(enabled);
        if (failure != null)
        {
            source.sendFailure(Component.literal(failure));
            return 0;
        }
        StylizedDamageIntegrationRegistrar.registerStylesIfPresent();
        ModNetwork.syncStylizedDamageStylesRuleToAll(enabled);
        source.sendSuccess(() -> Component.literal("StylizedDamage 跳字联动：" + (enabled ? "已开启" : "已关闭")), true);
        if (!enabled)
        {
            source.sendSuccess(() -> Component.literal("提示：关闭后停止投影叙事 ID；样式仍保留。对方 /stylizeddamage reload 后需再开一次或重进服以重建选择器。"), false);
        }
        return 1;
    }

    private static int executeModRulesStylizedDamageGet(ServerPlayer player)
    {
        player.sendSystemMessage(Component.literal(
                "StylizedDamage 跳字联动：" + (ModRules.StylizedDamageStyles.isEnabled() ? "已开启" : "已关闭")));
        player.sendSystemMessage(Component.literal(ModRules.StylizedDamageStyles.serverStatusLine()));
        player.sendSystemMessage(Component.literal(ModRules.StylizedDamageStyles.playerStatusLine(player)));
        if (ModRules.StylizedDamageStyles.isEnabled() && !ModRules.StylizedDamageStyles.hasPlayerMod(player))
        {
            player.sendSystemMessage(Component.literal("提示：联动已开启，但你的客户端未安装 StylizedDamage，无法看到定制跳字。"));
        }
        return 1;
    }

    private static int executeModRulesStylizedDamageCheck(ServerPlayer player)
    {
        player.sendSystemMessage(Component.literal("========== StylizedDamage 模组检测 =========="));
        player.sendSystemMessage(Component.literal(ModRules.StylizedDamageStyles.serverStatusLine()));
        player.sendSystemMessage(Component.literal(ModRules.StylizedDamageStyles.playerStatusLine(player)));
        boolean ready = ModRules.StylizedDamageStyles.isServerReady() && ModRules.StylizedDamageStyles.hasPlayerMod(player);
        player.sendSystemMessage(Component.literal("跳字联动可用：" + (ready ? "是" : "否")));
        return 1;
    }

    /**
     * 十影契约作弊：{@code /jjkz tenShadows tameAll}（或 {@code /jjkz tenShadows 调伏全部}）
     * 一键调伏全部十种式神。
     */
    private static LiteralArgumentBuilder<CommandSourceStack> buildTenShadowsCheatBranch()
    {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("tenShadows")
                .requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS));
        root.then(Commands.literal("tameAll").executes(ctx -> executeTenShadowsTameAll(ctx.getSource())));
        root.then(Commands.literal("调伏全部").executes(ctx -> executeTenShadowsTameAll(ctx.getSource())));
        return root;
    }

    private static int executeTenShadowsTameAll(CommandSourceStack source) throws CommandSyntaxException
    {
        ServerPlayer player = source.getPlayerOrException();
        PlayerTenShadowsContractData.grantAllTamed(player);
        StringBuilder names = new StringBuilder();
        for (TenShadowsShikigami shikigami : TenShadowsShikigami.values())
        {
            if (!names.isEmpty())
            {
                names.append("、");
            }
            names.append(shikigami.displayName());
        }
        player.sendSystemMessage(Component.literal(
                "已调伏全部十影式神（作弊）：" + names + "。"));
        return 1;
    }

    /** 作弊获取 Rush Into The ZONE 最高潮：{@code /jjkz rushIntoTheZoneClimax} */
    private static LiteralArgumentBuilder<CommandSourceStack> buildRushIntoTheZoneClimaxBranch()
    {
        return Commands.literal("rushIntoTheZoneClimax")
                .requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .executes(ctx ->
                {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    RushIntoTheZoneClimaxEffect.grant(player);
                    player.sendSystemMessage(Component.literal(
                            "已获得 Rush Into The ZONE 最高潮：ZONE III + 下一次体术攻击必定黑闪。"));
                    return 1;
                });
    }
}
