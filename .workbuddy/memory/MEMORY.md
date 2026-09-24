# 项目长期备忘（JujutsuKaisenZone / Forge 1.20.1）

> 只留「不知道就会踩坑」的结论；机制细节见 `2026-09-22.md` / `2026-09-23.md`。

## 环境 / 铁律
- ⛔ **禁止跑 `./gradlew`（含 compileJava）**：游戏锁 `build/` + 管道憋输出，能卡 10min~1h。编译由用户在 IDEA 做；AI 只做静态自查（读文件 / grep 全部调用点 / 核对签名）。长任务输出别接 `| tail`。
- 别 `cd` 到项目目录。PowerShell stdout 不可见。Bash 缺 coreutils，先补 PATH：`export PATH="/c/Users/19316/.workbuddy/binaries/PortableGit/versions/1.2.0/usr/bin:/c/Windows/System32:$PATH"`
- 裸 python 无 PIL/numpy；图像用 `.../python/envs/default/Scripts/python.exe`。**给 Windows exe 传路径必须写 `C:/...`**（`/c/...` 被当相对路径且不报错）。
- 同一文件多处 Edit 必须串行；不同文件可并行。EBUSY = 被游戏/IDEA 占用，重试即可。
- 工具集会**中途变化**，缺写入类工具时报 Tool Not Found：别重试，直接给用户精确补丁（行号 / 唯一锚点）。

## Forge 1.20.1 玩家数据
- **`getPersistentData()` 根标签死亡重生时全丢**：`PlayerList#respawn` → new `ServerPlayer` → `restoreFrom` **只搬 `PlayerPersisted` 子标签**，之后才发 `PlayerEvent.Clone`。
- ⇒ 跨死亡三选一：写 `Player.PERSISTED_NBT_TAG` 子标签 / `PlayerEvent.Clone` 手动搬 / SavedData `JjkzPlayerDataStorage`。
- ⛔ **服务端无法用 `deltaMovement` 移动玩家**（客户端每 tick 覆盖）⇒「高速平移 / 冲刺」只能每 tick `ServerPlayer#teleportTo(x,y,z)`（≤1.5 格/tick 即「高速」观感）；`lookAt(...)` 必须在 teleport **之前**，位置包才带朝向。玩家位移类技能的状态用**静态内存 Map + deadline 兜底**，别落盘（掉线会留残留）。
- **改维度不重建玩家实体**（`changeDimension` 复用对象）⇒ 服务端数据不丢，只有客户端 LocalPlayer 重建、需重新同步。
- 查源码：`~/.gradle/caches/forge_gradle/minecraft_user_repo/net/minecraftforge/forge/<ver>_mapped_official_1.20.1/...-sources.jar`（含补丁）；`mcp/.../decompile/output.jar` 是**未打补丁**版，别用。
- 杂项：按键注册走 `RegisterKeyMappingsEvent`；同时监听 mod + FORGE 总线的类必须拆开（`client/BarrierRulesKeyHandler` 漏 `bus = MOD` 待修）；推实体 `setDeltaMovement(dm.add(impulse)); hurtMarked = true;`；`MobEffect#addAttributeModifier` 第二参是 **String**。

## 技能解锁 / 死亡保留
- 键：`jujutsukaisenzone_<technique>_unlock_slot_N`(bool) / `_level_slot_N`(int) / `_cooldown_slot_N`(long)；空战是 `..._aerial_*_unlock`（**`_unlock` 结尾、无下划线**）。
- 缺 key：等级 = 1 级；解锁 = **未解锁**；空战 Shift 变体相反（缺失 = 已解锁）。
- 识别键的**唯一判据** = `SkillUnlockLogic.isUnlockTagKey/isLevelTagKey`（前缀 + 中缀 + **判 NBT 类型**；星域 `..._gate_unlocks` 是 long[] 必须排除）。`snapshotUnlockTags` 与 `copyProgressionFrom` 共用。
- 客户端开发树读**客户端** persistentData ⇒ 必须 `syncToClient`。
- 跨死亡靠 `SkillUnlockLogic.copyProgressionFrom(new, original)`（在 `onPlayerClone` 调）；只搬解锁 / 等级，咒力 / 连击 / 星域等战斗态保持清空。**新增持久化成长数据必须一起接进来**。
- 术式情报在 SavedData `JjkzPlayerDataStorage.TAG_TECHNIQUE_INTEL`；技能栏另有 `PlayerSkillBarConfig.copyAllFrom`。遗留：`*_cast_range_tier` 仍随死亡丢失。

