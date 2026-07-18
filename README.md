# 我的机器人 (My Bot Mod) - Minecraft 假人模组

[![Minecraft](https://img.shields.io/badge/Minecraft-1.20.1-green.svg)](https://www.minecraft.net/)
[![Fabric](https://img.shields.io/badge/Fabric-0.92.8-blue.svg)](https://fabricmc.net/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Version](https://img.shields.io/badge/Version-1.3.0-orange.svg)](https://github.com/skylhcss/my-bot-mod/releases)
[![GitHub](https://img.shields.io/badge/GitHub-skylhcss%2Fmy--bot--mod-blue.svg)](https://github.com/skylhcss/my-bot-mod)

一个类似 Carpet Mod 的假人（机器人玩家）模组，用于 Minecraft 1.20.1 Fabric。

## ✨ 功能特性

### 核心功能
- 🤖 **假人管理**：创建、删除和控制假人玩家
- 🎮 **动作控制**：攻击、使用物品、移动、跳跃、潜行、疾跑
- 👀 **视角控制**：看向指定方向或位置
- 🎒 **物品操作**：丢弃物品、交换主副手
- 🎒 **背包/末影箱**：对着假人右键打开设置面板，可编辑背包（主物品栏+盔甲+副手+手持槽位）与末影箱，改动实时同步
- 🖥️ **每假人设置面板**：右键假人打开，切换游戏模式/手持槽位，快速传送、停止、删除
- 🐴 **骑乘系统**：骑乘实体（支持白名单）
- 🎨 **皮肤系统**：三级优先级（Mojang API → PNG → 默认）
- ⚔️ **攻击系统**：射线追踪 + 方块破坏 + 杀戮光环
- ⏱️ **间隔模式**：支持 once、continuous、interval 三种模式
- 💾 **假人驻留**：退出世界后假人依然存在（可选）
- 🦘 **自动跳跃**：移动时自动检测并跳过1格高障碍
- 🗺️ **自动寻路**：A* 算法寻路到指定坐标
- ⚙️ **配置系统**：完整的图形化配置界面
- 🔧 **Carpet Mod 兼容**：自动检测并避免冲突

### 与 Carpet Mod 对比
| 功能          | 我的机器人    | Carpet Mod |
|-------------|----------|------------|
| 假人创建/删除     | ✅        | ✅          |
| 移动控制        | ✅        | ✅          |
| 攻击/使用       | ✅        | ✅          |
| 间隔模式        | ✅        | ✅          |
| 皮肤系统        | ✅        | ✅          |
| 自动寻路        | ✅        | ❌          |
| 配置界面        | ✅        | 只在其附属mod中有 |
| 假人驻留        | ✅        | 只在其附属mod中有 |
| Carpet 兼容   | ✅        | N/A        |
| Scarpet 集成  | ❌        | ✅          |
| **覆盖率**     | **~95%** | **80%**    |

**兼容性说明**：
- 默认启用 Carpet Mod 兼容模式
- 检测到 Carpet Mod 时自动禁用本模组假人功能
- 可在配置中禁用兼容模式以同时使用（可能有冲突）

## 📦 安装

### 前置要求
- Minecraft 1.20.1
- Fabric Loader 0.19.2+
- Fabric API 0.92.8+

### 安装步骤
1. 下载最新版本的模组 JAR 文件
2. 将 JAR 文件放入 `.minecraft/mods` 文件夹
3. 启动游戏

## 🚀 快速开始

### 创建假人
```
/bot <名字> spawn
/bot <名字> spawn at <x> <y> <z>
```

### 控制假人
```
/bot <名字> attack continuous    # 持续攻击
/bot <名字> move forward         # 向前移动
/bot <名字> goto <x> <y> <z>   # 寻路到指定位置
/bot <名字> look up              # 看向上方
/bot <名字> sneak                # 潜行
/bot <名字> jump                 # 跳跃
```

### 管理假人
```
/bot list                        # 列出所有假人
/bot <名字> stop                 # 停止所有动作
/bot <名字> kill                 # 删除假人
```

### 配置管理
```
/botmod config                   # 显示所有配置
/botmod config set <配置项> <值>  # 设置配置
/botmod whitelist list           # 查看骑乘白名单
/botmod info                     # 显示模组信息
```

### 假人背包 / 末影箱 / 设置面板
**对着假人右键**即可打开该假人的设置面板（全局配置界面风格，左侧为操作、右侧为个人配置），面板内可：
- 打开背包（可编辑：主物品栏、盔甲、副手，并设置手持槽位；界面左侧显示假人模型）
- 打开末影箱（可编辑）
- 切换游戏模式、设置手持槽位、停止动作、传送到我、删除假人
- 右侧个人配置（受伤/饥饿/死亡重生/自动跳跃/杀戮光环，三态：继承/开/关，**优先于全局配置**）

也可在全局配置界面（B 键）的"假人"标签页点击假人打开其设置面板。也可用命令：
```
/bot <名字> inventory     # 打开背包
/bot <名字> enderchest    # 打开末影箱
/bot <名字> panel         # 打开设置面板
```

### 打开配置界面
- 按 **B** 键打开配置菜单
- 或使用 Minecraft 设置 > 控制 > 按键绑定

## 📖 详细文档

- [更新日志](CHANGELOG.md) - 版本更新历史
- [命令参考](docs/COMMANDS.md) - 所有命令的详细说明
- [配置指南](docs/CONFIG.md) - 配置系统使用指南
- [皮肤系统](docs/SKINS.md) - 皮肤加载和自定义
- [开发文档](docs/DEVELOPMENT.md) - 开发者指南
- [常见问题](docs/FAQ.md) - 常见问题解答

## 🎨 皮肤系统

### 皮肤优先级
1. **Mojang API**：如果假人名字是正版玩家名，获取其皮肤
2. **PNG 文件**：从 `skins/` 文件夹随机选择
3. **Base64 文件**：从 `skins/` 文件夹的 .txt 文件加载
4. **默认皮肤**：使用 Minecraft 默认皮肤

### 添加自定义皮肤
1. 将 PNG 文件（64x64 或 64x32）放入 `skins/` 文件夹
2. 重启游戏或使用 `/bot reload` 命令
3. 创建假人时会随机使用皮肤

详见 [皮肤系统文档](docs/SKINS.md)

## ⚙️ 配置系统

### 配置文件
配置文件位于：`config/my-bot-mod.json`

### 主要配置项
- **总开关**：启用/禁用假人功能
- **攻击设置**：攻击距离、杀戮光环
- **骑乘设置**：白名单、允许骑乘其他假人
- **生存设置**：数量限制、权限、死亡重生、伤害、饥饿
- **驻留设置**：假人驻留、保留状态

详见 [配置指南](docs/CONFIG.md)

## 🔧 开发

### 构建项目
```bash
./gradlew build
```

### 运行测试
```bash
./gradlew runClient
```

在游戏中使用 `/bot test` 运行测试套件。

详见 [开发文档](docs/DEVELOPMENT.md)

## 📝 更新日志

### v1.3.0 (2026-07-18) - 背包 / 末影箱 / 设置面板
- 🎒 新增可编辑的假人背包界面（主物品栏 + 盔甲 + 副手 + 手持槽位，原版风格，左侧渲染假人模型）
- 🧰 新增可编辑的假人末影箱界面
- 🖥️ 新增每个假人独立的设置面板（右键假人打开）
- 🖱️ 打开方式：对着假人右键
- ⚙️ 设置面板为全局配置界面风格：左侧操作，右侧假人个人配置（三态，优先于全局）
- 🗂️ 全局配置界面支持折叠分类，新增"假人"标签页（点击假人打开其设置）
- 🐛 修复皮肤请求阻塞主线程、死亡自动重生不可靠、命令权限使用过期配置、PNG 纹理泄漏等 Bug
- 🔧 新增 inventory/enderchest/panel/slot/gamemode/tphere 命令

### v1.2.1a (2026-06-20) - 自动跳跃与寻路系统
- 🦘 新增假人自动跳跃功能（使用 `horizontalCollision` 碰撞检测，与玩家一致）
- 🗺️ 新增 A* 寻路系统（借鉴 Baritone 设计，`/bot <name> goto` 命令）
- ⚙️ 新增 `allowBotAutoJump` 配置项
- 🐛 修复 killAura 伤害异常、友军误伤、生存挖掘、反射、内存泄漏等 18 个 Bug
- 🔧 BotPathfinder 完全重写 + 物品栏/末影箱/药水效果完整驻留支持

### v1.2.0 (2026-06-14) - UI 全面重写 + use 命令重构
- 🎨 配置界面全新全屏面板布局，Section 卡片分组
- ✨ `/bot use` 模拟完整右键交互（放置方块、实体交互、使用物品）
- 🐛 修复滑动条、交互、Tab状态、Checkbox反射等 11 个 Bug

### v1.1.1a (2026-05-05) - 假人驻留系统完全重写
- 🔄 **假人驻留系统完全重写**，参考 GugleCarpetAddition (GCA) 的实现
  - 使用 SavedData 系统持久化假人数据
  - 添加区块加载票据（Chunk Ticket）机制，确保假人所在区块保持加载
  - 在第一个玩家加入时自动加载假人（而非服务器启动时）
  - 支持跨维度的假人驻留（主世界、下界、末地）
  - 每 5 秒自动刷新区块加载票据
  - 假人每 10 秒自动保存一次状态
- ✨ **扩展假人状态保存**
  - 新增经验等级和进度保存
  - 新增药水效果保存
  - 新增疲劳度保存
  - 计划添加物品栏和末影箱保存
- 🔧 **优化假人管理**
  - 创建假人时自动添加区块加载票据
  - 删除假人时自动清理区块加载票据
  - 假人移动到新区块时自动更新票据
  - 服务器关闭时清理所有区块加载票据
- 📝 **更新文档**
  - 详细说明假人驻留的工作原理
  - 添加区块加载机制的说明
  - 更新配置文档

详见 [CHANGELOG](CHANGELOG.md) 文件。

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

## 📄 许可证

本项目采用 MIT 许可证。详见 [LICENSE](LICENSE) 文件。

## 🙏 致谢

- [Carpet Mod](https://github.com/gnembon/fabric-carpet) - 灵感来源
- [Fabric](https://fabricmc.net/) - 模组加载器
- Minecraft 社区

## 📞 联系方式

- **作者**: Skyline_hcss、Kiro AI
- **邮箱**: Skyline.hcss@gmail.com
- **GitHub**: [https://github.com/skylhcss/my-bot-mod](https://github.com/skylhcss/my-bot-mod)
- **Issues**: [问题反馈](https://github.com/skylhcss/my-bot-mod/issues)

---

**注意**：本模组仅用于单人游戏和私人服务器。请遵守服务器规则。
