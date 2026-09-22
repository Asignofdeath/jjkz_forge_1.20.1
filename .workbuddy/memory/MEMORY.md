# 项目长期备忘（JujutsuKaisenZone / Forge 1.20.1）

## 环境备忘（重要）
- PowerShell 工具在本环境 **stdout 不可见**，不要用它读取输出。
- Bash 工具默认缺 coreutils（`ls`/`cat`/`head`/`dirname` 都不可用），但**在前面补一行 PATH 即可正常使用**：
  ```
  export PATH="/c/Windows/System32:/c/Windows:/c/Users/19316/.workbuddy/binaries/PortableGit/versions/1.2.0/usr/bin:$PATH"
  ```
  之后 `ls`/`cat`/`head`/`grep`/`git` 全部可用（已验证）。仍推荐优先用 Read/Grep/Glob。
- Python 用 managed：`C:/Users/19316/.workbuddy/binaries/python/versions/3.13.12/python.exe`。
- 编译验证：`./gradlew compileJava --project-cache-dir=build/.gradle-tmp --console=plain`（约 1~1.5 分钟，可后台跑）。

## 新增「生得术式」的完整改动面
以 `love_rendezvous`（中文显示名「星间飞行」）为例，固定 6 处：
1. `item/XxxTechniqueBinderItem.java` — 必须校验 `PlayerCursedEnergyData.isHeavenlyRestricted` + `PlayerFactionData.canBindInnateTechnique`，再 `PlayerTechniqueData.setInnateTechnique`。
2. `registry/ModItems.java` — `ITEMS.register("<id>", () -> new XxxBinderItem(new Item.Properties().stacksTo(1)))`。
3. **创造模式有两套独立的物品列表，新增物品必须两处都改**（只改一处 = 搜得到但栏里没有，或反之）：
   - `registry/ModCreativeTabEvents.java` — `event.accept(ModItems.XXX, CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY)`（原版标签页 + 搜索页）。
   - `client/gui/creative/JjkzCreativeTabCategory.java` — 把 `ModItems.XXX` 加进对应枚举常量（TECHNIQUE / WEAPONS / ITEMS）的硬编码 `List`。
     这个「重写的术式栏」是 LDLib 自绘面板（`JjkzCreativeTabPanel` + `CreativeModeInventoryScreenMixin`），与 Forge 标签页体系无关，`TabVisibility` 管不到它。
4. `player/PlayerTechniqueData.java` — 加 `TECHNIQUE_XXX` 常量、`isXxx()` 判定、`getDisplayName()` 的 switch 分支。
5. `assets/jujutsukaisenzone/models/item/<id>.json` + `textures/item/<id>.png`。
6. `lang/zh_cn.json` + `lang/en_us.json` — `item.jujutsukaisenzone.<id>`（中文命名规范：`生得术式：XXX`）。

## 新增术式「技能集 + 技能」的改动面
1. `technique/skillset/XxxTechniqueSkillSet extends AbstractInnateTechniqueSkillSet` —— 需实现
   `getSlotCost`/`getSlotCooldownTicks`/`isSlotUnlocked`/`getSlotSkillLevel`/`getSlotCooldownTag`/`getSlotDisplayName`/`getSlotIconPath`/`castSkillSlot`；
   技能定义在静态 `List<TechniqueSkill>`，节点用 `TechniqueSkillTree.builder(techniqueId)`，静态块调 `validateAgainstSkills(SKILLS)`。
2. `JuJutsuKaisenZone` 构造函数 `TechniqueSkillSetRegistry.register(XxxTechniqueSkillSet.INSTANCE)`（内部会顺带注册技能树）。
3. `technique/description/TechniqueSkillDescriptions` 注册描述参数解析器 + lang 加 `skill.jujutsukaisenzone.<skillId>.desc`。
4. 技能图标放 `textures/gui/technique/<name>.png`，`iconPath` 写相对路径（`ResourceLocation.fromNamespaceAndPath` 兜底）。

