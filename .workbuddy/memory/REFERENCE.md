# 子系统参考（JujutsuKaisenZone / Forge 1.20.1）

> `MEMORY.md` 的展开版：只留「不知道就会踩坑」的结论。逐日过程见 `2026-09-2*.md`。

## Forge 1.20.1 玩家数据 / 位移
- `getPersistentData()` 根标签死亡重生**全丢**（`PlayerList#respawn` → new `ServerPlayer` → `restoreFrom` **只搬 `PlayerPersisted` 子标签**，之后才发 `PlayerEvent.Clone`）⇒ 跨死亡三选一：写 `Player.PERSISTED_NBT_TAG` 子标签 / `PlayerEvent.Clone` 手动搬 / SavedData。
- ⛔ 服务端**无法用 `deltaMovement` 移动玩家**（客户端每 tick 覆盖）⇒ 平移只能每 tick `ServerPlayer#teleportTo(x,y,z)`（≤1.5 格/tick 即「高速」观感）；`lookAt(...)` 必须在 teleport **之前**。玩家位移类状态用**静态内存 Map + deadline**，别落盘。
- `ServerPlayer#teleportTo` → `connection.teleport(..., ROTATION)`：发位置包并置 `awaitingPositionFromClient`；该值非空时 `handleMovePlayer` **整包丢弃**，靠 `handleAcceptTeleportPacket` 清空 ⇒ 每 tick 连续 teleport 仍可用（accept 先于移动包到达）。
- **改维度不重建玩家实体**（`changeDimension` 复用对象）⇒ 服务端数据不丢，只有客户端 LocalPlayer 重建、需重新同步。
- 杂项：按键注册走 `RegisterKeyMappingsEvent`；同时监听 mod + FORGE 总线的类必须拆开（`client/BarrierRulesKeyHandler` 漏 `bus = MOD` 待修）；推实体 `setDeltaMovement(dm.add(impulse)); hurtMarked = true;`；`MobEffect#addAttributeModifier` 第二参是 **String**。

## 新增术式 / 技能集
- 生得术式 6 处：`item/XxxTechniqueBinderItem`、`ModItems`、创造模式**两套**列表（`ModCreativeTabEvents` + `client/gui/creative/JjkzCreativeTabCategory`，后者 LDLib 自绘、`TabVisibility` 管不到）、`PlayerTechniqueData`、模型+贴图、lang。
- 技能集 `extends AbstractInnateTechniqueSkillSet`：槽位 `skillForSlot(slot)`；tag = `tag("unlock"|"level"|"cooldown", slot)`；树 = `TechniqueSkillTree.builder(id)` + 静态块 `validateAgainstSkills(SKILLS)`；构造函数里 `register(INSTANCE)`。
- 加技能 = `SKILLS` 追加 + 树节点 + 描述 + lang + 图标 `textures/gui/technique/<name>.png`。命名：显示名用汉字，ID / 类名 / 注册名 / tag 用官方英文，**ID 与类名一律不改**。
- ⚠️ 伤害走 `TechniqueDamageSources.deal`：= 补归因 + `LivingKnockBackEvent` 吃掉 0.4 击退 + **刻意不填 `directEntity`**（保持术式无视盾牌）；只剩「生物仇恨施术者」这条连带（原版语义，保留）。1.20.1 **无 `no_knockback` 标签**，精确免击退只能从 Forge `LivingKnockBackEvent`（`knockback()` 第一行，可取消）下手。

## 技能解锁 / 死亡保留
- 键：`jujutsukaisenzone_<technique>_unlock_slot_N`(bool) / `_level_slot_N`(int) / `_cooldown_slot_N`(long)；空战是 `..._aerial_*_unlock`（**`_unlock` 结尾、无下划线**）。
- 缺 key：等级 = 1 级；解锁 = **未解锁**；空战 Shift 变体相反（缺失 = 已解锁）。
- 识别键的**唯一判据** = `SkillUnlockLogic.isUnlockTagKey/isLevelTagKey`（前缀 + 中缀 + **判 NBT 类型**；星域 `..._gate_unlocks` 是 long[] 必须排除）。`snapshotUnlockTags` 与 `copyProgressionFrom` 共用。
- 客户端开发树读**客户端** persistentData ⇒ 必须 `syncToClient`。
- 跨死亡靠 `SkillUnlockLogic.copyProgressionFrom(new, original)`（在 `onPlayerClone` 调）；只搬解锁 / 等级，咒力 / 连击 / 星域等战斗态保持清空。**新增持久化成长数据必须一起接进来**。
- 术式情报在 SavedData `JjkzPlayerDataStorage.TAG_TECHNIQUE_INTEL`；技能栏另有 `PlayerSkillBarConfig.copyAllFrom`。遗留：`*_cast_range_tier` 仍随死亡丢失。

