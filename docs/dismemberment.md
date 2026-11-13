# 断肢系统

## 为什么

PPs Dismemberment（模组 ID `mcg`）默认是「普攻打到哪就断哪」。本模组把断肢定为**近永久强控**，必须由技能经门面下令，并受护甲 / 体术抗性约制。

定稿：**本模组断肢系统是上司，mcg 是下属。**

## 通用行为（所有可断肢技能共用）

### 切痕（每肢独立）

- 部位：头 / 左臂 / 右臂 / 左腿 / 右腿（躯干不记切痕、不断肢）。
- **上限** = `clamp(ceil(maxHealth × 0.40), 8, 64)`。
- **加痕**：`applyCutMarks(attacker, target, part, baseMarks, damageDealt)`  
  - 原始量 = `baseMarks + damageDealt × 0.15`  
  - 再乘 `(1 - 护甲减伤) × (1 - 体术抗性减伤)`（穿甲可抵，与 `SkillDefenseResolver` 同形）  
  - 向下取整后累加，不超过该肢上限。
- **自然痊愈**：服务端每 40 tick 各肢切痕 −1（`DismembermentEvents`）。
- **断肢**：`trySever` 要求该肢切痕已满上限；成功后清空该肢切痕并调 mcg。

### 职责拆分

| 层 | 类 | 做什么 |
|---|---|---|
| 门面 | `dismemberment.DismembermentSystem` | 切痕 / 上限 / 减免 / `trySever` |
| 存贮 | `dismemberment.CutMarkData` | 每肢 NBT |
| 表现桥 | `compat.McgDismembermentCompat` | mcg sever / 断肢实体 / sync |
| 掐被动 | `mixin.DismembermentEngineMixin` | 取消 mcg `handleDamage` |

技能**只**决定何时 `applyCutMarks`、何时 `trySever`；不要自算抗性或自管切痕 NBT。

## 已接线技能

| 技能 | 时机 |
|---|---|
| 十划弱点击破 | `base=12` + 本击伤害加痕；满则 `trySever` |
| 解 / 切割 | 命中采样点相对碰撞箱选最近可断肢，`base=5` + 伤害（`ShrineCutMarkLogic`） |
| 捌 | 每次周期伤害结算成功后，随机一肢 `base=3` + 伤害 |
| 领域斩 / 强力斩 | 与捌相同：每次伤害成功后随机一肢（普通 `base=3`，强力 `base=5`） |
| 三级咒灵甲普攻 | 命中**玩家**时随机可断肢，`base=2` + 攻击力（仅叠痕，不 `trySever`） |

满痕自动断肢目前仅十划击破会 `trySever`；御厨子侧与三级咒灵甲暂只叠痕（收刀断肢可后续接）。

## 对策 SE（玩家装载 ∪ 咒灵天道）

断肢是近永久强控，用三条互不冲突的窄口 SE 作对策：

| 咒物 | SE ID | 窄口 | 效果 |
|---|---|---|---|
| 灌铅的护腰 | `cut_mark_vital_cap` | `CutMarkCapBonusSeMechanic` | 上限再加 `ceil(maxHP×0.15)`，软顶 80 |
| 干净的纱布卷 | `cut_mark_fast_recovery` | `CutMarkRecoverySeMechanic` | 衰减改为每 20 tick 各肢 −2 |
| 烧焦的止血棉 | `dismemberment_bleed_immune` | `DismembermentBleedImmuneSeMechanic` | `trySever` 时不启动 mcg 流血 |

门面查询走 `HeavenBalance.findActive`：玩家看装载；咒灵看天道资格∩维度压力解锁（见 [heaven.md](heaven.md)）。未命中则默认强控。对咒灵 `trySever` 成功会抬本维断肢压力。反转修肢是另一层，与「不流血」不重复。

## 进攻 SE（攻击方装载）

弱强化叠痕，不降满痕门槛、不自动断肢；不进天道咒灵池。

| 咒物 | SE ID | 窄口 | 效果 |
|---|---|---|---|
| 某人的破旧玩偶 | `cut_mark_base_bonus` | `CutMarkBaseBonusSeMechanic` | `baseMarks +2` |
| 渗血的创可贴 | `cut_mark_inflict_bonus` | `CutMarkInflictBonusSeMechanic` | 护甲结算后加痕 ×1.2 |

接线：`applyCutMarks` 查攻击方 `SeMechanicRegistry.findEquipped`。两条窄口不同，可同装。

## 修复（后续）

- 玩家：反转术式可长回肢体。
- 咒灵：天生可愈。
- 普通生物：断肢后无自愈；切痕仍会随时间淡去。

## 依赖

- `build.gradle`：`pps-dismemberment`；`mods.toml`：`modId="mcg"`。

## 验证

1. 普攻不断肢。
2. 解打头/腿附近，对应肢切痕上涨更快。
3. 捌 / 领域期间切痕在各肢间跳动上涨。
4. 高甲 / 高体抗：同一次加痕明显变少。
