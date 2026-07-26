# 开发文档

面向想了解内部实现或贡献代码的开发者。基于 **Fabric / Java 17 / Mojang mappings**，通过 **Stonecutter** 单代码库支持 **1.20–1.20.4** 多个 Minecraft 版本。

## 项目结构

```
src/main/java/name/modid/
├── bot/                    # 假人核心
│   ├── BotPlayer            # 假人实体（继承 ServerPlayer）
│   ├── BotActionController  # 动作/移动/攻击/挖掘
│   ├── BotManager           # 创建/删除/查询（placeNewPlayer 注册）
│   ├── BotPathfinder        # 非阻塞 A* 寻路（游泳/跑酷/危险规避）
│   ├── BotSettings          # 每假人三态个人配置
│   ├── BotSkinManager       # 皮肤加载与 Mojang 异步获取
│   ├── BotPersistenceManager# 驻留（SavedData + 区块票据）
│   └── FakeServerGamePacketListenerImpl
├── command/                # BotCommand / BotModCommand
├── config/ModConfig        # 配置
├── item/                   # 指挥棒（v1.3.1）
│   ├── ModItems             # 物品注册
│   └── CommandBatonItem
├── menu/                   # BotInventoryMenu / ModMenus（容器菜单）
├── net/                    # BotNetworking（S2C 面板/列表, C2S 设置/指挥棒） / BotPanelData
├── mixin/ServerPlayerMixin # 每 tick 应用移动输入
└── MyBotMod                # 主入口

src/client/java/name/modid/client/
├── screen/                 # 配置界面（ModernConfigScreen + pages/ + widget/）+ BotPanelScreen
├── menu/BotInventoryScreen # 背包界面
├── baton/                  # 指挥棒客户端（v1.3.1）
│   ├── BatonClientState     # 模式/选中假人本地状态
│   ├── BatonInputHandler    # 射线选人/下令，发 C2S
│   └── BatonHudOverlay      # 手持四周 HUD
├── mixin/                  # ClientPacketListenerMixin, PlayerInfoMixin, MouseHandlerMixin(v1.3.1 滚轮)
├── BotSkinTextureLoader    # PNG 纹理加载（负缓存）
├── BotClientData           # 假人列表缓存
└── MyBotModClient          # 客户端入口
```

## 核心机制

- **假人实体**：`BotPlayer` 继承 `ServerPlayer`，通过 `BotManager.placeNewPlayer` 规范注册（进入 `playersByUUID`），`ServerPlayerMixin` 在 `tick` HEAD 应用移动输入。动作由 `BotActionController` 驱动（参考 Carpet 的 `EntityPlayerActionPack`）。
- **寻路**：`BotPathfinder` 为**分帧增量 A***（每 tick 有迭代预算，避免卡服），节点为"可占据位置"（陆地站立或水面/水下游泳）；支持跳跃、下落、跨越裂谷（跑酷）、危险规避；`pathTo`/`tick`/`cancelPath` 为公开 API。
- **指挥棒**：纯客户端状态（`BatonClientState`）+ 输入回调（`BatonInputHandler` 用视线射线选人/取目标）+ `MouseHandlerMixin`（Ctrl/Alt+滚轮）+ HUD。动作经 C2S `baton_action` 在服务端执行寻路/传送。
- **背包/面板**：右键假人 → 服务端 `UseEntityCallback` → S2C `open_bot_panel` → 客户端 `BotPanelScreen`；背包用 `ExtendedScreenHandlerType`（`BotInventoryMenu`）直接绑定假人 `Inventory`。
- **驻留**：`BotPersistenceManager extends SavedData`，NBT 存于 `world/data/`，区块加载票据记录维度并无条件刷新。

## 构建与运行（多版本 / Stonecutter）

项目采用 [Stonecutter](https://stonecutter.kikugie.dev/) 单代码库多版本构建，构建目标：

| 构建目标 | 覆盖版本 | Java |
|----------|----------|------|
| `1.20.1` | 1.20–1.20.1 | 17 |
| `1.20.2` | 1.20.2 | 17 |
| `1.20.4` | 1.20.3–1.20.4 | 17 |

```bash
./gradlew chiseledBuild            # 一键构建所有版本，产物：versions/<mc>/build/libs/
./gradlew "Set active project to 1.20.4"   # 切换 IDE/活动版本（就地翻转条件注释）
./gradlew :1.20.4:build            # 只构建单个版本
./gradlew :1.20.1:runClient        # 运行指定版本客户端（共用根目录 run/）
```

- 各版本依赖（`minecraft_version`/`fabric_api_version`/`mc_dep`）定义在 `versions/<mc>/gradle.properties`。
- 跨版本差异代码一律用 Stonecutter 条件注释包裹（活动版本分支为真实代码，其余分支为注释，切换时自动翻转）：

```java
//? if >=1.20.2 {
/*server.getPlayerList().placeNewPlayer(connection, bot, CommonListenerCookie.createInitial(profile));
*///?} else {
server.getPlayerList().placeNewPlayer(connection, bot);
//?}
```

- 已有的版本分支点：假人注册（`BotPlayer`/`BotManager`/`FakeServerGamePacketListenerImpl` 的 `CommonListenerCookie`）、`SavedData.Factory`（`BotPersistenceManager`）、皮肤 `PlayerSkin` record（`PlayerInfoMixin`）、`Screen.renderBackground` 签名（5 处界面）。

GitHub Actions（`.github/workflows/build.yml`）在每次 push/PR 上执行 `chiseledBuild` 并上传全版本 artifact。

## 代码规范

4 空格缩进；类名 PascalCase、方法/变量 camelCase、常量 UPPER_SNAKE_CASE；公开 API 加 Javadoc。

## 贡献

Fork → 功能分支 → 遵循规范 → 更新文档 → 提交 PR。请附复现步骤/日志。

## 相关资源
[Fabric Wiki](https://fabricmc.net/wiki/) · [Carpet Mod](https://github.com/gnembon/fabric-carpet) · [GugleCarpetAddition](https://github.com/Gu-ZT/gugle-carpet-addition)
