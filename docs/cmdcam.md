# CMDCam 编排

新演出**直接照这张抄**，不要每次现场调间隔。

命令走 `ClientCommandHandler.runCommand`（**无前导斜杠**）。每条演出自己一个 `CmdCamScheduler`，互不并队。

实现：`CmdCamScheduler`、各 `*CmdCamClient`。`cam save` 的 OP-4 已由 `SetPathPacketMixin` 跳过。

20 tick = 1 秒。

## 默认间隔（照抄）

| 刚执行完 | 等到下一条 |
|---|---|
| `mode` / `clear` / `add` / `smooth_start` | **1** tick |
| `cam save …` | **5** tick |
| `cam load …` | **5** tick |
| `cam start Xs`（后面还有下一组） | **X × 20 − 5** tick，然后 `load` |

禁止：`start 6s` 却等 150。多等就会把视角还给玩家。

仍闪一下：只再减 **1** tick，不要加缓冲。

## 单组

不要 `save`/`load`。

```text
mode outside
clear
add …
add …
（揭幕）
smooth_start false
start Xs
```

## 多组（默认接法）

黑屏或演出开始时把各组 `add` + `save` 全部做完。揭幕只 `load` + `start`。

```text
mode outside
clear
add …          组 A
save pathA
clear
add …          组 B
save pathB
clear
load pathA

（揭幕）
smooth_start false     只写在第一条 start 前，组间不要再写
start As               例如 6s
load pathB             start 之后等 (6×20−5)=115 tick 再执行这条
start Bs               load 之后等 5 tick；例如 3s
```

下一组的 `start` 必须落在上一组结束的那一拍：`load` 那 5 tick 从上一组时长里扣，不要叠在时长后面。

`smooth_start false` 只防「从玩家插到第一点」。它**不能**阻止路径播完后还机位。还机位 = 上一组已经结束、下一组还没 `start`。

## 不要抄领域旧间隔

无量（4s 后等 80）、诛伏（3s 后等 60）是「等满时长再 load」，load 的 5 tick 会叠在后面，组间可能空 0.25 秒。镜头连着时不明显。新演出一律用 **时长 − 5**。

1.1 现用：组1 `6s` → 等 115 再 load；组2/3 `3s` → 等 55 再 load。

## 技巧

**定机（停住不移动）：** 路径上顺序相邻两点，`cam add` **全部参数一模一样**（xyz、yaw、pitch、roll、fov）。同坐标但朝向不同 = 原地转向，不是定机。

**`add` 朝向目标：** `cam add x y z yaw pitch roll fov`，yaw/pitch 用从机位指向目标的 atan2（见各 `*CmdCamClient` 的 `yawToward` / `pitchToward`）。
