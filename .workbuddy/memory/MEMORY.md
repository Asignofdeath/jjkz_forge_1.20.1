# 项目长期备忘（JujutsuKaisenZone / Forge 1.20.1）

> 只放**铁律速查**；子系统细节与工具原理一律见 `REFERENCE.md`，逐日过程见 `YYYY-MM-DD.md`。

## 环境 / 工具
- ⛔ **禁跑 `./gradlew`**（含 compileJava）：锁 `build/`、憋输出，能卡 10min~1h。编译交用户在 IDEA 做，AI 只做静态自查。
- ✅ **静态自查 = 全工程 javac**：跑 `tools/jjkz_static_check.py`（JDK bin 先入 PATH）。0 错误才敢说「已落地」。结果**只往 stdout 打印**（不写 `jjkz_check_report.txt`，那个文件是别的东西留下的）；产物落 `%TEMP%/jjkz_check_out`，与 `build/` 无关 ⇒ 不受游戏文件锁影响。**基线 2026-10-03 = 728 文件 / 292 jar / exit 0 / 0 错误 / 配置常量旁路 0**（当日净变化 +3：`config/CfgSpec.java`+`config/ConfigFileLayer.java`+ 联机同步三件套 `SyncModConfigS2CPacket`/`ConfigSyncEvents`/`ClientConfigSyncHandler`，−2 = `client/gui/JjkzModConfigScreen.java`+`client/compat/ForgeConfigScreensIntegration.java`；此前记的「旁路 3 条遗留」已不成立）。
- ⛔ **游戏客户端开着时 IDEA 绝不能构建 / 重跑 Client**：JVM 占住 `build/classes/java/main` ⇒ Gradle 前置清理「删一半」抛 `Unable to delete directory`，产物残缺（整包 class 消失），而游戏侧只报「找不到某个类」/ Mixin `ClassMetadataNotFoundException`，**症状与真因完全不像**。修复 = 关游戏后删掉 `build/classes/java/main` 重建。排查入口：`idea.log` 看 Gradle 真错 > `run/logs/latest.log` > `run/crash-reports/`（crash-report 的 "Suspected Mod" 会误导）。
- JDK：`C:\Users\19316\.gradle\jdks\eclipse_adoptium-17-amd64-windows\jdk-17.0.20.1+1\bin\`。javac 报错是 **GBK**，grep 中文会静默失败；无 classpath 时「方法不会覆盖或实现超类型的方法」是**误报**。
- ⛔ **同一文件多处 Edit 必须串行**（会**静默丢改动**，工具报成功、只在用户编译时暴露）⇒ 改完 grep 复核每一处调用点。EBUSY = 被占用，重试即可。
- 环境坑：别 `cd` 项目目录；Bash 缺 coreutils ⇒ **删 / 移文件一律用 Python `os.remove`**；PowerShell stdout 不可见；给 Windows exe 传路径写 `C:/...`。
- ⛔ PowerShell 读写 UTF-8 中文文件必须显式 `New-Object System.Text.UTF8Encoding $false`；⛔ 别用 `*>` 接子进程中文输出（⇒ 花码）。范式 `C:\Users\19316\AppData\Local\Temp\jjkz_run_check.py`。lang 文件**都是纯 LF**，python 改文案须 `io.open(..., newline='')`。
- 源码：MC `~/.gradle/caches/forge_gradle/.../1.20.1-47.4.10_mapped_official_1.20.1/*-sources.jar`（含补丁）；**EF 在 `C:\D\MineMOD\epicfight-1.20.1\`**（改 EF 先读源码别猜）。⚠️ 报 maven 证书失败**别折腾证书 / JDK**，`gradle.properties` 的 `-Dnet.minecraftforge.gradle.check.certs=false` **别删**。
- 看第三方模组真实行为：**没有源码 jar 就 javap 反汇编**（`$JDK/javap -p -c -v X.class`），注解与注册点一目了然。范式：`git-bash` 下先 python 从缓存 jar 里抽 class 到 `%TEMP%`，再 javap。跳字模组 = `~/.gradle/caches/forge_gradle/deobf_dependencies/.../stylizeddamage-1201.2.10*.jar`。

## 配置系统（2026-10-03 重构：**自写配置层，Forge 配置系统已彻底摘除**）
- ⛔ `JuJutsuKaisenZone` 里**不许**再加回 `context.registerConfig(...)`，也不要加 `ConfigScreenHandler` 扩展点。原因 = Forge `ConfigWatcher` 启动竞态：`ConfigTracker` 加载后立刻 `save()`，而 `Files.newOutputStream` **先把文件截断成 0 字节**再写、非原子，异步 watcher 可能读到截断/半写文件 ⇒ 解析失败 ⇒ 备份 + 用默认值重写 ⇒ **改过的值随机被重置**。本机铁证：`run/config/jujutsukaisenzone-common-1.toml.bak` 是 **0 字节**（备份的正是空文件）。修复在上游 NightConfig **3.7.0**（重写 FileConfig + 原子替换），本项目解析到 **3.6.4**（未修）；`fml.toml` 的 `disableConfigWatcher` 是 **NeoForge 专有**键，1.20.1 Forge 的 fmlcore 里没有（javap 确认 0 命中）。⇒ 结论：不用它，自己写。
- ✅ 现在的两层结构（新文件 2 个，`config/` 包）：
  - `config/CfgSpec.java`（568 行）：**取代 `ForgeConfigSpec` 的值容器**。刻意照抄 Forge 的签名（`define` / `defineInRange` / `defineListAllowEmpty` / `build`），所以 `Config.java` 的 **98 条定义链一字未改**，只换了类型名。`ConfigValue.get()` = **生效值**（可能被覆盖表改过），`getDefault()` = **工厂默认值**（只给模板 / 自查用）。⛔ 我们的 `get()` **没有加载状态依赖**，与 Forge 的 `get()`（未加载抛 `IllegalStateException`）是两回事 —— 这是 `loadDefaults()` 能保住原样 `.get()` 写法的原因。
  - `config/ConfigFileLayer.java`（231 行）：**全模组唯一碰配置文件的代码**。读一次、**无 watcher**、**文件已存在就永不重写**。
- ✅ 玩家覆盖表：`config/jujutsukaisenzone/settings.toml`，扁平 `dotted.key = value`。**只开放数字**；⛔ **布尔开关与列表不开放**（会改玩法契约）。越界 → 夹回范围内 + 警告；解析失败 → 用默认值；**一切失败只警告，绝不抛异常**。模板由代码默认值生成，且**每一行都注释掉**（`# path = value`）⇒ 文件里不存在第二份默认值。首次启动文件不存在时自动生成模板。
- ⚠️ **改数值**：模组作者改 `Config.java` 的 `defineInRange` 默认值；高级用户改 `settings.toml` 对应行（去掉注释）。两者都要**重启游戏**。（没有游戏内配置界面。）
- ✅ **实机已验（2026-10-03）**：首启自动生成 `run/config/jujutsukaisenzone/settings.toml`（299 行全注释 / 纯 LF）；端到端覆盖链（文件 → 解析 → spec 生效值 → `Config` 静态字段）四段全通，用 harness 在**脱离游戏**的情况下跑过（见 `REFERENCE.md#脱离游戏跑真实逻辑的 harness 范式`）。当前刻意留了一条测试覆盖 `damageVariance.spread = 0.35` 在文件里，测完把行首 `#` 加回去即可。
- ✅ **联机唯一真相（2026-10-03）**：联机时**以服务端（房主 / 服主）的配置为准**，由 `SyncModConfigS2CPacket`（包 ID **105**）在玩家登录时把服务端 `Config.exportServerOverrides()` 推给客户端，客户端 `Config.applyServerOverrides()` **整个盖掉本地**。客户端唯一读本模组 `Config` 的地方其实是 0 处（`client/` 下只有一个**未使用的残留 import**），所以玩法本来就是服端算的；这套同步是为了让「客户端静态字段 == 服务端」这条不变量真正成立、且将来加客户端读取时不会分叉。
  - ⛔ **单机 / 局域网房主跳过套用**（`Minecraft#hasSingleplayerServer()`）：同一个 JVM，静态字段本来就是同一份，再盖一次只是白跑 `loadDefaults()`。
  - ⛔ **断开必须还原**：`ClientConfigSyncHandler` 在 `ClientPlayerNetworkEvent.LoggingOut` 调 `Config.reloadFromLocalFile()`。**不还原就是真 bug** —— `Config` 的静态初始化一个 JVM 只跑一次，「先连服务器、再进单人存档」会继续沿用服务端的值。
  - ⛔⛔ **`CfgSpec.apply(Map)` 是「按键改写」不是「整体替换」**：文件/表里没写的键会**保留原值**。所以 `ConfigFileLayer.apply()` 与 `Config.applyServerOverrides()` **都必须在套用前先 `resetOverridableToDefaults()`**。漏这一步 = 断开后重读本地文件时，88 项里没被写进文件的几十项继续沿用服务端值（模板默认几乎全是注释行）。**这条是 harness 抓出来的真 bug，别再犯。**
  - ⚠️ `ModNetwork.PROTOCOL_VERSION` 已从 `"1"` 升到 `"2"`：加包必须升，好让「客户端缺包」在连接阶段被干净拒掉，而不是登录后抛解码异常掉线。
- ⚠️ `Config.SPEC` 保留：`BUILDER.build()` 仍在类初始化时校验重复键（`配置键重复定义` 当场抛），且 `templateText()` 需要它。⛔ 别当死代码删。
- ⚠️ `SkillCombatConfigRegistry.reloadFromSpecDefaults()`（旧名 `reloadFromForgeConfig`）必须在 `loadDefaults()` **末尾**调用，否则丢 `combatPowerScalingEnabled` 的 perRank 归零 ⇒ 伤害双计。
- ⛔ `Config` 类尾 `static {}` **顺序不能反**：`ConfigFileLayer.apply(SPEC)` 在先，`loadDefaults()` 在后（先让覆盖表改掉 spec 生效值，再灌进字段）。
- ⚠️ `ConfigFileLayer.apply()` 整段包在 `catch (Throwable)` 里：脱离游戏启动（纯 `java` 跑 harness）时 `FMLPaths.get()` 返回 null，会打一行 `[配置] 覆盖表读取失败，全部使用代码默认值` 然后继续 —— 这是**设计内的降级**，不是 bug。
- ✅ 已删（2026-10-03，用户批准）：`client/gui/JjkzModConfigScreen` + `client/compat/ForgeConfigScreensIntegration`（后者所在 `client/compat/` 空目录一并 rmdir）。理由：`ConfigScreenHandler` 已删 ⇒ 模组列表不会再给「配置」按钮 ⇒ 无入口 ⇒ 死代码；且它对 `forgeconfigscreens` 的软联动是给**Forge 配置界面**用的，现在没有 Forge 配置了。⛔ **别再重建游戏内配置界面**：要改数值就让玩家手改 `settings.toml`（或直接用编辑器），不重开 GUI。

## 跨模块铁律
- ⛔ **用户口述文案逐字照写**：不改大小写 / 引号、不补字顺句、不点评不追问（见用户级 MEMORY.md）。
- ⛔ **一切伤害乘算只在 `TechniqueDamageZoneEvents` 里落地**，别处不许裸写 `setAmount`。优先级链勿动。**现行段序**：`0 强度换算 → 1 增伤 → C 暴击 → [LivingDamageEvent 区 2 防御] → 3 ZONE 减免 → 4 韧性减免 → V 浮动(±10%)`；区 V 排除处刑必杀。加新乘区前先读那个类的注释。
- ⛔ **事件阶段**：`LivingHurtEvent` 在**原版护甲之前**；`LivingDamageEvent` 在护甲 / 附魔 / 抗性 / 黄心**之后**。
- ⛔ **Forge 对同优先级监听器不排序**：底层 `ListenerList.register` 只做追加 ⇒ **执行顺序 = 注册顺序**。`@SubscribeEvent` 自动订阅发生在 **mod 构造期**，比任何模组的 `commonSetup` 都**早**；要「跑在别的模组之后」只能等 **`FMLLoadCompleteEvent`**（所有 `commonSetup` 含 `enqueueWork` 回调都跑完才触发）手动 `MinecraftForge.EVENT_BUS.addListener(priority, receiveCancelled, EventType.class, handler)`。范式 = `TechniqueDamageSources.LateListenerRegistration`。**暴击 `.crit` 后缀就是被这条坑掉的**：我们的清理也是 LOWEST，却比跳字模组的读取端先跑 ⇒ 后缀在读取前被撤掉 ⇒ 任何暴击率下都不显示。
- ⛔ `@SubscribeEvent` 的 `receiveCanceled` **默认 false**（`ASMEventHandler.invoke` 里 `!event.isCancelable() || !event.isCanceled() || subInfo.receiveCanceled()`）⇒ 依赖 `event.isCanceled()` 的方法默认是**死代码**。手动注册时把 `receiveCancelled` 传 `true`。
- ⚠️ **技能描述必须写「玩家当前做得到的事」**，被门控内容整段消失。走 `TechniqueSkillDescriptionRegistry#registerVariant`，**`.brief` 简洁页也要换**；zh_cn / en_us **双侧都写**。
- ⚠️ 术式伤害走 `TechniqueDamageSources.deal`（否则无归因）；但**判据是「有没有归因」**——`thrown(direct, owner)` 本就通，硬改 deal 是**回归**。
- ⚠️ 新增持久化成长数据必须接进 `SkillUnlockLogic.copyProgressionFrom` + `syncToClient`。
- ⚠️ **伤害分型按叙事**（术式 / 体术 / 复合）；**别**在 `TechniqueSkill` 加分型字段、别在悬停标题栏写「伤害类型」。
- ⛔ **暴击**：体术默认放行、术式**保守默认拒绝**，每个术式招式由出伤点显式 `dealCrit` / `runWithTypeCrit` 放行（已放行：解/苍/捌/茈/超远程茈/灶开 + 赫/法槌/星痕/黑闪冲击波）。闸门 `applyCritZone` 必须在 `consumeForced` **之前**。
- ⛔ **「出伤频率高」不是拒绝暴击的理由**：无敌帧在 `hurt` 里、`actuallyHurt` 之前，而 `LivingHurtEvent`（暴击乘区）只在 `actuallyHurt` 内派发 ⇒ 真实结算最快 10 tick 一次。⚠️ `bypasses_cooldown` 会让结论失效。判据见 `CritSkillPolicy` 类文档。
- ⛔ **`CritRules.forceNext` 是玩家级待消费标记**：注入前必须先过 `critAllowed`（注入与消费同击），否则本招式的固定倍率会送给别的招式。`CritSkillPolicy#validate` 是**单向**检查，改运行时放行须人工同步展示名单。
- ⚠️ **暴击跳字只能靠「`.crit` message_id」**（生产 `TechniqueDamageSources#markCritDisplay` → 消费 `StylizedDamageCompat` 的 `.match(MSG_*_CRIT)`）：对方样式的 `critical` 伪选择器吃的是**原版摔落暴击**，本模组暴击影响不到。⛔ 判据只能是 `StylizedDamagePresence.isLoaded()`，**别挂 `ModRules.StylizedDamageStyles` 开关**（开关关时 `deal` 走普通 `DamageSource`，msgId 来自数据包注册表仍是 `jjkz.technique` ⇒ 开关对术式隐形，只有暴击后缀会静默消失）。⛔ `NarrativeDamageSource` 覆写了 `getMsgId()` ⇒ **完全遮住 `DamageSourceMixin`**，叠加层必须在那覆写里读。⛔ **后缀是纯字体渲染**：💥 在 SMP 无字形、原版缺字兜底是**方框**，现用 `✸` U+2738（改 `CRIT_SUFFIX` 一个常量即可换）。⛔ 客户端对未匹配的 msgId **整条丢弃**（不是少个后缀），加 `message_id` 必须同步加样式规则。
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