## 新增术式 / 技能集
- 生得术式 6 处：`item/XxxTechniqueBinderItem`、`ModItems`、创造模式**两套**列表（`ModCreativeTabEvents` + `client/gui/creative/JjkzCreativeTabCategory`，后者 LDLib 自绘、`TabVisibility` 管不到）、`PlayerTechniqueData`、模型+贴图、lang。
- 技能集 `extends AbstractInnateTechniqueSkillSet`：槽位 `skillForSlot(slot)`；tag = `tag("unlock"|"level"|"cooldown", slot)`；树 = `TechniqueSkillTree.builder(id)` + 静态块 `validateAgainstSkills(SKILLS)`；构造函数里 `register(INSTANCE)`。
- 加技能 = `SKILLS` 追加 + 树节点 + 描述 + lang；图标 `textures/gui/technique/<name>.png`。命名：显示名用汉字，ID / 类名 / 注册名 / tag 用官方英文，**ID 与类名一律不改**。
- ⚠️ **新增术式伤害必须走 `TechniqueDamageSources.deal(target, owner, amount)`**，别写 `damageSources().magic()`——后者 `getEntity()`/`getLastHurtByMob()` 恒为 null，击杀不给咒术经验、不掉术式情报、不记罪恶者、`BlackFlashEvents` 击杀检测也不触发。`deal` = 补归因 + `LivingKnockBackEvent` 吃掉 0.4 击退 + **刻意不填 `directEntity`**（保持术式无视盾牌）；只剩「生物仇恨施术者」这一条连带（原版语义，保留）。
- 原版 `LivingEntity#hurt` 的三件事全读 `getEntity()`（causingEntity）：盾挡看 `getSourcePosition()`（= directEntity 位置，全库只此一处用），击退 `if (entity1 != null && !IS_EXPLOSION)`，仇恨 `setLastHurtByMob` 受 `no_anger` 标签门控（1.20.1 该标签只有 `mob_attack_no_aggro`）。**1.20.1 无 `no_knockback` 标签**，要精确免击退只能从 Forge 的 `LivingKnockBackEvent`（`knockback()` 第一行，可取消）或「取消后自己抵消位移」下手。

## 特别招式 / 调节模式
- 链路：`SpecialMoveInput` → `TechniqueSpecialMoveC2SPacket` → registry `useSpecialMove` → `useSpecialMove(player, skillId, mask)` → 基类 → `castSpecialMoveBySkill`。数值与原技能同源，只有解锁可独立。
- ⚠️ **两道前置收口**（在扣咒力 / 上冷却**之前**）：`acceptsSpecialMove`（默认 = `getSpecialMoves` 掩码**精确相等**，不匹配静默 return）+ `isSpecialMoveUnlocked` ⇒ **先声明、后执行**：新增特别招式必须写进 `getSpecialMoves`。
- ⚠️ 免费 / 条件放行条目（清除、查询、自身标记）：覆写 `useSpecialMove(player, skillId, mask)` 分流，**同时覆写 `acceptsSpecialMove`**，否则被静默拦下。开发树自动派生节点 = 覆写 `getSpecialMoves(baseSkillId)`。
- 调节模式 `TechniqueSkillKeyEvents`：中键循环、侧键 4/5 = delta ∓1；`CAST_RANGE=0`/`BARRIER_RADIUS=1`/`STAR_MARK=2`；加新模式的钩子 = `resolveAdjustModeCount` + `applyAdjustModeDelta`。