## 特别招式 / 调节模式
- 链路：`SpecialMoveInput` → `TechniqueSpecialMoveC2SPacket` → registry `useSpecialMove` → `useSpecialMove(player, skillId, mask)` → 基类 → `castSpecialMoveBySkill`。数值与原技能同源，只有解锁可独立。
- ⚠️ **两道前置收口**（在扣咒力 / 上冷却**之前**）：`acceptsSpecialMove`（默认 = `getSpecialMoves` 掩码**精确相等**，不匹配静默 return）+ `isSpecialMoveUnlocked` ⇒ **先声明、后执行**：新增特别招式必须写进 `getSpecialMoves`。
- ⚠️ 免费 / 条件放行条目（清除、查询、自身标记）：覆写 `useSpecialMove(player, skillId, mask)` 分流，**同时覆写 `acceptsSpecialMove`**，否则被静默拦下。开发树自动派生节点 = 覆写 `getSpecialMoves(baseSkillId)`。
- 调节模式 `TechniqueSkillKeyEvents`：中键循环、侧键 4/5 = delta ∓1；`CAST_RANGE=0`/`BARRIER_RADIUS=1`/`STAR_MARK=2`；加新模式的钩子 = `resolveAdjustModeCount` + `applyAdjustModeDelta`。

## 领域展开
- 基类 `AbstractDomainExpansion`：`ACTIVE_DOMAINS`(UUID→Deque<`BarrierInstance`>)、`DOMAIN_RADIUS=24`、`setDomainName(String)`（public）；`getTechniqueType()` 来自接口 `IBarrierTechnique`。
- 生命周期钩子：`applyDomainSpecificEffect(player, instance)`（结界完成）/ `onBarrierClosed(player, instance)`（关闭）/ `tickMaintain(player)`（每 tick，**先调 `super`**）。
- 子类 `AbstractTechniqueDomain` 额外要求 `hasLearnedTechnique(player)`；`DomainExpansionRouter` 静态块 `ALL_DOMAINS.add(...)` 注册 + `getDomainForPlayer(player)` 按术式路由（不匹配回退基础领域 `DomainExpansionServerLogic`）+ `tickMaintainAll` 每 tick 维护。
- 术式标识：`PlayerTechniqueData.TECHNIQUE_*` + `isXxx(player)`。例：`TECHNIQUE_LOVE_RENDEZVOUS = "love_rendezvous"`、`isLoveRendezvous(player)`。
- ⚠️ 领域专属状态放**内存 `Map<UUID, ...>`**（与领域实例同生共死），别落玩家 NBT ⇒ 避免「无域却生效」幽灵态；数值与判定留在对应 Logic 类，别塞进领域类。
- 已实现：无量空处 / 伏魔神龛 / 诛伏赐死 / **爱与幽会**（`LoveRendezvousDomainExpansion`）。