## 战斗内「调节模式」体系（TechniqueSkillKeyEvents）
- 鼠标中键循环切换调节模式，侧键 4 → delta −1、侧键 5 → delta +1。
- 模式：`ADJUST_MODE_CAST_RANGE=0` 施法距离 / `ADJUST_MODE_BARRIER_RADIUS=1` 结界半径 / `ADJUST_MODE_STAR_MARK=2` 星星标记（仅生得术式为星间飞行时可用）。
- 新增「仅某术式可用」的模式：在 `resolveAdjustModeCount` 放开数量即可（中键用 `%` 循环、每 tick 超界自动退回 0），侧键派发在 `applyAdjustModeDelta` 加分支。
- 已有 `AdjustCastRangeTierC2SPacket` / `AdjustBarrierRadiusTierC2SPacket` / `SelectStarMarkC2SPacket` 三个 C2S 包，均为「delta ±1 + 服务端校验 + 动作栏提示」。
- 三个按键已注册为可改键：`TechniqueSkillKeyEvents` 里的 `KEY_ADJUST_MODE` / `KEY_ADJUST_TIER_DOWN` / `KEY_ADJUST_TIER_UP`（默认中键 / 侧键4 / 侧键5），在 `JuJutsuKaisenZone.ClientModEvents.onRegisterKeyMappings` 注册。
- 中键默认与**原版「选取方块」**同绑（都是 MOUSE:2），控制界面会标红、游戏内两者都会触发；这是刻意的（保持原有手感），想避开让玩家自己改键。

## Forge 1.20.1 按键绑定要点（已核实反编译源码）
- **注册必须走 `RegisterKeyMappingsEvent`**（mod 总线）：它做的事是 `options.keyMappings = ArrayUtils.add(options.keyMappings, key)`，决定控制界面能否看到、以及 options.txt 能否持久化。只 new 出 KeyMapping 不够（构造函数只写 `KeyMapping.ALL/MAP`，够 `isDown()` 用）。
- 事件订阅类若同时要监听 mod 总线与 FORGE 总线事件，**必须分开**（`@Mod.EventBusSubscriber` 的 `bus` 只能给一个）。`client/BarrierRulesKeyHandler` 就踩了这个坑（漏写 `bus = MOD` → 注册方法是死代码，待修）。
- 鼠标键用 `KeyMapping.isDown()` 是安全的：`MouseHandler.onPress` 只在 `screen == null && overlay == null` 时 `KeyMapping.set(Type.MOUSE, ...)`。
- `KeyMapping.isDown()` 会额外校验 `KeyConflictContext`，`IN_GAME.isActive()` 等价于「无界面」。

## 术式命名规范（重要，别搞反）
原作不少术式是「**汉字表记 + 片假名读音**」双名（如「星間飛行(ラヴランデヴー)」= 星间飞行 / Love Rendezvous）。本项目的对应规则：

| 用途 | 用哪个名 | 例子 |
|---|---|---|
| 中文显示名（`getDisplayName()`、lang `item.*`） | **原作汉字表记** | 无下限 / 御厨子 / 审判 / **星间飞行** |
| 术式 ID（`PlayerTechniqueData.TECHNIQUE_*`）、物品注册名、类名、tag | **原作读音 / 官方英文名** | `love_rendezvous` / `LoveRendezvousTechniqueSkillSet` |

即：**ID 与类名一律不改**（改了会牵动贴图路径、模型文件、注册名、存档里的术式字符串），要调整只改显示文案。

## 「星星标记」体系（星间飞行 / love_rendezvous）—— 结构约定
- **语义是「目标 → 星」，不是「星 → 目标」**：同一目标至多一个标记（重复打标 = 覆盖该目标的星），同一颗星可打给任意多个目标。`PlayerStarMarkData` 的 NBT 是根下一个 **ListTag**；旧格式会被 `getList(TAG_ROOT,10)` 静默忽略，**不需要写迁移**。
- 施加方式必须是**接触**（原作是「接触对方咒力才能打上」），两条入口都汇入 `LoveRendezvousTechniqueSkillSet.INSTANCE.tryApplyMark(player, mark, name)`（咒力 / 冷却 / 解锁都在里面结算）：
  - 实体：`AttackEntityEvent`（近战打实体 / 掉落物），挂在 `event/StarMarkEvents`。
  - 方块：**按「星星标记」技能键**（`useSkill(SKILL_STAR_MARK)` 分支，2026-09-22 改）。视线由**服务端**自算（`StarMarkSkillLogic.lookedAtBlock`，`ClipContext` OUTLINE，触及 4.5 格），客户端只上报「按了键」。
  - 历史坑：早前用 `PlayerInteractEvent.RightClickBlock` 打方块，**会与放置方块抢右键**，已废弃 —— 别再退回右键，也别为了「接触」而要求玩家真的贴近到 1 格。