## 星星标记 / 禁行星域（细节见 09-22、09-23 日志）
- 两套进度独立：① 星位等级 `stacks`（叠加驱动，衰减 8s/级、**最低 1 级**，`MAX_MARKS=32` 按最久未打顶掉）；② 跨序解锁（位图，**接触驱动**，10s 限时，**每次展开清零**）。结算唯一位置 `StarMarkSkillLogic.resolveAll`；同星叠满 = **引爆**（半径 4 给 debuff + 传染，**爆心单体伤害 22**——只有爆心那一个目标受伤，`damageCore` 里方块/掉落物/施术者自己一概豁免，层数清零）。
- ⚠️ 星域**无时限**（开关式），解除只有 Ctrl+技能键 / 换维度 / 换术式三条；⚠️ 星域表现与标记**解耦**（外环只看「域是否生效」，无标记也要画）；⚠️ 域开着时重复施放 = 新实例、进度全重置。
- ⚠️ 半径托管给 `BarrierTechniqueServerLogic.staticGetBarrierRadius`（`{8,16,32,64}`，默认 8）——**全项目唯一尺寸档位**，每 tick 现取；描述在客户端、档位在服务端 ⇒ 只能写「默认 / 上限」。
- ⭐ 展开瞬间弹射**单向**（方块弹实体、血多弹血少），初速与半径反相关 **× 被弹方星位倍率** `burstStackScale`（`1+0.25×(等级-1)`，1级×1 → 5级×2；等级取 `ResolvedMark.mark().stacks()`，`resolveAll` 已结算衰减），关键是固定上抛（离地后摩擦 45%→9%，同初速多飞约 5 倍）⇒ **只乘初速、绝不乘 `BURST_LIFT`**（乘了是弧线变高，不是变远）。
- ⭐ 「踢足球」制导 = 同星吸引里的 `GUIDANCE_IMPULSE=0.045` 附加横向加力，**只在 `!entity.onGround()` 的成员上生效**（成员级判据，必须在成员循环内现算，组级 strength/verticalScale 管不到），垂直分量清零。触发条件复用 `ATTRACT_RANGE=16`，**不需要发射方记状态**。⛔ 铁律：**用「加力」不用「转向」**（加力才能免费得到「速度越快越难命中」），且**绝不乘星位/半径倍率**（设计决策点：固定额度补偿才留得住难度梯度，见 09-24 日志）。
- ⭐ **撞击伤害**（`resolveImpacts`，踢足球的**收益点**）：伤害 = 水平速度 × 12，双方各一记，走 `TechniqueDamageSources.deal`。触发三件套 = 球身 `TAG_BURST_UNTIL` 未过期（弹射时盖，⇒「缓慢靠近不痛」精确成立） + 水平速度 ≥ 0.45 + 扫掠命中。⚠️ **必须扫掠**：实体间原版不碰撞，球速 2.8 格/tick vs 猪宽 0.7 ⇒ 逐点判定会隧穿（越用力越打不中）。用 `boundingBox.inflate(0.35).clip(head.subtract(deltaMovement), head)`——**tick END 时 `getDeltaMovement()` 就是本 tick 实际位移**。方块/掉落物不参与（球门零伤害）；`TAG_IMPACT_AT` 挂球身 ⇒ 多施术者天然去抖。⚠️ `AABB#intersects(Vec3,Vec3)` 是两点式，别当线段用。⚠️ 推力对**服务端玩家无效**（客户端每 tick 覆盖 deltaMovement）⇒「禁行」靠的是解锁门槛。
- 📌 **命名**：弹射 + 制导 + 撞击伤害这一整套玩法正式名为 **「星星大碰撞！」/ Star Crash!**，说明节点 id = `love_rendezvous_star_gate_collision`（挂在 `love_rendezvous_star_gate` 下，lang 键 `skill_tree.jujutsukaisenzone.love_rendezvous_star_gate_collision.title/.desc`）。
- 槽位 4 **「飞行，穿越」**（`StarFlightSkillLogic`，动画 `flying_turn` / `flying_turn_kick`）：条件 = 自身有标记 + 目标标记满级 + ≤20 格（**不筛跨序**）；流程 = `deactivateGate` → 每 tick 追踪位移 → **`detonate` 引爆旧印记** → 冲撞 12 + **补上「当前选中」印记**（`applySelectedMark`，直走 `PlayerStarMarkData.put`、不经经济）→ **仅当新印记与自身标记跨序时才 `StarMarkRules.flightKick`**（力度 `BURST_IMPULSE_NEAR × burstStackScale(5)`，**永远施术者弹目标**）。爆心须在 `detonate` 前取。前置校验**必须在 `castWithSkillEconomy` 之前**。⚠️ 射程 `CAST_RANGE` 与飞行时长上限**必须同步**：`MAX_FLIGHT_TICKS ≥ CAST_RANGE / TICKS_PER_BLOCK`（现 20/1.25 = 16，`FLIGHT_TIMEOUT_TICKS` 取两倍 32）。描述与动作栏提示全走 `CAST_RANGE`，改常量即自动跟随，lang 只有 `%s`。槽位 4 = 技能栏第二组第 1 格。
- 力学只在服务端 `StarMarkRules`：一律**增量**速度 + `hurtMarked=true`（防与 EpicFight 抢位移）；同标记用质心。⚠️ **制导与撞击的成员池 = `apply(owner, marks)` 里「该施术者自己的标记列表」，不是全局**：没标记的实体既不制导也不当球（`resolveImpacts:585` 球 / `:611` 靶）。⇒ 「飞行，穿越」踢飞的目标**能**继续当球，靠的正是末段补上的那颗新印记（`detonate` 会吃掉旧标记，**补印记必须排在弹飞之前**）；但**制导还要 `group.size() >= 2`**，即同一颗星必须也打给了另一个实体，否则只是纯抛物线。`MARK_RANGE=128`（须 > 半径上限 64）。`StarMark` **7 字段 → 6 处构造点**。

## 术式开发界面 / IDEA
- LDLib `drawInBackground` 按 `addWidget` 顺序绘制 ⇒ 层级只靠加入顺序。⚠️ 点选回调不能当场 `clearAllWidgets()`，登记 pending 到 `updateScreen()`；重建用 `rebuildGraph(false)` 才不重置平移缩放。
- IDEA：ForgeGradle `getIdeaModule()` 只把空格换下划线 ⇒ `settings.gradle` 固定 `rootProject.name = 'JuJutsuKaisenZone_forge-1_20_1'` → `genIntellijRuns` → Reload All from Disk。
