# 开发文档

本文档面向想要为 My Bot Mod 贡献代码或了解其内部实现的开发者。

## 目录
- [项目结构](#项目结构)
- [核心架构](#核心架构)
- [v1.1.1a 重构说明](#v111a-重构说明)
- [构建项目](#构建项目)
- [开发环境](#开发环境)
- [代码规范](#代码规范)
- [测试](#测试)
- [贡献指南](#贡献指南)

---

## 项目结构

```
my-bot-mod/
├── src/
│   ├── main/
│   │   ├── java/name/modid/
│   │   │   ├── bot/                    # 假人核心
│   │   │   │   ├── BotPlayer.java      # 假人实体
│   │   │   │   ├── BotActionController.java  # 动作控制
│   │   │   │   ├── BotManager.java     # 假人管理
│   │   │   │   ├── BotPathfinder.java  # A*寻路系统（v1.2.1 新增）
│   │   │   │   ├── BotSkinManager.java # 皮肤管理
│   │   │   │   ├── BotSettings.java    # 假人个人配置三态覆盖（v1.3.0）
│   │   │   │   ├── BotPersistenceManager.java  # 驻留系统（v1.1.1a 重构）
│   │   │   │   └── FakeServerGamePacketListenerImpl.java  # 网络连接
│   │   │   ├── command/
│   │   │   │   └── BotCommand.java     # 命令系统
│   │   │   ├── config/
│   │   │   │   └── ModConfig.java      # 配置系统
│   │   │   ├── menu/                   # 容器菜单（v1.3.0）
│   │   │   │   ├── BotInventoryMenu.java   # 假人背包菜单
│   │   │   │   └── ModMenus.java           # 菜单注册
│   │   │   ├── net/                    # 网络同步（v1.3.0）
│   │   │   │   ├── BotNetworking.java      # S2C 打开面板
│   │   │   │   └── BotPanelData.java       # 面板快照数据
│   │   │   ├── mixin/
│   │   │   │   └── ServerPlayerMixin.java  # 核心 Mixin
│   │   │   └── MyBotMod.java           # 主类
│   │   └── resources/
│   │       ├── fabric.mod.json         # 模组元数据
│   │       └── my-bot-mod.mixins.json  # Mixin 配置
│   └── client/
│       ├── java/name/modid/client/
│       │   ├── screen/                 # 配置界面
│       │   │   ├── ModernConfigScreen.java  # 主配置界面
│       │   │   ├── pages/              # 配置页面
│       │   │   │   ├── ConfigPage.java      # 页面基类（支持折叠）
│       │   │   │   ├── GeneralPage.java     # 基础设置
│       │   │   │   ├── CombatPage.java      # 战斗设置
│       │   │   │   ├── SurvivalPage.java    # 生存设置
│       │   │   │   ├── AdvancedPage.java    # 高级设置
│       │   │   │   └── BotsPage.java        # 假人列表（v1.3.0）
│       │   │   ├── widget/             # UI 组件（v1.1.1a 重写）
│       │   │   │   ├── ModernButton.java
│       │   │   │   ├── ModernCheckbox.java
│       │   │   │   └── ModernSlider.java    # 滑块（完全重写）
│       │   │   ├── KeybindConfigScreen.java
│       │   │   ├── AboutScreen.java
│       │   │   ├── MountWhitelistScreen.java
│       │   │   └── BotPanelScreen.java  # 每假人设置面板（v1.3.0）
│       │   ├── menu/                   # 背包界面（v1.3.0）
│       │   │   └── BotInventoryScreen.java
│       │   ├── mixin/
│       │   │   ├── ClientPacketListenerMixin.java  # 皮肤纹理清理（v1.3.0）
│       │   │   └── PlayerInfoMixin.java  # 皮肤渲染 Mixin
│       │   ├── BotSkinTextureLoader.java  # 皮肤加载
│       │   ├── BotClientData.java  # 假人列表客户端缓存（v1.3.0）
│       │   └── MyBotModClient.java     # 客户端主类
│       └── resources/
│           └── my-bot-mod.client.mixins.json
├── docs/                               # 文档
├── run/                                # 开发运行目录
├── build.gradle                        # Gradle 构建脚本
└── README.md                           # 项目说明
```

---

## 核心架构

### 1. BotPlayer（假人实体）

**职责**：
- 继承自 `ServerPlayer`
- 管理假人生命周期
- 持有 `BotActionController` 实例

**关键方法**：
- `tick()`：每 tick 更新状态
- `die()`：死亡处理
- `hurt()`：伤害处理

**关键字段**：
- `actionController`：动作控制器
- `creatorUUID`：创建者 UUID
- `creatorName`：创建者名字

---

### 2. BotActionController（动作控制器）

**职责**：
- 控制假人的所有动作
- 管理动作状态和移动状态
- 参考 Carpet Mod 的 `EntityPlayerActionPack`

**关键方法**：
- `tick()`：更新动作状态
- `applyMovement()`：应用移动输入
- `performAttack()`：执行攻击
- `performUse()`：执行使用物品

**关键字段**：
- `forward`：前后移动值
- `strafing`：左右移动值
- `attacking`：攻击状态
- `using`：使用状态

---

### 2.5 BotPathfinder（寻路系统）

**v1.2.1 新增**

**职责**：
- A* 算法网格寻路
- 路径跟随和路标管理
- 卡住检测和自动重寻路

**关键方法**：
- `pathTo(BlockPos)`：开始寻路
- `tick()`：每tick更新路径跟随
- `cancelPath()`：取消寻路
- `findPath()`：A* 核心算法

**关键字段**：
- `currentPath`：当前路径（List<BlockPos>）
- `target`：目标位置
- `isPathfinding`：是否在寻路

---

### 3. BotPersistenceManager（驻留系统）

**v1.1.1a 重构**：

**新架构**：
- 继承自 `SavedData`
- 使用 Minecraft 的数据持久化系统
- 数据保存在 `world/data/my_bot_mod_bots.dat`

**关键改进**：
- 每个世界独立存储，避免同名存档冲突
- 支持单人存档（延迟加载机制）
- NBT 格式存储，更可靠
- 自动管理数据生命周期

**关键方法**：
- `get(MinecraftServer)`：获取或创建实例
- `save(CompoundTag)`：保存数据到 NBT
- `load(CompoundTag)`：从 NBT 加载数据
- `saveBot(BotPlayer)`：保存单个假人
- `loadAllBots(MinecraftServer)`：加载所有假人

---

### 4. ModernSlider（滑块组件）

**v1.1.1a 完全重写**：

**新特性**：
- 独立的拖拽状态（`isDragging`）
- 严格的边界检查
- 焦点管理系统
- 精确的碰撞检测

**关键改进**：
```java
// 严格的边界检查
private void setValueFromMouse(double mouseX) {
    if (mouseX < this.getX() || mouseX > this.getX() + this.width) {
        return; // 不在范围内，直接返回
    }
    // 计算新值...
}

// 焦点管理
@Override
public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
    if (!this.isFocused()) {
        return false; // 只响应获得焦点的滑块
    }
    // 处理键盘输入...
}
```

---

### 5. 背包 / 末影箱 / 设置面板（v1.3.0）

**打开方式**：对着假人右键 → 服务端 `UseEntityCallback` 拦截 → 下发 S2C `open_bot_panel` → 客户端打开 `BotPanelScreen`。

**关键类**：
- `menu/BotInventoryMenu`：`ExtendedScreenHandlerType` 容器菜单，直接以假人 `Inventory`（41 格）为后端，含盔甲/副手与查看者物品栏；`clickMenuButton` 设置手持槽位；`DataSlot` 同步手持槽位；`quickMoveStack` 实现 Shift 转移。
- `menu/ModMenus`：注册 `BOT_INVENTORY` 菜单类型（主初始化调用）。
- `net/BotNetworking` + `net/BotPanelData`：S2C `open_bot_panel` 数据包与快照。
- `client/menu/BotInventoryScreen`：原版风格容器界面，程序化绘制槽位，`InventoryScreen.renderEntityInInventoryFollowsMouse` 渲染假人模型，3x3 手持槽位选择板。
- `client/screen/BotPanelScreen`：全局配置界面风格设置面板（左侧操作/右侧个人配置），操作按钮通过 `sendCommand` 调用 `/bot` 命令，个人配置通过 C2S 包更新。
- 末影箱：直接复用原版 `ChestMenu.threeRows` 绑定 `bot.getEnderChestInventory()`。
- `client/mixin/ClientPacketListenerMixin`：玩家信息移除时释放 PNG 皮肤动态纹理，修复 GPU 纹理泄漏。
- `bot/BotSettings`：假人个人配置（三态覆盖，优先于全局），由 BotPlayer/BotActionController 解析，BotPersistenceManager 驻留。
- `client/screen/pages/BotsPage` + `client/BotClientData`：全局配置"假人"标签页与列表缓存（S2C `bot_list`）；SectionCard 支持标题栏折叠。
- 新增网络通道：S2C `bot_list`、C2S `update_bot_setting`（更新个人配置）、C2S `request_bot_list`。

**新增命令**：`inventory`、`enderchest`、`panel`、`slot`、`gamemode`、`tphere`（见 [COMMANDS.md](COMMANDS.md)）。

---

## v1.1.1a 重构说明

### 假人驻留系统重构

**参考实现**：GugleCarpetAddition

**主要变更**：
1. **存储方式**：独立 JSON 文件 → SavedData 系统
2. **数据位置**：`world/data/bots/*.json` → `world/data/my_bot_mod_bots.dat`
3. **数据格式**：JSON → NBT
4. **加载机制**：同步加载 → 延迟加载（延迟 1 秒）

**技术细节**：
```java
public class BotPersistenceManager extends SavedData {
    // 使用 SavedData 系统
    public static BotPersistenceManager get(MinecraftServer server) {
        ServerLevel overworld = server.getLevel(ServerLevel.OVERWORLD);
        return overworld.getDataStorage().computeIfAbsent(
            BotPersistenceManager::load,
            BotPersistenceManager::new,
            DATA_NAME
        );
    }
    
    // 延迟加载，支持单人存档
    server.tell(new TickTask(
        server.getTickCount() + 20, // 延迟 1 秒
        () -> { /* 加载假人 */ }
    ));
}
```

**优势**：
- ✅ 每个世界独立存储
- ✅ 避免同名存档冲突
- ✅ 支持单人存档
- ✅ 自动管理数据生命周期
- ✅ 更可靠的数据持久化

---

### 滑块组件重写

**问题**：
- 滑块互相干扰
- 拖动一个滑块时其他滑块也会动
- 键盘事件影响所有滑块

**解决方案**：
1. **独立状态管理**：每个滑块有自己的 `isDragging` 状态
2. **严格边界检查**：只响应在组件范围内的鼠标事件
3. **焦点管理**：键盘事件只响应获得焦点的滑块
4. **精确碰撞检测**：`clicked()` 和 `isMouseOver()` 使用严格范围检查

**代码对比**：
```java
// 修复前
private void setValueFromMouse(double mouseX) {
    this.value = Mth.clamp(...); // 没有边界检查
}

// 修复后
private void setValueFromMouse(double mouseX) {
    if (mouseX < this.getX() || mouseX > this.getX() + this.width) {
        return; // 严格检查
    }
    this.value = Mth.clamp(...);
}
```

---

### UI 界面简化

**变更**：
- 删除所有黄色分组标题（`§e§l标题`）
- 删除所有灰色说明文本（`§7说明...`）
- 只保留配置项本身

**原因**：
- 减少视觉干扰
- 提高界面清晰度
- 解决文字重叠问题
- 更符合现代 UI 设计

**示例**：
```java
// 修复前
drawGroupTitle(graphics, "攻击设置", currentY);
drawDescription(graphics, "设置假人的攻击距离", currentY + 20);
ModernSlider slider = new ModernSlider(...);

// 修复后
ModernSlider slider = new ModernSlider(...); // 只保留配置项
```

---

## 构建项目

### 前置要求
- JDK 17 或更高版本
- Gradle 8.0 或更高版本（使用 Gradle Wrapper）

### 构建命令
```bash
# 构建项目
./gradlew build

# 清理构建
./gradlew clean

# 运行客户端
./gradlew runClient

# 运行服务器
./gradlew runServer
```

### 输出文件
构建后的 JAR 文件位于：
```
build/libs/my-bot-mod-1.3.0.jar
```

---

## 开发环境

### IDE 设置

#### IntelliJ IDEA
1. 导入项目（Gradle 项目）
2. 等待 Gradle 同步完成
3. 运行配置会自动创建

#### Eclipse
1. 运行 `./gradlew eclipse`
2. 导入为 Eclipse 项目

#### VS Code
1. 安装 Java Extension Pack
2. 打开项目文件夹
3. Gradle 会自动识别

---

### 运行配置

#### 客户端
```bash
./gradlew runClient
```

#### 服务器
```bash
./gradlew runServer
```

#### 调试
在 IDE 中使用 Gradle 任务的调试模式。

---

## 代码规范

### Java 代码风格
- 使用 4 空格缩进
- 类名使用 PascalCase
- 方法名和变量名使用 camelCase
- 常量使用 UPPER_SNAKE_CASE
- 每行最多 120 个字符

### 注释规范
```java
/**
 * 类或方法的简要说明
 * 
 * @param param 参数说明
 * @return 返回值说明
 */
```

### 命名规范
- 类名：`BotPlayer`, `BotManager`
- 接口名：`IBotController`
- 包名：`name.modid.bot`

---

## 测试

### 运行测试
```bash
./gradlew test
```

### 游戏内测试
```
/bot test
```

测试套件包括：
- 假人创建和删除
- 移动和动作控制
- 攻击和使用物品
- 皮肤加载
- 配置系统

---

## 贡献指南

### 提交 Issue
1. 检查是否已有相同 Issue
2. 使用 Issue 模板
3. 提供详细的复现步骤
4. 附上日志和截图

### 提交 Pull Request
1. Fork 项目
2. 创建功能分支
3. 遵循代码规范
4. 添加测试
5. 更新文档
6. 提交 PR

### 开发流程
1. 创建 Issue 讨论功能
2. 获得批准后开始开发
3. 提交 PR 并等待审核
4. 根据反馈修改
5. 合并到主分支

---

## 相关资源

- [Fabric Wiki](https://fabricmc.net/wiki/)
- [Minecraft Wiki](https://minecraft.fandom.com/)
- [Carpet Mod](https://github.com/gnembon/fabric-carpet)
- [GugleCarpetAddition](https://github.com/Gu-ZT/gugle-carpet-addition)

---

**提示**：如果你有任何问题，欢迎在 GitHub 上提 Issue 或加入我们的 Discord 服务器。