## 星间飞行 / 星星大碰撞
- 细节见 `2026-09-22.md` ~ `2026-09-24.md`。关键类：`StarMarkRules`（服务端三态力学）/ `StarMarkSkillLogic.resolveAll`（**唯一结算点**）/ `StarFlightSkillLogic`（槽位 4「飞行，穿越」）/ `LoveRendezvousDomainExpansion`（领域「爱与幽会」）。
- ⭐ **领域「爱与幽会」= 有源连锁**（对标原神达达利亚的「断流」）。引爆的播散改走另一套规则（`StarMarkDetonation#spreadMark` 的 `domainBroadcast` 分支）：域外只给**干净目标**打 **1 级**；域内给干净目标**满级**、并把半径内**已有的同星**标记一并拉满。切换点在引爆内部按**爆心是否在域内**判定，三条引爆路径（叠满 / 撞击 / 飞行穿越）自动跟随。
  - 域外那个「1 级」正是**链条断点**：被播散的目标离「可燃的 5 级」还差 4 次打标 ⇒ 域外是逐个清场，域内才是连锁。不要再试图用「全域点满」去补它（那等于删掉连锁的前提：标记靠传播获得）。
  - 另配「域内飞行球完全制导（必中）」——作用是让连锁每一环都不落空，不是单纯强化数值。
  - 引爆的**播散**必须走 `StarMark#maxed(now)`（会刷新 `lastMarkedAt`）；`withStacks` 保留时刻，只改等级会被同 tick 的衰减按回原样。
- 两套进度独立：① 星位等级`stacks`（叠加驱动，衰减 8s/级、**最低 1 级**，`MAX_MARKS=32`）；② 跨序解锁位图（**接触驱动**，10s 限时，每次展开清零）。同星叠满 = **引爆**。
- 星域**无时限**（开关式），解除只有 Ctrl+技能键 / 换维度 / 换术式；半径托管 `BarrierTechniqueServerLogic.staticGetBarrierRadius`（`{8,16,32,64}`，默认 8），**全项目唯一尺寸档位**。
- ⛔ 三条设计骨（动之前先看日志）：
  - ① 力学一律**增量速度** + `hurtMarked=true`，同标记用质心；成员池 = `apply(owner, marks)` 里**该施术者自己的标记**（非全局）。
  - ② 制导**用「加力」不用「转向」**（加力才免费得到「越快越难命中」），且**绝不乘星位 / 半径倍率**。
  - ③ 跨序斥力（芝诺墙）**约束位移不约束速度**。限速**绝不能加到通用 `push`**（`flightKick`/`flightRam`/`burstKick` 的 1.40 起步会被削成渣）。
- ⭐ **引力弹弓**（域外专属，`StarMarkRules#slingBalls`）：被弹飞的球掠过**同星方块**（`SLING_RANGE=2.5` 内）→ 水平速 `+SLING_BOOST(0.6)` 并夹 `MAX_BALL_SPEED(3.4)`、航向改指「下一个可撞目标」（找不到则退回原航向）、`+SLING_LIFT(0.30)` 上抛、刷新 `TAG_BURST_UNTIL`。
  - **与域内制导是 if/else 互斥分支**（域内=必中终结，与「续航」语义互斥，且域内撞击即摘球身份）。
  - **两道闸共用 `TAG_SLING_USED`（球身 `long[]`，元素 `BlockPos#asLong()`）**：数组长度=借力次数（上限 `SLING_MAX_PER_BALL=2`），同时充当「同一方块只弹一次」的黑名单。**每次重新踢出即清空**——收敛在 `markAsBall`（`burstNeighbors` / `flightKick` 两处共用）。
  - 速度是伤害**唯一**乘数 ⇒ **速限是硬要求**，不封顶即「一记即死」。
  - ⚠️ **未验前提**：方块锚点在方块上方 `BLOCK_MARK_HEIGHT`(1.15) 格，「掠过不撞」只在**锚点意义**成立；**原版方块物理碰撞仍在**，球贴地飞会怼在方块侧面。需实机确认「掠过 vs 撞停」。