- 力学（`technique/skill/StarMarkRules`，只在服务端）三条铁律：① 一律**增量**速度 `setDeltaMovement(dm.add(impulse))` + `hurtMarked=true`，**不覆盖**，否则与 EpicFight 抢位移；② 同标记用**质心**不用两两成对（收敛、不自激）；③ **方块是静态锚点**，只推会动的一侧，方块之间不判定。三态：同标记吸引 / 相邻无效果 / 跨序斥力（只在「禁行」星域内）。
- `StarMarkType` 的枚举顺序即原作星序，相邻判定就是 `Math.abs(ordinal差) == 1`。
- 玩家级数据仍写 `getPersistentData()` 根级 → **死亡重生清空**；牵引/禁行状态也在这里（`..._pull_until` / `..._gate_until`）。

## 技能集多槽位的省事写法
`getSlotCost` / `getSlotCooldownTicks` / `isSlotUnlocked` / `getSlotSkillLevel` / `getSlotCooldownTag` / `getSlotDisplayName` / `getSlotIconPath` 全部可以写成「按 `SKILLS` 下标取」（`skillForSlot(slot)`），不必逐槽写 if 链；tag 也可用 `tag("unlock"|"level"|"cooldown", slot)` 生成。**加技能 = 往 `SKILLS` 追加一项 + 加一个技能树节点 + 加一条描述与 lang。**

## 技能相关命名约定
- skillId：`<technique>_<name>`（如 `shrine_kai` / `judgment_gavel` / `love_rendezvous_star_mark`）。
- 三个 tag：`jujutsukaisenzone_<technique>_unlock_slot_N` / `_level_slot_N` / `_cooldown_slot_N`。
- 技能树节点：技能节点 nodeId 直接用 skillId；`autoGranted()` = 获得术式即解锁。
- 玩家级战斗数据统一写 `player.getPersistentData()`（见 `JudgmentTrialTargetData`），**死亡重生会清空**（Forge 只搬运 `PlayerPersisted` 子标签）；需要跨死亡保留得改用该子标签或 `JjkzPlayerDataStorage`。
- **未注册技能集也不会崩**：`SkillSlotHudOverlay`、`SkillBarConfigScreen`、`TechniqueDevelopmentCanvasWidget`、`SkillUnlockLogic` 全部对 `Optional.empty()` 兜底，只是不显示内容、不进随机觉醒池（`getRegisteredTechniqueIds()`）。

## IDEA 运行配置（点 runClient 弹「编辑配置」对话框）
- 症状：对话框里模块显示「未指定模块 / <无模块>」。
- 两个根因：① `.idea/runConfigurations/*.xml` 是**另一台机器**（用户 `25196`、H 盘；本机是 `19316`、D 盘）生成的，VM 参数与 `build/classpath/*.txt` 全是旧绝对路径；② 模块名对不上——ForgeGradle 用 `project.name.replace(' ','_')+".main"`（**不改点号**），IDEA 导入时把点号换成下划线，目录名 `...-1.20.1` 带点就必然错位。
- 修法：`settings.gradle` 固定 `rootProject.name = 'JuJutsuKaisenZone_forge-1_20_1'`，再 `./gradlew genIntellijRuns`（连带重下 assets / 重生成 classpath 与 srg，约 2 分钟）。改完必须在 IDEA 里 Reload All from Disk，否则会被内存里的旧配置覆盖。

## API 版本坑（Forge 1.20.1 / Forge 47.x）
- `InputEvent.MouseScrollingEvent` 只有 `getScrollDelta()`；`getDeltaX()/getDeltaY()` 属于更新分支（1.20.6+），照抄 GitHub `1.20.x` 分支源码会编不过。
- 查 Forge API 时要认准 `1.20.1` 分支，不是 `1.20.x`。
- 编译报错提取：`./gradlew compileJava ... > log 2>&1` 后用 `iconv -f GBK -t UTF-8` 转码再 grep「错误」。
- 推 / 拉实体：`Entity.hurtMarked`、`hasImpulse`、`fallDistance` 都是**公有可写字段**。惯用写法 `target.setDeltaMovement(target.getDeltaMovement().add(impulse)); target.hurtMarked = true;`（先例 `LimitlessRedArrowBehavior:177`、`AerialMultiJumpServerLogic:30-33`）。

