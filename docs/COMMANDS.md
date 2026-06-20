# 命令参考

本文档列出了 My Bot Mod 的所有命令及其用法。

## 目录
- [基础命令](#基础命令)
- [动作控制](#动作控制)
- [移动控制](#移动控制)
- [自动寻路](#自动寻路)
- [视角控制](#视角控制)
- [物品操作](#物品操作)
- [骑乘控制](#骑乘控制)
- [测试命令](#测试命令)
- [配置命令](#配置命令)

---

## 基础命令

### 创建假人
```
/bot <名字> spawn
```
在你的位置创建一个假人。

**示例**：
```
/bot Steve spawn
```

---

```
/bot <名字> spawn at <x> <y> <z>
```
在指定位置创建假人。

**示例**：
```
/bot Steve spawn at 100 64 200
```

---

```
/bot <名字> spawn at <x> <y> <z> <gamemode>
```
在指定位置以指定游戏模式创建假人。

**游戏模式**：`survival`、`creative`、`adventure`、`spectator`

**示例**：
```
/bot Steve spawn at 100 64 200 creative
```

---

### 删除假人
```
/bot <名字> kill
```
删除指定的假人。

**示例**：
```
/bot Steve kill
```

---

### 列出假人
```
/bot list
```
列出所有当前存在的假人。

---

### 停止所有动作
```
/bot <名字> stop
```
停止假人的所有动作（攻击、使用、移动等）。

**示例**：
```
/bot Steve stop
```

---

## 动作控制

### 攻击

#### 单次攻击
```
/bot <名字> attack once
```
攻击一次视线前方的目标或方块。

**示例**：
```
/bot Steve attack once
```

---

#### 持续攻击
```
/bot <名字> attack continuous
```
持续攻击视线前方的目标或方块。

**示例**：
```
/bot Steve attack continuous
```

---

#### 间隔攻击
```
/bot <名字> attack interval <ticks>
```
每隔指定 tick 数攻击一次（20 ticks = 1 秒）。

**示例**：
```
/bot Steve attack interval 20    # 每秒攻击一次
/bot Steve attack interval 10    # 每 0.5 秒攻击一次
```

---

### 右键交互

> **v1.2.0 更新**：`use` 命令现在模拟完整的右键点击，支持放置方块、与实体交互和使用物品。

#### 单次交互
```
/bot <名字> use once
```
执行一次右键交互（放置方块、与实体交互或使用物品）。

**示例**：
```
/bot Steve use once
```

---

#### 持续交互
```
/bot <名字> use continuous
```
持续执行右键交互。

**示例**：
```
/bot Steve use continuous
```

---

#### 间隔交互
```
/bot <名字> use interval <ticks>
```
每隔指定 tick 数执行一次右键交互。

**示例**：
```
/bot Steve use interval 40    # 每 2 秒交互一次
```

---

### 潜行
```
/bot <名字> sneak
```
开始潜行。

```
/bot <名字> unsneak
```
停止潜行。

**示例**：
```
/bot Steve sneak
/bot Steve unsneak
```

---

### 跳跃
```
/bot <名字> jump
```
跳跃一次。

**示例**：
```
/bot Steve jump
```

---

### 疾跑
```
/bot <名字> sprint
```
开始疾跑。

```
/bot <名字> unsprint
```
停止疾跑。

**示例**：
```
/bot Steve sprint
/bot Steve unsprint
```

---

## 移动控制

### 向前移动
```
/bot <名字> move forward
```
持续向前移动。

**示例**：
```
/bot Steve move forward
```

---

### 向后移动
```
/bot <名字> move backward
```
持续向后移动。

**示例**：
```
/bot Steve move backward
```

---

### 向左移动
```
/bot <名字> move left
```
持续向左移动。

**示例**：
```
/bot Steve move left
```

---

### 向右移动
```
/bot <名字> move right
```
持续向右移动。

**示例**：
```
/bot Steve move right
```

---

### 停止移动
```
/bot <名字> move stop
```
停止所有移动。

**示例**：
```
/bot Steve move stop
```

---

## 自动寻路

> **v1.2.1 新增**：使用 A* 算法自动寻路到指定坐标。

### 寻路到指定位置
```
/bot <名字> goto <x> <y> <z>
```
假人会自动计算路径并走向目标位置。

**示例**：
```
/bot Steve goto 100 64 200
/bot Steve goto ~10 ~ ~10
```

**特性**：
- 自动跳过1格高障碍（需启用自动跳跃）
- 最大寻路距离256格
- 每5秒自动重新计算路径
- 卡住时自动重新寻路
- 支持跳跃1格高障碍、下落最多3格

---

### 取消寻路
```
/bot <名字> goto stop
```
停止假人的寻路行为。

**示例**：
```
/bot Steve goto stop
```

---

## 视角控制

### 看向基本方向

#### 看向上方
```
/bot <名字> look up
```
看向正上方（pitch = -90°）。

---

#### 看向下方
```
/bot <名字> look down
```
看向正下方（pitch = 90°）。

---

#### 向左转
```
/bot <名字> look left
```
向左转 90°。

---

#### 向右转
```
/bot <名字> look right
```
向右转 90°。

---

### 看向指定方向

#### 看向北方
```
/bot <名字> look north
```
看向北方（Z-）。

---

#### 看向南方
```
/bot <名字> look south
```
看向南方（Z+）。

---

#### 看向东方
```
/bot <名字> look east
```
看向东方（X+）。

---

#### 看向西方
```
/bot <名字> look west
```
看向西方（X-）。

---

### 旋转视角
```
/bot <名字> turn <yaw> <pitch>
```
相对旋转视角。

**参数**：
- `yaw`：水平旋转角度（正值向右，负值向左）
- `pitch`：垂直旋转角度（正值向下，负值向上）

**示例**：
```
/bot Steve turn 45 0      # 向右转 45°
/bot Steve turn -90 0     # 向左转 90°
/bot Steve turn 0 -45     # 向上看 45°
```

---

## 物品操作

### 丢弃物品
```
/bot <名字> drop
```
丢弃主手物品（一个）。

**示例**：
```
/bot Steve drop
```

---

### 丢弃整组
```
/bot <名字> dropStack
```
丢弃主手物品（整组）。

**示例**：
```
/bot Steve dropStack
```

---

### 交换主副手
```
/bot <名字> swapHands
```
交换主手和副手的物品。

**示例**：
```
/bot Steve swapHands
```

---

## 骑乘控制

### 骑乘实体
```
/bot <名字> mount
```
骑乘附近的实体（根据白名单）。

**示例**：
```
/bot Steve mount
```

**注意**：
- 只能骑乘白名单中的实体
- 白名单可在配置界面中编辑
- 默认白名单包括：猪、马、驴、骡、羊驼、船、矿车、炽足兽

---

### 下马
```
/bot <名字> dismount
```
离开当前骑乘的实体。

**示例**：
```
/bot Steve dismount
```

---

## 测试命令

### 运行所有测试
```
/bot test
```
运行所有测试套件。

---

### 运行特定测试
```
/bot test movement
```
测试移动功能。

```
/bot test actions
```
测试动作功能。

```
/bot test skin
```
测试皮肤系统。

---

## 命令组合示例

### 自动挖矿机器人
```
/bot Miner spawn
/bot Miner look down
/bot Miner attack continuous
```

### 自动钓鱼机器人
```
/bot Fisher spawn
/bot Fisher use continuous
```

### 巡逻机器人
```
/bot Guard spawn
/bot Guard move forward
/bot Guard attack continuous
```

### 建筑助手
```
/bot Builder spawn
/bot Builder look down
/bot Builder use interval 10
```

---

## 配置命令

### 显示所有配置
```
/botmod config
```
显示所有当前配置项及其值。

---

### 重新加载配置
```
/botmod config reload
```
从配置文件重新加载配置。

---

### 重置配置
```
/botmod config reset
```
将所有配置重置为默认值。

---

### 设置配置项
```
/botmod config set <配置项> <值>
```

**布尔值配置项**：
```
/botmod config set enableBotFeature true
/botmod config set enableKillAura false
/botmod config set allowMountOtherBots true
/botmod config set allowNonOpCreateBot false
/botmod config set autoRespawnOnDeath true
/botmod config set botTakeDamage true
/botmod config set botHunger true
/botmod config set botPersistence false
/botmod config set preserveBotState false
/botmod config set carpetModCompatibility true
/botmod config set allowBotAutoJump true
```

**数值配置项**：
```
/botmod config set attackReachDistance 3.0
/botmod config set creativeAttackReachDistance 5.0
/botmod config set killAuraRange 3.0
/botmod config set maxBotCount 10
```

---

### 获取配置项
```
/botmod config get <配置项>
```

**示例**：
```
/botmod config get enableBotFeature
/botmod config get attackReachDistance
/botmod config get maxBotCount
```

---

### 管理骑乘白名单

#### 列出白名单
```
/botmod whitelist list
```
显示所有可骑乘的实体类型。

---

#### 添加到白名单
```
/botmod whitelist add <实体类型>
```

**示例**：
```
/botmod whitelist add minecraft:pig
/botmod whitelist add minecraft:horse
/botmod whitelist add minecraft:boat
```

---

#### 从白名单移除
```
/botmod whitelist remove <实体类型>
```

**示例**：
```
/botmod whitelist remove minecraft:pig
```

---

#### 清空白名单
```
/botmod whitelist clear
```
移除所有白名单实体。

---

### 显示模组信息
```
/botmod info
```
显示模组版本、作者、许可证等信息。

---

## 配置命令示例

### 启用杀戮光环
```
/botmod config set enableKillAura true
/botmod config set killAuraRange 5.0
```

### 设置假人数量限制
```
/botmod config set maxBotCount 10
```

### 启用假人驻留
```
/botmod config set botPersistence true
/botmod config set preserveBotState true
```

### 允许非 OP 创建假人
```
/botmod config set allowNonOpCreateBot true
```

### 禁用 Carpet Mod 兼容模式
```
/botmod config set carpetModCompatibility false
```
**注意**：如果检测到 Carpet Mod，可能需要手动启用假人功能：
```
/botmod config set enableBotFeature true
```

---

## 配置生效说明

**所有配置修改后立即生效**，无需重启游戏：
- ✅ 假人功能开关 - 立即生效
- ✅ 攻击和移动设置 - 立即应用到所有假人
- ✅ 骑乘白名单 - 立即更新
- ✅ Carpet Mod 兼容模式 - 立即检测并应用
- ✅ 所有其他配置项 - 立即生效

**手动编辑配置文件**后，使用 `/botmod config reload` 重新加载。

---

## 权限要求

- **`/bot` 命令**：默认需要 OP 权限（等级 2），可通过配置允许非 OP 使用
- **`/botmod` 命令**：需要 OP 权限（等级 2）

---

## 注意事项

1. **假人名字规则**：
   - 长度：3-16 个字符
   - 只能包含：字母、数字、下划线
   - 不能与现有玩家重名

2. **攻击系统**：
   - 默认只攻击视线前方的目标
   - 启用"杀戮光环"后攻击范围内所有实体
   - 可以破坏方块（创造模式直接破坏，生存模式持续挖掘）

3. **移动系统**：
   - 移动速度受潜行状态影响（潜行时速度降低到 30%）
   - 疾跑会增加移动速度
   - 可以同时前后移动和左右移动（斜向移动）

4. **间隔模式**：
   - `once`：执行一次后停止
   - `continuous`：持续执行
   - `interval <ticks>`：每 N tick 执行一次（20 ticks = 1 秒）

---

## 相关文档

- [配置指南](CONFIG.md) - 配置系统详细说明
- [皮肤系统](SKINS.md) - 皮肤加载和自定义
- [常见问题](FAQ.md) - 常见问题解答