## 术式开发界面 / IDEA
- LDLib `drawInBackground` 按 `addWidget` 顺序绘制 ⇒ 层级只靠加入顺序。⚠️ 点选回调不能当场 `clearAllWidgets()`，登记 pending 到 `updateScreen()`；重建用 `rebuildGraph(false)` 才不重置平移缩放。
- IDEA：ForgeGradle `getIdeaModule()` 只把空格换下划线 ⇒ `settings.gradle` 固定 `rootProject.name = 'JuJutsuKaisenZone_forge-1_20_1'` → `genIntellijRuns` → Reload All from Disk。
- **说明节点**（`SkillTreeNodeDef.info(id)`，用于给过长的技能描述瘦身）：
  - `titleKey` / `descriptionKey` 必填，且 **`visibleWhen(...)` 必填**（缺了 `TechniqueSkillTree#validate` 直接抛异常）；`requires` **只作自动布局锚点**——不画连线、不参与解锁判定。
  - ⛔ **描述走 `Component.translatable(key)` 无参翻译 ⇒ 吃不到 `%s`**：只能放**纯静态文案**，任何数值（或随常量变的东西）必须留在技能描述里，说明节点只能写「越大 / 越小」这类定性句。
  - 现有 4 个（全在星间飞行）：`..._star_mark_controls`「快捷操作」/ `..._star_mark_effects`「星的效果」/ `..._star_gate_collision`「星星大碰撞！」/ `..._star_flight_details`「相会细则」。
  - 加节点**不用动画布尺寸**：`TechniqueDevelopmentCanvasWidget` 每次重建都按 visibleNodes 重跑 `SkillTreeLayoutEngine`。
  - 判断「哪些描述太长」：临时脚本统计 `skill.jujutsukaisenzone.*.desc` 字数排名（注意 lang 里 `%s` 个数必须与 `TechniqueSkillDescriptions` 注入的 args 一一对应，删句记得同步删 args）。

## 技能描述的「简洁 / 完整」两页
- 形态：**默认简洁页**（无任何数值，几句叙事）；**按住 Shift** 换成完整页（原描述，数值照旧）。简洁页在标题下多一行 `shift_for_details`（`gui.jujutsukaisenzone.skill.shift_for_details`）。
- ⛔ **LDLib 的 tooltip 没有分页 / 点击能力**——`Widget` 里就是 `List<Component> tooltipTexts` + `drawTooltipTexts`（`javap` 查 deobf jar 确认）。所以分页只能做成「每帧读键盘状态」：`SkillTreeNodeWidget#updateScreen` 本来就每帧调 `refreshHoverTooltips()`，`Screen.hasShiftDown()` 直接放里面。别想着做可点击 tab。
- 判定「有没有简洁页」**只看 `brief` 键在不在**（`TechniqueSkillTooltipHelper#hasBrief`：`translatable` 结果落回键名即视为缺失 ⇒ 回退完整页）。好处：不必维护一份"哪些技能有两页"的名单，新技能写下 `.brief` 就自动带简洁页；没写的（空战能力、咒力强化）自动显示完整页，也不会冒出裸键名。
- 简洁文案走**无参** `Component.translatable`（**不吃 `%s`**，因为不显示数值）⇒ **不需要在 `TechniqueSkillDescriptions` 注册解析器**。键名 `skill.jujutsukaisenzone.<skillId>.brief`。
- 覆盖面：**21 个技能节点全部**（含 5 个机制型，机制型的浅紫「机制型技能」标注在简洁页照旧保留）。说明节点 / 特别招式节点走各自的 tooltip 分支，**不参与**分页。
- 未解锁节点仍只显示 `???`（`SKILL_LOCKED` 分支压根不调 `build`）⇒ 简洁页不会提前泄露未解锁技能。
- 两个调用点共用同一入口：`SkillTreeNodeWidget`（开发树）与 `SkillBarConfigScreen`（快捷栏配置），改 `TechniqueSkillTooltipHelper#build` 即两处同时生效。

## 机制型技能（可解锁的非主动机制）
- 落点：**复用普通 `SKILL` 节点，不要新增节点种类**。技能定义放进技能集的 `getMechanicSkills()`（`IInnateTechniqueSkillSet` 的默认方法），**不进** `getConfigurableSkills()`。
  - 于是它天然不出现在 `SkillBarConfigScreen` / `fillDefaults` 里；`defaultSlot = 0`（与真实槽位 1~9 永不相等）⇒ `TechniqueSkillKeyEvents#resolveConfiguredSkillId` 匹配不到它；`cost` / `cooldownTicks` / `levelTag` 填 0 与空串，只有 `unlockTag` 是实质字段。
  - ⚠️ **`SkillUnlockLogic#findSkillDefinition` 必须能查到它**：那是「节点 → 技能定义」的唯一解析路径（`isSkillNodeUnlocked` / `getVisualState` / tooltip 全靠它）。因此该方法改成 **configurable + mechanic 两路合并检索**。漏了这步 ⇒ 节点永远显示未解锁且点不动。
  - `validateAgainstSkills` 只做**单向**校验（技能 → 节点），所以要把 `SKILLS + MECHANICS` 一起传进去，少写节点会在类加载时就抛。
