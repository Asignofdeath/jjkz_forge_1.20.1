# 项目长期备忘（JujutsuKaisenZone / Forge 1.20.1）

> 只放**铁律速查**；子系统细节与工具原理一律见 `REFERENCE.md`，逐日过程见 `YYYY-MM-DD.md`。

## 环境 / 工具
- ⛔ **禁跑 `./gradlew`**（含 compileJava）：锁 `build/`、憋输出，能卡 10min~1h。编译交用户在 IDEA 做，AI 只做静态自查。
- ✅ **静态自查 = 全工程 javac**：跑 `tools/jjkz_static_check.py`（JDK bin 先入 PATH）。0 错误才敢说「已落地」。**基线 2026-10-03 = 725 文件 / 292 jar / exit 0**；旧守卫「配置常量旁路」3 条在 `SkillUpgradeStatCatalog.java`（既有遗留）。
- JDK：`C:\Users\19316\.gradle\jdks\eclipse_adoptium-17-amd64-windows\jdk-17.0.20.1+1\bin\`。javac 报错是 **GBK**，grep 中文会静默失败；无 classpath 时「方法不会覆盖或实现超类型的方法」是**误报**。
- ⛔ **同一文件多处 Edit 必须串行**（会**静默丢改动**，工具报成功、只在用户编译时暴露）⇒ 改完 grep 复核每一处调用点。EBUSY = 被占用，重试即可。
- 环境坑：别 `cd` 项目目录；Bash 缺 coreutils ⇒ **删 / 移文件一律用 Python `os.remove`**；PowerShell stdout 不可见；给 Windows exe 传路径写 `C:/...`。
- ⛔ PowerShell 读写 UTF-8 中文文件必须显式 `New-Object System.Text.UTF8Encoding $false`；⛔ 别用 `*>` 接子进程中文输出（⇒ 花码）。范式 `C:\Users\19316\AppData\Local\Temp\jjkz_run_check.py`。lang 文件**都是纯 LF**，python 改文案须 `io.open(..., newline='')`。
- 源码：MC `~/.gradle/caches/forge_gradle/.../1.20.1-47.4.10_mapped_official_1.20.1/*-sources.jar`（含补丁）；**EF 在 `C:\D\MineMOD\epicfight-1.20.1\`**（改 EF 先读源码别猜）。⚠️ 报 maven 证书失败**别折腾证书 / JDK**，`gradle.properties` 的 `-Dnet.minecraftforge.gradle.check.certs=false` **别删**。

## 跨模块铁律
- ⛔ **用户口述文案逐字照写**：不改大小写 / 引号、不补字顺句、不点评不追问（见用户级 MEMORY.md）。
- ⛔ **一切伤害乘算只在 `TechniqueDamageZoneEvents` 里落地**，别处不许裸写 `setAmount`。优先级链勿动。**现行段序**：`0 强度换算 → 1 增伤 → C 暴击 → [LivingDamageEvent 区 2 防御] → 3 ZONE 减免 → 4 韧性减免 → V 浮动(±10%)`；区 V 排除处刑必杀。加新乘区前先读那个类的注释。
- ⛔ **事件阶段**：`LivingHurtEvent` 在**原版护甲之前**；`LivingDamageEvent` 在护甲 / 附魔 / 抗性 / 黄心**之后**。
- ⚠️ **技能描述必须写「玩家当前做得到的事」**，被门控内容整段消失。走 `TechniqueSkillDescriptionRegistry#registerVariant`，**`.brief` 简洁页也要换**；zh_cn / en_us **双侧都写**。
- ⚠️ 术式伤害走 `TechniqueDamageSources.deal`（否则无归因）；但**判据是「有没有归因」**——`thrown(direct, owner)` 本就通，硬改 deal 是**回归**。
- ⚠️ 新增持久化成长数据必须接进 `SkillUnlockLogic.copyProgressionFrom` + `syncToClient`。
- ⚠️ **伤害分型按叙事**（术式 / 体术 / 复合）；**别**在 `TechniqueSkill` 加分型字段、别在悬停标题栏写「伤害类型」。
- ⛔ **暴击默认跟随分型**；术式**单发**招式必须显式 `dealCrit` / `runWithTypeCrit`。闸门 `CritRules.applyCritZone` 必须在 `consumeForced` **之前**。
- ⚠️ 技能树四种节点 `skill / specialMove / reference / info`；**领域展开是 `info` 节点**（无 skillId，按 nodeId 定位，tooltip 在 `SkillTreeNodeWidget` 手搓）。判据只有 `SkillTreeNodeDef#isDomainInfoNode()`。
- ⚠️ 字符串声明表要配启动自检（`CritSkillPolicy#validate()` / `SeEffectRegistry#validate()`），必须在技能集注册**之后**调用。
- ⛔ **skillCombat 数值只许写一次**，业务代码只走 `*SkillGrowth` 解析；守卫与 `jjkz-config-default-ok` 豁免见 `REFERENCE.md#配置来源与权威规则`。
- ⛔ 服务端不能用 `deltaMovement` 移动玩家 ⇒ 每 tick `teleportTo`；`lookAt` 必须在 teleport **之前**。玩家位移态用静态内存 Map + deadline，别落盘。
- ⛔ 可解锁机制只能是「**能力型**」，不能是「约束型」。
- ⛔ **强度换算只在 `TechniqueDamageSources.deal` 内做一次**；出伤点再乘即双计。黑闪 `pow(x, 2.5)` 必须拿**放大前**的原始值取幂。
- ⛔ `CursedGradeBonuses` 不许再给 `ATTACK_DAMAGE` 加等级值。
- ⛔ **技能演出默认不锁操作**：前摇只用「延后成型」制造时间成本。

## 特效 Behavior 层（`entity/behavior/*`）
- 结构 = `EffectBehavior`（`tick` / `onDiscard` / NBT + 静态工厂 `createFromId`）由 `SpecialEffectEntity` 委托，**只在服务端 tick**。骨架是好的策略模式，**别推翻**。
- ⛔ **坏味道是「机制级复制」不是「一技能一类」**：线性飞行 / 柱球清场 / Photon 挂载 / 定时 被逐字抄进多个 Behavior。新技能**优先复用**。
- ✅ 已抽 `entity/util/{AreaShape, BlockErase, SpecialEffectFlight, SpecialEffectChunkForce}`。
- ✅ **P0–P5 已落地（2026-10-03）**：删死代码 `ShrineKaiFlyobjBehavior`；`AreaShape` + `BlockErase` 替换 4 处手写三重循环；飞行循环并入 `SpecialEffectFlight.tickLinear`（`LinearMotion` + `StepCallback`，旧 NBT 键 `FlightDirX/Y/Z` 弃用）；行为层 10 处 Photon 挂载全走 `PhotonFxCommands.perform`；`createFromId` 的 switch 换成注册表；宿主 12 个 `configureXxx` 删除，只留 `attach(EffectBehavior)`。
- ⛔ **新技能写法固定为 `new XxxBehavior()` → `configure(...)` → `fx.attach(b)`**，别再加宿主包装方法。
- ⚠️ **`CompositeBehavior` 模块框架没做**（零消费者的空壳，要上必须先有具体案例）。细节见 `REFERENCE.md#特效 Behavior 层重构`。

## 其他子系统（细节全在 `REFERENCE.md`）
- **SE 术式侵入 / 统一质量池**：质量 = Σ SP 节点 spCost + Σ SE quality；超上限才吃超载罚，越「上限 + 宽限」拒装。装载五道闸见 `PlayerSeLoadoutData.tryEquip`（顺序即错误优先级）。⛔ SE 惩罚只削数值不碰身份；面板强度行惩罚必须加在 `PlayerCombatPowerData.resolveServer` **最后**。`CursedEnergyTrait` 只剩 SE 装载门卡一处用途，⛔ 特质产生规则 / 被动**别顺手扩**。
- **怪物韧性条**（载体 = EF `stun shield`）：⛔ EF 只在 `EpicFightDamageSource` 时碰盾 ⇒ 破韧自写，`MobPoiseEvents#onLivingHurt` @LOWEST，固定扣 `PoiseRules.POISE_DAMAGE_BASE`。⛔ 末影龙在 `applyPoise` / `onLivingHurt` / `onLivingTick` **三处都豁免**。⛔ 特质克制表 + 破韧质量闸门已删。⛔ 监守者 / 潜影贝 / 远古守卫者暂无条。
- **进程级静态会话状态**：领域 / 结界活跃实例由静态单例持有，退出世界不重载类 ⇒ 跨会话残留。统一兜底 `event/SessionStateCleanupEvents`。⛔ 新增任何「进程级静态会话容器」都要挂进这里，别零散 `.clear()`。
