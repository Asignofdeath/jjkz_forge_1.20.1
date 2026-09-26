# 项目长期备忘（JujutsuKaisenZone / Forge 1.20.1）

> 本文件只放**铁律速查**。子系统机制细节见同目录 `REFERENCE.md`；逐日过程见 `2026-09-2*.md` 日志。

## 环境 / 工具
- ⛔ **lang 文件换行风格**：2026-09-26 实测两个文件**都是纯 LF**（zh_cn / en_us 各 389 行、0 个 CRLF）——更早记的「`en_us.json` 是 CRLF」**已过期**，别再照它假设。python 批量改文案仍必须 `io.open(..., newline='')` 读、探测风格后按原风格写回，否则整文件换行被改（diff 爆炸）；插行脚本见 `tools/add_no_crit_tag_lang.py`（⚠️ 取缩进要 `raw[line_start:anchor_at].split('"')[0]`，直接切片会把那个引号带进去）。
- ⛔ **javac 的报错输出是 GBK**：用 grep 匹配中文错误关键字会静默失败（日志被当二进制）；要 `open(...,'rb').read().decode('gbk')` 后用 python 统计。另外无 classpath 时「方法不会覆盖或实现超类型的方法」是**误报**（父类不可见），别当真实错误。
- ⚠️ **PATH 里没有 `javap`**，JDK 在 `C:\Users\19316\.gradle\jdks\eclipse_adoptium-17-amd64-windows\jdk-17.0.20.1+1\bin\`。LDLib 的 class 在 `~/.gradle/caches/modules-2/files-2.1/curse.maven/ldlib-626676/7809449/*/ldlib-626676-7809449.jar`（`forge_gradle/deobf_dependencies` 那份是空壳）。
- ⛔ **禁止跑 `./gradlew`（含 compileJava）**：游戏锁 `build/`、管道憋输出，能卡 10min~1h。编译交用户在 IDEA 做；AI 只做静态自查（读文件 / grep 全部调用点 / 核对签名与参数个数）。长任务别接 `| tail`（管道要等进程结束才吐字）。
- ✅ **静态自查的最硬形式 = `javac` 全工程编译**（不碰 `build/`，2026-09-26 实测 555 文件 / exit 0）。已落地成脚本 `tools/jjkz_static_check.py`（直接跑即可，需先把 JDK bin 加进 PATH）；原理与全部坑见 `REFERENCE.md` 的「静态类型检查」。跑完是 0 错误才敢说「改动已落地」。
- 别 `cd` 项目目录；PowerShell stdout 不可见；Bash 缺 coreutils，先 `export PATH="/c/Users/19316/.workbuddy/binaries/PortableGit/versions/1.2.0/usr/bin:/c/Windows/System32:$PATH"`。
- 裸 python 无 PIL/numpy，图像用 `.../python/envs/default/Scripts/python.exe`；给 Windows exe 传路径必须写 `C:/...`（`/c/...` 被当相对路径且不报错）。
- 同一文件多处 Edit **必须串行**，不同文件才可并行；⛔ 同文件 Edit 会**静默丢改动**（工具仍报成功，只在用户编译时才暴露）——改完必须 **grep 复核每一处调用点**是否真的落地（2026-09-24 改 `triggerBlackFlash` 签名，2 个调用点只落地了 1 个，用户编译才炸）；EBUSY = 游戏/IDEA 占用，重试即可。
- 工具集中途变化、缺写入类工具报 Tool Not Found 时别重试，直接给用户精确补丁（行号 / 唯一锚点）。
- MC 源码：`~/.gradle/caches/forge_gradle/minecraft_user_repo/net/minecraftforge/forge/1.20.1-47.4.10_mapped_official_1.20.1/*-sources.jar`（含补丁）；`decompile/output.jar` 未打补丁，别用。
- ⚠️ 报 `Failed to validate certificate for host 'https://maven.minecraftforge.net/'` 时**别去折腾证书 / 换 JDK / 加信任库**：证书链（GTS WE1 → GTS Root R4）与 JDK cacerts 都正常，是 ForgeGradle 的 `EnvironmentChecks#testServerConnection` **无重试 + 只捕 `SSLException`** 把一次网络抖动误报成"证书无效"。已在 `gradle.properties` 的 `org.gradle.jvmargs` 里加 `-Dnet.minecraftforge.gradle.check.certs=false`，**别删这一行**。同类问题先读 `~/.gradle/caches/modules-2/files-2.1/net.minecraftforge.gradle/ForgeGradle/*/*-sources.jar` 源码（有 sources，别反编译）。

## 跨模块铁律
- ⛔ **用户口述文案一律逐字照写**：不改大小写 / 不换引号 / 不补字顺句 / 不点评不追问（见用户级 MEMORY.md）。
- ⚠️ **技能描述必须写「玩家当前做得到的事」，不能照满配形态写**。机制型技能（如「量刑」「从重论处」）门控掉的内容（死刑、概率加成），在未解锁时要从被影响技能的描述里**整段消失**，只留一句「解锁 X 后会怎样」——对照「三式法槌」的写法（「未解锁时法槌恒为标准形态」）。实现走 `TechniqueSkillDescriptionRegistry#registerVariant` + `TechniqueSkillDescriptionVariant`（键与参数由同一个 lambda 给全，避免走偏），**简洁页 `.brief` 也要换**（它是悬停框默认视图）。后缀键必须 zh_cn 与 en_us 双侧都写，缺一侧界面就直接显示键名。
- ⛔ **服务端无法用 `deltaMovement` 移动玩家**（客户端每 tick 覆盖）⇒ 只能每 tick `teleportTo(x,y,z)`；`lookAt(...)` 必须在 teleport **之前**（位置包才带朝向）。玩家位移状态用静态内存 Map + deadline，别落盘（掉线留残留）。
- ⚠️ **术式伤害必须走 `TechniqueDamageSources.deal(target, owner, amount)`**，别用 `damageSources().magic()`——后者 `getEntity()`/`getLastHurtByMob()` 恒 null ⇒ 不给咒术经验、不掉术式情报、不记罪恶者、黑闪击杀检测不触发。
  - 反过来别搞混：**判据是「这一击有没有归因」，不是「有没有走 deal」**。原版投射物源 `thrown(direct, owner)` 的 `causingEntity` 就是 owner ⇒ `getEntity()` = 投掷者，归因本来就是通的（法槌投射物就是这种）。把这类伤害硬改走 `deal` 是**回归**：`deal` 刻意把 `directEntity` 填 null（盾牌挡不住术式），会打掉依赖 `getDirectEntity()` 的判定（如 `GavelWeaponEvents#isGavelWeaponDamage` 的罪恶者增伤），并把物理伤害（受护甲/盾）换成 `magic()` 的**无视护甲**。查归因先读 `DamageSource#getEntity()`/`getDirectEntity()` 有没有值。
