# 开发文档

面向想了解内部实现或贡献代码的开发者。基于 **Fabric / Java 17+（1.20.5+ 目标需 Java 21）/ Mojang mappings**，通过 **Stonecutter** 单代码库支持 **1.20–1.21.4** 多个 Minecraft 版本。

## 项目结构

```
src/main/java/name/modid/
├── bot/                    # 假人核心
│   ├── BotPlayer            # 假人实体（继承 ServerPlayer）
│   ├── BotActionController  # 动作/移动/攻击/挖掘
│   ├── BotManager           # 创建/删除/查询（placeNewPlayer 注册，含异常回滚）
│   ├── BotPathfinder        # 同步优化 A* 寻路（游泳/跑酷/危险规避）
│   ├── BotSettings          # 每假人三态个人配置
│   ├── BotSkinManager       # 皮肤加载与 Mojang 获取
│   ├── BotPersistenceManager# 驻留（SavedData + 区块票据）
│   └── FakeServerGamePacketListenerImpl
├── behavior/               # 行为脚本系统
│   ├── BehaviorManager      # 扫描/缓存/播放列表/运行态
│   ├── BehaviorParser       # JSON（format=1）→ 语句树，白名单校验
│   ├── BehaviorRuntime      # Scratch 式多线程解释器（预算/挂起/事件）
│   ├── BehaviorProgram / BehaviorValue / BehaviorParseException
│   ├── BotOutput            # 外置输出（txt/jsonl/csv/模板/表格，异步单线程写入，追加/覆盖）
│   └── BehaviorStorage      # 行为文件保存/读取（游戏内编辑器导出，目录校验）
├── command/                # BotCommand / BotModCommand
├── config/ModConfig        # 配置
├── item/                   # 指挥棒（v1.3.1）
│   ├── ModItems             # 物品注册
│   └── CommandBatonItem
├── menu/                   # BotInventoryMenu / ModMenus（容器菜单）
├── net/                    # BotNetworking（S2C 面板/列表/行为, C2S 设置/指挥棒/行为指令） / BotPanelData
├── mixin/ServerPlayerMixin # 每 tick 应用移动输入（含异常兑底）
└── MyBotMod                # 主入口

src/client/java/name/modid/client/
├── screen/                 # 配置界面（ModernConfigScreen + pages/ + widget/）+ BotPanelScreen + BotBehaviorScreen + AboutScreen
├── menu/BotInventoryScreen # 背包界面
├── baton/                  # 指挥棒客户端（v1.3.1）
│   ├── BatonClientState     # 模式/选中假人本地状态
│   ├── BatonInputHandler    # 射线选人/下令，发 C2S
│   └── BatonHudOverlay      # 手持四周 HUD
├── mixin/                  # ClientPacketListenerMixin, PlayerInfoMixin, MouseHandlerMixin(v1.3.1 滚轮)
├── BotSkinTextureLoader    # PNG 纹理加载（负缓存，文件名防穿越）
├── BotClientData           # 假人列表缓存
├── BehaviorClientData      # 行为列表状态缓存（S2C behavior_list，含运行进度）
├── editor/                 # 游戏内行为编辑器
│   ├── BehaviorModels       # format=1 JSON 模型与双向序列化（与解析器/HTML 编辑器兼容）
│   ├── BlockDef             # 积木/传感器元数据注册表（数据驱动表单）
│   ├── BehaviorEditorScreen # 主界面（分类/调色板/语句列表/嵌套导航/保存导出）
│   ├── StmtFormScreen       # 单积木参数表单
│   └── ExprField            # 表达式输入组件（7 类型切换，递归嵌套）
└── MyBotModClient          # 客户端入口
```

## 核心机制

