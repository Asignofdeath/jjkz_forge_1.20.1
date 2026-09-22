# 项目长期备忘（JujutsuKaisenZone / Forge 1.20.1）

> 细节与过程见 `.workbuddy/memory/YYYY-MM-DD.md`；本文件只留**必须记牢的结论**。

## 环境（重要）
- PowerShell 工具 stdout 不可见，不要用它读输出。
- Bash 默认缺 coreutils，**命令前补一行 PATH 即可用**（`ls`/`cat`/`grep`/`git`）：
  `export PATH="/c/Windows/System32:/c/Windows:/c/Users/19316/.workbuddy/binaries/PortableGit/versions/1.2.0/usr/bin:/c/Users/19316/.workbuddy/binaries/PortableGit/versions/1.2.0/cmd:$PATH"`
- **不要 `cd` 到项目目录**：cwd 已是项目根，`cd D:/...` 必失败。`dirname: command not found` / `cd: null directory` 是宿主 shim 噪音。
- Python：`C:/Users/19316/.workbuddy/binaries/python/versions/3.13.12/python.exe`。
- 编译：`./gradlew compileJava --project-cache-dir=build/.gradle-tmp --console=plain`（1~1.5 分钟，后台跑）。报错提 log 后 `iconv -f GBK -t UTF-8` 再 grep「错误」。
- ⚠️ **同一文件的多次 Edit 不能放在同一条消息里并行发**：后一次基于旧内容写回，会静默覆盖掉先一次（表现为编译「找不到符号」）。必须串行。不同文件之间并行没问题。

## 新增生得术式（6 处固定改动面，以 `love_rendezvous` 为例）
1. `item/XxxTechniqueBinderItem` — 校验 `PlayerCursedEnergyData.isHeavenlyRestricted` + `PlayerFactionData.canBindInnateTechnique` 后 `PlayerTechniqueData.setInnateTechnique`。
2. `registry/ModItems` — `ITEMS.register("<id>", () -> new XxxBinderItem(new Item.Properties().stacksTo(1)))`。
3. **创造模式两套列表，必须两处都改**：`registry/ModCreativeTabEvents`（`TabVisibility.SEARCH_TAB_ONLY`，原版标签页/搜索页）＋ `client/gui/creative/JjkzCreativeTabCategory`（对应枚举的硬编码 `List`）。
   后者是 LDLib 自绘面板（`JjkzCreativeTabPanel` + `CreativeModeInventoryScreenMixin`），与 Forge 标签页体系无关，`TabVisibility` 管不到。
4. `player/PlayerTechniqueData` — `TECHNIQUE_XXX` 常量、`isXxx()`、`getDisplayName()` 分支。
5. `assets/.../models/item/<id>.json` + `textures/item/<id>.png`。
6. `lang/zh_cn.json` + `en_us.json` — `item.jujutsukaisenzone.<id>`。

## 新增技能集 / 技能
1. `XxxTechniqueSkillSet extends AbstractInnateTechniqueSkillSet`，实现 `getSlotCost`/`getSlotCooldownTicks`/`isSlotUnlocked`/`getSlotSkillLevel`/`getSlotCooldownTag`/`getSlotDisplayName`/`getSlotIconPath`/`castSkillSlot`。
   **全部写成「按 `SKILLS` 下标取」（`skillForSlot(slot)`），不要逐槽 if 链**；tag 用 `tag("unlock"|"level"|"cooldown", slot)`。
2. 技能定义在静态 `List<TechniqueSkill>`；节点用 `TechniqueSkillTree.builder(techniqueId)`；静态块 `validateAgainstSkills(SKILLS)`。
3. `JuJutsuKaisenZone` 构造函数 `TechniqueSkillSetRegistry.register(XxxTechniqueSkillSet.INSTANCE)`（内部顺带注册技能树）。
4. `TechniqueSkillDescriptions` 注册描述解析器 + lang `skill.jujutsukaisenzone.<skillId>.desc`。
5. 图标放 `textures/gui/technique/<name>.png`，`iconPath` 写相对路径。
6. **加技能 = `SKILLS` 追加一项 + 一个树节点 + 一条描述与 lang。**

## 命名约定
- **术式双名沿用原作**：中文显示名（`getDisplayName()`、lang `item.*`）用**汉字表记**（无下限 / 御厨子 / 审判 / 星间飞行）；术式 ID、注册名、类名、tag 用**原作读音 / 官方英文名**（`love_rendezvous` / `LoveRendezvousTechniqueSkillSet`）。
  **ID 与类名一律不改**（牵动贴图路径、模型、注册名、存档字符串），调整只改显示文案。
