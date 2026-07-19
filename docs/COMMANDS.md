# 命令参考

My Bot Mod 提供 `/bot`（控制假人）与 `/botmod`（配置/信息）两组命令。

**权限**：`/bot` 默认需 OP（等级 2），可用配置 `allowNonOpControlBot` 放开给非 OP；`/botmod` 始终需 OP。所有命令支持命令方块。间隔参数单位为 tick（20 tick = 1 秒）。

---

## `/bot` 命令

`<name>` 为假人名（3-16 位字母/数字/下划线，不与现有玩家重名）。

### 生命周期
| 命令 | 说明 |
|------|------|
| `/bot <name> spawn` | 在你的位置创建假人 |
| `/bot <name> spawn at <x> <y> <z> [gamemode]` | 在指定位置（可选游戏模式）创建 |
| `/bot <name> kill` | 删除假人 |
| `/bot list` | 列出所有假人 |
| `/bot <name> stop` | 停止所有动作 |

### 动作
| 命令 | 说明 |
|------|------|
| `/bot <name> attack once\|continuous\|interval <ticks>` | 攻击视线目标/方块（一次/持续/间隔） |
| `/bot <name> use once\|continuous\|interval <ticks>` | 右键交互（放置/交互/使用物品） |
| `/bot <name> sneak` / `unsneak` | 开始/停止潜行 |
| `/bot <name> jump` | 跳跃一次 |
| `/bot <name> sprint` / `unsprint` | 开始/停止疾跑 |
| `/bot <name> drop` / `dropStack` | 丢弃主手物品（一个/整组） |
| `/bot <name> swapHands` | 交换主副手 |

### 移动与视角
| 命令 | 说明 |
|------|------|
| `/bot <name> move forward\|backward\|left\|right\|stop` | 持续移动 / 停止 |
| `/bot <name> goto <x> <y> <z>` | A* 自动寻路到坐标（见下方特性） |
| `/bot <name> goto stop` | 取消寻路 |
| `/bot <name> look up\|down\|left\|right\|north\|south\|east\|west` | 看向方向 |
| `/bot <name> turn <yaw> <pitch>` | 相对旋转视角 |

**寻路特性**（v1.3.1 重构）：非阻塞分帧计算（不卡服）；支持跳 1 格、下落、跨越 1-3 格裂谷、游泳、跨维度（先传送到你的维度再寻路）；自动绕开岩浆/火/仙人掌等；卡住/定期自动重算。

### 骑乘
| 命令 | 说明 |
|------|------|
| `/bot <name> mount` | 骑乘附近白名单实体 |
| `/bot <name> dismount` | 下马 |

### 背包 / 面板（也可**右键假人**打开设置面板）
| 命令 | 说明 |
|------|------|
| `/bot <name> panel` | 打开设置面板（等同右键假人） |
| `/bot <name> inventory` | 打开可编辑背包（主物品栏+盔甲+副手，左侧模型，3x3 手持槽位选择） |
| `/bot <name> enderchest` | 打开可编辑末影箱 |
| `/bot <name> slot <0-8>` | 设置手持快捷栏格 |
| `/bot <name> gamemode <survival\|creative\|adventure\|spectator>` | 设置游戏模式 |
| `/bot <name> tphere` | 把假人传送到你身边（支持跨维度） |

### 测试
`/bot test [movement\|actions\|skin]` — 运行测试套件。

---

## 指挥棒（Command Baton）

用**两根木棍**合成，不可堆叠。**手持**该物品时屏幕左上/右上显示 HUD 信息，无需命令：

| 操作 | 效果 |
|------|------|
| `Ctrl + 滚轮` | 切换模式（指挥寻路 / 传送） |
| `Alt + 滚轮` / `Alt + 右键看向假人` | 选择假人 |
| `右键看向某处` | 让选中假人**寻路**前往 / **传送**至该处 |

传送模式默认仅手持玩家为创造模式时可用（配置 `allowBatonTeleportNonCreative` 放开）；传送/寻路均支持跨维度。

---

## `/botmod` 命令

| 命令 | 说明 |
|------|------|
| `/botmod config` | 显示所有配置 |
| `/botmod config reload` / `reset` | 重新加载 / 重置为默认 |
| `/botmod config set <key> <value>` | 设置配置项 |
| `/botmod config get <key>` | 获取配置项 |
| `/botmod whitelist list\|add <id>\|remove <id>\|clear` | 管理骑乘白名单 |
| `/botmod info` | 显示模组信息 |

**布尔配置项**：`enableBotFeature`、`enableKillAura`、`allowMountOtherBots`、`allowNonOpControlBot`、`autoRespawnOnDeath`、`botTakeDamage`、`botHunger`、`botPersistence`、`preserveBotState`、`carpetModCompatibility`、`allowBotAutoJump`、`allowBatonTeleportNonCreative`

**数值配置项**：`attackReachDistance`、`creativeAttackReachDistance`、`killAuraRange`、`maxBotCount`

配置修改后**立即生效**；手动编辑配置文件后用 `/botmod config reload`。各项含义见 [配置指南](CONFIG.md)。

---

## 组合示例

```
# 自动挖矿
/bot Miner spawn
/bot Miner look down
/bot Miner attack continuous

# 自动钓鱼
/bot Fisher spawn
/bot Fisher use continuous

# 建筑助手（每 0.5 秒放置一次）
/bot Builder spawn
/bot Builder look down
/bot Builder use interval 10
```

---

## 相关文档
[配置指南](CONFIG.md) · [皮肤系统](SKINS.md) · [常见问题](FAQ.md)