- 查询入口：`SkillUnlockLogic.isSkillUnlockedById(player, techniqueId, skillId)`（运行时门控，手里只有 skillId 时用）/ `isMechanicSkill(techniqueId, skillId)`（展示层区分）。
- 展示层：`TechniqueSkillTooltipHelper` 对机制型技能**只渲染名字 + 描述**，不出现「咒力消耗：0 / 冷却：0 秒」。空战能力（滞空 / 多段跳）当初是逐个特判的，新写法按 `isMechanicSkill` 判定，以后加机制不用再加分支。
- 描述数值照旧注入 `TechniqueSkillDescriptions`，直接引用常量（为此把 `StarMarkRules` 的 `SLING_RANGE` / `SLING_BOOST` / `MAX_BALL_SPEED` / `SLING_MAX_PER_BALL` 提成 `public`，与 `MARK_RANGE` 同立场）。
- ⛔ **只门控「能力型」机制，绝不门控「约束型」机制**：星位衰减、芝诺墙、持有上限这类是**限制**，做成可解锁 ⇒ 解锁前反而更强，方向是反的。可门控的是「新增收益 / 新选项」：引力弹弓、引爆传染、域内连锁拉满、依次穿越链、制导加力。
- 现有 5 个：
  - 星间飞行 2 条——`love_rendezvous_star_sling`「引力弹弓」（前置 `love_rendezvous_star_gate`、300 情报），门控在 `StarMarkRules#apply` 的 `else if (isSlingUnlocked(owner))`（未解锁则不扫同星方块）；`love_rendezvous_star_chain`「在星间穿梭」（前置 `love_rendezvous_star_flight`、450 情报），门控在 `StarFlightSkillLogic#cast` 的 `mask` 快照读取口（未解锁 ⇒ mask 恒 0 ⇒ 不排折线、单段突进）。**做这类技能优先找「已实现机制 + 单一门控点」，而不是新写玩法**——折线（`planChain`）本就存在。
  - 审判 3 条——`judgment_gavel_forms`「三式法槌」（前置 `judgment_gavel`、150）/ `judgment_sentencing`「量刑」（前置 `judgment_trial`、150）/ `judgment_aggravation`「从重论处」（前置 `judgment_sentencing`、200）。门控只有两处：`JudgmentWeaponLogic#cycleGavelForm` 开头（未解锁不切换形态），以及 `JudgmentTrialSkillLogic#rollGuiltyVerdictType(caster, target, crimeBonusPercent)` 这个**私有重载**（未解锁「量刑」→ 直接 `return CONFISCATION`；未解锁「从重论处」→ debuff 与重罪加成一律归零）。后一处即三路全覆盖：「审判」「指控」「领域审理」都经由它掷骰。
- ⚠️ **降级形态做成「减法」，不是新逻辑**：门控只让既有行为少发生（形态数变 1 / 不掷死刑 / 加成归零），不引入新分支。附带连带（未解锁量刑 ⇒ 不判死刑 ⇒ 不发【处刑人之剑】）是自然结果，不需要额外门控。
- ⚠️ **门控点优先选「多条路径共用的那个唯一结算点」**，而不是在每个调用点各写一遍——审判两条机制因此只动了一个方法。

