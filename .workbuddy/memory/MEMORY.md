# 项目长期备忘（JujutsuKaisenZone / Forge 1.20.1）

> 只放**铁律速查**；原理 / 代码位置 / 踩坑过程全在 `REFERENCE.md`，逐日过程在 `YYYY-MM-DD.md`。

## 环境 / 工具
- ⛔ **禁跑 `./gradlew`**（含 compileJava）：锁 `build/`、憋输出、能卡 10min~1h。编译由用户在 IDEA 做，AI 只做静态自查。
- ✅ **静态自查 = 全工程 javac**：**直接跑** `tools/jjkz_static_check.py`（JDK bin 先入 PATH），产物落 `%TEMP%/jjkz_check_out`，与 `build/` 无关 ⇒ 不怕游戏文件锁。**基线 2026-10-07 = 834 文件 / 294 jar / exit 0 / 0 错误 / 配置常量旁路 0**。
- ⛔⛔ **别用 `%TEMP%/jjkz_run_check.py` 那个 wrapper**：它把 stdout 重定向进 StringIO 再落盘，**会谎报 `exit = 0 / 0 错误`**（工程里塞 3 个编译错误它照样全绿）。脚本本身可靠（丢一个故意写错的 `__Probe.java` 进去，exit=1 且报出文件名）⇒ 怀疑「检查是不是真跑了」就用这个探针法验。
- ⛔ **游戏客户端开着时 IDEA 绝不能构建 / 重跑 Client**：JVM 占住 `build/classes/java/main` ⇒ Gradle 前置清理删一半 ⇒ 产物残缺，游戏侧只报「找不到某个类」/ Mixin `ClassMetadataNotFoundException`，**症状与真因完全不像**。修复 = 关游戏 → 删 `build/classes/java/main` 重建。排查顺序：`idea.log`(Gradle 真错) > `run/logs/latest.log` > `run/crash-reports/`（其 "Suspected Mod" 会误导）。
- JDK bin：`C:\Users\19316\.gradle\jdks\eclipse_adoptium-17-amd64-windows\jdk-17.0.20.1+1\bin\`。javac 报错是 **GBK**（grep 中文静默失败）；无 classpath 时「方法不会覆盖或实现超类型的方法」是**误报**。
- ⛔ **同一文件多处 Edit 必须串行**（并行会静默丢改动，工具却报成功）⇒ 改完 grep 复核每一处调用点。EBUSY = 被占用，重试。
- 环境坑：别 `cd` 项目目录；Bash 缺 coreutils ⇒ **删 / 移文件用 Python `os.remove`**；PowerShell stdout 不可见；给 Windows exe 传路径写 `C:/...`；⛔ PowerShell 读写 UTF-8 中文须显式 `New-Object System.Text.UTF8Encoding $false`，别用 `*>` 接子进程中文输出（花码）；lang 文件**纯 LF**，python 改文案须 `io.open(..., newline='')`。
- 源码：MC sources jar 在缓存同名目录；**EF 源码在 `C:\D\MineMOD\epicfight-1.20.1\`**（改 EF 先读源码）。无源码 jar 就 `javap -p -c -v` 反汇编（跳字模组 = `.../stylizeddamage-1201.2.10*.jar`）。⚠️ 报 maven 证书失败**别折腾证书 / JDK**，`gradle.properties` 的 `-Dnet.minecraftforge.gradle.check.certs=false` **别删**。

## 配置系统（2026-10-03 重构：**自写配置层，Forge 配置系统已彻底摘除**）
- ⛔ **不许**加回 `context.registerConfig(...)`、`ConfigScreenHandler`，⛔ **别再建游戏内配置界面**（根因 = Forge `ConfigWatcher` 非原子写竞争，铁证 / 推演见 `REFERENCE.md#配置来源与权威规则`）。
- 结构：`config/CfgSpec.java`（**取代 `ForgeConfigSpec`** 的值容器；刻意照抄 Forge 签名 ⇒ `Config.java` 98 条定义链一字未改。`get()` = **生效值**、`getDefault()` = 工厂默认值、**没有加载状态依赖**）+ `config/ConfigFileLayer.java`（**全模组唯一碰配置文件的代码**：读一次、**无 watcher**、文件已存在就永不重写）。
- 玩家覆盖表 `config/jujutsukaisenzone/settings.toml`：扁平 `dotted.key = value`，**只开放数字**（布尔与列表不开放，会改玩法契约）。越界夹回 + 警告、解析失败用默认值、**一切失败只警告绝不抛异常**；模板由代码默认值生成且**每行都注释掉**，首次启动自动生成。改数值（作者改 `Config.java` 默认值 / 玩家改 toml）都要**重启游戏**。
- ⛔ `Config` 类尾 `static {}` **顺序不能反**：`ConfigFileLayer.apply(SPEC)` 在先，`loadDefaults()` 在后。
- ⛔⛔ `CfgSpec.apply(Map)` 是「**按键改写**」不是整体替换 ⇒ `ConfigFileLayer.apply()` 与 `Config.applyServerOverrides()` **套用前都必须先 `resetOverridableToDefaults()`**（harness 抓出的真 bug）。
- ⛔ `SkillCombatConfigRegistry.reloadFromSpecDefaults()` 必须在 `loadDefaults()` **末尾**调用，否则丢 `combatPowerScalingEnabled` 的 perRank 归零 ⇒ 伤害双计。
- ⚠️ `Config.SPEC` 别当死代码删（`BUILDER.build()` 校验重复键 + `templateText()` 需要）。
- ✅ 联机唯一真相：**以服务端配置为准** —— `SyncModConfigS2CPacket`（包 ID **105**）在登录时下发 `exportServerOverrides()`，客户端 `applyServerOverrides()` 整个盖掉本地；⛔ 单机 / 局域网房主跳过（同一 JVM，静态字段本就同一份）；⛔ 断开必须由 `ClientConfigSyncHandler` 调 `reloadFromLocalFile()` 还原。
- ⚠️ 加包必须升 `ModNetwork.PROTOCOL_VERSION`（现 `"10"`；注释区逐版记了每个号新增/移除了哪些包 ID），让「客户端缺包」在连接阶段被干净拒掉。

