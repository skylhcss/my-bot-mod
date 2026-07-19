# 开发文档

面向想了解内部实现或贡献代码的开发者。基于 **Fabric 1.20.1 / Java 17 / Mojang mappings**。

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

## 构建与运行

```bash
./gradlew build       # 产物：build/libs/my-bot-mod-<version>.jar
./gradlew runClient   # 运行客户端
./gradlew runServer   # 运行服务端
```

GitHub Actions（`.github/workflows/build.yml`）在每次 push/PR 上构建并上传 artifact。

## 代码规范

4 空格缩进；类名 PascalCase、方法/变量 camelCase、常量 UPPER_SNAKE_CASE；公开 API 加 Javadoc。

## 贡献

Fork → 功能分支 → 遵循规范 → 更新文档 → 提交 PR。请附复现步骤/日志。

## 相关资源
[Fabric Wiki](https://fabricmc.net/wiki/) · [Carpet Mod](https://github.com/gnembon/fabric-carpet) · [GugleCarpetAddition](https://github.com/Gu-ZT/gugle-carpet-addition)