## 伤害乘区管线（2026-09-24 起）
- **汇合点 = `event/TechniqueDamageZoneEvents`**（`LivingHurtEvent` @HIGH）：术式（`deal`→`hurt`）与 EpicFight 体术都在此结算乘区。区序：①施术者增强（`TechniqueDamageBoosts` 分型匹配「下一击增伤」）→ ②目标减免（ZONE 减伤）。
- 优先级链（勿乱动）：处刑人 HIGHEST（伤害量**改写**）→ 管线 HIGH → 罪恶者 ×1.5 / 黑闪 pow(2.5) / 蓄力打断 NORMAL。黑闪 `pow(raw, 2.5) × 当前值 / raw` ⇒ 幂只放大底数，各乘区（强度 / 防御 / ZONE）**线性**通过；前提是区 0 **无条件**记下 raw。乘法交换律 ⇒ 乘算类迁移位置不改数值。
- **分型 = 叙事标签**（与通道解耦）：术式能力→TECHNIQUE；体术能力→MARTIAL；同一次叠加两者→COMPOSITE。挂法：`deal(..., type)` 或 `runWithType`/`callWithType`（thrown / explode / mobProjectile 等保留原通道时）。`currentDamageType()` 在 deal/runWithType 栈内有值。
- **结算现场判定**（`TechniqueDamageZoneEvents#resolveDamageType`）：ThreadLocal 优先 → 处刑剑+死刑挥击→COMPOSITE → 必定黑闪的 EF 基础攻击→COMPOSITE → EF `isBasicAttack`→MARTIAL → 赫飞块 `TAG_RED_DEBRIS`→TECHNIQUE → 其余 null。随机黑闪在 NORMAL 才掷骰，管线 HIGH 时仍按体术消耗加成（已知边界）。
- **技能描述**：正文写「N 点术式/体术伤害」等；**禁止**技能级 `damageTypes` 字段与悬停「伤害类型」标题行。分型只挂在出伤结算点。
- `TechniqueDamageBoosts`：纯内存 Map（`push/consume/clear`），「下一击」瞬时增益**不落盘、不进 copyProgressionFrom**；登出在 `BlackFlashEvents#onPlayerLoggedOut` 清。消耗严格匹配：TECHNIQUE 击只耗 TECHNIQUE 登记，COMPOSITE 击一次耗三类。
- 消耗语义：同分型多次 push 加算叠加（乘区内加算、乘区间乘算）。
- ⚠️ 新乘区（易伤/克制/标签抗性）一律加进 `TechniqueDamageZoneEvents` 并写明顺序，别再开裸 `setAmount`。武器特有条件判定（如法槌对罪恶者）留在各自 Events，不进通用管线。
## 目标防御层（2026-09-25 起）

汇合点仍在 `TechniqueDamageZoneEvents`：区 0 / 1 / 3 在 `LivingHurtEvent` @HIGH；**区 2 目标防御层**单独挂 `LivingDamageEvent` @HIGH（原版护甲 / 附魔 / 抗性药水算完之后，才能对总减伤精确封顶）。

- **`registry/ModAttributes`**：`jjkz:physical_resistance` / `jjkz:technique_resistance`，`RangedAttribute`，**单位是百分比点数**（25 = 25%），范围 −100~100，`setSyncable(true)`。默认 0 ⇒ **怪物不会凭空变软**。挂在实体类型上就用 `EntityAttributeModificationEvent`：`getTypes()` 是 `List<EntityType<? extends LivingEntity>>`，`add(type, attr)` 自动取 `getDefaultValue()`，**不做重复检查**。
- **体抗语义（用户定死）**：只吃 EF 基础攻击 / 原版怪物近战 / 原版玩家近战 / 本模组 `MARTIAL`；**不碰**箭、三叉戟、火球、爆炸、闪电、火、岩浆。术抗只吃 `TECHNIQUE`。复合（黑闪 / 处刑必杀）**两层各半**（`combatDefense.compositeResistanceShare`），与 `SkillDamageResolver` 的「复合取均值」两端对称。
- **`technique/SkillDefenseResolver`**：护甲层 + 抗性层换算，与 `SkillDamageResolver` 对称。护甲层**只该对 `JjkzDamageTypeTags.IS_TECHNIQUE` 调**——其余通道原版已经算过，再补一道就是双计。
- **`jjkz:technique` 伤害类型**（`TechniqueDamageSources#TECHNIQUE_DAMAGE_TYPE`，数据包 `message_id = "jjkz.technique"` 带点）替代了原先借用的 `MAGIC`。标签取舍：**加** `bypasses_armor`（原版公式对高频小伤害太狠）、`bypasses_shield`；**不加** `bypasses_resistance` / `bypasses_enchantments` / `witch_resistant_to`（后者顺手修掉女巫对术式 ×0.15 的意外减伤）。
- **近战判据 `TechniqueDamageSources#isMelee`**：`directEntity == getEntity() && instanceof LivingEntity && !IS_EXPLOSION`。比查标签准，不依赖 EF，`fallingBlock` 之类天然排除。
- ⚠️ **分型解析不再要求 `ServerPlayer`**：怪物近战也要能分出 MARTIAL，否则体抗对怪物完全无效。需要玩家身份的两条判据（处刑必杀 / 必定黑闪）自己 `instanceof` 收敛。
- ⚠️ **区 0 扩到 `MARTIAL || COMPOSITE`，但必须排除处刑必杀**（`!isExecutionersComposite(...)`）：处刑剑的伤害量已在 HIGHEST 被改写成固定致死值，再乘一道面板倍率会让低强度玩家杀不死。复合在近战里只有两条路，这条判据正好分开它们。
- ⚠️ **区 2 挂 `LivingDamageEvent`，对总减伤精确封顶**：`LivingHurtEvent` 里只交接「进入原版减免前的量 + 分型」，真正的护甲/抗性乘算在原版全部算完后用交接基准封顶（`defenseReductionCap` 默认 85%）。ZONE（区 3）在交接之后施加，不进封顶基准。

