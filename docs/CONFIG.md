# 配置指南

本文档详细说明 My Bot Mod 的配置系统。

## 目录
- [配置文件](#配置文件)
- [打开配置界面](#打开配置界面)
- [配置项说明](#配置项说明)
- [配置界面](#配置界面)
- [高级配置](#高级配置)

---

## 配置文件

### 文件位置
```
.minecraft/config/my-bot-mod.json
```

### 文件格式
配置文件使用 JSON 格式，支持手动编辑。

### 示例配置
```json
{
  "enableBotFeature": true,
  "configMenuKey": "key.keyboard.b",
  "attackReachDistance": 3.0,
  "creativeAttackReachDistance": 5.0,
  "enableKillAura": false,
  "killAuraRange": 3.0,
  "mountWhitelist": [
    "minecraft:pig",
    "minecraft:horse",
    "minecraft:boat"
  ],
  "allowMountOtherBots": false,
  "maxBotCount": 0,
  "allowNonOpCreateBot": false,
  "autoRespawnOnDeath": false,
  "botTakeDamage": true,
  "botHunger": true,
  "allowBotAutoJump": true,
  "botPersistence": false,
  "preserveBotState": false
}
```

---

## 打开配置界面

### 方法 1：快捷键
按 **B** 键（默认）打开配置菜单。

### 方法 2：按键绑定
1. 打开 Minecraft 设置
2. 选择"控制"
3. 找到"按键绑定"
4. 找到"My Bot Mod"分类
5. 修改"打开配置菜单"的快捷键

---

## 配置项说明

### 总开关

#### 启用假人功能
- **配置项**：`enableBotFeature`
- **类型**：布尔值
- **默认值**：`true`
- **说明**：控制是否启用假人功能。禁用后无法创建假人。

---

### 攻击设置

#### 攻击距离
- **配置项**：`attackReachDistance`
- **类型**：数字（格）
- **默认值**：`3.0`
- **说明**：生存模式下的攻击/破坏距离。

#### 创造模式攻击距离
- **配置项**：`creativeAttackReachDistance`
- **类型**：数字（格）
- **默认值**：`5.0`
- **说明**：创造模式下的攻击/破坏距离。

#### 启用杀戮光环
- **配置项**：`enableKillAura`
- **类型**：布尔值
- **默认值**：`false`
- **说明**：
  - **禁用**：只攻击视线前方的实体
  - **启用**：攻击范围内所有实体

#### 杀戮光环范围
- **配置项**：`killAuraRange`
- **类型**：数字（格）
- **默认值**：`3.0`
- **说明**：杀戮光环的攻击范围。

---

### 骑乘设置

#### 骑乘实体白名单
- **配置项**：`mountWhitelist`
- **类型**：字符串数组
- **默认值**：
  ```json
  [
    "minecraft:pig",
    "minecraft:horse",
    "minecraft:donkey",
    "minecraft:mule",
    "minecraft:llama",
    "minecraft:boat",
    "minecraft:chest_boat",
    "minecraft:minecart",
    "minecraft:strider"
  ]
  ```
- **说明**：假人可以骑乘的实体类型列表。

#### 允许骑乘其他假人
- **配置项**：`allowMountOtherBots`
- **类型**：布尔值
- **默认值**：`false`
- **说明**：是否允许假人骑乘其他假人。

---

### 生存设置

#### 假人最大数量
- **配置项**：`maxBotCount`
- **类型**：整数
- **默认值**：`0`（无限制）
- **说明**：服务器中假人的最大数量。0 表示无限制。

#### 允许非 OP 创建假人
- **配置项**：`allowNonOpCreateBot`
- **类型**：布尔值
- **默认值**：`false`
- **说明**：是否允许非 OP 玩家创建假人。

#### 死亡自动重生
- **配置项**：`autoRespawnOnDeath`
- **类型**：布尔值
- **默认值**：`false`
- **说明**：假人死亡后是否自动重生（1 秒后）。

#### 假人受到伤害
- **配置项**：`botTakeDamage`
- **类型**：布尔值
- **默认值**：`true`
- **说明**：假人是否会受到伤害。

#### 假人会饥饿
- **配置项**：`botHunger`
- **类型**：布尔值
- **默认值**：`true`
- **说明**：假人是否会饥饿。禁用后饱食度始终为满。

---

### 动作设置

#### 假人自动跳跃
- **配置项**：`allowBotAutoJump`
- **类型**：布尔值
- **默认值**：`true`
- **说明**：启用后，假人在移动时遇到1格高的障碍物会自动跳跃。
  - 模拟真实玩家行为
  - 需要假人正在向前移动或有寻路目标
  - 需要假人在地面上
  - 检测前方1格处是否有固体方块，且上方2格有空间

---

### 驻留设置

#### 假人驻留
- **配置项**：`botPersistence`
- **类型**：布尔值
- **默认值**：`false`
- **说明**：退出世界重进后假人是否依然存在。

**工作原理**（v1.1.1a 完全重写）：
- **SavedData 系统**：使用 Minecraft 的 SavedData 系统持久化假人数据
- **数据存储**：数据保存在世界文件夹的 `data/my_bot_mod_bots.dat` 中（NBT 格式）
- **区块加载**：使用区块加载票据（Chunk Ticket）确保假人所在区块保持加载
- **自动加载**：在第一个玩家加入时自动加载所有保存的假人（延迟 2 秒）
- **自动保存**：假人每 10 秒自动保存一次，服务器关闭时也会保存
- **跨维度支持**：支持主世界、下界、末地的假人驻留
- **区块票据刷新**：每 5 秒刷新一次区块加载票据，确保假人区块始终加载

**保存的数据**：
- 基本信息：名字、UUID、创建者信息
- 位置信息：维度、坐标（x, y, z）、旋转（yaw, pitch）
- 游戏模式：生存、创造、冒险、旁观
- 物品栏和末影箱（计划中）

**注意事项**：
- 使用当前加入的玩家作为假人的创建者
- 删除假人会自动删除驻留数据和区块加载票据
- 每个世界独立存储，避免同名存档冲突
- 区块加载票据确保假人所在区块不会被卸载

**参考实现**：
- 完全参考 GugleCarpetAddition (GCA) 的假人驻留实现
- 使用与 GCA 相同的 SavedData 架构
- 区块加载票据机制确保假人区块始终活跃

#### 保留假人状态
- **配置项**：`preserveBotState`
- **类型**：布尔值
- **默认值**：`false`
- **说明**：不但驻留，而且保留退出前的完整状态和动作。

**保留的状态**（v1.1.1a 扩展）：
- **动作状态**：攻击、使用、潜行、跳跃、疾跑
- **移动状态**：前后移动、左右移动
- **间隔动作**：攻击间隔、使用间隔
- **生存状态**：健康值、饥饿值、饱和度、疲劳度
- **经验系统**：经验等级、经验进度
- **药水效果**：所有活跃的药水效果
- 间隔动作：攻击间隔、使用间隔
- 健康和饥饿：生命值、饱食度、饱和度

---

### 兼容性设置

#### Carpet Mod 兼容模式
- **配置项**：`carpetModCompatibility`
- **类型**：布尔值
- **默认值**：`true`
- **说明**：启用后，如果检测到 Carpet Mod，将自动禁用本模组的假人功能以避免冲突。

**工作原理**：
- 模组加载时自动检测 Carpet Mod
- 如果检测到且兼容模式启用，自动禁用假人功能
- 在日志中会显示检测结果和处理方式

**使用场景**：
- **启用（推荐）**：避免与 Carpet Mod 冲突，只使用 Carpet Mod 的假人功能
- **禁用**：同时使用两个模组的假人功能（可能有冲突风险）

**注意事项**：
- 修改此选项后**立即生效**，无需重启游戏
- 如果禁用兼容模式，建议只使用其中一个模组的假人命令

---

## 配置界面

### 主配置界面

打开配置菜单后，你会看到：

1. **启用假人功能**：总开关复选框
2. **假人功能配置**：进入详细配置
3. **快捷键配置**：查看和修改快捷键
4. **关于 & 帮助**：查看模组信息和帮助
5. **重置为默认配置**：恢复所有默认设置
6. **完成**：保存并关闭

---

### 假人功能配置界面

分为 4 个分类：

#### 1. 攻击设置
- 攻击距离（格）
- 创造模式攻击距离（格）
- 启用杀戮光环
- 杀戮光环范围（格）

#### 2. 骑乘设置
- 允许骑乘其他假人
- 编辑骑乘白名单（按钮）

#### 3. 生存设置
- 假人最大数量（0=无限）
- 允许非 OP 创建假人
- 死亡自动重生
- 假人受到伤害
- 假人会饥饿
- 自动跳跃

#### 4. 驻留设置
- 假人驻留
- 保留假人状态

#### 5. 兼容性设置
- Carpet Mod 兼容模式

---

### 骑乘白名单编辑界面

**功能**：
- 显示当前白名单
- 添加新的实体类型
- 删除实体类型（点击）
- 滚动查看（鼠标滚轮）

**实体 ID 格式**：
```
minecraft:pig
minecraft:horse
minecraft:boat
```

**常用实体 ID**：
- `minecraft:pig` - 猪
- `minecraft:horse` - 马
- `minecraft:donkey` - 驴
- `minecraft:mule` - 骡
- `minecraft:llama` - 羊驼
- `minecraft:boat` - 船
- `minecraft:chest_boat` - 运输船
- `minecraft:minecart` - 矿车
- `minecraft:strider` - 炽足兽

---

## 高级配置

### 手动编辑配置文件

1. 关闭游戏
2. 打开 `.minecraft/config/my-bot-mod.json`
3. 使用文本编辑器修改
4. 保存文件
5. 启动游戏

**注意**：
- 确保 JSON 格式正确
- 数值类型要匹配
- 布尔值使用 `true` 或 `false`（小写）

---

### 配置验证

配置文件会在加载时自动验证：
- 无效的数值会使用默认值
- 缺失的配置项会自动添加
- 错误的格式会在日志中警告

---

### 配置重置

#### 方法 1：配置界面
在主配置界面点击"重置为默认配置"按钮。

#### 方法 2：删除配置文件
删除 `.minecraft/config/my-bot-mod.json`，游戏会自动创建默认配置。

---

## 配置建议

### 单人游戏
```json
{
  "enableBotFeature": true,
  "maxBotCount": 0,
  "allowNonOpCreateBot": false,
  "autoRespawnOnDeath": true,
  "botTakeDamage": false,
  "botHunger": false,
  "botPersistence": true,
  "preserveBotState": true
}
```

### 多人服务器
```json
{
  "enableBotFeature": true,
  "maxBotCount": 10,
  "allowNonOpCreateBot": false,
  "autoRespawnOnDeath": false,
  "botTakeDamage": true,
  "botHunger": true,
  "botPersistence": false,
  "preserveBotState": false
}
```

### 创造模式建筑
```json
{
  "enableBotFeature": true,
  "creativeAttackReachDistance": 10.0,
  "enableKillAura": false,
  "botTakeDamage": false,
  "botHunger": false
}
```

### 生存模式自动化
```json
{
  "enableBotFeature": true,
  "attackReachDistance": 3.0,
  "enableKillAura": true,
  "killAuraRange": 5.0,
  "autoRespawnOnDeath": true,
  "botPersistence": true
}
```

---

## 常见问题

### Q: 配置修改后不生效？
A: 确保点击了"保存"按钮。部分配置需要重新创建假人才能生效。

### Q: 配置文件在哪里？
A: `.minecraft/config/my-bot-mod.json`（Windows）或 `~/.minecraft/config/my-bot-mod.json`（Linux/Mac）

### Q: 如何恢复默认配置？
A: 在配置界面点击"重置为默认配置"，或删除配置文件。

### Q: 假人驻留数据保存在哪里？
A: `world/data/my_bot_mod_bots.dat`（NBT 格式，使用 SavedData 系统）

### Q: 假人驻留在单人存档中不生效？
A: v1.1.1a 已修复此问题。现在改为在玩家加入时触发加载，支持单人存档。

### Q: 同名存档的假人数据会冲突吗？
A: v1.1.1a 已修复此问题。每个世界的假人数据独立存储，不会冲突。

### Q: 可以为不同世界设置不同配置吗？
A: 配置是全局的，但假人驻留数据是按世界保存的。

### Q: 如何与 Carpet Mod 兼容？
A: 
- **默认情况**：启用"Carpet Mod 兼容模式"，检测到 Carpet Mod 时自动禁用本模组假人功能
- **同时使用**：在配置中禁用"Carpet Mod 兼容模式"（可能有冲突）
- **建议**：只使用其中一个模组的假人功能

---

## 相关文档

- [命令参考](COMMANDS.md) - 所有命令的详细说明
- [皮肤系统](SKINS.md) - 皮肤加载和自定义
- [常见问题](FAQ.md) - 常见问题解答
