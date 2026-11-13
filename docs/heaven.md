# 天道平衡

## 为什么

术师变强后，世界不应仍按原版节奏；断肢等强控也不该让咒灵永远「裸奔」。天道把这类**世界随术师适配**收成统一目录，而不是散落事件类。

## 适配器

| id | 作用 | 对象 |
|---|---|---|
| `external_mob_power` | 生命 ×(在线均血/20)，出伤 ×√该系数 | 外模敌对/中立；本模实体与十影式神除外 |
| `curse_counter_se` | 维度压力开对策池 + 等级掷骰资格 | 仅非玩家咒灵 |

登记：`HeavenAdapterRegistry`。探测均血：`OnlinePowerProbe`（原 `ServerOnlineHealthAverage` 已转发废弃）。

## 对策 SE（`curse_counter_se`）

- **效果**：复用玩家已有 `SeMechanic` 单例，不另写怪侧数值。
- **查询**：`HeavenBalance.findActive(entity, NarrowInterface.class)`  
  - 玩家 → 装载表  
  - 咒灵 → 个体资格 ∩ 本维已解锁  
  - 其它 → empty  
- **加压**：对咒灵 `trySever` 成功 → 本维 `dismemberment` +4（上限 100）；空闲 10 分钟后每分钟 −1。
- **解锁池**（首批，均可扩展）：

| SE | unlock 压力 |
|---|---|
| 快愈纱布 | ≥ 20 |
| 抬上限护腰 | ≥ 45 |
| 不流血止血棉 | ≥ 70 |

- **个体资格**：有序 SE 槽队列（早→晚），`offer` 入队；**溢出丢掉队首**。spawn / 首次查询按等级掷骰入队；压力涨跌只开关生效，不重掷。
- **槽位上限**：四级/三级 1 → 二级/一级 2 → 特级 3；持有概率同前（8%→90%）。
- **扩展**：新威胁加 `HeavenThreatIds` + 加压钩子；新对策 `HeavenCounterRegistry.register` 一行即可；以后动态发 SE 也走 `CurseSpiritHeavenSe.offer`。

## 刻意不做

- 咒灵不叠 `external_mob_power`（已有等级生命表 + 对策 SE）。
- 玩家咒灵阵营不走天道资格（只走装载）。
- 领域斩不加额外压力系数。
- 怪不真装备咒物。

## 关键文件

- 门面：`heaven/HeavenBalance`
- 压力：`heaven/threat/HeavenBalanceData`（每维 SavedData）
- 目录：`heaven/threat/HeavenCounterRegistry`
- 均血逻辑：`heaven/scale/ExternalMobPowerScale`（事件壳仍在 `VanillaMobHealthScaleEvents`）

## 验证

1. 高均血服务器：外模僵尸生命明显高于 20 基线。
2. 反复对三级咒灵甲断肢：本维压力升高后，**有资格**的个体切痕更难叠满 / 愈更快 / 不流血。
3. 压力清零或新维：同资格个体对策熄灭。
4. 多数三级仍无资格（约 80% 裸奔）。