## 跨模块铁律
- ⛔ **用户口述文案逐字照写**：不改大小写 / 引号、不补字顺句、不点评不追问（详见用户级 MEMORY.md）。
- ⛔⛔⛔ **不准违背指令，不准自作聪明**（2026-10-05 立，血债。本项目**最重**一条，跨所有任务）：
  - **指令是边界，不是参考**：用户说做什么就做什么，说到哪做到哪。没让做的**一律不做**；让做的**不打折、不夹带、不扩写**。拿不准 → 问，别猜。
  - **禁止用「我觉得 / 应该 / 更好 / 顺便 / 兜底 / 更完整」当作行动依据**。这些都不是用户的指令。动机说明（为什么做）只用来理解，**不当任务**。
  - **改名 / 换实现 / 挪调用点都是新行为**：哪怕方法名不变、调用点不动，只要副作用变了，就等同于一次未经要求的改动 ⇒ 改前必须重新确认「用户要过这个副作用吗」。
  - **不许拍脑袋定参数 / 时机 / 阈值 / 生命周期**：用户没明说的，去问，或交给既有规则，不许自己发明。
  - 反面教材（2026-10-05）：1.3 演出里我把「拆旧假球」的 `dismantleDomain` 实现体私自换成 `closeDomain + gojo.discard()`、挂在自拍的 `DOMAIN_HOLD_TICKS = 200` 到点处 ⇒ 领域「开一半就没了」、五条悟被抹掉。用户从未要求，纯属自作聪明，白耗半天。（修复 = 整块删除 `tickDomain`/`tickDone`/`dismantleDomain`/`Phase.DONE`。）
  - ⛔ **半成品不许替它收尾**（2026-10-05 立）：用户明说「这只是半成品」的东西，**停在当前进度即可**，别自作主张给演出 / 流程写「收尾、清场、交还控制」。1.3 演出不止 7 个阶段，阶段 7 只是当前终点 ⇒ 停驻，不做任何收尾。
