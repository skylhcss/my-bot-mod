# My Bot Mod - 假人模组

一个类似 Carpet Mod 的假人（机器人玩家）模组，适用于 Minecraft 1.20.1 Fabric。

## 功能特性

本模组提供了完整的假人（机器人玩家）功能，允许你通过命令创建和控制假人。

### 已实现功能

#### 1. 假人召唤
- `/bot <假人名字> spawn` - 在你的位置召唤假人
- `/bot <假人名字> spawn at <坐标>` - 在指定坐标召唤假人
- 假人默认使用与创建者相同的游戏模式
- **智能皮肤系统**：
  - 优先使用与假人同名的正版玩家皮肤（从 Mojang API 获取）
  - 如果找不到同名玩家，则从 `temporary` 文件夹随机选择一个皮肤
  - 支持 PNG 格式（64x64 或 64x32）和 Base64 编码的皮肤数据

#### 2. 假人控制

**攻击控制：**
- `/bot <假人名字> attack` - 攻击一次
- `/bot <假人名字> attack continuous` - 持续攻击
- `/bot <假人名字> stop attack` - 停止攻击

**使用物品：**
- `/bot <假人名字> use` - 使用物品一次
- `/bot <假人名字> use continuous` - 持续使用物品
- `/bot <假人名字> stop use` - 停止使用物品

**移动控制：**
- `/bot <假人名字> sneak` - 开始潜行
- `/bot <假人名字> unsneak` - 停止潜行
- `/bot <假人名字> sprint` - 开始疾跑
- `/bot <假人名字> unsprint` - 停止疾跑
- `/bot <假人名字> jump` - 开始跳跃
- `/bot <假人名字> stop jump` - 停止跳跃

**方向移动：**
- `/bot <假人名字> move forward` - 向前移动
- `/bot <假人名字> move backward` - 向后移动
- `/bot <假人名字> move left` - 向左移动
- `/bot <假人名字> move right` - 向右移动
- `/bot <假人名字> move stop` - 停止移动

**视角控制：**
- `/bot <假人名字> look up [角度]` - 向上看（默认15°）
- `/bot <假人名字> look down [角度]` - 向下看（默认15°）
- `/bot <假人名字> look left [角度]` - 向左看（默认15°）
- `/bot <假人名字> look right [角度]` - 向右看（默认15°）
- `/bot <假人名字> look north` - 看向北方
- `/bot <假人名字> look south` - 看向南方
- `/bot <假人名字> look east` - 看向东方
- `/bot <假人名字> look west` - 看向西方
- `/bot <假人名字> turn <偏航角> <俯仰角>` - 相对旋转视角

**物品操作：**
- `/bot <假人名字> drop` - 丢弃当前手持物品（一个）
- `/bot <假人名字> dropStack` - 丢弃当前手持物品（整组）
- `/bot <假人名字> swapHands` - 交换主副手物品

**骑乘控制：**
- `/bot <假人名字> mount` - 骑乘附近的实体
- `/bot <假人名字> dismount` - 下马/离开当前骑乘的实体

**综合控制：**
- `/bot <假人名字> stop` - 停止所有动作（攻击、使用、移动、跳跃、潜行、疾跑）

#### 3. 假人管理
- `/bot <假人名字> kill` - 移除指定假人
- `/bot list` - 列出所有假人及其创建者

#### 4. 快速测试 🧪
- `/bot test` - 运行所有自动化测试
- `/bot test movement` - 测试移动功能
- `/bot test actions` - 测试动作控制
- `/bot test skin` - 测试皮肤系统
- `/bot test all` - 运行所有测试

**测试内容包括：**
- ✅ 名字格式验证（3-16字符，只含字母数字下划线）
- ✅ 假人创建和删除
- ✅ 移动输入（前后左右）
- ✅ 动作控制（潜行、疾跑、视角旋转）
- ✅ 皮肤系统（Mojang API + 本地皮肤）
- ✅ 骑乘功能（防止骑到其他假人）

测试命令会自动创建临时假人，执行测试后自动清理，不会影响现有假人。

## 安装方法

1. 确保已安装 Fabric Loader 0.19.2 或更高版本
2. 确保已安装 Fabric API 0.92.8+1.20.1 或更高版本
3. 将模组 jar 文件放入 `.minecraft/mods` 文件夹
4. 启动游戏

## 使用示例

### 快速测试

