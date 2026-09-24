# 项目长期备忘（JujutsuKaisenZone / Forge 1.20.1）

> 本文件只放**铁律速查**。子系统机制细节见同目录 `REFERENCE.md`；逐日过程见 `2026-09-2*.md` 日志。

## 环境 / 工具
- ⛔ **禁止跑 `./gradlew`（含 compileJava）**：游戏锁 `build/`、管道憋输出，能卡 10min~1h。编译交用户在 IDEA 做；AI 只做静态自查（读文件 / grep 全部调用点 / 核对签名与参数个数）。长任务别接 `| tail`（管道要等进程结束才吐字）。
- 别 `cd` 项目目录；PowerShell stdout 不可见；Bash 缺 coreutils，先 `export PATH="/c/Users/19316/.workbuddy/binaries/PortableGit/versions/1.2.0/usr/bin:/c/Windows/System32:$PATH"`。
- 裸 python 无 PIL/numpy，图像用 `.../python/envs/default/Scripts/python.exe`；给 Windows exe 传路径必须写 `C:/...`（`/c/...` 被当相对路径且不报错）。
- 同一文件多处 Edit **必须串行**，不同文件才可并行；⛔ 同文件并发 Edit 会**静默丢改动**（工具仍报成功，只在事后自查里才暴露）；EBUSY = 游戏/IDEA 占用，重试即可。
- 工具集中途变化、缺写入类工具报 Tool Not Found 时别重试，直接给用户精确补丁（行号 / 唯一锚点）。
- MC 源码：`~/.gradle/caches/forge_gradle/minecraft_user_repo/net/minecraftforge/forge/1.20.1-47.4.10_mapped_official_1.20.1/*-sources.jar`（含补丁）；`decompile/output.jar` 未打补丁，别用。
- ⚠️ 报 `Failed to validate certificate for host 'https://maven.minecraftforge.net/'` 时**别去折腾证书 / 换 JDK / 加信任库**：证书链（GTS WE1 → GTS Root R4）与 JDK cacerts 都正常，是 ForgeGradle 的 `EnvironmentChecks#testServerConnection` **无重试 + 只捕 `SSLException`** 把一次网络抖动误报成"证书无效"。已在 `gradle.properties` 的 `org.gradle.jvmargs` 里加 `-Dnet.minecraftforge.gradle.check.certs=false`，**别删这一行**。同类问题先读 `~/.gradle/caches/modules-2/files-2.1/net.minecraftforge.gradle/ForgeGradle/*/*-sources.jar` 源码（有 sources，别反编译）。

## 跨模块铁律
- ⛔ **服务端无法用 `deltaMovement` 移动玩家**（客户端每 tick 覆盖）⇒ 只能每 tick `teleportTo(x,y,z)`；`lookAt(...)` 必须在 teleport **之前**（位置包才带朝向）。玩家位移状态用静态内存 Map + deadline，别落盘（掉线留残留）。
- ⚠️ **术式伤害必须走 `TechniqueDamageSources.deal(target, owner, amount)`**，别用 `damageSources().magic()`——后者 `getEntity()`/`getLastHurtByMob()` 恒 null ⇒ 不给咒术经验、不掉术式情报、不记罪恶者、黑闪击杀检测不触发。
  - 反过来别搞混：**判据是「这一击有没有归因」，不是「有没有走 deal」**。原版投射物源 `thrown(direct, owner)` 的 `causingEntity` 就是 owner ⇒ `getEntity()` = 投掷者，归因本来就是通的（法槌投射物就是这种）。把这类伤害硬改走 `deal` 是**回归**：`deal` 刻意把 `directEntity` 填 null（盾牌挡不住术式），会打掉依赖 `getDirectEntity()` 的判定（如 `GavelWeaponEvents#isGavelWeaponDamage` 的罪恶者增伤），并把物理伤害（受护甲/盾）换成 `magic()` 的**无视护甲**。查归因先读 `DamageSource#getEntity()`/`getDirectEntity()` 有没有值。
- ⚠️ **新增持久化成长数据必须一起接进 `SkillUnlockLogic.copyProgressionFrom`**（跨死亡保留），并记得 `syncToClient`（客户端开发树读的是客户端 persistentData）。
- ⚠️ **新增伤害乘算（增伤/减伤/易伤）一律加进 `TechniqueDamageZoneEvents` 统一乘区管线（`LivingHurtEvent` @HIGH），别在别的监听器里裸写 `setAmount` 乘算**。优先级次序勿乱动：处刑人 HIGHEST（改写）→ 管线 HIGH → 罪恶者/黑闪/打断 NORMAL。「下一击分型增伤」用 `TechniqueDamageBoosts.push/consume`（纯内存态）。黑闪 pow(2.5) 刻意留在 BlackFlashEvents 且晚于管线 ⇒ 黑闪放大一切乘区是设计预期。
- ⚠️ **伤害分型（`SkillDamageType`）按叙事，不按通道**：术式=术式能力一击；体术=体术能力一击；复合=**同一次**叠加两者（黑闪、处刑必杀）。粒度是每一次结算；一技能可多段多型。挂分型：`deal(..., type)` 或 `runWithType(type, ...)`（thrown/explode/mobProjectile 等不宜改走 deal 时用）。通道外：`EpicFight#isBasicAttack`→体术；必定黑闪 / 处刑剑+死刑→复合；赫飞块 `TAG_RED_DEBRIS`→术式。**不要**在 `TechniqueSkill` 上加分型字段、不要在悬停标题栏写「伤害类型」；描述正文直接写「N 点术式/体术伤害」。
- ⛔ **PowerShell 改 UTF-8 中文文件必须显式编码**：`[IO.File]::ReadAllText/WriteAllText` 无 Encoding 参数时用系统 ANSI（中文 Windows=GBK）⇒ 会把 `zh_cn.json` 读坏再写坏。正确：`$utf8 = New-Object System.Text.UTF8Encoding $false` 后读写都传 `$utf8`。
- ⛔ **要做成「可解锁机制」的只能是能力型机制，不能是约束型机制**：衰减 / 芝诺墙 / 持有上限这类是**限制**，做成可解锁 ⇒ 解锁前反而更强，方向是反的。落地手法见 `REFERENCE.md` 的「机制型技能」（复用 `SKILL` 节点 + `getMechanicSkills()`，别新增节点种类）。