- ⛔ **一切伤害乘算只在 `TechniqueDamageZoneEvents` 里落地**，别处不许裸写 `setAmount`。**现行段序**：`0 强度换算 → 1 增伤 → C 暴击 → [LivingDamageEvent 区 2 防御] → 3 ZONE 减免 → 4 韧性减免 → V 浮动(±10%)`；区 V 排除处刑必杀。⚠️ **区 V 默认开启**（`damageVariance.enabled=true`，`spread=0.10`）—— 想把伤害调成固定值去验别的机制时先记得它开着。
- ⛔ **事件阶段**：`LivingHurtEvent` 在**原版护甲之前**；`LivingDamageEvent` 在护甲 / 附魔 / 抗性 / 黄心**之后**。
- ⛔ **Forge 对同优先级监听器不排序**：执行顺序 = 注册顺序；`@SubscribeEvent` 自动订阅发生在 **mod 构造期**（比任何 `commonSetup` 都早）⇒ 要「跑在别的模组之后」只能等 **`FMLLoadCompleteEvent`** 手动 `EVENT_BUS.addListener(priority, receiveCancelled, EventType.class, handler)`（范式 `TechniqueDamageSources.LateListenerRegistration`）。**暴击 `.crit` 后缀就是被这条坑掉的**。
- ⛔ `@SubscribeEvent` 的 `receiveCanceled` **默认 false** ⇒ 依赖 `event.isCanceled()` 的方法默认是**死代码**；手动注册传 `true`。
- ⛔⛔ **CMDCam 运镜期间 `options.hideGui = true` ⇒ `Gui.render` 被跳过 ⇒ Forge `RenderGuiEvent` 整段不派发**（已 `javap` 扫 cmdcam jar 求证：`CMDCamClient` / `CamRun` 里就有 `hideGui`）。⇒ **任何要盖在运镜画面上的整屏效果（眼皮 / 黑幕 / 字幕 / 闪光）一律走 `TickEvent.RenderTickEvent` END + 自建 `GuiGraphics`**（backupProjectionMatrix → setOrtho(0,w,h,0,1000,3000) → modelView setIdentity + translate(0,0,1000-3000) → `new GuiGraphics(mc, mc.renderBuffers().bufferSource())` → flush → restore）。范式照抄 `QuestCinematicLetterboxOverlay`（`QuestMissionBriefingOverlay` / `MusicNowPlayingToast` 同）。反面教材：1.3「醒来眨眼」`QuestWakeBlinkOverlay` 原本挂 `RenderGuiEvent.Post`，与背景镜同拍启动 ⇒ 4 秒眼皮一帧都没画。
- ⚠️ **技能描述必须写「玩家当前做得到的事」**；走 `TechniqueSkillDescriptionRegistry#registerVariant`，**`.brief` 简洁页也要换**，zh_cn / en_us **双侧都写**。
- ⚠️ 术式伤害走 `TechniqueDamageSources.deal`（否则无归因）；但**判据是「有没有归因」**——`thrown(direct, owner)` 本就通，硬改 deal 是**回归**。
- ⚠️ 新增持久化成长数据必须接进 `SkillUnlockLogic.copyProgressionFrom` + `syncToClient`。
- ⚠️ **伤害分型按叙事**（术式 / 体术 / 复合）；**别**在 `TechniqueSkill` 加分型字段、别在悬停标题栏写「伤害类型」。
- ⛔ **暴击**：体术默认放行、术式**保守默认拒绝**，每个术式招式由出伤点显式 `dealCrit` / `runWithTypeCrit` 放行（已放行：解 / 苍 / 捌 / 茈 / 超远程茈 / 灶开 + 赫 / 法槌 / 星痕 / 黑闪冲击波）。闸门 `applyCritZone` 必须在 `consumeForced` **之前**。
- ⛔ **「出伤频率高」不是拒绝暴击的理由**：`LivingHurtEvent`（暴击乘区）只在 `actuallyHurt` 内派发 ⇒ 真实结算最快 10 tick 一次。⚠️ `bypasses_cooldown` 会让结论失效。判据见 `CritSkillPolicy` 类文档。
- ⛔ **`CritRules.forceNext` 是玩家级待消费标记** ⇒ 注入前必须先过 `critAllowed`；`CritSkillPolicy#validate` 是**单向**检查，改运行时放行须人工同步展示名单。
- ⚠️ **暴击跳字只能靠「`.crit` message_id」**（`TechniqueDamageSources#markCritDisplay` → `StylizedDamageCompat.match`）：对方样式的 `critical` 伪选择器吃的是**原版摔落暴击**。⛔ 判据只能是 `StylizedDamagePresence.isLoaded()`，**别挂 `ModRules.StylizedDamageStyles`**。⛔ `NarrativeDamageSource` 覆写了 `getMsgId()` ⇒ **完全遮住 `DamageSourceMixin`**，叠加层必须在那覆写里读。⛔ 后缀是纯字体渲染 ⇒ 用 `✸` U+2738（改 `CRIT_SUFFIX` 常量）。⛔ 客户端对未匹配 msgId **整条丢弃** ⇒ 加 `message_id` 必须同步加样式规则。
- ⚠️ 技能树四种节点 `skill / specialMove / reference / info`。⛔ **领域节点是 `skill` 节点，不是说明节点**（2026-10-03 起，**全员统一**）：它是机制型技能（`shrine_domain` / `limitless_domain` / `judgment_domain` / `love_rendezvous_domain`），靠 `.conditions(hasDomainExpansion())` + `.autoUnlockWhenConditionsMet()` 在习得领域展开的**瞬间自动点亮**——不花术式情报、**解锁状态也不写存档**（每次实时判条件）。⛔ 之所以必须是 skill 节点：`SkillTreeNodeDef#isConnectableEdge` 让说明节点不参与任何连线与解锁判定，**挂不住瞬发 / 开放式这两个前置**。
- ⚠️ **领域系判据只有 `SkillTreeNodeDef#isDomainNode()`**（看两份条件表里有没有「已习得领域展开」，不认节点种类也不认 id）。悬停框据此在标题下打红色「领域展开」标注，**skill 与 info 两个分支都要打**：skill 支覆盖领域本体 / 瞬发 / 开放式，info 支留给「上诉」这类只讲领域规则的说明节点。
- ⚠️ 字符串声明表要配启动自检（`CritSkillPolicy#validate()` / `SeEffectRegistry#validate()`），必须在技能集注册**之后**调用。
- ⛔ **skillCombat 数值只许写一次**，业务代码只走 `*SkillGrowth` 解析（守卫与 `jjkz-config-default-ok` 豁免见 `REFERENCE.md#配置来源与权威规则`）。
- ⛔ 服务端不能用 `deltaMovement` 移动玩家 ⇒ 每 tick `teleportTo`；`lookAt` 必须在 teleport **之前**。玩家位移态用静态内存 Map + deadline，别落盘。
- ⛔ 可解锁机制只能是「**能力型**」，不能是「**约束型**」。
- ⛔ **强度换算只在 `TechniqueDamageSources.deal` 内做一次**；出伤点再乘即双计。黑闪 `pow(x, 2.5)` 必须拿**放大前**的原始值取幂。
- ⛔ `CursedGradeBonuses` 不许再给 `ATTACK_DAMAGE` 加等级值。
- ⛔ **技能演出默认不锁操作**：前摇只用「延后成型」制造时间成本。
- ⚠️ **领域瞬发（`INSTANT`）**：三开关 `setSkyboxOnlyExpand` / `setAutoCloseOnComplete`（保持 40 tick）/ `setCustomSkyboxTexture`；批量 640；过半回调 `onInstantDomainHalfProgress`。模式**通用**（`PlayerDomainModeData`），每档可用性 = 当前术式对应的机制型技能是否解锁。细节见 `REFERENCE.md#领域展开`。