## LDLib 界面（术式开发 / 全屏 GUI）

入口：`client/gui/technique/TechniqueDevelopmentUI#open()`（`ui.setFullScreen()` + `ModularUIGuiContainer`）。
标签页 = 一排 `ButtonWidget`（`setButtonTexture` 切激活态）＋ 互斥切换的显示控件。

写自定义控件（`client/gui/technique/PlayerDataPanelWidget` 是最简范例）：
- `extends com.lowdragmc.lowdraglib.gui.widget.Widget`，构造 `super(new Position(0,0), new Size(1,1))` + `setClientSideWidget()`。
- 重写 `drawInBackground(GuiGraphics, int mouseX, int mouseY, float partialTicks)`；里面直接用原版 `graphics.fill(...)` / `graphics.drawString(font, s, x, y, color, false)`，无需 LDLib 绘制器。
- 尺寸：重写 `onScreenSizeUpdate(int w, int h)` 里 `setSelfPosition(0,0)` + `setSize(new Size(w,h))`（`Widget` 不会自动铺满）。
- ⚠️ **最稳的坐标策略：控件固定 (0,0) 铺满全屏，内部按绝对坐标画**——这样无论 LDLib 是否按控件自身位置做位移，落到屏幕上的结果都一致。若是子控件，参照 `SkillTreeNodeWidget`（用 `getSelfPosition()`）。
- `com.lowdragmc.lowdraglib.utils.Size` 的 `width` / `height` 是 **public final 字段**（另有 `getWidth()` / `getHeight()`）。
- `setVisible(boolean)` 对 `FreeGraphView` 也生效，可用来做「画布 ↔ 面板」互斥。

查 LDLib API：见 MEMORY.md 的 javap 路径。

## 静态类型检查（不跑 gradlew，全工程 javac）

**目的**：`./gradlew` 被明令禁止（游戏锁 `build/`、能卡 10min~1h）。但「改签名 / 改调用点」这类改动，编译器能报的错基本可穷尽 ⇒ 直接用 javac 编译整个 `src/main/java`，classpath 从 gradle 缓存现场拼。446 文件约 20 秒，**不写 `build/`**，输出到 `%TEMP%`。

> 已落地成脚本，**优先直接跑它**（省去每次重打）：
> ```
> export PATH="/c/Users/19316/.gradle/jdks/eclipse_adoptium-17-amd64-windows/jdk-17.0.20.1+1/bin:/c/Users/19316/.workbuddy/binaries/PortableGit/versions/1.2.0/usr/bin:/c/Windows/System32:$PATH"
> /c/Users/19316/.workbuddy/binaries/python/envs/default/Scripts/python.exe "C:/D/MineMOD/JuJutsuKaisenZone_forge-1.20.1/tools/jjkz_static_check.py"
> ```
> 2026-09-26 基线：555 文件 / 290 jar / **exit 0 / 0 错误**。下面的原理与坑仍然有效（脚本就是它）。