```bash
# 运行完整的自动化测试
/bot test

# 测试特定功能
/bot test movement    # 测试移动
/bot test actions     # 测试动作
/bot test skin        # 测试皮肤
```

### 基本操作

```
# 召唤一个名为 "Steve" 的假人（会尝试使用正版 Steve 的皮肤）
/bot Steve spawn

# 召唤一个名为 "Notch" 的假人（会尝试使用正版 Notch 的皮肤）
/bot Notch spawn

# 在指定坐标召唤假人
/bot Steve spawn at 100 64 200

# 让假人持续攻击
/bot Steve attack continuous

# 让假人潜行
/bot Steve sneak

# 让假人向前移动
/bot Steve move forward

# 让假人看向北方
/bot Steve look north

# 让假人旋转视角（偏航45度，俯仰-30度）
/bot Steve turn 45 -30

# 让假人丢弃物品
/bot Steve drop

# 让假人交换主副手物品
/bot Steve swapHands

# 让假人骑乘附近的马
/bot Steve mount

# 让假人下马
/bot Steve dismount

# 停止假人的所有动作
/bot Steve stop

# 移除假人
/bot Steve kill

# 查看所有假人
/bot list
```

## 皮肤系统

### 皮肤获取优先级

1. **正版玩家皮肤**（最高优先级）
   - 假人会自动尝试从 Mojang API 获取与假人同名的正版玩家皮肤
   - 例如：`/bot Notch spawn` 会使用 Notch 的皮肤

2. **PNG 皮肤文件**（第二优先级）
   - 从 `run/temporary` 文件夹随机选择一个 PNG 文件
   - 支持 64x64 或 64x32 像素的标准 Minecraft 皮肤格式

3. **Base64 皮肤文件**（第三优先级）
   - 从 `run/temporary` 文件夹随机选择一个 .txt 文件
   - 文件内容为 Mojang API 返回的 Base64 编码数据

4. **默认皮肤**（最低优先级）
   - 如果以上都失败，使用 Minecraft 默认皮肤（Steve/Alex）

### 添加自定义皮肤

**重要**：皮肤文件应放在 `run/temporary` 文件夹中（不是项目根目录的 `temporary` 文件夹）。

**方法一：使用 PNG 文件**（推荐）
1. 准备 64x64 或 64x32 的 PNG 皮肤文件
2. 将文件放入 `run/temporary` 文件夹（如 `bot1.png`, `bot2.png`, `bot3.png`）
3. 重启游戏

**方法二：使用 Base64 编码**
1. 获取皮肤的 Base64 编码数据（可从 Mojang API 或 MineSkin.org 获取）
2. 创建 .txt 文件并保存 Base64 数据（如 `skin1.txt`）
3. 将文件放入 `run/temporary` 文件夹
4. 重启游戏

**注意事项**：
- `run/temporary` 文件夹会在首次运行游戏时自动创建
- 项目根目录的 `temporary` 文件夹仅用于开发参考
- 不要将 README.txt 命名为其他名字，它会被自动忽略

### 皮肤缓存

- 从 Mojang API 获取的皮肤会被缓存，避免重复请求
- 重启服务器会清除缓存

## 权限要求

所有 `/bot` 命令需要 OP 权限（权限等级 2）。

## 名字规则 ⚠️

假人名字必须符合 Minecraft 玩家名规范：
- ✅ **长度**：3-16 个字符
- ✅ **字符**：只能包含字母、数字和下划线
- ✅ **有效示例**：`Bot1`, `TestBot`, `Steve_123`, `MyBot`
- ❌ **无效示例**：
  - `ab` - 太短（少于3个字符）
  - `verylongbotname123` - 太长（超过16个字符）
  - `bot.test` - 包含点号
  - `bot-test` - 包含连字符
  - `机器人` - 包含非英文字符

如果使用无效名字，命令会显示详细的错误提示。

## 技术细节

- **Minecraft 版本**: 1.20.1
- **Fabric Loader**: 0.19.2+
- **Fabric API**: 0.92.8+1.20.1
- **Java 版本**: 17+

## 开发信息

本模组使用 Fabric 模组加载器开发，参考了 Carpet Mod 的假人功能设计。

### 项目结构
- `name.modid.bot.BotPlayer` - 假人玩家实体类
- `name.modid.bot.BotActionController` - 假人动作控制器
- `name.modid.bot.BotManager` - 假人管理器
- `name.modid.command.BotCommand` - 命令实现

## 许可证

CC0-1.0

## 作者

- Skyline_hcss
- Kiro AI
