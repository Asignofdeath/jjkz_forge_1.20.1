# 项目长期备忘（JujutsuKaisenZone / Forge 1.20.1）

> 本文件只放**铁律速查**。子系统细节见同目录 `REFERENCE.md`；逐日过程见 `2026-09-2*.md` 日志。

## 环境 / 工具
- ⛔ **禁跑 `./gradlew`（含 compileJava）**：游戏/IDEA 锁 `build/`、管道憋输出，能卡 10min~1h。编译交用户在 IDEA 做；AI 只做静态自查。长任务别接 `| tail`。
- ✅ **静态自查最硬形式 = 全工程 `javac`**：直接跑 `tools/jjkz_static_check.py`（先把 JDK bin 加进 PATH）。0 错误才敢说「已落地」。基线 **578 文件 / exit 0**。原理见 `REFERENCE.md`。
- ⛔ javac 报错是 **GBK**：grep 中文关键字会静默失败，要 `decode('gbk')` 后统计。无 classpath 时「方法不会覆盖或实现超类型的方法」是**误报**。
- JDK：`C:\Users\19316\.gradle\jdks\eclipse_adoptium-17-amd64-windows\jdk-17.0.20.1+1\bin\`（PATH 里没有 javac/javap）。
- ⛔ **同一文件多处 Edit 必须串行**；同文件 Edit 会**静默丢改动**（工具报成功，只在用户编译时暴露）⇒ 改完 grep 复核每一处调用点。EBUSY = 被占用，重试即可。
- 别 `cd` 项目目录；Bash 缺 coreutils（连 `ls` 都没有）；PowerShell stdout 不可见；给 Windows exe 传路径写 `C:/...`（`/c/...` 被当相对路径且不报错）。裸 python 无 PIL/numpy。
- MC 源码：`~/.gradle/caches/forge_gradle/minecraft_user_repo/net/minecraftforge/forge/1.20.1-47.4.10_mapped_official_1.20.1/*-sources.jar`（含补丁）；`decompile/output.jar` 未打补丁别用。**EF 源码在 `C:\D\MineMOD\epicfight-1.20.1\`**（改 EF 相关先读源码，别猜）。
- ⚠️ 报 `maven.minecraftforge.net` 证书失败**别折腾证书/JDK/信任库**：是 ForgeGradle `EnvironmentChecks#testServerConnection` 无重试把网络抖动误报。`gradle.properties` 里 `-Dnet.minecraftforge.gradle.check.certs=false` **别删**。
- ⛔ **PowerShell 改 UTF-8 中文文件必须显式编码**：`[IO.File]::ReadAllText/WriteAllText` 默认系统 ANSI(GBK)，会读坏再写坏；要传 `New-Object System.Text.UTF8Encoding $false`。
- ⛔ **别用 PowerShell 的重定向接子进程中文输出**：`*>` 会把它转成 UTF-16，且中文再经一次编码误判 ⇒ 花码；结果文件 Read 时会直接判为「二进制文件」读不出来。**正确做法**：写个小 wrapper，用 `sys.stdout = io.StringIO()` 抓住脚本输出，跑完自己 `io.open(path, 'w', encoding='utf-8', newline='')` 落盘，再正常 Read。现成范式见 `C:\Users\19316\AppData\Local\Temp\jjkz_run_check.py`（跑 `tools/jjkz_static_check.py` 用）。
- ⛔ **Bash 基本废了**：`rm` 也走 shim，缺 `dirname` ⇒ exit 127。删/移文件一律用 Python（`os.remove`）。
- lang 文件**都是纯 LF**（zh_cn / en_us）；python 批量改文案必须 `io.open(..., newline='')` 读、按原风格写回。现成脚本 `tools/add_no_crit_tag_lang.py`、`tools/se_trait_lang.py`。

## 跨模块铁律
- ⛔ **用户口述文案逐字照写**：不改大小写/引号、不补字顺句、不点评不追问（见用户级 MEMORY.md）。
- ⛔ **一切伤害乘算只在 `TechniqueDamageZoneEvents` 里落地**（增伤/减伤/易伤/标签抗性…），**别在别的监听器裸写 `setAmount`**。优先级链勿动：处刑人 HIGHEST（改写）→ 管线 HIGH → 罪恶者/黑闪/打断 NORMAL。⚠️ 加新乘区前**先读那个类的注释**（区序与约束都写在那儿）。
- ⛔ **事件阶段口径**：`LivingHurtEvent` 在**原版护甲之前**；`LivingDamageEvent` 在护甲/附魔/抗性/黄心**之后**（所以区 2 目标防御层挂后者才能对总减伤封顶）。
- ⚠️ **技能描述必须写「玩家当前做得到的事」**：被门控掉的内容要整段消失，只留「解锁 X 后会怎样」（对照「三式法槌」的写法）。走 `TechniqueSkillDescriptionRegistry#registerVariant`，**`.brief` 简洁页也要换**（它是悬停框默认视图）；后缀键 zh_cn 与 en_us **双侧都写**，缺一侧直接显示键名。
- ⚠️ **术式伤害走 `TechniqueDamageSources.deal`**（否则无归因：不给经验/不掉情报/不记罪恶者/黑闪击杀检测失效）。但**判据是「有没有归因」不是「有没有走 deal」**——`thrown(direct, owner)` 归因本就通；硬改走 deal 是**回归**（deal 把 `directEntity` 填 null ⇒ 盾挡不住 + 打掉依赖 `getDirectEntity()` 的判定 + 物理伤害变无视护甲）。
- ⚠️ **新增持久化成长数据必须接进 `SkillUnlockLogic.copyProgressionFrom`**（跨死亡保留）+ `syncToClient`（客户端开发树读的是客户端 persistentData）。
- ⚠️ **伤害分型按叙事、不按通道**：术式 / 体术 / 复合（复合 = **同一次**叠加两者，如黑闪、处刑必杀）。挂法 `deal(..., type)` 或 `runWithType`。**不要**在 `TechniqueSkill` 上加分型字段、不要在悬停标题栏写「伤害类型」。
- ⛔ **暴击默认跟随分型**（体术可暴、术式不可暴），出伤点可双向覆盖：术式**单发**招式必须显式 `dealCrit` / `runWithTypeCrit`，**高频/周期出伤保持默认**。闸门 `CritRules.applyCritZone` **必须在 `consumeForced` 之前**（否则被拒的一击白吃十划弱点击破的强制暴击标记）。⚠️ 默认值**不能**改成「一律不可暴击」——EF 基础攻击/原版近战拿不到表态，会关掉现在唯一的暴击来源。连带：`HitCritState.critType()` 记分型 + `SeEffectEvents`/`BlackFlashEvents` 校验 `== MARTIAL`；名单在 `technique/crit/CritSkillPolicy`。
- ⚠️ **技能树四种节点**：`skill / specialMove / reference / info`。**领域展开是 `info` 说明节点**：它有 tooltip，在 `SkillTreeNodeWidget#refreshHoverTooltips` 的 `isInfoNode()` 分支**手搓**，**不走** `TechniqueSkillTooltipHelper`；无 skillId，只能按 `nodeId` 定位。判据只有 `SkillTreeNodeDef#isDomainInfoNode()`，**别各自再写一份**。
- ⚠️ **字符串声明表要配启动自检**（lang 键/技能 id/nodeId 全靠字符串，拼错是**静默失败**）。范式 `CritSkillPolicy#validate()` / `SeEffectRegistry.validate()`：必须在技能集注册**之后**调用。
- ⛔ **skillCombat 数值只许写一次，业务代码只许走 `*SkillGrowth` 解析**：常量（`entity/data/*Config`、`*SkillGrowth`，javadoc 打「【配置项·默认值】」）只是 Forge 配置项的默认值，`Config.java` 的 `defineInRange` 与 `SkillCombatConfigRegistry#buildCodeDefaults` 都**引用**它；⛔ 任何业务代码直接引用这些常量 = 同时丢掉配置修改与咒术等级成长（`TechniqueSkillDescriptions` 曾用常量拼伤害文案 ⇒ 玩家改 TOML 后文案与真实伤害对不上）。守卫在 `tools/jjkz_static_check.py` 尾部（`配置常量旁路(应为 0)`），确实只能读默认值的场合写 `jjkz-config-default-ok` 豁免 + 理由。细节见 `REFERENCE.md#配置来源与权威规则`。
- ⛔ **服务端不能用 `deltaMovement` 移动玩家**（客户端每 tick 覆盖）⇒ 每 tick `teleportTo(x,y,z)`；`lookAt(...)` 必须在 teleport **之前**。玩家位移态用静态内存 Map + deadline，别落盘。
- ⛔ **可解锁机制只能是「能力型」，不能是「约束型」**（衰减/芝诺墙/上限做成可解锁 ⇒ 解锁前反而更强，方向反了）。落地复用 `SKILL` 节点 + `getMechanicSkills()`，别新增节点种类。
- ⛔ **强度换算只在 `TechniqueDamageSources.deal` 内做一次**；出伤点再乘就是双计（例外：直连 `hurt` 不走 deal 的茈/远程茈/灶/法槌投掷）。EF / 原版近战由区 0 接管，靠 `POWER_ALREADY_SCALED` 识别。
- ⛔ `CursedGradeBonuses` **不许**再给 `ATTACK_DAMAGE` 加等级值（改与等级无关的 `ATTACK_BASE_UUID +1.0`）；等级成长全由强度面板承担。强化术的 +40% ATTACK 故意留在属性层。
- ⚠️ **黑闪 `pow(x, 2.5)` 必须拿「强度放大前」的原始值取幂**，再线性乘回（`pow(raw) × scaled/raw`）；直接对已放大值取幂 ⇒ 特级指数爆炸。

## SE 术式侵入 / 咒力特质 / 统一质量池（2026-09-27 落地）
- **质量 = Σ(已点亮 SP 节点的 spCost) + Σ(已装载 SE 的 quality)**，不是节点个数。上限 = 玩家数据 `spNodeQuality`（默认 6）；宽限 `Config.seQualityAllowance`（默认 2）；**超过上限才吃超载罚**（每点扣体术/术式强度各 `seOverloadPowerPenaltyPerPoint`），越过 上限+宽限 **直接拒装**。SP 点数（购买力）与质量（占位）是两套账。统一入口 `PlayerQualityPoolData`。
- **SE 装载五道闸**（`PlayerSeLoadoutData.tryEquip`，顺序即错误优先级）：已知 SE → 未重复 → **特质** → **专用术式**（`SeScope.matches`）→ 互斥组 → 背包有咒物 → 质量硬顶。UI 置灰判据必须同序，否则「能点但装不上」。
- ⛔ **SE 惩罚只能削数值、不能碰身份**：`PanelStat` 里**不许**出现特质等身份项。体术/术式强度走**面板负贡献行**，其余 7 条（抗性/穿透/反转效率/领域充能/暴击）走 **Attribute 层**（`PlayerSePenaltyApplier`，UUID 由 seId 派生、`recompute` 先全清再加；AttributeMap 不落盘 ⇒ 登录/重生靠 `CursedEnergyEvents.syncPlayerState` 重算）。
- ⛔ **面板强度行的惩罚必须加在 `PlayerCombatPowerData.resolveServer` 最后**：早了会被「十影·大蛇」倍率放大，或被天与咒缚分支 `technique.clear()` 抹掉。
- ⚠️ 本项目面板贡献行 label 是**硬编码中文**，不是 lang 键。`Component.translatable(key, args)` 的 args 传 **String 会被当字面量**，要传 `Component`。
- **咒力特质（`CursedEnergyTrait` 精/粗/属/常，玩家侧只存一个）现在只剩一处用途**：SE 装载门卡（`SeEffectDef#requiredTraits`）。~~怪物韧性条的克制表~~ 已于 2026-09-27 删除（见「怪物韧性条」小节）。⛔ 但**特质的产生规则 / 被动效果仍未实现**，用户说过那是后续铺垫，**别顺手扩**。
- 存档迁移：用户明确不要。旧 `seSlots` 丢弃，新 tag 是 `seEquipped`（集合）。

## 怪物韧性条（2026-09-27 起；阶段 0–5 已落地）
- **载体 = EF `stun shield`**：白条绘制与「盾>0 免硬直（霸体）」都是白送的；但 EF 的盾**只挡硬直不挡伤害** ⇒ 减伤要自己做，且必须是**独立乘区**（走 `resistanceReduction` 会被穿甲吃掉 = 吃穿甲，违反设计前提）。
- ⛔ **EF 只在伤害源是 `EpicFightDamageSource` 时才碰盾**（`EntityEvents:145` 取源、`:195` 要求非空）⇒ 本模组术式伤害走**原版 `DamageSource`**（`TechniqueDamageSources`），EF **整块跳过**；EF 自家近战也只扣 `impact`（默认 0.5）⇒ 铁傀儡 80 点要 160 刀。**破韧结算必须我们自己写**，不是「EF 会管」。
- **破韧口径**：每次玩家命中固定扣 `PoiseRules.POISE_DAMAGE_BASE`（10），**与本次伤害量无关**——破韧是独立于伤害的进度轴，挂上伤害换算等于堆伤害顺手把所有条秒掉。挂 `MobPoiseEvents#onLivingHurt` @ **LOWEST**（等区 4 改完 `amount` 再判落地；`LivingHurtEvent` 只在 `actuallyHurt` 内派发 ⇒ 自动滤掉无敌帧吞掉的重复命中）。回复走 `onLivingTick`（延迟 60t、每 20t 结算、`WeakHashMap<LivingEntity,Long>` 记命中时刻，随实体回收）。
- **破损态（阶段 4）**：判据 = 「这一击把**正数**的条打到 0」；窗口 10s，期内**禁止回复**（⛔ 回一点点 EF 霸体就提前回来，「移除霸体」就成假话：**霸体失效 ⟺ 盾为 0**）、虚弱 II + 体抗/术抗各 -25 点易伤（Attribute 层，专用 UUID `…-3005/3006`，**不能和 `MobCombatStatsEvents` 的 `…-3001~3004` 撞**）。到期恢复满上限并撤 debuff。⚠️ **盾 == 0 时 `HealthBar` 直接 return，白条是「消失」不是「空」** —— 所以破损期的「锁 30% 上限」没有视觉作用，只是兜底闸。
- ⛔ **末影龙在每个入口都要豁免**（`applyPoise` / `onLivingHurt` / `onLivingTick` 三处都查 `isEnderDragon`）：只跳过 `applyPoise` 不够——那只是不挂盾，扣盾那两处照样会去打 EF 的水晶盾并把它的上限锁到 30%。
- ⛔ **特质克制表已删除**（2026-09-27，用户拍板「Break 效力视为唯一解法」）。相克环、`poise.trait` 字段、`PoiseStats.trait` 分量、`counterMultiplier`/`beats`/`breakQualityFactor`/`COUNTER_*`/`BREAK_ONLY_PENALTY` 全部移除。**`PoiseStats` 现在只有 5 个分量**；`CursedEnergyTrait` 枚举保留，只服务 SE 装载门卡 `requiredTraits`。
- ⛔ **破韧质量闸门也一并删除**：它罚的是「用通用属性绕过特质」，特质没了就罚无对象，且 Break 是唯一解法还罚它 = 自相矛盾。⇒ **破损态恒满档**（虚弱 II + 体抗/术抗各 −25），与怎么破的无关。
- **Break 效力 = `ModAttributes.BREAK_POTENCY`**（玩家专属，百分点 0~150），原始比值由 `ModAttributes.breakPotency` 给；软上限折算 + 特攻怪打折在 `PoiseRules.effectiveBreakPotency`（现在只有 `poiseDamage` 一个调用点）。⚠️ **它目前没有任何获取来源** ⇒ 玩家恒 0，破韧速度恒基线（想让它有来源，只剩「加进 SP 节点」这条路，用户已说过要加）。
- ⛔ **不是所有怪都有盾**：`GlobalMobPatch` 不是 `LivingEntityPatch` ⇒ 通用怪既没盾也没条。**监守者/潜影贝/远古守卫者暂时没有韧性条**，待「自注册 patch」阶段补（`EntityPatchRegistryEvent`）。
- 资格「体抗 + 术抗 ≥ 45」；公式 `poiseDmg = base × (1 + B_eff)`，`B_eff = normalize(B) × (specialized ? resistance : 1)`。**`specialized` 只管 `resistance` 生不生效**，与其它一切无关。
- 全部细节（HealthBar 坐标、shouldDraw 门槛、setStun 夹取顺序、EntityStunEvent 取消语义、生成 patch 的 7 个抽象方法）见 `REFERENCE.md#EpicFight 硬直护盾`。