## 特效 Behavior 层（`entity/behavior/*`）
- 结构 = `EffectBehavior`（`tick` / `onDiscard` / NBT + 静态工厂 `createFromId`）由 `SpecialEffectEntity` 委托，**只在服务端 tick**。骨架是好的策略模式，**别推翻**。
- ⛔ **坏味道是「机制级复制」不是「一技能一类」**：线性飞行 / 柱球清场 / Photon 挂载 / 定时 被逐字抄进多个 Behavior。新技能**优先复用** `entity/util/{AreaShape, BlockErase, SpecialEffectFlight, SpecialEffectChunkForce}`。
- ✅ **P0–P5 已落地（2026-10-03）**：删死代码 `ShrineKaiFlyobjBehavior`；`AreaShape` + `BlockErase` 替换 4 处手写三重循环；飞行循环并入 `SpecialEffectFlight.tickLinear`（旧 NBT 键 `FlightDirX/Y/Z` 弃用）；行为层 10 处 Photon 挂载全走 `PhotonFxCommands.perform`；`createFromId` 的 switch 换成注册表；宿主 12 个 `configureXxx` 删除，只留 `attach(EffectBehavior)`。
- ⛔ **新技能写法固定为 `new XxxBehavior()` → `configure(...)` → `fx.attach(b)`**，别再加宿主包装方法。
- ⚠️ **`CompositeBehavior` 模块框架没做**（零消费者的空壳，要上必须先有具体案例）。

