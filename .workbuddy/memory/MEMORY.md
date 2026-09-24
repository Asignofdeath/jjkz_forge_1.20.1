# 项目长期备忘（JujutsuKaisenZone / Forge 1.20.1）

> 本文件只放**铁律速查**。子系统机制细节见同目录 `REFERENCE.md`；逐日过程见 `2026-09-2*.md` 日志。

## 环境 / 工具
- ⛔ **禁止跑 `./gradlew`（含 compileJava）**：游戏锁 `build/`、管道憋输出，能卡 10min~1h。编译交用户在 IDEA 做；AI 只做静态自查（读文件 / grep 全部调用点 / 核对签名与参数个数）。长任务别接 `| tail`（管道要等进程结束才吐字）。
- 别 `cd` 项目目录；PowerShell stdout 不可见；Bash 缺 coreutils，先 `export PATH="/c/Users/19316/.workbuddy/binaries/PortableGit/versions/1.2.0/usr/bin:/c/Windows/System32:$PATH"`。
- 裸 python 无 PIL/numpy，图像用 `.../python/envs/default/Scripts/python.exe`；给 Windows exe 传路径必须写 `C:/...`（`/c/...` 被当相对路径且不报错）。
- 同一文件多处 Edit 串行，不同文件可并行；EBUSY = 游戏/IDEA 占用，重试即可。
- 工具集中途变化、缺写入类工具报 Tool Not Found 时别重试，直接给用户精确补丁（行号 / 唯一锚点）。
- MC 源码：`~/.gradle/caches/forge_gradle/minecraft_user_repo/net/minecraftforge/forge/1.20.1-47.4.10_mapped_official_1.20.1/*-sources.jar`（含补丁）；`decompile/output.jar` 未打补丁，别用。

## 三条跨模块铁律
- ⛔ **服务端无法用 `deltaMovement` 移动玩家**（客户端每 tick 覆盖）⇒ 只能每 tick `teleportTo(x,y,z)`；`lookAt(...)` 必须在 teleport **之前**（位置包才带朝向）。玩家位移状态用静态内存 Map + deadline，别落盘（掉线留残留）。
- ⚠️ **术式伤害必须走 `TechniqueDamageSources.deal(target, owner, amount)`**，别用 `damageSources().magic()`——后者 `getEntity()`/`getLastHurtByMob()` 恒 null ⇒ 不给咒术经验、不掉术式情报、不记罪恶者、黑闪击杀检测不触发。
- ⚠️ **新增持久化成长数据必须一起接进 `SkillUnlockLogic.copyProgressionFrom`**（跨死亡保留），并记得 `syncToClient`（客户端开发树读的是客户端 persistentData）。
