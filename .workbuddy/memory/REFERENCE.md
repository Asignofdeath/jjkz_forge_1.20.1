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

## 术式开发界面 / IDEA
- LDLib `drawInBackground` 按 `addWidget` 顺序绘制 ⇒ 层级只靠加入顺序。⚠️ 点选回调不能当场 `clearAllWidgets()`，登记 pending 到 `updateScreen()`；重建用 `rebuildGraph(false)` 才不重置平移缩放。
- IDEA：ForgeGradle `getIdeaModule()` 只把空格换下划线 ⇒ `settings.gradle` 固定 `rootProject.name = 'JuJutsuKaisenZone_forge-1_20_1'` → `genIntellijRuns` → Reload All from Disk。
- **说明节点**（`SkillTreeNodeDef.info(id)`，用于给过长的技能描述瘦身）：
  - `titleKey` / `descriptionKey` 必填，且 **`visibleWhen(...)` 必填**（缺了 `TechniqueSkillTree#validate` 直接抛异常）；`requires` **只作自动布局锚点**——不画连线、不参与解锁判定。
  - ⛔ **描述走 `Component.translatable(key)` 无参翻译 ⇒ 吃不到 `%s`**：只能放**纯静态文案**，任何数值（或随常量变的东西）必须留在技能描述里，说明节点只能写「越大 / 越小」这类定性句。
  - 现有 4 个（全在星间飞行）：`..._star_mark_controls`「快捷操作」/ `..._star_mark_effects`「星的效果」/ `..._star_gate_collision`「星星大碰撞！」/ `..._star_flight_details`「相会细则」。
  - 加节点**不用动画布尺寸**：`TechniqueDevelopmentCanvasWidget` 每次重建都按 visibleNodes 重跑 `SkillTreeLayoutEngine`。
  - 判断「哪些描述太长」：临时脚本统计 `skill.jujutsukaisenzone.*.desc` 字数排名（注意 lang 里 `%s` 个数必须与 `TechniqueSkillDescriptions` 注入的 args 一一对应，删句记得同步删 args）。