## 其他子系统（细节全在 `REFERENCE.md`）
- **SE 术式侵入 / 统一质量池**：质量 = Σ SP 节点 spCost + Σ SE quality；超上限才吃超载罚，越「上限 + 宽限」拒装。装载五道闸见 `PlayerSeLoadoutData.tryEquip`（顺序即错误优先级）。⛔ SE 惩罚只削数值不碰身份；面板强度行惩罚必须加在 `PlayerCombatPowerData.resolveServer` **最后**。`CursedEnergyTrait` 只剩 SE 装载门卡一处用途，⛔ 特质产生规则 / 被动**别顺手扩**。
- **怪物韧性条**（载体 = EF `stun shield`）：⛔ EF 只在 `EpicFightDamageSource` 时碰盾 ⇒ 破韧自写，`MobPoiseEvents#onLivingHurt` @LOWEST，固定扣 `PoiseRules.POISE_DAMAGE_BASE`。⛔ 末影龙在 `applyPoise` / `onLivingHurt` / `onLivingTick` **三处都豁免**。⛔ 特质克制表 + 破韧质量闸门已删。⛔ 监守者 / 潜影贝 / 远古守卫者暂无条。
- **进程级静态会话状态**：领域 / 结界活跃实例由静态单例持有，退出世界不重载类 ⇒ 跨会话残留。统一兜底 `event/SessionStateCleanupEvents`。⛔ 新增任何「进程级静态会话容器」都要挂进这里，别零散 `.clear()`。
- ⛔ **领域实现必须有 caster 版 tick，否则 AI 施法者「演出哑火」**（2026-10-05 立）：`DomainCaster` 双路径（`PlayerCaster` / `EntityCaster`）下，`AbstractDomainExpansion#tickMaintain(DomainCaster)` 只跑通用咒力/崩解判定 —— **一切「某刻触发」的演出（延迟换天空盒 / 水墨 / 闪光 / 音效）都必须子类自己覆写 `tickMaintain(DomainCaster)` 去驱动**。只覆写 `tickMaintain(ServerPlayer)` 的领域，AI 施法者永远等不到这些压轴。同理 `cast(DomainCaster)` 是 AI 唯一施法入口，玩家版 `cast(ServerPlayer)` 里的开场演出（蓄力白幕 / 展开音效）在 AI 侧**不存在**，需要就在 `applyDomainSpecificEffect(DomainCaster)` AI 分支补。
- ⚠️ **1.2 鬼打墙 `MainQuest12LoopTrap`**：轨迹**只在玩家移动时**记点（`SAMPLE_MIN_MOVE = 1.0`），触发前再过 `MIN_REWIND_DISTANCE = 4.0` 距离闸门（不够就**延后且不消耗次数**）。触发是**固定编排** 1~2 次（`600` / `1500` 刻），已无概率。⛔ 反例教训：按**时间**采样的轨迹，玩家静止 >6 秒会被脚下同一坐标填满 ⇒ 折返 = 原地空转。
- ⛔ **对领域施法者动 `setNoAi` 只能在 `cast()` 之后**：`beginAiCasterPoseLock` 在 cast 当拍读 `isNoAi()` 存为 `originNoAi`、释放时写回 ⇒ 若在 cast **之前**先 `setNoAi(true)`，领域最终会把 AI 恢复成「关」，**永久不开**。（1.3 登场锁 `tickGojoSwapLock` 因「生成后下一 tick 才设」天然满足；`abort()` 里补了放行。）
- **客户端后处理**：`PostChain.passes` 是 private ⇒ 用 `addPass` 挂（**必须在 `resize()` 之前**）；`PostPass` 不下发 `ModelViewMat`；深度走 `depthTarget.copyDepthFrom` + `pass.addAuxAsset`；注入点 `RenderLevelStageEvent.Stage.AFTER_LEVEL`。⛔ **「以某个实体为中心」的后期效果，球心必须每帧跟踪实体**，不能只在触发瞬间发一次快照。
- **音频 / 版权（2026-10-03 立）**：第三方版权音频（动画官方曲 / 手游音效 / 声优语音）**不随 jar 分发**，统一走**资源包接口**——保留 `ModSounds` 注册 + `sounds.json` 条目 + 触发点的 `playSound`，只把 ogg 从 `assets/.../sounds/**` 删掉；资源包放同名 ogg 即自动生效，缺失仅日志告警不崩。⛔ 新增音频先判来源，官方素材一律不进 jar。**资源披露文档 = 仓库根 `ASSETS.md`**（中文 + 英文摘要）。