- skillId：`<technique>_<name>`。技能节点 nodeId 直接用 skillId；`autoGranted()` = 获得术式即解锁。
- tag：`jujutsukaisenzone_<technique>_{unlock|level|cooldown}_slot_N`。
- 玩家级战斗数据统一写 `player.getPersistentData()` → **死亡重生清空**（Forge 只搬 `PlayerPersisted` 子标签）；要跨死亡保留得用该子标签或 `JjkzPlayerDataStorage`。
- **未注册技能集不会崩**：`SkillSlotHudOverlay`/`SkillBarConfigScreen`/`TechniqueDevelopmentCanvasWidget`/`SkillUnlockLogic` 都对 `Optional.empty()` 兜底，只是不显示、不进随机觉醒池。

## 特别招式（修饰键 + 技能键）
**定义**：不占独立 1~9 槽位、由「修饰键 + 技能键」触发的独立技能。
链路：`SpecialMoveInput(modifierMask, baseSlot)` → `TechniqueSpecialMoveC2SPacket` → `TechniqueSkillSetRegistry.useSpecialMove`（按栏位配置解析 skillId）→ `IInnateTechniqueSkillSet#useSpecialMove(player, skillId, mask)` → `AbstractInnateTechniqueSkillSet` 默认实现 → `castSpecialMoveBySkill`。

**数值同步（默认，结构上无法不同步）**：特别招式**没有自己的 `TechniqueSkill` 记录**，`findSkillById(skillId)` 反查出「原技能」再走 `castWithSkillEconomy(skill, …)` → 等级 / 咒力 / 冷却一律取原技能那一份。**唯一可独立的是解锁**：`castWithSlotEconomy(player, slot, cast, isUnlocked)` 重载（无限术式用 `isShiftVariantUnlockedForSlotN`）。

**⚠️ 顺序坑**：默认链路「**先扣咒力 / 上冷却，再演出**」。所以在 `castSpecialMoveBySkill` 里 `if (mask != MOD_SHIFT) return;` 收口是错的 —— **任何非该掩码的组合（Ctrl / Alt / Shift+Ctrl…）都会白扣一次费**。根因是通路**完全不校验掩码**：客户端 `buildModifierMask` 按着什么发什么，`TechniqueSpecialMoveC2SPacket` 只查 `slot 1..9 && mask != 0`，registry 只按 skillId 路由。
→ **「不消耗经济」的条目（清除 / 查询类）必须在子类覆写 `useSpecialMove(player, skillId, mask)` 抢先分流并 return。**
> 既有隐患（未修）：`LimitlessTechniqueSkillSet.castSpecialMoveBySkill` 就是这个写法。
> 死代码：`AbstractInnateTechniqueSkillSet.useSpecialMove(player, SpecialMoveInput)` → `castSpecialMove(player, input)` 自 registry 改走三参重载后不可达（`Limitless`/`Shrine` 的 `castSpecialMove` 覆写同理）。

**在开发树显示特别招式**：只需覆写 `getSpecialMoves(baseSkillId)` 返回 `TechniqueSpecialMove` 列表，
`TechniqueSkillSetRegistry.register` 自动用 `TechniqueSpecialMoveAttacher` 派生节点（nodeId = `原技能节点id + "$" + variantId`，`requires(原技能节点)`，复用原技能图标与解锁状态），**不要手写树节点**。
`SkillTreeNodeKind.SPECIAL_MOVE` 不可单独解锁，解锁状态 = 原技能状态。界面：青蓝边框 + 青蓝连线。

## 战斗内「调节模式」体系（TechniqueSkillKeyEvents）
- 中键循环切换模式；侧键 4 → delta −1，侧键 5 → delta +1。
- 模式：`ADJUST_MODE_CAST_RANGE=0` / `ADJUST_MODE_BARRIER_RADIUS=1` / `ADJUST_MODE_STAR_MARK=2`（仅星间飞行可用）。
- 加「仅某术式可用」的模式：放开 `resolveAdjustModeCount`（中键 `%` 循环、每 tick 超界退回 0），侧键派发在 `applyAdjustModeDelta` 加分支。
- C2S 包：`AdjustCastRangeTierC2SPacket` / `AdjustBarrierRadiusTierC2SPacket` / `SelectStarMarkC2SPacket`，均「delta ±1 + 服务端校验 + 动作栏提示」。
- 三个可改键：`KEY_ADJUST_MODE` / `KEY_ADJUST_TIER_DOWN` / `KEY_ADJUST_TIER_UP`（中键 / 侧键4 / 侧键5），在 `JuJutsuKaisenZone.ClientModEvents.onRegisterKeyMappings` 注册。
- 中键默认与**原版「选取方块」**同绑（MOUSE:2）→ 控制界面标红、游戏内两者都触发；刻意为之。

## Forge 1.20.1 按键与 API 要点（已核实反编译源码）
- **注册必须走 `RegisterKeyMappingsEvent`**（mod 总线）：它做的是 `options.keyMappings = ArrayUtils.add(...)`，决定控制界面可见性与 options.txt 持久化。只 new 出 KeyMapping 只够 `isDown()` 用。
- 同时要监听 mod 总线与 FORGE 总线的事件类**必须拆开**（`@Mod.EventBusSubscriber` 的 `bus` 只能给一个）。
  ⚠️ `client/BarrierRulesKeyHandler` 踩了这个坑（漏 `bus = MOD` → 注册方法死代码，待修）。
