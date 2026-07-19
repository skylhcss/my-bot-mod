# 我的机器人 (My Bot Mod)

[![Minecraft](https://img.shields.io/badge/Minecraft-1.20.1-green.svg)](https://www.minecraft.net/)
[![Fabric](https://img.shields.io/badge/Fabric-0.92.8-blue.svg)](https://fabricmc.net/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Version](https://img.shields.io/badge/Version-1.3.1-orange.svg)](https://github.com/skylhcss/my-bot-mod/releases)

一个类似 Carpet Mod 的假人（机器人玩家）模组，用于 Minecraft 1.20.1 Fabric。

## ✨ 功能特性

- 🤖 **假人管理**：创建、删除、控制假人玩家（`placeNewPlayer` 规范注册，计分板/传送等原版逻辑正常）
- 🎮 **动作控制**：攻击、使用、移动、跳跃、潜行、疾跑、视角、丢物、换手、骑乘
- 🪄 **指挥棒**（v1.3.1 新增）：手持道具，四周 HUD 显示信息，Ctrl+滚轮切模式，指挥假人**寻路**或**传送**到你看向的位置
- 🗺️ **智能寻路**：非阻塞分帧 A*（不卡服），支持游泳、跨越裂谷、危险规避、跨维度
- 🎒 **背包/末影箱/设置面板**：右键假人打开，可编辑背包、切换游戏模式、传送、删除，并含每假人三态个人配置
- 🎨 **皮肤系统**：Mojang API → PNG → Base64 → 默认，三级回退
- 💾 **假人驻留**：退出世界后假人可保留（可选），跨维度区块保持加载
- ⚙️ **配置系统**：图形界面（B 键）+ 命令 + JSON 文件
- 🔧 **Carpet 兼容**：自动检测并避免冲突

## 📦 安装

需要 **Minecraft 1.20.1** + **Fabric Loader 0.19.2+** + **Fabric API 0.92.8+**。将 JAR 放入 `.minecraft/mods` 即可。

## 🚀 快速开始

```
/bot Steve spawn              # 创建假人
/bot Steve goto 100 64 200   # 寻路到坐标
/bot Steve attack continuous # 持续攻击
/bot list                     # 列出假人
/bot Steve kill               # 删除假人
```

**右键假人**打开设置面板（背包/末影箱/游戏模式/传送/删除 + 个人配置）。按 **B** 键打开全局配置界面。

## 🪄 指挥棒（Command Baton）

两根木棍合成，不可堆叠。**手持**时屏幕左上显示模式与选中假人信息、右上显示假人列表。

| 操作 | 效果 |
|------|------|
| `Ctrl + 滚轮` | 切换模式（指挥寻路 / 传送） |
| `Alt + 滚轮` / `Alt + 右键看向假人` | 选择假人 |
| `右键看向某处` | 让选中假人**寻路**前往 / **传送**至该处 |

- **传送模式**默认仅当手持玩家处于创造模式时可用，可用配置项 `allowBatonTeleportNonCreative` 放开。
- **跨维度**：传送直接跨维度；寻路会先把假人拉到你所在维度再寻路。

## 📖 文档

- [命令参考](docs/COMMANDS.md) · [配置指南](docs/CONFIG.md) · [皮肤系统](docs/SKINS.md) · [开发文档](docs/DEVELOPMENT.md) · [常见问题](docs/FAQ.md) · [更新日志](CHANGELOG.md)

## 🎨 皮肤

假人皮肤按 **Mojang API（正版玩家名）→ `skins/` 下的 PNG（64x64 / 64x32）→ Base64 .txt → 默认皮肤** 的顺序回退。将 PNG 放入 `skins/` 文件夹后重启即可随机使用。详见 [皮肤系统](docs/SKINS.md)。

## ⚙️ 配置

配置文件：`config/my-bot-mod.json`；命令：`/botmod config`；界面：**B** 键。常用项：攻击距离、杀戮光环、最大数量、假人驻留、非 OP 权限（`allowNonOpControlBot`）、指挥棒传送权限（`allowBatonTeleportNonCreative`）。详见 [配置指南](docs/CONFIG.md)。

## 🔧 开发

```bash
./gradlew build       # 构建，产物在 build/libs/
./gradlew runClient   # 运行客户端
```

详见 [开发文档](docs/DEVELOPMENT.md)。

## 📝 最新更新（v1.3.1）

- 🪄 新增**指挥棒**：手持 HUD + 指挥假人寻路/传送到准星位置（含跨维度、传送权限配置）
- 🗺️ **寻路全面重构**：非阻塞分帧 A*（不卡服）、游泳、跨越裂谷、危险规避、跨维度、修正到达判定
- 🐛 修复生存挖矿挖不动、配置损坏崩溃、驻留区块票据过期/跨维度泄漏、死亡重生忽略维度、皮肤加载刷屏、快捷键失效等多个 Bug
- ⚙️ 配置项 `allowNonOpCreateBot` 更名为 `allowNonOpControlBot`（旧配置自动迁移）

完整历史见 [CHANGELOG](CHANGELOG.md)。

## 📄 许可证

MIT License。作者：Skyline_hcss。

---

**注意**：本模组主要面向单人游戏和私人服务器，请遵守服务器规则。