- **假人实体**：`BotPlayer` 继承 `ServerPlayer`，通过 `BotManager.placeNewPlayer` 规范注册（进入 `playersByUUID`），`ServerPlayerMixin` 在 `tick` HEAD 应用移动输入。动作由 `BotActionController` 驱动（参考 Carpet 的 `EntityPlayerActionPack`）。
- **寻路**：`BotPathfinder` 为**同步优化 A***（发起时一次性完成搜索，以迭代/节点硬上限与 fastutil 免装箱哈希、懒删除、方块状态缓存、可采纳启发式约束开销），节点为"可占据位置"（陆地站立或水面/水下游泳）；支持跳跃、下落、跨越裂谷（跑酷）、危险规避；生成后可选**宽度感知视线路径平滑**（`pathfindingSmooth`，仅合并同高度且中心线与侧向净空均可站立的线段）；`pathTo`/`tick`/`cancelPath` 为公开 API。
- **指挥棒**：纯客户端状态（`BatonClientState`）+ 输入回调（`BatonInputHandler` 用视线射线选人/取目标）+ `MouseHandlerMixin`（Ctrl/Alt+滚轮）+ HUD。动作经 C2S `baton_action` 在服务端执行寻路/传送。
- **背包/面板**：右键假人 → 服务端 `UseEntityCallback` → S2C `open_bot_panel` → 客户端 `BotPanelScreen`；背包用 `ExtendedScreenHandlerType`（`BotInventoryMenu`）直接绑定假人 `Inventory`。
- **行为系统**：`BehaviorManager` 扫描 `config/my-bot-mod/behaviors/` 并由 `BehaviorParser` 解析为语句树（op/sensor 白名单校验）；`BehaviorRuntime` 以 Scratch 式多线程模型执行（每帽子块一线程、每线程每 tick 预算、挂起/事件触发），由 END_SERVER_TICK 驱动；启动时对播放列表取快照作为执行队列，"快速执行单个"走单行为临时队列（不改 ASSIGNED）；容器操作走拟真流程（寻路靠近→看向→openMenu）；输出经 `BotOutput` 异步单线程写入（追加/覆盖）。
- **游戏内行为编辑器**：客户端 `editor/` 包——`BehaviorModels` 维护与 `BehaviorParser` 完全一致的 format=1 JSON 模型（列表字面量必须写 `{"e":"list"}` 对象形式，裸数组会被解析器当语句块）；保存经 C2S `behavior_save` 由服务端 `BehaviorStorage` 校验（先解析、文件名清洗、拒绝 ".." 路径段）后写盘，默认目录自动重载；打开已有行为经 `behavior_source_request`/`behavior_source` 往返。
- **驻留**：`BotPersistenceManager extends SavedData`，NBT 存于 `world/data/`，区块加载票据记录维度并无条件刷新。

## 构建与运行（多版本 / Stonecutter）

项目采用 [Stonecutter](https://stonecutter.kikugie.dev/) 单代码库多版本构建，构建目标：

| 构建目标 | 覆盖版本 | Java |
|----------|----------|------|
| `1.20.1` | 1.20–1.20.1 | 17 |
| `1.20.2` | 1.20.2 | 17 |
| `1.20.4` | 1.20.3–1.20.4 | 17 |
| `1.20.6` | 1.20.5–1.20.6 | 21 |
| `1.21.1` | 1.21–1.21.1 | 21 |
| `1.21.3` | 1.21.2–1.21.3 | 21 |
| `1.21.4` | 1.21.4 | 21 |

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

- 已有的版本分支点：假人注册（`BotPlayer`/`BotManager`/`FakeServerGamePacketListenerImpl` 的 `CommonListenerCookie`）、`SavedData.Factory` 与 ItemStack/效果序列化（`BotPersistenceManager`）、皮肤 `PlayerSkin` record（`PlayerInfoMixin`）、重生后客户端实体重建包（`BotPlayer`：1.20.1 用 `ClientboundAddPlayerPacket`，1.20.2+ 用 `ClientboundAddEntityPacket`，1.21+ 用完整构造）、`Screen.renderBackground` 签名（5 处界面）、**网络层**（`BotNetworking`/`BotClientNetworking`：1.20.5+ Fabric 改 CustomPacketPayload，用 `RawPayload` 包装既有 buf 读写）、**扩展菜单**（`ModMenus`/`BotInventoryMenu`/`BotCommand.openInventory`：1.20.5+ 改 StreamCodec 数据对象）、伤害入口（1.21.2+ `Entity.hurt` 变 final，改覆盖 `hurtServer(ServerLevel, …)`）、跨维度传送（1.21.2+ `teleportTo` 增加 `Set<Relative>` 与 boolean 参数，见 `BotManager.teleportCrossLevel`）、`HudRenderCallback`/`UseItemCallback` 签名（1.21+/1.21.2+）。
- **注意**：源文件中不要使用 `'\\'` 反斜杠字符字面量（会使 Stonecutter 词法解析异常，导致该文件所有条件注释失效），用 `File.separatorChar` 等替代。

GitHub Actions（`.github/workflows/build.yml`）在每次 push/PR 上执行 `chiseledBuild` 并上传全版本 artifact。

## 代码规范

4 空格缩进；类名 PascalCase、方法/变量 camelCase、常量 UPPER_SNAKE_CASE；公开 API 加 Javadoc。

## 贡献

Fork → 功能分支 → 遵循规范 → 更新文档 → 提交 PR。请附复现步骤/日志。

## 相关资源
[Fabric Wiki](https://fabricmc.net/wiki/) · [Carpet Mod](https://github.com/gnembon/fabric-carpet) · [GugleCarpetAddition](https://github.com/Gu-ZT/gugle-carpet-addition)