- ⚠️ **新增持久化成长数据必须一起接进 `SkillUnlockLogic.copyProgressionFrom`**（跨死亡保留），并记得 `syncToClient`（客户端开发树读的是客户端 persistentData）。
- ⚠️ **新增伤害乘算（增伤/减伤/易伤）一律加进 `TechniqueDamageZoneEvents` 统一乘区管线（`LivingHurtEvent` @HIGH），别在别的监听器里裸写 `setAmount` 乘算**。优先级次序勿乱动：处刑人 HIGHEST（改写）→ 管线 HIGH → 罪恶者/黑闪/打断 NORMAL。「下一击分型增伤」用 `TechniqueDamageBoosts.push/consume`（纯内存态）。黑闪 pow(2.5) 刻意留在 BlackFlashEvents 且晚于管线 ⇒ 黑闪放大一切乘区是设计预期。
- ⚠️ **伤害分型（`SkillDamageType`）按叙事，不按通道**：术式=术式能力一击；体术=体术能力一击；复合=**同一次**叠加两者（黑闪、处刑必杀）。粒度是每一次结算；一技能可多段多型。挂分型：`deal(..., type)` 或 `runWithType(type, ...)`（thrown/explode/mobProjectile 等不宜改走 deal 时用）。通道外：`EpicFight#isBasicAttack`→体术；必定黑闪 / 处刑剑+死刑→复合；赫飞块 `TAG_RED_DEBRIS`→术式。**不要**在 `TechniqueSkill` 上加分型字段、不要在悬停标题栏写「伤害类型」；描述正文直接写「N 点术式/体术伤害」。
- ⛔ **暴击许可：默认跟随分型（体术可暴、术式不可暴），出伤点可双向覆盖**。术式**单发**招式必须显式走 `TechniqueDamageSources.dealCrit(...)` / `runWithTypeCrit(...)`；**高频/周期性出伤（每 tick、固定间隔）一律保持默认**（术式默认就不放行，`dealNoCrit` 只给体术侧收闸门用）。闸门在 `CritRules.applyCritZone`，**必须在 `consumeForced` 之前**——否则被拒的一击会白吃掉十划弱点击破注入的强制暴击标记。⚠️ 默认值**不能**改成「一律不可暴击」：EF 基础攻击 / 原版近战不走 `deal`、拿不到表态，会连带关掉现在唯一的暴击来源。放开术式暴击的连带项（改一处就得跟）：`HitCritState.critType()` 必须记分型，`SeEffectEvents`（`ON_MARTIAL_CRIT`）与 `BlackFlashEvents` 都要校验 `critType() == MARTIAL`。描述侧标注名单在 `technique/crit/CritSkillPolicy`（列进去的技能，悬停框打「不参加暴击」行）。
- ⚠️ **技能树有四种节点**：`SkillTreeNodeDef.skill / specialMove / reference / info`。**领域展开是 `info` 说明节点**（`visibleWhen(UnlockCondition.hasDomainExpansion())` 控制显隐），它有悬停框、不是「没有节点」——它的 tooltip 在 `SkillTreeNodeWidget#refreshHoverTooltips` 的 `isInfoNode()` 分支里手搓，**不走** `TechniqueSkillTooltipHelper`。改任何「节点级描述」前先 grep `SkillTreeNodeDef.info(` / `isInfoNode()`。领域节点没有 skillId，只能按 `nodeId` 定位（如 `ShrineTechniqueSkillSet.INFO_DOMAIN_FUMOSHINKAN`）。⚠️「是不是领域说明节点」的判据只有一处：`SkillTreeNodeDef#isDomainInfoNode()`（按 `visibleWhen(hasDomainExpansion())` 判定），界面与自检都复用它，**别各自再写一份**。
- ⚠️ **字符串声明表要配启动自检**（这是本项目反复踩的坑：lang 键、技能 id、nodeId 全靠字符串）。范式见 `CritSkillPolicy#validate()`：在 `JuJutsuKaisenZone` 全部技能集注册**之后**调用，对照 `TechniqueSkillTreeRegistry.all()`（技能表在这之前是空的，提前调用会命中「时机不对」守卫）检查「名单里的 id 都真实存在」+「节点种类符合预期」。拼错字符串在这种表里是**静默失败**（界面少一行字、功能不生效），必须在开发期炸掉。
- ⛔ **PowerShell 改 UTF-8 中文文件必须显式编码**：`[IO.File]::ReadAllText/WriteAllText` 无 Encoding 参数时用系统 ANSI（中文 Windows=GBK）⇒ 会把 `zh_cn.json` 读坏再写坏。正确：`$utf8 = New-Object System.Text.UTF8Encoding $false` 后读写都传 `$utf8`。
- ⛔ **要做成「可解锁机制」的只能是能力型机制，不能是约束型机制**：衰减 / 芝诺墙 / 持有上限这类是**限制**，做成可解锁 ⇒ 解锁前反而更强，方向是反的。落地手法见 `REFERENCE.md` 的「机制型技能」（复用 `SKILL` 节点 + `getMechanicSkills()`，别新增节点种类）。
- ⛔ **强度换算只在 `TechniqueDamageSources.deal` 内部做一次**。出伤点自己再乘 `SkillDamageResolver` 就是双计。唯一例外：直连 `hurt` 不走 deal 的那几处（茈 / 远程茈 / 灶 / 法槌投掷）才单独包一层。EF / 原版近战由 `TechniqueDamageZoneEvents` 区 0 接管，靠 `POWER_ALREADY_SCALED` 识别「deal 已算过」。
- ⛔ **`CursedGradeBonuses` 不许再给 `ATTACK_DAMAGE` 加等级值**（已删），改成与等级无关的 `ATTACK_BASE_UUID +1.0` 锚点；等级成长全部由强度面板承担，加回来就双计。强化术的 +40% ATTACK 修饰符**故意留在属性层**，不进面板。
- ⚠️ **黑闪 `pow(x, 2.5)` 必须拿「强度放大前」的原始值取幂**，再把面板倍率线性乘回（`pow(raw) × scaled/raw`）；直接对已放大值取幂 ⇒ 特级指数爆炸。见 `SkillDamageResolver.RAW_BEFORE_POWER`。