实测基线：2026-09-25 防御层改动后 **exit 0 / 0 错误**。

```python
# 用 python - <<'PY' ... PY 传给 python.exe 跑（脚本里用 C:/ 正斜杠路径）
import os, subprocess, re, collections, zipfile

# ⚠️ 唯一正确的 MC jar：official 映射，与源码一致
KEEP = r'C:\Users\19316\.gradle\caches\forge_gradle\minecraft_user_repo\net\minecraftforge\forge\1.20.1-47.4.10_mapped_official_1.20.1\forge-1.20.1-47.4.10_mapped_official_1.20.1.jar'
roots = [r'C:\Users\19316\.gradle\caches\forge_gradle',
         r'C:\Users\19316\.gradle\caches\modules-2\files-2.1']
jars = []
for r in roots:
    for dp, dn, fn in os.walk(r):
        for f in fn:
            if not f.endswith('.jar') or '-sources' in f or '-javadoc' in f:
                continue
            p = os.path.join(dp, f)
            # 待删除的 mcp 空壳会让 javac 直接 "zip END header not found"
            if 'mergeMappings' in p or os.path.getsize(p) < 2000:
                continue
            if os.path.normcase(p) == os.path.normcase(KEEP):
                jars.append(p); continue
            # 关键：缓存里有 11 个含 MC 类的 jar，映射风格各异。
            # SRG 名 jar 抢先命中 ⇒ 6000+ 条假错误（Component 上没有 literal、满屏 m_xxxx_）
            try:
                if 'net/minecraft/network/chat/Component.class' in zipfile.ZipFile(p).namelist():
                    continue
            except Exception:
                pass
            jars.append(p)
jars = [KEEP] + [j for j in jars if j != KEEP]        # KEEP 必须排第一

srcs = [os.path.join(dp, f).replace('\\', '/')
        for dp, dn, fn in os.walk(r'src\main\java') for f in fn if f.endswith('.java')]

# classpath 有 300 个 jar ⇒ 超过 Windows 命令行长度限制（WinError 206），
# 必须把 -cp 也一起塞进 @argfile；argfile 里用正斜杠 + 双引号（javac 把 \ 当转义）
argf = 'C:/Users/19316/AppData/Local/Temp/jjkz_args.txt'
with open(argf, 'w', encoding='utf-8') as fh:
    fh.write('-encoding UTF-8\n-proc:none\n-nowarn\n-Xmaxerrs 10000\n')
    fh.write('-d "C:/Users/19316/AppData/Local/Temp/jjkz_check_out"\n')
    fh.write('-cp "%s"\n' % ';'.join(x.replace('\\', '/') for x in jars))
    for s in srcs:
        fh.write('"%s"\n' % s)

p = subprocess.run(['javac', '@' + argf], capture_output=True)
raw = p.stdout.decode('gbk', errors='replace') + p.stderr.decode('gbk', errors='replace')  # ⚠️ 输出是 GBK
print('exit =', p.returncode)
kinds = collections.Counter(); files = collections.Counter()
for line in raw.splitlines():
    m = re.search(r'错误[:：]\s*(.*)', line)
    if m: kinds[m.group(1).strip()] += 1
    m2 = re.match(r'(.*\.java):(\d+):', line)
    if m2 and '错误' in line:
        files[m2.group(1).replace('\\', '/').split('/')[-1]] += 1
print('错误总行数:', sum(kinds.values()))
for k, v in kinds.most_common(20): print(v, '|', k)
for k, v in files.most_common(20): print(v, '|', k)
```

- `-proc:none` 跳过注解处理器（Mixin 处理器在这里不需要）。
- 报错前**先看涉事文件的 mtime**：用户可能在 IDEA 里**同时编辑**该文件。2026-09-25 出现过「1 个错误」实为检查当时文件正被写入（差 25 秒），重跑即 0。
- 查 Forge API 源码（比反编译靠谱）：解压上面那个官方 `-sources.jar`。例：`EntityAttributeModificationEvent` 的 `getTypes()` / `add()` 签名就是这么确认的。
