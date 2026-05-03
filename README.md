# 我的机器人 (My Bot Mod) - Minecraft 假人模组

[![Minecraft](https://img.shields.io/badge/Minecraft-1.20.1-green.svg)](https://www.minecraft.net/)
[![Fabric](https://img.shields.io/badge/Fabric-0.92.2-blue.svg)](https://fabricmc.net/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LI0CENSE)
[![GitHub](https://img.shields.io/badge/GitHub-skylhcss%2Fmy--bot--mod-blue.svg)](https://github.com/skylhcss/my-bot-mod)

一个类似 Carpet Mod 的假人（机器人玩家）模组，用于 Minecraft 1.20.1 Fabric。

## ✨ 功能特性

### 核心功能
- 🤖 **假人管理**：创建、删除和控制假人玩家
- 🎮 **动作控制**：攻击、使用物品、移动、跳跃、潜行、疾跑
- 👀 **视角控制**：看向指定方向或位置
- 🎒 **物品操作**：丢弃物品、交换主副手
- 🐴 **骑乘系统**：骑乘实体（支持白名单）
- 🎨 **皮肤系统**：三级优先级（Mojang API → PNG → 默认）
- ⚔️ **攻击系统**：射线追踪 + 方块破坏 + 杀戮光环
- ⏱️ **间隔模式**：支持 once、continuous、interval 三种模式
- 💾 **假人驻留**：退出世界后假人依然存在（可选）
- ⚙️ **配置系统**：完整的图形化配置界面
- 🔧 **Carpet Mod 兼容**：自动检测并避免冲突

### 与 Carpet Mod 对比
| 功能 | 我的机器人 | Carpet Mod |
|------|-----------|------------|
| 假人创建/删除 | ✅ | ✅ |
| 移动控制 | ✅ | ✅ |
| 攻击/使用 | ✅ | ✅ |
| 间隔模式 | ✅ | ✅ |
| 皮肤系统 | ✅ | ✅ |
| 配置界面 | ✅ | 只在其附属mod中有 |
| 假人驻留 | ✅ | 只在其附属mod中有 |
| Carpet 兼容 | ✅ | N/A |
| Scarpet 集成 | ❌ | ✅ |
| **覆盖率** | **~90%** | **80%** |

**兼容性说明**：
- 默认启用 Carpet Mod 兼容模式
- 检测到 Carpet Mod 时自动禁用本模组假人功能
- 可在配置中禁用兼容模式以同时使用（可能有冲突）

## 📦 安装

### 前置要求
- Minecraft 1.20.1
- Fabric Loader 0.15.11+
- Fabric API 0.92.2+

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

### 打开配置界面
- 按 **B** 键打开配置菜单
- 或使用 Minecraft 设置 > 控制 > 按键绑定

## 📖 详细文档

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

### v1.1.1 (2026-05-03)
- ✅ UI布局重构：左侧边栏 + 右侧内容区域
- ✅ 修复滚动功能：完整的滚动支持和滚动条显示
- ✅ 组件尺寸优化：统一高度24px，合理间距
- ✅ 新增SidebarButton组件：专门的侧边栏导航按钮
- ✅ 视觉改进：更深的背景色，绿色高亮激活状态
- ✅ 交互优化：正确的鼠标事件处理和内容裁剪

### v1.0.0 (2026-05-02)
- ✅ 完整的假人系统
- ✅ 配置界面
- ✅ 皮肤系统
- ✅ 假人驻留
- ✅ 所有核心功能

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