- 鼠标键用 `KeyMapping.isDown()` 安全：`MouseHandler.onPress` 只在 `screen == null && overlay == null` 时 `KeyMapping.set(Type.MOUSE, ...)`。`isDown()` 额外校验 `KeyConflictContext`，`IN_GAME.isActive()` == 无界面。
- `InputEvent.MouseScrollingEvent`（1.20.1）**只有 `getScrollDelta()`**；`getDeltaX()/getDeltaY()` 属 1.20.6+。查 Forge API 认准 `1.20.1` 分支。
- 推 / 拉实体：`hurtMarked` / `hasImpulse` / `fallDistance` 是公有可写字段。惯用 `target.setDeltaMovement(target.getDeltaMovement().add(impulse)); target.hurtMarked = true;`

## 「星星标记」体系（星间飞行 / love_rendezvous）
- **语义是「目标 → 星」**：同一目标至多一个标记（重复打标 = 覆盖），同一颗星可打给任意多个目标。`PlayerStarMarkData` 的 NBT 是根下一个 **ListTag**；旧格式被 `getList(TAG_ROOT,10)` 静默忽略，**不需要迁移**。
- 施加必须是**接触**，两条入口都汇入 `LoveRendezvousTechniqueSkillSet.INSTANCE.tryApplyMark(player, mark, name)`（咒力 / 冷却 / 解锁都在里面结算）：
  实体走 `AttackEntityEvent`（近战，含掉落物，挂 `event/StarMarkEvents`）；方块走**星星标记技能键**，视线由**服务端**自算（`StarMarkSkillLogic.lookedAtBlock`，`ClipContext` OUTLINE，4.5 格），客户端只报「按了键」。
  ⚠️ 历史坑：曾用 `PlayerInteractEvent.RightClickBlock` 打方块，会与放置方块抢右键，已废弃 —— 别再退回右键。
- 力学（`technique/skill/StarMarkRules`，只在服务端）三条铁律：① 一律**增量**速度 `dm.add(impulse)` + `hurtMarked=true`，**不覆盖**（否则与 EpicFight 抢位移）；② 同标记用**质心**不用两两成对（收敛、不自激）；③ **方块是静态锚点**，只推会动的一侧，方块之间不判定。
  三态：同标记吸引 / 相邻无效果 / 跨序斥力（只在「禁行」星域内）。`MIN_SEPARATION=1.5` 内不再吸引、斥力翻倍；`MAX_SIMULATED=16` 按距离截断；`MARK_RANGE=32` 外自动解除。
- `StarMarkType` 枚举顺序即原作星序，相邻判定 = `Math.abs(ordinal差) == 1`。牵引 / 禁行状态也存 `getPersistentData()` 根级（`..._pull_until` / `..._gate_until`）。

## 术式开发界面（client/gui/technique）
- **LDLib 的 `Widget.drawInBackground` 按子组件加入顺序绘制，后加入者在上层** → 「置顶 / 层级」只需控制 `addWidget` 先后，**不需要 z-index / bringToFront API**。
  证据：`TechniqueDevelopmentCanvasWidget.rebuildGraph()` 先加连线、后加节点，连线因此永远垫底。
- 点选高亮（已实现）：`SkillTreeHighlight` record 承载「选中节点 + 直连节点 + 直连连线」；`rebuildGraph(boolean refitView)` 的 refit **只有点选时传 false**，以免重置玩家的平移 / 缩放。
- ⚠️ 点选回调**不能当场重建**：它发生在父容器遍历子组件（`mouseClicked`）途中，当场 `clearAllWidgets()` 会并发修改 —— 登记 pending，放到 `updateScreen()` 执行。
- 「辉光」省事做法：用 `DrawerHelper.drawFilledFrameRoundBox` 在**外扩一圈**的圆角框上先画半透明色，再画本体覆盖内圈，只留外缘辉光。
- **说明节点（INFO）不参与任何连线**：`prerequisiteNodeIds()` 是双用途的 —— 对技能/特别招式/引用节点是「解锁前置会画线」，对说明节点只是**布局锚点**（`SkillTreeLayoutEngine` 用来算列深与行序）。
  统一判定入口 `SkillTreeNodeDef.isConnectableEdge(from, to)`（任一端 INFO 即 false），连线绘制与点选高亮都必须走它。

## IDEA 运行配置
- 根因：ForgeGradle `RunConfig.getIdeaModule()` = `project.getName().replace(' ', '_') + ".main"`（**只换空格不换点号**），而 IDEA 导入时把项目名点号换成下划线 → 目录名 `...-1.20.1` 带点必然错位。
- 修法：`settings.gradle` 固定 `rootProject.name = 'JuJutsuKaisenZone_forge-1_20_1'` 后 `./gradlew genIntellijRuns`（约 2 分钟）；改完必须在 IDEA 里 **Reload All from Disk**。
