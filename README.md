# My Bot Mod · 我的机器人

[![Minecraft](https://img.shields.io/badge/Minecraft-1.20--1.21.4-green.svg)](https://www.minecraft.net/)
[![Fabric](https://img.shields.io/badge/Fabric-0.92.8-blue.svg)](https://fabricmc.net/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Version](https://img.shields.io/badge/Version-1.3.3-orange.svg)](https://github.com/skylhcss/my-bot-mod/releases)

> A Carpet-style **fake player (bot)** mod for **Minecraft 1.20–1.21.4 (Fabric)**.
> 一个类似 Carpet Mod 的**假人（机器人玩家）**模组，用于 **Minecraft 1.20–1.21.4 Fabric**。

**English** | [简体中文](#简体中文)

---

## English

Spawn player-like bots, control their actions, send them around with smart pathfinding, and keep them in your world across restarts. One codebase, multiple Minecraft versions (via Stonecutter).

### ✨ Features
- 🤖 **Bot management** — create, remove and control fake players (registered via the vanilla `placeNewPlayer` path, so scoreboards, teleporting and other vanilla logic work normally)
- 🎮 **Action control** — attack, use, move, jump, sneak, sprint, look, drop, swap hands, ride
- 🪄 **Command Baton** — a handheld item with an on-screen HUD; `Ctrl+Scroll` to switch modes; command a bot to **pathfind** or **teleport** to wherever you look
- 🗺️ **Smart pathfinding** — optimized A* (one-shot search bounded by iteration/node caps; boxing-free hash structures, lazy deletion, block-state cache, admissible heuristic) with width-aware path smoothing (configurable), swimming, gap-crossing, hazard avoidance and stuck-detour/give-up logic (same-dimension only)
- 🎒 **Inventory / Ender Chest / settings panel** — right-click a bot to edit its inventory, switch game mode, teleport, delete, and set per-bot options (7 tri-state overrides incl. glowing & fire immunity)
- 🧩 **Behavior scripts** — program bots with a Scratch-like **visual Blockly editor** (bundled in the JAR and auto-extracted to `config/my-bot-mod/editor/behavior-editor.html` on first launch, offline): variables, expressions, loops, conditions, 100+ blocks; assign multi-behavior playlists in-game and export data to files (txt/JSON/CSV)
- 🎨 **Skin system** — Mojang API → local PNG → Base64 → default, with graceful fallback
- 💾 **Bot persistence** — bots can stay in the world after you leave (optional); their chunks stay loaded, across dimensions
- ⚙️ **Configuration** — in-game GUI (press **B**): browser-style tabs, crafting-table home page and a bilingual search box; plus commands + JSON file
- 🌏 **Fully bilingual** — every message and GUI text in English & 简体中文

### 📦 Installation
Supports **Minecraft 1.20–1.21.4** (pick the JAR matching your version: `+1.20.1` for 1.20–1.20.1, `+1.20.2` for 1.20.2, `+1.20.4` for 1.20.3–1.20.4, `+1.20.6` for 1.20.5–1.20.6, `+1.21.1` for 1.21–1.21.1, `+1.21.3` for 1.21.2–1.21.3, `+1.21.4` for 1.21.4). Requires **Fabric Loader 0.19.2+**, **Fabric API** and **Java 17+** (**Java 21+** for 1.20.5 and later). Drop the JAR into `.minecraft/mods`.

### 🚀 Quick start
```
/bot Steve spawn              # create a bot
/bot Steve goto 100 64 200    # pathfind to coordinates
/bot Steve attack continuous  # keep attacking
/bot list                     # list bots
/bot Steve kill               # remove a bot
```
**Right-click a bot** to open its panel (inventory / ender chest / game mode / teleport / delete + per-bot settings). Press **B** for the global config screen.

### 🪄 Command Baton
Crafted from two sticks; unstackable. While **held**, the top-left shows the current mode and the selected bot, and the top-right lists your bots.

| Action | Effect |
|--------|--------|
| `Ctrl + Scroll` | Switch mode (Pathfind / Teleport) |
| `Alt + Scroll` / `Alt + right-click a bot` | Select a bot |
| `Right-click somewhere` | Make the selected bot **pathfind** / **teleport** to that spot |

- **Teleport mode** is Creative-only by default; open it up with `allowBatonTeleportNonCreative`, or require OP with `batonRequiresOp`.
- **Cross-dimension**: teleport works across dimensions; pathfinding is **same-dimension only** (cross-dimension orders are rejected).

### ⚙️ Configuration
Config file: `config/my-bot-mod.json` · Command: `/botmod config` · GUI: **B** key.
Common options: attack range, kill aura, max bot count, bot persistence, non-OP control (`allowNonOpControlBot`), baton teleport permission (`allowBatonTeleportNonCreative`).

### 📖 Documentation
[Commands](docs/COMMANDS.md) · [Config](docs/CONFIG.md) · [Behaviors](docs/BEHAVIORS.md) · [Skins](docs/SKINS.md) · [Development](docs/DEVELOPMENT.md) · [FAQ](docs/FAQ.md) · [Changelog](CHANGELOG.md)

### 🔧 Development
```bash
./gradlew chiseledBuild   # build all Minecraft versions; artifacts in versions/<mc>/build/libs/
./gradlew :1.20.1:build   # build a single version
./gradlew :1.20.1:runClient   # run the client
```

### 📄 License
MIT License. Author: **Skyline_hcss**. Bots are intended for singleplayer and private servers — please follow your server's rules.

---

<a id="简体中文"></a>
## 简体中文

一个类似 Carpet Mod 的假人（机器人玩家）模组，用于 Minecraft 1.20–1.21.4 Fabric（单代码库多版本，Stonecutter 构建）。

### ✨ 功能特性
- 🤖 **假人管理**：创建、删除、控制假人玩家（`placeNewPlayer` 规范注册，计分板/传送等原版逻辑正常）
- 🎮 **动作控制**：攻击、使用、移动、跳跃、潜行、疾跑、视角、丢物、换手、骑乘
- 🪄 **指挥棒**（v1.3.1 新增）：手持道具，四周 HUD 显示信息，Ctrl+滚轮切模式，指挥假人**寻路**或**传送**到你看向的位置
- 🗺️ **智能寻路**：优化 A*（一次性同步搜索，受迭代/节点硬上限约束；免装箱哈希结构、懒删除、方块状态缓存、可采纳启发式），宽度感知路径平滑（可配置），支持游泳、跨越裂谷、危险规避，卡住自动绕行/放弃（仅限同维度）
- 🎒 **背包/末影箱/设置面板**：右键假人打开，可编辑背包、切换游戏模式、传送、删除，并含每假人 7 项三态个人配置（含发光、免疫火焰）
- 🧩 **行为脚本**：类 Scratch 的 **Blockly 图形化编辑器**（随 JAR 一并发布，首次启动自动释放到 `config/my-bot-mod/editor/behavior-editor.html`，离线可用）编排假人行为：变量/表达式/循环/条件、100+ 积木；游戏内分配多行为播放列表，可导出数据到文件（txt/JSON/CSV）
- 🎨 **皮肤系统**：Mojang API → PNG → Base64 → 默认，三级回退
- 💾 **假人驻留**：退出世界后假人可保留（可选），跨维度区块保持加载
- ⚙️ **配置系统**：图形界面（B 键）——浏览器式标签页、工作台风格主页、中英文搜索框；另有命令 + JSON 文件
- 🌏 **完全双语**：所有消息与界面文本均为中/英双语

### 📦 安装
支持 **Minecraft 1.20–1.21.4**（按版本选对应 JAR：`+1.20.1` 适用 1.20–1.20.1，`+1.20.2` 适用 1.20.2，`+1.20.4` 适用 1.20.3–1.20.4，`+1.20.6` 适用 1.20.5–1.20.6，`+1.21.1` 适用 1.21–1.21.1，`+1.21.3` 适用 1.21.2–1.21.3，`+1.21.4` 适用 1.21.4）。需 **Fabric Loader 0.19.2+**、**Fabric API** 与 **Java 17+**（1.20.5 及以上需 **Java 21+**）。将 JAR 放入 `.minecraft/mods` 即可。

### 🚀 快速开始
```
/bot Steve spawn              # 创建假人
/bot Steve goto 100 64 200   # 寻路到坐标
/bot Steve attack continuous # 持续攻击
/bot list                     # 列出假人
/bot Steve kill               # 删除假人
```
**右键假人**打开设置面板（背包/末影箱/游戏模式/传送/删除 + 个人配置）。按 **B** 键打开全局配置界面。

### 🪄 指挥棒（Command Baton）
两根木棍合成，不可堆叠。**手持**时屏幕左上显示模式与选中假人信息、右上显示假人列表。

| 操作 | 效果 |
|------|------|
| `Ctrl + 滚轮` | 切换模式（指挥寻路 / 传送） |
| `Alt + 滚轮` / `Alt + 右键看向假人` | 选择假人 |
| `右键看向某处` | 让选中假人**寻路**前往 / **传送**至该处 |

- **传送模式**默认仅当手持玩家处于创造模式时可用，可用配置项 `allowBatonTeleportNonCreative` 放开；`batonRequiresOp` 可要求 OP 权限。
- **跨维度**：传送支持跨维度；寻路**仅限同维度**（跨维度指令会被拒绝）。

### ⚙️ 配置
配置文件：`config/my-bot-mod.json`；命令：`/botmod config`；界面：**B** 键。常用项：攻击距离、杀戮光环、最大数量、假人驻留、非 OP 权限（`allowNonOpControlBot`）、指挥棒传送权限（`allowBatonTeleportNonCreative`）。

### 📖 文档
[命令参考](docs/COMMANDS.md) · [配置指南](docs/CONFIG.md) · [行为系统](docs/BEHAVIORS.md) · [皮肤系统](docs/SKINS.md) · [开发文档](docs/DEVELOPMENT.md) · [常见问题](docs/FAQ.md) · [更新日志](CHANGELOG.md)

### 📝 最新更新（v1.3.3-beta 预览版）
- 🧩 **假人行为系统**：类 Scratch 的 Blockly 图形化编辑器（**100+ 积木/11 分类**，含列表与文本处理，现代化深色 UI，离线可用）
- 🧵 **多脚本并行**：多个帽子块并联执行互不阻塞；每假人专属行为管理界面（右键面板），实时刷新
- ⚡ **事件积木**：行为启动/玩家发言/收到广播（跨假人协作）/血量过低/实体接近
- 📦 **容器拟真交互**：够不着先寻路靠近→看向→真实开箱→取出/放入/盘点（箱盖与声音生效）
- 📄 **外置输出**：行为可将数据写入 my-bot-mod-exports/（txt / JSON Lines / CSV）
- 🔧 含 v1.3.2 全部内容（配置界面重构、新配置项、寻路增强、全面双语等）
- 🗑️ 本版移除了实验性的编组与合作建造功能，聚焦假人核心与行为系统

### 📄 许可证
MIT License。作者：Skyline_hcss。本模组主要面向单人游戏和私人服务器，请遵守服务器规则。
